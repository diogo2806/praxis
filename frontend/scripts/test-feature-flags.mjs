import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDirectory = dirname(fileURLToPath(import.meta.url));
const frontendDirectory = resolve(scriptDirectory, "..");
const repositoryDirectory = resolve(frontendDirectory, "..");

const route = readFileSync(
  join(frontendDirectory, "src", "routes", "admin.feature-flags.tsx"),
  "utf8",
);
const api = readFileSync(
  join(frontendDirectory, "src", "lib", "api", "feature-flags.ts"),
  "utf8",
);
const manual = readFileSync(
  join(frontendDirectory, "src", "lib", "screen-manual-overrides.ts"),
  "utf8",
);
const service = readFileSync(
  join(
    repositoryDirectory,
    "backend",
    "src",
    "main",
    "java",
    "br",
    "com",
    "iforce",
    "praxis",
    "featureflag",
    "service",
    "FeatureFlagService.java",
  ),
  "utf8",
);

assert.match(route, /createFileRoute\("\/admin\/feature-flags"\)/);
assert.match(route, /toggleFeatureFlagKillSwitch/);
assert.match(route, /evaluateFeatureFlag/);
assert.match(route, /rolloutPercentage/);
assert.match(api, /\/api\/admin\/feature-flags/);
assert.match(manual, /id:\s*"feature-flags"/);
assert.match(manual, /Finalidade|purpose:/);

const precedence = [
  "KILL_SWITCH",
  '"USER"',
  '"COMPANY"',
  '"ROLE"',
  '"PLAN"',
  '"ENVIRONMENT"',
  '"ROLLOUT"',
  '"GLOBAL"',
  '"DEFAULT"',
];
let previousIndex = -1;
for (const marker of precedence) {
  const markerIndex = service.indexOf(marker);
  assert.ok(markerIndex > previousIndex, `Precedência ausente ou fora de ordem: ${marker}`);
  previousIndex = markerIndex;
}
assert.match(service, /SHA-256/);
assert.match(service, /Feature flags não podem alterar pontuação/);

console.log("Feature flags: painel, manual, API, precedência, rollout e proteção de score validados.");
