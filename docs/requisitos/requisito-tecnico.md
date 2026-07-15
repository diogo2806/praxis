# Requisitos técnicos pendentes — praxis

Status: atualizado em 2026-07-15 após conclusão de `INT15`.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## Contexto da auditoria

- Commit auditado da branch principal: `689bad357105b10d57ce84d56e52ddf9a0141a0a`.
- Finalidade identificada: plataforma de avaliações situacionais para recrutamento, com critérios explícitos, score determinístico, trilha auditável e integração com ATS.
- Stack principal: Java 21, Spring Boot 3.5, Spring Security, JPA, PostgreSQL/Flyway, React 19, TanStack Start/Router e TypeScript.
- Arquitetura predominante: frontend React consumindo API Spring Boot, persistência PostgreSQL, autenticação JWT nas rotas internas, Bearer token nas integrações e entrega assíncrona por outbox transacional.
- Fluxos revisados: catálogo Gupy, criação idempotente de tentativas, emissão de links públicos, execução do candidato, cálculo e entrega de resultado, callback, página pública de resultado, outbox por destino e documentação de compatibilidade.

## 1. Resultado público da integração Gupy

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| INT14 | Emitir uma URL de resultado da pessoa candidata que seja realmente consumível pelo endpoint público e tenha validade própria para consulta posterior. | `result_candidate_page_url` contém credencial assinada aceita por `/candidate/results/{token}`; a consulta não reutiliza ID interno nem depende do TTL curto da execução; conteúdo e escopo permanecem limitados ao candidato. | ⬜ Pendente |

### INT14 — credencial e validade da página pública de resultado

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/GupyTestResultMapper.java` | `candidateResultPageUrl()` e construtor | O mapper publica `/candidato/{attemptId}/resultado`; recebe `JwtService`, mas não o armazena nem gera credencial assinada. O valor produzido é um ID interno `att_...`. | Gerar uma credencial específica de consulta de resultado, com empresa, tentativa, tipo e expiração próprios, e inserir essa credencial na URL publicada. |
| `backend/src/main/java/br/com/iforce/praxis/candidate/service/CandidateResultPageService.java` | `findByToken()` e `parseToken()` | O fluxo alcançável exige `JwtService.parseCandidateResultToken()`. Portanto, a URL atualmente produzida pelo mapper é rejeitada com `401` antes da consulta da tentativa. | Aceitar somente o novo tipo de token de resultado e resolver empresa/tentativa a partir dele; não adicionar fallback para `attemptId` cru. |
| `backend/src/main/java/br/com/iforce/praxis/auth/service/JwtService.java` | `generateCandidateAttemptToken()` e `parseCandidateAttemptToken()` | Existe somente token de execução, com tipo `candidate_attempt` e TTL fornecido pelo fluxo da prova. Não há token separado para consulta histórica do resultado. | Adicionar `novo método sugerido: generateCandidateResultToken()` e parser correspondente, com claim de tipo e TTL independentes do token de execução. |
| `backend/src/main/java/br/com/iforce/praxis/config/PraxisProperties.java` | configuração de validade | A configuração não separa a validade do acesso à prova da validade da página de resultado. | Adicionar configuração explícita para TTL do resultado público e usá-la somente na geração da credencial de consulta. |
| `frontend/src/routes/candidato.$token.resultado.tsx` | parâmetro `$token` | A tela e o cliente HTTP já tratam o parâmetro como token e apresentam somente estado, avaliação, conclusão e retorno ao ATS. | Preservar esse contrato limitado e garantir que links de continuar avaliação não reutilizem token de resultado como token de execução. |

## Ordem recomendada

1. `INT14` — corrigir a URL pública que hoje é rejeitada pelo próprio endpoint.
