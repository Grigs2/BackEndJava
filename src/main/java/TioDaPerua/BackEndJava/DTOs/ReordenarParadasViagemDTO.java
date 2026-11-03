package TioDaPerua.BackEndJava.DTOs;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record ReordenarParadasViagemDTO(
        @NotEmpty List<@NotNull @Positive Long> idsParadas) {
}
