package ctn.informatica.sca.integration.enfermeria;

import ctn.informatica.sca.service.LoginAttemptService;
import ctn.informatica.sca.util.PasswordUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
 * API del sistema de escritorio de enfermería (SHME): sesión, usuarios, alumnos, ficha médica,
 * consultas (con descuento de stock), insumos, solicitudes y reportes. Contrato: {@code shmeapi/API_ENFERMERIA_REQUERIMIENTOS.md} — misma convención que
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
        String texto = q.trim();
        // "total" va aparte para no cambiar la forma de "datos" (sigue siendo la lista de la página).
        return Map.of("ok", true, "datos", dao.buscarAlumnos(texto, pagina, limite, anioActual()),
                "total", dao.contarAlumnos(texto));
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

    // ---------------------------------------------------------------- Consultas

    @RequestMapping("/consultas")
    public Map<String, Object> consultas(@RequestParam int alumno) throws SQLException {
        return ok(dao.consultas(alumno));
    }

    @RequestMapping("/consulta")
    public Map<String, Object> consulta(@RequestParam int id) throws SQLException {
        Map<String, Object> consulta = dao.consulta(id);
        if (consulta == null) throw error(HttpStatus.NOT_FOUND, "La consulta no existe");
        return ok(consulta);
    }

    @PostMapping("/consulta_crear")
    public Map<String, Object> consultaCrear(@AuthenticationPrincipal EnfermeriaDao.Usuario sesion,
            @RequestParam int alumno, @RequestParam(required = false) String motivo, @RequestParam(required = false) String fecha,
            @RequestParam(required = false) String tratamiento, @RequestParam(required = false) String horaEntrada,
            @RequestParam(required = false) String horaSalida, @RequestParam(required = false) String observaciones,
            @RequestParam(required = false) String insumos) throws SQLException {
        EnfermeriaDao.DatosConsulta datos = datosConsulta(alumno, motivo, fecha, tratamiento, horaEntrada, horaSalida, observaciones);
        Map<Integer, Integer> usados = cantidades(insumos);
        return ok(Map.of("idConsulta", dao.crearConsulta(datos, usados, sesion.id())));
    }

    @PostMapping("/consulta_editar")
    public Map<String, Object> consultaEditar(@RequestParam int id,
            @RequestParam int alumno, @RequestParam(required = false) String motivo, @RequestParam(required = false) String fecha,
            @RequestParam(required = false) String tratamiento, @RequestParam(required = false) String horaEntrada,
            @RequestParam(required = false) String horaSalida, @RequestParam(required = false) String observaciones,
            @RequestParam(required = false) String insumos) throws SQLException {
        EnfermeriaDao.DatosConsulta datos = datosConsulta(alumno, motivo, fecha, tratamiento, horaEntrada, horaSalida, observaciones);
        if (!dao.editarConsulta(id, datos, cantidades(insumos))) throw error(HttpStatus.NOT_FOUND, "La consulta no existe");
        return ok(Map.of("idConsulta", id));
    }

    @PostMapping("/consulta_eliminar")
    public Map<String, Object> consultaEliminar(@RequestParam int id) throws SQLException {
        if (!dao.eliminarConsulta(id)) throw error(HttpStatus.NOT_FOUND, "La consulta no existe");
        return ok(Map.of("idConsulta", id));
    }

    @RequestMapping("/motivos")
    public Map<String, Object> motivos() throws SQLException {
        return ok(dao.motivos());
    }

    @RequestMapping("/consultas_reporte")
    public Map<String, Object> consultasReporte(@RequestParam String desde, @RequestParam String hasta,
            @RequestParam(required = false) Integer alumno, @RequestParam(required = false) String motivo) throws SQLException {
        LocalDate d = fecha(desde, "desde");
        LocalDate h = fecha(hasta, "hasta");
        if (h.isBefore(d)) throw error(HttpStatus.UNPROCESSABLE_CONTENT, "'hasta' no puede ser anterior a 'desde'");
        return ok(dao.reporteConsultas(d, h, alumno, motivo == null || motivo.isBlank() ? null : motivo.trim(), anioActual()));
    }

    // ---------------------------------------------------------------- Insumos y solicitudes

    @RequestMapping("/insumos")
    public Map<String, Object> insumos(@RequestParam(defaultValue = "0") int todos) throws SQLException {
        return ok(dao.insumos(todos == 1));
    }

    @PostMapping("/insumo_crear")
    public Map<String, Object> insumoCrear(@RequestParam String nombre, @RequestParam("stock_actual") int stockActual,
            @RequestParam("stock_minimo") int stockMinimo) throws SQLException {
        String n = nombreInsumo(nombre);
        noNegativos(stockActual, stockMinimo);
        return ok(Map.of("id_insumo", dao.crearInsumo(n, stockActual, stockMinimo)));
    }

    @PostMapping("/insumo_editar")
    public Map<String, Object> insumoEditar(@RequestParam int id, @RequestParam String nombre,
            @RequestParam("stock_actual") int stockActual, @RequestParam("stock_minimo") int stockMinimo) throws SQLException {
        String n = nombreInsumo(nombre);
        noNegativos(stockActual, stockMinimo);
        if (!dao.editarInsumo(id, n, stockActual, stockMinimo)) throw error(HttpStatus.NOT_FOUND, "El insumo no existe");
        return ok(Map.of("id_insumo", id));
    }

    @PostMapping("/insumo_estado")
    public Map<String, Object> insumoEstado(@RequestParam int id, @RequestParam int activo) throws SQLException {
        if (activo != 0 && activo != 1) throw error(HttpStatus.UNPROCESSABLE_CONTENT, "'activo' debe ser 0 o 1");
        if (!dao.insumoEstado(id, activo == 1)) throw error(HttpStatus.NOT_FOUND, "El insumo no existe");
        return ok(Map.of("id_insumo", id, "activo", activo == 1));
    }

    @PostMapping("/insumo_sumar_stock")
    public Map<String, Object> insumoSumarStock(@RequestParam int id, @RequestParam int cantidad) throws SQLException {
        if (cantidad < 1) throw error(HttpStatus.UNPROCESSABLE_CONTENT, "'cantidad' debe ser 1 o más");
        if (!dao.sumarStock(id, cantidad)) throw error(HttpStatus.NOT_FOUND, "El insumo no existe");
        return ok(Map.of("id_insumo", id));
    }

    /** {@code items}: una línea por ítem, {@code id:cantidad:observacion} (la observación puede tener ':'). */
    @PostMapping("/solicitud_crear")
    public Map<String, Object> solicitudCrear(@AuthenticationPrincipal EnfermeriaDao.Usuario sesion,
            @RequestParam String items) throws SQLException {
        List<EnfermeriaDao.ItemSolicitud> lista = new ArrayList<>();
        for (String linea : items.split("\\R")) {
            if (linea.isBlank()) continue;
            String[] p = linea.split(":", 3);
            int id;
            int cantidad;
            try {
                id = Integer.parseInt(p[0].trim());
                cantidad = Integer.parseInt(p.length > 1 ? p[1].trim() : "");
            } catch (NumberFormatException ex) {
                throw error(HttpStatus.UNPROCESSABLE_CONTENT, "Cada ítem debe ser 'id:cantidad:observacion' (línea: " + linea.trim() + ")");
            }
            if (cantidad < 1) throw error(HttpStatus.UNPROCESSABLE_CONTENT, "La cantidad de cada ítem debe ser 1 o más");
            lista.add(new EnfermeriaDao.ItemSolicitud(id, cantidad, texto(p.length > 2 ? p[2] : null, "observacion", 255)));
        }
        if (lista.isEmpty()) throw error(HttpStatus.UNPROCESSABLE_CONTENT, "La solicitud no tiene ítems");
        return ok(Map.of("id_solicitud", dao.crearSolicitud(lista, LocalDate.now(PARAGUAY), sesion.id())));
    }

    @PostMapping("/reporte_registrar")
    public Map<String, Object> reporteRegistrar(@AuthenticationPrincipal EnfermeriaDao.Usuario sesion) throws SQLException {
        return ok(Map.of("id_reporte", dao.registrarReporte(LocalDate.now(PARAGUAY), sesion.id())));
    }

    @RequestMapping("/resumen")
    public Map<String, Object> resumen() throws SQLException {
        return ok(dao.resumen());
    }

    private EnfermeriaDao.DatosConsulta datosConsulta(int alumno, String motivo, String fecha, String tratamiento,
            String horaEntrada, String horaSalida, String observaciones) throws SQLException {
        String m = texto(motivo, "motivo", 150);
        if (m == null) throw error(HttpStatus.UNPROCESSABLE_CONTENT, "Falta el parámetro 'motivo'");
        if (fecha == null || fecha.isBlank()) throw error(HttpStatus.UNPROCESSABLE_CONTENT, "Falta el parámetro 'fecha'");
        LocalTime entrada = hora(horaEntrada, "horaEntrada");
        LocalTime salida = hora(horaSalida, "horaSalida");
        if (entrada != null && salida != null && salida.isBefore(entrada)) {
            throw error(HttpStatus.UNPROCESSABLE_CONTENT, "'horaSalida' no puede ser anterior a 'horaEntrada'");
        }
        EnfermeriaDao.DatosConsulta d = new EnfermeriaDao.DatosConsulta(alumno, m, fecha(fecha, "fecha"),
                texto(tratamiento, "tratamiento", 65_535), entrada, salida, texto(observaciones, "observaciones", 65_535));
        if (!dao.existeAlumno(alumno)) throw error(HttpStatus.NOT_FOUND, "El alumno no existe");
        return d;
    }

    private static LocalDate fecha(String valor, String campo) {
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException ex) {
            throw error(HttpStatus.UNPROCESSABLE_CONTENT, "'" + campo + "' debe tener el formato AAAA-MM-DD");
        }
    }

    /** "08:30:00" (también acepta "08:30"); vacío = null. */
    private static LocalTime hora(String valor, String campo) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return LocalTime.parse(valor.trim());
        } catch (DateTimeParseException ex) {
            throw error(HttpStatus.UNPROCESSABLE_CONTENT, "'" + campo + "' debe tener el formato HH:MM:SS");
        }
    }

    /** "3:2, 5:1" → {3=2, 5=1}; un id repetido suma sus cantidades. Vacío → sin insumos. */
    private static Map<Integer, Integer> cantidades(String csv) {
        Map<Integer, Integer> out = new LinkedHashMap<>();
        if (csv == null || csv.isBlank()) return out;
        for (String parte : csv.split(",")) {
            String[] p = parte.split(":");
            int id;
            int cantidad;
            try {
                if (p.length != 2) throw new NumberFormatException();
                id = Integer.parseInt(p[0].trim());
                cantidad = Integer.parseInt(p[1].trim());
            } catch (NumberFormatException ex) {
                throw error(HttpStatus.UNPROCESSABLE_CONTENT, "'insumos' debe ser 'id:cantidad' separados por coma");
            }
            if (cantidad < 1) throw error(HttpStatus.UNPROCESSABLE_CONTENT, "La cantidad de cada insumo debe ser 1 o más");
            out.merge(id, cantidad, Integer::sum);
        }
        return out;
    }

    private static String nombreInsumo(String nombre) {
        String n = texto(nombre, "nombre", 100);
        if (n == null) throw error(HttpStatus.UNPROCESSABLE_CONTENT, "Falta el parámetro 'nombre'");
        return n;
    }

    private static void noNegativos(int stockActual, int stockMinimo) {
        if (stockActual < 0 || stockMinimo < 0) throw error(HttpStatus.UNPROCESSABLE_CONTENT, "El stock no puede ser negativo");
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
