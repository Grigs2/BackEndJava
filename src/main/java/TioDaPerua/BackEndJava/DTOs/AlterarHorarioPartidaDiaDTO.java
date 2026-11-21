package TioDaPerua.BackEndJava.DTOs;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.time.LocalTime;

/** Horário de partida de uma execução. Horário nulo remove o ajuste e volta ao padrão da viagem. */
public record AlterarHorarioPartidaDiaDTO(
        @NotNull LocalDate data,
        @Pattern(regexp = HorarioPartida.REGEX, message = HorarioPartida.MENSAGEM) String horarioPartida) {

    public LocalTime horario() {
        return HorarioPartida.converter(horarioPartida);
    }
}
