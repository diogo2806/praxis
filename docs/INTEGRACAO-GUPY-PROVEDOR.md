# Integração Praxis como provedor de testes da Gupy

> **Propósito:** documentar somente o contrato público implementado pelo Práxis. A fonte externa e as regras de governança estão em [Fonte canônica da integração Gupy](GUPY-FONTE-CANONICA.md).

> **Estado em 18/07/2026:** compatibilidade técnica implementada e coberta por testes. A homologação formal continua dependente de vaga, token, callback, webhook e aprovação no ambiente real da Gupy.

## Escopo

O Práxis expõe:

| Método | Rota | Finalidade |
| --- | --- | --- |
| `GET` | `/test` | Listar avaliações publicadas disponíveis para a empresa autenticada. |
| `POST` | `/test/candidate` | Criar ou reutilizar uma tentativa e devolver o link de execução. |
| `GET` | `/test/result/{resultId}` | Consultar o resultado associado ao token da empresa. |

O callback de conclusão é um redirecionamento do navegador. A entrega de resultado para `result_webhook_url` é assíncrona e usa Outbox.

## Autenticação e isolamento

Todas as rotas `/test/**` exigem:

```text
Authorization: Bearer <token>
```

Fluxo de autenticação:

1. `IntegrationAuthService` calcula o SHA-256 do token.
2. O hash é convertido para Base64URL sem padding.
3. O hash deve existir em `integration_tokens` com o provider `gupy`.
4. O registro resolve `empresaId` e `company_id`.
5. Catálogo, tentativa e resultado são consultados dentro da empresa autenticada.

O token em claro é devolvido somente no momento da criação. O banco mantém apenas o hash.

`PRAXIS_INTEGRATION_TOKEN` não autentica `/test/**`.

## `GET /test`

Exemplo:

```text
GET /test?searchString=atendimento&offset=0&limit=50
Authorization: Bearer <token>
```

Regras:

- `searchString` é opcional;
- `offset` tem padrão `0`; valores negativos são normalizados para `0`;
- `limit` tem padrão `50` e é limitado ao intervalo de `0` a `400`;
- `limit=0` devolve `payload` vazio sem perder `total_tests`;
- somente versões publicadas da empresa do token são retornadas;
- `category` e `level` são omitidos enquanto não houver fonte configurável no domínio.

Resposta:

```json
{
  "limit": 50,
  "offset": 0,
  "total_tests": 1,
  "payload": [
    {
      "id": "sim-atendimento",
      "name": "Atendimento em situação crítica",
      "description": "Avaliação comportamental determinística."
    }
  ]
}
```

## `POST /test/candidate`

Exemplo:

```json
{
  "company_id": 1,
  "document_id": 4398157034,
  "test_id": "sim-atendimento",
  "name": "Candidato Teste",
  "email": "candidato@example.com",
  "job_id": 100,
  "callback_url": "https://empresa.gupy.io/candidate-return",
  "result_webhook_url": "https://empresa.gupy.io/result-webhook",
  "candidate_type": "external",
  "previous_result": "null"
}
```

### Campos

| Campo | Obrigatório | Regra |
| --- | --- | --- |
| `company_id` | Sim | Inteiro JSON `int64` positivo e igual ao identificador associado ao token. |
| `document_id` | Sim | Inteiro JSON `int64` positivo. Participa da identidade idempotente. |
| `test_id` | Sim | Avaliação publicada pertencente à mesma empresa. |
| `name` | Sim | Nome completo da pessoa candidata. |
| `email` | Sim | Endereço de e-mail válido. |
| `job_id` | Não | Identificador da vaga. Quando presente, diferencia a tentativa. |
| `callback_url` | Sim | URL absoluta usada pelo navegador após a conclusão. |
| `result_webhook_url` | Não | URL pública que recebe o resultado por `POST`. |
| `candidate_type` | Não | `internal`, `external` ou `null`. |
| `previous_result` | Não | `fail`, JSON `null`, ausência ou a string literal `"null"`. |
| `accommodation_time_multiplier` | Não | Extensão do Práxis para acomodação de tempo. |

Em produção, `callback_url` e `result_webhook_url` devem usar HTTPS e passar pelas políticas de URL de saída. Credenciais embutidas e fragmentos são rejeitados. HTTP é permitido somente no perfil local para loopback.

Resposta `201`:

