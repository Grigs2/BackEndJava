package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Dependente;
import TioDaPerua.BackEndJava.entities.Escola;
import TioDaPerua.BackEndJava.entities.Responsavel;
import TioDaPerua.BackEndJava.entities.Solicitacao;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.entities.Viagem;
import TioDaPerua.BackEndJava.entities.ViagemDependente;
import TioDaPerua.BackEndJava.entities.ViagemEscola;
import TioDaPerua.BackEndJava.enums.OrigemSolicitacao;
import TioDaPerua.BackEndJava.enums.StatusSolicitacao;
import TioDaPerua.BackEndJava.enums.StatusVinculoEscola;
import TioDaPerua.BackEndJava.enums.TipoNotificacao;
import TioDaPerua.BackEndJava.repositories.*;
import TioDaPerua.BackEndJava.exceptions.ResourceNotFoundException;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.exceptions.ConflictException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Solicitações de transporte nos dois sentidos:
 * - o motorista convida um aluno para a viagem (origem MOTORISTA) e o responsável responde;
 * - o responsável pede vaga em uma viagem (origem RESPONSAVEL) e o motorista responde.
 */
@Service
public class SolicitacaoService {

    /** Prazo para o destinatário responder uma solicitação. */
    public static final int PRAZO_RESPOSTA_DIAS = 3;

    @Autowired
    private SolicitacaoRepository solicitacaoRepository;

    @Autowired
    private ViagemEscolaRepository viagemEscolaRepository;

    @Autowired
    private DependenteRepository dependenteRepository;

    @Autowired
    private ViagemRepository viagemRepository;

    @Autowired
    private ViagemDependenteRepository viagemDependenteRepository;

    @Autowired
    private NotificacaoService notificacaoService;

    @Autowired
    private ResponsavelRepository responsavelRepository;

    /**
     * Alunos que o motorista pode convidar para a viagem: com escola, fora da viagem e sem pedido pendente.
     * Se a viagem tem escola, só alunos dessa escola. Não há filtro por nome, para não expor a identidade do aluno.
     */
    @Transactional(readOnly = true)
    public List<Dependente> listarDependentesDisponiveis(Long idViagem) {
        Viagem viagem = viagemRepository.findById(idViagem)
                .orElseThrow(() -> new ResourceNotFoundException("Viagem não encontrada com id: " + idViagem));
        List<Long> escolaIds = viagem.getEscolas().stream().map(Escola::getId).toList();
        return dependenteRepository.findDisponiveisParaViagem(idViagem).stream()
                .filter(d -> escolaIds.isEmpty() || (d.getEscola() != null && escolaIds.contains(d.getEscola().getId())))
                .toList();
    }

    public Solicitacao solicitarDependente(Long idViagem, Long idDependente) {
        return solicitarDependente(idViagem, idDependente, null);
    }

    /** Convite do motorista para incluir o aluno na viagem; o responsável é avisado. */
    public Solicitacao solicitarDependente(Long idViagem, Long idDependente, Long atorUsuarioId) {
        Viagem viagem = viagemRepository.findById(idViagem)
                .orElseThrow(() -> new ResourceNotFoundException("Viagem não encontrada com id: " + idViagem));
        Dependente dependente = dependenteRepository.findById(idDependente)
                .orElseThrow(() -> new ResourceNotFoundException("Dependente não encontrado com id: " + idDependente));

        validarAlunoParaViagem(viagem, dependente);

        Solicitacao solicitacao = new Solicitacao();
        solicitacao.setViagem(viagem);
        solicitacao.setDependente(dependente);

        if (dependente.getResponsaveis() != null && !dependente.getResponsaveis().isEmpty()) {
            Responsavel responsavelPrincipal = dependente.getResponsavelCriador();
            if (responsavelPrincipal == null && dependente.getResponsaveis().size() == 1) {
                responsavelPrincipal = dependente.getResponsaveis().get(0);
            }
            solicitacao.setResponsavel(responsavelPrincipal);
        } else {
            throw new BusinessException("Dependente não possui responsáveis vinculados");
        }

        LocalDateTime agora = LocalDateTime.now();
        solicitacao.setRespondido(false);
        solicitacao.setAceito(false);
        solicitacao.setStatus(StatusSolicitacao.PENDENTE);
        solicitacao.setDataInicio(agora);
        solicitacao.setDataExpiracao(agora.plusDays(PRAZO_RESPOSTA_DIAS));
        solicitacao.setOrigem(OrigemSolicitacao.MOTORISTA);

        Solicitacao salva = solicitacaoRepository.save(solicitacao);
        Responsavel destino = solicitacao.getResponsavel();
        if (atorUsuarioId != null && destino != null && destino.getUsuario() != null && destino.getUsuario().getId() != null) {
            notificacaoService.criarNotificacoesAutomaticas(
                    atorUsuarioId,
                    List.of(destino.getUsuario().getId()),
                    "Convite de transporte",
                    "Você recebeu um convite para incluir " + dependente.getNome()
                            + " em uma viagem. Responda em Vínculos.",
                    new NotificacaoService.Contexto(TipoNotificacao.SOLICITACAO, null, dependente.getId(), "solicitacao:" + salva.getId()));
        }
        return salva;
    }

