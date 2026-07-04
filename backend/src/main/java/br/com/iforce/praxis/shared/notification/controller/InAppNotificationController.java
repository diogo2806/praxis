package br.com.iforce.praxis.shared.notification.controller;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;

import br.com.iforce.praxis.shared.notification.dto.InAppNotificationResponse;

import br.com.iforce.praxis.shared.notification.persistence.entity.InAppNotificationEntity;

import br.com.iforce.praxis.shared.notification.persistence.repository.InAppNotificationRepository;

import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.server.ResponseStatusException;


import java.time.Instant;

import java.util.List;


/**
 * Porta de entrada (API) das notificações exibidas dentro do sistema.
 *
 * <p>Na visão do processo, alimenta o "sininho" de avisos da empresa: lista
 * as notificações internas (por exemplo, alertas sobre entregas de resultado
 * que falharam) da empresa logada, da mais recente para a mais antiga.</p>
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class InAppNotificationController {

    private final CurrentEmpresaService currentEmpresaService;
    private final InAppNotificationRepository notificationRepository;

    public InAppNotificationController(
            CurrentEmpresaService currentEmpresaService,
            InAppNotificationRepository notificationRepository
    ) {
        this.currentEmpresaService = currentEmpresaService;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Lista as notificações internas da empresa logada.
     *
     * @return as notificações, da mais recente para a mais antiga
     */
    @GetMapping
    public ResponseEntity<List<InAppNotificationResponse>> listNotifications() {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        return ResponseEntity.ok(notificationRepository.findByEmpresaIdOrderByCreatedAtDesc(empresaId)
                .stream()
                .map(this::toResponse)
                .toList());
    }

    /**
     * Informa quantas notificações ainda não foram lidas pela empresa.
     *
     * <p>Esse número alimenta o badge do menu lateral, ajudando o RH a perceber
     * rapidamente quando há uma pendência operacional, como entregas em DLQ.</p>
     */
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadNotificationCountResponse> unreadCount() {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        return ResponseEntity.ok(new UnreadNotificationCountResponse(
                notificationRepository.countByEmpresaIdAndReadAtIsNull(empresaId)
        ));
    }

    /**
     * Marca uma notificação como lida sem removê-la do histórico.
     *
     * <p>Na prática, a notificação deixa de aparecer como pendente no menu e na
     * tela de alertas, mas continua auditável para consulta posterior.</p>
     */
    @PostMapping("/{notificationId}/read")
    @Transactional
    public ResponseEntity<InAppNotificationResponse> markAsRead(@PathVariable Long notificationId) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        InAppNotificationEntity notification = notificationRepository.findByEmpresaIdAndId(empresaId, notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificação não encontrada."));
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
        }
        return ResponseEntity.ok(toResponse(notification));
    }

    /** Converte a notificação interna no formato simplificado exibido na tela. Uso interno. */
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

    public record UnreadNotificationCountResponse(long count) {
    }
}
