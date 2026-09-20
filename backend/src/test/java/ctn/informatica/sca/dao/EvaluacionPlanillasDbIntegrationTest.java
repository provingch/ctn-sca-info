package ctn.informatica.sca.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ctn.informatica.sca.clases.conexion;
import ctn.informatica.sca.controller.EvaluacionPlanillaController;
import ctn.informatica.sca.controller.EvaluacionPlanillaController.PlanillaResumenDto;
import ctn.informatica.sca.controller.PlanillaController;
import ctn.informatica.sca.controller.PlanillaController.PlanillaDetailResponse;
import ctn.informatica.sca.service.ActivityLogService;
import ctn.informatica.sca.service.PlanillaReaperturaService;
import ctn.informatica.sca.service.PlanillaReaperturaService.ReaperturaRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/**
 * Vista de evaluación contra una base real. Sólo corre con {@code SCA_IT_DB=1} y CTN_DB_* apuntando a una base
 * descartable: crea y borra sus propios datos.
 */
class EvaluacionPlanillasDbIntegrationTest {

    private static final int ESPECIALIDAD_ID = 9971;
    private static final int ANIO = 2026;

    private final PlanillaDao planillaDao = new PlanillaDao();
    private final ActivityLogService activityLog = mock(ActivityLogService.class);

    private int profesorId;
    private int evaluadorId;
    private int cursoId;
    private int planillaId;

    @BeforeEach
    void seed() throws Exception {
        assumeTrue("1".equals(System.getenv("SCA_IT_DB")), "Definí SCA_IT_DB=1 (con CTN_DB_* de una base descartable) para correr esto");
        try (Connection c = new conexion().getCon()) {
            // ok
        } catch (SQLException ex) {
            assumeTrue(false, "Sin base de integración: " + ex.getMessage());
        }
        limpiar();
        exec("INSERT INTO especialidad (id, nombre) VALUES (" + ESPECIALIDAD_ID + ", 'IT Especialidad Eval')");
        profesorId = insertar("INSERT INTO usuario (usuario, nombre, apellido, contrasenia, nivel) VALUES ('it_ev_prof', 'Marcos', 'Benitez', 'x', 1)");
        evaluadorId = insertar("INSERT INTO usuario (usuario, nombre, apellido, contrasenia, nivel) VALUES ('it_ev_eval', 'Eva', 'Luador', 'x', 2)");
        cursoId = insertar("INSERT INTO curso (especialidad_id, promocion, seccion) VALUES (" + ESPECIALIDAD_ID + ", 2028, 'A')");
        int materiaId = insertar("INSERT INTO materia (nombre, categoria) VALUES ('IT Materia Eval', 'comun')");
        insertar("INSERT INTO alumno (nombre, apellido, curso_id) VALUES ('Ana', 'Uno', " + cursoId + ")");
        insertar("INSERT INTO alumno (nombre, apellido, curso_id) VALUES ('Beto', 'Dos', " + cursoId + ")");
        planillaId = insertar("INSERT INTO planilla (curso_id, materia_id, periodo, etapa, usuario_id, fecha_cierre_etapa1, etapa1_confirmada) VALUES ("
                + cursoId + ", " + materiaId + ", " + ANIO + ", 'primera', " + profesorId + ", '2026-06-30', TRUE)");
    }

