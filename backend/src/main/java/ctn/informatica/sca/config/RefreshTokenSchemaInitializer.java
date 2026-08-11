package ctn.informatica.sca.config;

import ctn.informatica.sca.dao.RefreshTokenDao;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Ensures reload/remember-me sessions work on deployments missing the legacy migration. */
@Component
public class RefreshTokenSchemaInitializer implements ApplicationRunner {

    private final RefreshTokenDao refreshTokenDao;

    public RefreshTokenSchemaInitializer(RefreshTokenDao refreshTokenDao) {
        this.refreshTokenDao = refreshTokenDao;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        refreshTokenDao.ensureSchema();
    }
}
