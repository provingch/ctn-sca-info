import { useEffect, useState } from 'react';
import type { Anotacion } from '../api/anotaciones';
import { formatSqlDateTime } from '../utils/date';
import './anotaciones.css';

/** Miniatura de la foto de evidencia: se pide con el token y se muestra como object URL. */
function AnotacionFoto({ anotacion, loadFoto }: { anotacion: Anotacion; loadFoto: (anotacion: Anotacion) => Promise<Blob> }) {
  const [url, setUrl] = useState<string | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let active = true;
    let objectUrl: string | null = null;
    loadFoto(anotacion)
      .then((blob) => {
        if (!active) return;
        objectUrl = URL.createObjectURL(blob);
        setUrl(objectUrl);
      })
      .catch(() => { if (active) setFailed(true); });
    return () => {
      active = false;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [anotacion, loadFoto]);

  if (failed) return <div className="anotacion-foto anotacion-foto--vacia">Foto no disponible</div>;
  if (!url) return <div className="anotacion-foto anotacion-foto--vacia" aria-busy="true">Cargando foto…</div>;
  return <a className="anotacion-foto" href={url} target="_blank" rel="noreferrer" aria-label={`Ver foto de evidencia: ${anotacion.tipo}`}>
    <img src={url} alt={`Evidencia de la anotación: ${anotacion.tipo}`} loading="lazy" />
  </a>;
}

export default function AnotacionesList({ anotaciones, loadFoto, mostrarAlumno = false }: {
  anotaciones: Anotacion[];
  loadFoto: (anotacion: Anotacion) => Promise<Blob>;
  mostrarAlumno?: boolean;
}) {
  return <ul className="anotaciones-list">
    {anotaciones.map((anotacion) => <li key={anotacion.id} className="anotacion-item">
      {anotacion.tieneFoto && <AnotacionFoto anotacion={anotacion} loadFoto={loadFoto} />}
      <div className="anotacion-body">
        {mostrarAlumno && <p className="anotacion-alumno"><strong>{anotacion.alumnoApellido}, {anotacion.alumnoNombre}</strong>{anotacion.curso && <span> · {anotacion.curso}</span>}</p>}
        <h3>{anotacion.tipo}</h3>
        {anotacion.observacion && <p>{anotacion.observacion}</p>}
        <p className="anotacion-meta">
          {formatSqlDateTime(anotacion.fechaHora)}
          {anotacion.materia && ` · ${anotacion.materia}`}
          {anotacion.profesor && ` · ${anotacion.profesor}`}
        </p>
      </div>
    </li>)}
  </ul>;
}
