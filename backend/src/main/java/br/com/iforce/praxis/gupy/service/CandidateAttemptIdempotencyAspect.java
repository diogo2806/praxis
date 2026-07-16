package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.gupy.dto.CreateCandidateRequest;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.shared.integration.IntegrationEmpresaContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Aspect
@Component
public class CandidateAttemptIdempotencyAspect {

    private static final int LEGACY_FINGERPRINT_VERSION = 1;
    private static final int FINGERPRINT_VERSION = 2;
    private static final Set<AttemptStatus> RETESTABLE_STATUSES = Set.of(
            AttemptStatus.COMPLETED,
            AttemptStatus.ABANDONED,
            AttemptStatus.EXPIRED
    );

    private final CandidateAttemptRepository candidateAttemptRepository;

    public CandidateAttemptIdempotencyAspect(CandidateAttemptRepository candidateAttemptRepository) {
        this.candidateAttemptRepository = candidateAttemptRepository;
    }

    @Around("execution(* br.com.iforce.praxis.gupy.service.CandidateAttemptService.createOrReuse(..))"
            + " && args(request, empresaContext)")
    @Transactional
    public Object enforceEquivalentRetry(
            ProceedingJoinPoint joinPoint,
            CreateCandidateRequest request,
            IntegrationEmpresaContext empresaContext
    ) throws Throwable {
        String empresaId = empresaContext.empresaId();
        String initialSource = CandidateAttemptIdempotencyKeyFactory.initialSource(request, empresaContext);
        String serviceSource = CandidateAttemptIdempotencyKeyFactory.serviceSource(request, empresaContext);
        String idempotencyKey = CandidateAttemptIdempotencyKeyFactory.currentKey(request, empresaContext);
        String fingerprint = fingerprint(request);

        CandidateAttemptEntity existing = candidateAttemptRepository
                .findByEmpresaIdAndIdempotencyKey(empresaId, idempotencyKey)
                .orElse(null);

        if (existing == null && CandidateAttemptIdempotencyKeyFactory.isGupyRetest(request, empresaContext)) {
            assertRetestAllowed(request, empresaContext);
        }
        if (existing != null) {
            assertEquivalent(existing, request, fingerprint);
        }

        Object response;
        try (CandidateAttemptIdempotencyScope ignored =
                     CandidateAttemptIdempotencyScope.open(serviceSource, idempotencyKey)) {
            response = joinPoint.proceed();
        }

        CandidateAttemptEntity saved = candidateAttemptRepository
                .findByEmpresaIdAndIdempotencyKey(empresaId, idempotencyKey)
                .orElseThrow(() -> new IllegalStateException("Tentativa idempotente não foi persistida."));
        if (saved.getRequestFingerprint() == null
                || !Integer.valueOf(FINGERPRINT_VERSION).equals(saved.getRequestFingerprintVersion())) {
            saved.setRequestFingerprint(fingerprint);
            saved.setRequestFingerprintVersion(FINGERPRINT_VERSION);
            candidateAttemptRepository.save(saved);
        }
        return response;
    }

    private void assertRetestAllowed(CreateCandidateRequest request, IntegrationEmpresaContext context) {
        CandidateAttemptEntity previousAttempt = candidateAttemptRepository
                .findByEmpresaIdAndIdempotencyKey(
                        context.empresaId(),
                        CandidateAttemptIdempotencyKeyFactory.initialKey(request, context)
                )
                .orElseThrow(CandidateAttemptIdempotencyAspect::retestWithoutPreviousAttempt);

        if (!RETESTABLE_STATUSES.contains(previousAttempt.getStatus())) {
            throw retestBeforeTerminalState();
        }
    }

    private static void assertEquivalent(
            CandidateAttemptEntity existing,
            CreateCandidateRequest request,
            String currentFingerprint
    ) {
        if (existing.getRequestFingerprint() == null) {
            if (!matchesLegacySnapshot(existing, request)) {
                throw conflict();
            }
            return;
        }

        Integer version = existing.getRequestFingerprintVersion();
        String expectedFingerprint;
        if (Integer.valueOf(FINGERPRINT_VERSION).equals(version)) {
            expectedFingerprint = currentFingerprint;
        } else if (Integer.valueOf(LEGACY_FINGERPRINT_VERSION).equals(version)) {
            expectedFingerprint = legacyFingerprint(request);
        } else {
            throw conflict();
        }

        if (!expectedFingerprint.equals(existing.getRequestFingerprint())) {
            throw conflict();
        }
    }

