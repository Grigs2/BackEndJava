package TioDaPerua.BackEndJava.validations;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CNHValidator implements ConstraintValidator<CNH, String> {

    @Override
    public boolean isValid(String cnh, ConstraintValidatorContext context) {
        if (cnh == null || cnh.isEmpty()) {
            return true; // Use @NotBlank para validar presença
        }

        // Deve ter exatamente 11 dígitos
        if (!cnh.matches("^\\d{11}$")) {
            return false;
        }

        // Impede sequências repetidas como 11111111111, 00000000000, etc.
        if (cnh.matches("^(\\d)\\1{10}$")) {
            return false;
        }

        // Validação dos Dígitos Verificadores do SENATRAN
        int v = 0, j = 9;
        for (int i = 0; i < 9; i++, j--) {
            v += (cnh.charAt(i) - '0') * j;
        }

        int d1 = v % 11;
        int vlD1 = (d1 >= 10) ? 0 : d1;

        v = 0;
        j = 1;
        for (int i = 0; i < 9; i++, j++) {
            v += (cnh.charAt(i) - '0') * j;
        }

        int x = v % 11;
        int vlD2 = (x >= 10) ? 0 : x;

        return (vlD1 == (cnh.charAt(9) - '0')) && (vlD2 == (cnh.charAt(10) - '0'));
    }
}