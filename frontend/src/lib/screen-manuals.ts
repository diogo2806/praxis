export type ScreenManualField = {
  name: string;
  description: string;
};

export type ScreenManualDefinition = {
  id: string;
  title: string;
  purpose: string;
  flow: string[];
  fields: ScreenManualField[];
  permissions: string[];
  states: string[];
  blocks: string[];
  examples: string[];
  shortcuts: string[];
  matches: (pathname: string) => boolean;
};

export const SCREEN_MANUALS: ScreenManualDefinition[] = [
  {
    id: "manual",
    title: "Central de manuais",
    purpose: "Consultar, em um único lugar, as orientações operacionais das telas do Práxis.",
    flow: [
      "Localize o processo desejado.",
      "Leia a finalidade, os estados e os motivos de bloqueio.",
      "Retorne à tela operacional pelo menu principal.",
    ],
    fields: [
      { name: "Processo", description: "Agrupa as telas que fazem parte da mesma atividade operacional." },
      { name: "Estados", description: "Mostra as situações em que um registro pode se encontrar." },
      { name: "Bloqueios", description: "Explica por que uma ação pode ficar indisponível." },
    ],
    permissions: ["Acesso autenticado ao Práxis para consultar processos internos."],
    states: ["Disponível", "Conteúdo contextual por tela"],
    blocks: ["Uma funcionalidade pode não aparecer quando não está habilitada para a empresa ou para o perfil."],
    examples: ["Consultar o fluxo completo de criação de uma avaliação antes de iniciar o cadastro."],
    shortcuts: ["Use a busca do navegador para localizar palavras nesta página.", "Use Esc para fechar o manual lateral."],
    matches: (pathname) => pathname === "/manual",
  },
  {
    id: "dashboard",
    title: "Painel principal",
    purpose: "Apresentar o resumo da operação e destacar pendências que exigem atenção.",
    flow: [
      "Revise os indicadores e alertas do período.",
      "Abra a avaliação, jornada ou resultado relacionado.",
      "Conclua a pendência e retorne ao painel para confirmar a atualização.",
    ],
    fields: [
      { name: "Indicadores", description: "Totais consolidados de avaliações, jornadas, participantes e resultados." },
      { name: "Pendências", description: "Itens que ainda precisam de configuração, publicação ou análise." },
      { name: "Atividade recente", description: "Eventos mais novos registrados na operação da empresa." },
    ],
    permissions: ["Usuário autenticado vinculado à empresa."],
    states: ["Sem dados", "Operação em andamento", "Pendências encontradas", "Dados atualizados"],
    blocks: ["Empresa sem configuração inicial.", "Dados ainda não processados.", "Recurso indisponível no plano atual."],
    examples: ["Abrir uma jornada com convites pendentes diretamente pelo card do painel."],
    shortcuts: ["Use Tab para navegar entre cards e ações.", "Acesse Comece aqui para seguir o fluxo inicial completo."],
    matches: (pathname) => pathname === "/dashboard",
  },
  {
    id: "primeiros-passos",
    title: "Comece aqui",
    purpose: "Orientar a configuração inicial e a execução do primeiro processo completo no Práxis.",
    flow: [
      "Crie uma avaliação e defina o objetivo.",
      "Monte o cenário, revise e publique.",
      "Crie uma jornada, convide participantes e acompanhe os resultados.",
    ],
    fields: [
      { name: "Etapa", description: "Passo necessário para concluir o primeiro processo." },
      { name: "Situação", description: "Indica se a etapa está pendente, em andamento ou concluída." },
      { name: "Ação", description: "Abre a tela correta para executar o passo selecionado." },
    ],
    permissions: ["Usuário autenticado com acesso às funções operacionais da empresa."],
    states: ["Não iniciado", "Em andamento", "Concluído"],
    blocks: ["Etapa anterior obrigatória ainda não concluída.", "Cadastro mínimo da empresa incompleto."],
    examples: ["Criar uma avaliação, publicá-la, adicioná-la a uma jornada e enviar o primeiro convite."],
    shortcuts: ["Siga as etapas na ordem exibida.", "Use o botão da etapa para continuar exatamente do ponto pendente."],
    matches: (pathname) => pathname === "/comecar",
  },
  {
    id: "criacao-avaliacao",
    title: "Criação de avaliação",
    purpose: "Construir uma avaliação situacional estruturada, rastreável e pronta para publicação.",
    flow: [
      "Defina objetivo, cargo e contexto da avaliação.",
      "Cadastre competências, personagens, etapas e alternativas.",
      "Configure critérios e pontuações, valide a estrutura e publique.",
    ],
    fields: [
      { name: "Objetivo", description: "Resultado que a avaliação pretende observar no contexto da vaga." },
      { name: "Competências", description: "Dimensões avaliadas e respectivas regras de pontuação." },
      { name: "Cenário e alternativas", description: "Situação apresentada e decisões disponíveis para a pessoa participante." },
      { name: "Governança", description: "Regras de revisão, rastreabilidade e publicação da avaliação." },
    ],
    permissions: ["Acesso de criação ou edição de avaliações na empresa."],
    states: ["Rascunho", "Incompleta", "Pronta para revisão", "Publicada", "Arquivada"],
    blocks: ["Campo obrigatório não preenchido.", "Caminho sem saída ou alternativa sem destino.", "Competência sem critério ou peso válido.", "Avaliação publicada com edição estrutural restrita."],
    examples: ["Criar um cenário de atendimento em que cada alternativa pontua comunicação, empatia e resolução de conflitos."],
    shortcuts: ["Use Tab e Shift+Tab para percorrer os campos.", "Salve antes de mudar de etapa quando houver edição pendente."],
    matches: (pathname) => pathname.startsWith("/nova") || pathname.startsWith("/simulations/new"),
  },
  {
    id: "avaliacoes",
    title: "Avaliações situacionais",
    purpose: "Listar, revisar, publicar e administrar as avaliações da empresa.",
    flow: [
      "Localize a avaliação por nome ou situação.",
      "Abra o registro para revisar conteúdo e validações.",
      "Publique, duplique, arquive ou continue a edição conforme a necessidade.",
    ],
    fields: [
      { name: "Nome", description: "Identificação interna e operacional da avaliação." },
      { name: "Cargo ou contexto", description: "Uso pretendido da avaliação." },
      { name: "Situação", description: "Condição atual do ciclo de edição e publicação." },
      { name: "Última atualização", description: "Momento da modificação mais recente." },
    ],
    permissions: ["Acesso de consulta para visualizar.", "Acesso de gestão para criar, editar, publicar ou arquivar."],
    states: ["Rascunho", "Com pendências", "Publicada", "Arquivada"],
    blocks: ["Validação estrutural com erros.", "Avaliação vinculada a processo em andamento.", "Permissão insuficiente para a ação."],
    examples: ["Filtrar avaliações em rascunho e abrir uma delas para concluir os caminhos pendentes."],
    shortcuts: ["Use os filtros antes de percorrer a tabela.", "Abra o menu de ações do registro para operações secundárias."],
    matches: (pathname) => pathname === "/avaliacoes" || pathname.startsWith("/avaliacoes/"),
  },
  {
    id: "competencias",
    title: "Biblioteca de competências",
    purpose: "Padronizar as competências utilizadas na pontuação e na leitura dos resultados.",
    flow: [
      "Consulte as competências já cadastradas.",
      "Crie ou edite nome, descrição e critérios de observação.",
      "Utilize a competência nas avaliações aplicáveis.",
    ],
    fields: [
      { name: "Nome", description: "Título curto da competência avaliada." },
      { name: "Descrição", description: "Comportamento ou capacidade que deve ser observado." },
      { name: "Critérios", description: "Evidências usadas para interpretar a pontuação." },
    ],
    permissions: ["Acesso de gestão da biblioteca de competências."],
    states: ["Ativa", "Em uso", "Inativa"],
    blocks: ["Nome duplicado.", "Competência em uso por uma avaliação publicada.", "Dados obrigatórios incompletos."],
    examples: ["Cadastrar Empatia com critérios de escuta, acolhimento e adequação da resposta."],
    shortcuts: ["Pesquise antes de cadastrar para evitar duplicidade.", "Use Tab para navegar pelo formulário."],
    matches: (pathname) => pathname === "/competencias" || pathname.startsWith("/competencias/"),
  },
  {
    id: "jornadas",
    title: "Jornadas de avaliação",
    purpose: "Organizar avaliações em uma sequência aplicável a uma vaga ou processo seletivo.",
    flow: [
      "Crie a jornada e informe sua finalidade.",
      "Adicione as avaliações na ordem de execução.",
      "Revise, publique, gere convites e acompanhe as tentativas.",
    ],
    fields: [
      { name: "Nome da jornada", description: "Identificação do processo de avaliação." },
      { name: "Avaliações", description: "Etapas que serão realizadas pela pessoa participante." },
      { name: "Ordem", description: "Sequência obrigatória ou recomendada das avaliações." },
      { name: "Prazo", description: "Período disponível para acesso e conclusão." },
    ],
    permissions: ["Acesso de gestão de jornadas e convites."],
    states: ["Rascunho", "Pronta", "Publicada", "Em andamento", "Encerrada"],
    blocks: ["Jornada sem avaliação.", "Avaliação não publicada.", "Prazo inválido ou expirado.", "Alteração estrutural após o início das tentativas."],
    examples: ["Montar uma jornada com avaliação de atendimento seguida de avaliação de liderança."],
    shortcuts: ["Ordene as etapas antes de publicar.", "Use a visão da jornada para acessar participantes e resultados relacionados."],
    matches: (pathname) =>
      pathname === "/jornadas" ||
      pathname.startsWith("/jornada/") ||
      pathname.startsWith("/assessment-journeys"),
  },
  {
    id: "convites",
    title: "Convites e links de participação",
    purpose: "Disponibilizar uma avaliação ou jornada para a pessoa participante com rastreabilidade.",
    flow: [
      "Selecione a avaliação ou jornada publicada.",
      "Informe os dados da pessoa participante e o prazo.",
      "Envie ou copie o link e acompanhe o recebimento e a execução.",
    ],
    fields: [
      { name: "Participante", description: "Nome usado para identificação no processo." },
      { name: "E-mail", description: "Destino do convite e vínculo da tentativa." },
      { name: "Avaliação ou jornada", description: "Conteúdo que será disponibilizado." },
      { name: "Validade", description: "Data limite para utilização do link." },
    ],
    permissions: ["Acesso de envio de convites e consulta de participantes."],
    states: ["Criado", "Enviado", "Entregue", "Acessado", "Em andamento", "Concluído", "Expirado"],
    blocks: ["Avaliação ou jornada não publicada.", "E-mail inválido.", "Créditos ou limite do plano indisponíveis.", "Link expirado, cancelado ou já concluído."],
    examples: ["Enviar uma jornada para candidato@empresa.com com validade até o fim da semana."],
    shortcuts: ["Use o botão de copiar para compartilhar o link por outro canal.", "Confirme a validade antes de reenviar um convite."],
    matches: (pathname) =>
      pathname === "/enviar-link" ||
      pathname.startsWith("/candidate-links") ||
      pathname.startsWith("/convite/"),
  },
  {
    id: "resultados",
    title: "Resultados e evidências",
    purpose: "Analisar pontuações, decisões percorridas e evidências antes da decisão humana.",
    flow: [
      "Localize a tentativa ou participante.",
      "Revise o resumo, as competências e o percurso realizado.",
      "Registre a análise e utilize as evidências no processo decisório.",
    ],
    fields: [
      { name: "Pontuação", description: "Resultado calculado conforme regras definidas antes da aplicação." },
      { name: "Competências", description: "Distribuição dos resultados por dimensão avaliada." },
      { name: "Percurso", description: "Sequência de decisões tomada durante o cenário." },
      { name: "Evidências", description: "Informações que sustentam a interpretação humana." },
    ],
    permissions: ["Acesso autorizado aos resultados da empresa e do processo correspondente."],
    states: ["Não iniciado", "Em andamento", "Concluído", "Expirado", "Invalidado"],
    blocks: ["Tentativa ainda não concluída.", "Resultado em processamento.", "Usuário sem acesso ao processo ou à empresa."],
    examples: ["Comparar a pontuação de Empatia com as alternativas escolhidas no percurso da pessoa participante."],
    shortcuts: ["Use os filtros para reduzir a lista.", "Abra o detalhe para consultar o percurso completo e as evidências."],
    matches: (pathname) => pathname === "/results" || pathname.startsWith("/results/"),
  },
  {
    id: "talent-match",
    title: "Talent Match",
    purpose: "Cruzar resultados com critérios definidos para apoiar a priorização de perfis.",
    flow: [
      "Selecione o processo ou conjunto de resultados.",
      "Defina os critérios e pesos de comparação.",
      "Revise o ranqueamento e abra as evidências antes de decidir.",
    ],
    fields: [
      { name: "Critério", description: "Competência ou indicador utilizado na comparação." },
      { name: "Peso", description: "Importância relativa do critério no cálculo." },
      { name: "Aderência", description: "Resultado da comparação conforme os parâmetros escolhidos." },
    ],
    permissions: ["Acesso aos resultados e ao recurso Talent Match da empresa."],
    states: ["Sem critérios", "Calculado", "Atualizado", "Resultado indisponível"],
    blocks: ["Nenhum resultado elegível.", "Critério sem peso válido.", "Recurso não habilitado no plano."],
    examples: ["Priorizar Comunicação e Resolução de conflitos para uma vaga de atendimento."],
    shortcuts: ["Revise a soma e a coerência dos pesos.", "Abra o resultado individual antes de concluir a análise."],
    matches: (pathname) => pathname === "/talent-match",
  },
  {
    id: "operacao",
    title: "Operação, conformidade e notificações",
    purpose: "Acompanhar eventos operacionais, alertas, entregas e registros de conformidade.",
    flow: [
      "Selecione o período e os filtros relevantes.",
      "Identifique alertas, falhas ou eventos pendentes.",
      "Abra o detalhe, execute a ação corretiva e confirme o novo estado.",
    ],
    fields: [
      { name: "Evento", description: "Acontecimento registrado pela operação." },
      { name: "Origem", description: "Processo, integração ou usuário relacionado." },
      { name: "Data e hora", description: "Momento em que o evento ocorreu." },
      { name: "Situação", description: "Condição atual do item monitorado." },
    ],
    permissions: ["Acesso operacional ou de conformidade conforme o perfil da empresa."],
    states: ["Informativo", "Pendente", "Em tratamento", "Resolvido", "Falha"],
    blocks: ["Filtro sem resultados.", "Evento protegido por permissão.", "Ação indisponível após resolução ou expiração."],
    examples: ["Abrir uma falha de entrega de convite e reenviar após corrigir o endereço."],
    shortcuts: ["Comece pelos itens pendentes ou com falha.", "Use os filtros de período e situação antes de exportar ou revisar."],
    matches: (pathname) =>
      pathname === "/monitoramento" || pathname === "/compliance" || pathname === "/notifications",
  },
  {
    id: "integracoes",
    title: "Integrações",
    purpose: "Configurar e acompanhar conexões entre o Práxis e sistemas externos.",
    flow: [
      "Selecione o provedor ou tipo de integração.",
      "Informe credenciais, URLs e parâmetros obrigatórios.",
      "Teste a conexão, ative e acompanhe eventos e falhas.",
    ],
    fields: [
      { name: "Provedor", description: "Sistema externo conectado ao Práxis." },
      { name: "Credenciais", description: "Dados de autenticação armazenados e utilizados de forma segura." },
      { name: "Endpoints", description: "URLs usadas para envio, consulta, callback ou webhook." },
      { name: "Situação", description: "Condição atual da configuração e da comunicação." },
    ],
    permissions: ["Acesso de administração de integrações da empresa."],
    states: ["Não configurada", "Em configuração", "Em teste", "Ativa", "Com falha", "Desativada"],
    blocks: ["Credencial ausente ou inválida.", "Endpoint inacessível.", "Teste obrigatório não concluído.", "Permissão ou recurso não habilitado."],
    examples: ["Configurar a Gupy, validar a autenticação e confirmar o retorno de um resultado de teste."],
    shortcuts: ["Teste a conexão antes de ativar.", "Copie identificadores e URLs sem espaços extras."],
    matches: (pathname) => pathname.startsWith("/integrations") || pathname.startsWith("/configuracoes/api") || pathname.startsWith("/docs/integracao-api-propria"),
  },
  {
    id: "administracao-empresa",
    title: "Administração da empresa",
    purpose: "Manter dados da organização, usuários, conta e plano de uso.",
    flow: [
      "Abra a seção correspondente a perfil, equipe, conta ou plano.",
      "Revise os dados atuais e faça as alterações necessárias.",
      "Salve e confirme a atualização ou o novo estado do acesso.",
    ],
    fields: [
      { name: "Empresa", description: "Dados institucionais e identificação do ambiente." },
      { name: "Usuário e perfil", description: "Pessoa com acesso e conjunto de permissões associado." },
      { name: "Plano", description: "Limites, recursos contratados e situação da assinatura." },
      { name: "Conta", description: "Dados pessoais, segurança e preferências do usuário atual." },
    ],
    permissions: ["Consulta para o próprio usuário.", "Permissão administrativa para alterar empresa, equipe ou plano."],
    states: ["Ativo", "Convidado", "Bloqueado", "Suspenso", "Cancelado"],
    blocks: ["Campo obrigatório inválido.", "Último administrador não pode ser removido.", "Convite expirado.", "Alteração limitada pelo contrato ou plano."],
    examples: ["Convidar uma pessoa recrutadora e atribuir somente as permissões necessárias."],
    shortcuts: ["Revise o perfil antes de enviar o convite.", "Use a busca da equipe para localizar acessos existentes."],
    matches: (pathname) =>
      pathname.startsWith("/configuracoes") || pathname === "/team" || pathname === "/billing",
  },
  {
    id: "parceiros",
    title: "Parceiros e especialistas",
    purpose: "Administrar relacionamentos, especialistas e apoios associados à operação do Práxis.",
    flow: [
      "Consulte os parceiros cadastrados.",
      "Registre ou atualize dados, especialidades e situação.",
      "Vincule o parceiro ao processo aplicável e acompanhe sua atuação.",
    ],
    fields: [
      { name: "Parceiro", description: "Pessoa ou organização responsável pelo apoio." },
      { name: "Especialidade", description: "Área de atuação ou competência oferecida." },
      { name: "Contato", description: "Canal usado para comunicação operacional." },
      { name: "Situação", description: "Condição atual do relacionamento." },
    ],
    permissions: ["Acesso de gestão de parceiros da empresa."],
    states: ["Convidado", "Ativo", "Inativo", "Bloqueado"],
    blocks: ["Contato inválido.", "Cadastro duplicado.", "Parceiro vinculado a atividade em andamento."],
    examples: ["Cadastrar uma especialista em avaliação comportamental e vinculá-la à revisão de cenários."],
    shortcuts: ["Pesquise por nome ou especialidade.", "Valide o contato antes de enviar um convite."],
    matches: (pathname) => pathname.startsWith("/parceiros"),
  },
  {
    id: "administracao-plataforma",
    title: "Administração da plataforma",
    purpose: "Gerenciar empresas clientes, acessos e estados operacionais da plataforma.",
    flow: [
      "Consulte o painel administrativo e localize a empresa.",
      "Abra o cadastro para revisar plano, situação e usuários.",
      "Aplique a alteração autorizada e confirme o impacto operacional.",
    ],
    fields: [
      { name: "Empresa", description: "Organização cliente administrada na plataforma." },
      { name: "Plano", description: "Modelo comercial e recursos associados." },
      { name: "Situação", description: "Estado de operação da empresa ou do acesso." },
      { name: "Usuários", description: "Acessos vinculados à empresa selecionada." },
    ],
    permissions: ["Perfil administrativo da plataforma."],
    states: ["Ativo", "Em teste", "Suspenso", "Cancelado", "Convidado", "Bloqueado"],
    blocks: ["Ação incompatível com o estado atual.", "Empresa com dependências ativas.", "Usuário sem privilégio administrativo."],
    examples: ["Abrir uma empresa em teste, revisar seus acessos e alterar a situação após validação."],
    shortcuts: ["Use os filtros antes de abrir o detalhe.", "Confirme empresa e impacto antes de ações de suspensão ou cancelamento."],
    matches: (pathname) => pathname.startsWith("/admin"),
  },
  {
    id: "experiencia-participante",
    title: "Experiência da pessoa participante",
    purpose: "Orientar o acesso, a execução e a conclusão segura de uma avaliação ou jornada.",
    flow: [
      "Confirme a identificação e as instruções apresentadas.",
      "Inicie a etapa disponível e responda às situações na ordem exibida.",
      "Conclua o processo e aguarde a confirmação de envio.",
    ],
    fields: [
      { name: "Identificação", description: "Dados usados para vincular a participação ao convite." },
      { name: "Progresso", description: "Etapa atual e quantidade restante do processo." },
      { name: "Alternativas", description: "Decisões disponíveis em cada situação apresentada." },
    ],
    permissions: ["Link válido ou sessão autorizada da pessoa participante."],
    states: ["Disponível", "Em andamento", "Pausado", "Concluído", "Expirado"],
    blocks: ["Link inválido, expirado ou já utilizado.", "Etapa anterior não concluída.", "Conexão interrompida antes da confirmação de envio."],
    examples: ["Acessar o convite, concluir todas as situações e visualizar a confirmação final."],
    shortcuts: ["Use Tab para navegar e Enter ou Espaço para ativar controles focados.", "Não feche a página durante o envio final."],
    matches: (pathname) => pathname.startsWith("/candidato"),
  },
  {
    id: "acesso",
    title: "Acesso e recuperação de conta",
    purpose: "Permitir entrada segura, convite de acesso e recuperação de credenciais.",
    flow: [
      "Informe os dados solicitados ou abra o convite recebido.",
      "Conclua a autenticação ou definição de nova senha.",
      "Acesse o ambiente autorizado para o seu perfil.",
    ],
    fields: [
      { name: "E-mail", description: "Identificador da conta e destino das comunicações de acesso." },
      { name: "Senha", description: "Credencial pessoal protegida pelas regras de segurança." },
      { name: "Token", description: "Código temporário usado em convites e recuperação de senha." },
    ],
    permissions: ["Conta existente, convite válido ou solicitação de recuperação autorizada."],
    states: ["Aguardando autenticação", "Convite válido", "Token expirado", "Conta bloqueada", "Acesso liberado"],
    blocks: ["Credenciais inválidas.", "Token expirado ou já utilizado.", "Conta bloqueada, suspensa ou sem vínculo ativo."],
    examples: ["Solicitar recuperação, abrir o link recebido e definir uma nova senha válida."],
    shortcuts: ["Use Enter para enviar o formulário quando o foco estiver em um campo.", "Confira o domínio e o endereço de e-mail antes de solicitar novo link."],
    matches: (pathname) =>
      pathname === "/login" ||
      pathname === "/recuperar-senha" ||
      pathname.startsWith("/reset-password/") ||
      pathname.startsWith("/convite/"),
  },
];

