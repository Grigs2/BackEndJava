package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Viagem;
import TioDaPerua.BackEndJava.entities.ViagemDia;
import TioDaPerua.BackEndJava.enums.Periodo;
import TioDaPerua.BackEndJava.enums.StatusViagemDia;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.repositories.ViagemDiaRepository;
import TioDaPerua.BackEndJava.repositories.ViagemRepository;
import TioDaPerua.BackEndJava.repositories.ResponsavelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViagemAgendaTest {

    @Mock private ViagemRepository viagemRepository;
    @Mock private ViagemDiaRepository viagemDiaRepository;
    @Mock private ResponsavelRepository responsavelRepository;
    @Mock private NotificacaoService notificacaoService;
    @InjectMocks private ViagemService service;

    @Test
    void showsSevenBrasiliaDatesWithoutMaterializingFutureExecutions() {
        // 18/11 23:40 em Brasília = 19/11 02:40 UTC: o dia de Brasília deve ser 18/11.
        ReflectionTestUtils.setField(service, "relogio",
                Clock.fixed(Instant.parse("2026-11-19T02:40:00Z"), ZoneId.of("America/Sao_Paulo")));
        LocalDate hoje = LocalDate.of(2026, 11, 18);
        Viagem viagem = new Viagem();
        viagem.setId(8L);
        viagem.setPeriodo(Periodo.MANHA_IDA);
        ViagemDia existente = new ViagemDia();
        existente.setId(22L);
        existente.setViagem(viagem);
        existente.setData(hoje.plusDays(2));
        existente.setStatus(StatusViagemDia.PLANEJADA);
        when(viagemRepository.findAtivasByMotoristaId(4L)).thenReturn(List.of(viagem));
        when(viagemDiaRepository.findByMotoristaIdAndDataBetween(4L, hoje, hoje.plusDays(6)))
                .thenReturn(List.of(existente));

        var agenda = service.agendaMotorista(4L, null, null);

        assertEquals(7, agenda.size());
        assertEquals(hoje, agenda.get(0).data());
        assertEquals(hoje.plusDays(6), agenda.get(6).data());
        assertEquals(1, agenda.stream().filter(item -> item.materializada()).count());
        assertEquals(22L, agenda.get(2).idViagemDia());
        assertEquals(StatusViagemDia.PLANEJADA, agenda.get(2).status());
        assertNull(agenda.get(0).idViagemDia());
        verify(viagemDiaRepository).findByMotoristaIdAndDataBetween(4L, hoje, hoje.plusDays(6));
        verify(viagemDiaRepository, never()).save(any());
    }

    @Test
    void explicitRangeIsInclusiveAtBothEnds() {
        LocalDate de = LocalDate.of(2026, 11, 18);
        LocalDate ate = LocalDate.of(2026, 11, 19);
        when(viagemRepository.findAtivasByMotoristaId(4L)).thenReturn(List.of());
        when(viagemDiaRepository.findByMotoristaIdAndDataBetween(4L, de, ate)).thenReturn(List.of());

        var agenda = service.agendaMotorista(4L, de, ate);

        assertEquals(List.of(), agenda);
        verify(viagemDiaRepository).findByMotoristaIdAndDataBetween(4L, de, ate);
    }

    @Test
    void rejectsRangeEndingBeforeStart() {
        assertThrows(BusinessException.class,
                () -> service.agendaMotorista(4L, LocalDate.of(2026, 11, 19), LocalDate.of(2026, 11, 18)));
        verify(viagemDiaRepository, never()).findByMotoristaIdAndDataBetween(any(), any(), any());
    }

    @Test
    void preparationRejectsDatesOutsideTheSevenDayWindow() {
        assertThrows(BusinessException.class,
                () -> service.prepararViagemDia(8L, LocalDate.now().plusDays(7), 12L));
    }
}
