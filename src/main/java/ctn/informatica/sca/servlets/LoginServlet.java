/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package ctn.informatica.sca.servlets;

import ctn.informatica.sca.dao.EspecialidadDao;
import ctn.informatica.sca.dao.PadreDao;
import ctn.informatica.sca.dao.UserDao;
import ctn.informatica.sca.model.Especialidad;
import ctn.informatica.sca.model.Padre;
import ctn.informatica.sca.model.Profesor;
import ctn.informatica.sca.model.User;
import ctn.informatica.sca.util.RememberMeTokenStore;
import ctn.informatica.sca.util.ScaUiContext;
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
@WebServlet(name = "LoginServlet", urlPatterns = {"/login"})
public class LoginServlet extends HttpServlet {

    private static final String REMEMBER_COOKIE_NAME = "SCA_REMEMBER";
    private static final int REMEMBER_MAX_AGE_SECONDS = 60 * 60 * 24 * 30;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html");

        String username = request.getParameter("username");
        String password = request.getParameter("password");

        UserDao userDao = new UserDao();
        try {
            User user = userDao.findByUsernameAndPassword(username, password);
            if (user != null) {
                boolean rememberMe = "true".equalsIgnoreCase(request.getParameter("rememberMe"));
                String secret = null;
                Profesor profesor = null;
                Padre padre = null;
                try {
                    if (user.getLevel() == 4) {
                        padre = new PadreDao().findById(user.getId());
                        if (padre != null) {
                            secret = padre.getTotpSecret();
                        }
                    } else {
                        profesor = new ctn.informatica.sca.dao.ProfesorDao().findById(user.getId());
                        if (profesor != null) {
                            secret = profesor.getTotpSecret();
                        }
                    }
                } catch (Exception ignored) {
                    // ignore secret lookup failures; fall back to password-only login
                }

                if (secret != null && !secret.isBlank()) {
                    HttpSession session = request.getSession(true);
                    session.setMaxInactiveInterval(60 * 60 * 24 * 7);
                    session.setAttribute("pendingTotpLogin", new ctn.informatica.sca.model.PendingTotpLogin(user.getId(), user.getUsername(), user.getLevel(), rememberMe));
                    response.sendRedirect(request.getContextPath() + "/totp");
                    return;
                }

                HttpSession session = request.getSession(true);
                session.setMaxInactiveInterval(60 * 60 * 24 * 7);
                session.setAttribute("user", user);
                if (profesor != null) {
                    session.setAttribute("profesor", profesor);
                    String specialty = "informatica";
                    if (profesor.getEspecialidadId() != null) {
                        try {
                            Especialidad especialidad = new EspecialidadDao().findById(profesor.getEspecialidadId());
                            if (especialidad != null && especialidad.getNombre() != null && !especialidad.getNombre().isBlank()) {
                                specialty = ScaUiContext.normalizeSpecialty(especialidad.getNombre());
                            }
                        } catch (Exception ignoredEspecialidad) {
                            specialty = "informatica";
                        }
                    }
                    session.setAttribute("scaSpecialty", specialty);
                } else if (padre != null) {
                    session.setAttribute("padre", padre);
                }
                if (rememberMe) {
                    String token = RememberMeTokenStore.issueToken(user.getId());
                    setRememberMeCookie(request, response, token);
                } else {
                    RememberMeTokenStore.invalidateUserTokens(user.getId());
                    clearRememberMeCookie(request, response);
                }

                int level = user.getLevel();
                switch (level) {
                    case 1:
                        response.sendRedirect(request.getContextPath() + "/inicio");
                        break;
                    case 2:
                        response.sendRedirect(request.getContextPath() + "/evaluacion");
                        break;
                    case 3:
                        response.sendRedirect(request.getContextPath() + "/admin");
                        break;
                    case 4:
                        response.sendRedirect(request.getContextPath() + "/padre");
                        break;
                    default:
                        request.setAttribute("loginError", true);
                        request.getRequestDispatcher("/index.jsp").forward(request, response);
                }

            } else {
                // login failed: send back to login with error flag
                request.setAttribute("loginError", true);
                request.getRequestDispatcher("/index.jsp").forward(request, response);
            }
        } catch (Exception e) {
            throw new ServletException("DB error during login", e);
        }
    }

    private void setRememberMeCookie(HttpServletRequest request, HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(REMEMBER_COOKIE_NAME, token);
        cookie.setMaxAge(REMEMBER_MAX_AGE_SECONDS);
        cookie.setPath(request.getContextPath().isBlank() ? "/" : request.getContextPath());
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        response.addCookie(cookie);
    }

    private void clearRememberMeCookie(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = new Cookie(REMEMBER_COOKIE_NAME, "");
        cookie.setMaxAge(0);
        cookie.setPath(request.getContextPath().isBlank() ? "/" : request.getContextPath());
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        response.addCookie(cookie);
    }
}
