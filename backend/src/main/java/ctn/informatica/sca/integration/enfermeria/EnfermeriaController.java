package ctn.informatica.sca.integration.enfermeria;

import ctn.informatica.sca.service.LoginAttemptService;
import ctn.informatica.sca.util.PasswordUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * API del sistema de escritorio de enfermería (SHME). Etapa 1: sesión, usuarios, alumnos y ficha
 * médica. Contrato: {@code shmeapi/API_ENFERMERIA_REQUERIMIENTOS.md} — misma convención que
 * anotaciones (sin {@code .php}, parámetros por query o formulario, {@code {ok, datos}} / {@code {ok, error}}).
 * Los datos del alumno (CI, nombre, curso) son del colegio y ningún endpoint los modifica.
 */
@RestController
@RequestMapping("/api/enfermeria")
public class EnfermeriaController {

    static final ZoneOffset PARAGUAY = ZoneOffset.ofHours(-3);
    private static final Set<String> ROLES = Set.of("administrador", "enfermeria");
    private static final int MIN_CONTRASENA = 8;
    /** Para que un usuario inexistente tarde lo mismo que una contraseña mal (no revela qué usuarios existen). */
    private static final String HASH_FICTICIO = PasswordUtil.hash("usuario-inexistente");

    private final EnfermeriaDao dao;
    private final EnfermeriaSecurityConfig.Tokens tokens;
    private final LoginAttemptService intentos;

