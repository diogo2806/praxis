# Matriz de telas e informações do perfil EMPRESA

Data da revisão: 21/07/2026  
Base analisada: `main` após o merge do PR #419

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
| `CONCLUÍDA` | A correção já foi implementada e a responsabilidade da informação está centralizada. |
| `EM ANDAMENTO` | Parte da correção já foi implementada, mas ainda existem pendências. |
| `PENDENTE` | A correção ainda não foi implementada. |
| `BLOQUEADA` | A conclusão depende de integração, homologação externa, regra de negócio ou decisão ainda não disponível. |
| `NÃO SE APLICA` | A tela já funciona apenas como resumo, referência ou detalhe e não exige correção de centralização. |

### 2.2 Classificação do problema

| Classificação | Significado |
|---|---|
| `OK` | Responsabilidade clara e sem duplicidade operacional relevante. |
| `RESUMO` | Repete somente um resumo intencional, sem permitir manutenção do dado. |
| `CONTEXTUAL` | Deve existir somente como etapa ou detalhe aberto a partir da tela proprietária. |
| `PARCIAL` | Responsabilidade razoável, mas ainda há campos, ações ou navegação a corrigir. |
| `DUPLICADO` | Mantém ou altera a mesma informação que outra tela. |
| `SEM AÇÃO` | Exibe dados, mas não oferece ação útil para o usuário. |
| `CONDICIONAL` | Só deveria aparecer para empresas ou permissões específicas. |
| `CRÍTICO` | Pode ocultar registros, criar duas fontes de verdade ou induzir o usuário a erro. |

## 3. Matriz tela a tela

