package TioDaPerua.BackEndJava.DTOs;

import jakarta.validation.constraints.Size;

public record MotivoVinculoEscolaDTO(
        @Size(max = 500) String motivo) {
}
