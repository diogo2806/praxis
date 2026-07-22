import type { ScreenManualDefinition } from "@/lib/screen-manuals";

export const SCENARIO_OWNERSHIP_MANUALS: ScreenManualDefinition[] = [
  {
    id: "personagem-contexto-inicial",
    title: "Personagem e contexto inicial",
    purpose:
      "Definir o personagem fictício, seu estado emocional e o contexto apresentado na mensagem inicial da avaliação.",
    flow: [
      "Abra uma avaliação e versão em rascunho.",
      "Informe um nome ou identificador opcional para reconhecer o personagem.",
      "Preencha o estado emocional inicial e descreva o contexto, objetivo, histórico e limites da situação.",
      "Acompanhe o salvamento automático e corrija qualquer bloqueio indicado na tela.",
      "Confirme a configuração para seguir ao Validador e depois ao Editor de diálogo.",
      "Use o Editor de diálogo somente para as etapas posteriores, alternativas, mídia e pontuação.",
    ],
    fields: [
      {
        name: "Nome ou identificador",
        description:
          "Referência opcional para reconhecer o personagem sem utilizar dados pessoais reais.",
      },
      {
        name: "Estado emocional inicial",
        description:
          "Condição emocional obrigatória apresentada no início da situação, como frustrado, inseguro ou com pressa.",
      },
      {
        name: "Contexto do cliente",
        description:
          "Descrição obrigatória da situação inicial, objetivo da conversa, tentativas anteriores e limites relevantes.",
      },
      {
        name: "Mensagem inicial gerada",
        description:
          "Conteúdo persistido na primeira etapa da versão. Essa mensagem só pode ser alterada nesta tela.",
      },
      {
        name: "Status de salvamento",
        description:
          "Informa se o conteúdo está sendo salvo, já foi persistido ou continua protegido apenas como rascunho local.",
      },
    ],
    permissions: [
      "Usuário autenticado da empresa com permissão para criar ou editar avaliações.",
      "A versão deve estar em rascunho; versões publicadas exigem a criação de um novo rascunho.",
    ],
    states: [
      "Sem avaliação ou versão selecionada",
      "Carregando configuração e versão",
      "Rascunho editável",
      "Rascunho local recuperado",
      "Salvamento automático em andamento",
      "Conteúdo salvo no Práxis",
      "Versão publicada ou arquivada protegida",
      "Erro de carregamento ou gravação",
    ],
    blocks: [
      "Avaliação ou versão não informada.",
      "Estado emocional inicial vazio.",
      "Contexto do cliente vazio ou acima do limite permitido.",
      "Versão publicada ou arquivada.",
      "Configuração da empresa indisponível.",
      "Falha de conexão durante o salvamento.",
    ],
    examples: [
      "Personagem: gerente de loja frustrado porque o estoque prometido não chegou e precisa atender um cliente importante.",
      "Personagem sem nome, inseguro sobre como responder a uma reclamação e com prazo curto para decidir.",
    ],
    shortcuts: [
      "Use o indicador de salvamento no topo antes de sair da tela.",
      "Abra o Editor de diálogo para cadastrar as etapas posteriores sem alterar a mensagem inicial.",
      "Use o Validador para verificar se a primeira etapa possui alternativas e caminhos válidos.",
      "Use Tab e Shift+Tab para navegar entre os campos e ações.",
    ],
    matches: (pathname) => pathname === "/nova/personagem",
  },
  {
    id: "editor-dialogo",
    title: "Editor de diálogo",
    purpose:
      "Criar e manter etapas, ramificações, alternativas, tempo, mídia, criticidade e pontuação por competência, incluindo o resultado e o relatório dos encerramentos.",
    flow: [
      "Abra uma avaliação e versão em rascunho.",
      "Consulte a primeira etapa em modo somente leitura e use o atalho Personagem quando precisar alterar o contexto inicial.",
      "Crie ou selecione uma etapa posterior e informe sua mensagem.",
      "Cadastre de duas a quatro alternativas, configure pontuação, mídia e destino.",
      "No destino de uma alternativa, escolha Criar nova etapa ramificada para criar, vincular e abrir automaticamente a continuação daquele caminho.",
      "Preencha a mensagem da etapa recém-criada e continue ramificando quando necessário.",
      "Ao escolher Vai para FIM em uma alternativa ou em Quando o tempo acabar, confira as somas e percentuais calculados e preencha o relatório obrigatório.",
      "Abra o Mapa para revisar visualmente a hierarquia e as conexões e use o Validador antes da publicação.",
    ],
    fields: [
      {
        name: "Código visual da etapa",
        description:
          "Numeração hierárquica calculada pelo caminho, como 1, 1.1, 1.2 ou 1.3.1. O identificador técnico interno não é alterado.",
      },
      {
        name: "Mensagem inicial",
        description:
          "Conteúdo da primeira etapa exibido somente como referência. A alteração ocorre exclusivamente em Personagem.",
      },
      {
        name: "Mensagem da etapa posterior",
        description: "Texto apresentado na etapa selecionada após o contexto inicial.",
      },
      {
        name: "Tempo",
        description: "Prazo permitido para responder à etapa, conforme configuração da empresa.",
      },
      {
        name: "Quando o tempo acabar",
        description:
          "Define a etapa posterior ou o encerramento da avaliação. Quando o destino é Vai para FIM, exibe o resultado do caminho, atribui 0 pontos à etapa sem resposta e exige o relatório do encerramento.",
      },
      {
        name: "Mídia da etapa",
        description: "Imagem ou áudio opcional associado à situação apresentada.",
      },
      {
        name: "Alternativa",
        description: "Decisão disponível para a pessoa participante.",
      },
      {
        name: "Pontuação por competência",
        description: "Valor inteiro de 0 a 100 atribuído a cada competência para a alternativa.",
      },
      {
        name: "Resposta crítica — exige revisão humana",
        description:
          "Marque quando a resposta representar erro grave ou comportamento de risco. A pontuação continua sendo calculada, mas o resultado exige análise humana e nunca reprova automaticamente.",
      },
      {
        name: "Destino",
        description:
          "Permite escolher uma etapa existente, finalizar a avaliação ou criar uma nova etapa ramificada já vinculada à alternativa.",
      },
      {
        name: "Criar nova etapa ramificada",
        description:
          "Cria uma etapa vazia a partir da alternativa, estabelece a conexão, posiciona o novo card à direita no Mapa e abre o campo de mensagem para edição.",
      },
      {
        name: "Resumo do encerramento",
        description:
          "Quando uma alternativa ou o tempo esgotado levam para FIM, apresenta em modo somente leitura a soma acumulada, o percentual por competência e o resultado ponderado do caminho.",
      },
      {
        name: "Texto do relatório",
        description:
          "Descrição obrigatória do significado daquele encerramento para a equipe responsável; é registrada na trilha do resultado.",
      },
    ],
    permissions: [
      "Usuário autenticado da empresa com permissão para criar ou editar avaliações.",
      "A versão deve estar em rascunho; versões publicadas exigem a criação de novo rascunho.",
    ],
    states: [
      "Carregando avaliação",
      "Rascunho editável",
      "Mensagem inicial protegida",
      "Criando nova etapa ramificada",
      "Nova ramificação criada e aberta para edição",
      "Encerramento por resposta com resultado calculado",
      "Encerramento por tempo esgotado com 0 pontos na etapa sem resposta",
      "Encerramento pendente de relatório",
      "Versão publicada protegida",
      "Alteração salva",
      "Erro de validação ou gravação",
    ],
    blocks: [
      "Avaliação ou versão não informada.",
      "Tentativa de alterar ou remover a mensagem inicial fora de Personagem.",
      "Versão publicada ou arquivada.",
      "Criação de ramificação a partir de etapa final ou alternativa inexistente.",
      "Etapa posterior ou ramificada sem mensagem.",
      "Menos de duas ou mais de quatro alternativas.",
      "Alternativa sem pontuação por competência.",
      "Alternativa com destino Vai para FIM sem texto de relatório.",
      "Tempo esgotado com destino Vai para FIM sem texto de relatório.",
      "Configuração da empresa indisponível.",
    ],
    examples: [
      "Na etapa 1, criar duas continuações pelas alternativas; elas aparecem como etapas 1.1 e 1.2.",
      "Na etapa 1.3, criar uma nova continuação; ela aparece como etapa 1.3.1.",
      "Selecionar Vai para FIM, conferir Comunicação: soma 135 e 71%, e registrar o relatório daquele encerramento.",
      "Anexar um áudio à etapa e marcar como Resposta crítica uma decisão que promete prazo sem confirmação técnica.",
      "Selecionar Vai para FIM em Quando o tempo acabar e registrar o relatório aplicável à ausência de resposta.",
    ],
    shortcuts: [
      "Escolha + Criar nova etapa ramificada no destino para criar e abrir a continuação sem sair da alternativa.",
      "Use Editar personagem quando a etapa inicial estiver selecionada.",
      "Use o Mapa do fluxo para conferir a estrutura 1, 1.1, 1.2 e níveis seguintes.",
      "Use o Validador para abrir diretamente a etapa com problema.",
      "Use Tab e Shift+Tab para navegar entre campos e ações.",
    ],
    matches: (pathname) => pathname === "/nova/dialogo",
  },
  {
    id: "mapa-fluxo",
    title: "Mapa do fluxo",
    purpose:
      "Organizar visualmente a posição das etapas, compreender a hierarquia das ramificações e definir as conexões entre alternativas e destinos.",
    flow: [
      "Abra uma avaliação e versão com o contexto inicial configurado em Personagem e as alternativas cadastradas no Editor de diálogo.",
      "Leia os códigos hierárquicos para identificar o caminho: 1 é a raiz, 1.1 e 1.2 são ramificações e 1.1.1 é um nível seguinte.",
      "Arraste cada etapa para a posição desejada no canvas.",
      "Selecione uma etapa para revisar as alternativas em modo somente leitura.",
      "Defina um destino existente, finalize a avaliação ou escolha Criar nova etapa ramificada.",
      "Quando criar uma ramificação, abra seu conteúdo no Editor de diálogo e preencha a mensagem e as alternativas.",
      "Acesse o Validador para confirmar que não existem caminhos órfãos ou sem conclusão.",
    ],
    fields: [
      {
        name: "Código visual hierárquico",
        description:
          "Identifica a posição lógica no caminho, como 1, 1.1, 1.2 e 1.2.1, sem substituir o identificador técnico da etapa.",
      },
      {
        name: "Posição da etapa",
        description: "Coordenadas visuais persistidas ao finalizar o arraste no canvas.",
      },
      {
        name: "Etapa selecionada",
        description: "Nó usado para consultar suas alternativas e conexões.",
      },
      {
        name: "Mensagem",
        description:
          "Texto exibido somente como referência; a etapa inicial pertence a Personagem e as demais ao Editor de diálogo.",
      },
      {
        name: "Alternativa",
        description: "Texto exibido somente como referência; sua edição ocorre no Editor de diálogo.",
      },
      {
        name: "Destino",
        description:
          "Próxima etapa, encerramento ou comando para criar uma nova etapa ramificada já conectada.",
      },
      {
        name: "Indicadores de validação",
        description: "Resumo de bloqueios e avisos calculados pela mesma API do Validador.",
      },
    ],
    permissions: [
      "Usuário autenticado da empresa com permissão para editar avaliações.",
      "A versão deve estar em rascunho para mover etapas, alterar conexões ou criar ramificações.",
    ],
    states: [
      "Carregando mapa",
      "Mapa editável",
      "Versão protegida",
      "Posição salva",
      "Conexão salva",
      "Criando e posicionando ramificação",
      "Ramificação criada e selecionada",
      "Erro de gravação",
    ],
    blocks: [
      "Avaliação ou versão não informada.",
      "Versão publicada ou arquivada.",
      "Nenhuma etapa cadastrada.",
      "Destino inválido ou anterior que poderia criar ciclo.",
      "Tentativa de criar ramificação em uma etapa final.",
      "Falha ao carregar ou salvar a versão.",
    ],
    examples: [
      "Visualizar a etapa 1 com três saídas identificadas como 1.1, 1.2 e 1.3.",
      "Criar uma continuação na etapa 1.3 e ver o novo card 1.3.1 posicionado à direita.",
      "Direcionar uma alternativa para uma etapa existente e outra para o encerramento.",
    ],
    shortcuts: [
      "Escolha + Criar nova etapa ramificada no campo Destino para gerar a continuação automaticamente.",
      "Clique em Editar conteúdo desta etapa para preencher a mensagem da ramificação selecionada.",
      "Abra Personagem quando precisar alterar a mensagem inicial.",
      "Use o botão Validar fluxo para consultar os bloqueios completos.",
      "Arraste pelo cabeçalho da etapa para reposicioná-la.",
    ],
    matches: (pathname) => pathname === "/nova/mapa",
  },
];

export function resolveScenarioOwnershipManual(pathname: string) {
  return SCENARIO_OWNERSHIP_MANUALS.find((manual) => manual.matches(pathname));
}
