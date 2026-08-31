package ctn.informatica.sca.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Reenvía las rutas client-side de React Router (frontend/src/routes/AppRoutes.tsx)
 * hacia index.html, para que un F5 o un acceso directo a /home, /profile, etc.
 * no le pegue 404 al servidor (esas rutas no son archivos reales, las resuelve
 * React Router en el browser una vez que index.html + el bundle ya cargaron).
 *
 * IMPORTANTE: cada ruta nueva que se agregue a AppRoutes.tsx (Bloque 3: /planilla,
 * /evaluacion, etc.) tiene que sumarse acá Y en SecurityConfig.permitAll GET,
 * o el acceso directo/F5 a esa ruta va a devolver 404 o 401 respectivamente.
 */
@Controller
public class SpaForwardController {

    @GetMapping({
            "/", "/login", "/home", "/inicio", "/profile", "/perfil",
            "/evaluacion", "/coordinacion", "/admin", "/admin/{section}", "/padre", "/styleguide",
            "/privacidad", "/terminos", "/planilla/{planillaId}",
            "/planilla/{planillaId}/tarea", "/planilla/{planillaId}/tarea/{tareaId}",
            "/admin/horarios/{cursoId}", "/offline",
            "/google/callback", "/google/authorize"
    })
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
