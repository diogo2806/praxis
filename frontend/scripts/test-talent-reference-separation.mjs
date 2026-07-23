import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const frontendRoot = new URL("../", import.meta.url);
const repositoryRoot = new URL("../../", import.meta.url);

async function readFrontend(path) {
  return readFile(new URL(path, frontendRoot), "utf8");
}

async function readRepository(path) {
  return readFile(new URL(path, repositoryRoot), "utf8");
}

const [route, api, manual, backendResponse, migration] = await Promise.all([
  readFrontend("src/routes/talent-match.tsx"),
  readFrontend("src/lib/api/talent-match.ts"),
  readFrontend("src/lib/screen-manual-analysis-operations.ts"),
  readRepository("backend/src/main/java/br/com/iforce/praxis/simulation/dto/TalentMatchResponse.java"),
  readRepository("backend/src/main/resources/db/migration/V1021__talent_match_references.sql"),
]);

assert.match(route, /targetProfile/);
assert.match(route, /normativeReference/);
assert.match(route, /decisionThreshold/);
assert.match(route, /referenceSnapshot/);
assert.match(route, /minimumSample/);
assert.match(route, /pathCompatibilityConfirmed/);
assert.doesNotMatch(route, /result\.benchmark/);
assert.doesNotMatch(route, /Referência desejada/);

assert.match(api, /CompetencyTargetProfileDto/);
assert.match(api, /NormativeReferenceResponse/);
assert.match(api, /DecisionThresholdResponse/);
assert.match(api, /CandidateReferenceSnapshotDto/);
assert.doesNotMatch(api, /CompetencyBenchmarkDto/);

assert.match(backendResponse, /targetProfile/);
assert.match(backendResponse, /normativeReference/);
assert.match(backendResponse, /decisionThreshold/);
assert.doesNotMatch(backendResponse, /benchmark/);

assert.match(migration, /minimum_sample INTEGER NOT NULL DEFAULT 30/);
assert.match(migration, /talent_reference_snapshots/);
assert.match(migration, /uk_talent_snapshot_attempt/);

for (const requiredManualSection of [
  "purpose:",
  "flow:",
  "fields:",
  "permissions:",
  "states:",
  "blocks:",
  "examples:",
  "shortcuts:",
]) {
  assert.match(manual, new RegExp(requiredManualSection));
}
assert.match(manual, /Perfil-alvo configurado/);
assert.match(manual, /Referência normativa/);
assert.match(manual, /Nota de corte/);

console.log("Separação das referências do Talent Match validada.");
