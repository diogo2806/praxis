package br.com.iforce.praxis.shared.notification.persistence.repository;

import br.com.iforce.praxis.shared.notification.model.InAppNotificationType;

import br.com.iforce.praxis.shared.notification.persistence.entity.InAppNotificationEntity;

import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;


public interface InAppNotificationRepository extends JpaRepository<InAppNotificationEntity, Long> {

    boolean existsByEmpresaIdAndOutboxEventIdAndRecipientUserIdAndType(
            String empresaId,
            Long outboxEventId,
            Long recipientUserId,
            InAppNotificationType type
    );

    List<InAppNotificationEntity> findByEmpresaIdOrderByCreatedAtDesc(String empresaId);
}
