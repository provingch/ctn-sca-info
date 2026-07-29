/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ctn.informatica.sia.model;

import ctn.informatica.sia.dao.CursoDao;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jonat
 */
public class Planilla{

    private int id;
    private int cursoId;
    private int materiaId;
    private String nombre;
    private int periodo;
    private String etapa;
    private int profesorId;
    private int tareasCount;
    private String ultimaTarea;
    private String categoria;
    private double exigencia;
    private String googleCourseId;

    private Map<Integer, int[]> gradeRanges;
    private int limiteInferior;   // li
    private int limiteSuperior;   // ls (== totalPossiblePoints)

    public Planilla(int id, int cursoId, int materiaId, String nombre, int periodo, String etapa, int profesorId, int tareasCount, String ultimaTarea) {
        this.id = id;
        this.cursoId = cursoId;
        this.materiaId = materiaId;
        this.nombre = nombre;
        this.periodo = periodo;
        this.etapa = etapa;
        this.profesorId = profesorId;
        this.tareasCount = tareasCount;
        this.ultimaTarea = ultimaTarea;
    }

    public Planilla(int id, int cursoId, int materiaId, String categoria, String nombre, int periodo, String etapa, int profesorId) {
        this.id = id;
        this.cursoId = cursoId;
        this.materiaId = materiaId;
        this.categoria = categoria;
        this.nombre = nombre;
        this.periodo = periodo;
        this.etapa = etapa;
        this.profesorId = profesorId;
    }

    public Planilla() {
    }

    public int getEtapaIndex() {
        return switch (this.etapa) {
            case "primera" ->
                1;
            case "segunda" ->
                2;
            default ->
                1;
        };
    }

    public double getExigencia(String categoria) {
        return switch (categoria) {
            case "comun" ->
                .7;
            case "especifico" ->
                .8;
            default ->
                .7;
        };
    }

    public void computeGradeRanges(int totalPossiblePoints) {
        this.limiteSuperior = Math.max(0, totalPossiblePoints);
        // compute lower limit (li) as ceil(exigencia * totalPossiblePoints)
        this.exigencia = getExigencia(categoria);
        int li = (int) Math.ceil(this.exigencia * totalPossiblePoints);
        if (li < 1) {
            li = 1;
        }
        this.limiteInferior = li;

        gradeRanges = new LinkedHashMap<>();

        int ls = this.limiteSuperior;
        if (li > ls) {
            // degenerate case: no interval; put empty intervals and make 5 start=li..ls
            gradeRanges.put(2, new int[]{li, li - 1}); // empty
            gradeRanges.put(3, new int[]{li, li - 1});
            gradeRanges.put(4, new int[]{li, li - 1});
            gradeRanges.put(5, new int[]{li, ls});
            return;
        }

        int inclusiveCount = ls - li + 1; // number of point-values to split among 4 grades
        int base = inclusiveCount / 4;
        int rem = inclusiveCount % 4;

        int c2 = base, c3 = base, c4 = base, c5 = base;
        // residue distribution per the doc:
        switch (rem) {
            case 1:
                c3 += 1;
                break;
            case 2:
                c3 += 1;
                c4 += 1;
                break;
            case 3:
                c3 += 1;
                c4 += 1;
                c2 += 1;
                break;
            default:
                break;
        }

        int start = li;
        int end2 = start + c2 - 1;           // end for grade 2
        int start3 = end2 + 1;
        int end3 = start3 + c3 - 1;         // end for grade 3
        int start4 = end3 + 1;
        int end4 = start4 + c4 - 1;         // end for grade 4
        int start5 = end4 + 1;
        int end5 = ls;                      // end for grade 5

        gradeRanges.put(2, new int[]{li, Math.max(li, end2)});
        gradeRanges.put(3, new int[]{Math.max(start3, li), Math.max(start3, end3)});
        gradeRanges.put(4, new int[]{Math.max(start4, li), Math.max(start4, end4)});
        gradeRanges.put(5, new int[]{Math.max(start5, li), end5});
    }

