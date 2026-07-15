# Requisitos técnicos implementados — praxis

Status: atualizado em 2026-07-15 após entregas de `CFG10`, `INT1` e `INT2`.

Este arquivo registra somente comportamentos comprovadamente entregues no código e no fluxo real. Entregas parciais são descritas como parciais e apontam para os IDs que mantêm as lacunas remanescentes no backlog canônico.

## 2026-07-15

| Origem | Situação registrada | Entrega comprovada | Pendência remanescente |
|---|---|---|---|
| `CFG10` | Concluído | O Docker Compose não exige mais `PRAXIS_INTEGRATION_TOKEN`; o CI valida a configuração sem credencial global; Gupy e Recrutei continuam autenticando somente pelos tokens persistidos por empresa e provedor. | Nenhuma. |
| `INT1` | Concluído | `company_id` e `document_id` do `POST /test/candidate` são recebidos como inteiros JSON `int64` positivos, normalizados para identidade decimal canônica, validados contra o token e usados de forma estável na idempotência. | Nenhuma para o contrato de entrada; a homologação real continua necessária. |
| `INT2` | Concluído | `candidate_type` aceita somente `internal`/`external`; `previous_result` aceita `fail`, ausência e formas de `null` previstas pela documentação; `none` e valores desconhecidos são rejeitados antes do caso de uso. | Nenhuma para os enums de entrada. |
| `REQ-INTEGRACOES-REATIVACAO-TOKEN-ATS` | Concluído | Reativação de Gupy/Recrutei rotaciona a credencial, retorna o novo token uma única vez, persiste somente hash/prévia, muda o estado para `PENDENTE` e limpa a atividade da credencial anterior. | O ciclo geral de rotação/revogação ainda possui fluxos concorrentes; registrado em `SEC10`. |
| `REQ-INTEGRACOES-STATUS-CONEXAO-REAL` | Entrega parcial consolidada | Atualização manual de status deixou de promover conexão; conexões sem `lastSyncAt` são normalizadas para `PENDENTE`; a interface diferencia token configurado de conexão comprovada. | Registro completo da atividade externa, auditoria e proteção de `DESATIVADA` permanecem em `INT10`; consistência de credenciais permanece em `SEC10`. |

### CFG10 — runtime sem credencial global legada

| Caminho completo | Método/campo/contrato | Comportamento comprovado |
|---|---|---|
| `docker-compose.yml` | ambiente do backend | Removeu a expansão obrigatória de `PRAXIS_INTEGRATION_TOKEN`; a inicialização continua exigindo apenas as configurações efetivamente consumidas. |
| `.github/workflows/ci.yml` | job `compose-config` | Executa `docker compose config --quiet` sem definir a credencial global, impedindo a reintrodução da exigência legada. |
| `README.md` e `docs/IMPLANTACAO.md` | configuração local e implantação | Documentam o modelo real de token vinculado à empresa e ao provedor na tabela `integration_tokens`. |

### INT1 — identificadores oficiais e idempotência estável

