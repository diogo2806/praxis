CREATE TABLE marketplace_platform_config (
    config_key VARCHAR(80) PRIMARY KEY,
    config_value VARCHAR(200) NOT NULL,
    description VARCHAR(300),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

INSERT INTO marketplace_platform_config (config_key, config_value, description)
SELECT 'commission_percent', '20', 'Percentual de comissao da plataforma no marketplace'
WHERE NOT EXISTS (
    SELECT 1 FROM marketplace_platform_config WHERE config_key = 'commission_percent'
);

INSERT INTO marketplace_platform_config (config_key, config_value, description)
SELECT 'escrow_days', '7', 'Dias de retencao administrativa antes de liberar repasse'
WHERE NOT EXISTS (
    SELECT 1 FROM marketplace_platform_config WHERE config_key = 'escrow_days'
);
