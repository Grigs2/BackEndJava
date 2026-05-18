package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.ViagemDia;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ViagemDiaDTO(
    Long id,
    LocalDate data,
    String status,
    LocalDateTime dataUltimaAlteracaoStatus
) {
    public static ViagemDiaDTO toDTO(ViagemDia entity) {
        if (entity == null) return null;
        return new ViagemDiaDTO(
            entity.getId(),
            entity.getData(),
            entity.getStatus() != null ? entity.getStatus().name() : null,
            entity.getDataUltimaAlteracaoStatus()
        );
    }
}
