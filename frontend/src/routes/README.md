# 🗺️ Rotas — TanStack Start

> **Documentação de convenções de roteamento** para o sistema de rotas baseado em arquivos do TanStack Start.

## 📋 Sistema de Roteamento

TanStack Start utiliza **roteamento baseado em arquivos** (file-based routing). Cada arquivo `.tsx` neste diretório é automaticamente uma rota.

⚠️ **Não use convenções de outros frameworks:**
- ❌ Não crie `src/pages/`
- ❌ Não crie `src/routes/_app/index.tsx`
- ❌ Não crie `app/layout.tsx`

✅ **Convenções válidas:**
- `src/routes/__root.tsx` — Layout raiz (único permitido)
- `src/routes/**/*.tsx` — Qualquer rota filha

---

## 📂 Convenções de Arquivo → URL

| Padrão de Arquivo | URL Resultante | Tipo | Exemplo |
|---|---|---|---|
| `index.tsx` | `/` | Página inicial | `src/routes/index.tsx` → `/` |
| `about.tsx` | `/about` | Página simples | `src/routes/about.tsx` → `/about` |
| `docs/index.tsx` | `/docs` | Índice de pasta | `src/routes/docs/index.tsx` → `/docs` |
| `docs/faq.tsx` | `/docs/faq` | Aninhada | `src/routes/docs/faq.tsx` → `/docs/faq` |
| `users/$id.tsx` | `/users/:id` | Dinâmica | `src/routes/users/$id.tsx` → `/users/123` |
| `posts/{-$category}.tsx` | `/posts/:category?` | Opcional | `src/routes/posts/{-$category}.tsx` → `/posts` ou `/posts/tech` |
| `files/$.tsx` | `/files/*` | Curinga (splat) | `src/routes/files/$.tsx` → `/files/any/deep/path` |
| `_layout.tsx` | *sem URL própria* | Layout/grupo | `src/routes/docs/_layout.tsx` (envolve `/docs/**`) |
| `__root.tsx` | *sem URL própria* | Shell global | `src/routes/__root.tsx` (envolve toda a app) |

---

## 🔤 Acessar Parâmetros na Rota

### Parâmetro Dinâmico

```tsx
// Arquivo: src/routes/users/$id.tsx
import { useParams } from "@tanstack/react-router"

export default function UserPage() {
  const { id } = useParams({ from: "/users/$id" })
  return <h1>Usuário: {id}</h1>
}
```

### Parâmetro Opcional

```tsx
// Arquivo: src/routes/posts/{-$category}.tsx
import { useParams, useSearch } from "@tanstack/react-router"

export default function PostsPage() {
  const { category } = useParams({ from: "/posts/{-$category}" })
  return <h1>Categoria: {category || "Todas"}</h1>
}
```

### Curinga (Splat) — Caminhos Aninhados

```tsx
// Arquivo: src/routes/files/$.tsx
import { useParams } from "@tanstack/react-router"

export default function FileBrowser() {
  const { _splat } = useParams({ from: "/files/$" })
  return <p>Caminho: {_splat}</p>
}

// URLs:
// /files/foo → _splat = "foo"
// /files/foo/bar/baz → _splat = "foo/bar/baz"
```

---

## 📐 Layouts (Agrupamento)

### Layout de Seção

```
src/routes/
  docs/
    _layout.tsx          ← Envolve todas as rotas abaixo
    index.tsx            → /docs
    faq.tsx              → /docs/faq
    api.tsx              → /docs/api
```

**Exemplo: docs/_layout.tsx**
```tsx
import { Outlet } from "@tanstack/react-router"
import Navigation from "@/components/DocsNav"

export default function DocsLayout() {
  return (
    <div className="docs-container">
      <Navigation />
      <main>
        <Outlet />  {/* Renderiza a rota filha aqui */}
      </main>
    </div>
  )
}
```

### Layout Raiz (App Shell)

```tsx
// Arquivo: src/routes/__root.tsx
import { Outlet } from "@tanstack/react-router"
import Header from "@/components/Header"
import Footer from "@/components/Footer"

export default function RootLayout() {
  return (
    <html>
      <body>
        <Header />
        <Outlet />  {/* Toda a aplicação passa aqui */}
        <Footer />
      </body>
    </html>
  )
}
```

---

## ⚙️ Geração Automática

```
src/routes/routeTree.gen.ts  ← AUTO-GERADO
```

**⚠️ Nunca edite `routeTree.gen.ts` manualmente.** Ele é regenerado automaticamente ao:
- Adicionar/remover arquivo `.tsx`
- Alterar nomes de arquivos
- Executar `npm run dev` ou build

---

## 🏗️ Estrutura Atual (Praxis)

