package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.HistoricoOrdemParada;

import java.time.LocalDateTime;

public record HistoricoOrdemParadaDTO(
        Long id,
        Long idParada,
        int ordemAnterior,
        int ordemNova,
        Long atorUsuarioId,
        LocalDateTime ocorridoEm) {

    public static HistoricoOrdemParadaDTO toDTO(HistoricoOrdemParada historico) {
        return new HistoricoOrdemParadaDTO(
                historico.getId(),
                historico.getParada().getId(),
                historico.getOrdemAnterior(),
                historico.getOrdemNova(),
                historico.getAtor().getId(),
                historico.getOcorridoEm());
    }
}
