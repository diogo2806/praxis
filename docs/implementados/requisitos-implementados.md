# Requisitos técnicos implementados — praxis

Status: revalidado em 2026-07-16 após correção das fontes de verdade da integração Gupy e confirmação de `BUS12` e `UI13`.

Este arquivo registra comportamentos comprovadamente entregues no código e preserva a rastreabilidade de conclusões históricas posteriormente reclassificadas. Entregas parciais ou invalidadas apontam obrigatoriamente para o backlog canônico.

## 2026-07-16

| Origem | Situação registrada | Entrega comprovada | Pendência remanescente |
|---|---|---|---|
| `INT18` | Reclassificado | O GET servidor-servidor de `callback_url` foi desativado pela migration `V1005__disable_duplicate_gupy_callback_confirmation.sql`, porque duplicava o redirecionamento já executado pelo navegador. Eventos legados pendentes foram enviados para DLQ; a API mantém apenas consulta histórica e não expõe reprocessamento. | Confirmar o redirecionamento do navegador em vaga real da Gupy. |
| `SEC12` | Reclassificado no escopo de handoff seguro | A política valida o destino recebido, a resposta concluída reapresenta o mesmo `callback_url` para recuperação e `callback_presented` registra somente a disponibilização ao navegador. | Homologação externa do handoff em vaga real. |
| `BUS12` | Concluído | `ComparableSimulationValidationService`, registrada como `@Primary`, bloqueia a publicação quando caminhos possuem bases máximas diferentes ou zeradas por competência. | Nenhuma para comparabilidade de avaliações publicadas. |
| `UI13` | Concluído | O centro operacional e a listagem de links usam consultas paginadas, filtros e todos os estados relevantes, sem corte silencioso fixo. | Nenhuma para paginação e cobertura de estados. |
| `GUPY-CONTRACT-20260716` | Concluído | `previous_result` aceita JSON `null` e string `"null"`; `GET /test` preserva `limit=0`; a página pública mostra somente resultados `major`; o callback legado ficou somente leitura. | Homologação externa permanece pendente. |

## 2026-07-15

