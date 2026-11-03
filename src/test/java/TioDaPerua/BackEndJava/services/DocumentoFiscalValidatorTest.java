package TioDaPerua.BackEndJava.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentoFiscalValidatorTest {

    @Test
    void validatesCpfCheckDigits() {
        assertTrue(DocumentoFiscalValidator.cpfValido("529.982.247-25"));
        assertFalse(DocumentoFiscalValidator.cpfValido("111.111.111-11"));
        assertFalse(DocumentoFiscalValidator.cpfValido("529.982.247-24"));
    }

    @Test
    void validatesCnpjCheckDigitsAndNormalizesFormatting() {
        assertTrue(DocumentoFiscalValidator.cnpjValido("11.222.333/0001-81"));
        assertFalse(DocumentoFiscalValidator.cnpjValido("11.222.333/0001-80"));
    }
}
