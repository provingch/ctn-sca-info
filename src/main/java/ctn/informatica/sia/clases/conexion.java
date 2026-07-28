/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ctn.informatica.sia.clases;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
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

    public Connection getCon() throws SQLException {
        String url = "jdbc:mysql://" + host + "/" + base + "?useUnicode=true&characterEncoding=UTF-8";
        try {
            con = DriverManager.getConnection(url, this.usuario, this.contra);
            Logger.getLogger(conexion.class.getName()).log(Level.INFO, "DB connected to {0}/{1}", new Object[]{host, base});
            return con;
        } catch (SQLException ex) {
            Logger.getLogger(conexion.class.getName()).log(Level.SEVERE, "DB connection failed: {0}", ex.getMessage());
            throw ex; // propagate so callers can handle the error instead of getting null
        }
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
