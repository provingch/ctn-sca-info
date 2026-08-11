package ctn.informatica.sca.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class DemoDataInitializerTest {

    @Test
    void splitsStatementsWithoutBreakingQuotedSemicolonsOrEscapedQuotes() {
        String sql = """
                INSERT INTO ejemplo (texto) VALUES ('uno;dos');
                INSERT INTO ejemplo (texto) VALUES ('l''apóstrofe');
                SELECT 1;
                """;

        List<String> statements = DemoDataInitializer.splitStatements(sql);

        assertEquals(3, statements.size());
        assertEquals("INSERT INTO ejemplo (texto) VALUES ('uno;dos')", statements.get(0));
        assertEquals("INSERT INTO ejemplo (texto) VALUES ('l''apóstrofe')", statements.get(1));
        assertEquals("SELECT 1", statements.get(2));
    }
}
