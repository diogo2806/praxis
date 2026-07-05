import type { Language } from "./index";

export type DocumentMetadataTranslation = {
  title: string;
  description: string;
  socialTitle: string;
  socialDescription: string;
};

export const documentMetadataTranslations: Record<Language, DocumentMetadataTranslation> = {
  "pt-BR": {
    title: "Práxis — Avaliações por cenários estruturadas e rastreáveis",
    description:
      "Crie avaliações por cenários, configure critérios e pesos, compartilhe por link e acompanhe respostas, indicadores e registros do percurso.",
    socialTitle: "Práxis - Avaliações situacionais",
    socialDescription:
      "Crie cenários interativos, compartilhe avaliações por link e acompanhe indicadores definidos previamente pela sua equipe.",
  },
  en: {
    title: "Praxis — Structured and traceable scenario-based assessments",
    description:
      "Create scenario-based assessments, configure criteria and weights, share them by link, and follow responses, indicators, and activity records.",
    socialTitle: "Praxis - Situational assessments",
    socialDescription:
      "Create interactive scenarios, share assessments by link, and follow indicators defined in advance by your team.",
  },
  "es-MX": {
    title: "Praxis — Evaluaciones por escenarios estructuradas y rastreables",
    description:
      "Crea evaluaciones por escenarios, configura criterios y ponderaciones, compártelas por enlace y sigue respuestas, indicadores y registros del recorrido.",
    socialTitle: "Praxis - Evaluaciones situacionales",
    socialDescription:
      "Crea escenarios interactivos, comparte evaluaciones por enlace y sigue indicadores definidos previamente por tu equipo.",
  },
};
