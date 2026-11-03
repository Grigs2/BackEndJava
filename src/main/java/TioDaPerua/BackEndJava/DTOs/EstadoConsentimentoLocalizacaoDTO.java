package TioDaPerua.BackEndJava.DTOs;

import java.util.List;

public record EstadoConsentimentoLocalizacaoDTO(
        Long idViagemDia,
        String versaoPolitica,
        boolean motoristaConsentiu,
        List<Long> dependentesPendentes,
        boolean gpsSuspenso,
        boolean podeAtualizar) {
}
