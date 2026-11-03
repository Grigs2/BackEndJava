package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.repositories.ResponsavelRepository;
import TioDaPerua.BackEndJava.entities.HistoricoViagemPresenca;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.entities.Viagem;
import TioDaPerua.BackEndJava.entities.ViagemDia;
import TioDaPerua.BackEndJava.entities.ViagemParada;
import TioDaPerua.BackEndJava.entities.ViagemPresenca;
import TioDaPerua.BackEndJava.DTOs.ParadaDTO;
import TioDaPerua.BackEndJava.enums.StatusViagemDia;
import TioDaPerua.BackEndJava.enums.StatusViagemPresenca;
import TioDaPerua.BackEndJava.enums.TipoParada;
import TioDaPerua.BackEndJava.exceptions.ConflictException;
import TioDaPerua.BackEndJava.repositories.HistoricoViagemPresencaRepository;
import TioDaPerua.BackEndJava.repositories.DependenteResponsavelUsuarioProjection;
import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import TioDaPerua.BackEndJava.repositories.ViagemDiaRepository;
import TioDaPerua.BackEndJava.repositories.ViagemParadaRepository;
import TioDaPerua.BackEndJava.repositories.ViagemPresencaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViagemPresencaStatusTest {

    @Mock private ViagemDiaRepository viagemDiaRepository;
    @Mock private ViagemPresencaRepository viagemPresencaRepository;
    @Mock private HistoricoViagemPresencaRepository historicoViagemPresencaRepository;
    @Mock private ViagemParadaRepository viagemParadaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ResponsavelRepository responsavelRepository;
    @Mock private NotificacaoService notificacaoService;
    @InjectMocks private ViagemService service;

    private ViagemDia execucao;
    private ViagemPresenca presenca;
    private Usuario motoristaUsuario;

    @BeforeEach
    void setUp() {
        motoristaUsuario = new Usuario();
        motoristaUsuario.setId(100L);
        Motorista motorista = new Motorista();
        motorista.setUsuario(motoristaUsuario);
        Viagem viagem = new Viagem();
        viagem.setMotorista(motorista);
        execucao = new ViagemDia();
        execucao.setId(90L);
        execucao.setViagem(viagem);
        execucao.setStatus(StatusViagemDia.PLANEJADA);

        presenca = new ViagemPresenca();
        presenca.setId(110L);
        presenca.setViagemDia(execucao);
        presenca.setDependente(new TioDaPerua.BackEndJava.entities.Dependente(5L, null, null, null, null, null));
        presenca.setStatus(StatusViagemPresenca.ESPERANDO);

        ViagemParada parada = new ViagemParada();
        parada.setId(120L);
        parada.setTipoParada(TipoParada.MOTORISTA);
        when(viagemDiaRepository.findByIdForUpdate(90L)).thenReturn(Optional.of(execucao));
    }

    @Test
    void recordsBoardingAndDisembarkingWithActorAndPreviousState() {
        stubSuccessfulUpdate();
        when(usuarioRepository.findById(100L)).thenReturn(Optional.of(motoristaUsuario));
        service.alterarStatusPresenca(90L, 5L, "EMBARCADO", 100L);
        service.alterarStatusPresenca(90L, 5L, "DESEMBARCADO", 100L);

        assertEquals(StatusViagemPresenca.DESEMBARCADO, presenca.getStatus());
        assertNotNull(presenca.getHorarioEmbarque());
        assertNotNull(presenca.getHorarioDesembarque());

        ArgumentCaptor<HistoricoViagemPresenca> captor =
                ArgumentCaptor.forClass(HistoricoViagemPresenca.class);
        verify(historicoViagemPresencaRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertEquals(StatusViagemPresenca.ESPERANDO, captor.getAllValues().get(0).getStatusAnterior());
        assertEquals(StatusViagemPresenca.EMBARCADO, captor.getAllValues().get(0).getStatusNovo());
        assertEquals(StatusViagemPresenca.EMBARCADO, captor.getAllValues().get(1).getStatusAnterior());
        assertEquals(StatusViagemPresenca.DESEMBARCADO, captor.getAllValues().get(1).getStatusNovo());
        assertEquals(100L, captor.getAllValues().get(0).getAtor().getId());
        assertNotNull(captor.getAllValues().get(0).getOcorridoEm());
    }

    @Test
    void recordsAbsenceAsAValidTerminalPresenceTransition() {
        stubSuccessfulUpdate();
        when(usuarioRepository.findById(100L)).thenReturn(Optional.of(motoristaUsuario));
        service.alterarStatusPresenca(90L, 5L, "FALTOU", 100L);

        assertEquals(StatusViagemPresenca.FALTOU, presenca.getStatus());
        verify(historicoViagemPresencaRepository).save(any(HistoricoViagemPresenca.class));
    }

    @Test
    void treatsRetryOfCurrentStatusAsIdempotentWithoutAnotherEvent() {
        stubPresenceRead();
        stubRouteResponse();
        service.alterarStatusPresenca(90L, 5L, "ESPERANDO", 100L);

        verify(viagemPresencaRepository, never()).save(any(ViagemPresenca.class));
        verify(historicoViagemPresencaRepository, never()).save(any(HistoricoViagemPresenca.class));
        verify(notificacaoService, never()).criarNotificacoesAutomaticas(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsTransitionsThatSkipOrReverseTheApprovedSequence() {
        stubPresenceRead();
        assertThrows(ConflictException.class,
                () -> service.alterarStatusPresenca(90L, 5L, "DESEMBARCADO", 100L));

        presenca.setStatus(StatusViagemPresenca.FALTOU);
        assertThrows(ConflictException.class,
                () -> service.alterarStatusPresenca(90L, 5L, "EMBARCADO", 100L));

        verify(historicoViagemPresencaRepository, never()).save(any(HistoricoViagemPresenca.class));
    }

    @Test
    void rejectsUpdatesFromAnotherDriver() {
        assertThrows(AccessDeniedException.class,
                () -> service.alterarStatusPresenca(90L, 5L, "EMBARCADO", 999L));

        verify(viagemPresencaRepository, never()).save(any(ViagemPresenca.class));
        verify(historicoViagemPresencaRepository, never()).save(any(HistoricoViagemPresenca.class));
    }

    @Test
    void rejectsPresenceChangesForClosedExecutions() {
        execucao.setStatus(StatusViagemDia.FINALIZADA);

        assertThrows(ConflictException.class,
                () -> service.alterarStatusPresenca(90L, 5L, "EMBARCADO", 100L));

        verify(viagemPresencaRepository, never()).save(any(ViagemPresenca.class));
        verify(historicoViagemPresencaRepository, never()).save(any(HistoricoViagemPresenca.class));
    }

    @Test
    void permitsPresenceUpdatesWhileExecutionIsInProgress() {
        stubSuccessfulUpdate();
        when(usuarioRepository.findById(100L)).thenReturn(Optional.of(motoristaUsuario));
        execucao.setStatus(StatusViagemDia.EM_ANDAMENTO);

        service.alterarStatusPresenca(90L, 5L, "EMBARCADO", 100L);

        assertEquals(StatusViagemPresenca.EMBARCADO, presenca.getStatus());
    }

    @Test
    void notifiesLinkedResponsibleAfterEffectivePresenceTransition() {
        stubSuccessfulUpdate();
        when(usuarioRepository.findById(100L)).thenReturn(Optional.of(motoristaUsuario));
        DependenteResponsavelUsuarioProjection vinculo = mock(DependenteResponsavelUsuarioProjection.class);
        when(vinculo.getUsuarioId()).thenReturn(200L);
        when(responsavelRepository.findUsuariosAtivosPorDependentes(List.of(5L)))
                .thenReturn(List.of(vinculo));

        service.alterarStatusPresenca(90L, 5L, "EMBARCADO", 100L);

        verify(notificacaoService).criarNotificacoesAutomaticas(
                100L, java.util.Set.of(200L),
                "Atualização de presença", "O embarque do dependente foi registrado.");
    }

    private void stubSuccessfulUpdate() {
        stubPresenceRead();
        stubRouteResponse();
    }

    private void stubPresenceRead() {
        when(viagemPresencaRepository.findByViagemDiaIdAndDependenteId(90L, 5L))
                .thenReturn(Optional.of(presenca));
    }

    private void stubRouteResponse() {
        ViagemParada parada = new ViagemParada();
        parada.setId(120L);
        parada.setTipoParada(TipoParada.MOTORISTA);
        when(viagemDiaRepository.findById(90L)).thenReturn(Optional.of(execucao));
        when(viagemParadaRepository.listarPorViagemDia(90L)).thenReturn(List.of(parada));
    }
}
