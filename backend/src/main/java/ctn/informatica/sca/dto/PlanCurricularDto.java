package ctn.informatica.sca.dto;

import java.util.List;

public class PlanCurricularDto {
    // Campos para parsing y upload
    public String etapa;
    public int anio;
    public String disciplina;
    public String curso;
    public String seccion;
    public String turno;
    public String especialidad;
    public List<TemaPlanDto> temas;
    
    // Campos para consulta por id/asignación
    public Integer id;
    public String estado; // PENDIENTE, APROBADO, RECHAZADO, NO_CARGADO
    public String archivoNombre;
    public String fechaSubida;
    public String fechaRevision;
    public String observacionesEvaluador;
    
    // Campos adicionales para findPendientes
    public String materiaNombre;
    public String profesorNombre;
    public String cursoDescripcion;
}
