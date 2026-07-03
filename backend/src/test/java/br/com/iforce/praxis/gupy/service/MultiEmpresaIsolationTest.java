package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.auth.context.EmpresaContextHolder;

import br.com.iforce.praxis.gupy.model.AttemptStatus;

import br.com.iforce.praxis.gupy.model.ReliabilityLevel;

import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;

import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.test.context.TestPropertySource;


import java.time.Instant;


import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
// Não substituir o datasource por um embutido (H2): as migrations Flyway usam sintaxe
// exclusiva do PostgreSQL, então o teste precisa rodar contra o Postgres real (Testcontainers).
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
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

    // Versão própria e única para as tentativas do teste. Evita colidir com a versão 1 de
    // sim-atendimento-caos (que outras classes @SpringBootTest persistem/commitam no mesmo banco
    // compartilhado) tanto na PK de simulation_versions quanto nas contagens por versão.
    private static final long ISOLATION_VERSION_ID = 987001L;

    @BeforeEach
    void setUp() {
        EmpresaContextHolder.clear();
        // Os INSERTs são idempotentes: empresa-2/sim-1 podem já ter sido commitados por outra
        // classe de teste que roda antes desta no mesmo banco (o @DataJpaTest desta classe faz
        // rollback, mas dados commitados por classes @SpringBootTest persistem).
        jdbcTemplate.update("""
                INSERT INTO empresas (id, name, company_id)
                SELECT 'empresa-2', 'Empresa 2', 'company-2'
                WHERE NOT EXISTS (SELECT 1 FROM empresas WHERE id = 'empresa-2')
                """);
        jdbcTemplate.update("""
                INSERT INTO simulations (id, empresa_id, name, description, created_at)
                SELECT 'sim-1', 'empresa-1', 'Test Simulation', 'Test simulation description', CURRENT_TIMESTAMP
                WHERE NOT EXISTS (SELECT 1 FROM simulations WHERE id = 'sim-1')
                """);
        jdbcTemplate.update("""
                INSERT INTO simulation_versions (id, simulation_id, version_number, status, root_node_id, created_at)
                SELECT ?, 'sim-1', 1, 'draft', 'node-1', CURRENT_TIMESTAMP
                WHERE NOT EXISTS (SELECT 1 FROM simulation_versions WHERE id = ?)
                """, ISOLATION_VERSION_ID, ISOLATION_VERSION_ID);

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
        long empresa1Count = repository.countByEmpresaIdAndSimulationVersionId("empresa-1", ISOLATION_VERSION_ID);
        long empresa2Count = repository.countByEmpresaIdAndSimulationVersionId("empresa-2", ISOLATION_VERSION_ID);

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
        entity.setSimulationVersionId(ISOLATION_VERSION_ID);
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
