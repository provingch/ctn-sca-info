/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package ctn.informatica.sia.servlets;

import ctn.informatica.sia.util.RememberMeTokenStore;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 *
 * @author jonat
 */
@WebServlet(name = "LogoutServlet", urlPatterns = {"/LogoutServlet"})
public class LogoutServlet extends HttpServlet {


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
            //get current session
            HttpSession session = request.getSession(false);
            if (session != null) session.invalidate();

            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie existingCookie : cookies) {
                    if ("SIA_REMEMBER".equals(existingCookie.getName()) && existingCookie.getValue() != null && !existingCookie.getValue().isBlank()) {
                        RememberMeTokenStore.invalidateToken(existingCookie.getValue().trim());
                        break;
                    }
                }
            }
            Cookie cookie = new Cookie("SIA_REMEMBER", "");
            cookie.setMaxAge(0);
            cookie.setPath(request.getContextPath().isBlank() ? "/" : request.getContextPath());
            cookie.setHttpOnly(true);
            response.addCookie(cookie);
            
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            
    }

}
