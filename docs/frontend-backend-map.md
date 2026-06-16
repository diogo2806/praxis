# Mapa frontend x backend

## Estado atual

- Frontend: React + TanStack Router/Start em `frontend/src/routes`.
- Backend: Spring Boot em `backend/src/main/java/br/com/iforce/praxis`.
- Camada HTTP iniciada em `frontend/src/lib/api/praxis.ts`.
- Integracoes feitas:
  - Painel inicial em `/`, via listagem resumida de simulacoes.
  - Fluxo publico do candidato em `/candidato/:token`.
  - Diagnostico do validador em `/nova/validador?simulationId=ID&versionNumber=1`.
  - Monitoramento operacional em `/monitoramento?simulationId=ID&versionNumber=1`.
  - Governanca e AuditLog em `/governanca?simulationId=ID&versionNumber=1`.
  - Criacao de rascunho a partir do blueprint em `/nova/blueprint`.
  - Atualizacao estrutural do blueprint em `/nova/objetivo?simulationId=ID&versionNumber=1`.
  - Persistencia do personagem no primeiro no do grafo em `/nova/personagem?simulationId=ID&versionNumber=1`.
  - Edicao do grafo e alternativas em `/nova/dialogo?simulationId=ID&versionNumber=1`.
  - Piloto/calibracao em `/nova/piloto?simulationId=ID&versionNumber=1`, via monitoramento real.
  - Mapa e pesos de score em `/nova/mapa?simulationId=ID&versionNumber=1`, derivados da versao real.
  - Defensabilidade em `/defensabilidade?simulationId=ID&versionNumber=1`, com blueprint e AuditLog reais.
  - LGPD e explicabilidade em `/lgpd?simulationId=ID&versionNumber=1`, com grafo e AuditLog reais.
  - Transicoes do wizard de governanca em `/nova/governanca?simulationId=ID&versionNumber=1`.
  - Preflight Gupy em `/nova/gupy?simulationId=ID&versionNumber=1`.

## Rotas frontend

