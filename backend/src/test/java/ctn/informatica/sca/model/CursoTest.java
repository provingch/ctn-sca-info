package ctn.informatica.sca.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifica el cálculo de año (1º/2º/3º) a partir de la promoción (año de egreso)
 * y el período académico actual. Bug histórico: la fórmula tenía el signo invertido
 * (period - promocion + 3), lo que hacía que cualquier promoción de años anteriores
 * al actual quedara clampeada a 3er año en vez de distribuirse correctamente.
 */
class CursoTest {

    private static final int CURRENT_YEAR = LocalDate.now().getYear();

    private Curso cursoConPromocion(int promocion) {
        return new Curso(1, "Informática", promocion, "A");
    }

    @Test
    void promocionDelAnioActualEsTercerAnio() {
        assertEquals(3, cursoConPromocion(CURRENT_YEAR).getCurso());
    }

    @Test
    void promocionDelAnioAnteriorEsSegundoAnio() {
        assertEquals(2, cursoConPromocion(CURRENT_YEAR - 1).getCurso());
    }

    @Test
    void promocionDeDosAniosAtrasEsPrimerAnio() {
        assertEquals(1, cursoConPromocion(CURRENT_YEAR - 2).getCurso());
    }

    @Test
    void promocionMuyLejanaEnElPasadoQuedaClampeadaAPrimerAnio() {
        assertEquals(1, cursoConPromocion(CURRENT_YEAR - 10).getCurso());
    }

    @Test
    void promocionFuturaQuedaClampeadaATercerAnio() {
        assertEquals(3, cursoConPromocion(CURRENT_YEAR + 5).getCurso());
    }
}