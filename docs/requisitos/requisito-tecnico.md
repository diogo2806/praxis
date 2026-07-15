# Requisitos técnicos pendentes — praxis

Status: atualizado em 2026-07-15 após conclusão de `BUS13`.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## Contexto da auditoria

- Commit auditado da branch principal: `8a499412482fa956d0809c9fd40841cbfb047c79`.
- Finalidade identificada: plataforma de avaliações situacionais para recrutamento, com versionamento, pontuação determinística, revisão humana, trilha auditável e integrações com ATS.
- Stack principal: Java 21, Spring Boot 3.5, Spring Security, JPA, PostgreSQL/Flyway, React 19, TanStack Start/Router e TypeScript.
- Arquitetura predominante: frontend React consumindo API Spring Boot; persistência PostgreSQL; autenticação JWT nas rotas internas; Bearer token nas integrações; entrega assíncrona por outbox transacional.
- Fluxos de código revalidados: criação e repetição de tentativas, links diretos, execução do candidato, cálculo e comparação de resultados, callback Gupy, entrega de webhook, processamento do outbox, monitoramento operacional e relatórios de engajamento.
- Os Markdown foram tratados apenas como referência secundária. A classificação abaixo foi mantida após leitura da implementação alcançável na `main`.
- `LEGACY12` permanece concluído: `docs/backlog.txt` foi removido e não é fonte normativa.

## 1. Integração Gupy

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| INT17 | Impedir envio de eventos proprietários ao `result_webhook_url` da Gupy. | O destino fornecido pela Gupy recebe exclusivamente o `TestResult` contratual; eventos internos de engajamento não são enviados a esse endpoint nem carregam dados pessoais para ele. | ⬜ Pendente |
| INT18 | Confirmar o `callback_url` por chamada GET efetiva, persistente e recuperável. | A conclusão agenda chamada servidor-servidor ao callback autorizado, persiste tentativas, resposta, erro e confirmação, aplica retentativa/DLQ e não considera a mera apresentação da URL ao navegador como confirmação. | ⬜ Pendente |

### INT17 — uso indevido do webhook de resultado

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptService.java` | `publishEngagementTransitionIfNeeded()` e `publishAttemptEngagementEvent()` | Transições para início e abandono publicam `ATTEMPT_STARTED` e `ATTEMPT_ABANDONED` usando `CandidateAttemptEntity.resultWebhookUrl`. O payload inclui nome e e-mail do candidato. | Reservar `result_webhook_url` ao resultado oficial. Omitir esses eventos para Gupy ou encaminhá-los somente por canal genérico explicitamente configurado e com contrato próprio. |
| `backend/src/main/java/br/com/iforce/praxis/shared/outbox/service/OutboxProcessor.java` | `dispatch()` e `processAttemptEngagementEvent()` | O processador reconhece os eventos proprietários, valida a URL e envia `eventPayload` por HTTP ao endereço armazenado como webhook de resultado. | Remover esse despacho para o destino Gupy e impedir que eventos internos reutilizem o contrato de `TestResult`. |

### INT18 — callback sem confirmação servidor-servidor

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/gupy/observability/CandidateCallbackHandoffAdvice.java` | `beforeBodyWrite()` e `record()` | Registra `callback_presented` quando a URL é devolvida ao navegador. Não executa o GET e não comprova recebimento pelo ATS. | Manter esse registro apenas como telemetria de apresentação e criar confirmação servidor-servidor independente. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptService.java` | `redirectUrl()` e respostas de conclusão | A URL é devolvida ao frontend; a navegação do navegador continua sendo o mecanismo efetivo de retorno. | Publicar evento transacional após conclusão para executar o callback, mantendo o redirecionamento apenas como experiência complementar. |
| `backend/src/main/java/br/com/iforce/praxis/shared/outbox/` | novo fluxo sugerido: `GUPY_CALLBACK_CONFIRMATION` | Não existe evento persistido específico com tentativa, código HTTP, confirmação, erro e reprocessamento do GET. | Adicionar processamento idempotente com validação de URL, timeout, backoff, confirmação HTTP, DLQ e reprocessamento operacional. |

## 2. Processamento assíncrono

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| ASYNC11 | Rejeitar tipos desconhecidos no outbox. | Todo tipo não suportado gera erro explícito, não recebe status `SENT` e segue retentativa e DLQ com erro observável. | ⬜ Pendente |

### ASYNC11 — evento desconhecido marcado como entregue

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/shared/outbox/service/OutboxProcessor.java` | `dispatch()` | O método retorna normalmente para tipos diferentes de `RESULT_READY`, `ATTEMPT_STARTED` e `ATTEMPT_ABANDONED`. | Adicionar ramo padrão que lance exceção específica com o tipo e o ID do evento. |
| `backend/src/main/java/br/com/iforce/praxis/shared/outbox/service/OutboxProcessor.java` | `deliverAndFinalize()` | Qualquer retorno normal de `dispatch()` é persistido como `SENT`. | Manter tipos desconhecidos em retry e encaminhá-los à DLQ pela política existente, preservando `lastError`. |

