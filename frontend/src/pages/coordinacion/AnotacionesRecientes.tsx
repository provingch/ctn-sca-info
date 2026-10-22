import { useCallback, useEffect, useMemo, useState } from 'react';
import { ApiError } from '../../api/client';
import { getAnotacionesRecientes, getFotoAnotacionCoordinacion, type Anotacion } from '../../api/anotaciones';
import AnotacionesList from '../../components/AnotacionesList';
import ContentState from '../../components/ui/ContentState';
import FiltersToolbar, { FilterField } from '../../components/ui/FiltersToolbar';

const LIMITE = 200;

/** Últimas anotaciones de la app Android en todo el colegio, con su foto de evidencia. */
export default function AnotacionesRecientes() {
  const [anotaciones, setAnotaciones] = useState<Anotacion[] | null>(null);
  const [error, setError] = useState('');
  const [busqueda, setBusqueda] = useState('');

  const load = useCallback(() => {
    setAnotaciones(null);
    setError('');
    getAnotacionesRecientes(LIMITE)
      .then(setAnotaciones)
      .catch((reason) => setError(reason instanceof ApiError ? reason.message : 'Reintentá en unos minutos.'));
  }, []);

  useEffect(() => { load(); }, [load]);

  const loadFoto = useCallback((anotacion: Anotacion) => getFotoAnotacionCoordinacion(anotacion.id), []);

  const visibles = useMemo(() => {
    const texto = busqueda.trim().toLocaleLowerCase('es');
    if (!anotaciones || !texto) return anotaciones ?? [];
    return anotaciones.filter((a) => [a.alumnoNombre, a.alumnoApellido, a.curso, a.tipo, a.profesor, a.materia]
      .some((campo) => campo?.toLocaleLowerCase('es').includes(texto)));
  }, [anotaciones, busqueda]);

  return <section className="panel" aria-labelledby="anotaciones-recientes-title">
    <header className="panel-header">
      <div>
        <span>App de anotaciones</span>
        <h2 id="anotaciones-recientes-title">Anotaciones recientes</h2>
        <p>Las últimas {LIMITE} anotaciones cargadas por los profesores desde la app, con su foto de evidencia cuando la tienen.</p>
      </div>
      <button type="button" className="button secondary" onClick={load}>Actualizar</button>
    </header>
    {error ? <ContentState tone="error" title="No se pudieron cargar las anotaciones" detail={error} actions={<button type="button" className="button secondary" onClick={load}>Reintentar</button>} />
      : anotaciones === null ? <ContentState tone="loading" title="Cargando anotaciones…" />
      : anotaciones.length === 0 ? <ContentState tone="empty" title="Sin anotaciones" detail="Todavía no se cargaron anotaciones desde la app." />
      : <>
        <FiltersToolbar ariaLabel="Filtros de anotaciones" mostrando={visibles.length} total={anotaciones.length} unidad="anotaciones" activo={busqueda.trim() !== ''} onLimpiar={() => setBusqueda('')}>
          <FilterField label="Buscar"><input type="search" placeholder="Alumno, curso, tipo o profesor" value={busqueda} onChange={(event) => setBusqueda(event.target.value)} /></FilterField>
        </FiltersToolbar>
        {visibles.length === 0 ? <ContentState compact title="Sin resultados" detail="Ninguna anotación coincide con la búsqueda." />
          : <AnotacionesList anotaciones={visibles} loadFoto={loadFoto} mostrarAlumno />}
      </>}
  </section>;
}
