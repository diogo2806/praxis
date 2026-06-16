DELETE FROM audit_events
WHERE aggregate_id IN ('sim-atendimento-caos', 'sim-atendimento-caos:v1');

DELETE FROM result_deliveries
WHERE candidate_attempt_id IN (
    SELECT id FROM candidate_attempts WHERE simulation_id = 'sim-atendimento-caos'
);

DELETE FROM candidate_attempts
WHERE simulation_id = 'sim-atendimento-caos';

DELETE FROM simulations
WHERE id = 'sim-atendimento-caos';

INSERT INTO simulations (id, name, description, created_at, archived, deleted_at, deleted_by)
VALUES (
    'sim-atendimento-caos',
    'Cenario Seed de Teste',
    'Avaliacao situacional deterministica para priorizacao, comunicacao e decisao em contexto.',
    CURRENT_TIMESTAMP,
    FALSE,
    NULL,
    NULL
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
    1,
    'sim-atendimento-caos',
    1,
    'PUBLISHED',
    'turno-1',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO simulation_competencies (simulation_version_id, name, weight)
VALUES
    (1, 'Empatia', 0.4),
    (1, 'Resolucao de conflito', 0.4),
    (1, 'Aderencia a politica', 0.2);

INSERT INTO simulation_nodes (
    id,
    simulation_version_id,
    node_id,
    turn_index,
    speaker,
    message,
    time_limit_seconds
)
VALUES (
    1,
    1,
    'turno-1',
    1,
    'Cliente',
    'Mensagem inicial do cliente para teste.',
    45
);

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
    (
        1,
        1,
        'opcao-promete-estorno',
        'Resposta critica para validar revisao humana.',
        NULL,
        TRUE,
        'Exige revisao humana.'
    ),
    (
        2,
        1,
        'opcao-processo-frio',
        'Resposta orientada a processo.',
        NULL,
        FALSE,
        'Segue processo.'
    ),
    (
        3,
        1,
        'opcao-equilibrada',
        'Resposta equilibrada entre acolhimento e regra.',
        NULL,
        FALSE,
        'Equilibra acolhimento e limite de alcada.'
    );

INSERT INTO option_competency_scores (simulation_option_id, competency_name, score)
VALUES
    (1, 'Empatia', 82),
    (1, 'Resolucao de conflito', 42),
    (1, 'Aderencia a politica', 15),
    (2, 'Empatia', 48),
    (2, 'Resolucao de conflito', 70),
    (2, 'Aderencia a politica', 88),
    (3, 'Empatia', 86),
    (3, 'Resolucao de conflito', 78),
    (3, 'Aderencia a politica', 92);
