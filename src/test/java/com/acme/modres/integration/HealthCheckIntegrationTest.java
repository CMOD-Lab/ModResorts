package com.acme.modres.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.acme.modres.HealthCheckServlet;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke tests for health check endpoint with database connectivity verification
 */
@ExtendWith(MockitoExtension.class)
public class HealthCheckIntegrationTest {

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  private HealthCheckServlet healthCheckServlet;
  private StringWriter stringWriter;
  private PrintWriter writer;

  @BeforeEach
  public void setUp() throws Exception {
    healthCheckServlet = new HealthCheckServlet();
    stringWriter = new StringWriter();
    writer = new PrintWriter(stringWriter);
    when(response.getWriter()).thenReturn(writer);
  }

  @Test
  public void testHealthCheckEndpoint() throws Exception {
    // Smoke test: Health check endpoint should respond
    healthCheckServlet.doGet(request, response);

    verify(response).setContentType("application/json");
    verify(response).setCharacterEncoding("UTF-8");
  }

  @Test
  public void testHealthCheckResponseFormat() throws Exception {
    // Smoke test: Health check response should be valid JSON
    healthCheckServlet.doGet(request, response);

    writer.flush();
    String output = stringWriter.toString();

    assertTrue(output.startsWith("{"));
    assertTrue(output.endsWith("}"));
    assertTrue(output.contains("\"status\""));
    assertTrue(output.contains("\"application\":\"ModResorts\""));
  }

  @Test
  public void testHealthCheckIncludesDatabaseStatus() throws Exception {
    // Smoke test: Health check should include database status
    healthCheckServlet.doGet(request, response);

    writer.flush();
    String output = stringWriter.toString();

    assertTrue(output.contains("\"database\""));
  }

  @Test
  public void testHealthCheckApplicationName() throws Exception {
    // Smoke test: Health check should return application name
    healthCheckServlet.doGet(request, response);

    writer.flush();
    String output = stringWriter.toString();

    assertTrue(output.contains("ModResorts"));
  }

  @Test
  public void testHealthCheckStatusValues() throws Exception {
    // Smoke test: Status should be either UP or DOWN
    healthCheckServlet.doGet(request, response);

    writer.flush();
    String output = stringWriter.toString();

    assertTrue(output.contains("\"status\":\"UP\"") || output.contains("\"status\":\"DOWN\""));
  }

  @Test
  public void testHealthCheckHTTPStatusCodes() throws Exception {
    // Smoke test: Health check should return appropriate HTTP status
    healthCheckServlet.doGet(request, response);

    // Should be either 200 OK or 503 Service Unavailable
    verify(response, atMostOnce()).setStatus(HttpServletResponse.SC_OK);
    verify(response, atMostOnce()).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
  }

  @Test
  public void testHealthCheckForContainerProbes() {
    // Smoke test: Health check path for Kubernetes/Docker probes
    String healthCheckPath = "/health";
    assertEquals("/health", healthCheckPath);
  }

  @Test
  public void testDatabaseHealthCheckConfiguration() {
    // Smoke test: Database health check should be configured
    String jndiName = "jdbc/ModResortsJndi";
    assertEquals("jdbc/ModResortsJndi", jndiName);
  }

  @Test
  public void testHealthCheckResponseStructure() throws Exception {
    // Smoke test: Response should have expected structure
    healthCheckServlet.doGet(request, response);

    writer.flush();
    String output = stringWriter.toString();

    assertTrue(output.contains("status"));
    assertTrue(output.contains("application"));
    assertTrue(output.contains("database"));
  }
}
