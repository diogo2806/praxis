import type { ScreenManualDefinition } from "@/lib/screen-manuals";

export const COMPETENCY_OWNERSHIP_MANUALS: ScreenManualDefinition[] = [
  {
    id: "nova-avaliacao-catalogo",
    title: "Plano inicial e seleção de competências",
    purpose:
      "Definir cargo, situação crítica e selecionar para a versão apenas competências ativas do catálogo central da empresa.",
    flow: [
      "Informe o cargo ou contexto da avaliação.",
      "Descreva a situação crítica que será simulada.",
      "Pesquise e selecione uma ou mais competências já existentes no catálogo.",
      "Continue para Personagem; para criar ou alterar uma competência global, abra Competências.",
    ],
    fields: [
      {
        name: "Cargo ou contexto",
        description: "Nome operacional da avaliação e contexto em que ela será aplicada.",
      },
      {
        name: "Situação crítica",
        description: "Cenário realista que orienta a construção das etapas e alternativas.",
      },
      {
        name: "Busca de competências",
        description: "Filtra somente competências ativas disponíveis no catálogo da empresa.",
      },
      {
        name: "Competências selecionadas",
        description: "Dimensões do catálogo que farão parte desta versão da avaliação.",
      },
      {
        name: "Competências antigas",
        description:
          "Referências preservadas em versões antigas quando já não existem no catálogo ativo; podem ser removidas do rascunho.",
      },
    ],
    permissions: [
      "Perfil EMPRESA com acesso à criação ou edição de avaliações.",
      "A criação, edição e inativação do catálogo exigem acesso à tela Competências.",
    ],
    states: [
      "Novo plano",
      "Rascunho editável",
      "Versão publicada protegida",
      "Versão arquivada",
      "Catálogo carregando",
      "Catálogo indisponível",
    ],
    blocks: [
      "Cargo não informado.",
      "Situação crítica não informada.",
      "Nenhuma competência selecionada.",
      "Versão publicada ou arquivada sem rascunho editável.",
      "Falha ao carregar o catálogo central de competências.",
    ],
    examples: [
      "Criar uma avaliação para liderança selecionando Comunicação, Tomada de decisão e Resolução de conflitos já cadastradas.",
      "Remover de um rascunho uma competência antiga que não está mais ativa no catálogo.",
    ],
    shortcuts: [
      "Use a busca antes de percorrer todas as competências.",
      "Use Gerenciar catálogo para abrir Competências sem tentar criar opções nesta tela.",
      "Consulte o processo completo na Central de manuais.",
    ],
    matches: (pathname) => pathname === "/nova/avaliacao",
  },
];

export function resolveCompetencyOwnershipManual(pathname: string) {
  return COMPETENCY_OWNERSHIP_MANUALS.find((manual) => manual.matches(pathname));
}
