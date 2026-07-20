# Requisitos técnicos pendentes — praxis

Status: atualizado em 2026-07-20 após correção da branch `main`.

Commit auditado: `653e3574f03a7c08fefa8b30fa37d220e09399ea`.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

Nenhuma pendência técnica implementável permanece registrada após a correção da idempotência concorrente dos eventos de integridade.

## Histórico concluído

| ID | Tarefa técnica | Implementação | Status |
|---|---|---|---|
| ASYNC12 | Tornar atômico o registro idempotente dos eventos de integridade por sessão e número de sequência. | A persistência sequenciada usa `INSERT ... ON CONFLICT DO NOTHING` apoiado pela restrição `uq_integrity_event_sequence`. Reenvios simultâneos retornam sucesso sem duplicar o evento e sem mascarar outras violações de integridade. | ✅ Concluído |
