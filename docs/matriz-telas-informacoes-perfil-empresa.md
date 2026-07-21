# Matriz de telas e informações do perfil EMPRESA

Data da revisão: 21/07/2026  
Base analisada: `main` no commit `40aad39104c8993a28627fb7ac4937b21932257c`

## 1. Escopo

Este documento inventaria as telas acessíveis ao perfil `EMPRESA`, incluindo rotas principais, telas contextuais e rotas condicionais. Para cada tela, registra:

- tabelas, listas, cards e painéis exibidos;
- modais, diálogos e confirmações;
- campos e informações principais;
- informações repetidas em outras telas;
- tela que deve ser proprietária do dado;
- ação necessária para evitar mais de uma fonte de verdade;
- status atual.

Neste documento, **tabela** significa tabela ou lista visual da interface, não tabela física do banco de dados.

## 2. Legenda de status

| Status | Significado |
|---|---|
| `OK` | Responsabilidade clara e sem duplicidade operacional relevante. |
| `RESUMO` | Repete apenas um resumo intencional, sem permitir manutenção do mesmo dado. |
| `CONTEXTUAL` | Deve existir somente como etapa ou detalhe aberto a partir da tela proprietária. |
| `PARCIAL` | Responsabilidade razoável, mas ainda há campos, ações ou navegação a corrigir. |
| `DUPLICADO` | Mantém ou altera a mesma informação que outra tela. |
| `SEM AÇÃO` | Exibe dados, mas não oferece ação útil para o usuário. |
| `CONDICIONAL` | Só deveria aparecer para empresas ou permissões específicas. |
| `CRÍTICO` | A organização atual pode ocultar registros, criar duas fontes de verdade ou induzir o usuário a erro. |

## 3. Matriz tela a tela

