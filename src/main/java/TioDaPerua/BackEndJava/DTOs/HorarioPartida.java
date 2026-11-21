package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.Viagem;
import TioDaPerua.BackEndJava.entities.ViagemDia;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Formato HH:mm do horário de partida, sem fuso horário. */
public final class HorarioPartida {

    public static final String REGEX = "^([01]\\d|2[0-3]):[0-5]\\d$";
    public static final String MENSAGEM = "Horário de partida deve estar no formato HH:mm";

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("HH:mm");

    private HorarioPartida() {
    }

    public static LocalTime converter(String horario) {
        return horario == null ? null : LocalTime.parse(horario, FORMATO);
    }

    public static String formatar(LocalTime horario) {
        return horario == null ? null : horario.format(FORMATO);
    }

    /** Horário próprio da execução; se ausente, o padrão da viagem. */
    public static LocalTime resolver(Viagem viagem, ViagemDia dia) {
        if (dia != null && dia.getHorarioPartida() != null) return dia.getHorarioPartida();
        return viagem != null ? viagem.getHorarioPartidaPadrao() : null;
    }
}
