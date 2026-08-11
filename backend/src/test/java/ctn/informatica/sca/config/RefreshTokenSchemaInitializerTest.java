package ctn.informatica.sca.config;

import static org.mockito.Mockito.verify;

import ctn.informatica.sca.dao.RefreshTokenDao;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RefreshTokenSchemaInitializerTest {

    @Test
    void createsRefreshTokenSchemaAtStartup() throws Exception {
        RefreshTokenDao dao = Mockito.mock(RefreshTokenDao.class);

        new RefreshTokenSchemaInitializer(dao).run(null);

        verify(dao).ensureSchema();
    }
}
