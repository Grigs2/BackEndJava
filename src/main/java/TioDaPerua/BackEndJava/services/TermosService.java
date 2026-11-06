package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class TermosService {

    /** Caminho do PDF dos termos, relativo à base da API (servido como arquivo estático). */
    public static final String CAMINHO_PDF = "termos/termos-de-uso-e-privacidade.pdf";

    private final String versaoVigente;

    public TermosService(@Value("${app.terms.version:}") String versaoVigente) {
        this.versaoVigente = versaoVigente;
    }

    /** Versão vigente configurada em TERMS_VERSION; nula quando não configurada. */
    public String getVersaoVigente() {
        return versaoVigente == null || versaoVigente.isBlank() ? null : versaoVigente.trim();
    }

    public void registrarAceite(Usuario usuario, Boolean aceitouTermos) {
        if (!Boolean.TRUE.equals(aceitouTermos)) {
            throw new BusinessException("O aceite dos termos é obrigatório para concluir o cadastro");
        }
        usuario.setAceitouTermos(true);
        usuario.setTermosAceitosEm(Instant.now());
        usuario.setTermosVersao(getVersaoVigente());
    }

    /**
     * O usuário precisa aceitar de novo quando há versão vigente configurada e ela é diferente da que ele aceitou.
     * Sem versão configurada, nunca há pendência.
     */
    public boolean termosPendentes(Usuario usuario) {
        String vigente = getVersaoVigente();
        return usuario != null && vigente != null && !vigente.equals(usuario.getTermosVersao());
    }
}
