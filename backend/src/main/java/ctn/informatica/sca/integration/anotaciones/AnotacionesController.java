package ctn.informatica.sca.integration.anotaciones;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * API de la app Android de anotaciones (conducta, uniforme...), separada de la de GEMA.
 * Contrato: {@code ceciapi/API_REQUERIMIENTOS.md} — mismos endpoints que la API PHP original
 * (sin el {@code .php}) y mismas respuestas {@code {ok, datos}} / {@code {ok, error}}
 * (los errores los arma {@link AnotacionesErrorHandler}). Las lecturas aceptan GET o POST.
 */
@RestController
@RequestMapping("/api/anotaciones")
public class AnotacionesController {

    /** Paraguay: UTC-3 fijo. "Hoy" y {@code fecha_hora} no dependen de la zona del servidor. */
    static final ZoneOffset PARAGUAY = ZoneOffset.ofHours(-3);
    static final int MAX_FOTO_BYTES = 5 * 1024 * 1024;
    private static final int MAX_OBSERVACION = 500;

    private final AnotacionesDao dao;

    public AnotacionesController(AnotacionesDao dao) {
        this.dao = dao;
    }

    @RequestMapping("/login")
    public Map<String, Object> login(@RequestParam long ci) throws SQLException {
        Map<String, Object> profesor = dao.profesorPorCi(ci);
        if (profesor == null) throw error(HttpStatus.NOT_FOUND, "No existe un profesor con esa C.I.");
        return ok(profesor);
    }

    @RequestMapping("/menus")
    public Map<String, Object> menus(@RequestParam int profesor) throws SQLException {
        return ok(dao.menus(profesor));
    }

    @RequestMapping("/alumnos")
    public Map<String, Object> alumnos(@RequestParam int profesor, @RequestParam int materia,
            @RequestParam("curso_base") int cursoBase) throws SQLException {
        return ok(dao.alumnos(profesor, materia, cursoBase, LocalDate.now(PARAGUAY).getYear()));
    }

    @RequestMapping("/tipos_anotacion")
    public Map<String, Object> tiposAnotacion() throws SQLException {
        return ok(dao.tiposActivos());
    }

    @RequestMapping("/historial")
    public Map<String, Object> historial(@RequestParam int alumno) throws SQLException {
        return ok(dao.historial(alumno));
    }

    @RequestMapping("/foto")
    public ResponseEntity<byte[]> foto(@RequestParam(required = false) Integer id,
            @RequestParam(name = "anotacion_id", required = false) Integer anotacionId) throws SQLException {
        if (id == null && anotacionId == null) {
            throw error(HttpStatus.UNPROCESSABLE_CONTENT, "Indicá 'id' (de la foto) o 'anotacion_id'");
        }
        AnotacionesDao.Foto foto = dao.foto(id, anotacionId);
        if (foto == null) throw error(HttpStatus.NOT_FOUND, "No se encontró la foto");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.tipoMime()))
                .cacheControl(CacheControl.maxAge(600, TimeUnit.SECONDS).cachePrivate())
                .body(foto.imagen());
    }

    @PostMapping("/guardar_anotacion")
    public Map<String, Object> guardarAnotacion(@RequestParam int alumno, @RequestParam int profesor,
            @RequestParam int tipo, @RequestParam(required = false) Integer materia,
            @RequestParam(required = false) String observacion,
            @RequestParam(required = false) MultipartFile foto) throws Exception {
        String obs = observacion == null || observacion.isBlank() ? null : observacion.trim();
        if (obs != null && obs.length() > MAX_OBSERVACION) {
            throw error(HttpStatus.UNPROCESSABLE_CONTENT, "La observación supera los 500 caracteres");
        }
        AnotacionesDao.Tipo t = dao.tipoActivo(tipo);
        if (t == null) throw error(HttpStatus.NOT_FOUND, "El tipo de anotación no existe o está inactivo");
        if (t.requiereObservacion() && obs == null) {
            throw error(HttpStatus.UNPROCESSABLE_CONTENT, "Este tipo de anotación requiere que escribas la observación");
        }
        if (!dao.existeAlumno(alumno)) throw error(HttpStatus.NOT_FOUND, "El alumno no existe");
        if (!dao.existeProfesor(profesor)) throw error(HttpStatus.NOT_FOUND, "El profesor no existe");
        if (materia != null && !dao.existeMateria(materia)) throw error(HttpStatus.NOT_FOUND, "La materia no existe");

        LocalDateTime ahora = LocalDateTime.now(PARAGUAY).truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime hoy = ahora.toLocalDate().atStartOfDay();
        if (!t.repetible() && dao.existeDuplicado(alumno, tipo, hoy, hoy.plusDays(1), t.requiereObservacion(), obs)) {
            throw error(HttpStatus.CONFLICT, "Este alumno ya tiene registrada esa anotación el día de hoy.");
        }

        boolean hayFoto = foto != null && !foto.isEmpty();
        if (t.requiereFoto() && !hayFoto) {
            throw error(HttpStatus.UNPROCESSABLE_CONTENT, "Este tipo de anotación requiere una foto de evidencia");
        }
        byte[] imagen = hayFoto ? validarFoto(foto) : null;

        int id = dao.insertar(alumno, profesor, tipo, materia, ahora, obs, imagen);
        return ok(Map.of("id", id, "alumno_id", alumno, "tipo_id", tipo, "tiene_foto", imagen != null));
    }

    private static byte[] validarFoto(MultipartFile foto) throws IOException {
        if (foto.getSize() > MAX_FOTO_BYTES) throw error(HttpStatus.CONTENT_TOO_LARGE, "La foto supera los 5 MB");
        byte[] bytes = foto.getBytes();
        // Firma JPEG (FF D8 FF): no se confía en el Content-Type que manda el cliente.
        if (bytes.length < 3 || (bytes[0] & 0xFF) != 0xFF || (bytes[1] & 0xFF) != 0xD8 || (bytes[2] & 0xFF) != 0xFF) {
            throw error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Formato de imagen no permitido (solo JPEG)");
        }
        return bytes;
    }

    private static Map<String, Object> ok(Object datos) {
        return Map.of("ok", true, "datos", datos);
    }

    private static ResponseStatusException error(HttpStatus status, String mensaje) {
        return new ResponseStatusException(status, mensaje);
    }
}
