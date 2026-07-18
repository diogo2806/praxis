# Requisitos técnicos pendentes — praxis

Status: atualizado em 2026-07-18 após auditoria da branch `main`.

Commit auditado: `bba6e352b48a356757e7d27bdd66bb20003637a2`.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Inicialização e migrations

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| MIG10 | Eliminar a duplicidade da versão `V1010` nas migrations Flyway. | Cada migration possuir uma versão exclusiva, preservando a ordem já aplicada e permitindo que o Flyway valide e execute o conjunto sem conflito de versão. | ⬜ Pendente |

### MIG10 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/resources/db/migration/V1010__persist_candidate_token_window.sql` | versão Flyway `V1010` | A migration usa a mesma versão da migration de planos anuais. O Flyway exige uma única migration versionada por versão e interrompe a validação quando encontra descrições distintas para `V1010`. | Renomear esta migration ou a migration concorrente para a próxima versão livre, sem alterar a versão de uma migration que já tenha sido aplicada em ambiente persistente. |
| `backend/src/main/resources/db/migration/V1010__replace_professional_plans_with_annual_pools.sql` | versão Flyway `V1010` | A migration concorre com `V1010__persist_candidate_token_window.sql`, impedindo que o conjunto seja validado de forma determinística na inicialização. | Definir uma versão exclusiva conforme o histórico real de `flyway_schema_history` e manter a ordem necessária entre a inclusão de `candidate_token_issued_at` e a substituição dos planos profissionais. |
