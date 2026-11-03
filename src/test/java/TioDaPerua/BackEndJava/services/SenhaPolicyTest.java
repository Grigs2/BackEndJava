package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.validation.SenhaPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SenhaPolicyTest {

    @Test
    void acceptsTwelveCharactersWithinBcryptByteLimit() {
        assertTrue(SenhaPolicy.valida("senha1234567"));
        assertTrue(SenhaPolicy.valida("é".repeat(12)));
    }

    @Test
    void rejectsPasswordsBelowMinimumOrAboveUtf8ByteLimit() {
        assertFalse(SenhaPolicy.valida("curta"));
        assertFalse(SenhaPolicy.valida("a".repeat(73)));
        assertFalse(SenhaPolicy.valida("😀".repeat(19)));
    }
}
