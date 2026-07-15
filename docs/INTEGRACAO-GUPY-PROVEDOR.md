# Integração Praxis como provedor de testes da Gupy

> **Propósito:** documentar o comportamento realmente implementado e comparar esse comportamento com o contrato oficial de provedores externos da Gupy.
>
> **Estado em 15/07/2026:** contrato de entrada alinhado para `company_id`, `document_id`, `candidate_type` e `previous_result`. A integração ainda não deve ser declarada homologada antes da validação dos campos extras do resultado e da execução em uma vaga real da Gupy.

Fonte oficial usada na revisão:

- https://developers.gupy.io/docs/integra%C3%A7%C3%A3o-com-testes-de-provedores-externos

## Resumo executivo

O Praxis expõe:

- `GET /test` para listar avaliações publicadas;
- `POST /test/candidate` para criar ou reutilizar uma tentativa;
- `GET /test/result/{resultId}` para consultar o resultado;
- entrega assíncrona para `result_webhook_url` por outbox.

O fluxo interno implementa `callback_url`, `job_id`, retorno assíncrono, redirecionamento final, consulta oficial do resultado e páginas web reais para recrutador e candidato. O limite de entrada do `POST /test/candidate` recebe os identificadores oficiais como `int64`, valida faixa e enums antes do caso de uso, preserva pertencimento ao token e mantém uma identidade idempotente canônica.

## Compatibilidade com o contrato oficial

| Item do contrato Gupy | Implementação atual | Estado |
| --- | --- | --- |
| Bearer token no cabeçalho `Authorization` | Validado por `IntegrationAuthService` contra `integration_tokens` | Compatível |
| `GET /test` com `searchString`, `offset` e `limit` | Implementado; `limit` é normalizado entre 1 e 400 | Compatível |
| Resposta `TestItems` com `limit`, `offset`, `total_tests` e `payload` | Implementada | Compatível |
| `POST /test/candidate` | Implementado com validação do contrato antes do fluxo | Compatível |
| `name`, `email`, `document_id`, `test_id`, `company_id` | `document_id` e `company_id` são recebidos como inteiros JSON `int64` positivos | Compatível |
| `callback_url` obrigatório | Recebido, validado, persistido e devolvido ao navegador após conclusão | Compatível tecnicamente |
| `job_id` | Recebido e persistido; também participa da idempotência quando informado | Compatível |
| `candidate_type` | Enum de entrada restrito a `internal` e `external`; ausência e `null` são aceitos | Compatível |
| `previous_result` | Aceita `fail`, ausência, `null` JSON e o valor textual `null` publicado pela Gupy; rejeita `none` e valores desconhecidos | Compatível |
| `result_webhook_url` | Recebido como `URI`; resultado é enviado por POST | Compatível |
| Resposta `201` com `test_result_id` e `test_url` | Implementada | Compatível |
| `GET /test/result/{resultId}` somente com `resultId` | Implementado sem query adicional; empresa resolvida pelo token | Compatível |
| Callback GET após conclusão | O frontend navega para `callback_url`, fazendo o GET no navegador da pessoa candidata | Compatível tecnicamente |
| Redirecionamento do candidato de volta à Gupy | Executado automaticamente após a resposta final | Compatível tecnicamente |
| Payload `TestResult` | Campos principais são produzidos | Parcial |
| Status `notStarted`, `paused`, `done` | Implementado | Compatível |
| Resultado numérico de 0 a 100 | Implementado por competência | Compatível |
| `result_page_url` para recrutador | Aponta para `/results/{attemptId}`, página autenticada com competências, respostas e decisão humana | Compatível |
| `result_candidate_page_url` para candidato | Aponta para `/candidato/{token}/resultado`, página assinada e limitada a status, avaliação e retorno ao ATS | Compatível |
| Campos extras no resultado | Envia `reliabilityLevel` e `other_informations` no topo | Exige validação com a Gupy; não fazem parte do schema oficial publicado |

## Autenticação real

Todas as rotas `/test/**` exigem:

```text
Authorization: Bearer <token>
```

Fluxo:

1. `IntegrationAuthService` calcula o SHA-256 do token recebido.
2. O hash é codificado em Base64URL sem padding.
3. O hash precisa existir na tabela `integration_tokens` para o provider `gupy`.
4. A empresa e o `company_id` são resolvidos a partir desse registro.

O token é gerado pela Central de Integrações, usando os endpoints internos de integração. O valor em claro é retornado uma única vez; somente o hash é persistido.

O `company_id` cadastrado para a empresa integrada à Gupy deve conter a representação decimal positiva do identificador `int64` informado pela Gupy. O endpoint converte o inteiro recebido para essa forma canônica antes de validar o pertencimento ao token.

