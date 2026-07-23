import assert from "node:assert/strict";
import { readdir, readFile } from "node:fs/promises";
import { dirname, extname, relative, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const currentDir = dirname(fileURLToPath(import.meta.url));
const frontendDir = resolve(currentDir, "..");
const sourceDir = resolve(frontendDir, "src");
const legacyPath = resolve(sourceDir, "lib/api/praxis-legacy.ts");
const contractPath = resolve(sourceDir, "lib/api/praxis-contract.ts");
const corruptedNames = ["plainTextDescriptioetapa", "timeJustificatioetapa"];

async function collectSourceFiles(directory) {
  const entries = await readdir(directory, { withFileTypes: true });
  const files = [];

  for (const entry of entries) {
    const path = resolve(directory, entry.name);
    if (entry.isDirectory()) {
      files.push(...(await collectSourceFiles(path)));
      continue;
    }

    if ([".ts", ".tsx"].includes(extname(entry.name))) files.push(path);
  }

  return files;
}

function interfaceBody(source, interfaceName) {
  const match = source.match(
    new RegExp(`export interface ${interfaceName} \\{([\\s\\S]*?)\\n\\}`, "m"),
  );
  assert.ok(match, `Interface ${interfaceName} deve existir no contrato canônico`);
  return match[1];
}

const sourceFiles = await collectSourceFiles(sourceDir);
for (const file of sourceFiles) {
  const source = await readFile(file, "utf8");
  const relativePath = relative(sourceDir, file).replaceAll("\\", "/");

  for (const corruptedName of corruptedNames) {
    assert.equal(
      source.includes(corruptedName),
      false,
      `${relativePath} não pode referenciar o nome corrompido ${corruptedName}`,
    );
  }

  if (!relativePath.startsWith("lib/api/")) {
    assert.equal(
      /(?:from\s+|import\s*\()\s*["'][^"']*praxis-legacy(?:\.ts)?["']/.test(source),
      false,
      `${relativePath} deve consumir a fachada pública @/lib/api/praxis`,
    );
  }
}

const legacySource = await readFile(legacyPath, "utf8");
const contractSource = await readFile(contractPath, "utf8");

for (const interfaceName of ["SimulationVersionOptionResponse", "SimulationVersionNodeResponse"]) {
  assert.match(
    interfaceBody(legacySource, interfaceName),
    /plainTextDescription: string \| null;/,
    `${interfaceName} deve desserializar plainTextDescription`,
  );
}

for (const interfaceName of ["CreateNodeRequest", "UpdateNodeRequest"]) {
  const body = interfaceBody(legacySource, interfaceName);
  assert.match(
    body,
    /timeJustification\?: string \| null;/,
    `${interfaceName} deve serializar timeJustification`,
  );
  assert.match(
    body,
    /plainTextDescription\?: string \| null;/,
    `${interfaceName} deve serializar plainTextDescription`,
  );
}

for (const interfaceName of ["CreateOptionRequest", "UpdateOptionRequest"]) {
  assert.match(
    interfaceBody(legacySource, interfaceName),
    /plainTextDescription\?: string \| null;/,
    `${interfaceName} deve serializar plainTextDescription`,
  );
}

assert.equal(
  contractSource.includes("as unknown as SimulationVersionDetailResponse"),
  false,
  "A fachada não deve mascarar incompatibilidade do contrato de versão",
);
assert.match(
  contractSource,
  /type CreateNodeRequest as CanonicalCreateNodeRequest/,
  "A fachada deve apontar para o contrato canônico",
);

const nodePayload = {
  id: "node-1",
  plainTextDescription: "Descrição simplificada da etapa",
  options: [
    {
      id: "option-1",
      plainTextDescription: "Descrição simplificada da alternativa",
    },
  ],
};
const nodeSerialized = JSON.stringify(nodePayload);
const nodeDeserialized = JSON.parse(nodeSerialized);
assert.equal(nodeDeserialized.plainTextDescription, nodePayload.plainTextDescription);
assert.equal(
  nodeDeserialized.options[0].plainTextDescription,
  nodePayload.options[0].plainTextDescription,
);

const nodeRequest = {
  clientMessage: "Mensagem da etapa",
  timeJustification: "Tempo definido pela complexidade do cenário",
  plainTextDescription: "Versão simplificada da mensagem",
};
const requestSerialized = JSON.stringify(nodeRequest);
const requestDeserialized = JSON.parse(requestSerialized);
assert.equal(requestDeserialized.timeJustification, nodeRequest.timeJustification);
assert.equal(requestDeserialized.plainTextDescription, nodeRequest.plainTextDescription);

for (const corruptedName of corruptedNames) {
  assert.equal(nodeSerialized.includes(corruptedName), false);
  assert.equal(requestSerialized.includes(corruptedName), false);
}

console.log("Contratos canônicos, fronteira de importação e round-trip JSON validados.");
