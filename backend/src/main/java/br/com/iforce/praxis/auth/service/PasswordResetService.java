package br.com.iforce.praxis.auth.service;

import br.com.iforce.praxis.admin.model.UserStatus;
import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.audit.service.AuditMetadata;
import br.com.iforce.praxis.auth.dto.ForgotPasswordRequest;
import br.com.iforce.praxis.auth.dto.ResetPasswordRequest;
import br.com.iforce.praxis.auth.dto.ResetPasswordTokenResponse;
import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;
import br.com.iforce.praxis.auth.persistence.entity.UserEntity;
import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;
import br.com.iforce.praxis.auth.persistence.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Orquestra a recuperação de senha de usuários autenticáveis (EMPRESA e ADMIN).
 *
 * <p>O token possui 256 bits aleatórios e nunca é persistido em texto puro. O registro guarda
 * um SHA-256 indexado para localização direta e um BCrypt para comprovação final do token.</p>
 *
 * <p>Princípios de segurança aplicados:</p>
 * <ul>
 *   <li>A solicitação retorna sempre a mesma resposta, sem revelar se o usuário, e-mail ou empresa
 *       existem.</li>
 *   <li>O token expira automaticamente e é invalidado após o uso.</li>
 *   <li>Todo o fluxo respeita o isolamento por empresa.</li>
 *   <li>Solicitação e conclusão geram eventos de auditoria sem token, senha ou hashes.</li>
 * </ul>
 */
@Service
public class PasswordResetService {

    public static final String PLATFORM_EMPRESA_ID = "PLATFORM";

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final UserRepository userRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditEventService auditEventService;
    private final AuditMetadata auditMetadata;
    private final PasswordResetEmailSender emailSender;
    private final String publicBaseUrl;
    private final int ttlHours;

    public PasswordResetService(
            UserRepository userRepository,
            EmpresaRepository empresaRepository,
            PasswordEncoder passwordEncoder,
            AuditEventService auditEventService,
            AuditMetadata auditMetadata,
            PasswordResetEmailSender emailSender,
            @Value("${praxis.public-base-url:http://localhost:8080}") String publicBaseUrl,
            @Value("${praxis.auth.password-reset-ttl-hours:2}") int ttlHours
    ) {
        this.userRepository = userRepository;
        this.empresaRepository = empresaRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditEventService = auditEventService;
        this.auditMetadata = auditMetadata;
        this.emailSender = emailSender;
        this.publicBaseUrl = publicBaseUrl;
        this.ttlHours = ttlHours;
    }

    @Transactional
    public void requestReset(ForgotPasswordRequest request, String ip) {
        String empresaId = resolveEmpresaId(request.empresaId());
        String email = request.email() == null ? "" : request.email().trim();

        Optional<UserEntity> match = userRepository.findFirstByEmailAndEmpresaId(email, empresaId)
                .filter(user -> user.getStatus() == UserStatus.ATIVO)
                .filter(user -> !empresaBlocksAccess(user.getEmpresaId()));

        if (match.isEmpty()) {
            log.info("Solicitação de recuperação de senha sem correspondência ativa (empresa={}).", empresaId);
            return;
        }

        UserEntity user = match.get();
        String token = generateToken(user);
        userRepository.save(user);

        auditEventService.appendUserEvent(
                user.getEmpresaId(),
                String.valueOf(user.getId()),
                AuditEventType.PASSWORD_RESET_REQUESTED,
                "Recuperação de senha solicitada.",
                auditMetadata.of(
                        "userId", user.getId(),
                        "empresaId", user.getEmpresaId(),
                        "requestedAt", String.valueOf(user.getPasswordResetRequestedAt()),
                        "ip", ip == null ? "" : ip
                )
        );

        emailSender.sendPasswordResetEmail(user.getEmail(), user.getName(), resetUrl(token), ttlHours);
    }

    @Transactional(readOnly = true)
    public ResetPasswordTokenResponse validateToken(String token) {
        UserEntity user = findActiveUserByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Token inválido."));

        if (isExpired(user)) {
            throw new ResponseStatusException(HttpStatus.GONE, "Token expirado.");
        }

        return new ResetPasswordTokenResponse(true, user.getPasswordResetExpiresAt(), user.getName());
    }

    @Transactional
    public void confirmReset(ResetPasswordRequest request, String ip) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A confirmação de senha não confere.");
        }

        UserEntity user = findActiveUserByToken(request.token())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido ou já utilizado."));

        if (isExpired(user)) {
            clearResetState(user);
            userRepository.save(user);
            throw new ResponseStatusException(HttpStatus.GONE, "Token expirado.");
        }

        if (empresaBlocksAccess(user.getEmpresaId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cliente suspenso ou cancelado. Acesso bloqueado.");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A nova senha não pode ser igual à senha atual.");
        }

        Instant now = Instant.now();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setLastPasswordResetAt(now);
        clearResetState(user);
        userRepository.save(user);

        auditEventService.appendUserEvent(
                user.getEmpresaId(),
                String.valueOf(user.getId()),
                AuditEventType.PASSWORD_RESET_COMPLETED,
                "Recuperação de senha concluída.",
                auditMetadata.of(
                        "userId", user.getId(),
                        "empresaId", user.getEmpresaId(),
                        "completedAt", String.valueOf(now),
                        "ip", ip == null ? "" : ip
                )
        );
    }

    private String resolveEmpresaId(String requested) {
        return requested == null || requested.isBlank() ? PLATFORM_EMPRESA_ID : requested.trim();
    }

    private String generateToken(UserEntity user) {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String token = URL_ENCODER.encodeToString(bytes);
        Instant now = Instant.now();
        user.setPasswordResetTokenHash(passwordEncoder.encode(token));
        user.setPasswordResetTokenLookupHash(tokenLookupHash(token));
        user.setPasswordResetRequestedAt(now);
        user.setPasswordResetExpiresAt(now.plus(ttlHours, ChronoUnit.HOURS));
        return token;
    }

    private Optional<UserEntity> findActiveUserByToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        Optional<UserEntity> indexed = userRepository
                .findFirstByPasswordResetTokenLookupHash(tokenLookupHash(token))
                .filter(this::isActiveResetUser)
                .filter(user -> passwordEncoder.matches(token, user.getPasswordResetTokenHash()));
        if (indexed.isPresent()) {
            return indexed;
        }

        return userRepository
                .findByPasswordResetTokenHashIsNotNullAndPasswordResetTokenLookupHashIsNull()
                .stream()
                .filter(this::isActiveResetUser)
                .filter(user -> passwordEncoder.matches(token, user.getPasswordResetTokenHash()))
                .findFirst();
    }

    private boolean isActiveResetUser(UserEntity user) {
        return user.getStatus() == UserStatus.ATIVO && user.getPasswordResetTokenHash() != null;
    }

    private String tokenLookupHash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível.", exception);
        }
    }

    private boolean isExpired(UserEntity user) {
        return user.getPasswordResetExpiresAt() == null
                || user.getPasswordResetExpiresAt().isBefore(Instant.now());
    }

    private void clearResetState(UserEntity user) {
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetTokenLookupHash(null);
        user.setPasswordResetRequestedAt(null);
        user.setPasswordResetExpiresAt(null);
    }

    private boolean empresaBlocksAccess(String empresaId) {
        EmpresaEntity empresa = empresaRepository.findById(empresaId).orElse(null);
        return empresa != null && empresa.getStatus() != null && empresa.getStatus().blocksAccess();
    }

    private String resetUrl(String token) {
        return publicBaseUrl + "/reset-password/" + token;
    }
}
