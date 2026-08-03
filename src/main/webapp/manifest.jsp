<%@ page contentType="application/manifest+json; charset=UTF-8" %>
<%@ page pageEncoding="UTF-8" %>
<%
    String ctx = request.getContextPath();
    String basePath = (ctx == null || ctx.isEmpty()) ? "" : ctx;
    String startUrl = basePath + "/inicio";
    String scope = basePath + "/";
    String iconBase = basePath + "/icons/pwa";
    String assetVersion = application.getInitParameter("sca.asset.version");
    if (assetVersion == null || assetVersion.isBlank()) {
        assetVersion = "0.6.5";
    }
%>
{
  "name": "SCA",
  "short_name": "SCA",
  "description": "Sistema de informes académicos del Colegio T\u00e9cnico Nacional",
  "id": "<%= scope %>?v=<%= assetVersion %>",
  "version": "<%= assetVersion %>",
  "start_url": "<%= startUrl %>",
  "scope": "<%= scope %>",
  "display": "standalone",
  "background_color": "#1f2d3d",
  "theme_color": "#1f2d3d",
  "orientation": "portrait-primary",
  "icons": [
    { "src": "<%= iconBase %>/icon-192.png?v=<%= assetVersion %>", "sizes": "192x192", "type": "image/png" },
    { "src": "<%= iconBase %>/icon-512.png?v=<%= assetVersion %>", "sizes": "512x512", "type": "image/png" },
    { "src": "<%= iconBase %>/icon-maskable-192.png?v=<%= assetVersion %>", "sizes": "192x192", "type": "image/png", "purpose": "maskable" },
    { "src": "<%= iconBase %>/icon-maskable-512.png?v=<%= assetVersion %>", "sizes": "512x512", "type": "image/png", "purpose": "maskable" }
  ]
}
