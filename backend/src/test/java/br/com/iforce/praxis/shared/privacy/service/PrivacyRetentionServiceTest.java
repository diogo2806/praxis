package br.com.iforce.praxis.shared.privacy.service;

import br.com.iforce.praxis.audit.model.AuditEventType;

import br.com.iforce.praxis.audit.persistence.repository.AuditEventRepository;

import br.com.iforce.praxis.audit.service.AuditEventService;

import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;

import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.jdbc.SqlMergeMode;

import org.springframework.test.context.jdbc.Sql;


import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(scripts = {"/seed-simulation-fixture.sql", "/empresa-isolation-fixtures.sql"})
@Sql(statements = {
        "DELETE FROM audit_events WHERE aggregate_id IN ('retention-empresa1-old', 'retention-empresa1-recent', 'retention-empresa1-other', 'retention-empresa2-old')",
        "DELETE FROM candidate_attempts WHERE id IN ('retention-empresa1-old', 'retention-empresa1-recent', 'retention-empresa1-other', 'retention-empresa2-old')"
})
@Sql(statements = {
        "DELETE FROM audit_events WHERE aggregate_id IN ('retention-empresa1-old', 'retention-empresa1-recent', 'retention-empresa1-other', 'retention-empresa2-old')",
        "DELETE FROM candidate_attempts WHERE id IN ('retention-empresa1-old', 'retention-empresa1-recent', 'retention-empresa1-other', 'retention-empresa2-old')"
}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class PrivacyRetentionServiceTest {

    @Autowired
    private CandidateAttemptRepository candidateAttemptRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private PrivacyRetentionService privacyRetentionService;

    @Test
    @Sql(statements = {
            "INSERT INTO candidate_attempts (id, empresa_id, company_id, result_id, simulation_id, simulation_version_id, simulation_version_number, idempotency_key, candidate_name, candidate_email, result_webhook_url, status, score, decision, human_review_required, reliability_level, company_result_string, created_at, started_at, finished_at, anonymized_at) VALUES ('retention-empresa1-old', 'empresa-1', 'empresa-123', 'result-retention-old', 'sim-atendimento-caos', 1, 1, 'idem-retention-old', 'Ana Dados', 'ana.dados@example.com', 'https://cliente.gupy.io/result-webhook', 'COMPLETED', 82, 'RECOMMEND_INTERVIEW', FALSE, 'NORMAL', 'Resultado preservado', CURRENT_TIMESTAMP - INTERVAL '220' DAY, CURRENT_TIMESTAMP - INTERVAL '220' DAY, CURRENT_TIMESTAMP - INTERVAL '200' DAY, NULL)",
            "INSERT INTO candidate_attempts (id, empresa_id, company_id, result_id, simulation_id, simulation_version_id, simulation_version_number, idempotency_key, candidate_name, candidate_email, result_webhook_url, status, score, decision, human_review_required, reliability_level, company_result_string, created_at, started_at, finished_at, anonymized_at) VALUES ('retention-empresa1-recent', 'empresa-1', 'empresa-123', 'result-retention-recent', 'sim-atendimento-caos', 1, 1, 'idem-retention-recent', 'Bruno Recente', 'bruno@example.com', 'https://cliente.gupy.io/result-webhook', 'COMPLETED', 76, 'RECOMMEND_INTERVIEW', FALSE, 'NORMAL', 'Resultado recente', CURRENT_TIMESTAMP - INTERVAL '20' DAY, CURRENT_TIMESTAMP - INTERVAL '20' DAY, CURRENT_TIMESTAMP - INTERVAL '10' DAY, NULL)"
    })
    void anonymizesExpiredClosedAttemptsAndRegistersAuditEvent() {
        int count = privacyRetentionService.anonymizeExpiredAttemptsForEmpresa("empresa-1");

        assertThat(count).isEqualTo(1);

        CandidateAttemptEntity oldAttempt = candidateAttemptRepository.findById("retention-empresa1-old").orElseThrow();
        assertThat(oldAttempt.getCandidateName()).isEqualTo("Candidato anonimizado");
        assertThat(oldAttempt.getCandidateEmail()).isEqualTo("anonimizado+retention-empresa1-old@privacy.local");
        assertThat(oldAttempt.getIdempotencyKey()).isEqualTo("anonymized:retention-empresa1-old");
        assertThat(oldAttempt.getResultWebhookUrl()).isNull();
        assertThat(oldAttempt.getCompanyResultString()).isEqualTo("Resultado preservado");
        assertThat(oldAttempt.getAnonymizedAt()).isNotNull();

        CandidateAttemptEntity recentAttempt = candidateAttemptRepository.findById("retention-empresa1-recent").orElseThrow();
        assertThat(recentAttempt.getCandidateName()).isEqualTo("Bruno Recente");
        assertThat(recentAttempt.getAnonymizedAt()).isNull();

        var auditEvents = auditEventRepository.findByEmpresaIdAndAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
                "empresa-1",
                AuditEventService.CANDIDATE_ATTEMPT_AGGREGATE,
                "retention-empresa1-old"
        );
        assertThat(auditEvents).hasSize(1);
        assertThat(auditEvents.getFirst().getEventType()).isEqualTo(AuditEventType.ATTEMPT_ANONYMIZED);
    }

    @Test
    @Sql(statements = {
            "INSERT INTO candidate_attempts (id, empresa_id, company_id, result_id, simulation_id, simulation_version_id, simulation_version_number, idempotency_key, candidate_name, candidate_email, result_webhook_url, status, score, decision, human_review_required, reliability_level, company_result_string, created_at, started_at, finished_at, anonymized_at) VALUES ('retention-empresa1-other', 'empresa-1', 'empresa-123', 'result-retention-empresa1', 'sim-atendimento-caos', 1, 1, 'idem-retention-empresa1', 'Empresa Um', 'um@example.com', 'https://cliente.gupy.io/result-webhook', 'COMPLETED', 82, 'RECOMMEND_INTERVIEW', FALSE, 'NORMAL', 'Resultado empresa 1', CURRENT_TIMESTAMP - INTERVAL '220' DAY, CURRENT_TIMESTAMP - INTERVAL '220' DAY, CURRENT_TIMESTAMP - INTERVAL '200' DAY, NULL)",
            "INSERT INTO candidate_attempts (id, empresa_id, company_id, result_id, simulation_id, simulation_version_id, simulation_version_number, idempotency_key, candidate_name, candidate_email, result_webhook_url, status, score, decision, human_review_required, reliability_level, company_result_string, created_at, started_at, finished_at, anonymized_at) VALUES ('retention-empresa2-old', 'empresa-2', 'empresa-456', 'result-retention-empresa2', 'sim-empresa2', 9001, 1, 'idem-retention-empresa2', 'Empresa Dois', 'dois@example.com', 'https://cliente.gupy.io/result-webhook', 'COMPLETED', 82, 'RECOMMEND_INTERVIEW', FALSE, 'NORMAL', 'Resultado empresa 2', CURRENT_TIMESTAMP - INTERVAL '220' DAY, CURRENT_TIMESTAMP - INTERVAL '220' DAY, CURRENT_TIMESTAMP - INTERVAL '200' DAY, NULL)"
    })
    void empresaExecutionDoesNotAnonymizeAnotherEmpresa() {
        int count = privacyRetentionService.anonymizeExpiredAttemptsForEmpresa("empresa-1");

        assertThat(count).isEqualTo(1);
        assertThat(candidateAttemptRepository.findById("retention-empresa1-other").orElseThrow().getAnonymizedAt()).isNotNull();
        assertThat(candidateAttemptRepository.findById("retention-empresa2-old").orElseThrow().getCandidateName())
                .isEqualTo("Empresa Dois");
        assertThat(candidateAttemptRepository.findById("retention-empresa2-old").orElseThrow().getAnonymizedAt()).isNull();
    }
}
