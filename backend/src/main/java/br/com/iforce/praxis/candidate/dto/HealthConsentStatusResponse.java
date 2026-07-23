package br.com.iforce.praxis.candidate.dto;

/** Estado público mínimo do consentimento específico da vertical de saúde. */
public record HealthConsentStatusResponse(
        boolean healthVertical,
        boolean required,
        boolean valid,
        String noticeVersion
) {
}
