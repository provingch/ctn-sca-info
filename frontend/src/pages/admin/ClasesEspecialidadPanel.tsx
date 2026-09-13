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
        <input
          type="search"
          placeholder="Buscar por profesor, materia, tema o curso…"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          style={{ maxWidth: 320 }}
        />
      </header>

      {filtered.length === 0 ? (
        <ContentState tone="empty" title="No hay clases para mostrar" detail={query ? 'Ningún resultado coincide con la búsqueda.' : 'Todavía no se registraron clases en esta especialidad.'} />
      ) : (
        <div className="table-scroll" style={{ marginTop: 12 }}>
          <table className="grade-table" style={{ minWidth: 900 }}>
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Profesor</th>
                <th>Curso</th>
                <th>Materia</th>
                <th>Tema</th>
                <th>Alumnos</th>
                <th>Ausentes</th>
                <th>Justificados</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((c) => (
                <tr key={c.id}>
                  <td>{c.fechaClase ?? '—'}</td>
                  <td>{c.profesorNombre ?? '—'}</td>
                  <td>{c.cursoDescripcion || '—'}</td>
                  <td>{c.materiaNombre ?? '—'}</td>
                  <td>{c.tema}</td>
                  <td>{c.totalAlumnos}</td>
                  <td>{c.totalAusentes}</td>
                  <td>{c.totalJustificados}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