    /**
     * Pedido de vaga feito pelo responsável do aluno; o motorista da viagem é avisado e responde.
     * O responsável precisa confirmar que, se o motorista aceitar, os dados do dependente serão compartilhados.
     */
    @Transactional
    public Solicitacao solicitarPeloResponsavel(Long idViagem, Long idDependente, Long atorUsuarioId,
                                                Boolean compartilhamentoAutorizado) {
        if (idViagem == null || idDependente == null || atorUsuarioId == null) {
            throw new BusinessException("Viagem, dependente e usuário autenticado são obrigatórios");
        }
        if (!Boolean.TRUE.equals(compartilhamentoAutorizado)) {
            throw new BusinessException("Confirme que, se o motorista aceitar, os dados do dependente serão compartilhados com ele");
        }
        Responsavel responsavel = responsavelRepository.findByUsuarioId(atorUsuarioId)
                .orElseThrow(() -> new AccessDeniedException("Somente responsáveis podem pedir vaga"));
        Dependente dependente = dependenteRepository.findById(idDependente)
                .orElseThrow(() -> new ResourceNotFoundException("Dependente não encontrado com id: " + idDependente));
        boolean ehResponsavel = dependente.getResponsaveis() != null && dependente.getResponsaveis().stream()
                .anyMatch(r -> Objects.equals(r.getId(), responsavel.getId()));
        if (!ehResponsavel) {
            throw new AccessDeniedException("O dependente não pertence a este responsável");
        }
        Viagem viagem = viagemRepository.findById(idViagem)
                .orElseThrow(() -> new ResourceNotFoundException("Viagem não encontrada com id: " + idViagem));
        if (!Boolean.TRUE.equals(viagem.getAtivo())) {
            throw new BusinessException("Esta viagem não está mais ativa");
        }

        validarAlunoParaViagem(viagem, dependente);

        Solicitacao solicitacao = new Solicitacao();
        solicitacao.setViagem(viagem);
        solicitacao.setDependente(dependente);
        solicitacao.setResponsavel(responsavel);
        solicitacao.setRespondido(false);
        solicitacao.setAceito(false);
        solicitacao.setStatus(StatusSolicitacao.PENDENTE);
        solicitacao.setCompartilhamentoAutorizado(true);
        LocalDateTime agora = LocalDateTime.now();
        solicitacao.setDataInicio(agora);
        solicitacao.setDataExpiracao(agora.plusDays(PRAZO_RESPOSTA_DIAS));
        solicitacao.setOrigem(OrigemSolicitacao.RESPONSAVEL);
        Solicitacao salva = solicitacaoRepository.save(solicitacao);

        Usuario usuarioMotorista = viagem.getMotorista() != null ? viagem.getMotorista().getUsuario() : null;
        if (usuarioMotorista != null && usuarioMotorista.getId() != null) {
            notificacaoService.criarNotificacoesAutomaticas(
                    atorUsuarioId,
                    List.of(usuarioMotorista.getId()),
                    "Pedido de vaga",
                    responsavel.getNome() + " pediu vaga para " + dependente.getNome() + ". Responda em Vínculos.",
                    new NotificacaoService.Contexto(TipoNotificacao.SOLICITACAO, null, dependente.getId(), "solicitacao:" + salva.getId()));
        }
        return salva;
    }

