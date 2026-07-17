package br.com.iforce.praxis.admin.service;

import br.com.iforce.praxis.admin.dto.CreateEmpresaAdminRequest;
import br.com.iforce.praxis.admin.dto.CreateEmpresaAdminResponse;
import br.com.iforce.praxis.admin.dto.InviteUserAdminRequest;
import br.com.iforce.praxis.admin.dto.InviteUserAdminResponse;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.audit.service.AuditMetadata;
import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;
import br.com.iforce.praxis.auth.persistence.repository.UserRepository;
import br.com.iforce.praxis.auth.service.InviteTokenLookupIndexWriter;
import br.com.iforce.praxis.billing.service.CreditService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Variante principal do serviço administrativo que grava o índice do convite
 * explicitamente antes do commit da operação de negócio.
 */
@Service
@Primary
public class IndexedAdminEmpresaService extends AdminEmpresaService {

    private final InviteTokenLookupIndexWriter indexWriter;

    public IndexedAdminEmpresaService(
            EmpresaRepository empresaRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuditEventService auditEventService,
            AuditMetadata auditMetadata,
            AdminUsageService adminUsageService,
            CreditService creditService,
            @Value("${praxis.public-base-url:http://localhost:8080}") String publicBaseUrl,
            @Value("${praxis.admin.invite-ttl-hours:168}") int inviteTtlHours,
            @Value("${praxis.admin.usage-period-days:30}") int usagePeriodDays,
            InviteTokenLookupIndexWriter indexWriter
    ) {
        super(
                empresaRepository,
                userRepository,
                passwordEncoder,
                auditEventService,
                auditMetadata,
                adminUsageService,
                creditService,
                publicBaseUrl,
                inviteTtlHours,
                usagePeriodDays
        );
        this.indexWriter = indexWriter;
    }

    @Override
    @Transactional
    public CreateEmpresaAdminResponse create(String actorUserId, CreateEmpresaAdminRequest request) {
        CreateEmpresaAdminResponse response = super.create(actorUserId, request);
        indexWriter.index(response.responsibleUserId(), response.inviteUrl());
        return response;
    }

    @Override
    @Transactional
    public InviteUserAdminResponse inviteUser(
            String actorUserId,
            String empresaId,
            InviteUserAdminRequest request
    ) {
        InviteUserAdminResponse response = super.inviteUser(actorUserId, empresaId, request);
        indexWriter.index(response.user().id(), response.inviteUrl());
        return response;
    }

    @Override
    @Transactional
    public InviteUserAdminResponse resendInvite(String actorUserId, String empresaId, Long userId) {
        InviteUserAdminResponse response = super.resendInvite(actorUserId, empresaId, userId);
        indexWriter.index(response.user().id(), response.inviteUrl());
        return response;
    }
}
