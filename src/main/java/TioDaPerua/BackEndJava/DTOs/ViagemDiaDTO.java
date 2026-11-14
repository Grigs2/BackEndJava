package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.ViagemDia;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record   ViagemDiaDTO(
    Long id,
    LocalDate data,
    String status,
    LocalDateTime dataUltimaAlteracaoStatus,
    Long viagemId,
    String periodo
) {
    public static ViagemDiaDTO toDTO(ViagemDia entity) {
        if (entity == null) return null;
        var viagem = entity.getViagem();
        return new ViagemDiaDTO(
            entity.getId(),
            entity.getData(),
            entity.getStatus() != null ? entity.getStatus().name() : null,
            entity.getDataUltimaAlteracaoStatus(),
            viagem != null ? viagem.getId() : null,
            viagem != null && viagem.getPeriodo() != null ? viagem.getPeriodo().name() : null
        );
    }
}
