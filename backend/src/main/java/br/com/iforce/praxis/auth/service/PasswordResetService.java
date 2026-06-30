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


import java.security.SecureRandom;

import java.time.Instant;

import java.time.temporal.ChronoUnit;

import java.util.Base64;

import java.util.Optional;


/**
 * Orquestra a recuperação de senha de usuários autenticáveis (EMPRESA e ADMIN).
 *
 * <p>Reaproveita a própria {@link UserEntity} para guardar o estado do fluxo: apenas o hash BCrypt
 * do token é persistido, junto com o momento da solicitação e a expiração. O token puro de 32 bytes
 * só existe em memória e dentro do link enviado por e-mail.</p>
 *
 * <p>Princípios de segurança aplicados:</p>
 * <ul>
 *   <li>A solicitação retorna sempre a mesma resposta, sem revelar se o usuário, e-mail ou empresa
 *       existem.</li>
 *   <li>O token expira automaticamente (TTL configurável, padrão de 2 horas) e é invalidado após o
 *       uso, impedindo reutilização do mesmo link.</li>
 *   <li>Todo o fluxo respeita o isolamento por empresa (ADMIN usa o empresa técnico PLATFORM).</li>
 *   <li>Solicitação e conclusão geram eventos de auditoria, sem nunca registrar token, senha ou
 *       hashes.</li>
 * </ul>
 */
@Service
public class PasswordResetService {

    /** Empresa técnico do operador ADMIN, assumido quando a solicitação não informa empresa. */
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

    /**
     * Processa a solicitação de recuperação de senha.
     *
     * <p>Se houver um usuário ativo correspondente (respeitando o empresa), gera um token, grava o
     * hash, define a expiração, registra auditoria e envia o e-mail. Caso contrário, não faz nada.
     * Em ambos os casos a operação termina silenciosamente para que a resposta da API seja sempre
     * idêntica.</p>
     *
     * @param request dados informados (e-mail e, para EMPRESA, o empresa)
     * @param ip      IP de origem, quando disponível, apenas para auditoria
     */
    @Transactional
    public void requestReset(ForgotPasswordRequest request, String ip) {
        String empresaId = resolveEmpresaId(request.empresaId());
        String email = request.email() == null ? "" : request.email().trim();

        Optional<UserEntity> match = userRepository.findFirstByEmailAndEmpresaId(email, empresaId)
                .filter(user -> user.getStatus() == UserStatus.ATIVO)
                .filter(user -> !empresaBlocksAccess(user.getEmpresaId()));

        if (match.isEmpty()) {
            // Resposta uniforme: não revela usuário, e-mail ou empresa inexistente.
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

    /**
     * Valida um token de recuperação sem alterar nenhum dado.
     *
     * @param token token puro recebido do link
     * @return dados de apresentação quando o token é válido
     * @throws ResponseStatusException {@code 404} se o token é inválido/desconhecido,
     *                                 {@code 410 Gone} se expirou
     */
    @Transactional(readOnly = true)
    public ResetPasswordTokenResponse validateToken(String token) {
        UserEntity user = findActiveUserByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Token inválido."));

        if (isExpired(user)) {
            throw new ResponseStatusException(HttpStatus.GONE, "Token expirado.");
        }

        return new ResetPasswordTokenResponse(true, user.getPasswordResetExpiresAt(), user.getName());
    }

    /**
     * Conclui a redefinição de senha a partir do token.
     *
     * <p>Valida hash, expiração, usuário ativo, confirmação e política de senha. Em caso de sucesso,
     * troca a senha, invalida o token (impedindo reutilização do mesmo link) e registra auditoria.</p>
     *
     * @param request token, nova senha e confirmação
     * @param ip      IP de origem, quando disponível, apenas para auditoria
     * @throws ResponseStatusException {@code 400} para token/confirmação/política inválidos,
     *                                 {@code 410 Gone} quando expirado
     */
    @Transactional
    public void confirmReset(ResetPasswordRequest request, String ip) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A confirmação de senha não confere.");
        }

        UserEntity user = findActiveUserByToken(request.token())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido ou já utilizado."));

        if (isExpired(user)) {
            // Limpa o token expirado para que não permaneça pendente.
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
        return (requested == null || requested.isBlank()) ? PLATFORM_EMPRESA_ID : requested.trim();
    }

    private String generateToken(UserEntity user) {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String token = URL_ENCODER.encodeToString(bytes);
        Instant now = Instant.now();
        user.setPasswordResetTokenHash(passwordEncoder.encode(token));
        user.setPasswordResetRequestedAt(now);
        user.setPasswordResetExpiresAt(now.plus(ttlHours, ChronoUnit.HOURS));
        return token;
    }

    private Optional<UserEntity> findActiveUserByToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByPasswordResetTokenHashIsNotNull().stream()
                .filter(user -> user.getStatus() == UserStatus.ATIVO)
                .filter(user -> user.getPasswordResetTokenHash() != null)
                .filter(user -> passwordEncoder.matches(token, user.getPasswordResetTokenHash()))
                .findFirst();
    }

    private boolean isExpired(UserEntity user) {
        return user.getPasswordResetExpiresAt() == null
                || user.getPasswordResetExpiresAt().isBefore(Instant.now());
    }

    private void clearResetState(UserEntity user) {
        user.setPasswordResetTokenHash(null);
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
