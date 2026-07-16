package br.com.iforce.praxis.auth.service;

import br.com.iforce.praxis.admin.model.UserStatus;
import br.com.iforce.praxis.auth.dto.AcceptInviteRequest;
import br.com.iforce.praxis.auth.dto.LoginRequest;
import br.com.iforce.praxis.auth.dto.LoginResponse;
import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;
import br.com.iforce.praxis.auth.persistence.entity.UserEntity;
import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;
import br.com.iforce.praxis.auth.persistence.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

/**
 * Gerencia a autenticação de usuários no sistema.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            EmpresaRepository empresaRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.empresaRepository = empresaRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        UserEntity user = loadUser(request);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas.");
        }

        EmpresaEntity empresa = empresaRepository.findById(user.getEmpresaId()).orElse(null);
        if (empresa != null && empresa.getStatus() != null && empresa.getStatus().blocksAccess()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Cliente suspenso ou cancelado. Acesso bloqueado."
            );
        }

        if (user.getStatus() == UserStatus.CONVIDADO) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Convite pendente. Defina sua senha pelo link recebido."
            );
        }

        if (user.getStatus() == UserStatus.BLOQUEADO) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário bloqueado.");
        }

        user.setLastLoginAt(Instant.now());
        String token = jwtService.generateToken(user.getId().toString(), user.getEmpresaId(), user.getRoles());
        return new LoginResponse(token, user.getId(), user.getEmpresaId(), user.getName(), user.getRoles());
    }

    private UserEntity loadUser(LoginRequest request) {
        if (request.empresaId() == null || request.empresaId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a empresa.");
        }

        return userRepository.findFirstByEmailAndEmpresaId(request.email(), request.empresaId())
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

        EmpresaEntity empresa = empresaRepository.findById(user.getEmpresaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Cliente não encontrado."));

        if (empresa.getStatus() != null && empresa.getStatus().blocksAccess()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Cliente suspenso ou cancelado. Acesso bloqueado."
            );
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setStatus(UserStatus.ATIVO);
        user.setInviteTokenHash(null);
        user.setInviteTokenLookupHash(null);
        user.setInviteExpiresAt(null);
        user.setLastLoginAt(Instant.now());

        String token = jwtService.generateToken(user.getId().toString(), user.getEmpresaId(), user.getRoles());
        return new LoginResponse(
                token,
                user.getId(),
                user.getEmpresaId(),
                user.getName(),
                user.getRoles()
        );
    }

    private UserEntity loadUserByInviteToken(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token de convite obrigatório.");
        }

        Optional<UserEntity> indexed = userRepository
                .findFirstByInviteTokenLookupHash(TokenLookupHasher.sha256(token))
                .filter(this::isPendingInvite)
                .filter(user -> passwordEncoder.matches(token, user.getInviteTokenHash()));
        if (indexed.isPresent()) {
            return indexed.get();
        }

        return userRepository
                .findByStatusAndInviteTokenHashIsNotNullAndInviteTokenLookupHashIsNull(UserStatus.CONVIDADO)
                .stream()
                .filter(this::isPendingInvite)
                .filter(user -> passwordEncoder.matches(token, user.getInviteTokenHash()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Convite inválido ou já utilizado."
                ));
    }

    private boolean isPendingInvite(UserEntity user) {
        return user.getStatus() == UserStatus.CONVIDADO && user.getInviteTokenHash() != null;
    }
}
