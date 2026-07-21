import type { ScreenManualDefinition } from "@/lib/screen-manuals";

export const ANALYSIS_OPERATION_MANUALS: ScreenManualDefinition[] = [
  {
    id: "talent-match-contextual",
    title: "Talent Match contextual",
    purpose:
      "Comparar até cinco participações concluídas de uma avaliação e versão específicas com a referência configurada, sem manter uma segunda lista global de avaliações ou candidatos.",
    flow: [
      "Abra a avaliação desejada em Avaliações ou filtre o processo em Resultados.",
      "Acesse Talent Match preservando o identificador da avaliação e o número da versão.",
      "Revise o cabeçalho para confirmar o contexto carregado.",
      "Ative o modo cego quando a comparação não deve exibir dados identificáveis.",
      "Selecione até cinco participações concluídas da mesma avaliação e versão.",
      "Compare o radar com a referência, abra as evidências e registre a decisão humana quando aplicável.",
    ],
    fields: [
      {
        name: "Avaliação e versão",
        description:
          "Contexto obrigatório recebido de Avaliações ou Resultados; não pode ser escolhido em uma lista global nesta tela.",
      },
      {
        name: "Busca de participante",
        description:
          "Filtra somente as participações concluídas já limitadas pelo backend à avaliação e versão abertas.",
      },
      {
        name: "Modo cego",
        description:
          "Solicita ao backend a ocultação de dados identificáveis para reduzir viés durante a comparação.",
      },
      {
        name: "Participações selecionadas",
        description: "Conjunto de até cinco resultados exibidos simultaneamente no gráfico comparativo.",
      },
      {
        name: "Referência configurada",
        description: "Pontuação-alvo de cada competência definida previamente na versão da avaliação.",
      },
      {
        name: "Decisão humana",
        description:
          "Registro explícito de avanço, rejeição, contratação ou espera, com justificativa opcional e sem decisão automática pelo score.",
      },
    ],
    permissions: [
      "Perfil EMPRESA com acesso aos resultados da avaliação.",
      "A avaliação e a versão devem pertencer à empresa autenticada.",
      "O usuário precisa ter autorização para consultar evidências e registrar decisão humana.",
    ],
    states: [
      "Contexto ausente com retorno para Avaliações ou Resultados",
      "Carregando avaliação e participações concluídas",
      "Versão não publicada",
      "Sem participações concluídas",
      "Comparação aguardando seleção",
      "Comparação disponível",
      "Modo cego ativo",
      "Erro de carregamento ou registro",
    ],
    blocks: [
      "Identificador da avaliação ou número da versão ausente.",
      "Avaliação ou versão inexistente, arquivada ou sem acesso.",
      "Versão sem referência de competências configurada.",
      "Nenhuma participação concluída para o contexto informado.",
      "Limite de cinco participações selecionadas atingido.",
      "Falha ao carregar resultados, evidências ou registrar a decisão humana.",
    ],
    examples: [
      "Abrir a versão 2 da avaliação de atendimento e comparar três participações concluídas no modo cego.",
      "Baixar a evidência de uma participação e registrar avanço após a revisão humana do percurso.",
    ],
    shortcuts: [
      "Use o ícone de comparação na tela Avaliações para abrir o contexto correto.",
      "Use Limpar para retirar todas as participações do radar sem sair da versão.",
      "Abra o resultado individual para revisar competências e percurso antes de registrar a decisão.",
      "Quando faltar contexto, retorne a Avaliações em vez de procurar uma lista global dentro do Talent Match.",
    ],
    matches: (pathname) => pathname === "/talent-match",
  },
  {
    id: "central-operacional-fila",
    title: "Central operacional",
    purpose:
      "Concentrar somente exceções técnicas acionáveis: integrações com erro ou pendentes, entregas em retentativa, itens em DLQ e alertas ainda não lidos.",
    flow: [
      "Revise os quatro contadores da fila operacional.",
      "Selecione um contador para filtrar a tela pela categoria desejada.",
      "Abra o diagnóstico da integração quando a conexão estiver pendente ou com erro.",
      "Acompanhe a próxima tentativa automática sem executar ação manual enquanto a entrega estiver em retentativa.",
      "Nos itens em DLQ, corrija a causa e use Tentar novamente para reprocessar.",
      "Marque alertas como lidos depois de concluir a análise.",
    ],
    fields: [
      {
        name: "Integrações com atenção",
        description:
          "Exibe somente provedores em estado PENDENTE ou ERRO; conexões saudáveis ficam na tela Integrações.",
      },
      {
        name: "Em retentativa",
        description:
          "Entregas pendentes ou em nova tentativa automática, com quantidade de tentativas e próximo horário programado.",
      },
      {
        name: "Em DLQ",
        description:
          "Entregas que esgotaram a política automática e exigem diagnóstico e reprocessamento manual.",
      },
      {
        name: "Alertas não lidos",
        description: "Ocorrências operacionais que ainda não foram reconhecidas pelo usuário.",
      },
      {
        name: "Filtro ativo",
        description:
          "Categoria selecionada pelos contadores. Mostrar toda a fila restaura a visão completa das exceções.",
      },
      {
        name: "Tentar novamente",
        description:
          "Reprocessa somente uma entrega em DLQ; entregas com retentativa automática não oferecem esta ação.",
      },
    ],
    permissions: [
      "Perfil EMPRESA com acesso operacional às integrações, notificações e entregas da própria empresa.",
      "O reprocessamento pode exigir permissão específica de operação ou integração.",
    ],
    states: [
      "Carregando fila operacional",
      "Fila sem intervenções pendentes",
      "Integração pendente",
      "Integração com erro",
      "Entrega aguardando retentativa",
      "Entrega em DLQ",
      "Reprocessando entrega",
      "Alerta não lido",
      "Erro de carregamento ou ação",
    ],
    blocks: [
      "Falha ao consultar integrações, entregas ou notificações.",
      "Entrega ainda coberta pela política automática de retentativa.",
      "Causa da falha externa ainda não corrigida.",
      "Entrega já reprocessada ou fora da DLQ.",
      "Usuário sem permissão para reprocessar ou acessar o diagnóstico.",
      "Ação de leitura ou reprocessamento já em andamento.",
    ],
    examples: [
      "Filtrar por DLQ, corrigir a credencial do provedor e reprocessar uma entrega que esgotou as tentativas.",
      "Filtrar retentativas para confirmar o próximo horário sem disparar uma duplicidade manual.",
      "Marcar um alerta como lido após abrir o resultado relacionado e concluir a análise.",
    ],
    shortcuts: [
      "Clique novamente no contador selecionado para voltar à fila completa.",
      "Use Configurar integrações para alterar credenciais e parâmetros permanentes.",
      "Comece pelos itens em DLQ, depois revise integrações com erro e alertas.",
      "Integrações conectadas e entregas enviadas não aparecem nesta tela.",
    ],
    matches: (pathname) => pathname === "/monitoramento",
  },
];

export function resolveAnalysisOperationManual(pathname: string) {
  return ANALYSIS_OPERATION_MANUALS.find((manual) => manual.matches(pathname));
}