| Status da correção | Classificação | Tela e rota | Tabelas, listas, cards e painéis | Modais e diálogos | Campos ou informações repetidos | Ação para manter a informação em um único lugar | Evidência ou próximo passo |
|---|---|---|---|---|---|---|---|
| `NÃO SE APLICA` | `RESUMO` | **Dashboard** `/dashboard` | Cards de avaliações, jornadas, participações, resultados, próxima ação e tabela de resultados recentes. | Nenhum. | Totais operacionais, participante, processo, situação, resultado e data. | Manter somente indicadores e atalhos. Cada card deve abrir a tela proprietária. | A tela não mantém dados operacionais; somente resume. |
| `PENDENTE` | `PARCIAL` | **Avaliações** `/avaliacoes` | Tabela mestre de avaliações, versões, situação, uso e conclusões. | Duplicação usa `window.prompt`; arquivamento usa `window.confirm`. | Nome, descrição, competências, versão, status, aplicações e conclusões. | Tornar Avaliações proprietária do cadastro, versão e status. Substituir `prompt` e `confirm` por componentes padronizados. | Criar `Dialog` para duplicação e `AlertDialog` para arquivamento. |
| `PENDENTE` | `DUPLICADO` | **Nova avaliação** `/nova/avaliacao` | Formulário em cards, seletor e busca de competências. | Nenhum. | Cargo, situação crítica e competências também aparecem em Objetivo e demais etapas. | Manter o plano inicial aqui, mas remover a criação de competência global. | Permitir somente selecionar competências do catálogo. |
| `PENDENTE` | `DUPLICADO` | **Objetivo do modelo base** `/nova/objetivo` | Card da versão, lista de competências e resumo do modelo. | Nenhum. | Nome, descrição, status, primeira etapa, versão e competências. | Retirar edição de competências e transformar a tela em resumo somente leitura ou redirecionamento. | Alterar rota para consumir o plano sem manter outro editor. |
| `PENDENTE` | `CONTEXTUAL` | **Personagem** `/nova/personagem` | Formulário de personagem e banners de salvamento. | Nenhum. | Mensagem inicial também pode ser alterada em Diálogo, Mapa e Validador. | Tornar Personagem proprietária do contexto e da mensagem inicial. | Bloquear edição concorrente da primeira mensagem nos demais editores. |
| `PENDENTE` | `DUPLICADO` | **Editor de diálogo** `/nova/dialogo` | Lista de etapas e alternativas; editor de mensagem, tempo, mídia, destino, criticidade e pontuação. | Nenhum modal padronizado identificado. | Mesmas etapas, alternativas e conexões são alteradas em Mapa e Validador. | Definir Diálogo como editor textual principal. | Remover CRUD estrutural concorrente do Validador e reduzir edição completa no Mapa. |
| `PENDENTE` | `DUPLICADO` | **Construtor visual** `/nova/mapa` | Canvas de nós e conexões; painel de propriedades; resumo de validação. | Nenhum. | Etapas, mensagens, alternativas, conexões, competências e bloqueios. | Manter posição e conexão visual; conteúdo e pontuação devem ser editados em Diálogo. | Separar claramente propriedades gráficas de conteúdo. |
| `PENDENTE` | `CRÍTICO` | **Validador/Revisão** `/nova/validador` | Canvas, diagnósticos, bloqueios, avisos, qualidade e controles de edição. | Edições internas sem separação clara entre diagnóstico e autoria. | Cria, edita e exclui etapas e alternativas mantidas também em Diálogo e Mapa. | Manter somente diagnóstico, prontidão e links para correção. | Remover CRUD de nós e alternativas do Validador. |
| `PENDENTE` | `CONTEXTUAL` | **Piloto e indicadores** `/nova/piloto` | Cards de tentativas, conclusão, abandono, expiração e calibração. | Nenhum. | Métricas também aparecem em Avaliações, Compliance, Dashboard e Participações. | Manter somente calibração e indicadores analíticos da versão. | Abrir pelo contexto da avaliação e retirar seletor global independente. |
| `PENDENTE` | `CONTEXTUAL` | **Governança e publicação** `/nova/governanca` | Linha de estados, termos, aceite e auditoria. | Confirmação própria em sobreposição. | Status aparece em Avaliações; auditoria aparece em Compliance. | Manter publicação, termos e auditoria somente em Governança. | Trocar sobreposição por `AlertDialog` e retirar auditoria concorrente de Compliance. |
| `PENDENTE` | `DUPLICADO` | **Ativação Gupy por avaliação** `/nova/gupy` | Preflight e lista de entregas de resultado. | Nenhum. | Configuração, entregas, retry e DLQ aparecem em Integrações e Monitoramento. | Manter apenas preflight contextual da versão. | Remover lista operacional de entregas e criar link filtrado para Monitoramento. |
| `NÃO SE APLICA` | `CONTEXTUAL` | **Começar rápido** `/nova/rapido` | Cards de modelos prontos. | Nenhum. | Cria o mesmo rascunho do fluxo de Nova avaliação. | Manter como atalho, sem criar editor paralelo. | Fluxo já encaminha para a revisão da avaliação criada. |
| `EM ANDAMENTO` | `PARCIAL` | **Jornadas** `/jornadas` | Lista de jornadas, formulário e tabela de etapas. | Criação e edição inline; sem confirmação padronizada para todas as ações. | Avaliações, versões e status aparecem também em Avaliações e Dashboard. | Manter somente composição e ordenação das avaliações. | PR #417 retirou participantes e convites da tela; ainda falta padronizar confirmações. |
| `EM ANDAMENTO` | `CRÍTICO` | **Participações** `/participacoes` | Filtros e tabela de participante, avaliação, status, progresso, link, atividade e ações. | Ações de extensão e reenvio são inline. | Identidade, convite e status aparecem em Resultados, Talent Match e formulários de convite. | Tornar Participações proprietária de convite, validade, andamento e ações. | PR #417 centralizou a interface; falta endpoint único para participação individual e jornada. |
| `CONCLUÍDA` | `CONTEXTUAL` | **Nova participação individual** `/enviar-link` | Fluxo de criação, seleção de avaliação e compartilhamento do link. | Nenhum. | Nome, e-mail e link aparecem depois em Participações. | Manter somente como formulário contextual iniciado em Participações. | A rota está acessível pela Central de Participações e não mantém outra tabela mestre. |
| `EM ANDAMENTO` | `CRÍTICO` | **Convite por jornada** `/participacoes/jornada` | Formulário e card do link gerado. | Nenhum. | Identidade, jornada e link deveriam aparecer imediatamente em Participações. | Inserir o convite no mesmo read model de Participações. | Rota criada no PR #417; faltam validade, reenvio, cancelamento e unificação no backend. |
| `NÃO SE APLICA` | `OK` | **Resultados** `/results` | Filtros e tabela de resultados concluídos. | Nenhum. | Participante e avaliação aparecem em Participações; score aparece em Dashboard e Talent Match. | Manter lista de resultados concluídos e abrir o detalhe para análise. | Não mantém convite, validade ou progresso. |
| `NÃO SE APLICA` | `OK` | **Detalhe do resultado** `/results/$attemptId` | Cabeçalho, competências, respostas e decisão humana. | Nenhum. | Identidade e status aparecem em Participações; competências aparecem no catálogo. | Manter evidências e decisão humana somente no detalhe. | Responsabilidade já está centralizada. |
| `PENDENTE` | `CONTEXTUAL` | **Talent Match** `/talent-match` | Seleção de avaliação, lista de participantes, radar, benchmark e legenda. | Nenhum. | Resultados, candidatos, avaliação e competências. | Abrir com contexto obrigatório a partir de Resultados ou Avaliações. | Retirar seletor global e impedir que funcione como segunda lista mestre de candidatos. |
| `PENDENTE` | `PARCIAL` | **Central operacional** `/monitoramento` | Métricas, integrações com atenção, alertas e entregas em DLQ. | Nenhum. | Status saudável aparece em Integrações; entregas aparecem em telas Gupy. | Manter somente falhas, exceções, retry e DLQ. | Retirar integrações saudáveis e criar filtros por provedor, versão e tentativa. |
| `PENDENTE` | `CRÍTICO` | **Compliance** `/compliance` | Tabela global e diálogo de validação, auditoria e privacidade. | `Dialog` de detalhe. | Repete Avaliações, Piloto, Participações, Validador e Governança. | Remover lista global e abrir conformidade apenas no contexto de uma versão. | Validação deve abrir Validador; auditoria e publicação devem abrir Governança. |
| `PENDENTE` | `SEM AÇÃO` | **Perfil da empresa** `/configuracoes/perfil` | Cards e campos somente leitura. | Nenhum. | Nome, status e contatos aparecem em Dashboard, Billing e integrações. | Permitir edição autorizada ou solicitação de alteração. | Se permanecer somente leitura, retirar do menu principal. |
| `EM ANDAMENTO` | `OK` | **Competências** `/competencias` | Total, busca e tabela paginada. | Modal de criar/editar e confirmação de remoção. | Competências aparecem em criação, resultados e comparação. | Manter como única proprietária do catálogo global. | Estrutura principal está correta; falta atualizar manuais e impedir criação global em Nova avaliação. |
| `EM ANDAMENTO` | `PARCIAL` | **Minha equipe** `/team` | Tabela de usuários e ações. | Modal de convite e confirmação de bloqueio. | Nome e e-mail aparecem em Minha conta e no menu; especialistas aparecem em Parceiros. | Tornar Equipe proprietária dos vínculos e permissões internas. | PR #417 removeu navegação quebrada; faltam perfil, permissões e coluna de acesso. |
| `PENDENTE` | `CONDICIONAL` | **Parceiros e especialistas** `/parceiros` | Tabelas de especialistas e clientes, catálogo e formulários. | Formulários e confirmações não totalmente padronizados. | Usuários aparecem em Equipe; provedor e token aparecem em Integrações. | Restringir por feature flag e permissão de empresa parceira. | Separar token de cliente parceiro de token técnico de integração. |
| `NÃO SE APLICA` | `OK` | **Integrações** `/integrations` | Cards por provedor e ações de configuração. | Modal de configuração, desconexão e token. | Status e erro também aparecem em Monitoramento. | Manter configuração e estado saudável somente em Integrações. | Responsabilidade principal já está centralizada. |
| `NÃO SE APLICA` | `OK` | **Detalhe da integração** `/integrations/$provider` | Status, metadados, erro, token, webhook e API pública. | Diálogos de desconexão e revogação de token. | Falhas aparecem resumidas em Monitoramento e telas Gupy. | Manter diagnóstico e configuração do provedor no detalhe. | Responsabilidade principal já está centralizada. |
| `BLOQUEADA` | `CONTEXTUAL` | **Centro de homologação Gupy** `/integrations/gupy-homologacao` | Prontidão, métricas, endpoints e checklist de homologação. | Nenhum. | Avaliações, tentativas, webhooks e DLQ aparecem em outras telas. | Manter somente evidências de homologação e links para telas proprietárias. | Conclusão depende de token, vaga real e aprovação externa da Gupy. |
| `NÃO SE APLICA` | `OK` | **Plano e cobrança** `/billing` | Situação, créditos, assinatura, uso e históricos. | Confirmação de cancelamento e checkout externo. | Situação financeira pode gerar bloqueio em convites. | Manter todos os dados financeiros em Billing. | Responsabilidade já está centralizada. |
| `NÃO SE APLICA` | `OK` | **Minha conta** `/configuracoes/conta` | Card do usuário e formulário de senha. | Nenhum. | Nome e e-mail aparecem no menu e em Equipe. | Manter somente credenciais pessoais; vínculo e permissão ficam em Equipe. | Responsabilidade já está centralizada. |
| `PENDENTE` | `PARCIAL` | **Comece aqui** `/comecar` | Cards de etapas, progresso e links rápidos. | Nenhum. | Resume Dashboard e todas as telas do fluxo. | Manter como onboarding e abrir a tela proprietária de cada etapa. | Corrigir “Convide e acompanhe” para abrir Participações e ocultar após conclusão. |
| `EM ANDAMENTO` | `PARCIAL` | **Central de manuais** `/manual` e manual lateral | Lista de manuais e `Sheet` contextual. | `Sheet` lateral. | Alguns textos ainda descrevem campos ou responsabilidades antigas. | Manter manual específico por rota e alinhado à tela proprietária. | PR #417 criou manuais específicos; ainda faltam rotas de autoria, perfil, parceiros e permissões. |

