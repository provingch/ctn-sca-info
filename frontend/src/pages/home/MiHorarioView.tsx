import { useEffect, useState } from 'react';
import { getMiHorario } from '../../api/home';
import type { CargaHorariaItem } from '../../api/admin';
import { ApiError } from '../../api/client';
import CargaHorariaView from '../admin/CargaHorariaView';

export default function MiHorarioView() {
  const [data, setData] = useState<CargaHorariaItem | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    getMiHorario().then((result) => {
      if (active) setData(result);
    }).catch((err) => {
      if (active) setError(err instanceof ApiError ? err.message : 'No se pudo cargar tu horario.');
    });
    return () => { active = false; };
  }, []);

  if (error) return <div className="panel"><div className="notice error"><p className="u-m0">No se pudo cargar tu horario. {error}</p></div></div>;
  if (!data) return <div className="panel"><p className="u-m0">Cargando tu horario…</p></div>;
  return (
    <div className="panel">
      <h2>Mi horario semanal</h2>
      <CargaHorariaView data={data} />
    </div>
  );
}
