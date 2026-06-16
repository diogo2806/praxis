-- Adds a second PUBLISHED version to sim-atendimento-caos to assert catalog de-duplication.
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
    9101,
    'sim-atendimento-caos',
    2,
    'PUBLISHED',
    'turno-1',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO simulation_competencies (simulation_version_id, name, weight)
VALUES
    (9101, 'Empatia', 0.5),
    (9101, 'Resolucao', 0.5);

INSERT INTO simulation_nodes (
    id,
    simulation_version_id,
    node_id,
    turn_index,
    speaker,
    message,
    time_limit_seconds
)
VALUES (9101, 9101, 'turno-1', 1, 'Cliente', 'Mensagem v2.', 45);

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
    (9101, 9101, 'opcao-a', 'Resposta A v2.', NULL, FALSE, 'Opcao valida.'),
    (9102, 9101, 'opcao-b', 'Resposta B v2.', NULL, FALSE, 'Opcao valida.');

INSERT INTO option_competency_scores (simulation_option_id, competency_name, score)
VALUES
    (9101, 'Empatia', 80),
    (9101, 'Resolucao', 70),
    (9102, 'Empatia', 75),
    (9102, 'Resolucao', 72);
