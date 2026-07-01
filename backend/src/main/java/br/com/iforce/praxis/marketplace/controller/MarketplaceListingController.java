package br.com.iforce.praxis.marketplace.controller;

import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.marketplace.dto.CreateListingRequest;
import br.com.iforce.praxis.marketplace.dto.CreateListingResponse;
import br.com.iforce.praxis.marketplace.dto.ListingDetailResponse;
import br.com.iforce.praxis.marketplace.dto.ListingSearchFilter;
import br.com.iforce.praxis.marketplace.dto.ListingSummaryResponse;
import br.com.iforce.praxis.marketplace.dto.MarketplacePageResponse;
import br.com.iforce.praxis.marketplace.dto.UpdateListingRequest;
import br.com.iforce.praxis.marketplace.model.ListingCategory;
import br.com.iforce.praxis.marketplace.service.MarketplaceListingService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/marketplace/listings")
public class MarketplaceListingController {

    private final MarketplaceListingService listingService;
    private final CurrentUserService currentUserService;

    public MarketplaceListingController(
            MarketplaceListingService listingService,
            CurrentUserService currentUserService
    ) {
        this.listingService = listingService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ResponseEntity<CreateListingResponse> create(@Valid @RequestBody CreateListingRequest request) {
        CreateListingResponse response = listingService.create(currentUserService.requiredUserId(), request);
        return ResponseEntity
                .created(URI.create("/api/v1/marketplace/listings/" + response.id()))
                .body(response);
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<CreateListingResponse> submit(@PathVariable Long id) {
        return ResponseEntity.ok(listingService.submit(currentUserService.requiredUserId(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CreateListingResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateListingRequest request
    ) {
        return ResponseEntity.ok(listingService.update(currentUserService.requiredUserId(), id, request));
    }

    @GetMapping
    public ResponseEntity<MarketplacePageResponse<ListingSummaryResponse>> search(
            @RequestParam(required = false) ListingCategory category,
            @RequestParam(required = false) Long minPriceCents,
            @RequestParam(required = false) Long maxPriceCents,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false) String text,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(listingService.search(new ListingSearchFilter(
                category,
                minPriceCents,
                maxPriceCents,
                minRating,
                text,
                page,
                size
        )));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListingDetailResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(listingService.detail(id));
    }
}
