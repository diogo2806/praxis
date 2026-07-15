# Requisitos técnicos pendentes — praxis

Status: atualizado em 2026-07-15 após auditoria da branch main.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## Resultado da auditoria

Nenhuma pendência técnica permaneceu comprovada na HEAD auditada.

A antiga pendência `UI10` foi removida porque o fluxo público do dashboard passou a usar exclusivamente o endpoint oficial, representa respostas `404` como incompatibilidade explícita e não fabrica plano, operação ou conexão de integração a partir de tokens, fallbacks ou consultas auxiliares.

## Contexto da auditoria

- Commit auditado da branch principal: `1b231e791fdd22189e6441b35a762b5618f76fd8`.
- Finalidade identificada: plataforma de avaliações situacionais para recrutamento, com critérios explícitos, score determinístico, trilha auditável e integração com ATS.
- Stack principal: Java 21, Spring Boot 3.5, Spring Security, JPA, PostgreSQL/Flyway, React 19, TanStack Start/Router e TypeScript.
- Arquitetura predominante: frontend React consumindo API Spring Boot, persistência PostgreSQL, autenticação JWT nas rotas internas, Bearer token nas integrações e entrega assíncrona por outbox.
- Documentos canônicos verificados: `README.md`, documentação técnica em `docs/`, este backlog e o histórico de requisitos implementados.
