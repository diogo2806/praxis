# Matriz de telas e informações do perfil EMPRESA

Data da revisão: 21/07/2026  
Base analisada: `main` com as correções dos PRs #421 e #422

## 1. Escopo

Este documento inventaria as telas acessíveis ao perfil `EMPRESA`, incluindo rotas principais, telas contextuais e rotas condicionais. Para cada tela, registra:

- tabelas, listas, cards e painéis exibidos;
- modais, diálogos e confirmações;
- campos e informações repetidos;
- ação necessária para manter uma única fonte de verdade;
- classificação do problema encontrado;
- **status real da correção**;
- evidência disponível ou próximo passo.

Neste documento, **tabela** significa tabela ou lista visual da interface, não tabela física do banco de dados.

## 2. Legendas

### 2.1 Status da correção

| Status da correção | Significado |
|---|---|
| `CONCLUÍDA` | A correção foi implementada e a responsabilidade da informação está centralizada. |
| `EM ANDAMENTO` | Parte da correção foi implementada, mas ainda existem pendências. |
| `PENDENTE` | A correção ainda não foi implementada. |
| `BLOQUEADA` | A conclusão depende de integração, homologação externa, regra de negócio ou decisão ainda não disponível. |
| `NÃO SE APLICA` | A tela já funciona apenas como resumo, referência ou detalhe e não exige correção de centralização. |

### 2.2 Classificação do problema

| Classificação | Significado |
|---|---|
| `OK` | Responsabilidade clara e sem duplicidade operacional relevante. |
| `RESUMO` | Repete somente um resumo intencional, sem permitir manutenção do dado. |
| `CONTEXTUAL` | Existe somente como etapa ou detalhe aberto a partir da tela proprietária. |
| `PARCIAL` | Responsabilidade razoável, mas ainda há campos, ações ou navegação a corrigir. |
| `DUPLICADO` | Mantém ou altera a mesma informação que outra tela. |
| `SEM AÇÃO` | Exibe dados, mas não oferece ação útil para o usuário. |
| `CONDICIONAL` | Só deveria aparecer para empresas ou permissões específicas. |
| `CRÍTICO` | Pode ocultar registros, criar duas fontes de verdade ou induzir o usuário a erro. |

## 3. Matriz tela a tela

