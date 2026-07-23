import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { dirname, resolve } from "node:path";
import ts from "typescript";

const currentDir = dirname(fileURLToPath(import.meta.url));
const sourcePath = resolve(currentDir, "../src/lib/access-control.ts");
const source = await readFile(sourcePath, "utf8");
const output = ts.transpileModule(source, {
  compilerOptions: {
    module: ts.ModuleKind.ESNext,
    target: ts.ScriptTarget.ES2022,
  },
  fileName: sourcePath,
  reportDiagnostics: true,
});

const errors = (output.diagnostics ?? []).filter(
  (diagnostic) => diagnostic.category === ts.DiagnosticCategory.Error,
);
assert.equal(errors.length, 0, "access-control.ts deve transpilar sem erros");

const encoded = Buffer.from(output.outputText, "utf8").toString("base64");
const access = await import(`data:text/javascript;base64,${encoded}`);

const profiles = {
  admin: [access.ADMIN_ROLE],
  company: [access.EMPRESA_ROLE],
  author: [access.ASSESSMENT_EDITOR_ROLE],
  analyst: [access.RESULTS_ANALYST_ROLE],
  operator: [access.OPERATIONS_MANAGER_ROLE],
  partnerManager: [access.PARTNER_MANAGER_ROLE],
  specialist: [access.PARTNER_SPECIALIST_ROLE],
  unknown: ["UNKNOWN_ROLE"],
};

assert.equal(access.canAccessFrontendPath("/admin", profiles.admin), true);
assert.equal(access.canAccessFrontendPath("/admin", profiles.company), false);
assert.equal(access.canAccessFrontendPath("/dashboard", profiles.company), true);
assert.equal(access.canAccessFrontendPath("/avaliacoes/123", profiles.author), true);
assert.equal(access.canAccessFrontendPath("/nova/avaliacao", profiles.author), true);
assert.equal(access.canAccessFrontendPath("/results", profiles.analyst), true);
assert.equal(access.canAccessFrontendPath("/participacoes", profiles.operator), true);
assert.equal(access.canAccessFrontendPath("/integrations", profiles.operator), true);
assert.equal(access.canAccessFrontendPath("/parceiros", profiles.partnerManager), true);
assert.equal(access.canAccessFrontendPath("/avaliacoes/especialista", profiles.specialist), true);
assert.equal(access.canAccessFrontendPath("/competencias", profiles.specialist), true);
assert.equal(access.canAccessFrontendPath("/billing", profiles.specialist), false);
assert.equal(access.canAccessFrontendPath("/dashboard", profiles.specialist), false);
assert.equal(access.canAccessFrontendPath("/dashboard", profiles.unknown), false);
assert.equal(access.canAccessFrontendPath("/rota-sem-politica", profiles.company), false);
assert.equal(access.canAccessFrontendPath("/login?lang=en", []), true);

assert.equal(access.canPerformFrontendAction("assessment:create", profiles.author), true);
assert.equal(access.canPerformFrontendAction("assessment:publish", profiles.author), false);
assert.equal(access.canPerformFrontendAction("assessment:edit", profiles.specialist), true);
assert.equal(access.canPerformFrontendAction("assessment:publish", profiles.specialist), false);
assert.equal(access.canPerformFrontendAction("competency:manage", profiles.specialist), false);
assert.equal(access.canPerformFrontendAction("team:manage", [access.TEAM_MANAGER_ROLE]), true);
assert.equal(access.canPerformFrontendAction("partner:manage", profiles.partnerManager), true);
assert.equal(access.canPerformFrontendAction("billing:manage", profiles.company), true);
assert.equal(access.canPerformFrontendAction("billing:manage", profiles.unknown), false);

assert.equal(source.includes("MutationObserver"), false);
assert.equal(source.includes("textContent"), false);
assert.equal(source.includes("window.location.replace"), false);
assert.equal(source.includes("querySelectorAll"), false);

assert.equal(access.resolveDefaultAuthenticatedRoute(profiles.specialist), "/avaliacoes/especialista");
assert.equal(access.resolveDefaultAuthenticatedRoute(profiles.admin), "/admin");
assert.equal(access.resolveDefaultAuthenticatedRoute(profiles.unknown), "/dashboard");
assert.equal(access.resolveDefaultAuthenticatedRoute([]), "/login");

console.log("Políticas declarativas de rotas e ações validadas para todos os perfis.");
