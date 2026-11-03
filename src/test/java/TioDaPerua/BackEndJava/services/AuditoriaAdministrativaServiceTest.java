package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.AuditoriaAdministrativa;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.repositories.AuditoriaAdministrativaRepository;
import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditoriaAdministrativaServiceTest {

    @Mock private AuditoriaAdministrativaRepository auditoriaRepository;
    @Mock private UsuarioRepository usuarioRepository;

    private AuditoriaAdministrativaService service;

    @BeforeEach
    void setUp() {
        service = new AuditoriaAdministrativaService(auditoriaRepository, usuarioRepository);
    }

    @Test
    void persistsAdministrativeAuditWithActorTargetAndReason() {
        Usuario administrador = new Usuario();
        administrador.setId(7L);
        administrador.setTipoPerfil("ADMIN");
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(administrador));

        service.registrarAlteracao(
                7L, "USUARIO_SUSPENSO", "USUARIO", 20L, "SUCESSO", "Motivo");

        ArgumentCaptor<AuditoriaAdministrativa> captor =
                ArgumentCaptor.forClass(AuditoriaAdministrativa.class);
        verify(auditoriaRepository).save(captor.capture());
        AuditoriaAdministrativa evento = captor.getValue();
        assertSame(administrador, evento.getAtor());
        assertEquals("USUARIO_SUSPENSO", evento.getAcao());
        assertEquals("USUARIO", evento.getTipoAlvo());
        assertEquals(20L, evento.getAlvoId());
        assertEquals("SUCESSO", evento.getResultado());
        assertEquals("Motivo", evento.getMotivo());
        assertNotNull(evento.getOcorridoEm());
    }

    @Test
    void refusesToRecordAnOperationForANonAdministrator() {
        Usuario usuario = new Usuario();
        usuario.setTipoPerfil("MOTORISTA");
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));

        assertThrows(AccessDeniedException.class,
                () -> service.registrarConsulta(7L, "USUARIOS_PESQUISADOS", "USUARIO"));

        verifyNoInteractions(auditoriaRepository);
    }

    @Test
    void expiresAuditEventsAfterOneYear() {
        service.expurgarAuditoriasAntigas();

        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(auditoriaRepository).deleteByOcorridoEmBefore(captor.capture());
        long diasAteAgora = Duration.between(captor.getValue(), Instant.now()).toDays();
        assertTrue(diasAteAgora >= 365 && diasAteAgora < 366);
    }
}
