/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Filter.java to edit this template
 */
package ctn.informatica.sca.filter;

import java.io.IOException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import ctn.informatica.sca.dao.EspecialidadDao;
import ctn.informatica.sca.dao.UserDao;
import ctn.informatica.sca.model.Especialidad;
import ctn.informatica.sca.model.Padre;
import ctn.informatica.sca.model.Profesor;
import ctn.informatica.sca.model.User;
import java.util.List;
import java.util.Map;
import ctn.informatica.sca.util.RememberMeTokenStore;
import ctn.informatica.sca.util.ScaUiContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 *
 * @author jonat
 */
@WebFilter(filterName = "AuthFilter", urlPatterns = {"/inicio", "/planilla", "/tarea", "/legacy/tarea", "/perfil", "/evaluacion", "/admin", "/admin/materias", "/admin/usuarios", "/admin/asignaciones", "/admin/ingresantes", "/padre", "/push-subscription", "/index.jsp"})
public class AuthFilter implements Filter {

        private static final Map<String, List<Integer>> AUTHORIZED_LEVELS = Map.ofEntries(
                Map.entry("/inicio", List.of(1)),
                Map.entry("/planilla", List.of(1)),
                Map.entry("/tarea", List.of(1)),
                Map.entry("/legacy/tarea", List.of(1)),
                Map.entry("/perfil", List.of(1, 2, 3, 4)),
                Map.entry("/push-subscription", List.of(1, 2, 3, 4)),
                Map.entry("/evaluacion", List.of(2)),
                Map.entry("/admin", List.of(3)),
                Map.entry("/admin/materias", List.of(3)),
                Map.entry("/admin/usuarios", List.of(3)),
                Map.entry("/admin/asignaciones", List.of(3)),
                Map.entry("/admin/ingresantes", List.of(3)),
                Map.entry("/padre", List.of(4))
        );

    private boolean isAuthorized(String servletPath, Object user) {
        if (servletPath == null) {
            return false;
        }
        if (!(user instanceof User)) {
            return false;
        }
        User loggedUser = (User) user;
        List<Integer> allowedLevels = AUTHORIZED_LEVELS.get(servletPath);
        return allowedLevels != null && allowedLevels.contains(loggedUser.getLevel());
    }

    private String getRedirectPathForLevel(Object user) {
        if (!(user instanceof User)) {
            return "/index.jsp";
        }
        switch (((User) user).getLevel()) {
            case 1:
                return "/inicio";
            case 2:
                return "/evaluacion";
            case 3:
                return "/admin";
            case 4:
                return "/padre";
            default:
                return "/index.jsp";
        }
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String ctx = request.getContextPath();

        HttpSession session = request.getSession(false);
        Object user = (session == null) ? null : session.getAttribute("user");
        String currentPath = request.getServletPath();
        boolean isLoginPage = "/index.jsp".equals(currentPath);

        if (user == null) {
            User restoredUser = restoreUserFromRememberCookie(request, response);
            if (restoredUser != null) {
                session = request.getSession(true);
                session.setMaxInactiveInterval(60 * 60 * 24 * 7);
                session.setAttribute("user", restoredUser);
                try {
                    Profesor profesor = new ctn.informatica.sca.dao.ProfesorDao().findById(restoredUser.getId());
                    session.setAttribute("profesor", profesor);
                    String specialty = "general";
                    if (profesor != null && profesor.getEspecialidadId() != null) {
                        try {
                            Especialidad especialidad = new EspecialidadDao().findById(profesor.getEspecialidadId());
                            if (especialidad != null && especialidad.getNombre() != null && !especialidad.getNombre().isBlank()) {
                                specialty = ScaUiContext.normalizeSpecialty(especialidad.getNombre());
                            }
                        } catch (Exception ignoredEspecialidad) {
                            specialty = "general";
                        }
                    }
                    session.setAttribute("scaSpecialty", specialty);
                } catch (Exception ignored) {
                    // no-op
                }
                try {
                    Padre padre = new ctn.informatica.sca.dao.PadreDao().findById(restoredUser.getId());
                    if (padre != null) {
                        session.setAttribute("padre", padre);
                    }
                } catch (Exception ignored) {
                    // no-op
                }
                user = restoredUser;
            } else if (isLoginPage) {
                chain.doFilter(req, res);
                return;
            } else {
                response.sendRedirect(ctx + "/index.jsp");
                return; // stop processing
            }
        }

        if (isLoginPage) {
            response.sendRedirect(ctx + getRedirectPathForLevel(user));
            return;
        }

        // prevent caching of protected pages (helps with Back after logout)
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        if (!isAuthorized(currentPath, user)) {
            response.sendRedirect(ctx + getRedirectPathForLevel(user));
            return;
        }

        chain.doFilter(req, res);
    }

    private User restoreUserFromRememberCookie(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if ("SCA_REMEMBER".equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                String tokenValue = cookie.getValue().trim();
                try {
                    Integer userId = RememberMeTokenStore.resolveUserId(tokenValue).orElse(null);
                    if (userId != null) {
                        User user = new UserDao().findById(userId);
                        if (user != null) {
                            Cookie refreshedCookie = new Cookie("SCA_REMEMBER", tokenValue);
                            refreshedCookie.setMaxAge(60 * 60 * 24 * 30);
                            refreshedCookie.setPath(request.getContextPath().isBlank() ? "/" : request.getContextPath());
                            refreshedCookie.setHttpOnly(true);
                            refreshedCookie.setSecure(request.isSecure());
                            response.addCookie(refreshedCookie);
                            return user;
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("Unable to restore user session from remember-me cookie: " + ex.getMessage());
                }
                Cookie expiredCookie = new Cookie("SCA_REMEMBER", "");
                expiredCookie.setMaxAge(0);
                expiredCookie.setPath(request.getContextPath().isBlank() ? "/" : request.getContextPath());
                expiredCookie.setHttpOnly(true);
                expiredCookie.setSecure(request.isSecure());
                response.addCookie(expiredCookie);
                return null;
            }
        }
        return null;
    }

    @Override
    public void destroy() { /* no-op */ }

}
