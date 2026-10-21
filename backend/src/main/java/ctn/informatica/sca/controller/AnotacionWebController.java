package ctn.informatica.sca.controller;

import ctn.informatica.sca.dao.AnotacionWebDao;
import ctn.informatica.sca.dao.PadreDao;
import java.sql.SQLException;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Anotaciones de la app Android vistas desde la web. Las fotos son evidencia (por ejemplo,
 * el uniforme), así que solo las ven Coordinación Pedagógica (nivel 5) y el padre de ese alumno.
 * Los paths quedan bajo /api/admin y /api/padre para heredar los roles de SecurityConfig;
 * /api/anotaciones es de la app y usa otra cadena de seguridad.
 */
@RestController
public class AnotacionWebController {
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId ZONA = ZoneId.of("America/Asuncion");
    private static final int LIMITE_MAXIMO = 300;

    private final AnotacionWebDao dao;
    private final PadreDao padreDao;

    @Autowired
    public AnotacionWebController(AnotacionWebDao dao) {
        this(dao, new PadreDao());
    }

    AnotacionWebController(AnotacionWebDao dao, PadreDao padreDao) {
        this.dao = dao;
        this.padreDao = padreDao;
    }

    public record AnotacionDto(int id, int alumnoId, String alumnoNombre, String alumnoApellido, String curso,
            String fechaHora, String tipo, String observacion, String materia, String profesor, boolean tieneFoto) {
    }

    @GetMapping("/api/admin/anotaciones")
    @PreAuthorize("hasRole('LEVEL_5')")
    public List<AnotacionDto> recientes(@RequestParam(defaultValue = "100") int limite) {
        try {
            return dao.recientes(Math.max(1, Math.min(limite, LIMITE_MAXIMO))).stream().map(AnotacionWebController::dto).toList();
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudieron cargar las anotaciones", ex);
        }
    }

    @GetMapping("/api/admin/anotaciones/{anotacionId}/foto")
    @PreAuthorize("hasRole('LEVEL_5')")
    public ResponseEntity<byte[]> fotoCoordinacion(@PathVariable int anotacionId) {
        try {
            return imagen(dao.foto(anotacionId, null));
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo cargar la foto", ex);
        }
    }

    @GetMapping("/api/padre/alumnos/{alumnoId}/anotaciones")
    public List<AnotacionDto> delHijo(@PathVariable int alumnoId, Authentication authentication) {
        try {
            requireOwnChild(ApiAuth.requireUserId(authentication), alumnoId);
            return dao.porAlumno(alumnoId).stream().map(AnotacionWebController::dto).toList();
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudieron cargar las anotaciones", ex);
        }
    }

    @GetMapping("/api/padre/alumnos/{alumnoId}/anotaciones/{anotacionId}/foto")
    public ResponseEntity<byte[]> fotoPadre(@PathVariable int alumnoId, @PathVariable int anotacionId, Authentication authentication) {
        try {
            requireOwnChild(ApiAuth.requireUserId(authentication), alumnoId);
            return imagen(dao.foto(anotacionId, alumnoId));
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo cargar la foto", ex);
        }
    }

    private void requireOwnChild(int userId, int alumnoId) throws SQLException {
        if (padreDao.findChildrenByPadreId(userId).stream().noneMatch(child -> child.getId() == alumnoId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ese alumno no está vinculado a tu cuenta.");
        }
    }

    private static ResponseEntity<byte[]> imagen(AnotacionWebDao.Foto foto) {
        if (foto == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la foto");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.tipoMime()))
                .cacheControl(CacheControl.noStore())
                .body(foto.imagen());
    }

    static AnotacionDto dto(AnotacionWebDao.Anotacion a) {
        return new AnotacionDto(a.id(), a.alumnoId(), a.alumnoNombre(), a.alumnoApellido(), curso(a),
                a.fechaHora() == null ? null : a.fechaHora().format(FECHA_HORA), a.tipo(), a.observacion(), a.materia(),
                profesor(a), a.fotoId() != null);
    }

    /** "Informática · 2° B"; egresados (promoción ya pasada) muestran la promoción. */
    private static String curso(AnotacionWebDao.Anotacion a) {
        if (a.especialidad() == null || a.promocion() == null) return null;
        int nivel = Year.now(ZONA).getValue() - a.promocion() + 3;
        String seccion = a.seccion() == null ? "" : " " + a.seccion();
        return nivel >= 1 && nivel <= 3
                ? a.especialidad() + " · " + nivel + "°" + seccion
                : a.especialidad() + " · Promoción " + a.promocion() + seccion;
    }

    private static String profesor(AnotacionWebDao.Anotacion a) {
        String nombre = ((a.profesorNombre() == null ? "" : a.profesorNombre()) + " "
                + (a.profesorApellido() == null ? "" : a.profesorApellido())).trim();
        return nombre.isEmpty() ? null : nombre;
    }
}
