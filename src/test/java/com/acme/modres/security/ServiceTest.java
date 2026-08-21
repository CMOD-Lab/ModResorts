package com.acme.modres.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Service class.
 */
public class ServiceTest {

    private Service service;

    @BeforeEach
    void setUp() {
        service = new Service();
    }

    @Test
    void testConstructor_createsInstance() {
        assertNotNull(service);
    }

    @Test
    void testOperationConstant_notNull() {
        assertNotNull(Service.OPERATION);
    }

    @Test
    void testOperationConstant_value() {
        assertEquals("my-operation", Service.OPERATION);
    }

    @Test
    void testOperation_doesNotThrow() {
        assertDoesNotThrow(() -> service.operation());
    }

    @Test
    void testOperation_canBeCalledMultipleTimes() {
        assertDoesNotThrow(() -> {
            service.operation();
            service.operation();
            service.operation();
        });
    }

    @Test
    void testService_isInstantiable() {
        Service s1 = new Service();
        Service s2 = new Service();
        assertNotNull(s1);
        assertNotNull(s2);
        assertNotSame(s1, s2);
    }
}
