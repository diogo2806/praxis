# Requisitos técnicos implementados — praxis

Status: atualizado em 2026-07-15 após conclusão e revalidação de `ASYNC11`, `BUS13`, `INT18`, `DATA13` e `DATA14`.

Este arquivo registra comportamentos comprovadamente entregues no código e preserva a rastreabilidade de conclusões históricas posteriormente reclassificadas. Entregas parciais ou invalidadas apontam obrigatoriamente para o backlog canônico.

## 2026-07-15

| Origem | Situação registrada | Entrega comprovada | Pendência remanescente |
|---|---|---|---|
| `INT18` | Concluído | A conclusão de uma tentativa com `callback_url` gera `GUPY_CALLBACK_CONFIRMATION` no outbox pela mesma transação de banco. O processador executa GET com validação SSRF, timeouts configuráveis, redirecionamento desabilitado, confirmação exclusiva por HTTP 2xx, código HTTP persistido, backoff, DLQ e reprocessamento manual. | Nenhuma para a confirmação servidor-servidor; o redirecionamento do navegador permanece apenas como experiência complementar. |
| `DATA14` | Concluído | A criação direta exige `applicationCycleId`, aceita contexto opcional, usa identidade idempotente por empresa, candidato, avaliação e ciclo e valida fingerprint do pedido. Ciclos diferentes criam tentativas independentes; repetição equivalente reutiliza somente o mesmo ciclo; reenvio usa endpoint próprio por tentativa, com isolamento por empresa, auditoria e sem novo crédito. A interface apresenta separadamente “Criar nova aplicação” e “Reenviar link”. | Nenhuma para reaplicação e reenvio de links diretos. |
| `BUS13` | Concluído | O relatório trata horas poupadas como estimativa opcional, inclui período, fórmula, hipótese por avaliação, fonte metodológica e ressalva explícita, e permite desativar somente a estimativa sem interromper o relatório de uso. | Nenhuma para a explicitação metodológica da estimativa. |
| `DATA13` | Concluído | A chave idempotente distingue o ciclo inicial do ciclo autorizado por `previous_result=fail`; o reteste exige tentativa anterior terminal, repetições equivalentes reutilizam o mesmo resultado e a unicidade da chave impede duplicidade concorrente. A política detalhada está em `docs/implementados/data13-idempotencia-reteste-gupy.md`. | Nenhuma para o reteste previsto pelo contrato atual; ciclos adicionais exigem futuro identificador explícito de aplicação/ciclo fornecido pela Gupy. |
| `LEGACY12` | Concluído | Os fatos técnicos ainda válidos foram convertidos em requisitos objetivos em `docs/requisitos/requisito-tecnico.md` e o arquivo conversacional `docs/backlog.txt` foi removido como fonte concorrente. | Nenhuma para a consolidação documental. |
| `INT16` | Concluído | `docs/INTEGRACAO-GUPY-PROVEDOR.md` descreve `result_candidate_page_url` com JWT assinado do tipo `candidate_result`, empresa e tentativa, TTL próprio configurável, consumo por `CandidateResultPageService`, exemplo contratual e fluxo coerentes com essa implementação. | Nenhuma para o alinhamento documental da URL de resultado; homologação externa permanece não comprovada. |
| `INT14` | Concluído | `GupyTestResultMapper` gera credencial JWT exclusiva do tipo `candidate_result` e `CandidateResultPageService` aceita somente esse tipo de token, consultando a tentativa pelo par empresa/tentativa. | Nenhuma para a credencial pública de resultado. |
| `INT15` | Concluído | A documentação do catálogo Gupy reproduz o JSON real sem `category` e `level` artificiais e registra corretamente os estados externamente representáveis. | Nenhuma para o escopo documental originalmente definido. |
| `INT13` | Concluído | O catálogo Gupy usa `GupyTestCatalogMapper`; categoria e nível nulos são omitidos por `NON_NULL`, sem valores genéricos fabricados. | Nenhuma para metadados artificiais do catálogo. |
| `API2` | Concluído | A criação Gupy persiste fingerprint versionado, preserva a tentativa em repetições equivalentes e retorna `409` para conteúdo divergente dentro do mesmo ciclo idempotente. | Nenhuma para repetição equivalente e reteste contratual. |
| `ASYNC10` | Concluído no escopo de fan-out por destino | `RESULT_READY` mantém confirmação, tentativas, erro e conclusão independentes para Gupy e `CUSTOM_API`. | Nenhuma para o fan-out por destino. |
| `ASYNC11` | Concluído | `OutboxProcessor.dispatch()` rejeita explicitamente todo tipo não suportado, incluindo tipo e ID do evento no erro; o evento não recebe `SENT` e segue a política existente de retry, backoff, `lastError`, DLQ e alerta administrativo. | Nenhuma. |
| `INT11` | Reclassificado | A separação existe para `RESULT_READY`, mas a conclusão histórica de que todos os eventos proprietários deixaram de usar `result_webhook_url` não corresponde ao fluxo atual. `ATTEMPT_STARTED` e `ATTEMPT_ABANDONED` ainda usam esse destino. | `INT17` deve reservar o webhook Gupy exclusivamente ao `TestResult`. |
| `INT12` | Concluído | Tentativas abandonadas ou expiradas não são publicadas como resultado Gupy concluído com pontuações provisórias. | Nenhuma para estados terminais sem resultado válido. |
| `SEC11` | Concluído | O limite público sanitiza IDs, remove destinos futuros e regras de timeout das respostas e ignora identificadores internos enviados pelo navegador. | Nenhuma para exposição da topologia da avaliação. |
| `SEC12` | Concluído no escopo de proteção, telemetria e recuperação | A política de callback restringe destinos, o backend confirma o callback por processamento servidor-servidor persistente e `callback_presented` permanece identificado apenas como apresentação ao navegador. | Nenhuma para o handoff de conclusão e sua recuperação operacional. |
| `SEC13` | Concluído | Os endpoints públicos exigem JWT de tentativa válido; token inválido ou expirado retorna `401` e o padrão `att_...` não funciona como credencial no controller público. | Nenhuma para bypass pelas rotas públicas protegidas. |
| `UI10` | Concluído | O dashboard usa somente a API oficial e não fabrica plano, operação ou conexão por fallback. | Nenhuma. |
| `SEC10` | Concluído | Rotação, reativação, desconexão e revogação de credenciais usam um caso de uso transacional único. | Nenhuma. |
| `INT10` | Concluído | Endpoints Gupy e Recrutei registram atividade autenticada somente após sucesso do fluxo real. | Nenhuma. |
| `CFG10` | Concluído | O runtime não exige credencial global de integração; tokens permanecem vinculados à empresa e ao provedor. | Nenhuma. |
| `INT1` | Concluído | Identificadores externos são inteiros positivos canônicos e participam de pertencimento e idempotência de forma estável. | Nenhuma. |
| `INT2` | Concluído | Enums do contrato externo são fechados e valores artificiais são rejeitados. | Nenhuma. |
| `API1` | Concluído | Consulta e webhook usam o mesmo DTO externo sem extensões internas no topo. | Nenhuma. |
| `REQ-INTEGRACOES-REATIVACAO-TOKEN-ATS` | Concluído | Reativação rotaciona a credencial e expõe o novo token somente no retorno imediato. | Nenhuma. |
| `REQ-INTEGRACOES-STATUS-CONEXAO-REAL` | Concluído | O estado conectado depende de atividade externa autenticada e mantém evidência temporal. | Nenhuma. |

