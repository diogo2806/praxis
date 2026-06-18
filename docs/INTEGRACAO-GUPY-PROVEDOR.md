# Integração Praxis como Provedor de Testes da Gupy

> **Propósito:** Documentar contrato, endpoints e fluxo de integração com Gupy como provedor de testes comportamentais.
> **Público:** Times técnico e operacional da Gupy, gestor de integração da Praxis.
> **Status:** ✅ v0.1.0 — Pronta para homologação.

---

## 📋 Visão Geral

O Praxis atua como **provedor de testes externos da Gupy**, expondo endpoints públicos autenticados por Bearer token para listagem de simulações, criação de tentativas de candidatos e consulta de resultados finais.

**Características:**
- ✅ Testes determinísticos (sem IA)
- ✅ Integração por token Bearer
- ✅ Webhooks com retry + DLQ (Dead Letter Queue)
- ✅ Idempotência de tentativas
- ✅ Isolamento por tenant

---

## 🔌 Endpoints Implementados

| Endpoint | Método | Status | Descrição |
|---|---|---|---|
| `GET /test` | GET | ✅ Pronto | Lista simulações publicadas com paginação e busca |
| `POST /test/candidate` | POST | ✅ Pronto | Cria ou reutiliza tentativa (idempotente) |
| `GET /test/result/{resultId}` | GET | ✅ Pronto | Consulta resultado (escopado por empresa) |
| `POST result_webhook_url` | POST | ✅ Pronto | Recebe envio assíncrono do resultado |

### Parâmetros Detalhados

**GET /test**
```
Parâmetros:
  - offset (opcional): deslocamento para paginação
  - limit (opcional): quantidade de resultados por página
  - searchString (opcional): busca por título ou código da simulação
  
Autenticação: Bearer token de integração Gupy
```

**POST /test/candidate**
```
Body:
{
  "companyId": "empresa-123",
  "documentId": "cpf-ou-documento-candidato",
  "testId": "sim-atendimento",
  "candidateName": "João Silva",
  "candidateEmail": "joao@example.com",
  "candidatePhone": "11999999999",
  "callbackUrl": "https://gupy.com.br/callback",
  "resultWebhookUrl": "https://gupy.com.br/webhook" (opcional)
}

Retorno:
{
  "testId": "sim-atendimento",
  "testUrl": "https://praxis.com.br/candidato/att_xyz",
  "estimatedDurationInSeconds": 1200
}
  
Autenticação: Bearer token de integração Gupy
```

**GET /test/result/{resultId}**
```
Parâmetros:
  - company_id (obrigatório): empresa que criou a tentativa
  - resultId: identificador único do resultado

Autenticação: Bearer token de integração Gupy
```

---

## 🔄 Fluxo Completo (End-to-End)

```mermaid
1. Gupy lista simulações
   GET /test
   ↓
2. Gupy cria tentativa para candidato
   POST /test/candidate → Praxis gera testUrl e callbackUrl
   ↓
3. Candidato acessa link público
   GET /candidato/:attemptId
   ↓
4. Candidato completa a simulação
   POST /candidato/:attemptId/answers
   ↓
5. Praxis calcula score determinístico
   [Sem IA, apenas regras de negócio]
   ↓
6. Backend enfileira entrega
   EventProcessor lê Outbox
   ↓
7. Backend chama callback + webhook
   POST callbackUrl (síncrono)
   POST resultWebhookUrl (assíncrono com retry)
   ↓
8. Gupy consulta resultado (opcional)
   GET /test/result/{resultId}?company_id=empresa-123
```

### Descrição por Etapa

| Etapa | Responsável | Ação | Notas |
|---|---|---|---|
| 1 | Gupy | Lista testes publicados | Busca por `searchString` se necessário |
| 2 | Gupy → Praxis | Cria tentativa + salva URLs | Chave idempotente: `companyId\|documentId\|testId` |
| 3–4 | Candidato | Responde no link público | Sem autenticação JWT, apenas token de tentativa |
| 5 | Praxis | Calcula score (regras fixas) | Determinístico e auditável |
| 6 | Praxis (async) | Lê Outbox e despacha | EventProcessor em background |
| 7 | Backend → Gupy | Entrega resultado com retry | Webhook tem timeout, assinatura HMAC e idempotency key |
| 8 | Gupy (opcional) | Consulta resultado | Para sincronizar ou validar; exige `company_id` correto |

---

## 📊 Contrato de Resultado (JSON)

```json
{
  "title": "Cenário: Atendimento em Caos",
  "testCode": "sim-atendimento-caos",
  "description": "Avaliação situacional determinística.",
  "providerName": "Praxis",
  "company_result_string": "**Score geral: 100/100**\n\n- Empatia: 95%\n- Comunicação: 100%\n- Decisão: 100%",
  "providerLink": "https://praxis.com.br",
  "status": "done",
  "result_page_url": "https://praxis.com.br/test/result/res_123?company_id=empresa-123",
  "result_candidate_page_url": "https://praxis.com.br/candidato/att_123",
  "results": [
    {
      "score": 95,
      "result_string": "95%",
      "type_result": "percentage",
      "tier": "major",
      "title": "Empatia",
      "description": "Pontuação da competência Empatia em decisões.",
      "date": "2026-06-16T15:00:00Z",
      "other_informations": {
        "turns_completed": 8,
        "critical_decisions": 2,
        "evidence_count": 12
      }
    }
  ]
}
```