    private static String fingerprint(CreateCandidateRequest request) {
        String canonical = String.join("\n",
                field("company_id", normalized(request.companyId(), false)),
                field("document_id", normalized(request.documentId(), false)),
                field("test_id", normalized(request.testId(), false)),
                field("name", normalized(request.candidateName(), false)),
                field("email", normalized(request.candidateEmail(), true)),
                field("job_id", request.jobId() == null ? "" : request.jobId().toString()),
                field("callback_url", normalized(request.callbackUrl())),
                field("result_webhook_url", normalized(request.resultWebhookUrl())),
                field("accommodation_time_multiplier", normalized(request.accommodationTimeMultiplier())),
                field("candidate_type", request.candidateType() == null ? "" : request.candidateType().value()));
        return IdempotencyKeyHasher.sha256Hex("v" + FINGERPRINT_VERSION + "\n" + canonical);
    }

    private static String legacyFingerprint(CreateCandidateRequest request) {
        String canonical = String.join("\n",
                field("company_id", normalized(request.companyId(), false)),
                field("document_id", normalized(request.documentId(), false)),
                field("test_id", normalized(request.testId(), false)),
                field("name", normalized(request.candidateName(), false)),
                field("email", normalized(request.candidateEmail(), true)),
                field("job_id", request.jobId() == null ? "" : request.jobId().toString()),
                field("callback_url", normalized(request.callbackUrl())),
                field("result_webhook_url", normalized(request.resultWebhookUrl())),
                field("accommodation_time_multiplier", normalized(request.accommodationTimeMultiplier())),
                field("candidate_type", request.candidateType() == null ? "" : request.candidateType().value()),
                field("previous_result", request.previousResult() == null ? "" : request.previousResult().value()));
        return IdempotencyKeyHasher.sha256Hex("v" + LEGACY_FINGERPRINT_VERSION + "\n" + canonical);
    }

    private static boolean matchesLegacySnapshot(CandidateAttemptEntity entity, CreateCandidateRequest request) {
        return normalized(entity.getCompanyId(), false).equals(normalized(request.companyId(), false))
                && normalized(entity.getSimulationId(), false).equals(normalized(request.testId(), false))
                && normalized(entity.getCandidateName(), false).equals(normalized(request.candidateName(), false))
                && normalized(entity.getCandidateEmail(), true).equals(normalized(request.candidateEmail(), true))
                && Objects.equals(entity.getGupyJobId(), request.jobId())
                && normalized(entity.getCallbackUrl()).equals(normalized(request.callbackUrl()))
                && normalized(entity.getResultWebhookUrl()).equals(normalized(request.resultWebhookUrl()))
                && normalized(entity.getAccommodationTimeMultiplier())
                .equals(normalized(request.accommodationTimeMultiplier()));
    }

    private static String field(String name, String value) {
        return name + ":" + value.length() + ":" + value;
    }

    private static String normalized(Object value, boolean lowercase) {
        if (value == null) {
            return "";
        }
        String result = value.toString().trim();
        return lowercase ? result.toLowerCase(Locale.ROOT) : result;
    }

    private static String normalized(URI value) {
        return value == null ? "" : value.normalize().toASCIIString();
    }

    private static String normalized(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        try {
            return URI.create(value.trim()).normalize().toASCIIString();
        } catch (IllegalArgumentException exception) {
            return value.trim();
        }
    }

    private static String normalized(BigDecimal value) {
        BigDecimal normalized = value == null || value.compareTo(BigDecimal.ONE) < 0
                ? BigDecimal.ONE
                : value.min(BigDecimal.valueOf(9.99));
        return normalized.stripTrailingZeros().toPlainString();
    }

    private static ResponseStatusException conflict() {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                "A chave de idempotência já foi usada com dados diferentes."
        );
    }

    private static ResponseStatusException retestWithoutPreviousAttempt() {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Reteste Gupy exige uma tentativa anterior para a mesma empresa, pessoa candidata, teste e vaga."
        );
    }

    private static ResponseStatusException retestBeforeTerminalState() {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Reteste Gupy permitido somente após tentativa concluída, abandonada ou expirada."
        );
    }
}