| Status da correção | Classificação | Tela e rota | Tabelas, listas, cards e painéis | Modais e diálogos | Campos ou informações repetidos | Ação para manter a informação em um único lugar | Evidência ou próximo passo |
|---|---|---|---|---|---|---|---|
| `NÃO SE APLICA` | `RESUMO` | **Dashboard** `/dashboard` | Cards de avaliações, jornadas, participações, resultados, próxima ação e resultados recentes. | Nenhum. | Totais operacionais, participante, processo, situação, resultado e data. | Manter somente indicadores e atalhos. | A tela não mantém dados operacionais; somente resume. |
| `PENDENTE` | `PARCIAL` | **Avaliações** `/avaliacoes` | Tabela mestre de avaliações, versões, situação, uso e conclusões. | Usa `window.prompt` e `window.confirm`. | Nome, descrição, competências, versão, status, aplicações e conclusões. | Tornar Avaliações proprietária do cadastro, versão e status. | Criar `Dialog` para duplicação e `AlertDialog` para arquivamento. |
| `PENDENTE` | `DUPLICADO` | **Nova avaliação** `/nova/avaliacao` | Formulário, seletor e busca de competências. | Nenhum. | Cargo, situação crítica e competências. | Manter aqui o plano inicial, mas remover criação de competência global. | Permitir somente selecionar competências do catálogo. |
| `PENDENTE` | `DUPLICADO` | **Objetivo do modelo base** `/nova/objetivo` | Card da versão, competências e resumo. | Nenhum. | Nome, descrição, status, primeira etapa, versão e competências. | Transformar em resumo somente leitura ou remover a rota. | Retirar a edição de competências. |
| `PENDENTE` | `CONTEXTUAL` | **Personagem** `/nova/personagem` | Formulário e banners de salvamento. | Nenhum. | Mensagem inicial ainda pode ser alterada em Diálogo e Mapa. | Personagem deve ser proprietária do contexto e mensagem inicial. | Bloquear edição concorrente nos demais editores. |
| `PENDENTE` | `DUPLICADO` | **Editor de diálogo** `/nova/dialogo` | Etapas, alternativas, mensagem, tempo, mídia, destino, criticidade e pontuação. | Nenhum modal padronizado identificado. | Conteúdo e parte das conexões também são alterados no Mapa. | Manter Diálogo como editor textual principal. | Reduzir a edição de conteúdo concorrente no Mapa. |
| `PENDENTE` | `DUPLICADO` | **Construtor visual** `/nova/mapa` | Canvas, nós, conexões, propriedades e validação resumida. | Nenhum. | Conteúdo e pontuação ainda se sobrepõem ao Diálogo. | Manter posição e conexão visual. | Separar propriedades gráficas da edição de conteúdo. |
| `CONCLUÍDA` | `OK` | **Validador/Revisão** `/nova/validador` | Cards de prontidão, bloqueios, avisos, qualidade e lista de diagnósticos. | Nenhum CRUD ou confirmação de autoria. | Exibe somente diagnóstico calculado pelo backend. | Manter apenas diagnóstico e links para Diálogo, Mapa, Governança e Compliance contextual. | PR #421 removeu criação, edição e exclusão de nós e alternativas e adicionou manual específico. |
| `PENDENTE` | `CONTEXTUAL` | **Piloto e indicadores** `/nova/piloto` | Tentativas, conclusão, abandono, expiração e calibração. | Nenhum. | Métricas também aparecem em Avaliações, Dashboard e Participações. | Manter somente calibração e indicadores analíticos. | Abrir pelo contexto da avaliação e retirar seletor global. |
| `PENDENTE` | `CONTEXTUAL` | **Governança e publicação** `/nova/governanca` | Estados, termos, aceite e auditoria. | Confirmação própria em sobreposição. | Status aparece em Avaliações. | Concentrar publicação, termos e auditoria em Governança. | Trocar sobreposição por `AlertDialog`. |
| `PENDENTE` | `DUPLICADO` | **Ativação Gupy** `/nova/gupy` | Preflight e lista de entregas. | Nenhum. | Configuração, entregas, retry e DLQ. | Manter somente preflight da versão. | Remover entregas e criar link filtrado para Monitoramento. |
| `NÃO SE APLICA` | `CONTEXTUAL` | **Começar rápido** `/nova/rapido` | Cards de modelos prontos. | Nenhum. | Cria o mesmo tipo de rascunho de Nova avaliação. | Manter apenas como atalho. | Já encaminha para a revisão da avaliação criada. |
| `EM ANDAMENTO` | `PARCIAL` | **Jornadas** `/jornadas` | Lista de jornadas, formulário e tabela de etapas. | Edição inline e confirmações não padronizadas. | Avaliações, versões e status. | Manter somente composição e ordenação. | PR #417 retirou participantes e convites; faltam confirmações. |
| `CONCLUÍDA` | `OK` | **Participações** `/participacoes` | Uma tabela de participações individuais e por jornada com tipo, processo, situação, progresso, validade e ações. | `AlertDialog` para cancelamento de jornada. | Identidade e resultado aparecem como referências em outras telas. | Concentrar convite, validade, reenvio, reativação, cancelamento e andamento. | PR #421 criou endpoint/read model unificado e excluiu tentativas internas da jornada da lista mestre. |
| `CONCLUÍDA` | `CONTEXTUAL` | **Nova participação individual** `/enviar-link` | Criação, seleção de avaliação e compartilhamento. | Nenhum. | Nome, e-mail e link aparecem depois em Participações. | Manter somente como formulário iniciado em Participações. | Não mantém outra tabela mestre. |
| `CONCLUÍDA` | `CONTEXTUAL` | **Convite por jornada** `/participacoes/jornada` | Formulário e card do link gerado. | Nenhum. | Nome, e-mail, jornada, sequência e link aparecem na Central de Participações. | Manter somente como formulário de criação; toda gestão ocorre em Participações. | PR #421 implementou validade, reenvio, extensão, reativação, cancelamento e exibição imediata no read model único. |
| `NÃO SE APLICA` | `OK` | **Resultados** `/results` | Filtros e tabela de resultados concluídos. | Nenhum. | Participante, avaliação e score. | Manter somente resultados concluídos. | Não mantém convite, validade ou progresso. |
| `NÃO SE APLICA` | `OK` | **Detalhe do resultado** `/results/$attemptId` | Cabeçalho, competências, respostas e decisão humana. | Nenhum. | Identidade, status e competências. | Concentrar evidências e decisão humana. | Responsabilidade já centralizada. |
| `PENDENTE` | `CONTEXTUAL` | **Talent Match** `/talent-match` | Seleção de avaliação, candidatos, radar e benchmark. | Nenhum. | Resultados, candidatos, avaliação e competências. | Abrir com contexto obrigatório. | Retirar seletor global e impedir segunda lista de candidatos. |
| `PENDENTE` | `PARCIAL` | **Central operacional** `/monitoramento` | Métricas, integrações com atenção, alertas e DLQ. | Nenhum. | Status saudável e entregas aparecem em outras telas. | Manter somente erros, exceções, retry e DLQ. | Retirar integrações saudáveis e criar filtros. |
| `CONCLUÍDA` | `CONTEXTUAL` | **Compliance** `/compliance` | Cards de contexto, prontidão e atalhos para telas proprietárias. | Nenhum diálogo ou tabela global. | Exibe somente resumo de validação da versão selecionada. | Abrir somente com avaliação e versão; direcionar cada responsabilidade. | PR #421 removeu a lista global e o item de menu; Validador, Governança, Privacidade e Avaliações permanecem proprietários dos dados. |
| `CONCLUÍDA` | `OK` | **Perfil da empresa** `/configuracoes/perfil` | Cards de consulta e formulário de edição de nome, razão social, CNPJ, e-mail, telefone e site. | Nenhum modal; ações Salvar e Cancelar ficam no próprio formulário. | Dados cadastrais podem aparecer como referência curta no Dashboard, Billing e integrações. | Manter criação e alteração dos dados cadastrais nesta tela; demais telas mostram somente referência. | PR #422 adicionou atualização protegida por perfil EMPRESA, validação, auditoria, testes e manual específico. |
| `EM ANDAMENTO` | `OK` | **Competências** `/competencias` | Total, busca e tabela paginada. | Criar/editar e confirmação de remoção. | Competências usadas em criação, resultados e comparação. | Manter como única proprietária do catálogo. | Falta impedir criação global em Nova avaliação e atualizar outros manuais. |
| `EM ANDAMENTO` | `PARCIAL` | **Minha equipe** `/team` | Tabela de usuários e ações. | Convite e confirmação de bloqueio. | Nome e e-mail em Minha conta, menu e Parceiros. | Concentrar vínculos e permissões internas. | Faltam perfil, permissões e coluna de acesso. |
| `PENDENTE` | `CONDICIONAL` | **Parceiros e especialistas** `/parceiros` | Especialistas, clientes, catálogo e formulários. | Confirmações não totalmente padronizadas. | Usuários, provedores e tokens. | Restringir por feature flag e permissão. | Separar token de parceiro do token técnico de integração. |
| `NÃO SE APLICA` | `OK` | **Integrações** `/integrations` | Cards por provedor e configuração. | Configuração, desconexão e token. | Status e erro aparecem em Monitoramento. | Manter configuração e estado saudável em Integrações. | Responsabilidade principal centralizada. |
| `NÃO SE APLICA` | `OK` | **Detalhe da integração** `/integrations/$provider` | Status, metadados, erro, token, webhook e API. | Desconexão e revogação de token. | Falhas resumidas em Monitoramento e Gupy. | Manter diagnóstico e configuração no detalhe. | Responsabilidade principal centralizada. |
| `BLOQUEADA` | `CONTEXTUAL` | **Homologação Gupy** `/integrations/gupy-homologacao` | Prontidão, métricas, endpoints e checklist. | Nenhum. | Avaliações, tentativas, webhooks e DLQ. | Manter somente evidências de homologação. | Depende de token, vaga real e aprovação externa da Gupy. |
| `NÃO SE APLICA` | `OK` | **Plano e cobrança** `/billing` | Créditos, assinatura, uso e históricos. | Cancelamento e checkout externo. | Situação financeira pode bloquear convites. | Manter dados financeiros em Billing. | Responsabilidade centralizada. |
| `NÃO SE APLICA` | `OK` | **Minha conta** `/configuracoes/conta` | Usuário e alteração de senha. | Nenhum. | Nome e e-mail aparecem no menu e em Equipe. | Manter somente credenciais pessoais. | Vínculo e permissão ficam em Equipe. |
| `PENDENTE` | `PARCIAL` | **Comece aqui** `/comecar` | Etapas, progresso e links rápidos. | Nenhum. | Resume Dashboard e demais telas. | Manter somente como onboarding. | Corrigir convite para abrir Participações e ocultar após conclusão. |
| `EM ANDAMENTO` | `PARCIAL` | **Central de manuais** `/manual` | Lista de manuais e painel lateral. | `Sheet` lateral. | Alguns textos ainda descrevem responsabilidades antigas. | Manter manual específico por rota. | PR #422 adicionou o manual de Perfil da empresa; outras rotas pendentes da matriz ainda precisam de revisão. |

