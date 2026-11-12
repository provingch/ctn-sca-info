package ctn.informatica.sca.integration.coresync;

import java.time.LocalDateTime;
import java.util.List;

public record CoreSnapshot(
        int version,
        LocalDateTime generatedAt,
        List<Especialidad> especialidades,
        List<Curso> cursos,
        List<CursoBase> cursosBase,
        List<Materia> materias,
        List<MateriaEspecialidad> materiasEspecialidades,
        List<Perfil> perfiles,
        List<Alumno> alumnos,
        List<VinculoFamiliar> vinculosFamiliares,
        List<HistorialCurso> historialCursos,
        List<Asignacion> asignaciones,
        List<CodigoConducta> codigosConducta) {

    public record Especialidad(int id, String nombre) {}
    public record Curso(int id, int especialidadId, int promocion, String seccion) {}
    public record CursoBase(int id, int especialidadId, int nivel, String seccion) {}
    public record Materia(int id, String nombre, String categoria) {}
    public record MateriaEspecialidad(int materiaId, int especialidadId) {}
    public record Perfil(
            int id, String nombre, String apellido, Integer ci, String telefono, String celular, String correo,
            int nivel, Integer especialidadId) {}
    public record Alumno(int id, String ci, String nombre, String apellido, int cursoId, String email, String telefono) {}
    public record VinculoFamiliar(int alumnoId, int perfilId, String parentesco) {}
    public record HistorialCurso(
            int id, int alumnoId, int cursoAnteriorId, int cursoNuevoId, LocalDateTime fecha, Integer perfilId) {}
    public record Asignacion(int id, int profesorId, int materiaId, int cursoBaseId) {}
    public record CodigoConducta(int id, String codigo, String descripcion, boolean activo, LocalDateTime createdAt) {}
}
