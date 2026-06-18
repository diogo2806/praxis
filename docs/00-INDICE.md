# 📚 Índice de Documentação — Praxis

> **Guia centralizado para navegar toda a documentação do Praxis.** Selecione seu perfil ou interesse para começar.

---

## 👥 Começar por Perfil

### 🚀 Novo Desenvolvedor?
Começar aqui para entender a arquitetura e estrutura do projeto:

1. [📖 README principal](../README.md) — Visão geral, garantias, papéis
2. [🏗️ Arquitetura do Sistema](ARQUITETURA_OUTBOX_PATTERN.md) — Outbox, eventos, ATS
3. [🔌 Integração Gupy](INTEGRACAO-GUPY-PROVEDOR.md) — Endpoints e contrato
4. [🎨 Mapa Frontend-Backend](frontend-backend-map.md) — Rotas, APIs, integração
5. [🗂️ Rotas TanStack Start](../frontend/src/routes/README.md) — Sistema de roteamento

### 🏢 Operador (RH/Gestor)?
Documentação para gerenciar simulações e monitorar entregas:

1. [📊 Relatório de Usabilidade](USABILIDADE_IMPLEMENTACAO.md) — Interfaces e fluxo
2. [🎯 Governança e Publicação](frontend-backend-map.md#governança) — Workflow de aprovação
3. [📈 Monitoramento Operacional](frontend-backend-map.md#monitoramento) — Dashboard de entregas

### 🔧 Engenheiro Backend (Java)?
Implementação, regras de negócio e padrões de código:

1. [🏗️ Arquitetura Outbox](ARQUITETURA_OUTBOX_PATTERN.md) — Transações, outbox, retry
2. [🎯 Garantias de Produção](../README.md#-garantias-de-produção) — Segurança, isolamento, determinismo
3. [🔌 Integração Gupy (backend)](INTEGRACAO-GUPY-PROVEDOR.md#-segurança-e-autenticação) — Filtros, validação, webhook

### 🎨 Engenheiro Frontend (React/TypeScript)?
Componentes, rotas, integração com backend:

1. [🎨 Mapa Frontend-Backend](frontend-backend-map.md) — Rotas e endpoints
2. [🗂️ Rotas TanStack Start](../frontend/src/routes/README.md) — Convenções de roteamento
3. [📋 Implementação Usabilidade](USABILIDADE_IMPLEMENTACAO.md) — UX/UI, glossário, estados

### 🔐 Responsável de Segurança/Compliance?
Garantias de conformidade, auditoria e privacidade:

1. [🔐 Garantias de Produção](../README.md#-garantias-de-produção) — Segurança, webhooks, LGPD
2. [🏗️ Arquitetura Outbox](ARQUITETURA_OUTBOX_PATTERN.md#fluxo) — Auditoria append-only
3. [🔌 Integração Gupy (segurança)](INTEGRACAO-GUPY-PROVEDOR.md#-segurança-e-autenticação) — Isolamento, HMAC, allow-list

---

## 📖 Documentos por Categoria

### Arquitetura & Design
| Documento | Escopo | Leitor |
|---|---|---|
| [Arquitetura Outbox Pattern](ARQUITETURA_OUTBOX_PATTERN.md) | Outbox transacional, eventos, ATS | Backend, Arquiteto |
| [Mapa Frontend-Backend](frontend-backend-map.md) | Rotas, APIs, integração | Frontend, Backend, PM |
| [Implementação Summary](IMPLEMENTATION_SUMMARY.md) | Resumo técnico do MVP | Backend, Arquiteto |

### Integrações
| Documento | Escopo | Leitor |
|---|---|---|
| [Integração Gupy](INTEGRACAO-GUPY-PROVEDOR.md) | Endpoints, contrato, fluxo E2E | Backend, Frontend, Integração |

### Operacional
| Documento | Escopo | Leitor |
|---|---|---|
| [Usabilidade — Implementação](USABILIDADE_IMPLEMENTACAO.md) | UX/UI, glossário, acessibilidade | Frontend, PM, RH |

### Pesquisa & Contexto
| Documento | Escopo | Leitor |
|---|---|---|
| [Reclamações sobre Concorrentes](research/competitor-complaints-research-2025-2026.md) | Oportunidades de mercado, dores | PM, Estratégia |

### Desenvolvimento
| Documento | Escopo | Leitor |
|---|---|---|
| [Regras Backend Java](backend-java.md) | Padrões, SOLID, segurança | Backend |

### Frontend
| Documento | Escopo | Leitor |
|---|---|---|
| [Rotas TanStack Start](../frontend/src/routes/README.md) | Roteamento, convenções | Frontend |

---

## 🔍 Buscar por Tópico

### Banco de Dados & Persistência
- [Arquitetura Outbox](ARQUITETURA_OUTBOX_PATTERN.md) — Transações, PostgreSQL, append-only

### Autenticação & Segurança
- [Garantias de Produção](../README.md#-garantias-de-produção) — JWT, role-based access, tenant isolado
- [Integração Gupy — Segurança](INTEGRACAO-GUPY-PROVEDOR.md#-segurança-e-autenticação) — API Key, webhook HMAC, whitelist

### Score & Determinismo
- [Garantias de Produção](../README.md#-garantias-de-produção) — Score determinístico (sem IA)
- [Implementação Summary](IMPLEMENTATION_SUMMARY.md) — Verificação e build

### LGPD & Compliance
- [Garantias de Produção](../README.md#-garantias-de-produção) — Anonimização, retenção
- [Mapa Frontend-Backend](frontend-backend-map.md#lgpd) — Tela `/lgpd`

### Integração Gupy
- [Integração Gupy (completo)](INTEGRACAO-GUPY-PROVEDOR.md)
- [Mapa Frontend-Backend](frontend-backend-map.md#integracao-gupy-da-empresa) — APIs Gupy, entregas

### Monitoramento & Observabilidade
- [Mapa Frontend-Backend](frontend-backend-map.md#monitoramento) — Dashboard, métricas
- [Integração Gupy — Monitoramento](INTEGRACAO-GUPY-PROVEDOR.md#-monitoramento) — Entregas, retry, DLQ

### Testes & Qualidade
- [Implementação Summary](IMPLEMENTATION_SUMMARY.md) — Verificação de build, testes
- [Usabilidade — Implementação](USABILIDADE_IMPLEMENTACAO.md) — Testes de acessibilidade (WCAG)

### Frontend & Rotas
- [Mapa Frontend-Backend](frontend-backend-map.md) — Todas as rotas, integração
- [Rotas TanStack Start](../frontend/src/routes/README.md) — Convenções, setup

### Fluxos de Negócio
- [Integração Gupy — Fluxo E2E](INTEGRACAO-GUPY-PROVEDOR.md#-fluxo-completo-end-to-end) — Passo a passo
- [Mapa Frontend-Backend](frontend-backend-map.md) — Rotas operacionais

---

## 📋 Estado da Documentação

| Documento | Status | Atualizado | Completo? |
|---|---|---|---|
| README (raiz) | ✅ Atualizado | 18/06/2026 | ✅ Sim |
| Índice (este arquivo) | ✅ Novo | 18/06/2026 | ✅ Sim |
| Arquitetura Outbox | ✅ Atual | 18/06/2026 | ✅ Sim |
| Integração Gupy | ✅ Expandido | 18/06/2026 | ✅ Sim |
| Mapa Frontend-Backend | ✅ Atual | 18/06/2026 | ✅ Sim |
| Rotas TanStack | ✅ Traduzido | 18/06/2026 | ✅ Sim |
| Usabilidade | ✅ Histórico | 18/06/2026 | ⏳ Parcial |
| Backend Java | ⚠️ Rascunho | 18/06/2026 | ⏳ Parcial |
| Pesquisa Concorrentes | ✅ Atual | 16/06/2026 | ✅ Sim |

---

## 🔗 Links Internos Rápidos

**Raiz:**
- [README principal](../README.md)

**Docs:**
- [Arquitetura](ARQUITETURA_OUTBOX_PATTERN.md)
- [Integração Gupy](INTEGRACAO-GUPY-PROVEDOR.md)
- [Mapa Frontend-Backend](frontend-backend-map.md)
- [Usabilidade](USABILIDADE_IMPLEMENTACAO.md)

**Frontend:**
- [Rotas TanStack](../frontend/src/routes/README.md)

**Pesquisa:**
- [Reclamações Concorrentes](research/competitor-complaints-research-2025-2026.md)

---

## 📞 Como Contribuir

1. **Atualizar documentação:** Editar arquivo existente → Criar PR → Review
2. **Adicionar novo documento:** Criar em `docs/` com nome descritivo → Adicionar ao índice
3. **Reportar erro:** Issues no GitHub com label `documentation`

**Padrão de nomenclatura:**
- Usar kebab-case para nomes: `NOME-DESCRITIVO.md`
- Usar CAPS para acronyms: `LGPD`, `API`, `JWT`
- Começar número para ordenar: `00-INDICE.md`, `01-GARANTIAS.md`

**Padrão de conteúdo:**
```markdown
# Título — Subtítulo (se houver)

> **Propósito:** Uma frase.  
> **Público:** Perfis que devem ler.  
> **Status:** ✅ Completo / 🔮 Em progresso / ⚠️ Depreciado.

## Seção

Conteúdo...
```

---

**Última atualização:** 18/06/2026  
**Mantido por:** @time-dev  
**Próxima revisão:** 01/07/2026
