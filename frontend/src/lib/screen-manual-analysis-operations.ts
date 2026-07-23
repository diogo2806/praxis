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
    title: "Central operacional e revisão técnica",
    purpose:
      "Concentrar exceções acionáveis e permitir a revisão humana de evidências técnicas sem classificar intenção, aplicar penalidade ou alterar o resultado da avaliação.",
    flow: [
      "Revise os contadores de integrações, retentativas, DLQ e alertas não lidos.",
      "Trate as falhas operacionais pelo diagnóstico ou reprocessamento apropriado.",
      "Na seção Revisão técnica de integridade, selecione uma participação encaminhada por regra determinística.",
      "Abra as evidências; esse acesso fica registrado na trilha de auditoria.",
      "Leia os alertas explicáveis, as sessões e a linha do tempo sem inferir intenção da pessoa candidata.",
      "Selecione um dos quatro pareceres neutros, informe a justificativa obrigatória e salve.",
      "Marque o compartilhamento somente quando o relatório empresarial puder exibir o status neutro e a data.",
    ],
    fields: [
      {
        name: "Integrações com atenção",
        description:
          "Exibe somente provedores em estado PENDENTE ou ERRO; conexões saudáveis ficam na tela Integrações.",
      },
      {
        name: "Em retentativa e DLQ",
        description:
          "Mostra entregas ainda cobertas pela política automática ou que já exigem reprocessamento manual.",
      },
      {
        name: "Fila de revisão técnica",
        description:
          "Lista participações que atingiram regras determinísticas de interrupção, visibilidade, retomada ou mudança de entrada.",
      },
      {
        name: "Alertas explicáveis",
        description:
          "Apresentam regra, quantidade de ocorrências e explicação neutra; não representam classificação automática de fraude.",
      },
      {
        name: "Sessões e linha do tempo",
        description:
          "Evidências técnicas restritas, sem endereço de rede bruto e sem vínculo com score ou competência.",
      },
      {
        name: "Parecer humano",
        description:
          "Aceita Sem impacto, Problema técnico confirmado, Reaplicação recomendada ou Análise de privacidade/compliance.",
      },
      {
        name: "Justificativa obrigatória",
        description:
          "Registra os fatos verificados e fica preservada na trilha de mudanças do parecer.",
      },
      {
        name: "Compartilhar no relatório",
        description:
          "Quando marcado, libera somente o status neutro e a data; evidências, justificativa e responsável permanecem restritos.",
      },
      {
        name: "Trilha de auditoria",
        description:
          "Registra criação do encaminhamento, cada acesso às evidências, alterações de parecer e descarte pela retenção.",
      },
    ],
    permissions: [
      "A fila e as evidências exigem TEAM_MANAGER, PARTNER_MANAGER ou OPERATIONS_MANAGER da própria empresa.",
      "O resultado compartilhado segue a permissão normal da Central de Resultados.",
      "O acesso é isolado por empresa e cada abertura de evidência é auditada.",
    ],
    states: [
      "Fila operacional sem pendências",
      "Revisão técnica pendente",
      "Evidências carregando",
      "Evidências disponíveis",
      "Parecer humano decidido",
      "Status neutro compartilhado",
      "Evidências descartadas por retenção",
      "Erro de carregamento ou salvamento",
    ],
    blocks: [
      "Usuário sem perfil autorizado para consultar evidências técnicas.",
      "Participação sem alerta determinístico suficiente para entrar na fila.",
      "Parecer sem justificativa textual.",
      "Evidências já descartadas pela política de retenção.",
      "Falha ao consultar a fila, registrar o acesso ou salvar o parecer.",
      "Entrega ainda coberta pela política automática de retentativa.",
    ],
    examples: [
      "Confirmar que uma sessão expirou por queda de conexão e recomendar reaplicação com justificativa.",
      "Registrar Sem impacto após verificar alternâncias de visibilidade compatíveis com o fluxo operacional.",
      "Encaminhar para privacidade/compliance sem alterar nota, competência ou decisão de contratação.",
      "Compartilhar no relatório apenas o status Problema técnico confirmado e a data do parecer.",
    ],
    shortcuts: [
      "Comece pelos itens em DLQ e depois trate as revisões técnicas pendentes.",
      "Abra o resultado em outra aba para comparar contexto sem misturar evidência técnica e pontuação.",
      "Não recarregue o detalhe sem necessidade, pois cada novo acesso fica auditado.",
      "Use o ícone Ajuda desta tela para abrir este processo completo na Central de manuais.",
    ],
    matches: (pathname) => pathname === "/monitoramento",
  },
];

export function resolveAnalysisOperationManual(pathname: string) {
  return ANALYSIS_OPERATION_MANUALS.find((manual) => manual.matches(pathname));
}
