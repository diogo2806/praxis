package br.com.iforce.praxis.billing.persistence.repository;

import br.com.iforce.praxis.billing.persistence.entity.MpWebhookReceiptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MpWebhookReceiptRepository extends JpaRepository<MpWebhookReceiptEntity, Long> {

    Optional<MpWebhookReceiptEntity> findByNotificationId(String notificationId);

    boolean existsByNotificationId(String notificationId);
}
