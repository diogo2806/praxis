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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        UserEntity user = invitedUser();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        InviteTokenLookupIndexService service = new InviteTokenLookupIndexService(userRepository);
        service.index(7L, "https://praxis.example.com/convite/token-aleatorio");

        assertThat(user.getInviteTokenLookupHash())
                .isEqualTo(TokenLookupHasher.sha256("token-aleatorio"));
        verify(userRepository).save(user);
    }

    @Test
    void ignoresQueryStringAndFragmentWhenExtractingToken() {
        UserEntity user = invitedUser();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        InviteTokenLookupIndexService service = new InviteTokenLookupIndexService(userRepository);
        service.index(7L, "https://praxis.example.com/convite/token-aleatorio?utm=mail#access");

        assertThat(user.getInviteTokenLookupHash())
                .isEqualTo(TokenLookupHasher.sha256("token-aleatorio"));
        verify(userRepository).save(user);
    }

    @Test
    void ignoresMissingInvitationData() {
        InviteTokenLookupIndexService service = new InviteTokenLookupIndexService(userRepository);

        service.index(null, "https://praxis.example.com/convite/token");
        service.index(7L, null);
        service.index(7L, "");
        service.index(7L, "   ");

        verifyNoInteractions(userRepository);
    }

    @Test
    void rejectsInvitationUrlWithoutToken() {
        InviteTokenLookupIndexService service = new InviteTokenLookupIndexService(userRepository);

        assertThatThrownBy(() -> service.index(7L, "https://praxis.example.com/convite/"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("URL de convite sem token");

        verifyNoInteractions(userRepository);
    }

    @Test
    void ignoresUnknownUser() {
        when(userRepository.findById(7L)).thenReturn(Optional.empty());

        InviteTokenLookupIndexService service = new InviteTokenLookupIndexService(userRepository);
        service.index(7L, "https://praxis.example.com/convite/token-aleatorio");

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(UserEntity.class));
    }

    @Test
    void doesNotIndexUserThatIsNoLongerInvited() {
        UserEntity user = invitedUser();
        user.setStatus(UserStatus.ATIVO);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        InviteTokenLookupIndexService service = new InviteTokenLookupIndexService(userRepository);
        service.index(7L, "https://praxis.example.com/convite/token-aleatorio");

        assertThat(user.getInviteTokenLookupHash()).isNull();
        verify(userRepository, never()).save(user);
    }

    @Test
    void doesNotIndexInvitationWithoutCryptographicProofHash() {
        UserEntity user = invitedUser();
        user.setInviteTokenHash(null);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        InviteTokenLookupIndexService service = new InviteTokenLookupIndexService(userRepository);
        service.index(7L, "https://praxis.example.com/convite/token-aleatorio");

        assertThat(user.getInviteTokenLookupHash()).isNull();
        verify(userRepository, never()).save(user);
    }

    private UserEntity invitedUser() {
        UserEntity user = new UserEntity();
        user.setId(7L);
        user.setStatus(UserStatus.CONVIDADO);
        user.setInviteTokenHash("bcrypt-token");
        return user;
    }
}
