package br.com.iforce.praxis.marketplace.controller;

import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.marketplace.dto.ProfessionalDashboardResponse;
import br.com.iforce.praxis.marketplace.dto.ProfessionalPublicProfileResponse;
import br.com.iforce.praxis.marketplace.dto.RegisterProfessionalRequest;
import br.com.iforce.praxis.marketplace.dto.RegisterProfessionalResponse;
import br.com.iforce.praxis.marketplace.dto.UpdateProfessionalProfileRequest;
import br.com.iforce.praxis.marketplace.service.MarketplaceProfessionalService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/marketplace/professionals")
public class MarketplaceProfessionalController {

    private final MarketplaceProfessionalService professionalService;
    private final CurrentUserService currentUserService;

    public MarketplaceProfessionalController(
            MarketplaceProfessionalService professionalService,
            CurrentUserService currentUserService
    ) {
        this.professionalService = professionalService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterProfessionalResponse> register(
            @Valid @RequestBody RegisterProfessionalRequest request
    ) {
        RegisterProfessionalResponse response = professionalService.register(request);
        return ResponseEntity
                .created(URI.create("/api/v1/marketplace/professionals/" + response.id()))
                .body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<ProfessionalPublicProfileResponse> me() {
        return ResponseEntity.ok(professionalService.currentProfile(currentUserService.requiredUserId()));
    }

    @GetMapping("/me/dashboard")
    public ResponseEntity<ProfessionalDashboardResponse> dashboard() {
        return ResponseEntity.ok(professionalService.dashboard(currentUserService.requiredUserId()));
    }

    @PutMapping("/me")
    public ResponseEntity<ProfessionalPublicProfileResponse> updateMe(
            @Valid @RequestBody UpdateProfessionalProfileRequest request
    ) {
        return ResponseEntity.ok(professionalService.updateCurrentProfile(
                currentUserService.requiredUserId(),
                request
        ));
    }
}
