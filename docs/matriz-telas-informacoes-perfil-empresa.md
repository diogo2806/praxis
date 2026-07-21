# Matriz de telas e informações do perfil EMPRESA

Data da revisão: 21/07/2026  
Base analisada: `main` com as correções dos PRs #421, #422, #425, #426, #427, #428, #429, #430, #431, #432, #433, #434, #435, #436, #438, #441, #442, #443 e #446

## 1. Escopo

Este documento inventaria as telas acessíveis ao perfil `EMPRESA` e registra a responsabilidade de cada uma, o status real da centralização das informações e a evidência da correção.

A repetição é permitida quando representa apenas resumo, referência somente leitura ou cabeçalho contextual. Duas telas não devem criar ou alterar o mesmo dado.

## 2. Legendas

| Status | Significado |
|---|---|
| `CONCLUÍDA` | A responsabilidade foi centralizada e a correção está implementada. |
| `EM ANDAMENTO` | Parte do fluxo foi corrigida, mas ainda existe pendência. |
| `PENDENTE` | A correção ainda não foi implementada. |
| `BLOQUEADA` | A conclusão depende de integração, homologação ou decisão externa. |
| `NÃO SE APLICA` | A tela já funciona somente como resumo, referência ou detalhe. |

| Classificação | Significado |
|---|---|
| `OK` | Responsabilidade clara, sem duplicidade operacional relevante. |
| `RESUMO` | Exibe somente informações resumidas e atalhos. |
| `CONTEXTUAL` | Existe como etapa ou detalhe aberto a partir de outra tela. |
| `PARCIAL` | A responsabilidade é razoável, mas ainda existem ações a corrigir. |
| `DUPLICADO` | Mantém ou altera a mesma informação que outra tela. |
| `CONDICIONAL` | Deve aparecer apenas para empresas ou permissões específicas. |

## 3. Matriz atualizada

