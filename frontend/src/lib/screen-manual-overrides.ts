import type { ScreenManualDefinition } from "@/lib/screen-manuals";

export const SCREEN_MANUAL_OVERRIDES: ScreenManualDefinition[] = [
  {
    id: "jornadas-composicao",
    title: "Composição de jornadas",
    purpose: "Montar, ordenar, publicar e arquivar sequências de avaliações que serão aplicadas em conjunto.",
    flow: [
      "Crie ou selecione uma jornada.",
      "Adicione avaliações publicadas e organize a ordem de cada sequência.",
      "Publique a jornada e siga para Participações para gerar convites.",
    ],
    fields: [
      { name: "Nome", description: "Identificação interna do processo ou conjunto de avaliações." },
      { name: "Descrição", description: "Finalidade e contexto operacional da jornada." },
      { name: "Sequência", description: "Agrupamento que define quais etapas uma pessoa deverá executar e em qual ordem." },
      { name: "Obrigatória", description: "Indica se a avaliação precisa ser concluída para avançar na jornada." },
    ],
    permissions: ["Perfil EMPRESA com acesso à criação e gestão de jornadas."],
    states: ["Rascunho", "Publicada", "Arquivada"],
    blocks: [
      "Uma jornada sem avaliações não pode ser publicada.",
      "Somente avaliações publicadas podem ser adicionadas.",
      "Jornadas arquivadas não permitem alterações estruturais.",
    ],
    examples: ["Criar uma jornada de trainee com uma avaliação de atendimento seguida por liderança."],
    shortcuts: ["Organize todas as etapas antes de publicar.", "Use Participações para criar e acompanhar os convites."],
    matches: (pathname) => pathname === "/jornadas",
  },
  {
    id: "participacoes",
    title: "Central de participações",
    purpose: "Centralizar convites, validade dos links, andamento das aplicações e acesso aos resultados concluídos.",
    flow: [
      "Crie uma participação individual ou um convite por jornada.",
      "Acompanhe a situação e o progresso da pessoa participante.",
      "Reenvie, amplie a validade ou abra o resultado conforme o estado atual.",
    ],
    fields: [
      { name: "Participante", description: "Nome e e-mail vinculados à aplicação." },
      { name: "Avaliação", description: "Conteúdo aplicado e versão utilizada." },
      { name: "Situação", description: "Indica se a aplicação aguarda início, está em andamento, foi concluída ou exige atenção." },
      { name: "Progresso", description: "Quantidade de etapas executadas e percentual estimado de conclusão." },
      { name: "Validade", description: "Condição atual do link e quantidade de dias restantes." },
    ],
    permissions: ["Perfil EMPRESA com acesso a convites e resultados da própria empresa."],
    states: ["Aguardando início", "Em andamento", "Concluída", "Abandonada", "Expirada", "Sem atividade recente"],
    blocks: [
      "Avaliação ou jornada ainda não publicada.",
      "E-mail inválido.",
      "Créditos insuficientes para uma nova aplicação.",
      "Link expirado para ações de cópia ou reenvio sem reativação.",
    ],
    examples: ["Localizar uma pessoa, ampliar o link em sete dias e reenviar o acesso sem criar outra cobrança."],
    shortcuts: ["Use os filtros de situação antes da busca por nome.", "Resultados concluídos podem ser abertos diretamente pela coluna de ações."],
    matches: (pathname) => pathname === "/participacoes",
  },
  {
    id: "convite-jornada",
    title: "Convite por jornada",
    purpose: "Criar uma participação vinculada a uma jornada já publicada.",
    flow: [
      "Selecione a jornada publicada.",
      "Informe nome, e-mail e sequência aplicável.",
      "Crie a participação, copie o link e acompanhe pela Central de Participações.",
    ],
    fields: [
      { name: "Jornada", description: "Composição publicada que será disponibilizada à pessoa participante." },
      { name: "Nome", description: "Identificação da pessoa no processo." },
      { name: "E-mail", description: "Contato associado ao convite e à tentativa." },
      { name: "Sequência", description: "Conjunto de avaliações que deverá ser executado quando a jornada possui variações." },
    ],
    permissions: ["Perfil EMPRESA com acesso à jornada e à criação de participações."],
    states: ["Aguardando seleção", "Pronta para criar", "Criando", "Participação criada"],
    blocks: ["Nenhuma jornada publicada.", "Nome ou e-mail ausente.", "Sequência indisponível.", "Limite financeiro ou operacional atingido."],
    examples: ["Enviar a sequência principal da jornada de liderança para candidato@empresa.com."],
    shortcuts: ["Publique a jornada antes de abrir esta tela.", "Após copiar o link, volte para Participações para acompanhar o andamento."],
    matches: (pathname) => pathname === "/participacoes/jornada",
  },
  {
    id: "central-operacional",
    title: "Central operacional",
    purpose: "Tratar somente exceções técnicas que exigem intervenção, como falhas de integração, retentativas e entregas em DLQ.",
    flow: [
      "Revise os indicadores de atenção.",
      "Abra o alerta, integração ou entrega com falha.",
      "Execute a correção, reprocesse quando permitido e confirme a mudança de estado.",
    ],
    fields: [
      { name: "Integrações com atenção", description: "Conexões pendentes ou com erro que precisam de diagnóstico." },
      { name: "Retentativas", description: "Entregas aguardando nova execução automática." },
      { name: "DLQ", description: "Falhas que esgotaram as tentativas automáticas e precisam de ação manual." },
      { name: "Alertas não lidos", description: "Eventos operacionais ainda não reconhecidos pelo usuário." },
    ],
    permissions: ["Perfil EMPRESA com responsabilidade operacional ou de integração."],
    states: ["Sem pendências", "Pendente", "Em retentativa", "Falha", "Resolvido"],
    blocks: ["Entrega ainda em processamento.", "Integração sem credencial válida.", "Item já resolvido ou reprocessado.", "Falha externa ainda ativa."],
    examples: ["Corrigir a credencial da integração e reprocessar uma entrega de resultado que chegou à DLQ."],
    shortcuts: ["Comece pelos itens em DLQ.", "Configurações permanentes devem ser alteradas na tela Integrações."],
    matches: (pathname) => pathname === "/monitoramento",
  },
  {
    id: "conformidade-avaliacoes",
    title: "Conformidade das avaliações",
    purpose: "Consultar validações, cobertura, auditoria e requisitos de publicação das versões de avaliação.",
    flow: [
      "Localize a avaliação e a versão.",
      "Abra os detalhes de validação, cobertura e histórico.",
      "Corrija os bloqueios na edição da avaliação e execute uma nova validação.",
    ],
    fields: [
      { name: "Versão", description: "Recorte imutável ou em preparação do conteúdo avaliado." },
      { name: "Cobertura", description: "Percentual de caminhos e competências atendidos pelas regras estruturais." },
      { name: "Bloqueios", description: "Falhas que impedem revisão ou publicação segura." },
      { name: "Auditoria", description: "Histórico das mudanças e ações realizadas na versão." },
    ],
    permissions: ["Perfil EMPRESA com acesso à gestão ou auditoria das avaliações."],
    states: ["Rascunho", "Em revisão", "Com bloqueios", "Publicada", "Arquivada"],
    blocks: ["Caminho sem saída.", "Competência sem critério válido.", "Pontuação ou peso inconsistente.", "Versão não encontrada ou sem permissão."],
    examples: ["Abrir uma versão com cobertura insuficiente e identificar qual caminho precisa de uma alternativa adicional."],
    shortcuts: ["Corrija o conteúdo na tela da avaliação.", "Use os filtros de versão e situação antes de abrir os detalhes."],
    matches: (pathname) => pathname === "/compliance",
  },
];

export function resolveScreenManualOverride(pathname: string) {
  return SCREEN_MANUAL_OVERRIDES.find((manual) => manual.matches(pathname));
}
