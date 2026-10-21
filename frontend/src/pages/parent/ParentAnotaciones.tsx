import { useCallback, useEffect, useState } from 'react';
import { ApiError } from '../../api/client';
import { getAnotacionesHijo, getFotoAnotacionHijo, type Anotacion } from '../../api/anotaciones';
import AnotacionesList from '../../components/AnotacionesList';
import ContentState from '../../components/ui/ContentState';

/** Anotaciones del hijo cargadas desde la app de los profesores, con su foto de evidencia. */
export default function ParentAnotaciones({ alumnoId, nombre }: { alumnoId: number; nombre: string }) {
  const [anotaciones, setAnotaciones] = useState<Anotacion[] | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    setAnotaciones(null);
    setError('');
    getAnotacionesHijo(alumnoId)
      .then((result) => { if (active) setAnotaciones(result); })
      .catch((reason) => { if (active) setError(reason instanceof ApiError ? reason.message : 'Reintentá en unos minutos.'); });
    return () => { active = false; };
  }, [alumnoId]);

  const loadFoto = useCallback((anotacion: Anotacion) => getFotoAnotacionHijo(alumnoId, anotacion.id), [alumnoId]);

  return <section className="panel parent-anotaciones" aria-labelledby="parent-anotaciones-title">
    <header className="panel-header">
      <div>
        <span>Anotaciones</span>
        <h2 id="parent-anotaciones-title">Anotaciones de profesores</h2>
        <p>Registros de uniforme y comportamiento cargados durante la jornada. Algunos incluyen una foto como evidencia.</p>
      </div>
    </header>
    {error ? <ContentState tone="error" title="No se pudieron cargar las anotaciones" detail={error} />
      : anotaciones === null ? <ContentState tone="loading" title="Cargando anotaciones…" />
      : anotaciones.length === 0 ? <ContentState tone="empty" title="Sin anotaciones" detail={`${nombre} no tiene anotaciones registradas.`} />
      : <AnotacionesList anotaciones={anotaciones} loadFoto={loadFoto} />}
  </section>;
}
