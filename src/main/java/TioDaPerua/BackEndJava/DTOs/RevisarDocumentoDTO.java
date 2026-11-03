package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.enums.StatusDocumento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RevisarDocumentoDTO(@NotNull StatusDocumento status,
                                 @Size(max = 1000) String motivoRecusa) {
}
