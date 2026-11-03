package TioDaPerua.BackEndJava.DTOs;

import jakarta.validation.constraints.Size;

public record AdminReativarContaDTO(
        @Size(max = 1000) String motivo) {
}
