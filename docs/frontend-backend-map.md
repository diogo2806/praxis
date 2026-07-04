# Mapa Frontend-Backend

> **Proposito:** mapear as rotas frontend, APIs backend e pontos de integracao atuais.

## Arquitetura atual

**Frontend**

- React 19 + TanStack Start/Router em `frontend/src/routes`.
- Camada HTTP central em `frontend/src/lib/api/praxis.ts`.
- Configuracao de runtime em `frontend/src/lib/runtime-config.ts` e `frontend/src/lib/runtime-config.server.ts`.
- CSS global em `frontend/src/styles/app.css`, importado por `frontend/src/routes/__root.tsx`.
- Sessao local em `frontend/src/lib/session.ts`; quando ha token JWT, ele e anexado nas chamadas autenticadas.

**Backend**

- Spring Boot 3.5 + Java 21 em `backend/src/main/java/br/com/iforce/praxis`.
- PostgreSQL + Flyway em `backend/src/main/resources/db/migration`.
- APIs internas em `/api/v1/**`.
- Experiencia publica do candidato em `/candidate/**` e redirecionamento `/candidato/{token}`.
- Contrato externo da Gupy em `/test/**`.

## Fluxos principais

### Criacao e publicacao

O assistente tem 4 passos canonicos (`frontend/src/lib/simulation-meta.ts`): `avaliacao -> personagem -> validador -> governanca`.

1. `/avaliacoes` lista simulacoes por `GET /api/v1/simulations` e permite arquivar sem apagar historico.
2. `/nova/avaliacao` (Teste) cria rascunho por `POST /api/v1/simulations/drafts` e ajusta o plano por `PATCH /blueprint`.
3. `/nova/personagem` (Cenario) carrega a versao e cria/edita nos, alternativas e midias.
4. `/nova/validador` (Revisao) consulta `GET /validation`.
5. `/nova/governanca` (Publicacao) publica por `POST /publish` ou clona por `POST /clone-draft`.

Alternativa por modelo: `/nova/rapido` usa `GET /api/v1/simulations/quick-start/templates` e `POST /api/v1/simulations/quick-start`, caindo no editor `/nova/mapa`. As rotas `/nova/objetivo`, `/nova/dialogo`, `/nova/mapa`, `/nova/piloto` e `/nova/gupy` continuam existindo como paginas standalone, fora dos 4 passos do assistente.

### Execucao do candidato

1. A tentativa nasce por `POST /test/candidate` (Gupy) ou `POST /api/v1/candidate-links` (empresa).
2. A UI publica usa `/candidato/$token`.
3. A API publica usa `GET /candidate/attempts/{attemptToken}` e `POST /candidate/attempts/{attemptToken}/answers`. O controller chama o parametro de `attemptId`, mas o valor esperado no fluxo publico e o token JWT da tentativa.

### Entrega Gupy

1. `/test` lista simulacoes publicadas.
2. `/test/candidate` cria tentativa e retorna `test_url` (pagina do candidato em `/candidato/{token}`) + `test_result_id`.
3. `/test/result/{resultId}` consulta resultado.
4. `result_webhook_url`, quando enviado, recebe resultado por outbox.

## Rotas frontend

O menu principal da empresa e definido em `frontend/src/components/app-shell.tsx`.

### Nucleo operacional

