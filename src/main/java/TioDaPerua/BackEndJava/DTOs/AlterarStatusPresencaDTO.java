package TioDaPerua.BackEndJava.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AlterarStatusPresencaDTO(
    @NotNull(message = "ID da viagem está nullo") Long idViagemDia,
    @NotNull(message = "ID do dependente está nullo") Long idDependente,
    @NotBlank(message = "Novo status está nullo") String novoStatus
) {}
