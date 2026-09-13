import { useCallback, useEffect, useState } from 'react';
import { ApiError } from '../../api/client';
import { getMiClase, getMisClases, updateAttendance, type ClaseDadaDto, type ClaseDetalleDto } from '../../api/home';
import ContentState from '../../components/ui/ContentState';
import { useToast } from '../../context/toast';

function estadoLabel(estado: string): string {
  switch (estado) {
    case 'presente': return 'Presente';
    case 'ausente': return 'Ausente';
    case 'ausente_justificado': return 'Ausente justificado';
    case 'pendiente': return 'Pendiente';
    default: return estado;
  }
}

export default function MisClasesView() {
  const [clases, setClases] = useState<ClaseDadaDto[] | null>(null);
  const [error, setError] = useState('');
  const [selected, setSelected] = useState<ClaseDetalleDto | null>(null);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [saving, setSaving] = useState<number | null>(null);
  const { showToast } = useToast();

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
              </>
            )}
        </div>
      )}
    </section>
  );
}
