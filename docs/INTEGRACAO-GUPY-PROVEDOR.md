# Praxis como Provedor de Testes Externos da Gupy

## Status da Integracao

- Endpoints implementados
- Autenticacao Bearer + API Key
- Fila de entrega com retry + DLQ
- DTOs de resultado alinhados ao schema esperado pela Gupy

## Endpoints Expostos

| Metodo | Path | Responsabilidade |
| --- | --- | --- |
| GET | `/test` | Lista testes publicados |
| POST | `/test/candidate` | Registra candidato e devolve `testUrl`, `testResultId` e `attemptId` |
| GET | `/test/result/{resultId}?company_id={companyId}` | Retorna o `TestResult` completo escopado pela empresa |

## Fluxo de Conclusao

Apos o candidato finalizar, o sistema calcula o resultado deterministico e:

1. Disponibiliza o resultado em `GET /test/result/{resultId}?company_id={companyId}`.
2. Chama o `callbackUrl` por GET em background, quando informado pela Gupy.
3. Enfileira o POST assincrono para `resultWebhookUrl`, quando informado pela Gupy.
4. Usa retry exponencial e DLQ para falhas permanentes ou limite de tentativas no envio do resultado.

## Como Ativar na Gupy

1. Fornecer a URL base publica do Praxis e a API Key por empresa.
2. Configurar a Gupy para chamar `GET /test` e `POST /test/candidate`.
3. Testar com vaga nao listada e inscricao ficticia.
4. Confirmar o recebimento do resultado via `GET /test/result/{resultId}?company_id={companyId}` e via `resultWebhookUrl`.

## Checklist para Producao

- [x] DTOs `TestResultResponse` e `TestResultItemResponse` alinhados ao schema Gupy.
- [x] Callback de conclusao via `callbackUrl`, restrito por allowlist `PRAXIS_WEBHOOK_ALLOWED_HOSTS`.
- [x] Trigger de `resultWebhookUrl` na conclusao da tentativa, restrito pela mesma allowlist.
- [x] Fila com retry e DLQ para entrega assincrona.
- [ ] Teste de homologacao com payload real da Gupy.
- [ ] Validacao de rate limiting especifico para o volume esperado por empresa.
