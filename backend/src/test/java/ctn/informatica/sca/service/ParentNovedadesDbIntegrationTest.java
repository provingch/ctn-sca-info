package ctn.informatica.sca.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ctn.informatica.sca.clases.conexion;
import ctn.informatica.sca.controller.ParentController;
import ctn.informatica.sca.dao.NotificacionDao;
import ctn.informatica.sca.dao.PadreDao;
import ctn.informatica.sca.util.FirebaseMessagingClient;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

/**
 * Recordatorio diario a padres — "marcar visto" y el ciclo de dedup/recreación de la fila
 * NOVEDAD_ALUMNO, contra una base real. Sólo corre con {@code SCA_IT_DB=1}.
 */
class ParentNovedadesDbIntegrationTest {

    private static final int ESPECIALIDAD_ID = 9_000_060;

    private final ParentController controller = new ParentController();
    private final NotificacionDao notificacionDao = new NotificacionDao();
    private final PadreDao padreDao = new PadreDao();
    private final ParentPushService parentPushService = new ParentPushService(
            new FirebaseMessagingClient(), padreDao, notificacionDao);

    private int padre1Id;
    private int padre2Id;
    private int alumno1Id;
    private int alumno2Id;

    @BeforeEach
    void seed() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue("1".equals(System.getenv("SCA_IT_DB")),
                "Definí SCA_IT_DB=1 (con CTN_DB_* de una base descartable) para correr esto");
        try (Connection c = new conexion().getCon()) {
            // ok
        } catch (SQLException ex) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "Sin base de integración: " + ex.getMessage());
        }
        limpiar();
        exec("INSERT INTO especialidad (id, nombre) VALUES (" + ESPECIALIDAD_ID + ", 'IT Especialidad Novedades')");
        int cursoId = insertar("INSERT INTO curso (especialidad_id, promocion, seccion) VALUES (" + ESPECIALIDAD_ID + ", 2028, 'A')");
        padre1Id = insertar("INSERT INTO usuario (usuario, nombre, apellido, contrasenia, nivel) VALUES ('it_nov_padre1', 'Pedro', 'Padre', 'x', 4)");
        padre2Id = insertar("INSERT INTO usuario (usuario, nombre, apellido, contrasenia, nivel) VALUES ('it_nov_padre2', 'Petra', 'Madre', 'x', 4)");
        alumno1Id = insertar("INSERT INTO alumno (nombre, apellido, curso_id) VALUES ('Hijo', 'Uno', " + cursoId + ")");
        alumno2Id = insertar("INSERT INTO alumno (nombre, apellido, curso_id) VALUES ('Hijo', 'Dos', " + cursoId + ")");
        padreDao.linkPadre(alumno1Id, padre1Id);
        padreDao.linkPadre(alumno2Id, padre2Id);
    }

    @AfterEach
    void limpiar() throws Exception {
        if (!"1".equals(System.getenv("SCA_IT_DB"))) {
            return;
        }
        try {
            exec("DELETE FROM notificacion WHERE usuario_id IN (SELECT id FROM usuario WHERE usuario IN ('it_nov_padre1','it_nov_padre2'))");
            exec("DELETE FROM alumno_usuario WHERE usuario_id IN (SELECT id FROM usuario WHERE usuario IN ('it_nov_padre1','it_nov_padre2'))");
            exec("DELETE FROM alumno WHERE curso_id IN (SELECT id FROM curso WHERE especialidad_id = " + ESPECIALIDAD_ID + ")");
            exec("DELETE FROM usuario WHERE usuario IN ('it_nov_padre1','it_nov_padre2')");
            exec("DELETE FROM curso WHERE especialidad_id = " + ESPECIALIDAD_ID);
            exec("DELETE FROM especialidad WHERE id = " + ESPECIALIDAD_ID);
        } catch (SQLException ignored) {
            // sin base: el @BeforeEach ya hizo skip
        }
    }

    private static void exec(String sql) throws SQLException {
        try (Connection c = new conexion().getCon(); Statement st = c.createStatement()) {
            st.executeUpdate(sql);
        }
    }

    private static int insertar(String sql) throws SQLException {
        try (Connection c = new conexion().getCon(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private static String scalar(String sql) throws SQLException {
        try (Connection c = new conexion().getCon(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    private UsernamePasswordAuthenticationToken como(int usuarioId) {
        return new UsernamePasswordAuthenticationToken((long) usuarioId, null, List.of());
    }

    private String contarNoLeidas(int usuarioId, int alumnoId) throws SQLException {
        return scalar("SELECT COUNT(*) FROM notificacion WHERE usuario_id = " + usuarioId
                + " AND entidad_id = " + alumnoId + " AND tipo = 'NOVEDAD_ALUMNO' AND leida = 0");
    }

    @Test
    void marcarVistoCierraSoloLaNovedadDelPadreYAlumnoIndicados() throws Exception {
        parentPushService.registrarNovedadAlumnos(Set.of(alumno1Id));
        parentPushService.registrarNovedadAlumnos(Set.of(alumno2Id));
        assertEquals("1", contarNoLeidas(padre1Id, alumno1Id));
        assertEquals("1", contarNoLeidas(padre2Id, alumno2Id));

        var resultado = controller.marcarVisto(alumno1Id, como(padre1Id));

        assertEquals(true, resultado.get("ok"));
        assertEquals(1, resultado.get("actualizadas"));
        assertEquals("0", contarNoLeidas(padre1Id, alumno1Id));
        // La novedad del otro padre/alumno no se toca.
        assertEquals("1", contarNoLeidas(padre2Id, alumno2Id));
    }

    @Test
    void unPadreNoPuedeMarcarComoVistoUnHijoAjeno() throws Exception {
        parentPushService.registrarNovedadAlumnos(Set.of(alumno2Id));
        assertEquals("1", contarNoLeidas(padre2Id, alumno2Id));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.marcarVisto(alumno2Id, como(padre1Id)));

        assertEquals(403, ex.getStatusCode().value());
        // La novedad ajena sigue sin leer: el intento fallido no la tocó.
        assertEquals("1", contarNoLeidas(padre2Id, alumno2Id));
    }

    @Test
    void despuesDeMarcarVistoUnaNuevaNovedadVuelveACrearFila() throws Exception {
        parentPushService.registrarNovedadAlumnos(Set.of(alumno1Id));
        controller.marcarVisto(alumno1Id, como(padre1Id));
        assertEquals("0", contarNoLeidas(padre1Id, alumno1Id));

        parentPushService.registrarNovedadAlumnos(Set.of(alumno1Id));

        assertEquals("1", contarNoLeidas(padre1Id, alumno1Id));
    }
}
