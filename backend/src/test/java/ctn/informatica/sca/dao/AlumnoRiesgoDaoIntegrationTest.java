package ctn.informatica.sca.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ctn.informatica.sca.clases.conexion;
import ctn.informatica.sca.dto.AlumnoRiesgoDto;
import ctn.informatica.sca.model.Alumno;
import ctn.informatica.sca.model.Tarea;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Contra una base MySQL/MariaDB real (mismo esquema que {@code database/db-tables-properties.sql}).
 * Sólo corre con {@code SCA_IT_DB=1} (y CTN_DB_* apuntando a una base descartable): inserta y borra
 * filas propias, pero no debe apuntar nunca a la base de producción.
 */
class AlumnoRiesgoDaoIntegrationTest {

    private static final int ESPECIALIDAD_ID = 9_000_050;
    private static final int ANIO = ctn.informatica.sca.util.AcademicPeriod.current();

    private final AlumnoRiesgoDao dao = new AlumnoRiesgoDao();
    private final PlanillaDao planillaDao = new PlanillaDao();
    private final TareaDao tareaDao = new TareaDao();
    private final RegistroDao registroDao = new RegistroDao();
    private final GradeDao gradeDao = new GradeDao();
    private final RasgoPlanillaDao rasgoPlanillaDao = new RasgoPlanillaDao();
    private final CodigoConductaDao codigoConductaDao = new CodigoConductaDao();
    private final ConfiguracionSistemaDao configDao = new ConfiguracionSistemaDao();

    private int profesorId;
    private int cursoId;
    private int materiaId;
    private int instrumentoId;
    private int alumnoId;
    private int planillaId;
    private int registroId;
    private Alumno alumno;

    @BeforeEach
    void seed() throws Exception {
        assumeTrue("1".equals(System.getenv("SCA_IT_DB")), "Definí SCA_IT_DB=1 (con CTN_DB_* de una base descartable) para correr esto");
        try (Connection c = new conexion().getCon()) {
            // ok
        } catch (SQLException ex) {
            assumeTrue(false, "Sin base de integración: " + ex.getMessage());
        }
        limpiar();
        exec("INSERT INTO especialidad (id, nombre) VALUES (" + ESPECIALIDAD_ID + ", 'IT Especialidad Riesgo')");
        profesorId = insertar("INSERT INTO usuario (usuario, nombre, apellido, contrasenia, nivel) VALUES ('it_riesgo_prof', 'Rita', 'Esgo', 'x', 1)");
        cursoId = insertar("INSERT INTO curso (especialidad_id, promocion, seccion) VALUES (" + ESPECIALIDAD_ID + ", " + (ANIO + 2) + ", 'A')");
        materiaId = insertar("INSERT INTO materia (nombre, categoria) VALUES ('IT Materia Riesgo', 'comun')");
        instrumentoId = insertar("INSERT INTO instrumento (nombre) VALUES ('IT Instrumento Riesgo')");
        alumnoId = insertar("INSERT INTO alumno (nombre, apellido, curso_id) VALUES ('Ana', 'EnRiesgo', " + cursoId + ")");
        alumno = new Alumno();
        alumno.setId(alumnoId);
        alumno.setNombre("Ana");
        alumno.setApellido("EnRiesgo");

        planillaId = planillaDao.crear(cursoId, materiaId, 1, profesorId).getId();
        registroDao.ensureRegistroRowsForPlanilla(planillaId, cursoId);
        registroId = registroDao.getRegistroIdsForPlanilla(planillaId, Set.of(alumnoId)).get(alumnoId);
    }

