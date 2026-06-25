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

/**
 * Cuida dos dados da própria conta do usuário que está logado.
 *
 * <p>Na visão do processo, é aqui que a pessoa consulta os seus dados de
 * cadastro (nome, e-mail, perfis de acesso) e troca a sua própria senha.
 * Todas as operações são feitas sempre no contexto da empresa (tenant) e do
 * usuário autenticado naquele momento, garantindo que ninguém acesse a conta
 * de outra pessoa.</p>
 */
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

    /**
     * Mostra os dados da conta da pessoa que está logada.
     *
     * <p>Serve para preencher a tela de "Minha conta": devolve o nome, o
     * e-mail e os perfis de acesso do usuário atual. É apenas consulta, não
     * altera nada.</p>
     *
     * @return os dados de cadastro do usuário logado
     */
    @Transactional(readOnly = true)
    public AccountResponse currentAccount() {
        return toResponse(loadCurrentUser());
    }

    /**
     * Troca a senha do usuário logado.
     *
     * <p>Fluxo do processo: a pessoa informa a senha atual e a nova senha.
     * O sistema confere se a senha atual está correta e exige que a nova
     * senha seja diferente da anterior. Se algo estiver errado, a troca é
     * recusada com uma mensagem explicando o motivo; caso contrário, a nova
     * senha é guardada de forma criptografada.</p>
     *
     * @param request senha atual (para conferência) e a nova senha desejada
     * @return os dados atualizados da conta após a troca
     */
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

    /**
     * Localiza, com segurança, o cadastro do usuário que está logado,
     * garantindo que ele pertença à empresa (tenant) atual. Uso interno.
     */
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

    /**
     * Converte o cadastro interno do usuário no formato simplificado que é
     * devolvido para a tela. Uso interno.
     */
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
