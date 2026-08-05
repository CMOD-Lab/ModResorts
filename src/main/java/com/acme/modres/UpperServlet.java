package com.acme.modres;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// Replaced IBM WebSphere-specific com.ibm.websphere.servlet.response.ResponseUtils
// with portable Jakarta EE / standard Java HTML encoding utility

@WebServlet("/resorts/upper")
public class UpperServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;

  /**
   * Portable HTML encoding replacement for ResponseUtils.encodeDataString().
   * Encodes special HTML characters to prevent XSS.
   */
  private static String encodeHtml(String input) {
    if (input == null) {
      return "";
    }
    return input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#x27;");
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    response.setContentType("text/html");

    String originalStr = request.getParameter("input");
    if (originalStr == null) {
      originalStr = "";
    }

    String newStr = originalStr.toUpperCase();
    // Replaced com.ibm.websphere.servlet.response.ResponseUtils.encodeDataString()
    // with portable HTML encoding helper method
    newStr = encodeHtml(newStr);

    PrintWriter out = response.getWriter();
    out.print("<br/><b>upper case input " + newStr + "</b>");
  }
}
