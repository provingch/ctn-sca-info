package ctn.informatica.sca;

import ctn.informatica.sca.config.DatabaseMigrationInitializer;
import ctn.informatica.sca.config.RefreshTokenSchemaInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class ScaApplicationTests {

	@MockitoBean
	DatabaseMigrationInitializer databaseMigrationInitializer;

	@MockitoBean
	RefreshTokenSchemaInitializer refreshTokenSchemaInitializer;

	@Test
	void contextLoads() {
	}

}