## 4. Campos e informações que aparecem em mais de uma tela

A repetição pode permanecer quando for apenas **resumo**, **referência somente leitura** ou **cabeçalho contextual**. O problema ocorre quando duas telas permitem criar, alterar, decidir ou operar sobre o mesmo dado.

| Status da correção | Informação | Onde aparece atualmente | Tela proprietária proposta | Regra para as outras telas | Ação necessária | Evidência ou próximo passo |
|---|---|---|---|---|---|---|
| `PENDENTE` | Nome, descrição, versão e status da avaliação | Dashboard, Avaliações, etapas `/nova/*`, Jornadas, Compliance, Piloto, Resultados e Talent Match | **Avaliações e versão contextual** | Mostrar somente referência e link. | Retirar lista global de Compliance e seletores globais desnecessários. | Nenhuma correção estrutural concluída. |
| `PENDENTE` | Competências do catálogo | Competências, Nova avaliação, Objetivo, Diálogo, Mapa, Validador, Resultados e Talent Match | **Competências** | Criação e edição global somente no catálogo. | Remover criação global em Nova avaliação e edição do conjunto em Objetivo. | Catálogo existe, mas os outros editores ainda alteram o dado. |
| `PENDENTE` | Competências e pesos da versão | Nova avaliação, Objetivo, Diálogo, Mapa, Validador, Piloto e Talent Match | **Nova avaliação** | Demais telas consomem snapshot. | Definir um único editor de conjunto, peso, meta e tier. | Pendente de reorganização do fluxo de autoria. |
| `PENDENTE` | Personagem e mensagem inicial | Personagem, Diálogo, Mapa e Validador | **Personagem** | Mostrar referência ou link. | Bloquear edição concorrente da primeira mensagem. | Pendente. |
| `PENDENTE` | Etapas, alternativas e conexões | Diálogo, Mapa e Validador | **Diálogo para conteúdo; Mapa para posição e conexão** | Validador somente diagnostica. | Remover CRUD estrutural do Validador. | P0 pendente. |
| `PENDENTE` | Bloqueios, avisos e qualidade | Diálogo, Mapa, Validador, Compliance e Governança | **Validador** | Mostrar contagem e link. | Centralizar lista, severidade e ação corretiva. | Pendente. |
| `PENDENTE` | Publicação, aceite e auditoria | Avaliações, Governança e Compliance | **Governança** | Avaliações mostra status e Compliance somente linka. | Retirar auditoria concorrente de Compliance. | Pendente. |
| `EM ANDAMENTO` | Nome e e-mail do participante | Convites, Participações, Resultados, detalhe e Talent Match | **Participações** | Resultados e comparação usam snapshot. | Usar o mesmo identificador de participação em todas as telas. | Interface centralizada no PR #417; backend ainda separado. |
| `EM ANDAMENTO` | Status e progresso da participação | Dashboard, Participações, Resultados, Piloto e jornadas | **Participações** | Dashboard e Piloto mostram agregados; Resultados mostra concluídos. | Unificar tentativa individual e jornada. | Endpoint/read model unificado pendente. |
| `EM ANDAMENTO` | Link, validade, reenvio e reativação | Convite individual, convite por jornada e Participações | **Participações** | Formulários apenas criam. | Implementar ciclo completo também para jornadas. | Rota de convite por jornada criada no PR #417. |
| `CONCLUÍDA` | Score, evidências e decisão humana | Dashboard, Resultados, detalhe, Talent Match e integrações | **Detalhe do resultado** | Listas mostram somente resumo. | Manter decisão e evidências no detalhe. | Responsabilidade já centralizada. |
| `PENDENTE` | Configuração e token de integração | Integrações, detalhe, Parceiros e telas Gupy | **Detalhe da integração** | Outras telas verificam prontidão. | Distinguir token de parceiro de token de integração. | Pendente. |
| `PENDENTE` | Falhas, retry e DLQ | Monitoramento, Ativação Gupy e Homologação | **Monitoramento** | Outras telas mostram contagem e link filtrado. | Remover lista de entregas de `/nova/gupy`. | Pendente. |
| `PENDENTE` | Status saudável da integração | Integrações, Monitoramento e Homologação | **Integrações** | Monitoramento não mostra itens saudáveis. | Retirar “Integrações conectadas” de Monitoramento. | Pendente. |
| `PENDENTE` | Dados cadastrais da empresa | Perfil, Dashboard, Billing e integrações | **Configurações da empresa** | Outras telas mostram identificação curta. | Permitir edição ou solicitação de alteração. | Pendente. |
| `EM ANDAMENTO` | Usuário, e-mail, status e acesso | Equipe, Minha conta, menu e Parceiros | **Equipe para vínculo; Minha conta para credencial** | Menu somente resume; Parceiros adiciona função especializada. | Criar perfis e permissões e incluir coluna Perfil. | Limpeza inicial feita no PR #417; autorização granular pendente. |
| `CONCLUÍDA` | Plano, créditos e situação financeira | Billing e bloqueios de criação ou convite | **Billing** | Outras telas mostram somente o motivo do bloqueio. | Manter saldo e histórico fora das demais telas. | Responsabilidade já centralizada. |
| `PENDENTE` | Progresso de onboarding | Comece aqui e Dashboard | **Comece aqui durante a configuração inicial** | Dashboard mostra somente próxima ação. | Corrigir destino do convite e ocultar guia após conclusão. | Pendente. |

