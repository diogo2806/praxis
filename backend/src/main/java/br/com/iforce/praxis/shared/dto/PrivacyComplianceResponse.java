package br.com.iforce.praxis.shared.dto;

import java.util.List;

public record PrivacyComplianceResponse(
        List<LegalBasisDto> legalBases,
        int retentionDays,
        String retentionPolicy,
        ControllerContactDto controllerContact,
        String reviewChannel,
        String reviewSla,
        boolean automatedDecisionWithoutReviewAllowed
) {
    public record LegalBasisDto(
            String name,
            String description
    ) {
    }

    public record ControllerContactDto(
            String controllerName,
            String serviceEmail,
            String serviceUrl,
            String dataProtectionOfficerContact,
            String reviewInstructions
    ) {
    }
}
