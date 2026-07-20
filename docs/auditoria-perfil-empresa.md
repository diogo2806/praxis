# Auditoria de acesso e organização das telas — perfil EMPRESA

Data da auditoria: 20/07/2026  
Base analisada: branch `main`

## 1. Conclusão sobre o perfil de acesso

O sistema possui atualmente um único perfil operacional para os usuários de uma empresa: `EMPRESA`.

Quando uma pessoa é convidada pela tela **Minha equipe**, o backend atribui obrigatoriamente a função `EMPRESA`. O formulário não permite selecionar administrador da empresa, recrutador, gestor, analista, financeiro, integração ou somente leitura.

Consequências atuais:

- todos os usuários `EMPRESA` recebem o mesmo conjunto de APIs;
- o menu não é filtrado por subperfil ou permissão;
- qualquer usuário `EMPRESA` pode, em princípio, acessar avaliações, resultados, equipe, integrações, cobrança e configurações;
- os estados `ATIVO`, `CONVIDADO` e `BLOQUEADO` controlam a situação do acesso, mas não alteram o que a pessoa pode fazer depois de entrar;
- o sistema ainda não possui segregação de funções para ações sensíveis, como convidar usuários, bloquear acessos, gerar tokens, desconectar integrações, contratar plano ou cancelar assinatura.

### Perfis efetivamente existentes

| Perfil | Finalidade | Visão do sistema |
|---|---|---|
| `EMPRESA` | Operação completa da empresa cliente | Todas as telas operacionais, administrativas, financeiras e de integração descritas neste documento |
| `PARTNER_SPECIALIST` | Apoio especializado de parceiro | Compartilha somente algumas APIs de avaliações, mídia e catálogo; não corresponde a um subperfil da empresa |
| `ADMIN` | Administração interna da plataforma Práxis | Área `/admin`; não pertence ao fluxo da empresa cliente |
| Pessoa participante | Realização pública da avaliação | Rotas públicas de candidato, sem acesso ao painel da empresa |

## 2. Menu que o usuário EMPRESA vê

### Fluxo principal

1. Dashboard
2. Avaliações
3. Jornadas
4. Participações
5. Resultados

### Mais opções — operação técnica

1. Central operacional
2. Compliance

### Mais opções — configurações

1. Perfil da empresa
2. Catálogo de competências
3. Minha equipe
4. Integrações
5. Plano
6. Minha conta

### Mais opções — ajuda

1. Primeiros passos
2. Central de manuais

Além dessas telas, ações internas abrem criação/edição de avaliação, nova participação individual, detalhe de resultado, comparação Talent Match e diagnóstico de integração.

## 3. Matriz de telas, informações, ações e duplicidades