## 5. Modelo de propriedade recomendado

| Domínio | Tela proprietária | Telas que podem mostrar resumo ou referência |
|---|---|---|
| Cadastro, versões e ciclo da avaliação | Avaliações | Dashboard, Jornadas, Resultados e Integrações |
| Catálogo global de competências | Competências | Nova avaliação e relatórios, somente seleção ou snapshot |
| Plano inicial da versão | Nova avaliação | Objetivo somente leitura ou removido |
| Contexto e personagem inicial | Personagem | Diálogo, Mapa e Validador com link |
| Conteúdo de etapas e alternativas | Diálogo | Mapa visual e Validador diagnóstico |
| Posição e conexão visual | Mapa | Diálogo mostra destino textual |
| Validação estrutural | Validador | Diálogo, Mapa e Governança mostram contagem e atalho |
| Publicação, termos e auditoria | Governança | Avaliações mostra status |
| Composição de processos | Jornadas | Dashboard e convites mostram referência |
| Convites, validade e progresso | Participações | Dashboard, Resultados e formulários de criação |
| Evidências e decisão humana | Detalhe do resultado | Resultados e Talent Match mostram resumo |
| Configuração de integrações | Integrações e detalhe | Monitoramento mostra somente erro |
| Falhas e retentativas | Monitoramento | Gupy e Dashboard mostram contagem e atalho |
| Dados empresariais | Configurações da empresa | Dashboard e Billing mostram identificação curta |
| Usuários e permissões | Minha equipe | Minha conta mantém somente senha própria |
| Financeiro | Billing | Outras telas mostram bloqueio com link |

