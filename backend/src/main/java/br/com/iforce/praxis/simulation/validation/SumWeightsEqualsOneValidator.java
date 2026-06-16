package br.com.iforce.praxis.simulation.validation;

import br.com.iforce.praxis.simulation.dto.UpdateBlueprintRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SumWeightsEqualsOneValidator implements ConstraintValidator<SumWeightsEqualsOne, UpdateBlueprintRequest> {

    private static final double TOLERANCE = 0.001;

    @Override
    public boolean isValid(UpdateBlueprintRequest request, ConstraintValidatorContext context) {
        if (request == null || request.competencies() == null || request.competencies().isEmpty()) {
            return true;
        }

        double sum = request.competencies()
                .stream()
                .mapToDouble(competency -> competency.weight() == null ? 0.0 : competency.weight())
                .sum();

        boolean valid = Math.abs(sum - 1.0) <= TOLERANCE;
        if (!valid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                            "A soma dos pesos das competencias deve ser 1.0 (atual: " + sum + ")."
                    )
                    .addPropertyNode("competencies")
                    .addConstraintViolation();
        }

        return valid;
    }
}