    @Override
    public String toString() {
        Curso c = null;
        try {
            c = new CursoDao().findById(cursoId);
        } catch (SQLException ex) {
            Logger.getLogger(Planilla.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (c != null){
            return nombre + " " + c.getEspecialidad() + " " + c.getCursoOrdinal() + " " + c.getSeccion();
        } else {
            return null;
        }
    }
    
    /**
     * Return the computed ranges map (may be null until computeGradeRanges is
     * called).
     */
    public Map<Integer, int[]> getGradeRanges() {
        return gradeRanges;
    }

    /**
     * Return the lower limit li (or 0 if not computed).
     */
    public int getLimiteInferior() {
        return limiteInferior;
    }

    /**
     * Return the upper limit ls (or 0 if not computed).
     */
    public int getLimiteSuperior() {
        return limiteSuperior;
    }

    /**
     * Convenience: determine nota (1..5) for a given accumulated points 'sum'
     * using the stored ranges. If ranges haven't been computed this falls back
     * to 1 for safety.
     */
    public int getNotaForSum(int sum) {
        if (gradeRanges == null) {
            // fallback behavior if computeGradeRanges wasn't called
            return (sum >= limiteInferior && limiteInferior > 0) ? 5 : 1;
        }
        // below the minimum -> 1
        if (sum < limiteInferior) {
            return 1;
        }

        // check grades 2..5
        for (int grade = 2; grade <= 5; grade++) {
            int[] r = gradeRanges.get(grade);
            if (r == null) {
                continue;
            }
            int s = r[0], e = r[1];
            if (s <= e && sum >= s && sum <= e) {
                return grade;
            }
        }
        // if sum bigger than top range, return 5
        return 5;
    }

    public Map<String, Integer[]> getGradeRangesForJsp() {
        Map<String, Integer[]> gradeRangesForJsp = new LinkedHashMap<>();

        if (gradeRanges != null) {
            for (Map.Entry<Integer, int[]> e : gradeRanges.entrySet()) {
                Integer key = e.getKey();
                int[] arr = e.getValue();
                if (arr == null) {
                    continue;
                }
                Integer[] boxed = new Integer[arr.length];
                for (int i = 0; i < arr.length; i++) {
                    boxed[i] = arr[i];
                }
                gradeRangesForJsp.put(String.valueOf(key), boxed);
            }
            return gradeRangesForJsp;
        }
        return null;
    }

    // make sure to have all the getters!
    public int getId() {
        return id;
    }

    public double getExigencia() {
        return exigencia;
    }

    public int getCursoId() {
        return cursoId;
    }

    public String getCategoria() {
        return categoria;
    }

    public int getMateriaId() {
        return materiaId;
    }

    public String getNombre() {
        return nombre;
    }

    public int getPeriodo() {
        return periodo;
    }

    public String getEtapa() {
        return etapa;
    }

    public int getProfesorId() {
        return profesorId;
    }

    public int getTareasCount() {
        return tareasCount;
    }

    public String getUltimaTarea() {
        return ultimaTarea;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setCursoId(int cursoId) {
        this.cursoId = cursoId;
    }

    public void setMateriaId(int materiaId) {
        this.materiaId = materiaId;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setPeriodo(int periodo) {
        this.periodo = periodo;
    }

    public void setEtapa(String etapa) {
        this.etapa = etapa;
    }

    public void setProfesorId(int profesorId) {
        this.profesorId = profesorId;
    }

    public void setTareasCount(int tareasCount) {
        this.tareasCount = tareasCount;
    }

    public void setUltimaTarea(String ultimaTarea) {
        this.ultimaTarea = ultimaTarea;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public void setExigencia(double exigencia) {
        this.exigencia = exigencia;
    }

    public String getGoogleCourseId() {
        return googleCourseId;
    }

    public void setGoogleCourseId(String googleCourseId) {
        this.googleCourseId = googleCourseId;
    }

    public void setGradeRanges(Map<Integer, int[]> gradeRanges) {
        this.gradeRanges = gradeRanges;
    }

    public void setLimiteInferior(int limiteInferior) {
        this.limiteInferior = limiteInferior;
    }

    public void setLimiteSuperior(int limiteSuperior) {
        this.limiteSuperior = limiteSuperior;
    }

}
