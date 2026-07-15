# Requisitos técnicos pendentes — praxis

Status: atualizado em 2026-07-15 após revalidação funcional da branch `main`.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas meramente observacionais, publicação ou marketing.

## Contexto da auditoria

- Commit auditado da branch principal: `9d3d9c99a3556f597a71180c770ee9801c0832ab`.
- Finalidade identificada: plataforma de avaliações situacionais para recrutamento, com critérios explícitos, score determinístico, trilha auditável e integração com ATS.
- Stack principal: Java 21, Spring Boot 3.5, Spring Security, JPA, PostgreSQL/Flyway, React 19, TanStack Start/Router e TypeScript.
- Arquitetura predominante: frontend React consumindo API Spring Boot, persistência PostgreSQL, autenticação JWT nas rotas internas, Bearer token nas integrações e entrega assíncrona por outbox transacional.
- Fluxos revisados: criação e repetição de tentativas, links diretos, execução do candidato, cálculo e comparação de resultados, callback Gupy, entrega de webhook, outbox, monitoramento operacional, relatórios de engajamento e documentação normativa.
- Os identificadores `INT15`, `INT16` e `ASYNC10` já possuíam entregas históricas registradas. As novas lacunas receberam `INT17`, `INT18` e `ASYNC11` para evitar colisão e preservar rastreabilidade.
- `LEGACY12` foi concluído durante esta normalização: os fatos válidos foram convertidos em requisitos objetivos e `docs/backlog.txt` foi removido como fonte concorrente.

## Resumo das pendências

| ID | Prioridade | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|---|
| `INT17` | Crítica | Impedir envio de eventos proprietários ao `result_webhook_url` da Gupy. | O destino fornecido pela Gupy recebe exclusivamente o `TestResult` contratual; eventos de engajamento são omitidos ou enviados somente por integração genérica explicitamente configurada, sem dados pessoais destinados ao endpoint Gupy. | ⬜ Pendente |
| `INT18` | Crítica | Confirmar o `callback_url` por chamada GET efetiva, persistente e recuperável. | A conclusão agenda uma chamada servidor-servidor ao callback autorizado, persiste tentativas, resposta, erro e confirmação, aplica retentativa/DLQ e não considera a mera apresentação da URL ao navegador como confirmação. | ⬜ Pendente |
| `ASYNC11` | Crítica | Rejeitar tipos desconhecidos no outbox. | Todo tipo não suportado lança erro explícito, permanece fora de `SENT` e segue a política normal de retentativa e DLQ com erro observável. | ⬜ Pendente |
| `DATA13` | Crítica | Separar repetição idempotente de criação legítima de nova tentativa na Gupy. | O sistema distingue repetição equivalente, reapresentação do mesmo resultado, reteste autorizado e nova aplicação após falha, abandono ou expiração, sem transformar uma nova tentativa legítima em `409`. | ⬜ Pendente |
| `DATA14` | Alta | Permitir novas aplicações de links diretos por ciclo, vaga ou comando explícito. | A empresa consegue criar uma nova aplicação para o mesmo candidato e avaliação quando informa um novo ciclo/contexto ou solicita explicitamente nova tentativa; reenvios equivalentes continuam idempotentes. | ⬜ Pendente |
| `BUS12` | Alta | Garantir comparabilidade de pontuação entre caminhos ou bloquear comparações incompatíveis. | Resultados comparados usam uma base comum de competências, pesos e máximos alcançáveis; alternativamente, o backend classifica resultados incompatíveis e o Talent Match impede ou sinaliza claramente a comparação. | ⬜ Pendente |
| `UI13` | Média | Paginar o centro operacional e incluir todos os estados relevantes. | A API e a interface suportam paginação e filtros por estado, incluindo não iniciadas, em andamento, concluídas, abandonadas e expiradas, sem corte silencioso fixo em 200 registros. | ⬜ Pendente |
| `BUS13` | Média | Apresentar horas economizadas como estimativa configurável com metodologia explícita. | Relatórios e telas identificam o valor como estimativa, exibem período, fórmula, parâmetro configurado e ressalva metodológica; nenhuma mensagem trata o número como economia observada sem dados comparativos reais. | ⬜ Pendente |

