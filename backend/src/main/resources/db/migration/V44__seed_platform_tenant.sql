-- Empresa técnico reservado para a própria plataforma. Não representa um cliente real;
-- é o empresa ao qual os operadores ADMIN ficam vinculados (UserEntity.empresaId = 'PLATFORM').
-- Ações administrativas sem cliente alvo usam este empresa na trilha de auditoria.

INSERT INTO tenants (
    id, name, company_id, health_vertical,
    status, commercial_plan_type, created_at, updated_at
)
SELECT 'PLATFORM', 'Plataforma Praxis', 'PLATFORM', FALSE,
       'ATIVO', 'ENTERPRISE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM tenants WHERE id = 'PLATFORM'
);
