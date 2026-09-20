package ctn.informatica.sca.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ctn.informatica.sca.clases.conexion;
import ctn.informatica.sca.dto.PlanCurricularDto;
import ctn.informatica.sca.dto.TemaPlanDto;
import ctn.informatica.sca.service.TemaVerificacionService;
import ctn.informatica.sca.service.VerificacionResultado;
import ctn.informatica.sca.web.PlanCurricularController;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Temas sin cumplir de una etapa cerrada y su reconocimiento en la etapa siguiente, contra una base real. Sólo corre
 * con {@code SCA_IT_DB=1} y CTN_DB_* apuntando a una base descartable: crea y borra sus propios datos.
 */
class EtapaAnteriorDbIntegrationTest {

    private static final int ESPECIALIDAD_ID = 9973;
    private static final int ANIO = 2026;

    private final PlanCurricularDao planDao = new PlanCurricularDao();
    private final PlanillaDao planillaDao = new PlanillaDao();

    private int profesorId;
    private int evaluadorId;
    private int asignacionId;
    private int asignacionAjenaId;
    private int cursoId;
    private int materiaId;

    /** Servicio real (SQL real) pero fijado en septiembre de Etapa 2, para no depender del día en que corre. */
    private final TemaVerificacionService servicioEnEtapaDos = new TemaVerificacionService() {
        @Override
        protected int mesActual() {
            return 9;
        }

        @Override
        protected int etapaActual() {
            return 2;
        }

        @Override
        protected int anioActual() {
            return ANIO;
        }
    };

    @BeforeEach
    void seed() throws Exception {
        assumeTrue("1".equals(System.getenv("SCA_IT_DB")), "Definí SCA_IT_DB=1 (con CTN_DB_* de una base descartable) para correr esto");
        try (Connection c = new conexion().getCon()) {
            // ok
        } catch (SQLException ex) {
            assumeTrue(false, "Sin base de integración: " + ex.getMessage());
        }
        limpiar();
        exec("INSERT INTO especialidad (id, nombre) VALUES (" + ESPECIALIDAD_ID + ", 'IT Especialidad Etapa')");
        profesorId = insertar("INSERT INTO usuario (usuario, nombre, apellido, contrasenia, nivel) VALUES ('it_et_prof', 'Pro', 'Fesor', 'x', 1)");
        evaluadorId = insertar("INSERT INTO usuario (usuario, nombre, apellido, contrasenia, nivel) VALUES ('it_et_eval', 'Eva', 'Luador', 'x', 2)");
        int cursoBaseId = insertar("INSERT INTO curso_base (especialidad_id, nivel, seccion) VALUES (" + ESPECIALIDAD_ID + ", 1, 'A')");
        materiaId = insertar("INSERT INTO materia (nombre, categoria) VALUES ('IT Materia Etapa', 'comun')");
        int otraMateriaId = insertar("INSERT INTO materia (nombre, categoria) VALUES ('IT Otra Materia Etapa', 'comun')");
        asignacionId = insertar("INSERT INTO asignacion (usuario_id, materia_id, curso_base_id) VALUES (" + profesorId + ", " + materiaId + ", " + cursoBaseId + ")");
        asignacionAjenaId = insertar("INSERT INTO asignacion (usuario_id, materia_id, curso_base_id) VALUES (" + profesorId + ", " + otraMateriaId + ", " + cursoBaseId + ")");
        // promocion = anio - nivel + 3 => el 1° año de 2026 es la promoción 2028
        cursoId = insertar("INSERT INTO curso (especialidad_id, promocion, seccion) VALUES (" + ESPECIALIDAD_ID + ", 2028, 'A')");
    }

