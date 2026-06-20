# Resumo de Implementacao

> Estado tecnico resumido do Praxis em 20/06/2026.

## Backend

- Spring Boot 3.5.3 com Java 21.
- PostgreSQL com Flyway.
- Seguranca por JWT nas rotas internas quando `PRAXIS_SECURITY_ENABLED=true`.
- Tenant resolvido por contexto de seguranca nas rotas internas.
- Integracao Gupy por Bearer token validado contra `tenants.integration_token_hash`.
- Outbox transacional para entrega de resultados/eventos externos.
- Auditoria por tentativa e por versao de simulacao.
- Upload de midia por `/api/v1/media`.
- Tenant config por `/api/v1/tenant-config`.

## Frontend

- React 19 + TanStack Start/Router.
- Rotas em `frontend/src/routes`.
- Camada HTTP central em `frontend/src/lib/api/praxis.ts`.
- SSR/proxy em `frontend/src/server.ts`.
- Configuracao runtime em `runtime-config.ts` e `runtime-config.server.ts`.
- CSS global em `frontend/src/styles/app.css`.
- Design system exposto pelo `@theme inline` do Tailwind, com tokens OKLCH em `:root` e `.dark`.
- Paleta de marca "Confianca Serena": azul-petroleo em `--primary`, dourado de veredito em `--gold`, fundo off-white quente, neutros azulados e graficos derivados em `--chart-1` a `--chart-5`.

## Integracao Frontend/Backend

- As telas operacionais usam a camada HTTP em `frontend/src/lib/api/praxis.ts`.
- Catalogos de tenant sao carregados por `GET /api/v1/tenant-config`.
- A criacao de rascunho usa `POST /api/v1/simulations/drafts`.
- Edicao de fluxo, validacao, governanca, monitoramento, LGPD, envio de link, Talent Match e candidato consomem endpoints reais do backend.
- Nao ha fallback local de catalogo, sessao demo ou server function de exemplo no frontend.

## Integracao Gupy

- A integracao operacional em producao e explicitamente Gupy.
- Nao ha camada generica `ATSAdapter` nem registry de adapters.
- Os fluxos de catalogo, tentativa, resultado e entrega usam servicos concretos de Gupy.
- O contrato externo usa snake_case em `POST /test/candidate`.
- `GET /test` retorna `total_tests` e `payload`.
- `POST /test/candidate` retorna `test_url` e `test_result_id`.

## Lacunas conhecidas

Estas lacunas tambem aparecem no [Mapa Frontend-Backend](frontend-backend-map.md#lacunas-conhecidas-entre-backend-e-ui).

- `POST /api/v1/auth/login` existe, mas nao ha tela `/login`.
- `GET /api/v1/notifications` existe, mas nao ha helper/tela dedicada no frontend.
- Reprocessamento manual de DLQ existe no backend, mas a UI atual apenas lista entregas.
- `PRAXIS_INTEGRATION_TOKEN` e exigida no `docker-compose.yml`, mas a autenticacao Gupy real usa `tenants.integration_token_hash`.
- `test_url` da Gupy e montada como URL de API em `/candidate/attempts/{token}`; validar se a homologacao precisa de URL de browser.

## Verificacao recomendada

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

Documentacao:

```bash
rg -n "api key antiga|callback de retorno" README.md docs frontend/src/routes/README.md
```
