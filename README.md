## Garantias de producao

- O score e deterministico e nao usa IA para julgar candidato.
- Toda simulacao pertence a um tenant.
- Endpoints administrativos filtram por tenant do JWT.
- Endpoints Gupy resolvem tenant por token de integracao ou companyId.
- Cada simulacao possui no maximo uma versao PUBLISHED ativa.
- Tentativas sao pinadas por simulation_version_id para preservar historico.
- Respostas do candidato usam lock pessimista para evitar corrida.
- Entrega de webhook usa fila, claim transacional curto e HTTP fora de transacao.
- Webhooks outbound possuem allow-list, bloqueio de rede local, timeout, assinatura HMAC e idempotency key.
- Auditoria e append-only no PostgreSQL.
- Dados pessoais de tentativas sao anonymizados apos a retencao configurada.
