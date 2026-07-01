package br.com.iforce.praxis.marketplace.service;

import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.marketplace.dto.CreateReviewRequest;
import br.com.iforce.praxis.marketplace.dto.ReviewResponse;
import br.com.iforce.praxis.marketplace.model.OrderStatus;
import br.com.iforce.praxis.shared.notification.model.InAppNotificationType;
import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceOrderEntity;
import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceProfessionalEntity;
import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceReviewEntity;
import br.com.iforce.praxis.marketplace.persistence.repository.MarketplaceOrderRepository;
import br.com.iforce.praxis.marketplace.persistence.repository.MarketplaceProfessionalRepository;
import br.com.iforce.praxis.marketplace.persistence.repository.MarketplaceReviewRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
/**
 * Controla as avalia&ccedil;&otilde;es deixadas pelos clientes ap&oacute;s o uso do item comprado.
 *
 * <p>No processo do marketplace, este servi&ccedil;o garante que a reputa&ccedil;&atilde;o do profissional seja
 * formada apenas a partir de compras pagas e efetivamente utilizadas.</p>
 */
public class MarketplaceReviewService {

    private final MarketplaceReviewRepository reviewRepository;
    private final MarketplaceOrderRepository orderRepository;
    private final MarketplaceProfessionalRepository professionalRepository;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final MarketplaceNotificationService notificationService;

    public MarketplaceReviewService(
            MarketplaceReviewRepository reviewRepository,
            MarketplaceOrderRepository orderRepository,
            MarketplaceProfessionalRepository professionalRepository,
            CandidateAttemptRepository candidateAttemptRepository,
            MarketplaceNotificationService notificationService
    ) {
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
        this.professionalRepository = professionalRepository;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    /**
     * Registra a avalia&ccedil;&atilde;o de um cliente sobre uma compra conclu&iacute;da.
     *
     * <p>O sistema s&oacute; aceita esse passo depois que o pedido foi pago e houve pelo menos uma
     * tentativa conclu&iacute;da na simula&ccedil;&atilde;o clonada, para que a nota reflita uso real.</p>
     */
    public ReviewResponse create(String reviewerTenantId, CreateReviewRequest request) {
        MarketplaceOrderEntity order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido nao encontrado."));
        if (!order.getBuyerTenantId().equals(reviewerTenantId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Pedido pertence a outro tenant.");
        }
        if (order.getStatus() != OrderStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Avaliacao exige pedido pago.");
        }
        if (reviewRepository.existsByOrderId(order.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pedido ja avaliado.");
        }
        if (order.getClonedSimulationId() == null || order.getClonedSimulationId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Simulacao clonada ainda nao disponivel.");
        }
        long completedAttempts = candidateAttemptRepository.countByEmpresaIdAndSimulationIdAndStatus(
                reviewerTenantId,
                order.getClonedSimulationId(),
                AttemptStatus.COMPLETED
        );
        if (completedAttempts <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Avaliacao exige ao menos uma tentativa concluida na simulacao comprada."
            );
        }

        MarketplaceReviewEntity review = new MarketplaceReviewEntity();
        review.setOrderId(order.getId());
        review.setListingId(order.getListingId());
        review.setProfessionalId(order.getProfessionalId());
        review.setReviewerTenantId(reviewerTenantId);
        review.setRating(request.rating());
        review.setComment(blankToNull(request.comment()));
        review = reviewRepository.save(review);

        refreshProfessionalRating(order.getProfessionalId());
        notificationService.notifyProfessional(
                order.getProfessionalId(),
                InAppNotificationType.MARKETPLACE_REVIEW_RECEIVED,
                "Nova avaliacao recebida",
                "Um cliente avaliou seu teste com " + request.rating() + " estrela(s)."
        );
        return toResponse(review);
    }

    @Transactional(readOnly = true)
    /**
     * Lista as avalia&ccedil;&otilde;es j&aacute; publicadas para um item do marketplace.
     */
    public List<ReviewResponse> listByListing(Long listingId) {
        return reviewRepository.findByListingIdOrderByCreatedAtDesc(listingId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void refreshProfessionalRating(Long professionalId) {
        MarketplaceProfessionalEntity professional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profissional nao encontrado."));
        List<MarketplaceReviewEntity> reviews = reviewRepository.findByProfessionalIdOrderByCreatedAtDesc(professionalId);
        BigDecimal average = reviews.isEmpty()
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(reviews.stream().mapToInt(MarketplaceReviewEntity::getRating).average().orElse(0))
                .setScale(2, RoundingMode.HALF_UP);
        professional.setAverageRating(average);
        professional.setTotalReviews(reviews.size());
    }

    private ReviewResponse toResponse(MarketplaceReviewEntity review) {
        return new ReviewResponse(
                review.getId(),
                review.getOrderId(),
                review.getListingId(),
                review.getProfessionalId(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
