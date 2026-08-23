package ctn.informatica.sca.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RasgoPlanillaDaoTest {

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
                () -> RasgoPlanillaDao.validarCodigos(List.of("N9")));
    }

    @Test
    void eliminaDuplicadosAntesDeInsertar() {
        assertEquals(Set.of("N2"),
                RasgoPlanillaDao.validarCodigos(List.of("N2", "n2", "N2")));
    }
}