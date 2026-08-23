package ctn.informatica.sca.web;

import ctn.informatica.sca.dao.AsignacionDao;
import ctn.informatica.sca.dao.PlanCurricularDao;
import ctn.informatica.sca.dto.PlanCurricularDto;
import ctn.informatica.sca.service.PlanCurricularParser;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api/plan-curricular")
public class PlanCurricularController {

    @Autowired
    private PlanCurricularParser parser;

    @Autowired
    private PlanCurricularDao dao;

    @Autowired
    private AsignacionDao asignacionDao;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam("asignacionId") int asignacionId, @RequestParam("file") MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) return ResponseEntity.badRequest().body("Archivo requerido");
        PlanCurricularDto dto;
        try {
            dto = parser.parse(file.getInputStream());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
        // validate header matches asignacion
        var asignacion = asignacionDao.findById(asignacionId);
        if (asignacion == null) return ResponseEntity.badRequest().body("Asignación inexistente");
        if (dto.disciplina != null && !dto.disciplina.equalsIgnoreCase(asignacion.getMateriaNombre()))
            return ResponseEntity.badRequest().body("Disciplina no coincide con la asignación");
        if (dto.especialidad != null && asignacion.getEspecialidad() != null && !dto.especialidad.equalsIgnoreCase(asignacion.getEspecialidad()))
            return ResponseEntity.badRequest().body("Especialidad no coincide con la asignación");
        if (dto.seccion != null && asignacion.getCursoSeccion() != null && !dto.seccion.equalsIgnoreCase(asignacion.getCursoSeccion()))
            return ResponseEntity.badRequest().body("Sección no coincide con la asignación");

        // save
        byte[] content = file.getBytes();
        int id = dao.saveOrReplace(asignacionId, dto.etapa, dto.anio, file.getOriginalFilename(), content, dto.temas);
        return ResponseEntity.ok().body("Saved plan id:" + id);
    }

    @PostMapping("/{id}/aprobar")
    public ResponseEntity<?> aprobar(@PathVariable int id, @RequestParam("evaluadorId") int evaluadorId) throws Exception {
        dao.aprobar(id, evaluadorId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/rechazar")
    public ResponseEntity<?> rechazar(@PathVariable int id, @RequestParam("evaluadorId") int evaluadorId, @RequestParam("observaciones") String observaciones) throws Exception {
        if (observaciones == null || observaciones.isBlank()) return ResponseEntity.badRequest().body("Observaciones requeridas");
        dao.rechazar(id, evaluadorId, observaciones);
        return ResponseEntity.ok().build();
    }
}
