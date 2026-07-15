package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.gupy.dto.CreateCandidateRequest;
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

/**
 * Garante que a mesma identidade idempotente só possa ser repetida com o
 * mesmo conteúdo canônico da solicitação.
 */
@Aspect
@Component
public class CandidateAttemptIdempotencyAspect {

    private static final int FINGERPRINT_VERSION = 1;

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
        String idempotencyKey = idempotencyKey(request, empresaContext);
        String fingerprint = fingerprint(request);

        CandidateAttemptEntity existing = candidateAttemptRepository
                .findByEmpresaIdAndIdempotencyKey(empresaId, idempotencyKey)
                .orElse(null);
        if (existing != null && existing.getRequestFingerprint() != null) {
            if (!fingerprint.equals(existing.getRequestFingerprint())
                    || !Integer.valueOf(FINGERPRINT_VERSION).equals(existing.getRequestFingerprintVersion())) {
                throw conflict();
            }
        } else if (existing != null && !matchesLegacySnapshot(existing, request)) {
            throw conflict();
        }

        Object response = joinPoint.proceed();

        CandidateAttemptEntity saved = candidateAttemptRepository
                .findByEmpresaIdAndIdempotencyKey(empresaId, idempotencyKey)
                .orElseThrow(() -> new IllegalStateException("Tentativa idempotente não foi persistida."));
        if (saved.getRequestFingerprint() == null) {
            saved.setRequestFingerprint(fingerprint);
            saved.setRequestFingerprintVersion(FINGERPRINT_VERSION);
            candidateAttemptRepository.save(saved);
        }
        return response;
    }

    private static String idempotencyKey(
            CreateCandidateRequest request,
            IntegrationEmpresaContext empresaContext
    ) {
        String source = empresaContext.empresaId() + "|" + empresaContext.companyId()
                + "|" + request.documentId() + "|" + request.testId();
        if (request.jobId() != null) {
            source += "|" + request.jobId();
        }
        return IdempotencyKeyHasher.sha256Hex(source);
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
                field("candidate_type", request.candidateType() == null ? "" : request.candidateType().value()),
                field("previous_result", request.previousResult() == null ? "" : request.previousResult().value())
        );
        return IdempotencyKeyHasher.sha256Hex("v" + FINGERPRINT_VERSION + "\n" + canonical);
    }

    private static boolean matchesLegacySnapshot(CandidateAttemptEntity entity, CreateCandidateRequest request) {
        return normalized(entity.getCompanyId(), false).equals(normalized(request.companyId(), false))
                && normalized(entity.getSimulationId(), false).equals(normalized(request.testId(), false))
                && normalized(entity.getCandidateName(), false).equals(normalized(request.candidateName(), false))
                && normalized(entity.getCandidateEmail(), true).equals(normalized(request.candidateEmail(), true))
                && java.util.Objects.equals(entity.getGupyJobId(), request.jobId())
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
}