| Grupo | Tela / rota | O que o perfil EMPRESA vê | Ações disponíveis | A informação aparece em outra tela? | Faz sentido manter assim? | Recomendação | Manual atual |
|---|---|---|---|---|---|---|---|
| Principal | **Dashboard** `/dashboard` | Próxima ação recomendada; avaliações ativas; jornadas; participações em andamento; resultados dos últimos 30 dias; cinco resultados recentes | Atualizar; abrir a tela responsável; acompanhar participação; abrir resultado | Sim. Os números vêm de Avaliações, Jornadas, Participações e Resultados | **Sim, como resumo** | Tornar a rota inicial após o login. Manter somente indicadores e atalhos; não adicionar operações completas | Específico |
| Principal | **Avaliações** `/avaliacoes` | Lista de avaliações, descrição, competências, situação, versão publicada, quantidade de aplicações e conclusões | Criar; buscar; filtrar; duplicar; arquivar; abrir; comparar participantes | Sim. Situação e quantidade de tentativas também aparecem no Dashboard, Compliance, Jornadas e Talent Match | **Sim, como tela dona do conteúdo** | Manter como única tela de gestão da avaliação. Outras telas devem apenas mostrar resumo e link para ela | Específico |
| Contextual | **Criação e edição de avaliação** `/nova/*` e `/simulations/new` | Objetivo, cargo, contexto, competências, personagens, cenário, alternativas, critérios, pesos, governança, validação e publicação | Criar; editar; salvar; validar; publicar; navegar entre etapas | Sim. Competências vêm do Catálogo; validações reaparecem em Compliance; resumo reaparece em Avaliações | **Sim, mas somente como fluxo contextual** | Não colocar cada etapa no menu. Compliance de uma avaliação deve ficar no validador/detalhe desta avaliação | Específico por grupo de rotas |
| Principal | **Jornadas** `/jornadas` | Lista de jornadas, composição por avaliações, sequências e tentativas relacionadas | Criar rascunho; editar; adicionar/remover/reordenar etapas; publicar; arquivar; criar tentativa; copiar convite | **Sim.** Criação de convite e acompanhamento de tentativas também existem em Participações | **Parcialmente** | Manter apenas composição, publicação e configuração da jornada. Depois de publicar, o botão deve encaminhar para Participações para convidar e acompanhar | Específico, mas descreve também convites e tentativas |
| Principal | **Participações** `/participacoes` | Convites individuais e de jornadas; aguardando início; em andamento; concluídos; expirados; problemas; participante; avaliação; validade | Buscar; filtrar; atualizar; criar participação individual; convidar por jornada; copiar link; reenviar; ampliar validade; abrir resultado | Sim. Tentativas aparecem em Jornadas, Dashboard, Resultados e telas antigas de monitoramento | **Sim, como tela dona da operação com pessoas** | Centralizar aqui todos os convites, tentativas, prazos e acompanhamento. Remover listas operacionais duplicadas de Jornadas | **Genérico. Não possui manual específico** |
| Contextual | **Nova participação individual** `/enviar-link` | Seleção de avaliação publicada; nome e e-mail; link gerado; forma de compartilhamento | Criar participação; copiar; abrir e-mail; compartilhar por WhatsApp; iniciar outra; voltar para Participações | Sim. A participação criada aparece em Participações | **Sim, como fluxo de criação** | Manter como ação iniciada em Participações, sem item próprio no menu | Específico como “Convites e links” |
| Principal | **Resultados** `/results` | Apenas participações concluídas; participante; avaliação; nota; origem; filtros por período e integração | Buscar; filtrar; atualizar; acompanhar participações; comparar; analisar resultado | Sim. Dashboard mostra os cinco recentes; Participações mostra que uma tentativa foi concluída; Talent Match reutiliza os mesmos resultados | **Sim, como tela dona dos resultados concluídos** | Manter a lista somente para resultados concluídos e evidências. Não trazer convites em andamento para esta tela | Específico |
| Contextual | **Detalhe do resultado** `/results/$attemptId` | Pontuação, competências, percurso, evidências, integridade e decisão humana | Revisar evidências; registrar decisão; voltar para lista ou participação | Sim. Talent Match usa parte dos indicadores; Dashboard exibe resumo | **Sim** | Manter como detalhe contextual, sempre acessado por Resultado ou Participação | Específico por prefixo `/results/` |
| Contextual | **Talent Match** `/talent-match` | Avaliação, participantes concluídos, modo cego, referência, gráfico radar e comparação de até cinco pessoas | Selecionar participantes; ativar modo cego; limpar seleção; abrir evidências e registrar decisão quando disponível | Sim. Usa as mesmas participações e competências de Resultados | **Sim, como ferramenta complementar** | Manter como ação “Comparar” dentro de Resultados e Avaliações; não promover a item principal de navegação | Específico |
| Operação | **Central operacional** `/monitoramento` | Integrações com atenção; entregas em retentativa; DLQ; alertas não lidos; resumo de integrações conectadas | Atualizar; marcar alerta como lido; reprocessar entrega; abrir diagnóstico; abrir Participações | Sim. Situação das integrações aparece em Integrações; alertas antigos redirecionam de `/notifications` | **Sim, somente para exceções** | Exibir apenas falhas e itens que exigem ação. Remover detalhes de integrações saudáveis, mantendo apenas contagem resumida | Compartilhado com Compliance e Notificações; precisa ser específico |
| Operação | **Compliance** `/compliance` | Lista novamente avaliações e versões; situação; “taxa de conclusão”; bloqueios; tentativas; auditoria; validação e privacidade | Buscar; filtrar; abrir detalhes | **Sim, intensamente.** Repete Avaliações, validador, tentativas e auditoria; o campo de bloqueios pode aparecer sem dados | **Não no formato atual** | Dividir: qualidade estrutural e bloqueios no detalhe/validador da avaliação; privacidade e governança da empresa em Configurações; auditoria contextual no registro correspondente. Retirar do menu principal | Manual genérico compartilhado com Central operacional |
| Configuração | **Perfil da empresa** `/configuracoes/perfil` | Nome fantasia, razão social, CNPJ, e-mail, telefone e site | Nenhuma. Somente consulta; alteração depende do administrador da plataforma | Parte desses dados pode aparecer no Dashboard, contrato, cobrança e integrações | **Não como tela isolada** | Transformar em aba “Empresa” dentro de Configurações. Permitir edição dos campos autorizados ou indicar claramente o canal de alteração | Compartilhado em “Administração da empresa” |
| Configuração | **Catálogo de competências** `/competencias` | Competências cadastradas e situação | Buscar; criar; editar; remover; paginar | Sim. As competências aparecem na criação da avaliação, Resultados, Talent Match e Compliance | **Sim, como cadastro mestre** | Manter como única tela de administração do catálogo. Nas demais telas, somente selecionar ou consultar | Específico |
| Configuração | **Minha equipe** `/team` | Usuários, e-mail, estado e último acesso | Convidar; reenviar convite; bloquear; desbloquear | Dados de acesso também aparecem em Minha conta e auditoria | **Sim, mas falta controle de permissão** | Criar subperfis e permissões. Remover o botão `/parceiros` enquanto a rota não existir. Ocultar o texto de parceiros para empresas sem esse recurso | Compartilhado em “Administração da empresa” |
| Configuração | **Integrações** `/integrations` | Gupy, Recrutei e API própria; situação, descrição, fluxo, credenciais e atividade | Configurar; gerar token; testar; sincronizar; reativar; desconectar; abrir diagnóstico e documentação | Sim. Falhas aparecem na Central operacional; configuração antiga redireciona para esta tela | **Sim, como tela dona da configuração** | Manter configurações e estados saudáveis aqui. Central operacional deve mostrar apenas exceções com link para o diagnóstico | Específico |
| Contextual | **Detalhe da integração** `/integrations/$provider` | Diagnóstico, dados específicos do provedor, endpoints, eventos e instruções | Testar; corrigir; copiar dados; executar ações específicas do provedor | Sim. O estado resumido aparece em Integrações e Monitoramento | **Sim** | Manter como detalhe contextual aberto pela Integração ou por uma falha operacional | Específico por prefixo `/integrations` |
| Configuração | **Planos, pagamentos e créditos** `/billing` | Plano, empresa, situação financeira, saldo total, reservado e livre; recarga; assinatura; uso; histórico | Comprar créditos; mudar plano; sincronizar; cancelar; solicitar Enterprise; recarregar | Saldo e bloqueios financeiros podem aparecer no Dashboard e no fluxo de convite | **Sim, como tela dona do financeiro** | Manter toda a operação financeira aqui; outras telas mostram somente alerta e link para corrigir | Compartilhado em “Administração da empresa” |
| Configuração | **Minha conta** `/configuracoes/conta` | Nome, e-mail e formulário de troca de senha | Alterar senha | Nome e e-mail aparecem no rodapé do menu e em Minha equipe | **Sim** | Manter separada da configuração empresarial, pois pertence ao usuário atual | Compartilhado em “Administração da empresa” |
| Ajuda | **Primeiros passos** `/comecar` | Progresso da implantação e seis etapas do primeiro processo | Abrir avaliação; jornada; resultado; integrações e monitoramento | Sim. Usa os mesmos indicadores do Dashboard | **Sim durante onboarding** | Exibir com destaque enquanto incompleto; depois mover para Ajuda. Corrigir “Convide e acompanhe” para abrir Participações, não Jornadas | Específico |
| Ajuda | **Central de manuais** `/manual` | Manuais de processos com finalidade, fluxo, campos, permissões, estados, bloqueios, exemplos e atalhos | Navegar por âncoras; abrir manual contextual em qualquer tela | O ícone global repete o trecho correspondente ao processo atual | **Sim** | Completar cobertura por tela e retirar processos inexistentes. Garantir que a rota esteja presente na árvore gerada do TanStack Router | Específico |
| Técnica | **Aliases e rotas antigas** `/notifications`, `/configuracoes/integracoes`, `/configuracoes/api`, `/assessment-journeys/*` | Não exibem conteúdo próprio; redirecionam para a tela consolidada | Redirecionar | Sim, por definição | **Sim temporariamente** | Manter apenas para compatibilidade com links antigos. Não exibir no menu e remover depois de medir que não há mais acessos externos | Herdam o manual do destino depois do redirecionamento |

