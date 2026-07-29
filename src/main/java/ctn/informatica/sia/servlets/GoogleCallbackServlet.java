package ctn.informatica.sia.servlets;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfo;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;

import ctn.informatica.sia.config.AppConfig;
import ctn.informatica.sia.dao.ProfesorDao;
import ctn.informatica.sia.model.Profesor;
import ctn.informatica.sia.model.User;

@WebServlet("/GoogleCallbackServlet")
public class GoogleCallbackServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {

        HttpSession session = req.getSession(false);
        String returnedState = req.getParameter("state");
        String expectedState = session == null ? null : (String) session.getAttribute("googleOAuthState");

        if (expectedState == null || !expectedState.equals(returnedState)) {
            resp.sendRedirect(req.getContextPath() + "/index.jsp?error=oauth_state_invalid");
            return;
        }
        session.removeAttribute("googleOAuthState");

        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/index.jsp?error=login_required");
            return;
        }

        String code = req.getParameter("code");
        String error = req.getParameter("error");

        if (error != null) {
            resp.sendRedirect(req.getContextPath() + "/index.jsp?error=oauth_denied");
            return;
        }

        if (code == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Falta parámetro 'code'");
            return;
        }

        String clientId = AppConfig.get("google.client.id");
        String clientSecret = AppConfig.get("google.client.secret");
        String redirectUri = AppConfig.get("google.redirect.uri");

        try {
            GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    "https://oauth2.googleapis.com/token",
                    clientId,
                    clientSecret,
                    code,
                    redirectUri)
                .execute();

            String accessToken = tokenResponse.getAccessToken();
            String refreshToken = tokenResponse.getRefreshToken();
            long expiresInSeconds = tokenResponse.getExpiresInSeconds();

            AccessToken tokenWrapper = new AccessToken(accessToken, null);
            GoogleCredentials credentials = GoogleCredentials.create(tokenWrapper);
            HttpRequestInitializer credential = new HttpCredentialsAdapter(credentials);
            
            Oauth2 oauth2 = new Oauth2.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
                .setApplicationName("CTN-SIA")
                .build();

            Userinfo userInfo = oauth2.userinfo().get().execute();
            String googleEmail = userInfo.getEmail();

            ProfesorDao profesorDao = new ProfesorDao();
            Profesor profesor = profesorDao.findById(user.getId());

            if (profesor == null) {
                resp.sendRedirect(req.getContextPath() + "/index.jsp?error=profesor_no_encontrado");
                return;
            }

            long expiry = (System.currentTimeMillis() / 1000) + expiresInSeconds;

            profesorDao.updateGoogleTokens(
                profesor.getId(),
                accessToken,
                refreshToken,
                expiry,
                googleEmail
            );

            session.setAttribute("googleAccessToken", accessToken);
            session.setAttribute("profesor", profesor);
            session.setAttribute("flashMessage", "Cuenta de Google vinculada: " + googleEmail);

            resp.sendRedirect(req.getContextPath() + "/ProfileServlet");

        } catch (IOException e) {
            e.printStackTrace();
            resp.sendRedirect(req.getContextPath() + "/index.jsp?error=token_exchange_failed");
        }
    }
}