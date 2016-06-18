package com.welovecoding.web.login.google;

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet(urlPatterns = {URL.GOOGLE_PLUS_LOGIN_CALLBACK})
/**
 * @see https://code.google.com/p/google-api-java-client/wiki/OAuth2
 */
public class GooglePlusLoginCallbackServlet extends HttpServlet {

  private static final Logger LOG = Logger.getLogger(GooglePlusLoginCallbackServlet.class.getName());

  static {
    LOG.setLevel(Level.INFO);
  }

  /**
   * @param request servlet request
   * @param response servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   * @see https://developers.google.com/google-apps/tasks/oauth-authorization-callback-handler?hl=de
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException {
    if (request.getParameter("error") != null) {
      ServletContext context = getServletContext();
      RequestDispatcher rd = context.getRequestDispatcher(URL.GOOGLE_PLUS_LOGIN_ERROR);
      rd.forward(request, response);
      return;
    }

    // Google returns a "code" that can be exchanged for an access token
    String code = request.getParameter("code");
    if (code == null) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The 'code' URL parameter is missing");
    } else {
      // Get the access token by post to Google
      HttpSession session = request.getSession();
      OAuth20Service service = (OAuth20Service) session.getAttribute("scribejava");

      OAuth2AccessToken accessToken = service.getAccessToken(code);

      // Send response
      LOG.log(Level.INFO, "Access Token: {0}", accessToken.getRawResponse());
      response.setContentType("text/html;charset=UTF-8");
      try (PrintWriter out = response.getWriter()) {
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<title>Servlet Bla</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<p>Access token: </p><pre>" + accessToken.getRawResponse() + "</pre>");
        out.println("</body>");
        out.println("</html>");
      }
    }
  }

}
