package br.com.iforce.praxis.simulation.validation;

import br.com.iforce.praxis.simulation.dto.CompetencyWeightDto;
import br.com.iforce.praxis.simulation.dto.CreateSimulationRequest;
import br.com.iforce.praxis.simulation.dto.UpdateBlueprintRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;

public class SumWeightsEqualsOneValidator implements ConstraintValidator<SumWeightsEqualsOne, Object> {

    private static final double TOLERANCE = 0.001;

    @Override
    public boolean isValid(Object request, ConstraintValidatorContext context) {
        List<Double> weights = weights(request);
        if (weights == null || weights.isEmpty()) {
            return true;
        }

        double sum = weights
                .stream()
                .mapToDouble(weight -> weight == null ? 0.0 : weight)
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

    private List<Double> weights(Object request) {
        if (request instanceof UpdateBlueprintRequest updateBlueprintRequest) {
            return updateBlueprintRequest.competencies()
                    .stream()
                    .map(UpdateBlueprintRequest.CompetencyRequest::weight)
                    .toList();
        }

        if (request instanceof CreateSimulationRequest createSimulationRequest) {
            return createSimulationRequest.competencies()
                    .stream()
                    .map(CompetencyWeightDto::weight)
                    .toList();
        }

        return List.of();
    }
}
