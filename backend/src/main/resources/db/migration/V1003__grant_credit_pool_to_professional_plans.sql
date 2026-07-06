-- Planos PROFISSIONAL passam a conceder créditos de avaliação a cada ciclo pago:
-- mensais creditam a cota do mês; anuais creditam o pool do ano inteiro de uma vez.
UPDATE subscription_plans SET credit_amount = 30 WHERE code = 'PRO_30_MONTHLY';
UPDATE subscription_plans SET credit_amount = 60 WHERE code = 'PRO_60_MONTHLY';
UPDATE subscription_plans SET credit_amount = 100 WHERE code = 'PRO_100_MONTHLY';
UPDATE subscription_plans SET credit_amount = 360 WHERE code = 'PRO_30_ANNUAL';
UPDATE subscription_plans SET credit_amount = 720 WHERE code = 'PRO_60_ANNUAL';
UPDATE subscription_plans SET credit_amount = 1200 WHERE code = 'PRO_100_ANNUAL';
