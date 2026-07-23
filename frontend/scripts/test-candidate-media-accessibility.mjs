import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const source = await readFile(new URL("../src/routes/candidato.tsx", import.meta.url), "utf8");

assert.match(source, /accessibleDescription\?: string \| null/);
assert.match(source, /alt=\{accessibleDescription\?\.trim\(\) \|\| ""\}/);
assert.match(source, /aria-label=\{audioLabel\}/);
assert.match(source, /accessibleDescription=\{currentNode\.descricaoAcessivel\}/);
assert.match(source, /accessibleDescription=\{option\.descricaoAcessivel\}/);
assert.match(source, /aria-label="Audiodescrição do cenário"/);
assert.match(source, /Audiodescrição da alternativa/);
assert.match(source, /audioLabel="Áudio do cenário"/);
assert.match(source, /Áudio da alternativa/);
assert.doesNotMatch(source, /alt="Mídia do atendimento"/);

console.log("Descrições acessíveis das mídias do candidato validadas.");
