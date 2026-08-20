package com.acme.modres;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Replaced com.ibm.websphere.servlet.response.ResponseUtils with portable Jakarta EE HTML encoding
import org.apache.commons.text.StringEscapeUtils;

@WebServlet("/resorts/upper")
public class UpperServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    response.setContentType("text/html");

    String originalStr = request.getParameter("input");

    // Input validation
    if (originalStr == null || originalStr.trim().isEmpty()) {
      PrintWriter out = response.getWriter();
      out.print("<br/><b>No input provided</b>");
      return;
    }

    // Limit input length to prevent abuse
    if (originalStr.length() > 1000) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      PrintWriter out = response.getWriter();
      out.print("<br/><b>Input too long (max 1000 characters)</b>");
      return;
    }

    String newStr = originalStr.toUpperCase();
    // Replace ResponseUtils.encodeDataString with portable HTML encoding
    newStr = StringEscapeUtils.escapeHtml4(newStr);

    PrintWriter out = response.getWriter();
    out.print("<br/><b>upper case input " + newStr + "</b>");
  }
}
