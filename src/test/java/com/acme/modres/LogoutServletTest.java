package com.acme.modres;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Tests for LogoutServlet class.
 */
public class LogoutServletTest {

    private LogoutServlet servlet;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        servlet = new LogoutServlet();
    }

    @Test
    void testServlet_isNotNull() {
        assertNotNull(servlet);
    }

    @Test
    void testDoGet_withActiveSession_invalidatesSession() throws Exception {
        when(request.getSession(false)).thenReturn(session);

        servlet.doGet(request, response);

        verify(session).invalidate();
    }

    @Test
    void testDoGet_withNoSession_doesNotThrow() throws Exception {
        when(request.getSession(false)).thenReturn(null);

        assertDoesNotThrow(() -> servlet.doGet(request, response));
    }

    @Test
    void testDoGet_redirectsToLoginPage() throws Exception {
        when(request.getSession(false)).thenReturn(null);

        servlet.doGet(request, response);

        verify(response).sendRedirect("login.jsp");
    }

    @Test
    void testDoGet_withActiveSession_redirectsToLoginPage() throws Exception {
        when(request.getSession(false)).thenReturn(session);

        servlet.doGet(request, response);

        verify(response).sendRedirect("login.jsp");
    }

    @Test
    void testServlet_extendsHttpServlet() {
        assertTrue(servlet instanceof jakarta.servlet.http.HttpServlet);
    }

    @Test
    void testDoGet_withSessionThatThrowsException_stillRedirects() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        doThrow(new RuntimeException("Session error")).when(session).invalidate();

        // Should not throw, should still redirect
        assertDoesNotThrow(() -> servlet.doGet(request, response));
        verify(response).sendRedirect("login.jsp");
    }
}