O menu principal da empresa fica em `src/components/app-shell.tsx`. O assistente de
criação de avaliações tem **4 passos**, definidos em `src/lib/simulation-meta.ts`
(`avaliacao → personagem → validador → governanca`).

```
src/routes/
├── __root.tsx                           # Shell global (header, estilos)
├── index.tsx                            # Tela inicial
├── dashboard.tsx                        # Painel de indicadores
├── avaliacoes.tsx                       # Ver/editar avaliações
├── results.tsx / results.$attemptId.tsx # Resultados e decisão do recrutador
├── enviar-link.tsx                      # Links internos de candidato
├── integrations.tsx                     # Central de Integrações
│   └── integrations.$provider.tsx       # Detalhe/config (gupy, recrutei, custom-api)
├── monitoramento.tsx                    # Monitoramento operacional
├── jornadas.tsx / jornada.$attemptId    # Jornadas de avaliação
├── talent-match.tsx                     # Comparação de candidatos
├── marketplace.tsx (+ filhos)           # Marketplace de profissionais
├── billing.tsx                          # Plano, uso e cobrança
├── compliance.tsx                       # Defensabilidade e LGPD (substitui /defensabilidade e /lgpd)
├── competencias.tsx                     # Catálogos da empresa
├── configuracoes.perfil / .conta        # Perfil da empresa / conta do usuário
├── team.tsx / convite.$token.tsx        # Equipe e aceite de convite
├── candidato.tsx / candidato.$token.tsx # Fluxo público do candidato
├── admin*.tsx                           # Painel ADMIN da plataforma
├── nova.avaliacao.tsx                   # Passo 1: Teste (cria rascunho)
├── nova.personagem.tsx                  # Passo 2: Cenário (nós/alternativas)
├── nova.validador.tsx                   # Passo 3: Revisão (validação)
├── nova.governanca.tsx                  # Passo 4: Publicação
├── nova.rapido.tsx                      # Quick-start por modelo pronto
├── nova.objetivo / .dialogo / .mapa /   # Telas standalone fora dos 4 passos
│   .piloto / .gupy.tsx
└── README.md                            # Este arquivo
```

> Rotas legadas redirecionam: `nova.blueprint.tsx → /nova/avaliacao`,
> `nova.competencias.tsx → /competencias`, `configuracoes.integracoes.tsx → /integrations`,
> `configuracoes.api.tsx → /integrations/custom-api`. Não há `nova/_layout.tsx`;
> o passo atual é renderizado pelo componente `WizardStepper`.

---

## 💡 Dicas Práticas

### 1️⃣ Dinâmica com Busca (Query String)

```tsx
// Arquivo: src/routes/nova/validador.tsx
import { useSearch } from "@tanstack/react-router"

export default function ValidadorPage() {
  const { simulationId, versionNumber } = useSearch({ 
    from: "/nova/validador" 
  })
  
  return <h1>Validando: {simulationId} v{versionNumber}</h1>
}

// Uso: /nova/validador?simulationId=123&versionNumber=1
```

### 2️⃣ Navegação Programática

```tsx
import { useNavigate } from "@tanstack/react-router"

function MyComponent() {
  const navigate = useNavigate()
  
  const goToUser = (id: string) => {
    navigate({ to: "/users/$id", params: { id } })
  }
  
  return <button onClick={() => goToUser("42")}>Ver Usuário 42</button>
}
```

### 3️⃣ Link Tipado

```tsx
import { Link } from "@tanstack/react-router"

function Navigation() {
  return (
    <nav>
      <Link to="/">Início</Link>
      <Link to="/users/$id" params={{ id: "123" }}>Usuário 123</Link>
      <Link to="/nova/avaliacao">Criar Simulação</Link>
    </nav>
  )
}
```

---

## 🚀 Referência Rápida

| Tarefa | Solução |
|---|---|
| Criar página simples | Arquivo `.tsx` na raiz de `src/routes/` |
| Parâmetro dinâmico | Usar `$` no nome: `$id.tsx` |
| Parâmetro opcional | Usar `{-$var}.tsx` |
| Capturar caminho completo | Usar `$.tsx` (splat) |
| Compartilhar layout | Criar `_layout.tsx` na pasta |
| CSS/componentes global | Usar `__root.tsx` |
| Query string | `useSearch()` hook |
| Parâmetro de rota | `useParams()` hook |
| Navegar programaticamente | `useNavigate()` hook |

---

## 📖 Documentação Oficial

- [TanStack Router Docs](https://tanstack.com/router/latest)
- [TanStack Start Docs](https://tanstack.com/start/latest)

---

**Última atualização:** 03/07/2026 | Tradução: PT-BR
