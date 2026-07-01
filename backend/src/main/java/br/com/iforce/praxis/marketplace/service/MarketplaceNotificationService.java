package br.com.iforce.praxis.marketplace.service;

import br.com.iforce.praxis.auth.persistence.entity.UserEntity;
import br.com.iforce.praxis.auth.persistence.repository.UserRepository;
import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceProfessionalEntity;
import br.com.iforce.praxis.marketplace.persistence.repository.MarketplaceProfessionalRepository;
import br.com.iforce.praxis.shared.notification.model.InAppNotificationType;
import br.com.iforce.praxis.shared.notification.persistence.entity.InAppNotificationEntity;
import br.com.iforce.praxis.shared.notification.persistence.repository.InAppNotificationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
/**
 * Registra notifica&ccedil;&otilde;es internas relacionadas aos acontecimentos do marketplace.
 *
 * <p>Na pr&aacute;tica, este servi&ccedil;o transforma eventos importantes do processo, como aprova&ccedil;&otilde;es,
 * vendas, avalia&ccedil;&otilde;es e libera&ccedil;&atilde;o de repasse, em avisos que aparecem para os envolvidos.</p>
 */
public class MarketplaceNotificationService {

    private static final String PLATFORM_EMPRESA_ID = "PLATFORM";
    private static final String EMPRESA_ROLE = "EMPRESA";

    private final InAppNotificationRepository notificationRepository;
    private final MarketplaceProfessionalRepository professionalRepository;
    private final UserRepository userRepository;

    public MarketplaceNotificationService(
            InAppNotificationRepository notificationRepository,
            MarketplaceProfessionalRepository professionalRepository,
            UserRepository userRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.professionalRepository = professionalRepository;
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    /**
     * Envia uma notifica&ccedil;&atilde;o para o profissional dono do cadastro indicado.
     *
     * <p>Esse fluxo &eacute; usado quando o assunto do marketplace precisa chegar diretamente a quem
     * vende ou administra o conte&uacute;do profissional.</p>
     */
    public void notifyProfessional(
            Long professionalId,
            InAppNotificationType type,
            String title,
            String message
    ) {
        professionalRepository.findById(professionalId)
                .ifPresent(professional -> createNotification(
                        PLATFORM_EMPRESA_ID,
                        professional.getUserId(),
                        type,
                        title,
                        message
                ));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    /**
     * Envia uma notifica&ccedil;&atilde;o para os administradores da empresa cliente.
     *
     * <p>Serve para avisar o tenant comprador sobre fatos relevantes da opera&ccedil;&atilde;o, como
     * disponibilidade de conte&uacute;do, atualiza&ccedil;&otilde;es de pedido ou outras a&ccedil;&otilde;es do marketplace.</p>
     */
    public void notifyTenantAdmins(
            String tenantId,
            InAppNotificationType type,
            String title,
            String message
    ) {
        List<UserEntity> users = userRepository.findByEmpresaIdAndRole(tenantId, EMPRESA_ROLE);
        for (UserEntity user : users) {
            createNotification(tenantId, user.getId(), type, title, message);
        }
    }

    private void createNotification(
            String empresaId,
            Long recipientUserId,
            InAppNotificationType type,
            String title,
            String message
    ) {
        InAppNotificationEntity notification = new InAppNotificationEntity();
        notification.setEmpresaId(empresaId);
        notification.setRecipientUserId(recipientUserId);
        notification.setType(type);
        notification.setTitle(truncate(title, 180));
        notification.setMessage(truncate(message, 1000));
        notification.setCreatedAt(Instant.now());
        notificationRepository.save(notification);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
