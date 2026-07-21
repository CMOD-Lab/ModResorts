package com.acme.modres;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UpperServletTest {

    private UpperServlet upperServlet;

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;

    @BeforeEach
    void setUp() {
        upperServlet = new UpperServlet();
    }

    @Test
    void testDoGet_withInput_returnsUpperCase() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(mockRequest.getParameter("input")).thenReturn("hello");
        when(mockResponse.getWriter()).thenReturn(printWriter);

        upperServlet.doGet(mockRequest, mockResponse);

        verify(mockResponse).setContentType("text/html");
        String output = stringWriter.toString();
        assertTrue(output.contains("HELLO"));
    }

    @Test
    void testDoGet_withNullInput_returnsEmpty() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(mockRequest.getParameter("input")).thenReturn(null);
        when(mockResponse.getWriter()).thenReturn(printWriter);

        upperServlet.doGet(mockRequest, mockResponse);

        verify(mockResponse).setContentType("text/html");
        String output = stringWriter.toString();
        assertNotNull(output);
    }

    @Test
    void testDoGet_withMixedCase_returnsAllUpperCase() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(mockRequest.getParameter("input")).thenReturn("HeLLo WoRLd");
        when(mockResponse.getWriter()).thenReturn(printWriter);

        upperServlet.doGet(mockRequest, mockResponse);

        String output = stringWriter.toString();
        assertTrue(output.contains("HELLO WORLD"));
    }

    @Test
    void testDoGet_withHtmlSpecialChars_encodesHtml() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(mockRequest.getParameter("input")).thenReturn("<script>");
        when(mockResponse.getWriter()).thenReturn(printWriter);

        upperServlet.doGet(mockRequest, mockResponse);

        String output = stringWriter.toString();
        assertFalse(output.contains("<SCRIPT>"));
        assertTrue(output.contains("&lt;SCRIPT&gt;"));
    }

    @Test
    void testDoGet_withAmpersand_encodesHtml() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(mockRequest.getParameter("input")).thenReturn("a&b");
        when(mockResponse.getWriter()).thenReturn(printWriter);

        upperServlet.doGet(mockRequest, mockResponse);

        String output = stringWriter.toString();
        assertTrue(output.contains("&amp;"));
    }

    @Test
    void testDoGet_withQuotes_encodesHtml() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(mockRequest.getParameter("input")).thenReturn("say \"hello\"");
        when(mockResponse.getWriter()).thenReturn(printWriter);

        upperServlet.doGet(mockRequest, mockResponse);

        String output = stringWriter.toString();
        assertTrue(output.contains("&quot;"));
    }

    @Test
    void testEncodeHtml_withNullInput_returnsEmpty() throws Exception {
        Method encodeHtml = UpperServlet.class.getDeclaredMethod("encodeHtml", String.class);
        encodeHtml.setAccessible(true);
        String result = (String) encodeHtml.invoke(upperServlet, (Object) null);
        assertEquals("", result);
    }

    @Test
    void testEncodeHtml_withAmpersand() throws Exception {
        Method encodeHtml = UpperServlet.class.getDeclaredMethod("encodeHtml", String.class);
        encodeHtml.setAccessible(true);
        String result = (String) encodeHtml.invoke(upperServlet, "a&b");
        assertEquals("a&amp;b", result);
    }

    @Test
    void testEncodeHtml_withLessThan() throws Exception {
        Method encodeHtml = UpperServlet.class.getDeclaredMethod("encodeHtml", String.class);
        encodeHtml.setAccessible(true);
        String result = (String) encodeHtml.invoke(upperServlet, "<div>");
        assertEquals("&lt;div&gt;", result);
    }

    @Test
    void testEncodeHtml_withSingleQuote() throws Exception {
        Method encodeHtml = UpperServlet.class.getDeclaredMethod("encodeHtml", String.class);
        encodeHtml.setAccessible(true);
        String result = (String) encodeHtml.invoke(upperServlet, "it's");
        assertEquals("it&#x27;s", result);
    }

    @Test
    void testDoGet_setsContentTypeHtml() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(mockRequest.getParameter("input")).thenReturn("test");
        when(mockResponse.getWriter()).thenReturn(printWriter);

        upperServlet.doGet(mockRequest, mockResponse);

        verify(mockResponse).setContentType("text/html");
    }
}
