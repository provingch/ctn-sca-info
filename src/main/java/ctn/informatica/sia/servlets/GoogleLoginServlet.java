package ctn.informatica.sia.servlets;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.UUID;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import ctn.informatica.sia.config.AppConfig;
import ctn.informatica.sia.model.User;

@WebServlet("/GoogleLoginServlet")
public class GoogleLoginServlet extends HttpServlet {

    private static final String SCOPES = String.join(" ",
        "https://www.googleapis.com/auth/classroom.courses.readonly",
        "https://www.googleapis.com/auth/classroom.coursework.students",
        "https://www.googleapis.com/auth/classroom.rosters.readonly",
        "email", "profile"
    );

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");

        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/index.jsp?error=login_required");
            return;
        }

        String clientId = resolveClientId();
        String redirectUri = resolveRedirectUri(req);

        if (clientId == null || clientId.isBlank()) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Google client ID no configurado");
            return;
        }

        String state = UUID.randomUUID().toString();
        session = req.getSession(true);
        session.setAttribute("googleOAuthState", state);

        String authUrl = "https://accounts.google.com/o/oauth2/v2/auth"
            + "?client_id=" + URLEncoder.encode(clientId, "UTF-8")
            + "&redirect_uri=" + URLEncoder.encode(redirectUri, "UTF-8")
            + "&response_type=code"
            + "&scope=" + URLEncoder.encode(SCOPES, "UTF-8")
            + "&access_type=offline"
            + "&prompt=consent"
            + "&state=" + URLEncoder.encode(state, "UTF-8");

        resp.sendRedirect(authUrl);
    }

    private String resolveClientId() {
        try {
            return AppConfig.get("google.client.id");
        } catch (IllegalStateException | IllegalArgumentException ex) {
            return System.getenv("GOOGLE_CLIENT_ID");
        }
    }

    private String resolveRedirectUri(HttpServletRequest req) {
        try {
            String configuredUri = AppConfig.get("google.redirect.uri");
            if (configuredUri != null && !configuredUri.isBlank()) {
                return configuredUri;
            }
        } catch (IllegalStateException | IllegalArgumentException ex) {
            // fallback a la URI dinámica basada en la request
        }

        String contextPath = req.getContextPath();
        return req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() + contextPath + "/GoogleCallbackServlet";
    }
}