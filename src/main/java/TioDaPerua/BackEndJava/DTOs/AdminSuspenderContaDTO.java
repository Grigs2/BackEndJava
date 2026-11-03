package TioDaPerua.BackEndJava.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminSuspenderContaDTO(
        @NotBlank @Size(max = 1000) String motivo) {
}
