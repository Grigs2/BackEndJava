package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.enums.CategoriaVinculoResponsavel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CriarConviteResponsavelDTO(
        @NotBlank @Email @Size(max = 254) String email,
        @NotNull CategoriaVinculoResponsavel categoria) {
}