## 6. Plano de ação com status

### P0 — fontes de verdade e risco operacional

| Status da correção | Ação | Critério de conclusão |
|---|---|---|
| `EM ANDAMENTO` | Unificar Participações no backend. | Convite individual e de jornada aparecem imediatamente na mesma tabela e usam as mesmas ações. |
| `PENDENTE` | Definir um único fluxo de autoria do cenário. | Diálogo edita conteúdo, Mapa edita posição e conexão, Validador somente diagnostica. |
| `PENDENTE` | Retirar Compliance como lista concorrente. | Compliance é aberto somente no contexto da avaliação e direciona para as telas proprietárias. |

### P1 — clareza operacional

| Status da correção | Ação | Critério de conclusão |
|---|---|---|
| `PENDENTE` | Remover edição de competências de Objetivo e criação global em Nova avaliação. | Competência global só é criada e editada em Competências. |
| `PENDENTE` | Corrigir o destino de convite em Comece aqui. | A etapa abre Participações ou convite por jornada. |
| `EM ANDAMENTO` | Reconhecer convite por jornada como contexto de Participações. | AppShell, manual, retorno e seleção da linha funcionam como um único fluxo. |
| `PENDENTE` | Retirar integrações saudáveis de Monitoramento. | Monitoramento apresenta somente exceções e retentativas. |
| `PENDENTE` | Remover entregas operacionais de Ativação Gupy. | Entregas ficam somente em Monitoramento com filtro contextual. |
| `PENDENTE` | Tornar Perfil da empresa acionável. | Usuário autorizado edita ou solicita alteração. |
| `PENDENTE` | Padronizar confirmações e modais. | Avaliações, Jornadas e Governança usam `Dialog` ou `AlertDialog`. |

### P2 — permissões e acabamento

| Status da correção | Ação | Critério de conclusão |
|---|---|---|
| `PENDENTE` | Criar subperfis de EMPRESA. | Backend e frontend distinguem administrador, recrutador, avaliador, conteúdo, integração, financeiro e leitura. |
| `PENDENTE` | Incluir perfil e permissões em Equipe. | Modal e tabela mostram e alteram o perfil autorizado. |
| `PENDENTE` | Restringir Parceiros. | Rota aparece somente para empresa parceira e permissão específica. |
| `EM ANDAMENTO` | Atualizar manuais de tela. | Todas as rotas possuem manual específico e alinhado à tela proprietária. |
| `PENDENTE` | Ocultar Comece aqui após onboarding. | Guia deixa o menu operacional e permanece disponível em Ajuda. |

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