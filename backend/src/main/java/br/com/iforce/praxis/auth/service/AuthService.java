package br.com.iforce.praxis.auth.service;

import br.com.iforce.praxis.auth.dto.LoginRequest;
import br.com.iforce.praxis.auth.dto.LoginResponse;
import br.com.iforce.praxis.auth.persistence.entity.UserEntity;
import br.com.iforce.praxis.auth.persistence.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        UserEntity user = loadUser(request);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais invalidas.");
        }

        String token = jwtService.generateToken(user.getId().toString(), user.getTenantId(), user.getRoles());
        return new LoginResponse(token, user.getId(), user.getTenantId(), user.getName(), user.getRoles());
    }

    private UserEntity loadUser(LoginRequest request) {
        if (request.tenantId() == null || request.tenantId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tenantId e obrigatorio.");
        }

        return userRepository.findFirstByEmailAndTenantId(request.email(), request.tenantId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais invalidas."));
    }
}
