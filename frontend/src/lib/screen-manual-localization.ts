import type { ScreenManualDefinition } from "@/lib/screen-manuals";

export const LOCALIZATION_MANUALS: ScreenManualDefinition[] = [
  {
    id: "idiomas-avaliacao",
    title: "Idiomas da avaliação",
    purpose:
      "Configurar, traduzir, revisar e aprovar conteúdos em vários idiomas sem alterar nós, alternativas, pesos, destinos ou fórmula de pontuação.",
    flow: [
      "Informe o ID da versão e configure o idioma base e os idiomas habilitados.",
      "Selecione um idioma e traduza somente os campos textuais e de acessibilidade.",
      "Salve o rascunho e corrija IDs ausentes, duplicados ou desconhecidos.",
      "Envie a tradução estruturalmente válida para revisão.",
      "A pessoa gestora aprova o idioma para participantes e relatórios.",
      "Exporte ou importe pacotes JSON preservando os identificadores estruturais.",
    ],
    fields: [
      { name: "ID da versão", description: "Identifica a versão imutável da avaliação que compartilha o mesmo grafo entre idiomas." },
      { name: "Idioma base", description: "Idioma original usado como referência e retorno seguro quando a tradução não estiver aprovada." },
      { name: "Idiomas habilitados", description: "Lista de localidades disponíveis, como pt-BR, en e es-MX." },
      { name: "Título, descrição e instruções", description: "Textos gerais apresentados antes e durante a avaliação." },
      { name: "Nós e alternativas", description: "Textos localizados vinculados aos mesmos nodeId e optionId do idioma base." },
      { name: "Acessibilidade", description: "Descrição textual e transcrição de mídia específicas por idioma." },
      { name: "Competências e relatório", description: "Nomes e textos explicativos apresentados no resultado localizado." },
      { name: "Pacote JSON", description: "Formato de exportação/importação para revisão externa sem permitir edição estrutural." },
    ],
    permissions: [
      "Gestores e editores podem configurar e editar rascunhos.",
      "Especialistas parceiros podem traduzir e enviar para revisão.",
      "Somente gestores da empresa podem aprovar um idioma.",
      "Participantes recebem apenas idioma aprovado ou o idioma base como fallback.",
    ],
    states: [
      "Não configurado",
      "Rascunho",
      "Em revisão",
      "Aprovado",
      "Estrutura inválida",
      "Acessibilidade incompleta",
      "Fallback para idioma base",
    ],
    blocks: [
      "Versão inexistente ou pertencente a outra empresa.",
      "Nós, alternativas ou competências ausentes, duplicados ou desconhecidos.",
      "Campos obrigatórios ou acessíveis não preenchidos.",
      "Tentativa de alterar idioma base pelo editor de tradução.",
      "Tentativa de editar conteúdo já aprovado.",
      "Aprovação sem passagem pelo estado de revisão.",
    ],
    examples: [
      "Traduzir a versão 12 de pt-BR para en mantendo exatamente os mesmos IDs e pesos.",
      "Importar um pacote revisado por especialista externo e corrigir uma alternativa ausente.",
      "Permitir que a pessoa candidata escolha es-MX no link e manter o idioma até o resultado.",
    ],
    shortcuts: [
      "Use Exportar JSON para revisão externa e Importar para validar a devolução.",
      "A barra de completude compara traduções com o grafo da versão base.",
      "Mudar o idioma não cria nova tentativa nem altera o avanço já realizado.",
      "O idioma escolhido fica persistido na participação e é reutilizado no relatório.",
      "O processo completo está disponível na Central de manuais.",
    ],
    matches: (pathname) => pathname === "/avaliacoes/traducoes",
  },
];

export function resolveLocalizationManual(pathname: string) {
  return LOCALIZATION_MANUALS.find((manual) => manual.matches(pathname));
}
