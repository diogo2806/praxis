import {
  createSimulationNode as createSimulationNodeLegacy,
  createSimulationOption as createSimulationOptionLegacy,
  getGupyPreflight as getGupyPreflightLegacy,
  getSimulationVersion as getSimulationVersionLegacy,
  updateSimulationNode as updateSimulationNodeLegacy,
  updateSimulationOption as updateSimulationOptionLegacy,
  type CreateNodeRequest as CanonicalCreateNodeRequest,
  type CreateOptionRequest as CanonicalCreateOptionRequest,
  type DashboardResponse as LegacyDashboardResponse,
  type GupyPreflightCheckCode as LegacyGupyPreflightCheckCode,
  type GupyPreflightResponse as LegacyGupyPreflightResponse,
  type SimulationVersionDetailResponse as CanonicalSimulationVersionDetailResponse,
  type SimulationVersionNodeResponse as CanonicalSimulationVersionNodeResponse,
  type SimulationVersionOptionResponse as CanonicalSimulationVersionOptionResponse,
  type UpdateNodeRequest as CanonicalUpdateNodeRequest,
  type UpdateOptionRequest as CanonicalUpdateOptionRequest,
} from "./praxis-legacy";

/**
 * Contratos públicos do frontend alinhados diretamente ao payload do backend.
 *
 * Os aliases abaixo apontam para uma única definição canônica. Este módulo mantém
 * apenas os comportamentos adicionais da fachada, sem reconstruir campos ou
 * mascarar incompatibilidades de nomes.
 */
export type CreateNodeRequest = CanonicalCreateNodeRequest;
export type CreateOptionRequest = CanonicalCreateOptionRequest;
export type SimulationVersionDetailResponse = CanonicalSimulationVersionDetailResponse;
export type SimulationVersionNodeResponse = CanonicalSimulationVersionNodeResponse;
export type SimulationVersionOptionResponse = CanonicalSimulationVersionOptionResponse;
export type UpdateNodeRequest = CanonicalUpdateNodeRequest;
export type UpdateOptionRequest = CanonicalUpdateOptionRequest;

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

const competencyLevelsSnapshot = Symbol("competencyLevelsSnapshot");

type TaggedCompetencyLevels = Record<string, number> & {
  [competencyLevelsSnapshot]?: Record<string, number>;
};

const optionCompetencyLevelsCache = new Map<string, Record<string, number>>();
const optionUpdateQueues = new Map<string, Promise<void>>();
const versionNodesCache = new Map<string, SimulationVersionNodeResponse[]>();
let competencyUpdateRevision = 0;

function optionCacheKey(
  simulationId: string,
  versionNumber: number,
  nodeId: string,
  optionId: string,
) {
  return `${simulationId}:${versionNumber}:${nodeId}:${optionId}`;
}

function versionCacheKey(simulationId: string, versionNumber: number) {
  return `${simulationId}:${versionNumber}`;
}

function stabilizeNodes(
  simulationId: string,
  versionNumber: number,
  nextNodes: SimulationVersionNodeResponse[],
) {
  const key = versionCacheKey(simulationId, versionNumber);
  const cachedNodes = versionNodesCache.get(key);

  if (!cachedNodes) {
    versionNodesCache.set(key, nextNodes);
    return nextNodes;
  }

  cachedNodes.splice(0, cachedNodes.length, ...nextNodes);
  return cachedNodes;
}

function tagCompetencyLevels(levels: Record<string, number>, snapshot: Record<string, number>) {
  const taggedLevels = { ...levels } as TaggedCompetencyLevels;
  Object.defineProperty(taggedLevels, competencyLevelsSnapshot, {
    value: { ...snapshot },
    enumerable: true,
  });
  return taggedLevels;
}

export async function getSimulationVersion(
  simulationId: string,
  versionNumber: number,
): Promise<SimulationVersionDetailResponse> {
  const requestRevision = competencyUpdateRevision;
  const response = await getSimulationVersionLegacy(simulationId, versionNumber);

  const mappedNodes = response.nodes.map((node) => ({
    ...node,
    options: node.options.map((option) => {
      const key = optionCacheKey(simulationId, versionNumber, node.id, option.id);
      const serverLevels = { ...option.competencyLevels };
      const hasNewerLocalChanges =
        requestRevision !== competencyUpdateRevision || optionUpdateQueues.has(key);
      const effectiveLevels = hasNewerLocalChanges
        ? optionCompetencyLevelsCache.get(key) ?? serverLevels
        : serverLevels;

      if (!hasNewerLocalChanges) {
        optionCompetencyLevelsCache.set(key, serverLevels);
      }

      return {
        ...option,
        competencyLevels: tagCompetencyLevels(effectiveLevels, effectiveLevels),
      };
    }),
  }));

  return {
    ...response,
    nodes: stabilizeNodes(simulationId, versionNumber, mappedNodes),
  };
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
  const normalizedBody =
    Object.prototype.hasOwnProperty.call(body, "timeLimitSeconds") &&
    body.timeLimitSeconds === null
      ? { ...body, timeLimitSeconds: 0 }
      : body;

  return updateSimulationNodeLegacy(simulationId, versionNumber, nodeId, normalizedBody);
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
  if (!body.competencyLevels) {
    return updateSimulationOptionLegacy(simulationId, versionNumber, nodeId, optionId, body);
  }

  const key = optionCacheKey(simulationId, versionNumber, nodeId, optionId);
  const taggedLevels = body.competencyLevels as TaggedCompetencyLevels;
  const snapshot = taggedLevels[competencyLevelsSnapshot];
  const submittedLevels = Object.fromEntries(Object.entries(taggedLevels));

  const changedLevels = snapshot
    ? Object.fromEntries(
        Object.entries(submittedLevels).filter(([name, value]) => snapshot[name] !== value),
      )
    : submittedLevels;

  if (Object.keys(changedLevels).length === 0) return Promise.resolve();

  const currentLevels = optionCompetencyLevelsCache.get(key) ?? snapshot ?? submittedLevels;
  const mergedLevels = snapshot
    ? { ...currentLevels, ...changedLevels }
    : { ...submittedLevels };

  optionCompetencyLevelsCache.set(key, mergedLevels);
  competencyUpdateRevision += 1;

  const previousUpdate = optionUpdateQueues.get(key) ?? Promise.resolve();
  const update = previousUpdate
    .catch(() => undefined)
    .then(async () => {
      await updateSimulationOptionLegacy(simulationId, versionNumber, nodeId, optionId, {
        ...body,
        competencyLevels: mergedLevels,
      });
    });
  const settledUpdate = update.then(
    () => undefined,
    () => undefined,
  );

  optionUpdateQueues.set(key, settledUpdate);
  void settledUpdate.then(() => {
    if (optionUpdateQueues.get(key) === settledUpdate) optionUpdateQueues.delete(key);
  });

  return update;
}

export function getGupyPreflight(
  simulationId: string,
  versionNumber: number,
): Promise<GupyPreflightResponse> {
  return getGupyPreflightLegacy(simulationId, versionNumber) as Promise<GupyPreflightResponse>;
}
