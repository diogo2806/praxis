# DATA16 — gestão da validade do link do candidato

Status: concluído em 2026-07-17.

## Problema

A empresa via apenas o andamento da tentativa e o endereço do link. Não havia informação sobre emissão, vencimento ou dias restantes, nem uma ação explícita para reativar uma credencial expirada.

## Entrega

- persistência de `candidate_token_expires_at` com backfill dos registros existentes;
- separação entre andamento da avaliação e situação do link;
- estados operacionais `active`, `expiringSoon` e `expired`;
- colunas de criação, emissão, vencimento e dias restantes;
- bloqueio de cópia e reenvio quando o link está expirado;
- comando `POST /api/v1/candidate-links/{attemptId}/extend` para acrescentar de 1 a 365 dias;
- reativação a partir do instante atual quando expirado e extensão a partir do vencimento quando ainda ativo;
- lock pessimista, isolamento por empresa, auditoria e ausência de consumo adicional de crédito.

## Regra operacional

Consultar ou listar nunca renova o link. A empresa precisa escolher explicitamente os dias adicionais. A tentativa existente é preservada e nenhum novo ciclo é criado.
