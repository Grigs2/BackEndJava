package TioDaPerua.BackEndJava.validations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CNHValidator.class)
public @interface CNH {
    String message() default "Número de CNH inválido.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}