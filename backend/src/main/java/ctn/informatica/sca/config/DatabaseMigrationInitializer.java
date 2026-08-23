package ctn.informatica.sca.config;

import ctn.informatica.sca.dao.SchemaMigrationDao;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMigrationInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationInitializer.class);
    private static final Map<String, String> LEGACY_VERSIONS = Map.of(
            "00X_sync_rol_nivel.sql", "001_sync_rol_nivel.sql",
            "00Y_drop_rol_usuario.sql", "002_drop_rol_usuario.sql",
            "00Z_drop_especialidad_usuario.sql", "003_drop_especialidad_usuario.sql",
            "00AA_complete_especialidades_cursos.sql", "004_complete_especialidades_cursos.sql",
            "00AB_rasgo_asistencia_codigos.sql", "005_rasgo_asistencia_codigos.sql");
    private final SchemaMigrationDao migrationDao;

    public DatabaseMigrationInitializer(SchemaMigrationDao migrationDao) {
        this.migrationDao = migrationDao;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        migrationDao.ensureSchema();
        for (Map.Entry<String, String> entry : LEGACY_VERSIONS.entrySet()) {
            migrationDao.renameVersion(entry.getKey(), entry.getValue());
        }
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath*:db/migrations/*.sql");
        Arrays.sort(resources, Comparator.comparing(resource -> resource.getFilename() == null ? "" : resource.getFilename()));
        for (Resource resource : resources) {
            String version = resource.getFilename();
            if (version == null || migrationDao.isApplied(version)) continue;
            try {
                log.info("Aplicando migración de base de datos {}", version);
                migrationDao.executeAndRecord(version, resource.getContentAsString(StandardCharsets.UTF_8));
            } catch (IOException | RuntimeException ex) {
                log.error("No se pudo leer la migración {}", version, ex);
                throw ex;
            } catch (Exception ex) {
                log.error("Falló la migración {}. El backend no continuará arrancando.", version, ex);
                throw ex;
            }
        }
    }
}