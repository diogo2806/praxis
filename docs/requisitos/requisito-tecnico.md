# Requisitos técnicos pendentes — praxis

Status: revalidado em 2026-07-16 contra a implementação atual da `main`.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, homologação externa, QA manual, publicação ou marketing.

## Resultado da revalidação

Nenhuma pendência técnica confirmada permanece nesta auditoria.

As duas tarefas que ainda apareciam como pendentes neste documento já estavam implementadas no código:

| ID | Evidência atual | Estado |
|---|---|---|
| BUS12 | `ComparableSimulationValidationService`, registrada como `@Primary`, bloqueia a publicação quando caminhos da mesma avaliação possuem bases máximas diferentes ou zeradas por competência. | ✅ Concluído |
| UI13 | A central operacional usa consulta paginada, filtros por estado, avaliação e candidato, e contempla tentativas não iniciadas, em andamento, concluídas, abandonadas e expiradas. A listagem de links também possui paginação sem corte silencioso em 200 registros. | ✅ Concluído |

## Correções adicionais desta revalidação

- O contrato Gupy passa a aceitar tanto JSON `null` quanto a string `"null"` em `previous_result`, conforme o exemplo oficial publicado.
- `GET /test` preserva `limit=0`, retornando página vazia com o total disponível, sem forçar o valor para `1`.
- `result_candidate_page_url` apresenta à pessoa candidata somente os resultados `major`, mantendo e-mail, respostas, pesos e regras internas fora da resposta pública.
- Foram adicionados testes de regressão para os três comportamentos.

## Limites externos

A homologação completa da integração Gupy continua dependendo de uma vaga real, porque o provedor não disponibiliza ambiente de sandbox para esse fluxo. Isso é uma validação externa e não uma pendência de implementação comprovada no repositório.
