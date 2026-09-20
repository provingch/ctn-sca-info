import type { TemaPlanDto } from '../api/planCurricular';
import { coberturaTema } from '../utils/coberturaTema';
import { formatSqlDateTime } from '../utils/date';

/** Estado de cobertura de un tema: fecha si se cumplió, "No cumplido" si su etapa cerró sin cubrirlo, o "Pendiente". */
export default function CoberturaTemaCelda({ tema, etapaCerrada }: { tema: Pick<TemaPlanDto, 'estadoCobertura' | 'fechaCobertura'>; etapaCerrada?: boolean }) {
  const cobertura = coberturaTema(tema, etapaCerrada);
  if (cobertura.estado === 'cumplido') return <>{formatSqlDateTime(cobertura.fecha, { dateStyle: 'short', timeStyle: 'short' }, 'Cumplido')}</>;
  if (cobertura.estado === 'no-cumplido') return <span className="badge badge--danger">No cumplido</span>;
  return <>Pendiente</>;
}
