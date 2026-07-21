package com.acme.modres;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LogoutServletTest {

    private LogoutServlet logoutServlet;

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;

    @Mock
    private HttpSession mockSession;

    @BeforeEach
    void setUp() {
        logoutServlet = new LogoutServlet();
    }

    @Test
    void testDoGet_withActiveSession_invalidatesSession() throws Exception {
        when(mockRequest.getSession(false)).thenReturn(mockSession);

        logoutServlet.doGet(mockRequest, mockResponse);

        verify(mockSession).invalidate();
        verify(mockResponse).sendRedirect("login.jsp");
    }

    @Test
    void testDoGet_withNoSession_redirectsToLogin() throws Exception {
        when(mockRequest.getSession(false)).thenReturn(null);

        logoutServlet.doGet(mockRequest, mockResponse);

        verify(mockSession, never()).invalidate();
        verify(mockResponse).sendRedirect("login.jsp");
    }

    @Test
    void testDoGet_redirectsToLoginJsp() throws Exception {
        when(mockRequest.getSession(false)).thenReturn(null);

        logoutServlet.doGet(mockRequest, mockResponse);

        verify(mockResponse).sendRedirect("login.jsp");
    }

    @Test
    void testDoGet_sessionInvalidateThrowsException_stillRedirects() throws Exception {
        when(mockRequest.getSession(false)).thenReturn(mockSession);
        doThrow(new RuntimeException("Session error")).when(mockSession).invalidate();

        // Should not throw, should still redirect
        assertDoesNotThrow(() -> logoutServlet.doGet(mockRequest, mockResponse));
    }
}
