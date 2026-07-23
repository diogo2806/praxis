# Documentação de Implantação — Práxis

> **Propósito:** permitir instalar o Práxis do zero seguindo apenas esta documentação.
>
> **Escopo:** Parte B do requisito de Documentação Operacional e de Implantação. A operação em
> produção está em [OPERACAO.md](OPERACAO.md).
>
> **Regra de sincronização:** sempre que uma funcionalidade alterar a implantação (nova variável,
> nova dependência, nova migração que exija passo manual), atualize este documento na mesma entrega.

Fiel a `docker-compose.yml`, `backend/Dockerfile`, `frontend/Dockerfile`, `backend/pom.xml`,
`backend/src/main/resources/application.properties` e às migrações Flyway da `main` no commit
`1f6ff281210e6aa71b1880da1119d22f8aabb68e`.

---

## 13. Pré-requisitos

| Componente | Versão | Observação |
| --- | --- | --- |
| Java (JDK) | 21 | `java.version=21` no `pom.xml` |
| Maven | 3.9+ | Build do backend (`maven:3.9-eclipse-temurin-21` no Dockerfile) |
| PostgreSQL | 14+ (validado em 17) | `postgres:17-alpine` no compose |
| Docker / Docker Compose | recente | Quando usar containers |
| Node.js + pnpm/npm | Node 22 no container | Build do frontend (`pnpm` dev, `npm ci` no container) |
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
| `PRAXIS_PUBLIC_BASE_URL` | Base pública HTTPS para links/resultados | `PUBLIC_BASE_URL` |
| `PRAXIS_CANDIDATE_PAGE_BASE_URL` | Base pública HTTPS do candidato | — |
| `PRAXIS_CORS_ALLOWED_ORIGINS` | Origens HTTPS permitidas | — |
| `PRAXIS_PRIVACY_CONTROLLER_NAME` | Identificação do controlador LGPD | — |
| `PRAXIS_PRIVACY_SERVICE_EMAIL` ou `PRAXIS_PRIVACY_SERVICE_URL` | Canal de privacidade | — |

O perfil `prod` valida essas configurações no startup. SMTP, Mercado Pago e Object Storage também são
validados quando os respectivos módulos estão habilitados/configurados.

### 14.2 Recomendadas

| Variável real | Finalidade | Padrão |
| --- | --- | --- |
| `DB_SCHEMA` | Schema do Flyway/JPA | `public` |
| `PRAXIS_JPA_DDL_AUTO` | `validate` em produção | `none` |
| `PRAXIS_SECURITY_ENABLED` | Manter `true` em produção | `true` |
| `PRAXIS_ADMIN_BOOTSTRAP_EMAIL` | Operador ADMIN inicial | vazio |
| `PRAXIS_ADMIN_BOOTSTRAP_PASSWORD` | Senha do ADMIN inicial | vazio |
| `SPRING_PROFILES_ACTIVE` | Perfil ativo (compose usa `prod`) | — |
| `SPRINGDOC_SWAGGER_UI_ENABLED` | Habilita Swagger UI em `/docs` | `false` |
| `SPRINGDOC_API_DOCS_ENABLED` | Habilita OpenAPI em `/v3/api-docs` | `false` |
| `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE` | Endpoints Actuator expostos | `health,info` |

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
| `MP_NOTIFICATION_URL` | URL pública HTTPS do webhook | — |

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
- As migrações ficam em `backend/src/main/resources/db/migration`, com sequência versionada atualmente acima de `V1000`, e em `.../postgresql` para SQL específico do PostgreSQL.
- Não fixe um intervalo numérico no runbook: consulte o diretório de migrations ou `flyway:info` para identificar a versão mais recente.
- `spring.flyway.default-schema`/`schemas` usam `DB_SCHEMA` (padrão `public`).
- `spring.flyway.out-of-order` é `false` por padrão e permanece forçado como `false` no perfil `prod`.
- Não é necessário criar tabelas manualmente: subir o backend aplica todo o schema e os seeds versionados.
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

Em produção, ative `SPRING_PROFILES_ACTIVE=prod` e forneça todas as configurações exigidas pelo
validador de startup, não apenas as credenciais mínimas de desenvolvimento.

