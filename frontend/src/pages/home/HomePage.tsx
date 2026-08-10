import { useEffect, useState } from 'react';
import { getHome, type HomeResponse } from '../../api/home';
import { ApiError } from '../../api/client';

export default function HomePage() {
  const [data, setData] = useState<HomeResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    getHome()
      .then((res) => {
        if (!cancelled) setData(res);
      })
      .catch((err) => {
        if (!cancelled) setError(err instanceof ApiError ? err.message : 'Error al cargar el home.');
      });
    return () => {
      cancelled = true;
    };
  }, []);

  if (error) return <p className="form-error">{error}</p>;
  if (!data) return <p>Cargando…</p>;

  return (
    <div>
      <h1>Home</h1>
      <p>
        Curso seleccionado: {data.selCurso ? `${data.selCurso.curso} ${data.selCurso.seccion}` : 'ninguno'}
      </p>
      <p>Planillas: {data.planillas.length}</p>
      {/* Bloque 3 en curso: acá va Planilla/Evaluación cuando su pantalla esté lista */}
    </div>
  );
}
