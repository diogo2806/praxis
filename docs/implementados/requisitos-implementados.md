# Requisitos técnicos implementados — praxis

Status: atualizado em 2026-07-15 após conclusão de `INT15`.

Este arquivo registra somente comportamentos comprovadamente entregues no código e no fluxo real. Entregas parciais permanecem no backlog canônico.

## 2026-07-15

| Origem | Situação registrada | Entrega comprovada | Pendência remanescente |
|---|---|---|---|
| `INT15` | Concluído | A documentação do provedor Gupy passou a reproduzir o JSON real do catálogo sem `category` e `level` artificiais, identifica `result_candidate_page_url` como incompatível enquanto publicar `attemptId` cru, registra que `ABANDONED` e `EXPIRED` são rejeitados e mantém explícita a ausência de homologação. | `INT14` continua pendente para gerar a credencial assinada da página pública de resultado; a homologação real também permanece externa ao código. |
| `INT13` | Concluído | O catálogo Gupy usa `GupyTestCatalogMapper` no fluxo real de `GET /test`; como o domínio publicado não possui fonte configurável para categoria e nível, ambos são mantidos nulos e omitidos da serialização externa por `NON_NULL`, sem valores genéricos fabricados. | Nenhuma para metadados artificiais do catálogo. |
| `API2` | Concluído | A criação Gupy calcula impressão canônica versionada da requisição, persiste o fingerprint e retorna `409` quando a mesma chave idempotente reaparece com conteúdo divergente; repetições equivalentes preservam a tentativa original. | Nenhuma para consistência da repetição idempotente. |
| `ASYNC10` | Concluído | O processamento de `RESULT_READY` mantém confirmação, tentativas, erro e conclusão por destino Gupy e `CUSTOM_API`; falha de um destino não apaga sua entrega nem repete automaticamente o destino já confirmado. | Nenhuma para fan-out e retry independente. |
| `INT11` | Concluído | Eventos proprietários de engajamento deixaram de usar o `result_webhook_url`; o destino Gupy permanece reservado ao `TestResult`, enquanto eventos internos dependem de integração genérica explicitamente configurada. | Nenhuma para separação dos contratos de webhook. |
| `INT12` | Concluído | Tentativas abandonadas ou expiradas não são mais publicadas como resultado Gupy concluído com pontuações provisórias; a entrega externa exige conclusão real e resultado calculado. | Nenhuma para estados terminais sem resultado válido. |
| `SEC11` | Concluído | O limite público sanitiza IDs, remove destinos futuros e regras de timeout das respostas e ignora identificadores internos enviados pelo navegador; a navegação permanece resolvida no servidor. | Nenhuma para exposição da topologia da avaliação. |
| `SEC12` | Concluído | A política de callback Gupy passou a restringir destinos e o handoff de conclusão é registrado no backend, sem depender apenas do atraso e redirecionamento em JavaScript. | Nenhuma para proteção do callback e evidência do handoff. |
| `SEC13` | Concluído | Todos os endpoints públicos exigem JWT de tentativa válido; token inválido ou expirado retorna `401` e o padrão `att_...` não funciona mais como credencial alternativa. | Nenhuma para bypass por identificador cru. |
| `UI10` | Concluído | O dashboard usa somente a API oficial e não fabrica plano, operação ou conexão por fallback. | Nenhuma. |
| `SEC10` | Concluído | Rotação, reativação, desconexão e revogação de credenciais usam um caso de uso transacional único. | Nenhuma. |
| `INT10` | Concluído | Endpoints Gupy e Recrutei registram atividade autenticada somente após sucesso do fluxo real. | Nenhuma. |
| `CFG10` | Concluído | O runtime não exige credencial global de integração; tokens permanecem vinculados à empresa e ao provedor. | Nenhuma. |
| `INT1` | Concluído | Identificadores externos são inteiros positivos canônicos e participam de pertencimento e idempotência de forma estável. | Nenhuma. |
| `INT2` | Concluído | Enums do contrato externo são fechados e valores artificiais são rejeitados. | Nenhuma. |
| `API1` | Concluído | Consulta e webhook usam o mesmo DTO externo sem extensões internas no topo. | Nenhuma. |
| `REQ-INTEGRACOES-REATIVACAO-TOKEN-ATS` | Concluído | Reativação rotaciona a credencial e expõe o novo token somente no retorno imediato. | Nenhuma. |
| `REQ-INTEGRACOES-STATUS-CONEXAO-REAL` | Concluído | O estado conectado depende de atividade externa autenticada e mantém evidência temporal. | Nenhuma. |

