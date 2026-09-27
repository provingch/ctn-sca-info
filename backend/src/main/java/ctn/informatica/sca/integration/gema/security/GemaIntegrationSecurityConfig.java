package ctn.informatica.sca.integration.gema.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Cadena de seguridad propia para {@code /api/integracion/gema/**}, separada por completo de
 * {@code SecurityConfig} (JWT de usuario): stateless, sin CSRF, autenticada por
 * {@link GemaApiKeyFilter} con rol fijo {@code ROLE_INTEGRATION_GEMA}. {@code @Order(1)} la pone
 * antes que la cadena de {@code SecurityConfig} (sin {@code @Order}, así que queda última);
 * {@code securityMatcher} evita que compita por rutas fuera de la integración.
 */
@Configuration
public class GemaIntegrationSecurityConfig {

    private final GemaApiKeyFilter gemaApiKeyFilter;

    public GemaIntegrationSecurityConfig(GemaApiKeyFilter gemaApiKeyFilter) {
        this.gemaApiKeyFilter = gemaApiKeyFilter;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain gemaIntegrationFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/integracion/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("INTEGRATION_GEMA"))
            .addFilterBefore(gemaApiKeyFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
