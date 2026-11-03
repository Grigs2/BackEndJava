package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.repositories.DependenteRepository;
import TioDaPerua.BackEndJava.repositories.EscolaRepository;
import TioDaPerua.BackEndJava.repositories.MotoristaRepository;
import TioDaPerua.BackEndJava.repositories.NotificacaoRepository;
import TioDaPerua.BackEndJava.repositories.ResponsavelRepository;
import TioDaPerua.BackEndJava.repositories.SolicitacaoRepository;
import TioDaPerua.BackEndJava.repositories.ViagemDiaRepository;
import TioDaPerua.BackEndJava.repositories.ViagemDependenteRepository;
import TioDaPerua.BackEndJava.repositories.ViagemRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

@Service("resourceAuthorization")
public class AutorizacaoRecursoService {

    private final MotoristaRepository motoristaRepository;
    private final ResponsavelRepository responsavelRepository;
    private final EscolaRepository escolaRepository;
    private final DependenteRepository dependenteRepository;
    private final SolicitacaoRepository solicitacaoRepository;
    private final ViagemRepository viagemRepository;
    private final ViagemDiaRepository viagemDiaRepository;
    private final ViagemDependenteRepository viagemDependenteRepository;
    private final NotificacaoRepository notificacaoRepository;

    public AutorizacaoRecursoService(MotoristaRepository motoristaRepository,
                                     ResponsavelRepository responsavelRepository,
                                     EscolaRepository escolaRepository,
                                     DependenteRepository dependenteRepository,
                                     SolicitacaoRepository solicitacaoRepository,
                                     ViagemRepository viagemRepository,
                                     ViagemDiaRepository viagemDiaRepository,
                                     ViagemDependenteRepository viagemDependenteRepository,
                                     NotificacaoRepository notificacaoRepository) {
        this.motoristaRepository = motoristaRepository;
        this.responsavelRepository = responsavelRepository;
        this.escolaRepository = escolaRepository;
        this.dependenteRepository = dependenteRepository;
        this.solicitacaoRepository = solicitacaoRepository;
        this.viagemRepository = viagemRepository;
        this.viagemDiaRepository = viagemDiaRepository;
        this.viagemDependenteRepository = viagemDependenteRepository;
        this.notificacaoRepository = notificacaoRepository;
    }

    public boolean isMotorista(Authentication authentication, Long motoristaId) {
        Long usuarioId = usuarioId(authentication);
        return hasRole(authentication, "MOTORISTA") && usuarioId != null && motoristaId != null
                && motoristaRepository.existsByIdAndUsuario_Id(motoristaId, usuarioId);
    }

    public boolean isResponsavel(Authentication authentication, Long responsavelId) {
        Long usuarioId = usuarioId(authentication);
        return hasRole(authentication, "RESPONSAVEL") && usuarioId != null && responsavelId != null
                && responsavelRepository.existsByIdAndUsuario_Id(responsavelId, usuarioId);
    }

    public boolean isEscola(Authentication authentication, Long escolaId) {
        Long usuarioId = usuarioId(authentication);
        return hasRole(authentication, "ESCOLA") && usuarioId != null && escolaId != null
                && escolaRepository.existsByIdAndUsuario_Id(escolaId, usuarioId);
    }

    public boolean isResponsavelDoDependente(Authentication authentication, Long dependenteId) {
        Long usuarioId = usuarioId(authentication);
        if (!hasRole(authentication, "RESPONSAVEL") || usuarioId == null || dependenteId == null) {
            return false;
        }
        return responsavelRepository.findByUsuarioId(usuarioId)
                .map(responsavel -> dependenteRepository
                        .findByIdAndResponsavelId(dependenteId, responsavel.getId()).isPresent())
                .orElse(false);
    }

    public boolean isResponsavelDaSolicitacao(Authentication authentication, Long solicitacaoId) {
        Long usuarioId = usuarioId(authentication);
        return hasRole(authentication, "RESPONSAVEL") && usuarioId != null && solicitacaoId != null
                && solicitacaoRepository.pertenceAoUsuario(solicitacaoId, usuarioId);
    }

    public boolean isDonoDaViagem(Authentication authentication, Long viagemId) {
        Long usuarioId = usuarioId(authentication);
        return hasRole(authentication, "MOTORISTA") && usuarioId != null && viagemId != null
                && viagemRepository.existsByIdAndMotorista_Usuario_Id(viagemId, usuarioId);
    }

    public boolean isDonoDaExecucao(Authentication authentication, Long viagemDiaId) {
        Long usuarioId = usuarioId(authentication);
        return hasRole(authentication, "MOTORISTA") && usuarioId != null && viagemDiaId != null
                && viagemDiaRepository.existsByIdAndViagem_Motorista_Usuario_Id(viagemDiaId, usuarioId);
    }

    public boolean podeAcessarViagem(Authentication authentication, Long viagemId) {
        if (isDonoDaViagem(authentication, viagemId)) {
            return true;
        }
        Long usuarioId = usuarioId(authentication);
        return hasRole(authentication, "RESPONSAVEL") && usuarioId != null && viagemId != null
                && viagemDependenteRepository.existeVinculoResponsavel(viagemId, usuarioId);
    }

    public boolean podeAcessarExecucao(Authentication authentication, Long viagemDiaId) {
        if (isDonoDaExecucao(authentication, viagemDiaId)) {
            return true;
        }
        Long usuarioId = usuarioId(authentication);
        return hasRole(authentication, "RESPONSAVEL") && usuarioId != null && viagemDiaId != null
                && viagemDiaRepository.existeVinculoResponsavel(viagemDiaId, usuarioId);
    }

    public boolean isUsuarioAutenticado(Authentication authentication, Long usuarioId) {
        return usuarioId != null && usuarioId.equals(usuarioId(authentication));
    }

    public boolean isDestinatarioNotificacao(Authentication authentication, Long notificacaoId) {
        Long usuarioId = usuarioId(authentication);
        return usuarioId != null && notificacaoId != null
                && notificacaoRepository.existsByIdAndDestinatario_Id(notificacaoId, usuarioId);
    }

    private Long usuarioId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(("ROLE_" + role)::equals);
    }
}