`PRAXIS_INTEGRATION_TOKEN` não é usado por `/test/**`. A autenticação é feita pelo token cadastrado no banco.

## Contrato implementado

### `GET /test`

```text
GET /test?searchString=<texto>&offset=0&limit=50
Authorization: Bearer <token>
```

Regras:

- `searchString`: opcional;
- `offset`: padrão `0`, valores negativos viram `0`;
- `limit`: padrão `50`, normalizado entre `1` e `400`;
- somente avaliações publicadas da empresa do token são retornadas.

Exemplo:

```json
{
  "limit": 50,
  "offset": 0,
  "total_tests": 1,
  "payload": [
    {
      "id": "sim-atendimento",
      "name": "Atendimento em situação crítica",
      "category": "Situational Judgment",
      "description": "Avaliação comportamental determinística.",
      "level": "advanced"
    }
  ]
}
```

### `POST /test/candidate`

Body aceito no contrato Gupy:

```json
{
  "company_id": 1,
  "document_id": 4398157034,
  "test_id": "sim-atendimento",
  "name": "Candidato Teste",
  "email": "candidato@example.com",
  "job_id": 100,
  "callback_url": "https://integracao.gupy.example/candidate-return",
  "result_webhook_url": "https://integracao.gupy.example/webhook",
  "accommodation_time_multiplier": 1.5,
  "candidate_type": "external",
  "previous_result": null
}
```

Campos:

| Campo | Obrigatório no código | Observação |
| --- | --- | --- |
| `company_id` | Sim | Inteiro JSON `int64` positivo. A forma decimal canônica deve ser igual ao `company_id` associado ao token. Strings, zero, negativos e valores fora de `int64` são rejeitados antes do fluxo. |
| `document_id` | Sim | Inteiro JSON `int64` positivo. A forma decimal canônica participa da chave idempotente. |
| `test_id` | Sim | Deve identificar avaliação publicada da mesma empresa. |
| `name` | Sim | Nome da pessoa candidata. |
| `email` | Sim | Validado como e-mail. |
| `job_id` | Não | Identificador da vaga; quando informado, diferencia a chave idempotente. |
| `callback_url` | Sim | URL absoluta HTTP(S), persistida para o retorno final à Gupy. |
| `result_webhook_url` | Não | Se presente, recebe `TestResult` por POST. |
| `accommodation_time_multiplier` | Não | Extensão própria para acessibilidade. |
| `candidate_type` | Não | Aceita somente `internal`, `external`, ausência ou `null`. |
| `previous_result` | Não | Aceita `fail`, ausência, `null` JSON ou o texto `null`; não aceita `none`. |

A desserialização e a Bean Validation rejeitam identificadores em formato incorreto, ausentes, não positivos ou fora da faixa de `long`, além de enums desconhecidos, antes de `CandidateAttemptService.createOrReuse()` iniciar a resolução da simulação ou a persistência.

O fluxo compartilhado com a Recrutei mantém o contrato textual próprio desse provedor por meio de um construtor interno explícito. Esse caminho não participa da desserialização do endpoint Gupy e não altera o tipo público de `company_id` ou `document_id`.

Após a conclusão, a API pública devolve `redirectUrl` à tela. O frontend navega diretamente para a `callback_url` recebida da Gupy, fazendo o GET final no navegador da pessoa candidata.

Resposta:

```json
{
  "test_url": "https://app.exemplo.com/candidato/<token-publico-da-tentativa>",
  "test_result_id": "res_123"
}
```

A idempotência usa o hash de:

```text
empresaId | companyId decimal canônico | documentId decimal canônico | testId | jobId (quando informado)
```

Como os identificadores são desserializados como `Long` e convertidos uma única vez com `Long.toString`, chamadas equivalentes não produzem identidades distintas por espaços, sinal positivo, zeros à esquerda ou outra representação textual. Chamadas repetidas com a mesma combinação reutilizam a tentativa existente; vagas diferentes continuam produzindo tentativas distintas.

### `GET /test/result/{resultId}`

Implementação atual:

```text
GET /test/result/res_123
Authorization: Bearer <token>
```

O backend valida:

- Bearer token;
- empresa e `company_id` associados ao token;
- propriedade do resultado pela empresa autenticada;
- existência do resultado.

O endpoint não recebe parâmetros de query. O isolamento permanece garantido pelo token, pelo `empresaId` e pelo `companyId` resolvidos em `integration_tokens`.

## Resultado produzido

Campos principais:

