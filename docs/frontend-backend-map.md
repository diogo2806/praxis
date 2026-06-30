# Mapa Frontend-Backend

> **Proposito:** mapear as rotas frontend, APIs backend e pontos de integracao atuais.

## Arquitetura atual

**Frontend**

- React 19 + TanStack Start/Router em `frontend/src/routes`.
- Camada HTTP central em `frontend/src/lib/api/praxis.ts`.
- Configuracao de runtime em `frontend/src/lib/runtime-config.ts` e `frontend/src/lib/runtime-config.server.ts`.
- CSS global em `frontend/src/styles/app.css`, importado por `frontend/src/routes/__root.tsx`.
- Sessao local em `frontend/src/lib/session.ts`; o token JWT e anexado apenas nas rotas administrativas.

**Backend**

- Spring Boot 3.5 + Java 21 em `backend/src/main/java/br/com/iforce/praxis`.
- PostgreSQL + Flyway em `backend/src/main/resources/db/migration`.
- APIs internas em `/api/v1/**`.
- Experiencia publica do candidato em `/candidate/**` e redirecionamento `/candidato/{token}`.
- Contrato externo da Gupy em `/test/**`.

## Fluxos principais

### Criacao e publicacao

1. `/app` lista simulacoes por `GET /api/v1/simulations`.
2. `/nova/blueprint` cria rascunho por `POST /api/v1/simulations/drafts`.
3. `/nova/objetivo`, `/nova/personagem` e `/nova/dialogo` editam plano, nos, alternativas e midias.
4. `/nova/validador` consulta `GET /validation`.
5. `/nova/governanca` publica por `POST /publish` ou clona por `POST /clone-draft`.

### Execucao do candidato

1. A tentativa nasce por `POST /test/candidate` (Gupy) ou `POST /api/v1/candidate-links` (empresa).
2. A UI publica usa `/candidato/$token`.
3. A API publica usa `GET /candidate/attempts/{attemptToken}` e `POST /candidate/attempts/{attemptToken}/answers`. O controller chama o parametro de `attemptId`, mas o valor esperado no fluxo publico e o token JWT da tentativa.

### Entrega Gupy

1. `/test` lista simulacoes publicadas.
2. `/test/candidate` cria tentativa e retorna `test_url` + `test_result_id`.
3. `/test/result/{resultId}` consulta resultado.
4. `result_webhook_url`, quando enviado, recebe resultado por outbox.

## Rotas frontend

| Rota | Arquivo | Integracao atual |
| --- | --- | --- |
| `/` | `frontend/src/routes/index.tsx` | Landing/entrada inicial; o painel operacional e `/app`. |
| `/app` | `frontend/src/routes/app.tsx` | Painel principal com `GET /api/v1/simulations` e exclusao definitiva por `DELETE /api/v1/simulations/{id}`. |
| `/comecar` | `frontend/src/routes/comecar.tsx` | Inicio do fluxo de criacao. |
| `/nova/blueprint` | `frontend/src/routes/nova.blueprint.tsx` | Cria rascunho com `POST /api/v1/simulations/drafts`; usa catalogos de `GET /api/v1/empresa-config`. |
| `/nova/competencias` | `frontend/src/routes/nova.competencias.tsx` | Configuracao de competencias do empresa via `GET/PUT /api/v1/empresa-config`. |
| `/nova/objetivo` | `frontend/src/routes/nova.objetivo.tsx` | Atualiza plano com `PATCH /api/v1/simulations/{id}/versions/{n}/blueprint`. |
| `/nova/personagem` | `frontend/src/routes/nova.personagem.tsx` | Carrega versao e cria/atualiza o primeiro no por `POST/PUT /nodes`. |
| `/nova/dialogo` | `frontend/src/routes/nova.dialogo.tsx` | Editor de grafo com CRUD de nos e alternativas; upload por `POST /api/v1/media`. |
| `/nova/validador` | `frontend/src/routes/nova.validador.tsx` | Valida com `GET /api/v1/simulations/{id}/versions/{n}/validation`. |
| `/nova/piloto` | `frontend/src/routes/nova.piloto.tsx` | Usa `GET /api/v1/simulations/{id}/versions/{n}/monitoring`. |
| `/nova/mapa` | `frontend/src/routes/nova.mapa.tsx` | Exibe grafo, pesos e destino das alternativas a partir de `GET /api/v1/simulations/{id}/versions/{n}`. |
| `/nova/governanca` | `frontend/src/routes/nova.governanca.tsx` | Auditoria por `GET /api/v1/audit/simulations/{id}/versions/{n}`; clona e publica com `clone-draft` e `publish`. |
| `/nova/gupy` | `frontend/src/routes/nova.gupy.tsx` | Preflight com `GET /api/v1/simulations/{id}/versions/{n}/gupy-preflight`; entregas com `GET /api/v1/gupy/result-deliveries`. Nao existe endpoint separado de ativacao Gupy e a UI atual apenas lista entregas. |
| `/governanca` | `frontend/src/routes/governanca.tsx` | Lista simulacoes e permite acompanhar status `draft`, `published` e `archived`. |
| `/monitoramento` | `frontend/src/routes/monitoramento.tsx` | Usa monitoramento da versao e entregas Gupy filtradas por simulacao/versao. |
| `/defensabilidade` | `frontend/src/routes/defensabilidade.tsx` | Carrega versao e auditoria para explicar score e evidencias. |
| `/lgpd` | `frontend/src/routes/lgpd.tsx` | Usa `GET /api/v1/privacy/compliance`, versao e auditoria quando ha contexto. |
| `/compliance` | `frontend/src/routes/compliance.tsx` | Tela de analise operacional/compliance. |
| `/enviar-link` | `frontend/src/routes/enviar-link.tsx` | Lista e cria links com `GET/POST /api/v1/candidate-links`; acompanha `GET /api/v1/candidate-links/live-attempts`. |
| `/talent-match` | `frontend/src/routes/talent-match.tsx` | Compara candidatos com `GET /api/v1/simulations/{id}/versions/{n}/talent-match?attemptIds=a,b`. |
| `/candidato` | `frontend/src/routes/candidato.tsx` | Entrada por codigo/link recebido. |
| `/candidato/$token` | `frontend/src/routes/candidato.$token.tsx` | Experiencia publica com `GET /candidate/attempts/{attemptToken}` e `POST /candidate/attempts/{attemptToken}/answers`. |

