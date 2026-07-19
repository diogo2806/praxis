# Requisitos técnicos pendentes — praxis

Status: atualizado em 2026-07-19 após auditoria da branch `main`.

Commit auditado: `d20c2c4e43bf67d75e08b7f87dc94ab91fc9c0f0`.

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

## 2. Interface e contratos

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| UI14 | Remover a reconstrução local da validade e do estado dos links de candidato quando a API retorna campos ausentes ou inválidos. | A interface usa `linkExpiresAt`, `remainingDays` e `linkStatus` produzidos pelo fluxo persistido do backend; respostas incompatíveis geram falha explícita e não são convertidas em validade artificial de sete dias ou em estado calculado no navegador. | ⬜ Pendente |

### UI14 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `frontend/src/lib/api/candidate-links.ts` | `CandidateLinkPayload`, `normalizeCandidateLink()`, `normalizeLinkExpiresAt()`, `normalizeRemainingDays()` e `normalizeLinkStatus()` | O cliente tornou opcionais os campos obrigatórios do contrato e, quando eles faltam ou são inválidos, fabrica `linkExpiresAt` somando sete dias a `createdAt`, recalcula dias restantes com o relógio do navegador e deriva o status localmente. Isso cria uma segunda fonte de verdade e pode apresentar um link como ativo mesmo quando a janela persistida no backend é diferente ou inexistente. | Manter o contrato de resposta obrigatório, validar os três campos recebidos e propagar erro contratual quando forem ausentes ou inválidos; não reconstruir validade ou status no frontend. |
| `backend/src/main/java/br/com/iforce/praxis/candidate/service/CandidateLinkQueryService.java` | `toResponse()` | O fluxo oficial obtém a janela por `CandidateTokenWindowService`, gera o token com `issuedAt` e `expiresAt` persistidos e devolve `linkExpiresAt`, `remainingDays` e `linkStatus` completos. | Preservar este fluxo como fonte única da validade e garantir que falhas ao obter a janela não sejam escondidas pela interface. |
| `backend/src/main/java/br/com/iforce/praxis/candidate/dto/CandidateLinkResponse.java` | contrato `linkExpiresAt`, `remainingDays` e `linkStatus` | O DTO declara os campos como parte do contrato de resposta; a normalização introduzida no frontend enfraquece esse contrato sem alterar a API. | Manter os campos obrigatórios no contrato consumido pela interface e tratar incompatibilidade de payload como erro observável. |
