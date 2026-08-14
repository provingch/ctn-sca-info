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
     * Materias del catálogo asociadas al profesor. Se une directamene con la
     * relación profesor_materia y conserva compatibilidad con planillas ya
     * existentes en la base.
     */
    public List<Materia> listByProfesor(int profesorId) throws SQLException {
        String sql = "SELECT DISTINCT m.id, m.nombre, m.categoria "
                + "FROM ( "
                + "    SELECT pm.materia_id FROM profesor_materia pm WHERE pm.profesor_id = ? "
                + "    UNION "
                + "    SELECT p.materia_id FROM planilla p WHERE p.profesor_id = ? "
                + ") ids "
                + "JOIN materia m ON m.id = ids.materia_id "
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

    /**
     * Materias válidas para un profesor: las 'comun' (aplican a cualquier especialidad)
     * más las 'especifico' de las especialidades de los cursos que el profesor
     * efectivamente tiene asignados (vía planilla -> curso -> especialidad), o las
     * materias que el propio profesor haya registrado en su perfil.
     */
    public List<Materia> listAvailableForProfesor(int profesorId) throws SQLException {
        String sql = "SELECT DISTINCT m.id, m.nombre, m.categoria "
                + "FROM materia m "
                + "WHERE m.categoria = 'comun' "
                + "   OR m.id IN ( "
                + "        SELECT me.materia_id FROM materia_especialidad me "
                + "        WHERE me.especialidad_id IN ( "
                + "            SELECT DISTINCT c.especialidad_id "
                + "            FROM planilla p "
                + "            JOIN curso c ON p.curso_id = c.id "
                + "            WHERE p.profesor_id = ? "
                + "        ) "
                + "   ) "
                + "   OR m.id IN ( "
                + "        SELECT pm.materia_id FROM profesor_materia pm WHERE pm.profesor_id = ? "
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

    public boolean linkProfesorMateria(int profesorId, int materiaId) throws SQLException {
        String sql = "INSERT IGNORE INTO profesor_materia (profesor_id, materia_id) VALUES (?, ?)";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, profesorId);
            ps.setInt(2, materiaId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean unlinkProfesorMateria(int profesorId, int materiaId) throws SQLException {
        String sql = "DELETE FROM profesor_materia WHERE profesor_id = ? AND materia_id = ?";
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, profesorId);
            ps.setInt(2, materiaId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<String> findNamesByProfesor(int profesorId) throws SQLException {
        String sql = "SELECT DISTINCT m.nombre "
                + "FROM ( "
                + "    SELECT pm.materia_id FROM profesor_materia pm WHERE pm.profesor_id = ? "
                + "    UNION "
                + "    SELECT p.materia_id FROM planilla p WHERE p.profesor_id = ? "
                + ") ids "
                + "JOIN materia m ON m.id = ids.materia_id "
                + "ORDER BY m.nombre";
        List<String> names = new ArrayList<>();
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, profesorId);
            ps.setInt(2, profesorId);
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
        String sql = "SELECT materia_id, COUNT(*) AS cnt FROM profesor_materia GROUP BY materia_id";
        java.util.Map<Integer, Integer> out = new java.util.HashMap<>();
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.put(rs.getInt("materia_id"), rs.getInt("cnt"));
            }
        }
        return out;
    }

    public int countOtherProfesores(int materiaId, int excludingProfesorId) throws SQLException {
        String sql = "SELECT COUNT(*) AS cnt FROM profesor_materia WHERE materia_id = ? AND profesor_id != ?";
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
     * fail with SQLException if conflicting planillas exist (same curso/periodo/etapa).
     *
     * @throws SQLException with a descriptive message when merge cannot proceed
     */
    public boolean mergeMaterias(int fromMateriaId, int toMateriaId) throws SQLException {
        if (fromMateriaId <= 0 || toMateriaId <= 0 || fromMateriaId == toMateriaId) {
            throw new SQLException("Invalid materia ids for merge");
        }

        String conflictSql = "SELECT p.id, p.curso_id, p.periodo, p.etapa "
                + "FROM planilla p "
                + "WHERE p.materia_id = ? AND EXISTS ("
                + "  SELECT 1 FROM planilla q WHERE q.materia_id = ? AND q.curso_id = p.curso_id AND q.periodo = p.periodo AND q.etapa = p.etapa"
                + ")";

        try (Connection c = getCon()) {
            try {
                c.setAutoCommit(false);

                // detect conflicts
                try (PreparedStatement ps = c.prepareStatement(conflictSql)) {
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

                // 1) update planilla references
                try (PreparedStatement updPlan = c.prepareStatement("UPDATE planilla SET materia_id = ? WHERE materia_id = ?")) {
                    updPlan.setInt(1, toMateriaId);
                    updPlan.setInt(2, fromMateriaId);
                    updPlan.executeUpdate();
                }

                // 2) copy profesor_materia entries
                try (PreparedStatement insPm = c.prepareStatement("INSERT IGNORE INTO profesor_materia (profesor_id, materia_id) SELECT profesor_id, ? FROM profesor_materia WHERE materia_id = ?")) {
                    insPm.setInt(1, toMateriaId);
                    insPm.setInt(2, fromMateriaId);
                    insPm.executeUpdate();
                }

                // 3) delete old profesor_materia rows
                try (PreparedStatement delPm = c.prepareStatement("DELETE FROM profesor_materia WHERE materia_id = ?")) {
                    delPm.setInt(1, fromMateriaId);
                    delPm.executeUpdate();
                }

                // 4) copy materia_especialidad rows
                try (PreparedStatement insMe = c.prepareStatement("INSERT IGNORE INTO materia_especialidad (materia_id, especialidad_id) SELECT ?, especialidad_id FROM materia_especialidad WHERE materia_id = ?")) {
                    insMe.setInt(1, toMateriaId);
                    insMe.setInt(2, fromMateriaId);
                    insMe.executeUpdate();
                }

                // 5) delete old materia_especialidad rows
                try (PreparedStatement delMe = c.prepareStatement("DELETE FROM materia_especialidad WHERE materia_id = ?")) {
                    delMe.setInt(1, fromMateriaId);
                    delMe.executeUpdate();
                }

                // 6) delete materia
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
     * Checks for conflicting planillas that would prevent merging from->to.
     * Returns an empty list when no conflicts are found.
     */
    public List<String> checkMergeConflicts(int fromMateriaId, int toMateriaId) throws SQLException {
        if (fromMateriaId <= 0 || toMateriaId <= 0 || fromMateriaId == toMateriaId) {
            return new ArrayList<>();
        }

        String conflictSql = "SELECT p.id, p.curso_id, p.periodo, p.etapa "
                + "FROM planilla p "
                + "WHERE p.materia_id = ? AND EXISTS ("
                + "  SELECT 1 FROM planilla q WHERE q.materia_id = ? AND q.curso_id = p.curso_id AND q.periodo = p.periodo AND q.etapa = p.etapa"
                + ")";
        List<String> conflicts = new ArrayList<>();
        try (Connection c = getCon(); PreparedStatement ps = c.prepareStatement(conflictSql)) {
            ps.setInt(1, fromMateriaId);
            ps.setInt(2, toMateriaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    conflicts.add("planilla#" + rs.getInt("id") + " (curso=" + rs.getInt("curso_id")
                            + " periodo=" + rs.getInt("periodo") + " etapa=" + rs.getString("etapa") + ")");
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
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM profesor_materia WHERE materia_id = ?")) {
                ps.setInt(1, materiaId);
                ps.executeUpdate();
            }
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
