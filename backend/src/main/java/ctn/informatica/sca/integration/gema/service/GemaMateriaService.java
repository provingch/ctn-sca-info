package ctn.informatica.sca.integration.gema.service;

import ctn.informatica.sca.dao.MateriaDao;
import ctn.informatica.sca.integration.gema.dto.MateriaDto;
import ctn.informatica.sca.model.Materia;
import java.sql.SQLException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Fase 1: catálogo de materias, sobre {@link MateriaDao} tal cual (sin tocarlo). */
@Service
public class GemaMateriaService {

    private final MateriaDao materiaDao;

    public GemaMateriaService(MateriaDao materiaDao) {
        this.materiaDao = materiaDao;
    }

    public List<MateriaDto> listMaterias() {
        try {
            List<Materia> materias = materiaDao.listAll();
            List<MateriaDto> out = new java.util.ArrayList<>(materias.size());
            for (Materia m : materias) {
                List<Integer> especialidadIds = materiaDao.listEspecialidadIdsForMateria(m.getId());
                out.add(new MateriaDto(m.getId(), m.getNombre(), m.getCategoria(), especialidadIds));
            }
            return out;
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo consultar el catálogo de materias", ex);
        }
    }
}