| Rota | Arquivo | Estado de integracao |
| --- | --- | --- |
| `/` | `frontend/src/routes/index.tsx` | Integrada com `GET /api/v1/simulations`; exibe totais, filtros, busca, status, maturidade, tentativas e link contextual para validar a versao. |
| `/candidato` | `frontend/src/routes/candidato.tsx` | Demo local preservada. |
| `/candidato/$token` | `frontend/src/routes/candidato.$token.tsx` | Integrada com `GET /candidate/attempts/{attemptId}` e `POST /candidate/attempts/{attemptId}/answers`. |
| `/monitoramento` | `frontend/src/routes/monitoramento.tsx` | Integrada com `GET /api/v1/simulations/{id}/versions/{n}/monitoring` quando a URL tem `simulationId` e `versionNumber`; tambem lista `/api/v1/gupy/result-deliveries`. Sem parametros, preserva demo local. |
| `/governanca` | `frontend/src/routes/governanca.tsx` | Parcialmente integrada: lista AuditLog real com `GET /api/v1/audit/simulations/{id}/versions/{n}` e cria rascunho via `POST /api/v1/simulations/{id}/versions/{n}/clone-draft` quando a URL tem `simulationId` e `versionNumber`; sem parametros, preserva demo local. |
| `/defensabilidade` | `frontend/src/routes/defensabilidade.tsx` | Parcialmente integrada: quando recebe `simulationId` e `versionNumber`, carrega `GET /api/v1/simulations/{id}/versions/{n}` e `GET /api/v1/audit/simulations/{id}/versions/{n}` para exibir blueprint, pesos e eventos; sem parametros, mostra seletor de versoes reais. |
| `/lgpd` | `frontend/src/routes/lgpd.tsx` | Parcialmente integrada: quando recebe `simulationId` e `versionNumber`, carrega `GET /api/v1/simulations/{id}/versions/{n}` e `GET /api/v1/audit/simulations/{id}/versions/{n}` para explicar turnos, alternativas, criticidade e evidencias; sem parametros, mostra seletor de versoes reais. |
| `/nova/blueprint` | `frontend/src/routes/nova.blueprint.tsx` | Integrada com `POST /api/v1/simulations/drafts`; cria simulacao + versao `draft` e navega com `simulationId`/`versionNumber`. |
| `/nova/objetivo` | `frontend/src/routes/nova.objetivo.tsx` | Parcialmente integrada: quando recebe `simulationId` e `versionNumber`, salva `rootNodeId` e pesos de competencias via `PATCH /api/v1/simulations/{id}/versions/{n}/blueprint`; sem parametros, preserva demo local. |
| `/nova/personagem` | `frontend/src/routes/nova.personagem.tsx` | Parcialmente integrada: quando recebe `simulationId` e `versionNumber`, carrega `GET /api/v1/simulations/{id}/versions/{n}` e persiste o personagem no no raiz com `POST /nodes` ou `PUT /nodes/{nodeId}`; sem parametros, preserva demo local. |
| `/nova/dialogo` | `frontend/src/routes/nova.dialogo.tsx` | Integrada com `GET /api/v1/simulations/{id}/versions/{n}` e CRUD de grafo: `POST/PUT/DELETE /nodes` e `POST/PUT/DELETE /nodes/{nodeId}/options`; sem parametros, mostra seletor de versoes reais para abrir no editor. |
| `/nova/validador` | `frontend/src/routes/nova.validador.tsx` | Parcialmente integrada: `issues` e estado de publicacao vem de `GET /api/v1/simulations/{id}/versions/{n}/validation` quando a URL tem `simulationId` e `versionNumber`; score/breakdown ainda local. |
| `/nova/piloto` | `frontend/src/routes/nova.piloto.tsx` | Integrada com `GET /api/v1/simulations/{id}/versions/{n}/monitoring`; sem parametros, mostra seletor de versoes reais. |
| `/nova/mapa` | `frontend/src/routes/nova.mapa.tsx` | Integrada com `GET /api/v1/simulations/{id}/versions/{n}`; renderiza nos, alternativas, destinos, criticidade e pesos reais do blueprint; sem parametros, mostra seletor de versoes reais. |
| `/nova/governanca` | `frontend/src/routes/nova.governanca.tsx` | Parcialmente integrada: lista AuditLog real e executa `submit-review`, `approve`, `reject` e `publish` com confirmação quando a URL tem `simulationId` e `versionNumber`; sem parametros, preserva demo local. |
| `/nova/gupy` | `frontend/src/routes/nova.gupy.tsx` | Parcialmente integrada: checklist de ativacao vem de `GET /api/v1/simulations/{id}/versions/{n}/gupy-preflight` quando a URL tem `simulationId` e `versionNumber`; sem parametros, preserva demo local. |

## APIs backend disponiveis

| Area | Endpoint base | Uso provavel no frontend |
| --- | --- | --- |
| Candidato | `/candidate/attempts` | Experiencia publica por token, envio de resposta e timeout. |
| Simulacoes admin | `/api/v1/simulations` | Criacao de rascunho, atualizacao de blueprint, listagem resumida, validacao, workflow de revisao, publicacao, preflight, monitoramento e arquivamento. |
| Auditoria | `/api/v1/audit` | Governanca, defensabilidade e trilha de decisoes. |
| Entregas Gupy | `/api/v1/gupy/result-deliveries` | Monitoramento operacional, retry e DLQ. |
| Integracao Gupy | `/test`, `/test/candidate`, `/test/result/{resultId}` | Contrato externo da Gupy; usar no frontend interno com cuidado por exigir token de integracao. |
| Enums | `/api/v1/enums` | Popular selects/status sem duplicar labels no frontend. |

## Proximas integracoes recomendadas

1. `/monitoramento`: filtrar a fila Gupy por simulacao/versao quando o backend expuser esse filtro. Hoje a listagem de entregas e global.
2. `/nova/validador`: mover score/breakdown para backend quando o contrato expuser pesos ou score detalhado.
3. `/nova/gupy`: ligar "Marcar integracao como ativa" quando existir endpoint backend para persistir esse estado.
4. `/lgpd`: adicionar bases legais, retencao e canal de revisao quando houver endpoint dedicado de privacidade.
