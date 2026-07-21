import type { ScreenManualDefinition } from "@/lib/screen-manuals";

const PARTNER_SPECIALIST_HOME_MANUAL: ScreenManualDefinition = {
  id: "area-especialista",
  title: "Área do especialista parceiro",
  purpose:
    "Centralizar as funções autorizadas para criar, editar e revisar rascunhos de avaliações sem expor áreas administrativas da empresa.",
  flow: [
    "Abra Minhas avaliações para localizar um rascunho existente ou selecione Criar avaliação.",
    "Preencha o plano, o personagem, o diálogo, a pontuação e o mapa do fluxo.",
    "Execute a validação estrutural e corrija os bloqueios encontrados.",
    "Entregue a versão revisada para que a empresa realize a publicação e a distribuição.",
  ],
  fields: [
    {
      name: "Minhas avaliações",
      description: "Abre a lista de avaliações que podem ser consultadas ou editadas pelo especialista.",
    },
    {
      name: "Criar avaliação",
      description: "Inicia um novo rascunho usando as competências disponibilizadas pela empresa.",
    },
    {
      name: "Consultar competências",
      description: "Exibe o catálogo da empresa em modo de consulta para uso nos rascunhos.",
    },
    {
      name: "Minha conta",
      description: "Permite alterar dados pessoais e credenciais do próprio acesso.",
    },
    {
      name: "Central de manuais",
      description: "Apresenta o processo completo e as orientações operacionais de cada tela.",
    },
  ],
  permissions: [
    "Conta ativa com o perfil PARTNER_SPECIALIST vinculada a uma empresa parceira.",
    "A edição estrutural exige uma versão em rascunho.",
    "Publicação, arquivamento, duplicação e administração permanecem exclusivas da empresa.",
  ],
  states: [
    "Área disponível",
    "Sem avaliações",
    "Rascunho em edição",
    "Rascunho com bloqueios",
    "Rascunho validado e aguardando a empresa",
    "Acesso bloqueado ou convite pendente",
  ],
  blocks: [
    "Conta sem o perfil de especialista parceiro.",
    "Usuário bloqueado, convite expirado ou sessão encerrada.",
    "Versão publicada ou arquivada, que permanece somente para consulta.",
    "Competência necessária ainda não disponibilizada pela empresa.",
    "Ação administrativa reservada a um usuário da empresa.",
  ],
  examples: [
    "Criar uma avaliação de atendimento, configurar o diálogo e corrigir os bloqueios indicados pelo Validador.",
    "Abrir um rascunho existente, ajustar alternativas e entregar a versão pronta para publicação pela empresa.",
  ],
  shortcuts: [
    "Use Criar avaliação para iniciar um rascunho sem passar pela lista.",
    "Use o menu lateral para alternar entre avaliações, competências, conta e manuais.",
    "Use Tab e Shift+Tab para navegar pelos cards e ações.",
    "Abra o manual de cada tela pelo ícone de livro no canto da interface.",
  ],
  matches: (pathname) => pathname === "/avaliacoes/especialista",
};

export function resolvePartnerSpecialistManual(
  pathname: string,
): ScreenManualDefinition | undefined {
  return PARTNER_SPECIALIST_HOME_MANUAL.matches(pathname)
    ? PARTNER_SPECIALIST_HOME_MANUAL
    : undefined;
}
