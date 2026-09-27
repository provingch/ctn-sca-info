package ctn.informatica.sca.integration.gema.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ctn.informatica.sca.dao.ProfesorDao;
import ctn.informatica.sca.integration.gema.dto.DocenteDto;
import ctn.informatica.sca.model.Profesor;
import java.util.List;
import org.junit.jupiter.api.Test;

class GemaDocenteServiceTest {

    private static Profesor profesor(int id, int ci, int nivel) {
        Profesor p = new Profesor();
        p.setId(id);
        p.setCi(ci);
        p.setNombre("Ana");
        p.setApellido("Pérez");
        p.setNivel(nivel);
        p.setContrasenia("$2a$10$hashSecreto");
        p.setTotpSecret("secreto-totp");
        p.setGcAccessToken("token-google");
        return p;
    }

    @Test
    void porDefectoSoloDocentesNivel1() {
        ProfesorDao dao = mock(ProfesorDao.class);
        when(dao.findAll()).thenReturn(List.of(profesor(1, 100, 1), profesor(2, 200, 2), profesor(3, 300, 3)));
        GemaDocenteService service = new GemaDocenteService(dao);

        List<DocenteDto> result = service.listDocentes(false);

        assertEquals(1, result.size());
        assertEquals(100, result.get(0).ci());
    }

    @Test
    void incluirEvaluadoresSumaNivel2() {
        ProfesorDao dao = mock(ProfesorDao.class);
        when(dao.findAll()).thenReturn(List.of(profesor(1, 100, 1), profesor(2, 200, 2), profesor(3, 300, 3)));
        GemaDocenteService service = new GemaDocenteService(dao);

        List<DocenteDto> result = service.listDocentes(true);

        assertEquals(2, result.size());
    }

    @Test
    void findByCiSinCoincidenciaDevuelveNull() {
        ProfesorDao dao = mock(ProfesorDao.class);
        when(dao.findAll()).thenReturn(List.of(profesor(1, 100, 1)));
        GemaDocenteService service = new GemaDocenteService(dao);

        assertNull(service.findByCi(999, false));
    }

    @Test
    void elDtoNuncaExponeContraseniaNiTotpNiTokens() {
        Profesor sensible = profesor(1, 100, 1);
        DocenteDto dto = GemaDocenteService.toDto(sensible);
        String json = dto.toString();

        assertTrue(dto.ci() == 100 && dto.nombre().equals("Ana"));
        assertTrue(!json.contains("hashSecreto") && !json.contains("secreto-totp") && !json.contains("token-google"),
                "El DTO no debe contener campos sensibles del modelo Profesor");
    }
}
