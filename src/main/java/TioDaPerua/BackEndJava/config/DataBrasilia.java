package TioDaPerua.BackEndJava.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Datas de calendário e horários do sistema seguem Brasília, independentemente do fuso do servidor. */
public final class DataBrasilia {

    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private DataBrasilia() {
    }

    public static LocalDate hoje() {
        return LocalDate.now(FUSO);
    }

    public static LocalDateTime agora() {
        return LocalDateTime.now(FUSO);
    }
}
