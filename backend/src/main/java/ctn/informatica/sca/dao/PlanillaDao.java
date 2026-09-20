/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ctn.informatica.sca.dao;

import org.springframework.stereotype.Repository;
import ctn.informatica.sca.clases.conexion;
import ctn.informatica.sca.dto.PlanillaResumenDto;
import ctn.informatica.sca.model.Curso;
import ctn.informatica.sca.model.Materia;
import ctn.informatica.sca.model.Planilla;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author jonat
 */
@Repository
public class PlanillaDao extends conexion {

    private static final int DEFAULT_PERIOD = ctn.informatica.sca.util.AcademicPeriod.current();

    private String normalizeEtapa(int etapaIndex) {
        return etapaIndex == 2 ? "segunda" : "primera";
    }

    public static boolean shouldIncludePlanillaForProfesor(int materiaId, Set<Integer> allowedMateriaIds) {
        return allowedMateriaIds != null && !allowedMateriaIds.isEmpty() && allowedMateriaIds.contains(materiaId);
    }

    private Set<Integer> findAllowedMateriaIdsForProfesor(int profesorId) throws SQLException {
        String sql = "SELECT DISTINCT materia_id FROM asignacion WHERE usuario_id = ?";
        Set<Integer> allowedMateriaIds = new HashSet<>();
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, profesorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    allowedMateriaIds.add(rs.getInt("materia_id"));
                }
            }
        }
        return allowedMateriaIds;
    }

    public ArrayList<Planilla> consultarPlanillas(int userId, int cursoId, int etapaIndex) throws SQLException {
        Set<Integer> allowedMateriaIds = findAllowedMateriaIdsForProfesor(userId);
        if (allowedMateriaIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> placeholders = new ArrayList<>(allowedMateriaIds.size());
        for (int ignored = 0; ignored < allowedMateriaIds.size(); ignored++) {
            placeholders.add("?");
        }

        String sql = "SELECT p.id, m.nombre AS nombre, curso_id, materia_id, periodo, etapa, p.usuario_id AS profesor_id, p.google_course_id, p.fecha_cierre_etapa1, p.etapa1_confirmada, p.fecha_cierre_etapa2, p.etapa2_confirmada, COUNT(DISTINCT t.id) AS tareas_count "
                + "FROM planilla p "
                + "JOIN materia m ON p.materia_id = m.id "
                + "LEFT JOIN tarea t ON t.planilla_id = p.id "
                + "WHERE curso_id = ? AND p.usuario_id = ? AND etapa = ? AND periodo = ? AND p.materia_id IN (" + String.join(", ", placeholders) + ") "
                + "GROUP BY p.id, m.nombre, p.curso_id, p.materia_id, p.periodo, p.etapa, p.usuario_id, p.google_course_id";
        try (Connection con = getCon(); PreparedStatement stm = con.prepareStatement(sql)) {
            int index = 1;
            stm.setInt(index++, cursoId);
            stm.setInt(index++, userId);
            stm.setString(index++, normalizeEtapa(etapaIndex));
            stm.setInt(index++, DEFAULT_PERIOD);
            for (Integer materiaId : allowedMateriaIds) {
                stm.setInt(index++, materiaId);
            }
            ResultSet rs = stm.executeQuery();
            ArrayList<Planilla> planillas = new ArrayList<>();
            while (rs.next()) {
                int id = rs.getInt("id");
                int curso_id = rs.getInt("curso_id");
                int materia_id = rs.getInt("materia_id");
                String nombre = rs.getString("nombre");
                int periodo = rs.getInt("periodo");
                String etapa = rs.getString("etapa");// NOTE might cause problems in the future
                int profesor_id = rs.getInt("profesor_id");
                String googleCourseId = rs.getString("google_course_id");
                int tareas_count = rs.getInt("tareas_count");

                String ultimaTarea = consultarUltimaTarea(id);
                Planilla p = new Planilla(id, curso_id, materia_id, nombre, periodo, etapa, profesor_id, tareas_count, ultimaTarea);
                p.setGoogleCourseId(googleCourseId);
                planillas.add(p);
            }
            return planillas;
        }
    }

    public ArrayList<Planilla> consultarPlanillasUser(int userId, int etapaIndex) throws SQLException {
        Set<Integer> allowedMateriaIds = findAllowedMateriaIdsForProfesor(userId);
        if (allowedMateriaIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> placeholders = new ArrayList<>(allowedMateriaIds.size());
        for (int ignored = 0; ignored < allowedMateriaIds.size(); ignored++) {
            placeholders.add("?");
        }

        String sql = "SELECT p.id, m.nombre AS nombre, curso_id, materia_id, categoria, periodo, etapa, p.usuario_id AS profesor_id, p.fecha_cierre_etapa1, p.etapa1_confirmada, p.fecha_cierre_etapa2, p.etapa2_confirmada "
                + "FROM planilla p "
                + "JOIN materia m ON p.materia_id = m.id "
                + "WHERE p.usuario_id = ? AND etapa = ? AND periodo = ? AND p.materia_id IN (" + String.join(", ", placeholders) + ") "
                + "GROUP BY p.id, m.nombre, p.curso_id, p.materia_id, p.periodo, p.etapa, p.usuario_id";
        try (Connection con = getCon(); PreparedStatement stm = con.prepareStatement(sql)) {
            int index = 1;
            stm.setInt(index++, userId);
            stm.setString(index++, normalizeEtapa(etapaIndex));
            stm.setInt(index++, DEFAULT_PERIOD);
            for (Integer materiaId : allowedMateriaIds) {
                stm.setInt(index++, materiaId);
            }
            ResultSet rs = stm.executeQuery();
            ArrayList<Planilla> planillas = new ArrayList<>();
            while (rs.next()) {
                int planilla_id = rs.getInt("id");
                int curso_id = rs.getInt("curso_id");
                int materia_id = rs.getInt("materia_id");
                String categoria = rs.getString("categoria");
                String nombre = rs.getString("nombre");
                int periodo = rs.getInt("periodo");
                String etapa = rs.getString("etapa");// NOTE might cause problems in the future
                int profesor_id = rs.getInt("profesor_id");

                Planilla p = new Planilla(planilla_id, curso_id, materia_id, categoria, nombre, periodo, etapa, profesor_id);
                planillas.add(p);
            }
            return planillas;
        }
    }

    // Listado liviano para el home sin cursoId seleccionado: todas las planillas del
    // profesor en todos sus cursos, con lo que necesita cada tarjeta. tienePortada se
    // calcula con "portada IS NOT NULL" en la misma consulta (nunca la imagen), para
    // evitar tanto N+1 como mandar varios MB en una sola respuesta.
    public List<PlanillaResumenDto> consultarResumenPlanillasProfesor(int userId, int etapaIndex) throws SQLException {
        Set<Integer> allowedMateriaIds = findAllowedMateriaIdsForProfesor(userId);
        if (allowedMateriaIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> placeholders = new ArrayList<>(allowedMateriaIds.size());
        for (int ignored = 0; ignored < allowedMateriaIds.size(); ignored++) {
            placeholders.add("?");
        }

        String sql = "SELECT p.id, p.materia_id, m.nombre AS materia_nombre, "
                + "p.curso_id, c.especialidad_id, e.nombre AS especialidad_nombre, c.promocion, c.seccion, "
                + "p.etapa, (p.portada IS NOT NULL) AS tiene_portada "
                + "FROM planilla p "
                + "JOIN materia m ON m.id = p.materia_id "
                + "JOIN curso c ON c.id = p.curso_id "
                + "JOIN especialidad e ON e.id = c.especialidad_id "
                + "WHERE p.usuario_id = ? AND p.etapa = ? AND p.periodo = ? AND p.materia_id IN (" + String.join(", ", placeholders) + ") "
                + "ORDER BY e.nombre, c.promocion DESC, c.seccion, m.nombre";
        try (Connection con = getCon(); PreparedStatement stm = con.prepareStatement(sql)) {
            int index = 1;
            stm.setInt(index++, userId);
            stm.setString(index++, normalizeEtapa(etapaIndex));
            stm.setInt(index++, DEFAULT_PERIOD);
            for (Integer materiaId : allowedMateriaIds) {
                stm.setInt(index++, materiaId);
            }
            List<PlanillaResumenDto> out = new ArrayList<>();
            try (ResultSet rs = stm.executeQuery()) {
                while (rs.next()) {
                    String especialidadNombre = rs.getString("especialidad_nombre");
                    Curso curso = new Curso(rs.getInt("curso_id"), especialidadNombre, rs.getInt("promocion"), rs.getString("seccion"));
                    out.add(new PlanillaResumenDto(
                            rs.getInt("id"),
                            rs.getInt("materia_id"),
                            rs.getString("materia_nombre"),
                            rs.getInt("curso_id"),
                            curso.getCursoOrdinal(),
                            curso.getSeccion(),
                            especialidadNombre,
                            "segunda".equals(rs.getString("etapa")) ? 2 : 1,
                            rs.getBoolean("tiene_portada")));
                }
            }
            return out;
        }
    }

    public List<Materia> findMateriasSinPlanilla(int profesorId, int cursoId, int cursoBaseId, int etapaIndex) throws SQLException {
        String sql = "SELECT DISTINCT m.id, m.nombre, m.categoria "
                + "FROM materia m "
                + "JOIN asignacion a ON a.materia_id = m.id AND a.usuario_id = ? "
                + "WHERE a.curso_base_id = ? "
                + "AND m.id NOT IN ("
                + "    SELECT p.materia_id FROM planilla p "
                + "    WHERE p.curso_id = ? AND p.usuario_id = ? AND p.etapa = ? AND p.periodo = ?"
                + ") "
                + "ORDER BY m.nombre";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            int index = 1;
            ps.setInt(index++, profesorId);
            ps.setInt(index++, cursoBaseId);
            ps.setInt(index++, cursoId);
            ps.setInt(index++, profesorId);
            ps.setString(index++, normalizeEtapa(etapaIndex));
            ps.setInt(index++, DEFAULT_PERIOD);
            try (ResultSet rs = ps.executeQuery()) {
                List<Materia> materias = new ArrayList<>();
                while (rs.next()) {
                    materias.add(new Materia(rs.getInt("id"), rs.getString("nombre"), rs.getString("categoria")));
                }
                return materias;
            }
        }
    }

    public Planilla findById(int id) throws SQLException {// could create an interface
        String sql = "SELECT p.id, m.nombre AS nombre, curso_id, materia_id, categoria, periodo, etapa, p.usuario_id AS profesor_id, p.google_course_id, p.fecha_cierre_etapa1, p.etapa1_confirmada, p.fecha_cierre_etapa2, p.etapa2_confirmada, "
                + "p.rsa_puntos, p.rsa_tolerancia_valor, p.rsa_tolerancia_unidad "
                + "FROM planilla p JOIN materia m ON p.materia_id = m.id "
                + "WHERE p.id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return fromResultSet(rs);
            }
        }
    }

    public Planilla findByCompositeKey(int cursoId, int materiaId, int etapa) throws SQLException {
        String sql = "SELECT p.id, m.nombre AS nombre, curso_id, materia_id, categoria, periodo, etapa, p.usuario_id AS profesor_id, p.google_course_id, p.fecha_cierre_etapa1, p.etapa1_confirmada, p.fecha_cierre_etapa2, p.etapa2_confirmada, "
                + "p.rsa_puntos, p.rsa_tolerancia_valor, p.rsa_tolerancia_unidad "
                + "FROM planilla p JOIN materia m ON p.materia_id = m.id "
                + "WHERE curso_id = ? AND materia_id = ? AND periodo = ? AND etapa = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, cursoId);
            ps.setInt(2, materiaId);
            ps.setInt(3, DEFAULT_PERIOD);
            ps.setString(4, normalizeEtapa(etapa));
            try (ResultSet rs = ps.executeQuery()) {
                return fromResultSet(rs);
            }
        }
    }

    /**
     * Crea una planilla nueva para un curso/materia/etapa que todavia no tiene
     * una fila en la BD (por ejemplo, al entrar por primera vez desde un bloque
     * de Google Classroom). Se genera al vuelo, sin pasos manuales de por medio.
     */
    public Planilla crear(int cursoId, int materiaId, int etapaIndex, int profesorId) throws SQLException {
        String sql = "INSERT INTO planilla (curso_id, materia_id, periodo, etapa, usuario_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, cursoId);
            ps.setInt(2, materiaId);
            ps.setInt(3, DEFAULT_PERIOD);
            ps.setString(4, normalizeEtapa(etapaIndex));
            ps.setInt(5, profesorId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getInt(1));
                }
            }
        }
        return null;
    }

    public String consultarUltimaTarea(int planillaId) throws SQLException {
        String sql = "SELECT * FROM tarea WHERE planilla_id = ? ORDER BY id DESC LIMIT 1";
        try (Connection con = getCon(); PreparedStatement stm = con.prepareStatement(sql)) {
            stm.setInt(1, planillaId);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                return rs.getString("titulo");
            }
            return "";
        }
    }

    public List<String> findSubjectsByProfesor(int profesorId) throws SQLException {
        String sql = "SELECT DISTINCT m.nombre AS materia_nombre "
                + "FROM asignacion a "
                + "JOIN materia m ON m.id = a.materia_id "
                + "WHERE a.usuario_id = ? "
                + "ORDER BY m.nombre";
        List<String> subjects = new ArrayList<>();
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, profesorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String subject = rs.getString("materia_nombre");
                    if (subject != null && !subject.trim().isEmpty()) {
                        subjects.add(subject.trim());
                    }
                }
            }
        }
        return subjects;
    }

    public List<Materia> findMateriasByProfesor(int profesorId) throws SQLException {
        String sql = "SELECT DISTINCT m.id, m.nombre, m.categoria "
                + "FROM asignacion a "
                + "JOIN materia m ON m.id = a.materia_id "
                + "WHERE a.usuario_id = ? "
                + "ORDER BY m.nombre";
        List<Materia> materias = new ArrayList<>();
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, profesorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    materias.add(new Materia(rs.getInt("id"), rs.getString("nombre"), rs.getString("categoria")));
                }
            }
        }
        return materias;
    }

    public List<Materia> findMateriasWithPlanilla(int cursoId, int periodo) throws SQLException {
        String sql = "SELECT DISTINCT m.id, m.nombre, m.categoria "
                + "FROM planilla p "
                + "JOIN materia m ON p.materia_id = m.id "
                + "WHERE p.curso_id = ? AND p.periodo = ? "
                + "ORDER BY m.nombre";
        List<Materia> materias = new ArrayList<>();
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cursoId);
            ps.setInt(2, periodo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    materias.add(new Materia(rs.getInt("id"), rs.getString("nombre"), rs.getString("categoria")));
                }
            }
        }
        return materias;
    }

    public boolean updateFechaCierreEtapa1(int planillaId, java.time.LocalDate fecha) throws SQLException {
        String sql = "UPDATE planilla SET fecha_cierre_etapa1 = ? WHERE id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (fecha == null) {
                ps.setNull(1, java.sql.Types.DATE);
            } else {
                ps.setDate(1, java.sql.Date.valueOf(fecha));
            }
            ps.setInt(2, planillaId);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean updateEtapa1Confirmada(int planillaId, boolean confirmed) throws SQLException {
        String sql = "UPDATE planilla SET etapa1_confirmada = ? WHERE id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBoolean(1, confirmed);
            ps.setInt(2, planillaId);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean updateFechaCierreEtapa2(int planillaId, java.time.LocalDate fecha) throws SQLException {
        String sql = "UPDATE planilla SET fecha_cierre_etapa2 = ? WHERE id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (fecha == null) {
                ps.setNull(1, java.sql.Types.DATE);
            } else {
                ps.setDate(1, java.sql.Date.valueOf(fecha));
            }
            ps.setInt(2, planillaId);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Si la etapa de un plan curricular ya cerró: hay al menos una planilla de esa asignación (materia y curso del
     * año lectivo) con la etapa confirmada. Se cruza asignacion -> curso_base -> curso con la misma regla de
     * "promoción vigente" que AsignacionDao ({@code promocion = anio - nivel + 3}); una asignación es de una sola
     * sección, así que en la práctica es una planilla, pero "alguna" no depende de eso. La confirmación se mira en
     * cualquier planilla de la asignación y año, no sólo en la fila de esa etapa.
     */
    public boolean existeAlgunaCerrada(int asignacionId, int etapa, int anio) throws SQLException {
        String columna = etapa == 2 ? "etapa2_confirmada" : "etapa1_confirmada";
        String sql = "SELECT EXISTS(SELECT 1 FROM asignacion a "
                + "JOIN curso_base cb ON cb.id = a.curso_base_id "
                + "JOIN curso c ON c.especialidad_id = cb.especialidad_id AND c.seccion = cb.seccion AND c.promocion = (? - cb.nivel + 3) "
                + "JOIN planilla p ON p.curso_id = c.id AND p.materia_id = a.materia_id AND p.periodo = ? "
                + "WHERE a.id = ? AND p." + columna + " = TRUE)";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, anio);
            ps.setInt(2, anio);
            ps.setInt(3, asignacionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }

    public boolean updateEtapa2Confirmada(int planillaId, boolean confirmed) throws SQLException {
        String sql = "UPDATE planilla SET etapa2_confirmada = ? WHERE id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBoolean(1, confirmed);
            ps.setInt(2, planillaId);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean updatePortada(int planillaId, String portadaDataUri) throws SQLException {
        String sql = "UPDATE planilla SET portada = ?, portada_actualizada_en = ? WHERE id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, portadaDataUri);
            if (portadaDataUri == null) {
                ps.setNull(2, java.sql.Types.TIMESTAMP);
            } else {
                ps.setTimestamp(2, new java.sql.Timestamp(System.currentTimeMillis()));
            }
            ps.setInt(3, planillaId);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Guarda la configuración RSA de la planilla; con {@code puntos == null} la desactiva
     * y deja las tres columnas en NULL.
     */
    public boolean updateRsa(int planillaId, Integer puntos, java.math.BigDecimal toleranciaValor, String toleranciaUnidad) throws SQLException {
        String sql = "UPDATE planilla SET rsa_puntos = ?, rsa_tolerancia_valor = ?, rsa_tolerancia_unidad = ? WHERE id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (puntos == null) {
                ps.setNull(1, java.sql.Types.INTEGER);
                ps.setNull(2, java.sql.Types.DECIMAL);
                ps.setNull(3, java.sql.Types.VARCHAR);
            } else {
                ps.setInt(1, puntos);
                ps.setBigDecimal(2, toleranciaValor);
                ps.setString(3, toleranciaUnidad);
            }
            ps.setInt(4, planillaId);
            return ps.executeUpdate() == 1;
        }
    }

    public String findPortada(int planillaId) throws SQLException {
        String sql = "SELECT portada FROM planilla WHERE id = ?";
        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, planillaId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    public boolean updateClassroomCourseId(int planillaId, String classroomCourseId) throws SQLException {
        try (Connection con = getCon()) {
            DatabaseMetaData metaData = con.getMetaData();
            try (ResultSet columns = metaData.getColumns(null, null, "planilla", "google_course_id")) {
                if (!columns.next()) {
                    return false;
                }
            }

            String sql = "UPDATE planilla SET google_course_id = ? WHERE id = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, classroomCourseId);
                ps.setInt(2, planillaId);
                return ps.executeUpdate() == 1;
            }
        }
    }

    public int clearClassroomCourseIds() throws SQLException {
        try (Connection con = getCon()) {
            DatabaseMetaData metaData = con.getMetaData();
            try (ResultSet columns = metaData.getColumns(null, null, "planilla", "google_course_id")) {
                if (!columns.next()) {
                    return 0;
                }
            }

            String sql = "UPDATE planilla SET google_course_id = NULL WHERE google_course_id IS NOT NULL";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                return ps.executeUpdate();
            }
        }
    }

    public Planilla fromResultSet(ResultSet rs) throws SQLException {
        if (rs.next()) {
            int planilla_id = rs.getInt("id");
            int curso_id = rs.getInt("curso_id");
            int materia_id = rs.getInt("materia_id");
            String categoria = rs.getString("categoria");
            String nombre = rs.getString("nombre");
            int periodo = rs.getInt("periodo");
            String etapa = rs.getString("etapa");// NOTE might cause problems in the future
            int profesor_id = rs.getInt("profesor_id");
            String googleCourseId = null;
            java.time.LocalDate fechaCierreEtapa1 = null;
            boolean etapa1Confirmada = false;
            java.time.LocalDate fechaCierreEtapa2 = null;
            boolean etapa2Confirmada = false;
            try {
                googleCourseId = rs.getString("google_course_id");
            } catch (SQLException ex) {
                // older schema may not include this column
            }
            try {
                java.sql.Date cierre = rs.getDate("fecha_cierre_etapa1");
                if (cierre != null) {
                    fechaCierreEtapa1 = cierre.toLocalDate();
                }
            } catch (SQLException ex) {
                // older schema may not include this column
            }
            try {
                etapa1Confirmada = rs.getBoolean("etapa1_confirmada");
            } catch (SQLException ex) {
                // older schema may not include this column
            }
            try {
                java.sql.Date cierre2 = rs.getDate("fecha_cierre_etapa2");
                if (cierre2 != null) {
                    fechaCierreEtapa2 = cierre2.toLocalDate();
                }
            } catch (SQLException ex) {
                // older schema may not include this column
            }
            try {
                etapa2Confirmada = rs.getBoolean("etapa2_confirmada");
            } catch (SQLException ex) {
                // older schema may not include this column
            }
            Integer rsaPuntos = null;
            java.math.BigDecimal rsaToleranciaValor = null;
            String rsaToleranciaUnidad = null;
            try {
                int puntos = rs.getInt("rsa_puntos");
                if (!rs.wasNull()) {
                    rsaPuntos = puntos;
                }
                rsaToleranciaValor = rs.getBigDecimal("rsa_tolerancia_valor");
                rsaToleranciaUnidad = rs.getString("rsa_tolerancia_unidad");
            } catch (SQLException ex) {
                // this query (or an older schema) does not include the RSA columns
            }

            Planilla p = new Planilla(planilla_id, curso_id, materia_id, categoria, nombre, periodo, etapa, profesor_id);
            p.setGoogleCourseId(googleCourseId);
            p.setRsaPuntos(rsaPuntos);
            p.setRsaToleranciaValor(rsaToleranciaValor);
            p.setRsaToleranciaUnidad(rsaToleranciaUnidad);
            // Si estamos cargando la fila de Segunda Etapa y la fecha de cierre
            // de Etapa 1 viene nula (porque en la fila de segunda no se persiste),
            // intentar recuperar la fecha real desde la fila de Primera Etapa
            // para el mismo curso+materia. Esto garantiza que el cálculo de
            // `etapaSugerida` sea consistente sin importar qué fila se consulte.
            if (fechaCierreEtapa1 == null && "segunda".equalsIgnoreCase(etapa)) {
                String sqlPrimera = "SELECT fecha_cierre_etapa1, etapa1_confirmada FROM planilla WHERE curso_id = ? AND materia_id = ? AND periodo = ? AND etapa = 'primera' LIMIT 1";
                try (Connection con2 = getCon(); PreparedStatement ps2 = con2.prepareStatement(sqlPrimera)) {
                    ps2.setInt(1, curso_id);
                    ps2.setInt(2, materia_id);
                    ps2.setInt(3, periodo);
                    try (ResultSet rs2 = ps2.executeQuery()) {
                        if (rs2.next()) {
                            java.sql.Date cierre2 = rs2.getDate("fecha_cierre_etapa1");
                            if (cierre2 != null) {
                                fechaCierreEtapa1 = cierre2.toLocalDate();
                            }
                            // No copiar `etapa1_confirmada` desde la fila de Primera Etapa:
                            // dejar el valor original que vino en la fila cargada.
                        }
                    }
                } catch (SQLException ex) {
                    // best-effort: if this lookup fails, keep original null/flag
                }
            }

            p.setFechaCierreEtapa1(fechaCierreEtapa1);
            p.setEtapa1Confirmada(etapa1Confirmada);
            // Etapa 2 lee y escribe únicamente su propia columna: sin el fallback
            // "heredar de primera" que sí aplica a fecha_cierre_etapa1 más arriba.
            p.setFechaCierreEtapa2(fechaCierreEtapa2);
            p.setEtapa2Confirmada(etapa2Confirmada);
            return p;
        } else {
            return null;
        }
    }

    public static class PlanillaInfo {

        private final Planilla planilla;
        private final String materiaNombre;

        public PlanillaInfo(Planilla planilla, String materiaNombre) {
            this.planilla = planilla;
            this.materiaNombre = materiaNombre;
        }

        public Planilla getPlanilla() {
            return planilla;
        }

        public String getMateriaNombre() {
            return materiaNombre;
        }
    }

    public List<PlanillaInfo> findPlanillasByCourse(int especialidadId, int promocion, String seccion, int periodo) throws SQLException, ClassNotFoundException {
        List<PlanillaInfo> out = new ArrayList<>();

        // SQL: join planilla -> curso -> materia
        String sql = "SELECT p.id AS planilla_id, p.curso_id, p.materia_id, p.periodo, p.etapa, p.usuario_id AS profesor_id, m.nombre AS materia_nombre "
                + "FROM planilla p "
                + "JOIN curso c ON p.curso_id = c.id "
                + "JOIN materia m ON p.materia_id = m.id "
                + "WHERE c.especialidad_id = ? AND c.promocion = ? AND c.seccion = ? AND p.periodo = ? "
                + "ORDER BY m.nombre";

        try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, especialidadId);
            ps.setInt(2, promocion);
            ps.setString(3, seccion);
            ps.setInt(4, periodo);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Planilla p = new Planilla();
                    p.setId(rs.getInt("planilla_id"));
                    p.setCursoId(rs.getInt("curso_id"));
                    p.setMateriaId(rs.getInt("materia_id"));
                    p.setPeriodo(rs.getInt("periodo"));
                    p.setEtapa(rs.getString("etapa"));
                    p.setProfesorId(rs.getInt("profesor_id"));
                    String materiaNombre = rs.getString("materia_nombre");
                    out.add(new PlanillaInfo(p, materiaNombre));
                }
            }
        }
        return out;
    }

    // optional: convenience to load planilla + tareas in one call
//    public Planilla findByIdWithTareas(int id) throws SQLException {
//        Planilla p = findById(id);
//        if (p != null) {
//            TareaDao tdao = new TareaDao();
//            p.setTareas(tdao.findByPlanillaId(id));
//        }
//        return p;
//    }
}
