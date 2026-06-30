package br.com.iforce.praxis.marketplace.persistence.entity;

import br.com.iforce.praxis.marketplace.model.ProfessionalVerificationStatus;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Profissional de psicometria cadastrado no marketplace.
 *
 * <p>O profissional reaproveita o login da plataforma: é um {@code UserEntity} vinculado à
 * empresa técnica {@code 'PLATFORM'} com a role {@code PROFISSIONAL}. Esta entidade guarda os
 * dados específicos do marketplace (perfil público, dados de repasse, reputação). O token do
 * Mercado Pago é sensível e nunca deve ser exposto em DTO de resposta.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "marketplace_professionals",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_marketplace_professionals_user", columnNames = "user_id"),
                @UniqueConstraint(name = "uq_marketplace_professionals_document", columnNames = "document")
        }
)
public class MarketplaceProfessionalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /** FK para {@code users(id)} — o usuário de login deste profissional. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    /** CPF ou CNPJ (apenas dígitos). */
    @Column(name = "document", nullable = false, length = 20)
    private String document;

    /** Registro profissional, p.ex. CRP. Opcional. */
    @Column(name = "professional_registration", length = 50)
    private String professionalRegistration;

    @Column(name = "bio", columnDefinition = "text")
    private String bio;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "marketplace_professional_specialties",
            joinColumns = @JoinColumn(name = "professional_id")
    )
    @Column(name = "specialty", nullable = false, length = 60)
    private Set<String> specialties = new LinkedHashSet<>();

    @Column(name = "linkedin_url", length = 300)
    private String linkedinUrl;

    @Column(name = "pix_key", length = 150)
    private String pixKey;

    /** Id do vendedor no Mercado Pago (via OAuth Connect). */
    @Column(name = "mp_seller_id", length = 100)
    private String mpSellerId;

    /** Token de acesso do Mercado Pago, criptografado em repouso. Nunca exposto em DTO. */
    @Column(name = "mp_access_token", columnDefinition = "text")
    private String mpAccessToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    private ProfessionalVerificationStatus verificationStatus = ProfessionalVerificationStatus.PENDING_VERIFICATION;

    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "total_reviews", nullable = false)
    private int totalReviews = 0;

    @Column(name = "total_sales", nullable = false)
    private int totalSales = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
