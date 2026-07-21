package com.acme.modres.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

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
    void testOperationConstant_hasCorrectValue() {
        assertEquals("my-operation", Service.OPERATION);
    }

    @Test
    void testOperation_doesNotThrow() {
        assertDoesNotThrow(() -> service.operation());
    }

    @Test
    void testOperation_printsMessage() {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        try {
            service.operation();
            String output = outContent.toString();
            assertTrue(output.contains("Operation is executed"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testOperationConstant_isStatic() throws Exception {
        java.lang.reflect.Field field = Service.class.getField("OPERATION");
        assertTrue(java.lang.reflect.Modifier.isStatic(field.getModifiers()));
    }

    @Test
    void testOperationConstant_isFinal() throws Exception {
        java.lang.reflect.Field field = Service.class.getField("OPERATION");
        assertTrue(java.lang.reflect.Modifier.isFinal(field.getModifiers()));
    }
}
