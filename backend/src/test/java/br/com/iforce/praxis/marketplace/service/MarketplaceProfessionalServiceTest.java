package br.com.iforce.praxis.marketplace.service;

import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;
import br.com.iforce.praxis.auth.persistence.entity.UserEntity;
import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;
import br.com.iforce.praxis.auth.persistence.repository.UserRepository;
import br.com.iforce.praxis.marketplace.dto.RegisterProfessionalRequest;
import br.com.iforce.praxis.marketplace.model.ListingCategory;
import br.com.iforce.praxis.marketplace.model.ListingStatus;
import br.com.iforce.praxis.marketplace.model.OrderStatus;
import br.com.iforce.praxis.marketplace.model.PayoutStatus;
import br.com.iforce.praxis.marketplace.model.ProfessionalVerificationStatus;
import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceListingEntity;
import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceOrderEntity;
import br.com.iforce.praxis.marketplace.persistence.entity.MarketplacePayoutEntity;
import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceProfessionalEntity;
import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceReviewEntity;
import br.com.iforce.praxis.marketplace.persistence.repository.MarketplaceListingRepository;
import br.com.iforce.praxis.marketplace.persistence.repository.MarketplaceOrderRepository;
import br.com.iforce.praxis.marketplace.persistence.repository.MarketplacePayoutRepository;
import br.com.iforce.praxis.marketplace.persistence.repository.MarketplaceProfessionalRepository;
import br.com.iforce.praxis.marketplace.persistence.repository.MarketplaceReviewRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketplaceProfessionalServiceTest {

    @Mock private MarketplaceProfessionalRepository professionalRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmpresaRepository empresaRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private MarketplaceListingRepository listingRepository;
    @Mock private MarketplaceOrderRepository orderRepository;
    @Mock private MarketplacePayoutRepository payoutRepository;
    @Mock private MarketplaceReviewRepository reviewRepository;

    private MarketplaceProfessionalService service;

    @BeforeEach
    void setUp() {
        service = new MarketplaceProfessionalService(
                professionalRepository,
                userRepository,
                empresaRepository,
                passwordEncoder,
                listingRepository,
                orderRepository,
                payoutRepository,
                reviewRepository
        );
    }

    @Test
    void registerCreatesPlatformProfessionalWithValidatedDocument() {
        EmpresaEntity platform = new EmpresaEntity();
        platform.setId("PLATFORM");
        when(empresaRepository.findById("PLATFORM")).thenReturn(Optional.of(platform));
        when(userRepository.existsByEmpresaIdAndEmail("PLATFORM", "ana@example.com")).thenReturn(false);
        when(professionalRepository.existsByDocument("52998224725")).thenReturn(false);
        when(passwordEncoder.encode("senha-segura")).thenReturn("hash");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });
        when(professionalRepository.save(any(MarketplaceProfessionalEntity.class))).thenAnswer(invocation -> {
            MarketplaceProfessionalEntity professional = invocation.getArgument(0);
            professional.setId(20L);
            return professional;
        });

        var response = service.register(new RegisterProfessionalRequest(
                "Ana Souza",
                "ANA@example.com",
                "senha-segura",
                "529.982.247-25",
                "CRP 06/12345",
                "Bio",
                Set.of("Seleção", " Liderança "),
                "https://linkedin.com/in/ana",
                "ana@example.com"
        ));

        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.email()).isEqualTo("ana@example.com");
        assertThat(response.status()).isEqualTo(ProfessionalVerificationStatus.PENDING_VERIFICATION);
        verify(userRepository).save(any(UserEntity.class));
        verify(professionalRepository).save(any(MarketplaceProfessionalEntity.class));
    }

    @Test
    void registerRejectsInvalidCpfOrCnpj() {
        EmpresaEntity platform = new EmpresaEntity();
        platform.setId("PLATFORM");
        when(empresaRepository.findById("PLATFORM")).thenReturn(Optional.of(platform));
        when(userRepository.existsByEmpresaIdAndEmail("PLATFORM", "ana@example.com")).thenReturn(false);

        assertThatThrownBy(() -> service.register(new RegisterProfessionalRequest(
                "Ana Souza",
                "ana@example.com",
                "senha-segura",
                "111.111.111-11",
                null,
                null,
                Set.of(),
                null,
                null
        )))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST);
    }

    @Test
    void dashboardAggregatesRevenueListingsReviewsAndPayouts() {
        MarketplaceProfessionalEntity professional = professional();
        when(professionalRepository.findByUserId(10L)).thenReturn(Optional.of(professional));

        MarketplaceListingEntity approved = listing(100L, "Atendimento sob pressão", ListingStatus.APPROVED);
        MarketplaceListingEntity draft = listing(101L, "Conflito em equipe", ListingStatus.DRAFT);
        when(listingRepository.findByProfessionalIdOrderByCreatedAtDesc(20L)).thenReturn(List.of(approved, draft));

        MarketplaceOrderEntity paid = order(300L, 100L, OrderStatus.PAID, 19000, 15200);
        MarketplaceOrderEntity pending = order(301L, 100L, OrderStatus.PENDING_PAYMENT, 19000, 15200);
        when(orderRepository.findByProfessionalIdOrderByCreatedAtDesc(20L)).thenReturn(List.of(paid, pending));

        MarketplacePayoutEntity escrow = payout(400L, 300L, PayoutStatus.ESCROW, 15200);
        MarketplacePayoutEntity released = payout(401L, 301L, PayoutStatus.RELEASED, 7600);
        when(payoutRepository.findByProfessionalIdOrderByCreatedAtDesc(20L)).thenReturn(List.of(escrow, released));

        MarketplaceReviewEntity review = review(500L, 300L, 100L, (short) 5, "Critérios claros");
        when(reviewRepository.findByProfessionalIdOrderByCreatedAtDesc(20L)).thenReturn(List.of(review));

        var dashboard = service.dashboard("10");

        assertThat(dashboard.totalRevenueCents()).isEqualTo(15200);
        assertThat(dashboard.pendingEscrowCents()).isEqualTo(15200);
        assertThat(dashboard.releasedCents()).isEqualTo(7600);
        assertThat(dashboard.salesCount()).isEqualTo(1);
        assertThat(dashboard.listings()).extracting("id").containsExactly(100L, 101L);
        assertThat(dashboard.listings().getFirst().salesCount()).isEqualTo(1);
        assertThat(dashboard.recentReviews()).hasSize(1);
        assertThat(dashboard.payouts()).hasSize(2);
        assertThat(dashboard.payouts().getFirst().listingTitle()).isEqualTo("Atendimento sob pressão");
    }

    private static MarketplaceProfessionalEntity professional() {
        MarketplaceProfessionalEntity professional = new MarketplaceProfessionalEntity();
        professional.setId(20L);
        professional.setUserId(10L);
        professional.setDisplayName("Ana Souza");
        professional.setDocument("52998224725");
        professional.setVerificationStatus(ProfessionalVerificationStatus.VERIFIED);
        return professional;
    }

    private static MarketplaceListingEntity listing(Long id, String title, ListingStatus status) {
        MarketplaceListingEntity listing = new MarketplaceListingEntity();
        listing.setId(id);
        listing.setProfessionalId(20L);
        listing.setSourceSimulationId("sim-1");
        listing.setSourceVersionId(1L);
        listing.setTitle(title);
        listing.setDescription("Descrição");
        listing.setCategory(ListingCategory.ATENDIMENTO);
        listing.setPriceCents(19000);
        listing.setStatus(status);
        return listing;
    }

    private static MarketplaceOrderEntity order(
            Long id,
            Long listingId,
            OrderStatus status,
            long priceCents,
            long payoutCents
    ) {
        MarketplaceOrderEntity order = new MarketplaceOrderEntity();
        order.setId(id);
        order.setListingId(listingId);
        order.setBuyerTenantId("empresa-1");
        order.setProfessionalId(20L);
        order.setStatus(status);
        order.setPriceCents(priceCents);
        order.setProfessionalPayoutCents(payoutCents);
        order.setPlatformFeeCents(priceCents - payoutCents);
        order.setCreatedAt(Instant.now());
        return order;
    }

    private static MarketplacePayoutEntity payout(Long id, Long orderId, PayoutStatus status, long amountCents) {
        MarketplacePayoutEntity payout = new MarketplacePayoutEntity();
        payout.setId(id);
        payout.setOrderId(orderId);
        payout.setProfessionalId(20L);
        payout.setAmountCents(amountCents);
        payout.setStatus(status);
        payout.setEscrowReleaseAt(Instant.now());
        payout.setCreatedAt(Instant.now());
        if (status == PayoutStatus.RELEASED) {
            payout.setReleasedAt(Instant.now());
        }
        return payout;
    }

    private static MarketplaceReviewEntity review(
            Long id,
            Long orderId,
            Long listingId,
            short rating,
            String comment
    ) {
        MarketplaceReviewEntity review = new MarketplaceReviewEntity();
        review.setId(id);
        review.setOrderId(orderId);
        review.setListingId(listingId);
        review.setProfessionalId(20L);
        review.setReviewerTenantId("empresa-1");
        review.setRating(rating);
        review.setComment(comment);
        review.setCreatedAt(Instant.now());
        return review;
    }
}