    @AfterEach
    void limpiar() throws Exception {
        SecurityContextHolder.clearContext();
        if (!"1".equals(System.getenv("SCA_IT_DB"))) {
            return;
        }
        try {
            exec("DELETE FROM planilla_rasgo WHERE usuario_id IN (SELECT id FROM usuario WHERE usuario IN ('it_et_prof','it_et_eval'))");
            exec("DELETE FROM planilla WHERE usuario_id IN (SELECT id FROM usuario WHERE usuario IN ('it_et_prof','it_et_eval'))");
            exec("DELETE FROM usuario WHERE usuario IN ('it_et_prof','it_et_eval')"); // cascada: asignaciones y planes
            exec("DELETE FROM curso WHERE especialidad_id = " + ESPECIALIDAD_ID);
            exec("DELETE FROM curso_base WHERE especialidad_id = " + ESPECIALIDAD_ID);
            exec("DELETE FROM materia WHERE nombre IN ('IT Materia Etapa', 'IT Otra Materia Etapa')");
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

    private static TemaPlanDto tema(String mes, int orden, String contenido) {
        TemaPlanDto t = new TemaPlanDto();
        t.mes = mes;
        t.ordenMes = orden;
        t.bloque = 1;
        t.temasContenidos = contenido;
        return t;
    }

    private int planAprobado(String etapa, List<TemaPlanDto> temas) throws SQLException {
        int id = planDao.saveOrReplace(asignacionId, etapa, ANIO, "plan.xlsx", new byte[] { 1 }, temas);
        planDao.aprobar(id, evaluadorId);
        return id;
    }

    private int planilla(String etapa, boolean etapa1Cerrada, boolean etapa2Cerrada) throws SQLException {
        return insertar("INSERT INTO planilla (curso_id, materia_id, periodo, etapa, usuario_id, etapa1_confirmada, etapa2_confirmada) VALUES ("
                + cursoId + ", " + materiaId + ", " + ANIO + ", '" + etapa + "', " + profesorId + ", " + etapa1Cerrada + ", " + etapa2Cerrada + ")");
    }

    private int clase(String tema) throws SQLException {
        return insertar("INSERT INTO planilla_rasgo (curso_id, usuario_id, asignacion_id, tema, fecha_clase) VALUES ("
                + cursoId + ", " + profesorId + ", " + asignacionId + ", '" + tema + "', '2026-09-10')");
    }

    private String estadoCobertura(int planId, String contenido) throws SQLException {
        return scalar("SELECT estado_cobertura FROM tema_plan_curricular WHERE plan_curricular_id = " + planId + " AND temas_contenidos = '" + contenido + "'");
    }

    // ---- existeAlgunaCerrada -----------------------------------------------------------------------------

    @Test
    void unaEtapaEstaCerradaSoloSiUnaPlanillaDeLaAsignacionLaTieneConfirmada() throws Exception {
        assertFalse(planillaDao.existeAlgunaCerrada(asignacionId, 1, ANIO), "sin planillas no hay nada cerrado");

        planilla("primera", false, false);
        assertFalse(planillaDao.existeAlgunaCerrada(asignacionId, 1, ANIO), "planilla abierta");

        exec("UPDATE planilla SET etapa1_confirmada = TRUE WHERE curso_id = " + cursoId);
        assertTrue(planillaDao.existeAlgunaCerrada(asignacionId, 1, ANIO), "etapa 1 confirmada");
        assertFalse(planillaDao.existeAlgunaCerrada(asignacionId, 2, ANIO), "la etapa 2 sigue corriendo");
        assertFalse(planillaDao.existeAlgunaCerrada(asignacionId, 1, ANIO - 1), "otro año lectivo");
        assertFalse(planillaDao.existeAlgunaCerrada(asignacionAjenaId, 1, ANIO), "otra materia del mismo curso no se ve afectada");

        exec("UPDATE planilla SET etapa2_confirmada = TRUE WHERE curso_id = " + cursoId);
        assertTrue(planillaDao.existeAlgunaCerrada(asignacionId, 2, ANIO));
    }

    @Test
    void elPlanDelProfesorYElDelEvaluadorInformanSiLaEtapaCerro() throws Exception {
        int plan1 = planAprobado("1", List.of(tema("Marzo", 1, "Unidad 1 Sistemas")));
        planilla("primera", true, false);
        PlanCurricularController controller = new PlanCurricularController();
        ReflectionTestUtils.setField(controller, "dao", planDao);
        ReflectionTestUtils.setField(controller, "asignacionDao", new AsignacionDao());
        ReflectionTestUtils.setField(controller, "userDao", new UserDao());
        ReflectionTestUtils.setField(controller, "planillaDao", planillaDao);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken((long) profesorId, null, List.of()));

        ResponseEntity<?> miPlan = controller.miPlan(asignacionId, "1", ANIO);
        ResponseEntity<?> detalle = controller.getPlanDetalle(plan1);

        assertTrue(((PlanCurricularDto) miPlan.getBody()).etapaCerrada, "vista del profesor");
        assertTrue(((PlanCurricularDto) detalle.getBody()).etapaCerrada, "vista de detalle (evaluación/seguimiento)");
        planAprobado("2", List.of(tema("Septiembre", 3, "Unidad 5 Bases de datos")));
        assertFalse(((PlanCurricularDto) controller.miPlan(asignacionId, "2", ANIO).getBody()).etapaCerrada, "la etapa 2 sigue corriendo");
    }

    // ---- Reconocer un tema pendiente de la etapa anterior ---------------------------------------------

    @Test
    void unTemaSinCubrirDeEtapaUnoSeReconoceEnEtapaDosYAlCubrirloDejaDeSerPendiente() throws Exception {
        int plan1 = planAprobado("1", List.of(tema("Marzo", 1, "Unidad 1 Sistemas"), tema("Abril", 2, "Unidad 2 Redes")));
        int plan2 = planAprobado("2", List.of(tema("Septiembre", 3, "Unidad 5 Bases de datos")));

        VerificacionResultado retomado = servicioEnEtapaDos.verificar(asignacionId, "Unidad 2 Redes");

        assertEquals("ATRASADO", retomado.estado());
        assertTrue(retomado.atrasado());
        assertTrue(retomado.temaDeEtapaAnterior());
        String temaRedes = scalar("SELECT id FROM tema_plan_curricular WHERE plan_curricular_id = " + plan1 + " AND temas_contenidos = 'Unidad 2 Redes'");
        assertEquals(Integer.valueOf(temaRedes), retomado.temaPlanCurricularId());

        // el Iniciar clase lo da por cumplido en el plan viejo
        planDao.marcarCubierto(retomado.temaPlanCurricularId(), clase("Unidad 2 Redes"));
        assertEquals("CUBIERTO", estadoCobertura(plan1, "Unidad 2 Redes"));
        assertEquals("PENDIENTE", estadoCobertura(plan1, "Unidad 1 Sistemas"), "los demás siguen sin cumplir");
        assertEquals("PENDIENTE", estadoCobertura(plan2, "Unidad 5 Bases de datos"), "el plan de etapa 2 no se toca");

        // ya cubierto, volver a entrarlo no lo reconoce como pendiente de la etapa anterior
        VerificacionResultado otraVez = servicioEnEtapaDos.verificar(asignacionId, "Unidad 2 Redes");
        assertFalse(otraVez.temaDeEtapaAnterior());
        assertEquals("DUDOSO", otraVez.estado());
    }

    @Test
    void recorreTodosLosPendientesDeLaEtapaAnteriorNoSoloElProximo() throws Exception {
        int plan1 = planAprobado("1", List.of(tema("Marzo", 1, "Unidad 1 Sistemas"), tema("Abril", 2, "Unidad 2 Redes"), tema("Mayo", 3, "Unidad 3 Seguridad")));
        planAprobado("2", List.of(tema("Septiembre", 3, "Unidad 5 Bases de datos")));

        // el próximo pendiente de etapa 1 sería "Unidad 1"; se retoma la 3, que está más adelante
        VerificacionResultado resultado = servicioEnEtapaDos.verificar(asignacionId, "Unidad 3 Seguridad");

        assertEquals("ATRASADO", resultado.estado());
        assertTrue(resultado.temaDeEtapaAnterior());
        String temaSeguridad = scalar("SELECT id FROM tema_plan_curricular WHERE plan_curricular_id = " + plan1 + " AND temas_contenidos = 'Unidad 3 Seguridad'");
        assertEquals(Integer.valueOf(temaSeguridad), resultado.temaPlanCurricularId());
        assertNotNull(temaSeguridad);
    }

    @Test
    void unTemaAjenoATodosLosPlanesSigueSiendoDudoso() throws Exception {
        planAprobado("1", List.of(tema("Marzo", 1, "Unidad 1 Sistemas")));
        planAprobado("2", List.of(tema("Septiembre", 3, "Unidad 5 Bases de datos")));

        VerificacionResultado resultado = servicioEnEtapaDos.verificar(asignacionId, "Geografía de Asia");

        assertEquals("DUDOSO", resultado.estado());
        assertFalse(resultado.temaDeEtapaAnterior());
        assertFalse(resultado.atrasado());
    }
}
