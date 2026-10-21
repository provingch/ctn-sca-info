package ctn.informatica.sca.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ctn.informatica.sca.clases.conexion;
import ctn.informatica.sca.model.ParentSummaryItem;
import ctn.informatica.sca.model.Planilla;
import ctn.informatica.sca.model.StudentRow;
import ctn.informatica.sca.model.Tarea;
import ctn.informatica.sca.util.AcademicPeriod;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Traslado de un alumno a otro curso y borrado completo, contra una base MySQL/MariaDB real (esquema de
 * {@code db-tables-properties.sql} más las migraciones del backend). Sólo corre con {@code SCA_IT_DB=1}
 * (y CTN_DB_* apuntando a una base descartable): inserta y borra filas propias.
 */
class AlumnoTrasladoBorradoDbIntegrationTest {

    private static final int ESPECIALIDAD_ORIGEN = 9_000_060;
    private static final int ESPECIALIDAD_DESTINO = 9_000_061;
    private static final int ANIO = AcademicPeriod.current();

    private final AlumnoDao alumnoDao = new AlumnoDao();
    private final PlanillaDao planillaDao = new PlanillaDao();
    private final RegistroDao registroDao = new RegistroDao();

    private int profesorId;
    private int padreId;
    private int cursoOrigen;
    private int cursoDestino;
    private int materiaId;
    private int instrumentoId;
    private int alumnoId;
    private int companeroId;
    private int planillaOrigen;
    private int tareaId;

    @BeforeEach
    void seed() throws Exception {
        assumeTrue("1".equals(System.getenv("SCA_IT_DB")), "Definí SCA_IT_DB=1 (con CTN_DB_* de una base descartable) para correr esto");
        try (Connection c = new conexion().getCon()) {
            // ok
        } catch (SQLException ex) {
            assumeTrue(false, "Sin base de integración: " + ex.getMessage());
        }
        limpiar();
        exec("INSERT INTO especialidad (id, nombre) VALUES (" + ESPECIALIDAD_ORIGEN + ", 'IT Origen'), (" + ESPECIALIDAD_DESTINO + ", 'IT Destino')");
        profesorId = insertar("INSERT INTO usuario (usuario, nombre, apellido, contrasenia, nivel) VALUES ('it_traslado_prof', 'Pro', 'Fe', 'x', 1)");
        padreId = insertar("INSERT INTO usuario (usuario, nombre, apellido, contrasenia, nivel) VALUES ('it_traslado_padre', 'Pa', 'Dre', 'x', 4)");
        cursoOrigen = insertar("INSERT INTO curso (especialidad_id, promocion, seccion) VALUES (" + ESPECIALIDAD_ORIGEN + ", " + (ANIO + 1) + ", 'A')");
        cursoDestino = insertar("INSERT INTO curso (especialidad_id, promocion, seccion) VALUES (" + ESPECIALIDAD_DESTINO + ", " + (ANIO + 1) + ", 'A')");
        materiaId = insertar("INSERT INTO materia (nombre, categoria) VALUES ('IT Materia Traslado', 'comun')");
        instrumentoId = ESPECIALIDAD_ORIGEN; // instrumento.id no es AUTO_INCREMENT
        exec("INSERT INTO instrumento (id, nombre) VALUES (" + instrumentoId + ", 'IT Instrumento Traslado')");
        alumnoId = insertar("INSERT INTO alumno (nombre, apellido, curso_id) VALUES ('Tomás', 'Trasladado', " + cursoOrigen + ")");
        companeroId = insertar("INSERT INTO alumno (nombre, apellido, curso_id) VALUES ('Ana', 'Aquí', " + cursoOrigen + ")");
        exec("INSERT INTO alumno_usuario (alumno_id, usuario_id) VALUES (" + alumnoId + ", " + padreId + ")");

        // Abrir la planilla crea registro para todo el curso: es lo que antes trababa el borrado.
        planillaOrigen = planillaDao.crear(cursoOrigen, materiaId, 1, profesorId).getId();
        registroDao.ensureRegistroRowsForPlanilla(planillaOrigen, cursoOrigen);
        Tarea t = new Tarea();
        t.setPlanillaId(planillaOrigen);
        t.setInstrumentoId(instrumentoId);
        t.setFecha(LocalDate.now());
        t.setTotal(10);
        t.setTitulo("Trabajo práctico");
        new TareaDao().insertarTarea(t);
        tareaId = t.getId();
        int registroId = registroDao.getRegistroIdsForPlanilla(planillaOrigen, Set.of(alumnoId)).get(alumnoId);
        new GradeDao().saveGradesBatch(planillaOrigen, Map.of(registroId, Map.of(tareaId, 7)));
    }