### Regras de Resultado

| Campo | Regra |
|---|---|
| `status` | Apenas: `notStarted`, `paused`, `done` |
| `score` | Percentual de 0 a 100 |
| `tier` | `major` (visível para candidato e empresa) ou `minor` (apenas empresa) |
| `company_result_string` | Markdown/texto formatado para leitura executiva |
| `date` | ISO 8601 UTC (`YYYY-MM-DDTHH:mm:ssZ`) |
| Isolamento | `company_id` deve corresponder exatamente ao da tentativa |

---

## 🔐 Segurança e Autenticação

### Filtro de API Key Gupy
```java
GupyApiKeyFilter
├── Endpoints: /test/**
├── Extrai token Bearer do header
├── Valida contra PRAXIS_GUPY_API_KEY
└── Atribui role GUPY se válido
```

### Validação de URLs Externas
```
GupyOutboundUrlValidator
├── Permitidas: HTTPS, domínios públicos (whitelist PRAXIS_WEBHOOK_ALLOWED_HOSTS)
├── Bloqueadas: localhost, 127.0.0.1, 192.168.*, 10.0.0.*, links internos
├── Timeout: 30s para webhook
└── Assinatura: HMAC-SHA256 no header X-Praxis-Signature
```

### Isolamento por Tenant
- Cada tentativa é vinculada a `companyId`
- Consulta de resultado exige `company_id` coincidente
- Evita acesso cruzado entre clientes

### Papéis (Roles)
| Role | Endpoints | Descrição |
|---|---|---|
| `GUPY` | `/test/**` | Integração técnica da Gupy |
| `EMPRESA` | `/api/v1/**` | Usuário da empresa contratante (operacional) |
| `ADMIN` | (futuro) | Administrador global (ainda não implementado) |

---

## 📋 Regras Importantes

1. **Idempotência**: Mesma chave `companyId|documentId|testId` retorna tentativa existente
2. **Callback síncrono**: Executado no contexto de conclusão da tentativa
3. **Webhook assíncrono**: Fila com retry exponencial (1s, 2s, 4s, 8s, 16s)
4. **DLQ (Dead Letter Queue)**: Falhas após 5 retries vão para DLQ
5. **Whitelist obrigatória**: Webhooks só para domínios pré-aprovados
6. **Atualização de tentativa**: Não permitida após `COMPLETED`
7. **Retenção de dados**: Conforme LGPD e configuração do tenant

---

## 📈 Monitoramento

**Acompanhar entregas via:**
```
GET /api/v1/gupy/result-deliveries?simulationId={id}&versionNumber={n}
```

**Filtros disponíveis:**
- `status`: `pending`, `delivered`, `failed`, `dlq`
- `simulationId`: ID da simulação
- `versionNumber`: Versão da simulação
- `dateFrom` / `dateTo`: Período

**Exemplo de resposta:**
```json
{
  "resultDeliveries": [
    {
      "id": "delivery_123",
      "resultId": "res_456",
      "companyId": "empresa-789",
      "status": "delivered",
      "deliveredAt": "2026-06-16T15:00:00Z",
      "retryCount": 2,
      "errorMessage": null
    }
  ],
  "total": 42,
  "page": 1
}
```

---

## ✅ Checklist Pré-Homologação

- [ ] URL base e token compartilhados com time de integrações Gupy
- [ ] Teste com vaga real não-listada + candidato fictício
- [ ] Validar callback e webhook recebem resultados completos
- [ ] Testar retry de webhook em caso de falha simulada (5xx)
- [ ] Verificar idempotência (mesma tentativa retorna mesmo resultado)
- [ ] Validar isolamento de tenant (empresa A não vê dados de empresa B)
- [ ] Ativar scheduler de entrega (`@Scheduled`)
- [ ] Documentar porta, IP whitelist e zona de DNS com Gupy
- [ ] Configurar alertas para DLQ

---

## 📞 Próximos Passos

1. **Curto prazo (semana 1–2):**
   - Enviar credenciais (URL base + token) para time Gupy
   - Ativar agendador de entrega em produção
   - Fazer primeiro teste end-to-end

2. **Médio prazo (semana 3–4):**
   - Implementar retry com backoff exponencial no webhook
   - Adicionar observabilidade (logs estruturados + métricas Prometheus)
   - Criar dashboard de monitoramento de entregas

3. **Longo prazo:**
   - Estender para novos provedores de ATS (além de Gupy)
   - Adicionar suporte a autenticação OAuth 2.0
   - Implementar rate limiting por tenant

---

**Última atualização:** 18/06/2026  
**Dono:** @time-integracao  
**Próximo:** Consultar [Arquitetura Outbox](ARQUITETURA_OUTBOX_PATTERN.md) | [Garantias de Produção](../README.md)