## 4. Campos e informações que aparecem em mais de uma tela

A repetição pode permanecer quando for apenas **resumo**, **referência somente leitura** ou **cabeçalho contextual**. O problema ocorre quando duas telas permitem criar, alterar, decidir ou operar sobre o mesmo dado.

| Status da correção | Informação | Tela proprietária | Situação após os PRs #421 e #422 | Próximo passo |
|---|---|---|---|---|
| `EM ANDAMENTO` | Nome, descrição, versão e status da avaliação | **Avaliações** | Compliance deixou de manter lista concorrente, mas outras telas ainda possuem seletores globais. | Retirar seletores desnecessários. |
| `PENDENTE` | Competências do catálogo | **Competências** | Catálogo existe, mas Nova avaliação e Objetivo ainda mantêm ações concorrentes. | Remover criação e edição fora do catálogo. |
| `PENDENTE` | Competências e pesos da versão | **Nova avaliação** | Ainda há mais de um ponto de edição. | Definir um único editor. |
| `PENDENTE` | Personagem e mensagem inicial | **Personagem** | Validador deixou de editar, mas Diálogo e Mapa ainda podem concorrer. | Bloquear edição concorrente. |
| `EM ANDAMENTO` | Etapas, alternativas e conexões | **Diálogo para conteúdo; Mapa para posição e conexão** | Validador não possui mais CRUD. | Retirar edição completa de conteúdo do Mapa. |
| `CONCLUÍDA` | Bloqueios, avisos e qualidade | **Validador** | Lista, severidade, qualidade e ação corretiva estão centralizadas. | Manter apenas contagens e links nas telas secundárias. |
| `CONCLUÍDA` | Publicação, aceite e auditoria | **Governança** | Compliance não mantém outra auditoria ou ação de publicação. | Manter somente status e atalhos fora de Governança. |
| `CONCLUÍDA` | Nome e e-mail do participante | **Participações** | Um read model identifica participações individuais e por jornada. | Outras telas usam referência somente leitura. |
| `CONCLUÍDA` | Status e progresso da participação | **Participações** | Tentativas individuais e jornadas aparecem na mesma tabela; tentativas internas da jornada são ocultadas da lista mestre. | Manter agregados no Dashboard e Piloto. |
| `CONCLUÍDA` | Link, validade, reenvio, reativação e cancelamento | **Participações** | O mesmo contrato oferece ações conforme o tipo e o estado da participação. | Formulários de convite continuam somente criando registros. |
| `CONCLUÍDA` | Score, evidências e decisão humana | **Detalhe do resultado** | Responsabilidade já centralizada. | Manter somente resumo nas listas. |
| `PENDENTE` | Configuração e token de integração | **Detalhe da integração** | Parceiros e telas Gupy ainda precisam separar responsabilidades. | Distinguir tokens. |
| `PENDENTE` | Falhas, retry e DLQ | **Monitoramento** | Ativação e Homologação Gupy ainda repetem informações. | Remover listas concorrentes. |
| `PENDENTE` | Status saudável da integração | **Integrações** | Monitoramento ainda mostra itens saudáveis. | Retirar itens saudáveis da central operacional. |
| `CONCLUÍDA` | Dados cadastrais da empresa | **Perfil da empresa** | A própria empresa consulta e altera os campos em uma API isolada por empresa, com validação e auditoria. | Dashboard, Billing e integrações devem continuar somente exibindo referências. |
| `EM ANDAMENTO` | Usuário, e-mail, status e acesso | **Equipe e Minha conta** | Responsabilidades básicas separadas. | Criar perfis e permissões. |
| `CONCLUÍDA` | Plano, créditos e situação financeira | **Billing** | Responsabilidade já centralizada. | Manter saldo e histórico fora das demais telas. |
| `PENDENTE` | Progresso de onboarding | **Comece aqui** | Destino do convite e ocultação ainda pendentes. | Corrigir fluxo de conclusão. |

