# 🏗️ Arquitetura: Outbox Pattern & Integration Layer

**Data:** 18 de junho de 2026  
**Branch:** `claude/compassionate-edison-t2k0pp`  
**Status:** Implementação completa ✅

---

## 📋 Resumo Executivo

Implementação do **Outbox Pattern** e **Integration Layer** conforme arquitetura descrita no backlog.txt. O sistema evolui de "integração específica com Gupy" para **"Assessment Platform plugável com múltiplos ATSs"**.

---

## 🧱 Arquitetura Implementada

### Camadas

```
┌─────────────────────────────────────┐
│      ATS / HR Systems               │
│  Gupy | Workday | Greenhouse | etc. │
└──────────────┬──────────────────────┘
               │ REST/Webhooks
               ▼
┌─────────────────────────────────────┐
│    INTEGRATION LAYER (Adapters)     │
│  ┌──────────────────────────────┐   │
│  │ ATSAdapter interface         │   │
│  │ ├─ GupyAdapter ✅            │   │
│  │ ├─ WorkdayAdapter (stub)     │   │
│  │ └─ GreenhouseAdapter (stub)  │   │
│  └──────────────────────────────┘   │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│    OUTBOX PATTERN (Event Driven)    │
│  ┌──────────────────────────────┐   │
│  │ OutboxEventEntity            │   │
│  │ OutboxService (publish)      │   │
│  │ OutboxProcessor (async)      │   │
│  └──────────────────────────────┘   │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│       CORE DOMAIN                   │
│  CandidateAttemptService            │
│  ResultScoringService               │
│  SimulationEngine                   │
└─────────────────────────────────────┘
```

---

## 🔌 Arquivos Criados

### 1. **Outbox Pattern**

```
src/main/java/br/com/iforce/praxis/shared/outbox/
├── persistence/
│   ├── entity/OutboxEventEntity.java      ← Tabela append-only
│   └── repository/OutboxEventRepository.java
├── service/
│   ├── OutboxService.java                  ← Publica eventos
│   └── OutboxProcessor.java                ← Processa assincronamente
```

**Responsabilidade:**
- Persistir eventos no banco de dados **antes** de enviar
- Processar eventos com retry automático
- Garantir entrega sem perda de eventos

### 2. **Integration Layer**

```
src/main/java/br/com/iforce/praxis/integration/ats/
├── adapter/
│   ├── ATSAdapter.java                     ← Interface padrão
│   ├── GupyAdapter.java                    ← Implementação para Gupy ✅
│   └── AdapterRegistry.java                ← Registro central
├── model/
│   ├── CandidateContext.java               ← Contexto normalizado
│   └── ResultPayload.java                  ← Resultado canônico
```

**Responsabilidade:**
- Abstrair diferentes ATS atrás de interface comum
- Normalizar entrada/saída
- Permitir suporte a múltiplos ATS sem quebrar core

### 3. **Database Migration**

```
src/main/resources/db/migration/
└── V14__create_outbox_events_table.sql    ← Tabela com índices
```

---

## 🔄 Fluxo: Antes vs. Depois

### ANTES (Modelo atual)

```
CandidateAttemptService
  ↓
save attempt
  ↓
call webhook directly
  ↓ (risk: crash aqui = evento perdido)
```

**Problemas:**
- ❌ Se servidor cair → webhook nunca é enviado
- ❌ Retry é manual/reativo
- ❌ Acoplamento transacional (DB + HTTP)
- ❌ Difícil escalar para múltiplos ATS

### DEPOIS (Outbox Pattern)

```
CandidateAttemptService
  ↓
save attempt + INSERT OUTBOX_EVENT
  ↓ (transação ACID)
return response immediately
  ↓
OutboxProcessor (scheduler)
  ↓
retry automático + DLQ para falhas
```

**Ganhos:**
- ✅ Evento nunca é perdido (salvo no DB)
- ✅ Retry automático com backoff exponencial
- ✅ Desacoplamento (DB ≠ HTTP)
- ✅ Suporte para múltiplos ATS via AdapterRegistry

---

## 🎯 Como Usar

### 1. Publicar um Evento (de CandidateAttemptService)

```java
// Antes (síncrono, risco de perda):
resultDeliveryService.enqueueWebhookDelivery(candidateAttempt);

// Depois (assíncrono, garantido):
outboxService.publish(
    candidateAttempt.getTenantId(),
    "RESULT_READY",
    "CandidateAttempt",
    candidateAttempt.getId().toString(),
    new ResultPayload(...)
);
```

