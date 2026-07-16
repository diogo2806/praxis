package br.com.iforce.praxis.auth.service;

import br.com.iforce.praxis.admin.model.UserStatus;
import br.com.iforce.praxis.auth.persistence.entity.UserEntity;
import br.com.iforce.praxis.auth.persistence.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;

/**
 * Registra o hash de localização de um convite depois que o fluxo gerador
 * conclui sua própria transação.
 */
@Service
public class InviteTokenLookupIndexService {

    private final UserRepository userRepository;

    public InviteTokenLookupIndexService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void index(Long userId, String inviteUrl) {
        if (userId == null || inviteUrl == null || inviteUrl.isBlank()) {
            return;
        }

        String token = extractToken(inviteUrl);
        UserEntity user = userRepository.findById(userId).orElse(null);
        if (user == null
                || user.getStatus() != UserStatus.CONVIDADO
                || user.getInviteTokenHash() == null) {
            return;
        }

        user.setInviteTokenLookupHash(TokenLookupHasher.sha256(token));
        userRepository.save(user);
    }

    private String extractToken(String inviteUrl) {
        URI uri = URI.create(inviteUrl);
        String path = uri.getPath();
        if (path == null || path.isBlank() || path.endsWith("/")) {
            throw new IllegalArgumentException("URL de convite sem token.");
        }

        int separator = path.lastIndexOf('/');
        String token = separator >= 0 ? path.substring(separator + 1) : path;
        if (token.isBlank()) {
            throw new IllegalArgumentException("URL de convite sem token.");
        }
        return token;
    }
}
