package com.acme.modres;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.annotation.Resource;
import jakarta.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Health check endpoint for container liveness/readiness probes.
 * Returns HTTP 200 with JSON status when the application is healthy.
 * Accessible at GET /health
 */
@WebServlet({ "/health" })
public class HealthCheckServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;
  private static final Logger logger = Logger.getLogger(HealthCheckServlet.class.getName());

  @Resource(lookup = "jdbc/ModResortsJndi")
  private DataSource dataSource;

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");

    StringBuilder healthStatus = new StringBuilder();
    healthStatus.append("{\"status\":\"UP\",\"application\":\"ModResorts\"");

    // Database health check
    boolean dbHealthy = checkDatabaseHealth();
    healthStatus.append(",\"database\":{\"status\":\"");
    healthStatus.append(dbHealthy ? "UP" : "DOWN");
    healthStatus.append("\"}");

    healthStatus.append("}");

    if (dbHealthy) {
      response.setStatus(HttpServletResponse.SC_OK);
    } else {
      response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    }

    PrintWriter out = response.getWriter();
    out.print(healthStatus.toString());
    out.flush();
  }

  private boolean checkDatabaseHealth() {
    if (dataSource == null) {
      logger.log(Level.WARNING, "DataSource is not configured");
      return false;
    }

    try (Connection conn = dataSource.getConnection()) {
      if (conn != null && !conn.isClosed()) {
        // Test connection with a simple validation query
        return conn.isValid(5); // 5 second timeout
      }
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Database health check failed", e);
      return false;
    }
    return false;
  }
}
