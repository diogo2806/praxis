# Requisitos técnicos pendentes — praxis

Status: atualizado em 2026-07-19 após conclusão de `UI14`.

Commit auditado: `42d9457fd257c83e6ee346fd32e1c59a01206b36`.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Inicialização e migrations

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| MIG10 | Eliminar a duplicidade da versão `V1010` em três migrations Flyway e preservar a dependência entre a emissão e a expiração do token de candidato. | Cada migration possui versão exclusiva; a migration que cria `candidate_token_issued_at` executa antes da migration que calcula `candidate_token_expires_at`; o histórico aplicado é preservado e o Flyway valida o conjunto sem conflito. | ⬜ Pendente |

### MIG10 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/resources/db/migration/V1010__persist_candidate_token_window.sql` | versão Flyway `V1010`; coluna `candidate_token_issued_at` | Cria e preenche `candidate_token_issued_at`, mas compartilha a versão `V1010` com outras duas migrations. A inclusão posterior da migration Java `V1011__universal_table_auditing` não resolve o conflito de validação existente em `V1010`. | Definir versão exclusiva conforme o histórico real de `flyway_schema_history`, preservando esta migration antes da criação de `candidate_token_expires_at`. |
| `backend/src/main/resources/db/migration/V1010__add_candidate_token_expiration.sql` | versão Flyway `V1010`; coluna `candidate_token_expires_at` | Usa `candidate_token_issued_at` para calcular a expiração, porém possui a mesma versão da migration que cria essa coluna; o Flyway rejeita migrations versionadas concorrentes antes de alcançar migrations posteriores. | Atribuir versão exclusiva posterior à migration de `candidate_token_issued_at`, mantendo o preenchimento e o índice após a coluna dependente existir. |
| `backend/src/main/resources/db/migration/V1010__replace_professional_plans_with_annual_pools.sql` | versão Flyway `V1010`; catálogo `subscription_plans` | Altera os planos anuais com a mesma versão das duas migrations de token, criando três descrições distintas para `V1010`. | Atribuir uma versão exclusiva compatível com os ambientes já migrados, sem renomear arbitrariamente uma migration registrada no histórico persistente. |
