package TioDaPerua.BackEndJava.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SenhaValidaValidator.class)
public @interface SenhaValida {
    String message() default "A senha deve conter ao menos 12 caracteres e no máximo 72 bytes UTF-8";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
