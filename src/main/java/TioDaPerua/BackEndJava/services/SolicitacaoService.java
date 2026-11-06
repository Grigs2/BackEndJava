package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Dependente;
import TioDaPerua.BackEndJava.entities.Responsavel;
import TioDaPerua.BackEndJava.entities.Solicitacao;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.entities.Viagem;
import TioDaPerua.BackEndJava.entities.ViagemDependente;
import TioDaPerua.BackEndJava.enums.OrigemSolicitacao;
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

    @Autowired
    private SolicitacaoRepository solicitacaoRepository;

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

    public List<Dependente> listarDependentesDisponiveis(Long idViagem) {
        return listarDependentesDisponiveis(idViagem, null);
    }

    /**
     * Alunos que o motorista pode convidar para a viagem: com escola, fora da viagem e sem pedido pendente.
     * Se a viagem tem escola, só alunos dessa escola. O filtro por nome ignora maiúsculas e acentos.
     */
    @Transactional(readOnly = true)
    public List<Dependente> listarDependentesDisponiveis(Long idViagem, String nome) {
        Viagem viagem = viagemRepository.findById(idViagem)
                .orElseThrow(() -> new ResourceNotFoundException("Viagem não encontrada com id: " + idViagem));
        Long escolaId = viagem.getEscola() != null ? viagem.getEscola().getId() : null;
        String filtro = normalizar(nome);
        return dependenteRepository.findDisponiveisParaViagem(idViagem).stream()
                .filter(d -> escolaId == null || (d.getEscola() != null && escolaId.equals(d.getEscola().getId())))
                .filter(d -> filtro.isEmpty() || normalizar(d.getNome()).contains(filtro))
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

        solicitacao.setRespondido(false);
        solicitacao.setAceito(false);
        solicitacao.setDataInicio(LocalDateTime.now());
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

    /** Pedido de vaga feito pelo responsável do aluno; o motorista da viagem é avisado e responde. */
    @Transactional
    public Solicitacao solicitarPeloResponsavel(Long idViagem, Long idDependente, Long atorUsuarioId) {
        if (idViagem == null || idDependente == null || atorUsuarioId == null) {
            throw new BusinessException("Viagem, dependente e usuário autenticado são obrigatórios");
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
        solicitacao.setDataInicio(LocalDateTime.now());
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

    /** Resposta do responsável a um convite feito pelo motorista. */
    @Transactional
    public Solicitacao responderSolicitacao(Long idSolicitacao, boolean aceito, Long atorUsuarioId) {
        if (idSolicitacao == null || atorUsuarioId == null) {
            throw new BusinessException("Solicitação e usuário autenticado são obrigatórios");
        }
        Solicitacao solicitacao = solicitacaoRepository.findById(idSolicitacao)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada com id: " + idSolicitacao));

        if (solicitacao.getResponsavel() == null || solicitacao.getResponsavel().getUsuario() == null
                || !atorUsuarioId.equals(solicitacao.getResponsavel().getUsuario().getId())) {
            throw new AccessDeniedException("Somente o responsável da solicitação pode respondê-la");
        }
        if (solicitacao.isRespondido()) {
            throw new ConflictException("Solicitação já foi respondida");
        }
        if (solicitacao.getOrigemEfetiva() == OrigemSolicitacao.RESPONSAVEL) {
            throw new BusinessException("Este pedido foi feito por você e aguarda a resposta do motorista");
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
        Solicitacao solicitacao = solicitacaoRepository.findById(idSolicitacao)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada com id: " + idSolicitacao));
        Usuario usuarioMotorista = solicitacao.getViagem() == null || solicitacao.getViagem().getMotorista() == null
                ? null : solicitacao.getViagem().getMotorista().getUsuario();
        if (usuarioMotorista == null || !atorUsuarioId.equals(usuarioMotorista.getId())) {
            throw new AccessDeniedException("Somente o motorista da viagem pode responder este pedido");
        }
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
    @Transactional(readOnly = true)
    public List<Solicitacao> listarDoMotorista(Long idMotorista) {
        return solicitacaoRepository.findAbertasByMotoristaId(idMotorista);
    }

    private Solicitacao registrarResposta(Solicitacao solicitacao, boolean aceito) {
        solicitacao.setRespondido(true);
        solicitacao.setAceito(aceito);
        if (aceito) {
            ViagemDependente vinculo = new ViagemDependente();
            vinculo.setViagem(solicitacao.getViagem());
            vinculo.setDependente(solicitacao.getDependente());
            vinculo.setAtivo(true);
            viagemDependenteRepository.save(vinculo);
        }
        return solicitacaoRepository.save(solicitacao);
    }

    private void validarAlunoParaViagem(Viagem viagem, Dependente dependente) {
        if (dependente.getEscola() == null) {
            throw new BusinessException("Dependente deve estar vinculado a uma escola para solicitar transporte");
        }
        if (viagem.getEscola() != null && !Objects.equals(viagem.getEscola().getId(), dependente.getEscola().getId())) {
            throw new BusinessException("Esta viagem vai para " + viagem.getEscola().getNome()
                    + ", e o aluno estuda em outra escola");
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
    }

    static String normalizar(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