| Status | Tela e rota | Tabelas, listas, cards e painéis | Modais e diálogos | Campos e informações exibidos | Informações repetidas | Ação para manter cada informação em um único lugar |
|---|---|---|---|---|---|---|
| `RESUMO` | **Dashboard** `/dashboard` | Cards de avaliações ativas, jornadas, participações em andamento e resultados em 30 dias; card de próxima ação; tabela de até cinco resultados recentes. | Nenhum. | Empresa, totais operacionais, pendência prioritária, participante, processo, situação, resultado e data. | Totais de Avaliações, Jornadas, Participações e Resultados. | Manter somente indicadores e atalhos. Não permitir criar, editar, reenviar, publicar ou decidir no Dashboard. Cada card deve abrir a tela proprietária. |
| `PARCIAL` | **Avaliações** `/avaliacoes` | Tabela mestre com avaliação, descrição, competências, atualização, situação, versão publicada, aplicações e conclusões. | Não possui modal padronizado. Duplicação usa `window.prompt`; arquivamento usa `window.confirm`. | Nome, descrição, quantidade de competências, versão, status, percentual configurado, última atualização, aplicações e conclusões. | Status, versão, competências, uso e conclusão também aparecem em Compliance, Piloto, Talent Match, Jornada e criação. | Tornar Avaliações proprietária do cadastro, versão e status da avaliação. Outras telas recebem somente referência de nome/versão. Substituir `prompt` e `confirm` por `Dialog` e `AlertDialog`. |
| `DUPLICADO` | **Nova avaliação** `/nova/avaliacao` | Formulário em cards; seletor e busca de competências. | Nenhum. | Cargo/nome, situação crítica, competências selecionadas e criação rápida de nova competência. | Competências também são criadas em Competências e alteradas em Objetivo; nome, situação e competências reaparecem em várias etapas. | Esta tela deve ser proprietária do plano inicial da avaliação. Remover a criação direta de competência global; permitir somente selecionar competências existentes e oferecer link para o catálogo. |
| `DUPLICADO` | **Objetivo do modelo base** `/nova/objetivo` | Card da versão, lista de competências e resumo do modelo. | Nenhum. | Nome, descrição, status, primeira etapa, versão e competências; permite adicionar e remover competências em rascunho. | Repete e altera competências já mantidas em Nova avaliação; também repete nome, descrição, versão e status. | Retirar a edição de competências. Transformar em resumo somente leitura ou redirecionar para `/nova/avaliacao`. A manutenção deve existir em uma única etapa. |
| `CONTEXTUAL` | **Personagem** `/nova/personagem` | Formulário de personagem e banners de autosave/rascunho local. | Nenhum. | Nome do personagem, emoção, contexto, mensagem inicial e status de salvamento. | A mensagem inicial também pode ser alterada em Diálogo, Mapa e Validador. | Personagem deve ser o proprietário do contexto e da mensagem inicial. Nos outros editores, mostrar a etapa inicial com link para editar aqui ou deixar somente edição avançada explicitamente identificada. |
| `DUPLICADO` | **Editor de diálogo** `/nova/dialogo` | Lista de etapas e alternativas; editor de mensagem, tempo, mídia, destino, criticidade e pontuação; resumo de validação. | Nenhum modal padronizado identificado. | Etapa, mensagem, tempo, mídia, alternativa, próxima etapa, competência, nível/pontuação, criticidade e erros por nó. | As mesmas etapas, alternativas e conexões são editadas em Mapa e Validador. | Escolher um editor principal. Recomendação: Diálogo como editor textual principal. Mapa fica visual e Validador fica diagnóstico, ambos sem CRUD completo. |
| `DUPLICADO` | **Construtor visual** `/nova/mapa` | Canvas de nós e conexões; painel de propriedades; resumo de validação. | Nenhum. | Etapas, mensagens, alternativas, conexões, posição visual, competências e bloqueios. | Altera as mesmas entidades de Diálogo e Validador. | Tornar Mapa uma visualização e organização gráfica. Permitir posicionar e conectar, mas encaminhar edição de conteúdo e pontuação para Diálogo. Não criar uma segunda interface completa de autoria. |
| `CRÍTICO` | **Validador/Revisão** `/nova/validador` | Canvas, grupos de diagnóstico, bloqueios, avisos, qualidade e controles de edição de nós/opções. | Edições e confirmações internas; não há uma separação clara entre diagnóstico e autoria. | Bloqueios, avisos, qualidade, nó, mensagem, tempo, alternativas, conexões e competências. | Além de validar, cria, edita e exclui etapas e alternativas, repetindo Diálogo e Mapa. Compliance também exibe validação. | Tornar esta tela proprietária apenas dos diagnósticos, bloqueios, avisos e prontidão. Cada erro deve ter ação “Corrigir em Diálogo/Mapa”. Remover CRUD estrutural completo do Validador. |
| `CONTEXTUAL` | **Piloto e indicadores** `/nova/piloto` | Cards de tentativas criadas, em andamento, concluídas, abandonadas, expiradas, conclusão e desistência; relatório de calibração. | Nenhum. | Métricas de execução e calibração da versão. | Aplicações, conclusões e taxas também aparecem em Avaliações, Compliance, Dashboard e Participações. | Manter apenas calibração e indicadores analíticos da versão. Totais operacionais devem ser referências; acompanhamento de pessoas fica em Participações. Abrir Piloto a partir da avaliação selecionada, sem seletor global próprio. |
| `CONTEXTUAL` | **Governança e publicação** `/nova/governanca` | Linha de estados, termos de responsabilidade/saúde e lista de auditoria. | Confirmação de publicação implementada como sobreposição própria, não componente padronizado. | Status, termo, aceite, data de aceite, bloqueio de saúde e eventos de auditoria. | Status aparece em Avaliações; auditoria aparece em Compliance; validação é mencionada no fluxo. | Governança deve ser proprietária de publicação, aceite e auditoria da versão. Avaliações mostra apenas status e atalho. Compliance não deve repetir a mesma auditoria. Trocar a sobreposição por `AlertDialog`. |
| `DUPLICADO` | **Ativação Gupy por avaliação** `/nova/gupy` | Lista de preflight e lista de até dez entregas de resultado da versão. | Nenhum. | Avaliação/versão, checks técnicos, bloqueios, resultId, attemptId, status de entrega e próxima tentativa. | Configuração aparece em Integrações; entregas e DLQ aparecem em Monitoramento; homologação geral aparece em Centro de homologação Gupy. | Manter apenas o preflight contextual da versão. Remover a lista operacional de entregas daqui; fornecer link filtrado para Monitoramento. Configuração e token ficam em Integrações. |
| `CONTEXTUAL` | **Começar rápido** `/nova/rapido` | Cards de modelos prontos. | Nenhum. | Categoria, título, descrição e quantidade de cenários do modelo. | Cria o mesmo rascunho que Nova avaliação, mas por atalho. | Manter como atalho de criação. Após escolher modelo, abrir o editor principal definido, sem criar um fluxo paralelo de manutenção. |
| `PARCIAL` | **Jornadas** `/jornadas` | Lista lateral de jornadas; formulário de nova jornada; tabela de etapas com ordem, avaliação, versão, obrigatoriedade e ações. | Nenhum; criação e edição são inline. | Nome, descrição, status, quantidade de avaliações/sequências, sequência, ordem e obrigatoriedade. | Nome/status aparecem no Dashboard e convite por jornada; avaliações/versões aparecem em Avaliações. | Manter Jornadas como proprietária da composição. Convites e pessoas não devem voltar para esta tela. Referências de avaliação devem ser somente leitura. Adicionar confirmação padronizada para arquivar/remover etapa. |
| `CRÍTICO` | **Participações** `/participacoes` | Filtros por situação, avaliação e participante; tabela com participante, avaliação, situação, progresso, link, última atividade e ações. | Nenhum; extensão e reenvio são ações inline. | Nome, e-mail, avaliação/versão, status, progresso, validade, última atividade e link. | Identidade e status aparecem em Resultados, Talent Match, Dashboard, convite individual e convite por jornada. | Criar um endpoint/read model unificado que combine tentativa individual e tentativa de jornada. Participações deve ser a proprietária de convite, validade, andamento e ações. Hoje a consulta usa somente tentativas individuais e pode omitir convites de jornada. |
| `CONTEXTUAL` | **Nova participação individual** `/enviar-link` | Fluxo em três passos; lista de avaliações publicadas; resumo e compartilhamento do link. | Nenhum. | Avaliação, nome, e-mail, link gerado, operação realizada e canais de compartilhamento. | Nome/e-mail/link aparecem depois em Participações; avaliação aparece em Avaliações. | Manter somente como formulário contextual iniciado em Participações. Após criar, redirecionar ou destacar o registro correspondente na tabela central. |
| `CRÍTICO` | **Convite por jornada** `/participacoes/jornada` | Formulário e card do link gerado. | Nenhum. | Jornada, nome, e-mail, sequência e link. | Identidade e link deveriam aparecer em Participações; jornada aparece em Jornadas. | Após criar, o registro deve entrar imediatamente no read model unificado de Participações. Implementar validade, reenvio e cancelamento para o convite de jornada ou deixar explícito quais ações são diferentes. |
| `OK` | **Resultados** `/results` | Filtros; tabela de participante, avaliação, resultado, competência em destaque, origem e ação. | Nenhum. | Nome, e-mail, avaliação, score geral, competência em destaque e origem. | Identidade/avaliação aparecem em Participações; resultado resumido aparece no Dashboard e Talent Match. | Manter como proprietária da lista de resultados concluídos. Não exibir tentativas em andamento. Identidade e avaliação são referências somente leitura. |
| `OK` | **Detalhe do resultado** `/results/$attemptId` | Cards/seções de cabeçalho, competências, respostas e decisão humana. | Nenhum. | Participante, e-mail, score, avaliação, status, datas, competências, níveis, respostas, pontuações e decisão humana. | Competências aparecem no catálogo/Talent Match; participante/status aparecem em Participações. | Manter como proprietária de evidências e decisão humana. Outras telas exibem somente resumo e link para este detalhe. |
| `CONTEXTUAL` | **Talent Match** `/talent-match` | Tabela de seleção de avaliação quando sem contexto; lista paginada de participantes; gráfico radar; benchmark e legenda. | Nenhum. | Avaliação/versão, candidatos concluídos, modo cego, competências, meta e score comparado. | Repete resultados, candidatos, avaliação e competências. | Abrir preferencialmente a partir de Resultados ou Avaliações com contexto obrigatório. Manter somente comparação temporária; não cadastrar competência, alterar resultado ou manter outra lista mestre de candidatos. |
| `PARCIAL` | **Central operacional** `/monitoramento` | Cards de métricas; painel de integrações com atenção; lista de alertas não lidos; lista de entregas em DLQ. | Nenhum. | Integrações conectadas/com atenção, entregas em retry/DLQ, notificações, erros e datas. | Integrações saudáveis aparecem em Integrações; entregas aparecem em Ativação Gupy/Homologação; alertas podem aparecer no Dashboard. | Tornar Monitoramento proprietário somente de exceções e retentativas. Remover “Integrações conectadas” e dados saudáveis. Ativação Gupy e Homologação devem apenas linkar para uma visão filtrada desta central. |
| `CRÍTICO` | **Compliance** `/compliance` | Tabela de avaliação, versão, status, taxa de conclusão, bloqueios, tentativas, atualização e ação; detalhe contextual. | `Dialog` com validação, auditoria e privacidade da versão. | Avaliação, versão, status, conclusão, tentativas, bloqueios, validação, auditoria e privacidade. | Repete Avaliações, Piloto, Participações, Validador e Governança. A coluna Bloqueios ainda não possui dado útil na listagem. | Remover a lista global e o item de menu. Abrir Compliance como detalhe contextual da versão. Validação fica no Validador, auditoria/publicação em Governança e privacidade corporativa em Configurações. |
| `SEM AÇÃO` | **Perfil da empresa** `/configuracoes/perfil` | Cards/campos somente leitura. | Nenhum. | Nome fantasia, razão social, CNPJ, e-mail corporativo, telefone e site. | Nome/status da empresa aparecem no Dashboard e Billing; contatos podem aparecer em integrações/documentos. | Transformar em “Configurações da empresa” com edição autorizada ou botão “Solicitar alteração”. Se continuar somente leitura, retirar do menu principal e mostrar no contexto de cobrança/conta. |
| `OK` | **Competências** `/competencias` | Card de total; busca; tabela paginada de competência, status e ações. | Modal de criar/editar; modal de confirmação de remoção. | Nome/identificador e status da competência. | Competências aparecem em Nova avaliação, Objetivo, Diálogo, Validador, Resultados e Talent Match. | Tornar esta tela proprietária do catálogo global. Todas as outras telas apenas selecionam ou exibem snapshots da competência. Ajustar o manual, que hoje descreve campos de descrição e critérios não existentes na tela. |
| `PARCIAL` | **Minha equipe** `/team` | Tabela de nome, e-mail, status, último acesso e ações. | Modal de convite; modal de confirmação de bloqueio. | Nome, e-mail, status e último acesso. | Nome/e-mail do próprio usuário aparecem em Minha conta e no rodapé do menu; especialistas têm gestão separada em Parceiros. | Tornar Equipe proprietária dos vínculos e permissões internas. Adicionar perfil/permissões ao convite e à tabela. Manter Minha conta proprietária apenas das credenciais do usuário atual. |
| `CONDICIONAL` | **Parceiros e especialistas** `/parceiros` | Tabela de especialistas; tabela/lista de clientes do parceiro; catálogo de avaliações; formulários de especialista e cliente; card/token gerado. | Não usa modal padronizado para os principais formulários e confirmações. | Especialista, e-mail, status, cliente, provedor, ID externo, catálogo atribuído, token e situação. | Especialistas também são usuários de Equipe; provedores/tokens aparecem em Integrações; avaliações aparecem em Avaliações. | Restringir por feature flag e permissão de empresa parceira. Equipe mantém o usuário; Parceiros mantém função especializada, clientes e catálogo. Tokens de integração devem ser geridos em Integrações ou claramente separados como token do cliente parceiro. |
| `OK` | **Integrações** `/integrations` | Cards por provedor; cards explicativos de candidatos, resultados e webhooks. | Modal de configuração; diálogo de desconexão; modal de token gerado. | Provedor, nome, status, descrição, fluxo, erro, última atividade, configuração, token e ações disponíveis. | Status/erro aparecem em Monitoramento; fluxos e resultados aparecem em outras telas. | Manter Integrações como proprietária da configuração e do estado saudável. Monitoramento recebe somente exceções. Remover cards explicativos se não gerarem ação e tornarem a tela longa. |
| `OK` | **Detalhe da integração** `/integrations/$provider` | Cards de status, metadados, falha, token, webhook e API pública conforme o provedor. | Diálogo de desconexão; diálogo de revogação de token; token gerado é exibido no detalhe. | Tipo, status, última atividade, data de configuração, erro, token, webhook, segredo, endpoint e ações. | Status e erro aparecem na Central operacional; Gupy possui telas adicionais de ativação/homologação. | Manter como proprietária do diagnóstico/configuração do provedor. Monitoramento mostra apenas resumo da falha e link para este detalhe. |
| `CONTEXTUAL` | **Centro de homologação Gupy** `/integrations/gupy-homologacao` | Cards de prontidão e métricas; lista de endpoints; checklist de homologação. | Nenhum. | Status geral, percentual, avaliações publicadas, tentativas Gupy, conclusões, webhooks, DLQ, endpoints e evidências. | Repete Dashboard, Participações, Resultados, Integrações e Monitoramento. | Manter somente para período de homologação e empresas com Gupy. Métricas devem ser evidências somente leitura. DLQ e falhas abrem Monitoramento; tokens/configuração abrem Integrações. |
| `OK` | **Plano e cobrança** `/billing` | Cards de situação, créditos, recarga, assinatura e uso; histórico de eventos e movimentações; opções de plano. | Confirmação de cancelamento é controlada dentro do card; checkouts abrem externamente. | Plano, empresa, situação financeira, saldo total/reservado/livre, recarga, pagamento, assinatura, renovação, uso e histórico. | Situação da empresa aparece no Perfil; bloqueios de crédito podem aparecer em convites. | Manter Billing como proprietária de todos os dados financeiros. Outras telas mostram apenas bloqueio resumido com link para Billing, sem saldo detalhado ou ações financeiras. |
| `OK` | **Minha conta** `/configuracoes/conta` | Card do usuário e formulário de senha. | Nenhum. | Nome, e-mail, senha atual, nova senha e confirmação. | Nome/e-mail aparecem no menu e na tabela da Equipe. | Minha conta é proprietária das credenciais pessoais. Menu e Equipe exibem cópias somente leitura. Alterações de vínculo, status e perfil ficam em Equipe. |
| `PARCIAL` | **Comece aqui** `/comecar` | Cards das seis etapas, barra de progresso e links rápidos. | Nenhum. | Progresso de onboarding, avaliação, cenário, publicação, jornada, convite e resultado. | Resume dados de Dashboard e todas as telas do fluxo. | Manter apenas durante onboarding ou deixar dentro de Ajuda. Corrigir “Convide e acompanhe”, que ainda abre Jornadas; deve abrir `/participacoes/jornada` ou `/participacoes`. |
| `PARCIAL` | **Central de manuais** `/manual` e manual lateral global | Lista de manuais; manual lateral em `Sheet` disponível nas telas. | `Sheet` lateral com finalidade, fluxo, campos, permissões, estados, bloqueios, exemplos, atalhos e link completo. | Conteúdo operacional por rota. | Alguns textos ainda descrevem responsabilidades antigas ou campos inexistentes. | Atualizar manual por rota a partir desta matriz. Não usar manual genérico em telas operacionais. Corrigir Jornada, Competências, Participações, Compliance, Perfil, Parceiros e todas as etapas `/nova/*`. |

