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

## 🏗️ Estrutura Recomendada (Praxis)

```
src/routes/
├── __root.tsx                           # Header, footer, estilos globais
├── index.tsx                            # Tela inicial (/simulations)
├── candidato.tsx                        # Entrada por token (/candidato)
├── candidato/
│   └── $token.tsx                       # Tela pública do candidato
├── monitoramento.tsx                    # Dashboard operacional
├── governanca.tsx                       # Auditoria e governança
├── defensabilidade.tsx                  # Explicabilidade de score
├── lgpd.tsx                             # Conformidade LGPD
├── nova/                                # Assistente de criação
│   ├── _layout.tsx                      # Sidebar do wizard
│   ├── avaliacao.tsx                    # Passo 1: criar simulação
│   ├── blueprint.tsx                    # Redirect de compatibilidade → /nova/avaliacao
│   ├── objetivo.tsx                     # Passo 2: objetivo e competências
│   ├── personagem.tsx                   # Passo 3: personagem principal
│   ├── dialogo.tsx                      # Passo 4: diálogos/nós
│   ├── validador.tsx                    # Passo 5: validação
│   ├── piloto.tsx                       # Passo 6: calibração
│   ├── mapa.tsx                         # Passo 7: mapa de score
│   ├── governanca.tsx                   # Passo 8: governança
│   └── gupy.tsx                         # Passo 9: ativar Gupy
└── README.md                            # Este arquivo
```

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

**Última atualização:** 18/06/2026 | Tradução: PT-BR
