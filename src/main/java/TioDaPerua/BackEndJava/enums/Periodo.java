package TioDaPerua.BackEndJava.enums;

import java.time.LocalTime;

public enum Periodo {
    MANHA_IDA,
    MANHA_VOLTA,
    TARDE_IDA,
    TARDE_VOLTA,
    NOITE_IDA,
    NOITE_VOLTA;

    /** Horário de partida padrão do turno: 07:00 (manhã), 12:00 (tarde) ou 17:00 (noite). */
    public LocalTime horarioPartidaPadrao() {
        return switch (this) {
            case MANHA_IDA, MANHA_VOLTA -> LocalTime.of(7, 0);
            case TARDE_IDA, TARDE_VOLTA -> LocalTime.of(12, 0);
            case NOITE_IDA, NOITE_VOLTA -> LocalTime.of(17, 0);
        };
    }
}
