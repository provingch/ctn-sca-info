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
}
