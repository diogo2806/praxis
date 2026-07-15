# Requisitos técnicos implementados — praxis

Status: atualizado em 2026-07-15 após entregas de `CFG10`, `INT1`, `INT2` e `API1`.

Este arquivo registra somente comportamentos comprovadamente entregues no código e no fluxo real. Entregas parciais são descritas como parciais e apontam para os IDs que mantêm as lacunas remanescentes no backlog canônico.

## 2026-07-15

| Origem | Situação registrada | Entrega comprovada | Pendência remanescente |
|---|---|---|---|
| `CFG10` | Concluído | O Docker Compose não exige mais `PRAXIS_INTEGRATION_TOKEN`; o CI valida a configuração sem credencial global; Gupy e Recrutei continuam autenticando somente pelos tokens persistidos por empresa e provedor. | Nenhuma. |
| `INT1` | Concluído | `company_id` e `document_id` do `POST /test/candidate` são recebidos como inteiros JSON `int64` positivos, normalizados para identidade decimal canônica, validados contra o token e usados de forma estável na idempotência. | Nenhuma para o contrato de entrada; a homologação real continua necessária. |
| `INT2` | Concluído | `candidate_type` aceita somente `internal` ou `external`; `previous_result` aceita somente `fail`, ausência ou JSON `null`; valores desconhecidos, `pass`, `none` e a string `"null"` retornam `400`; o construtor auxiliar usa os mesmos enums fechados. | Nenhuma para os enums de entrada. |
| `API1` | Concluído | Consulta e webhook usam o mesmo DTO externo sem `reliabilityLevel` nem `other_informations` no topo; informações operacionais permanecem no domínio e na persistência interna. | Nenhuma para o schema externo de resultado. |
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
| `backend/src/test/java/br/com/iforce/praxis/gupy/controller/GupyIntegrationControllerTest.java` | contrato e idempotência | Cobre rejeição de identificadores textuais, ausentes e não positivos, pertencimento ao token, repetição idempotente e diferenciação por `job_id`. |

### INT2 — enums oficiais no limite de entrada

| Caminho completo | Método/campo/contrato | Comportamento comprovado |
|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/gupy/dto/CreateCandidateRequest.java` | `CandidateType` | Aceita somente `internal` e `external`; ausência e JSON `null` permanecem opcionais. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/dto/CreateCandidateRequest.java` | `PreviousResult` | Aceita `fail`, ausência e JSON `null`; rejeita `none`, `pass`, a string `"null"` e valores desconhecidos. |
| `backend/src/test/java/br/com/iforce/praxis/gupy/controller/GupyCandidateContractValidationTest.java` | `POST /test/candidate` | Comprova `201` para os valores oficiais e `400` para valores artificiais ou desconhecidos. |
| `docs/INTEGRACAO-GUPY-PROVEDOR.md` | contrato de `POST /test/candidate` | Registra os tipos e enums efetivamente aceitos e remove essas divergências dos bloqueadores de homologação. |

### API1 — schema externo de resultado

| Caminho completo | Método/campo/contrato | Comportamento comprovado |
|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/gupy/dto/TestResultResponse.java` | contrato serializado | Contém somente os campos oficiais do `TestResult` publicado pela Gupy. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/GupyTestResultMapper.java` | consulta e webhook | Produz o mesmo DTO externo restrito para os dois canais de entrega. |
| `backend/src/test/java/br/com/iforce/praxis/gupy/controller/GupyResultEndpointContractTest.java` | resposta da consulta | Confirma que `reliabilityLevel` e `other_informations` não aparecem no topo. |
| `backend/src/test/java/br/com/iforce/praxis/gupy/delivery/service/RestClientResultWebhookClientTest.java` | payload assíncrono | Confirma que o webhook também não envia extensões internas. |

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