| Rota | Arquivo | Integracao atual |
| --- | --- | --- |
| `/` | `frontend/src/routes/index.tsx` | Landing/entrada inicial. |
| `/login` | `frontend/src/routes/login.tsx` | Login da empresa por `POST /api/v1/auth/login`. |
| `/dashboard` | `frontend/src/routes/dashboard.tsx` | Painel de indicadores por `GET /api/v1/dashboard`. |
| `/avaliacoes` | `frontend/src/routes/avaliacoes.tsx` | Ver e editar avaliacoes com `GET /api/v1/simulations`; arquiva por `POST /api/v1/simulations/{id}/archive` preservando historico. |
| `/results` | `frontend/src/routes/results.tsx` | Lista resultados por `GET /api/v1/results`. |
| `/results/$attemptId` | `frontend/src/routes/results.$attemptId.tsx` | Detalhe por `GET /api/v1/results/{attemptId}` e decisao por `POST /api/v1/results/{attemptId}/decision`. |
| `/enviar-link` | `frontend/src/routes/enviar-link.tsx` | Lista e cria links com `GET/POST /api/v1/candidate-links`; acompanha `GET /api/v1/candidate-links/live-attempts`. |
| `/monitoramento` | `frontend/src/routes/monitoramento.tsx` | Monitoramento da versao e entregas filtradas por simulacao/versao. |
| `/notifications` | `frontend/src/routes/notifications.tsx` | Alertas internos por `GET /api/v1/notifications`; lista DLQ e reprocessa por `POST /api/v1/gupy/result-deliveries/{deliveryId}/reprocess`. |
| `/talent-match` | `frontend/src/routes/talent-match.tsx` | Compara candidatos com `GET /api/v1/simulations/{id}/versions/{n}/talent-match?attemptIds=a,b`. |
| `/compliance` | `frontend/src/routes/compliance.tsx` | Analise operacional, defensabilidade e LGPD; usa `GET /api/v1/privacy/compliance`, versao e auditoria. Substitui as antigas `/defensabilidade` e `/lgpd`. |
| `/competencias` | `frontend/src/routes/competencias.tsx` | Catalogos da empresa via `GET/PUT /api/v1/empresa-config`. |
| `/configuracoes/perfil` | `frontend/src/routes/configuracoes.perfil.tsx` | Perfil da empresa por `GET /api/v1/company-profile`. |
| `/configuracoes/conta` | `frontend/src/routes/configuracoes.conta.tsx` | Conta do usuario por `GET /api/v1/account/me` e `POST /api/v1/account/password`. |
| `/team` | `frontend/src/routes/team.tsx` | Equipe da empresa via `/api/v1/team` (convite, bloqueio). |
| `/convite/$token` | `frontend/src/routes/convite.$token.tsx` | Aceite de convite de equipe por token. |

### Assistente de criacao (`/nova/**`)

| Rota | Arquivo | Integracao atual |
| --- | --- | --- |
| `/comecar` | `frontend/src/routes/comecar.tsx` | Entrada de landing/autenticacao. Com seguranca ativa redireciona para `/login`; em modo publico de teste redireciona para `/avaliacoes`. A criacao real segue por `/nova/avaliacao` ou `/nova/rapido`. |
| `/nova/avaliacao` | `frontend/src/routes/nova.avaliacao.tsx` | Passo 1 (Teste): cria rascunho com `POST /api/v1/simulations/drafts` e ajusta plano por `PATCH /blueprint`; usa catalogos de `GET /api/v1/empresa-config`. |
| `/nova/personagem` | `frontend/src/routes/nova.personagem.tsx` | Passo 2 (Cenario): carrega versao e cria/edita nos por `POST/PUT /nodes` e alternativas. |
| `/nova/validador` | `frontend/src/routes/nova.validador.tsx` | Passo 3 (Revisao): valida com `GET /api/v1/simulations/{id}/versions/{n}/validation`. |
| `/nova/governanca` | `frontend/src/routes/nova.governanca.tsx` | Passo 4 (Publicacao): auditoria por `GET /api/v1/audit/...`; clona e publica com `clone-draft` e `publish`. |
| `/nova/rapido` | `frontend/src/routes/nova.rapido.tsx` | Quick-start: `GET /api/v1/simulations/quick-start/templates` e `POST /api/v1/simulations/quick-start`; cai no editor `/nova/mapa`. |
| `/nova/objetivo` | `frontend/src/routes/nova.objetivo.tsx` | Standalone (fora dos 4 passos): plano por `PATCH /blueprint`. |
| `/nova/dialogo` | `frontend/src/routes/nova.dialogo.tsx` | Standalone: editor de grafo com CRUD de nos/alternativas; upload por `POST /api/v1/media`. |
| `/nova/mapa` | `frontend/src/routes/nova.mapa.tsx` | Standalone: exibe grafo, pesos e destino das alternativas a partir de `GET /api/v1/simulations/{id}/versions/{n}`. |
| `/nova/piloto` | `frontend/src/routes/nova.piloto.tsx` | Standalone: usa `GET /api/v1/simulations/{id}/versions/{n}/monitoring`. |
| `/nova/gupy` | `frontend/src/routes/nova.gupy.tsx` | Standalone: preflight com `GET /gupy-preflight`; entregas com `GET /api/v1/gupy/result-deliveries`. Nao existe endpoint separado de ativacao Gupy. |