## 5. Modelo de propriedade recomendado

| Domínio | Tela proprietária | Telas que podem mostrar resumo ou referência |
|---|---|---|
| Cadastro, versões e ciclo da avaliação | Avaliações | Dashboard, Jornadas, Resultados e Integrações |
| Catálogo global de competências | Competências | Nova avaliação e relatórios |
| Plano inicial da versão | Nova avaliação | Objetivo somente leitura ou removido |
| Contexto e personagem inicial | Personagem | Diálogo e Mapa com link |
| Conteúdo de etapas e alternativas | Diálogo | Mapa visual; Validador somente diagnóstico |
| Posição e conexão visual | Mapa | Diálogo mostra destino textual |
| Validação estrutural | Validador | Diálogo, Mapa, Governança e Compliance mostram contagem ou atalho |
| Publicação, termos e auditoria | Governança | Avaliações e Compliance mostram status ou atalho |
| Composição de processos | Jornadas | Dashboard e convites mostram referência |
| Convites, validade e progresso | Participações | Dashboard, Resultados e formulários de criação |
| Evidências e decisão humana | Detalhe do resultado | Resultados e Talent Match mostram resumo |
| Configuração de integrações | Integrações e detalhe | Monitoramento mostra somente erro |
| Falhas e retentativas | Monitoramento | Gupy e Dashboard mostram contagem ou atalho |
| Dados empresariais | Perfil da empresa | Dashboard e Billing mostram identificação curta |
| Usuários e permissões | Minha equipe | Minha conta mantém somente senha própria |
| Financeiro | Billing | Outras telas mostram bloqueio com link |

