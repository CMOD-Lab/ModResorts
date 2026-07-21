package com.acme.modres;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecondFilterTest {

    private SecondFilter secondFilter;

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
        secondFilter = new SecondFilter();
    }

    @Test
    void testInit_doesNotThrow() throws Exception {
        assertDoesNotThrow(() -> secondFilter.init(mockFilterConfig));
    }

    @Test
    void testDestroy_doesNotThrow() {
        assertDoesNotThrow(() -> secondFilter.destroy());
    }

    @Test
    void testDoFilter_withContent_writesContent() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        BufferedReader reader = new BufferedReader(new StringReader("Hello"));

        when(mockRequest.getReader()).thenReturn(reader);
        when(mockResponse.getWriter()).thenReturn(printWriter);

        secondFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        verify(mockResponse).setContentType("text/plain");
        verify(mockFilterChain).doFilter(mockRequest, mockResponse);
        String output = stringWriter.toString();
        assertTrue(output.contains("Hello"));
        assertTrue(output.contains("to our site!"));
    }

    @Test
    void testDoFilter_withEmptyContent_writesEmptyPlusMessage() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        BufferedReader reader = new BufferedReader(new StringReader(""));

        when(mockRequest.getReader()).thenReturn(reader);
        when(mockResponse.getWriter()).thenReturn(printWriter);

        secondFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        verify(mockFilterChain).doFilter(mockRequest, mockResponse);
        String output = stringWriter.toString();
        assertTrue(output.contains("to our site!"));
    }

    @Test
    void testDoFilter_setsContentTypePlain() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        BufferedReader reader = new BufferedReader(new StringReader("test"));

        when(mockRequest.getReader()).thenReturn(reader);
        when(mockResponse.getWriter()).thenReturn(printWriter);

        secondFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        verify(mockResponse).setContentType("text/plain");
    }

    @Test
    void testDoFilter_callsChainDoFilter() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        BufferedReader reader = new BufferedReader(new StringReader("data"));

        when(mockRequest.getReader()).thenReturn(reader);
        when(mockResponse.getWriter()).thenReturn(printWriter);

        secondFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        verify(mockFilterChain, times(1)).doFilter(mockRequest, mockResponse);
    }
}
