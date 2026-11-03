package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.EventoPresencaOfflineDTO;
import TioDaPerua.BackEndJava.DTOs.ResultadoEventoPresencaDTO;
import TioDaPerua.BackEndJava.DTOs.SincronizarPresencasOfflineDTO;
import TioDaPerua.BackEndJava.enums.StatusEventoSincronizacao;
import TioDaPerua.BackEndJava.enums.StatusViagemPresenca;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.exceptions.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SincronizacaoOfflineServiceTest {

    @Mock private ViagemService viagemService;
    @InjectMocks private SincronizacaoOfflineService service;

    @Test
    void returnsIndependentResultForEachEventInBatch() {
        EventoPresencaOfflineDTO primeiro = evento(StatusViagemPresenca.EMBARCADO);
        EventoPresencaOfflineDTO segundo = evento(StatusViagemPresenca.FALTOU);
        when(viagemService.sincronizarPresencaOffline(primeiro, 100L))
                .thenReturn(new ResultadoEventoPresencaDTO(
                        primeiro.idEvento(), 90L, 5L, StatusEventoSincronizacao.APLICADO,
                        StatusViagemPresenca.EMBARCADO, null));
        when(viagemService.sincronizarPresencaOffline(segundo, 100L))
                .thenThrow(new ConflictException("Estado atual diverge"));

        var resposta = service.sincronizarPresencas(
                new SincronizarPresencasOfflineDTO(List.of(primeiro, segundo)), 100L);

        assertEquals(2, resposta.resultados().size());
        assertEquals(StatusEventoSincronizacao.APLICADO, resposta.resultados().get(0).resultado());
        assertEquals(StatusEventoSincronizacao.CONFLITO, resposta.resultados().get(1).resultado());
    }

    @Test
    void rejectsInvalidItemAndContinuesProcessingTheBatch() {
        EventoPresencaOfflineDTO invalido = evento(StatusViagemPresenca.EMBARCADO);
        EventoPresencaOfflineDTO valido = evento(StatusViagemPresenca.FALTOU);
        when(viagemService.sincronizarPresencaOffline(invalido, 100L))
                .thenThrow(new BusinessException("Transição inválida"));
        when(viagemService.sincronizarPresencaOffline(valido, 100L))
                .thenReturn(new ResultadoEventoPresencaDTO(
                        valido.idEvento(), 90L, 5L, StatusEventoSincronizacao.APLICADO,
                        StatusViagemPresenca.FALTOU, null));

        var resposta = service.sincronizarPresencas(
                new SincronizarPresencasOfflineDTO(List.of(invalido, valido)), 100L);

        assertEquals(StatusEventoSincronizacao.REJEITADO, resposta.resultados().get(0).resultado());
        assertEquals(StatusEventoSincronizacao.APLICADO, resposta.resultados().get(1).resultado());
    }

    private EventoPresencaOfflineDTO evento(StatusViagemPresenca novo) {
        return new EventoPresencaOfflineDTO(
                UUID.randomUUID(), 90L, 5L, StatusViagemPresenca.ESPERANDO, novo, Instant.now());
    }
}
