package ctn.informatica.sca.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ctn.informatica.sca.model.Planilla;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PlanillaDaoIntegrationTest {

    @BeforeEach
    public void setUp() throws SQLException {
        Connection testConnection;
        try {
            testConnection = new PlanillaDao().getCon();
        } catch (SQLException exception) {
            assumeTrue(false, () -> "Requiere la base MySQL de integración: " + exception.getMessage());
            return;
        }

        try (Connection c = testConnection; PreparedStatement ps = c.prepareStatement(
                "DELETE FROM planilla; DELETE FROM asignacion; DELETE FROM materia; DELETE FROM curso; DELETE FROM usuario; DELETE FROM especialidad;")) {
            ps.executeUpdate();
        }

        try (Connection c = new PlanillaDao().getCon(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO especialidad (id, nombre) VALUES (1, 'Informática')")) {
            ps.executeUpdate();
        }
        try (Connection c = new PlanillaDao().getCon(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO curso (id, especialidad_id, promocion, seccion) VALUES (1, 1, 2027, 'A')")) {
            ps.executeUpdate();
        }
    }

    private int createUsuario(String usuario, int nivel) throws SQLException {
        String sql = "INSERT INTO usuario (nombre, apellido, usuario, nivel) VALUES (?, ?, ?, ?)";
        try (Connection c = new PlanillaDao().getCon(); PreparedStatement ps = c.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
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
        try (Connection c = new PlanillaDao().getCon(); PreparedStatement ps = c.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
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
        try (Connection c = new PlanillaDao().getCon(); PreparedStatement ps = c.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
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
    public void updateFechaYConfirmacion_debePersistirYLeerse() throws SQLException {
        PlanillaDao dao = new PlanillaDao();

        int profesorId = createUsuario("prof-plan-1", 1);
        int materiaId = createMateria("PruebaMateria");
        int asignacionId = createAsignacion(profesorId, materiaId, 1);

        // crear planilla para curso=1, materia creada, etapa 1
        Planilla p = dao.crear(1, materiaId, 1, profesorId);
        assertEquals(1, p.getCursoId());

        LocalDate cierre = LocalDate.of(2026, 9, 2);

        boolean updatedFecha = dao.updateFechaCierreEtapa1(p.getId(), cierre);
        assertEquals(true, updatedFecha);

        boolean updatedConfirm = dao.updateEtapa1Confirmada(p.getId(), true);
        assertEquals(true, updatedConfirm);

        Planilla loaded = dao.findById(p.getId());
        assertEquals(cierre, loaded.getFechaCierreEtapa1());
        assertEquals(true, loaded.isEtapa1Confirmada());
    }
}
