package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.enums.StatusEventoSincronizacao;
import TioDaPerua.BackEndJava.enums.StatusViagemPresenca;

import java.util.UUID;

public record ResultadoEventoPresencaDTO(
        UUID idEvento,
        Long idViagemDia,
        Long idDependente,
        StatusEventoSincronizacao resultado,
        StatusViagemPresenca statusAtual,
        String mensagem) {
}
