import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const frontendRoot = new URL("../", import.meta.url);
const repositoryRoot = new URL("../../", import.meta.url);

async function readFrontend(relativePath) {
  return readFile(new URL(relativePath, frontendRoot), "utf8");
}

async function readBackend(relativePath) {
  return readFile(new URL(relativePath, repositoryRoot), "utf8");
}

const [candidateClient, consentClient, publicExports, controller, service, entity, migration] =
  await Promise.all([
    readFrontend("src/lib/api/candidate-attempt-public.ts"),
    readFrontend("src/lib/api/candidate-health-consent-public.ts"),
    readFrontend("src/lib/api/praxis.ts"),
    readBackend("backend/src/main/java/br/com/iforce/praxis/candidate/controller/CandidateAttemptController.java"),
    readBackend("backend/src/main/java/br/com/iforce/praxis/candidate/service/CandidateHealthConsentService.java"),
    readBackend("backend/src/main/java/br/com/iforce/praxis/gupy/persistence/entity/CandidateAttemptEntity.java"),
    readBackend("backend/src/main/resources/db/migration/V1019__persist_health_consent.sql"),
  ]);

assert.match(candidateClient, /HEALTH_CONSENT_REQUIRED_STATUS = 428/);
assert.match(candidateClient, /healthConsentRequiredAttempt/);
assert.match(candidateClient, /verticalSaude: true/);
assert.match(consentClient, /await recordHealthConsentLegacy/);
assert.match(consentClient, /window\.location\.reload\(\)/);
assert.match(publicExports, /recordHealthConsent.*candidate-health-consent-public/);

const controllerGuards = controller.match(/candidateHealthConsentService\.assertConsentGranted\(attemptToken\)/g) ?? [];
assert.equal(controllerGuards.length, 2, "Leitura e resposta devem exigir consentimento de saúde.");
assert.match(service, /HttpStatus\.PRECONDITION_REQUIRED/);
assert.match(service, /setHealthConsentRecordedAt/);
assert.match(service, /setHealthConsentVersion/);
assert.match(service, /getHealthConsentRevokedAt\(\) == null/);
assert.match(entity, /health_consent_recorded_at/);
assert.match(entity, /health_consent_version/);
assert.match(entity, /health_consent_subject_type/);
assert.match(entity, /health_consent_revoked_at/);
assert.match(migration, /ALTER TABLE candidate_attempts/);

console.log("Barreira persistida de consentimento de saúde validada.");
