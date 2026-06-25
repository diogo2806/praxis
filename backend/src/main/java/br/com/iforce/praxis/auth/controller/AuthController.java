package br.com.iforce.praxis.auth.controller;

import br.com.iforce.praxis.auth.dto.LoginRequest;
import br.com.iforce.praxis.auth.dto.LoginResponse;
import br.com.iforce.praxis.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Porta de entrada (API) da autenticação no sistema.
 *
 * <p>É por aqui que a tela de login conversa com o sistema: recebe o e-mail,
 * a senha e a empresa do usuário e devolve um token de acesso quando os dados
 * estão corretos. Esse token é o "crachá" que autoriza as próximas ações.</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Recebe a tentativa de login feita na tela.
     *
     * @param request e-mail, senha e empresa informados pelo usuário
     * @return o token de acesso e os dados básicos do usuário autenticado
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