## 4. Campos e informações que aparecem em mais de uma tela

A repetição não precisa ser eliminada quando for apenas **resumo**, **referência somente leitura** ou **cabeçalho contextual**. O problema ocorre quando duas telas permitem alterar, decidir ou operar sobre o mesmo dado.

| Informação | Onde aparece atualmente | Tela proprietária proposta | Regra para as outras telas | Status | Ação necessária |
|---|---|---|---|---|---|
| Nome, descrição, versão e status da avaliação | Dashboard, Avaliações, todas as etapas `/nova/*`, Jornadas, Compliance, Piloto, Resultados e Talent Match | **Avaliações + versão contextual** | Mostrar nome/versão como referência e link; não manter outro cadastro. | `PARCIAL` | Retirar a lista global de Compliance e seletores globais de telas que deveriam receber contexto. |
| Competências do catálogo | Competências, Nova avaliação, Objetivo, Diálogo, Mapa, Validador, Resultados e Talent Match | **Competências** | Criação/edição global somente no catálogo; avaliações salvam um snapshot selecionado. | `DUPLICADO` | Remover criação global em Nova avaliação e edição do conjunto em Objetivo. |
| Competências e pesos da versão | Nova avaliação, Objetivo, Diálogo, Mapa, Validador, Piloto e Talent Match | **Plano da avaliação em `/nova/avaliacao`** | Demais telas apenas consomem o snapshot; ajustes de calibração voltam ao plano por ação explícita. | `DUPLICADO` | Definir uma única tela para editar conjunto, peso, meta e tier. |
| Personagem e mensagem inicial | Personagem, Diálogo, Mapa e Validador | **Personagem** | Outros editores mostram referência ou link para edição. | `DUPLICADO` | Bloquear edição concorrente da primeira mensagem em três telas. |
| Etapas, alternativas e conexões | Diálogo, Mapa e Validador | **Diálogo para conteúdo; Mapa para posição/conexão** | Validador somente diagnostica; Mapa não edita pontuação/conteúdo completo. | `CRÍTICO` | Remover CRUD estrutural do Validador e reduzir sobreposição entre Diálogo e Mapa. |
| Bloqueios, avisos e qualidade da versão | Diálogo, Mapa, Validador, Compliance e Governança | **Validador** | Mostrar apenas contagem e link “Ver diagnóstico”. | `DUPLICADO` | Centralizar lista, severidade e ação corretiva no Validador. |
| Publicação, aceite e auditoria da versão | Avaliações, Governança e Compliance | **Governança** | Avaliações exibe status/atalho; Compliance não mantém outra trilha. | `DUPLICADO` | Retirar auditoria do modal de Compliance ou apenas linkar para Governança. |
| Nome e e-mail do participante | Convite individual, convite por jornada, Participações, Resultados, detalhe e Talent Match | **Participações** para identidade operacional da tentativa | Resultados/Talent Match exibem snapshot somente leitura. | `PARCIAL` | Criar read model único e usar o mesmo identificador da participação em todas as telas. |
| Status e progresso da participação | Dashboard, Participações, Resultados, Piloto e jornadas internas | **Participações** | Dashboard/Piloto mostram agregados; Resultados somente concluídos. | `CRÍTICO` | Unificar tentativa individual e tentativa de jornada no endpoint da Central de Participações. |
| Link, validade, reenvio e reativação | Convite individual, convite por jornada e Participações | **Participações** | Formulários apenas criam; depois a gestão ocorre na central. | `CRÍTICO` | Implementar ciclo de validade e reenvio também para jornadas ou declarar regra específica. |
| Score e evidências | Dashboard, Resultados, detalhe, Talent Match, Piloto e entregas de integração | **Detalhe do resultado** | Listas mostram score resumido; integrações recebem payload técnico sem criar outra análise. | `OK` | Manter decisão humana e evidências somente no detalhe. |
| Configuração e token de integração | Integrações, detalhe, Parceiros e telas Gupy | **Integrações/detalhe do provedor** | Outras telas apenas verificam prontidão ou usam referência. | `DUPLICADO` | Distinguir token de parceiro de token de integração e remover configuração das telas de ativação. |
| Falhas, retry e DLQ | Monitoramento, Ativação Gupy e Homologação Gupy | **Monitoramento** | Outras telas mostram contagem e link filtrado. | `DUPLICADO` | Remover lista de entregas de `/nova/gupy`; fornecer filtros por provedor, versão e tentativa. |
| Status saudável da integração | Integrações, Monitoramento e Homologação | **Integrações** | Monitoramento não mostra itens saudáveis. | `PARCIAL` | Remover “Integrações conectadas” da Central operacional. |
| Dados cadastrais da empresa | Perfil, Dashboard, Billing e integrações | **Configurações da empresa** | Cabeçalhos podem mostrar nome; Billing mostra apenas vínculo financeiro. | `SEM AÇÃO` | Permitir edição autorizada ou solicitação de alteração; caso contrário esconder do menu. |
| Usuário, e-mail, status e acesso | Equipe, Minha conta, menu e Parceiros | **Equipe** para vínculo/permissão; **Minha conta** para credencial pessoal | Menu somente resume; Parceiros adiciona função especializada sem duplicar o usuário. | `PARCIAL` | Criar perfis/permissões e incluir a coluna Perfil na equipe. |
| Plano, créditos e situação financeira | Billing e bloqueios de criação/convite | **Billing** | Outras telas exibem apenas motivo do bloqueio e link para resolver. | `OK` | Evitar mostrar saldo e histórico fora de Billing. |
| Progresso de onboarding | Comece aqui e Dashboard | **Comece aqui**, durante a configuração inicial | Dashboard mostra somente próxima ação operacional. | `PARCIAL` | Corrigir destino da etapa de convite e esconder o guia após conclusão, mantendo acesso em Ajuda. |

