# Requisitos técnicos pendentes — praxis

Status: atualizado em 2026-07-15 após auditoria da branch `main`.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## Contexto da auditoria

- Commit auditado da branch principal: `7883116a029b0ae839e7f68b6d6ae5c3221fd236`.
- Finalidade identificada: plataforma de avaliações situacionais versionadas para recrutamento, com pontuação determinística, revisão humana, trilha auditável e integrações com ATS.
- Stack principal: Java 21, Spring Boot 3.5, Spring Security, JPA, PostgreSQL/Flyway, React 19, TanStack Start/Router e TypeScript.
- Arquitetura predominante: frontend React consumindo API Spring Boot, persistência PostgreSQL, autenticação JWT nas rotas internas, Bearer token nas integrações e entrega assíncrona por outbox transacional.
- Fluxos revisados: integração Gupy, criação e reaproveitamento de tentativas, links diretos, cálculo de pontuação, Talent Match, processamento do outbox, monitoramento operacional, relatórios de engajamento e documentos canônicos.

## 1. Integração Gupy

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| INT17 | Restringir o `result_webhook_url` da Gupy ao payload oficial de resultado. | Somente eventos `RESULT_READY` serializados como `TestResult` são enviados ao `result_webhook_url`; eventos internos de início e abandono não usam esse destino nem expõem dados pessoais ao ATS. | ⬜ Pendente |
| INT18 | Confirmar o `callback_url` da Gupy por chamada servidor-servidor persistida. | O backend executa GET no callback ao concluir a tentativa, registra código HTTP, data e estado da confirmação, aplica retentativa idempotente e não considera `callback_presented` como confirmação. | ⬜ Pendente |

### INT17 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `src/main/java/**/OutboxProcessor.java` | despacho de `ATTEMPT_STARTED`, `ATTEMPT_ABANDONED` e `RESULT_READY` | Eventos proprietários de início e abandono são enviados por HTTP ao mesmo `result_webhook_url` reservado ao resultado oficial da Gupy. | Separar os destinos e permitir no `result_webhook_url` apenas o payload oficial `TestResult` produzido por `RESULT_READY`. |
| `src/main/java/**/CandidateAttemptService.java` e produtores do outbox | payloads de início e abandono | Os payloads internos podem incluir nome e e-mail do candidato e seguem para um endpoint externo cujo contrato não prevê esses eventos. | Manter esses eventos apenas em canais internos ou em um webhook próprio explicitamente configurado, sem reutilizar o endpoint de resultados da Gupy. |
| `docs/implementados/requisitos-implementados.md` | registro anterior de `INT11` | A documentação registra como corrigido um comportamento que permanece ativo na implementação auditada. | Reabrir a lacuna funcional no backlog e só registrar nova conclusão quando o fluxo real estiver separado. |

### INT18 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `src/main/java/**/Gupy*Callback*.java` e fluxo de conclusão | `callback_url`, `handoff=callback_presented` | O sistema registra apenas que a URL foi disponibilizada ao navegador; isso não comprova que o GET foi executado nem recebido pela Gupy. | Implementar chamada GET servidor-servidor após conclusão, com persistência do resultado e retentativa segura. |
| `src/main/java/**/Audit*.java` ou evento de auditoria do callback | mensagem “Callback de conclusão disponibilizado ao navegador” | A auditoria pode transmitir confirmação operacional inexistente. | Distinguir `APRESENTADO_AO_NAVEGADOR`, `CONFIRMADO_PELO_BACKEND` e `FALHA_DE_CONFIRMACAO`, registrando status HTTP e instante. |

## 2. Tentativas e idempotência

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| DATA13 | Diferenciar repetição idempotente de nova tentativa autorizada pela Gupy. | Reenvios idênticos reutilizam a mesma tentativa, enquanto `previous_result=fail` ou outro comando explícito de reteste cria nova aplicação sem retornar `409` indevido. | ⬜ Pendente |
| DATA14 | Modelar aplicações independentes para links diretos. | A empresa consegue criar nova aplicação da mesma avaliação para o mesmo candidato por vaga, ciclo ou comando explícito, preservando a idempotência de cada aplicação. | ⬜ Pendente |

### DATA13 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `src/main/java/**/CandidateAttemptService.java` | `createOrReuse()` | A chave idempotente usa empresa, candidato, teste e vaga, mas não representa uma nova aplicação; `previous_result` entra no fingerprint e não na decisão de reteste. | Separar `idempotencyKey` de `applicationId` ou `attemptCycle`; tratar `previous_result=fail` conforme política explícita de reteste. |
| `src/main/java/**/Gupy*Request*.java` | `previous_result` | O campo é aceito pelo DTO, mas uma alteração de valor pode gerar a mesma chave com fingerprint diferente e resultar em `409 Conflict`. | Fazer o contrato alcançar a regra de negócio, criando nova tentativa quando a solicitação representar reteste legítimo. |

### DATA14 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `src/main/java/**/CandidateAttemptService.java` ou serviço de links diretos | chave `empresa + e-mail + avaliação` | Qualquer tentativa existente pode ser reaproveitada, inclusive abandonada, expirada ou pertencente a outra aplicação legítima. | Introduzir identificador de aplicação, vaga, ciclo ou comando explícito de nova tentativa e limitar o reaproveitamento à mesma aplicação. |
| `src/main/java/**/CandidateLink*.java` | criação e listagem de links | O domínio não expressa de forma inequívoca quando um novo link deve criar nova tentativa. | Novo campo ou entidade sugerida: `applicationId`/`assessmentCycleId`, persistida e usada na chave idempotente. |

