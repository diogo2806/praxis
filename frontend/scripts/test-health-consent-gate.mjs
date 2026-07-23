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

const [candidateClient, candidateExperience, publicExports, controller, service, entity, migration] =
  await Promise.all([
    readFrontend("src/lib/api/candidate-attempt-public.ts"),
    readFrontend("src/features/candidate/candidate-experience.tsx"),
    readFrontend("src/lib/api/praxis.ts"),
    readBackend("backend/src/main/java/br/com/iforce/praxis/candidate/controller/CandidateAttemptController.java"),
    readBackend("backend/src/main/java/br/com/iforce/praxis/candidate/service/CandidateHealthConsentService.java"),
    readBackend("backend/src/main/java/br/com/iforce/praxis/gupy/persistence/entity/CandidateAttemptEntity.java"),
    readBackend("backend/src/main/resources/db/migration/V1020__complete_health_consent_state.sql"),
  ]);

assert.ok(
  candidateClient.indexOf("getHealthConsentStatus(token)") <
    candidateClient.indexOf("apiRequest<LegacyCandidateAttemptResponse>"),
  "O estado persistido deve ser consultado antes de requisitar a etapa",
);
assert.match(candidateClient, /consentStatus\.required && !consentStatus\.valid/);
assert.match(candidateClient, /healthConsentValid: consentStatus\.valid/);
assert.match(candidateClient, /JSON\.stringify\(\{ version, onBehalfOfMinor \}\)/);
assert.match(candidateClient, /method: "DELETE"/);
assert.match(candidateExperience, /!attempt\?\.healthConsentValid/);
assert.match(candidateExperience, /const refreshedAttempt = await attemptQuery\.refetch\(\)/);
assert.match(candidateExperience, /setLiveAttempt\(refreshedAttempt\.data\)/);
assert.match(candidateExperience, /recordHealthConsent\(token, onBehalfOfMinor, noticeVersion\)/);
assert.match(publicExports, /getHealthConsentStatus/);
assert.match(publicExports, /revokeHealthConsent/);
assert.equal((controller.match(/candidateHealthConsentService\.assertConsentGranted/g) ?? []).length, 2);
assert.match(controller, /@DeleteMapping\("\/\{attemptToken\}\/health-consent"\)/);
assert.match(service, /currentNoticeVersion\.equals\(requestedVersion\)/);
assert.match(service, /hasValidCurrentConsent\(attempt\)/);
assert.match(service, /HEALTH_CONSENT_REVOKED/);
assert.match(entity, /healthConsentSource/);
assert.match(migration, /ck_candidate_attempt_health_consent_complete/);

console.log("Contrato completo de consentimento de saúde validado.");
