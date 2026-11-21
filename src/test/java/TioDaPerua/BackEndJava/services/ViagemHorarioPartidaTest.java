package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.HorarioPartida;
import TioDaPerua.BackEndJava.entities.Viagem;
import TioDaPerua.BackEndJava.entities.ViagemDia;
import TioDaPerua.BackEndJava.enums.Periodo;
import TioDaPerua.BackEndJava.enums.StatusViagemDia;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.repositories.ResponsavelRepository;
import TioDaPerua.BackEndJava.repositories.ViagemDiaRepository;
import TioDaPerua.BackEndJava.repositories.ViagemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViagemHorarioPartidaTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 18);

    @Mock private ViagemRepository viagemRepository;
    @Mock private ViagemDiaRepository viagemDiaRepository;
    @Mock private ResponsavelRepository responsavelRepository;
    @Mock private NotificacaoService notificacaoService;
    @InjectMocks private ViagemService service;

    @Test
    void resolvesOwnTimeBeforeViagemDefault() {
        Viagem viagem = new Viagem();
        viagem.setPeriodo(Periodo.TARDE_IDA);
        viagem.setHorarioPartidaPadrao(LocalTime.of(12, 0));
        ViagemDia dia = new ViagemDia();
        dia.setViagem(viagem);

        assertEquals(LocalTime.of(12, 0), HorarioPartida.resolver(viagem, dia));
        dia.setHorarioPartida(LocalTime.of(13, 30));
        assertEquals(LocalTime.of(13, 30), HorarioPartida.resolver(viagem, dia));
        assertEquals("13:30", HorarioPartida.formatar(HorarioPartida.resolver(viagem, dia)));
    }

    @Test
    void periodoProvidesDefaultDeparture() {
        assertEquals(LocalTime.of(7, 0), Periodo.MANHA_IDA.horarioPartidaPadrao());
        assertEquals(LocalTime.of(12, 0), Periodo.TARDE_VOLTA.horarioPartidaPadrao());
        assertEquals(LocalTime.of(17, 0), Periodo.NOITE_IDA.horarioPartidaPadrao());
    }

    @Test
    void changingViagemDefaultFreezesUnstartedExecutionsWithoutOwnTime() {
        ReflectionTestUtils.setField(service, "relogio",
                Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneId.of("America/Sao_Paulo")));
        Viagem viagem = new Viagem();
        viagem.setId(8L);
        viagem.setAtivo(true);
        viagem.setPeriodo(Periodo.MANHA_IDA);
        viagem.setHorarioPartidaPadrao(LocalTime.of(7, 0));
        ViagemDia futura = new ViagemDia();
        futura.setData(HOJE.plusDays(1));
        when(viagemRepository.findByIdForUpdate(8L)).thenReturn(Optional.of(viagem));
        when(viagemDiaRepository.findSemHorarioProprioParaCongelar(8L, StatusViagemDia.PLANEJADA, HOJE))
                .thenReturn(List.of(futura));
        when(viagemRepository.save(viagem)).thenReturn(viagem);

        Viagem salva = service.alterarHorarioPartidaPadrao(8L, LocalTime.of(8, 15));

        assertEquals(LocalTime.of(8, 15), salva.getHorarioPartidaPadrao());
        assertEquals(LocalTime.of(7, 0), futura.getHorarioPartida());
        verify(viagemDiaRepository).saveAll(anyList());
    }

    @Test
    void changingDayTimeIsRejectedOutsideSevenDayWindow() {
        ReflectionTestUtils.setField(service, "relogio",
                Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneId.of("America/Sao_Paulo")));

        assertThrows(BusinessException.class, () -> service.alterarHorarioPartidaDia(
                8L, HOJE.minusDays(1), LocalTime.of(9, 0), 4L));
        assertThrows(BusinessException.class, () -> service.alterarHorarioPartidaDia(
                8L, HOJE.plusDays(7), LocalTime.of(9, 0), 4L));
        verify(viagemRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void invalidTimeStringIsRejectedByConverter() {
        assertThrows(DateTimeParseException.class, () -> HorarioPartida.converter("25:00"));
        assertThrows(DateTimeParseException.class, () -> HorarioPartida.converter("7h"));
        assertNull(HorarioPartida.resolver(new Viagem(), null));
    }
}