const DEFAULT_SCREEN_MANUAL: ScreenManualDefinition = {
  id: "visao-geral",
  title: "Manual da tela",
  purpose: "Orientar a utilização segura e consistente desta tela do Práxis.",
  flow: [
    "Revise o objetivo e os dados apresentados.",
    "Preencha ou selecione somente as informações necessárias.",
    "Confirme a ação e verifique a mensagem ou situação resultante.",
  ],
  fields: [
    { name: "Campos obrigatórios", description: "São identificados na interface e precisam ser preenchidos antes da confirmação." },
    { name: "Situação", description: "Representa o estado atual do registro ou processo." },
    { name: "Ações", description: "Operações disponíveis conforme o perfil e o estado atual." },
  ],
  permissions: ["Acesso válido à tela e à empresa ou processo correspondente."],
  states: ["Carregando", "Sem dados", "Disponível", "Com pendência", "Concluído"],
  blocks: ["Dados obrigatórios ausentes.", "Permissão insuficiente.", "Estado atual incompatível com a ação.", "Falha temporária de comunicação."],
  examples: ["Preencher os dados obrigatórios, salvar e confirmar a mensagem de sucesso."],
  shortcuts: ["Use Tab e Shift+Tab para navegar.", "Use Esc para fechar caixas de diálogo e o manual lateral."],
  matches: () => true,
};

export function resolveScreenManual(pathname: string): ScreenManualDefinition {
  return SCREEN_MANUALS.find((manual) => manual.matches(pathname)) ?? DEFAULT_SCREEN_MANUAL;
}
