import atendimentoTemplate from "../../../backend/src/main/resources/quickstart-templates/atendimento.json";
import complianceTemplate from "../../../backend/src/main/resources/quickstart-templates/compliance.json";
import liderancaTemplate from "../../../backend/src/main/resources/quickstart-templates/lideranca.json";
import onboardingTemplate from "../../../backend/src/main/resources/quickstart-templates/onboarding.json";
import vendasTemplate from "../../../backend/src/main/resources/quickstart-templates/vendas.json";
import type { QuickStartCategory } from "@/lib/api/praxis";

export type EmbeddedQuickStartCompetency = {
  name: string;
  weight: number;
  targetScore: number;
  tier: "major" | "minor";
};

export type EmbeddedQuickStartOption = {
  optionId: string;
  text: string;
  nextNodeId?: string | null;
  critical?: boolean;
  auditNote?: string | null;
  competencyScores: Record<string, number>;
};

export type EmbeddedQuickStartNode = {
  nodeId: string;
  turnIndex: number;
  speaker: string;
  message: string;
  timeLimitSeconds?: number | null;
  timeoutNextNodeId?: string | null;
  isFinal?: boolean;
  reportText?: string | null;
  options?: EmbeddedQuickStartOption[];
};

export type EmbeddedQuickStartTemplate = {
  category: QuickStartCategory;
  title: string;
  description: string;
  name: string;
  criticalSituation: string;
  resultUse: string;
  rootNodeId: string;
  competencies: EmbeddedQuickStartCompetency[];
  nodes: EmbeddedQuickStartNode[];
};

export const embeddedQuickStartTemplates: Record<QuickStartCategory, EmbeddedQuickStartTemplate> = {
  ATENDIMENTO: atendimentoTemplate as EmbeddedQuickStartTemplate,
  COMPLIANCE: complianceTemplate as EmbeddedQuickStartTemplate,
  LIDERANCA: liderancaTemplate as EmbeddedQuickStartTemplate,
  ONBOARDING: onboardingTemplate as EmbeddedQuickStartTemplate,
  VENDAS: vendasTemplate as EmbeddedQuickStartTemplate,
};

export function listEmbeddedQuickStartTemplates() {
  return Object.values(embeddedQuickStartTemplates).map((template) => ({
    category: template.category,
    title: template.title,
    description: template.description,
    nodeCount: template.nodes.filter((node) => !node.isFinal).length,
  }));
}
