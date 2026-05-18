package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.ViagemPresenca;
import java.time.LocalDateTime;

public record ViagemPresencaDTO(Long id,
                               Long viagemDiaId,
                               Long dependenteId,
                               String status,
                               LocalDateTime horarioEmbarque,
                               LocalDateTime horarioDesembarque) {

    public static ViagemPresencaDTO toDTO(ViagemPresenca vp) {
        if (vp == null) return null;
        return new ViagemPresencaDTO(
                vp.getId(),
                vp.getViagemDia() != null ? vp.getViagemDia().getId() : null,
                vp.getDependente() != null ? vp.getDependente().getId() : null,
                vp.getStatus() != null ? vp.getStatus().name() : null,
                vp.getHorarioEmbarque(),
                vp.getHorarioDesembarque()
        );
    }
}
