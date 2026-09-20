package ctn.informatica.sca.dao;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ctn.informatica.sca.clases.conexion;
import ctn.informatica.sca.controller.EvaluacionCatalogController;
import ctn.informatica.sca.dto.TemaPlanDto;
import ctn.informatica.sca.service.PlanCurricularRecordatorioService;
import ctn.informatica.sca.service.PlanCurricularRetroactivoService;
import ctn.informatica.sca.service.TemaVerificacionService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/**
 * Contra una base MySQL/MariaDB real con el esquema de {@code database/db-tables-properties.sql}.
 * Sólo corre con {@code SCA_IT_DB=1} (y CTN_DB_* apuntando a una base descartable): inserta y borra filas
 * con nombres propios, pero no debe apuntar nunca a la base de producción.
 */
class RetroactivoDbIntegrationTest {

    private static final int ESPECIALIDAD_ID = 9_000_001;
    private static final int ANIO = 2026;

    private final PlanCurricularDao planDao = new PlanCurricularDao();
    private final RasgoPlanillaDao rasgoDao = new RasgoPlanillaDao();
    private final IncumplimientoRevisionDao incumplimientoDao = new IncumplimientoRevisionDao();
    private final NotificacionDao notificacionDao = new NotificacionDao();
    private final UserDao userDao = new UserDao();

    private int profesorId;
    private int evaluadorId;
    private int asignacionId;
    private int cursoId;
    private final List<Integer> cursoBaseIds = new ArrayList<>();
    private int materiaId;

    @BeforeEach
    void seed() throws Exception {
        assumeTrue("1".equals(System.getenv("SCA_IT_DB")), "Definí SCA_IT_DB=1 (con CTN_DB_* de una base descartable) para correr esto");
        try (Connection c = new conexion().getCon()) {
            // ok
        } catch (SQLException ex) {
            assumeTrue(false, "Sin base de integración: " + ex.getMessage());
        }
        limpiar();
        exec("INSERT INTO especialidad (id, nombre) VALUES (" + ESPECIALIDAD_ID + ", 'IT Especialidad')");
        profesorId = insertar("INSERT INTO usuario (usuario, nombre, apellido, contrasenia, nivel) VALUES ('it_prof', 'Pro', 'Fesor', 'x', 1)");
        evaluadorId = insertar("INSERT INTO usuario (usuario, nombre, apellido, contrasenia, nivel) VALUES ('it_eval', 'Eva', 'Luador', 'x', 2)");
        int cursoBaseId = insertar("INSERT INTO curso_base (especialidad_id, nivel, seccion) VALUES (" + ESPECIALIDAD_ID + ", 1, 'A')");
        cursoBaseIds.add(cursoBaseId);
        materiaId = insertar("INSERT INTO materia (nombre, categoria) VALUES ('IT Materia', 'comun')");
        asignacionId = insertar("INSERT INTO asignacion (usuario_id, materia_id, curso_base_id) VALUES (" + profesorId + ", " + materiaId + ", " + cursoBaseId + ")");
        cursoId = insertar("INSERT INTO curso (especialidad_id, promocion, seccion) VALUES (" + ESPECIALIDAD_ID + ", 2028, 'A')");
    }

    @AfterEach
    void limpiar() throws Exception {
        if (!"1".equals(System.getenv("SCA_IT_DB"))) {
            return;
        }
        try {
            exec("DELETE FROM notificacion WHERE usuario_id IN (SELECT id FROM usuario WHERE usuario IN ('it_prof','it_eval'))");
            exec("DELETE FROM usuario WHERE usuario IN ('it_prof','it_eval')"); // cascada: asignacion, plan, planillas, incumplimientos
            exec("DELETE FROM curso WHERE especialidad_id = " + ESPECIALIDAD_ID);
            exec("DELETE FROM curso_base WHERE especialidad_id = " + ESPECIALIDAD_ID);
            exec("DELETE FROM materia WHERE nombre = 'IT Materia'");
            exec("DELETE FROM especialidad WHERE id = " + ESPECIALIDAD_ID);
        } catch (SQLException ignored) {
            // sin base: el @BeforeEach ya hizo skip
        }
    }

