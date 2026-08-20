package com.acme.modres;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@ExtendWith(MockitoExtension.class)
public class WeatherServletTest {

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private ServletOutputStream outputStream;

  private WeatherServlet weatherServlet;

  @BeforeEach
  public void setUp() {
    weatherServlet = new WeatherServlet();
  }

  @Test
  public void testDoGetWithNullCity() throws Exception {
    when(request.getParameter("selectedCity")).thenReturn(null);
    when(response.getOutputStream()).thenReturn(outputStream);

    weatherServlet.doGet(request, response);

    verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    verify(response).setContentType("application/json");
  }

  @Test
  public void testDoGetWithEmptyCity() throws Exception {
    when(request.getParameter("selectedCity")).thenReturn("");
    when(response.getOutputStream()).thenReturn(outputStream);

    weatherServlet.doGet(request, response);

    verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    verify(response).setContentType("application/json");
  }

  @Test
  public void testDoGetWithInvalidCityCharacters() throws Exception {
    when(request.getParameter("selectedCity")).thenReturn("City123!@#");
    when(response.getOutputStream()).thenReturn(outputStream);

    weatherServlet.doGet(request, response);

    verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    verify(response).setContentType("application/json");
  }

  @Test
  public void testDoGetWithValidCity() throws Exception {
    when(request.getParameter("selectedCity")).thenReturn("Paris");
    when(response.getOutputStream()).thenReturn(outputStream);

    weatherServlet.doGet(request, response);

    verify(response).setContentType("application/json");
  }

  @Test
  public void testDoPostCallsDoGet() throws Exception {
    when(request.getParameter("selectedCity")).thenReturn("Paris");
    when(response.getOutputStream()).thenReturn(outputStream);

    weatherServlet.doPost(request, response);

    verify(response).setContentType("application/json");
  }
}
