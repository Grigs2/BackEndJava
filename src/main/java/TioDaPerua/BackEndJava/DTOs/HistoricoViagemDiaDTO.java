package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.HistoricoViagemDia;
import TioDaPerua.BackEndJava.enums.StatusViagemDia;

import java.time.LocalDateTime;

public record HistoricoViagemDiaDTO(
        Long id,
        StatusViagemDia statusAnterior,
        StatusViagemDia statusNovo,
        Long atorUsuarioId,
        LocalDateTime ocorridoEm,
        String motivo) {

    public static HistoricoViagemDiaDTO toDTO(HistoricoViagemDia historico) {
        return new HistoricoViagemDiaDTO(
                historico.getId(),
                historico.getStatusAnterior(),
                historico.getStatusNovo(),
                historico.getAtor().getId(),
                historico.getOcorridoEm(),
                historico.getMotivo());
    }
}
