package TioDaPerua.BackEndJava.DTOs;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SincronizarPresencasOfflineDTO(
        @NotNull @NotEmpty @Size(max = 100) List<EventoPresencaOfflineDTO> eventos) {
}
