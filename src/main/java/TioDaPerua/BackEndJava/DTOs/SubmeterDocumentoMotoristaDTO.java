package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.enums.TipoDocumento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SubmeterDocumentoMotoristaDTO(
        @NotNull TipoDocumento tipo,
        @NotBlank String numero,
        String categoria,
        LocalDate dataRealizacao,
        @NotNull LocalDate dataValidade
) {
}
