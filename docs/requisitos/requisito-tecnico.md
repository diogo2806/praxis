# Requisitos técnicos pendentes — praxis

Status: atualizado em 2026-07-20 após auditoria da branch `main`.

Commit auditado: `de1e5f6135c0e4f2b427c207d1db9e011a9d01a8`.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Integridade e idempotência

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| ASYNC12 | Tornar atômico o registro idempotente dos eventos de integridade por sessão e número de sequência. | Reenvios simultâneos do mesmo evento não geram erro `500`, persistem no máximo um registro e retornam sucesso idempotente ao cliente. | ⬜ Pendente |

### ASYNC12 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/integrity/service/CandidateIntegrityService.java` | `recordEvent(String, IntegrityEventRequest)` | O método consulta `existsBySessionIdAndSequenceNumber(...)` e depois executa `eventRepository.save(event)` em operações separadas. Duas requisições concorrentes com o mesmo `sessionId` e `sequenceNumber` podem passar pela consulta antes da gravação; a restrição única `uq_integrity_event_sequence` rejeita uma delas e a exceção de integridade é convertida em erro interno, embora o contrato pretenda tratar reenvio como sucesso idempotente. | Substituir o fluxo de consulta seguida de inserção por gravação atômica que ignore conflito da chave `(session_id, sequence_number)`, ou capturar especificamente a violação dessa restrição e concluir com sucesso após confirmar que o registro conflitante pertence à mesma sessão e sequência. Não mascarar outras violações de integridade. |
| `backend/src/main/java/br/com/iforce/praxis/integrity/persistence/repository/CandidateIntegrityEventRepository.java` | `existsBySessionIdAndSequenceNumber(...)` e persistência do evento | A consulta preventiva reduz duplicações sequenciais, mas não garante idempotência sob concorrência. | Disponibilizar operação de inserção idempotente/atômica compatível com PostgreSQL, mantendo a restrição única como fonte final de verdade. |
| `backend/src/main/resources/db/migration/V1013__create_candidate_integrity_telemetry.sql` | `uq_integrity_event_sequence` | A restrição única protege os dados corretamente, mas hoje seu conflito esperado em retry concorrente sobe como falha da requisição. | Preservar a restrição e alinhar o serviço/repositório para tratar apenas o conflito esperado como repetição já processada. |