    /**
     * Resposta do responsável a um convite feito pelo motorista.
     * Aceitar exige confirmar o compartilhamento dos dados do(s) dependente(s) com o motorista.
     */
    @Transactional
    public Solicitacao responderSolicitacao(Long idSolicitacao, boolean aceito, Boolean compartilhamentoAutorizado,
                                            Long atorUsuarioId) {
        if (idSolicitacao == null || atorUsuarioId == null) {
            throw new BusinessException("Solicitação e usuário autenticado são obrigatórios");
        }
        if (aceito && !Boolean.TRUE.equals(compartilhamentoAutorizado)) {
            throw new BusinessException("Para aceitar, confirme que os dados do dependente serão compartilhados com o motorista");
        }
        expirarVencidas();
        Solicitacao solicitacao = solicitacaoRepository.findById(idSolicitacao)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada com id: " + idSolicitacao));

        if (solicitacao.getResponsavel() == null || solicitacao.getResponsavel().getUsuario() == null
                || !atorUsuarioId.equals(solicitacao.getResponsavel().getUsuario().getId())) {
            throw new AccessDeniedException("Somente o responsável da solicitação pode respondê-la");
        }
        verificarPrazo(solicitacao);
        if (solicitacao.isRespondido()) {
            throw new ConflictException("Solicitação já foi respondida");
        }
        if (solicitacao.getOrigemEfetiva() == OrigemSolicitacao.RESPONSAVEL) {
            throw new BusinessException("Este pedido foi feito por você e aguarda a resposta do motorista");
        }

        if (aceito) {
            solicitacao.setCompartilhamentoAutorizado(true);
        }
        Solicitacao salva = registrarResposta(solicitacao, aceito);
        Usuario usuarioMotorista = solicitacao.getViagem() == null
                || solicitacao.getViagem().getMotorista() == null
                ? null : solicitacao.getViagem().getMotorista().getUsuario();
        if (usuarioMotorista == null || usuarioMotorista.getId() == null) {
            throw new ResourceNotFoundException("Motorista da solicitação não encontrado");
        }
        String estado = aceito ? "aceita" : "recusada";
        notificacaoService.criarNotificacoesAutomaticas(
                atorUsuarioId,
                List.of(usuarioMotorista.getId()),
                "Solicitação " + estado,
                "Sua solicitação de transporte foi " + estado + ".",
                new NotificacaoService.Contexto(TipoNotificacao.SOLICITACAO, null,
                        solicitacao.getDependente() == null ? null : solicitacao.getDependente().getId(),
                        "solicitacao-resposta:" + solicitacao.getId()));
        return salva;
    }

    /** Resposta do motorista a um pedido de vaga feito pelo responsável. */
    @Transactional
    public Solicitacao responderComoMotorista(Long idSolicitacao, boolean aceito, Long atorUsuarioId) {
        if (idSolicitacao == null || atorUsuarioId == null) {
            throw new BusinessException("Solicitação e usuário autenticado são obrigatórios");
        }
        expirarVencidas();
        Solicitacao solicitacao = solicitacaoRepository.findById(idSolicitacao)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada com id: " + idSolicitacao));
        Usuario usuarioMotorista = solicitacao.getViagem() == null || solicitacao.getViagem().getMotorista() == null
                ? null : solicitacao.getViagem().getMotorista().getUsuario();
        if (usuarioMotorista == null || !atorUsuarioId.equals(usuarioMotorista.getId())) {
            throw new AccessDeniedException("Somente o motorista da viagem pode responder este pedido");
        }
        verificarPrazo(solicitacao);
        if (solicitacao.isRespondido()) {
            throw new ConflictException("Solicitação já foi respondida");
        }
        if (solicitacao.getOrigemEfetiva() != OrigemSolicitacao.RESPONSAVEL) {
            throw new BusinessException("Este convite foi feito por você e aguarda a resposta do responsável");
        }

        Solicitacao salva = registrarResposta(solicitacao, aceito);
        Responsavel responsavel = solicitacao.getResponsavel();
        if (responsavel != null && responsavel.getUsuario() != null && responsavel.getUsuario().getId() != null) {
            String estado = aceito ? "aceito" : "recusado";
            String aluno = solicitacao.getDependente() != null ? solicitacao.getDependente().getNome() : "o aluno";
            notificacaoService.criarNotificacoesAutomaticas(
                    atorUsuarioId,
                    List.of(responsavel.getUsuario().getId()),
                    "Pedido de vaga " + estado,
                    "Seu pedido de vaga para " + aluno + " foi " + estado + ".",
                    new NotificacaoService.Contexto(TipoNotificacao.SOLICITACAO, null,
                            solicitacao.getDependente() == null ? null : solicitacao.getDependente().getId(),
                            "solicitacao-resposta:" + solicitacao.getId()));
        }
        return salva;
    }

    /** Solicitações em aberto das viagens do motorista (convites enviados e pedidos recebidos). */
    @Transactional
    public List<Solicitacao> listarDoMotorista(Long idMotorista) {
        expirarVencidas();
        return solicitacaoRepository.findAbertasByMotoristaId(idMotorista);
    }

