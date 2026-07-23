import assert from "node:assert/strict";
import { readdirSync, readFileSync, statSync } from "node:fs";
import { dirname, join, relative, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDirectory = dirname(fileURLToPath(import.meta.url));
const frontendDirectory = resolve(scriptDirectory, "..");
const routesDirectory = join(frontendDirectory, "src", "routes");
const libraryDirectory = join(frontendDirectory, "src", "lib");
const manualRouteSource = readFileSync(join(routesDirectory, "manual.tsx"), "utf8");

const PUBLIC_ROUTES_WITHOUT_INTERNAL_MANUAL = new Set([
  "/",
  "/cookies",
  "/privacidade",
  "/termos",
]);

const REQUIRED_SECTIONS = [
  "purpose",
  "flow",
  "fields",
  "permissions",
  "states",
  "blocks",
  "examples",
  "shortcuts",
];

function listFiles(directory, predicate) {
  return readdirSync(directory).flatMap((entry) => {
    const absolutePath = join(directory, entry);
    if (statSync(absolutePath).isDirectory()) {
      return listFiles(absolutePath, predicate);
    }
    return predicate(absolutePath) ? [absolutePath] : [];
  });
}

function routeSample(routePath) {
  return routePath
    .replace(/\{\-\$[^}]+\}/g, "amostra")
    .replace(/\$[^/]+/g, "amostra")
    .replace(/\/$/, routePath === "/" ? "/" : "");
}

function isRedirectOnlyRoute(source) {
  return /\bredirect\s*\(/.test(source) && /\bbeforeLoad\b|\bthrow\b|\breturn\b/.test(source);
}

function extractRoute(filePath) {
  const source = readFileSync(filePath, "utf8");
  const match = source.match(/createFileRoute\(\s*["'`]([^"'`]+)["'`]\s*\)/);
  if (!match) {
    return undefined;
  }

  return {
    filePath,
    routePath: match[1],
    pathname: routeSample(match[1]),
    redirectOnly: isRedirectOnlyRoute(source),
  };
}

function compileMatcher(expression, manualId, filePath) {
  try {
    return Function(`"use strict"; return (${expression});`)();
  } catch (error) {
    throw new Error(
      `Não foi possível interpretar o matcher do manual ${manualId} em ${relative(frontendDirectory, filePath)}: ${error.message}`,
    );
  }
}

function extractManualDefinitions(filePath) {
  const source = readFileSync(filePath, "utf8");
  const definitions = [];
  const matcher = /id:\s*["']([^"']+)["']([\s\S]*?)matches:\s*([\s\S]*?),\s*\n\s*(?:};|},)/g;

  for (const match of source.matchAll(matcher)) {
    const [, id, body, matcherExpression] = match;
    const completeDefinition = `id: "${id}"${body}matches: ${matcherExpression}`;

    for (const section of REQUIRED_SECTIONS) {
      assert.match(
        completeDefinition,
        new RegExp(`\\b${section}\\s*:`),
        `Manual ${id} sem a seção obrigatória ${section} em ${relative(frontendDirectory, filePath)}`,
      );
      assert.doesNotMatch(
        completeDefinition,
        new RegExp(`\\b${section}\\s*:\\s*\\[\\s*\\]`),
        `Manual ${id} possui a seção vazia ${section} em ${relative(frontendDirectory, filePath)}`,
      );
    }

    definitions.push({
      id,
      filePath,
      matches: compileMatcher(matcherExpression.trim(), id, filePath),
    });
  }

  return definitions;
}

const routeFiles = listFiles(
  routesDirectory,
  (filePath) => filePath.endsWith(".tsx") && !filePath.endsWith("__root.tsx"),
);
const routes = routeFiles.map(extractRoute).filter(Boolean);

const manualFiles = listFiles(
  libraryDirectory,
  (filePath) => /screen-manual.*\.ts$/.test(filePath),
);
const manuals = manualFiles.flatMap(extractManualDefinitions);

assert.ok(routes.length > 0, "Nenhuma rota TanStack foi encontrada.");
assert.ok(manuals.length > 0, "Nenhuma definição de manual foi encontrada.");

const uncoveredRoutes = [];
const missingAnchors = [];

for (const route of routes) {
  if (route.redirectOnly || PUBLIC_ROUTES_WITHOUT_INTERNAL_MANUAL.has(route.routePath)) {
    continue;
  }

  const matchingManuals = manuals.filter(
    (manual) => manual.id !== "visao-geral" && manual.matches(route.pathname),
  );

  if (matchingManuals.length === 0) {
    uncoveredRoutes.push(
      `${route.routePath} (${relative(frontendDirectory, route.filePath)})`,
    );
    continue;
  }

  const hasValidAnchor = matchingManuals.some((manual) =>
    manualRouteSource.includes(`"${manual.id}"`),
  );
  if (!hasValidAnchor) {
    missingAnchors.push(
      `${route.routePath} -> ${matchingManuals.map((manual) => manual.id).join(", ")}`,
    );
  }
}

assert.deepEqual(
  uncoveredRoutes,
  [],
  `Rotas sem manual contextual específico:\n${uncoveredRoutes.join("\n")}`,
);
assert.deepEqual(
  missingAnchors,
  [],
  `Rotas cujo manual não possui destino na Central de manuais:\n${missingAnchors.join("\n")}`,
);

console.log(
  `Cobertura de manuais validada: ${routes.length} rotas inventariadas, ${manuals.length} definições verificadas e ${PUBLIC_ROUTES_WITHOUT_INTERNAL_MANUAL.size} exceções públicas explícitas.`,
);