## 5. Modelo de propriedade recomendado

| Domínio | Tela proprietária | Telas que podem mostrar resumo ou referência |
|---|---|---|
| Cadastro, versões e ciclo da avaliação | Avaliações | Dashboard, Jornadas, Resultados, Integrações |
| Catálogo global de competências | Competências | Nova avaliação e relatórios, somente seleção/snapshot |
| Plano inicial da versão | Nova avaliação | Objetivo somente leitura ou removido |
| Contexto/personagem inicial | Personagem | Diálogo/Mapa/Validador com link |
| Conteúdo de etapas e alternativas | Diálogo | Mapa visual; Validador diagnóstico |
| Posição e conexão visual | Mapa | Diálogo mostra destino textual |
| Validação estrutural | Validador | Diálogo, Mapa e Governança mostram contagem/atalho |
| Publicação, termos e auditoria | Governança | Avaliações mostra status |
| Composição de processos | Jornadas | Dashboard e convites mostram referência |
| Convites, validade e progresso | Participações | Dashboard, Resultados e formulários de criação |
| Evidências e decisão humana | Detalhe do resultado | Resultados e Talent Match mostram resumo |
| Configuração de integrações | Integrações/detalhe | Monitoramento mostra apenas erro |
| Falhas e retentativas | Monitoramento | Gupy e Dashboard mostram contagem/atalho |
| Dados empresariais | Configurações da empresa | Dashboard/Billing mostram identificação curta |
| Usuários e permissões | Minha equipe | Minha conta mantém apenas senha própria |
| Financeiro | Billing | Outras telas mostram bloqueio com link |

