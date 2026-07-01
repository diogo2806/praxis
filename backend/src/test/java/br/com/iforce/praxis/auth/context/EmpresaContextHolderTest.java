package br.com.iforce.praxis.auth.context;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

class EmpresaContextHolderTest {

    @BeforeEach
    void setUp() {
        EmpresaContextHolder.clear();
    }

    @Test
    void testSetAndGet() {
        EmpresaContextHolder.set("empresa-123");
        assertEquals("empresa-123", EmpresaContextHolder.get());
    }

    @Test
    void testGetReturnsNullWhenNotSet() {
        assertNull(EmpresaContextHolder.get());
    }

    @Test
    void testGetRequiredThrowsWhenNotSet() {
        assertThrows(IllegalStateException.class, EmpresaContextHolder::getRequired);
    }

    @Test
    void testGetRequiredReturnsValueWhenSet() {
        EmpresaContextHolder.set("empresa-456");
        assertEquals("empresa-456", EmpresaContextHolder.getRequired());
    }

    @Test
    void testClear() {
        EmpresaContextHolder.set("empresa-789");
        EmpresaContextHolder.clear();
        assertNull(EmpresaContextHolder.get());
    }

    @Test
    void testThreadIsolation() throws InterruptedException {
        EmpresaContextHolder.set("empresa-main");

        Thread thread = new Thread(() -> {
            EmpresaContextHolder.set("empresa-other");
            assertEquals("empresa-other", EmpresaContextHolder.get());
        });

        thread.start();
        thread.join();

        assertEquals("empresa-main", EmpresaContextHolder.get());
    }
}
