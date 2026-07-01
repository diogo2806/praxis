package br.com.iforce.praxis.shared.security;

import br.com.iforce.praxis.marketplace.persistence.repository.MarketplaceProfessionalRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("tenantSecurity")
public class TenantSecurity {

    private final MarketplaceProfessionalRepository professionalRepository;

    public TenantSecurity(MarketplaceProfessionalRepository professionalRepository) {
        this.professionalRepository = professionalRepository;
    }

    public boolean isOwnerProfessional(Long professionalId) {
        Long currentUserId = currentUserId();
        if (currentUserId == null || professionalId == null) {
            return false;
        }
        return professionalRepository.findByUserId(currentUserId)
                .map(professional -> professionalId.equals(professional.getId()))
                .orElse(false);
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();
        if (!(principal instanceof String value) || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
