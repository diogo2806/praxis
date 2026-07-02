UPDATE tenant_integrations
SET status = 'PENDENTE',
    updated_at = now()
WHERE status = 'CONECTADA'
  AND last_sync_at IS NULL;
