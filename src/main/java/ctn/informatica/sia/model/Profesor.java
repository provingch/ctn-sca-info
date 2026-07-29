/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ctn.informatica.sia.model;

/**
 *
 * @author jonat
 */
public class Profesor {

    private int id;
    private String nombre;
    private String apellido;
    private String usuario;
    private String contrasenia;
    private Integer ci;
    private Integer telefono;
    private Integer celular;
    private String correo;
    private Integer especialidadId;
    private int nivel;

    private String googleEmail;
    private String gcAccessToken;
    private String gcRefreshToken;
    private long gcTokenExpiry;
    private String totpSecret;

    // getters y setters
    public String getGoogleEmail() { return googleEmail; }
    public void setGoogleEmail(String googleEmail) { this.googleEmail = googleEmail; }

    public String getGcAccessToken() { return gcAccessToken; }
    public void setGcAccessToken(String gcAccessToken) { this.gcAccessToken = gcAccessToken; }

    public String getGcRefreshToken() { return gcRefreshToken; }
    public void setGcRefreshToken(String gcRefreshToken) { this.gcRefreshToken = gcRefreshToken; }

    public long getGcTokenExpiry() { return gcTokenExpiry; }
    public void setGcTokenExpiry(long gcTokenExpiry) { this.gcTokenExpiry = gcTokenExpiry; }

    public String getTotpSecret() { return totpSecret; }
    public void setTotpSecret(String totpSecret) { this.totpSecret = totpSecret; }

    public String getFullName() {
        String n = nombre == null ? "" : nombre;
        String a = apellido == null ? "" : apellido;
        return (n + " " + a).trim();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getContrasenia() {
        return contrasenia;
    }

    public void setContrasenia(String contrasenia) {
        this.contrasenia = contrasenia;
    }

    public Integer getCi() {
        return ci;
    }

    public void setCi(Integer ci) {
        this.ci = ci;
    }

    public Integer getTelefono() {
        return telefono;
    }

    public void setTelefono(Integer telefono) {
        this.telefono = telefono;
    }

    public Integer getCelular() {
        return celular;
    }

    public void setCelular(Integer celular) {
        this.celular = celular;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public Integer getEspecialidadId() {
        return especialidadId;
    }

    public void setEspecialidadId(Integer especialidadId) {
        this.especialidadId = especialidadId;
    }

    public int getNivel() {
        return nivel;
    }

    public void setNivel(int nivel) {
        this.nivel = nivel;
    }

}
