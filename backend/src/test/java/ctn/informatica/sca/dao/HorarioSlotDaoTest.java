package ctn.informatica.sca.dao;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HorarioSlotDaoTest {

    @BeforeEach
    public void setUp() throws SQLException {
        Connection testConnection;
        try {
            testConnection = new HorarioSlotDao().getCon();
        } catch (SQLException exception) {
            assumeTrue(false, () -> "Requiere la base MySQL de integración: " + exception.getMessage());
            return;
        }

        try (Connection c = testConnection; PreparedStatement ps = c.prepareStatement(
                "DELETE FROM horario_slot; DELETE FROM asignacion; DELETE FROM materia; DELETE FROM curso; DELETE FROM usuario; DELETE FROM especialidad; DELETE FROM hora_catedra;")) {
            ps.executeUpdate();
        }

        try (Connection c = new HorarioSlotDao().getCon(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO especialidad (id, nombre) VALUES (1, 'Informática')")) {
            ps.executeUpdate();
        }
        try (Connection c = new HorarioSlotDao().getCon(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO curso (id, especialidad_id, promocion, seccion) VALUES (1, 1, 2027, 'A')")) {
            ps.executeUpdate();
        }
        try (Connection c = new HorarioSlotDao().getCon(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO hora_catedra (id, numero, etiqueta, hora_inicio, hora_fin) VALUES (1, 1, 'M', '07:00', '07:35'), (2, 2, 'M', '07:35', '08:10')")) {
            ps.executeUpdate();
        }
    }

    private int createUsuario(String usuario, int nivel) throws SQLException {
        String sql = "INSERT INTO usuario (nombre, apellido, usuario, nivel) VALUES (?, ?, ?, ?)";
        try (Connection c = new HorarioSlotDao().getCon(); PreparedStatement ps = c.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "Profesor");
            ps.setString(2, "Test");
            ps.setString(3, usuario);
            ps.setInt(4, nivel);
            ps.executeUpdate();
            try (java.sql.ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("No se pudo crear usuario de prueba");
    }

    private int createMateria(String nombre) throws SQLException {
        String sql = "INSERT INTO materia (nombre, categoria) VALUES (?, 'comun')";
        try (Connection c = new HorarioSlotDao().getCon(); PreparedStatement ps = c.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.executeUpdate();
            try (java.sql.ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("No se pudo crear materia de prueba");
    }

    private int createAsignacion(int profesorId, int materiaId, int cursoId) throws SQLException {
        String sql = "INSERT INTO asignacion (usuario_id, materia_id, curso_id) VALUES (?, ?, ?)";
        try (Connection c = new HorarioSlotDao().getCon(); PreparedStatement ps = c.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, profesorId);
            ps.setInt(2, materiaId);
            ps.setInt(3, cursoId);
            ps.executeUpdate();
            try (java.sql.ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("No se pudo crear asignación de prueba");
    }

    @Test
    public void crearSlot_mismoProfesorMismoDiaHora_debeFallarPorConstraint() throws SQLException {
        HorarioSlotDao dao = new HorarioSlotDao();
        int profesorId = createUsuario("prof-1", 1);
        int materiaId = createMateria("Matemática");
        int asignacionId = createAsignacion(profesorId, materiaId, 1);

        dao.crear(asignacionId, profesorId, 1, 1, 1, "Aula 1");

        SQLIntegrityConstraintViolationException ex = assertThrows(SQLIntegrityConstraintViolationException.class,
                () -> dao.crear(asignacionId, profesorId, 1, 1, 1, "Aula 2"));
        assertEquals("uq_horario_profesor", ex.getMessage().contains("uq_horario_profesor") ? "uq_horario_profesor" : ex.getMessage());
    }

    @Test
    public void crearSlot_mismoCursoMismoDiaHora_debeFallarPorConstraint() throws SQLException {
        HorarioSlotDao dao = new HorarioSlotDao();
        int profesor1 = createUsuario("prof-2", 1);
        int profesor2 = createUsuario("prof-3", 1);
        int materia1 = createMateria("Lengua");
        int materia2 = createMateria("Historia");
        int asignacion1 = createAsignacion(profesor1, materia1, 1);
        int asignacion2 = createAsignacion(profesor2, materia2, 1);

        dao.crear(asignacion1, profesor1, 1, 2, 1, "Aula 10");

        SQLIntegrityConstraintViolationException ex = assertThrows(SQLIntegrityConstraintViolationException.class,
                () -> dao.crear(asignacion2, profesor2, 1, 2, 1, "Aula 11"));
        assertEquals("uq_horario_curso", ex.getMessage().contains("uq_horario_curso") ? "uq_horario_curso" : ex.getMessage());
    }

    @Test
    public void crearSlot_mismoProfesorDistintaHora_debePermitir() throws SQLException {
        HorarioSlotDao dao = new HorarioSlotDao();
        int profesorId = createUsuario("prof-4", 1);
        int materiaId = createMateria("Física");
        int asignacionId = createAsignacion(profesorId, materiaId, 1);

        assertDoesNotThrow(() -> dao.crear(asignacionId, profesorId, 1, 3, 1, "Lab 1"));
        assertDoesNotThrow(() -> dao.crear(asignacionId, profesorId, 1, 3, 2, "Lab 2"));
    }

    @Test
    public void eliminarSlot_yRecrearIgual_debePermitir() throws SQLException {
        HorarioSlotDao dao = new HorarioSlotDao();
        int profesorId = createUsuario("prof-5", 1);
        int materiaId = createMateria("Química");
        int asignacionId = createAsignacion(profesorId, materiaId, 1);

        int slotId = dao.crear(asignacionId, profesorId, 1, 4, 1, "Aula 3");
        assertEquals(true, dao.eliminar(slotId));
        assertDoesNotThrow(() -> dao.crear(asignacionId, profesorId, 1, 4, 1, "Aula 4"));
    }
}