## 4. Principais duplicidades encontradas

| Informação ou operação | Onde aparece hoje | Tela que deveria ser dona |
|---|---|---|
| Estado e uso de avaliações | Dashboard, Avaliações, Jornadas, Compliance, Talent Match | **Avaliações** |
| Validação, bloqueios e qualidade da avaliação | Fluxo `/nova/*`, Avaliações e Compliance | **Detalhe/validador da avaliação** |
| Criação de convite de jornada | Jornadas e Participações | **Participações**; Jornadas apenas fornece o processo selecionado |
| Acompanhamento de tentativas | Jornadas, Participações, Dashboard, Resultados e APIs de monitoramento | **Participações** |
| Resultados concluídos | Dashboard, Participações, Resultados e Talent Match | **Resultados**; Dashboard e Participações mostram somente resumo/link |
| Estado de integrações | Dashboard, Monitoramento, Integrações e detalhe do provedor | **Integrações** para configuração; **Monitoramento** somente para falhas |
| Dados da empresa | Perfil da empresa, Dashboard, cobrança e integrações | **Configurações da empresa** |
| Alertas e notificações | Dashboard, banner global, Monitoramento e alias `/notifications` | **Monitoramento**; Dashboard apenas alerta prioritário |
| Competências | Catálogo, criação, resultado, Talent Match e Compliance | **Catálogo** para gestão; demais telas somente utilizam os dados |

