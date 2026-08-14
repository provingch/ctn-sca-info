package ctn.informatica.sca.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifica el cálculo de año (1º/2º/3º) a partir de la promoción (año de egreso)
 * y el período académico actual.
 *
 * Regla de negocio confirmada:
 *  - promocion == period       -> 3° (último año, egresa este año)
 *  - promocion == period + 1   -> 2°
 *  - promocion == period + 2   -> 1° (recién ingresa)
 *  - promocion  < period       -> ya egresó; en producción CursoDao filtra
 *    estas filas antes de que lleguen acá (WHERE ... AND c.promocion >= ?),
 *    así que getCurso() no debería recibir este caso en operación normal.
 *
 * Bug real (encontrado en producción, ver captura de pantalla del dropdown de
 * cursos mostrando "3° A" y "3° B" duplicados): la fórmula tenía el signo
 * invertido como `promocion - period + 3`. Con promociones reales de la BD
 * (2026 y 2027 coexistiendo en el período 2026), esa fórmula da 3 y 4→clamp 3
 * respectivamente, así que dos cursos distintos (año actual real y el que
 * debería ser 2°) terminaban mostrando la misma etiqueta "3°". La fórmula
 * correcta es `period - promocion + 3`.
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
    void promocionDelAnioSiguienteEsSegundoAnio() {
        assertEquals(2, cursoConPromocion(CURRENT_YEAR + 1).getCurso());
    }

    @Test
    void promocionDeDosAniosEnElFuturoEsPrimerAnio() {
        assertEquals(1, cursoConPromocion(CURRENT_YEAR + 2).getCurso());
    }

    @Test
    void promocionMuyLejanaEnElFuturoQuedaClampeadaAPrimerAnio() {
        assertEquals(1, cursoConPromocion(CURRENT_YEAR + 10).getCurso());
    }

    @Test
    void promocionYaEgresadaQuedaClampeadaATercerAnioComoResguardoDefensivo() {
        // Este caso no debería llegar acá en producción (CursoDao ya lo filtra
        // con c.promocion >= period), pero si llegara, el clamp defensivo lo
        // absorbe en 3° en vez de romper con un valor fuera de rango.
        assertEquals(3, cursoConPromocion(CURRENT_YEAR - 5).getCurso());
    }
}