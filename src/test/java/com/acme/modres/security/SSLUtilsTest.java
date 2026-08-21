package com.acme.modres.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SSLUtils class.
 */
public class SSLUtilsTest {

    @Test
    void testSSLUtils_canBeInstantiated() {
        SSLUtils sslUtils = new SSLUtils();
        assertNotNull(sslUtils);
    }

    @Test
    void testSSLUtils_classExists() {
        assertNotNull(SSLUtils.class);
    }

    @Test
    void testSSLUtils_multipleInstances() {
        SSLUtils s1 = new SSLUtils();
        SSLUtils s2 = new SSLUtils();
        assertNotNull(s1);
        assertNotNull(s2);
        assertNotSame(s1, s2);
    }
}
