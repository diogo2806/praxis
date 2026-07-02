-- As entidades JPA usam a nomenclatura "empresa" (tabela empresas, coluna empresa_id),
-- mas as migrations anteriores criaram tudo com a nomenclatura legada "tenant".
-- Esta migration alinha o schema ao código, evitando "relation does not exist" /
-- "column not found" em runtime.

ALTER TABLE tenants RENAME TO empresas;

ALTER TABLE users RENAME COLUMN tenant_id TO empresa_id;
ALTER TABLE simulations RENAME COLUMN tenant_id TO empresa_id;
ALTER TABLE candidate_attempts RENAME COLUMN tenant_id TO empresa_id;
ALTER TABLE audit_events RENAME COLUMN tenant_id TO empresa_id;
ALTER TABLE outbox_events RENAME COLUMN tenant_id TO empresa_id;
ALTER TABLE in_app_notifications RENAME COLUMN tenant_id TO empresa_id;
ALTER TABLE term_acceptances RENAME COLUMN tenant_id TO empresa_id;
ALTER TABLE assessment_journeys RENAME COLUMN tenant_id TO empresa_id;
ALTER TABLE assessment_journey_steps RENAME COLUMN tenant_id TO empresa_id;
ALTER TABLE assessment_journey_attempts RENAME COLUMN tenant_id TO empresa_id;
ALTER TABLE assessment_journey_attempt_steps RENAME COLUMN tenant_id TO empresa_id;

ALTER TABLE tenant_config_options RENAME TO empresa_config_options;
ALTER TABLE empresa_config_options RENAME COLUMN tenant_id TO empresa_id;

ALTER TABLE tenant_integrations RENAME TO empresa_integrations;
ALTER TABLE empresa_integrations RENAME COLUMN tenant_id TO empresa_id;

ALTER TABLE tenant_billing_events RENAME TO empresa_billing_events;
ALTER TABLE empresa_billing_events RENAME COLUMN tenant_id TO empresa_id;

ALTER TABLE tenant_credit_balances RENAME TO empresa_credit_balances;
ALTER TABLE empresa_credit_balances RENAME COLUMN tenant_id TO empresa_id;

ALTER TABLE tenant_credit_ledger RENAME TO empresa_credit_ledger;
ALTER TABLE empresa_credit_ledger RENAME COLUMN tenant_id TO empresa_id;

ALTER TABLE tenant_subscriptions RENAME TO empresa_subscriptions;
ALTER TABLE empresa_subscriptions RENAME COLUMN tenant_id TO empresa_id;

ALTER TABLE integration_tokens RENAME COLUMN tenant_id TO empresa_id;
