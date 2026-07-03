# Documentação de Implantação — Práxis

> **Propósito:** permitir instalar o Práxis do zero seguindo apenas esta documentação.
>
> **Escopo:** Parte B do requisito de Documentação Operacional e de Implantação. A operação em
> produção está em [OPERACAO.md](OPERACAO.md).
>
> **Regra de sincronização:** sempre que uma funcionalidade alterar a implantação (nova variável,
> nova dependência, nova migração que exija passo manual), atualize este documento na mesma entrega.

Fiel a `docker-compose.yml`, `backend/Dockerfile`, `backend/pom.xml`,
`backend/src/main/resources/application.properties` e às migrações Flyway.

---

## 13. Pré-requisitos

| Componente | Versão | Observação |
| --- | --- | --- |
| Java (JDK) | 21 | `java.version=21` no `pom.xml` |
| Maven | 3.9+ | Build do backend (`maven:3.9-eclipse-temurin-21` no Dockerfile) |
| PostgreSQL | 14+ (validado em 17) | `postgres:17-alpine` no compose |
| Docker / Docker Compose | recente | Quando usar containers |
| Node.js + pnpm/npm | Node 20+ | Build do frontend (`pnpm` dev, `npm ci` no container) |
| AWS S3 ou MinIO | — | Opcional; só para mídia |
| Conta Mercado Pago | — | Opcional; só para billing (Parte B) |
| Acesso Gupy/Recrutei | — | Opcional; só para integração ATS |

---

## 14. Variáveis de ambiente

> O requisito lista nomes de exemplo (`JWT_SECRET`, `DATABASE_URL`, `AWS_ACCESS_KEY`, `S3_BUCKET`…).
> Os nomes **reais** lidos pelo backend são os abaixo. A coluna "Equivalente do requisito" faz o de-para.

### 14.1 Obrigatórias em produção

| Variável real | Finalidade | Equivalente do requisito |
| --- | --- | --- |
| `PRAXIS_JWT_SECRET` | Segredo de assinatura do JWT (sem padrão; app não autentica sem ela) | `JWT_SECRET` |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | Host, porta e nome do banco (compõem o JDBC URL) | `DATABASE_URL` |
| `DB_USER` | Usuário do banco | `DATABASE_USER` |
| `DB_PASS` (ou `SPRING_DATASOURCE_PASSWORD`) | Senha do banco | `DATABASE_PASSWORD` |
| `PRAXIS_PUBLIC_BASE_URL` | Base pública para links/resultados | `PUBLIC_BASE_URL` |

### 14.2 Recomendadas

| Variável real | Finalidade | Padrão |
| --- | --- | --- |
| `DB_SCHEMA` | Schema do Flyway/JPA | `public` |
| `PRAXIS_JPA_DDL_AUTO` | `validate` em produção | `none` |
| `PRAXIS_SECURITY_ENABLED` | Manter `true` em produção | `true` |
| `PRAXIS_CORS_ALLOWED_ORIGINS` | Origens do frontend | dev localhost |
| `PRAXIS_CANDIDATE_PAGE_BASE_URL` | Base pública do candidato | herda `PRAXIS_PUBLIC_BASE_URL` |
| `PRAXIS_ADMIN_BOOTSTRAP_EMAIL` | Operador ADMIN inicial | vazio |
| `PRAXIS_ADMIN_BOOTSTRAP_PASSWORD` | Senha do ADMIN inicial | vazio |
| `SPRING_PROFILES_ACTIVE` | Perfil ativo (compose usa `prod`) | — |

### 14.3 Object Storage (S3) — opcional

| Variável real | Finalidade | Equivalente do requisito |
| --- | --- | --- |
| `OBJECT_STORAGE_ACCESS_KEY` | Chave de acesso | `AWS_ACCESS_KEY` |
| `OBJECT_STORAGE_SECRET_KEY` | Chave secreta | `AWS_SECRET_KEY` |
| `OBJECT_STORAGE_BUCKET` | Bucket (padrão `praxis-media`) | `S3_BUCKET` |
| `OBJECT_STORAGE_ENDPOINT` | Endpoint S3-compatível | — |
| `OBJECT_STORAGE_PUBLIC_URL` | URL pública dos objetos | — |
| `OBJECT_STORAGE_REGION` | Região (padrão `us-east-1`) | — |
| `OBJECT_STORAGE_PATH_STYLE` | Path-style (MinIO), padrão `true` | — |

### 14.4 Mercado Pago (billing) — opcional

| Variável real | Finalidade | Equivalente do requisito |
| --- | --- | --- |
| `MP_ENABLED` | Habilita billing (padrão `false`) | — |
| `MP_ACCESS_TOKEN` | Access Token (segredo, só backend) | `MERCADO_PAGO_ACCESS_TOKEN` |
| `MP_PUBLIC_KEY` | Public Key | — |
| `MP_WEBHOOK_SECRET` | Valida assinatura do webhook | — |
| `MP_NOTIFICATION_URL` | URL pública do webhook | — |

