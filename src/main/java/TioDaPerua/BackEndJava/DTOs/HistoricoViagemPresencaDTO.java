package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.HistoricoViagemPresenca;
import TioDaPerua.BackEndJava.enums.StatusViagemPresenca;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record HistoricoViagemPresencaDTO(
        Long id,
        Long idPresenca,
        Long idDependente,
        StatusViagemPresenca statusAnterior,
        StatusViagemPresenca statusNovo,
        Long atorUsuarioId,
        LocalDateTime ocorridoEm,
        UUID eventoClienteId,
        Instant instanteDispositivo,
        Instant recebidoEm) {

    public static HistoricoViagemPresencaDTO toDTO(HistoricoViagemPresenca historico) {
        return new HistoricoViagemPresencaDTO(
                historico.getId(),
                historico.getPresenca().getId(),
                historico.getPresenca().getDependente().getId(),
                historico.getStatusAnterior(),
                historico.getStatusNovo(),
                historico.getAtor().getId(),
                historico.getOcorridoEm(),
                historico.getEventoClienteId(),
                historico.getInstanteDispositivo(),
                historico.getRecebidoEm());
    }
}
