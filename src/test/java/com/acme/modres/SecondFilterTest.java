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
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Tests for SecondFilter class.
 */
public class SecondFilterTest {

    private SecondFilter filter;

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
        filter = new SecondFilter();
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
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("Hello")));

        filter.doFilter(request, response, chain);

        verify(response).setContentType("text/plain");
    }

    @Test
    void testDoFilter_writesRequestContent() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("Hello")));

        filter.doFilter(request, response, chain);

        pw.flush();
        assertTrue(sw.toString().contains("Hello"));
    }

    @Test
    void testDoFilter_appendsSiteMessage() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("Welcome")));

        filter.doFilter(request, response, chain);

        pw.flush();
        assertTrue(sw.toString().contains("to our site!"));
    }

    @Test
    void testDoFilter_callsChain() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("test")));

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void testDoFilter_withEmptyBody() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("")));

        assertDoesNotThrow(() -> filter.doFilter(request, response, chain));
    }

    @Test
    void testDoFilter_withMultilineBody() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("line1\nline2")));

        filter.doFilter(request, response, chain);

        pw.flush();
        assertTrue(sw.toString().contains("line1line2"));
    }

    @Test
    void testFilter_implementsFilter() {
        assertTrue(filter instanceof jakarta.servlet.Filter);
    }
}