    public EnfermeriaController(EnfermeriaDao dao, EnfermeriaSecurityConfig.Tokens tokens, LoginAttemptService intentos) {
        this.dao = dao;
        this.tokens = tokens;
        this.intentos = intentos;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestParam String usuario, @RequestParam String contrasena,
            HttpServletRequest request) throws SQLException {
        String[] claves = {"enf-user:" + usuario.trim().toLowerCase(), "enf-ip:" + request.getRemoteAddr()};
        long bloqueo = intentos.blockedSeconds(claves);
        if (bloqueo > 0) throw error(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos. Probá de nuevo en " + bloqueo + " segundos.");
        EnfermeriaDao.Usuario u = dao.usuarioPorNombre(usuario.trim());
        boolean valida = PasswordUtil.matches(contrasena, u != null ? u.hash() : HASH_FICTICIO);
        if (u == null || !valida) {
            intentos.recordFailure(claves);
            throw error(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos");
        }
        intentos.clear(claves);
        return ok(Map.of("id_usuario", u.id(), "usuario", u.usuario(), "rol", u.rol(), "token", tokens.emitir(u)));
    }

    @PostMapping("/usuario_crear")
    public Map<String, Object> usuarioCrear(@AuthenticationPrincipal EnfermeriaDao.Usuario sesion,
            @RequestParam String usuario, @RequestParam String contrasena, @RequestParam String rol) throws SQLException {
        if (!"administrador".equals(sesion.rol())) throw error(HttpStatus.FORBIDDEN, "Solo un administrador puede crear usuarios");
        String nombre = texto(usuario, "usuario", 50);
        if (nombre == null || nombre.length() < 3) throw error(HttpStatus.UNPROCESSABLE_CONTENT, "El usuario debe tener al menos 3 caracteres");
        if (!ROLES.contains(rol)) throw error(HttpStatus.UNPROCESSABLE_CONTENT, "El rol debe ser 'administrador' o 'enfermeria'");
        validarContrasena(contrasena);
        try {
            return ok(Map.of("id_usuario", dao.crearUsuario(nombre, PasswordUtil.hash(contrasena), rol)));
        } catch (EnfermeriaDao.Duplicado ex) {
            throw error(HttpStatus.CONFLICT, "Ya existe un usuario con ese nombre");
        }
    }

    /** Devuelve un token nuevo: el anterior deja de valer al cambiar la contraseña. */
    @PostMapping("/usuario_cambiar_contrasena")
    public Map<String, Object> cambiarContrasena(@AuthenticationPrincipal EnfermeriaDao.Usuario sesion,
            @RequestParam("contrasena_actual") String actual, @RequestParam("contrasena_nueva") String nueva) throws SQLException {
        if (!PasswordUtil.matches(actual, sesion.hash())) throw error(HttpStatus.UNAUTHORIZED, "La contraseña actual no es correcta");
        validarContrasena(nueva);
        String hash = PasswordUtil.hash(nueva);
        dao.cambiarContrasena(sesion.id(), hash);
        return ok(Map.of("token", tokens.emitir(new EnfermeriaDao.Usuario(sesion.id(), sesion.usuario(), hash, sesion.rol()))));
    }

    @RequestMapping("/alumnos_buscar")
    public Map<String, Object> alumnosBuscar(@RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "1") int pagina, @RequestParam(defaultValue = "20") int limite) throws SQLException {
        if (pagina < 1 || limite < 1 || limite > 100) {
            throw error(HttpStatus.UNPROCESSABLE_CONTENT, "'pagina' debe ser 1 o más y 'limite' entre 1 y 100");
        }
        return ok(dao.buscarAlumnos(q.trim(), pagina, limite, anioActual()));
    }

    @RequestMapping("/ficha")
    public Map<String, Object> ficha(@RequestParam int alumno) throws SQLException {
        Map<String, Object> ficha = dao.ficha(alumno, anioActual());
        if (ficha == null) throw error(HttpStatus.NOT_FOUND, "El alumno no existe");
        return ok(ficha);
    }

    @PostMapping("/ficha_guardar")
    public Map<String, Object> fichaGuardar(@RequestParam int alumno,
            @RequestParam(required = false) String enfermedadBase, @RequestParam(required = false) String alergias,
            @RequestParam(name = "tutor_nombre", required = false) String tutorNombre,
            @RequestParam(name = "tutor_apellido", required = false) String tutorApellido,
            @RequestParam(name = "tutor_correo", required = false) String tutorCorreo,
            @RequestParam(name = "tutor_telefono", required = false) String tutorTelefono) throws SQLException {
        String enfermedad = texto(enfermedadBase, "enfermedadBase", 255);
        String nombre = texto(tutorNombre, "tutor_nombre", 100);
        String apellido = texto(tutorApellido, "tutor_apellido", 100);
        String correo = texto(tutorCorreo, "tutor_correo", 150);
        String telefono = texto(tutorTelefono, "tutor_telefono", 30);
        List<Integer> ids = ids(alergias);
        if (!dao.existeAlumno(alumno)) throw error(HttpStatus.NOT_FOUND, "El alumno no existe");
        if (dao.contarAlergias(ids) != ids.size()) throw error(HttpStatus.UNPROCESSABLE_CONTENT, "Alguna de las alergias no existe en el catálogo");
        dao.guardarFicha(alumno, enfermedad, ids, nombre, apellido, correo, telefono);
        return ok(Map.of("alumno", alumno));
    }

    @PostMapping("/ficha_estado")
    public Map<String, Object> fichaEstado(@RequestParam int alumno, @RequestParam int activo) throws SQLException {
        if (activo != 0 && activo != 1) throw error(HttpStatus.UNPROCESSABLE_CONTENT, "'activo' debe ser 0 o 1");
        if (!dao.fichaEstado(alumno, activo == 1)) throw error(HttpStatus.NOT_FOUND, "El alumno no tiene ficha médica");
        return ok(Map.of("alumno", alumno, "activo", activo == 1));
    }

    @RequestMapping("/alergias")
    public Map<String, Object> alergias() throws SQLException {
        return ok(dao.alergias());
    }

    @PostMapping("/alergia_crear")
    public Map<String, Object> alergiaCrear(@RequestParam String nombre) throws SQLException {
        String n = texto(nombre, "nombre", 100);
        if (n == null) throw error(HttpStatus.UNPROCESSABLE_CONTENT, "Falta el parámetro 'nombre'");
        return ok(Map.of("id_alergia", dao.alergiaCrear(n)));
    }

    private static int anioActual() {
        return LocalDate.now(PARAGUAY).getYear();
    }

    private static void validarContrasena(String contrasena) {
        if (contrasena.length() < MIN_CONTRASENA || contrasena.length() > 72) {
            throw error(HttpStatus.UNPROCESSABLE_CONTENT, "La contraseña debe tener entre 8 y 72 caracteres");
        }
    }

    /** Texto recortado; vacío = null. 422 si supera el largo de la columna. */
    private static String texto(String valor, String campo, int max) {
        if (valor == null || valor.isBlank()) return null;
        String t = valor.trim();
        if (t.length() > max) throw error(HttpStatus.UNPROCESSABLE_CONTENT, "'" + campo + "' supera los " + max + " caracteres");
        return t;
    }

    /** "1, 2,3" → [1, 2, 3] sin repetidos; vacío → []. */
    private static List<Integer> ids(String csv) {
        Set<Integer> out = new LinkedHashSet<>();
        if (csv != null && !csv.isBlank()) {
            for (String parte : csv.split(",")) {
                try {
                    out.add(Integer.parseInt(parte.trim()));
                } catch (NumberFormatException ex) {
                    throw error(HttpStatus.UNPROCESSABLE_CONTENT, "'alergias' deben ser ids numéricos separados por coma");
                }
            }
        }
        return new ArrayList<>(out);
    }

    private static Map<String, Object> ok(Object datos) {
        return Map.of("ok", true, "datos", datos);
    }

    private static ResponseStatusException error(HttpStatus status, String mensaje) {
        return new ResponseStatusException(status, mensaje);
    }
}
