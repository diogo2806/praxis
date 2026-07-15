# Requisitos técnicos pendentes — praxis

Status: atualizado em 2026-07-15 após auditoria da branch main.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## Contexto da auditoria

- Commit auditado da branch principal: `2c3e118ac797bd52ba02553f53d6f9a298f7e296`.
- Finalidade identificada: plataforma de avaliações situacionais para recrutamento, com critérios explícitos, score determinístico, trilha auditável e integração com ATS.
- Stack principal: Java 21, Spring Boot 3.5, Spring Security, JPA, PostgreSQL/Flyway, React 19, TanStack Start/Router e TypeScript.
- Arquitetura predominante: frontend React consumindo API Spring Boot, persistência PostgreSQL, autenticação JWT nas rotas internas, Bearer token nas integrações e entrega assíncrona por outbox transacional.
- Fluxos revisados: catálogo Gupy, criação idempotente de tentativas, emissão de links públicos, execução do candidato, cálculo e entrega de resultado, callback, página pública de resultado, outbox por destino, proxy SSR das rotas públicas e documentação de compatibilidade.

## 1. Documentação do contrato Gupy

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| INT16 | Alinhar novamente o documento do provedor Gupy à credencial pública de resultado efetivamente implementada na `main`. | O resumo, a matriz de compatibilidade, o exemplo de `TestResult`, a descrição da página pública e o diagrama de fluxo mostram `result_candidate_page_url` com JWT `candidate_result`, TTL próprio e consumo por `CandidateResultPageService`; `INT14` não é mais citado como pendente e a ausência de homologação externa continua explícita. | ⬜ Pendente |

### INT16 — documentação desatualizada após implementação da credencial de resultado

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `docs/INTEGRACAO-GUPY-PROVEDOR.md` | resumo executivo e estado da integração | O documento afirma que `result_candidate_page_url` ainda contém o identificador interno da tentativa e permanece bloqueada por `INT14`. Na `main`, `GupyTestResultMapper.candidateResultPageUrl()` gera JWT específico de resultado e o insere na URL. | Remover a divergência já concluída, registrar a URL assinada como comportamento implementado e manter separada a ressalva de homologação externa. |
| `docs/INTEGRACAO-GUPY-PROVEDOR.md` | matriz de compatibilidade de `result_candidate_page_url` | A matriz descreve `/candidato/{attemptId}/resultado` como incompatível. O mapper atual chama `JwtService.generateCandidateResultToken(empresaId, attemptId, candidateResultTtlHours)` e publica `/candidato/{token}/resultado`. | Atualizar caminho, credencial, validade e estado de compatibilidade técnica conforme o fluxo real. |
| `docs/INTEGRACAO-GUPY-PROVEDOR.md` | exemplo de `TestResult` e explicação subsequente | O JSON usa `att_123` no segmento público e o texto afirma que a consulta falha com `401`. O endpoint alcançável usa `JwtService.parseCandidateResultToken()` e resolve empresa e tentativa a partir do token assinado produzido pelo mapper. | Substituir o identificador cru por um marcador de JWT de resultado e descrever a validação de tipo, assinatura, expiração, empresa e tentativa. |
| `docs/INTEGRACAO-GUPY-PROVEDOR.md` | descrição da página pública e diagrama do fluxo | O texto e o fluxo ainda tratam a página como inacessível por incompatibilidade de credencial. A implementação atual alcança `CandidateResultPageService.findByToken()` com token `candidate_result` e mantém a resposta limitada a avaliação, estado, conclusão e retorno ao ATS. | Documentar o fluxo alcançável e preservar a distinção entre token de execução e token de consulta de resultado. |

## Ordem recomendada

1. `INT16` — corrigir o contrato documental que voltou a representar uma versão anterior da implementação.
