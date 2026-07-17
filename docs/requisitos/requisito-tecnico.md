# Requisitos técnicos pendentes — praxis

Status: atualizado em 2026-07-17 após auditoria da branch `main` na HEAD `1cd770e5fbe8cb0c8ee8156d19be5259972ce753`.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## Resultado da auditoria

Nenhuma pendência técnica real, comprovada e implementável permanece aberta na HEAD auditada.

A implementação atual foi considerada fonte da verdade. Foram revalidados os fluxos de criação e execução de tentativas, idempotência, publicação de resultados, separação entre o `result_webhook_url` contratual da Gupy e eventos proprietários da integração `CUSTOM_API`, validação de destinos externos, processamento do outbox, retry, DLQ e apresentação do `callback_url` ao navegador.

A HEAD valida o `result_webhook_url` antes da persistência sem depender de DNS externo, repete a validação completa imediatamente antes da entrega e removeu o callback GET servidor-servidor legado. Limitações de homologação externa da integração Gupy, ausência de sandbox do provedor e atividades de validação operacional não foram incluídas por estarem fora do escopo deste backlog.
