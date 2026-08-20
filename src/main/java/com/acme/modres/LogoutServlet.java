package com.acme.modres;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet({ "/logout" })
public class LogoutServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = Logger.getLogger(LogoutServlet.class.getName());

  @Override
  protected void doGet(HttpServletRequest request,
      HttpServletResponse response) throws IOException {

    try {
      // Replace WSSecurityHelper.revokeSSOCookies with portable Jakarta EE session invalidation
      HttpSession session = request.getSession(false);
      if (session != null) {
        session.invalidate();
        logger.log(Level.INFO, "Session invalidated successfully");
      } else {
        logger.log(Level.INFO, "No session to invalidate");
      }
    } catch (IllegalStateException e) {
      // Session already invalidated
      logger.log(Level.WARNING, "Session already invalidated", e);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error logging out", e);
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;
    }

    response.sendRedirect("login.jsp");
  }
}
