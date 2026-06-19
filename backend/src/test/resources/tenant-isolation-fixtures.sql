-- Second tenant with its own published simulation, used to assert cross-tenant isolation.
-- tenant-2 carries an integration_token_hash matching the bearer token "tenant2-token".

DELETE FROM audit_events
WHERE aggregate_id IN ('sim-tenant2', 'sim-tenant2:v1');

DELETE FROM outbox_events
WHERE aggregate_type = 'CandidateAttempt'
  AND aggregate_id IN (
      SELECT id FROM candidate_attempts WHERE simulation_id = 'sim-tenant2'
  );

DELETE FROM candidate_attempts
WHERE simulation_id = 'sim-tenant2';

DELETE FROM simulations
WHERE id = 'sim-tenant2';

DELETE FROM tenants
WHERE id = 'tenant-2';

INSERT INTO tenants (id, name, company_id, integration_token_hash)
VALUES ('tenant-2', 'Globex S.A.', 'empresa-456', 'gEsbpGwny-8MuW5S2dQT-oBgWd_PPiIrttUcKopCFQQ');

INSERT INTO simulations (id, tenant_id, name, description, created_at)
VALUES (
    'sim-tenant2',
    'tenant-2',
    'Cenario Tenant 2',
    'Simulacao publicada pertencente exclusivamente ao tenant-2.',
    CURRENT_TIMESTAMP
);

INSERT INTO simulation_versions (
    id,
    simulation_id,
    version_number,
    status,
    root_node_id,
    published_at,
    created_at
)
VALUES (
    9001,
    'sim-tenant2',
    1,
    'PUBLISHED',
    'turno-1',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO simulation_competencies (simulation_version_id, name, weight)
VALUES
    (9001, 'Empatia', 0.5),
    (9001, 'Resolucao', 0.5);

INSERT INTO simulation_nodes (
    id,
    simulation_version_id,
    node_id,
    turn_index,
    speaker,
    message,
    time_limit_seconds
)
VALUES (9001, 9001, 'turno-1', 1, 'Cliente', 'Mensagem do cliente do tenant 2.', 45);

INSERT INTO simulation_options (
    id,
    simulation_node_id,
    option_id,
    text,
    next_node_id,
    critical,
    audit_note
)
VALUES
    (9001, 9001, 'opcao-a', 'Resposta A do tenant 2.', NULL, FALSE, 'Opcao valida.'),
    (9002, 9001, 'opcao-b', 'Resposta B do tenant 2.', NULL, FALSE, 'Opcao valida.');

INSERT INTO option_competency_scores (simulation_option_id, competency_name, score)
VALUES
    (9001, 'Empatia', 80),
    (9001, 'Resolucao', 70),
    (9002, 'Empatia', 75),
    (9002, 'Resolucao', 72);