```json
{
  "test_result_id": "res_123",
  "test_url": "https://app.exemplo.com/candidato/<jwt-candidate-attempt>"
}
```

### Idempotência

A identidade canônica usa:

```text
empresaId | companyId | documentId | testId | jobId
```

`company_id` e `document_id` são desserializados como `Long` e convertidos uma única vez para texto decimal. Assim, representações equivalentes não criam tentativas distintas. O mesmo conjunto reutiliza a tentativa existente; outro `job_id` cria uma identidade diferente.

## `GET /test/result/{resultId}`

Exemplo:

```text
GET /test/result/res_123
Authorization: Bearer <token>
```

O backend valida:

- token Bearer;
- empresa e `company_id`;
- propriedade do resultado;
- existência da tentativa;
- estado representável no contrato externo.

O endpoint não recebe parâmetros adicionais.

## Resultado externo

Consulta e webhook usam o mesmo `TestResultResponse`:

```json
{
  "title": "Nome da avaliação",
  "testCode": "sim-atendimento",
  "description": "Descrição da avaliação",
  "providerName": "Praxis",
  "company_result_string": "Resultado em Markdown para o RH",
  "providerLink": "https://praxis.iforce.com.br",
  "status": "done",
  "result_page_url": "https://praxis.iforce.com.br/results/att_123",
  "result_candidate_page_url": "https://praxis.iforce.com.br/candidato/<jwt-candidate-result>/resultado",
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

Regras:

- `score` é inteiro de `0` a `100`;
- resultados `major` são visíveis para empresa e pessoa candidata;
- resultados `minor` podem ser usados apenas na visão da empresa;
- `reliabilityLevel` permanece interno;
- extensões de topo não são serializadas;
- `other_informations` só aparece dentro de `TestResultItem`.

## Mapeamento de estados

| Estado interno | Status Gupy | Resultado |
| --- | --- | --- |
| `NOT_STARTED` | `notStarted` | Lista de resultados vazia. |
| `IN_PROGRESS` | `paused` | Lista de resultados vazia. |
| `COMPLETED` | `done` | Pontuações finais publicadas. |
| `ABANDONED` | Não representável | Consulta e entrega são rejeitadas. |
| `EXPIRED` | Não representável | Consulta e entrega são rejeitadas. |

Não existe conversão artificial de `ABANDONED` ou `EXPIRED` para `done`.

## URLs de resultado

- `test_url` usa JWT do tipo `candidate_attempt`, destinado à execução.
- `result_candidate_page_url` usa JWT do tipo `candidate_result`, destinado somente à consulta final.
- O token de resultado contém empresa, tentativa, emissão e expiração.
- A página pública valida assinatura, expiração, tipo e pertencimento.
- A pessoa candidata vê apenas avaliação, estado, conclusão, data e fatores `major`.
- E-mail, respostas, pesos, gabarito e regras internas não são expostos.

## Callback e webhook

### `callback_url`

Depois da conclusão, o frontend recebe `redirectUrl` e o navegador executa um único `GET` para a URL informada pela Gupy.

O backend não repete esse `GET`. Registros antigos de confirmação permanecem apenas para compatibilidade histórica.

### `result_webhook_url`

O resultado é persistido no Outbox na mesma transação da conclusão e enviado de forma assíncrona. Retry, backoff, DLQ e reprocessamento pertencem à [Arquitetura de Outbox](ARQUITETURA_OUTBOX_PATTERN.md).

## Segurança

- autenticação por token individual da empresa;
- isolamento por `empresaId` e `company_id`;
- token armazenado somente como hash;
- URLs externas validadas contra SSRF;
- HTTPS obrigatório em produção;
- JWTs distintos para execução e resultado;
- DTO externo limitado ao schema publicado;
- logs e respostas não expõem o token em claro.

## Responsabilidades documentais

| Assunto | Documento |
| --- | --- |
| Contrato e exemplos de payload | Este documento |
| Fonte externa e aliases proibidos | [Fonte canônica](GUPY-FONTE-CANONICA.md) |
| Prontidão, evidências e aprovação externa | [Homologação Gupy](HOMOLOGACAO-GUPY.md) |
| Retry, backoff, DLQ e operação de entregas | [Arquitetura de Outbox](ARQUITETURA_OUTBOX_PATTERN.md) |

Última revisão: 18/07/2026.
