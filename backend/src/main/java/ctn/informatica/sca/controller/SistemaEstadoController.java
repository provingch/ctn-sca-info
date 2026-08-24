package ctn.informatica.sca.controller;

import ctn.informatica.sca.dao.ClassroomSyncLogDao;
import ctn.informatica.sca.dao.SchemaMigrationDao;
import ctn.informatica.sca.dto.MigracionDto;
import ctn.informatica.sca.dto.SistemaEstadoDto;
import ctn.informatica.sca.service.ActivityLogService;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/sistema-estado")
public class SistemaEstadoController {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final ActivityLogService activityLogService;

    @Autowired
    public SistemaEstadoController(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LEVEL_3')")
    public SistemaEstadoDto estado(Authentication auth) {
        ApiAuth.requireUserId(auth);
        try {
            SchemaMigrationDao schemaMigrationDao = new SchemaMigrationDao();
            boolean dbConectada = schemaMigrationDao.dbConectada();
            List<MigracionDto> migraciones = schemaMigrationDao.listApplied().stream()
                    .map(item -> new MigracionDto(item.version(), item.appliedAt() == null ? null : FORMATTER.format(item.appliedAt().toInstant())))
                    .toList();

            Timestamp lastSync = new ClassroomSyncLogDao().getLastSyncedAt();
            String ultimaSyncClassroom = lastSync == null ? null : FORMATTER.format(lastSync.toInstant());
            long espacioLogsBytes = activityLogService == null ? 0L : activityLogService.getTotalBytes();
            return new SistemaEstadoDto(dbConectada, migraciones, ultimaSyncClassroom, espacioLogsBytes);
        } catch (Exception ex) {
            throw new RuntimeException("No se pudo cargar el estado del sistema", ex);
        }
    }
}
