import { useCallback, useEffect, useMemo, useState } from 'react';
import { ApiError } from '../../api/client';
import { getSistemaEstado, type SistemaEstadoResponse } from '../../api/admin';
import ConnectionState from '../../components/ui/ConnectionState';
import ContentState from '../../components/ui/ContentState';
import { useToast } from '../../context/toast';
import { formatBytes, formatRelativeDate } from './adminFormatters';
import { formatSqlDateTime } from '../../utils/date';

export default function SistemaEstadoPanel() {
  const [data, setData] = useState<SistemaEstadoResponse | null>(null);
  const { showToast } = useToast();
  const setError = useCallback((msg: string) => {
    if (msg) showToast(msg, { tone: 'error', autoDismiss: false });
  }, [showToast]);
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async () => {
    setRefreshing(true);
    setError('');
    try {
      setData(await getSistemaEstado());
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : 'No se pudo consultar el estado del sistema.');
    } finally {
      setRefreshing(false);
    }
  }, [setError]);

  useEffect(() => { void load(); }, [load]);
  const migrations = useMemo(() => [...(data?.migraciones ?? [])].sort((first, second) => (second.appliedAt ?? '').localeCompare(first.appliedAt ?? '')), [data]);

  if (!data && refreshing) return <ContentState tone="loading" title="Consultando el sistema…" detail="Estamos verificando servicios y migraciones." />;
  if (!data) return <ContentState tone="error" title={'No se pudo consultar el sistema'} detail="Usá Actualizar para volver a intentarlo." actions={<button className="button" type="button" onClick={() => void load()}>Actualizar</button>} />;

  return <div className="system-status-layout">
    <div className="toolbar system-status-toolbar"><p>Información operativa actual del servidor.</p><button className="button secondary" type="button" disabled={refreshing} onClick={() => void load()}>{refreshing ? 'Actualizando…' : 'Actualizar'}</button></div>
    {/* operational errors are shown as persistent toasts via ToastProvider */}
    <section className="system-status-grid" aria-label="Resumen del sistema">
      <article className={`panel system-db-card ${data.dbConectada ? 'is-connected' : 'is-disconnected'}`}><span>Base de datos</span><ConnectionState active={data.dbConectada} title={data.dbConectada ? 'Conectada' : 'Sin conexión'} detail={data.dbConectada ? 'El servicio responde correctamente.' : 'Requiere revisión del servidor.'} /></article>
      <article className="panel"><span>Classroom</span><strong>{formatRelativeDate(data.ultimaSyncClassroom)}</strong><small>Última sincronización registrada</small></article>
      <article className="panel"><span>Logs de actividad</span><strong>{formatBytes(data.espacioLogsBytes)}</strong><small>Espacio utilizado actualmente</small></article>
    </section>
    <section className="panel system-migrations-panel">
      <header className="admin-summary-heading"><div><span>Base de datos</span><h2>Migraciones aplicadas</h2></div><strong>{migrations.length}</strong></header>
      {migrations.length === 0 ? <ContentState compact title="Sin migraciones registradas" detail="El servidor no informó migraciones aplicadas." /> : <div className="admin-list system-migrations-list">
        {migrations.map((migration, index) => <div key={`${migration.version}-${migration.appliedAt ?? index}`}><span><strong>{migration.version}</strong><small>{formatSqlDateTime(migration.appliedAt, { dateStyle: 'medium', timeStyle: 'short' })}</small></span>{index === 0 && <em>Más reciente</em>}</div>)}
      </div>}
    </section>
  </div>;
}
