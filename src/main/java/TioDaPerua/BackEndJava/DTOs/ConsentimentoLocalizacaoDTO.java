package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.ConsentimentoLocalizacao;
import TioDaPerua.BackEndJava.enums.AcaoConsentimentoLocalizacao;

import java.time.Instant;

public record ConsentimentoLocalizacaoDTO(
        boolean aceito,
        String versaoPolitica,
        Instant ocorridoEm) {

    public static ConsentimentoLocalizacaoDTO from(ConsentimentoLocalizacao consentimento) {
        return new ConsentimentoLocalizacaoDTO(
                consentimento != null && consentimento.getAcao() == AcaoConsentimentoLocalizacao.ACEITO,
                consentimento == null ? null : consentimento.getVersaoPolitica(),
                consentimento == null ? null : consentimento.getOcorridoEm());
    }
}
