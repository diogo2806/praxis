# Requisitos técnicos pendentes — praxis

Status: revalidado em 2026-07-15 sobre a `main` no commit `2519605f74776905944ac01002b222908f926a8e`.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, QA, homologação externa, métricas observacionais, publicação, marketing ou refatoração estética.

## Contexto da auditoria

- Finalidade: plataforma de avaliações situacionais para recrutamento, com versionamento, pontuação determinística, revisão humana, trilha auditável e integrações com ATS.
- Stack principal: Java 21, Spring Boot 3.5, Spring Security, JPA, PostgreSQL/Flyway, React 19, TanStack Start/Router e TypeScript.
- Arquitetura: frontend React consumindo API Spring Boot; autenticação JWT nas rotas internas; Bearer token nas integrações; persistência PostgreSQL e entrega assíncrona por outbox transacional.
- A implementação alcançável foi tratada como fonte da verdade; documentos foram usados como referência secundária.

## Resultado

Não há pendência técnica canônica remanescente entre os itens revalidados nesta rodada.

| ID | Situação comprovada | Evidência principal |
|---|---|---|
| BUS12 | Concluído. Publicações com bases máximas diferentes por competência são bloqueadas antes de chegarem ao Talent Match. | `ComparableSimulationValidationService` adiciona bloqueador por competência e `ComparableSimulationValidationServiceTest` cobre caminhos incompatíveis. |
| UI13 | Concluído. O centro operacional e a consulta de links possuem paginação, filtros e todos os estados relevantes. O endpoint legado de links percorre todas as páginas e não corta silenciosamente em 200 registros. | `CandidateAttemptMonitoringQueryService`, `CandidateLinkQueryService`, `CandidateLinkQueryController` e `LegacyCandidateLinkQueryService`. |
| DATA14 | Revalidado. Novas aplicações públicas exigem `applicationCycleId`; repetição equivalente reutiliza apenas o mesmo ciclo e reenvio possui comando próprio. | `CreateCandidateLinkRequest`, `CompanyCandidateLinkService` e `CompanyCandidateLinkController`. |

## Regras de manutenção

- Uma nova pendência só deve ser registrada depois de verificação direta do código e do fluxo alcançável.
- Uma conclusão histórica invalidada por nova evidência deve ser reaberta com caminho, método e comportamento reproduzível.
- `docs/backlog.txt` permanece removido e não deve voltar a concorrer com este arquivo.
