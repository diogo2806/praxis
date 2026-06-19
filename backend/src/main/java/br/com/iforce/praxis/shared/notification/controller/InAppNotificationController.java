package br.com.iforce.praxis.shared.notification.controller;

import br.com.iforce.praxis.auth.service.CurrentTenantService;
import br.com.iforce.praxis.shared.notification.dto.InAppNotificationResponse;
import br.com.iforce.praxis.shared.notification.persistence.entity.InAppNotificationEntity;
import br.com.iforce.praxis.shared.notification.persistence.repository.InAppNotificationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class InAppNotificationController {

    private final CurrentTenantService currentTenantService;
    private final InAppNotificationRepository notificationRepository;

    public InAppNotificationController(
            CurrentTenantService currentTenantService,
            InAppNotificationRepository notificationRepository
    ) {
        this.currentTenantService = currentTenantService;
        this.notificationRepository = notificationRepository;
    }

    @GetMapping
    public ResponseEntity<List<InAppNotificationResponse>> listNotifications() {
        String tenantId = currentTenantService.requiredTenantId();
        return ResponseEntity.ok(notificationRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(this::toResponse)
                .toList());
    }

    private InAppNotificationResponse toResponse(InAppNotificationEntity notification) {
        return new InAppNotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getCandidateAttemptId(),
                notification.getCandidateName(),
                notification.getCandidateEmail(),
                notification.getOutboxEventId(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }
}
