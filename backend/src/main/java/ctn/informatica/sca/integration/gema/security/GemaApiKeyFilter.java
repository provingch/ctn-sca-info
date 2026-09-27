package ctn.informatica.sca.integration.gema.security;

import ctn.informatica.sca.security.JwtAuthenticationEntryPoint;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autenticación por clave de sistema para {@code /api/integracion/gema/**} (ver
 * {@link GemaIntegrationSecurityConfig}). No identifica a una persona sino a GEMA como sistema:
 * clave fija en el header {@code X-API-Key}, comparada en tiempo constante.
 *
 * <p>Orden de rechazo: integración deshabilitada (503) antes que nada — ni vale la pena mirar el
 * origen o la clave; después el origen ({@code allowed-ips}, 403) — un origen no permitido no
 * debería ni gastar una comparación de clave; recién al final la clave en sí (401).
 */
@Component
public class GemaApiKeyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(GemaApiKeyFilter.class);
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String GEMA_USER_HEADER = "X-GEMA-User";

    private final GemaProperties properties;
    private final JwtAuthenticationEntryPoint entryPoint;

    public GemaApiKeyFilter(GemaProperties properties, JwtAuthenticationEntryPoint entryPoint) {
        this.properties = properties;
        this.entryPoint = entryPoint;
    }

    /**
     * Este filtro es un {@code @Component}: además de correr dentro de la cadena de
     * {@link GemaIntegrationSecurityConfig} (vía {@code addFilterBefore}, ya acotada por
     * {@code securityMatcher}), Spring Boot lo registra también como filtro de servlet global
     * para {@code /*} — el mismo caso que {@code JwtAuthenticationFilter}. A diferencia de ese
     * filtro (que no hace nada sin header Bearer), este SÍ actúa sin ningún header (503 si la
     * integración está deshabilitada), así que sin este guard rechazaría cualquier request de
     * toda la aplicación, no solo las de GEMA.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/integracion/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            if (authorize(request, response)) {
                chain.doFilter(request, response);
            }
        } finally {
            log.info("integracion=gema client={} gemaUser={} method={} path={} status={} durationMs={}",
                    properties.getClientId(), request.getHeader(GEMA_USER_HEADER), request.getMethod(),
                    request.getRequestURI(), response.getStatus(), System.currentTimeMillis() - start);
        }
    }

    /** @return true si la request queda autenticada y debe seguir la cadena; si no, ya escribió la respuesta de error. */
    private boolean authorize(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!properties.isEnabled()) {
            writeError(response, 503, "Service Unavailable", "La integración con GEMA no está habilitada.");
            return false;
        }
        List<String> allowedIps = properties.allowedIpList();
        if (!allowedIps.isEmpty() && !allowedIps.contains(request.getRemoteAddr())) {
            writeError(response, 403, "Forbidden", "Origen no autorizado para la integración con GEMA.");
            return false;
        }
        String provided = request.getHeader(API_KEY_HEADER);
        if (provided == null || provided.isBlank() || !matches(provided, properties.getApiKey())) {
            entryPoint.writeUnauthorized(response, "Clave de integración inválida o ausente.");
            return false;
        }

        var authorities = List.of(new SimpleGrantedAuthority("ROLE_INTEGRATION_GEMA"));
        var authToken = new UsernamePasswordAuthenticationToken(properties.getClientId(), null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authToken);
        return true;
    }

    /** Comparación en tiempo constante: nunca == / equals directo sobre la clave recibida. */
    private static boolean matches(String provided, String expected) {
        if (expected == null || expected.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(sha256(provided), sha256(expected));
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    private static void writeError(HttpServletResponse response, int status, String error, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":" + status + ",\"error\":\"" + error + "\",\"message\":\"" + escape(message) + "\"}");
    }

    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ");
    }
}
