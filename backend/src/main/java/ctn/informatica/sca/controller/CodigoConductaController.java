package ctn.informatica.sca.controller;

import ctn.informatica.sca.dao.CodigoConductaDao;
import ctn.informatica.sca.model.CodigoConducta;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/codigos-conducta")
public class CodigoConductaController {

    private final CodigoConductaDao dao;

    public CodigoConductaController(CodigoConductaDao dao) {
        this.dao = dao;
    }

    @GetMapping
    public List<CodigoConducta> listar() {
        try {
            return dao.listarActivos();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudieron cargar los códigos de conducta.", ex);
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('LEVEL_2','LEVEL_3')")
    public CodigoConducta crear(@RequestBody CodigoConductaRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Se requiere código y descripción.");
        }
        try {
            return dao.crear(request.codigo(), request.descripcion());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo crear el código de conducta.", ex);
        }
    }

    @PostMapping("/{id}/desactivar")
    @PreAuthorize("hasAnyRole('LEVEL_2','LEVEL_3')")
    public void desactivar(@PathVariable int id) {
        try {
            if (!dao.desactivar(id)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Código de conducta no encontrado o ya inactivo.");
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo desactivar el código de conducta.", ex);
        }
    }

    public record CodigoConductaRequest(String codigo, String descripcion) {
    }
}
