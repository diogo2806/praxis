package br.com.iforce.praxis.marketplace.dto;

import br.com.iforce.praxis.marketplace.model.ProfessionalVerificationStatus;

import java.math.BigDecimal;
import java.util.Set;

public record ProfessionalPublicProfileResponse(
        Long id,
        String displayName,
        String bio,
        Set<String> specialties,
        String linkedinUrl,
        ProfessionalVerificationStatus verificationStatus,
        BigDecimal averageRating,
        int totalReviews,
        int totalSales,
        boolean mercadoPagoConnected
) {
}
