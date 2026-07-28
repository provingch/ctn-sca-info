/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Filter.java to edit this template
 */
package ctn.informatica.sia.filter;

import java.io.IOException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;

/**
 *
 * @author jonat
 */
@WebFilter(filterName = "DateFilter", urlPatterns = {"/*"})
public class DateFilter implements Filter {

    private static final Locale PY_LOCALE = new Locale.Builder().setLanguage("es").setRegion("PY").build();
    private static final DateTimeFormatter FMT
            = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", PY_LOCALE);
    private static final ZoneId PY_ZONE = ZoneId.of("America/Asuncion");

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        String uri = request.getRequestURI();

        // Exclude login page(s) and static resources:
        if (!uri.contains("/login") && !isStaticResource(uri)) {
            ZonedDateTime now = ZonedDateTime.now(PY_ZONE);
            String nowFormatted = now.format(FMT);
            nowFormatted = capitalizeFirst(nowFormatted, PY_LOCALE);
            request.setAttribute("nowFormatted", nowFormatted);
        }

        chain.doFilter(req, res);
    }

    private boolean isStaticResource(String uri) {
        String lower = uri.toLowerCase();
        return lower.endsWith(".css") || lower.endsWith(".js")
                || lower.endsWith(".png") || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg") || lower.endsWith(".gif")
                || lower.endsWith(".woff") || lower.endsWith(".woff2")
                || lower.endsWith(".map") || lower.contains("/static/");
    }

    private String capitalizeFirst(String s, Locale locale) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase(locale) + s.substring(1);
    }
}
