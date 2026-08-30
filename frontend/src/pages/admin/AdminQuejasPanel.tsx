import { useMemo, useState } from 'react';
import AnimatedSelect from '../../components/AnimatedSelect';
import { ApiError } from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import type { AdminCatalog } from '../../api/admin';
import { createQueja, getAdminQuejas, type QuejaItem } from '../../api/quejas';

export default function AdminQuejasPanel({ data, reload, status }: { data: AdminCatalog; reload: () => Promise<void>; status: (s: string) => void }) {
  const [cursoId, setCursoId] = useState<number | ''>('');
  const [profesorId, setProfesorId] = useState<number | ''>('');
  const [motivo, setMotivo] = useState('');
  const [lista, setLista] = useState<QuejaItem[]>([]);
  const [loading, setLoading] = useState(false);

  const cursos = data.cursos;
  const asignaciones = data.asignaciones;

  const profesoresForCurso = useMemo(() => {
    if (!cursoId) return [] as { id: number; nombre: string }[];
    const items = asignaciones.filter((a) => a.cursoId === Number(cursoId)).map((a) => ({ id: a.profesorId, nombre: a.profesor }));
    // unique
    const map = new Map<number, string>();
    items.forEach((i) => map.set(i.id, i.nombre));
    return Array.from(map.entries()).map(([id, nombre]) => ({ id, nombre }));
  }, [cursoId, asignaciones]);

  const { user } = useAuth();

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!cursoId || !profesorId || !motivo) return status('Completá todos los campos.');
    setLoading(true);
    try {
      const especialidadId = user?.especialidadId ?? (data.especialidades[0]?.id ?? 0);
      await createQueja({ cursoId: Number(cursoId), profesorId: Number(profesorId), especialidadId: Number(especialidadId), motivo });
      setMotivo('');
      setProfesorId('');
      setCursoId('');
      await reload();
      const all = await getAdminQuejas();
      setLista(all);
      status('Queja registrada.');
    } catch (err) {
      status(err instanceof ApiError ? err.message : 'No se pudo registrar la queja.');
    } finally {
      setLoading(false);
    }
  }

  async function loadList() {
    try {
      const all = await getAdminQuejas();
      setLista(all);
    } catch (err) {
      status(err instanceof ApiError ? err.message : 'No se pudo cargar la lista de quejas.');
    }
  }

  return <div>
    <section className="panel form-grid">
      <p className="lead">Registrar una queja por un profesor en tu especialidad.</p>
      <form onSubmit={submit}>
        <label>Curso
          <AnimatedSelect ariaLabel="Curso" value={cursoId || ''} onChange={(v) => { setCursoId(Number(v)); setProfesorId(''); }} options={cursos.map((c) => ({ value: c.id, label: `${c.especialidad} ${c.nivel}° Sección ${c.seccion}` }))} />
        </label>
        <label>Profesor
          <AnimatedSelect ariaLabel="Profesor" value={profesorId || ''} onChange={(v) => setProfesorId(Number(v))} disabled={!cursoId || profesoresForCurso.length === 0} options={profesoresForCurso.map((p) => ({ value: p.id, label: p.nombre }))} />
        </label>
        <label>Motivo<textarea value={motivo} onChange={(e) => setMotivo(e.target.value)} required /></label>
        <div>
          <button className="button" type="submit" disabled={loading}>Registrar queja</button>
          <button type="button" className="button secondary" onClick={() => void loadList()} style={{ marginLeft: 8 }}>Refrescar lista</button>
        </div>
      </form>
    </section>

    <section className="panel" style={{ marginTop: 16 }}>
      <h3>Quejas en este alcance</h3>
      <button className="button secondary" type="button" onClick={() => void loadList()} style={{ marginBottom: 8 }}>Refrescar</button>
      {lista.length === 0 ? <p>No hay quejas registradas.</p> : (
        <ul className="list">
          {lista.map((q) => (
            <li key={q.id}>
              <p><strong>{q.profesorNombre}</strong> — {q.cursoDescripcion}</p>
              <p>{q.motivo}</p>
              <p><small>{q.fecha} — cargada por {q.creadoPor ?? '—'}</small></p>
            </li>
          ))}
        </ul>
      )}
    </section>
  </div>;
}
