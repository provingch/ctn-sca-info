package ctn.informatica.sca.service;

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
import ctn.informatica.sca.dao.ProfesorDao;
import ctn.informatica.sca.dto.GoogleOAuthCallbackRequest;
import ctn.informatica.sca.dto.GoogleOAuthCallbackResponse;
import ctn.informatica.sca.model.Profesor;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GoogleOAuthService {

    private final ProfesorDao profesorDao;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public GoogleOAuthService(
            ProfesorDao profesorDao,
            @Value("${google.client.id:${GOOGLE_CLIENT_ID:}}") String clientId,
            @Value("${google.client.secret:${GOOGLE_CLIENT_SECRET:}}") String clientSecret,
                @Value("${google.redirect-uri:${GOOGLE_REDIRECT_URI:http://localhost:5173/google/callback}}") String redirectUri) {
        this.profesorDao = profesorDao;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

            public String buildAuthorizeUrl() {
            // Scopes necesarios para Classroom + profile/email
            String scopes = String.join(" ", new String[]{
                "https://www.googleapis.com/auth/classroom.courses.readonly",
                "https://www.googleapis.com/auth/classroom.rosters.readonly",
                // coursework scopes required to list/import tareas
                "https://www.googleapis.com/auth/classroom.coursework.me",
                "https://www.googleapis.com/auth/classroom.coursework.students.readonly",
                "openid",
                "email",
                "profile"
            });

            String encodedScopes = java.net.URLEncoder.encode(scopes, java.nio.charset.StandardCharsets.UTF_8);
            String encodedRedirect = java.net.URLEncoder.encode(redirectUri, java.nio.charset.StandardCharsets.UTF_8);

            return String.format(
                "https://accounts.google.com/o/oauth2/v2/auth?client_id=%s&redirect_uri=%s&response_type=code&scope=%s&access_type=offline&prompt=consent&include_granted_scopes=true",
                clientId,
                encodedRedirect,
                encodedScopes
            );
            }

    public GoogleOAuthCallbackResponse handleCallback(int userId, GoogleOAuthCallbackRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request inválido");
        }

        if (request.error() != null && !request.error().isBlank()) {
            return new GoogleOAuthCallbackResponse("error", "OAuth denegado por el usuario", null, null);
        }

        if (request.code() == null || request.code().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta parámetro code");
        }

        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Google OAuth no está configurado");
        }

        Profesor profesor;
        try {
            profesor = profesorDao.findById(userId);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo validar el usuario", ex);
        }

        if (profesor == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo perfiles de profesor pueden vincular Google Classroom");
        }

        try {
            GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    "https://oauth2.googleapis.com/token",
                    clientId,
                    clientSecret,
                    request.code(),
                    redirectUri)
                    .execute();

            String accessToken = tokenResponse.getAccessToken();
            String refreshToken = tokenResponse.getRefreshToken();
            long expiresInSeconds = tokenResponse.getExpiresInSeconds() != null ? tokenResponse.getExpiresInSeconds() : 0L;
            long expiryEpochSeconds = (System.currentTimeMillis() / 1000) + expiresInSeconds;

            String googleEmail = fetchGoogleEmail(accessToken);
            boolean updated = profesorDao.updateGoogleTokens(
                    profesor.getId(),
                    accessToken,
                    refreshToken,
                    expiryEpochSeconds,
                    googleEmail);

            if (!updated) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar la vinculación de Google");
            }

            return new GoogleOAuthCallbackResponse(
                    "ok",
                    "Cuenta de Google vinculada correctamente",
                    googleEmail,
                    expiryEpochSeconds);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falló el intercambio de token con Google", ex);
        }
    }

    private String fetchGoogleEmail(String accessToken) throws IOException {
        AccessToken tokenWrapper = new AccessToken(accessToken, null);
        GoogleCredentials credentials = GoogleCredentials.create(tokenWrapper);
        HttpRequestInitializer credential = new HttpCredentialsAdapter(credentials);

        Oauth2 oauth2 = new Oauth2.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential)
                .setApplicationName("SCA")
                .build();

        Userinfo userInfo = oauth2.userinfo().get().execute();
        return userInfo.getEmail();
    }
}
