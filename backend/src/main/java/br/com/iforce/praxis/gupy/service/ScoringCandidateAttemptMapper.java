package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.gupy.model.CandidateAttempt;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Complementa o mapeamento legado com o snapshot imutável da fórmula de
 * pontuação. O mapper base continua disponível para testes unitários antigos.
 */
@Primary
@Component
public class ScoringCandidateAttemptMapper extends CandidateAttemptMapper {

    public ScoringCandidateAttemptMapper(PraxisProperties praxisProperties) {
        super(praxisProperties);
    }

    @Override
    public void applyDomainToEntity(
            CandidateAttempt attempt,
            CandidateAttemptEntity candidateAttemptEntity
    ) {
        super.applyDomainToEntity(attempt, candidateAttemptEntity);
        candidateAttemptEntity.setRawScore(attempt.rawScore());
        candidateAttemptEntity.setPathMaximumScore(attempt.pathMaximumScore());
        candidateAttemptEntity.setNormalizedScore(attempt.normalizedScore());
        candidateAttemptEntity.setScoringAlgorithmVersion(attempt.scoringAlgorithmVersion());
    }

    @Override
    public CandidateAttempt toDomain(CandidateAttemptEntity candidateAttemptEntity) {
        CandidateAttempt attempt = super.toDomain(candidateAttemptEntity);
        Integer normalizedScore = candidateAttemptEntity.getNormalizedScore() == null
                ? candidateAttemptEntity.getScore()
                : candidateAttemptEntity.getNormalizedScore();
        String algorithmVersion = candidateAttemptEntity.getScoringAlgorithmVersion();
        if (algorithmVersion == null && normalizedScore != null) {
            algorithmVersion = "legacy-path-normalized-v1";
        }
        return attempt.toBuilder()
                .rawScore(candidateAttemptEntity.getRawScore())
                .pathMaximumScore(candidateAttemptEntity.getPathMaximumScore())
                .normalizedScore(normalizedScore)
                .scoringAlgorithmVersion(algorithmVersion)
                .build();
    }
}
