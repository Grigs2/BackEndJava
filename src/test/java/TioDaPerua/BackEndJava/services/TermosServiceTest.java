package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TermosServiceTest {

    @Test
    void recordsConfiguredVersionAndAcceptanceInstant() {
        Usuario usuario = new Usuario();
        new TermosService("31-10-2026").registrarAceite(usuario, true);

        assertEquals("31-10-2026", usuario.getTermosVersao());
        assertNotNull(usuario.getTermosAceitosEm());
    }

    @Test
    void keepsAcceptanceRequiredWhenVersionIsNotConfigured() {
        Usuario usuario = new Usuario();
        new TermosService("").registrarAceite(usuario, true);

        assertNull(usuario.getTermosVersao());
        assertNotNull(usuario.getTermosAceitosEm());
        assertThrows(BusinessException.class,
                () -> new TermosService("").registrarAceite(new Usuario(), false));
    }

    @Test
    void pedeNovoAceiteQuandoAVersaoVigenteMuda() {
        Usuario usuario = new Usuario();
        new TermosService("31-10-2026").registrarAceite(usuario, true);

        org.junit.jupiter.api.Assertions.assertFalse(new TermosService("31-10-2026").termosPendentes(usuario));
        org.junit.jupiter.api.Assertions.assertTrue(new TermosService("15-01-2027").termosPendentes(usuario));
        // Sem versão configurada não há pendência.
        org.junit.jupiter.api.Assertions.assertFalse(new TermosService("").termosPendentes(new Usuario()));
    }
}
