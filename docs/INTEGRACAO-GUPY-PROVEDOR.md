# Praxis como Provedor de Testes Externos da Gupy (v0.1.0)

**Status:** pronta para homologacao

## Endpoints Implementados

| Endpoint | Metodo | Status | Observacao |
| --- | --- | --- | --- |
| `/test` | GET | Pronto | Paginacao + searchString |
| `/test/candidate` | POST | Pronto | Idempotencia + callback + result_webhook_url |
| `/test/result/{resultId}` | GET | Pronto | DTO compativel com o contrato Gupy |

## Fluxo Completo

1. Gupy chama `POST /test/candidate`.
2. Praxis cria `CandidateAttempt` e salva as URLs de callback/resultado.
3. Candidato responde a simulacao.
4. Ao concluir (`COMPLETED`), o backend chama `callback_url` e enfileira envio para `result_webhook_url` com retry + DLQ.
5. Gupy pode consultar `GET /test/result/{resultId}`.

## Seguranca

- `GupyApiKeyFilter` exclusivo para `/test/**`, atribuindo role `GUPY`.
- Validacao rigorosa de URLs externas via `GupyOutboundUrlValidator`.
- Rotas operacionais da empresa usam JWT com role `EMPRESA` e tenant isolado.
- Role `ADMIN` fica reservado para administracao global futura da plataforma.

## Proximos Passos

- [ ] Enviar URL base + token para time de integracoes da Gupy.
- [ ] Testar com vaga nao-listada + candidato ficticio.
- [ ] Ativar scheduler de entrega (`@Scheduled`).

Projeto pronto para homologacao como provedor de testes da Gupy.
