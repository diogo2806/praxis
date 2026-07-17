package br.com.iforce.praxis.team.service;

import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.audit.service.AuditMetadata;
import br.com.iforce.praxis.auth.persistence.repository.UserRepository;
import br.com.iforce.praxis.auth.service.InviteTokenLookupIndexWriter;
import br.com.iforce.praxis.team.dto.InviteTeamUserRequest;
import br.com.iforce.praxis.team.dto.InviteTeamUserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Variante principal do serviço de equipe que grava o índice do convite
 * explicitamente antes do commit da operação de negócio.
 */
@Service
@Primary
public class IndexedTeamService extends TeamService {

    private final InviteTokenLookupIndexWriter indexWriter;

    public IndexedTeamService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuditEventService auditEventService,
            AuditMetadata auditMetadata,
            @Value("${praxis.public-base-url:http://localhost:8080}") String publicBaseUrl,
            @Value("${praxis.admin.invite-ttl-hours:168}") int inviteTtlHours,
            InviteTokenLookupIndexWriter indexWriter
    ) {
        super(
                userRepository,
                passwordEncoder,
                auditEventService,
                auditMetadata,
                publicBaseUrl,
                inviteTtlHours
        );
        this.indexWriter = indexWriter;
    }

    @Override
    @Transactional
    public InviteTeamUserResponse inviteUser(
            String actorUserId,
            String tenantId,
            InviteTeamUserRequest request
    ) {
        InviteTeamUserResponse response = super.inviteUser(actorUserId, tenantId, request);
        indexWriter.index(response.user().id(), response.inviteUrl());
        return response;
    }

    @Override
    @Transactional
    public InviteTeamUserResponse resendInvite(String actorUserId, String tenantId, Long userId) {
        InviteTeamUserResponse response = super.resendInvite(actorUserId, tenantId, userId);
        indexWriter.index(response.user().id(), response.inviteUrl());
        return response;
    }
}
