import type { ScreenManualDefinition } from "@/lib/screen-manuals";

export const ANALYSIS_OPERATION_MANUALS: ScreenManualDefinition[] = [
  {
    id: "talent-match-contextual",
    title: "Talent Match contextual",
    purpose:
      "Comparar até cinco participações concluídas da mesma avaliação e versão, distinguindo perfil-alvo, referência normativa elegível e nota de corte aprovada, sem transformar score em decisão automática.",
    flow: [
      "Abra a avaliação desejada em Avaliações ou filtre o processo em Resultados.",
      "Acesse Talent Match preservando o identificador da avaliação e o número da versão.",
      "Revise o perfil-alvo, que representa apenas a expectativa configurada pela organização.",
      "Confirme se existe referência normativa elegível para a mesma versão, população, período e caminho avaliativo.",
      "Revise separadamente a nota de corte, sua justificativa, evidência, população e validade.",
      "Ative o modo cego quando a comparação não deve exibir dados identificáveis.",
      "Selecione até cinco participações concluídas e confira o snapshot histórico aplicado a cada uma.",
      "Abra as evidências e registre a decisão humana quando aplicável.",
    ],
    fields: [
      {
        name: "Avaliação e versão",
        description:
          "Contexto obrigatório recebido de Avaliações ou Resultados. Todas as participações e referências devem pertencer à mesma empresa e versão.",
      },
      {
        name: "Perfil-alvo configurado",
        description:
          "Pontuação desejada por competência definida pela organização. Não é média de candidatos, referência normativa nem nota de corte.",
      },
      {
        name: "Referência normativa",
        description:
          "Grupo comparável por cargo, senioridade, vaga, população, período e versão. Média, desvio e percentil só aparecem quando a amostra mínima e a comparabilidade foram validadas.",
      },
      {
        name: "Nota de corte",
        description:
          "Limiar decisório independente, versionado e aprovado, com população, justificativa, evidência e prazo de validade próprios.",
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
        description:
          "Conjunto de até cinco resultados exibidos simultaneamente, cada um com o snapshot das referências vigentes quando a tentativa foi concluída.",
      },
      {
        name: "Decisão humana",
        description:
          "Registro explícito de avanço, rejeição, contratação ou espera, com justificativa opcional e sem decisão automática pelo score ou pela nota de corte.",
      },
    ],
    permissions: [
      "Perfis TEAM_MANAGER ou RESULTS_ANALYST podem consultar resultados e configurar referências da própria empresa.",
      "A avaliação, a versão, as tentativas e os grupos normativos devem pertencer à empresa autenticada.",
      "A aprovação de nota de corte e ativação de grupo normativo geram trilha de auditoria.",
      "O usuário precisa ter autorização para consultar evidências e registrar decisão humana.",
    ],
    states: [
      "Contexto ausente com retorno para Avaliações ou Resultados",
      "Carregando avaliação, referências e participações concluídas",
      "Perfil-alvo disponível",
      "Grupo normativo elegível e ativo",
      "Grupo normativo inelegível ou ausente",
      "Nota de corte aprovada e vigente",
      "Nota de corte ausente, futura, vencida ou revogada",
      "Comparação aguardando seleção",
      "Comparação disponível com snapshot histórico",
      "Modo cego ativo",
      "Erro de carregamento, configuração ou registro",
    ],
    blocks: [
      "Identificador da avaliação ou número da versão ausente.",
      "Avaliação, versão ou tentativa inexistente ou sem acesso.",
      "Tentativas de versões diferentes ou ainda não concluídas.",
      "Grupo normativo com menos de 30 casos, caminho não comparável ou população incompatível.",
      "Média e percentil ocultos quando a referência normativa não é elegível.",
      "Recomendação binária indisponível quando não existe nota de corte aprovada e vigente.",
      "Limite de cinco participações selecionadas atingido.",
      "Usuário sem permissão para configurar referências ou registrar decisão.",
    ],
    examples: [
      "Comparar três pessoas da versão 2 com o perfil-alvo, sem chamar essa expectativa de benchmark.",
      "Ativar um grupo normativo de atendentes seniores da mesma vaga, com 45 casos concluídos no período informado.",
      "Aprovar nota de corte 70 com justificativa, evidência e validade até o fim do ciclo seletivo.",
      "Abrir uma tentativa antiga e confirmar que ela preserva a referência vigente na data da conclusão.",
    ],
    shortcuts: [
      "Use o ícone de comparação na tela Avaliações para abrir o contexto correto.",
      "Use Limpar para retirar todas as participações do radar sem sair da versão.",
      "Ative o modo cego antes de selecionar participantes quando quiser reduzir viés de identificação.",
      "Abra o resultado individual para revisar competências e percurso antes da decisão humana.",
      "Não configure nota de corte a partir da média do perfil-alvo; registre metodologia e evidência próprias.",
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
