# 🎯 Praxis — Plataforma de Avaliação Comportamental Determinística

> **Missão:** Prover simulações comportamentais justas, auditáveis e livres de viés algorítmico para recrutamento e desenvolvimento de talentos.

---

## 📚 Documentação

**Começar por aqui:**
- 🔒 **[Garantias de Produção](docs/GARANTIAS-PRODUCAO.md)** — Princípios de segurança, isolamento de dados e determinismo
- 🏗️ **[Arquitetura do Sistema](docs/ARQUITETURA-SISTEMA.md)** — Outbox, integração ATS, fluxo de eventos
- 🔌 **[Integração Gupy](docs/INTEGRACAO-GUPY-PROVEDOR.md)** — Endpoints, contrato e fluxo
- 🎨 **[Mapa Frontend-Backend](docs/frontend-backend-map.md)** — Rotas, APIs e integração

**Para operadores:**
- 📊 [Monitoramento](docs/OPERACIONAL-MONITORAMENTO.md)
- 🏛️ [Governança e Publicação](docs/OPERACIONAL-GOVERNANCA.md)
- ✅ [Usabilidade — Implementação](docs/USABILIDADE_IMPLEMENTACAO.md)

**Para desenvolvedores:**
- 🛠️ [Regras Backend Java](docs/DESENVOLVIMENTO-BACKEND-JAVA.md)
- 🗂️ [Rotas TanStack Start](docs/INTEGRACAO-ROUTES-TANSTACK.md)

**Pesquisa e contexto:**
- 📈 [Reclamações sobre Concorrentes](docs/research/RECLAMACOES-CONCORRENTES.md)

---

## 🔐 Garantias de Produção

### Scoring
- ✅ **Determinístico:** Score calculado por regras fixas, sem IA
- ✅ **Auditável:** Cada decisão registrada no PostgreSQL (append-only)
- ✅ **Reproduzível:** Mesmas respostas = mesmo resultado sempre

### Isolamento
- ✅ **Multi-tenant:** Toda simulação isolada por tenant (`companyId`)
- ✅ **Acesso controlado:** Endpoints filtram JWT e validam role + tenant
- ✅ **Histórico preservado:** Tentativas vinculadas a `simulation_version_id` imutável

### Segurança
- ✅ **Respostas sincronizadas:** Lock pessimista evita race conditions
- ✅ **Webhooks seguros:** Assinatura HMAC, allow-list de domínios, timeout
- ✅ **Privacidade:** Dados pessoais anonimizados após retenção (LGPD)

### Entregas
- ✅ **Fila com retry:** Webhook assíncrono com backoff exponencial
- ✅ **Dead Letter Queue:** Falhas permanentes vão para DLQ (rastreabilidade)
- ✅ **Idempotência:** Reentrega não causa duplicação

---

## 👥 Papéis (Roles)

| Perfil | Acesso | Descrição |
|---|---|---|
| **EMPRESA** | `/api/v1/**` | Usuário da empresa contratante; acessa próprio tenant (simulações, auditoria, monitoramento) |
| **GUPY** | `/test/**` | Token de integração Gupy; endpoints para listagem, criação de tentativa e consulta de resultado |
| **ADMIN** | *(futuro)* | Administrador global; gerencia empresas/tenants (ainda não implementado) |

**Segurança configurável:**
```
PRAXIS_SECURITY_ENABLED=false  → Rotas sem JWT; usa tenant padrão
PRAXIS_SECURITY_ENABLED=true   → JWT obrigatório; role + tenant isolados
```

---

## 🚀 Quick Start

### Backend
```bash
mvn clean install
mvn spring-boot:run
```

### Frontend
```bash
cd frontend
npm install
npm run dev
```

### Testar integração
```bash
curl -X GET http://localhost:8080/test \
  -H "Authorization: Bearer seu-token-gupy"
```

---

## 📊 Status

- ✅ **MVP pronto** — Simulações, score, auditoria, Gupy, LGPD
- ✅ **Segurança ativa** — JWT, tenant, role-based access, webhooks HMAC
- 🔮 **Próximo** — Multi-tenant admin, dashboard operacional, SLA de entrega

---

## 📞 Contato

- **Gerente de Projeto:** @diogo  
- **Time técnico:** @backend @frontend  
- **Issues/Roadmap:** [GitHub Projects](https://github.com/diogo2806/praxis/projects)

---

**Última atualização:** 18/06/2026 | Versão: 0.1.0-alpha
