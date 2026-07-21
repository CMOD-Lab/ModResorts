package com.acme.modres.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import jakarta.servlet.ServletException;
import java.util.logging.Logger;

public class ExceptionHandlerTest {

    private static final Logger logger = Logger.getLogger(ExceptionHandlerTest.class.getName());

    @Test
    void testHandleException_withNullException_throwsServletException() {
        String errorMsg = "Test error message";
        assertThrows(ServletException.class, () -> {
            ExceptionHandler.handleException(null, errorMsg, logger);
        });
    }

    @Test
    void testHandleException_withException_throwsServletException() {
        Exception cause = new RuntimeException("Root cause");
        String errorMsg = "Test error message";
        assertThrows(ServletException.class, () -> {
            ExceptionHandler.handleException(cause, errorMsg, logger);
        });
    }

    @Test
    void testHandleException_withNullException_messageInServletException() {
        String errorMsg = "Specific error message";
        try {
            ExceptionHandler.handleException(null, errorMsg, logger);
            fail("Expected ServletException");
        } catch (ServletException e) {
            assertEquals(errorMsg, e.getMessage());
        }
    }

    @Test
    void testHandleException_withException_causeInServletException() {
        RuntimeException cause = new RuntimeException("Root cause");
        String errorMsg = "Wrapper error message";
        try {
            ExceptionHandler.handleException(cause, errorMsg, logger);
            fail("Expected ServletException");
        } catch (ServletException e) {
            assertEquals(errorMsg, e.getMessage());
            assertEquals(cause, e.getCause());
        }
    }

    @Test
    void testHandleException_withIOException_throwsServletException() {
        java.io.IOException cause = new java.io.IOException("IO error");
        String errorMsg = "IO error occurred";
        assertThrows(ServletException.class, () -> {
            ExceptionHandler.handleException(cause, errorMsg, logger);
        });
    }

    @Test
    void testHandleException_withEmptyMessage_throwsServletException() {
        assertThrows(ServletException.class, () -> {
            ExceptionHandler.handleException(null, "", logger);
        });
    }
}
