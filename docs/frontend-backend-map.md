# Mapa frontend x backend

## Estado atual

- Frontend: React + TanStack Router/Start em `frontend/src/routes`.
- Backend: Spring Boot em `backend/src/main/java/br/com/iforce/praxis`.
- Camada HTTP iniciada em `frontend/src/lib/api/praxis.ts`.
- Integracoes feitas:
  - Painel inicial em `/`, via listagem resumida de simulacoes.
  - Fluxo publico do candidato em `/candidato/:token`.
  - Diagnostico do validador em `/nova/validador?simulationId=ID&versionNumber=1`, incluindo score e contadores do backend.
  - Monitoramento operacional em `/monitoramento?simulationId=ID&versionNumber=1`, incluindo entregas Gupy filtradas por versao.
  - Governanca e AuditLog em `/governanca?simulationId=ID&versionNumber=1`.
  - Criacao de rascunho a partir do blueprint em `/nova/blueprint`.
  - Atualizacao estrutural do blueprint em `/nova/objetivo?simulationId=ID&versionNumber=1`.
  - Persistencia do personagem no primeiro no do grafo em `/nova/personagem?simulationId=ID&versionNumber=1`.
  - Edicao do grafo e alternativas em `/nova/dialogo?simulationId=ID&versionNumber=1`.
  - Piloto/calibracao em `/nova/piloto?simulationId=ID&versionNumber=1`, via monitoramento real.
  - Mapa e pesos de score em `/nova/mapa?simulationId=ID&versionNumber=1`, derivados da versao real.
  - Defensabilidade em `/defensabilidade?simulationId=ID&versionNumber=1`, com blueprint e AuditLog reais.
  - LGPD e explicabilidade em `/lgpd?simulationId=ID&versionNumber=1`, com grafo, AuditLog e politica de privacidade reais.
  - Transicoes do wizard de governanca em `/nova/governanca?simulationId=ID&versionNumber=1`.
  - Preflight e ativacao Gupy em `/nova/gupy?simulationId=ID&versionNumber=1`.

## Rotas frontend

| Rota | Arquivo | Estado de integracao |
| --- | --- | --- |
| `/` | `frontend/src/routes/index.tsx` | Integrada com `GET /api/v1/simulations`; exibe totais, filtros, busca, status, maturidade, tentativas e link contextual para validar a versao. |
| `/candidato` | `frontend/src/routes/candidato.tsx` | Demo local preservada. |
| `/candidato/$token` | `frontend/src/routes/candidato.$token.tsx` | Integrada com `GET /candidate/attempts/{attemptId}` e `POST /candidate/attempts/{attemptId}/answers`. |
| `/monitoramento` | `frontend/src/routes/monitoramento.tsx` | Integrada com `GET /api/v1/simulations/{id}/versions/{n}/monitoring` e `GET /api/v1/gupy/result-deliveries?simulationId={id}&versionNumber={n}` quando a URL tem `simulationId` e `versionNumber`; sem parametros, mostra seletor de versoes reais. |
| `/governanca` | `frontend/src/routes/governanca.tsx` | Integrada para contexto real: lista AuditLog com `GET /api/v1/audit/simulations/{id}/versions/{n}` e cria rascunho via `POST /api/v1/simulations/{id}/versions/{n}/clone-draft` quando a URL tem `simulationId` e `versionNumber`; sem parametros, preserva demo local. |
| `/defensabilidade` | `frontend/src/routes/defensabilidade.tsx` | Integrada para contexto real: quando recebe `simulationId` e `versionNumber`, carrega `GET /api/v1/simulations/{id}/versions/{n}` e `GET /api/v1/audit/simulations/{id}/versions/{n}` para exibir blueprint, pesos e eventos; sem parametros, mostra seletor de versoes reais. |
| `/lgpd` | `frontend/src/routes/lgpd.tsx` | Integrada com `GET /api/v1/privacy/compliance`; quando recebe `simulationId` e `versionNumber`, tambem carrega `GET /api/v1/simulations/{id}/versions/{n}` e `GET /api/v1/audit/simulations/{id}/versions/{n}` para explicar turnos, alternativas, criticidade e evidencias; sem parametros, mostra seletor de versoes reais. |
| `/nova/blueprint` | `frontend/src/routes/nova.blueprint.tsx` | Integrada com `POST /api/v1/simulations/drafts`; cria simulacao + versao `draft` e navega com `simulationId`/`versionNumber`. |
| `/nova/objetivo` | `frontend/src/routes/nova.objetivo.tsx` | Integrada para rascunho real: quando recebe `simulationId` e `versionNumber`, salva `rootNodeId` e pesos de competencias via `PATCH /api/v1/simulations/{id}/versions/{n}/blueprint`; sem parametros, preserva demo local. |
| `/nova/personagem` | `frontend/src/routes/nova.personagem.tsx` | Integrada para rascunho real: quando recebe `simulationId` e `versionNumber`, carrega `GET /api/v1/simulations/{id}/versions/{n}` e persiste o personagem no no raiz com `POST /nodes` ou `PUT /nodes/{nodeId}`; sem parametros, preserva demo local. |
| `/nova/dialogo` | `frontend/src/routes/nova.dialogo.tsx` | Integrada com `GET /api/v1/simulations/{id}/versions/{n}` e CRUD de grafo: `POST/PUT/DELETE /nodes` e `POST/PUT/DELETE /nodes/{nodeId}/options`; sem parametros, mostra seletor de versoes reais para abrir no editor. |
| `/nova/validador` | `frontend/src/routes/nova.validador.tsx` | Integrada com `GET /api/v1/simulations/{id}/versions/{n}/validation`; consome `issues`, `publishable`, `blockerCount`, `warningCount` e `qualityScore` do backend quando a URL tem `simulationId` e `versionNumber`; sem parametros, mostra seletor de versoes reais. |
| `/nova/piloto` | `frontend/src/routes/nova.piloto.tsx` | Integrada com `GET /api/v1/simulations/{id}/versions/{n}/monitoring`; sem parametros, mostra seletor de versoes reais. |
| `/nova/mapa` | `frontend/src/routes/nova.mapa.tsx` | Integrada com `GET /api/v1/simulations/{id}/versions/{n}`; renderiza nos, alternativas, destinos, criticidade e pesos reais do blueprint; sem parametros, mostra seletor de versoes reais. |
| `/nova/governanca` | `frontend/src/routes/nova.governanca.tsx` | Integrada para contexto real: lista AuditLog e executa `submit-review`, `approve`, `reject` e `publish` com confirmação quando a URL tem `simulationId` e `versionNumber`; sem parametros, preserva demo local. |
| `/nova/gupy` | `frontend/src/routes/nova.gupy.tsx` | Integrada com `GET /api/v1/simulations/{id}/versions/{n}/gupy-preflight`, `POST /api/v1/simulations/{id}/versions/{n}/gupy-activation` e `GET /api/v1/gupy/result-deliveries?simulationId={id}&versionNumber={n}` quando a URL tem `simulationId` e `versionNumber`; sem parametros, mostra seletor de versoes reais. |

