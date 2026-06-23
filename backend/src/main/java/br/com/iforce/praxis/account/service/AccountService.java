package br.com.iforce.praxis.account.service;

import br.com.iforce.praxis.account.dto.AccountResponse;
import br.com.iforce.praxis.account.dto.ChangePasswordRequest;
import br.com.iforce.praxis.auth.persistence.entity.UserEntity;
import br.com.iforce.praxis.auth.persistence.repository.UserRepository;
import br.com.iforce.praxis.auth.service.CurrentTenantService;
import br.com.iforce.praxis.auth.service.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountService {

    private final CurrentUserService currentUserService;
    private final CurrentTenantService currentTenantService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(
            CurrentUserService currentUserService,
            CurrentTenantService currentTenantService,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.currentUserService = currentUserService;
        this.currentTenantService = currentTenantService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public AccountResponse currentAccount() {
        return toResponse(loadCurrentUser());
    }

    @Transactional
    public AccountResponse changePassword(ChangePasswordRequest request) {
        UserEntity user = loadCurrentUser();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha atual incorreta.");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A nova senha deve ser diferente da senha atual.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        return toResponse(userRepository.save(user));
    }

    private UserEntity loadCurrentUser() {
        String tenantId = currentTenantService.requiredTenantId();
        String userId = currentUserService.requiredUserId();
        try {
            Long id = Long.valueOf(userId);
            return userRepository.findById(id)
                    .filter(user -> tenantId.equals(user.getTenantId()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        } catch (NumberFormatException exception) {
            return userRepository.findFirstByTenantId(tenantId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        }
    }

    private static AccountResponse toResponse(UserEntity user) {
        return new AccountResponse(
                user.getId(),
                user.getTenantId(),
                user.getName(),
                user.getEmail(),
                user.getRoles()
        );
    }
}