    @AfterEach
    void limpiar() throws Exception {
        if (!"1".equals(System.getenv("SCA_IT_DB"))) {
            return;
        }
        try {
            String planillas = "SELECT id FROM planilla WHERE usuario_id IN (SELECT id FROM usuario WHERE usuario LIKE 'it_traslado_%')";
            String alumnos = "SELECT id FROM alumno WHERE curso_id IN (SELECT id FROM curso WHERE especialidad_id IN (" + ESPECIALIDAD_ORIGEN + ", " + ESPECIALIDAD_DESTINO + "))";
            exec("DELETE FROM puntaje WHERE tarea_id IN (SELECT id FROM tarea WHERE planilla_id IN (" + planillas + "))");
            exec("DELETE FROM tarea WHERE planilla_id IN (" + planillas + ")");
            exec("DELETE FROM registro WHERE planilla_id IN (" + planillas + ")");
            exec("DELETE FROM planilla WHERE usuario_id IN (SELECT id FROM usuario WHERE usuario LIKE 'it_traslado_%')");
            exec("DELETE FROM anotacion WHERE alumno_id IN (" + alumnos + ")");
            exec("DELETE FROM tipo_anotacion WHERE descripcion = 'IT Tipo Traslado'");
            exec("DELETE FROM enf_consulta WHERE alumno_id IN (" + alumnos + ")");
            exec("DELETE FROM enf_ficha WHERE alumno_id IN (" + alumnos + ")");
            exec("DELETE FROM alumno_curso_historial WHERE alumno_id IN (" + alumnos + ")");
            exec("DELETE FROM alumno WHERE curso_id IN (SELECT id FROM curso WHERE especialidad_id IN (" + ESPECIALIDAD_ORIGEN + ", " + ESPECIALIDAD_DESTINO + "))");
            exec("DELETE FROM usuario WHERE usuario LIKE 'it_traslado_%'");
            exec("DELETE FROM curso WHERE especialidad_id IN (" + ESPECIALIDAD_ORIGEN + ", " + ESPECIALIDAD_DESTINO + ")");
            exec("DELETE FROM materia WHERE nombre = 'IT Materia Traslado'");
            exec("DELETE FROM instrumento WHERE nombre = 'IT Instrumento Traslado'");
            exec("DELETE FROM especialidad WHERE id IN (" + ESPECIALIDAD_ORIGEN + ", " + ESPECIALIDAD_DESTINO + ")");
        } catch (SQLException ignored) {
            // sin base: el @BeforeEach ya hizo skip
        }
    }

    @Test
    void cambiarDeCursoDejaHistorialYGuardarSinCambioNo() throws Exception {
        assertTrue(alumnoDao.update(alumnoId, "Tomás", "Trasladado", cursoOrigen, null, profesorId));
        assertEquals(0, contar("SELECT COUNT(*) FROM alumno_curso_historial WHERE alumno_id = " + alumnoId));

        assertTrue(alumnoDao.update(alumnoId, "Tomás", "Trasladado", cursoDestino, null, profesorId));
        assertEquals(cursoDestino, alumnoDao.findById(alumnoId).getCursoId());
        assertEquals(1, contar("SELECT COUNT(*) FROM alumno_curso_historial WHERE alumno_id = " + alumnoId
                + " AND curso_anterior_id = " + cursoOrigen + " AND curso_nuevo_id = " + cursoDestino + " AND usuario_id = " + profesorId));
    }

    @Test
    void laPlanillaViejaLoMuestraAlFinalComoTrasladadoYNoAceptaNotas() throws Exception {
        alumnoDao.update(alumnoId, "Tomás", "Trasladado", cursoDestino, null, profesorId);
        Planilla planilla = planillaDao.findById(planillaOrigen);
        planilla.computeGradeRanges(10);

        List<StudentRow> sinTrasladados = new StudentRowDao().loadRowsForPlanilla(planilla, Map.of(tareaId, 10), 10);
        assertEquals(List.of(companeroId), sinTrasladados.stream().map(StudentRow::getAlumnoId).toList());

        List<StudentRow> conTrasladados = new StudentRowDao().loadRowsForPlanilla(planilla, Map.of(tareaId, 10), 10, true);
        assertEquals(List.of(companeroId, alumnoId), conTrasladados.stream().map(StudentRow::getAlumnoId).toList());
        StudentRow trasladado = conTrasladados.get(1);
        assertEquals("IT Destino 2ºA", trasladado.getTrasladadoA());
        assertEquals(7, trasladado.getTotal());
        assertNull(conTrasladados.get(0).getTrasladadoA());

        assertEquals(Set.of(alumnoId), registroDao.findAlumnoIdsTrasladados(planillaOrigen, Set.of(alumnoId, companeroId)));
    }