### Integracoes

| Rota | Arquivo | Integracao atual |
| --- | --- | --- |
| `/integrations` | `frontend/src/routes/integrations.tsx` | Central de Integracoes (Gupy, Recrutei, API propria) por `GET /api/v1/integrations` e acoes (`configure`, `sync`, `tokens`...). |
| `/integrations/$provider` | `frontend/src/routes/integrations.$provider.tsx` | Detalhe/config do provedor (`gupy`, `recrutei`, `custom-api`). No `custom-api`, exibe webhook (`/api/v1/integrations/custom-api/webhook`) e token de API publica (`/api/v1/integrations/custom-api/api-token`). |
| `/docs/integracao-api-propria` | `frontend/src/routes/docs.integracao-api-propria.tsx` | Pagina de documentacao da integracao por API propria (HMAC, payload de webhook, endpoints). |

### Jornadas de avaliacao

| Rota | Arquivo | Integracao atual |
| --- | --- | --- |
| `/jornadas` | `frontend/src/routes/jornadas.tsx` | Lista/gestao de jornadas por `/api/v1/assessment-journeys`. |
| `/assessment-journeys/` e `/assessment-journeys/new` | `frontend/src/routes/assessment-journeys/*.tsx` | Autoria de jornada (encadeia avaliacoes publicadas). |
| `/jornada/$attemptId` | `frontend/src/routes/jornada.$attemptId.tsx` | Execucao publica da jornada por `/candidate/journey-attempts/{attemptId}`. |

### Plano e admin

| Rota | Arquivo | Integracao atual |
| --- | --- | --- |
| `/billing` | `frontend/src/routes/billing.tsx` | Plano, uso e historico por `GET /api/v1/billing` e `GET /api/v1/billing/events`. |
| `/admin` e filhos | `frontend/src/routes/admin*.tsx` | Painel ADMIN da plataforma por `/api/admin/**`. |

### Fluxo publico do candidato

| Rota | Arquivo | Integracao atual |
| --- | --- | --- |
| `/candidato` | `frontend/src/routes/candidato.tsx` | Entrada por codigo/link recebido. |
| `/candidato/$token` | `frontend/src/routes/candidato.$token.tsx` | Experiencia publica com `GET /candidate/attempts/{attemptToken}` e `POST /candidate/attempts/{attemptToken}/answers`. |

## APIs backend disponiveis

