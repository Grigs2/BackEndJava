package TioDaPerua.BackEndJava.DTOs;

import jakarta.validation.constraints.NotBlank;

public record AlterarStatusPresencaDTO(
    @NotBlank(message = "ID da viagem está nullo") Long idViagemDia,
    @NotBlank(message = "ID do dependente está nullo") Long idDependente,
    @NotBlank(message = "Novo status está nullo") String novoStatus
) {}
