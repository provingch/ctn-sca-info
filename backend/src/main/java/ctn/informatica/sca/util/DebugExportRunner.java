package ctn.informatica.sca.util;

import ctn.informatica.sca.model.CursoBase;
import ctn.informatica.sca.model.HoraCatedra;
import ctn.informatica.sca.model.HorarioSlot;
import java.io.FileOutputStream;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Small runner to generate a demo XLSX using HorarioWorkbookBuilder without DB.
 */
public class DebugExportRunner {

    public static void main(String[] args) throws Exception {
        CursoBase curso = new CursoBase();
        curso.setId(1);
        curso.setNivel(1);
        curso.setSeccion("A");
        curso.setEspecialidad("Ciencias");

        List<HoraCatedra> horas = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            HoraCatedra h = new HoraCatedra();
            h.setId(i);
            h.setNumero(i);
            h.setEtiqueta(i <= 4 ? "M" : "T");
            h.setHoraInicio(LocalTime.of(8 + (i-1), 0));
            h.setHoraFin(LocalTime.of(8 + i, 0));
            horas.add(h);
        }

        List<HorarioSlot> slots = new ArrayList<>();
        // Lunes materia en sala 101
        HorarioSlot s1 = new HorarioSlot();
        s1.setId(1);
        s1.setCursoId(1);
        s1.setDiaSemana(1);
        s1.setHoraCatedraId(1);
        s1.setAsignacionId(10);
        s1.setMateriaNombre("Matemática");
        s1.setProfesorNombre("Dra. Pérez");
        s1.setSalaNombre("101");
        slots.add(s1);

        // Martes- miercoles same asignacion spanning two horas (merged range)
        HorarioSlot s2 = new HorarioSlot();
        s2.setId(2);
        s2.setCursoId(1);
        s2.setDiaSemana(2);
        s2.setHoraCatedraId(2);
        s2.setAsignacionId(20);
        s2.setMateriaNombre("Historia");
        s2.setProfesorNombre("Prof. Gómez");
        s2.setSalaNombre("201");
        slots.add(s2);

        HorarioSlot s3 = new HorarioSlot();
        s3.setId(3);
        s3.setCursoId(1);
        s3.setDiaSemana(2);
        s3.setHoraCatedraId(3);
        s3.setAsignacionId(20); // same assignment -> merged
        s3.setMateriaNombre("Historia");
        s3.setProfesorNombre("Prof. Gómez");
        s3.setSalaNombre("202"); // changed sala within block -> should show both
        slots.add(s3);

        // Add a receso after hora 4 automatically by layout

           try (var wb = new HorarioWorkbookBuilder().buildEspecialidad(curso.getEspecialidad(), List.of(curso), horas, slots);
               var out = new FileOutputStream("target/demo-horario.xlsx")) {
            wb.write(out);
        }
        System.out.println("Wrote backend/target/demo-horario.xlsx");
    }
}
