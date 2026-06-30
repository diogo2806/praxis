package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.auth.context.EmpresaContextHolder;

import br.com.iforce.praxis.gupy.model.AttemptStatus;

import br.com.iforce.praxis.gupy.model.ReliabilityLevel;

import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;

import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.test.context.TestPropertySource;


import java.time.Instant;


import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = {
        "praxis.security.enabled=false",
        "praxis.default-empresa-id=empresa-1"
})
class MultiEmpresaIsolationTest {

    @Autowired
    private CandidateAttemptRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private CandidateAttemptEntity empresa1Attempt;
    private CandidateAttemptEntity empresa2Attempt;

    @BeforeEach
    void setUp() {
        EmpresaContextHolder.clear();
        jdbcTemplate.update("""
                INSERT INTO empresas (id, name, company_id)
                VALUES ('empresa-2', 'Empresa 2', 'company-2')
                """);
        jdbcTemplate.update("""
                INSERT INTO simulations (id, empresa_id, name, description, created_at)
                VALUES ('sim-1', 'empresa-1', 'Test Simulation', 'Test simulation description', CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO simulation_versions (id, simulation_id, version_number, status, root_node_id, created_at)
                VALUES (1, 'sim-1', 1, 'draft', 'node-1', CURRENT_TIMESTAMP)
                """);

        empresa1Attempt = createAttempt("empresa-1", "attempt-1", "idempotency-1", "result-1");
        repository.save(empresa1Attempt);

        empresa2Attempt = createAttempt("empresa-2", "attempt-2", "idempotency-2", "result-2");
        repository.save(empresa2Attempt);
    }

    @Test
    void testFindByEmpresaIdAndIdRespectsIsolation() {
        assertTrue(repository.findByEmpresaIdAndId("empresa-1", "attempt-1").isPresent());
        assertTrue(repository.findByEmpresaIdAndId("empresa-2", "attempt-2").isPresent());

        assertFalse(repository.findByEmpresaIdAndId("empresa-1", "attempt-2").isPresent());
        assertFalse(repository.findByEmpresaIdAndId("empresa-2", "attempt-1").isPresent());
    }

    @Test
    void testFindByEmpresaIdAndIdempotencyKeyRespectsIsolation() {
        assertTrue(repository.findByEmpresaIdAndIdempotencyKey("empresa-1", "idempotency-1").isPresent());
        assertTrue(repository.findByEmpresaIdAndIdempotencyKey("empresa-2", "idempotency-2").isPresent());

        assertFalse(repository.findByEmpresaIdAndIdempotencyKey("empresa-1", "idempotency-2").isPresent());
        assertFalse(repository.findByEmpresaIdAndIdempotencyKey("empresa-2", "idempotency-1").isPresent());
    }

    @Test
    void testFindByEmpresaIdAndResultIdRespectsIsolation() {
        assertTrue(repository.findByEmpresaIdAndResultId("empresa-1", "result-1").isPresent());
        assertTrue(repository.findByEmpresaIdAndResultId("empresa-2", "result-2").isPresent());

        assertFalse(repository.findByEmpresaIdAndResultId("empresa-1", "result-2").isPresent());
        assertFalse(repository.findByEmpresaIdAndResultId("empresa-2", "result-1").isPresent());
    }

    @Test
    void testCountByEmpresaIdRespectsIsolation() {
        long empresa1Count = repository.countByEmpresaIdAndSimulationVersionId("empresa-1", 1L);
        long empresa2Count = repository.countByEmpresaIdAndSimulationVersionId("empresa-2", 1L);

        assertEquals(1, empresa1Count);
        assertEquals(1, empresa2Count);
    }

    private CandidateAttemptEntity createAttempt(
            String empresaId,
            String id,
            String idempotencyKey,
            String resultId
    ) {
        CandidateAttemptEntity entity = new CandidateAttemptEntity();
        entity.setEmpresaId(empresaId);
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
        entity.setReliabilityLevel(ReliabilityLevel.NORMAL);
        entity.setCompanyResultString("{}");
        entity.setCreatedAt(Instant.now());
        return entity;
    }
}