### Entregas e comportamentos comprovados nesta revalidação

| Caminho completo | Método/campo/contrato | Comportamento comprovado |
|---|---|---|
| `backend/src/main/resources/db/migration/V1001__gupy_callback_confirmation_outbox.sql` | trigger `enqueue_gupy_callback_confirmation()` | Insere o evento de callback na mesma transação da conclusão e deduplica por empresa, tentativa e URL, permitindo novo evento quando o provedor renovar o callback. |
| `backend/src/main/java/br/com/iforce/praxis/shared/outbox/service/OutboxProcessor.java` | `processGupyCallbackConfirmationEvent()` | Executa o GET, persiste tentativas, status, código HTTP, erro e instante de confirmação, reutilizando retry exponencial, DLQ e alerta administrativo do outbox. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/delivery/service/RestClientResultWebhookClient.java` | `getCallback()` | Aplica timeout de conexão e leitura, não segue redirecionamento e considera confirmado somente status 2xx. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/delivery/controller/CallbackDeliveryController.java` | listagem e `/{deliveryId}/reprocess` | Disponibiliza consulta operacional e reprocessamento manual das confirmações de callback da empresa autenticada. |
| `backend/src/test/java/br/com/iforce/praxis/gupy/delivery/service/RestClientResultWebhookClientTest.java` | cenários 2xx e redirecionamento | Comprova confirmação por 204 e rejeição de 302 sem seguir o novo destino. |
| `backend/src/main/java/br/com/iforce/praxis/candidate/service/CompanyCandidateLinkService.java` | `createNewApplication()` | Exige ciclo explícito, inclui ciclo e contexto no fingerprint, cria nova tentativa somente para ciclo novo e retorna `409` quando o mesmo ciclo recebe conteúdo divergente. |
| `backend/src/main/java/br/com/iforce/praxis/candidate/service/CompanyCandidateLinkService.java` | `resendExisting()` | Recupera a tentativa pelo par empresa/ID, retorna o mesmo link, não cria tentativa nem consulta novo crédito e registra `CANDIDATE_LINK_RESENT`. |
| `backend/src/main/java/br/com/iforce/praxis/candidate/controller/CompanyCandidateLinkController.java` | `POST /api/v1/candidate-links` e `POST /{attemptId}/resend` | Expõe comandos separados e informa no retorno se a operação criou, reconciliou por idempotência ou reenviou a tentativa. |
| `frontend/src/routes/enviar-link.tsx` | criação e reenvio | Mostra ações distintas, descreve os efeitos e exige confirmação antes de criar uma nova aplicação. |
| `backend/src/test/java/br/com/iforce/praxis/candidate/service/CompanyCandidateLinkServiceTest.java` | cenários de DATA14 | Cobre repetição equivalente, divergência no mesmo ciclo, ciclos diferentes, reenvio sem novo crédito e isolamento entre empresas. |
| `backend/src/main/java/br/com/iforce/praxis/auth/service/JwtService.java` | `generateCandidateResultToken()` e `parseCandidateResultToken()` | Gera e valida token assinado com tipo `candidate_result`, empresa, tentativa e expiração; os tipos de token de execução e resultado permanecem separados. |
| `backend/src/main/java/br/com/iforce/praxis/config/PraxisProperties.java` | `candidateResultTtlHours` | Mantém validade própria para consulta do resultado, com padrão de 720 horas. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/GupyTestResultMapper.java` | `candidateResultPageUrl()` | Publica `/candidato/{token}/resultado` com token `candidate_result`. |
| `backend/src/main/java/br/com/iforce/praxis/candidate/service/CandidateResultPageService.java` | `findByToken()` e `parseToken()` | Aceita somente token de resultado e limita a resposta pública. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptIdempotencyAspect.java` | `enforceEquivalentRetry()` | Separa ciclo inicial e reteste Gupy, exige estado anterior terminal, valida fingerprints versão 1 e 2 e mantém repetições equivalentes no mesmo resultado. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptIdempotencyKeyFactory.java` | `currentKey()` e `initialKey()` | Centraliza a identidade base e acrescenta marcador determinístico somente ao ciclo autorizado por `previous_result=fail`. |
| `backend/src/main/java/br/com/iforce/praxis/shared/outbox/service/OutboxProcessor.java` | `dispatch()` e `handleEventFailure()` | Tipos desconhecidos lançam erro explícito com tipo e ID, permanecem em retry e chegam à DLQ após o limite sem serem marcados como `SENT`. |
| `backend/src/test/java/br/com/iforce/praxis/shared/outbox/service/OutboxProcessorTest.java` | cenários de evento desconhecido | Verifica ausência de `sentAt`, persistência de `lastError`, transição para `RETRYING` e encaminhamento à DLQ na quinta tentativa. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptService.java` | eventos de resultado e engajamento | Publica `RESULT_READY` no outbox, mas também associa eventos proprietários de engajamento ao `resultWebhookUrl`, pendência `INT17`. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/observability/CandidateCallbackHandoffAdvice.java` | `callback_presented` | Registra somente a apresentação da URL ao navegador; a confirmação efetiva é independente e persistida no evento `GUPY_CALLBACK_CONFIRMATION`. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/ResultScoringService.java` | normalização | Renormaliza pesos sobre competências cobertas pelo caminho; a comparabilidade entre bases distintas permanece em `BUS12`. |
| `backend/src/main/java/br/com/iforce/praxis/engagement/dto/EngagementReportSummary.java` | contrato da estimativa | Expõe período, indicador de habilitação, valor estimado, hipótese em horas por avaliação, fórmula, fonte metodológica e ressalva explícita. |
| `backend/src/main/java/br/com/iforce/praxis/engagement/service/EngagementReportService.java` | `sendMonthlyReports()` | Calcula a estimativa somente quando habilitada, valida a hipótese positiva e mantém o relatório de conclusões quando a estimativa está desativada. |
| `backend/src/main/java/br/com/iforce/praxis/engagement/service/LoggingEngagementReportEmailSender.java` | mensagem do relatório | Identifica o valor como estimativa potencial, informa o período, a fórmula, o parâmetro, a fonte metodológica e a ressalva de ausência de comprovação causal. |
| `backend/src/main/resources/application.properties` | configuração da estimativa | Documenta a unidade do parâmetro, permite desativar a estimativa e configurar a origem metodológica por ambiente. |

## Regras de manutenção

- Uma entrega só entra neste histórico depois de verificação direta do código e do fluxo alcançável.
- Uma conclusão histórica invalidada por nova evidência deve ser reclassificada, sem apagar a rastreabilidade.
- Requisitos parcialmente entregues permanecem no backlog com referência explícita à lacuna remanescente.
- O histórico não registra tarefas de CI/CD, testes, QA, homologação, métricas meramente observacionais, publicação ou marketing.
