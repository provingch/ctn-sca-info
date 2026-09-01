import { useMemo, useState } from 'react';
import AnimatedSelect from '../../components/AnimatedSelect';
import { ApiError } from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import type { AdminCatalog } from '../../api/admin';
import { createQueja, getAdminQuejas, type QuejaItem } from '../../api/quejas';
import { formatSqlDateTime } from '../../utils/date';

export default function AdminQuejasPanel({ data, reload, status }: { data: AdminCatalog; reload: () => Promise<void>; status: (s: string) => void }) {
  const [cursoId, setCursoId] = useState<number | ''>('');
  const [profesorId, setProfesorId] = useState<number | ''>('');
  const [motivo, setMotivo] = useState('');
  const [lista, setLista] = useState<QuejaItem[]>([]);
  const [loading, setLoading] = useState(false);

  const cursos = data.cursos;
  const asignaciones = data.asignaciones;

  const usuariosPorId = useMemo(() => {
    const map = new Map<number, string>();
    data.usuarios.forEach((u) => map.set(u.id, `${u.nombre} ${u.apellido}`.trim()));
    return map;
  }, [data.usuarios]);

  function nombreCreador(creadaPor: number): string {
    return usuariosPorId.get(creadaPor) ?? `Usuario #${creadaPor}`;
  }

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
    <section className="panel">
      <header className="planilla-table-heading" style={{ borderLeftColor: 'var(--accent)' }}>
        <div><span>Registrar</span><h2>Registrar queja</h2></div>
        <small>Registrar una queja por un profesor en tu especialidad.</small>
      </header>
      <div className="panel form-grid" style={{ paddingTop: 18 }}>
        <form onSubmit={submit}>
          <label>
            <div className="form-label">Curso</div>
            <AnimatedSelect ariaLabel="Curso" value={cursoId || ''} onChange={(v) => { setCursoId(Number(v)); setProfesorId(''); }} options={cursos.map((c) => ({ value: c.id, label: `${c.especialidad} ${c.nivel}° Sección ${c.seccion}` }))} />
          </label>
          <label>
            <div className="form-label">Profesor</div>
            <AnimatedSelect ariaLabel="Profesor" value={profesorId || ''} onChange={(v) => setProfesorId(Number(v))} disabled={!cursoId || profesoresForCurso.length === 0} options={profesoresForCurso.map((p) => ({ value: p.id, label: p.nombre }))} />
          </label>
          <label>
            <div className="form-label">Motivo</div>
            <textarea placeholder="Describa brevemente el motivo de la queja" value={motivo} onChange={(e) => setMotivo(e.target.value)} required />
          </label>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            <button className="button" type="submit" disabled={loading}><svg viewBox="0 0 20 20" aria-hidden="true" style={{ width: 16, height: 16, marginRight: 8 }}><path d="M2 11v5h5" stroke="currentColor" strokeWidth="1.5" fill="none" strokeLinecap="round" strokeLinejoin="round"/></svg>Registrar queja</button>
            <button type="button" className="button secondary" onClick={() => void loadList()} style={{ marginLeft: 'auto' }}><svg viewBox="0 0 20 20" aria-hidden="true" style={{ width: 14, height: 14, marginRight: 6 }}><path d="M3 10a7 7 0 0112.12-4.95L17 5" stroke="currentColor" strokeWidth="1.5" fill="none" strokeLinecap="round" strokeLinejoin="round"/></svg>Refrescar lista</button>
          </div>
        </form>
      </div>

      <header className="planilla-table-heading" style={{ borderLeftColor: 'var(--muted)', marginTop: 16 }}>
        <div><span>Listado</span><h2>Quejas en este alcance</h2></div>
        <small className="muted-copy">Registros cargados para la especialidad actual.</small>
      </header>
      <div className="panel" style={{ marginTop: 0, paddingTop: 12 }}>
        <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
          <button className="button secondary" type="button" onClick={() => void loadList()} style={{ marginBottom: 8 }}><svg viewBox="0 0 20 20" aria-hidden="true" style={{ width: 14, height: 14, marginRight: 6 }}><path d="M3 10a7 7 0 0112.12-4.95L17 5" stroke="currentColor" strokeWidth="1.5" fill="none" strokeLinecap="round" strokeLinejoin="round"/></svg>Refrescar</button>
        </div>
        {lista.length === 0 ? <p>No hay quejas registradas.</p> : (
          <ul className="list" style={{ listStyle: 'none', padding: 0, margin: 0 }}>
            {lista.map((q) => (
              <li key={q.id} style={{ display: 'flex', gap: 12, alignItems: 'flex-start', padding: 12, borderBottom: '1px solid var(--line)' }}>
                <div className="avatar" style={{ width: 44, height: 44, borderRadius: 999, fontSize: '0.95rem', fontWeight: 900, display: 'grid', placeItems: 'center', background: 'var(--bg-soft)', color: 'var(--muted)' }}>{(q.profesorNombre ?? 'P').slice(0,1)}</div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <strong style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{`${q.profesorNombre ?? ''} ${q.profesorApellido ?? ''}`.trim()}</strong>
                    <small style={{ color: 'var(--muted)', marginLeft: 6 }}>{`${q.cursoEspecialidad ?? ''} ${q.cursoNivel ?? ''}° ${q.cursoSeccion ?? ''}`.trim()}</small>
                    <div style={{ marginLeft: 'auto' }}><span className={`badge`} style={{ borderColor: 'color-mix(in srgb, var(--accent) 32%, var(--line))' }}>{'Pendiente'}</span></div>
                  </div>
                  <div style={{ color: 'var(--muted)', marginTop: 6, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{q.motivo}</div>
                  <div style={{ marginTop: 8 }}><small style={{ color: 'var(--muted)' }}>{formatSqlDateTime(q.creadaEn)} — cargada por {nombreCreador(q.creadaPor)}</small></div>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  </div>;
}
