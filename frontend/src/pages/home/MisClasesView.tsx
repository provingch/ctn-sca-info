import { useCallback, useEffect, useState } from 'react';
import { ApiError } from '../../api/client';
import { getMiClase, getMisClases, listarCodigosConducta, updateAttendance, updateClase, type ClaseDadaDto, type ClaseDetalleDto, type CodigoConducta } from '../../api/home';
import ContentState from '../../components/ui/ContentState';
import { useToast } from '../../context/toast';
import RasgosAsistenciaEditor from './RasgosAsistenciaEditor';

function estadoLabel(estado: string): string {
  switch (estado) {
    case 'presente': return 'Presente';
    case 'ausente': return 'Ausente';
    case 'ausente_justificado': return 'Ausente justificado';
    case 'pendiente': return 'Pendiente';
    default: return estado;
  }
}

function edicionVigente(fechaClase: string | null): boolean {
  if (!fechaClase) return false;
  const fecha = new Date(`${fechaClase}T00:00:00`);
  if (Number.isNaN(fecha.getTime())) return false;
  const limite = new Date(fecha);
  limite.setDate(limite.getDate() + 7);
  const hoy = new Date();
  hoy.setHours(0, 0, 0, 0);
  return hoy <= limite;
}

export default function MisClasesView() {
  const [clases, setClases] = useState<ClaseDadaDto[] | null>(null);
  const [error, setError] = useState('');
  const [selected, setSelected] = useState<ClaseDetalleDto | null>(null);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [saving, setSaving] = useState<number | null>(null);
  const [editing, setEditing] = useState(false);
  const [editSaving, setEditSaving] = useState(false);
  const [editTema, setEditTema] = useState('');
  const [editAusentes, setEditAusentes] = useState<number[]>([]);
  const [editCodigos, setEditCodigos] = useState<Record<number, string[]>>({});
  const [codigosConducta, setCodigosConducta] = useState<CodigoConducta[]>([]);
  const { showToast } = useToast();

  useEffect(() => {
    void listarCodigosConducta().then(setCodigosConducta).catch(() => setCodigosConducta([]));
  }, []);

  const load = useCallback(async () => {
    setError('');
    try {
      setClases(await getMisClases());
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'No se pudieron cargar las clases.');
    }
  }, []);

  useEffect(() => { void load(); }, [load]);

  const openDetail = async (planillaId: number) => {
    setSelectedId(planillaId);
    setEditing(false);
    setDetailLoading(true);
    try {
      setSelected(await getMiClase(planillaId));
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : 'No se pudo cargar el detalle de la clase.');
      setSelected(null);
    } finally {
      setDetailLoading(false);
    }
  };

  const beginEdit = () => {
    if (!selected || !edicionVigente(selected.fechaClase)) return;
    setEditTema(selected.tema);
    setEditAusentes(selected.asistencias.filter((item) => item.estado === 'ausente' || item.estado === 'ausente_justificado').map((item) => item.alumnoId));
    setEditCodigos(Object.fromEntries(selected.asistencias.map((item) => [item.alumnoId, item.codigos ?? []])));
    setEditing(true);
  };

  const saveEdit = async () => {
    if (!selectedId || !selected || editSaving) return;
    setEditSaving(true);
    try {
      await updateClase(selectedId, {
        tema: editTema.trim(),
        asistencias: selected.asistencias.map((item) => ({
          asistenciaId: item.id,
          estado: editAusentes.includes(item.alumnoId) ? 'ausente' : 'presente',
          codigos: editCodigos[item.alumnoId] ?? [],
        })),
      });
      showToast('Clase actualizada.');
      setEditing(false);
      await openDetail(selectedId);
      await load();
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : 'No se pudo actualizar la clase.');
    } finally {
      setEditSaving(false);
    }
  };

  const marcarJustificado = async (asistenciaId: number) => {
    setSaving(asistenciaId);
    try {
      await updateAttendance(asistenciaId, 'ausente_justificado');
      showToast('Ausencia marcada como justificada.');
      if (selectedId) await openDetail(selectedId);
      await load();
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : 'No se pudo actualizar la asistencia.');
    } finally {
      setSaving(null);
    }
  };

  if (error) {
    return <ContentState tone="error" title="No se pudieron cargar las clases" detail={error} actions={<button className="button" type="button" onClick={() => void load()}>Reintentar</button>} />;
  }
  if (!clases) {
    return <ContentState tone="loading" title="Cargando clases…" detail="Estamos consultando el historial de clases dictadas." />;
  }
  if (clases.length === 0) {
    return <ContentState tone="empty" title="Todavía no registraste clases" detail="Cuando registres una clase desde 'Iniciar clase' aparecerá acá." />;
  }

  return (
    <section className="panel">
      <header className="panel-header"><h2>Clases dadas</h2><p>Historial de clases que registraste. Podés marcar una ausencia como justificada.</p></header>
      <div className="card-grid" style={{ marginTop: 12 }}>
        {clases.map((c) => (
          <button
            key={c.id}
            type="button"
            className="nav-card"
            onClick={() => void openDetail(c.id)}
            style={{ textAlign: 'left' }}
          >
            <span>{c.fechaClase ?? 'Sin fecha'}</span>
            <h3>{c.materiaNombre ?? 'Materia'} — {c.cursoDescripcion}</h3>
            <p style={{ marginTop: 4 }}>{c.tema}</p>
            <strong style={{ marginTop: 8 }}>
              Ausentes: {c.totalAusentes} · Justificados: {c.totalJustificados} · Total: {c.totalAlumnos}
            </strong>
          </button>
        ))}
      </div>

      {selectedId && (
        <div className="data-modal" role="dialog" aria-modal="true" aria-labelledby="clase-detalle-title" tabIndex={-1} style={{ maxWidth: 720 }}>
          <div className="signature-modal-header">
            <div>
              <span>Clase dictada</span>
              <h2 id="clase-detalle-title">Detalle de clase</h2>
            </div>
            <button type="button" className="signature-modal-close" aria-label="Cerrar detalle" onClick={() => { setSelectedId(null); setSelected(null); }}>×</button>
          </div>
            {detailLoading || !selected ? (
              <ContentState tone="loading" title="Cargando detalle…" />
            ) : (
              <>
                <div className="signature-modal-actions" style={{ justifyContent: 'flex-start', marginBottom: 12 }}>
                  {edicionVigente(selected.fechaClase) ? <button type="button" className="button secondary" onClick={beginEdit}>Editar</button> : <span className="tag">El plazo de edición ya venció.</span>}
                </div>
                {editing ? <>
                  <RasgosAsistenciaEditor
                    alumnos={selected.asistencias.map((item) => ({ id: item.alumnoId, nombre: item.alumnoNombreCompleto, apellido: '' }))}
                    tema={editTema}
                    onTemaChange={setEditTema}
                    ausentes={editAusentes}
                    onAusenteChange={(alumnoId, ausente) => setEditAusentes((current) => ausente ? [...current, alumnoId] : current.filter((id) => id !== alumnoId))}
                    codigosPorAlumno={editCodigos}
                    onCodigoChange={(alumnoId, codigo) => setEditCodigos((current) => {
                      const actual = current[alumnoId] ?? [];
                      return { ...current, [alumnoId]: actual.includes(codigo) ? actual.filter((item) => item !== codigo) : [...actual, codigo] };
                    })}
                    codigosConducta={codigosConducta}
                    titulo="Editar asistencia y justificativos"
                    descripcion="Actualizá el tema, la asistencia y los rasgos conductuales de esta clase."
                  />
                  <div className="signature-modal-actions">
                    <button type="button" className="button secondary" disabled={editSaving} onClick={() => setEditing(false)}>Cancelar</button>
                    <button type="button" className="button" disabled={editSaving || !editTema.trim()} onClick={() => void saveEdit()}>{editSaving ? 'Guardando…' : 'Guardar cambios'}</button>
                  </div>
                </> : <>
                <p><strong>Tema:</strong> {selected.tema}</p>
                <p><strong>Fecha:</strong> {selected.fechaClase ?? 'Sin fecha'}</p>
                <table className="grade-table" style={{ marginTop: 12, width: '100%' }}>
                  <thead><tr><th>Alumno</th><th>Estado</th><th>Acción</th></tr></thead>
                  <tbody>
                    {selected.asistencias.map((a) => (
                      <tr key={a.id}>
                        <td>{a.alumnoNombreCompleto}</td>
                        <td>{estadoLabel(a.estado)}</td>
                        <td>
                          {a.estado === 'ausente' ? (
                            <button
                              type="button"
                              className="button small"
                              disabled={saving === a.id}
                              onClick={() => void marcarJustificado(a.id)}
                            >
                              {saving === a.id ? 'Guardando…' : 'Marcar justificada'}
                            </button>
                          ) : (
                            <span className="tag">—</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                </>}
              </>
            )}
        </div>
      )}
    </section>
  );
}
