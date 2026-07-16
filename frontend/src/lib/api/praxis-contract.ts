import {
  createSimulationNode as createSimulationNodeLegacy,
  createSimulationOption as createSimulationOptionLegacy,
  getGupyPreflight as getGupyPreflightLegacy,
  getSimulationVersion as getSimulationVersionLegacy,
  updateSimulationNode as updateSimulationNodeLegacy,
  updateSimulationOption as updateSimulationOptionLegacy,
  type CreateNodeRequest as LegacyCreateNodeRequest,
  type CreateOptionRequest as LegacyCreateOptionRequest,
  type DashboardResponse as LegacyDashboardResponse,
  type GupyPreflightCheckCode as LegacyGupyPreflightCheckCode,
  type GupyPreflightResponse as LegacyGupyPreflightResponse,
  type SimulationVersionDetailResponse as LegacySimulationVersionDetailResponse,
  type SimulationVersionNodeResponse as LegacySimulationVersionNodeResponse,
  type SimulationVersionOptionResponse as LegacySimulationVersionOptionResponse,
  type UpdateNodeRequest as LegacyUpdateNodeRequest,
  type UpdateOptionRequest as LegacyUpdateOptionRequest,
} from "./praxis-legacy";

/**
 * Contratos públicos normalizados do frontend.
 *
 * O módulo legado continua como implementação interna para preservar os demais
 * consumidores, mas nomes corrompidos por substituições antigas não são mais
 * expostos por `@/lib/api/praxis`.
 */
export type SimulationVersionOptionResponse = Omit<
  LegacySimulationVersionOptionResponse,
  "plainTextDescriptioetapa"
> & {
  plainTextDescription: string | null;
};

export type SimulationVersionNodeResponse = Omit<
  LegacySimulationVersionNodeResponse,
  "plainTextDescriptioetapa" | "options"
> & {
  plainTextDescription: string | null;
  options: SimulationVersionOptionResponse[];
};

export type SimulationVersionDetailResponse = Omit<
  LegacySimulationVersionDetailResponse,
  "nodes"
> & {
  nodes: SimulationVersionNodeResponse[];
};

export type CreateNodeRequest = Omit<
  LegacyCreateNodeRequest,
  "timeJustificatioetapa" | "plainTextDescriptioetapa"
> & {
  timeJustification?: string | null;
  plainTextDescription?: string | null;
};

export type UpdateNodeRequest = Omit<
  LegacyUpdateNodeRequest,
  "timeJustificatioetapa" | "plainTextDescriptioetapa"
> & {
  timeJustification?: string | null;
  plainTextDescription?: string | null;
};

export type CreateOptionRequest = Omit<LegacyCreateOptionRequest, "plainTextDescriptioetapa"> & {
  plainTextDescription?: string | null;
};

export type UpdateOptionRequest = Omit<LegacyUpdateOptionRequest, "plainTextDescriptioetapa"> & {
  plainTextDescription?: string | null;
};

export type GupyPreflightCheckCode = LegacyGupyPreflightCheckCode | "publicationStatus";

export type GupyPreflightResponse = Omit<LegacyGupyPreflightResponse, "checks"> & {
  checks: Array<
    Omit<LegacyGupyPreflightResponse["checks"][number], "code"> & {
      code: GupyPreflightCheckCode;
    }
  >;
};

type LegacyRecommendedAction = LegacyDashboardResponse["recommendedActions"][number];

export type DashboardActionType = LegacyRecommendedAction["type"] | "CHECK_DLQ";

export type DashboardResponse = Omit<LegacyDashboardResponse, "recommendedActions"> & {
  recommendedActions: Array<
    Omit<LegacyRecommendedAction, "type"> & {
      type: DashboardActionType;
    }
  >;
};

export function getSimulationVersion(
  simulationId: string,
  versionNumber: number,
): Promise<SimulationVersionDetailResponse> {
  return getSimulationVersionLegacy(
    simulationId,
    versionNumber,
  ) as Promise<SimulationVersionDetailResponse>;
}

export function createSimulationNode(
  simulationId: string,
  versionNumber: number,
  body: CreateNodeRequest,
) {
  return createSimulationNodeLegacy(simulationId, versionNumber, body);
}

export function updateSimulationNode(
  simulationId: string,
  versionNumber: number,
  nodeId: string,
  body: UpdateNodeRequest,
) {
  return updateSimulationNodeLegacy(simulationId, versionNumber, nodeId, body);
}

export function createSimulationOption(
  simulationId: string,
  versionNumber: number,
  nodeId: string,
  body: CreateOptionRequest,
) {
  return createSimulationOptionLegacy(simulationId, versionNumber, nodeId, body);
}

export function updateSimulationOption(
  simulationId: string,
  versionNumber: number,
  nodeId: string,
  optionId: string,
  body: UpdateOptionRequest,
) {
  return updateSimulationOptionLegacy(simulationId, versionNumber, nodeId, optionId, body);
}

export function getGupyPreflight(
  simulationId: string,
  versionNumber: number,
): Promise<GupyPreflightResponse> {
  return getGupyPreflightLegacy(simulationId, versionNumber) as Promise<GupyPreflightResponse>;
}
