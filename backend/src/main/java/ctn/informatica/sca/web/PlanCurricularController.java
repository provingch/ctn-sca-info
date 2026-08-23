package ctn.informatica.sca.web;

import ctn.informatica.sca.dao.AsignacionDao;
import ctn.informatica.sca.dao.PlanCurricularDao;
import ctn.informatica.sca.dto.PlanCurricularDto;
import ctn.informatica.sca.service.PlanCurricularParser;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.context.SecurityContextHolder;
import ctn.informatica.sca.service.PlanCurricularTemplateBuilder;

@RestController
@RequestMapping("/api/plan-curricular")
public class PlanCurricularController {

    @Autowired
    private PlanCurricularParser parser;

    @Autowired
    private PlanCurricularDao dao;

    @Autowired
    private AsignacionDao asignacionDao;

    @Autowired
    private PlanCurricularTemplateBuilder templateBuilder;

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

    @GetMapping("/plantilla")
    public ResponseEntity<byte[]> plantilla(@RequestParam("asignacionId") int asignacionId) throws Exception {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        long userId = principal instanceof Long ? (Long) principal : Long.parseLong(principal.toString());
        var asignacion = asignacionDao.findById(asignacionId);
        if (asignacion == null) return ResponseEntity.notFound().build();
        if (asignacion.getProfesorId() != (int)userId) return ResponseEntity.status(403).build();
        byte[] data = templateBuilder.buildForAsignacion(asignacionId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "plan-curricular-plantilla.xlsx");
        return ResponseEntity.ok().headers(headers).body(data);
    }

    @GetMapping("/asignaciones-disponibles")
    public ResponseEntity<?> asignacionesDisponibles(@RequestParam("cursoId") int cursoId) throws Exception {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        long userId = principal instanceof Long ? (Long) principal : Long.parseLong(principal.toString());
        var list = asignacionDao.findByProfesorAndCurso((int)userId, cursoId);
        return ResponseEntity.ok(list);
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