| Nº | Status | Classificação | Tela e rota | Responsabilidade atual | Evidência ou próximo passo |
|---:|---|---|---|---|---|
| 1 | `NÃO SE APLICA` | `RESUMO` | **Dashboard** `/dashboard` | Indicadores, gráficos, alertas e atalhos, sem tabela operacional. | PR #428 transformou o dashboard em visão analítica. |
| 2 | `CONCLUÍDA` | `OK` | **Avaliações** `/avaliacoes` | Cadastro, versões, status e ações administrativas. | PR #425 padronizou duplicação e arquivamento. |
| 3 | `CONCLUÍDA` | `OK` | **Nova avaliação** `/nova/avaliacao` | Plano inicial e seleção de competências ativas. | PR #426 removeu criação global de competências. |
| 4 | `CONCLUÍDA` | `CONTEXTUAL` | **Objetivo do modelo base** `/nova/objetivo` | Resumo somente leitura da versão. | PR #426 removeu edição e redistribuição de pesos. |
| 5 | `CONCLUÍDA` | `OK` | **Personagem** `/nova/personagem` | Única proprietária do nome, estado emocional, contexto e mensagem inicial. | PR #431 protegeu o nó inicial nos editores seguintes e adicionou manual específico. |
| 6 | `CONCLUÍDA` | `OK` | **Editor de diálogo** `/nova/dialogo` | Autoria das etapas posteriores, alternativas, tempo, mídia, criticidade e pontuação. | PR #429 retirou a autoria concorrente do Mapa; PR #431 tornou a mensagem inicial somente leitura com atalho para Personagem. |
| 7 | `CONCLUÍDA` | `OK` | **Mapa do fluxo** `/nova/mapa` | Posição visual das etapas e destino das alternativas. | PR #429 removeu criação e edição de conteúdo, persistiu coordenadas e manteve apenas conexões. |
| 8 | `CONCLUÍDA` | `OK` | **Validador/Revisão** `/nova/validador` | Diagnóstico de bloqueios, avisos e qualidade. | PR #421 removeu CRUD de etapas e alternativas. |
| 9 | `CONCLUÍDA` | `CONTEXTUAL` | **Piloto e indicadores** `/nova/piloto` | Calibração e indicadores analíticos da avaliação e versão abertas. | PR #434 removeu o seletor global e passou a exigir contexto do fluxo de autoria. |
| 10 | `CONCLUÍDA` | `CONTEXTUAL` | **Governança e publicação** `/nova/governanca` | Publicação, aceite, termos e auditoria. | PR #434 substituiu a confirmação própria por `AlertDialog` acessível. |
| 11 | `CONCLUÍDA` | `CONTEXTUAL` | **Ativação Gupy** `/nova/gupy` | Preflight técnico da avaliação e versão publicadas. | PR #438 removeu entregas operacionais e direcionou falhas, retentativas e DLQ para Monitoramento. |
| 12 | `NÃO SE APLICA` | `CONTEXTUAL` | **Começar rápido** `/nova/rapido` | Atalho para criar rascunho por modelo. | Já encaminha para a avaliação criada. |
| 13 | `CONCLUÍDA` | `OK` | **Jornadas** `/jornadas` | Composição, ordenação e situação das jornadas. | PR #438 adicionou `AlertDialog` para publicação, arquivamento e remoção de avaliações. |
| 14 | `CONCLUÍDA` | `OK` | **Participações** `/participacoes` | Lista e gestão unificadas de participações individuais e por jornada. | PR #421 criou read model, tabela e ações únicas; PR #430 restaurou o endpoint no controller principal. |
| 15 | `CONCLUÍDA` | `CONTEXTUAL` | **Nova participação individual** `/enviar-link` | Formulário de criação de convite individual. | A gestão posterior ocorre em Participações. |
| 16 | `CONCLUÍDA` | `CONTEXTUAL` | **Convite por jornada** `/participacoes/jornada` | Formulário de criação de convite por jornada. | PR #421 adicionou validade, reenvio, extensão, reativação e cancelamento. |
| 17 | `NÃO SE APLICA` | `OK` | **Resultados** `/results` | Lista de resultados concluídos. | Não mantém convite, validade ou progresso. |
| 18 | `NÃO SE APLICA` | `OK` | **Detalhe do resultado** `/results/$attemptId` | Evidências, competências, percurso e decisão humana. | Responsabilidade centralizada. |
| 19 | `CONCLUÍDA` | `CONTEXTUAL` | **Talent Match** `/talent-match` | Comparação analítica da avaliação e versão abertas. | PR #442 removeu a seleção global e passou a consultar somente participações concluídas do contexto informado. |
| 20 | `CONCLUÍDA` | `OK` | **Central operacional** `/monitoramento` | Fila de integrações com atenção, retentativas, DLQ e alertas não lidos. | PR #442 retirou integrações saudáveis, adicionou filtros e separou retentativa automática de reprocessamento manual. |
| 21 | `CONCLUÍDA` | `CONTEXTUAL` | **Compliance** `/compliance` | Resumo contextual e atalhos para telas proprietárias. | PR #421 removeu lista e diálogo concorrentes. |
| 22 | `CONCLUÍDA` | `OK` | **Perfil da empresa** `/configuracoes/perfil` | Consulta e edição dos dados cadastrais. | PR #422 implementou edição e auditoria; PR #427 corrigiu o import da API. |
| 23 | `CONCLUÍDA` | `OK` | **Competências** `/competencias` | Única proprietária do catálogo global. | PR #426 removeu criação e edição concorrentes. |
| 24 | `CONCLUÍDA` | `OK` | **Minha equipe** `/team` | Vínculos, perfis, permissões e situação de acesso dos usuários da empresa. | PR #446 criou subperfis efetivos, coluna de acesso, alteração auditada e restrições no menu e nas APIs. |
| 25 | `CONCLUÍDA` | `CONDICIONAL` | **Parceiros e especialistas** `/parceiros` | Operação de parceria somente quando contratada e autorizada. | PR #446 condicionou frontend e backend à flag `PRAXIS_PARTNER_ENABLED` e à permissão `PARTNER_MANAGER`. |
| 26 | `NÃO SE APLICA` | `OK` | **Integrações** `/integrations` | Configuração e estado saudável das integrações. | Responsabilidade centralizada. |
| 27 | `NÃO SE APLICA` | `OK` | **Detalhe da integração** `/integrations/$provider` | Diagnóstico, credenciais e configuração do provedor. | Responsabilidade centralizada. |
| 28 | `BLOQUEADA` | `CONTEXTUAL` | **Homologação Gupy** `/integrations/gupy-homologacao` | Evidências e checklist de homologação. | Depende de token, vaga real e aprovação externa da Gupy. |
| 29 | `NÃO SE APLICA` | `OK` | **Plano e cobrança** `/billing` | Créditos, assinatura, uso e histórico financeiro. | Responsabilidade centralizada. |
| 30 | `NÃO SE APLICA` | `OK` | **Minha conta** `/configuracoes/conta` | Credenciais pessoais do usuário autenticado. | Vínculo e permissões ficam em Minha equipe. |
| 31 | `CONCLUÍDA` | `CONTEXTUAL` | **Comece aqui** `/comecar` | Guia temporário do primeiro ciclo da empresa. | PR #446 corrigiu o convite para jornada, calculou o progresso real e removeu o item da navegação após a conclusão. |
| 32 | `CONCLUÍDA` | `OK` | **Central de manuais** `/manual` | Processo completo, busca, filtros e manual contextual por rota. | PR #443 tornou a central escalável; PR #446 adicionou os últimos manuais específicos pendentes. |

## 4. Propriedade das informações