| Origem | Situação registrada | Entrega comprovada | Pendência remanescente |
|---|---|---|---|
| `INT17` | Concluído | `result_webhook_url` ficou reservado ao `TestResult` contratual. `ATTEMPT_STARTED` e `ATTEMPT_ABANDONED` somente são publicados quando o evento foi explicitamente selecionado na integração `CUSTOM_API`, cuja entrega usa URL própria e assinatura HMAC. | Nenhuma para a separação dos contratos de webhook. |
| `DATA14` | Concluído | A criação direta exige `applicationCycleId`, aceita contexto opcional, usa identidade idempotente por empresa, candidato, avaliação e ciclo e valida fingerprint do pedido. Ciclos diferentes criam tentativas independentes; repetição equivalente reutiliza somente o mesmo ciclo; reenvio usa endpoint próprio por tentativa, com isolamento por empresa, auditoria e sem novo crédito. A interface apresenta separadamente “Criar nova aplicação” e “Reenviar link”. | Nenhuma para reaplicação e reenvio de links diretos. |
| `BUS13` | Concluído | O relatório trata horas poupadas como estimativa opcional, inclui período, fórmula, hipótese por avaliação, fonte metodológica e ressalva explícita, e permite desativar somente a estimativa sem interromper o relatório de uso. | Nenhuma para a explicitação metodológica da estimativa. |
| `DATA13` | Concluído | A chave idempotente distingue o ciclo inicial do ciclo autorizado por `previous_result=fail`; o reteste exige tentativa anterior terminal, repetições equivalentes reutilizam o mesmo resultado e a unicidade da chave impede duplicidade concorrente. A política detalhada está em `docs/implementados/data13-idempotencia-reteste-gupy.md`. | Ciclos adicionais exigem futuro identificador explícito de aplicação/ciclo fornecido pela Gupy. |
| `LEGACY12` | Concluído | Os fatos técnicos ainda válidos foram convertidos em requisitos objetivos em `docs/requisitos/requisito-tecnico.md` e o arquivo conversacional `docs/backlog.txt` foi removido como fonte concorrente. | Nenhuma para a consolidação documental. |
| `INT16` | Concluído | `docs/INTEGRACAO-GUPY-PROVEDOR.md` descreve `result_candidate_page_url` com JWT assinado do tipo `candidate_result`, empresa e tentativa, TTL próprio configurável, consumo por `CandidateResultPageService`, exemplo contratual e fluxo coerentes com essa implementação. | Homologação externa permanece não comprovada. |
| `INT14` | Concluído | `GupyTestResultMapper` gera credencial JWT exclusiva do tipo `candidate_result` e `CandidateResultPageService` aceita somente esse tipo de token, consultando a tentativa pelo par empresa/tentativa. | Nenhuma para a credencial pública de resultado. |
| `INT15` | Concluído | A documentação do catálogo Gupy reproduz o JSON real sem `category` e `level` artificiais e registra corretamente os estados externamente representáveis. | Nenhuma. |
| `INT13` | Concluído | O catálogo Gupy usa `GupyTestCatalogMapper`; categoria e nível nulos são omitidos por `NON_NULL`, sem valores genéricos fabricados. | Nenhuma. |
| `API2` | Concluído | A criação Gupy persiste fingerprint versionado, preserva a tentativa em repetições equivalentes e retorna `409` para conteúdo divergente dentro do mesmo ciclo idempotente. | Nenhuma. |
| `ASYNC10` | Concluído no escopo de fan-out por destino | `RESULT_READY` mantém confirmação, tentativas, erro e conclusão independentes para Gupy e `CUSTOM_API`. | Nenhuma. |
| `ASYNC11` | Concluído | `OutboxProcessor.dispatch()` rejeita explicitamente todo tipo não suportado, incluindo tipo e ID do evento no erro; o evento não recebe `SENT` e segue retry, backoff, `lastError`, DLQ e alerta administrativo. | Nenhuma. |
| `INT11` | Reclassificado e corrigido por `INT17` | Eventos proprietários deixaram de usar `result_webhook_url`, e o endpoint Gupy recebe somente `TestResult`. | Nenhuma. |
| `INT12` | Concluído | Tentativas abandonadas ou expiradas não são publicadas como resultado Gupy concluído com pontuações provisórias. | Nenhuma. |
| `SEC11` | Concluído | O limite público sanitiza IDs, remove destinos futuros e regras de timeout das respostas e ignora identificadores internos enviados pelo navegador. | Nenhuma. |
| `SEC13` | Concluído | Os endpoints públicos exigem JWT de tentativa válido; token inválido ou expirado retorna `401` e o padrão `att_...` não funciona como credencial. | Nenhuma. |
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
| `backend/src/main/resources/db/migration/V1005__disable_duplicate_gupy_callback_confirmation.sql` | remoção de trigger e saneamento do outbox | Remove a geração automática do callback servidor-servidor e envia eventos legados executáveis para DLQ. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/delivery/controller/CallbackDeliveryController.java` | `GET /api/v1/gupy/callback-deliveries` | Mantém somente consulta do histórico legado; não expõe comando de reprocessamento. |
| `backend/src/test/java/br/com/iforce/praxis/gupy/delivery/controller/CallbackDeliveryControllerTest.java` | histórico e rota removida | Comprova a listagem somente leitura e a resposta `404` no antigo POST de reprocessamento. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/observability/CandidateCallbackHandoffAdvice.java` | `callback_presented` | Registra a apresentação da URL ao navegador sem afirmar confirmação externa. |
| `backend/src/main/java/br/com/iforce/praxis/candidate/service/CandidateResultPageService.java` | `findByToken()` | Reapresenta o retorno somente após conclusão e divulga apenas resultados `major`. |
| `backend/src/main/java/br/com/iforce/praxis/simulation/service/ComparableSimulationValidationService.java` | validação de publicação | Impede publicação de versões com bases máximas incompatíveis ou zeradas por competência. |
| `backend/src/main/java/br/com/iforce/praxis/candidate/service/CompanyCandidateLinkService.java` | `createNewApplication()` | Exige ciclo explícito, inclui ciclo e contexto no fingerprint e retorna `409` para conteúdo divergente no mesmo ciclo. |
| `backend/src/main/java/br/com/iforce/praxis/candidate/service/CompanyCandidateLinkService.java` | `resendExisting()` | Retorna o mesmo link, não cria tentativa nem consome novo crédito e registra `CANDIDATE_LINK_RESENT`. |
| `backend/src/main/java/br/com/iforce/praxis/candidate/controller/CompanyCandidateLinkController.java` | criação e reenvio | Expõe comandos separados e informa se a operação criou, reconciliou ou reenviou a tentativa. |
| `frontend/src/routes/enviar-link.tsx` | criação e reenvio | Mostra ações distintas e exige confirmação antes de criar nova aplicação. |
| `backend/src/test/java/br/com/iforce/praxis/candidate/service/CompanyCandidateLinkServiceTest.java` | cenários de `DATA14` | Cobre repetição equivalente, divergência, ciclos diferentes, reenvio sem novo crédito e isolamento. |
| `backend/src/main/java/br/com/iforce/praxis/auth/service/JwtService.java` | tokens de tentativa e resultado | Gera e valida tipos distintos, empresa, tentativa e expiração. |
| `backend/src/main/java/br/com/iforce/praxis/config/PraxisProperties.java` | `candidateResultTtlHours` | Mantém validade própria para consulta do resultado, com padrão de 720 horas. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/GupyTestResultMapper.java` | `candidateResultPageUrl()` | Publica `/candidato/{token}/resultado` com token `candidate_result`. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptIdempotencyAspect.java` | `enforceEquivalentRetry()` | Separa ciclo inicial e reteste, exige estado anterior terminal e valida fingerprints. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptIdempotencyKeyFactory.java` | chaves canônicas | Centraliza a identidade base e acrescenta marcador somente ao reteste por `previous_result=fail`. |
| `backend/src/main/java/br/com/iforce/praxis/shared/outbox/service/OutboxProcessor.java` | entrega por destino e falhas | Mantém retry/DLQ de resultados e separa o destino Gupy do webhook proprietário `CUSTOM_API`. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptService.java` | eventos de resultado e engajamento | Publica `RESULT_READY` e só enfileira eventos proprietários quando habilitados no webhook genérico. |
| `backend/src/main/java/br/com/iforce/praxis/shared/integration/service/AttemptEngagementWebhookService.java` | contrato `CUSTOM_API` | Resolve configuração própria, valida destino, assina com HMAC e registra a entrega. |
| `backend/src/main/java/br/com/iforce/praxis/engagement/dto/EngagementReportSummary.java` | contrato da estimativa | Expõe período, hipótese, fórmula, fonte metodológica e ressalva. |
| `backend/src/main/java/br/com/iforce/praxis/engagement/service/EngagementReportService.java` | relatório | Calcula a estimativa somente quando habilitada e mantém o relatório quando desativada. |
| `backend/src/main/java/br/com/iforce/praxis/engagement/service/LoggingEngagementReportEmailSender.java` | mensagem | Identifica o valor como estimativa potencial e informa parâmetros e ressalvas. |
| `backend/src/main/resources/application.properties` | configuração da estimativa | Documenta unidade, habilitação e origem metodológica por ambiente. |

## Regras de manutenção

- Uma entrega só entra neste histórico depois de verificação direta do código e do fluxo alcançável.
- Uma conclusão histórica invalidada por nova evidência deve ser reclassificada, sem apagar a rastreabilidade.
- Requisitos parcialmente entregues permanecem no backlog com referência explícita à lacuna remanescente.
- O histórico não registra tarefas de CI/CD, QA, homologação, métricas meramente observacionais, publicação ou marketing.
