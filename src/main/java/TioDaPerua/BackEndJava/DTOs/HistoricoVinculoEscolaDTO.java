package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.HistoricoVinculoEscola;

import java.time.Instant;

public record HistoricoVinculoEscolaDTO(
        Long id,
        Long vinculoId,
        String statusAnterior,
        String statusNovo,
        Long atorUsuarioId,
        String perfilAtor,
        String motivo,
        Instant ocorridoEm) {

    public static HistoricoVinculoEscolaDTO toDTO(HistoricoVinculoEscola historico) {
        return new HistoricoVinculoEscolaDTO(
                historico.getId(),
                historico.getVinculo().getId(),
                historico.getStatusAnterior() == null ? null : historico.getStatusAnterior().name(),
                historico.getStatusNovo().name(),
                historico.getAtor().getId(),
                historico.getAtor().getTipoPerfil(),
                historico.getMotivo(),
                historico.getOcorridoEm());
    }
}
