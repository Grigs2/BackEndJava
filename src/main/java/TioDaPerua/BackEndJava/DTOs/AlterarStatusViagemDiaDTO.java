package TioDaPerua.BackEndJava.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AlterarStatusViagemDiaDTO(
        @NotNull(message = "ID da viagem está nullo") Long idViagemDia,
        @NotBlank(message = "Novo status está nullo") String novoStatus
) {
}
