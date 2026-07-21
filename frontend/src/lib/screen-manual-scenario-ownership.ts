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
      "Criar e manter as etapas posteriores, alternativas, tempo, mídia, criticidade e pontuação por competência, sem alterar o contexto inicial definido em Personagem.",
    flow: [
      "Abra uma avaliação e versão em rascunho.",
      "Consulte a primeira etapa em modo somente leitura e use o atalho Personagem quando precisar alterar o contexto inicial.",
      "Crie ou selecione uma etapa posterior e informe sua mensagem.",
      "Cadastre de duas a quatro alternativas, configure pontuação, criticidade, mídia e destino.",
      "Salve as alterações e use o Mapa apenas para organizar posições e revisar conexões.",
      "Abra o Validador para verificar bloqueios e avisos antes da publicação.",
    ],
    fields: [
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
        name: "Crítica",
        description:
          "Indica alternativa que representa risco ou decisão incompatível com o cenário.",
      },
      {
        name: "Destino",
        description:
          "Próxima etapa ou encerramento do fluxo; também pode ser revisado visualmente no Mapa.",
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
      "Versão publicada protegida",
      "Alteração salva",
      "Erro de validação ou gravação",
    ],
    blocks: [
      "Avaliação ou versão não informada.",
      "Tentativa de alterar ou remover a mensagem inicial fora de Personagem.",
      "Versão publicada ou arquivada.",
      "Etapa posterior sem mensagem.",
      "Menos de duas ou mais de quatro alternativas.",
      "Alternativa sem pontuação por competência.",
      "Configuração da empresa indisponível.",
    ],
    examples: [
      "Consultar a mensagem inicial e usar Editar personagem para corrigir o contexto.",
      "Criar uma etapa posterior de atendimento com três respostas e pontuações diferentes para Empatia e Comunicação.",
      "Anexar um áudio à etapa e marcar uma alternativa como crítica.",
    ],
    shortcuts: [
      "Use Editar personagem quando a etapa inicial estiver selecionada.",
      "Use o Validador para abrir diretamente a etapa com problema.",
      "Use Mapa do fluxo para organizar as etapas sem alterar o conteúdo.",
      "Use Tab e Shift+Tab para navegar entre campos e ações.",
    ],
    matches: (pathname) => pathname === "/nova/dialogo",
  },
  {
    id: "mapa-fluxo",
    title: "Mapa do fluxo",
    purpose:
      "Organizar visualmente a posição das etapas e definir as conexões entre alternativas e destinos, sem editar o conteúdo ou o contexto inicial do cenário.",
    flow: [
      "Abra uma avaliação e versão com o contexto inicial configurado em Personagem e as demais etapas cadastradas no Editor de diálogo.",
      "Arraste cada etapa para a posição desejada no canvas.",
      "Selecione uma etapa para revisar as alternativas em modo somente leitura.",
      "Defina o destino de cada alternativa como uma etapa posterior ou finalização.",
      "Abra Personagem para alterar o contexto inicial ou o Editor de diálogo para alterar as etapas posteriores.",
      "Acesse o Validador para confirmar que não existem caminhos órfãos ou sem conclusão.",
    ],
    fields: [
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
        description: "Próxima etapa permitida ou encerramento da avaliação.",
      },
      {
        name: "Indicadores de validação",
        description: "Resumo de bloqueios e avisos calculados pela mesma API do Validador.",
      },
    ],
    permissions: [
      "Usuário autenticado da empresa com permissão para editar avaliações.",
      "A versão deve estar em rascunho para mover etapas ou alterar conexões.",
    ],
    states: [
      "Carregando mapa",
      "Mapa editável",
      "Versão protegida",
      "Posição salva",
      "Conexão salva",
      "Erro de gravação",
    ],
    blocks: [
      "Avaliação ou versão não informada.",
      "Versão publicada ou arquivada.",
      "Nenhuma etapa cadastrada.",
      "Destino inválido ou anterior que poderia criar ciclo.",
      "Falha ao carregar ou salvar a versão.",
    ],
    examples: [
      "Mover as etapas para formar uma leitura da esquerda para a direita.",
      "Direcionar uma alternativa da etapa 1 para a etapa 3 e outra para o encerramento.",
    ],
    shortcuts: [
      "Abra Personagem quando precisar alterar a mensagem inicial.",
      "Clique em Editar conteúdo no diálogo para alterar as etapas posteriores.",
      "Use o botão Validar fluxo para consultar os bloqueios completos.",
      "Arraste pelo cabeçalho da etapa para reposicioná-la.",
    ],
    matches: (pathname) => pathname === "/nova/mapa",
  },
];

export function resolveScenarioOwnershipManual(pathname: string) {
  return SCENARIO_OWNERSHIP_MANUALS.find((manual) => manual.matches(pathname));
}
