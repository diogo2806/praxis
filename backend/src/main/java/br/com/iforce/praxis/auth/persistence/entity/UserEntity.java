package br.com.iforce.praxis.auth.persistence.entity;

import br.com.iforce.praxis.admin.model.UserStatus;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_empresa_email", columnNames = {"empresa_id", "email"})
)
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "empresa_id", nullable = false, length = 120)
    private String empresaId;

    @Column(name = "email", nullable = false, length = 180)
    private String email;

    @Column(name = "name", nullable = false, length = 180)
    private String name;

    @Column(name = "password_hash", nullable = false, length = 120)
    private String passwordHash;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false, length = 80)
    private Set<String> roles = new LinkedHashSet<>();

    /** Situação do usuário de acesso (ATIVO, CONVIDADO, BLOQUEADO). */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private UserStatus status = UserStatus.ATIVO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "invited_at")
    private Instant invitedAt;

    /** Hash BCrypt usado para comprovar o token de convite sem persistir o valor puro. */
    @Column(name = "invite_token_hash", length = 120)
    private String inviteTokenHash;

    /** SHA-256 usado somente para localizar diretamente o convite aleatório. */
    @Column(name = "invite_token_lookup_hash", length = 64)
    private String inviteTokenLookupHash;

    @Column(name = "invite_expires_at")
    private Instant inviteExpiresAt;

    /** Hash BCrypt usado para comprovar o token de recuperação sem persistir o valor puro. */
    @Column(name = "password_reset_token_hash", length = 120)
    private String passwordResetTokenHash;

    /** SHA-256 usado somente para localizar diretamente o registro associado ao token aleatório. */
    @Column(name = "password_reset_token_lookup_hash", length = 64)
    private String passwordResetTokenLookupHash;

    @Column(name = "password_reset_requested_at")
    private Instant passwordResetRequestedAt;

    @Column(name = "password_reset_expires_at")
    private Instant passwordResetExpiresAt;

    /** Momento da última redefinição concluída; mantido como histórico. */
    @Column(name = "last_password_reset_at")
    private Instant lastPasswordResetAt;
}
