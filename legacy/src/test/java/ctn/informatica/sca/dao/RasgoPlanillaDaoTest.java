package ctn.informatica.sca.dao;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RasgoPlanillaDaoTest {

    @Test
    void buildInsertAsistenciaSqlOmitsOptionalColumnsWhenMissing() {
        String sql = RasgoPlanillaDao.buildInsertAsistenciaSql(false, false);

        assertTrue(sql.contains("INSERT INTO rasgo_asistencia"));
        assertFalse(sql.contains("falta_codigo"));
        assertFalse(sql.contains("falta_observacion"));
    }

    @Test
    void buildPlanillaListSqlIncludesOptionalColumnsWhenSupported() {
        String sql = RasgoPlanillaDao.buildPlanillaListSql(true, true);

        assertTrue(sql.contains("fecha_clase"));
        assertTrue(sql.contains("created_at"));
    }

    @Test
    void buildPlanillaListSqlForCourseFilteringHasNoProfesorConstraint() {
        String sql = RasgoPlanillaDao.buildPlanillaListSql(true, true, false);

        assertTrue(sql.contains("FROM planilla_rasgo WHERE"));
        assertFalse(sql.contains("profesor_id = ?"));
        assertTrue(sql.contains("curso_id = ?"));
    }

    @Test
    void buildRespuestaUpdateSqlOmitsMissingOptionalColumns() {
        String sql = RasgoPlanillaDao.buildRespuestaUpdateSql(false, false, false);

        assertTrue(sql.contains("UPDATE rasgo_asistencia"));
        assertFalse(sql.contains("falta_codigo"));
        assertFalse(sql.contains("falta_observacion"));
        assertFalse(sql.contains("responded_at"));
    }
}
