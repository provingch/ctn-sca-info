package ctn.informatica.sca.service;

/**
 * @param temaDeEtapaAnterior el tema reconocido es un pendiente del plan de la etapa anterior: la clase lo retoma
 *                            (siempre como atraso) y hay que marcarlo cubierto en ese plan, no en el de la etapa actual
 */
public record VerificacionResultado(String estado, Integer temaPlanCurricularId, boolean atrasado, boolean temaDeEtapaAnterior) {

    public VerificacionResultado(String estado, Integer temaPlanCurricularId, boolean atrasado) {
        this(estado, temaPlanCurricularId, atrasado, false);
    }
}
