# Requisitos técnicos pendentes — praxis

Status: atualizado em 2026-07-15 após conclusão de `ASYNC11`, `BUS13`, `INT18`, `DATA13` e `DATA14`.

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

### INT17 — uso indevido do webhook de resultado

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptService.java` | `publishEngagementTransitionIfNeeded()` e `publishAttemptEngagementEvent()` | Transições para início e abandono publicam `ATTEMPT_STARTED` e `ATTEMPT_ABANDONED` usando `CandidateAttemptEntity.resultWebhookUrl`. O payload inclui nome e e-mail do candidato. | Reservar `result_webhook_url` ao resultado oficial. Omitir esses eventos para Gupy ou encaminhá-los somente por canal genérico explicitamente configurado e com contrato próprio. |
| `backend/src/main/java/br/com/iforce/praxis/shared/outbox/service/OutboxProcessor.java` | `dispatch()` e `processAttemptEngagementEvent()` | O processador reconhece os eventos proprietários, valida a URL e envia `eventPayload` por HTTP ao endereço armazenado como webhook de resultado. | Remover esse despacho para o destino Gupy e impedir que eventos internos reutilizem o contrato de `TestResult`. |

## 2. Regras de negócio

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| BUS12 | Garantir comparabilidade de pontuação entre caminhos ou bloquear comparações incompatíveis. | Resultados comparados usam base comum de competências, pesos e máximos alcançáveis; alternativamente, o backend classifica incompatibilidade e o Talent Match bloqueia ou sinaliza claramente a comparação. | ⬜ Pendente |

### BUS12 — comparabilidade entre caminhos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/ResultScoringService.java` | `calculate()` | O máximo é somado por nó do caminho percorrido usando a melhor alternativa daquele nó; competências sem máximo positivo são removidas e os pesos restantes são renormalizados. Caminhos diferentes podem produzir bases efetivas diferentes. | Definir matriz comum de competências e máximos por versão ou gerar assinatura explícita de comparabilidade por caminho. |
| `backend/src/main/java/br/com/iforce/praxis/simulation/service/SimulationValidationService.java` | `validatePathCompetencyCoverage()` | A cobertura desigual entre caminhos não impede necessariamente a publicação. | Bloquear publicação quando a política exigir base comum ou registrar formalmente grupos de caminhos comparáveis. |
| `frontend/src/routes/talent-match.tsx` | consulta e exibição do Talent Match | Seleciona e exibe candidatos da mesma avaliação sem validar assinatura comum de competências, pesos e máximos efetivos. | Consumir metadado de comparabilidade e bloquear, separar ou sinalizar resultados incompatíveis. |

## 3. Operação e interface

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
2. `BUS12` — tornar resultados comparáveis ou bloquear comparações incompatíveis.
3. `UI13` — paginar e completar o centro operacional.
