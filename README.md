## Garantias de producao

- O score e deterministico e nao usa IA para julgar candidato.
- Toda simulacao pertence a um tenant.
- Endpoints da empresa filtram por tenant do JWT e exigem role `EMPRESA` quando a seguranca esta ativa.
- Endpoints Gupy resolvem tenant por token de integracao ou companyId.
- Cada simulacao possui no maximo uma versao PUBLISHED ativa.
- Tentativas sao pinadas por simulation_version_id para preservar historico.
- Respostas do candidato usam lock pessimista para evitar corrida.
- Entrega de webhook usa fila, claim transacional curto e HTTP fora de transacao.
- Webhooks outbound possuem allow-list, bloqueio de rede local, timeout, assinatura HMAC e idempotency key.
- Auditoria e append-only no PostgreSQL.
- Dados pessoais de tentativas sao anonymizados apos a retencao configurada.

## Perfis de acesso

- `EMPRESA`: usuario da empresa contratante. Acessa simulacoes, configuracoes do tenant, auditoria e monitoramento de entregas Gupy do proprio tenant.
- `GUPY`: perfil tecnico da integracao Gupy, aplicado por token Bearer de integracao nos endpoints `/test/**`.
- `ADMIN`: reservado para o futuro administrador global da plataforma, responsavel por gerenciar empresas/tenants contratantes. Ainda nao possui rotas dedicadas.

`PRAXIS_SECURITY_ENABLED=false` e configuracao do backend. Quando desativada, as rotas deixam de exigir JWT e usam o tenant padrao configurado em `PRAXIS_DEFAULT_TENANT_ID` (default: `tenant-1`).
