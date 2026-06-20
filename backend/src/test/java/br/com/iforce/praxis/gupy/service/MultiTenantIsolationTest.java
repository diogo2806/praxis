package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.auth.context.TenantContextHolder;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Sql(scripts = {"/seed-simulation-fixture.sql", "/tenant-isolation-fixtures.sql"})
@TestPropertySource(properties = {
        "praxis.security.enabled=false",
        "praxis.default-tenant-id=tenant-1"
})
class MultiTenantIsolationTest {

    @Autowired
    private CandidateAttemptRepository repository;

    private CandidateAttemptEntity tenant1Attempt;
    private CandidateAttemptEntity tenant2Attempt;

    @BeforeEach
    void setUp() {
        TenantContextHolder.clear();

        tenant1Attempt = createAttempt("tenant-1", "attempt-1", "idempotency-1", "result-1");
        repository.save(tenant1Attempt);

        tenant2Attempt = createAttempt("tenant-2", "attempt-2", "idempotency-2", "result-2");
        repository.save(tenant2Attempt);
    }

    @Test
    void testFindByTenantIdAndIdRespectsIsolation() {
        assertTrue(repository.findByTenantIdAndId("tenant-1", "attempt-1").isPresent());
        assertTrue(repository.findByTenantIdAndId("tenant-2", "attempt-2").isPresent());

        assertFalse(repository.findByTenantIdAndId("tenant-1", "attempt-2").isPresent());
        assertFalse(repository.findByTenantIdAndId("tenant-2", "attempt-1").isPresent());
    }

    @Test
    void testFindByTenantIdAndIdempotencyKeyRespectsIsolation() {
        assertTrue(repository.findByTenantIdAndIdempotencyKey("tenant-1", "idempotency-1").isPresent());
        assertTrue(repository.findByTenantIdAndIdempotencyKey("tenant-2", "idempotency-2").isPresent());

        assertFalse(repository.findByTenantIdAndIdempotencyKey("tenant-1", "idempotency-2").isPresent());
        assertFalse(repository.findByTenantIdAndIdempotencyKey("tenant-2", "idempotency-1").isPresent());
    }

    @Test
    void testFindByTenantIdAndResultIdRespectsIsolation() {
        assertTrue(repository.findByTenantIdAndResultId("tenant-1", "result-1").isPresent());
        assertTrue(repository.findByTenantIdAndResultId("tenant-2", "result-2").isPresent());

        assertFalse(repository.findByTenantIdAndResultId("tenant-1", "result-2").isPresent());
        assertFalse(repository.findByTenantIdAndResultId("tenant-2", "result-1").isPresent());
    }

    @Test
    void testCountByTenantIdRespectsIsolation() {
        long tenant1Count = repository.countByTenantIdAndSimulationVersionId("tenant-1", 1L);
        long tenant2Count = repository.countByTenantIdAndSimulationVersionId("tenant-2", 1L);

        assertEquals(1, tenant1Count);
        assertEquals(1, tenant2Count);
    }

    private CandidateAttemptEntity createAttempt(
            String tenantId,
            String id,
            String idempotencyKey,
            String resultId
    ) {
        CandidateAttemptEntity entity = new CandidateAttemptEntity();
        entity.setTenantId(tenantId);
        entity.setId(id);
        entity.setIdempotencyKey(idempotencyKey);
        entity.setResultId(resultId);
        entity.setSimulationVersionId(1L);
        entity.setCandidateName("Test Candidate");
        entity.setCandidateEmail("test@example.com");
        entity.setCompanyId("company-1");
        entity.setSimulationId("sim-1");
        entity.setStatus(AttemptStatus.NOT_STARTED);
        entity.setDecision(br.com.iforce.praxis.gupy.model.ResultDecision.RECOMMEND_INTERVIEW);
        entity.setHumanReviewRequired(false);
        entity.setCompanyResultString("{}");
        entity.setCreatedAt(Instant.now());
        return entity;
    }
}
