# Requisitos técnicos pendentes — praxis

Status: atualizado em 2026-07-15 após implementação de `SEC10`.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## Como usar

- Colunas obrigatórias: `ID | Tarefa técnica | Critério de conclusão | Status`.
- Status permitidos: `⬜ Pendente`, `🟡 Em andamento` ou `⛔ Bloqueado`.
- Ao concluir uma linha, removê-la deste arquivo e registrar a entrega em `docs/implementados/requisitos-implementados.md` no mesmo PR.
- Cada pendência deve ser comprovada pela implementação atual e possuir efeito observável no comportamento do sistema.

## Contexto da auditoria

- Commit base da branch principal: `f110bd93bd1aef97f954df504864f3bd79af84bb`.
- Finalidade identificada: plataforma de avaliações situacionais para recrutamento, com regras explícitas, score determinístico, trilha auditável e integração com ATS.
- Stack principal: Java 21, Spring Boot 3.5, Spring Security, JPA, PostgreSQL/Flyway, React 19, TanStack Start/Router e TypeScript.
- Arquitetura predominante: frontend React consumindo API Spring Boot, persistência PostgreSQL, autenticação JWT nas rotas internas, Bearer token nas integrações e entrega assíncrona por outbox.

## 1. Integrações ATS — estado de conexão real

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| INT10 | Registrar atividade autenticada em todos os fluxos externos Gupy e Recrutei e auditar a primeira conexão real. | Cada requisição externa concluída com Bearer token válido registra provedor, endpoint/evidência e horário; `PENDENTE` ou `ERRO` passa para `CONECTADA` somente após essa evidência; chamadas posteriores atualizam `lastSyncAt`; `DESATIVADA` nunca é reativada implicitamente; falhas de registro não são descartadas silenciosamente; a primeira conexão e a recuperação de erro ficam auditadas com estado anterior e novo. | ⬜ Pendente |

### INT10 — produtores de evidência, persistência e auditoria

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/gupy/controller/GupyIntegrationController.java` | `listPublishedTests()`, `createCandidateAttempt()` e `getTestResult()` | Os três endpoints validam o Bearer token. Somente `POST /test/candidate` alcança uma tentativa de registro de atividade; `GET /test` e `GET /test/result/{resultId}` não atualizam a integração. | Registrar a atividade após a conclusão bem-sucedida de cada fluxo, usando o contexto autenticado e identificando o endpoint que forneceu a evidência. |
| `backend/src/main/java/br/com/iforce/praxis/recrutei/controller/RecruteiIntegrationController.java` | `listPublishedTests()`, `createCandidateAttempt()` e `getTestResult()` | Repete a lacuna da Gupy: somente a criação de candidato passa pelo serviço que tenta registrar atividade. | Aplicar o mesmo contrato de atividade real a todos os endpoints Recrutei concluídos com sucesso. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptService.java` | `recordIncomingActivity()` | Converte o provedor e chama `recordActivity()`, mas captura qualquer `RuntimeException` e ignora a falha. O caso de uso principal retorna sucesso mesmo quando o estado operacional não foi registrado. | Não descartar a falha silenciosamente. Persistir a evidência de forma coerente com a transação ou registrar uma ação durável de retry/recuperação que deixe a falha observável. |
| `backend/src/main/java/br/com/iforce/praxis/shared/integration/IntegrationManagementService.java` | `recordActivity()` | Define qualquer integração existente como `CONECTADA`, atualiza `lastSyncAt` e limpa erro, inclusive sem verificar `DESATIVADA`; não registra evento de auditoria nem a origem da evidência. | Restringir transições permitidas, preservar `DESATIVADA`, atualizar atividade de conexões já válidas e auditar a primeira conexão/recuperação com provedor, endpoint, horário, estado anterior e novo. |
| `backend/src/main/java/br/com/iforce/praxis/audit/service/AuditEventService.java` | eventos de integração | A infraestrutura de auditoria existe e já é usada em configuração, reativação, desconexão e token, mas não participa da promoção por atividade externa. | Registrar a transição real usando a infraestrutura existente, sem criar uma trilha paralela. |

## 2. Interface e fallbacks de compatibilidade

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

1. `INT10` — tornar conexão e atividade ATS reais, persistidas e auditáveis.
2. `UI10` — remover o fallback que fabrica estados operacionais e comerciais.
