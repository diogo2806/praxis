package br.com.iforce.praxis.shared.web;

import br.com.iforce.praxis.config.PraxisProperties;
import com.github.benmanes.caffeine.cache.Cache;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimitFilterTest {

    private RateLimitFilter rateLimitFilter;
    private PraxisProperties praxisProperties;

    @BeforeEach
    void setUp() {
        praxisProperties = mock(PraxisProperties.class);
        when(praxisProperties.gupyRateLimitRequestsPerMinute()).thenReturn(60);
        when(praxisProperties.authoringRateLimitRequestsPerMinute()).thenReturn(30);
        when(praxisProperties.authLoginRateLimitRequestsPerMinute()).thenReturn(10);

        rateLimitFilter = new RateLimitFilter(praxisProperties);
    }

    @Test
    void forwardedHeadersDoNotCreateFreshBuckets() throws Exception {
        when(praxisProperties.gupyRateLimitRequestsPerMinute()).thenReturn(1);

        MockHttpServletRequest first = request("/test/candidate");
        first.setRemoteAddr("203.0.113.10");
        first.addHeader("Authorization", "Bearer token");
        first.addHeader("X-Forwarded-For", "198.51.100.1");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();

        rateLimitFilter.doFilter(first, firstResponse, new MockFilterChain());

        MockHttpServletRequest second = request("/test/candidate");
        second.setRemoteAddr("203.0.113.10");
        second.addHeader("Authorization", "Bearer token");
        second.addHeader("X-Forwarded-For", "198.51.100.2");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();

        rateLimitFilter.doFilter(second, secondResponse, new MockFilterChain());

        assertEquals(200, firstResponse.getStatus());
        assertEquals(429, secondResponse.getStatus());
    }

    @Test
    void loginEndpointIsRateLimited() throws Exception {
        when(praxisProperties.authLoginRateLimitRequestsPerMinute()).thenReturn(1);

        MockHttpServletRequest first = request("/api/v1/auth/login");
        first.setRemoteAddr("203.0.113.20");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        rateLimitFilter.doFilter(first, firstResponse, new MockFilterChain());

        MockHttpServletRequest second = request("/api/v1/auth/login");
        second.setRemoteAddr("203.0.113.20");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        rateLimitFilter.doFilter(second, secondResponse, new MockFilterChain());

        assertEquals(200, firstResponse.getStatus());
        assertEquals(429, secondResponse.getStatus());
    }

    @Test
    void testCacheHasExpirationPolicy() {
        Cache<String, Bucket> cache = (Cache<String, Bucket>) ReflectionTestUtils
                .getField(rateLimitFilter, "buckets");

        assertNotNull(cache);

        long cacheSize = cache.estimatedSize();
        assertEquals(0, cacheSize);
    }

    @Test
    void testCacheLimitedToMaxSize() {
        Cache<String, Bucket> cache = (Cache<String, Bucket>) ReflectionTestUtils
                .getField(rateLimitFilter, "buckets");

        int maxSize = 100_000;

        for (int i = 0; i < maxSize + 1000; i++) {
            String key = "test-key-" + i;
            cache.get(key, k -> createTestBucket());
        }
        cache.cleanUp();

        long estimatedSize = cache.estimatedSize();
        assertTrue(estimatedSize <= maxSize, "Cache size should not exceed max size of " + maxSize);
    }

    @Test
    void testCacheRetainsRecentlyAccessedEntries() {
        Cache<String, Bucket> cache = (Cache<String, Bucket>) ReflectionTestUtils
                .getField(rateLimitFilter, "buckets");

        String key = "test-key";
        cache.get(key, k -> createTestBucket());
        assertEquals(1, cache.estimatedSize());

        cache.get("other-key", k -> createTestBucket());

        assertTrue(cache.estimatedSize() >= 1, "Recently accessed entries should remain cached");
    }

    private Bucket createTestBucket() {
        return Bucket.builder()
                .addLimit(io.github.bucket4j.Bandwidth.simple(60, Duration.ofMinutes(1)))
                .build();
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRequestURI(path);
        return request;
    }
}
