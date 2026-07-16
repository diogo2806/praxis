package br.com.iforce.praxis.auth.service;

import br.com.iforce.praxis.admin.model.UserStatus;
import br.com.iforce.praxis.auth.persistence.entity.UserEntity;
import br.com.iforce.praxis.auth.persistence.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InviteTokenLookupIndexServiceTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void indexesInviteTokenFromReturnedUrl() {
        UserEntity user = new UserEntity();
        user.setId(7L);
        user.setStatus(UserStatus.CONVIDADO);
        user.setInviteTokenHash("bcrypt-token");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        InviteTokenLookupIndexService service = new InviteTokenLookupIndexService(userRepository);
        service.index(7L, "https://praxis.example.com/convite/token-aleatorio");

        assertThat(user.getInviteTokenLookupHash())
                .isEqualTo(TokenLookupHasher.sha256("token-aleatorio"));
        verify(userRepository).save(user);
    }

    @Test
    void ignoresMissingInvitationUrl() {
        InviteTokenLookupIndexService service = new InviteTokenLookupIndexService(userRepository);

        service.index(7L, null);

        verifyNoInteractions(userRepository);
    }

    @Test
    void doesNotIndexUserThatIsNoLongerInvited() {
        UserEntity user = new UserEntity();
        user.setId(7L);
        user.setStatus(UserStatus.ATIVO);
        user.setInviteTokenHash("bcrypt-token");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        InviteTokenLookupIndexService service = new InviteTokenLookupIndexService(userRepository);
        service.index(7L, "https://praxis.example.com/convite/token-aleatorio");

        assertThat(user.getInviteTokenLookupHash()).isNull();
        verify(userRepository, never()).save(user);
    }
}
