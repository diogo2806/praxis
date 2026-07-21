-- Usuários antigos da empresa possuíam somente o papel genérico EMPRESA e, por isso,
-- exerciam funções administrativas. A partir dos subperfis, esse acesso passa a ser explícito.
INSERT INTO user_roles (user_id, role)
SELECT users.id, 'TEAM_MANAGER'
FROM users
WHERE EXISTS (
    SELECT 1
    FROM user_roles empresa_role
    WHERE empresa_role.user_id = users.id
      AND empresa_role.role = 'EMPRESA'
)
AND NOT EXISTS (
    SELECT 1
    FROM user_roles additional_role
    WHERE additional_role.user_id = users.id
      AND additional_role.role <> 'EMPRESA'
)
ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT users.id, 'PARTNER_MANAGER'
FROM users
WHERE EXISTS (
    SELECT 1
    FROM user_roles empresa_role
    WHERE empresa_role.user_id = users.id
      AND empresa_role.role = 'EMPRESA'
)
AND EXISTS (
    SELECT 1
    FROM user_roles team_manager
    WHERE team_manager.user_id = users.id
      AND team_manager.role = 'TEAM_MANAGER'
)
ON CONFLICT (user_id, role) DO NOTHING;
