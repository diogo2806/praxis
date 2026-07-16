package br.com.iforce.praxis.auth.service;

import br.com.iforce.praxis.admin.dto.CreateEmpresaAdminResponse;
import br.com.iforce.praxis.admin.dto.InviteUserAdminResponse;
import br.com.iforce.praxis.team.dto.InviteTeamUserResponse;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Acrescenta o índice de localização aos convites produzidos pelos fluxos já
 * existentes, sem alterar seus contratos públicos.
 */
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class InviteTokenLookupIndexAspect {

    private static final Logger log = LoggerFactory.getLogger(InviteTokenLookupIndexAspect.class);

    private final InviteTokenLookupIndexService indexService;

    public InviteTokenLookupIndexAspect(InviteTokenLookupIndexService indexService) {
        this.indexService = indexService;
    }

    @AfterReturning(
            pointcut = "execution(* br.com.iforce.praxis.team.service.TeamService.inviteUser(..))"
                    + " || execution(* br.com.iforce.praxis.team.service.TeamService.resendInvite(..))",
            returning = "response"
    )
    public void indexTeamInvite(InviteTeamUserResponse response) {
        if (response == null || response.user() == null) {
            return;
        }
        indexSafely(response.user().id(), response.inviteUrl());
    }

    @AfterReturning(
            pointcut = "execution(* br.com.iforce.praxis.admin.service.AdminEmpresaService.inviteUser(..))"
                    + " || execution(* br.com.iforce.praxis.admin.service.AdminEmpresaService.resendInvite(..))",
            returning = "response"
    )
    public void indexAdminInvite(InviteUserAdminResponse response) {
        if (response == null || response.user() == null) {
            return;
        }
        indexSafely(response.user().id(), response.inviteUrl());
    }

    @AfterReturning(
            pointcut = "execution(* br.com.iforce.praxis.admin.service.AdminEmpresaService.create(..))",
            returning = "response"
    )
    public void indexResponsibleUserInvite(CreateEmpresaAdminResponse response) {
        if (response == null) {
            return;
        }
        indexSafely(response.responsibleUserId(), response.inviteUrl());
    }

    private void indexSafely(Long userId, String inviteUrl) {
        try {
            indexService.index(userId, inviteUrl);
        } catch (RuntimeException exception) {
            // O convite continua válido pelo caminho legado; não expõe o token no log.
            log.warn("Não foi possível registrar o índice do convite do usuário {}.", userId, exception);
        }
    }
}
