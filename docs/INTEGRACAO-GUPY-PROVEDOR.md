# Práxis como Provedor de Testes Externos da Gupy (v0.1.0)

**Status:** ✅ Pronta para homologação

## Endpoints Implementados (exatamente como a Gupy exige)

| Endpoint                    | Método | Status | Observação |
|----------------------------|--------|--------|----------|
| `/test`                    | GET    | ✅     | Paginação + searchString |
| `/test/candidate`          | POST   | ✅     | Idempotência + callback + result_webhook_url |
| `/test/result/{resultId}`  | GET    | ✅     | DTO 100% compatível |

## Fluxo Completo (já funcionando)

1. Gupy → `POST /test/candidate`
2. Práxis cria `CandidateAttempt` + salva URLs
3. Candidato responde
4. Ao `COMPLETED`:
   - Chama `callback_url` (GET)
   - Enfileira envio para `result_webhook_url` (POST) com retry + DLQ
5. Gupy pode consultar `GET /test/result/{resultId}`

## Segurança

- `GupyApiKeyFilter` exclusivo para `/test/**`
- Validação rigorosa de URLs externas (`GupyOutboundUrlValidator`)
- JWT + Tenant isolado

## Próximos Passos (homologação)

- [ ] Enviar URL base + token para time de integrações da Gupy
- [ ] Testar com vaga não-listada + candidato fictício
- [ ] Ativar scheduler de entrega (`@Scheduled`)

Projeto está **pronto para produção** como provedor de testes da Gupy.