    @AfterEach
    void limpiar() throws Exception {
        if (!"1".equals(System.getenv("SCA_IT_DB"))) {
            return;
        }
        try {
            exec("DELETE FROM puntaje WHERE tarea_id IN (SELECT id FROM tarea WHERE planilla_id IN (SELECT id FROM planilla WHERE usuario_id IN (SELECT id FROM usuario WHERE usuario LIKE 'it_riesgo_%')))");
            exec("DELETE FROM tarea WHERE planilla_id IN (SELECT id FROM planilla WHERE usuario_id IN (SELECT id FROM usuario WHERE usuario LIKE 'it_riesgo_%'))");
            exec("DELETE FROM registro WHERE planilla_id IN (SELECT id FROM planilla WHERE usuario_id IN (SELECT id FROM usuario WHERE usuario LIKE 'it_riesgo_%'))");
            exec("DELETE FROM planilla WHERE usuario_id IN (SELECT id FROM usuario WHERE usuario LIKE 'it_riesgo_%')");
            exec("DELETE FROM rasgo_asistencia_codigo WHERE rasgo_asistencia_id IN (SELECT id FROM rasgo_asistencia WHERE planilla_rasgo_id IN (SELECT id FROM planilla_rasgo WHERE usuario_id IN (SELECT id FROM usuario WHERE usuario LIKE 'it_riesgo_%')))");
            exec("DELETE FROM rasgo_asistencia WHERE planilla_rasgo_id IN (SELECT id FROM planilla_rasgo WHERE usuario_id IN (SELECT id FROM usuario WHERE usuario LIKE 'it_riesgo_%'))");
            exec("DELETE FROM planilla_rasgo WHERE usuario_id IN (SELECT id FROM usuario WHERE usuario LIKE 'it_riesgo_%')");
            exec("DELETE FROM codigo_conducta WHERE codigo LIKE 'NRIESGO%'");
            exec("DELETE FROM configuracion_sistema WHERE clave LIKE 'riesgo.%'");
            exec("DELETE FROM alumno WHERE curso_id IN (SELECT id FROM curso WHERE especialidad_id = " + ESPECIALIDAD_ID + ")");
            exec("DELETE FROM usuario WHERE usuario LIKE 'it_riesgo_%'");
            exec("DELETE FROM curso WHERE especialidad_id = " + ESPECIALIDAD_ID);
            exec("DELETE FROM materia WHERE nombre = 'IT Materia Riesgo'");
            exec("DELETE FROM instrumento WHERE nombre = 'IT Instrumento Riesgo'");
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

    /** Tarea con fecha_limite vencida (por default, dentro del año lectivo actual): cuenta como no entregada salvo excepción. */
    private int tareaVencida(String titulo, LocalDate fechaLimite, String googleCourseworkId) throws SQLException {
        Tarea t = new Tarea();
        t.setPlanillaId(planillaId);
        t.setInstrumentoId(instrumentoId);
        t.setFecha(LocalDate.now().minusDays(60));
        t.setTotal(10);
        t.setTitulo(titulo);
        t.setFechaLimite(fechaLimite);
        t.setGoogleCourseworkId(googleCourseworkId);
        tareaDao.insertarTarea(t);
        return t.getId();
    }

    private void puntuar(int tareaId, Integer puntos) throws SQLException {
        Map<Integer, Integer> tareaPuntos = new HashMap<>();
        tareaPuntos.put(tareaId, puntos);
        gradeDao.saveGradesBatch(planillaId, Map.of(registroId, tareaPuntos));
    }

    private Optional<AlumnoRiesgoDto> buscar(List<AlumnoRiesgoDto> lista, int alumnoId) {
        return lista.stream().filter(a -> a.alumnoId() == alumnoId).findFirst();
    }

    @Test
    void cincoTareasNoEntregadasNoAlcanzanElUmbralYSeisSi() throws Exception {
        LocalDate vencida = LocalDate.now().minusDays(10);
        for (int i = 0; i < 5; i++) {
            tareaVencida("Tarea " + i, vencida, null);
        }

        List<AlumnoRiesgoDto> resultado = dao.listarPorProfesor(profesorId, ANIO, 5, 5);
        assertTrue(buscar(resultado, alumnoId).isEmpty(), "5 tareas no entregadas no supera el umbral (>5)");

        tareaVencida("Tarea 6", vencida, null);

        resultado = dao.listarPorProfesor(profesorId, ANIO, 5, 5);
        AlumnoRiesgoDto riesgo = buscar(resultado, alumnoId).orElseThrow();
        assertEquals(6, riesgo.tareasNoEntregadas());
        assertTrue(riesgo.motivos().contains("TAREAS"));
        assertEquals("EnRiesgo, Ana", riesgo.nombreCompleto());
    }

    @Test
    void tareasQueNoDebenContarNoInflanElConteo() throws Exception {
        LocalDate vencida = LocalDate.now().minusDays(10);
        // 5 realmente no entregadas: por debajo del umbral.
        for (int i = 0; i < 5; i++) {
            tareaVencida("Vencida " + i, vencida, null);
        }
        // Trampas: cada una NO debería sumar al conteo.
        int conNotaCero = tareaVencida("Con nota 0", vencida, null);
        puntuar(conNotaCero, 0);

        int classroomPendiente = tareaVencida("Classroom pendiente", vencida, "gc-123");
        puntuar(classroomPendiente, null);

        tareaVencida("Sin fecha límite", null, null);

        tareaVencida("Del año anterior", LocalDate.of(ANIO - 1, 3, 1), null);

        List<AlumnoRiesgoDto> resultado = dao.listarPorProfesor(profesorId, ANIO, 5, 5);
        assertTrue(buscar(resultado, alumnoId).isEmpty(), "las 4 tareas trampa no deben sumar al conteo de no entregadas");
    }

    @Test
    void cincoNotasConductualesNoAlcanzanElUmbralYSeisSi() throws Exception {
        for (int i = 1; i <= 6; i++) {
            codigoConductaDao.crear("NRIESGO" + i, "Código de riesgo " + i);
        }

        rasgoPlanillaDao.crearPlanillaRasgo(cursoId, profesorId, "Clase 1", List.of(alumno), Set.of(),
                Map.of(alumnoId, List.of("NRIESGO1", "NRIESGO2", "NRIESGO3", "NRIESGO4", "NRIESGO5")));

        List<AlumnoRiesgoDto> resultado = dao.listarPorProfesor(profesorId, ANIO, 5, 5);
        assertTrue(buscar(resultado, alumnoId).isEmpty(), "5 notas conductuales no superan el umbral (>5)");

        rasgoPlanillaDao.crearPlanillaRasgo(cursoId, profesorId, "Clase 2", List.of(alumno), Set.of(),
                Map.of(alumnoId, List.of("NRIESGO6")));

        resultado = dao.listarPorProfesor(profesorId, ANIO, 5, 5);
        AlumnoRiesgoDto riesgo = buscar(resultado, alumnoId).orElseThrow();
        assertEquals(6, riesgo.notasConductuales());
        assertTrue(riesgo.motivos().contains("CONDUCTA"));
    }

    @Test
    void unAlumnoDeOtroCursoDeOtroProfesorNoAparece() throws Exception {
        int otroProfesorId = insertar("INSERT INTO usuario (usuario, nombre, apellido, contrasenia, nivel) VALUES ('it_riesgo_prof2', 'Otro', 'Profesor', 'x', 1)");
        int otroCursoId = insertar("INSERT INTO curso (especialidad_id, promocion, seccion) VALUES (" + ESPECIALIDAD_ID + ", " + (ANIO + 2) + ", 'B')");
        int otroAlumnoId = insertar("INSERT INTO alumno (nombre, apellido, curso_id) VALUES ('Beto', 'DeOtroCurso', " + otroCursoId + ")");
        int otraPlanillaId = planillaDao.crear(otroCursoId, materiaId, 1, otroProfesorId).getId();
        registroDao.ensureRegistroRowsForPlanilla(otraPlanillaId, otroCursoId);

        LocalDate vencida = LocalDate.now().minusDays(10);
        for (int i = 0; i < 6; i++) {
            Tarea t = new Tarea();
            t.setPlanillaId(otraPlanillaId);
            t.setInstrumentoId(instrumentoId);
            t.setFecha(vencida.minusDays(7));
            t.setTotal(10);
            t.setTitulo("Otro curso " + i);
            t.setFechaLimite(vencida);
            tareaDao.insertarTarea(t);
        }

        List<AlumnoRiesgoDto> resultado = dao.listarPorProfesor(profesorId, ANIO, 5, 5);
        assertTrue(buscar(resultado, otroAlumnoId).isEmpty(), "un alumno de un curso ajeno a este profesor no debe aparecer");
    }

    @Test
    void elUmbralSeRespetaComoParametroDelDao() throws Exception {
        exec("INSERT INTO configuracion_sistema (clave, valor) VALUES ('riesgo.umbral_tareas', '3')");
        assertEquals(3, configDao.getInt("riesgo.umbral_tareas", 5));

        LocalDate vencida = LocalDate.now().minusDays(10);
        for (int i = 0; i < 4; i++) {
            tareaVencida("Tarea " + i, vencida, null);
        }

        assertTrue(buscar(dao.listarPorProfesor(profesorId, ANIO, 5, 5), alumnoId).isEmpty(), "4 tareas no superan el umbral por defecto (>5)");
        assertTrue(buscar(dao.listarPorProfesor(profesorId, ANIO, 3, 5), alumnoId).isPresent(), "4 tareas superan un umbral configurado más bajo (>3)");
    }
}
