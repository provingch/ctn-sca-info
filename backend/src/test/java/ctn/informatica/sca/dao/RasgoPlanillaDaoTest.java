package ctn.informatica.sca.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RasgoPlanillaDaoTest {

    @Test
    void edicionAceptaEstadosExistentesSinConvertirJustificadasEnPresentes() {
        for (String estado : List.of("presente", "ausente", "ausente_justificado", "pendiente")) {
            assertEquals(estado, RasgoPlanillaDao.normalizarEstadoEditable(estado.toUpperCase()));
        }
        assertThrows(IllegalArgumentException.class, () -> RasgoPlanillaDao.normalizarEstadoEditable("otro"));
    }

    @Test
    void edicionConservaJustificacionAntesDeAsignarEstadoYSoportaEsquemasAntiguos() {
        assertEquals("UPDATE rasgo_asistencia SET estado = ? WHERE id = ?",
                RasgoPlanillaDao.buildEdicionUpdateSql(false, false, false));
        assertEquals("UPDATE rasgo_asistencia SET "
                + "falta_codigo = CASE WHEN estado = ? THEN falta_codigo ELSE NULL END, "
                + "falta_observacion = CASE WHEN estado = ? THEN falta_observacion ELSE NULL END, "
                + "responded_at = CASE WHEN estado = ? THEN responded_at ELSE CURRENT_TIMESTAMP END, "
                + "estado = ? WHERE id = ?", RasgoPlanillaDao.buildEdicionUpdateSql(true, true, true));
    }

    @Test
    void aceptaCodigosConductualesValidos() {
        assertEquals(Set.of("N1", "N4", "N8"),
                RasgoPlanillaDao.validarCodigos(List.of("N1", "N4", "N8")));
    }

    @Test
    void rechazaCodigoFueraDelCatalogo() {
        assertThrows(IllegalArgumentException.class,
                () -> RasgoPlanillaDao.validarCodigos(List.of("V")));
        assertThrows(IllegalArgumentException.class,
            () -> RasgoPlanillaDao.validarCodigos(List.of("N1234567890")));
        assertThrows(IllegalArgumentException.class,
            () -> RasgoPlanillaDao.validarCodigos(List.of("")));
        assertThrows(IllegalArgumentException.class,
            () -> RasgoPlanillaDao.validarCodigos(List.of("   ")));
    }

    @Test
    void eliminaDuplicadosAntesDeInsertar() {
        assertEquals(Set.of("N2"),
                RasgoPlanillaDao.validarCodigos(List.of("N2", "n2", "N2")));
    }
}
