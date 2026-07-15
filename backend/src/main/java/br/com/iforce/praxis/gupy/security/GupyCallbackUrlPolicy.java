package br.com.iforce.praxis.gupy.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Política centralizada para impedir que callback_url transforme a conclusão da avaliação
 * em um redirecionamento aberto. Em produção, somente HTTPS e hosts explicitamente
 * autorizados são aceitos. HTTP fica restrito aos perfis locais e aos hosts de loopback.
 */
@Component
public class GupyCallbackUrlPolicy {

    private final List<String> allowedHostPatterns;
    private final Environment environment;

    public GupyCallbackUrlPolicy(
            @Value("${praxis.gupy-callback-allowed-host-patterns:gupy.io,*.gupy.io}")
            List<String> allowedHostPatterns,
            Environment environment
    ) {
        this.allowedHostPatterns = allowedHostPatterns.stream()
                .map(String::trim)
                .filter(pattern -> !pattern.isBlank())
                .map(pattern -> pattern.toLowerCase(Locale.ROOT))
                .toList();
        this.environment = environment;
    }

    public void validate(URI callbackUrl) {
        if (callbackUrl == null || !callbackUrl.isAbsolute() || callbackUrl.getHost() == null) {
            reject("callback_url deve ser uma URL absoluta com host autorizado.");
        }

        String scheme = callbackUrl.getScheme().toLowerCase(Locale.ROOT);
        String host = callbackUrl.getHost().toLowerCase(Locale.ROOT);
        boolean localProfile = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.equals("local") || profile.equals("dev") || profile.equals("test"));
        boolean loopback = host.equals("localhost") || host.equals("127.0.0.1") || host.equals("::1");

        if (!"https".equals(scheme) && !(localProfile && loopback && "http".equals(scheme))) {
            reject("callback_url deve usar HTTPS; HTTP é permitido apenas em ambiente local.");
        }
        if (callbackUrl.getUserInfo() != null || callbackUrl.getFragment() != null) {
            reject("callback_url não pode conter credenciais ou fragmento.");
        }
        if (!loopback && allowedHostPatterns.stream().noneMatch(pattern -> matches(pattern, host))) {
            reject("Host de callback_url não autorizado para a integração Gupy.");
        }
    }

    private boolean matches(String pattern, String host) {
        if (pattern.startsWith("*.")) {
            String suffix = pattern.substring(1);
            return host.endsWith(suffix) && host.length() > suffix.length();
        }
        return host.equals(pattern);
    }

    private void reject(String message) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
