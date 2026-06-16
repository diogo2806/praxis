package br.com.iforce.praxis.shared.web;

import br.com.iforce.praxis.config.PraxisProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_TENANT_ID = "X-Tenant-Id";

    private final PraxisProperties praxisProperties;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(PraxisProperties praxisProperties) {
        this.praxisProperties = praxisProperties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        RateLimitPolicy policy = policyFor(request);
        if (policy == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Bucket bucket = buckets.computeIfAbsent(policy.key(), ignored -> newBucket(policy.requestsPerMinute()));
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"Rate limit excedido.\"}");
    }

    private RateLimitPolicy policyFor(HttpServletRequest request) {
        String path = request.getRequestURI();
        String ip = clientIp(request);

        if (path.equals("/test") || path.startsWith("/test/")) {
            String apiKeyHash = sha256(nullToAnonymous(request.getHeader(HEADER_AUTHORIZATION)));
            return new RateLimitPolicy(
                    "gupy:%s:%s".formatted(apiKeyHash, ip),
                    praxisProperties.gupyRateLimitRequestsPerMinute()
            );
        }

        if (path.equals("/api/v1/simulations") || path.startsWith("/api/v1/simulations/")) {
            String userId = nullToAnonymous(request.getHeader(HEADER_USER_ID));
            String tenantId = nullToAnonymous(request.getHeader(HEADER_TENANT_ID));
            return new RateLimitPolicy(
                    "authoring:%s:%s:%s".formatted(userId, tenantId, ip),
                    praxisProperties.authoringRateLimitRequestsPerMinute()
            );
        }

        return null;
    }

    private Bucket newBucket(int requestsPerMinute) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(requestsPerMinute)
                .refillGreedy(requestsPerMinute, Duration.ofMinutes(1))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(HEADER_FORWARDED_FOR);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String nullToAnonymous(String value) {
        return value == null || value.isBlank() ? "anonymous" : value;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponivel.", exception);
        }
    }

    private record RateLimitPolicy(String key, int requestsPerMinute) {
    }
}
