package br.com.iforce.praxis.results.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Relatório executivo determinístico de uma tentativa concluída.
 *
 * <p>O contrato separa evidência observada, interpretação configurada na avaliação,
 * roteiro de entrevista editado pelo recrutador e trilha de auditoria. Nenhuma
 * recomendação de contratação, reprovação ou ranqueamento é produzida.</p>
 */
public record ResultExecutiveReportResponse(
        String attemptId,
        String simulationTitle,
        Integer simulationVersionNumber,
        Instant generatedAt,
        ExecutiveSummary summary,
        InterviewGuide interviewGuide,
        List<AuditEntry> auditTrail
) {

    public record ExecutiveSummary(
            List<CompetencyInsight> competencies,
            List<Evidence> criticalEvidence,
            List<String> deepDiveCompetencies,
            List<String> limitations
    ) {
    }

    public record CompetencyInsight(
            String name,
            int score,
            String level,
            String interpretation,
            int evidenceCount,
            List<String> evidenceReferences
    ) {
    }

    public record Evidence(
            String reference,
            String stepTitle,
            String observedAnswer,
            String configuredInterpretation,
            boolean critical,
            Map<String, Integer> competencyScores
    ) {
    }

    public record InterviewGuide(
            List<InterviewQuestion> questions,
            String interviewerNotes,
            boolean persisted,
            String savedBy,
            Instant savedAt
    ) {
    }

    public record InterviewQuestion(
            String id,
            String competency,
            String question,
            String sourceType,
            String evidenceReference
    ) {
    }

    public record AuditEntry(
            String eventType,
            String message,
            Instant createdAt
    ) {
    }
}
