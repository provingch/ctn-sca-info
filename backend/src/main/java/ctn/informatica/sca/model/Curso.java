/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ctn.informatica.sca.model;

import java.text.Normalizer;
import java.util.Locale;

/**
 *
 * @author jonat
 */
public class Curso {

    private int id;
    private String especialidad;
    private int promocion;
    private String seccion;
    private int period = ctn.informatica.sca.util.AcademicPeriod.current();

    public Curso(int id, String especialidad, int promocion, String seccion) {
        this.id = id;
        this.especialidad = especialidad;
        this.promocion = promocion;
        this.seccion = seccion;
    }

    public int getCurso() {
        // promocion = año de egreso. Con period como año actual:
        //  promocion == period       -> 3° (último año, egresa este año)
        //  promocion == period + 1   -> 2°
        //  promocion == period + 2   -> 1° (recién ingresa)
        // Las promociones ya egresadas (promocion < period) se filtran antes,
        // en CursoDao (WHERE ... AND c.promocion >= ?), así que acá no debería
        // llegar ningún caso con curso < 1 en operación normal — el clamp
        // queda como resguardo defensivo, no como comportamiento esperado.
        int curso = period - promocion + 3;
        if (curso < 1) {
            return 1;
        }
        return Math.min(curso, 3);
    }

    public int getNivel() {
        return getCurso();
    }
    
    public String getCursoOrdinal() {
        int cursoInt = getCurso();
        return switch (cursoInt) {
            case 1 -> "1º";
            case 2 -> "2º";
            case 3 -> "3º";
            default -> "Desconocido";
        };
    }

    public boolean matchesCourseKey(ctn.informatica.sca.google.GoogleClassroomUtils.CourseKey courseKey) {
        if (courseKey == null) {
            return false;
        }
        boolean sameLevel = this.getNivel() == courseKey.getNivel();
        boolean sameSection = this.seccion != null && this.seccion.equalsIgnoreCase(courseKey.getSeccion());
        String expectedSpecialty = normalizeValue(courseKey.getSala());
        if (expectedSpecialty.isBlank()) {
            return false;
        }
        boolean sameSpecialty = normalizeValue(this.especialidad).equals(expectedSpecialty);
        return sameLevel && sameSection && sameSpecialty;
    }

    private String normalizeValue(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents.replaceAll("[^\\p{Alnum}]", "").trim().toLowerCase(Locale.ROOT);
    }

    public String getCourseKey() {
        return getNivel() + "-" + (seccion == null ? "" : seccion.toUpperCase());
    }

    @Override
    public String toString() {
        return especialidad + " " + getCursoOrdinal() + " Sección: " + seccion;
    }

    public int getId() {
        return id;
    }

    // getters
    public String getEspecialidad() {
        return especialidad;
    }

    public int getPromocion() {
        return promocion;
    }

    public String getSeccion() {
        return seccion;
    }

    public int getPeriod() {
        return period;
    }

}