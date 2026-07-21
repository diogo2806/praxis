import type { ScreenManualDefinition } from "@/lib/screen-manuals";

export const ACCESS_ONBOARDING_MANUALS: ScreenManualDefinition[] = [
  {
    id: "equipe-acessos",
    title: "Minha equipe",
    purpose:
      "Administrar os vínculos de usuários da empresa, definindo perfil, permissões e situação de acesso em uma única tela.",
    flow: [
      "Consulte os usuários vinculados e confira perfil, permissões, situação e último acesso.",
      "Clique em Convidar usuário e informe nome, e-mail e perfil operacional.",
      "Depois do convite, altere o perfil quando a responsabilidade da pessoa mudar.",
      "Reenvie convites ainda pendentes ou bloqueie e desbloqueie acessos quando necessário.",
      "Confirme a nova situação na tabela e consulte a auditoria quando precisar rastrear a alteração.",
    ],
    fields: [
      { name: "Nome e e-mail", description: "Identificam a pessoa e o login usado para receber o convite e acessar o Práxis." },
      { name: "Perfil", description: "Agrupa as permissões em Administrador, Autor de avaliações, Analista de resultados ou Operador." },
      { name: "Permissões", description: "Resumo das atividades autorizadas pelo perfil atual." },
      { name: "Acesso", description: "Mostra se o usuário está ativo, convidado ou bloqueado e informa o último acesso registrado." },
      { name: "Ações", description: "Permite alterar perfil, reenviar convite, bloquear ou desbloquear, conforme o estado atual." },
    ],
    permissions: [
      "Qualquer usuário autenticado da empresa pode consultar a equipe.",
      "Somente o perfil Administrador pode convidar usuários, alterar perfis e modificar a situação de acesso.",
      "O próprio administrador não pode reduzir o próprio perfil nem bloquear o próprio acesso.",
    ],
    states: [
      "Carregando equipe",
      "Sem usuários adicionais",
      "Convidado aguardando aceite",
      "Ativo",
      "Bloqueado",
      "Alterando perfil",
      "Erro de operação",
    ],
    blocks: [
      "Usuário sem perfil Administrador para ações de gestão.",
      "E-mail inválido ou já cadastrado na mesma empresa.",
      "Tentativa de alterar o próprio perfil ou bloquear o próprio usuário.",
      "Convite já aceito quando a ação escolhida é reenviar.",
      "Falha temporária ao carregar ou salvar a alteração.",
    ],
    examples: [
      "Convidar uma recrutadora como Analista de resultados para consultar evidências sem administrar a equipe.",
      "Alterar um Operador para Administrador depois de transferir a responsabilidade pela conta.",
    ],
    shortcuts: [
      "Use o seletor de perfil no convite para conceder somente o acesso necessário desde o início.",
      "Consulte a coluna Acesso antes de reenviar ou bloquear.",
      "Especialistas parceiros são gerenciados no módulo Parceiros e especialistas, quando habilitado.",
    ],
    matches: (pathname) => pathname === "/team",
  },
  {
    id: "parceiros-condicional",
    title: "Parceiros e especialistas",
    purpose:
      "Gerenciar especialistas, clientes atendidos, catálogo liberado e tokens de integração somente quando o módulo comercial estiver habilitado.",
    flow: [
      "Confirme que o módulo está habilitado para a empresa e que seu usuário possui perfil Administrador.",
      "Convide especialistas que criarão e revisarão rascunhos sem acessar a administração da empresa.",
      "Cadastre cada cliente atendido e informe a plataforma e o identificador externo.",
      "Selecione as avaliações publicadas que formarão o catálogo do cliente.",
      "Ative o cliente e gere ou rotacione o token de integração, copiando-o no momento da emissão.",
    ],
    fields: [
      { name: "Especialista", description: "Usuário restrito à criação e revisão de avaliações, sem acesso a clientes, cobrança ou publicação." },
      { name: "Cliente", description: "Organização atendida pela empresa parceira." },
      { name: "Plataforma", description: "Provedor externo associado ao cliente, como Gupy ou Recrutei." },
      { name: "ID externo", description: "Identificador da empresa cliente na plataforma integrada." },
      { name: "Catálogo", description: "Avaliações publicadas liberadas especificamente para o cliente selecionado." },
      { name: "Token", description: "Credencial apresentada uma única vez para autenticar a integração do cliente." },
    ],
    permissions: [
      "Feature flag PRAXIS_PARTNER_ENABLED ativada para a instalação.",
      "Perfil Administrador com a permissão PARTNER_MANAGER.",
      "Avaliações publicadas pertencentes à mesma empresa para liberação no catálogo.",
    ],
    states: [
      "Módulo desabilitado",
      "Permissão insuficiente",
      "Sem especialistas ou clientes",
      "Cliente ativo",
      "Cliente inativo",
      "Token não configurado",
      "Token gerado",
      "Catálogo salvo",
    ],
    blocks: [
      "Módulo comercial não habilitado para a empresa.",
      "Usuário sem perfil Administrador.",
      "Cliente inativo ao gerar token ou alterar o catálogo.",
      "Identificador externo já utilizado para o mesmo provedor.",
      "Avaliação ainda não publicada ou não pertencente à empresa.",
    ],
    examples: [
      "Convidar uma psicóloga como especialista para revisar cenários sem conceder acesso financeiro.",
      "Cadastrar um cliente Gupy, liberar duas avaliações publicadas e gerar a credencial de integração.",
    ],
    shortcuts: [
      "O item de menu aparece somente quando a flag e a permissão estão presentes.",
      "Copie o token imediatamente; o valor completo não poderá ser recuperado depois.",
      "Remover a função de especialista devolve o usuário ao perfil Operador, sem torná-lo administrador.",
    ],
    matches: (pathname) => pathname === "/parceiros",
  },
  {
    id: "onboarding-inicial",
    title: "Comece aqui",
    purpose:
      "Orientar somente a configuração inicial da empresa até a conclusão do primeiro ciclo de avaliação e depois permanecer como referência na Central de manuais.",
    flow: [
      "Crie uma avaliação e defina objetivo, situação crítica e competências.",
      "Monte o cenário, revise os bloqueios e publique uma versão.",
      "Crie uma jornada com a avaliação publicada.",
      "Abra Convite por jornada, informe a pessoa participante e gere o acesso.",
      "Acompanhe a participação e revise o primeiro resultado concluído.",
      "Após a conclusão do ciclo, continue pelo Dashboard e consulte este processo na Central de manuais quando necessário.",
    ],
    fields: [
      { name: "Etapa", description: "Parte do primeiro ciclo que precisa ser concluída." },
      { name: "Situação", description: "Indica se a etapa está pendente ou já foi confirmada pelos dados reais do Dashboard." },
      { name: "Progresso", description: "Percentual calculado a partir das seis etapas do onboarding." },
      { name: "Próxima ação", description: "Atalho para a primeira etapa ainda pendente." },
      { name: "Convite por jornada", description: "Destino correto para selecionar uma jornada publicada e convidar a pessoa participante." },
    ],
    permissions: [
      "Usuário autenticado da empresa com acesso às funções necessárias para executar as etapas.",
      "As ações específicas continuam sujeitas ao perfil operacional do usuário.",
    ],
    states: [
      "Calculando progresso",
      "Não iniciado",
      "Em andamento",
      "Erro ao calcular progresso",
      "Onboarding concluído",
      "Guia oculto da navegação",
    ],
    blocks: [
      "Avaliação ainda não publicada.",
      "Jornada ainda não criada ou publicada.",
      "Ausência de participação criada ou concluída.",
      "Usuário sem permissão para a próxima ação.",
      "Falha temporária ao consultar o Dashboard.",
    ],
    examples: [
      "Criar uma avaliação de atendimento, publicá-la, adicioná-la a uma jornada e convidar a primeira candidata.",
      "Após revisar o primeiro resultado, acessar o processo apenas pela Central de manuais porque o onboarding saiu do menu.",
    ],
    shortcuts: [
      "Use Próxima ação para continuar exatamente da primeira etapa pendente.",
      "A etapa Convide e acompanhe abre diretamente /participacoes/jornada.",
      "Depois da conclusão, use /manual#onboarding-inicial para revisar o processo completo.",
    ],
    matches: (pathname) => pathname === "/comecar",
  },
];

export function resolveAccessOnboardingManual(pathname: string) {
  return ACCESS_ONBOARDING_MANUALS.find((manual) => manual.matches(pathname));
}
