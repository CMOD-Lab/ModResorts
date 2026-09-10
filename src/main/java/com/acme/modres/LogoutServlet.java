package com.acme.modres;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * LogoutServlet - Updated for Java 17 / Jakarta EE compatibility.
 * The IBM WebSphere-specific WSSecurityHelper.revokeSSOCookies() has been
 * replaced with a standard Jakarta EE session invalidation approach.
 */
@WebServlet({ "/logout" })
public class LogoutServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  private static final Logger logger = Logger.getLogger(LogoutServlet.class.getName());

  @Override
  protected void doGet(HttpServletRequest request,
      HttpServletResponse response) throws IOException {

    try {
      // Replaced IBM WebSphere-specific WSSecurityHelper.revokeSSOCookies()
      // with standard Jakarta EE session invalidation for Java 17 compatibility
      if (request.getSession(false) != null) {
        request.getSession(false).invalidate();
      }
    } catch (Exception e) {
      logger.severe("[ERROR] Error logging out: " + e.getMessage());
      e.printStackTrace();
    }

    response.sendRedirect("login.jsp");
  }
}