| Caminho completo | Método/campo/contrato | Comportamento comprovado |
|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/gupy/dto/CreateCandidateRequest.java` | `company_id` e `document_id` | O limite externo usa `Long`, exige presença e valor positivo e rejeita tokens JSON que não sejam inteiros ou que excedam a faixa `int64`. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/dto/CreateCandidateRequest.java` | `companyId()` e `documentId()` | Converte os identificadores uma única vez com `Long.toString`, produzindo representação decimal canônica para pertencimento, persistência e chave idempotente. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptService.java` | `assertCompanyMatchesToken()` e `createOrReuse()` | O fluxo existente continua comparando o identificador canônico com o `company_id` resolvido pelo Bearer token e compõe a chave com empresa, companhia, documento, teste e vaga opcional. |
| `backend/src/main/java/br/com/iforce/praxis/recrutei/controller/RecruteiIntegrationController.java` | criação do comando compartilhado | O construtor interno textual foi preservado para o contrato próprio da Recrutei sem participar da desserialização pública da Gupy. |

### INT2 — enums oficiais no limite de entrada

| Caminho completo | Método/campo/contrato | Comportamento comprovado |
|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/gupy/dto/CreateCandidateRequest.java` | `CandidateType` | Aceita somente `internal` e `external`; ausência e `null` permanecem opcionais. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/dto/CreateCandidateRequest.java` | `PreviousResult` | Aceita `fail`, ausência, `null` JSON e o texto `null` publicado no exemplo oficial; rejeita `none`, `pass` e valores desconhecidos. |
| `backend/src/test/java/br/com/iforce/praxis/gupy/controller/GupyIntegrationControllerTest.java` | testes de contrato | Cobre tipos oficiais, faixa positiva, pertencimento ao token, idempotência, diferenciação por vaga, opcionais nulos e rejeição dos valores fora do contrato. |
| `docs/INTEGRACAO-GUPY-PROVEDOR.md` | contrato de `POST /test/candidate` | Registra os tipos e enums efetivamente aceitos e remove essas divergências dos bloqueadores de homologação. |

### REQ-INTEGRACOES-REATIVACAO-TOKEN-ATS — reativação segura entregue

| Caminho completo | Método/campo/contrato | Comportamento comprovado |
|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/shared/integration/IntegrationManagementService.java` | `reactivate()` | Exige estado `DESATIVADA`, rotaciona o token ATS, persiste hash e prévia, define `PENDENTE`, limpa `disabledAt`, `lastSyncAt` e erro, registra auditoria e inclui o token completo somente na resposta da operação. |
| `backend/src/main/java/br/com/iforce/praxis/shared/integration/IntegrationTokenAdminService.java` | `rotateToken()` | Remove a credencial anterior de `integration_tokens`, grava somente o hash da nova credencial e devolve o valor em claro apenas no retorno imediato. |
| `backend/src/main/java/br/com/iforce/praxis/shared/integration/IntegrationManagementController.java` | `POST /api/v1/integrations/{provider}/reactivate` | Expõe a resposta de reativação produzida pelo caso de uso, incluindo o token novo somente nessa chamada. |
| `frontend/src/routes/integrations.tsx` | `reactivateMutation` e `GeneratedTokenModal` | Recebe o token retornado, apresenta o valor para cópia e invalida as consultas de integração sem tentar recuperar o segredo posteriormente. |

### REQ-INTEGRACOES-STATUS-CONEXAO-REAL — partes entregues

| Caminho completo | Método/campo/contrato | Comportamento comprovado |
|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/shared/integration/IntegrationStatusRefreshService.java` | `refreshStatus()` | Executa leitura de estado sem promover `PENDENTE` para `CONECTADA`; uma integração marcada como conectada sem evidência temporal é apresentada como pendente. |
| `backend/src/main/java/br/com/iforce/praxis/shared/integration/IntegrationManagementController.java` | `POST /api/v1/integrations/{provider}/test-connection` e `POST /api/v1/integrations/{provider}/refresh-status` | Os dois endpoints delegam à leitura segura de status e não usam mais o método que inferia conexão pela presença do token. |
| `frontend/src/routes/integrations.tsx` | `statusLabel`, texto de `PENDENTE` e ação de atualização | Exibe “Token configurado · aguardando primeiro evento”, explica que a conexão depende da primeira requisição autenticada e trata a ação manual como atualização de estado, não como prova de conectividade. |

## Regras de manutenção

- Uma entrega só entra neste histórico depois de verificação direta do código e do fluxo alcançável.
- Requisitos parcialmente entregues permanecem no backlog com novo ID ou referência explícita à lacuna ainda existente.
- O histórico não registra tarefas de CI/CD, testes, QA, homologação, métricas observacionais, publicação ou marketing.
