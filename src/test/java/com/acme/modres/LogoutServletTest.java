package com.acme.modres;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
public class LogoutServletTest {

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private HttpSession session;

  private LogoutServlet servlet;

  @BeforeEach
  public void setUp() {
    servlet = new LogoutServlet();
  }

  @Test
  public void testDoGetWithValidSession() throws Exception {
    when(request.getSession(false)).thenReturn(session);

    servlet.doGet(request, response);

    verify(session).invalidate();
    verify(response).sendRedirect("login.jsp");
  }

  @Test
  public void testDoGetWithNullSession() throws Exception {
    when(request.getSession(false)).thenReturn(null);

    servlet.doGet(request, response);

    verify(response).sendRedirect("login.jsp");
  }

  @Test
  public void testDoGetWithAlreadyInvalidatedSession() throws Exception {
    when(request.getSession(false)).thenReturn(session);
    doThrow(new IllegalStateException("Session already invalidated")).when(session).invalidate();

    servlet.doGet(request, response);

    verify(response).sendRedirect("login.jsp");
  }

  @Test
  public void testDoGetWithSessionInvalidationException() throws Exception {
    when(request.getSession(false)).thenReturn(session);
    doThrow(new RuntimeException("Test exception")).when(session).invalidate();

    servlet.doGet(request, response);

    verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }

  @Test
  public void testDoGetRedirectsToLoginPage() throws Exception {
    when(request.getSession(false)).thenReturn(session);

    servlet.doGet(request, response);

    verify(response).sendRedirect("login.jsp");
  }
}
