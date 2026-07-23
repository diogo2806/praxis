import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDirectory = dirname(fileURLToPath(import.meta.url));
const frontendDirectory = resolve(scriptDirectory, "..");
const routeSource = readFileSync(
  join(frontendDirectory, "src", "routes", "nova.previa.tsx"),
  "utf8",
);
const analysisSource = readFileSync(
  join(frontendDirectory, "src", "lib", "preview-analysis.ts"),
  "utf8",
);
const wizardSource = readFileSync(
  join(frontendDirectory, "src", "components", "wizard-stepper.tsx"),
  "utf8",
);
const manualSource = readFileSync(
  join(frontendDirectory, "src", "lib", "screen-manual-preview.ts"),
  "utf8",
);

assert.match(routeSource, /createFileRoute\("\/nova\/previa"\)/);
assert.match(routeSource, /não\s+cria participação/);
assert.match(routeSource, /não\s+consome crédito/);
assert.match(routeSource, /não\s+gera resultado oficial/);
assert.match(routeSource, /getSimulationValidation/);
assert.match(routeSource, /Cobertura da sessão/);
assert.match(routeSource, /Mapa de cobertura/);
assert.match(routeSource, /sessionStorage/);
assert.doesNotMatch(routeSource, /createCandidateAttempt|submitAnswer|createParticipation|consumeCredit/);

assert.match(analysisSource, /"missing-destination"/);
assert.match(analysisSource, /"missing-node"/);
assert.match(analysisSource, /"cycle"/);
assert.match(analysisSource, /"long-path"/);
assert.match(analysisSource, /"unreachable-final"/);
assert.match(analysisSource, /visitedOptions/);
assert.match(analysisSource, /visitedFinals/);

assert.match(wizardSource, /Testar como candidato/);
assert.match(wizardSource, /canonicalAuthoringRoutes\.preview/);

assert.match(manualSource, /id: "previa-jornada-candidato"/);
assert.match(manualSource, /purpose:/);
assert.match(manualSource, /flow:/);
assert.match(manualSource, /fields:/);
assert.match(manualSource, /permissions:/);
assert.match(manualSource, /states:/);
assert.match(manualSource, /blocks:/);
assert.match(manualSource, /examples:/);
assert.match(manualSource, /shortcuts:/);

console.log("Contrato da prévia da jornada e do mapa de cobertura validado.");