    /** Marca como EXPIRADA as solicitações sem resposta cujo prazo de 3 dias já passou. */
    void expirarVencidas() {
        solicitacaoRepository.expirarVencidas(LocalDateTime.now(), StatusSolicitacao.EXPIRADA);
    }

    private void verificarPrazo(Solicitacao solicitacao) {
        if (solicitacao.getStatusEfetivo() == StatusSolicitacao.EXPIRADA
                || solicitacao.isExpiradaEm(LocalDateTime.now())) {
            throw new BusinessException("Esta solicitação expirou. O prazo para resposta é de "
                    + PRAZO_RESPOSTA_DIAS + " dias");
        }
    }

    private Solicitacao registrarResposta(Solicitacao solicitacao, boolean aceito) {
        solicitacao.setRespondido(true);
        solicitacao.setAceito(aceito);
        solicitacao.setStatus(aceito ? StatusSolicitacao.ACEITA : StatusSolicitacao.RECUSADA);
        if (aceito) {
            abrirAcordo(solicitacao);
            encerrarDuplicadas(solicitacao);
        }
        return solicitacaoRepository.save(solicitacao);
    }

    /**
     * Com o aluno já na viagem, outras solicitações em aberto para o mesmo aluno e a mesma viagem (por exemplo um convite
     * do motorista e um pedido do responsável feitos ao mesmo tempo) deixam de fazer sentido: são encerradas para não
     * continuarem aparecendo como "Aceitar/Recusar".
     */
    private void encerrarDuplicadas(Solicitacao aceita) {
        LocalDateTime agora = LocalDateTime.now();
        Long viagemId = aceita.getViagem().getId();
        for (Solicitacao outra : solicitacaoRepository.findPendentesByDependenteId(aceita.getDependente().getId())) {
            if (Objects.equals(outra.getId(), aceita.getId()) || outra.getViagem() == null
                    || !Objects.equals(outra.getViagem().getId(), viagemId)) {
                continue;
            }
            outra.setRespondido(true);
            outra.setAceito(false);
            outra.setStatus(StatusSolicitacao.RECUSADA);
            outra.setDataFim(agora);
            solicitacaoRepository.save(outra);
        }
    }

    /**
     * Acordo de transporte do dependente na viagem: é um ViagemDependente ativo, criado ou reativado ao aceitar.
     */
    private void abrirAcordo(Solicitacao solicitacao) {
        ViagemDependente vinculo = viagemDependenteRepository
                .findByViagemIdAndDependenteId(solicitacao.getViagem().getId(), solicitacao.getDependente().getId())
                .orElseGet(ViagemDependente::new);
        vinculo.setViagem(solicitacao.getViagem());
        vinculo.setDependente(solicitacao.getDependente());
        vinculo.setAtivo(true);
        viagemDependenteRepository.save(vinculo);
    }

    private void validarAlunoParaViagem(Viagem viagem, Dependente dependente) {
        if (dependente.getEscola() == null) {
            throw new BusinessException("Dependente deve estar vinculado a uma escola para solicitar transporte");
        }
        if (!viagem.getEscolas().isEmpty() && viagem.getEscolas().stream()
                .noneMatch(e -> Objects.equals(e.getId(), dependente.getEscola().getId()))) {
            String atendidas = viagem.getEscolas().stream().map(Escola::getNome).collect(java.util.stream.Collectors.joining(", "));
            throw new BusinessException("Esta viagem atende " + atendidas
                    + ", e o aluno estuda em " + dependente.getEscola().getNome());
        }
        Optional<Solicitacao> existente = solicitacaoRepository.findPendenteByDependenteAndViagem(dependente.getId(), viagem.getId());
        if (existente.isPresent()) {
            throw new ConflictException("Já existe uma solicitação pendente para este dependente nesta viagem");
        }
        boolean jaNaViagem = viagemDependenteRepository.findByViagemIdAndDependenteId(viagem.getId(), dependente.getId())
                .map(ViagemDependente::isAtivo)
                .orElse(false);
        if (jaNaViagem) {
            throw new ConflictException("O aluno já faz parte desta viagem");
        }
        Long motoristaId = viagem.getMotorista() != null ? viagem.getMotorista().getId() : null;
        boolean vinculoAtivo = motoristaId != null && viagemEscolaRepository.existsByEscola_IdAndMotorista_IdAndStatusIn(
                dependente.getEscola().getId(), motoristaId, List.of(StatusVinculoEscola.ATIVO));
        if (!vinculoAtivo) {
            throw new BusinessException("O motorista não tem vínculo ativo com a escola " + dependente.getEscola().getNome());
        }
    }

    static String normalizar(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
