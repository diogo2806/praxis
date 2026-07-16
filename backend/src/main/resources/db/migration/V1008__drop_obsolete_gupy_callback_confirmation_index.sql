-- A confirmação servidor-servidor do callback_url foi desativada em V1005.
-- Remove o índice criado pela implementação antiga, inclusive em bancos que
-- executaram o mesmo conteúdo quando ele ainda estava versionado como V1001.
DROP INDEX IF EXISTS uq_outbox_gupy_callback_confirmation;
