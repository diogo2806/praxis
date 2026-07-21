# Matriz de telas e informações do perfil EMPRESA

Data da revisão: 21/07/2026  
Base analisada: `main` com as correções dos PRs #421, #422, #425, #426, #427, #428, #429, #430 e #431

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
| 9 | `PENDENTE` | `CONTEXTUAL` | **Piloto e indicadores** `/nova/piloto` | Calibração e indicadores analíticos da versão. | Exigir contexto e retirar seletor global. |
| 10 | `PENDENTE` | `CONTEXTUAL` | **Governança e publicação** `/nova/governanca` | Publicação, aceite, termos e auditoria. | Trocar confirmação própria por `AlertDialog`. |
| 11 | `PENDENTE` | `DUPLICADO` | **Ativação Gupy** `/nova/gupy` | Deve manter somente o preflight da versão. | Remover entregas e direcionar para Monitoramento. |
| 12 | `NÃO SE APLICA` | `CONTEXTUAL` | **Começar rápido** `/nova/rapido` | Atalho para criar rascunho por modelo. | Já encaminha para a avaliação criada. |
| 13 | `EM ANDAMENTO` | `PARCIAL` | **Jornadas** `/jornadas` | Composição e ordenação de avaliações. | PR #417 retirou participantes; faltam confirmações padronizadas. |
| 14 | `CONCLUÍDA` | `OK` | **Participações** `/participacoes` | Lista e gestão unificadas de participações individuais e por jornada. | PR #421 criou read model, tabela e ações únicas; PR #430 restaurou o endpoint no controller principal. |
| 15 | `CONCLUÍDA` | `CONTEXTUAL` | **Nova participação individual** `/enviar-link` | Formulário de criação de convite individual. | A gestão posterior ocorre em Participações. |
| 16 | `CONCLUÍDA` | `CONTEXTUAL` | **Convite por jornada** `/participacoes/jornada` | Formulário de criação de convite por jornada. | PR #421 adicionou validade, reenvio, extensão, reativação e cancelamento. |
| 17 | `NÃO SE APLICA` | `OK` | **Resultados** `/results` | Lista de resultados concluídos. | Não mantém convite, validade ou progresso. |
| 18 | `NÃO SE APLICA` | `OK` | **Detalhe do resultado** `/results/$attemptId` | Evidências, competências, percurso e decisão humana. | Responsabilidade centralizada. |
| 19 | `PENDENTE` | `CONTEXTUAL` | **Talent Match** `/talent-match` | Comparação analítica de resultados. | Exigir contexto e retirar segunda lista global de candidatos. |
| 20 | `PENDENTE` | `PARCIAL` | **Central operacional** `/monitoramento` | Deve apresentar somente falhas, exceções, retry e DLQ. | Retirar integrações saudáveis e criar filtros. |
| 21 | `CONCLUÍDA` | `CONTEXTUAL` | **Compliance** `/compliance` | Resumo contextual e atalhos para telas proprietárias. | PR #421 removeu lista e diálogo concorrentes. |
| 22 | `CONCLUÍDA` | `OK` | **Perfil da empresa** `/configuracoes/perfil` | Consulta e edição dos dados cadastrais. | PR #422 implementou edição e auditoria; PR #427 corrigiu o import da API. |
| 23 | `CONCLUÍDA` | `OK` | **Competências** `/competencias` | Única proprietária do catálogo global. | PR #426 removeu criação e edição concorrentes. |
| 24 | `EM ANDAMENTO` | `PARCIAL` | **Minha equipe** `/team` | Vínculos e acesso dos usuários da empresa. | Faltam perfil, permissões e coluna de acesso. |
| 25 | `PENDENTE` | `CONDICIONAL` | **Parceiros e especialistas** `/parceiros` | Operação de parceria quando habilitada. | Restringir por feature flag e permissão específica. |
| 26 | `NÃO SE APLICA` | `OK` | **Integrações** `/integrations` | Configuração e estado saudável das integrações. | Responsabilidade centralizada. |
| 27 | `NÃO SE APLICA` | `OK` | **Detalhe da integração** `/integrations/$provider` | Diagnóstico, credenciais e configuração do provedor. | Responsabilidade centralizada. |
| 28 | `BLOQUEADA` | `CONTEXTUAL` | **Homologação Gupy** `/integrations/gupy-homologacao` | Evidências e checklist de homologação. | Depende de token, vaga real e aprovação externa da Gupy. |
| 29 | `NÃO SE APLICA` | `OK` | **Plano e cobrança** `/billing` | Créditos, assinatura, uso e histórico financeiro. | Responsabilidade centralizada. |
| 30 | `NÃO SE APLICA` | `OK` | **Minha conta** `/configuracoes/conta` | Credenciais pessoais do usuário autenticado. | Vínculo e permissões ficam em Minha equipe. |
| 31 | `PENDENTE` | `PARCIAL` | **Comece aqui** `/comecar` | Onboarding inicial da empresa. | Corrigir destino do convite e ocultar após conclusão. |
| 32 | `EM ANDAMENTO` | `PARCIAL` | **Central de manuais** `/manual` | Processo completo e manual contextual por rota. | PR #431 adicionou Personagem e atualizou Diálogo e Mapa; ainda faltam telas pendentes. |

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
| `CONCLUÍDA` | Publicação, aceite e auditoria | **Governança** | Compliance mostra apenas referência contextual. |
| `CONCLUÍDA` | Convite, validade e progresso | **Participações** | Individuais e jornadas usam o mesmo read model. |
| `CONCLUÍDA` | Score, evidências e decisão humana | **Detalhe do resultado** | Listas exibem somente resumo. |
| `PENDENTE` | Falhas, retry e DLQ | **Monitoramento** | Gupy ainda repete informações operacionais. |
| `CONCLUÍDA` | Dados cadastrais da empresa | **Perfil da empresa** | Outras telas exibem somente identificação curta. |
| `EM ANDAMENTO` | Usuário, vínculo, perfil e acesso | **Minha equipe** | Minha conta mantém apenas credenciais próprias. |
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
| `PENDENTE` | Corrigir o destino de convite em Comece aqui. | Abrir Participações ou convite por jornada. |
| `PENDENTE` | Retirar integrações saudáveis de Monitoramento. | Manter somente exceções e retentativas. |
| `PENDENTE` | Remover entregas operacionais de Ativação Gupy. | Entregas ficam somente em Monitoramento. |
| `CONCLUÍDA` | Tornar Perfil da empresa acionável. | PR #422. |
| `EM ANDAMENTO` | Padronizar confirmações e modais. | Avaliações concluída; faltam Jornadas e Governança. |

### P2 — permissões e acabamento

| Status | Ação | Evidência ou pendência |
|---|---|---|
| `PENDENTE` | Criar subperfis de EMPRESA. | Backend e frontend devem distinguir os perfis autorizados. |
| `PENDENTE` | Incluir perfil e permissões em Equipe. | Modal e tabela devem mostrar e alterar o acesso. |
| `PENDENTE` | Restringir Parceiros. | Rota somente para empresa parceira e permissão específica. |
| `EM ANDAMENTO` | Atualizar manuais de tela. | PR #431 adicionou Personagem e atualizou Diálogo e Mapa; outras rotas continuam pendentes. |
| `PENDENTE` | Ocultar Comece aqui após onboarding. | Guia deve permanecer apenas em Ajuda. |

## 6. Critério para atualizar o status

Uma correção só recebe `CONCLUÍDA` quando existe uma única tela capaz de criar ou alterar a informação, as telas secundárias exibem apenas resumo ou referência com link para a proprietária, os estados e bloqueios usam a mesma regra de negócio, o manual descreve a responsabilidade correta e existe evidência em código, teste ou PR mesclado.
