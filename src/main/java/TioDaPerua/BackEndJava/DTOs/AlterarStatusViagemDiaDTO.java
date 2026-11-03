package TioDaPerua.BackEndJava.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AlterarStatusViagemDiaDTO(
        @NotNull @Positive Long idViagemDia,
        @NotBlank String novoStatus,
        @Size(max = 1000) String motivo
) {

    public AlterarStatusViagemDiaDTO(Long idViagemDia, String novoStatus) {
        this(idViagemDia, novoStatus, null);
    }
}
