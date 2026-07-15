-- A tabela de auditoria é append-only em produção. O fixture precisa limpar os
-- registros determinísticos entre métodos de teste, então suspende somente os
-- triggers de usuário durante esta preparação transacional e os restaura logo
-- em seguida.
ALTER TABLE audit_events DISABLE TRIGGER USER;

DELETE FROM audit_events
WHERE aggregate_id IN ('sim-atendimento-caos', 'sim-atendimento-caos:v1', 'sim-timeout-fallback', 'sim-timeout-fallback:v1');

DELETE FROM audit_events
WHERE empresa_id = 'empresa-1'
  AND aggregate_type = 'Integration'
  AND aggregate_id = 'GUPY';

ALTER TABLE audit_events ENABLE TRIGGER USER;

DELETE FROM outbox_events
WHERE aggregate_type = 'CandidateAttempt'
  AND aggregate_id IN (
      SELECT id FROM candidate_attempts WHERE simulation_id IN ('sim-atendimento-caos', 'sim-timeout-fallback')
  );

DELETE FROM candidate_attempts
WHERE simulation_id IN ('sim-atendimento-caos', 'sim-timeout-fallback');

DELETE FROM simulations
WHERE id IN ('sim-atendimento-caos', 'sim-timeout-fallback');

INSERT INTO empresas (id, name, company_id, integration_token_hash)
SELECT 'empresa-1', 'Acme S.A.', '1', 'HQkNHnvADAg8tGffiSYY9Lx094NTkUZ9OLLSNKjSTGk'
WHERE NOT EXISTS (
    SELECT 1 FROM empresas WHERE id = 'empresa-1'
);

UPDATE empresas
SET company_id = '1',
    integration_token_hash = 'HQkNHnvADAg8tGffiSYY9Lx094NTkUZ9OLLSNKjSTGk'
WHERE id = 'empresa-1';

DELETE FROM integration_tokens
WHERE empresa_id = 'empresa-1'
  AND provider = 'gupy';

INSERT INTO integration_tokens (empresa_id, provider, token_hash)
VALUES ('empresa-1', 'gupy', 'HQkNHnvADAg8tGffiSYY9Lx094NTkUZ9OLLSNKjSTGk');

DELETE FROM empresa_integrations
WHERE empresa_id = 'empresa-1'
  AND provider = 'GUPY';

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
    'empresa-1',
    'GUPY',
    'ATS',
    'PENDENTE',
    'HQkNHnvADAg8tGffiSYY9Lx094NTkUZ9OLLSNKjSTGk',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO simulations (id, empresa_id, name, description, created_at)
VALUES (
    'sim-atendimento-caos',
    'empresa-1',
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

INSERT INTO simulation_competencies (simulation_version_id, name, weight, tier)
VALUES
    (1, 'Empatia', 0.4, 'MAJOR'),
    (1, 'Resolucao de conflito', 0.4, 'MAJOR'),
    (1, 'Aderencia a politica', 0.2, 'MINOR');

INSERT INTO simulation_nodes (
    id,
    simulation_version_id,
    node_id,
    turn_index,
    speaker,
    message,
    time_limit_seconds,
    timeout_next_node_id,
    is_final,
    report_text
)
VALUES
    (
        1,
        1,
        'turno-1',
        1,
        'Cliente',
        'Mensagem inicial do cliente para teste.',
        45,
        'encerramento',
        FALSE,
        NULL
    ),
    (
        2,
        1,
        'encerramento',
        2,
        'Sistema',
        'Encerramento da avaliacao.',
        NULL,
        NULL,
        TRUE,
        'Participacao encerrada. Resumo gerado para a equipe responsavel.'
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
        'encerramento',
        TRUE,
        'Exige revisao humana.'
    ),
    (
        2,
        1,
        'opcao-processo-frio',
        'Resposta orientada a processo.',
        'encerramento',
        FALSE,
        'Segue processo.'
    ),
    (
        3,
        1,
        'opcao-equilibrada',
        'Resposta equilibrada entre acolhimento e regra.',
        'encerramento',
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

INSERT INTO simulations (id, empresa_id, name, description, created_at)
VALUES (
    'sim-timeout-fallback',
    'empresa-1',
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
