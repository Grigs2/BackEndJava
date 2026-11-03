package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.repositories.ResponsavelRepository;
import TioDaPerua.BackEndJava.DTOs.EventoPresencaOfflineDTO;
import TioDaPerua.BackEndJava.DTOs.ResultadoEventoPresencaDTO;
import TioDaPerua.BackEndJava.entities.HistoricoViagemPresenca;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.entities.Viagem;
import TioDaPerua.BackEndJava.entities.ViagemDia;
import TioDaPerua.BackEndJava.entities.ViagemPresenca;
import TioDaPerua.BackEndJava.enums.StatusEventoSincronizacao;
import TioDaPerua.BackEndJava.enums.StatusViagemDia;
import TioDaPerua.BackEndJava.enums.StatusViagemPresenca;
import TioDaPerua.BackEndJava.repositories.HistoricoViagemDiaRepository;
import TioDaPerua.BackEndJava.repositories.HistoricoViagemPresencaRepository;
import TioDaPerua.BackEndJava.repositories.LocalizacaoViagemAtualRepository;
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

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViagemPresencaOfflineSyncTest {

    @Mock private ViagemDiaRepository viagemDiaRepository;
    @Mock private ViagemPresencaRepository viagemPresencaRepository;
    @Mock private HistoricoViagemPresencaRepository historicoViagemPresencaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private HistoricoViagemDiaRepository historicoViagemDiaRepository;
    @Mock private LocalizacaoViagemAtualRepository localizacaoViagemAtualRepository;
    @Mock private ViagemParadaRepository viagemParadaRepository;
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
        execucao.setStatus(StatusViagemDia.EM_ANDAMENTO);
        presenca = new ViagemPresenca();
        presenca.setId(110L);
        presenca.setViagemDia(execucao);
        presenca.setDependente(new TioDaPerua.BackEndJava.entities.Dependente(
                5L, "Dependente", null, null, null, null));
        presenca.setStatus(StatusViagemPresenca.ESPERANDO);
    }

    @Test
    void appliesOfflineTransitionAndStoresClientAndServerTimestamps() {
        EventoPresencaOfflineDTO evento = evento(
                StatusViagemPresenca.ESPERANDO, StatusViagemPresenca.EMBARCADO);
        stubExecutionAndPresence();
        when(historicoViagemPresencaRepository.findByAtor_IdAndEventoClienteId(100L, evento.idEvento()))
                .thenReturn(Optional.empty());
        when(viagemPresencaRepository.save(any(ViagemPresenca.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(historicoViagemPresencaRepository.save(any(HistoricoViagemPresenca.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(usuarioRepository.findById(100L)).thenReturn(Optional.of(motoristaUsuario));

        ResultadoEventoPresencaDTO resultado = service.sincronizarPresencaOffline(evento, 100L);

        assertEquals(StatusEventoSincronizacao.APLICADO, resultado.resultado());
        assertEquals(StatusViagemPresenca.EMBARCADO, presenca.getStatus());
        ArgumentCaptor<HistoricoViagemPresenca> captor =
                ArgumentCaptor.forClass(HistoricoViagemPresenca.class);
        verify(historicoViagemPresencaRepository).save(captor.capture());
        assertEquals(evento.idEvento(), captor.getValue().getEventoClienteId());
        assertEquals(evento.ocorridoEm(), captor.getValue().getInstanteDispositivo());
        assertEquals(100L, captor.getValue().getAtor().getId());
        org.junit.jupiter.api.Assertions.assertNotNull(captor.getValue().getRecebidoEm());
    }

    @Test
    void returnsConflictWithoutOverwritingWhenExpectedStateIsStale() {
        presenca.setStatus(StatusViagemPresenca.EMBARCADO);
        EventoPresencaOfflineDTO evento = evento(
                StatusViagemPresenca.ESPERANDO, StatusViagemPresenca.FALTOU);
        stubExecutionAndPresence();
        when(historicoViagemPresencaRepository.findByAtor_IdAndEventoClienteId(100L, evento.idEvento()))
                .thenReturn(Optional.empty());

        ResultadoEventoPresencaDTO resultado = service.sincronizarPresencaOffline(evento, 100L);

        assertEquals(StatusEventoSincronizacao.CONFLITO, resultado.resultado());
        assertEquals(StatusViagemPresenca.EMBARCADO, resultado.statusAtual());
        verify(viagemPresencaRepository, never()).save(any(ViagemPresenca.class));
    }

    @Test
    void recognizesSameEventUuidAsDuplicateWithoutAnotherHistoryRow() {
        EventoPresencaOfflineDTO evento = evento(
                StatusViagemPresenca.ESPERANDO, StatusViagemPresenca.EMBARCADO);
        stubExecutionAndPresence();
        presenca.setStatus(StatusViagemPresenca.EMBARCADO);
        HistoricoViagemPresenca existente = new HistoricoViagemPresenca();
        existente.setPresenca(presenca);
        existente.setStatusAnterior(StatusViagemPresenca.ESPERANDO);
        existente.setStatusNovo(StatusViagemPresenca.EMBARCADO);
        existente.setInstanteDispositivo(evento.ocorridoEm());
        when(historicoViagemPresencaRepository.findByAtor_IdAndEventoClienteId(100L, evento.idEvento()))
                .thenReturn(Optional.of(existente));
        when(viagemPresencaRepository.findByViagemDiaIdAndDependenteId(90L, 5L))
                .thenReturn(Optional.of(presenca));

        ResultadoEventoPresencaDTO resultado = service.sincronizarPresencaOffline(evento, 100L);

        assertEquals(StatusEventoSincronizacao.DUPLICADO, resultado.resultado());
        verify(historicoViagemPresencaRepository, never()).save(any(HistoricoViagemPresenca.class));
        verify(viagemPresencaRepository, never()).save(any(ViagemPresenca.class));
        verifyNoInteractions(notificacaoService);
    }

    @Test
    void rejectsOfflineEventsAfterExecutionIsTerminal() {
        execucao.setStatus(StatusViagemDia.FINALIZADA);
        EventoPresencaOfflineDTO evento = evento(
                StatusViagemPresenca.ESPERANDO, StatusViagemPresenca.EMBARCADO);
        stubExecutionAndPresence();
        when(historicoViagemPresencaRepository.findByAtor_IdAndEventoClienteId(100L, evento.idEvento()))
                .thenReturn(Optional.empty());

        ResultadoEventoPresencaDTO resultado = service.sincronizarPresencaOffline(evento, 100L);

        assertEquals(StatusEventoSincronizacao.CONFLITO, resultado.resultado());
        verify(viagemPresencaRepository, never()).save(any(ViagemPresenca.class));
    }

    @Test
    void rejectsEventsFromAnotherDriver() {
        EventoPresencaOfflineDTO evento = evento(
                StatusViagemPresenca.ESPERANDO, StatusViagemPresenca.EMBARCADO);
        when(viagemDiaRepository.findByIdForUpdate(90L)).thenReturn(Optional.of(execucao));

        assertThrows(AccessDeniedException.class,
                () -> service.sincronizarPresencaOffline(evento, 999L));

        verify(viagemPresencaRepository, never()).save(any(ViagemPresenca.class));
    }

    @Test
    void rejectsNonV4EventIdentifiers() {
        EventoPresencaOfflineDTO evento = new EventoPresencaOfflineDTO(
                UUID.fromString("00000000-0000-1000-8000-000000000001"),
                90L, 5L, StatusViagemPresenca.ESPERANDO,
                StatusViagemPresenca.EMBARCADO, Instant.now());

        assertThrows(TioDaPerua.BackEndJava.exceptions.BusinessException.class,
                () -> service.sincronizarPresencaOffline(evento, 100L));

        verify(viagemDiaRepository, never()).findByIdForUpdate(90L);
    }

    private void stubExecutionAndPresence() {
        when(viagemDiaRepository.findByIdForUpdate(90L)).thenReturn(Optional.of(execucao));
        when(viagemPresencaRepository.findByViagemDiaIdAndDependenteId(90L, 5L))
                .thenReturn(Optional.of(presenca));
    }

    private EventoPresencaOfflineDTO evento(
            StatusViagemPresenca esperado, StatusViagemPresenca novo) {
        return new EventoPresencaOfflineDTO(
                UUID.randomUUID(), 90L, 5L, esperado, novo, Instant.now().minusSeconds(10));
    }
}
