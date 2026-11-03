package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.enums.Periodo;
import TioDaPerua.BackEndJava.enums.StatusViagemDia;

import java.time.LocalDate;

public record AgendaViagemDTO(
        Long idViagem,
        Periodo periodo,
        LocalDate data,
        Long idViagemDia,
        StatusViagemDia status,
        boolean materializada) {
}
