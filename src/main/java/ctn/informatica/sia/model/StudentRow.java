/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ctn.informatica.sia.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @author jonat
 */
public class StudentRow {
    private int registroId;              // registro.id
    private int alumnoId;
    private String alumnoNombre;
    private Map<Integer, Integer> grades = new HashMap<>(); // tareaId -> puntos
    private int total;                   // sum of puntos
    private int porcentaje;              // rounded percentage of totalPossiblePoints
    private int nota;                    // final grade (can be different from porcentaje)

    public StudentRow() {
    }

    public StudentRow(int registroId, int alumnoId, String alumnoNombre) {
        this.registroId = registroId;
        this.alumnoId = alumnoId;
        this.alumnoNombre = alumnoNombre;
    }

    public int getRegistroId() {
        return registroId;
    }

    public void setRegistroId(int registroId) {
        this.registroId = registroId;
    }

    public int getAlumnoId() {
        return alumnoId;
    }

    public void setAlumnoId(int alumnoId) {
        this.alumnoId = alumnoId;
    }

    public String getAlumnoNombre() {
        return alumnoNombre;
    }

    public void setAlumnoNombre(String alumnoNombre) {
        this.alumnoNombre = alumnoNombre;
    }

    public Map<Integer, Integer> getGrades() {
        return grades;
    }

    public void setGrades(Map<Integer, Integer> grades) {
        this.grades = grades;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getPorcentaje() {
        return porcentaje;
    }

    public void setPorcentaje(int porcentaje) {
        this.porcentaje = porcentaje;
    }

    public int getNota() {
        return nota;
    }

    public void setNota(int nota) {
        this.nota = nota;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StudentRow)) return false;
        StudentRow that = (StudentRow) o;
        return registroId == that.registroId &&
               alumnoId == that.alumnoId &&
               Objects.equals(alumnoNombre, that.alumnoNombre);
    }

    @Override
    public int hashCode() {
        return Objects.hash(registroId, alumnoId, alumnoNombre);
    }

    @Override
    public String toString() {
        return "StudentRow{" +
               "registroId=" + registroId +
               ", alumnoId=" + alumnoId +
               ", alumnoNombre='" + alumnoNombre + '\'' +
               ", grades=" + grades +
               ", total=" + total +
               ", porcentaje=" + porcentaje +
               ", nota=" + nota +
               '}';
    }
}
