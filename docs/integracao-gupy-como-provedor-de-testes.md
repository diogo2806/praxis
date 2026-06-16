# Integracao do Praxis como Provedor de Testes Externos da Gupy

## Visao Geral

O Praxis atua como provedor de testes externos da Gupy. A integracao expoe endpoints publicos autenticados por Bearer token para listagem de testes, criacao de tentativa do candidato e consulta de resultado.

## Endpoints Implementados

| Endpoint | Metodo | Finalidade | Autenticacao |
| --- | --- | --- | --- |
| `/test` | GET | Lista testes publicados com `offset`, `limit` e `searchString` | Bearer token |
| `/test/candidate` | POST | Cria ou reutiliza tentativa idempotente para candidato | Bearer token |
| `/test/result/{resultId}` | GET | Retorna o resultado no formato `TestResult` da Gupy | Bearer token |
| `result_webhook_url` | POST | Recebe envio assincrono do resultado pela fila de retry/DLQ | URL informada pela Gupy |

## Fluxo Completo

1. A Gupy chama `GET /test` para listar simulacoes publicadas.
2. A Gupy chama `POST /test/candidate` com `companyId`, `documentId`, `testId`, dados do candidato e, opcionalmente, `resultWebhookUrl`.
3. O Praxis cria ou reutiliza a tentativa por chave idempotente `companyId|documentId|testId`.
4. O candidato conclui a simulacao no link retornado em `testUrl`.
5. Ao finalizar, o Praxis calcula o score deterministico e monta o payload `TestResult`.
6. A Gupy pode consultar `GET /test/result/{resultId}`.
7. Se `resultWebhookUrl` tiver sido informado, o Praxis enfileira o envio assincrono com retry exponencial e DLQ.

## Contrato de Resultado

O payload de resultado segue o shape externo da Gupy:

```json
{
  "title": "Cenario Seed de Teste",
  "testCode": "sim-atendimento-caos",
  "description": "Avaliacao situacional deterministica.",
  "providerName": "Praxis",
  "company_result_string": "Score geral: 100/100",
  "providerLink": "http://localhost:8080",
  "status": "done",
  "result_page_url": "http://localhost:8080/test/result/res_123",
  "result_candidate_page_url": "http://localhost:8080/candidate/attempts/att_123",
  "results": [
    {
      "score": 100,
      "result_string": "100%",
      "type_result": "percentage",
      "tier": "major",
      "title": "Empatia",
      "description": "Pontuacao da competencia Empatia.",
      "date": "2026-06-16T15:00:00Z",
      "other_informations": {}
    }
  ]
}
```

## Regras Importantes

- `status` externo usa apenas `notStarted`, `paused` ou `done`.
- `company_result_string` e enviado em Markdown/texto para leitura da empresa.
- `score` de cada item sempre usa porcentagem de 0 a 100.
- `tier = major` aparece para candidato e empresa.
- `tier = minor` fica restrito a visao da empresa.
- O envio para `result_webhook_url` reaproveita `ResultDeliveryService`, com retry exponencial e DLQ para erro permanente.

## Monitoramento

As entregas assincronas podem ser acompanhadas em `/api/v1/gupy/result-deliveries`, com filtros opcionais por `status`, `simulationId` e `versionNumber`.