    @AfterEach
    void limpiar() throws Exception {
        if (!"1".equals(System.getenv("SCA_IT_DB"))) {
            return;
        }
        try {
            exec("DELETE FROM notificacion WHERE usuario_id IN (SELECT id FROM usuario WHERE usuario IN ('it_ev_prof','it_ev_eval'))");
            exec("DELETE FROM registro WHERE planilla_id IN (SELECT id FROM planilla WHERE usuario_id IN (SELECT id FROM usuario WHERE usuario IN ('it_ev_prof','it_ev_eval')))");
            exec("DELETE FROM planilla WHERE usuario_id IN (SELECT id FROM usuario WHERE usuario IN ('it_ev_prof','it_ev_eval'))");
            exec("DELETE FROM alumno WHERE curso_id IN (SELECT id FROM curso WHERE especialidad_id = " + ESPECIALIDAD_ID + ")");
            exec("DELETE FROM usuario WHERE usuario IN ('it_ev_prof','it_ev_eval')");
            exec("DELETE FROM curso WHERE especialidad_id = " + ESPECIALIDAD_ID);
            exec("DELETE FROM materia WHERE nombre = 'IT Materia Eval'");
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

    private EvaluacionPlanillaController controller() {
        PlanillaReaperturaService reapertura = new PlanillaReaperturaService(planillaDao, new NotificacionDao(), new UserDao(), activityLog);
        return new EvaluacionPlanillaController(planillaDao, new CursoDao(), new EspecialidadDao(), new ProfesorDao(), reapertura);
    }

    private UsernamePasswordAuthenticationToken comoEvaluador() {
        return new UsernamePasswordAuthenticationToken((long) evaluadorId, null, List.of());
    }

    @Test
    void listaLasPlanillasDelCursoConProfesorYEstadoDeCierre() {
        List<PlanillaResumenDto> primera = controller().listarPlanillas(cursoId, "primera", ANIO, null, comoEvaluador());
        List<PlanillaResumenDto> segunda = controller().listarPlanillas(cursoId, "segunda", ANIO, null, comoEvaluador());

        assertEquals(1, primera.size());
        PlanillaResumenDto dto = primera.get(0);
        assertEquals(planillaId, dto.id());
        assertEquals("IT Materia Eval", dto.materiaNombre());
        assertEquals(profesorId, dto.profesorId());
        assertEquals("Marcos Benitez", dto.profesorNombre());
        assertTrue(dto.etapa1Confirmada());
        assertEquals("2026-06-30", String.valueOf(dto.fechaCierreEtapa1()));
        assertTrue(segunda.isEmpty(), "no hay planilla de segunda etapa");
    }

    @Test
    void elDetalleSeVeSinSerElDuenoYSinEscribirEnLaBase() throws Exception {
        assertEquals("0", scalar("SELECT COUNT(*) FROM registro WHERE planilla_id = " + planillaId));

        PlanillaDetailResponse detalle = controller().verPlanilla(planillaId, comoEvaluador());

        assertEquals(planillaId, detalle.planilla().id());
        assertEquals("IT Materia Eval", detalle.planilla().materiaNombre());
        assertEquals(profesorId, detalle.planilla().profesorId());
        assertTrue(detalle.planilla().etapa1Confirmada());
        assertEquals("0", scalar("SELECT COUNT(*) FROM registro WHERE planilla_id = " + planillaId),
                "la vista de evaluación es de sólo lectura: no crea filas de registro");
    }

    @Test
    void elProfesorAlAbrirSuPlanillaSiCreaLasFilasDeRegistroDeSusAlumnos() throws Exception {
        PlanillaController profesorController = new PlanillaController(planillaDao, new ProfesorDao(), null);

        PlanillaDetailResponse detalle = profesorController.getById(planillaId,
                new UsernamePasswordAuthenticationToken((long) profesorId, null, List.of()));

        assertEquals(2, detalle.rows().size());
        assertEquals("2", scalar("SELECT COUNT(*) FROM registro WHERE planilla_id = " + planillaId));
    }

    @Test
    void reabrirConMotivoDejaLaEtapaAbiertaElLogConElMotivoYAvisaAlProfesor() throws Exception {
        controller().reabrirEtapa1(planillaId, new ReaperturaRequest("El profesor pidió corregir una nota"), comoEvaluador());

        assertEquals("0", scalar("SELECT etapa1_confirmada FROM planilla WHERE id = " + planillaId));
        verify(activityLog).registrar(evaluadorId, "Reabrió Etapa 1 de la planilla " + planillaId + " — motivo: El profesor pidió corregir una nota");
        assertEquals("1", scalar("SELECT COUNT(*) FROM notificacion WHERE usuario_id = " + profesorId
                + " AND tipo = 'PLANILLA_REABIERTA' AND entidad_tipo = 'PLANILLA' AND entidad_id = " + planillaId
                + " AND cuerpo LIKE '%El profesor pidió corregir una nota%'"));
        assertEquals("profesor", scalar("SELECT user_type FROM notificacion WHERE usuario_id = " + profesorId + " AND tipo = 'PLANILLA_REABIERTA'"));
    }

    @Test
    void reabrirSinMotivoNoCambiaNada() throws Exception {
        org.springframework.web.server.ResponseStatusException ex = org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> controller().reabrirEtapa1(planillaId, new ReaperturaRequest("  "), comoEvaluador()));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("1", scalar("SELECT etapa1_confirmada FROM planilla WHERE id = " + planillaId));
        assertEquals("0", scalar("SELECT COUNT(*) FROM notificacion WHERE usuario_id = " + profesorId + " AND tipo = 'PLANILLA_REABIERTA'"));
    }
}