## 1. Integração Gupy

### INT17 — uso indevido do webhook de resultado

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptService.java` | `publishAttemptEngagementEvent()` | Publica `ATTEMPT_STARTED` e `ATTEMPT_ABANDONED` usando o `resultWebhookUrl` armazenado na tentativa e inclui nome e e-mail da pessoa candidata no payload proprietário. | Remover o destino Gupy desses eventos. Eventos proprietários devem depender exclusivamente de webhook genérico explicitamente configurado e com contrato próprio. |
| `backend/src/main/java/br/com/iforce/praxis/shared/outbox/service/OutboxProcessor.java` | `processAttemptEngagementEvent()` | Valida a URL e envia o payload proprietário com `ResultWebhookClient.postPayload()` para o endereço recebido como webhook de resultado. | Eliminar esse despacho para o destino Gupy e manter o `result_webhook_url` reservado ao `TestResult`. |
| `docs/implementados/requisitos-implementados.md` | registro de `INT11` | A conclusão histórica não correspondia ao fluxo alcançável atual. | Manter a reclassificação e concluir `INT17` antes de voltar a declarar separação integral dos contratos. |

### INT18 — callback sem confirmação servidor-servidor

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/gupy/observability/CandidateCallbackHandoffAdvice.java` | `beforeBodyWrite()` e `record()` | Registra apenas que o callback foi apresentado ao navegador (`callback_presented`). Não executa o GET nem comprova recebimento pelo ATS. | Criar processamento servidor-servidor persistente para o callback e reservar o evento atual apenas como telemetria de apresentação ao navegador. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptService.java` | `redirectUrl()` e respostas de conclusão | Devolve a URL ao frontend, mantendo a navegação do navegador como mecanismo efetivo de retorno. | Publicar evento transacional de callback após conclusão e preservar o redirecionamento apenas como experiência complementar. |
| `backend/src/main/java/br/com/iforce/praxis/shared/outbox/` | processamento assíncrono | Não há estado persistido específico para tentativa, confirmação, erro e DLQ do callback GET. | Adicionar destino/evento próprio, idempotência, validação de URL, timeout, backoff, confirmação HTTP e operação de reprocessamento. |

## 2. Dados e idempotência

### DATA13 — reteste Gupy versus repetição idempotente

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptIdempotencyAspect.java` | `idempotencyKey()` | A chave usa empresa, companhia, documento, teste e vaga, mas não representa uma aplicação ou ciclo de reteste. | Introduzir identificador de aplicação/ciclo contratual ou regra determinística que permita nova tentativa autorizada sem perder idempotência da mesma solicitação. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptIdempotencyAspect.java` | `fingerprint()` | Inclui `previous_result`; a mudança desse campo mantém a mesma chave e produz fingerprint divergente, resultando em `409`. | Separar campos de identidade idempotente dos campos que autorizam novo ciclo e definir transições explícitas para reteste. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/dto/CreateCandidateRequest.java` | `previous_result` | O contrato aceita `fail`, mas o fluxo não o converte em uma nova aplicação legítima. | Usar o campo dentro de uma política documentada de reteste, incluindo estados anteriores aceitos e prevenção de duplicidade concorrente. |

### DATA14 — reaplicação de links diretos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptService.java` | `createCompanyLink()` | A chave usa empresa, e-mail e avaliação; qualquer tentativa anterior é reaproveitada indefinidamente. | Adicionar `applicationCycleId`, vaga/contexto ou comando explícito de nova aplicação, mantendo uma chave separada para reenvio equivalente. |
| API e frontend de criação de link | contrato de criação | Não existe escolha clara entre reenviar o link existente e gerar uma nova tentativa. | Expor ações distintas, validar autorização e apresentar ao usuário o efeito de cada opção. |

## 3. Regras de negócio

