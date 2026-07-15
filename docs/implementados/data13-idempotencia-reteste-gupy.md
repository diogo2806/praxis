# DATA13 — idempotência e reteste Gupy

Status: implementado em 2026-07-15.

## Contrato considerado

O contrato público da Gupy oferece somente dois valores para `previous_result`:

- ausência ou JSON `null`: primeira aplicação;
- `fail`: autorização para reteste após um resultado anterior.

Como não existe no payload um identificador arbitrário de aplicação ou número de ciclo, o Práxis aplica uma regra determinística com dois ciclos por identidade contratual: ciclo inicial e ciclo de reteste.

A identidade base é formada por:

```text
empresaId | companyId | documentId | testId | jobId (quando informado)
```

A composição continua sendo persistida apenas como SHA-256. O documento da pessoa candidata não é gravado em claro na chave.

## Ciclo inicial

Quando `previous_result` está ausente ou é `null`, a chave histórica é preservada. Repetições equivalentes retornam a mesma tentativa e o mesmo `test_result_id`.

Conteúdo divergente dentro dessa mesma chave continua retornando `409 Conflict`.

## Ciclo de reteste

Quando `previous_result=fail`, a chave recebe um marcador interno de ciclo antes do SHA-256. Isso produz uma identidade diferente da aplicação inicial, mas estável para todas as repetições equivalentes do reteste.

O reteste só pode ser criado quando a tentativa inicial da mesma identidade está em um dos estados:

- `COMPLETED`;
- `ABANDONED`;
- `EXPIRED`.

Um pedido de reteste sem tentativa anterior, ou enquanto a tentativa anterior está `NOT_STARTED` ou `IN_PROGRESS`, retorna `409 Conflict`.

A restrição única existente sobre `candidate_attempts.idempotency_key` permanece responsável por impedir duplicidade em chamadas concorrentes. Se duas requisições equivalentes tentarem criar o reteste ao mesmo tempo, uma cria e a outra reutiliza o registro persistido.

## Fingerprint

O fingerprint versão 2 não inclui `previous_result`, porque esse campo autoriza a mudança de ciclo e já está representado na chave idempotente.

Continuam no fingerprint os campos que precisam permanecer equivalentes dentro do mesmo ciclo:

- empresa, documento, teste e vaga;
- nome e e-mail;
- `callback_url` e `result_webhook_url`;
- multiplicador de acomodação;
- `candidate_type`.

Fingerprints versão 1 continuam aceitos quando correspondem à requisição legada. Após uma repetição equivalente, o registro é atualizado para a versão 2.

## Limite contratual

O contrato atual não diferencia um terceiro ciclo de uma repetição do primeiro reteste. Portanto, ciclos adicionais exigem que a Gupy forneça futuramente um identificador explícito de aplicação/ciclo. Criar ciclos ilimitados apenas com `previous_result=fail` eliminaria a garantia de idempotência.
