package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.HistoricoLocalizacaoViagem;
import TioDaPerua.BackEndJava.entities.LocalizacaoViagemAtual;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

public record LocalizacaoViagemDTO(
        Long idViagemDia,
        BigDecimal latitude,
        BigDecimal longitude,
        Instant instanteDispositivo,
        Instant recebidoEm,
        boolean desatualizada) {

    public static LocalizacaoViagemDTO fromCurrent(LocalizacaoViagemAtual localizacao, Instant agora) {
        return criar(localizacao.getViagemDia().getId(), localizacao.getLatitude(), localizacao.getLongitude(),
                localizacao.getInstanteDispositivo(), localizacao.getRecebidoEm(), agora);
    }

    public static LocalizacaoViagemDTO fromHistory(HistoricoLocalizacaoViagem localizacao, Instant agora) {
        return criar(localizacao.getViagemDia().getId(), localizacao.getLatitude(), localizacao.getLongitude(),
                localizacao.getInstanteDispositivo(), localizacao.getRecebidoEm(), agora);
    }

    private static LocalizacaoViagemDTO criar(
            Long viagemDiaId,
            BigDecimal latitude,
            BigDecimal longitude,
            Instant instanteDispositivo,
            Instant recebidoEm,
            Instant agora) {
        boolean desatualizada = Duration.between(recebidoEm, agora).compareTo(Duration.ofSeconds(45)) > 0;
        return new LocalizacaoViagemDTO(
                viagemDiaId, latitude, longitude, instanteDispositivo, recebidoEm, desatualizada);
    }
}
