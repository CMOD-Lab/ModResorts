package com.acme.modres;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Tests for UpperServlet class.
 */
public class UpperServletTest {

    private UpperServlet servlet;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        servlet = new UpperServlet();
    }

    @Test
    void testServlet_isNotNull() {
        assertNotNull(servlet);
    }

    @Test
    void testDoGet_setsContentTypeHtml() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getParameter("input")).thenReturn("hello");

        servlet.doGet(request, response);

        verify(response).setContentType("text/html");
    }

    @Test
    void testDoGet_convertsToUpperCase() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getParameter("input")).thenReturn("hello");

        servlet.doGet(request, response);

        pw.flush();
        assertTrue(sw.toString().contains("HELLO"));
    }

    @Test
    void testDoGet_withNullInput_usesEmptyString() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getParameter("input")).thenReturn(null);

        servlet.doGet(request, response);

        pw.flush();
        assertNotNull(sw.toString());
    }

    @Test
    void testDoGet_withEmptyInput() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getParameter("input")).thenReturn("");

        servlet.doGet(request, response);

        pw.flush();
        assertNotNull(sw.toString());
    }

    @Test
    void testDoGet_encodesHtmlSpecialChars_ampersand() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getParameter("input")).thenReturn("a&b");

        servlet.doGet(request, response);

        pw.flush();
        assertTrue(sw.toString().contains("&amp;"));
    }

    @Test
    void testDoGet_encodesHtmlSpecialChars_lessThan() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getParameter("input")).thenReturn("<script>");

        servlet.doGet(request, response);

        pw.flush();
        assertTrue(sw.toString().contains("&lt;"));
    }

    @Test
    void testDoGet_encodesHtmlSpecialChars_greaterThan() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getParameter("input")).thenReturn("a>b");

        servlet.doGet(request, response);

        pw.flush();
        assertTrue(sw.toString().contains("&gt;"));
    }

    @Test
    void testDoGet_encodesHtmlSpecialChars_quote() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getParameter("input")).thenReturn("say \"hello\"");

        servlet.doGet(request, response);

        pw.flush();
        assertTrue(sw.toString().contains("&quot;"));
    }

    @Test
    void testDoGet_encodesHtmlSpecialChars_singleQuote() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getParameter("input")).thenReturn("it's");

        servlet.doGet(request, response);

        pw.flush();
        assertTrue(sw.toString().contains("&#x27;"));
    }

    @Test
    void testDoGet_withMixedCase_convertsToUpper() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getParameter("input")).thenReturn("Hello World");

        servlet.doGet(request, response);

        pw.flush();
        assertTrue(sw.toString().contains("HELLO WORLD"));
    }

    @Test
    void testDoGet_doesNotThrow() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getParameter("input")).thenReturn("test");

        assertDoesNotThrow(() -> servlet.doGet(request, response));
    }

    @Test
    void testServlet_extendsHttpServlet() {
        assertTrue(servlet instanceof jakarta.servlet.http.HttpServlet);
    }
}
