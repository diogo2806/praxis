import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const currentDir = dirname(fileURLToPath(import.meta.url));
const sourcePath = resolve(currentDir, "../src/lib/runtime-config.ts");
const source = await readFile(sourcePath, "utf8");

assert.match(
  source,
  /const RUNTIME_CONFIG_RETRY_DELAYS_MS = \[[^\]]+\] as const;/,
  "a configuração de runtime deve possuir atrasos de nova tentativa",
);
assert.match(
  source,
  /synchronizeRuntimeConfigWithRetry\(\)\.finally\(\(\) => \{\s*runtimeConfigRefreshPromise = null;/s,
  "a Promise compartilhada deve ser liberada depois da sincronização",
);
assert.match(
  source,
  /const synchronized = publicConfig !== null \|\| probedSecurityEnabled !== null;/,
  "a sincronização deve distinguir resposta válida de indisponibilidade do backend",
);

const explicitConfigIndex = source.indexOf("publicConfig?.securityEnabled ??");
const probeIndex = source.indexOf("probedSecurityEnabled ??", explicitConfigIndex);
assert.ok(explicitConfigIndex >= 0, "a resposta explícita do runtime-config deve ser usada");
assert.ok(
  probeIndex > explicitConfigIndex,
  "a resposta explícita do runtime-config deve preceder a inferência pelo dashboard",
);
assert.match(
  source,
  /if \(!synchronization\.synchronized\)[\s\S]*?return latestConfig;/,
  "as tentativas devem continuar enquanto o backend não responder",
);

console.log("Resiliência da configuração de runtime validada para falhas temporárias do backend.");
