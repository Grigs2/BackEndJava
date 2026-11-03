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

import java.time.LocalDate;
import java.util.List;

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
    void showsSevenCalendarDatesWithoutMaterializingFutureExecutions() {
        LocalDate hoje = LocalDate.now();
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

        var agenda = service.agendaMotorista(4L);

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
    void preparationRejectsDatesOutsideTheSevenDayWindow() {
        assertThrows(BusinessException.class,
                () -> service.prepararViagemDia(8L, LocalDate.now().plusDays(7), 12L));
    }
}
