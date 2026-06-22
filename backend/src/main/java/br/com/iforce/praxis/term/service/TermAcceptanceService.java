package br.com.iforce.praxis.term.service;

import br.com.iforce.praxis.auth.service.CurrentTenantService;
import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.term.dto.AcceptTermRequest;
import br.com.iforce.praxis.term.dto.TermAcceptanceStatusResponse;
import br.com.iforce.praxis.term.dto.TermResponse;
import br.com.iforce.praxis.term.model.HealthUseTerm;
import br.com.iforce.praxis.term.model.ResponsibilityTerm;
import br.com.iforce.praxis.term.persistence.entity.TermAcceptanceEntity;
import br.com.iforce.praxis.term.persistence.repository.TermAcceptanceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

/**
 * Termos aceitos pelo recrutador: termo de responsabilidade (REQ-L5) e termo de uso em saúde
 * (Minuta C). Expõe o texto/versão corrente e registra o aceite (quem, quando, qual versão) de
 * forma insert-only, para comprovar que o cliente assumiu as responsabilidades aplicáveis.
 */
@Service
public class TermAcceptanceService {

    /** Descritor de um termo: tipo e versão correntes exibidos ao usuário. */
    private record TermDescriptor(String type, String version, String text) {
    }

    private static final TermDescriptor RESPONSIBILITY =
            new TermDescriptor(ResponsibilityTerm.TYPE, ResponsibilityTerm.VERSION, ResponsibilityTerm.TEXT);
    private static final TermDescriptor HEALTH_USE =
            new TermDescriptor(HealthUseTerm.TYPE, HealthUseTerm.VERSION, HealthUseTerm.TEXT);

    private final TermAcceptanceRepository termAcceptanceRepository;
    private final CurrentTenantService currentTenantService;
    private final CurrentUserService currentUserService;

    public TermAcceptanceService(
            TermAcceptanceRepository termAcceptanceRepository,
            CurrentTenantService currentTenantService,
            CurrentUserService currentUserService
    ) {
        this.termAcceptanceRepository = termAcceptanceRepository;
        this.currentTenantService = currentTenantService;
        this.currentUserService = currentUserService;
    }

    public TermResponse responsibilityTerm() {
        return termResponse(RESPONSIBILITY);
    }

    @Transactional(readOnly = true)
    public TermAcceptanceStatusResponse responsibilityStatus() {
        return statusFor(RESPONSIBILITY);
    }

    @Transactional
    public TermAcceptanceStatusResponse acceptResponsibility(AcceptTermRequest request) {
        return accept(RESPONSIBILITY, request);
    }

    public TermResponse healthUseTerm() {
        return termResponse(HEALTH_USE);
    }

    @Transactional(readOnly = true)
    public TermAcceptanceStatusResponse healthUseStatus() {
        return statusFor(HEALTH_USE);
    }

    @Transactional
    public TermAcceptanceStatusResponse acceptHealthUse(AcceptTermRequest request) {
        return accept(HEALTH_USE, request);
    }

    /**
     * Indica se o usuário atual aceitou a versão corrente do termo de uso em saúde. Usado como
     * trava de publicação quando o tenant opera na vertical de saúde.
     */
    @Transactional(readOnly = true)
    public boolean isHealthUseAcceptedByCurrentUser() {
        String tenantId = currentTenantService.requiredTenantId();
        String userId = currentUserService.requiredUserId();
        return latestAcceptance(HEALTH_USE, tenantId, userId)
                .map(entity -> HEALTH_USE.version().equals(entity.getTermVersion()))
                .orElse(false);
    }

    private TermResponse termResponse(TermDescriptor descriptor) {
        return new TermResponse(descriptor.type(), descriptor.version(), descriptor.text());
    }

    private TermAcceptanceStatusResponse statusFor(TermDescriptor descriptor) {
        String tenantId = currentTenantService.requiredTenantId();
        String userId = currentUserService.requiredUserId();
        return toStatus(descriptor, latestAcceptance(descriptor, tenantId, userId));
    }

    private TermAcceptanceStatusResponse accept(TermDescriptor descriptor, AcceptTermRequest request) {
        if (!descriptor.version().equals(request.version())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A versão do termo mudou. Recarregue e aceite a versão atual."
            );
        }
        String tenantId = currentTenantService.requiredTenantId();
        String userId = currentUserService.requiredUserId();

        TermAcceptanceEntity acceptance = new TermAcceptanceEntity();
        acceptance.setTenantId(tenantId);
        acceptance.setUserId(userId);
        acceptance.setTermType(descriptor.type());
        acceptance.setTermVersion(descriptor.version());
        acceptance.setAcceptedAt(Instant.now());
        termAcceptanceRepository.save(acceptance);

        return toStatus(descriptor, Optional.of(acceptance));
    }

    private Optional<TermAcceptanceEntity> latestAcceptance(
            TermDescriptor descriptor, String tenantId, String userId) {
        return termAcceptanceRepository
                .findFirstByTenantIdAndUserIdAndTermTypeOrderByAcceptedAtDesc(
                        tenantId, userId, descriptor.type());
    }

    private TermAcceptanceStatusResponse toStatus(
            TermDescriptor descriptor, Optional<TermAcceptanceEntity> acceptance) {
        return acceptance
                .map(entity -> new TermAcceptanceStatusResponse(
                        descriptor.type(),
                        descriptor.version(),
                        descriptor.version().equals(entity.getTermVersion()),
                        entity.getTermVersion(),
                        entity.getAcceptedAt()
                ))
                .orElseGet(() -> new TermAcceptanceStatusResponse(
                        descriptor.type(),
                        descriptor.version(),
                        false,
                        null,
                        null
                ));
    }
}
