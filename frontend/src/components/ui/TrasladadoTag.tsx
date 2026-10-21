/** Etiqueta de un alumno que ya no está en el curso de la planilla: se lo trasladó y sus notas quedan de solo lectura. */
export default function TrasladadoTag({ curso }: { curso: string }) {
  return <span className="trasladado-tag" title={`Se trasladó a ${curso}. Sus notas en esta planilla quedan de solo lectura.`}>Trasladado a {curso}</span>;
}
