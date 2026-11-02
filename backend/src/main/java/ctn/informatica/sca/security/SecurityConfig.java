package ctn.informatica.sca.security;

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
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain coreSnapshotFilterChain(HttpSecurity http,
            @Value("${sca.core-sync.api-key:}") String apiKey) throws Exception {
        http
            .securityMatcher("/api/internal/core/snapshot")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("CORE_SYNC"))
            .addFilterBefore(new CoreSnapshotKeyFilter(apiKey), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // La API usa JWT Bearer; las cookies httpOnly solo rotan o revocan
            // credenciales de renovación y además se emiten con SameSite=Lax.
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/error", "/api/auth/login", "/api/auth/2fa/verify", "/api/auth/refresh", "/api/auth/logout", "/api/health").permitAll()
                // Shell estático de la SPA (React): tiene que poder cargar sin estar
                // autenticado, porque ahí vive la propia pantalla de login. La
                // protección real sigue pasando por cada endpoint /api/**, no por
                // estos archivos. "/login","/home","/profile" son rutas client-side
                // de React Router: el navegador las pide como GET normal en un F5 o
                // acceso directo, y SpaForwardController las reenvía a index.html.
                // Agregar acá cada ruta nueva de React a medida que se sume un Bloque.
                .requestMatchers(HttpMethod.GET, "/", "/index.html", "/assets/**", "/favicon.svg", "/icons.svg",
                    "/icons/pwa/**", "/manifest.webmanifest", "/sw.js", "/offline.html").permitAll()
                .requestMatchers(HttpMethod.GET, "/pdfs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/app/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/plantillas/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/login", "/home", "/inicio", "/profile", "/perfil",
                    "/evaluacion", "/admin", "/admin/**", "/padre", "/coordinacion", "/styleguide", "/privacidad", "/terminos", "/acerca-de",
                    "/admin/salas", "/admin/horarios/**",
                    "/planilla/**", "/google/callback", "/offline").permitAll()
                .requestMatchers("/api/planillas/**", "/api/tareas/**").hasRole("LEVEL_1")
                .requestMatchers("/api/evaluacion/especialidades").hasAnyRole("LEVEL_1", "LEVEL_2")
                .requestMatchers("/api/evaluacion/**").hasRole("LEVEL_2")
                .requestMatchers("/api/admin/**").hasAnyRole("LEVEL_3", "LEVEL_5")
                .requestMatchers("/api/padre/**").hasRole("LEVEL_4")
                .requestMatchers("/api/google/oauth/callback").hasAnyRole("LEVEL_1", "LEVEL_2", "LEVEL_3", "LEVEL_5")
                .requestMatchers("/api/instrumentos").hasRole("LEVEL_1")
                .anyRequest().authenticated()
            )
            // Sin esto, Spring Security usa Http403ForbiddenEntryPoint por default para
            // cualquier request no autenticada, indistinguible del 403 real (autenticado
            // sin el rol necesario, que sigue resolviendo el AccessDeniedHandler default).
            .exceptionHandling(handling -> handling.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Nota: PasswordUtil.java (legacy) ya maneja BCrypt con el mismo BCryptPasswordEncoder.
        // Este bean queda disponible si en el futuro quieren usar el PasswordEncoder
        // estándar de Spring Security en vez de PasswordUtil. Por ahora, AuthController
        // sigue usando PasswordUtil tal cual, este bean no se usa todavía.
        return new BCryptPasswordEncoder();
    }

    static class CoreSnapshotKeyFilter extends OncePerRequestFilter {

        private final byte[] expected;

        CoreSnapshotKeyFilter(String apiKey) {
            expected = apiKey == null || apiKey.isBlank() ? null : sha256(apiKey);
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            if (expected == null) {
                write(response, 503, "La exportación del Core no está habilitada");
                return;
            }
            String provided = request.getHeader("X-Core-Sync-Key");
            if (provided == null || !MessageDigest.isEqual(sha256(provided), expected)) {
                write(response, 401, "Credencial de sincronización inválida o ausente");
                return;
            }
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "ctnapi-core-sync", null, List.of(new SimpleGrantedAuthority("ROLE_CORE_SYNC"))));
            chain.doFilter(request, response);
        }

        private static void write(HttpServletResponse response, int status, String message) throws IOException {
            response.setStatus(status);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"ok\":false,\"error\":\"" + message + "\"}");
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
