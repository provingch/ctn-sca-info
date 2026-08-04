package ctn.informatica.sca.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
            .csrf(csrf -> csrf.disable()) // no hay cookies de sesión, no aplica CSRF clásico
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/api/auth/2fa/verify", "/api/auth/refresh", "/api/auth/logout", "/api/health").permitAll()
                .requestMatchers("/api/planillas/**", "/api/tareas/**").hasRole("LEVEL_1")
                .requestMatchers("/api/evaluacion/**").hasRole("LEVEL_2")
                .requestMatchers("/api/google/oauth/callback").hasAnyRole("LEVEL_1", "LEVEL_2", "LEVEL_3")
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