-- Adiciona campo 'active' para rastrear se uma opcao de configuracao esta ativa/inativa
-- Permite desativar competencias, niveis de senioridade, etc. sem apaga-los do banco
-- DEFAULT TRUE para manter compatibilidade com dados existentes

ALTER TABLE tenant_config_options ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