    @Test
    void elPadreVeLasNotasDelCursoAnteriorAparteConLaFecha() throws Exception {
        int planillaDestino = planillaDao.crear(cursoDestino, materiaId, 1, profesorId).getId();
        int planillaSinNotas = planillaDao.crear(cursoOrigen, materiaId, 2, profesorId).getId();
        registroDao.ensureRegistroRowsForPlanilla(planillaSinNotas, cursoOrigen);
        alumnoDao.update(alumnoId, "Tomás", "Trasladado", cursoDestino, null, profesorId);

        List<ParentSummaryItem> items = new PadreDao().findParentSummary(padreId);
        assertEquals(List.of(planillaDestino, planillaOrigen), items.stream().map(ParentSummaryItem::getPlanillaId).toList(),
                "la del curso nuevo primero; del curso viejo sólo la que tiene notas");
        assertNull(items.get(0).getCursoAnterior());
        ParentSummaryItem anterior = items.get(1);
        assertEquals("IT Origen 2ºA", anterior.getCursoAnterior());
        assertEquals(LocalDate.now(), anterior.getCursoAnteriorHasta());
        assertEquals(7, anterior.getPuntos());
    }

    @Test
    void borrarSeLlevaTodoLoDelAlumnoYNadaDeOtros() throws Exception {
        int tipoId = insertar("INSERT INTO tipo_anotacion (descripcion) VALUES ('IT Tipo Traslado')");
        exec("INSERT INTO anotacion (alumno_id, usuario_id, tipo_id) VALUES (" + alumnoId + ", " + profesorId + ", " + tipoId + ")");
        exec("INSERT INTO enf_ficha (alumno_id) VALUES (" + alumnoId + ")");
        exec("INSERT INTO enf_consulta (motivo, fecha, alumno_id) VALUES ('Dolor de cabeza', CURDATE(), " + alumnoId + ")");
        exec("INSERT INTO notificacion (usuario_id, user_type, tipo, titulo, cuerpo, entidad_tipo, entidad_id) VALUES ("
                + padreId + ", 'padre', 'NOVEDAD_ALUMNO', 't', 'c', 'ALUMNO', " + alumnoId + ")");
        alumnoDao.update(alumnoId, "Tomás", "Trasladado", cursoDestino, null, profesorId);

        AlumnoDao.ResumenAlumno resumen = alumnoDao.resumen(alumnoId);
        assertEquals(1, resumen.notas());
        assertEquals(1, resumen.planillasConNotas());
        assertEquals(0, resumen.planillasConNotasCursoActual(), "la planilla con notas es del curso viejo");
        assertEquals(1, resumen.anotaciones());
        assertTrue(resumen.fichaEnfermeria());
        assertEquals(1, resumen.consultasEnfermeria());

        assertTrue(alumnoDao.delete(alumnoId));

        assertNull(alumnoDao.findById(alumnoId));
        for (String tabla : List.of("registro", "anotacion", "enf_ficha", "enf_consulta", "alumno_usuario", "alumno_curso_historial")) {
            assertEquals(0, contar("SELECT COUNT(*) FROM " + tabla + " WHERE alumno_id = " + alumnoId), tabla);
        }
        assertEquals(0, contar("SELECT COUNT(*) FROM puntaje WHERE tarea_id = " + tareaId));
        assertEquals(0, contar("SELECT COUNT(*) FROM notificacion WHERE entidad_tipo = 'ALUMNO' AND entidad_id = " + alumnoId));
        assertNotNull(alumnoDao.findById(companeroId));
        assertEquals(1, contar("SELECT COUNT(*) FROM registro WHERE alumno_id = " + companeroId));
        assertFalse(alumnoDao.delete(alumnoId), "un alumno que ya no existe no se borra dos veces");
    }

    private static int contar(String sql) throws SQLException {
        try (Connection c = new conexion().getCon(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
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
}