## 3. Dados, aplicação e idempotência

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| DATA13 | Separar repetição idempotente de criação legítima de nova tentativa na Gupy. | O sistema distingue reenvio equivalente, retomada da mesma tentativa, reteste autorizado e nova aplicação após falha, abandono ou expiração, sem transformar nova tentativa legítima em `409`. | ⬜ Pendente |
| DATA14 | Permitir novas aplicações de links diretos por ciclo, vaga ou comando explícito. | A empresa consegue criar nova aplicação para o mesmo candidato e avaliação ao informar novo ciclo/contexto ou solicitar nova tentativa; reenvios equivalentes continuam idempotentes. | ⬜ Pendente |

### DATA13 — reteste Gupy versus repetição idempotente

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptIdempotencyAspect.java` | `idempotencyKey()` | A chave usa empresa, companhia, documento, teste e vaga, mas não representa aplicação ou ciclo de reteste. | Introduzir identificador de aplicação/ciclo contratual ou regra determinística equivalente. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptIdempotencyAspect.java` | `fingerprint()` | `previous_result` participa do fingerprint, mas não da chave. A mesma chave com alteração desse campo resulta em `409`. | Definir transições explícitas que usem `previous_result` para autorizar novo ciclo sem enfraquecer a idempotência da mesma requisição. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptService.java` | `createOrReuse()` | O serviço também deriva a chave por empresa, companhia, documento, teste e `job_id`, reaproveitando a tentativa encontrada. | Centralizar uma única regra de identidade da aplicação e impedir divergência entre aspecto e serviço. |

### DATA14 — reaplicação de links diretos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptService.java` | `createCompanyLink()` | A chave usa empresa, e-mail e avaliação; qualquer tentativa anterior é reaproveitada indefinidamente. | Adicionar `applicationCycleId`, vaga/contexto ou comando explícito de nova aplicação, mantendo chave separada para reenvio equivalente. |
| API e frontend de criação de link | contrato de criação | Não há escolha explícita entre reenviar link existente e criar nova tentativa. | Expor ações distintas, validar autorização e informar o efeito da operação. |

## 4. Regras de negócio

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| BUS12 | Garantir comparabilidade de pontuação entre caminhos ou bloquear comparações incompatíveis. | Resultados comparados usam base comum de competências, pesos e máximos alcançáveis; alternativamente, o backend classifica incompatibilidade e o Talent Match bloqueia ou sinaliza claramente a comparação. | ⬜ Pendente |

### BUS12 — comparabilidade entre caminhos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/ResultScoringService.java` | `calculate()` | O máximo é somado por nó do caminho percorrido usando a melhor alternativa daquele nó; competências sem máximo positivo são removidas e os pesos restantes são renormalizados. Caminhos diferentes podem produzir bases efetivas diferentes. | Definir matriz comum de competências e máximos por versão ou gerar assinatura explícita de comparabilidade por caminho. |
| `backend/src/main/java/br/com/iforce/praxis/simulation/service/SimulationValidationService.java` | `validatePathCompetencyCoverage()` | A cobertura desigual entre caminhos não impede necessariamente a publicação. | Bloquear publicação quando a política exigir base comum ou registrar formalmente grupos de caminhos comparáveis. |
| `frontend/src/routes/talent-match.tsx` | consulta e exibição do Talent Match | Seleciona e exibe candidatos da mesma avaliação sem validar assinatura comum de competências, pesos e máximos efetivos. | Consumir metadado de comparabilidade e bloquear, separar ou sinalizar resultados incompatíveis. |

## 5. Operação e interface

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| UI13 | Paginar o centro operacional e incluir todos os estados relevantes. | API e interface suportam paginação e filtros por estado, incluindo não iniciadas, em andamento, concluídas, abandonadas e expiradas, sem corte silencioso fixo em 200 registros. | ⬜ Pendente |

### UI13 — paginação e estados do centro operacional

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptService.java` | `listLiveAttempts()` | Consulta somente `IN_PROGRESS` e `COMPLETED` com `PageRequest.of(0, 200)`. | Criar endpoint paginado com filtros por estado, período, avaliação e candidato. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptService.java` | `listCompanyLinks()` | Limita a listagem aos 200 registros mais recentes e retorna apenas lista, sem total ou cursor. | Retornar página, total, cursor ou metadados equivalentes, sem corte invisível. |
| `frontend/src/routes/monitoramento.tsx` | centro operacional | A interface depende do conjunto parcial devolvido pela API e não oferece visão completa de convites não iniciados, abandonos e expirações. | Adicionar filtros, paginação e estados coerentes com a API paginada. |

## Ordem recomendada

1. `INT17` — interromper o uso indevido do webhook Gupy e a exposição de payload proprietário.
2. `INT18` — executar e confirmar o callback por processamento servidor-servidor persistente.
3. `ASYNC11` — impedir perda silenciosa de tipos desconhecidos no outbox.
4. `DATA13` — separar reteste legítimo de repetição idempotente.
5. `DATA14` — permitir nova aplicação explícita em links diretos.
6. `BUS12` — tornar resultados comparáveis ou bloquear comparações incompatíveis.
7. `UI13` — paginar e completar o centro operacional.
