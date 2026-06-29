package br.com.iforce.praxis.auth.service;

import br.com.iforce.praxis.admin.model.UserStatus;
import br.com.iforce.praxis.auth.dto.LoginRequest;
import br.com.iforce.praxis.auth.dto.LoginResponse;
import br.com.iforce.praxis.auth.persistence.entity.TenantEntity;
import br.com.iforce.praxis.auth.persistence.entity.UserEntity;
import br.com.iforce.praxis.auth.persistence.repository.TenantRepository;
import br.com.iforce.praxis.auth.persistence.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import br.com.iforce.praxis.auth.dto.AcceptInviteRequest;
import java.time.Instant;

/**
 * Gerencia a autenticação de usuários no sistema.
 *
 * Permite que os usuários façam login fornecendo seu email e a empresa à qual
 * estão vinculados. Valida as credenciais (email e senha) e gera um token de
 * acesso que autoriza as requisições subsequentes. O token válida por um período
 * limitado, garantindo que apenas usuários autenticados possam usar o sistema.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            TenantRepository tenantRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Realiza o login de um usuário.
     *
     * Autentica o usuário validando que ele existe na empresa informada e que a
     * senha fornecida está correta. Se as credenciais forem válidas, gera um token
     * de acesso que pode ser usado nas próximas requisições. Se inválidas, rejeita
     * o login e retorna um erro.
     *
     * @param request Contém o email, senha e empresa do usuário que deseja acessar o sistema
     * @return Dados do login bem-sucedido: token de acesso, ID do usuário, nome completo e permissões
     * @throws ResponseStatusException se o email/empresa não existem ou a senha está incorreta
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        UserEntity user = loadUser(request);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas.");
        }

        // Cliente suspenso ou cancelado não pode autenticar (mesmo com credenciais válidas).
        TenantEntity tenant = tenantRepository.findById(user.getTenantId()).orElse(null);
        if (tenant != null && tenant.getStatus() != null && tenant.getStatus().blocksAccess()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Cliente suspenso ou cancelado. Acesso bloqueado.");
        }

        // Usuário bloqueado não autentica; o histórico permanece preservado.
        if (user.getStatus() == UserStatus.BLOQUEADO) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário bloqueado.");
        }

        user.setLastLoginAt(Instant.now());

        String token = jwtService.generateToken(user.getId().toString(), user.getTenantId(), user.getRoles());
        return new LoginResponse(token, user.getId(), user.getTenantId(), user.getName(), user.getRoles());
    }

    /**
     * Busca o usuário no banco de dados.
     *
     * Procura um usuário que corresponda ao email informado dentro da empresa
     * especificada. Isso garante isolamento de dados: usuários de diferentes
     * empresas nunca terão acesso aos dados uns dos outros, mesmo que compartilhem
     * o mesmo email em diferentes contextos.
     *
     * @param request Os dados de login (email e empresa)
     * @return O usuário encontrado
     * @throws ResponseStatusException se a empresa não foi informada ou o usuário não existe
     */
    private UserEntity loadUser(LoginRequest request) {
        if (request.tenantId() == null || request.tenantId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a empresa.");
        }

        return userRepository.findFirstByEmailAndTenantId(request.email(), request.tenantId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas."));
    }

    @Transactional
    public LoginResponse acceptInvite(AcceptInviteRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A confirmação de senha não confere.");
        }

        UserEntity user = loadUserByInviteToken(request.token());

        if (user.getInviteExpiresAt() == null || user.getInviteExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Convite expirado.");
        }

        TenantEntity tenant = tenantRepository.findById(user.getTenantId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Cliente não encontrado."));

        if (tenant.getStatus() != null && tenant.getStatus().blocksAccess()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Cliente suspenso ou cancelado. Acesso bloqueado."
            );
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setStatus(UserStatus.ATIVO);

        // Invalida o convite após uso.
        user.setInviteTokenHash(null);
        user.setInviteExpiresAt(null);

        // Mantém invitedAt como histórico; atualiza último login porque já vamos devolver JWT.
        user.setLastLoginAt(Instant.now());

        String token = jwtService.generateToken(user.getId().toString(), user.getTenantId(), user.getRoles());

        return new LoginResponse(
                token,
                user.getId(),
                user.getTenantId(),
                user.getName(),
                user.getRoles()
        );
    }

    private UserEntity loadUserByInviteToken(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token de convite obrigatório.");
        }

        return userRepository.findByStatusAndInviteTokenHashIsNotNull(UserStatus.CONVIDADO).stream()
                .filter(user -> user.getInviteTokenHash() != null)
                .filter(user -> passwordEncoder.matches(token, user.getInviteTokenHash()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Convite inválido ou já utilizado."
                ));
    }
}