## 3. Pontuação e comparabilidade

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| BUS12 | Garantir comparabilidade válida entre candidatos de caminhos distintos. | O score usa uma base comum de competências e máximos alcançáveis, ou o Talent Match identifica e bloqueia comparações entre resultados calculados sobre bases incompatíveis. | ⬜ Pendente |

### BUS12 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `src/main/java/**/Scoring*.java` | cálculo do máximo por etapa e redistribuição de pesos | O denominador combina máximos de alternativas que podem levar a ramos diferentes; competências ausentes no caminho são removidas e os pesos restantes são redistribuídos. | Calcular contra uma matriz comum e alcançável por versão da avaliação, ou produzir metadados explícitos de comparabilidade. |
| `src/main/java/**/TalentMatch*.java` e frontend correspondente | radar, benchmark e comparação | Candidatos podem ser colocados lado a lado apesar de terem sido medidos em competências e pesos efetivos diferentes. | Validar a compatibilidade das bases antes de comparar; quando incompatíveis, impedir benchmark único e informar o motivo. |
| `src/main/java/**/AssessmentValidator*.java` | aviso de competência ausente em caminhos | A validação permite publicação apenas com aviso, embora isso possa invalidar comparações posteriores. | Transformar em erro quando a avaliação declarar comparabilidade global, ou exigir uma política explícita de comparação por caminho. |

## 4. Processamento assíncrono

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| ASYNC10 | Fazer tipos desconhecidos do outbox falharem explicitamente. | Qualquer tipo não suportado gera erro, permanece pendente para retentativa e segue para DLQ conforme a política existente; nunca é marcado como `SENT`. | ⬜ Pendente |

### ASYNC10 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `src/main/java/**/OutboxProcessor.java` | despacho por tipo de evento | Somente três tipos são tratados; um tipo desconhecido encerra o método sem erro e depois é marcado como entregue. | Adicionar ramo padrão que lance exceção específica, por exemplo `novo método sugerido: rejectUnsupportedEventType()`. |
| `src/main/java/**/OutboxProcessor.java` | transição para `SENT` | A transição ocorre mesmo quando nenhum handler processou o evento. | Marcar como `SENT` apenas após um handler reconhecido concluir com sucesso. |

## 5. Monitoramento operacional

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| UI13 | Paginar o centro operacional e incluir todos os estados relevantes. | Convites não abertos, tentativas em andamento, concluídas, abandonadas e expiradas podem ser consultados com paginação e filtros, sem corte silencioso em 200 registros. | ⬜ Pendente |

### UI13 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `src/main/java/**/CandidateLink*.java` ou repositório de tentativas | consulta limitada a 200 registros | A listagem geral possui limite fixo e não expõe paginação. | Receber parâmetros de página, tamanho, estado, período e ordenação; retornar metadados de paginação. |
| `src/main/java/**/Operations*.java` e frontend do centro operacional | filtros de status | A tela carrega somente `IN_PROGRESS` e `COMPLETED`, ocultando convites não abertos, abandonos e expirações. | Expor todos os estados operacionais relevantes e permitir filtros combináveis. |

## 6. Relatórios de valor

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| BUS13 | Tratar horas economizadas como estimativa configurável e metodologicamente explícita. | A interface, API e relatórios usam o termo “estimativa de horas potencialmente economizadas”, exibem a metodologia e permitem configurar a média por cliente. | ⬜ Pendente |

### BUS13 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `src/main/java/**/EngagementReportService.java` | cálculo `avaliacoesConcluidas * 1.5` | Uma constante genérica é apresentada como economia efetivamente entregue, sem medição do processo anterior. | Renomear o indicador, expor a fórmula e ler a média configurada pela empresa, mantendo um default claramente identificado como estimativa. |
| frontend e templates de relatório | rótulo “horas economizadas” | O texto não diferencia hipótese de medição real. | Exibir metodologia, valor configurado e aviso de estimativa em todas as superfícies. |

## 7. Documentação concorrente

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| LEGACY12 | Remover `docs/backlog.txt` como fonte concorrente e obsoleta. | Conteúdo ainda válido é convertido em requisitos objetivos no backlog canônico; recomendações especulativas e informações superadas são removidas ou arquivadas como material não normativo. | ⬜ Pendente |

### LEGACY12 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `docs/backlog.txt` | conteúdo integral | O arquivo mistura conversa, propostas hipotéticas, tecnologias não adotadas, código ilustrativo e afirmações superadas, incluindo sugestão de outbox já existente. | Remover o arquivo ou substituí-lo por aviso de arquivo histórico não normativo; migrar somente pendências comprovadas para este documento. |
| `docs/requisitos/requisito-tecnico.md` | fonte canônica de pendências | O documento declarava ausência de pendências apesar das inconsistências funcionais comprovadas na implementação. | Manter somente requisitos verificáveis, específicos e implementáveis, conforme esta auditoria. |

## Ordem recomendada

1. `INT17` — interromper o envio de eventos proprietários ao webhook oficial da Gupy.
2. `INT18` — confirmar o callback por chamada servidor-servidor persistida.
3. `ASYNC10` — impedir perda silenciosa de eventos desconhecidos.
4. `DATA13` e `DATA14` — separar repetição idempotente de nova aplicação legítima.
5. `BUS12` — corrigir a base de comparação do Talent Match ou bloquear comparações incompatíveis.
6. `UI13` — paginar o monitoramento e incluir todos os estados operacionais.
7. `BUS13` — corrigir a semântica e a configuração da estimativa de horas.
8. `LEGACY12` — eliminar a documentação concorrente e obsoleta.
