package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class TermosService {

    private final String versaoVigente;

    public TermosService(@Value("${app.terms.version:}") String versaoVigente) {
        this.versaoVigente = versaoVigente;
    }

    public void registrarAceite(Usuario usuario, Boolean aceitouTermos) {
        if (!Boolean.TRUE.equals(aceitouTermos)) {
            throw new BusinessException("O aceite dos termos é obrigatório para concluir o cadastro");
        }
        usuario.setTermosAceitosEm(Instant.now());
        usuario.setTermosVersao(versaoVigente == null || versaoVigente.isBlank()
                ? null : versaoVigente.trim());
    }
}
