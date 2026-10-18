package ctn.informatica.sca.integration.anotaciones;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Cadena de seguridad propia para {@code /api/anotaciones/**}, independiente de la de GEMA
 * ({@code /api/integracion/**}) y de la de JWT de usuarios. La app manda una clave fija en
 * {@code X-API-Key} ({@code anotaciones.api-key}); sin clave configurada la API responde 503.
 * El profesor se identifica después solo con su C.I. ({@code /login}), como se acordó con la app.
 */
@Configuration
public class AnotacionesSecurityConfig {

    /** Tope del cuerpo antes de que Spring lo parsee: foto de 5 MB + campos del formulario. */
    private static final long MAX_REQUEST_BYTES = AnotacionesController.MAX_FOTO_BYTES + 512 * 1024;

    @Bean
    @Order(2)
    public SecurityFilterChain anotacionesFilterChain(HttpSecurity http,
            @Value("${anotaciones.api-key:}") String apiKey) throws Exception {
        http
            .securityMatcher("/api/anotaciones/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("APP_ANOTACIONES"))
            // Instancia propia, no @Component: así no se registra también como filtro global.
            .addFilterBefore(new ApiKeyFilter(apiKey), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    static class ApiKeyFilter extends OncePerRequestFilter {

        private final byte[] expected;

        ApiKeyFilter(String apiKey) {
            this.expected = apiKey == null || apiKey.isBlank() ? null : sha256(apiKey);
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            if (expected == null) {
                write(response, 503, "La API de anotaciones no está habilitada");
                return;
            }
            String provided = request.getHeader("X-API-Key");
            // Comparación en tiempo constante sobre hashes (mismo criterio que GemaApiKeyFilter).
            if (provided == null || !MessageDigest.isEqual(sha256(provided), expected)) {
                write(response, 401, "Clave de la app inválida o ausente");
                return;
            }
            if (request.getContentLengthLong() > MAX_REQUEST_BYTES) {
                write(response, 413, "El envío supera el tamaño máximo permitido por el servidor");
                return;
            }
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "app-anotaciones", null, List.of(new SimpleGrantedAuthority("ROLE_APP_ANOTACIONES"))));
            chain.doFilter(request, response);
        }

        private static void write(HttpServletResponse response, int status, String mensaje) throws IOException {
            response.setStatus(status);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"ok\":false,\"error\":\"" + mensaje + "\"}");
        }

        private static byte[] sha256(String value) {
            try {
                return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            } catch (java.security.NoSuchAlgorithmException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
}