### 2. Usar um Adapter

```java
// Obter adapter para Gupy
ATSAdapter adapter = adapterRegistry.getAdapter(ATSPlatform.GUPY);

// Criar candidato
CandidateContext context = adapter.createCandidate(new CreateCandidateCommand(...));

// Enviar resultado
adapter.pushResult(resultPayload);
```

### 3. Adicionar Novo Adapter

```java
@Component
public class WorkdayAdapter implements ATSAdapter {
    @Override
    public CandidateContext createCandidate(CreateCandidateCommand cmd) {
        // Implementação específica do Workday
    }

    @Override
    public void pushResult(ResultPayload payload) {
        // Transformar payload para formato Workday
    }

    @Override
    public ATSPlatform type() {
        return ATSPlatform.WORKDAY;
    }
}
// AdapterRegistry registra automaticamente via Spring
```

---

## 📊 Estrutura de Dados

### OutboxEventEntity

```
outbox_events
├── id (PK)
├── tenant_id
├── event_type           ← "RESULT_READY", "SIMULATION_PUBLISHED", etc.
├── aggregate_type       ← "CandidateAttempt", "SimulationVersion"
├── aggregate_id         ← ID da tentativa ou versão
├── payload (JSON)       ← Dados completos do evento
├── status               ← PENDING, SENT, RETRYING, DLQ
├── attempts
├── next_attempt_at      ← Quando tentar novamente
└── created_at
```

**Índices:**
- `idx_outbox_pending` — Para buscar eventos prontos (`status, next_attempt_at, tenant_id`)
- `idx_outbox_event_type` — Para buscar por tipo
- `idx_outbox_aggregate` — Para histórico por agregado

---

## 🔁 Retry Strategy

**Backoff exponencial (segundos):**
- 1ª tentativa: 1s
- 2ª tentativa: 4s
- 3ª tentativa: 16s
- 4ª tentativa: 64s
- 5ª tentativa: 256s (~4 minutos)
- **Depois:** DLQ (Dead Letter Queue) para análise manual

**Tratamento de erros:**
- **4xx** (erro de contrato) → Direto para DLQ
- **5xx** (erro temporário) → Retry com backoff
- **Timeout** → Retry com backoff

---

## 🚀 Próximos Passos

### Curto prazo (pronto para implementar):
1. ✅ **Outbox Pattern** (implementado)
2. ✅ **Integration Layer** (implementado)
3. **Modificar CandidateAttemptService** para usar OutboxService em vez de ResultDeliveryService
4. **Criar migration de dados** (ResultDelivery → OutboxEvent)

### Médio prazo:
5. **WorkdayAdapter** — Integração com Workday
6. **GreenhouseAdapter** — Integração com Greenhouse
7. **LinkedIn Adapter** — Integração com LinkedIn Careers

### Longo prazo:
8. **Event bus interno** — Kafka para eventos entre microsserviços
9. **Public Assessment API** — Tipo Stripe de avaliação
10. **Analytics layer** — Export de eventos para BI tools

---

## ✅ Checklist de Implementação

- ✅ OutboxEventEntity criada
- ✅ OutboxEventRepository criada
- ✅ OutboxService criada (publish)
- ✅ OutboxProcessor criada (scheduler)
- ✅ ATSAdapter interface criada
- ✅ GupyAdapter implementado
- ✅ AdapterRegistry criada
- ✅ ResultPayload e CandidateContext criados
- ✅ Migration SQL criada
- ⏳ CandidateAttemptService atualizado (próximo)
- ⏳ Testes unitários para Outbox
- ⏳ Testes de integração para adapters

---

## 📖 Observações

1. **Não breaking change** — O código antigo (ResultDeliveryService) continua funcionando
2. **Gradual migration** — Pode migrar de ResultDelivery para Outbox graduamente
3. **Multi-tenant safe** — Cada evento carrega seu tenant_id, sem mistura
4. **Event-sourcing ready** — Estrutura pronta para upgrade para event sourcing completo

---

## 🔗 Referências

- Arquivo original: `BACKLOG.txt` (seção "Outbox Pattern")
- Padrão: Martin Fowler - Event Sourcing
- Implementação: Spring Boot + JPA + Scheduled Tasks

**Commit:** `[para ser feito com Outbox migrations]`
