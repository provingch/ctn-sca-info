package ctn.informatica.sca.dao;

import org.springframework.stereotype.Repository;
import ctn.informatica.sca.clases.conexion;
import ctn.informatica.sca.model.Materia;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Repository
public class MateriaDao extends conexion {

    private Materia fromResultSet(ResultSet rs) throws SQLException {
        return new Materia(rs.getInt("id"), rs.getString("nombre"), rs.getString("categoria"));
    }

    /**
     * Todas las materias del catálogo.
     */
    public List<Materia> listAll() throws SQLException {
        String sql = "SELECT id, nombre, categoria FROM materia ORDER BY nombre";
        List<Materia> out = new ArrayList<>();
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(fromResultSet(rs));
            }
        }
        return out;
    }

    /**
     * Materias del catálogo asociadas al profesor a través de sus asignaciones.
     */
    public List<Materia> listByProfesor(int profesorId) throws SQLException {
        String sql = "SELECT DISTINCT m.id, m.nombre, m.categoria "
                + "FROM asignacion a "
                + "JOIN materia m ON m.id = a.materia_id "
                + "WHERE a.usuario_id = ? "
                + "ORDER BY m.nombre";
        List<Materia> out = new ArrayList<>();
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, profesorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(fromResultSet(rs));
                }
            }
        }
        return out;
    }

    /**
     * Materias válidas para un profesor: las 'comun' y las materias compatibles
     * con las especialidades de sus cursos asignados.
     */
    public List<Materia> listAvailableForProfesor(int profesorId) throws SQLException {
        String sql = "SELECT DISTINCT m.id, m.nombre, m.categoria "
                + "FROM materia m "
                + "WHERE m.categoria = 'comun' "
                + "   OR m.id IN ( "
                + "        SELECT DISTINCT me.materia_id "
                + "        FROM asignacion a "
                + "        JOIN curso_base cb ON cb.id = a.curso_base_id "
                + "        JOIN materia_especialidad me ON me.especialidad_id = cb.especialidad_id "
                + "        WHERE a.usuario_id = ? "
                + "   ) "
                + "   OR m.id IN ( "
                + "        SELECT DISTINCT a.materia_id FROM asignacion a WHERE a.usuario_id = ? "
                + "   ) "
                + "ORDER BY m.nombre";
        List<Materia> out = new ArrayList<>();
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, profesorId);
            ps.setInt(2, profesorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(fromResultSet(rs));
                }
            }
        }
        return out;
    }

    public List<String> findNamesByProfesor(int profesorId) throws SQLException {
        String sql = "SELECT DISTINCT m.nombre "
                + "FROM asignacion a "
                + "JOIN materia m ON m.id = a.materia_id "
                + "WHERE a.usuario_id = ? "
                + "ORDER BY m.nombre";
        List<String> names = new ArrayList<>();
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, profesorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("nombre");
                    if (name != null && !name.trim().isEmpty()) {
                        names.add(name.trim());
                    }
                }
            }
        }
        return names;
    }

    /**
     * Especialidades a las que pertenece una materia (vacío para 'especifico' sin
     * vínculo cargado, o varias filas para 'comun').
     */
    public List<Integer> listEspecialidadIdsForMateria(int materiaId) throws SQLException {
        String sql = "SELECT especialidad_id FROM materia_especialidad WHERE materia_id = ?";
        List<Integer> out = new ArrayList<>();
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, materiaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(rs.getInt("especialidad_id"));
                }
            }
        }
        return out;
    }

    /**
     * Count linked professors per materia for all materias. Returns a map materiaId->count.
     */
    public java.util.Map<Integer, Integer> countProfesoresForAll() throws SQLException {
        String sql = "SELECT materia_id, COUNT(DISTINCT usuario_id) AS cnt FROM asignacion GROUP BY materia_id";
        java.util.Map<Integer, Integer> out = new java.util.HashMap<>();
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.put(rs.getInt("materia_id"), rs.getInt("cnt"));
            }
        }
        return out;
    }

    public int countOtherProfesores(int materiaId, int excludingProfesorId) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT usuario_id) AS cnt FROM asignacion WHERE materia_id = ? AND usuario_id != ?";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, materiaId);
            ps.setInt(2, excludingProfesorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("cnt");
            }
        }
        return 0;
    }

    /**
     * Find materias with names similar to the provided name (simple LIKE match).
     */
    public List<Materia> findSimilarByName(String name) throws SQLException {
        if (name == null) return java.util.Collections.emptyList();
        String normalized = name.trim().toLowerCase();
        String sql = "SELECT id, nombre, categoria FROM materia WHERE LOWER(nombre) LIKE ? ORDER BY nombre LIMIT 10";
        List<Materia> out = new ArrayList<>();
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + normalized + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(fromResultSet(rs));
                }
            }
        }
        return out;
    }

    public boolean linkEspecialidad(int materiaId, int especialidadId) throws SQLException {
        String sql = "INSERT IGNORE INTO materia_especialidad (materia_id, especialidad_id) VALUES (?, ?)";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, materiaId);
            ps.setInt(2, especialidadId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean unlinkEspecialidad(int materiaId, int especialidadId) throws SQLException {
        String sql = "DELETE FROM materia_especialidad WHERE materia_id = ? AND especialidad_id = ?";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, materiaId);
            ps.setInt(2, especialidadId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean replaceEspecialidades(int materiaId, List<Integer> especialidadIds) throws SQLException {
        String deleteSql = "DELETE FROM materia_especialidad WHERE materia_id = ?";
        String insertSql = "INSERT IGNORE INTO materia_especialidad (materia_id, especialidad_id) VALUES (?, ?)";
        try (Connection c = getCon()) {
            try (PreparedStatement deletePs = c.prepareStatement(deleteSql)) {
                deletePs.setInt(1, materiaId);
                deletePs.executeUpdate();
            }

            if (especialidadIds == null || especialidadIds.isEmpty()) {
                return true;
            }

            try (PreparedStatement insertPs = c.prepareStatement(insertSql)) {
                for (Integer especialidadId : especialidadIds) {
                    if (especialidadId == null) {
                        continue;
                    }
                    insertPs.setInt(1, materiaId);
                    insertPs.setInt(2, especialidadId);
                    insertPs.addBatch();
                }
                if (especialidadIds.stream().anyMatch(id -> id != null)) {
                    insertPs.executeBatch();
                }
            }
            return true;
        }
    }

    public int create(String nombre, String categoria) throws SQLException {
        String sql = "INSERT INTO materia (nombre, categoria) VALUES (?, ?)";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.setString(2, categoria == null ? "especifico" : categoria.trim().toLowerCase());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    public boolean update(int materiaId, String nombre, String categoria) throws SQLException {
        String sql = "UPDATE materia SET nombre = ?, categoria = ? WHERE id = ?";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nombre == null ? "" : nombre.trim());
            ps.setString(2, categoria == null ? "especifico" : categoria.trim().toLowerCase());
            ps.setInt(3, materiaId);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean updateCategoria(int materiaId, String categoria) throws SQLException {
        String sql = "UPDATE materia SET categoria = ? WHERE id = ?";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, categoria == null ? "especifico" : categoria.trim().toLowerCase());
            ps.setInt(2, materiaId);
            return ps.executeUpdate() == 1;
        }
    }

    public Materia findByNombre(String nombre) throws SQLException {
        String sql = "SELECT id, nombre, categoria FROM materia WHERE LOWER(nombre) = LOWER(?)";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nombre == null ? "" : nombre.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Materia(rs.getInt("id"), rs.getString("nombre"), rs.getString("categoria"));
                }
            }
        }
        return null;
    }
    
    public Materia findById(int id) throws SQLException {
        String sql = "SELECT id, nombre, categoria FROM materia WHERE id = ?";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Materia(rs.getInt("id"), rs.getString("nombre"), rs.getString("categoria"));
                }
            }
        }
        return null;
    }

    /**
     * Merge two materias: move all references from fromMateriaId into toMateriaId
     * and delete the source materia. This runs in a single transaction and will
     * fail with SQLException if conflicting planillas or asignaciones exist.
     *
     * @throws SQLException with a descriptive message when merge cannot proceed
     */
    public boolean mergeMaterias(int fromMateriaId, int toMateriaId) throws SQLException {
        if (fromMateriaId <= 0 || toMateriaId <= 0 || fromMateriaId == toMateriaId) {
            throw new SQLException("Invalid materia ids for merge");
        }

        String planillaConflictSql = "SELECT p.id, p.curso_id, p.periodo, p.etapa "
                + "FROM planilla p "
                + "WHERE p.materia_id = ? AND EXISTS ("
                + "  SELECT 1 FROM planilla q WHERE q.materia_id = ? AND q.curso_id = p.curso_id AND q.periodo = p.periodo AND q.etapa = p.etapa"
                + ")";
        String asignacionConflictSql = "SELECT a.id, a.usuario_id, a.curso_base_id "
                + "FROM asignacion a "
                + "WHERE a.materia_id = ? AND EXISTS ("
                + "  SELECT 1 FROM asignacion q WHERE q.materia_id = ? AND q.usuario_id = a.usuario_id AND q.curso_base_id = a.curso_base_id"
                + ")";

        try (Connection c = getCon()) {
            try {
                c.setAutoCommit(false);

                // detect conflicts
                try (PreparedStatement ps = c.prepareStatement(planillaConflictSql)) {
                    ps.setInt(1, fromMateriaId);
                    ps.setInt(2, toMateriaId);
                    try (ResultSet rs = ps.executeQuery()) {
                        StringBuilder conflicts = new StringBuilder();
                        while (rs.next()) {
                            if (conflicts.length() > 0) {
                                conflicts.append(", ");
                            }
                            conflicts.append("planilla#").append(rs.getInt("id"))
                                    .append("(curso=").append(rs.getInt("curso_id"))
                                    .append(" periodo=").append(rs.getInt("periodo"))
                                    .append(" etapa=").append(rs.getString("etapa"))
                                    .append(")");
                        }
                        if (conflicts.length() > 0) {
                            c.rollback();
                            throw new SQLException("Conflicting planillas exist: " + conflicts.toString());
                        }
                    }
                }

                try (PreparedStatement ps = c.prepareStatement(asignacionConflictSql)) {
                    ps.setInt(1, fromMateriaId);
                    ps.setInt(2, toMateriaId);
                    try (ResultSet rs = ps.executeQuery()) {
                        StringBuilder conflicts = new StringBuilder();
                        while (rs.next()) {
                            if (conflicts.length() > 0) {
                                conflicts.append(", ");
                            }
                            conflicts.append("asignacion#").append(rs.getInt("id"))
                                    .append("(profesor=").append(rs.getInt("usuario_id"))
                                    .append(" curso_base=").append(rs.getInt("curso_base_id"))
                                    .append(")");
                        }
                        if (conflicts.length() > 0) {
                            c.rollback();
                            throw new SQLException("Conflicting asignaciones exist: " + conflicts.toString());
                        }
                    }
                }

                // 1) update planilla references
                try (PreparedStatement updPlan = c.prepareStatement("UPDATE planilla SET materia_id = ? WHERE materia_id = ?")) {
                    updPlan.setInt(1, toMateriaId);
                    updPlan.setInt(2, fromMateriaId);
                    updPlan.executeUpdate();
                }

                // 2) update assignment references
                try (PreparedStatement updAsig = c.prepareStatement("UPDATE asignacion SET materia_id = ? WHERE materia_id = ?")) {
                    updAsig.setInt(1, toMateriaId);
                    updAsig.setInt(2, fromMateriaId);
                    updAsig.executeUpdate();
                }

                // 3) copy materia_especialidad rows
                try (PreparedStatement insMe = c.prepareStatement("INSERT IGNORE INTO materia_especialidad (materia_id, especialidad_id) SELECT ?, especialidad_id FROM materia_especialidad WHERE materia_id = ?")) {
                    insMe.setInt(1, toMateriaId);
                    insMe.setInt(2, fromMateriaId);
                    insMe.executeUpdate();
                }

                // 4) delete old materia_especialidad rows
                try (PreparedStatement delMe = c.prepareStatement("DELETE FROM materia_especialidad WHERE materia_id = ?")) {
                    delMe.setInt(1, fromMateriaId);
                    delMe.executeUpdate();
                }

                // 5) delete materia
                try (PreparedStatement delM = c.prepareStatement("DELETE FROM materia WHERE id = ?")) {
                    delM.setInt(1, fromMateriaId);
                    delM.executeUpdate();
                }

                c.commit();
                return true;
            } catch (SQLException ex) {
                try {
                    c.rollback();
                } catch (SQLException ignore) {
                }
                throw ex;
            } finally {
                try {
                    c.setAutoCommit(true);
                } catch (SQLException ignore) {
                }
            }
        }
    }

    /**
     * Checks for conflicting planillas or asignaciones that would prevent merging from->to.
     * Returns an empty list when no conflicts are found.
     */
    public List<String> checkMergeConflicts(int fromMateriaId, int toMateriaId) throws SQLException {
        if (fromMateriaId <= 0 || toMateriaId <= 0 || fromMateriaId == toMateriaId) {
            return new ArrayList<>();
        }

        String planillaConflictSql = "SELECT p.id, p.curso_id, p.periodo, p.etapa "
                + "FROM planilla p "
                + "WHERE p.materia_id = ? AND EXISTS ("
                + "  SELECT 1 FROM planilla q WHERE q.materia_id = ? AND q.curso_id = p.curso_id AND q.periodo = p.periodo AND q.etapa = p.etapa"
                + ")";
        List<String> conflicts = new ArrayList<>();
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(planillaConflictSql)) {
            ps.setInt(1, fromMateriaId);
            ps.setInt(2, toMateriaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    conflicts.add("planilla#" + rs.getInt("id") + " (curso=" + rs.getInt("curso_id")
                            + " periodo=" + rs.getInt("periodo") + " etapa=" + rs.getString("etapa") + ")");
                }
            }
        }
        String asignacionConflictSql = "SELECT a.id, a.usuario_id, a.curso_base_id "
                + "FROM asignacion a "
                + "WHERE a.materia_id = ? AND EXISTS ("
                + "  SELECT 1 FROM asignacion q WHERE q.materia_id = ? AND q.usuario_id = a.usuario_id AND q.curso_base_id = a.curso_base_id"
                + ")";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(asignacionConflictSql)) {
            ps.setInt(1, fromMateriaId);
            ps.setInt(2, toMateriaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    conflicts.add("asignacion#" + rs.getInt("id") + " (profesor=" + rs.getInt("usuario_id")
                            + " curso_base=" + rs.getInt("curso_base_id") + ")");
                }
            }
        }
        return conflicts;
    }

    public boolean delete(int materiaId) throws SQLException {
        String checkSql = "SELECT 1 FROM planilla WHERE materia_id = ? LIMIT 1";
        try (Connection c = getCon(); PreparedStatement checkPs = c.prepareStatement(checkSql)) {
            checkPs.setInt(1, materiaId);
            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next()) return false; // referenced by planilla
            }
        }

        try (Connection c = getCon()) {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM materia_especialidad WHERE materia_id = ?")) {
                ps.setInt(1, materiaId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM materia WHERE id = ?")) {
                ps.setInt(1, materiaId);
                return ps.executeUpdate() == 1;
            }
        }
    }
}
