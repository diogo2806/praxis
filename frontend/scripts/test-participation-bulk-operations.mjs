import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDirectory = dirname(fileURLToPath(import.meta.url));
const frontendDirectory = resolve(scriptDirectory, "..");
const repositoryDirectory = resolve(frontendDirectory, "..");

const route = readFileSync(
  join(frontendDirectory, "src", "routes", "participacoes.operacoes-em-lote.tsx"),
  "utf8",
);
const mainParticipations = readFileSync(
  join(frontendDirectory, "src", "routes", "participacoes.tsx"),
  "utf8",
);
const api = readFileSync(
  join(frontendDirectory, "src", "lib", "api", "participation-operations.ts"),
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
    "participationops",
    "service",
    "ParticipationBulkService.java",
  ),
  "utf8",
);
const worker = readFileSync(
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
    "participationops",
    "service",
    "ParticipationBulkWorker.java",
  ),
  "utf8",
);

assert.match(route, /createFileRoute\("\/participacoes\/operacoes-em-lote"\)/);
assert.match(route, /Selecionar página/);
assert.match(route, /Todos do filtro/);
assert.match(route, /Revisar impacto/);
assert.match(route, /Confirmar e processar/);
assert.match(route, /Relatório CSV/);
assert.match(mainParticipations, /\/participacoes\/operacoes-em-lote/);
assert.match(api, /bulk\/preview/);
assert.match(api, /idempotencyKey/);
assert.match(manual, /id:\s*"participacoes-operacoes-em-lote"/);
assert.match(manual, /Finalidade|purpose:/);
assert.match(service, /MAX_EXPLICIT_SELECTION = 500/);
assert.match(service, /MAX_FILTER_SELECTION = 5_000/);
assert.match(service, /findByEmpresaIdAndIdempotencyKey/);
assert.match(service, /Cancelamento em lote exige justificativa/);
assert.match(worker, /@Async/);
assert.doesNotMatch(worker, /HUMAN_DECISION/);

console.log("Operações em lote: seleção, prévia, idempotência, processamento, relatório e manual validados.");
