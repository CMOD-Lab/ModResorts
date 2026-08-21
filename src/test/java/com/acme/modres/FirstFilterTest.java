package com.acme.modres;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Tests for FirstFilter class.
 */
public class FirstFilterTest {

    private FirstFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @Mock
    private FilterConfig filterConfig;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        filter = new FirstFilter();
    }

    @Test
    void testFilter_isNotNull() {
        assertNotNull(filter);
    }

    @Test
    void testInit_doesNotThrow() {
        assertDoesNotThrow(() -> filter.init(filterConfig));
    }

    @Test
    void testDestroy_doesNotThrow() {
        assertDoesNotThrow(() -> filter.destroy());
    }

    @Test
    void testDoFilter_setsContentType() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getParameter("user")).thenReturn("testUser");

        filter.doFilter(request, response, chain);

        verify(response).setContentType("text/plain");
    }

    @Test
    void testDoFilter_withUser_writesWelcomeMessage() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getParameter("user")).thenReturn("Alice");

        filter.doFilter(request, response, chain);

        pw.flush();
        assertTrue(sw.toString().contains("Welcome Alice"));
    }

    @Test
    void testDoFilter_withNullUser_usesDefaultUser() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getParameter("user")).thenReturn(null);

        filter.doFilter(request, response, chain);

        pw.flush();
        assertTrue(sw.toString().contains("Welcome defaultUser"));
    }

    @Test
    void testDoFilter_callsChain() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getParameter("user")).thenReturn("testUser");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void testDoFilter_doesNotThrow() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getParameter("user")).thenReturn("testUser");

        assertDoesNotThrow(() -> filter.doFilter(request, response, chain));
    }

    @Test
    void testFilter_implementsFilter() {
        assertTrue(filter instanceof jakarta.servlet.Filter);
    }
}