### BUS12 — comparabilidade entre caminhos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/ResultScoringService.java` | normalização por caminho | Competências sem máximo positivo no caminho são removidas e os pesos restantes são renormalizados, produzindo bases efetivas distintas. | Definir matriz comum de competências e máximos alcançáveis por versão, ou gerar metadado explícito de assinatura de comparabilidade por caminho. |
| `backend/src/main/java/br/com/iforce/praxis/simulation/service/SimulationValidationService.java` | `validatePathCompetencyCoverage()` | Caminhos sem cobertura de uma competência geram apenas aviso e continuam publicáveis. | Bloquear publicação quando a política exigir comparação comum ou marcar formalmente os grupos de caminhos compatíveis. |
| `frontend/src/routes/talent-match.tsx` | comparação de candidatos | Exibe candidatos no mesmo radar sem comprovar que foram pontuados sobre a mesma base efetiva. | Consumir a informação de comparabilidade do backend e bloquear, separar ou sinalizar comparações incompatíveis. |

### BUS13 — metodologia das horas economizadas

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/engagement/service/EngagementReportService.java` | `hoursSaved = completed * hoursSavedPerEvaluation` | Multiplica conclusões por uma constante e apresenta o resultado como horas economizadas, sem comparação observada. | Nomear o valor como estimativa, incluir fórmula, parâmetro, período e origem metodológica no DTO e na mensagem. |
| `backend/src/main/resources/application.properties` | `praxis.engagement.hours-saved-per-evaluation` | O padrão de 1,5 hora não traz unidade e metodologia explícitas para quem recebe o relatório. | Documentar a hipótese, permitir configuração por empresa ou desativação e impedir linguagem de comprovação quando só houver estimativa. |

## 4. Processamento assíncrono

### ASYNC11 — evento desconhecido marcado como entregue

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/shared/outbox/service/OutboxProcessor.java` | `dispatch()` | Tipos fora dos casos reconhecidos retornam sem erro; `deliverAndFinalize()` marca o evento como `SENT`. | Adicionar ramo padrão que lance `UnsupportedOutboxEventTypeException` com tipo e ID do evento. |
| `backend/src/main/java/br/com/iforce/praxis/shared/outbox/service/OutboxProcessor.java` | `deliverAndFinalize()` | Não diferencia ausência de handler de entrega bem-sucedida. | Manter o evento em retry e encaminhá-lo à DLQ segundo a política existente, preservando erro operacional explícito. |

Comportamento mínimo esperado:

```java
default -> throw new UnsupportedOutboxEventTypeException(event.getEventType());
```

## 5. Operação e interface

### UI13 — paginação e estados do centro operacional

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptService.java` | `listLiveAttempts()` | Consulta somente `IN_PROGRESS` e `COMPLETED` e aplica `PageRequest.of(0, 200)`. | Criar endpoint paginado com filtros de estado, período, avaliação e busca por candidato. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptService.java` | `listCompanyLinks()` | Limita silenciosamente a listagem aos 200 registros mais recentes. | Retornar página, total, cursor ou metadados equivalentes sem corte invisível. |
| `frontend/src/routes/monitoramento.tsx` | centro operacional | Não oferece visão completa de convites não iniciados, abandonos e expirações. | Adicionar filtros, paginação e estados vazios/erros coerentes com a API paginada. |

## Ordem recomendada

1. `INT17` — interromper o uso indevido do webhook Gupy e a exposição de payload proprietário.
2. `INT18` — executar e confirmar o callback por processamento servidor-servidor persistente.
3. `ASYNC11` — impedir perda silenciosa de tipos desconhecidos no outbox.
4. `DATA13` — separar reteste legítimo de repetição idempotente.
5. `DATA14` — permitir nova aplicação explícita em links diretos.
6. `BUS12` — tornar os resultados comparáveis ou bloquear comparações incompatíveis.
7. `UI13` — paginar e completar o centro operacional.
8. `BUS13` — explicitar a metodologia da estimativa de horas economizadas.