> A variável `ADMIN_BOOTSTRAP_EMAIL`/`ADMIN_BOOTSTRAP_PASSWORD` do requisito corresponde a
> `PRAXIS_ADMIN_BOOTSTRAP_EMAIL`/`PRAXIS_ADMIN_BOOTSTRAP_PASSWORD`. A Gupy não usa variável de
> ambiente para autenticar: o token é validado contra a tabela `integration_tokens` no banco.

---

## 15. Banco

### 15.1 Criar banco, usuário e permissões

```sql
CREATE DATABASE praxis;
CREATE USER praxis WITH ENCRYPTED PASSWORD 'senha-forte';
GRANT ALL PRIVILEGES ON DATABASE praxis TO praxis;
-- No banco praxis, garantir o schema usado (DB_SCHEMA, padrão "public"):
\connect praxis
GRANT ALL ON SCHEMA public TO praxis;
```

### 15.2 Flyway e migrações

- O Flyway está habilitado (`spring.flyway.enabled=true`) e roda **automaticamente no startup**.
- As migrações ficam em `backend/src/main/resources/db/migration` (versões `V1..V47`) e em
  `.../postgresql` para SQL específico do PostgreSQL.
- `spring.flyway.default-schema`/`schemas` usam `DB_SCHEMA` (padrão `public`).
- Não é necessário criar tabelas manualmente: subir o backend aplica todo o schema, incluindo seeds
  (ex.: `V16__seed_default_empresa`, `V44__seed_platform_empresa`).
- Conferir estado: `mvn -pl backend flyway:info` (ou logs de startup do backend).

---

## 16. Build

### Compilar (backend)

```bash
cd backend
mvn clean package
```

Gera `target/praxis-backend-0.1.0-SNAPSHOT.jar` (fat jar Spring Boot). Para pular testes:
`mvn clean package -DskipTests`.

### Executar (backend)

```bash
java -jar target/praxis-backend-0.1.0-SNAPSHOT.jar
```

Variáveis mínimas para subir: `PRAXIS_JWT_SECRET`, `DB_HOST/DB_PORT/DB_NAME`, `DB_USER`, `DB_PASS`.

### Frontend

```bash
cd frontend
pnpm install   # dev local
pnpm build     # build de produção (container usa npm ci)
```

---

## 17. Deploy

### 17.1 Servidor Linux (jar)

1. Instalar JRE 21 e PostgreSQL (ou apontar para banco gerenciado).
2. Copiar o jar e definir as variáveis de ambiente (§14).
3. Executar como serviço (systemd) com `java -jar app.jar`.
4. Colocar atrás de um reverse proxy (Nginx) com HTTPS (§18).

### 17.2 Docker / Docker Compose

O repositório já traz `docker-compose.yml` (backend + frontend + postgres). Crie um `.env` na raiz:

```bash
POSTGRES_USER=praxis
POSTGRES_PASSWORD=troque-esta-senha
PRAXIS_INTEGRATION_TOKEN=troque-este-token
PRAXIS_JWT_SECRET=troque-este-segredo-com-tamanho-suficiente
PRAXIS_SECURITY_ENABLED=true
PRAXIS_PUBLIC_BASE_URL=https://app.seu-dominio.com.br
PRAXIS_CANDIDATE_PAGE_BASE_URL=https://app.seu-dominio.com.br
```

Subir tudo:

```bash
docker compose up --build
```

Serviços resultantes:

- Backend: `http://localhost:8080` (`PRAXIS_JPA_DDL_AUTO=validate`, `SPRING_PROFILES_ACTIVE=prod`).
- Frontend: `http://localhost` (Nginx na porta 80).
- PostgreSQL: rede interna do Compose, volume `postgres_data`.

> O Compose exige `PRAXIS_INTEGRATION_TOKEN`, mas a autenticação real de `/test/**` (Gupy/Recrutei)
> não lê essa variável. O token é cadastrado pela área de Integrações da empresa
> (`POST /api/v1/integrations/{provider}/tokens`), que gera um token `prx_...`, o exibe uma única
> vez e guarda apenas o SHA-256 Base64URL dele na tabela `integration_tokens`.

### 17.3 Cloud / homologação / produção

- **Imagem:** o `backend/Dockerfile` faz build multi-stage (Maven → JRE 21) e expõe `8080` com
  `HEALTHCHECK` em `/actuator/health`. Publique em um registry e implante (ECS, Kubernetes, etc.).
- **Banco:** prefira PostgreSQL gerenciado; aponte `DB_*` para ele.
- **Segredos:** injete `PRAXIS_JWT_SECRET`, `DB_PASS`, `MP_*` e `OBJECT_STORAGE_*` por secret manager,
  nunca em imagem ou repositório.
- **Homologação × Produção:** diferencie por variáveis de ambiente (URLs, credenciais, `MP_ENABLED`,
  CORS). Não há arquivo `application-prod.properties` adicional — o perfil `prod` apenas é ativado.
- **Recursos JVM:** o Dockerfile usa `-Xmx256m`; ajuste `JAVA_OPTS` conforme a carga.

---

## 18. SSL / HTTPS