### Frontend

```bash
cd frontend
pnpm install   # dev local
pnpm build     # build de produção (container usa npm ci)
```

O build gera `.output`; o runtime do container executa `.output/server/index.mjs` com Node.js 22.

---

## 17. Deploy

### 17.1 Servidor Linux (jar)

1. Instalar JRE 21 e PostgreSQL (ou apontar para banco gerenciado).
2. Copiar o jar e definir as variáveis de ambiente (§14).
3. Executar como serviço (systemd) com `java -jar app.jar`.
4. Colocar atrás de um reverse proxy (Nginx, Traefik ou ALB) com HTTPS (§18).

### 17.2 Docker / Docker Compose

O repositório traz `docker-compose.yml` para backend, frontend e PostgreSQL. Crie um `.env` na raiz
com as credenciais e URLs exigidas pelo perfil `prod`, por exemplo:

```bash
POSTGRES_USER=praxis
POSTGRES_PASSWORD=troque-esta-senha
PRAXIS_JWT_SECRET=troque-este-segredo-com-tamanho-suficiente
PRAXIS_SECURITY_ENABLED=true
PRAXIS_PUBLIC_BASE_URL=https://app.seu-dominio.com.br
PRAXIS_CANDIDATE_PAGE_BASE_URL=https://app.seu-dominio.com.br
PRAXIS_CORS_ALLOWED_ORIGINS=https://app.seu-dominio.com.br
PRAXIS_PRIVACY_CONTROLLER_NAME="Empresa responsável"
PRAXIS_PRIVACY_SERVICE_EMAIL=privacidade@seu-dominio.com.br
```

Subir tudo:

```bash
docker compose up --build
```

Serviços resultantes:

- Backend: `http://localhost:8080` (`PRAXIS_JPA_DDL_AUTO=validate`, `SPRING_PROFILES_ACTIVE=prod`).
- Frontend: `http://localhost`, servidor Node.js gerado pelo TanStack Start na porta 80.
- PostgreSQL: rede interna do Compose, volume `postgres_data`.

> As integrações Gupy e Recrutei não usam uma credencial global de ambiente. O token é cadastrado
> pela área de Integrações da empresa (`POST /api/v1/integrations/{provider}/tokens`), que gera um
> token `prx_...`, o exibe uma única vez e guarda apenas o SHA-256 Base64URL dele na tabela
> `integration_tokens`.

### 17.3 Cloud / homologação / produção

- **Backend:** o `backend/Dockerfile` faz build multi-stage (Maven → JRE 21), expõe `8080` e possui
  `HEALTHCHECK` em `/actuator/health`.
- **Frontend:** o `frontend/Dockerfile` faz build com Node.js 22 e executa o servidor SSR do TanStack
  Start com `node .output/server/index.mjs`, expondo a porta `80`.
- **Banco:** prefira PostgreSQL gerenciado; aponte `DB_*` para ele.
- **Segredos:** injete `PRAXIS_JWT_SECRET`, `DB_PASS`, `MP_*` e `OBJECT_STORAGE_*` por secret manager,
  nunca em imagem ou repositório.
- **Homologação × Produção:** diferencie por variáveis de ambiente e mantenha o perfil `prod` ativo.
- **Recursos JVM:** ajuste `JAVA_OPTS` conforme a carga e os limites do container.

---

## 18. SSL / HTTPS e proxies confiáveis

- O backend roda HTTP atrás de um **reverse proxy** que termina TLS (Nginx, Traefik, ALB).
- `server.forward-headers-strategy=native` está habilitado.
- O Tomcat só aceita `X-Forwarded-For` e `X-Forwarded-Proto` quando o peer imediato corresponde a
  `PRAXIS_TRUSTED_PROXY_REGEX`; ajuste a expressão para a rede real da infraestrutura.
