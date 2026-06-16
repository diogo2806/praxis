package br.com.iforce.praxis.simulation.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SumWeightsEqualsOneValidator.class)
public @interface SumWeightsEqualsOne {

    String message() default "A soma dos pesos das competencias deve ser 1.0.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
