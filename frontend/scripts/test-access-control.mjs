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

const specialist = [access.PARTNER_SPECIALIST_ROLE];
const company = [access.EMPRESA_ROLE];
const hybrid = [access.PARTNER_SPECIALIST_ROLE, access.EMPRESA_ROLE];

assert.equal(access.isRestrictedPartnerSpecialist(specialist), true);
assert.equal(access.isRestrictedPartnerSpecialist(company), false);
assert.equal(access.isRestrictedPartnerSpecialist(hybrid), false);

for (const path of [
  "/avaliacoes",
  "/competencias",
  "/nova/avaliacao",
  "/nova/personagem",
  "/nova/dialogo",
  "/nova/validador",
  "/nova/mapa",
]) {
  assert.equal(access.canAccessFrontendPath(path, specialist), true, `${path} deveria ser permitido`);
}

for (const path of [
  "/dashboard",
  "/nova/piloto",
  "/nova/governanca",
  "/integrations",
  "/billing",
]) {
  assert.equal(access.canAccessFrontendPath(path, specialist), false, `${path} deveria ser bloqueado`);
}

assert.equal(
  access.isPartnerSpecialistForbiddenAction("/nova/validador", "Ir para publicação →"),
  true,
);
assert.equal(
  access.isPartnerSpecialistForbiddenAction("/nova/personagem", "Criar rascunho"),
  true,
);
assert.equal(
  access.isPartnerSpecialistForbiddenAction("/nova/dialogo", "Criar rascunho para editar"),
  true,
);
assert.equal(
  access.isPartnerSpecialistForbiddenAction("/nova/avaliacao", "Adicionar e salvar"),
  true,
);
assert.equal(
  access.isPartnerSpecialistForbiddenAction("/nova/avaliacao", "Próximo: Cenário"),
  false,
);
assert.equal(
  access.isPartnerSpecialistForbiddenAction("/nova/validador", "Exportar diagnóstico"),
  false,
);

assert.equal(access.resolveDefaultAuthenticatedRoute(specialist), "/avaliacoes");
assert.equal(access.resolveDefaultAuthenticatedRoute([access.ADMIN_ROLE]), "/admin");

console.log("Controle de acesso do PARTNER_SPECIALIST validado.");
