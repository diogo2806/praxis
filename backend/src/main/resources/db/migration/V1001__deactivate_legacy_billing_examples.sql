UPDATE subscription_plans
SET active = FALSE
WHERE code IN ('AVULSO_100', 'PRO_MENSAL');
