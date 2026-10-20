package ctn.informatica.sca.integration.enfermeria;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.SecretKey;
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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Cadena de seguridad propia para {@code /api/enfermeria/**}, aparte de la de anotaciones y de la
 * de usuarios del colegio. Dos capas, porque son datos de salud de menores:
 * <ol>
 *   <li>{@code X-API-Key} fija de la app ({@code enfermeria.api-key}, distinta de la de anotaciones);
 *       sin clave configurada responde 503.</li>
 *   <li>Salvo {@code /login}, un {@code Authorization: Bearer} emitido por {@code /login}: con solo
 *       la clave de la app no se leen fichas.</li>
 * </ol>
 */
@Configuration
public class EnfermeriaSecurityConfig {

    private static final long MAX_REQUEST_BYTES = 256 * 1024;

    @Bean
    @Order(3)
    public SecurityFilterChain enfermeriaFilterChain(HttpSecurity http, EnfermeriaDao dao, Tokens tokens,
            @Value("${enfermeria.api-key:}") String apiKey) throws Exception {
        http
            .securityMatcher("/api/enfermeria/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/enfermeria/login").hasRole("APP_ENFERMERIA")
                .anyRequest().hasAnyRole("ENF_ADMINISTRADOR", "ENF_ENFERMERIA"))
            // Instancia propia, no @Component: así no se registra también como filtro global.
            .addFilterBefore(new Filtro(apiKey, dao, tokens), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Token de sesión de enfermería: JWT firmado con {@code jwt.secret} pero con {@code type=enf},
     * así que el filtro del colegio lo rechaza (solo acepta {@code access}) y este rechaza los del colegio.
     * Lleva una huella del hash de la contraseña: al cambiarla, los tokens viejos dejan de valer.
     */
    @Component
    public static class Tokens {

        private final SecretKey key;
        private final long horas;

        public Tokens(@Value("${jwt.secret}") String secret, @Value("${enfermeria.token-horas:12}") long horas) {
            this.key = Keys.hmacShaKeyFor(secret.getBytes());
            this.horas = horas;
        }

        public String emitir(EnfermeriaDao.Usuario u) {
            Date now = new Date();
            return Jwts.builder()
                    .subject(String.valueOf(u.id()))
                    .claim("type", "enf")
                    .claim("pw", huella(u.hash()))
                    .issuedAt(now)
                    .expiration(new Date(now.getTime() + horas * 3_600_000))
                    .signWith(key)
                    .compact();
        }

        /** Usuario vigente del token, o null si el token es inválido, vencido, de otro tipo o quedó viejo. */
        EnfermeriaDao.Usuario validar(String token, EnfermeriaDao dao) throws Exception {
            Claims c;
            try {
                c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            } catch (Exception ex) {
                return null;
            }
            if (!"enf".equals(c.get("type", String.class))) return null;
            EnfermeriaDao.Usuario u = dao.usuarioPorId(Integer.parseInt(c.getSubject()));
            return u != null && huella(u.hash()).equals(c.get("pw", String.class)) ? u : null;
        }

        private static String huella(String hash) {
            return HexFormat.of().formatHex(sha256(hash)).substring(0, 16);
        }
    }

    static class Filtro extends OncePerRequestFilter {

        private final byte[] expected;
        private final EnfermeriaDao dao;
        private final Tokens tokens;

        Filtro(String apiKey, EnfermeriaDao dao, Tokens tokens) {
            this.expected = apiKey == null || apiKey.isBlank() ? null : sha256(apiKey);
            this.dao = dao;
            this.tokens = tokens;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            if (expected == null) {
                write(response, 503, "La API de enfermería no está habilitada");
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
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                EnfermeriaDao.Usuario u;
                try {
                    u = tokens.validar(header.substring(7), dao);
                } catch (Exception ex) {
                    logger.warn("No se pudo validar el token de enfermería", ex);
                    write(response, 500, "Error interno del servidor");
                    return;
                }
                if (u == null) {
                    write(response, 401, "Sesión vencida o inválida. Volvé a iniciar sesión.");
                    return;
                }
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                        u, null, List.of(new SimpleGrantedAuthority("ROLE_APP_ENFERMERIA"),
                                new SimpleGrantedAuthority("ROLE_ENF_" + u.rol().toUpperCase()))));
            } else if (request.getRequestURI().equals(request.getContextPath() + "/api/enfermeria/login")) {
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                        "app-enfermeria", null, List.of(new SimpleGrantedAuthority("ROLE_APP_ENFERMERIA"))));
            } else {
                write(response, 401, "Falta iniciar sesión");
                return;
            }
            chain.doFilter(request, response);
        }

        private static void write(HttpServletResponse response, int status, String mensaje) throws IOException {
            response.setStatus(status);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"ok\":false,\"error\":\"" + mensaje + "\"}");
        }
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
