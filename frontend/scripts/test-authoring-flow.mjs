import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const frontendRoot = new URL("../", import.meta.url);

async function read(relativePath) {
  return readFile(new URL(relativePath, frontendRoot), "utf8");
}

const [
  authoringFlow,
  simulationMeta,
  wizardStepper,
  objectiveRoute,
  quickStartRoute,
  simulationsNewRoute,
  pilotRoute,
  gupyRoute,
  competencyManual,
] = await Promise.all([
  read("src/lib/authoring-flow.ts"),
  read("src/lib/simulation-meta.ts"),
  read("src/components/wizard-stepper.tsx"),
  read("src/routes/nova.objetivo.tsx"),
  read("src/routes/nova.rapido.tsx"),
  read("src/routes/simulations/new.tsx"),
  read("src/routes/nova.piloto.tsx"),
  read("src/routes/nova.gupy.tsx"),
  read("src/lib/screen-manual-competency-ownership.ts"),
]);

const canonicalRoutes = [
  'assessment: "/nova/avaliacao"',
  'character: "/nova/personagem"',
  'dialogue: "/nova/dialogo"',
  'map: "/nova/mapa"',
  'review: "/nova/validador"',
  'governance: "/nova/governanca"',
];

for (const route of canonicalRoutes) {
  assert.ok(authoringFlow.includes(route), `Rota canônica ausente: ${route}`);
}

const sequenceIndexes = canonicalRoutes.map((route) => authoringFlow.indexOf(route));
for (let index = 1; index < sequenceIndexes.length; index += 1) {
  assert.ok(
    sequenceIndexes[index] > sequenceIndexes[index - 1],
    "A ordem declarada do fluxo de autoria foi alterada.",
  );
}

assert.match(simulationMeta, /canonicalAuthoringRoutes\.assessment/);
assert.match(simulationMeta, /canonicalAuthoringRoutes\.character/);
assert.match(simulationMeta, /dialogo: "cenario"/);
assert.match(simulationMeta, /mapa: "cenario"/);
assert.match(simulationMeta, /piloto: "publicacao"/);
assert.match(simulationMeta, /gupy: "publicacao"/);

assert.match(wizardStepper, /isScenarioAuthoringPath\(currentPathname\)/);
assert.match(wizardStepper, /aria-label="Subetapas do cenário"/);
assert.match(wizardStepper, /canonicalAuthoringRoutes\.character/);
assert.match(wizardStepper, /canonicalAuthoringRoutes\.dialogue/);
assert.match(wizardStepper, /canonicalAuthoringRoutes\.map/);

assert.match(objectiveRoute, /throw redirect/);
assert.match(objectiveRoute, /legacyAuthoringRedirects\.objective/);
assert.match(objectiveRoute, /search,/);

assert.match(simulationsNewRoute, /throw redirect/);
assert.match(simulationsNewRoute, /legacyAuthoringRedirects\.simulationsNew/);
assert.match(simulationsNewRoute, /validateSearch/);

assert.match(
  quickStartRoute,
  /const QUICK_START_DESTINATION = canonicalAuthoringRoutes\.assessment/,
);
assert.doesNotMatch(quickStartRoute, /QUICK_START_DESTINATION = "\/nova\/validador"/);

assert.doesNotMatch(pilotRoute, /WizardStepper/);
assert.match(pilotRoute, /Operação pós-publicação/);
assert.doesNotMatch(gupyRoute, /WizardStepper/);
assert.match(gupyRoute, /Operação pós-publicação/);

assert.doesNotMatch(competencyManual, /objetivo-somente-leitura/);
assert.doesNotMatch(competencyManual, /pathname === "\/nova\/objetivo"/);

console.log("Fluxo de autoria consolidado e rotas legadas protegidas.");
