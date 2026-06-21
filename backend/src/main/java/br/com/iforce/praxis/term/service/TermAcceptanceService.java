package br.com.iforce.praxis.term.service;

import br.com.iforce.praxis.auth.service.CurrentTenantService;
import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.term.dto.AcceptTermRequest;
import br.com.iforce.praxis.term.dto.TermAcceptanceStatusResponse;
import br.com.iforce.praxis.term.dto.TermResponse;
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
 * Termo de responsabilidade do recrutador (REQ-L5): expõe o texto/versão corrente e registra o
 * aceite (quem, quando, qual versão) de forma insert-only, para comprovar que o cliente assumiu
 * ser o responsável pelo conteúdo e pela decisão.
 */
@Service
public class TermAcceptanceService {

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
        return new TermResponse(ResponsibilityTerm.TYPE, ResponsibilityTerm.VERSION, ResponsibilityTerm.TEXT);
    }

    @Transactional(readOnly = true)
    public TermAcceptanceStatusResponse responsibilityStatus() {
        String tenantId = currentTenantService.requiredTenantId();
        String userId = currentUserService.requiredUserId();
        return toStatus(latestAcceptance(tenantId, userId));
    }

    @Transactional
    public TermAcceptanceStatusResponse acceptResponsibility(AcceptTermRequest request) {
        if (!ResponsibilityTerm.VERSION.equals(request.version())) {
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
        acceptance.setTermType(ResponsibilityTerm.TYPE);
        acceptance.setTermVersion(ResponsibilityTerm.VERSION);
        acceptance.setAcceptedAt(Instant.now());
        termAcceptanceRepository.save(acceptance);

        return toStatus(Optional.of(acceptance));
    }

    private Optional<TermAcceptanceEntity> latestAcceptance(String tenantId, String userId) {
        return termAcceptanceRepository
                .findFirstByTenantIdAndUserIdAndTermTypeOrderByAcceptedAtDesc(
                        tenantId, userId, ResponsibilityTerm.TYPE);
    }

    private TermAcceptanceStatusResponse toStatus(Optional<TermAcceptanceEntity> acceptance) {
        return acceptance
                .map(entity -> new TermAcceptanceStatusResponse(
                        ResponsibilityTerm.TYPE,
                        ResponsibilityTerm.VERSION,
                        ResponsibilityTerm.VERSION.equals(entity.getTermVersion()),
                        entity.getTermVersion(),
                        entity.getAcceptedAt()
                ))
                .orElseGet(() -> new TermAcceptanceStatusResponse(
                        ResponsibilityTerm.TYPE,
                        ResponsibilityTerm.VERSION,
                        false,
                        null,
                        null
                ));
    }
}
