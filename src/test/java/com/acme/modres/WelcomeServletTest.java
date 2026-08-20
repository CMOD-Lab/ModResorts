package com.acme.modres;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.PrintWriter;
import java.io.StringWriter;

@ExtendWith(MockitoExtension.class)
public class WelcomeServletTest {

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  private WelcomeServlet servlet;
  private StringWriter stringWriter;
  private PrintWriter writer;

  @BeforeEach
  public void setUp() throws Exception {
    servlet = new WelcomeServlet();
    stringWriter = new StringWriter();
    writer = new PrintWriter(stringWriter);
    when(response.getWriter()).thenReturn(writer);
  }

  @Test
  public void testDoGetWithNoUser() throws Exception {
    when(request.getParameter("user")).thenReturn(null);

    servlet.doGet(request, response);

    verify(response).setContentType("text/plain");
    writer.flush();
    assert(stringWriter.toString().contains("Welcome"));
  }

  @Test
  public void testDoGetWithValidUser() throws Exception {
    when(request.getParameter("user")).thenReturn("John");

    servlet.doGet(request, response);

    verify(response).setContentType("text/plain");
    writer.flush();
    assert(stringWriter.toString().contains("Welcome John"));
  }

  @Test
  public void testDoGetWithXSSAttempt() throws Exception {
    when(request.getParameter("user")).thenReturn("<script>alert('XSS')</script>");

    servlet.doGet(request, response);

    verify(response).setContentType("text/plain");
    writer.flush();
    String output = stringWriter.toString();
    // Verify XSS is escaped
    assert(!output.contains("<script>"));
    assert(output.contains("&lt;script&gt;"));
  }

  @Test
  public void testDoGetWithEmptyUser() throws Exception {
    when(request.getParameter("user")).thenReturn("");

    servlet.doGet(request, response);

    verify(response).setContentType("text/plain");
    writer.flush();
    assert(stringWriter.toString().contains("Welcome!"));
  }

  @Test
  public void testDoGetWithSpecialCharacters() throws Exception {
    when(request.getParameter("user")).thenReturn("John & Jane");

    servlet.doGet(request, response);

    verify(response).setContentType("text/plain");
    writer.flush();
    String output = stringWriter.toString();
    assert(output.contains("&amp;"));
  }
}
