package br.com.iforce.praxis.marketplace.service;

import br.com.iforce.praxis.admin.model.UserStatus;
import br.com.iforce.praxis.auth.persistence.entity.UserEntity;
import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;
import br.com.iforce.praxis.auth.persistence.repository.UserRepository;
import br.com.iforce.praxis.marketplace.dto.ProfessionalPublicProfileResponse;
import br.com.iforce.praxis.marketplace.dto.ProfessionalDashboardResponse;
import br.com.iforce.praxis.marketplace.dto.PayoutSummaryResponse;
import br.com.iforce.praxis.marketplace.dto.RegisterProfessionalRequest;
import br.com.iforce.praxis.marketplace.dto.ReviewResponse;
import br.com.iforce.praxis.marketplace.dto.RegisterProfessionalResponse;
import br.com.iforce.praxis.marketplace.dto.UpdateProfessionalProfileRequest;
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

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
/**
 * Gerencia a vida cadastral e operacional do profissional que vende no marketplace.
 *
 * <p>Na jornada do produto, este servi&ccedil;o cobre a entrada do profissional na plataforma,
 * consulta de perfil p&uacute;blico, edi&ccedil;&atilde;o de dados e acompanhamento de desempenho comercial.</p>
 */
public class MarketplaceProfessionalService {

    private static final String PLATFORM_EMPRESA_ID = "PLATFORM";
    private static final String PROFESSIONAL_ROLE = "PROFESSIONAL";

    private final MarketplaceProfessionalRepository professionalRepository;
    private final UserRepository userRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;
    private final MarketplaceListingRepository listingRepository;
    private final MarketplaceOrderRepository orderRepository;
    private final MarketplacePayoutRepository payoutRepository;
    private final MarketplaceReviewRepository reviewRepository;

    public MarketplaceProfessionalService(
            MarketplaceProfessionalRepository professionalRepository,
            UserRepository userRepository,
            EmpresaRepository empresaRepository,
            PasswordEncoder passwordEncoder,
            MarketplaceListingRepository listingRepository,
            MarketplaceOrderRepository orderRepository,
            MarketplacePayoutRepository payoutRepository,
            MarketplaceReviewRepository reviewRepository
    ) {
        this.professionalRepository = professionalRepository;
        this.userRepository = userRepository;
        this.empresaRepository = empresaRepository;
        this.passwordEncoder = passwordEncoder;
        this.listingRepository = listingRepository;
        this.orderRepository = orderRepository;
        this.payoutRepository = payoutRepository;
        this.reviewRepository = reviewRepository;
    }

