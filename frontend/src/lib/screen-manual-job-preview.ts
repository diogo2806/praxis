import type { ScreenManualDefinition } from "@/lib/screen-manuals";

export const JOB_PREVIEW_MANUALS: ScreenManualDefinition[] = [
  {
    id: "previas-realistas-vaga",
    title: "Prévias realistas da vaga",
    purpose:
      "Criar e versionar conteúdo equilibrado sobre a realidade do trabalho, separado da pontuação da avaliação e reutilizável em vagas, jornadas e integrações.",
    flow: [
      "Escolha se a prévia pertence a uma vaga, cargo ou jornada.",
      "Defina se ela será mostrada antes, depois ou nas duas etapas da avaliação.",
      "Preencha responsabilidades, autonomia, pressão, contato, situações críticas, rotina, condições e aspectos positivos.",
      "Adicione mídia somente com alternativa textual e transcrição quando aplicável.",
      "Relacione os cenários usados como exemplos, sem revelar pontuação ou resposta esperada.",
      "Crie o rascunho, revise o equilíbrio do conteúdo e publique a versão.",
      "Acompanhe métricas agregadas somente após atingir a amostra mínima.",
    ],
    fields: [
      { name: "Escopo", description: "Define se o conteúdo é aplicado por vaga, cargo ou jornada." },
      { name: "Identificador do escopo", description: "Código da vaga, cargo ou jornada que receberá a prévia." },
      { name: "Exibição", description: "Determina apresentação antes, depois ou em ambas as etapas." },
      { name: "Ciência informativa", description: "Registra apenas que a pessoa visualizou o conteúdo; não cria aceite contratual." },
      { name: "Responsabilidades e autonomia", description: "Descrevem entregas esperadas e limites reais de decisão." },
      { name: "Pressão, contato e situações críticas", description: "Apresentam exigências difíceis da rotina sem linguagem promocional enganosa." },
      { name: "Rotina e condições de trabalho", description: "Explicam frequência, ambiente, horários e condições relevantes." },
      { name: "Aspectos positivos", description: "Apresentam oportunidades e benefícios reais sem omitir os desafios." },
      { name: "Mídia e alternativa textual", description: "Imagem, áudio ou vídeo devem possuir descrição equivalente e transcrição quando aplicável." },
      { name: "Cenários relacionados", description: "Referenciam situações da avaliação sem mostrar gabarito, nota ou peso." },
    ],
    permissions: [
      "Gestores da empresa podem criar, publicar e consultar métricas.",
      "Editores de avaliação podem criar e atualizar rascunhos.",
      "Participantes acessam somente a versão publicada vinculada à própria tentativa.",
    ],
    states: [
      "Sem prévias",
      "Rascunho",
      "Publicada",
      "Versão arquivada",
      "Métricas suprimidas",
      "Com métricas agregadas",
      "Erro",
    ],
    blocks: [
      "Escopo ou identificador não informado.",
      "Aspectos positivos, pressão, situações críticas ou condições de trabalho ausentes.",
      "Mídia sem alternativa textual.",
      "Tentativa pertencente a outra empresa.",
      "Versão ainda não publicada para apresentação ao participante.",
      "Amostra abaixo do mínimo para exibir médias de reação.",
    ],
    examples: [
      "Apresentar antes do teste a rotina de atendimento com picos de demanda e autonomia limitada.",
      "Mostrar após a avaliação os aspectos positivos e as situações críticas do cargo sem alterar a nota.",
      "Reutilizar a URL da prévia em convite, página de vaga ou ATS com etapa e destino definidos.",
    ],
    shortcuts: [
      "Use um identificador de vaga para priorizar conteúdo específico sobre o conteúdo da jornada.",
      "Publique somente depois de revisar se desafios e aspectos positivos estão equilibrados.",
      "A versão apresentada fica preservada no histórico da tentativa.",
      "A prévia não substitui descrição da vaga, entrevista ou contrato.",
      "O processo completo está disponível na Central de manuais.",
    ],
    matches: (pathname) => pathname === "/previas-realistas",
  },
];

export function resolveJobPreviewManual(pathname: string) {
  return JOB_PREVIEW_MANUALS.find((manual) => manual.matches(pathname));
}
