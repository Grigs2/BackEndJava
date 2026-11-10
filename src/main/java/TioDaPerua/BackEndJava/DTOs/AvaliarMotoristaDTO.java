package TioDaPerua.BackEndJava.DTOs;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AvaliarMotoristaDTO(
        @NotNull @Min(1) @Max(5) Integer nota,
        @Size(max = 500) String comentario) {
}
