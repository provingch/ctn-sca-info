package ctn.informatica.sca.dao;

import ctn.informatica.sca.clases.conexion;
import ctn.informatica.sca.model.Alumno;
import ctn.informatica.sca.model.RasgoAsistencia;
import ctn.informatica.sca.model.RasgoPlanilla;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RasgoPlanillaDao extends conexion {

    public int crearPlanillaRasgo(int cursoId, int profesorId, String tema, List<Alumno> alumnos) throws SQLException {
        if (alumnos == null || alumnos.isEmpty()) {
            throw new SQLException("No hay alumnos elegibles para crear la planilla de rasgos");
        }

        String insertPlanillaSql = "INSERT INTO planilla_rasgo (curso_id, profesor_id, tema, fecha_clase) VALUES (?, ?, ?, CURRENT_DATE())";
        String insertAsistenciaSql = "INSERT INTO rasgo_asistencia (planilla_rasgo_id, alumno_id, alumno_nombre, alumno_apellido, alumno_email) VALUES (?, ?, ?, ?, ?)";

        try (Connection con = getCon()) {
            boolean originalAutoCommit = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                int planillaId;
                try (PreparedStatement ps = con.prepareStatement(insertPlanillaSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, cursoId);
                    ps.setInt(2, profesorId);
                    ps.setString(3, tema);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("No se pudo generar la planilla de rasgos");
                        }
                        planillaId = keys.getInt(1);
                    }
                }

                try (PreparedStatement ps = con.prepareStatement(insertAsistenciaSql)) {
                    for (Alumno alumno : alumnos) {
                        ps.setInt(1, planillaId);
                        ps.setInt(2, alumno.getId());
                        ps.setString(3, alumno.getNombre());
                        ps.setString(4, alumno.getApellido());
                        ps.setString(5, alumno.getGoogleEmail());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                con.commit();
                return planillaId;
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(originalAutoCommit);
            }
        }
    }

    public List<RasgoPlanilla> listarPorProfesorCurso(int profesorId, int cursoId) throws SQLException {
        String sql = "SELECT id, curso_id, profesor_id, tema, fecha_clase, created_at "
                + "FROM planilla_rasgo WHERE profesor_id = ? AND curso_id = ? ORDER BY created_at DESC";
        List<RasgoPlanilla> planillas = new ArrayList<>();
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, profesorId);
            ps.setInt(2, cursoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    planillas.add(fromPlanillaResultSet(rs));
                }
            }
        }
        return planillas;
    }

    public RasgoPlanilla findPlanillaById(int planillaId) throws SQLException {
        String sql = "SELECT id, curso_id, profesor_id, tema, fecha_clase, created_at FROM planilla_rasgo WHERE id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planillaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return fromPlanillaResultSet(rs);
                }
            }
        }
        return null;
    }

    public List<RasgoAsistencia> listarAsistencias(int planillaRasgoId) throws SQLException {
        String sql = "SELECT ra.id, ra.planilla_rasgo_id, ra.alumno_id, ra.alumno_nombre, ra.alumno_apellido, ra.alumno_email, "
                + "ra.estado, ra.responded_at, pr.tema "
                + "FROM rasgo_asistencia ra "
                + "INNER JOIN planilla_rasgo pr ON pr.id = ra.planilla_rasgo_id "
                + "WHERE ra.planilla_rasgo_id = ? ORDER BY ra.alumno_apellido, ra.alumno_nombre";
        List<RasgoAsistencia> asistencias = new ArrayList<>();
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planillaRasgoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    asistencias.add(fromAsistenciaResultSet(rs));
                }
            }
        }
        return asistencias;
    }

    public RasgoAsistencia findAsistenciaById(int asistenciaId) throws SQLException {
        String sql = "SELECT ra.id, ra.planilla_rasgo_id, ra.alumno_id, ra.alumno_nombre, ra.alumno_apellido, ra.alumno_email, "
                + "ra.estado, ra.responded_at, pr.tema "
                + "FROM rasgo_asistencia ra "
                + "INNER JOIN planilla_rasgo pr ON pr.id = ra.planilla_rasgo_id "
                + "WHERE ra.id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, asistenciaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return fromAsistenciaResultSet(rs);
                }
            }
        }
        return null;
    }

    public boolean registrarRespuesta(int asistenciaId, String estado) throws SQLException {
        String sql = "UPDATE rasgo_asistencia SET estado = ?, responded_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setInt(2, asistenciaId);
            return ps.executeUpdate() == 1;
        }
    }

    private RasgoPlanilla fromPlanillaResultSet(ResultSet rs) throws SQLException {
        RasgoPlanilla planilla = new RasgoPlanilla();
        planilla.setId(rs.getInt("id"));
        planilla.setCursoId(rs.getInt("curso_id"));
        planilla.setProfesorId(rs.getInt("profesor_id"));
        planilla.setTema(rs.getString("tema"));
        Date fechaClase = rs.getDate("fecha_clase");
        planilla.setFechaClase(fechaClase);
        planilla.setCreatedAt(rs.getTimestamp("created_at"));
        return planilla;
    }

    private RasgoAsistencia fromAsistenciaResultSet(ResultSet rs) throws SQLException {
        RasgoAsistencia asistencia = new RasgoAsistencia();
        asistencia.setId(rs.getInt("id"));
        asistencia.setPlanillaRasgoId(rs.getInt("planilla_rasgo_id"));
        asistencia.setAlumnoId(rs.getInt("alumno_id"));
        asistencia.setAlumnoNombre(rs.getString("alumno_nombre"));
        asistencia.setAlumnoApellido(rs.getString("alumno_apellido"));
        asistencia.setAlumnoEmail(rs.getString("alumno_email"));
        asistencia.setEstado(rs.getString("estado"));
        asistencia.setRespondedAt(rs.getTimestamp("responded_at"));
        asistencia.setTema(rs.getString("tema"));
        return asistencia;
    }
}