## 5. Problemas objetivos que exigem correção

| Prioridade | Problema | Impacto | Correção recomendada |
|---|---|---|---|
| P0 | Login redireciona para `/avaliacoes`, apesar de o Dashboard ter sido criado como central de prioridades | O usuário começa no meio do processo e perde a visão geral | Alterar a rota autenticada padrão para `/dashboard` |
| P0 | Todos os convidados recebem `EMPRESA` | Usuários de RH, gestores, financeiro e TI recebem o mesmo poder | Criar RBAC granular e aplicar a filtragem no menu e no backend |
| P0 | Jornadas cria e lista tentativas, enquanto Participações se declara ponto único de acompanhamento | Duplicidade operacional e dúvida sobre onde trabalhar | Deixar convites e tentativas em Participações |
| P0 | Minha equipe aponta para `/parceiros`, rota inexistente na árvore atual | Navegação para 404 | Remover o botão ou implementar o módulo antes de exibi-lo |
| P0 | Participações não possui manual específico | O ícone exibe orientação genérica na principal tela operacional | Criar definição própria com campos, estados, bloqueios e ações reais |
| P1 | Compliance mistura qualidade da avaliação, privacidade, auditoria e tentativas | Tela extensa, repetitiva e sem uma ação corretiva clara | Dividir por contexto e retirar o item amplo do menu |
| P1 | Perfil da empresa é uma tela somente leitura sem ação | Ocupa espaço de navegação sem permitir concluir uma tarefa | Consolidar em Configurações da empresa e permitir alterações autorizadas |
| P1 | Primeiros passos envia “Convide e acompanhe” para Jornadas | Reforça a duplicidade e contradiz Participações | Direcionar a etapa para `/participacoes` |
| P1 | Manual agrupa Monitoramento, Compliance e Notificações | Finalidades e ações diferentes recebem a mesma orientação | Criar manuais distintos |
| P2 | Existem aliases técnicos e rotas antigas na árvore | Aumentam o custo de manutenção e dificultam o inventário | Manter redirecionamentos sem menu e planejar remoção controlada |

