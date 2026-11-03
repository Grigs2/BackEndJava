package TioDaPerua.BackEndJava.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitarVinculoEscolaDTO(
        @NotBlank @Size(max = 18) String cnpj) {
}