## APIs backend disponiveis

| Area | Endpoint base | Uso provavel no frontend |
| --- | --- | --- |
| Candidato | `/candidate/attempts` | Experiencia publica por token, envio de resposta e timeout. |
| Simulacoes da empresa | `/api/v1/simulations` | Criacao de rascunho, atualizacao de blueprint, listagem resumida, validacao, workflow de revisao, publicacao, preflight, monitoramento e arquivamento. Exige role `EMPRESA` quando a seguranca esta ativa. |
| Auditoria da empresa | `/api/v1/audit` | Governanca, defensabilidade e trilha de decisoes. Exige role `EMPRESA` quando a seguranca esta ativa. |
| Entregas Gupy da empresa | `/api/v1/gupy/result-deliveries` | Monitoramento operacional, retry e DLQ. Exige role `EMPRESA` quando a seguranca esta ativa. |
| Integracao Gupy | `/test`, `/test/candidate`, `/test/result/{resultId}?company_id={companyId}` | Contrato externo da Gupy; usar no frontend interno com cuidado por exigir token de integracao. |
| Enums | `/api/v1/enums` | Popular selects/status sem duplicar labels no frontend. |
| Privacidade | `/api/v1/privacy/compliance` | Bases legais, retencao, canal de revisao e regra de decisao automatizada para LGPD. |

## Seguranca e perfis

- `EMPRESA`: usuario da empresa contratante. Acessa as rotas operacionais do proprio tenant: simulacoes, tenant-config, auditoria e entregas Gupy.
- `GUPY`: perfil tecnico da integracao externa. Acessa `/test/**` via token Bearer de integracao.
- `ADMIN`: reservado para administracao global futura da plataforma, para gerenciar empresas/tenants contratantes. Ainda nao possui rotas dedicadas.

`PRAXIS_SECURITY_ENABLED=false` e variavel do backend. Quando desligada, o backend libera as rotas sem JWT e usa `PRAXIS_DEFAULT_TENANT_ID` como tenant padrao.

## Proximas integracoes recomendadas

1. Refinar filtros por empresa/tenant quando autenticacao e multi-tenant forem implementados.
2. Trocar dados estaticos de privacidade por configuracao administravel quando houver tela de administracao.
