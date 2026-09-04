package TioDaPerua.BackEndJava.DTOs;

import jakarta.validation.constraints.NotBlank;

public record AlterarStatusViagemDiaDTO(
        @NotBlank(message = "ID da viagem está nullo") Long idViagemDia,
        @NotBlank(message = "Novo status está nullo") String novoStatus
) {
}
