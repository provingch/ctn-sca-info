package ctn.informatica.sia.servlets;

import ctn.informatica.sia.model.Alumno;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ParentServletTest {

    @Test
    void shouldDefaultToFirstChildWhenNoSelectedAlumnoIdIsProvided() {
        ParentServlet servlet = new ParentServlet();
        List<Alumno> hijos = new ArrayList<>();
        hijos.add(createAlumno(101, "Ana", "García"));
        hijos.add(createAlumno(102, "Luis", "Pérez"));

        Integer selectedAlumnoId = servlet.resolveSelectedAlumnoId(hijos, null);

        assertEquals(101, selectedAlumnoId);
    }

    @Test
    void shouldKeepSelectionWhenTargetChildStillBelongsToParent() {
        ParentServlet servlet = new ParentServlet();
        List<Alumno> hijos = new ArrayList<>();
        hijos.add(createAlumno(101, "Ana", "García"));
        hijos.add(createAlumno(102, "Luis", "Pérez"));

        Integer selectedAlumnoId = servlet.resolveSelectedAlumnoId(hijos, 102);

        assertEquals(102, selectedAlumnoId);
    }

    @Test
    void shouldReturnNullWhenParentHasNoChildren() {
        ParentServlet servlet = new ParentServlet();

        Integer selectedAlumnoId = servlet.resolveSelectedAlumnoId(List.of(), null);

        assertNull(selectedAlumnoId);
    }

    private Alumno createAlumno(int id, String nombre, String apellido) {
        Alumno alumno = new Alumno();
        alumno.setId(id);
        alumno.setNombre(nombre);
        alumno.setApellido(apellido);
        return alumno;
    }
}