| Area | Endpoint base | Uso |
| --- | --- | --- |
| Auth | `/api/v1/auth/login` | API de login e emissao de JWT, usada pela rota frontend `/login`. |
| Dashboard | `/api/v1/dashboard` | Indicadores da empresa autenticada. |
| Simulacoes | `/api/v1/simulations` | Listagem, criacao, edicao de grafo, validacao, clone, publicacao, quick-start, preflight, monitoramento, Talent Match e arquivamento seguro por `/archive`. |
| Resultados | `/api/v1/results` | Lista e detalha resultados de tentativas; `POST /{attemptId}/decision` registra a decisao do recrutador. |
| Jornadas | `/api/v1/assessment-journeys`, `/api/v1/assessment-journey-attempts` | Autoria e execucao de jornadas que encadeiam avaliacoes publicadas. |
| Empresa config | `/api/v1/empresa-config` | Catalogos de competencias, senioridade, idiomas, usos de resultado e tempos de resposta. |
| Midia | `/api/v1/media` | Upload de imagem/audio para nos e alternativas. |
| Links de candidato | `/api/v1/candidate-links` | Geracao e listagem de links internos de candidato. |
| Candidato publico | `/candidate/attempts`, `/candidate/journey-attempts` | Estado da tentativa, envio de respostas e execucao publica de jornadas. |
| Integracoes | `/api/v1/integrations` | Central que unifica Gupy, Recrutei e API propria; inclui webhook e token de API publica em `/api/v1/integrations/custom-api/**`. |
| Auditoria | `/api/v1/audit` | Eventos por tentativa ou versao de simulacao. |
| Privacidade | `/api/v1/privacy/compliance` | Bases legais, retencao e politica de decisao automatizada. |
| Entregas Gupy | `/api/v1/gupy/result-deliveries` | Outbox de entrega, status, retry, DLQ e reprocessamento no backend. |
| Notificacoes | `/api/v1/notifications` | API para alertas internos, inclusive DLQ; a tela dedicada fica em `/notifications`. |
| Equipe/Conta | `/api/v1/team`, `/api/v1/account`, `/api/v1/company-profile`, `/api/v1/terms` | Usuarios da empresa, conta do usuario, perfil cadastral e aceite de termos. |
| Plano/cobranca | `/api/v1/billing` | Leitura de plano, uso e eventos; criacao de cobranca fica no painel ADMIN. |
| Admin plataforma | `/api/admin/**` | Dashboard, clientes (empresas), usuarios e auditoria da plataforma (perfil `ADMIN`). |
| Integracao Gupy | `/test`, `/test/candidate`, `/test/result/{resultId}` | Contrato externo consumido pela Gupy. |
| Webhook Mercado Pago | `/api/webhooks/mercado-pago` | Notificacoes de pagamento/assinatura com validacao de assinatura. |

## Seguranca

- `PRAXIS_SECURITY_ENABLED=false`: libera rotas e usa `PRAXIS_DEFAULT_EMPRESA_ID`.
- `PRAXIS_SECURITY_ENABLED=true`: exige JWT nas rotas internas e valida role `EMPRESA`.
- `/candidate/**`, `/candidato/**`, `/test/**`, `/api/v1/auth/login`, `/api/webhooks/**`, healthcheck e docs ficam permitidos pela configuracao Spring Security.
- A integracao Gupy/Recrutei valida Bearer token em `IntegrationAuthService` calculando o SHA-256 Base64URL do token e comparando com a tabela `integration_tokens` (por provider). A Recrutei e a API propria seguem o mesmo modelo de token por provedor.
- O painel `/api/admin/**` exige role `ADMIN`.

## Estados e entregas

- Versoes de simulacao: `draft`, `published`, `archived`.
- Tentativas: `notStarted`, `inProgress`, `paused`, `completed`, `abandoned`, `expired`, `failed`.
- Entregas de resultado: `pending`, `retrying`, `sent`, `dlq`.

Na API publica do candidato, alguns status sao traduzidos para portugues, por exemplo `nao_iniciada`, `em_andamento`, `concluida`, `abandonada`, `expirada` e `falhou`.

## Lacunas conhecidas entre backend e UI

- `/comecar` e uma entrada/redirecionamento, nao uma tela de autoria: com seguranca ativa leva para `/login`; em modo publico de teste leva para `/avaliacoes`.
- O menu lateral ainda pode destacar melhor `/notifications`, mas a rota e a tela dedicada ja existem.
- Billing segue parcialmente self-service: cliente consulta plano/uso/eventos, enquanto criacao/sincronizacao de cobranca fica no painel ADMIN/Mercado Pago.
- O endpoint legado `DELETE /api/v1/simulations/{id}` ainda existe para administracao tecnica, mas a jornada principal de RH usa arquivamento por `POST /api/v1/simulations/{id}/archive`.

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

Ultima revisao: 04/07/2026.
