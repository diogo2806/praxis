package br.com.iforce.praxis.auth.service;

import br.com.iforce.praxis.admin.dto.AdminUserResponse;
import br.com.iforce.praxis.admin.dto.CreateEmpresaAdminResponse;
import br.com.iforce.praxis.admin.dto.InviteUserAdminResponse;
import br.com.iforce.praxis.admin.model.UserStatus;
import br.com.iforce.praxis.team.dto.InviteTeamUserResponse;
import br.com.iforce.praxis.team.dto.TeamUserResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Set;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InviteTokenLookupIndexAspectTest {

    @Mock
    private InviteTokenLookupIndexService indexService;

    @Test
    void indexesTeamInvitationResponse() {
        InviteTokenLookupIndexAspect aspect = new InviteTokenLookupIndexAspect(indexService);
        TeamUserResponse user = new TeamUserResponse(
                10L,
                "Pessoa",
                "pessoa@example.com",
                Set.of("EMPRESA"),
                UserStatus.CONVIDADO,
                null,
                Instant.now()
        );

        aspect.indexTeamInvite(new InviteTeamUserResponse(user, "https://praxis/convite/team-token"));

        verify(indexService).index(10L, "https://praxis/convite/team-token");
    }

    @Test
    void indexesAdminInvitationResponse() {
        InviteTokenLookupIndexAspect aspect = new InviteTokenLookupIndexAspect(indexService);
        AdminUserResponse user = new AdminUserResponse(
                11L,
                "Pessoa",
                "pessoa@example.com",
                Set.of("EMPRESA"),
                UserStatus.CONVIDADO,
                null,
                Instant.now()
        );

        aspect.indexAdminInvite(new InviteUserAdminResponse(user, "https://praxis/convite/admin-token"));

        verify(indexService).index(11L, "https://praxis/convite/admin-token");
    }

    @Test
    void indexesResponsibleUserCreatedWithEmpresa() {
        InviteTokenLookupIndexAspect aspect = new InviteTokenLookupIndexAspect(indexService);

        aspect.indexResponsibleUserInvite(new CreateEmpresaAdminResponse(
                null,
                12L,
                "https://praxis/convite/responsible-token"
        ));

        verify(indexService).index(12L, "https://praxis/convite/responsible-token");
    }
}
