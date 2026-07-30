package ctn.informatica.sca.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

/**
 * Fuerza UTF-8 antes de que cualquier filtro o servlet lea parámetros.
 */
public class CharacterEncodingFilter implements Filter {

    private static final String UTF_8 = StandardCharsets.UTF_8.name();

    @Override
    public void init(FilterConfig filterConfig) {
        // No requiere configuración adicional.
    }

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        request.setCharacterEncoding(UTF_8);
        response.setCharacterEncoding(UTF_8);
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // No mantiene recursos.
    }
}
