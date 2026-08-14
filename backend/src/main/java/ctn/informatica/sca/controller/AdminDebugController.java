package ctn.informatica.sca.controller;

import ctn.informatica.sca.dao.TareaDao;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/debug")
public class AdminDebugController {

    @PostMapping("/wipe-tareas")
    public ResponseEntity<Map<String, Object>> wipeTareas(@RequestParam(required = false) Integer planillaId) {
        TareaDao dao = new TareaDao();
        Map<String, Object> result = new HashMap<>();
        try {
            int deleted = dao.deleteImportedTasks(planillaId);
            result.put("deleted", deleted);
            result.put("scope", planillaId == null ? "all_imported" : "planilla_id");
            return ResponseEntity.ok(result);
        } catch (SQLException ex) {
            result.put("error", ex.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
}
