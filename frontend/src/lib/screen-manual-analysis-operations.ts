import type { ScreenManualDefinition } from "@/lib/screen-manuals";

export const ANALYSIS_OPERATION_MANUALS: ScreenManualDefinition[] = [
  {
    id: "resultado-executivo-entrevista",
    title: "Relatório executivo e entrevista estruturada",
    purpose:
      "Transformar o percurso concluído em uma leitura operacional rastreável, preparar perguntas de entrevista vinculadas a regras ou evidências e preservar a decisão final como responsabilidade humana.",
    flow: [
      "Confirme a pessoa participante, a avaliação, a versão e a data de conclusão no cabeçalho.",
      "Revise a visão executiva, distinguindo dado observado, interpretação por regra e interpretação cadastrada pela empresa.",
      "Abra as evidências críticas e os pontos indicados para aprofundamento.",
      "Revise o roteiro determinístico: edite, remova ou acrescente perguntas conforme a entrevista planejada.",
      "Registre o roteiro e as anotações; cada salvamento cria um evento imutável na trilha da tentativa.",
      "Conduza a entrevista, registre a decisão humana final e informe a justificativa.",
      "Use Imprimir / salvar PDF para gerar o documento com avaliação, versão, datas, evidências e auditoria.",
    ],
    fields: [
      {
        name: "Visão executiva",
        description:
          "Resume competências, quantidade de evidências, referências do percurso, alternativas críticas e limitações sem recomendar contratação ou reprovação.",
      },
      {
        name: "Dado observado",
        description:
          "Resposta efetivamente registrada na tentativa e referência técnica do nó e da alternativa que originaram a evidência.",
      },
      {
        name: "Interpretação configurada pela empresa",
        description:
          "Texto cadastrado pelo autor na alternativa ou no cenário da versão respondida; quando ausente, a tela informa explicitamente a lacuna.",
      },
      {
        name: "Roteiro de entrevista",
        description:
          "Perguntas iniciais produzidas por regras fixas e vinculadas a uma competência ou evidência; podem ser editadas, removidas ou complementadas.",
      },
      {
        name: "Anotações do entrevistador",
        description:
          "Registro factual da entrevista, separado da interpretação da avaliação e da decisão humana final.",
      },
      {
        name: "Decisão humana final",
        description:
          "Avançar, reprovar, contratar ou colocar em espera, sempre registrada por uma pessoa responsável e com justificativa própria.",
      },
      {
        name: "Trilha de auditoria",
        description:
          "Linha cronológica dos eventos da tentativa, incluindo conclusão, registros de roteiro e decisões humanas.",
      },
    ],
    permissions: [
      "Perfil EMPRESA autenticado e vinculado à empresa dona da tentativa.",
      "Acesso à Central de Resultados para consultar evidências e competências.",
      "Permissão operacional para registrar roteiro, anotações e decisão humana.",
    ],
    states: [
      "Carregando resultado e relatório executivo",
      "Resultado disponível com roteiro inicial por regras",
      "Roteiro editado ainda não registrado",
      "Roteiro registrado na auditoria",
      "Decisão humana ainda não registrada",
      "Decisão humana registrada",
      "Relatório pronto para impressão ou PDF",
      "Falha parcial no relatório executivo com resultado básico ainda disponível",
    ],
    blocks: [
      "Tentativa inexistente ou pertencente a outra empresa.",
      "Versão histórica da avaliação indisponível para reconstruir os textos do percurso.",
      "Pergunta vazia, identificador duplicado ou limite de 30 perguntas atingido.",
      "Anotações acima de 4000 caracteres.",
      "Tentativa sem evidências recuperáveis para determinada competência.",
      "Falha de conexão ao carregar ou registrar o roteiro.",
      "Decisão final sem opção selecionada.",
    ],
    examples: [
      "Revisar a evidência Situação 3:opcao-b, aprofundar Comunicação e registrar duas perguntas adicionais do entrevistador.",
      "Remover uma pergunta que não se aplica à vaga, salvar o roteiro revisado e depois gerar o PDF para o gestor requisitante.",
      "Registrar a decisão de avançar somente após a entrevista, mantendo a justificativa separada da pontuação da avaliação.",
    ],
    shortcuts: [
      "Use o botão Imprimir / salvar PDF para abrir o diálogo nativo do navegador e escolher Salvar como PDF.",
      "Use Tab e Shift+Tab para percorrer perguntas, anotações e ações sem o mouse.",
      "Use o ícone Manual e depois Ver processo completo para abrir /manual#resultado-executivo-entrevista.",
      "Retorne à lista preservando os filtros pelo botão Voltar para resultados.",
    ],
    matches: (pathname) => /^\/results\/[^/]+$/.test(pathname),
  },
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
