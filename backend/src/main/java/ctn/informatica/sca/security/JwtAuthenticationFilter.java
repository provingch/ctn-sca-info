package ctn.informatica.sca.security;

import ctn.informatica.sca.dao.UserDao;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserDao userDao;
    private final JwtAuthenticationEntryPoint entryPoint;

    public JwtAuthenticationFilter(JwtService jwtService, UserDao userDao, JwtAuthenticationEntryPoint entryPoint) {
        this.jwtService = jwtService;
        this.userDao = userDao;
        this.entryPoint = entryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response); // sin token: sigue anónimo, SecurityConfig decide si la ruta lo requiere
            return;
        }

        String token = header.substring(7);

        if (!jwtService.isValid(token) || !jwtService.isAccessToken(token)) {
            // token presente pero inválido, vencido, o es un temp token usado donde no corresponde: 401 directo,
            // no lo dejamos pasar como anónimo (eso es lo que hacía que el cliente nunca disparara el refresh).
            entryPoint.writeUnauthorized(response, "Token inválido o vencido.");
            return;
        }

        Long userId = jwtService.extractUserId(token);
        Integer level = jwtService.extractLevel(token);
        Integer sessionVersion = jwtService.extractSessionVersion(token);

        try {
            UserDao.SessionState state = userDao.findSessionState(userId.intValue());
            if (state == null || level == null || sessionVersion == null
                    || state.level() != level || state.version() != sessionVersion) {
                entryPoint.writeUnauthorized(response, "La sesión ya no es válida.");
                return;
            }
        } catch (Exception e) {
            log.warn("No se pudo validar el estado de sesión para el token entrante", e);
            entryPoint.writeUnauthorized(response, "No se pudo validar la sesión.");
            return;
        }

        var authorities = List.of(new SimpleGrantedAuthority("ROLE_LEVEL_" + level));
        var authToken = new UsernamePasswordAuthenticationToken(userId, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authToken);

        chain.doFilter(request, response);
    }
}
