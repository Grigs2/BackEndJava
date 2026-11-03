package TioDaPerua.BackEndJava.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AlterarStatusPresencaDTO(
    @NotNull @Positive Long idViagemDia,
    @NotNull @Positive Long idDependente,
    @NotBlank String novoStatus
) {}
