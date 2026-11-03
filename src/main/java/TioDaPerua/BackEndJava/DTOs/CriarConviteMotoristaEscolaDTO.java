package TioDaPerua.BackEndJava.DTOs;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CriarConviteMotoristaEscolaDTO(
        @NotNull @Positive Long motoristaId) {
}
