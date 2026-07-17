package br.com.iforce.praxis.auth.service;

import br.com.iforce.praxis.admin.model.UserStatus;
import br.com.iforce.praxis.auth.persistence.entity.UserEntity;
import br.com.iforce.praxis.auth.persistence.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.net.URI;

/**
 * Persiste explicitamente o índice determinístico de localização do token de convite.
 * Deve ser chamado dentro da mesma transação que gera ou renova o convite.
 */
@Service
public class InviteTokenLookupIndexWriter {

    private final UserRepository userRepository;

    public InviteTokenLookupIndexWriter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void index(Long userId, String inviteUrl) {
        if (userId == null || inviteUrl == null || inviteUrl.isBlank()) {
            return;
        }

        String token = extractToken(inviteUrl);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Usuário do convite não encontrado."));

        if (user.getStatus() != UserStatus.CONVIDADO || user.getInviteTokenHash() == null) {
            throw new IllegalStateException("O índice só pode ser gravado para convite pendente.");
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