### Entregas comprovadas nesta auditoria

| Caminho completo | Método/campo/contrato | Comportamento comprovado |
|---|---|---|
| `docs/INTEGRACAO-GUPY-PROVEDOR.md` | catálogo, matriz de compatibilidade, URLs e estados | O exemplo de `GET /test` corresponde à serialização real; a URL de resultado da pessoa candidata é marcada como incompatível até `INT14`; estados sem resultado final deixam de ser descritos como `done`; a documentação não afirma homologação. |
| `README.md` | ressalva da integração Gupy | A apresentação permanece sem afirmação de homologação e encaminha para o documento técnico atualizado. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/controller/GupyIntegrationController.java` | `listPublishedTests()` | O endpoint alcançável autentica a integração, busca somente simulações publicadas da empresa e delega cada item ao mapper de catálogo. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/GupyTestCatalogMapper.java` | `toResponse()` | Preserva ID, nome e descrição reais da simulação e não fabrica categoria ou nível sem fonte no domínio publicado. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/dto/GupyTestResponse.java` | `@JsonInclude(NON_NULL)` | Remove os campos opcionais nulos do JSON efetivamente enviado pelo catálogo. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptIdempotencyAspect.java` | `enforceEquivalentRetry()` | Intercepta o caso de uso real, compara fingerprint versionado ou snapshot legado antes da mutação e bloqueia divergência com conflito. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/persistence/entity/CandidateAttemptEntity.java` | `requestFingerprint` e `requestFingerprintVersion` | Mantém a evidência necessária para validar repetições futuras. |
| `backend/src/main/resources/db/migration/V39__candidate_attempt_request_fingerprint.sql` | novas colunas | Persiste a impressão canônica sem substituir dados funcionais da tentativa. |
| `backend/src/main/java/br/com/iforce/praxis/shared/outbox/service/OutboxProcessor.java` | estado de entrega por destino | Retoma somente destinos pendentes e finaliza o evento após todos os destinos aplicáveis serem confirmados. |
| `backend/src/main/java/br/com/iforce/praxis/shared/integration/service/ConfirmableGenericWebhookDeliveryService.java` | `deliverResultReady()` | Propaga falha persistida do webhook genérico para o outbox manter retry ou DLQ. |
| `backend/src/main/java/br/com/iforce/praxis/candidate/service/PublicCandidateFlowSecurity.java` | validação e sanitização | Exige token válido, gera IDs opacos e remove transições futuras do contrato público. |
| `backend/src/main/java/br/com/iforce/praxis/candidate/controller/CandidateAttemptController.java` | endpoints públicos | Aplica a validação e sanitização antes e depois do caso de uso em leitura, resposta e solicitações do candidato. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptService.java` | publicação de eventos, estados e callback | Separa destinos, evita resultado artificial e preserva o handoff de conclusão no fluxo alcançável. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/GupyTestResultMapper.java` | montagem do `TestResult` | Só produz resultado externo compatível quando existe conclusão válida e pontuação final. |

## Regras de manutenção

- Uma entrega só entra neste histórico depois de verificação direta do código e do fluxo alcançável.
- Requisitos parcialmente entregues permanecem no backlog com o mesmo ID ou referência explícita à lacuna remanescente.
- O histórico não registra tarefas de CI/CD, testes, QA, homologação, métricas observacionais, publicação ou marketing.
