/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ctn.informatica.sca.clases;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jonat
 */
public class conexion {

    private String base;
    private String host;
    private String usuario;
    private String contra;
    private Connection con;

    public conexion() {
        this.base = config("CTN_DB_NAME", "ctn.db.name", "ctndb");
        /* name of the database */
        this.host = config("CTN_DB_HOST", "ctn.db.host", "localhost:3306");
        this.usuario = config("CTN_DB_USER", "ctn.db.user", "testadmin");
        this.contra = config("CTN_DB_PASSWORD", "ctn.db.password", "");
    }

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            // fail fast if the driver is missing
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Pool compartido por todas las DAOs (cada una hereda de esta clase y se instancia con
     * {@code new}, asi que el pool tiene que ser estatico). Se indexa por url+usuario+clave
     * para que los setters de host/base sigan funcionando.
     */
    private static final Map<String, HikariDataSource> POOLS = new ConcurrentHashMap<>();

    /**
     * Devuelve una conexion del pool. Los llamadores siguen usando {@code try (Connection c = getCon())}:
     * {@code close()} la devuelve al pool en vez de cerrarla de verdad.
     */
    public Connection getCon() throws SQLException {
        try {
            return pool().getConnection();
        } catch (SQLException ex) {
            Logger.getLogger(conexion.class.getName()).log(Level.SEVERE, "DB connection failed: {0}", ex.getMessage());
            throw ex; // propagate so callers can handle the error instead of getting null
        }
    }

    private HikariDataSource pool() throws SQLException {
        String url = "jdbc:mysql://" + host + "/" + base + "?useUnicode=true&characterEncoding=UTF-8";
        String key = url + "|" + usuario + "|" + contra;
        HikariDataSource existente = POOLS.get(key);
        if (existente != null) {
            return existente;
        }
        synchronized (POOLS) {
            existente = POOLS.get(key);
            if (existente == null) {
                existente = crearPool(url, usuario, contra);
                POOLS.put(key, existente);
            }
            return existente;
        }
    }

    private static HikariDataSource crearPool(String url, String usuario, String contra) throws SQLException {
        HikariConfig cfg = new HikariConfig();
        cfg.setPoolName("sca-db");
        cfg.setJdbcUrl(url);
        cfg.setUsername(usuario);
        cfg.setPassword(contra);
        cfg.setMaximumPoolSize(10);
        cfg.setMinimumIdle(2);
        cfg.setConnectionTimeout(5_000);
        cfg.setIdleTimeout(300_000);
        // Una conexion que no se devuelve al pool se come un cupo para siempre: que quede en el log.
        cfg.setLeakDetectionThreshold(60_000);
        // Con el default (fail-fast) el pool prueba una conexion al crearse; si la base no responde falla
        // enseguida (como el DriverManager de antes) en vez de esperar connectionTimeout, y no queda en el
        // mapa: el proximo getCon() vuelve a intentar.
        try {
            HikariDataSource ds = new HikariDataSource(cfg);
            Logger.getLogger(conexion.class.getName()).log(Level.INFO, "Pool de conexiones creado para {0}", url);
            return ds;
        } catch (RuntimeException ex) {
            Throwable causa = ex.getCause() == null ? ex : ex.getCause();
            throw new SQLException(causa.getMessage(), causa instanceof SQLException sql ? sql.getSQLState() : null, causa);
        }
    }

    /** Cierra los pools (apagado de la JVM o tests). */
    public static void cerrarPools() {
        POOLS.values().forEach(HikariDataSource::close);
        POOLS.clear();
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(conexion::cerrarPools, "sca-db-pool-shutdown"));
    }

    private static String config(String envName, String propertyName, String defaultValue) {
        String value = System.getenv(envName);
        if (value == null || value.isBlank()) {
            value = System.getProperty(propertyName);
        }
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public conexion(String base, String host, String usuario, String contra, Connection con) {
        this.base = base;
        this.host = host;
        this.usuario = usuario;
        this.contra = contra;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getContra() {
        return contra;
    }

    public void setContra(String contra) {
        this.contra = contra;
    }

    public void setCon(Connection con) {
        this.con = con;
    }

}
