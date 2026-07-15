# Requisitos técnicos pendentes — praxis

Status: atualizado em 2026-07-15 após conclusão de `INT16`.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## Contexto da auditoria

- Commit auditado da branch principal: `2c3e118ac797bd52ba02553f53d6f9a298f7e296`.
- Finalidade identificada: plataforma de avaliações situacionais para recrutamento, com critérios explícitos, score determinístico, trilha auditável e integração com ATS.
- Stack principal: Java 21, Spring Boot 3.5, Spring Security, JPA, PostgreSQL/Flyway, React 19, TanStack Start/Router e TypeScript.
- Arquitetura predominante: frontend React consumindo API Spring Boot, persistência PostgreSQL, autenticação JWT nas rotas internas, Bearer token nas integrações e entrega assíncrona por outbox transacional.
- Fluxos revisados: catálogo Gupy, criação idempotente de tentativas, emissão de links públicos, execução do candidato, cálculo e entrega de resultado, callback, página pública de resultado, outbox por destino, proxy SSR das rotas públicas e documentação de compatibilidade.

## Pendências técnicas

Nenhuma pendência técnica implementável permanece nesta auditoria após o alinhamento da documentação do contrato Gupy à credencial pública `candidate_result` efetivamente implementada.

## Ordem recomendada

Não há item técnico pendente neste backlog. A homologação com uma vaga real da Gupy permanece como validação externa e não como implementação de código ou documentação.
