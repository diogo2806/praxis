package br.com.iforce.praxis.auth.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TenantContextHolderTest {

    @BeforeEach
    void setUp() {
        TenantContextHolder.clear();
    }

    @Test
    void testSetAndGet() {
        TenantContextHolder.set("tenant-123");
        assertEquals("tenant-123", TenantContextHolder.get());
    }

    @Test
    void testGetReturnsNullWhenNotSet() {
        assertNull(TenantContextHolder.get());
    }

    @Test
    void testGetRequiredThrowsWhenNotSet() {
        assertThrows(IllegalStateException.class, TenantContextHolder::getRequired);
    }

    @Test
    void testGetRequiredReturnsValueWhenSet() {
        TenantContextHolder.set("tenant-456");
        assertEquals("tenant-456", TenantContextHolder.getRequired());
    }

    @Test
    void testClear() {
        TenantContextHolder.set("tenant-789");
        TenantContextHolder.clear();
        assertNull(TenantContextHolder.get());
    }

    @Test
    void testThreadIsolation() throws InterruptedException {
        TenantContextHolder.set("tenant-main");

        Thread thread = new Thread(() -> {
            TenantContextHolder.set("tenant-other");
            assertEquals("tenant-other", TenantContextHolder.get());
        });

        thread.start();
        thread.join();

        assertEquals("tenant-main", TenantContextHolder.get());
    }
}