## 6. Plano de ação

### P0 — bloqueios de organização e fonte de verdade

1. **Unificar Participações no backend**
   - Criar um DTO/read model que represente participação individual e participação por jornada.
   - Incluir `participationType`, `journeyId`, `journeyName`, `sequenceKey`, progresso agregado, validade, ações permitidas e tentativa atual.
   - Fazer `/participacoes` consumir somente esse endpoint.
   - Garantir que o convite por jornada apareça na central imediatamente após a criação.

2. **Definir um único fluxo de autoria do cenário**
   - Diálogo: conteúdo, alternativas, pontuação e mídia.
   - Mapa: posição e conexão visual.
   - Validador: bloqueios, avisos e links de correção.
   - Remover do Validador o CRUD completo de nós e alternativas.

3. **Retirar Compliance como lista concorrente**
   - Remover a tabela global e o item principal de menu.
   - Abrir conformidade no contexto de uma avaliação/versão.
   - Direcionar validação para Validador, auditoria/publicação para Governança e privacidade para Configurações.

### P1 — clareza operacional

4. Remover edição de competências de `/nova/objetivo` e criação global em `/nova/avaliacao`.
5. Corrigir “Convide e acompanhe” em Comece aqui para abrir Participações.
6. Reconhecer `/participacoes/jornada` como contexto de Participações no AppShell e no manual.
7. Retirar integrações conectadas da Central operacional.
8. Remover entregas operacionais da tela `/nova/gupy` e criar link filtrado para Monitoramento.
9. Transformar Perfil da empresa em tela editável/solicitação ou removê-la do menu.
10. Padronizar confirmações de Avaliações, Jornadas e Governança com `Dialog`/`AlertDialog`.

### P2 — permissões e acabamento

11. Criar subperfis de EMPRESA: administrador, recrutador, avaliador, conteúdo, integração/TI, financeiro e somente leitura.
12. Incluir perfil e permissões na tabela e no modal de Equipe.
13. Restringir Parceiros por feature flag e permissão específica.
14. Atualizar todos os manuais de tela para refletir o proprietário real de cada informação.
15. Ocultar Comece aqui após onboarding concluído, mantendo o acesso em Ajuda.

## 7. Critério de aceite para futuras correções

Uma correção desta matriz só deve receber status `OK` quando:

1. existir uma única tela capaz de criar ou alterar a informação;
2. telas secundárias mostrarem apenas resumo ou referência somente leitura;
3. o resumo possuir link para a tela proprietária;
4. os estados e bloqueios vierem da mesma API/regra de negócio;
5. o manual da tela explicar a responsabilidade correta;
6. a permissão do usuário for validada no backend e refletida no menu;
7. tabelas e modais possuírem ações coerentes com a finalidade da tela;
8. rotas contextuais não aparecerem como módulos independentes sem necessidade.
