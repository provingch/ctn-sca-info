package ctn.informatica.sca.integration.gema.service;

import ctn.informatica.sca.clases.conexion;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** {@code SELECT 1} contra la base de SCA, para que GEMA distinga "clave mal" (401) de "SCA sin base" (503). */
@Service
public class GemaPingService {

    private static final Logger log = LoggerFactory.getLogger(GemaPingService.class);

    public boolean isDatabaseUp() {
        try (Connection con = new conexion().getCon(); Statement st = con.createStatement()) {
            st.execute("SELECT 1");
            return true;
        } catch (SQLException ex) {
            log.warn("Ping de integración GEMA: base no disponible: {}", ex.getMessage());
            return false;
        }
    }

    /** Versión del jar empaquetado, o null en desarrollo (sin repackage, no hay manifiesto). */
    public String jarVersion() {
        return getClass().getPackage().getImplementationVersion();
    }
}
