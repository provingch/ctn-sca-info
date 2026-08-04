package ctn.informatica.sca.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response); // sin token: sigue, SecurityConfig decide si la ruta lo requiere
            return;
        }

        String token = header.substring(7);

        if (!jwtService.isValid(token) || !jwtService.isAccessToken(token)) {
            chain.doFilter(request, response); // token inválido o es un temp token: sigue sin autenticar
            return;                             // SecurityConfig lo va a rechazar si la ruta requiere auth
        }

        Long userId = jwtService.extractUserId(token);
        Integer level = jwtService.extractLevel(token);

        var authorities = List.of(new SimpleGrantedAuthority("ROLE_LEVEL_" + level));
        var authToken = new UsernamePasswordAuthenticationToken(userId, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authToken);

        chain.doFilter(request, response);
    }
}