- O backend roda HTTP atrás de um **reverse proxy** que termina TLS (Nginx, Traefik, ALB).
- `server.forward-headers-strategy=framework` já está habilitado: o backend respeita
  `X-Forwarded-Proto/Host` para gerar URLs públicas corretas.
- Configure `PRAXIS_PUBLIC_BASE_URL`/`PRAXIS_CANDIDATE_PAGE_BASE_URL` com o domínio **https** público.
- Use certificados válidos (Let's Encrypt ou corporativos). Exemplo Nginx:

```nginx
server {
  listen 443 ssl;
  server_name app.seu-dominio.com.br;
  ssl_certificate     /etc/letsencrypt/live/app/fullchain.pem;
  ssl_certificate_key /etc/letsencrypt/live/app/privkey.pem;
  location / {
    proxy_pass http://backend:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
  }
}
```

---

## 19. Segurança

| Item | Como está implementado |
| --- | --- |
| JWT | Tokens assinados com `PRAXIS_JWT_SECRET`; sessão `STATELESS`; expiração `PRAXIS_JWT_EXPIRATION_HOURS` (8h). |
| BCrypt | Senhas com `BCryptPasswordEncoder` (`SecurityConfig`). |
| CORS | `WebConfig` libera origens de `PRAXIS_CORS_ALLOWED_ORIGINS`; defina o domínio do frontend em produção. |
| Headers | `frameOptions(deny)` e `contentTypeOptions` ativos. CSRF desabilitado (API stateless). |
| Roles | `ADMIN` para `/api/admin/**`; `EMPRESA` para `/api/v1/**` protegidas (`SecurityConfig`). |
| Empresa | Isolamento por empresa via JWT/token; `SUSPENSO`/`CANCELADO` bloqueiam acesso. |
| Proteção de Webhook | `/api/webhooks/mercado-pago` valida assinatura `x-signature` (`MP_WEBHOOK_SECRET`), idempotência e consulta a API MP antes de aplicar mudança financeira. |
| Tokens de integração (ATS) | Comparação por SHA-256 Base64URL contra a tabela `integration_tokens` (por provider); o token em claro nunca é persistido. |
| Secrets | `PRAXIS_JWT_SECRET`, `DB_PASS`, `MP_*`, `OBJECT_STORAGE_*` só por variável de ambiente/secret manager. Nunca commitar. |

> **Importante:** `PRAXIS_SECURITY_ENABLED=false` libera todas as rotas e usa
> `PRAXIS_DEFAULT_EMPRESA_ID`. Use **apenas em desenvolvimento**, nunca em produção.

---

## 20. Pós-implantação (checklist)

- [ ] Banco criado e acessível (`DB_*` corretos).
- [ ] Migrações executadas (Flyway `OK` nos logs / `flyway:info`).
- [ ] Operador ADMIN bootstrap criado (`PRAXIS_ADMIN_BOOTSTRAP_EMAIL`/`_PASSWORD`; empresa `PLATFORM` existe).
- [ ] Login funcionando (`POST /api/v1/auth/login`).
- [ ] Upload de mídia funcionando (`POST /api/v1/media`) — se `OBJECT_STORAGE_*` configurado.
- [ ] Mercado Pago conectado — se `MP_ENABLED=true` (webhook e `MP_*` válidos).
- [ ] Gupy/Recrutei conectada — token cadastrado pela área de Integrações (guardado em `integration_tokens`); `GET /test` responde.
- [ ] Health Check `UP` (`GET /actuator/health`).
- [ ] Auditoria funcionando (ações geram eventos em `audit_events`).
- [ ] Backup configurado (banco + storage) — ver [OPERACAO.md §8](OPERACAO.md#8-backup).
- [ ] HTTPS ativo e `PRAXIS_PUBLIC_BASE_URL` com domínio público.
- [ ] CORS restrito ao domínio do frontend (`PRAXIS_CORS_ALLOWED_ORIGINS`).

---

## 21. Critério de aceite

A documentação é considerada concluída quando:

- ✅ Toda configuração necessária para executar o sistema está documentada (§4 de OPERACAO + §14).
- ✅ Um novo ambiente pode ser instalado seguindo apenas esta documentação (§13–§20).
- ✅ Os principais fluxos operacionais estão descritos ([OPERACAO §6](OPERACAO.md#6-fluxos-operacionais)).
- ✅ As rotinas de backup e recuperação estão documentadas ([OPERACAO §8](OPERACAO.md#8-backup)).
- ✅ Os procedimentos de atualização e rollback estão definidos ([OPERACAO §12](OPERACAO.md#12-atualizações-procedimento-oficial)).
- ✅ Todas as variáveis de ambiente estão descritas (§14 e [OPERACAO §4](OPERACAO.md#4-configurações)).
- ✅ As integrações externas estão documentadas ([OPERACAO §2.6](OPERACAO.md#26-integrações-externas), Gupy, Recrutei, Mercado Pago).
- ✅ A documentação permanece sincronizada com a versão do sistema (ver rodapé).

**Versão do sistema coberta:** backend `0.1.0-SNAPSHOT`, Spring Boot 3.5.3, Java 21.
**Última revisão:** 30/06/2026.
