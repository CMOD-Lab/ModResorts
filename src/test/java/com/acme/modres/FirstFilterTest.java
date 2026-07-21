package com.acme.modres;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FirstFilterTest {

    private FirstFilter firstFilter;

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;

    @Mock
    private FilterChain mockFilterChain;

    @Mock
    private FilterConfig mockFilterConfig;

    @BeforeEach
    void setUp() {
        firstFilter = new FirstFilter();
    }

    @Test
    void testInit_doesNotThrow() throws Exception {
        assertDoesNotThrow(() -> firstFilter.init(mockFilterConfig));
    }

    @Test
    void testDestroy_doesNotThrow() {
        assertDoesNotThrow(() -> firstFilter.destroy());
    }

    @Test
    void testDoFilter_withUserParameter_writesWelcomeMessage() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(mockRequest.getParameter("user")).thenReturn("Alice");
        when(mockResponse.getWriter()).thenReturn(printWriter);

        firstFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        verify(mockResponse).setContentType("text/plain");
        verify(mockFilterChain).doFilter(mockRequest, mockResponse);
        assertTrue(stringWriter.toString().contains("Alice"));
    }

    @Test
    void testDoFilter_withNullUser_usesDefaultUser() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(mockRequest.getParameter("user")).thenReturn(null);
        when(mockResponse.getWriter()).thenReturn(printWriter);

        firstFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        verify(mockResponse).setContentType("text/plain");
        verify(mockFilterChain).doFilter(mockRequest, mockResponse);
        assertTrue(stringWriter.toString().contains("defaultUser"));
    }

    @Test
    void testDoFilter_setsContentTypePlain() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(mockRequest.getParameter("user")).thenReturn("Bob");
        when(mockResponse.getWriter()).thenReturn(printWriter);

        firstFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        verify(mockResponse).setContentType("text/plain");
    }

    @Test
    void testDoFilter_callsChainDoFilter() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(mockRequest.getParameter("user")).thenReturn("TestUser");
        when(mockResponse.getWriter()).thenReturn(printWriter);

        firstFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        verify(mockFilterChain, times(1)).doFilter(mockRequest, mockResponse);
    }

    @Test
    void testDoFilter_welcomeMessageFormat() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(mockRequest.getParameter("user")).thenReturn("Charlie");
        when(mockResponse.getWriter()).thenReturn(printWriter);

        firstFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        String output = stringWriter.toString();
        assertTrue(output.startsWith("Welcome"));
        assertTrue(output.contains("Charlie"));
    }
}