| Status | Informação | Tela proprietária | Situação atual |
|---|---|---|---|
| `CONCLUÍDA` | Cadastro, versões e status da avaliação | **Avaliações** | Ações administrativas centralizadas. |
| `CONCLUÍDA` | Catálogo global de competências | **Competências** | Nova avaliação apenas seleciona; Objetivo apenas exibe. |
| `CONCLUÍDA` | Plano inicial e competências da versão | **Nova avaliação** | Objetivo funciona somente como resumo. |
| `CONCLUÍDA` | Contexto, personagem e mensagem inicial | **Personagem** | PR #431 bloqueou a alteração do nó inicial no Editor de diálogo e manteve referência com atalho. |
| `CONCLUÍDA` | Etapas posteriores, alternativas, mídia, criticidade e pontuação | **Editor de diálogo** | PRs #429 e #431 separaram o conteúdo inicial das demais etapas. |
| `CONCLUÍDA` | Posição das etapas e destino das alternativas | **Mapa do fluxo** | PR #429 limita a tela a coordenadas e conexões. |
| `CONCLUÍDA` | Bloqueios, avisos e qualidade | **Validador** | Telas secundárias mostram somente resumo ou link. |
| `CONCLUÍDA` | Publicação, aceite e auditoria | **Governança** | PR #434 padronizou a confirmação; Compliance mostra apenas referência contextual. |
| `CONCLUÍDA` | Convite, validade e progresso | **Participações** | Individuais e jornadas usam o mesmo read model. |
| `CONCLUÍDA` | Score, evidências e decisão humana | **Detalhe do resultado** | Listas exibem somente resumo. |
| `CONCLUÍDA` | Comparação de resultados por avaliação e versão | **Talent Match** | PR #442 exige contexto e consulta somente participantes concluídos desse recorte. |
| `CONCLUÍDA` | Falhas, retry e DLQ | **Monitoramento** | PR #442 centralizou somente exceções acionáveis e filtros da fila operacional. |
| `CONCLUÍDA` | Dados cadastrais da empresa | **Perfil da empresa** | Outras telas exibem somente identificação curta. |
| `CONCLUÍDA` | Usuário, vínculo, perfil e acesso | **Minha equipe** | PR #446 aplica perfis reais no backend, filtra a navegação e centraliza as alterações de acesso. |
| `CONCLUÍDA` | Plano, créditos e situação financeira | **Billing** | Outras telas mostram somente bloqueio e atalho. |

## 5. Plano de ação

### P0 — fontes de verdade e risco operacional

| Status | Ação | Evidência ou pendência |
|---|---|---|
| `CONCLUÍDA` | Unificar Participações no backend e frontend. | PRs #421 e #430. |
| `CONCLUÍDA` | Definir um único fluxo de autoria do cenário. | PR #429 separou Diálogo e Mapa; PR #431 centralizou contexto e mensagem inicial em Personagem. |
| `CONCLUÍDA` | Retirar Compliance como lista concorrente. | PR #421. |

### P1 — clareza operacional

| Status | Ação | Evidência ou pendência |
|---|---|---|
| `CONCLUÍDA` | Centralizar catálogo e seleção de competências. | PR #426. |
| `CONCLUÍDA` | Corrigir o destino de convite em Comece aqui. | PR #446 abre diretamente o convite por jornada. |
| `CONCLUÍDA` | Retirar integrações saudáveis de Monitoramento. | PR #442 manteve somente integrações com atenção, retentativas, DLQ e alertas. |
| `CONCLUÍDA` | Remover entregas operacionais de Ativação Gupy. | PR #438 manteve apenas o preflight e criou atalho para Monitoramento. |
| `CONCLUÍDA` | Tornar Perfil da empresa acionável. | PR #422. |
| `CONCLUÍDA` | Padronizar confirmações e modais. | PR #438 concluiu Jornadas após Avaliações e Governança. |

### P2 — permissões e acabamento

| Status | Ação | Evidência ou pendência |
|---|---|---|
| `CONCLUÍDA` | Criar subperfis de EMPRESA. | PR #446 criou Administrador, Autor, Analista e Operador com restrições efetivas no menu e nas APIs. |
| `CONCLUÍDA` | Incluir perfil e permissões em Equipe. | PR #446 adicionou seleção, alteração, permissões, situação e último acesso. |
| `CONCLUÍDA` | Restringir Parceiros. | PR #446 exige feature flag e permissão administrativa específica no frontend e backend. |
| `CONCLUÍDA` | Atualizar manuais de tela. | PRs #443 e #446 concluíram a central escalável e os manuais específicos restantes. |
| `CONCLUÍDA` | Ocultar Comece aqui após onboarding. | PR #446 mantém o processo na Central de manuais e retira o item da navegação após o primeiro ciclo. |

## 6. Critério para atualizar o status

Uma correção só recebe `CONCLUÍDA` quando existe uma única tela capaz de criar ou alterar a informação, as telas secundárias exibem apenas resumo ou referência com link para a proprietária, os estados e bloqueios usam a mesma regra de negócio, o manual descreve a responsabilidade correta e existe evidência em código, teste ou PR mesclado.
