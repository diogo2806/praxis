# Módulo Marketplace de Psicometria — Especificação Técnica (reconciliada)

> **Status:** especificação + fundação de persistência da Fase 1.
> **Decisão de produto confirmada:** sem IA generativa em nenhuma parte do sistema.
> **Objetivo:** profissionais de psicometria se cadastram gratuitamente, publicam testes
> (simulações) prontos para venda, empresas compram e clonam o teste calibrado para a
> própria conta, com repasse via PIX (split payment Mercado Pago).

Este documento parte da especificação original e a **reconcilia com o código real** do
repositório. A seção [§0 Reconciliação](#0-reconciliação-com-o-código-real) é a parte mais
importante: a especificação original assumia várias coisas que **não correspondem** ao
estado atual do `backend/`. Leia-a antes de implementar qualquer fase.

---

## 0. Reconciliação com o código real

Levantamento feito lendo `backend/src/main/...` e as migrations Flyway. Cada item abaixo
corrige uma premissa da especificação original.

### 0.1 `tenant` virou `empresa` — mas só no código Java, não no schema

O repositório passou por um rename `tenant → empresa` (commits `37fe839`, `#92`–`#94`).
Esse rename atingiu **apenas o código Java**: classes, métodos e variáveis agora usam
`empresa`/`empresaId`. As migrations SQL **mantiveram** os nomes físicos antigos.

| Camada | Nome usado |
|---|---|
| Entidades JPA | `@Table(name = "empresas")`, `@Column(name = "empresa_id")`, `empresa_subscriptions`, `empresa_billing_events`, `empresa_credit_*`, … |
| Migrations Flyway (`V1`–`V51`) | `tenants`, `tenant_id`, `tenant_subscriptions`, `tenant_billing_events`, `tenant_credit_*`, … |

> ⚠️ **Inconsistência pré-existente (não introduzida por este módulo).** Com
> `spring.jpa.hibernate.ddl-auto=validate`, entidades mapeadas para `empresas`/`empresa_id`
> não batem com tabelas físicas `tenants`/`tenant_id`. Em CI/Postgres isso só não explode
> se o banco real já tiver as colunas renomeadas por fora das migrations versionadas — o que
> é um risco de drift. **Recomendação:** criar uma migration de rename
> (`ALTER TABLE tenants RENAME TO empresas`, `RENAME COLUMN tenant_id TO empresa_id`, etc.)
> que alinhe o schema às entidades, antes de evoluir o marketplace. Veja §0.6.

**Impacto na spec original:** toda referência a `tenants`/`buyer_tenant_id`/`TenantSecurity`
deve ser lida como `empresas`/`buyer_empresa_id`/`EmpresaSecurity` (o equivalente atual).

### 0.2 IDs de empresa e de simulação são `VARCHAR(120)`, não `BIGINT`

A spec original modelava FKs como `BIGINT REFERENCES tenants(id)` e
`BIGINT REFERENCES simulations(id)`. No código real:

| Tabela | PK | Tipo |
|---|---|---|
| `empresas` (fisicamente `tenants`) | `id` | `VARCHAR(120)` |
| `simulations` | `id` | `VARCHAR(120)` |
| `simulation_versions` | `id` | `BIGINT` (identity) |
| `users` | `id` | `BIGINT` (identity) |

Logo:
- `marketplace_listings.source_simulation_id` → **`VARCHAR(120)` REFERENCES `simulations(id)`**.
- `marketplace_listings.source_version_id` → `BIGINT` REFERENCES `simulation_versions(id)`.
- Compras/threads que referenciam a empresa compradora → **`VARCHAR(120)`** (FK para a
  tabela de empresas), não `BIGINT`.

### 0.3 Roles: string sem prefixo `ROLE_`; não existe `ROLE_PLATFORM_ADMIN`

- Roles são `String` em coleção `user_roles` (`UserEntity.roles`), **sem** prefixo `ROLE_`.
  Valores reais: `"EMPRESA"` (`AdminEmpresaService.EMPRESA_ROLE`) e `"ADMIN"`
  (`AdminBootstrap.ADMIN_ROLE`).
- O prefixo `ROLE_` é adicionado em runtime por `JwtAuthenticationFilter.extractAuthorities`
  (`role.startsWith("ROLE_") ? role : "ROLE_" + role`). Por isso `SecurityConfig` usa
  `hasRole("EMPRESA")` / `hasRole("ADMIN")`.
- **Não existe `ROLE_PLATFORM_ADMIN`.** A moderação admin do marketplace deve usar a role
  `ADMIN` já existente (`hasRole("ADMIN")`), a menos que se decida criar uma role nova.
- Para o profissional, a role nova deve ser uma string simples, p.ex. **`"PROFISSIONAL"`**,
  protegida via `hasRole("PROFISSIONAL")`. **Não** é preciso enum de banco nem alteração de
  constraint — basta inserir a string em `user_roles`.

### 0.4 Empresa da plataforma e o profissional como `UserEntity`

- O *platform tenant* existe: `V44__seed_platform_tenant.sql` insere a empresa de id
  **`'PLATFORM'`** (na tabela física `tenants`). Comentário do arquivo:
  *"empresa ao qual os operadores ADMIN ficam vinculados (`UserEntity.empresaId = 'PLATFORM'`)"*.
- O profissional reaproveita 100% de `AuthService`/`JwtService`/`JwtAuthenticationFilter`:
  é um `UserEntity` com `empresaId = 'PLATFORM'` e role `"PROFISSIONAL"`, mais uma linha em
  `marketplace_professionals`. Login normal via `/api/v1/auth/login`.

### 0.5 Clone de simulação: o que existe e o que falta

- Existe `SimulationAdminService.clonePublishedVersionToDraft(simulationId, versionNumber)`,
  que cria **uma nova versão da mesma simulação** (mesma empresa). Os helpers privados
  `cloneVersion` / `cloneNode` / `cloneOption` copiam todo o grafo (competências, nós,
  opções, scores).
- O marketplace precisa de algo **diferente**: clonar a `SimulationVersionEntity` de origem
  para uma **nova `SimulationEntity` de outra empresa** (a compradora). Não existe método
  para isso hoje. É preciso extrair um `cloneVersionToEmpresa(sourceVersionId, destEmpresaId)`
  que: cria nova `SimulationEntity` (novo id, `empresaId = comprador`), e copia o grafo da
  versão de origem como versão 1 em DRAFT.
- ⚠️ **Decisão de produto em aberto (bloqueia a Fase 1 de ponta a ponta):** o profissional é
  usuário da empresa `'PLATFORM'`. **Onde vivem as simulações que ele publica?** Opções:
  (a) o profissional cria simulações sob a empresa `'PLATFORM'` e o listing referencia essas;
  (b) cria-se uma empresa "de autor" por profissional. Isso afeta `source_simulation_id` e o
  fluxo de criação. **Resolver antes de implementar o clone.**

### 0.6 Migrations: numeração, vendor e proteção append-only

- Última migration: `V51__add_token_preview_to_integrations.sql`. As novas começam em `V52`.
- Há migrations **Java** (`src/main/java/db/migration/`): `V13_1__protect_audit_events_append_only.java`
  e `V47_1__protect_billing_events_append_only.java`. O padrão append-only que a spec pede
  para `marketplace_orders`/`marketplace_payouts` **já existe** e deve ser replicado por aqui.
- Há migrations **vendor-specific** em `db/migration/postgresql/` (Flyway `{vendor}`), p.ex.
  `V13__protect_audit_events_append_only.sql`.
- ⚠️ **Índices parciais (`CREATE INDEX ... WHERE ...`) são exclusivos do PostgreSQL.** O H2
  usado nos testes (`jdbc:h2:mem;MODE=PostgreSQL`) **não** suporta — `V14` já falha no H2 por
  causa disso, o que impede o boot do contexto Spring nos testes locais com H2. Evite índices
  parciais em migrations comuns; se precisar, coloque-os apenas na pasta `postgresql/`.

### 0.7 Mercado Pago: o que já existe

Já existem: `billing/config/MercadoPagoProperties`, `billing/service/MercadoPagoClient`,
`MercadoPagoWebhookService`, `MercadoPagoSignatureValidator`,
`billing/controller/MercadoPagoWebhookController`. O split/marketplace e o OAuth Connect do
profissional são novos, mas reaproveitam essa base (Fase 2).

### 0.8 Saúde do build de teste (achado)

`mvn test` não sobe o contexto Spring **neste ambiente** porque o Flyway falha em `V14`
(índice parcial não suportado no H2) — condição **pré-existente**, independente deste módulo.
Consequência prática: nesta entrega foi possível validar **`mvn compile` / `mvn test-compile`**
e testes unitários puros (Mockito), mas **não** testes de integração JPA. Migrations novas
são PostgreSQL-alvo e precisam ser validadas em CI/Postgres.

---

## 1. Decisões de arquitetura (inalteradas)

| Decisão | Justificativa |
|---|---|
| Profissional **não** é `UserEntity` de empresa comum | É ator independente; vincula-se à empresa `'PLATFORM'` com role `PROFISSIONAL` |
| Reaproveitar o platform tenant (`V44`) | Reaproveita `AuthService`/`JwtService`/`JwtAuthenticationFilter` sem segundo login |
| Listing referencia `SimulationVersionEntity` existente | Reaproveita o domínio de simulação (nós, opções, critérios) |
| Clone para o comprador reaproveita lógica de `SimulationAdminService` | Precisa de uma variante que aceite empresa de destino diferente (§0.5) |
| Pagamento via Mercado Pago split | Reaproveita `MercadoPagoClient`/webhook, adicionando split |
| Escrow antes do repasse PIX | Novo scheduler nos moldes de `OutboxScheduler`/`PrivacyRetentionScheduler` |

---

## 2. Modelo de dados (tipos corrigidos)

Numeração a partir de `V52`. Tipos ajustados conforme §0.2/§0.3.

| Migration | Conteúdo |
|---|---|
| `V52__create_marketplace_professionals.sql` | Tabela `marketplace_professionals` + tabela filha de especialidades |
| `V53__create_marketplace_listings.sql` | Tabela `marketplace_listings` + tabela filha de nós de preview |
| `V54__create_marketplace_orders.sql` | Tabela `marketplace_orders` (Fase 2) |
| `V55__create_marketplace_payouts.sql` | Tabela `marketplace_payouts` (Fase 2) |
| `V56__create_marketplace_reviews.sql` | Tabela `marketplace_reviews` (Fase 3) |
| `V57__create_marketplace_messages.sql` | `marketplace_message_threads` + `marketplace_messages` (Fase 3) |
| `V58__add_marketplace_platform_config.sql` | Config de comissão (%) e dias de escrow (Fase 2) |
| `V59__protect_marketplace_orders_append_only.java` | Append-only em `marketplace_orders`/`marketplace_payouts`, padrão `V47_1` (Fase 2) |

### Tabelas implementadas nesta entrega (Fase 1)

`marketplace_professionals` (referencia `users(id)` — `BIGINT`) e `marketplace_listings`
(referencia `marketplace_professionals(id)`, `simulations(id)` — `VARCHAR(120)`,
`simulation_versions(id)` — `BIGINT`). Coleções (`specialties`, `preview_node_ids`) modeladas
como tabelas filhas via `@ElementCollection`, seguindo o padrão de `user_roles` — em vez de
`TEXT[]`/`BIGINT[]`, que têm portabilidade ruim com `ddl-auto=validate`/H2.

Demais tabelas (orders, payouts, reviews, messages, config) ficam para as Fases 2–3; o
schema-alvo está descrito na spec original e deve usar os tipos corrigidos acima.

---

## 3. Backend — mapa de implementação por fase

Pacote novo: `backend/src/main/java/br/com/iforce/praxis/marketplace/`.

**Fase 1 (esta entrega — fundação):**
- `model/`: `ProfessionalVerificationStatus`, `ListingStatus`, `ListingCategory` ✅
- `persistence/entity/`: `MarketplaceProfessionalEntity`, `MarketplaceListingEntity` ✅
- `persistence/repository/`: `MarketplaceProfessionalRepository`, `MarketplaceListingRepository` ✅
- migrations `V52`, `V53` ✅

**Fase 1 (a seguir — comportamento):**
- `service/`: `MarketplaceProfessionalService` (cadastro via `AuthService` + empresa `'PLATFORM'`
  + role `PROFISSIONAL`; validação de CPF/CNPJ), `MarketplaceListingService` (criar/editar/
  submeter; validar posse da versão de origem), `MarketplaceListingCloneService`
  (clone cross-empresa, §0.5), `MarketplaceAdminModerationService`.
- `controller/`: `MarketplaceProfessionalController`, `MarketplaceListingController`,
  `AdminMarketplaceController`.
- `dto/`: conforme spec original (`RegisterProfessionalRequest/Response`,
  `ListingSummaryResponse`, `ListingDetailResponse`, `ListingSearchFilter`, …).
- Edições: `SecurityConfig` (rotas `/api/v1/marketplace/**`; GET de vitrine público;
  `/api/v1/admin/marketplace/**` com `hasRole("ADMIN")`); `InAppNotificationType`
  (`MARKETPLACE_LISTING_APPROVED`, `MARKETPLACE_LISTING_REJECTED`,
  `MARKETPLACE_PROFESSIONAL_VERIFIED`, …).

**Fase 2 — pagamento:** `MarketplaceOrderService`, `MarketplaceMercadoPagoService`,
`MercadoPagoConnectService`, `MarketplacePayoutService`, `PayoutReleaseScheduler`; edições em
`MercadoPagoClient` (`createSplitPreference`) e `MercadoPagoWebhookController` (rotear
`order_type=marketplace`). Migrations `V54`–`V59`.

**Fase 3 — social:** `MarketplaceReviewService` (exige `order PAID` + `CandidateAttempt`
concluído), `MarketplaceMessageService` (rate limit nos moldes de `PasswordResetRateLimiter`).
Migrations `V56`–`V57`.

**Fase 4 — admin:** dashboards/métricas, disputas, reembolso; auditoria via `AuditEventService`.

---

## 4–8. Contratos de API, frontend, wireframes, split PIX, segurança

Permanecem como na especificação original (ver histórico desta proposta), com as correções da
§0 aplicadas — em especial: rotas admin sob `hasRole("ADMIN")`; IDs de empresa/simulação como
`VARCHAR`; role do profissional `"PROFISSIONAL"`.

Nota técnica sobre "iframe do LinkedIn": o LinkedIn bloqueia embed de perfis de terceiros
(`X-Frame-Options`/CSP). Seguir com **link estilizado** "Ver perfil no LinkedIn ↗" (nova aba),
salvando apenas `linkedin_url`.

---

## 9. Ordem de implementação

| Fase | Escopo | Depende de |
|---|---|---|
| **1** | Cadastro de profissional, perfil, criação/moderação de listing, clone gratuito | Resolver §0.5 (onde vivem as simulações do profissional) e idealmente §0.1 (rename de schema) |
| **2** | Checkout + split Mercado Pago + escrow/payout | Fase 1 |
| **3** | Reviews + mensagens | Fase 2 |
| **4** | Painel admin (moderação, disputas, métricas) | Fases 1–3 |

### Bloqueadores a resolver com o time antes de prosseguir

1. **§0.5** — modelo de posse das simulações do profissional (empresa `'PLATFORM'` vs.
   empresa-de-autor). Define `source_simulation_id` e o fluxo de criação/clone.
2. **§0.1** — alinhar schema (`tenants`→`empresas`) às entidades, ou confirmar que o banco
   real já está renomeado, para destravar testes de integração e evitar drift.
3. **§0.8** — corrigir `V14` (índice parcial) para o boot de testes em H2, ou padronizar os
   testes de integração em Postgres (Testcontainers).
</content>
</invoke>
