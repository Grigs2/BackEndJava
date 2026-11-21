package TioDaPerua.BackEndJava.DTOs;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalTime;

/** Novo horário de partida padrão da viagem. */
public record AlterarHorarioPartidaDTO(@NotNull @Pattern(regexp = HorarioPartida.REGEX, message = HorarioPartida.MENSAGEM) String horarioPartida) {

    public LocalTime horario() {
        return HorarioPartida.converter(horarioPartida);
    }
}