## APIs backend disponiveis

| Area | Endpoint base | Uso |
| --- | --- | --- |
| Auth | `/api/v1/auth/login` | API de login e emissao de JWT. Nao ha rota `/login` dedicada no frontend atual. |
| Simulacoes | `/api/v1/simulations` | Listagem, criacao, edicao de grafo, validacao, clone, publicacao, preflight, monitoramento, Talent Match e exclusao. |
| Empresa config | `/api/v1/empresa-config` | Catalogos de competencias, senioridade, idiomas, usos de resultado e tempos de resposta. |
| Midia | `/api/v1/media` | Upload de imagem/audio para nos e alternativas. |
| Links de candidato | `/api/v1/candidate-links` | Geracao e listagem de links internos de candidato. |
| Candidato publico | `/candidate/attempts` | Estado da tentativa e envio de respostas. |
| Auditoria | `/api/v1/audit` | Eventos por tentativa ou versao de simulacao. |
| Privacidade | `/api/v1/privacy/compliance` | Bases legais, retencao e politica de decisao automatizada. |
| Entregas Gupy | `/api/v1/gupy/result-deliveries` | Outbox de entrega, status, retry, DLQ e reprocessamento no backend. A camada `praxis.ts` lista entregas; reprocessamento manual ainda nao tem helper/tela. |
| Notificacoes | `/api/v1/notifications` | API existe no backend para alertas internos, inclusive DLQ. O frontend ainda nao possui helper/tela dedicada. |
| Integracao Gupy | `/test`, `/test/candidate`, `/test/result/{resultId}` | Contrato externo consumido pela Gupy. |

## Seguranca

- `PRAXIS_SECURITY_ENABLED=false`: libera rotas e usa `PRAXIS_DEFAULT_EMPRESA_ID`.
- `PRAXIS_SECURITY_ENABLED=true`: exige JWT nas rotas internas e valida role `EMPRESA`.
- `/candidate/**`, `/candidato/**`, `/test/**`, `/api/v1/auth/login`, healthcheck e docs ficam permitidos pela configuracao Spring Security.
- A integracao Gupy valida Bearer token em `GupyAuthService` por hash salvo em `empresas.integration_token_hash`.

## Estados e entregas

- Versoes de simulacao: `draft`, `published`, `archived`.
- Tentativas: `notStarted`, `inProgress`, `paused`, `completed`, `abandoned`, `expired`, `failed`.
- Entregas de resultado: `pending`, `retrying`, `sent`, `dlq`.

Na API publica do candidato, alguns status sao traduzidos para portugues, por exemplo `nao_iniciada`, `em_andamento`, `concluida`, `abandonada`, `expirada` e `falhou`.

## Lacunas conhecidas entre backend e UI

- `POST /api/v1/auth/login` existe, mas nao ha rota `/login`.
- `GET /api/v1/notifications` existe, mas nao ha tela de notificacoes.
- Reprocessamento de DLQ existe no backend, mas a UI atual apenas lista entregas.
- `DELETE /api/v1/simulations/{id}` remove definitivamente; nao e arquivamento/soft delete.
- O `test_url` retornado por `POST /test/candidate` vem do backend como URL de API em `/candidate/attempts/{token}`; validar expectativa de browser na homologacao Gupy.

## Padroes de CSS e rotas

- Todo CSS global fica em `frontend/src/styles/app.css`.
- Rotas nao devem importar CSS proprio; use componentes compartilhados e classes do design system existente.
- `frontend/src/routeTree.gen.ts` e gerado automaticamente e nao deve ser editado manualmente.

## Design system e tema

- A fonte de verdade dos tokens visuais e `frontend/src/styles/app.css`, nos blocos `:root`, `.dark` e `@theme inline`.
- As cores usam OKLCH para manter previsibilidade de contraste e derivacao de tons.
- A paleta principal do Praxis e "Confianca Serena": azul-petroleo como primario, dourado de veredito em `--gold`, neutros azulados e fundo off-white quente.
- `--gold` esta registrado como `--color-gold` e deve ser usado com parcimonia para decisoes, score ou selo de recomendacao; acoes principais continuam em `--primary`.
- `--chart-1` a `--chart-5` derivam da marca: petroleo, teal, dourado, verde e ardosia. Evite recolocar a sequencia padrao do shadcn.
- Estados semanticos usam `--success`, `--warning`, `--danger` e `--destructive`; nao codifique cores fixas nas rotas.

Ultima revisao: 20/06/2026.
