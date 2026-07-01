package br.com.iforce.praxis.marketplace.controller;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.marketplace.dto.CreateReviewRequest;
import br.com.iforce.praxis.marketplace.dto.ReviewResponse;
import br.com.iforce.praxis.marketplace.service.MarketplaceReviewService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/marketplace/reviews")
public class MarketplaceReviewController {

    private final MarketplaceReviewService reviewService;
    private final CurrentEmpresaService currentEmpresaService;

    public MarketplaceReviewController(
            MarketplaceReviewService reviewService,
            CurrentEmpresaService currentEmpresaService
    ) {
        this.reviewService = reviewService;
        this.currentEmpresaService = currentEmpresaService;
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> create(@Valid @RequestBody CreateReviewRequest request) {
        return ResponseEntity.status(201).body(reviewService.create(
                currentEmpresaService.requiredEmpresaId(),
                request
        ));
    }

    @GetMapping
    public ResponseEntity<List<ReviewResponse>> listByListing(@RequestParam Long listingId) {
        return ResponseEntity.ok(reviewService.listByListing(listingId));
    }
}
