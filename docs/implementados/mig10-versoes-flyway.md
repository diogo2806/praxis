# MIG10 — versões exclusivas e compatíveis das migrations Flyway

Status: concluído em 2026-07-20.

## Problema corrigido

O diretório canônico de migrations possuía três scripts SQL com a versão `V1010`:

- persistência de `candidate_token_issued_at`;
- persistência de `candidate_token_expires_at`;
- substituição dos planos Profissional por pacotes anuais.

Além disso, restaurar simplesmente a migration de emissão para `V1009` criaria nova colisão com a migration do módulo de parceiros.

## Histórico preservado

A sequência original comprovada pelo histórico do repositório foi mantida:

1. `V1009__persist_candidate_token_window.sql` criou e preencheu `candidate_token_issued_at`;
2. `V1010__add_candidate_token_expiration.sql` criou `candidate_token_expires_at` e calculou o valor usando `candidate_token_issued_at`.

Os dois arquivos mantiveram o conteúdo SQL original. Dessa forma, ambientes que já registraram essas versões no `flyway_schema_history` continuam encontrando as mesmas versões, descrições e checksums.

## Ordem final

| Versão | Migration | Motivo |
|---|---|---|
| `V1009` | `persist_candidate_token_window` | Preserva a migration histórica que cria `candidate_token_issued_at`. |
| `V1009.1` | `create_partner_distribution_module` | Elimina a colisão posterior em `V1009` e mantém as tabelas de parceiros disponíveis antes da auditoria universal. |
| `V1010` | `add_candidate_token_expiration` | Preserva a migration histórica dependente de `candidate_token_issued_at`. |
| `V1011` | `universal_table_auditing` | Continua auditando as tabelas existentes, inclusive as do módulo de parceiros. |
| `V1012` | `replace_professional_plans_with_annual_pools` | Move a alteração posterior de catálogo para uma versão exclusiva. |

## Proteção contra regressão

`FlywayMigrationCompatibilityTest` passou a validar:

- a unicidade das versões SQL;
- a ordem `1009 < 1009.1 < 1010 < 1011 < 1012` usando `MigrationVersion`;
- a existência dos quatro scripts nos caminhos canônicos;
- a dependência explícita da expiração em `candidate_token_issued_at`.

## Resultado

O Flyway volta a resolver o conjunto de migrations sem versões concorrentes, a criação da emissão ocorre antes do cálculo da expiração e o histórico originalmente aplicável permanece representado no código.
