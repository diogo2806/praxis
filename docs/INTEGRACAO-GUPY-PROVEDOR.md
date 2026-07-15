# Integração Praxis como provedor de testes da Gupy

> **Propósito:** documentar o comportamento realmente implementado e comparar esse comportamento com o contrato oficial de provedores externos da Gupy.
>
> **Estado em 12/07/2026:** implementação técnica parcial. Os endpoints principais existem, mas a integração **não está pronta para ser declarada homologada** enquanto as incompatibilidades desta página não forem corrigidas e validadas em uma vaga real da Gupy.

Fonte oficial usada na revisão:

- https://developers.gupy.io/docs/integra%C3%A7%C3%A3o-com-testes-de-provedores-externos

## Resumo executivo

O Praxis expõe:

- `GET /test` para listar avaliações publicadas;
- `POST /test/candidate` para criar ou reutilizar uma tentativa;
- `GET /test/result/{resultId}` para consultar o resultado;
- entrega assíncrona para `result_webhook_url` por outbox.

O fluxo interno implementa `callback_url`, `job_id`, retorno assíncrono e redirecionamento final. Permanecem divergências no endpoint de resultado, tipos e enums que ainda impedem declarar a integração homologada.

## Compatibilidade com o contrato oficial

| Item do contrato Gupy | Implementação atual | Estado |
| --- | --- | --- |
| Bearer token no cabeçalho `Authorization` | Validado por `IntegrationAuthService` contra `integration_tokens` | Compatível |
| `GET /test` com `searchString`, `offset` e `limit` | Implementado; `limit` é normalizado entre 1 e 400 | Compatível |
| Resposta `TestItems` com `limit`, `offset`, `total_tests` e `payload` | Implementada | Compatível |
| `POST /test/candidate` | Implementado | Parcial |
| `name`, `email`, `document_id`, `test_id`, `company_id` | Recebidos | Parcial: `document_id` e `company_id` são `String`, enquanto o contrato oficial os descreve como `int64` |
| `callback_url` obrigatório | Recebido, validado, persistido e devolvido ao navegador após conclusão | Compatível tecnicamente |
| `job_id` | Recebido e persistido; também participa da idempotência quando informado | Compatível |
| `candidate_type` | Recebido, sem validação de enum no domínio | Parcial |
| `previous_result` | Aceita qualquer texto; OpenAPI local exemplifica `pass`, `fail`, `none` | **Incompatível** com `fail` ou `null` do contrato oficial |
| `result_webhook_url` | Recebido como `URI`; resultado é enviado por POST | Compatível |
| Resposta `201` com `test_result_id` e `test_url` | Implementada | Compatível |
| `GET /test/result/{resultId}` somente com `resultId` | Exige também `?company_id=...` | **Incompatível** |
| Callback GET após conclusão | O navegador acessa `/candidate/attempts/{token}/redirect` e recebe `302` para `callback_url` | Compatível tecnicamente |
| Redirecionamento do candidato de volta à Gupy | Executado automaticamente após a resposta final | Compatível tecnicamente |
| Payload `TestResult` | Campos principais são produzidos | Parcial |
| Status `notStarted`, `paused`, `done` | Implementado | Compatível |
| Resultado numérico de 0 a 100 | Implementado por competência | Compatível |
| `result_candidate_page_url` como página para a pessoa candidata | Hoje aponta para `/candidate/attempts/{attemptId}`, que é uma API JSON | **Incompatível para experiência de navegador** |
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

`PRAXIS_INTEGRATION_TOKEN` não é usado por `/test/**`. O `docker-compose.yml` ainda exige essa variável por legado de configuração, mas ela não substitui o token cadastrado no banco.

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

Body atualmente aceito:

```json
{
  "company_id": "empresa-123",
  "document_id": "candidate-document-456",
  "test_id": "sim-atendimento",
  "name": "Candidato Teste",
  "email": "candidato@example.com",
  "job_id": 100,
  "callback_url": "https://integracao.gupy.example/candidate-return",
  "result_webhook_url": "https://integracao.gupy.example/webhook",
  "accommodation_time_multiplier": 1.5,
  "candidate_type": "external",
  "previous_result": "none"
}
```

Campos atuais:

