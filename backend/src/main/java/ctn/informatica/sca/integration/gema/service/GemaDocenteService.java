package ctn.informatica.sca.integration.gema.service;

import ctn.informatica.sca.dao.ProfesorDao;
import ctn.informatica.sca.integration.gema.dto.DocenteDto;
import ctn.informatica.sca.model.Profesor;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Fase 1: catálogo de docentes, sobre {@link ProfesorDao#findAll()} tal cual (ya excluye padres,
 * nivel 4). Se filtra en memoria por nivel en vez de agregar una consulta nueva: el catálogo de
 * docentes de un colegio es chico, y así no se toca {@code ProfesorDao}.
 */
@Service
public class GemaDocenteService {

    private final ProfesorDao profesorDao;

    public GemaDocenteService(ProfesorDao profesorDao) {
        this.profesorDao = profesorDao;
    }

    public List<DocenteDto> listDocentes(boolean incluirEvaluadores) {
        return profesorDao.findAll().stream()
                .filter(p -> p.getNivel() == 1 || (incluirEvaluadores && p.getNivel() == 2))
                .map(GemaDocenteService::toDto)
                .toList();
    }

    public DocenteDto findByCi(int ci, boolean incluirEvaluadores) {
        return listDocentes(incluirEvaluadores).stream()
                .filter(d -> Objects.equals(d.ci(), ci))
                .findFirst()
                .orElse(null);
    }

    static DocenteDto toDto(Profesor p) {
        return new DocenteDto(p.getId(), p.getCi(), p.getNombre(), p.getApellido(), p.getNivel());
    }
}
