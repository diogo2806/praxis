package br.com.iforce.praxis.marketplace.controller;

import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.marketplace.dto.AdminMarketplaceDashboardResponse;
import br.com.iforce.praxis.marketplace.dto.AdminModerateListingRequest;
import br.com.iforce.praxis.marketplace.dto.AdminModerateProfessionalRequest;
import br.com.iforce.praxis.marketplace.dto.AdminRefundOrderRequest;
import br.com.iforce.praxis.marketplace.dto.CreateListingResponse;
import br.com.iforce.praxis.marketplace.dto.MarketplaceOrderResponse;
import br.com.iforce.praxis.marketplace.dto.ProfessionalPublicProfileResponse;
import br.com.iforce.praxis.marketplace.model.ListingStatus;
import br.com.iforce.praxis.marketplace.model.ProfessionalVerificationStatus;
import br.com.iforce.praxis.marketplace.service.MarketplaceAdminModerationService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/professionals")
    public ResponseEntity<List<ProfessionalPublicProfileResponse>> professionals(
            @RequestParam(required = false) ProfessionalVerificationStatus status
    ) {
        return ResponseEntity.ok(moderationService.professionalsByStatus(status));
    }

    @PostMapping("/professionals/{id}/verify")
    public ResponseEntity<ProfessionalPublicProfileResponse> verifyProfessional(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) AdminModerateProfessionalRequest request
    ) {
        if (request != null && Boolean.TRUE.equals(request.approved())) {
            return ResponseEntity.ok(moderationService.approveProfessional(currentUserService.requiredUserId(), id, request));
        }
        return ResponseEntity.ok(moderationService.rejectProfessional(currentUserService.requiredUserId(), id, request));
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

    @GetMapping("/listings")
    public ResponseEntity<List<CreateListingResponse>> listings(
            @RequestParam(required = false) ListingStatus status
    ) {
        return ResponseEntity.ok(moderationService.listingsByStatus(status));
    }

    @PostMapping("/listings/{id}/moderate")
    public ResponseEntity<CreateListingResponse> moderateListing(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) AdminModerateListingRequest request
    ) {
        if (request != null && Boolean.TRUE.equals(request.approved())) {
            return ResponseEntity.ok(moderationService.approveListing(currentUserService.requiredUserId(), id, request));
        }
        return ResponseEntity.ok(moderationService.rejectListing(currentUserService.requiredUserId(), id, request));
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

    @GetMapping("/disputes")
    public ResponseEntity<List<MarketplaceOrderResponse>> disputes() {
        return ResponseEntity.ok(moderationService.disputedOrders());
    }

    @PostMapping("/orders/{id}/refund")
    public ResponseEntity<MarketplaceOrderResponse> refundOrder(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) AdminRefundOrderRequest request
    ) {
        return ResponseEntity.ok(moderationService.refundOrder(currentUserService.requiredUserId(), id, request));
    }
}