| Campo | Obrigatório no código | Observação |
| --- | --- | --- |
| `company_id` | Sim | Deve ser igual ao `company_id` associado ao token. |
| `document_id` | Sim | Participa da chave idempotente. |
| `test_id` | Sim | Deve identificar avaliação publicada da mesma empresa. |
| `name` | Sim | Nome da pessoa candidata. |
| `email` | Sim | Validado como e-mail. |
| `job_id` | Não | Identificador da vaga; quando informado, diferencia a chave idempotente. |
| `callback_url` | Sim | URL absoluta HTTP(S), persistida para o retorno final à Gupy. |
| `result_webhook_url` | Não | Se presente, recebe `TestResult` por POST. |
| `accommodation_time_multiplier` | Não | Extensão própria para acessibilidade. |
| `candidate_type` | Não | Não há validação de enum no domínio. |
| `previous_result` | Não | Não há validação de enum no domínio. |

Após a conclusão, a API pública devolve `redirectUrl` à tela. O frontend navega para `/candidate/attempts/{token}/redirect`, e esse endpoint responde `302 Location` para a `callback_url` recebida da Gupy. Assim, o GET final ocorre no navegador da pessoa candidata.

Resposta:

```json
{
  "test_url": "https://app.exemplo.com/candidato/<token-publico-da-tentativa>",
  "test_result_id": "res_123"
}
```

A idempotência usa o hash de:

```text
empresaId | companyId | documentId | testId | jobId (quando informado)
```

Chamadas repetidas com a mesma combinação reutilizam a tentativa existente.

### `GET /test/result/{resultId}`

Implementação atual:

```text
GET /test/result/res_123?company_id=empresa-123
Authorization: Bearer <token>
```

O backend valida:

- Bearer token;
- empresa associada ao token;
- correspondência entre o `company_id` da query e o token;
- propriedade do resultado pela empresa;
- existência do resultado.

A query `company_id` é uma proteção adicional interna, mas não aparece no endpoint oficial publicado pela Gupy. Para homologação, o isolamento deve continuar sendo feito pelo token sem alterar a assinatura esperada pela Gupy.

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
  "result_page_url": "https://app.exemplo.com/test/result/res_123?company_id=empresa-123",
  "result_candidate_page_url": "https://app.exemplo.com/candidate/attempts/att_123",
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
  Gupy->>Praxis: GET /test/result/{resultId}?company_id=...
```

Fluxo de callback e redirecionamento implementado:

```mermaid
sequenceDiagram
  participant Candidato
  participant Praxis
  participant Gupy

  Candidato->>Praxis: conclui teste
  Praxis-->>Candidato: redirectUrl após a resposta final
  Candidato->>Praxis: GET /candidate/attempts/{token}/redirect
  Praxis-->>Candidato: 302 Location: callback_url
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

1. Remover a obrigatoriedade de `company_id` da query do endpoint de resultado, mantendo isolamento pelo token.
2. Definir compatibilidade de tipos para `company_id` e `document_id`.
3. Aceitar e validar `previous_result` conforme `fail` ou `null`.
4. Corrigir `result_candidate_page_url` para uma página de navegador.
5. Validar com a Gupy se campos extras no `TestResult` são aceitos ou removê-los do contrato externo.
6. Executar homologação em vaga real, pois a própria documentação da Gupy informa que não há ambiente de sandbox para esse fluxo.

## Checklist de validação

- [ ] Gerar token Gupy pela Central de Integrações.
- [ ] Validar `GET /test`.
- [ ] Validar paginação e busca.
- [ ] Validar o payload oficial completo de `POST /test/candidate`.
- [ ] Confirmar idempotência.
- [ ] Confirmar `test_url` na página `/candidato/{token}`.
- [ ] Concluir uma tentativa.
- [x] Validar callback e redirecionamento em testes automatizados; falta confirmar na homologação real da Gupy.
- [ ] Validar `GET /test/result/{resultId}` sem parâmetros extras.
- [ ] Validar o `TestResult` exibido para empresa e candidato.
- [ ] Testar `result_webhook_url`.
- [ ] Testar retry, `408`, `429`, 4xx permanente e DLQ.
- [ ] Homologar com cliente e vaga real na Gupy.

Última revisão: 15/07/2026.
