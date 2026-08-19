package ctn.informatica.sca;

import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ScaApplication {

	public static void main(String[] args) {
		// Ensure BouncyCastle provider is available for webpush crypto operations
		try {
			Security.addProvider(new BouncyCastleProvider());
		} catch (Exception ignore) {
			// provider may already be present or addition may fail in restricted environments
		}
		SpringApplication.run(ScaApplication.class, args);
	}

}
