package ctn.informatica.sca.integration.coresync;

import java.sql.SQLException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/core/snapshot")
public class CoreSnapshotController {

    private final CoreSnapshotDao dao;

    public CoreSnapshotController(CoreSnapshotDao dao) {
        this.dao = dao;
    }

    @GetMapping
    public CoreSnapshot snapshot() throws SQLException {
        return dao.exportar();
    }
}
