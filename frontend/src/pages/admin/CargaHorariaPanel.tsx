import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiError } from '../../api/client';
import { getCargaHoraria, getCargaHorariaProfesor, type CargaHorariaItem, type CargaHorariaProfesorItem } from '../../api/admin';
import ContentState from '../../components/ui/ContentState';
import CargaHorariaView from './CargaHorariaView';

const normalize = (value: string) => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();

function formatMinutes(total: number) {
  return `${Math.floor(total / 60)}h ${String(total % 60).padStart(2, '0')}min`;
}

export default function CargaHorariaPanel({ status }: { status: (message: string) => void }) {
  const { usuarioId } = useParams();
  const [items, setItems] = useState<CargaHorariaProfesorItem[] | null>(null);
  const [detalle, setDetalle] = useState<CargaHorariaItem | null>(null);
  const [error, setError] = useState('');
  const [query, setQuery] = useState('');

  useEffect(() => {
    let active = true;
    setError('');
    setItems(null);
    setDetalle(null);
    const request = usuarioId
      ? getCargaHorariaProfesor(Number(usuarioId)).then((data) => { if (active) setDetalle(data); })
      : getCargaHoraria().then((list) => { if (active) setItems(list); });
    request.catch((reason) => {
      if (!active) return;
      const message = reason instanceof ApiError ? reason.message : 'No se pudo cargar la carga horaria.';
      setError(message);
      status(message);
    });
    return () => { active = false; };
  }, [usuarioId, status]);

  const filtered = useMemo(() => {
    const needle = normalize(query.trim());
    return (items ?? []).filter((item) => normalize(item.profesorNombre).includes(needle));
  }, [items, query]);

  if (error) return <ContentState tone="error" title={error} detail="Volvé a intentarlo recargando esta página." />;

  if (usuarioId) {
    if (!detalle) return <ContentState tone="loading" title="Cargando horario…" detail="Estamos armando el horario semanal del profesor." />;
    return (
      <>
        <nav className="toolbar" aria-label="Navegación de carga horaria">
          <Link className="button secondary" to="/admin/carga-horaria">← Todos los profesores</Link>
        </nav>
        <h2>{detalle.profesorNombre}</h2>
        <CargaHorariaView data={detalle} />
      </>
    );
  }

  if (!items) return <ContentState tone="loading" title="Cargando carga horaria…" detail="Estamos sumando las horas de cada profesor." />;
  if (items.length === 0) return <ContentState title="Todavía no hay horarios cargados" detail="Los profesores con horario aparecerán en este panel." />;

  return (
    <>
      <label className="form-label">Buscar profesor
        <input type="search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Nombre o apellido" />
      </label>
      <div className="table-wrap">
        <table className="grade-table">
          <thead>
            <tr><th>Profesor</th><th>Horas cátedra</th><th>Común</th><th>Específicas</th><th>Horas reloj</th></tr>
          </thead>
          <tbody>
            {filtered.map((item) => (
              <tr key={item.usuarioId}>
                <td><Link to={`/admin/carga-horaria/${item.usuarioId}`}>{item.profesorNombre}</Link></td>
                <td>{item.horas}</td>
                <td>{item.horasComun}</td>
                <td>{item.horasEspecificas}</td>
                <td>{formatMinutes(item.minutos)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {filtered.length === 0 && <ContentState title="Sin resultados" detail="Ningún profesor coincide con la búsqueda." />}
    </>
  );
}
