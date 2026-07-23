import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const frontendRoot = new URL("../", import.meta.url);

async function read(relativePath) {
  return readFile(new URL(relativePath, frontendRoot), "utf8");
}

const [experience, route, catalog, translationIndex] = await Promise.all([
  read("src/features/candidate/candidate-experience.tsx"),
  read("src/routes/candidato.tsx"),
  read("src/lib/translations/candidate-execution.ts"),
  read("src/lib/translations/index.ts"),
]);

assert.match(experience, /const \{ t \} = useLanguage\(\)/);
assert.match(experience, /const copy = t\.candidateExecution/);
assert.match(route, /t\.candidateExecution\.entry/);
assert.match(route, /export \{ CandidateExperience \}/);
assert.match(translationIndex, /candidateExecution: candidateExecutionTranslations\[language\]/);

assert.match(catalog, /"pt-BR": \{/);
assert.match(catalog, /\ben: \{/);
assert.match(catalog, /"es-MX": \{/);
assert.match(catalog, /Confirmar resposta final/);
assert.match(catalog, /Confirm final answer/);
assert.match(catalog, /Confirmar respuesta final/);
assert.match(catalog, /Request human review/);
assert.match(catalog, /Solicitar revisión humana/);

for (const hardcodedText of [
  "Carregando",
  "Alto contraste",
  "Confirmar resposta final",
  "Solicitar revisão humana",
  "Uso dos seus dados nesta atividade",
  "Não foi possível carregar a avaliação",
]) {
  assert.equal(
    experience.includes(`>${hardcodedText}<`) || experience.includes(`"${hardcodedText}"`),
    false,
    `Texto operacional fixo encontrado na execução: ${hardcodedText}`,
  );
}

assert.match(experience, /\{currentNode\.descricao\}/);
assert.match(experience, /\{option\.texto\}/);
assert.doesNotMatch(experience, /translate\(currentNode\.descricao/);
assert.doesNotMatch(experience, /translate\(option\.texto/);

console.log("Internacionalização integral da execução do candidato validada.");
