package br.com.iforce.praxis.marketplace.controller;

import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.marketplace.dto.AdminMarketplaceDashboardResponse;
import br.com.iforce.praxis.marketplace.dto.AdminModerateListingRequest;
import br.com.iforce.praxis.marketplace.dto.AdminModerateProfessionalRequest;
import br.com.iforce.praxis.marketplace.dto.CreateListingResponse;
import br.com.iforce.praxis.marketplace.dto.ProfessionalPublicProfileResponse;
import br.com.iforce.praxis.marketplace.service.MarketplaceAdminModerationService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/marketplace")
public class AdminMarketplaceController {

    private final MarketplaceAdminModerationService moderationService;
    private final CurrentUserService currentUserService;

    public AdminMarketplaceController(
            MarketplaceAdminModerationService moderationService,
            CurrentUserService currentUserService
    ) {
        this.moderationService = moderationService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<AdminMarketplaceDashboardResponse> dashboard() {
        return ResponseEntity.ok(moderationService.dashboard());
    }

    @GetMapping("/professionals/pending")
    public ResponseEntity<List<ProfessionalPublicProfileResponse>> pendingProfessionals() {
        return ResponseEntity.ok(moderationService.pendingProfessionals());
    }

    @PostMapping("/professionals/{id}/approve")
    public ResponseEntity<ProfessionalPublicProfileResponse> approveProfessional(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) AdminModerateProfessionalRequest request
    ) {
        return ResponseEntity.ok(moderationService.approveProfessional(currentUserService.requiredUserId(), id, request));
    }

    @PostMapping("/professionals/{id}/reject")
    public ResponseEntity<ProfessionalPublicProfileResponse> rejectProfessional(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) AdminModerateProfessionalRequest request
    ) {
        return ResponseEntity.ok(moderationService.rejectProfessional(currentUserService.requiredUserId(), id, request));
    }

    @PostMapping("/professionals/{id}/suspend")
    public ResponseEntity<ProfessionalPublicProfileResponse> suspendProfessional(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) AdminModerateProfessionalRequest request
    ) {
        return ResponseEntity.ok(moderationService.suspendProfessional(currentUserService.requiredUserId(), id, request));
    }

    @GetMapping("/listings/pending")
    public ResponseEntity<List<CreateListingResponse>> pendingListings() {
        return ResponseEntity.ok(moderationService.pendingListings());
    }

    @PostMapping("/listings/{id}/approve")
    public ResponseEntity<CreateListingResponse> approveListing(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) AdminModerateListingRequest request
    ) {
        return ResponseEntity.ok(moderationService.approveListing(currentUserService.requiredUserId(), id, request));
    }

    @PostMapping("/listings/{id}/reject")
    public ResponseEntity<CreateListingResponse> rejectListing(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) AdminModerateListingRequest request
    ) {
        return ResponseEntity.ok(moderationService.rejectListing(currentUserService.requiredUserId(), id, request));
    }

    @PostMapping("/listings/{id}/suspend")
    public ResponseEntity<CreateListingResponse> suspendListing(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) AdminModerateListingRequest request
    ) {
        return ResponseEntity.ok(moderationService.suspendListing(currentUserService.requiredUserId(), id, request));
    }
}
