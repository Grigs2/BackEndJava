package TioDaPerua.BackEndJava.DTOs;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Aviso de atraso da viagem do dia: minutos (5 a 180) e motivo opcional. */
public record AvisoAtrasoDTO(@NotNull @Min(5) @Max(180) Integer minutos,
                             @Size(max = 200) String motivo) {
}