    @Transactional
    /**
     * Cadastra um novo profissional no marketplace.
     *
     * <p>Esse passo cria tanto o acesso do usu&aacute;rio quanto o perfil comercial que depois poder&aacute;
     * ser validado pela plataforma e usado para publicar testes.</p>
     */
    public RegisterProfessionalResponse register(RegisterProfessionalRequest request) {
        if (empresaRepository.findById(PLATFORM_EMPRESA_ID).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Tenant tecnico da plataforma ausente.");
        }

        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmpresaIdAndEmail(PLATFORM_EMPRESA_ID, email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail ja cadastrado como profissional.");
        }

        String document = onlyDigits(request.document());
        if (!isValidCpfOrCnpj(document)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CPF/CNPJ invalido.");
        }
        if (professionalRepository.existsByDocument(document)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF/CNPJ ja cadastrado.");
        }

        UserEntity user = new UserEntity();
        user.setEmpresaId(PLATFORM_EMPRESA_ID);
        user.setEmail(email);
        user.setName(request.name().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRoles(Set.of(PROFESSIONAL_ROLE));
        user.setMarketplaceProfessional(true);
        user.setStatus(UserStatus.ATIVO);
        user.setCreatedAt(Instant.now());
        user = userRepository.save(user);

        MarketplaceProfessionalEntity professional = new MarketplaceProfessionalEntity();
        professional.setUserId(user.getId());
        professional.setDisplayName(request.name().trim());
        professional.setDocument(document);
        professional.setProfessionalRegistration(blankToNull(request.professionalRegistration()));
        professional.setBio(blankToNull(request.bio()));
        professional.setLinkedinUrl(blankToNull(request.linkedinUrl()));
        professional.setPixKey(blankToNull(request.pixKey()));
        professional.setSpecialties(normalizeSpecialties(request.specialties()));
        professional = professionalRepository.save(professional);

        return new RegisterProfessionalResponse(
                professional.getId(),
                professional.getVerificationStatus(),
                user.getEmail()
        );
    }

    @Transactional(readOnly = true)
    /**
     * Exibe o perfil do profissional autenticado para gest&atilde;o do pr&oacute;prio cadastro.
     */
    public ProfessionalPublicProfileResponse currentProfile(String userId) {
        return toProfile(loadByUserId(userId));
    }

    @Transactional(readOnly = true)
    /**
     * Exibe o perfil p&uacute;blico de um profissional verificado.
     *
     * <p>Esta vis&atilde;o &eacute; destinada a clientes e interessados que precisam avaliar quem &eacute;
     * o vendedor por tr&aacute;s de um item do marketplace.</p>
     */
    public ProfessionalPublicProfileResponse publicProfile(Long professionalId) {
        MarketplaceProfessionalEntity professional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profissional nao encontrado."));
        if (professional.getVerificationStatus() != ProfessionalVerificationStatus.VERIFIED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profissional nao encontrado.");
        }
        return toProfile(professional);
    }

    @Transactional(readOnly = true)
    /**
     * Consolida os principais indicadores operacionais do profissional.
     *
     * <p>O retorno resume vendas, valores em escrow, valores liberados, itens publicados,
     * avalia&ccedil;&otilde;es recentes e hist&oacute;rico de repasses.</p>
     */
    public ProfessionalDashboardResponse dashboard(String userId) {
        MarketplaceProfessionalEntity professional = loadByUserId(userId);
        List<MarketplaceOrderEntity> orders = orderRepository.findByProfessionalIdOrderByCreatedAtDesc(
                professional.getId()
        );
        List<MarketplacePayoutEntity> payouts = payoutRepository.findByProfessionalIdOrderByCreatedAtDesc(
                professional.getId()
        );
        List<MarketplaceListingEntity> listings = listingRepository.findByProfessionalIdOrderByCreatedAtDesc(
                professional.getId()
        );
        List<MarketplaceReviewEntity> reviews = reviewRepository.findByProfessionalIdOrderByCreatedAtDesc(
                professional.getId()
        );

        long totalRevenueCents = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.PAID)
                .mapToLong(MarketplaceOrderEntity::getProfessionalPayoutCents)
                .sum();
        long pendingEscrowCents = payouts.stream()
                .filter(payout -> payout.getStatus() == PayoutStatus.ESCROW)
                .mapToLong(MarketplacePayoutEntity::getAmountCents)
                .sum();
        long releasedCents = payouts.stream()
                .filter(payout -> payout.getStatus() == PayoutStatus.RELEASED)
                .mapToLong(MarketplacePayoutEntity::getAmountCents)
                .sum();
        long salesCount = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.PAID)
                .count();

