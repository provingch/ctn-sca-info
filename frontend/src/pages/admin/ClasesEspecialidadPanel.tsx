import { useCallback, useEffect, useMemo, useState } from 'react';
import { ApiError } from '../../api/client';
import { getClasesEspecialidad, type ClaseEspecialidadItem } from '../../api/admin';
import ContentState from '../../components/ui/ContentState';

export default function ClasesEspecialidadPanel({ status }: { status: (message: string) => void }) {
  const [items, setItems] = useState<ClaseEspecialidadItem[] | null>(null);
  const [error, setError] = useState('');
  const [query, setQuery] = useState('');

  const load = useCallback(async () => {
    setError('');
    try {
      setItems(await getClasesEspecialidad());
    } catch (err) {
      const msg = err instanceof ApiError ? err.message : 'No se pudieron cargar las clases.';
      setError(msg);
      status(msg);
    }
  }, [status]);

  useEffect(() => { void load(); }, [load]);

  const filtered = useMemo(() => {
    if (!items) return [];
    const q = query.trim().toLowerCase();
    if (!q) return items;
    return items.filter((c) =>
      (c.tema ?? '').toLowerCase().includes(q)
      || (c.materiaNombre ?? '').toLowerCase().includes(q)
      || (c.profesorNombre ?? '').toLowerCase().includes(q)
      || (c.cursoDescripcion ?? '').toLowerCase().includes(q)
    );
  }, [items, query]);

  if (error && !items) {
    return <ContentState tone="error" title="No se pudieron cargar las clases" detail={error} actions={<button className="button" type="button" onClick={() => void load()}>Reintentar</button>} />;
  }
  if (!items) {
    return <ContentState tone="loading" title="Cargando clases…" detail="Estamos consultando las clases registradas en la especialidad." />;
  }

  return (
    <section className="panel">
      <header className="panel-header">
        <div>
          <h2>Clases dadas</h2>
          <p>Historial de clases registradas por los profesores. Vista de solo lectura.</p>
        </div>
        <div className="form-grid u-narrow-form">
          <label htmlFor="clases-profesor-search">Buscar clases
            <input
              id="clases-profesor-search"
              type="search"
              placeholder="Buscar por profesor, materia, tema o curso…"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
          </label>
        </div>
      </header>

      {filtered.length === 0 ? (
        <ContentState tone="empty" title="No hay clases para mostrar" detail={query ? 'Ningún resultado coincide con la búsqueda.' : 'Todavía no se registraron clases en esta especialidad.'} />
      ) : (
        <div className="table-scroll u-mt">
          <table className="grade-table data-table u-table-xwide">
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Profesor</th>
                <th>Curso</th>
                <th>Materia</th>
                <th>Tema</th>
                <th className="num">Alumnos</th>
                <th className="num">Ausentes</th>
                <th className="num">Justificados</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((c) => (
                <tr key={c.id}>
                  <th scope="row">{c.fechaClase ?? '—'}</th>
                  <td data-label="Profesor">{c.profesorNombre ?? '—'}</td>
                  <td data-label="Curso">{c.cursoDescripcion || '—'}</td>
                  <td data-label="Materia">{c.materiaNombre ?? '—'}</td>
                  <td data-label="Tema">{c.tema}</td>
                  <td className="num" data-label="Alumnos">{c.totalAlumnos}</td>
                  <td className="num" data-label="Ausentes">{c.totalAusentes}</td>
                  <td className="num" data-label="Justificados">{c.totalJustificados}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
