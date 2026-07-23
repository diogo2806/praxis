import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const source = await readFile(
  new URL("../src/features/candidate/candidate-experience.tsx", import.meta.url),
  "utf8",
);

assert.match(source, /accessibleDescription\?: string \| null/);
assert.match(source, /alt=\{accessibleDescription\?\.trim\(\) \|\| ""\}/);
assert.match(source, /aria-label=\{audioLabel\}/);
assert.match(source, /accessibleDescription=\{currentNode\.descricaoAcessivel\}/);
assert.match(source, /accessibleDescription=\{option\.descricaoAcessivel\}/);
assert.match(source, /aria-label=\{copy\.media\.scenarioAudioDescription\}/);
assert.match(source, /copy\.media\.optionAudioDescription\(optionLabel\)/);
assert.match(source, /audioLabel=\{copy\.media\.scenarioAudio\}/);
assert.match(source, /copy\.media\.optionAudio\(optionLabel\)/);
assert.doesNotMatch(source, /alt="Mídia do atendimento"/);

console.log("Descrições acessíveis das mídias do candidato validadas.");
