import { JOB_PREVIEW_MANUALS } from "@/lib/screen-manual-job-preview";
import type { ScreenManualDefinition } from "@/lib/screen-manuals";

export const QUALITY_MANUALS: ScreenManualDefinition[] = [
  {
    id: "qualidade-psicometrica-justica",
    title: "Qualidade psicométrica e justiça",
    purpose:
      "Analisar se uma avaliação diferencia respostas, mantém precisão aceitável e se relaciona com critérios externos sem transformar associação em decisão automática.",
    flow: [
      "Informe avaliação, versão, vaga e período comparáveis.",
      "Calcule os indicadores e revise alertas de amostra insuficiente.",
      "Analise distribuição, alternativas, caminhos, cenários e precisão por competência.",
      "Cadastre critérios externos observados após a aplicação.",
      "Execute análises entre grupos somente com finalidade e base legal registradas.",
      "Exporte o relatório técnico com metodologia e limitações.",
    ],
    fields: [
      { name: "Avaliação e versão", description: "Recorte que impede misturar conteúdos incompatíveis." },
      { name: "Vaga Gupy", description: "Contexto opcional usado para limitar a amostra ao mesmo processo." },
      { name: "Período", description: "Intervalo de criação das participações analisadas." },
      { name: "Critério externo", description: "Medida numérica ou categórica, como entrevista, aprovação, desempenho ou retenção." },
      { name: "Finalidade e base legal", description: "Justificativa obrigatória para análise protegida entre grupos." },
      { name: "Amostra mínima", description: "Quantidade mínima para exibir um grupo sem risco indevido de identificação." },
    ],
    permissions: [
      "Perfil de gestão da empresa ou analista de resultados autorizado.",
      "Análises entre grupos exigem acesso restrito aos resultados da própria empresa.",
    ],
    states: [
      "Sem filtros",
      "Calculando",
      "Amostra insuficiente",
      "Com dados observados",
      "Com estimativas",
      "Análise sensível auditada",
      "Erro",
    ],
    blocks: [
      "Avaliação não informada.",
      "Período inicial posterior ao final.",
      "Participação ou critério externo pertencente a outra empresa.",
      "Critério de grupo sem valores categóricos.",
      "Finalidade, base legal ou amostra mínima não informada.",
      "Grupo abaixo da amostra mínima, exibido como suprimido.",
    ],
    examples: [
      "Comparar a frequência das alternativas da versão 3 em uma vaga específica.",
      "Relacionar a nota final à entrevista estruturada sem afirmar causalidade.",
      "Analisar diferenças por um critério categórico com supressão automática de grupos pequenos.",
    ],
    shortcuts: [
      "Filtre sempre uma versão antes de concluir sobre qualidade.",
      "Use o relatório CSV para revisão técnica e documentação metodológica.",
      "Dado observado, estimativa e recomendação aparecem identificados separadamente.",
      "O processo completo está disponível na Central de manuais.",
    ],
    matches: (pathname) => pathname === "/resultados/qualidade",
  },
  ...JOB_PREVIEW_MANUALS,
];

export function resolveQualityManual(pathname: string) {
  return QUALITY_MANUALS.find((manual) => manual.matches(pathname));
}
