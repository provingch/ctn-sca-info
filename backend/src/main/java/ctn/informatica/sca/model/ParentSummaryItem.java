package ctn.informatica.sca.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class ParentSummaryItem {
    private String alumnoNombre;
    private String materiaNombre;
    private int puntos;
    private int totalPosible;
    private int porcentaje;
    private int nota;
    private Integer alumnoId;
    private Integer materiaId;
    private Integer cursoId;
    private Integer planillaId;
    private String especialidadNombre;
    private String etapa;
    private String categoria;
    private int tareasCount;
    private Map<Integer, Integer> tareaPuntajes = new LinkedHashMap<>();

    public void recomputeDerivedValues() {
        if (totalPosible <= 0) {
            porcentaje = 0;
            nota = 1;
            return;
        }

        porcentaje = (int) Math.round((puntos * 100.0) / totalPosible);
        Planilla planilla = new Planilla();
        planilla.setCategoria(categoria);
        planilla.computeGradeRanges(totalPosible);
        nota = planilla.getNotaForSum(puntos);
    }

    public String getAlumnoNombre() { return alumnoNombre; }
    public void setAlumnoNombre(String alumnoNombre) { this.alumnoNombre = alumnoNombre; }
    public String getMateriaNombre() { return materiaNombre; }
    public void setMateriaNombre(String materiaNombre) { this.materiaNombre = materiaNombre; }
    public int getPuntos() { return puntos; }
    public void setPuntos(int puntos) { this.puntos = puntos; }
    public int getTotalPosible() { return totalPosible; }
    public void setTotalPosible(int totalPosible) { this.totalPosible = totalPosible; }
    public int getPorcentaje() { return porcentaje; }
    public void setPorcentaje(int porcentaje) { this.porcentaje = porcentaje; }
    public int getNota() { return nota; }
    public void setNota(int nota) { this.nota = nota; }
    public Integer getAlumnoId() { return alumnoId; }
    public void setAlumnoId(Integer alumnoId) { this.alumnoId = alumnoId; }
    public Integer getMateriaId() { return materiaId; }
    public void setMateriaId(Integer materiaId) { this.materiaId = materiaId; }
    public Integer getCursoId() { return cursoId; }
    public void setCursoId(Integer cursoId) { this.cursoId = cursoId; }
    public Integer getPlanillaId() { return planillaId; }
    public void setPlanillaId(Integer planillaId) { this.planillaId = planillaId; }
    public String getEspecialidadNombre() { return especialidadNombre; }
    public void setEspecialidadNombre(String especialidadNombre) { this.especialidadNombre = especialidadNombre; }
    public String getEtapa() { return etapa; }
    public void setEtapa(String etapa) { this.etapa = etapa; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public int getTareasCount() { return tareasCount; }
    public void setTareasCount(int tareasCount) { this.tareasCount = tareasCount; }
    public Map<Integer, Integer> getTareaPuntajes() { return tareaPuntajes; }
    public void setTareaPuntajes(Map<Integer, Integer> tareaPuntajes) { this.tareaPuntajes = tareaPuntajes; }
}
