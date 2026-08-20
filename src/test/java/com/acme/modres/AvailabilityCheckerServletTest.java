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
public class AvailabilityCheckerServletTest {

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  private AvailabilityCheckerServlet servlet;
  private StringWriter stringWriter;
  private PrintWriter writer;

  @BeforeEach
  public void setUp() throws Exception {
    servlet = new AvailabilityCheckerServlet();
    servlet.init();
    stringWriter = new StringWriter();
    writer = new PrintWriter(stringWriter);
    when(response.getWriter()).thenReturn(writer);
  }

  @Test
  public void testDoGetWithNullDate() throws Exception {
    when(request.getParameter("date")).thenReturn(null);

    servlet.doGet(request, response);

    verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    verify(response).setContentType("application/json");
  }

  @Test
  public void testDoGetWithEmptyDate() throws Exception {
    when(request.getParameter("date")).thenReturn("");

    servlet.doGet(request, response);

    verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    verify(response).setContentType("application/json");
  }

  @Test
  public void testDoGetWithInvalidDateFormat() throws Exception {
    when(request.getParameter("date")).thenReturn("12/31/2024");

    servlet.doGet(request, response);

    verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    verify(response).setContentType("application/json");
  }

  @Test
  public void testDoGetWithValidDate() throws Exception {
    when(request.getParameter("date")).thenReturn("2024-12-31");

    servlet.doGet(request, response);

    verify(response).setContentType("application/json");
    verify(response).setCharacterEncoding("UTF-8");
  }

  @Test
  public void testDoGetWithSQLInjectionAttempt() throws Exception {
    when(request.getParameter("date")).thenReturn("2024-01-01'; DROP TABLE users; --");

    servlet.doGet(request, response);

    verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
  }

  @Test
  public void testDoPostCallsDoGet() throws Exception {
    when(request.getParameter("date")).thenReturn("2024-06-15");

    servlet.doPost(request, response);

    verify(response).setContentType("application/json");
  }
}
