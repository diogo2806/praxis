# Resumo de Implementação

> Estado técnico resumido do Praxis em 12/07/2026.

## Backend

- Spring Boot 3.5.3 com Java 21.
- PostgreSQL com Flyway.
- Segurança por JWT nas rotas internas quando `PRAXIS_SECURITY_ENABLED=true`.
- Empresa resolvida por contexto de segurança nas rotas internas.
- Integração Gupy/Recrutei por Bearer token validado contra `integration_tokens` usando SHA-256 Base64URL.
- Outbox transacional com lotes de até 100 eventos, retry, recuperação de `PROCESSING` órfão e DLQ.
- HTTP 4xx permanente vai para DLQ; `408` e `429` são tratados como transitórios.
- Auditoria por tentativa e por versão de avaliação.
- Upload de mídia por `/api/v1/media`.
- Configuração da empresa por `/api/v1/empresa-config`.
- Notificações internas por `GET /api/v1/notifications`.
- Reprocessamento operacional por `/api/v1/gupy/result-deliveries`.

## Frontend

- React 19 + TanStack Start/Router.
- Rotas em `frontend/src/routes`.
- `/login` usa `POST /api/v1/auth/login`.
- `/integrations` configura provedores e gera tokens de integração.
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
- Bearer token por empresa;
- idempotência de tentativa;
- `test_url` para `/candidato/{token}`;
- `result_webhook_url` com outbox;
- resultado percentual por competência;
- retry e DLQ.

Ainda incompatível com o contrato oficial publicado:

- ausência de `callback_url`;
- ausência de callback GET e redirecionamento final;
- `GET /test/result/{resultId}` exige `company_id` extra;
- `company_id` e `document_id` usam `String`, não `int64`;
- `job_id` não é recebido;
- `previous_result` não segue o enum oficial;
- `result_candidate_page_url` aponta para API JSON;
- existem campos extras no `TestResult` que precisam ser validados.

Por isso, a integração deve ser descrita como **implementação técnica em preparação para homologação**, não como integração Gupy homologada.

Detalhes: [INTEGRACAO-GUPY-PROVEDOR.md](INTEGRACAO-GUPY-PROVEDOR.md).

## Configuração legada do Compose

`docker-compose.yml` ainda exige `PRAXIS_INTEGRATION_TOKEN`, mas `/test/**` não lê essa variável. A autenticação real usa tokens gerados na Central de Integrações e persistidos somente como hash em `integration_tokens`.

Não usar a variável legada como evidência de que a integração está configurada.

## Verificação recomendada

Backend:

```bash
cd backend
mvn test
```

Frontend:

```bash
cd frontend
pnpm build
```

Documentação:

```bash
rg -n "Gupy.*homologad|PRAXIS_INTEGRATION_TOKEN|callback_url|company_id" README.md docs
```