    private static void exec(String sql) throws SQLException {
        try (Connection c = new conexion().getCon(); Statement st = c.createStatement()) {
            st.executeUpdate(sql);
        }
    }

    private static int insertar(String sql) throws SQLException {
        try (Connection c = new conexion().getCon(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private static String scalar(String sql) throws SQLException {
        try (Connection c = new conexion().getCon(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    private int clase(String tema, String fecha) throws SQLException {
        return insertar("INSERT INTO planilla_rasgo (curso_id, usuario_id, asignacion_id, tema, fecha_clase) VALUES ("
                + cursoId + ", " + profesorId + ", " + asignacionId + ", '" + tema + "', '" + fecha + "')");
    }

    private static TemaPlanDto tema(String mes, int orden, String contenido) {
        TemaPlanDto t = new TemaPlanDto();
        t.mes = mes;
        t.ordenMes = orden;
        t.bloque = 1;
        t.temasContenidos = contenido;
        return t;
    }

    private int planEtapa1(byte[] archivo) throws SQLException {
        return planDao.saveOrReplace(asignacionId, "1", ANIO, "plan.xlsx", archivo,
                List.of(tema("Marzo", 1, "Unidad 1 Sistemas"), tema("Abril", 2, "Unidad 2 Redes")));
    }

    private PlanCurricularRetroactivoService retroactivoService() {
        return new PlanCurricularRetroactivoService(new TemaVerificacionService(), rasgoDao, planDao,
                incumplimientoDao, notificacionDao, userDao);
    }

    private EvaluacionCatalogController controller() {
        return new EvaluacionCatalogController(new CursoDao(), new EspecialidadDao(), new InstrumentoDao(), rasgoDao,
                incumplimientoDao, new AsignacionDao(), notificacionDao, userDao, new ProfesorDao(), new ConfiguracionSistemaDao());
    }

    private UsernamePasswordAuthenticationToken comoEvaluador() {
        return new UsernamePasswordAuthenticationToken((long) evaluadorId, null, List.of());
    }

    // ---- Bloque 1 -------------------------------------------------------------------------------------

    @Test
    void rechazarUnPlanNoTiraExcepcionYConservaElArchivoSubido() throws Exception {
        byte[] archivo = {1, 2, 3, 4, 5};
        int planId = planEtapa1(archivo);

        planDao.rechazar(planId, evaluadorId, "Faltan indicadores");

        assertArrayEquals(archivo, planDao.getArchivoOriginal(planId));
        var plan = planDao.findById(planId);
        assertEquals("RECHAZADO", plan.estado);
        assertEquals("Faltan indicadores", plan.observacionesEvaluador);
        assertEquals(asignacionId, plan.asignacionId);
    }

    // ---- Bloque 3 -------------------------------------------------------------------------------------

    @Test
    void existePlanCuentaCualquierEstado() throws Exception {
        assertFalse(planDao.existePlan(asignacionId, "1", ANIO));
        int planId = planEtapa1(new byte[] {1});
        assertTrue(planDao.existePlan(asignacionId, "1", ANIO));
        planDao.rechazar(planId, evaluadorId, "x");
        assertTrue(planDao.existePlan(asignacionId, "1", ANIO));
        assertFalse(planDao.existePlan(asignacionId, "2", ANIO));
    }

    @Test
    void recordatorioSeCreaUnaVezSeCierraAlSubirElPlanYNoSeMarcaAMano() throws Exception {
        PlanCurricularRecordatorioService recordatorio = new PlanCurricularRecordatorioService(
                new AsignacionDao(), planDao, notificacionDao, userDao);
        LocalDate abril = LocalDate.of(ANIO, 4, 10);

        recordatorio.recordarPlanesPendientes(abril);
        recordatorio.recordarPlanesPendientes(abril); // dedupe: no repite mientras siga sin leer

        assertEquals("1", scalar("SELECT COUNT(*) FROM notificacion WHERE usuario_id = " + profesorId + " AND tipo = 'PLAN_PENDIENTE' AND entidad_id = " + asignacionId));
        var lista = notificacionDao.listarPorUsuario(profesorId, "profesor", true);
        String cuerpo = (String) lista.get(0).get("cuerpo");
        assertTrue(cuerpo.startsWith("Todavía no subiste tu plan curricular de IT Materia · "), cuerpo);
        assertTrue(cuerpo.endsWith(" para esta etapa."), cuerpo);
        long notifId = (Long) lista.get(0).get("id");
        assertEquals("PLAN_PENDIENTE", notificacionDao.findTipo((int) notifId, profesorId, "profesor").orElseThrow());

        // "leer todas" no la toca
        assertEquals(0, notificacionDao.marcarTodasLeidas(profesorId, "profesor"));
        assertEquals(1, notificacionDao.contarNoLeidas(profesorId, "profesor"));

        // se cierra sola al subir el plan
        assertEquals(1, notificacionDao.marcarLeidasPorEntidad("ASIGNACION", asignacionId, "PLAN_PENDIENTE"));
        assertEquals(0, notificacionDao.contarNoLeidas(profesorId, "profesor"));
        // y con plan cargado no se vuelve a crear
        planEtapa1(new byte[] {1});
        recordatorio.recordarPlanesPendientes(abril);
        assertEquals("1", scalar("SELECT COUNT(*) FROM notificacion WHERE usuario_id = " + profesorId + " AND tipo = 'PLAN_PENDIENTE'"));
    }

    // ---- Bloque 4 -------------------------------------------------------------------------------------

    @Test
    void reprocesoRetroactivoComparaLasClasesPreviasContraElPlanAprobado() throws Exception {
        int aTiempo = clase("Unidad 1 Sistemas", ANIO + "-03-10");
        int atrasada = clase("Repaso general", ANIO + "-06-10");
        int otraEtapa = clase("Tema de septiembre", ANIO + "-09-10");
        int otroAnio = clase("Tema viejo", (ANIO - 1) + "-04-10");
        int planId = planEtapa1(new byte[] {9});
        planDao.aprobar(planId, evaluadorId);

        int incongruencias = retroactivoService().reprocesarClasesPrevias(planId, asignacionId, "1", ANIO, profesorId);

        assertEquals(1, incongruencias);
        assertEquals("OK", estadoClase(aTiempo));
        assertEquals("ATRASADO", estadoClase(atrasada));
        assertEquals("SIN_PLAN", estadoClase(otraEtapa), "septiembre es de la etapa 2");
        assertEquals("SIN_PLAN", estadoClase(otroAnio), "clase de otro año lectivo");
        assertEquals("CUBIERTO", scalar("SELECT estado_cobertura FROM tema_plan_curricular WHERE plan_curricular_id = " + planId + " AND orden_mes = 1"));
        assertEquals("PENDIENTE", scalar("SELECT estado_cobertura FROM tema_plan_curricular WHERE plan_curricular_id = " + planId + " AND orden_mes = 2"));

        var pendientes = incumplimientoDao.listarIncongruenciasPendientesPorUsuario(profesorId);
        assertEquals(1, pendientes.size());
        Map<String, Object> fila = pendientes.get(0);
        assertEquals("INCONGRUENCIA_RETROACTIVA", fila.get("tipo"));
        assertEquals(atrasada, fila.get("planillaRasgoId"));
        assertEquals("Repaso general", fila.get("temaIngresado"));
        assertEquals("Unidad 2 Redes", fila.get("temaEsperado"));
        assertEquals(LocalDate.of(ANIO, 6, 10), fila.get("fechaClase"));
        assertEquals("1", scalar("SELECT COUNT(*) FROM notificacion WHERE usuario_id = " + profesorId + " AND tipo = 'PLAN_RETROACTIVO_INCONGRUENCIAS'"));

        // una incongruencia pendiente sola no bloquea "Iniciar clase"
        assertFalse(incumplimientoDao.existeBloqueoActivo(asignacionId));

        // volver a correrlo no duplica nada
        assertEquals(0, retroactivoService().reprocesarClasesPrevias(planId, asignacionId, "1", ANIO, profesorId));
        assertEquals("1", scalar("SELECT COUNT(*) FROM incumplimiento_revision WHERE asignacion_id = " + asignacionId));
    }

    @Test
    void elProfesorJustificaSoloLasSuyasYMientrasEstenPendientes() throws Exception {
        int clase = clase("Repaso", ANIO + "-06-10");
        int id = incumplimientoDao.registrarIncongruenciaRetroactiva(asignacionId, profesorId, null, clase, "desc");

        assertFalse(incumplimientoDao.justificar(id, evaluadorId, "no es mía"));
        assertTrue(incumplimientoDao.justificar(id, profesorId, "Estaba de licencia"));
        assertEquals("Estaba de licencia", incumplimientoDao.listarIncongruenciasPendientesPorUsuario(profesorId).get(0).get("justificacionProfesor"));
        assertEquals("Estaba de licencia", incumplimientoDao.listarPendientes().get(0).get("justificacionProfesor"));

        incumplimientoDao.resolver(id, "PERMITIDO", evaluadorId);
        assertFalse(incumplimientoDao.justificar(id, profesorId, "tarde"));
        assertTrue(incumplimientoDao.listarIncongruenciasPendientesPorUsuario(profesorId).isEmpty());
    }

    @Test
    void tresRechazosBloqueanIniciarClaseHastaQueEvaluacionReactiveConNota() throws Exception {
        EvaluacionCatalogController controller = controller();
        int[] ids = new int[6];
        for (int i = 0; i < ids.length; i++) {
            int clase = clase("Repaso " + i, ANIO + "-06-1" + i);
            ids[i] = incumplimientoDao.registrarIncongruenciaRetroactiva(asignacionId, profesorId, null, clase, "desc " + i);
        }
        assertFalse(incumplimientoDao.existeBloqueoActivo(asignacionId), "seis pendientes solas no bloquean");

        controller.resolverIncumplimiento(ids[0], Map.of("estado", "PERMITIDO"), comoEvaluador());
        controller.resolverIncumplimiento(ids[1], Map.of("estado", "RECHAZADO"), comoEvaluador());
        controller.resolverIncumplimiento(ids[2], Map.of("estado", "RECHAZADO"), comoEvaluador());
        assertFalse(incumplimientoDao.existeBloqueoActivo(asignacionId), "dos rechazos + un permitido: sigue habilitado");

        Map<String, Object> tercero = controller.resolverIncumplimiento(ids[3], Map.of("estado", "RECHAZADO"), comoEvaluador());
        assertEquals(true, tercero.get("bloqueoGenerado"));
        assertTrue(incumplimientoDao.existeBloqueoActivo(asignacionId), "tercer rechazo: bloqueado, sin fecha de vencimiento");
        assertEquals("1", scalar("SELECT COUNT(*) FROM notificacion WHERE usuario_id = " + profesorId + " AND tipo = 'INICIAR_CLASE_BLOQUEADO'"));

        // un cuarto rechazo mientras sigue el bloqueo no crea otra fila
        controller.resolverIncumplimiento(ids[4], Map.of("estado", "RECHAZADO"), comoEvaluador());
        assertEquals("1", scalar("SELECT COUNT(*) FROM incumplimiento_revision WHERE tipo = 'BLOQUEO_INCONGRUENCIA_RETROACTIVA' AND asignacion_id = " + asignacionId));

        // reactivación con nota
        int bloqueoId = Integer.parseInt(scalar("SELECT id FROM incumplimiento_revision WHERE tipo = 'BLOQUEO_INCONGRUENCIA_RETROACTIVA' AND asignacion_id = " + asignacionId));
        controller.resolverIncumplimiento(bloqueoId, Map.of("estado", "PERMITIDO", "nota", "Reunión con coordinación: plan ajustado"), comoEvaluador());
        assertFalse(incumplimientoDao.existeBloqueoActivo(asignacionId), "reactivado: Iniciar clase vuelve a funcionar");
        assertEquals("Reunión con coordinación: plan ajustado", scalar("SELECT nota_resolucion FROM incumplimiento_revision WHERE id = " + bloqueoId));
        assertEquals("1", scalar("SELECT COUNT(*) FROM notificacion WHERE usuario_id = " + profesorId
                + " AND tipo = 'BLOQUEO_RETROACTIVO_LEVANTADO' AND cuerpo LIKE '%Reunión con coordinación: plan ajustado%'"));

        // Los rechazos anteriores a la reactivación no cuentan: el próximo rechazo NO vuelve a bloquear.
        exec("UPDATE incumplimiento_revision SET fecha_resolucion = NOW() - INTERVAL 2 HOUR WHERE tipo = 'INCONGRUENCIA_RETROACTIVA' AND estado = 'RECHAZADO' AND asignacion_id = " + asignacionId);
        exec("UPDATE incumplimiento_revision SET fecha_resolucion = NOW() - INTERVAL 1 HOUR WHERE id = " + bloqueoId);
        Map<String, Object> quinto = controller.resolverIncumplimiento(ids[5], Map.of("estado", "RECHAZADO"), comoEvaluador());
        assertEquals(false, quinto.get("bloqueoGenerado"));
        assertEquals(1, incumplimientoDao.contarRechazadosPorTipo(asignacionId, profesorId, "INCONGRUENCIA_RETROACTIVA"));
        assertFalse(incumplimientoDao.existeBloqueoActivo(asignacionId));
    }

    // ---- Bloque 5 -------------------------------------------------------------------------------------

    @Test
    void cadaAtrasoSeRevisaPorSeparadoYSoloLaTerceraFaltaBloqueaHastaLaReactivacionConNota() throws Exception {
        EvaluacionCatalogController controller = controller();
        int[] ids = new int[6];
        for (int i = 0; i < ids.length; i++) {
            int clase = clase("Clase atrasada " + i, ANIO + "-06-1" + i);
            ids[i] = incumplimientoDao.registrarAtraso(asignacionId, profesorId, null, clase, "Justificación " + i);
        }
        assertFalse(incumplimientoDao.existeBloqueoActivo(asignacionId), "seis atrasos pendientes solos no bloquean");
        assertEquals("Justificación 0", scalar("SELECT justificacion_profesor FROM incumplimiento_revision WHERE id = " + ids[0]));
        assertEquals(6, incumplimientoDao.listarPendientes().stream().filter(f -> "ATRASO".equals(f.get("tipo"))).count());

        controller.resolverIncumplimiento(ids[0], Map.of("estado", "PERMITIDO"), comoEvaluador());
        controller.resolverIncumplimiento(ids[1], Map.of("estado", "RECHAZADO"), comoEvaluador());
        controller.resolverIncumplimiento(ids[2], Map.of("estado", "RECHAZADO"), comoEvaluador());
        assertFalse(incumplimientoDao.existeBloqueoActivo(asignacionId), "dos rechazos + un permitido: sigue habilitado");

        Map<String, Object> tercero = controller.resolverIncumplimiento(ids[3], Map.of("estado", "RECHAZADO"), comoEvaluador());
        assertEquals(true, tercero.get("bloqueoGenerado"));
        assertTrue(incumplimientoDao.existeBloqueoActivo(asignacionId), "tercer rechazo: bloqueado, sin fecha de vencimiento");
        assertEquals("1", scalar("SELECT COUNT(*) FROM notificacion WHERE usuario_id = " + profesorId + " AND tipo = 'INICIAR_CLASE_BLOQUEADO'"));

        // los rechazos de atraso no mezclan con los de incongruencias retroactivas: cada tipo cuenta el suyo
        assertEquals(3, incumplimientoDao.contarRechazadosPorTipo(asignacionId, profesorId, "ATRASO"));
        assertEquals(0, incumplimientoDao.contarRechazadosPorTipo(asignacionId, profesorId, "INCONGRUENCIA_RETROACTIVA"));

        controller.resolverIncumplimiento(ids[4], Map.of("estado", "RECHAZADO"), comoEvaluador());
        assertEquals("1", scalar("SELECT COUNT(*) FROM incumplimiento_revision WHERE tipo = 'BLOQUEO_ATRASO_TIEMPO_REAL' AND asignacion_id = " + asignacionId));

        int bloqueoId = Integer.parseInt(scalar("SELECT id FROM incumplimiento_revision WHERE tipo = 'BLOQUEO_ATRASO_TIEMPO_REAL' AND asignacion_id = " + asignacionId));
        controller.resolverIncumplimiento(bloqueoId, Map.of("estado", "PERMITIDO", "nota", "Regularizó las clases atrasadas"), comoEvaluador());
        assertFalse(incumplimientoDao.existeBloqueoActivo(asignacionId), "reactivado: Iniciar clase vuelve a funcionar");
        assertEquals("Regularizó las clases atrasadas", scalar("SELECT nota_resolucion FROM incumplimiento_revision WHERE id = " + bloqueoId));
        assertEquals("1", scalar("SELECT COUNT(*) FROM notificacion WHERE usuario_id = " + profesorId
                + " AND tipo = 'BLOQUEO_RETROACTIVO_LEVANTADO' AND cuerpo LIKE '%Regularizó las clases atrasadas%'"));

        // tras la reactivación la tolerancia arranca de cero
        exec("UPDATE incumplimiento_revision SET fecha_resolucion = NOW() - INTERVAL 2 HOUR WHERE tipo = 'ATRASO' AND estado = 'RECHAZADO' AND asignacion_id = " + asignacionId);
        exec("UPDATE incumplimiento_revision SET fecha_resolucion = NOW() - INTERVAL 1 HOUR WHERE id = " + bloqueoId);
        Map<String, Object> quinto = controller.resolverIncumplimiento(ids[5], Map.of("estado", "RECHAZADO"), comoEvaluador());
        assertEquals(false, quinto.get("bloqueoGenerado"));
        assertEquals(1, incumplimientoDao.contarRechazadosPorTipo(asignacionId, profesorId, "ATRASO"));
        assertFalse(incumplimientoDao.existeBloqueoActivo(asignacionId));
    }

    @Test
    void unAtrasoRechazadoConSuspensionDeAntesDeV030SigueBloqueandoHastaQueVence() throws Exception {
        // Filas viejas: ATRASO rechazado con rango de fechas. Ya no se generan, pero las existentes se respetan.
        int id = incumplimientoDao.registrar(asignacionId, profesorId, null, "ATRASO", "atraso viejo", "RECHAZADO", evaluadorId,
                java.time.LocalDateTime.now().minusHours(1), java.time.LocalDateTime.now().plusDays(1));
        assertTrue(incumplimientoDao.existeBloqueoActivo(asignacionId));
        exec("UPDATE incumplimiento_revision SET suspension_hasta = NOW() - INTERVAL 1 MINUTE WHERE id = " + id);
        assertFalse(incumplimientoDao.existeBloqueoActivo(asignacionId));
    }

    // ---- Bloque 2 -------------------------------------------------------------------------------------

    @Test
    void variasConsultasConcurrentesUsanElPoolSinErrores() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(30);
        try {
            List<Future<Long>> resultados = new ArrayList<>();
            for (int i = 0; i < 300; i++) {
                resultados.add(pool.submit(() -> new NotificacionDao().contarNoLeidas(profesorId, "profesor")));
            }
            for (Future<Long> f : resultados) {
                assertEquals(0L, f.get());
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private String estadoClase(int id) throws SQLException {
        return scalar("SELECT estado_verificacion_tema FROM planilla_rasgo WHERE id = " + id);
    }
}
