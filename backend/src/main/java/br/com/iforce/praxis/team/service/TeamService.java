package br.com.iforce.praxis.team.service;

import br.com.iforce.praxis.admin.model.UserStatus;
import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.audit.service.AuditMetadata;
import br.com.iforce.praxis.auth.persistence.entity.UserEntity;
import br.com.iforce.praxis.auth.persistence.repository.UserRepository;
import br.com.iforce.praxis.shared.security.SecureTokens;
import br.com.iforce.praxis.team.dto.InviteTeamUserRequest;
import br.com.iforce.praxis.team.dto.InviteTeamUserResponse;
import br.com.iforce.praxis.team.dto.TeamUserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Gerencia os usuários da equipe da empresa autenticada. */
@Service
public class TeamService {

    private static final String EMPRESA_ROLE = "EMPRESA";
    private static final int INVITE_TOKEN_BYTES = 32;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditEventService auditEventService;
    private final AuditMetadata auditMetadata;
    private final String publicBaseUrl;
    private final int inviteTtlHours;

    public TeamService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuditEventService auditEventService,
            AuditMetadata auditMetadata,
            @Value("${praxis.public-base-url:http://localhost:8080}") String publicBaseUrl,
            @Value("${praxis.admin.invite-ttl-hours:168}") int inviteTtlHours
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditEventService = auditEventService;
        this.auditMetadata = auditMetadata;
        this.publicBaseUrl = publicBaseUrl;
        this.inviteTtlHours = inviteTtlHours;
    }

    @Transactional(readOnly = true)
    public List<TeamUserResponse> listUsers(String tenantId) {
        return userRepository.findByEmpresaIdOrderByCreatedAtAsc(tenantId).stream()
                .map(TeamService::toResponse)
                .toList();
    }

    @Transactional
    public InviteTeamUserResponse inviteUser(String actorUserId, String tenantId, InviteTeamUserRequest request) {
        if (userRepository.existsByEmpresaIdAndEmail(tenantId, request.email())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe um usuário com este e-mail nesta empresa."
            );
        }

        UserEntity user = new UserEntity();
        user.setEmpresaId(tenantId);
        user.setName(request.name().trim());
        user.setEmail(request.email().trim());
        user.setRoles(Set.of(EMPRESA_ROLE));
        user.setCreatedAt(Instant.now());
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));

        String token = generateInviteToken(user);
        user.setStatus(UserStatus.CONVIDADO);
        userRepository.save(user);

        auditEventService.auditAdminAction(
                actorUserId,
                tenantId,
                AuditEventType.TEAM_USER_INVITED,
                "Usuário convidado pela empresa: " + user.getEmail(),
                auditMetadata.of(
                        "userId", user.getId(),
                        "email", user.getEmail(),
                        "role", EMPRESA_ROLE
                )
        );

        return new InviteTeamUserResponse(toResponse(user), inviteUrl(token));
    }

    @Transactional
    public InviteTeamUserResponse resendInvite(String actorUserId, String tenantId, Long userId) {
        UserEntity user = requireTenantUser(tenantId, userId);

        if (user.getStatus() != UserStatus.CONVIDADO) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Só é possível reenviar convite para usuário que ainda está como CONVIDADO."
            );
        }

        String token = generateInviteToken(user);
        auditEventService.auditAdminAction(
                actorUserId,
                tenantId,
                AuditEventType.TEAM_USER_INVITE_RESENT,
                "Convite reenviado pela empresa para " + user.getEmail(),
                auditMetadata.of("userId", user.getId(), "email", user.getEmail())
        );

        return new InviteTeamUserResponse(toResponse(user), inviteUrl(token));
    }

    @Transactional
    public TeamUserResponse blockUser(String actorUserId, String tenantId, Long userId) {
        try {
            Long actorId = Long.parseLong(actorUserId);
            if (actorId.equals(userId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível bloquear o próprio usuário.");
            }
        } catch (NumberFormatException ignored) {
            // Usuário técnico usado apenas quando a segurança está desabilitada.
        }

        UserEntity user = requireTenantUser(tenantId, userId);
        user.setStatus(UserStatus.BLOQUEADO);

        auditEventService.auditAdminAction(
                actorUserId,
                tenantId,
                AuditEventType.TEAM_USER_BLOCKED,
                "Usuário bloqueado pela empresa: " + user.getEmail(),
                auditMetadata.of("userId", user.getId(), "email", user.getEmail())
        );

        return toResponse(user);
    }

    @Transactional
    public TeamUserResponse unblockUser(String actorUserId, String tenantId, Long userId) {
        UserEntity user = requireTenantUser(tenantId, userId);
        user.setStatus(UserStatus.ATIVO);

        auditEventService.auditAdminAction(
                actorUserId,
                tenantId,
                AuditEventType.TEAM_USER_UNBLOCKED,
                "Usuário desbloqueado pela empresa: " + user.getEmail(),
                auditMetadata.of("userId", user.getId(), "email", user.getEmail())
        );

        return toResponse(user);
    }

    private UserEntity requireTenantUser(String tenantId, Long userId) {
        return userRepository.findByIdAndEmpresaId(userId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
    }

    private String generateInviteToken(UserEntity user) {
        String token = SecureTokens.randomUrlSafe(INVITE_TOKEN_BYTES);
        Instant now = Instant.now();
        user.setInviteTokenHash(passwordEncoder.encode(token));
        user.setInvitedAt(now);
        user.setInviteExpiresAt(now.plus(inviteTtlHours, ChronoUnit.HOURS));
        return token;
    }

    private String inviteUrl(String token) {
        return publicBaseUrl + "/convite/" + token;
    }

    private static TeamUserResponse toResponse(UserEntity user) {
        return new TeamUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRoles(),
                user.getStatus(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }
}
