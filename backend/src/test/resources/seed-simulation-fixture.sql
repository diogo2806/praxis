DELETE FROM audit_events
WHERE aggregate_id IN ('sim-atendimento-caos', 'sim-atendimento-caos:v1', 'sim-timeout-fallback', 'sim-timeout-fallback:v1');

DELETE FROM outbox_events
WHERE aggregate_type = 'CandidateAttempt'
  AND aggregate_id IN (
      SELECT id FROM candidate_attempts WHERE simulation_id IN ('sim-atendimento-caos', 'sim-timeout-fallback')
  );

DELETE FROM candidate_attempts
WHERE simulation_id IN ('sim-atendimento-caos', 'sim-timeout-fallback');

DELETE FROM simulations
WHERE id IN ('sim-atendimento-caos', 'sim-timeout-fallback');

INSERT INTO tenants (id, name, company_id, integration_token_hash)
SELECT 'tenant-1', 'Acme S.A.', 'empresa-123', 'mIpDNk36Ser0rI9x2CntfUygEZ8TN-9xe3Ux_VOl6xE'
WHERE NOT EXISTS (
    SELECT 1 FROM tenants WHERE id = 'tenant-1'
);

UPDATE tenants
SET integration_token_hash = 'mIpDNk36Ser0rI9x2CntfUygEZ8TN-9xe3Ux_VOl6xE'
WHERE id = 'tenant-1';

DELETE FROM integration_tokens
WHERE tenant_id = 'tenant-1'
  AND provider = 'gupy';

INSERT INTO integration_tokens (tenant_id, provider, token_hash)
VALUES ('tenant-1', 'gupy', 'mIpDNk36Ser0rI9x2CntfUygEZ8TN-9xe3Ux_VOl6xE');

INSERT INTO simulations (id, tenant_id, name, description, created_at)
VALUES (
    'sim-atendimento-caos',
    'tenant-1',
    'Cenario Seed de Teste',
    'Avaliacao situacional deterministica para priorizacao, comunicacao e decisao em contexto.',
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

INSERT INTO simulations (id, tenant_id, name, description, created_at)
VALUES (
    'sim-timeout-fallback',
    'tenant-1',
    'Cenario Timeout com Fallback',
    'Avaliacao com dois turnos e rota de timeout obrigatoria.',
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
    2,
    'sim-timeout-fallback',
    1,
    'PUBLISHED',
    'turno-1',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO simulation_competencies (simulation_version_id, name, weight)
VALUES
    (2, 'Empatia', 1.0);

INSERT INTO simulation_nodes (
    id,
    simulation_version_id,
    node_id,
    turn_index,
    speaker,
    message,
    time_limit_seconds,
    timeout_next_node_id
)
VALUES
    (
        10,
        2,
        'turno-1',
        1,
        'Cliente',
        'Primeiro turno com fallback por timeout.',
        45,
        'turno-2'
    ),
    (
        11,
        2,
        'turno-2',
        2,
        'Cliente',
        'Segundo turno terminal.',
        45,
        NULL
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
        10,
        10,
        'n1-melhor',
        'Segue para o segundo turno.',
        'turno-2',
        FALSE,
        'Continua fluxo.'
    ),
    (
        11,
        10,
        'n1-fraca',
        'Tambem segue para o segundo turno.',
        'turno-2',
        FALSE,
        'Continua fluxo.'
    ),
    (
        12,
        11,
        'n2-melhor',
        'Resposta final adequada.',
        NULL,
        FALSE,
        'Finaliza bem.'
    ),
    (
        13,
        11,
        'n2-fraca',
        'Resposta final fraca.',
        NULL,
        FALSE,
        'Finaliza com baixa aderencia.'
    );

INSERT INTO option_competency_scores (simulation_option_id, competency_name, score)
VALUES
    (10, 'Empatia', 100),
    (11, 'Empatia', 20),
    (12, 'Empatia', 100),
    (13, 'Empatia', 30);
