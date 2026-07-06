ALTER TABLE subscription_plans ADD COLUMN IF NOT EXISTS billing_interval_months INTEGER NOT NULL DEFAULT 1;

INSERT INTO subscription_plans (code, name, plan_type, price_cents, currency, credit_amount, billing_interval_months, active, created_at)
SELECT 'AVULSO_1', 'Avulso 1 avaliação', 'AVULSO', 6990, 'BRL', 1, 1, TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM subscription_plans WHERE code = 'AVULSO_1');

INSERT INTO subscription_plans (code, name, plan_type, price_cents, currency, credit_amount, billing_interval_months, active, created_at)
SELECT 'AVULSO_4', 'Avulso 4 avaliações', 'AVULSO', 27960, 'BRL', 4, 1, TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM subscription_plans WHERE code = 'AVULSO_4');

INSERT INTO subscription_plans (code, name, plan_type, price_cents, currency, credit_amount, billing_interval_months, active, created_at)
SELECT 'AVULSO_8', 'Avulso 8 avaliações', 'AVULSO', 55920, 'BRL', 8, 1, TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM subscription_plans WHERE code = 'AVULSO_8');

INSERT INTO subscription_plans (code, name, plan_type, price_cents, currency, credit_amount, billing_interval_months, active, created_at)
SELECT 'AVULSO_20', 'Avulso 20 avaliações', 'AVULSO', 139800, 'BRL', 20, 1, TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM subscription_plans WHERE code = 'AVULSO_20');

INSERT INTO subscription_plans (code, name, plan_type, price_cents, currency, credit_amount, billing_interval_months, active, created_at)
SELECT 'PRO_30_MONTHLY', 'Profissional 30 avaliações/mês', 'PROFISSIONAL', 164700, 'BRL', NULL, 1, TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM subscription_plans WHERE code = 'PRO_30_MONTHLY');

INSERT INTO subscription_plans (code, name, plan_type, price_cents, currency, credit_amount, billing_interval_months, active, created_at)
SELECT 'PRO_60_MONTHLY', 'Profissional 60 avaliações/mês', 'PROFISSIONAL', 299400, 'BRL', NULL, 1, TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM subscription_plans WHERE code = 'PRO_60_MONTHLY');

INSERT INTO subscription_plans (code, name, plan_type, price_cents, currency, credit_amount, billing_interval_months, active, created_at)
SELECT 'PRO_100_MONTHLY', 'Profissional 100 avaliações/mês', 'PROFISSIONAL', 449000, 'BRL', NULL, 1, TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM subscription_plans WHERE code = 'PRO_100_MONTHLY');

INSERT INTO subscription_plans (code, name, plan_type, price_cents, currency, credit_amount, billing_interval_months, active, created_at)
SELECT 'PRO_30_ANNUAL', 'Profissional anual 30 avaliações/mês', 'PROFISSIONAL', 1778760, 'BRL', NULL, 12, TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM subscription_plans WHERE code = 'PRO_30_ANNUAL');

INSERT INTO subscription_plans (code, name, plan_type, price_cents, currency, credit_amount, billing_interval_months, active, created_at)
SELECT 'PRO_60_ANNUAL', 'Profissional anual 60 avaliações/mês', 'PROFISSIONAL', 3053880, 'BRL', NULL, 12, TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM subscription_plans WHERE code = 'PRO_60_ANNUAL');

INSERT INTO subscription_plans (code, name, plan_type, price_cents, currency, credit_amount, billing_interval_months, active, created_at)
SELECT 'PRO_100_ANNUAL', 'Profissional anual 100 avaliações/mês', 'PROFISSIONAL', 4310400, 'BRL', NULL, 12, TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM subscription_plans WHERE code = 'PRO_100_ANNUAL');