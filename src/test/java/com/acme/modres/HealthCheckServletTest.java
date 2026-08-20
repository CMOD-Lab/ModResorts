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
public class HealthCheckServletTest {

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  private HealthCheckServlet servlet;
  private StringWriter stringWriter;
  private PrintWriter writer;

  @BeforeEach
  public void setUp() throws Exception {
    servlet = new HealthCheckServlet();
    stringWriter = new StringWriter();
    writer = new PrintWriter(stringWriter);
    when(response.getWriter()).thenReturn(writer);
  }

  @Test
  public void testDoGetReturnsHealthStatus() throws Exception {
    servlet.doGet(request, response);

    verify(response).setContentType("application/json");
    verify(response).setCharacterEncoding("UTF-8");
    writer.flush();
    String output = stringWriter.toString();
    assert(output.contains("\"application\":\"ModResorts\""));
    assert(output.contains("\"status\""));
  }

  @Test
  public void testDoGetIncludesDatabaseStatus() throws Exception {
    servlet.doGet(request, response);

    writer.flush();
    String output = stringWriter.toString();
    assert(output.contains("\"database\""));
  }

  @Test
  public void testHealthCheckResponseIsJSON() throws Exception {
    servlet.doGet(request, response);

    verify(response).setContentType("application/json");
    writer.flush();
    String output = stringWriter.toString();
    assert(output.startsWith("{"));
    assert(output.endsWith("}"));
  }

  @Test
  public void testHealthCheckResponseContainsStatus() throws Exception {
    servlet.doGet(request, response);

    writer.flush();
    String output = stringWriter.toString();
    assert(output.contains("\"status\""));
  }
}
