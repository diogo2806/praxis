# Resumo de Implementação

> Estado técnico resumido do Práxis em 18/07/2026.

## Backend

- Spring Boot 3.5.3 com Java 21.
- PostgreSQL com Flyway.
- Segurança por JWT nas rotas internas quando `PRAXIS_SECURITY_ENABLED=true`.
- Empresa resolvida por contexto de segurança nas rotas internas.
- Integrações ATS autenticadas por Bearer token validado contra `integration_tokens` usando SHA-256 Base64URL.
- Outbox transacional com processamento em lote, retry, recuperação de `PROCESSING` órfão e DLQ.
- Auditoria por tentativa e por versão de avaliação.
- Upload de mídia por `/api/v1/media`.
- Configuração da empresa por `/api/v1/empresa-config`.
- Notificações internas por `GET /api/v1/notifications`.
- Reprocessamento operacional por `/api/v1/gupy/result-deliveries`.

Detalhes de retry, backoff e DLQ pertencem a [ARQUITETURA_OUTBOX_PATTERN.md](ARQUITETURA_OUTBOX_PATTERN.md).

## Frontend

- React 19 + TanStack Start/Router.
- Rotas em `frontend/src/routes`.
- `/login` usa `POST /api/v1/auth/login`.
- `/integrations` configura provedores e gera tokens de integração.
- `/integrations/gupy-homologacao` mostra prontidão, bloqueios e evidências.
- `/nova/gupy` executa preflight local da versão e lista entregas.
- `/notifications` lista alertas e permite reprocessar DLQ.
- `/monitoramento` consolida tentativas e entregas.
- Camada HTTP central em `frontend/src/lib/api/praxis.ts`.
- Runtime em `runtime-config.ts`, `runtime-config.server.ts` e `server.ts`.

## Integração Gupy

Implementado:

- `GET /test`;
- `POST /test/candidate`;
- `GET /test/result/{resultId}`;
- Bearer token individual por empresa;
- `company_id` e `document_id` como JSON `int64` positivo;
- `job_id`, `candidate_type` e `previous_result`;
- `callback_url` validada e redirecionamento final pelo navegador;
- idempotência canônica por empresa, candidato, avaliação e vaga;
- `test_url` com JWT `candidate_attempt`;
- `result_candidate_page_url` com JWT `candidate_result`;
- `result_webhook_url` com Outbox;
- consulta e webhook usando o mesmo DTO externo;
- resultado percentual por competência;
- estados externos `notStarted`, `paused` e `done`;
- rejeição explícita de `ABANDONED` e `EXPIRED`;
- proteção contra SSRF, isolamento por empresa e testes de contrato.

Estado atual:

- **compatibilidade técnica implementada**;
- **homologação formal pendente**;
- o fluxo ainda precisa ser executado com token, vaga, callback e webhook reais da Gupy.

Documentos responsáveis:

- [Fonte canônica da integração Gupy](GUPY-FONTE-CANONICA.md);
- [Contrato implementado](INTEGRACAO-GUPY-PROVEDOR.md);
- [Centro de homologação](HOMOLOGACAO-GUPY.md);
- [Arquitetura de Outbox](ARQUITETURA_OUTBOX_PATTERN.md).

## Configuração do Compose

O `docker-compose.yml` exige:

- `POSTGRES_USER`;
- `POSTGRES_PASSWORD`;
- `PRAXIS_JWT_SECRET`.

`PRAXIS_INTEGRATION_TOKEN` não é exigido pelo Compose e não autentica `/test/**`. Tokens de integração são gerados pela Central de Integrações e persistidos somente como hash.

## Verificação recomendada

Backend:

```bash
cd backend
mvn -B -ntp verify
```

Frontend:

```bash
cd frontend
npm ci
npm run build
```

Documentação:

```bash
python3 scripts/validate_docs.py
```

Última revisão: 18/07/2026.
