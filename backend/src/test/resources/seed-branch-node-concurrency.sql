DELETE FROM option_competency_scores
WHERE simulation_option_id IN (
    SELECT simulation_option.id
    FROM simulation_options simulation_option
    JOIN simulation_nodes simulation_node ON simulation_node.id = simulation_option.simulation_node_id
    JOIN simulation_versions simulation_version ON simulation_version.id = simulation_node.simulation_version_id
    WHERE simulation_version.simulation_id = 'sim-data17-concurrency'
);

DELETE FROM simulation_options
WHERE simulation_node_id IN (
    SELECT simulation_node.id
    FROM simulation_nodes simulation_node
    JOIN simulation_versions simulation_version ON simulation_version.id = simulation_node.simulation_version_id
    WHERE simulation_version.simulation_id = 'sim-data17-concurrency'
);

DELETE FROM simulation_nodes
WHERE simulation_version_id IN (
    SELECT id
    FROM simulation_versions
    WHERE simulation_id = 'sim-data17-concurrency'
);

DELETE FROM simulation_competencies
WHERE simulation_version_id IN (
    SELECT id
    FROM simulation_versions
    WHERE simulation_id = 'sim-data17-concurrency'
);

DELETE FROM simulation_versions
WHERE simulation_id = 'sim-data17-concurrency';

DELETE FROM simulations
WHERE id = 'sim-data17-concurrency';

INSERT INTO empresas (
    id,
    name,
    company_id,
    health_vertical,
    status,
    commercial_plan_type,
    created_at,
    updated_at
)
VALUES (
    'empresa-data17',
    'Empresa DATA17',
    'data17',
    FALSE,
    'EM_TESTE',
    'ENTERPRISE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO simulations (
    id,
    empresa_id,
    name,
    description,
    created_at
)
VALUES (
    'sim-data17-concurrency',
    'empresa-data17',
    'Simulação concorrente DATA17',
    'Fixture para validar a serialização da criação de etapas ramificadas.',
    CURRENT_TIMESTAMP
);

INSERT INTO simulation_versions (
    simulation_id,
    version_number,
    status,
    root_node_id,
    created_at
)
VALUES (
    'sim-data17-concurrency',
    1,
    'DRAFT',
    'turno-1',
    CURRENT_TIMESTAMP
);

INSERT INTO simulation_nodes (
    simulation_version_id,
    node_id,
    turn_index,
    speaker,
    message,
    position_x,
    position_y,
    is_final
)
SELECT
    simulation_version.id,
    'turno-1',
    1,
    'Cliente',
    'Como você responderia?',
    40.0,
    40.0,
    FALSE
FROM simulation_versions simulation_version
WHERE simulation_version.simulation_id = 'sim-data17-concurrency'
  AND simulation_version.version_number = 1;

INSERT INTO simulation_options (
    simulation_node_id,
    option_id,
    text,
    critical,
    audit_note
)
SELECT
    simulation_node.id,
    option_data.option_id,
    option_data.text,
    FALSE,
    ''
FROM simulation_nodes simulation_node
JOIN simulation_versions simulation_version ON simulation_version.id = simulation_node.simulation_version_id
CROSS JOIN (
    VALUES
        ('opcao-a', 'Alternativa A'),
        ('opcao-b', 'Alternativa B')
) AS option_data(option_id, text)
WHERE simulation_version.simulation_id = 'sim-data17-concurrency'
  AND simulation_node.node_id = 'turno-1';
