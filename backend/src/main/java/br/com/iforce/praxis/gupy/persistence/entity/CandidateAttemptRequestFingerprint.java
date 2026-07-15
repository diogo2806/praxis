package br.com.iforce.praxis.gupy.persistence.entity;

import br.com.iforce.praxis.gupy.service.IdempotencyKeyHasher;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Locale;

/**
 * Gera uma impressão estável dos campos da requisição que definem o conteúdo
 * de uma criação idempotente de tentativa.
 */
final class CandidateAttemptRequestFingerprint {

    static final int VERSION = 1;

    private CandidateAttemptRequestFingerprint() {
    }

    static String from(CandidateAttemptEntity entity) {
        String canonical = String.join("\n",
                field("company_id", normalizeText(entity.getCompanyId(), false)),
                field("test_id", normalizeText(entity.getSimulationId(), false)),
                field("name", normalizeText(entity.getCandidateName(), false)),
                field("email", normalizeText(entity.getCandidateEmail(), true)),
                field("job_id", entity.getGupyJobId() == null ? "" : entity.getGupyJobId().toString()),
                field("callback_url", normalizeUri(entity.getCallbackUrl())),
                field("result_webhook_url", normalizeUri(entity.getResultWebhookUrl())),
                field("accommodation_time_multiplier", normalizeMultiplier(entity.getAccommodationTimeMultiplier()))
        );
        return IdempotencyKeyHasher.sha256Hex("v" + VERSION + "\n" + canonical);
    }

    private static String field(String name, String value) {
        return name + ":" + value.length() + ":" + value;
    }

    private static String normalizeText(String value, boolean lowercase) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        return lowercase ? normalized.toLowerCase(Locale.ROOT) : normalized;
    }

    private static String normalizeUri(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        try {
            return URI.create(value.trim()).normalize().toASCIIString();
        } catch (IllegalArgumentException exception) {
            return value.trim();
        }
    }

    private static String normalizeMultiplier(BigDecimal value) {
        BigDecimal normalized = value == null || value.compareTo(BigDecimal.ONE) < 0
                ? BigDecimal.ONE
                : value.min(BigDecimal.valueOf(9.99));
        return normalized.stripTrailingZeros().toPlainString();
    }
}