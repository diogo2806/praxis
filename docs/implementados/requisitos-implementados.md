# Requisitos técnicos implementados — praxis

Status: atualizado em 2026-07-15 após conclusão e revalidação de `ASYNC11`, `BUS13` e `INT17`.

Este arquivo registra comportamentos comprovadamente entregues no código e preserva a rastreabilidade de conclusões históricas posteriormente reclassificadas. Entregas parciais ou invalidadas apontam obrigatoriamente para o backlog canônico.

## 2026-07-15

| Origem | Situação registrada | Entrega comprovada | Pendência remanescente |
|---|---|---|---|
| `BUS13` | Concluído | O relatório trata horas poupadas como estimativa opcional, inclui período, fórmula, hipótese por avaliação, fonte metodológica e ressalva explícita, e permite desativar somente a estimativa sem interromper o relatório de uso. | Nenhuma para a explicitação metodológica da estimativa. |
| `LEGACY12` | Concluído | Os fatos técnicos ainda válidos foram convertidos em requisitos objetivos em `docs/requisitos/requisito-tecnico.md` e o arquivo conversacional `docs/backlog.txt` foi removido como fonte concorrente. | Nenhuma para a consolidação documental. |
| `INT16` | Concluído | `docs/INTEGRACAO-GUPY-PROVEDOR.md` descreve `result_candidate_page_url` com JWT assinado do tipo `candidate_result`, empresa e tentativa, TTL próprio configurável, consumo por `CandidateResultPageService`, exemplo contratual e fluxo coerentes com essa implementação. | Nenhuma para o alinhamento documental da URL de resultado; homologação externa permanece não comprovada. |
| `INT17` | Concluído | `result_webhook_url` ficou reservado ao `TestResult` contratual. `ATTEMPT_STARTED` e `ATTEMPT_ABANDONED` somente são publicados quando o evento foi explicitamente selecionado na integração `CUSTOM_API`, cuja entrega usa URL própria e assinatura HMAC. | Nenhuma para a separação dos contratos de webhook. |
| `INT14` | Concluído | `GupyTestResultMapper` gera credencial JWT exclusiva do tipo `candidate_result` e `CandidateResultPageService` aceita somente esse tipo de token, consultando a tentativa pelo par empresa/tentativa. | Nenhuma para a credencial pública de resultado. |
| `INT15` | Concluído | A documentação do catálogo Gupy reproduz o JSON real sem `category` e `level` artificiais e registra corretamente os estados externamente representáveis. | Nenhuma para o escopo documental originalmente definido. |
| `INT13` | Concluído | O catálogo Gupy usa `GupyTestCatalogMapper`; categoria e nível nulos são omitidos por `NON_NULL`, sem valores genéricos fabricados. | Nenhuma para metadados artificiais do catálogo. |
| `API2` | Concluído no escopo de repetição equivalente | A criação Gupy persiste fingerprint versionado, preserva a tentativa em repetições equivalentes e retorna `409` para conteúdo divergente sob a mesma identidade idempotente. | `DATA13` deve separar divergência indevida de uma nova aplicação ou reteste legitimamente autorizado. |
| `ASYNC10` | Concluído no escopo de fan-out por destino | `RESULT_READY` mantém confirmação, tentativas, erro e conclusão independentes para Gupy e `CUSTOM_API`. | Nenhuma para o fan-out por destino. |
| `ASYNC11` | Concluído | `OutboxProcessor.dispatch()` rejeita explicitamente todo tipo não suportado, incluindo tipo e ID do evento no erro; o evento não recebe `SENT` e segue a política existente de retry, backoff, `lastError`, DLQ e alerta administrativo. | Nenhuma. |
| `INT11` | Reclassificado e corrigido por `INT17` | A reclassificação histórica foi preservada. A lacuna então identificada foi eliminada: eventos proprietários deixaram de usar `result_webhook_url`, e o endpoint Gupy recebe somente `TestResult`. | Nenhuma para a separação integral dos contratos. |
| `INT12` | Concluído | Tentativas abandonadas ou expiradas não são publicadas como resultado Gupy concluído com pontuações provisórias. | Nenhuma para estados terminais sem resultado válido. |
| `SEC11` | Concluído | O limite público sanitiza IDs, remove destinos futuros e regras de timeout das respostas e ignora identificadores internos enviados pelo navegador. | Nenhuma para exposição da topologia da avaliação. |
| `SEC12` | Concluído parcialmente no escopo de proteção e telemetria | A política de callback restringe destinos e o backend registra quando a URL é apresentada ao navegador. | `INT18` deve executar o GET servidor-servidor, persistir confirmação, erro e retentativas; `callback_presented` não equivale a callback confirmado. |
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
| `backend/src/main/java/br/com/iforce/praxis/auth/service/JwtService.java` | `generateCandidateResultToken()` e `parseCandidateResultToken()` | Gera e valida token assinado com tipo `candidate_result`, empresa, tentativa e expiração; os tipos de token de execução e resultado permanecem separados. |
| `backend/src/main/java/br/com/iforce/praxis/config/PraxisProperties.java` | `candidateResultTtlHours` | Mantém validade própria para consulta do resultado, com padrão de 720 horas. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/GupyTestResultMapper.java` | `candidateResultPageUrl()` | Publica `/candidato/{token}/resultado` com token `candidate_result`. |
| `backend/src/main/java/br/com/iforce/praxis/candidate/service/CandidateResultPageService.java` | `findByToken()` e `parseToken()` | Aceita somente token de resultado e limita a resposta pública. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptIdempotencyAspect.java` | `enforceEquivalentRetry()` | Bloqueia fingerprint divergente sob a mesma chave; a ausência de um conceito de novo ciclo permanece em `DATA13`. |
| `backend/src/main/java/br/com/iforce/praxis/shared/outbox/service/OutboxProcessor.java` | `dispatch()`, entrega por destino e `handleEventFailure()` | Tipos desconhecidos lançam erro explícito com tipo e ID, permanecem em retry e chegam à DLQ sem `SENT`; eventos de engajamento são entregues somente ao destino `CUSTOM_API`. |
| `backend/src/test/java/br/com/iforce/praxis/shared/outbox/service/OutboxProcessorTest.java` | cenários de evento desconhecido e engajamento | Verifica ausência de `sentAt`, retry/DLQ de tipo desconhecido e impede uso de `ResultWebhookClient.postPayload()` em eventos proprietários. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptService.java` | eventos de resultado e engajamento | Publica `RESULT_READY` para os destinos de resultado aplicáveis e somente enfileira `ATTEMPT_STARTED` ou `ATTEMPT_ABANDONED` quando o evento está habilitado no webhook genérico. Não lê `resultWebhookUrl` nesse fluxo. |
| `backend/src/main/java/br/com/iforce/praxis/shared/integration/service/AttemptEngagementWebhookService.java` | contrato proprietário `CUSTOM_API` | Resolve URL, eventos e segredo exclusivamente na configuração `CUSTOM_API`, valida o destino, assina o corpo com HMAC e registra sucesso ou erro da entrega. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/observability/CandidateCallbackHandoffAdvice.java` | `callback_presented` | Registra que o callback foi incluído na resposta ao navegador; não comprova execução nem confirmação do GET, pendência `INT18`. |
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
