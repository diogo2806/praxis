DELETE FROM audit_events
WHERE aggregate_id IN ('sim-publish-gate:v1', 'sim-review-flow:v1');

DELETE FROM simulations
WHERE id IN ('sim-publish-gate', 'sim-review-flow');

INSERT INTO simulations (id, name, description, created_at)
VALUES
    ('sim-publish-gate', 'Publish Gate', 'Fixture para testar bloqueio de publicacao sem aprovacao.', CURRENT_TIMESTAMP),
    ('sim-review-flow', 'Review Flow', 'Fixture para testar fluxo de revisao e aprovacao.', CURRENT_TIMESTAMP);

INSERT INTO simulation_versions (
    id,
    simulation_id,
    version_number,
    status,
    root_node_id,
    published_at,
    created_at
)
VALUES
    (201, 'sim-publish-gate', 1, 'DRAFT', 'turno-1', NULL, CURRENT_TIMESTAMP),
    (202, 'sim-review-flow', 1, 'DRAFT', 'turno-1', NULL, CURRENT_TIMESTAMP);

INSERT INTO simulation_competencies (simulation_version_id, name, weight)
VALUES
    (201, 'Empatia', 0.5),
    (201, 'Resolucao', 0.5),
    (202, 'Empatia', 0.5),
    (202, 'Resolucao', 0.5);

INSERT INTO simulation_nodes (
    id,
    simulation_version_id,
    node_id,
    turn_index,
    speaker,
    message,
    time_limit_seconds
)
VALUES
    (201, 201, 'turno-1', 1, 'Cliente ficticio', 'Mensagem do cliente para teste.', 45),
    (202, 202, 'turno-1', 1, 'Cliente ficticio', 'Mensagem do cliente para teste.', 45);

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
    (201, 201, 'opcao-a', 'Resposta A.', NULL, FALSE, 'Opcao valida para teste.'),
    (202, 202, 'opcao-a', 'Resposta A.', NULL, FALSE, 'Opcao valida para teste.'),
    (203, 201, 'opcao-b', 'Resposta B.', NULL, FALSE, 'Opcao valida para teste.'),
    (204, 202, 'opcao-b', 'Resposta B.', NULL, FALSE, 'Opcao valida para teste.');

INSERT INTO option_competency_scores (simulation_option_id, competency_name, score)
VALUES
    (201, 'Empatia', 80),
    (201, 'Resolucao', 70),
    (202, 'Empatia', 80),
    (202, 'Resolucao', 70),
    (203, 'Empatia', 75),
    (203, 'Resolucao', 72),
    (204, 'Empatia', 75),
    (204, 'Resolucao', 72);