        Map<Long, Long> paidOrdersByListing = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.PAID)
                .collect(Collectors.groupingBy(
                        MarketplaceOrderEntity::getListingId,
                        Collectors.counting()
                ));
        Map<Long, String> listingTitles = listings.stream()
                .collect(Collectors.toMap(
                        MarketplaceListingEntity::getId,
                        MarketplaceListingEntity::getTitle,
                        (first, ignored) -> first
                ));

        return new ProfessionalDashboardResponse(
                totalRevenueCents,
                pendingEscrowCents,
                releasedCents,
                salesCount,
                listings.stream()
                        .map(listing -> new ProfessionalDashboardResponse.ListingDashboardItemResponse(
                                listing.getId(),
                                listing.getTitle(),
                                listing.getStatus(),
                                paidOrdersByListing.getOrDefault(listing.getId(), 0L)
                        ))
                        .toList(),
                reviews.stream()
                        .limit(5)
                        .map(this::toReview)
                        .toList(),
                payouts.stream()
                        .limit(20)
                        .map(payout -> toPayoutSummary(payout, listingTitles, orders))
                        .toList()
        );
    }

    @Transactional
    /**
     * Atualiza os dados edit&aacute;veis do perfil profissional.
     *
     * <p>Esse fluxo permite manter a apresenta&ccedil;&atilde;o comercial e os dados de recebimento alinhados
     * com a realidade atual do profissional.</p>
     */
    public ProfessionalPublicProfileResponse updateCurrentProfile(
            String userId,
            UpdateProfessionalProfileRequest request
    ) {
        MarketplaceProfessionalEntity professional = loadByUserId(userId);
        if (request.displayName() != null && !request.displayName().isBlank()) {
            professional.setDisplayName(request.displayName().trim());
        }
        if (request.bio() != null) {
            professional.setBio(blankToNull(request.bio()));
        }
        if (request.specialties() != null) {
            professional.setSpecialties(normalizeSpecialties(request.specialties()));
        }
        if (request.linkedinUrl() != null) {
            professional.setLinkedinUrl(blankToNull(request.linkedinUrl()));
        }
        if (request.pixKey() != null) {
            professional.setPixKey(blankToNull(request.pixKey()));
        }
        return toProfile(professional);
    }

    private MarketplaceProfessionalEntity loadByUserId(String userId) {
        Long id;
        try {
            id = Long.valueOf(userId);
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessao invalida.");
        }
        return professionalRepository.findByUserId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profissional nao encontrado."));
    }

    private ProfessionalPublicProfileResponse toProfile(MarketplaceProfessionalEntity professional) {
        return new ProfessionalPublicProfileResponse(
                professional.getId(),
                professional.getDisplayName(),
                professional.getBio(),
                Set.copyOf(professional.getSpecialties()),
                professional.getLinkedinUrl(),
                professional.getVerificationStatus(),
                professional.getAverageRating(),
                professional.getTotalReviews(),
                professional.getTotalSales(),
                professional.getMpSellerId() != null && !professional.getMpSellerId().isBlank()
        );
    }

    private ReviewResponse toReview(MarketplaceReviewEntity review) {
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

    private PayoutSummaryResponse toPayoutSummary(
            MarketplacePayoutEntity payout,
            Map<Long, String> listingTitles,
            List<MarketplaceOrderEntity> orders
    ) {
        Long listingId = orders.stream()
                .filter(order -> Objects.equals(order.getId(), payout.getOrderId()))
                .map(MarketplaceOrderEntity::getListingId)
                .findFirst()
                .orElse(null);
        return new PayoutSummaryResponse(
                payout.getId(),
                payout.getOrderId(),
                listingId == null ? null : listingTitles.getOrDefault(listingId, "Listing #" + listingId),
                payout.getAmountCents(),
                payout.getStatus(),
                payout.getEscrowReleaseAt(),
                payout.getReleasedAt(),
                payout.getCreatedAt()
        );
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String onlyDigits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static Set<String> normalizeSpecialties(Set<String> specialties) {
        Set<String> normalized = new LinkedHashSet<>();
        if (specialties == null) {
            return normalized;
        }
        for (String specialty : specialties) {
            if (specialty != null && !specialty.isBlank()) {
                normalized.add(specialty.trim().toLowerCase(Locale.ROOT));
            }
        }
        return normalized;
    }

    private static boolean isValidCpfOrCnpj(String document) {
        return isValidCpf(document) || isValidCnpj(document);
    }

    private static boolean isValidCpf(String cpf) {
        if (cpf.length() != 11 || cpf.chars().distinct().count() == 1) {
            return false;
        }
        int first = cpfDigit(cpf, 9, 10);
        int second = cpfDigit(cpf, 10, 11);
        return first == Character.digit(cpf.charAt(9), 10)
                && second == Character.digit(cpf.charAt(10), 10);
    }

    private static int cpfDigit(String cpf, int length, int weight) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += Character.digit(cpf.charAt(i), 10) * (weight - i);
        }
        int value = 11 - (sum % 11);
        return value >= 10 ? 0 : value;
    }

    private static boolean isValidCnpj(String cnpj) {
        if (cnpj.length() != 14 || cnpj.chars().distinct().count() == 1) {
            return false;
        }
        int first = cnpjDigit(cnpj, 12);
        int second = cnpjDigit(cnpj, 13);
        return first == Character.digit(cnpj.charAt(12), 10)
                && second == Character.digit(cnpj.charAt(13), 10);
    }

    private static int cnpjDigit(String cnpj, int length) {
        int[] weights = length == 12
                ? new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}
                : new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += Character.digit(cnpj.charAt(i), 10) * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
