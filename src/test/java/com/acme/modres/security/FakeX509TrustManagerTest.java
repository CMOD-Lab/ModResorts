package com.acme.modres.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FakeX509TrustManager class.
 */
public class FakeX509TrustManagerTest {

    @Test
    void testConstructor_createsInstance() {
        FakeX509TrustManager trustManager = new FakeX509TrustManager();
        assertNotNull(trustManager);
    }

    @Test
    void testClass_exists() {
        assertNotNull(FakeX509TrustManager.class);
    }

    @Test
    void testMultipleInstances_areDistinct() {
        FakeX509TrustManager tm1 = new FakeX509TrustManager();
        FakeX509TrustManager tm2 = new FakeX509TrustManager();
        assertNotNull(tm1);
        assertNotNull(tm2);
        assertNotSame(tm1, tm2);
    }
}
