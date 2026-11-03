package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Dependente;
import TioDaPerua.BackEndJava.entities.Responsavel;
import TioDaPerua.BackEndJava.entities.Solicitacao;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.entities.Viagem;
import TioDaPerua.BackEndJava.entities.ViagemDependente;
import TioDaPerua.BackEndJava.repositories.*;
import TioDaPerua.BackEndJava.exceptions.ResourceNotFoundException;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.exceptions.ConflictException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    public List<Dependente> listarDependentesDisponiveis(Long idViagem) {
        return dependenteRepository.findDisponiveisParaViagem(idViagem);
    }

    public Solicitacao solicitarDependente(Long idViagem, Long idDependente) {
        Viagem viagem = viagemRepository.findById(idViagem)
                .orElseThrow(() -> new ResourceNotFoundException("Viagem não encontrada com id: " + idViagem));
        Dependente dependente = dependenteRepository.findById(idDependente)
                .orElseThrow(() -> new ResourceNotFoundException("Dependente não encontrado com id: " + idDependente));

        // Validar dependente possui escola
        if (dependente.getEscola() == null) {
            throw new BusinessException("Dependente deve estar vinculado a uma escola para solicitar transporte");
        }

        // Validar solicitação ainda não existe
        Optional<Solicitacao> existente = solicitacaoRepository.findPendenteByDependenteAndViagem(idDependente, idViagem);
        if (existente.isPresent()) {
            throw new ConflictException("Já existe uma solicitação pendente para este dependente nesta viagem");
        }

        // Criar solicitação
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

        return solicitacaoRepository.save(solicitacao);
    }

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

        solicitacao.setRespondido(true);
        solicitacao.setAceito(aceito);

        if (aceito) {
            // Criar vínculo ativo em ViagemDependente
            ViagemDependente vinculo = new ViagemDependente();
            vinculo.setViagem(solicitacao.getViagem());
            vinculo.setDependente(solicitacao.getDependente());
            vinculo.setAtivo(true);
            viagemDependenteRepository.save(vinculo);
        }

        Solicitacao salva = solicitacaoRepository.save(solicitacao);
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
                "Sua solicitação de transporte foi " + estado + ".");
        return salva;
    }
}
