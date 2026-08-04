package ctn.informatica.sca.servlets;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "LegalPageServlet", urlPatterns = {"/privacidad", "/terminos"})
public class LegalPageServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String view;
        switch (request.getServletPath()) {
            case "/privacidad":
                view = "/WEB-INF/legal/PrivacyPolicy.jsp";
                break;
            case "/terminos":
                view = "/WEB-INF/legal/TermsOfService.jsp";
                break;
            default:
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
        }
        request.getRequestDispatcher(view).forward(request, response);
    }
}
