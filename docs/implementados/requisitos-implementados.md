# Requisitos técnicos implementados — praxis

Status: atualizado em 2026-07-15 após auditoria da `main` no commit `319102895c981eae48eb9595c3a61da0dcd43899`.

Este arquivo registra somente comportamentos comprovadamente entregues no código e no fluxo real. Entregas parciais são descritas como parciais e apontam para os IDs que mantêm as lacunas remanescentes no backlog canônico.

## 2026-07-15

| Origem | Situação registrada | Entrega comprovada | Pendência remanescente |
|---|---|---|---|
| `REQ-INTEGRACOES-REATIVACAO-TOKEN-ATS` | Concluído | Reativação de Gupy/Recrutei rotaciona a credencial, retorna o novo token uma única vez, persiste somente hash/prévia, muda o estado para `PENDENTE` e limpa a atividade da credencial anterior. | O ciclo geral de rotação/revogação ainda possui fluxos concorrentes; registrado em `SEC10`. |
| `REQ-INTEGRACOES-STATUS-CONEXAO-REAL` | Entrega parcial consolidada | Atualização manual de status deixou de promover conexão; conexões sem `lastSyncAt` são normalizadas para `PENDENTE`; a interface diferencia token configurado de conexão comprovada. | Registro completo da atividade externa, auditoria e proteção de `DESATIVADA` permanecem em `INT10`; consistência de credenciais permanece em `SEC10`. |
| `INT2` | Concluído | `candidate_type` aceita somente `internal` ou `external`; `previous_result` aceita somente `fail` ou ausência/JSON `null`; valores desconhecidos, `pass`, `none` e a string `"null"` retornam `400`; o construtor auxiliar usa os mesmos enums fechados. | Nenhuma para `INT2`; a divergência de tipos de `company_id` e `document_id` permanece separada em `INT1`. |

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

### INT2 — enums oficiais do contrato de entrada Gupy

| Caminho completo | Método/campo/contrato | Comportamento comprovado |
|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/gupy/dto/CreateCandidateRequest.java` | campos `candidateType`, `previousResult`, enums `CandidateType` e `PreviousResult` | A desserialização aceita somente os valores oficiais, preserva ausência como `null`, não cria fallback `none` e lança erro de entrada para qualquer texto desconhecido. O construtor auxiliar recebe os mesmos tipos fechados e não permite strings livres em chamadas internas. |
| `backend/src/test/java/br/com/iforce/praxis/gupy/dto/CreateCandidateRequestTest.java` | testes dos enums e construtores | Comprova os valores de wire aceitos, a compatibilidade do construtor auxiliar tipado e a rejeição de `partner`, `none` e `pass`. |
| `backend/src/test/java/br/com/iforce/praxis/gupy/controller/GupyCandidateContractValidationTest.java` | `POST /test/candidate` | Comprova `201` para `internal` com `fail` e `400` para `candidate_type` desconhecido, `none`, `pass` e a string `"null"`. |
| `backend/src/test/java/br/com/iforce/praxis/gupy/controller/GupyIntegrationControllerTest.java` | payload válido de criação | Passa a representar ausência de resultado anterior como JSON `null`, sem usar o valor artificial `none`. |
| `docs/INTEGRACAO-GUPY-PROVEDOR.md` | compatibilidade, contrato e bloqueadores | Registra os enums como compatíveis e remove a validação de `previous_result` da lista de bloqueadores de homologação. |

## Regras de manutenção

- Uma entrega só entra neste histórico depois de verificação direta do código e do fluxo alcançável.
- Requisitos parcialmente entregues permanecem no backlog com novo ID ou referência explícita à lacuna ainda existente.
- O histórico não registra tarefas de CI/CD, testes, QA, homologação, métricas observacionais, publicação ou marketing.
