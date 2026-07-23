import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import ts from "typescript";

const currentDir = dirname(fileURLToPath(import.meta.url));
const sourcePath = resolve(currentDir, "../src/lib/app-shell-context.ts");
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
assert.equal(errors.length, 0, "app-shell-context.ts deve transpilar sem erros");

const encoded = Buffer.from(output.outputText, "utf8").toString("base64");
const context = await import(`data:text/javascript;base64,${encoded}`);

for (const pathname of ["/avaliacoes", "/avaliacoes/123"]) {
  assert.deepEqual(context.resolveAppShellContext(pathname, false), {
    section: "workspace",
    label: "assessments",
  });
  assert.equal(context.resolveAppShellGoalKey(pathname, false), "assessments");
  assert.equal(context.resolveAppShellRouteDataKey(pathname), "avaliacoes");
}

assert.deepEqual(context.resolveAppShellContext("/avaliacoes/especialista", true), {
  section: "specialistArea",
  label: "home",
});
assert.equal(context.resolveAppShellGoalKey("/avaliacoes/especialista", true), "specialist");
assert.equal(context.resolveAppShellRouteDataKey("/avaliacoes/especialista"), "especialista");

for (const pathname of [
  "/nova/avaliacao",
  "/nova/personagem",
  "/nova/validador",
  "/nova/governanca",
]) {
  assert.deepEqual(context.resolveAppShellContext(pathname, false), {
    section: "workspace",
    label: "assessments",
  });
  assert.deepEqual(context.resolveAppShellContext(pathname, true), {
    section: "specialistArea",
    label: "assessments",
  });
  assert.equal(context.resolveAppShellGoalKey(pathname, false), "assessments");
}

assert.equal(source.includes("/avalicoes"), false, "o prefixo incorreto não pode reaparecer");

console.log("Contexto das rotas do AppShell validado.");
