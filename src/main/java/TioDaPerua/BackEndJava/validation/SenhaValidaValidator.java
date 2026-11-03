package TioDaPerua.BackEndJava.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SenhaValidaValidator implements ConstraintValidator<SenhaValida, String> {

    @Override
    public boolean isValid(String senha, ConstraintValidatorContext context) {
        if (senha == null) {
            return true;
        }
        return SenhaPolicy.valida(senha);
    }
}
