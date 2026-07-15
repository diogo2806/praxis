# Requisitos técnicos pendentes — praxis

Status: atualizado em 2026-07-15 após implementação de `SEC10` e `INT10`.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## Como usar

- Colunas obrigatórias: `ID | Tarefa técnica | Critério de conclusão | Status`.
- Status permitidos: `⬜ Pendente`, `🟡 Em andamento` ou `⛔ Bloqueado`.
- Ao concluir uma linha, removê-la deste arquivo e registrar a entrega em `docs/implementados/requisitos-implementados.md` no mesmo PR.
- Cada pendência deve ser comprovada pela implementação atual e possuir efeito observável no comportamento do sistema.

## Contexto da auditoria

- Commit base da branch principal: `2f6f1ef37cad33fd0f889c733cd9ef9ba368bef6`.
- Finalidade identificada: plataforma de avaliações situacionais para recrutamento, com regras explícitas, score determinístico, trilha auditável e integração com ATS.
- Stack principal: Java 21, Spring Boot 3.5, Spring Security, JPA, PostgreSQL/Flyway, React 19, TanStack Start/Router e TypeScript.
- Arquitetura predominante: frontend React consumindo API Spring Boot, persistência PostgreSQL, autenticação JWT nas rotas internas, Bearer token nas integrações e entrega assíncrona por outbox.

## 1. Interface e fallbacks de compatibilidade

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| UI10 | Impedir que a ausência do endpoint de dashboard seja convertida em operação, plano e integrações falsamente ativos. | Um `404` ou backend incompatível produz estado explícito de indisponibilidade/compatibilidade ou dados calculados somente a partir de fontes comprovadas; o frontend nunca infere `CONECTADA` apenas porque existe token, nunca fabrica plano `ENTERPRISE/ATIVO` e não declara operação ativa quando os dados reais são desconhecidos. | ⬜ Pendente |

### UI10 — fallback que transforma ausência em sucesso

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `frontend/src/lib/api/praxis.ts` | `getDashboard()` | Ao receber `404` de `/api/v1/dashboard`, chama `getDashboardFallback()` em vez de informar incompatibilidade ou ausência de dados. | Limitar o fallback a dados verificáveis e representar explicitamente informações desconhecidas; não converter `404` funcional em dashboard aparentemente normal. |
| `frontend/src/lib/api/praxis.ts` | `getDashboardFallback()` | Ignora falhas de várias consultas auxiliares, usa valores vazios e fabrica billing como `ENTERPRISE`, `ATIVO` e “Sob contrato”. | Remover valores comerciais sintéticos e propagar estado parcial/indisponível para a interface. |
| `frontend/src/lib/api/praxis.ts` | `integrationStatusesFromTokens()` | Converte a mera existência de token Gupy/Recrutei em status `CONECTADA`, contradizendo a regra de conexão comprovada por atividade externa. | Exibir `PENDENTE`/desconhecido quando só houver token ou consultar a fonte operacional real de `empresa_integrations`. |
| `frontend/src/lib/api/praxis.ts` | `dashboardFallbackActions()` | Sempre adiciona a ação “Sua operação está ativa”, mesmo quando o fallback não conseguiu recuperar as fontes reais. | Produzir recomendações apenas com evidência suficiente e mostrar falha/estado parcial quando os dados não forem confiáveis. |
| `backend/src/main/java/br/com/iforce/praxis/dashboard/controller/DashboardController.java` | `GET /api/v1/dashboard` | O endpoint existe na branch atual; o fallback é acionado em versões incompatíveis ou respostas `404` e mascara essa divergência. | Tratar incompatibilidade de versão como tal, sem apresentar dados artificiais como estado real do cliente. |

## Ordem recomendada

1. `UI10` — remover o fallback que fabrica estados operacionais e comerciais.
