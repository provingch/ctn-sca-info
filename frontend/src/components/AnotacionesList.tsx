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

/** Con fotoAlAmpliar la evidencia no se descarga hasta que se pide con el botón de la anotación. */
export default function AnotacionesList({ anotaciones, loadFoto, mostrarAlumno = false, fotoAlAmpliar = false }: {
  anotaciones: Anotacion[];
  loadFoto: (anotacion: Anotacion) => Promise<Blob>;
  mostrarAlumno?: boolean;
  fotoAlAmpliar?: boolean;
}) {
  const [ampliadas, setAmpliadas] = useState<Set<number>>(() => new Set());
  const alternar = (id: number) => setAmpliadas((actual) => {
    const siguiente = new Set(actual);
    if (!siguiente.delete(id)) siguiente.add(id);
    return siguiente;
  });

  return <ul className={`anotaciones-list${fotoAlAmpliar ? ' anotaciones-list--ficha' : ''}`}>
    {anotaciones.map((anotacion) => <li key={anotacion.id} className={`anotacion-item${fotoAlAmpliar ? ' anotacion-item--compacta' : ''}`}>
      {anotacion.tieneFoto && (!fotoAlAmpliar || ampliadas.has(anotacion.id)) && <AnotacionFoto anotacion={anotacion} loadFoto={loadFoto} />}
      <div className="anotacion-body">
        {mostrarAlumno && <p className="anotacion-alumno"><strong>{anotacion.alumnoApellido}, {anotacion.alumnoNombre}</strong>{anotacion.curso && <span> · {anotacion.curso}</span>}</p>}
        <h3>{anotacion.tipo}</h3>
        {anotacion.observacion && <p>{anotacion.observacion}</p>}
        <p className="anotacion-meta">
          {formatSqlDateTime(anotacion.fechaHora)}
          {anotacion.materia && ` · ${anotacion.materia}`}
          {anotacion.profesor && ` · ${anotacion.profesor}`}
        </p>
        {fotoAlAmpliar && anotacion.tieneFoto && <button type="button" className="button secondary anotacion-ampliar" aria-expanded={ampliadas.has(anotacion.id)} onClick={() => alternar(anotacion.id)}>
          {ampliadas.has(anotacion.id) ? 'Ocultar evidencia' : 'Ver evidencia'}
        </button>}
      </div>
    </li>)}
  </ul>;
}
