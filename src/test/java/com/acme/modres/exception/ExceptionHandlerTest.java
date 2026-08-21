package com.acme.modres.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import jakarta.servlet.ServletException;
import java.util.logging.Logger;

/**
 * Tests for ExceptionHandler class.
 */
public class ExceptionHandlerTest {

    private static final Logger logger = Logger.getLogger(ExceptionHandlerTest.class.getName());

    @Test
    void testHandleException_withNullException_throwsServletException() {
        assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(null, "Test error message", logger)
        );
    }

    @Test
    void testHandleException_withNullException_messageInException() {
        ServletException thrown = assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(null, "Test error message", logger)
        );
        assertEquals("Test error message", thrown.getMessage());
    }

    @Test
    void testHandleException_withException_throwsServletException() {
        Exception cause = new RuntimeException("Root cause");
        assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(cause, "Wrapper error message", logger)
        );
    }

    @Test
    void testHandleException_withException_messageInException() {
        Exception cause = new RuntimeException("Root cause");
        ServletException thrown = assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(cause, "Wrapper error message", logger)
        );
        assertEquals("Wrapper error message", thrown.getMessage());
    }

    @Test
    void testHandleException_withException_causeIsSet() {
        Exception cause = new RuntimeException("Root cause");
        ServletException thrown = assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(cause, "Wrapper error message", logger)
        );
        assertNotNull(thrown.getCause());
        assertEquals("Root cause", thrown.getCause().getMessage());
    }

    @Test
    void testHandleException_withNullException_causeIsNull() {
        ServletException thrown = assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(null, "Error message", logger)
        );
        assertNull(thrown.getCause());
    }

    @Test
    void testHandleException_withIOException_throwsServletException() {
        Exception cause = new java.io.IOException("IO error");
        assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(cause, "IO error occurred", logger)
        );
    }

    @Test
    void testHandleException_withEmptyMessage_throwsServletException() {
        assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(null, "", logger)
        );
    }

    @Test
    void testHandleException_withNullMessage_throwsServletException() {
        assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(null, null, logger)
        );
    }

    @Test
    void testHandleException_withIllegalArgumentException() {
        Exception cause = new IllegalArgumentException("Bad argument");
        ServletException thrown = assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(cause, "Illegal argument", logger)
        );
        assertNotNull(thrown.getCause());
        assertTrue(thrown.getCause() instanceof IllegalArgumentException);
    }
}
