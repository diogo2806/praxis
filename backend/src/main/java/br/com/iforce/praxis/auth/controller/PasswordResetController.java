package br.com.iforce.praxis.auth.controller;

import br.com.iforce.praxis.auth.dto.ForgotPasswordRequest;
import br.com.iforce.praxis.auth.dto.ResetPasswordRequest;
import br.com.iforce.praxis.auth.dto.ResetPasswordTokenResponse;
import br.com.iforce.praxis.auth.service.PasswordResetRateLimiter;
import br.com.iforce.praxis.auth.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Porta de entrada (API) do fluxo público de recuperação de senha.
 *
 * <p>Todos os endpoints são públicos (não exigem JWT). A solicitação responde sempre da mesma forma
 * para não revelar a existência de contas; a validação e a confirmação do token são limitadas por
 * IP para conter força bruta.</p>
 */
@RestController
@RequestMapping("/api/v1/auth/password")
public class PasswordResetController {

    private static final String UNIFORM_FORGOT_MESSAGE =
            "Se existir uma conta correspondente, enviaremos um e-mail com as instruções para redefinir sua senha.";

    private final PasswordResetService passwordResetService;
    private final PasswordResetRateLimiter rateLimiter;

    public PasswordResetController(
            PasswordResetService passwordResetService,
            PasswordResetRateLimiter rateLimiter
    ) {
        this.passwordResetService = passwordResetService;
        this.rateLimiter = rateLimiter;
    }

    /**
     * Solicita a recuperação de senha. Retorna sempre {@code 200 OK} com a mesma mensagem.
     */
    @PostMapping("/forgot")
    public ResponseEntity<Map<String, String>> forgot(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        String ip = clientIp(httpRequest);
        enforceRateLimit("forgot:ip:" + ip);
        enforceRateLimit("forgot:user:" + request.tenantId() + "|" + request.email());

        passwordResetService.requestReset(request, ip);
        return ResponseEntity.ok(Map.of("message", UNIFORM_FORGOT_MESSAGE));
    }

    /**
     * Valida um token de recuperação. {@code 200} quando válido, {@code 404} inválido,
     * {@code 410 Gone} quando expirado.
     */
    @GetMapping("/reset/{token}")
    public ResponseEntity<ResetPasswordTokenResponse> validate(
            @PathVariable String token,
            HttpServletRequest httpRequest
    ) {
        enforceRateLimit("reset-validate:ip:" + clientIp(httpRequest));
        return ResponseEntity.ok(passwordResetService.validateToken(token));
    }

    /**
     * Conclui a redefinição de senha a partir do token.
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> reset(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        String ip = clientIp(httpRequest);
        enforceRateLimit("reset-confirm:ip:" + ip);

        passwordResetService.confirmReset(request, ip);
        return ResponseEntity.ok(Map.of("message", "Senha redefinida com sucesso."));
    }

    private void enforceRateLimit(String key) {
        if (!rateLimiter.tryAcquire(key)) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Muitas tentativas. Aguarde alguns minutos e tente novamente."
            );
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
