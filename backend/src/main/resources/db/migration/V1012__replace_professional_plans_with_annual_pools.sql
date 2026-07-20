-- O plano Profissional passa a ser comercializado somente em pacotes anuais explícitos.
-- Os planos legados permanecem no banco para preservar assinaturas existentes, mas não ficam
-- disponíveis para novas contratações.
UPDATE subscription_plans
SET active = FALSE
WHERE code IN (
    'PRO_30_MONTHLY',
    'PRO_60_MONTHLY',
    'PRO_100_MONTHLY',
    'PRO_30_ANNUAL',
    'PRO_60_ANNUAL',
    'PRO_100_ANNUAL'
);

INSERT INTO subscription_plans (
    code,
    name,
    plan_type,
    price_cents,
    currency,
    credit_amount,
    billing_interval_months,
    active,
    created_at
)
SELECT
    'PRO_ANNUAL_100',
    'Profissional anual 100 avaliações',
    'PROFISSIONAL',
    549000,
    'BRL',
    100,
    12,
    TRUE,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM subscription_plans WHERE code = 'PRO_ANNUAL_100'
);

INSERT INTO subscription_plans (
    code,
    name,
    plan_type,
    price_cents,
    currency,
    credit_amount,
    billing_interval_months,
    active,
    created_at
)
SELECT
    'PRO_ANNUAL_300',
    'Profissional anual 300 avaliações',
    'PROFISSIONAL',
    1497000,
    'BRL',
    300,
    12,
    TRUE,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM subscription_plans WHERE code = 'PRO_ANNUAL_300'
);

INSERT INTO subscription_plans (
    code,
    name,
    plan_type,
    price_cents,
    currency,
    credit_amount,
    billing_interval_months,
    active,
    created_at
)
SELECT
    'PRO_ANNUAL_1000',
    'Profissional anual 1.000 avaliações',
    'PROFISSIONAL',
    4490000,
    'BRL',
    1000,
    12,
    TRUE,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM subscription_plans WHERE code = 'PRO_ANNUAL_1000'
);

INSERT INTO subscription_plans (
    code,
    name,
    plan_type,
    price_cents,
    currency,
    credit_amount,
    billing_interval_months,
    active,
    created_at
)
SELECT
    'PRO_ANNUAL_3000',
    'Profissional anual 3.000 avaliações',
    'PROFISSIONAL',
    11970000,
    'BRL',
    3000,
    12,
    TRUE,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM subscription_plans WHERE code = 'PRO_ANNUAL_3000'
);

-- Garante os valores corretos caso a migração seja reaplicada em uma base preparada manualmente.
UPDATE subscription_plans
SET name = 'Profissional anual 100 avaliações',
    plan_type = 'PROFISSIONAL',
    price_cents = 549000,
    currency = 'BRL',
    credit_amount = 100,
    billing_interval_months = 12,
    active = TRUE
WHERE code = 'PRO_ANNUAL_100';

UPDATE subscription_plans
SET name = 'Profissional anual 300 avaliações',
    plan_type = 'PROFISSIONAL',
    price_cents = 1497000,
    currency = 'BRL',
    credit_amount = 300,
    billing_interval_months = 12,
    active = TRUE
WHERE code = 'PRO_ANNUAL_300';

UPDATE subscription_plans
SET name = 'Profissional anual 1.000 avaliações',
    plan_type = 'PROFISSIONAL',
    price_cents = 4490000,
    currency = 'BRL',
    credit_amount = 1000,
    billing_interval_months = 12,
    active = TRUE
WHERE code = 'PRO_ANNUAL_1000';

UPDATE subscription_plans
SET name = 'Profissional anual 3.000 avaliações',
    plan_type = 'PROFISSIONAL',
    price_cents = 11970000,
    currency = 'BRL',
    credit_amount = 3000,
    billing_interval_months = 12,
    active = TRUE
WHERE code = 'PRO_ANNUAL_3000';
