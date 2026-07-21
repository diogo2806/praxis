import type { ScreenManualDefinition } from "@/lib/screen-manuals";

export const SCENARIO_OWNERSHIP_MANUALS: ScreenManualDefinition[] = [
  {
    id: "editor-dialogo",
    title: "Editor de diálogo",
    purpose:
      "Criar e manter o conteúdo operacional do cenário: etapas, falas, alternativas, tempo, mídia, criticidade e pontuação por competência.",
    flow: [
      "Abra uma avaliação e versão em rascunho.",
      "Selecione ou crie uma etapa e informe a mensagem apresentada à pessoa participante.",
      "Cadastre de duas a quatro alternativas, configure pontuação, criticidade, mídia e destino.",
      "Salve as alterações e use o Mapa apenas para organizar posições e revisar conexões.",
      "Abra o Validador para verificar bloqueios e avisos antes da publicação.",
    ],
    fields: [
      {
        name: "Mensagem do cliente",
        description: "Texto principal apresentado na etapa selecionada.",
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
        description: "Valor de 0 a 100 atribuído a cada competência para a alternativa.",
      },
      {
        name: "Crítica",
        description: "Indica alternativa que representa risco ou decisão incompatível com o cenário.",
      },
      {
        name: "Destino",
        description: "Próxima etapa ou encerramento do fluxo; também pode ser revisado visualmente no Mapa.",
      },
    ],
    permissions: [
      "Usuário autenticado da empresa com permissão para criar ou editar avaliações.",
      "A versão deve estar em rascunho; versões publicadas exigem a criação de novo rascunho.",
    ],
    states: [
      "Carregando avaliação",
      "Rascunho editável",
      "Versão publicada protegida",
      "Alteração salva",
      "Erro de validação ou gravação",
    ],
    blocks: [
      "Avaliação ou versão não informada.",
      "Versão publicada ou arquivada.",
      "Etapa sem mensagem.",
      "Menos de duas ou mais de quatro alternativas.",
      "Alternativa sem pontuação por competência.",
      "Configuração da empresa indisponível.",
    ],
    examples: [
      "Criar uma etapa de atendimento ao cliente com três respostas e pontuações diferentes para Empatia e Comunicação.",
      "Anexar um áudio à etapa e marcar uma alternativa como crítica.",
    ],
    shortcuts: [
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
      "Organizar visualmente a posição das etapas e definir as conexões entre alternativas e destinos, sem editar o conteúdo do cenário.",
    flow: [
      "Abra uma avaliação e versão com etapas já cadastradas no Editor de diálogo.",
      "Arraste cada etapa para a posição desejada no canvas.",
      "Selecione uma etapa para revisar as alternativas em modo somente leitura.",
      "Defina o destino de cada alternativa como uma etapa posterior ou finalização.",
      "Abra o Editor de diálogo quando precisar alterar texto, mídia, criticidade ou pontuação.",
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
      "Nenhuma etapa cadastrada no Editor de diálogo.",
      "Destino inválido ou anterior que poderia criar ciclo.",
      "Falha ao carregar ou salvar a versão.",
    ],
    examples: [
      "Mover as etapas para formar uma leitura da esquerda para a direita.",
      "Direcionar uma alternativa da etapa 1 para a etapa 3 e outra para o encerramento.",
    ],
    shortcuts: [
      "Clique em Editar conteúdo no diálogo para alterar a etapa selecionada.",
      "Use o botão Validar fluxo para consultar os bloqueios completos.",
      "Arraste pelo cabeçalho da etapa para reposicioná-la.",
    ],
    matches: (pathname) => pathname === "/nova/mapa",
  },
];

export function resolveScenarioOwnershipManual(pathname: string) {
  return SCENARIO_OWNERSHIP_MANUALS.find((manual) => manual.matches(pathname));
}