```json
{
  "title": "Nome da avaliação",
  "testCode": "sim-atendimento",
  "description": "Descrição da avaliação",
  "providerName": "Praxis",
  "company_result_string": "Resultado em Markdown para o RH",
  "providerLink": "https://app.exemplo.com",
  "status": "done",
  "result_page_url": "https://app.exemplo.com/results/att_123",
  "result_candidate_page_url": "https://app.exemplo.com/candidato/<token-assinado>/resultado",
  "reliabilityLevel": "NORMAL",
  "other_informations": {
    "timeout_count": 0,
    "situational_omission_count": 0
  },
  "results": [
    {
      "score": 73,
      "result_string": "73%",
      "type_result": "percentage",
      "tier": "major",
      "title": "Comunicação",
      "description": "Pontuação da competência Comunicação.",
      "date": "2026-07-12T12:00:00Z",
      "other_informations": {}
    }
  ]
}
```

`result_page_url` abre a página autenticada do recrutador. `result_candidate_page_url` usa token assinado e abre uma página separada que não expõe pontuação, respostas, e-mail ou regras internas; ela mostra apenas o estado da participação, a avaliação e o retorno ao processo seletivo.

Mapeamento de status:

| Estado interno | Status Gupy |
| --- | --- |
| `NOT_STARTED` | `notStarted` |
| `IN_PROGRESS` | `paused` |
| `COMPLETED` | `done` |
| `ABANDONED` | `done` |
| `EXPIRED` | `done` |

## Fluxo atual

```mermaid
sequenceDiagram
  participant Gupy
  participant Praxis
  participant Candidato
  participant Outbox

  Gupy->>Praxis: GET /test
  Praxis-->>Gupy: avaliações publicadas
  Gupy->>Praxis: POST /test/candidate
  Praxis-->>Gupy: test_url + test_result_id
  Candidato->>Praxis: abre /candidato/{token}
  Candidato->>Praxis: envia respostas
  Praxis->>Praxis: calcula score determinístico
  Praxis->>Outbox: grava RESULT_READY
  Outbox->>Gupy: POST result_webhook_url
  Gupy->>Praxis: GET /test/result/{resultId}
  Gupy-->>Recrutador: abre /results/{attemptId}
  Gupy-->>Candidato: abre /candidato/{token}/resultado
```

Fluxo de callback e redirecionamento implementado:

```mermaid
sequenceDiagram
  participant Candidato
  participant Praxis
  participant Gupy

  Candidato->>Praxis: conclui teste
  Praxis-->>Candidato: redirectUrl após a resposta final
  Candidato->>Gupy: GET callback_url
```

## Outbox e entrega assíncrona

Estados:

- `pending`;
- `processing`;
- `retrying`;
- `sent`;
- `dlq`.

Backoff:

| Tentativa | Próxima tentativa |
| --- | --- |
| 1 | 1 segundo |
| 2 | 4 segundos |
| 3 | 16 segundos |
| 4 | 64 segundos |
| 5 | DLQ |

Tratamento de erro:

- HTTP 4xx vai para DLQ imediatamente, exceto `408` e `429`;
- `408`, `429`, erros 5xx, rede, DNS e falhas transitórias entram em retry;
- após cinco tentativas, o evento vai para DLQ;
- o processamento reivindica lotes de até 100 eventos;
- eventos presos em `PROCESSING` por mais de cinco minutos podem ser retomados.

Monitoramento:

```text
GET  /api/v1/gupy/result-deliveries
GET  /api/v1/gupy/result-deliveries/ready
POST /api/v1/gupy/result-deliveries/process-ready
POST /api/v1/gupy/result-deliveries/{deliveryId}/reprocess
```

## Bloqueadores para homologação

1. Validar com a Gupy se campos extras no `TestResult` são aceitos ou removê-los do contrato externo.
2. Executar homologação em vaga real, pois a própria documentação da Gupy informa que não há ambiente de sandbox para esse fluxo.

## Checklist de validação

- [ ] Gerar token Gupy pela Central de Integrações.
- [ ] Validar `GET /test` em ambiente de homologação.
- [x] Validar paginação e busca em testes automatizados.
- [x] Validar tipos, faixa e enums do payload oficial de `POST /test/candidate` em testes automatizados.
- [x] Confirmar idempotência e diferenciação por `job_id` em testes automatizados.
- [ ] Confirmar `test_url` na página `/candidato/{token}` em ambiente integrado.
- [ ] Concluir uma tentativa em vaga real.
- [x] Validar callback e redirecionamento em testes automatizados; falta confirmar na homologação real da Gupy.
- [x] Validar `GET /test/result/{resultId}` sem parâmetros extras e com isolamento pelo token.
- [x] Validar páginas reais do `TestResult` para recrutador e candidato, incluindo isolamento de dados.
- [ ] Testar `result_webhook_url` com a Gupy.
- [x] Testar retry, 4xx permanente e DLQ em testes automatizados; falta validar a comunicação real.
- [ ] Homologar com cliente e vaga real na Gupy.

Última revisão: 15/07/2026.
