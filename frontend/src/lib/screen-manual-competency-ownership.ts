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
      { name: "Cargo ou contexto", description: "Nome operacional da avaliação e contexto em que ela será aplicada." },
      { name: "Situação crítica", description: "Cenário realista que orienta a construção das etapas e alternativas." },
      { name: "Busca de competências", description: "Filtra somente competências ativas disponíveis no catálogo da empresa." },
      { name: "Competências selecionadas", description: "Dimensões do catálogo que farão parte desta versão da avaliação." },
      { name: "Competências antigas", description: "Referências preservadas em versões antigas quando já não existem no catálogo ativo; podem ser removidas do rascunho." },
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
  {
    id: "objetivo-somente-leitura",
    title: "Resumo do objetivo e modelo base",
    purpose:
      "Revisar, sem editar, o objetivo, a versão, a situação crítica e as competências selecionadas antes da autoria do personagem.",
    flow: [
      "Abra a tela com avaliação e versão informadas.",
      "Revise nome, descrição, primeira etapa, situação crítica e situação da versão.",
      "Confira competências, pesos, importância e metas em modo somente leitura.",
      "Volte para Nova avaliação para alterar a seleção ou siga para Personagem.",
    ],
    fields: [
      { name: "Avaliação e versão", description: "Contexto obrigatório do resumo exibido." },
      { name: "Primeira etapa", description: "Identificador do nó inicial configurado no modelo base." },
      { name: "Situação crítica", description: "Contexto definido em Nova avaliação e exibido apenas para conferência." },
      { name: "Competência", description: "Nome da dimensão previamente selecionada em Nova avaliação." },
      { name: "Peso", description: "Participação percentual da competência no resultado da versão." },
      { name: "Importância e meta", description: "Classificação e valor-alvo mantidos pela configuração da versão." },
    ],
    permissions: ["Perfil EMPRESA com acesso de leitura à avaliação e à versão da própria empresa."],
    states: [
      "Sem contexto",
      "Carregando",
      "Resumo disponível",
      "Sem competências selecionadas",
      "Falha de carregamento",
    ],
    blocks: [
      "Avaliação ou versão não informada.",
      "Versão não encontrada ou pertencente a outra empresa.",
      "Nenhuma competência selecionada para avançar ao Personagem.",
      "Falha temporária ao carregar o resumo.",
    ],
    examples: [
      "Conferir que a versão 2 possui três competências antes de começar a escrever o personagem.",
      "Identificar ausência de competências e voltar para Nova avaliação para selecionar itens do catálogo.",
    ],
    shortcuts: [
      "Use Alterar seleção para retornar diretamente ao rascunho da versão.",
      "Use Abrir catálogo apenas para manter competências globais.",
      "Esta tela não salva nem redistribui pesos.",
      "Consulte o processo completo na Central de manuais.",
    ],
    matches: (pathname) => pathname === "/nova/objetivo",
  },
];

export function resolveCompetencyOwnershipManual(pathname: string) {
  return COMPETENCY_OWNERSHIP_MANUALS.find((manual) => manual.matches(pathname));
}
