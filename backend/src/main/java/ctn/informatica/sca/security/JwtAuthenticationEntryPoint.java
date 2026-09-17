package ctn.informatica.sca.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Reemplaza el 403 por default de Spring Security (Http403ForbiddenEntryPoint,
 * usado cuando no hay AuthenticationEntryPoint configurado) por un 401 real
 * para requests sin autenticación válida. El 403 queda reservado al caso de
 * un usuario autenticado sin el rol necesario (AccessDeniedHandler, sin tocar).
 *
 * El body se arma a mano (sin ObjectMapper): jackson-databind solo está en
 * el classpath en scope runtime (vía jjwt-jackson), no en compile, y esto
 * no amerita sumar una dependencia nueva para un JSON de tres campos fijos.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        writeUnauthorized(response, authException.getMessage() != null ? authException.getMessage() : "No autenticado.");
    }

    /** Usado también por JwtAuthenticationFilter para los casos que detecta antes de que la cadena llegue a la autorización. */
    public void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String body = "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"" + escape(message) + "\"}";
        response.getWriter().write(body);
    }

    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ");
    }
}
