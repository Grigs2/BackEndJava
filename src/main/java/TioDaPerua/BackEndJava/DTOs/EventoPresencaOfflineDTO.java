package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.enums.StatusViagemPresenca;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.UUID;

public record EventoPresencaOfflineDTO(
        @NotNull UUID idEvento,
        @NotNull @Positive Long idViagemDia,
        @NotNull @Positive Long idDependente,
        @NotNull StatusViagemPresenca statusEsperado,
        @NotNull StatusViagemPresenca statusNovo,
        @NotNull Instant ocorridoEm) {
}
