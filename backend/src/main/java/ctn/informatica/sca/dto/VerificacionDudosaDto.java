package ctn.informatica.sca.dto;

public class VerificacionDudosaDto {
    public int planillaId;
    public int cursoId;
    public Integer asignacionId;
    public String materiaNombre;
    public String profesorNombre;
    public String temaIngresado;
    public Integer temaPlanId;
    public String temaEsperado;
    public String fechaClase;

    public VerificacionDudosaDto() {}

    public VerificacionDudosaDto(int planillaId, int cursoId, Integer asignacionId, String materiaNombre, String profesorNombre, String temaIngresado, Integer temaPlanId, String temaEsperado, String fechaClase) {
        this.planillaId = planillaId;
        this.cursoId = cursoId;
        this.asignacionId = asignacionId;
        this.materiaNombre = materiaNombre;
        this.profesorNombre = profesorNombre;
        this.temaIngresado = temaIngresado;
        this.temaPlanId = temaPlanId;
        this.temaEsperado = temaEsperado;
        this.fechaClase = fechaClase;
    }
}
