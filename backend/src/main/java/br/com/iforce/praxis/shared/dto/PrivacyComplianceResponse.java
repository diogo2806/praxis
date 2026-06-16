package br.com.iforce.praxis.shared.dto;

import java.util.List;

public record PrivacyComplianceResponse(
        List<LegalBasisDto> legalBases,
        int retentionDays,
        String retentionPolicy,
        String reviewChannel,
        String reviewSla,
        boolean automatedDecisionWithoutReviewAllowed
) {
    public record LegalBasisDto(
            String name,
            String description
    ) {
    }
}
