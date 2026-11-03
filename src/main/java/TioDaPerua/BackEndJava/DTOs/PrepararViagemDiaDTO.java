package TioDaPerua.BackEndJava.DTOs;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PrepararViagemDiaDTO(@NotNull LocalDate data) {
}
