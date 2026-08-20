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
public class UpperServletTest {

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  private UpperServlet servlet;
  private StringWriter stringWriter;
  private PrintWriter writer;

  @BeforeEach
  public void setUp() throws Exception {
    servlet = new UpperServlet();
    stringWriter = new StringWriter();
    writer = new PrintWriter(stringWriter);
    when(response.getWriter()).thenReturn(writer);
  }

  @Test
  public void testDoGetWithValidInput() throws Exception {
    when(request.getParameter("input")).thenReturn("hello world");

    servlet.doGet(request, response);

    verify(response).setContentType("text/html");
    writer.flush();
    String output = stringWriter.toString();
    assert(output.contains("HELLO WORLD"));
  }

  @Test
  public void testDoGetWithNullInput() throws Exception {
    when(request.getParameter("input")).thenReturn(null);

    servlet.doGet(request, response);

    verify(response).setContentType("text/html");
    writer.flush();
    assert(stringWriter.toString().contains("No input provided"));
  }

  @Test
  public void testDoGetWithEmptyInput() throws Exception {
    when(request.getParameter("input")).thenReturn("");

    servlet.doGet(request, response);

    verify(response).setContentType("text/html");
    writer.flush();
    assert(stringWriter.toString().contains("No input provided"));
  }

  @Test
  public void testDoGetWithXSSAttempt() throws Exception {
    when(request.getParameter("input")).thenReturn("<script>alert('test')</script>");

    servlet.doGet(request, response);

    verify(response).setContentType("text/html");
    writer.flush();
    String output = stringWriter.toString();
    // Verify XSS is escaped
    assert(!output.contains("<SCRIPT>"));
    assert(output.contains("&lt;SCRIPT&gt;"));
  }

  @Test
  public void testDoGetWithLongInput() throws Exception {
    String longInput = "a".repeat(1001);
    when(request.getParameter("input")).thenReturn(longInput);

    servlet.doGet(request, response);

    verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    writer.flush();
    assert(stringWriter.toString().contains("Input too long"));
  }

  @Test
  public void testDoGetWithMaxLengthInput() throws Exception {
    String maxInput = "a".repeat(1000);
    when(request.getParameter("input")).thenReturn(maxInput);

    servlet.doGet(request, response);

    verify(response).setContentType("text/html");
    writer.flush();
    String output = stringWriter.toString();
    assert(output.contains("A".repeat(1000)));
  }
}
