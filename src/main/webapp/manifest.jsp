<%@ page contentType="application/manifest+json; charset=UTF-8" %>
<%@ page pageEncoding="UTF-8" %>
<%
    String ctx = request.getContextPath();
    String basePath = (ctx == null || ctx.isEmpty()) ? "" : ctx;
    String startUrl = basePath + "/inicio";
    String scope = basePath + "/";
    String iconBase = basePath + "/icons/pwa";
%>
{
  "name": "SCA",
  "short_name": "SCA",
  "description": "Sistema de informes académicos del Colegio Técnico Nacional",
  "start_url": "<%= startUrl %>",
  "scope": "<%= scope %>",
  "display": "standalone",
  "background_color": "#1f2d3d",
  "theme_color": "#1f2d3d",
  "orientation": "portrait-primary",
  "icons": [
    { "src": "<%= iconBase %>/icon-192.png", "sizes": "192x192", "type": "image/png" },
    { "src": "<%= iconBase %>/icon-512.png", "sizes": "512x512", "type": "image/png" },
    { "src": "<%= iconBase %>/icon-maskable-192.png", "sizes": "192x192", "type": "image/png", "purpose": "maskable" },
    { "src": "<%= iconBase %>/icon-maskable-512.png", "sizes": "512x512", "type": "image/png", "purpose": "maskable" }
  ]
}
