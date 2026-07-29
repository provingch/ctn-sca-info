package ctn.informatica.sca.servlets;

import ctn.informatica.sca.dao.PadreDao;
import ctn.informatica.sca.dao.ProfesorDao;
import ctn.informatica.sca.dao.UserDao;
import ctn.informatica.sca.model.Padre;
import ctn.informatica.sca.model.PendingTotpLogin;
import ctn.informatica.sca.model.Profesor;
import ctn.informatica.sca.model.User;
import ctn.informatica.sca.util.RememberMeTokenStore;
import ctn.informatica.sca.util.TotpUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet(name = "TotpServlet", urlPatterns = {"/totp"})
public class TotpServlet extends HttpServlet {

    private static final String REMEMBER_COOKIE_NAME = "SCA_REMEMBER";
    private static final int REMEMBER_MAX_AGE_SECONDS = 60 * 60 * 24 * 30;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        PendingTotpLogin pending = session == null ? null : (PendingTotpLogin) session.getAttribute("pendingTotpLogin");
        if (pending == null) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }
        request.setAttribute("pendingUsername", pending.getUsername());
        request.getRequestDispatcher("/TotpChallenge.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        PendingTotpLogin pending = session == null ? null : (PendingTotpLogin) session.getAttribute("pendingTotpLogin");
        if (pending == null) {
            response.sendRedirect(request.getContextPath() + "/index.jsp");
            return;
        }

        String totpCode = request.getParameter("totpCode");
        boolean verified = false;
        try {
            String secret = null;
            if (pending.getLevel() == 4) {
                Padre padre = new PadreDao().findById(pending.getUserId());
                if (padre != null) {
                    secret = padre.getTotpSecret();
                }
            } else {
                Profesor profesor = new ProfesorDao().findById(pending.getUserId());
                if (profesor != null) {
                    secret = profesor.getTotpSecret();
                }
            }
            verified = TotpUtils.verifyCode(secret, totpCode);
        } catch (Exception ex) {
            log("Error verifying TOTP code", ex);
        }

        if (!verified) {
            request.setAttribute("verifyError", "El código de autenticación no es válido. Intenta de nuevo.");
            request.setAttribute("pendingUsername", pending.getUsername());
            request.getRequestDispatcher("/TotpChallenge.jsp").forward(request, response);
            return;
        }

        if (session != null) {
            session.removeAttribute("pendingTotpLogin");
        }

        try {
            User user = new UserDao().findById(pending.getUserId());
            if (user == null) {
                response.sendRedirect(request.getContextPath() + "/index.jsp");
                return;
            }
            if (session == null) {
                session = request.getSession(true);
            }
            session.setMaxInactiveInterval(60 * 60 * 24 * 7);
            session.setAttribute("user", user);
            if (user.getLevel() == 4) {
                Padre padre = new PadreDao().findById(user.getId());
                if (padre != null) {
                    session.setAttribute("padre", padre);
                }
            } else {
                try {
                    Profesor profesor = new ProfesorDao().findById(user.getId());
                    session.setAttribute("profesor", profesor);
                    String specialty = "informatica";
                    if (profesor != null && profesor.getEspecialidadId() != null) {
                        String specialtyCandidate = new ctn.informatica.sca.dao.EspecialidadDao().findById(profesor.getEspecialidadId()).getNombre();
                        if (specialtyCandidate != null && !specialtyCandidate.isBlank()) {
                            specialty = ctn.informatica.sca.util.ScaUiContext.normalizeSpecialty(specialtyCandidate);
                        }
                    }
                    session.setAttribute("scaSpecialty", specialty);
                } catch (Exception ignored) {
                    // no-op
                }
            }

            if (pending.isRememberMe()) {
                String token = RememberMeTokenStore.issueToken(user.getId());
                setRememberMeCookie(request, response, token);
            } else {
                RememberMeTokenStore.invalidateUserTokens(user.getId());
                clearRememberMeCookie(request, response);
            }

            response.sendRedirect(request.getContextPath() + getRedirectTarget(user.getLevel()));
        } catch (Exception ex) {
            throw new ServletException("DB error during TOTP verification", ex);
        }
    }

    private String getRedirectTarget(int level) {
        switch (level) {
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
