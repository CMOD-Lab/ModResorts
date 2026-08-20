package com.acme.modres;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.text.StringEscapeUtils;

@WebServlet("/resorts/welcome")
public class WelcomeServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    response.setContentType("text/plain");

    String userName = request.getParameter("user");
    PrintWriter out = response.getWriter();

    // XSS protection - sanitize user input
    if (userName != null && !userName.trim().isEmpty()) {
      String sanitizedUserName = StringEscapeUtils.escapeHtml4(userName);
      out.println("Welcome " + sanitizedUserName + "! Enjoy!");
    } else {
      out.println("Welcome! Enjoy!");
    }
  }
}
