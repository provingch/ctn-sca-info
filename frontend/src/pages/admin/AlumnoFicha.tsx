import { useCallback, useEffect, useState } from 'react';
import { ApiError } from '../../api/client';
import { getPadresDeAlumno, getResumenAlumno, type CourseItem, type PadreSummary, type ResumenAlumno, type StudentItem } from '../../api/admin';
import { getAnotacionesAlumnoAdmin, getFotoAnotacionAlumnoAdmin, type Anotacion } from '../../api/anotaciones';
import AnotacionesList from '../../components/AnotacionesList';
import useAccessibleDialog from '../../hooks/useAccessibleDialog';
import '../../components/seleccion-tabla.css';

type Carga<T> = { estado: 'cargando' } | { estado: 'listo'; valor: T } | { estado: 'error'; mensaje: string };

function useCarga<T>(pedir: () => Promise<T>, error: string): Carga<T> {
  const [carga, setCarga] = useState<Carga<T>>({ estado: 'cargando' });
  useEffect(() => {
    let activo = true;
    setCarga({ estado: 'cargando' });
    pedir()
      .then((valor) => { if (activo) setCarga({ estado: 'listo', valor }); })
      .catch((e: unknown) => { if (activo) setCarga({ estado: 'error', mensaje: e instanceof ApiError ? e.message : error }); });
    return () => { activo = false; };
  }, [pedir, error]);
  return carga;
}

function Estado<T>({ carga, children }: { carga: Carga<T>; children: (valor: T) => React.ReactNode }) {
  if (carga.estado === 'cargando') return <p className="muted-copy" aria-busy="true">Cargando…</p>;
  if (carga.estado === 'error') return <p className="muted-copy">{carga.mensaje}</p>;
  return <>{children(carga.valor)}</>;
}

/**
 * Ficha general del alumno: datos, padres vinculados, lo que tiene cargado y sus anotaciones.
 * Las anotaciones se listan sin foto; la evidencia se descarga recién cuando se abre una.
 */
export default function AlumnoFicha({ alumno, curso, onClose, onEditar }: {
  alumno: StudentItem;
  curso: CourseItem | undefined;
  onClose: () => void;
  onEditar: () => void;
}) {
  const dialogRef = useAccessibleDialog(true, onClose);
  const padres = useCarga(useCallback(() => getPadresDeAlumno(alumno.id), [alumno.id]), 'No se pudieron cargar los padres.');
  const resumen = useCarga(useCallback(() => getResumenAlumno(alumno.id), [alumno.id]), 'No se pudo cargar el resumen.');
  const anotaciones = useCarga(useCallback(() => getAnotacionesAlumnoAdmin(alumno.id), [alumno.id]), 'No se pudieron cargar las anotaciones.');
  const loadFoto = useCallback((anotacion: Anotacion) => getFotoAnotacionAlumnoAdmin(alumno.id, anotacion.id), [alumno.id]);

  return <div ref={dialogRef} className="data-modal" role="dialog" aria-modal="true" aria-labelledby="alumno-ficha-title" tabIndex={-1}>
    <div className="signature-modal-header">
      <div>
        <span>Ficha del alumno</span>
        <h2 id="alumno-ficha-title">{alumno.apellido}, {alumno.nombre}</h2>
      </div>
      <button type="button" className="signature-modal-close" aria-label="Cerrar ficha" data-dialog-initial-focus onClick={onClose}>×</button>
    </div>

    <div className="ficha-alumno">
      <dl className="ficha-alumno-datos">
        <div><dt>Cédula</dt><dd>{alumno.ci ?? '—'}</dd></div>
        <div><dt>Especialidad</dt><dd>{curso?.especialidad ?? '—'}</dd></div>
        <div><dt>Curso</dt><dd>{curso ? `${curso.nivel}° ${curso.seccion}` : '—'}</dd></div>
      </dl>

      <section aria-labelledby="ficha-cargado">
        <h3 id="ficha-cargado">Lo que tiene cargado</h3>
        <Estado carga={resumen}>{(r: ResumenAlumno) => <ul className="ficha-alumno-cifras">
          <li><strong>{r.notas}</strong><span>notas en {r.planillasConNotas} {r.planillasConNotas === 1 ? 'planilla' : 'planillas'}</span></li>
          <li><strong>{r.asistencias}</strong><span>registros de asistencia</span></li>
          <li><strong>{r.anotaciones}</strong><span>anotaciones</span></li>
          <li><strong>{r.consultasEnfermeria}</strong><span>consultas de enfermería{r.fichaEnfermeria ? ' · tiene ficha' : ''}</span></li>
        </ul>}</Estado>
      </section>

      <section aria-labelledby="ficha-padres">
        <h3 id="ficha-padres">Padres vinculados</h3>
        <Estado carga={padres}>{(lista: PadreSummary[]) => lista.length === 0
          ? <p className="muted-copy">No hay padres vinculados. Se vinculan desde Editar.</p>
          : <div className="u-stack">{lista.map((padre) => <div className="linked-item" key={padre.id}>
            <span>{padre.nombre} {padre.apellido} · {padre.usuario}{padre.ci ? ` · CI ${padre.ci}` : ''}</span>
          </div>)}</div>}</Estado>
      </section>

      <section aria-labelledby="ficha-anotaciones">
        <h3 id="ficha-anotaciones">Anotaciones</h3>
        <Estado carga={anotaciones}>{(lista: Anotacion[]) => lista.length === 0
          ? <p className="muted-copy">Sin anotaciones.</p>
          : <AnotacionesList anotaciones={lista} loadFoto={loadFoto} fotoAlAmpliar />}</Estado>
      </section>

      <div className="signature-modal-actions">
        <button type="button" className="button secondary" onClick={onClose}>Cerrar</button>
        <button type="button" className="button" onClick={onEditar}>Editar alumno</button>
      </div>
    </div>
  </div>;
}
