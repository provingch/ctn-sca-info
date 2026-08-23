package ctn.informatica.sca.config;

import ctn.informatica.sca.dao.SchemaMigrationDao;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
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
    private final SchemaMigrationDao migrationDao;

    public DatabaseMigrationInitializer(SchemaMigrationDao migrationDao) {
        this.migrationDao = migrationDao;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        migrationDao.ensureSchema();
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