package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Dependente;
import TioDaPerua.BackEndJava.entities.Responsavel;
import TioDaPerua.BackEndJava.repositories.DependenteRepository;
import TioDaPerua.BackEndJava.repositories.EscolaRepository;
import TioDaPerua.BackEndJava.repositories.MotoristaRepository;
import TioDaPerua.BackEndJava.repositories.NotificacaoRepository;
import TioDaPerua.BackEndJava.repositories.ResponsavelRepository;
import TioDaPerua.BackEndJava.repositories.SolicitacaoRepository;
import TioDaPerua.BackEndJava.repositories.ViagemDiaRepository;
import TioDaPerua.BackEndJava.repositories.ViagemDependenteRepository;
import TioDaPerua.BackEndJava.repositories.ViagemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutorizacaoRecursoServiceTest {

    @Mock private MotoristaRepository motoristaRepository;
    @Mock private ResponsavelRepository responsavelRepository;
    @Mock private EscolaRepository escolaRepository;
    @Mock private DependenteRepository dependenteRepository;
    @Mock private SolicitacaoRepository solicitacaoRepository;
    @Mock private ViagemRepository viagemRepository;
    @Mock private ViagemDiaRepository viagemDiaRepository;
    @Mock private ViagemDependenteRepository viagemDependenteRepository;
    @Mock private NotificacaoRepository notificacaoRepository;

    private AutorizacaoRecursoService autorizacao;

    @BeforeEach
    void setUp() {
        autorizacao = new AutorizacaoRecursoService(
                motoristaRepository, responsavelRepository, escolaRepository,
                dependenteRepository, solicitacaoRepository, viagemRepository,
                viagemDiaRepository, viagemDependenteRepository, notificacaoRepository);
    }

    @Test
    void allowsOnlyTheAuthenticatedDriverToManageTheirProfile() {
        Authentication motorista = autenticacao("17", "ROLE_MOTORISTA");
        when(motoristaRepository.existsByIdAndUsuario_Id(9L, 17L)).thenReturn(true);

        assertTrue(autorizacao.isMotorista(motorista, 9L));
        assertFalse(autorizacao.isMotorista(autenticacao("18", "ROLE_MOTORISTA"), 9L));
        assertFalse(autorizacao.isMotorista(autenticacao("17", "ROLE_RESPONSAVEL"), 9L));
    }

    @Test
    void allowsAnyCurrentlyLinkedGuardianToAccessTheDependent() {
        Responsavel responsavel = new Responsavel();
        responsavel.setId(8L);
        Dependente dependente = new Dependente();
        dependente.setId(9L);
        when(responsavelRepository.findByUsuarioId(17L)).thenReturn(Optional.of(responsavel));
        when(dependenteRepository.findByIdAndResponsavelId(9L, 8L))
                .thenReturn(Optional.of(dependente));

        assertTrue(autorizacao.isResponsavelDoDependente(
                autenticacao("17", "ROLE_RESPONSAVEL"), 9L));
    }

    private Authentication autenticacao(String usuarioId, String authority) {
        return UsernamePasswordAuthenticationToken.authenticated(
                usuarioId, null, List.of(new SimpleGrantedAuthority(authority)));
    }
}
