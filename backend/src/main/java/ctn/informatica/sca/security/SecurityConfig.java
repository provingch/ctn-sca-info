package ctn.informatica.sca.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
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
                .requestMatchers(HttpMethod.GET, "/plantillas/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/login", "/home", "/inicio", "/profile", "/perfil",
                    "/evaluacion", "/admin", "/admin/**", "/padre", "/styleguide", "/privacidad", "/terminos",
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
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Nota: PasswordUtil.java (legacy) ya maneja BCrypt directo con jbcrypt.
        // Este bean queda disponible si en el futuro quieren usar el PasswordEncoder
        // estándar de Spring Security en vez de PasswordUtil. Por ahora, AuthController
        // sigue usando PasswordUtil tal cual, este bean no se usa todavía.
        return new BCryptPasswordEncoder();
    }
}
