package br.com.iforce.praxis.candidate.dto;

import br.com.iforce.praxis.audit.dto.AuditEventResponse;
import br.com.iforce.praxis.gupy.model.ReliabilityLevel;
import br.com.iforce.praxis.gupy.model.ResultDecision;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Relatório consolidado de transparência do scoring (REQ-L4). Documento legível por
 * compliance/jurídico do cliente (ou um regulador) que comprova que a pontuação é determinística,
 * sem IA e sem dados de treino, mostrando a fórmula, a versão do blueprint, o caminho percorrido,
 * os pontos por competência, a trilha append-only e a decisão humana (REQ-L1).
 */
@Schema(description = "Relatório de transparência do scoring de um candidato.")
public record EvidenceReport(
        String attemptId,
        String candidateName,
        String candidateEmail,
        String simulationId,
        String simulationName,
        Integer versionNumber,
        Long versionId,
        Integer generalScore,
        ResultDecision decision,
        ReliabilityLevel reliabilityLevel,
        boolean humanReviewRequired,
        String summaryMarkdown,
        Instant startedAt,
        Instant finishedAt,
        ScoringDeclaration declaration,
        List<CompetencyEvidence> competencies,
        List<PathStepEvidence> path,
        HumanDecisionEvidence humanDecision,
        List<AuditEventResponse> auditTrail
) {

    @Schema(description = "Declaração formal de como o score é produzido.")
    public record ScoringDeclaration(
            boolean deterministic,
            boolean usesArtificialIntelligence,
            boolean usesTrainingData,
            String statement,
            String formula,
            int recommendInterviewThreshold
    ) {
    }

    @Schema(description = "Pontuação por competência e o peso configurado no blueprint.")
    public record CompetencyEvidence(
            String name,
            int score,
            String tier,
            Double weight
    ) {
    }

    @Schema(description = "Um turno do caminho percorrido pelo candidato, com os pontos da escolha.")
    public record PathStepEvidence(
            int turnIndex,
            String nodeId,
            String speaker,
            String prompt,
            String answeredOptionId,
            String answeredOptionText,
            boolean timedOut,
            boolean critical,
            Map<String, Integer> competencyPoints,
            Instant answeredAt
    ) {
    }

    @Schema(description = "Decisão humana registrada (REQ-L1), extraída da trilha append-only.")
    public record HumanDecisionEvidence(
            String decision,
            String decidedByUserId,
            String reason,
            Instant decidedAt
    ) {
    }
}
