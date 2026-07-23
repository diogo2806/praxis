import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import path from "node:path";

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const frontendDirectory = path.resolve(scriptDirectory, "..");

const commercialSource = await readFile(
  path.join(frontendDirectory, "src/components/landing-commercial.tsx"),
  "utf8",
);
const landingSource = await readFile(path.join(frontendDirectory, "src/routes/index.tsx"), "utf8");
const rootSource = await readFile(path.join(frontendDirectory, "src/routes/__root.tsx"), "utf8");

const expectedTiers = [
  ["100", "54,90", "5.490,00"],
  ["300", "49,90", "14.970,00"],
  ["1_000", "44,90", "44.900,00"],
  ["3_000", "39,90", "119.700,00"],
];

for (const [quantity, unitPrice, annualTotal] of expectedTiers) {
  assert.match(
    commercialSource,
    new RegExp(`annualAssessments: ${quantity}[^\\n]+unitPrice: "${unitPrice.replace(".", "\\.")}"[^\\n]+annualTotal: "${annualTotal.replace(".", "\\.")}"`),
    `Pacote anual ${quantity} não corresponde à tabela comercial esperada`,
  );
}

assert.match(
  commercialSource,
  /A assinatura Profissional é anual\. O pacote completo entra no saldo após a confirmação do pagamento e pode ser usado durante os 12 meses\./,
  "A descrição do ciclo anual deve estar declarada na fonte tipada da landing",
);
assert.match(
  commercialSource,
  /aria-label="Pacotes anuais por volume"/,
  "A tabela de preços deve possuir nome acessível",
);
assert.match(
  landingSource,
  /<LandingPricingSection \/>/,
  "A landing deve renderizar o componente React de preços",
);
assert.match(
  landingSource,
  /<LandingFaqSection \/>/,
  "A landing deve renderizar o componente React de FAQ",
);
assert.doesNotMatch(
  rootSource,
  /landingPricingBootstrap|data-praxis-pricing-version/,
  "O shell global não pode injetar o bootstrap imperativo de preços",
);
assert.doesNotMatch(
  commercialSource,
  /innerHTML|getElementById|querySelector|MutationObserver|setTimeout/,
  "Preços e FAQ não podem depender de manipulação imperativa do DOM",
);

console.log("Landing comercial validada: preços anuais, ciclo, FAQ e ausência do bootstrap imperativo.");
