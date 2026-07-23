import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDirectory = dirname(fileURLToPath(import.meta.url));
const frontendDirectory = resolve(scriptDirectory, "..");
const routeSource = readFileSync(
  join(frontendDirectory, "src", "routes", "results.$attemptId.tsx"),
  "utf8",
);
const apiSource = readFileSync(
  join(frontendDirectory, "src", "lib", "api", "result-executive-report.ts"),
  "utf8",
);
const manualSource = readFileSync(
  join(frontendDirectory, "src", "lib", "screen-manual-analysis-operations.ts"),
  "utf8",
);

assert.match(routeSource, /Visão executiva/);
assert.match(routeSource, /Dado observado/);
assert.match(routeSource, /Interpretação configurada pela empresa/);
assert.match(routeSource, /Sem recomendação automática/);
assert.match(routeSource, /Registrar roteiro na auditoria/);
assert.match(routeSource, /window\.print\(\)/);
assert.match(routeSource, /Decisão humana final/);
assert.match(apiSource, /\/executive-report/);
assert.match(apiSource, /\/interview-guide/);
assert.match(manualSource, /id: "resultado-executivo-entrevista"/);
assert.match(manualSource, /\^\\\/results\\\/\[\^\/\]\+\$/);

console.log("Contrato do relatório executivo e da entrevista estruturada validado.");
