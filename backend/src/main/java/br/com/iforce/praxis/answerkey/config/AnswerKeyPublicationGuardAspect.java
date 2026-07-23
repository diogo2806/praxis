package br.com.iforce.praxis.answerkey.config;

import br.com.iforce.praxis.answerkey.service.AnswerKeyReviewService;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AnswerKeyPublicationGuardAspect {

    private final AnswerKeyReviewService answerKeyReviewService;

    public AnswerKeyPublicationGuardAspect(AnswerKeyReviewService answerKeyReviewService) {
        this.answerKeyReviewService = answerKeyReviewService;
    }

    @Before(
            value = "execution(* br.com.iforce.praxis.simulation.controller.SimulationAdminController.publishVersion(..)) "
                    + "&& args(simulationId, versionNumber)",
            argNames = "simulationId,versionNumber"
    )
    public void requireApprovedAnswerKey(String simulationId, int versionNumber) {
        answerKeyReviewService.requireApproved(simulationId, versionNumber);
    }
}
