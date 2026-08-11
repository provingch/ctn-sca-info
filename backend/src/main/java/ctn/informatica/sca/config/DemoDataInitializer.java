package ctn.informatica.sca.config;

import ctn.informatica.sca.clases.conexion;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Loads the opt-in, non-destructive demo dataset using the application's DB connection. */
@Component
@Order(100)
@ConditionalOnProperty(name = "sca.demo-data.enabled", havingValue = "true")
public class DemoDataInitializer implements ApplicationRunner {

    private static final Logger LOGGER = Logger.getLogger(DemoDataInitializer.class.getName());
    private static final String SCRIPT = "db/seed-pruebas-seguro.sql";

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<String> statements = loadStatements();

        try (Connection connection = new conexion().getCon();
             Statement statement = connection.createStatement()) {
            try {
                for (String sql : statements) {
                    statement.execute(sql);
                }
                LOGGER.log(Level.INFO, "Demo data verified/loaded from {0}", SCRIPT);
            } catch (SQLException ex) {
                rollbackQuietly(connection);
                throw new SQLException("No se pudo cargar el seed de demostración " + SCRIPT, ex);
            }
        }
    }

    private List<String> loadStatements() throws IOException {
        ClassPathResource resource = new ClassPathResource(SCRIPT);
        try (InputStream input = resource.getInputStream();
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(input, StandardCharsets.UTF_8))) {
            StringBuilder script = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.stripLeading().startsWith("--")) {
                    script.append(line).append('\n');
                }
            }
            return splitStatements(script.toString());
        }
    }

    static List<String> splitStatements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean escaped = false;

        for (int i = 0; i < script.length(); i++) {
            char character = script.charAt(i);
            if (inSingleQuote && character == '\\' && !escaped) {
                escaped = true;
                current.append(character);
                continue;
            }
            if (character == '\'' && !escaped) {
                if (inSingleQuote && i + 1 < script.length() && script.charAt(i + 1) == '\'') {
                    current.append("''");
                    i++;
                    continue;
                }
                inSingleQuote = !inSingleQuote;
            }
            if (character == ';' && !inSingleQuote) {
                addStatement(statements, current);
            } else {
                current.append(character);
            }
            escaped = false;
        }
        addStatement(statements, current);
        return statements;
    }

    private static void addStatement(List<String> statements, StringBuilder current) {
        String sql = current.toString().trim();
        if (!sql.isEmpty()) {
            statements.add(sql);
        }
        current.setLength(0);
    }

    private void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            LOGGER.log(Level.WARNING, "No se pudo revertir la carga de datos demo", rollbackError);
        }
    }
}