- Configure `PRAXIS_PUBLIC_BASE_URL`/`PRAXIS_CANDIDATE_PAGE_BASE_URL` com o domínio **https** público.
- Use certificados válidos. Exemplo Nginx:

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
| CORS | `WebConfig` libera origens de `PRAXIS_CORS_ALLOWED_ORIGINS`; em produção são exigidas origens HTTPS sem wildcard. |
| Headers | `frameOptions(deny)` e `contentTypeOptions` ativos. CSRF desabilitado (API stateless). |
| Papéis | `ADMIN` e `EMPRESA`, com subperfis `TEAM_MANAGER`, `ASSESSMENT_EDITOR`, `RESULTS_ANALYST`, `OPERATIONS_MANAGER`, `PARTNER_MANAGER` e `PARTNER_SPECIALIST` conforme a rota. |
| Empresa | Isolamento por empresa via JWT/token; `SUSPENSO`/`CANCELADO` bloqueiam acesso. |
| Recuperação de senha | Rotas públicas `/api/v1/auth/password/forgot` e `/api/v1/auth/password/reset`, com token temporário e limite de tentativas. |
| Proteção de Webhook | `/api/webhooks/mercado-pago` valida assinatura, idempotência e consulta a API MP antes de aplicar mudança financeira. |
| Tokens de integração | Comparação por SHA-256 Base64URL; o token em claro nunca é persistido. |
| Proxies | Cabeçalhos encaminhados só são aceitos de peers correspondentes a `PRAXIS_TRUSTED_PROXY_REGEX`. |
| Secrets | Segredos somente por variável de ambiente/secret manager. Nunca commitar. |

> **Importante:** `PRAXIS_SECURITY_ENABLED=false` libera todas as rotas e usa
> `PRAXIS_DEFAULT_EMPRESA_ID`. Use **apenas em desenvolvimento**, nunca em produção.
>
> **Travas de inicialização:** o perfil `prod` recusa iniciar com segurança desabilitada, segredo JWT
> fraco/ausente, URLs ou CORS inseguros, informações LGPD ausentes ou integrações parcialmente
> configuradas.

---

## 20. Pós-implantação (checklist)

- [ ] Banco criado e acessível (`DB_*` corretos).
- [ ] Migrações executadas (Flyway `OK` nos logs / `flyway:info`).
- [ ] Operador ADMIN bootstrap criado, quando necessário.
- [ ] Login funcionando (`POST /api/v1/auth/login`).
- [ ] Recuperação de senha validada (`/forgot` e `/reset`) com SMTP habilitado.
- [ ] Upload de mídia funcionando — se Object Storage configurado.
- [ ] Mercado Pago conectado — se `MP_ENABLED=true`.
- [ ] Gupy/Recrutei conectada — token cadastrado e `GET /test` respondendo.
- [ ] Health Check `UP` (`GET /actuator/health`) e build identificado em `/actuator/info`.
- [ ] Swagger indisponível, ou restrito a `ADMIN` quando explicitamente habilitado.
- [ ] Auditoria funcionando.
- [ ] Backup configurado — ver [OPERACAO.md §8](OPERACAO.md#8-backup).
- [ ] HTTPS ativo e URLs públicas corretas.
- [ ] CORS e proxies confiáveis restritos à infraestrutura real.

---

## 21. Critério de aceite

A documentação é considerada concluída quando:

- ✅ Toda configuração necessária para executar o sistema está documentada (§4 de OPERACAO + §14).
- ✅ Um novo ambiente pode ser instalado seguindo apenas esta documentação (§13–§20).
- ✅ Os principais fluxos operacionais estão descritos ([OPERACAO §6](OPERACAO.md#6-fluxos-operacionais)).
- ✅ As rotinas de backup e recuperação estão documentadas ([OPERACAO §8](OPERACAO.md#8-backup)).
- ✅ Os procedimentos de atualização e rollback estão definidos ([OPERACAO §12](OPERACAO.md#12-atualizações-procedimento-oficial)).
- ✅ Todas as variáveis de ambiente estão descritas (§14 e [OPERACAO §4](OPERACAO.md#4-configurações)).
- ✅ As integrações externas estão documentadas.
- ✅ `python scripts/validate_docs.py` confirma a sincronização verificável com propriedades, rotas e runtime.

**Versão do sistema coberta:** backend `0.1.0-SNAPSHOT`, Spring Boot 3.5.3, Java 21.
**Base revisada:** `main` no commit `1f6ff281210e6aa71b1880da1119d22f8aabb68e`.
**Última revisão:** 23/07/2026.