## 6. Plano de ação com status

### P0 — fontes de verdade e risco operacional

| Status da correção | Ação | Evidência ou pendência |
|---|---|---|
| `CONCLUÍDA` | Unificar Participações no backend e frontend. | PR #421 criou contrato, tabela e ações únicas para individual e jornada. |
| `EM ANDAMENTO` | Definir um único fluxo de autoria do cenário. | PR #421 concluiu o Validador diagnóstico; ainda falta separar completamente Diálogo e Mapa. |
| `CONCLUÍDA` | Retirar Compliance como lista concorrente. | PR #421 removeu tabela, diálogo concorrente e item de menu. |

### P1 — clareza operacional

| Status da correção | Ação | Critério de conclusão |
|---|---|---|
| `PENDENTE` | Remover edição de competências de Objetivo e criação global em Nova avaliação. | Competência global só é criada e editada em Competências. |
| `PENDENTE` | Corrigir o destino de convite em Comece aqui. | A etapa abre Participações ou convite por jornada. |
| `CONCLUÍDA` | Reconhecer convite por jornada como contexto de Participações. | AppShell, manual, read model e ações funcionam como um único fluxo no PR #421. |
| `PENDENTE` | Retirar integrações saudáveis de Monitoramento. | Monitoramento apresenta somente exceções e retentativas. |
| `PENDENTE` | Remover entregas operacionais de Ativação Gupy. | Entregas ficam somente em Monitoramento. |
| `CONCLUÍDA` | Tornar Perfil da empresa acionável. | PR #422 adicionou edição, validação, auditoria, testes e manual específico. |
| `PENDENTE` | Padronizar confirmações e modais. | Avaliações, Jornadas e Governança usam `Dialog` ou `AlertDialog`. |

### P2 — permissões e acabamento

| Status da correção | Ação | Critério de conclusão |
|---|---|---|
| `PENDENTE` | Criar subperfis de EMPRESA. | Backend e frontend distinguem os perfis autorizados. |
| `PENDENTE` | Incluir perfil e permissões em Equipe. | Modal e tabela mostram e alteram o perfil autorizado. |
| `PENDENTE` | Restringir Parceiros. | Rota aparece somente para empresa parceira e permissão específica. |
| `EM ANDAMENTO` | Atualizar manuais de tela. | PR #422 adicionou o manual de Perfil da empresa; outras rotas da matriz continuam pendentes. |
| `PENDENTE` | Ocultar Comece aqui após onboarding. | Guia deixa o menu operacional e permanece em Ajuda. |

## 7. Critério para atualizar o status

Uma correção só deve receber `CONCLUÍDA` quando:

1. existir uma única tela capaz de criar ou alterar a informação;
2. telas secundárias mostrarem somente resumo ou referência;
3. o resumo possuir link para a tela proprietária;
4. estados e bloqueios vierem da mesma API e regra de negócio;
5. o manual explicar a responsabilidade correta;
6. a permissão for validada no backend e refletida no menu;
7. tabelas e modais possuírem ações coerentes com a finalidade da tela;
8. existir evidência em código, teste ou PR mesclado.

A atualização do status deve incluir a data e a referência do PR ou commit que concluiu a correção.
