package br.com.iforce.praxis.shared.web;

import br.com.iforce.praxis.config.PraxisProperties;
import com.github.benmanes.caffeine.cache.Cache;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

        rateLimitFilter = new RateLimitFilter(praxisProperties);
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

        long estimatedSize = cache.estimatedSize();
        assertTrue(estimatedSize <= maxSize, "Cache size should not exceed max size of " + maxSize);
    }

    @Test
    void testCacheEvictionOnAccess() throws InterruptedException {
        Cache<String, Bucket> cache = (Cache<String, Bucket>) ReflectionTestUtils
                .getField(rateLimitFilter, "buckets");

        String key = "test-key";
        cache.get(key, k -> createTestBucket());
        assertEquals(1, cache.estimatedSize());

        Thread.sleep(1100);

        cache.get("other-key", k -> createTestBucket());

        assertTrue(cache.estimatedSize() <= 1, "Entry should be evicted due to inactivity");
    }

    private Bucket createTestBucket() {
        return Bucket.builder()
                .addLimit(io.github.bucket4j.Bandwidth.simple(60, Duration.ofMinutes(1)))
                .build();
    }
}
