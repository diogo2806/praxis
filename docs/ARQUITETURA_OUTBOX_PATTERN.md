# Arquitetura de Outbox e Integração ATS

> **Propósito:** Documentar padrão Outbox transacional e estratégia de integração com provedores ATS.  
> **Público:** Engenheiros backend, arquitetos de sistema.  
> **Status:** ✅ Completo.

---

## 📊 Estado Atual

- ✅ Backend possui outbox transacional para entrega assíncrona de eventos
- ✅ A integração operacional é Gupy, sem registry ou interface genérica de ATS
- ✅ Nenhum adapter futuro/hipotético é registrado como bean Spring
- ✅ Novas plataformas entram apenas com chamada real, contrato de autenticação, tratamento de erro e teste de integração

---

## 🔄 Fluxo de Entrega (Outbox Pattern)

```
1. Domínio cria/atualiza tentativa
   └─ evento: CandidateAttemptCreated ou Updated
   
2. Evento persistido no OUTBOX na mesma transação
   └─ atomicidade garantida (mesmo commit que tentativa)
   
3. Processador assíncrono lê eventos pendentes
   └─ job rodando a cada 5-30 segundos
   
4. Entrega chama os serviços concretos da Gupy
   └─ HTTP POST para callback_url + result_webhook_url
   
5. Resultado persistido em OutboxEvent.processedAt
   └─ sucesso → marked as delivered
   └─ falha → retry com backoff exponencial
   └─ falha permanente (5+ retries) → DLQ
```

**Vantagens do Outbox Pattern:**
- ✅ Não há perda de eventos mesmo em falha de rede
- ✅ Entrega exatamente-uma-vez (idempotência)
- ✅ Separação entre transação de negócio e entrega
- ✅ Auditória completa em banco (append-only)

---

## 🏗️ Componentes-Chave

### OutboxEvent (Entidade)
```java
@Entity
public class OutboxEvent {
  @Id
  private UUID id;
  private String aggregateType;      // "CandidateAttempt"
  private UUID aggregateId;          // tentativa ID
  private String eventType;          // "CandidateAttemptCompleted"
  private String payload;            // JSON serializado
  private LocalDateTime createdAt;
  private LocalDateTime processedAt; // null até entregar
  private Integer retryCount;
  private LocalDateTime nextRetryAt;
  private String dlqReason;          // Se falha permanente
}
```

### Entrega Gupy Concreta
```java
@Component
public class OutboxProcessor {
  public void deliverResult(CandidateAttempt attempt, TestResult result) {
    // 1. Valida URLs externas (allow-list)
    // 2. Assina payload com HMAC-SHA256
    // 3. Faz POST com timeout + retry
  }
}
```

### EventProcessor (Scheduler)
```java
@Component
public class OutboxEventProcessor {
  @Scheduled(fixedDelay = 10000) // A cada 10s
  public void processEvents() {
    List<OutboxEvent> pending = outboxRepository
      .findByProcessedAtIsNullAndNextRetryAtLessThanEqual(now());
    
    pending.forEach(event -> {
      try {
        gupyResultDeliveryService.deliver(event.payload);
        event.setProcessedAt(now());
      } catch (Exception e) {
        event.incrementRetryCount();
        event.setNextRetryAt(calculateBackoff(event.retryCount));
        if (event.retryCount() > MAX_RETRIES) {
          event.setDlqReason(e.getMessage());
        }
      }
    });
  }
}
```

---

## 📋 Regra Para Novas Integracoes

Antes de integrar novo provedor, crie um fluxo concreto apenas após definir:

| Pré-requisito | Exemplo (Gupy) |
|---|---|
| **Endpoint real ou webhook real** | `POST https://gupy.com.br/callback`, `POST https://gupy.com.br/webhook` |
| **Credenciais/configuração por tenant** | `GUPY_API_KEY` armazenada por `companyId` |
| **Mapeamento de payload entrada/saída** | `CandidateAttempt` → `POST /test/candidate` body |
| **Timeout, retry e tratamento de erro** | Timeout 30s, retry exponencial, DLQ após 5 falhas |
| **Testes** | Integração contra cliente real ou mock HTTP controlado (WireMock) |

✅ **Implementação:**
1. Criar serviços concretos para o novo provedor
2. Expor a ativação com guard/flag até a validação fim a fim
3. Testes cobrindo sucesso + falha + timeout
4. Deploy com flag desativada até validar em ambiente real

❌ **Evitar:**
- Adapters ou registries criados antes de existir uma integração real
- Testes apenas com unit-tests (validar contra cliente real)
- Integração hardcoded sem configuração por tenant

---

## 🔐 Segurança na Entrega

| Aspecto | Implementação |
|---|---|
| **Autenticação** | Bearer token para API externa (armazenado em secrets) |
| **Autorização** | Entrega Gupy validada por tenant (não mescla dados) |
| **Assinatura** | HMAC-SHA256 no header `X-Praxis-Signature` (webhook) |
| **Validação de URL** | Allow-list de domínios; bloqueio de localhost/10.0.0.0/192.168 |
| **Timeout** | 30s por requisição HTTP |
| **Idempotência** | Cada evento tem UUID único (`idempotencyKey`) |
| **Logs** | Payload (sem dados sensíveis) salvo em `OutboxEvent.payload` |

---

## 📊 Monitoramento

**Consultar status de entregas:**
```
GET /api/v1/gupy/result-deliveries
  ?simulationId={id}
  &versionNumber={n}
  &status=pending|delivered|failed|dlq
```

**Métrica-chave: Taxa de Sucesso**
```
delivered_count / (delivered_count + dlq_count + pending_count)
Alvo: > 99.5%
```

**Alertas recomendados:**
- ⚠️ Mais de 100 eventos em DLQ
- ⚠️ Taxa de falha > 5% em 1 hora
- ⚠️ Latência média > 60s

---

## 🚀 Implementação Futura

1. **Backpressure:** Se fila > 1000 eventos, desacelerar ingestão
2. **Novas integrações concretas:** Suporte a outros provedores apenas quando houver contrato e cliente real
3. **Replay:** API para reenviar eventos de DLQ
4. **Webhooks internos:** Notificar aplicação quando entrega completar

---

**Última atualização:** 18/06/2026  
**Dono:** @backend-team  
**Próximo:** [Integração Gupy](INTEGRACAO-GUPY-PROVEDOR.md)
