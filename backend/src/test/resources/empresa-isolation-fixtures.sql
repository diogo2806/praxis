-- Second empresa with its own published simulation, used to assert cross-empresa isolation.
-- empresa-2 carries an integration_token_hash matching the bearer token "empresa2-token".

DELETE FROM audit_events
WHERE aggregate_id IN ('sim-empresa2', 'sim-empresa2:v1');

DELETE FROM audit_events
WHERE empresa_id = 'empresa-2'
  AND aggregate_type = 'Integration'
  AND aggregate_id = 'GUPY';

DELETE FROM outbox_events
WHERE aggregate_type = 'CandidateAttempt'
  AND aggregate_id IN (
      SELECT id FROM candidate_attempts WHERE simulation_id = 'sim-empresa2'
  );

DELETE FROM candidate_attempts
WHERE simulation_id = 'sim-empresa2';

DELETE FROM simulations
WHERE id = 'sim-empresa2';

DELETE FROM integration_tokens
WHERE empresa_id = 'empresa-2';

DELETE FROM empresa_integrations
WHERE empresa_id = 'empresa-2';

DELETE FROM empresas
WHERE id = 'empresa-2';

INSERT INTO empresas (id, name, company_id, integration_token_hash)
VALUES ('empresa-2', 'Globex S.A.', '2', 'Xg4vISzoD08ZGOoujdIpczYOTp7_djZuo_y18yDRqRk');

INSERT INTO integration_tokens (empresa_id, provider, token_hash)
VALUES ('empresa-2', 'gupy', 'Xg4vISzoD08ZGOoujdIpczYOTp7_djZuo_y18yDRqRk');

INSERT INTO empresa_integrations (
    empresa_id,
    provider,
    type,
    status,
    credentials_hash,
    configured_at,
    created_at,
    updated_at
)
VALUES (
    'empresa-2',
    'GUPY',
    'ATS',
    'PENDENTE',
    'Xg4vISzoD08ZGOoujdIpczYOTp7_djZuo_y18yDRqRk',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO simulations (id, empresa_id, name, description, created_at)
VALUES (
    'sim-empresa2',
    'empresa-2',
    'Cenario Empresa 2',
    'Simulacao publicada pertencente exclusivamente ao empresa-2.',
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
    'sim-empresa2',
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
VALUES (9001, 9001, 'turno-1', 1, 'Cliente', 'Mensagem do cliente do empresa 2.', 45);

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
    (9001, 9001, 'opcao-a', 'Resposta A do empresa 2.', NULL, FALSE, 'Opcao valida.'),
    (9002, 9001, 'opcao-b', 'Resposta B do empresa 2.', NULL, FALSE, 'Opcao valida.');

INSERT INTO option_competency_scores (simulation_option_id, competency_name, score)
VALUES
    (9001, 'Empatia', 80),
    (9001, 'Resolucao', 70),
    (9002, 'Empatia', 75),
    (9002, 'Resolucao', 72);