## 6. Navegação recomendada

### Fluxo principal

1. **Dashboard** — o que precisa de atenção hoje.
2. **Avaliações** — criar e administrar o conteúdo.
3. **Jornadas** — montar e publicar processos compostos.
4. **Participações** — convidar e acompanhar pessoas.
5. **Resultados** — revisar evidências e registrar decisão humana.

### Operação

1. **Central operacional** — somente falhas, alertas, retentativas e DLQ.

### Configurações

1. **Empresa** — perfil e governança organizacional.
2. **Competências** — catálogo mestre.
3. **Equipe e permissões** — usuários e subperfis.
4. **Integrações** — configuração e diagnóstico.
5. **Plano e cobrança** — créditos, pagamentos e contrato.
6. **Minha conta** — dados e senha da pessoa conectada.

### Ajuda

1. **Primeiros passos** — onboarding.
2. **Central de manuais** — documentação completa.

### Telas contextuais, sem item próprio no menu

- criação e edição de avaliação;
- nova participação individual;
- detalhe do resultado;
- Talent Match;
- detalhe de integração;
- formulários e diálogos de ação.

## 7. Modelo de subperfis recomendado

| Subperfil | Consulta | Operação sugerida |
|---|---|---|
| Administrador da empresa | Todas as telas | Empresa, equipe, permissões, integrações, plano e todas as operações |
| Recrutador | Avaliações, jornadas, participações e resultados | Criar processos, convidar, acompanhar e registrar decisões |
| Gestor avaliador | Participações e resultados autorizados | Consultar evidências e registrar parecer, sem editar avaliações ou integrações |
| Especialista de conteúdo | Avaliações e competências | Criar e revisar conteúdo, sem acesso a candidatos, cobrança ou credenciais |
| Integração/TI | Integrações e Central operacional | Configurar, testar, reprocessar e diagnosticar integrações |
| Financeiro | Plano e cobrança | Comprar créditos, consultar pagamentos e administrar contrato |
| Somente leitura/Auditoria | Telas explicitamente autorizadas | Consultar, sem executar alterações |

A autorização deve ser validada no backend. Esconder itens apenas no menu não é controle de acesso.

## 8. Regra de organização proposta

Cada informação deve possuir uma única tela responsável:

- **Dashboard resume**;
- **Avaliações administra conteúdo**;
- **Jornadas administra composição**;
- **Participações administra pessoas, convites, prazos e andamento**;
- **Resultados administra conclusão, evidências e decisão**;
- **Integrações administra configuração**;
- **Central operacional administra exceções**;
- **Configurações administra cadastros, permissões e contrato**.

As outras telas podem exibir uma síntese, mas devem encaminhar para a tela responsável em vez de repetir a operação completa.
