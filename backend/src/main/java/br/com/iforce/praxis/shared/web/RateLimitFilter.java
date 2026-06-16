package br.com.iforce.praxis.shared.web;

import br.com.iforce.praxis.config.PraxisProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
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

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final int MAX_CACHE_SIZE = 100_000;

    private final PraxisProperties praxisProperties;
    private final Cache<String, Bucket> buckets;

    public RateLimitFilter(PraxisProperties praxisProperties) {
        this.praxisProperties = praxisProperties;
        this.buckets = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofHours(1))
                .maximumSize(MAX_CACHE_SIZE)
                .build();
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

        Bucket bucket = buckets.get(policy.key(), ignored -> newBucket(policy.requestsPerMinute()));
        response.setHeader("X-RateLimit-Limit", String.valueOf(policy.requestsPerMinute()));

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setHeader("Retry-After", "60");
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
            return new RateLimitPolicy(
                    "authoring:%s:%s".formatted(authenticatedPrincipal(), ip),
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
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String authenticatedPrincipal() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return "anonymous";
        }
        return authentication.getName();
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
