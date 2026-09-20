import { useEffect, useState, type FormEvent } from 'react';
import { ApiError } from '../api/client';
import { crearCodigoConducta, desactivarCodigoConducta, listarCodigosConducta, type CodigoConducta } from '../api/home';

interface CatalogoConductaPanelProps {
  onCodesChange?: (codes: CodigoConducta[]) => void;
}

export default function CatalogoConductaPanel({ onCodesChange }: CatalogoConductaPanelProps) {
  const [codigosConducta, setCodigosConducta] = useState<CodigoConducta[]>([]);
  const [nuevoCodigo, setNuevoCodigo] = useState('');
  const [nuevaDescripcion, setNuevaDescripcion] = useState('');
  const [catalogStatus, setCatalogStatus] = useState('');

  async function loadCodigosConducta() {
    try {
      const codes = await listarCodigosConducta();
      setCodigosConducta(codes);
      onCodesChange?.(codes);
    } catch (err) {
      setCatalogStatus(err instanceof ApiError ? err.message : 'No se pudo cargar el catálogo de conducta.');
    }
  }

  // Carga inicial única: loadCodigosConducta se recrea en cada render y onCodesChange suele llegar inline.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => { void loadCodigosConducta(); }, []);

  async function saveCodigoConducta(event: FormEvent) {
    event.preventDefault();
    const codigo = nuevoCodigo.trim().toUpperCase();
    const descripcion = nuevaDescripcion.trim();
    if (!codigo || codigo.length > 10 || !/^N[A-Z0-9]{0,9}$/.test(codigo)) {
      setCatalogStatus('El código debe comenzar con N, tener entre 2 y 10 caracteres y usar solo letras o números.');
      return;
    }
    if (!descripcion || descripcion.length > 255) {
      setCatalogStatus('La descripción es obligatoria y no puede superar 255 caracteres.');
      return;
    }
    try {
      await crearCodigoConducta(codigo, descripcion);
      setNuevoCodigo('');
      setNuevaDescripcion('');
      setCatalogStatus('Código de conducta guardado.');
      await loadCodigosConducta();
    } catch (err) {
      setCatalogStatus(err instanceof ApiError ? err.message : 'No se pudo guardar el código de conducta.');
    }
  }

  async function disableCodigoConducta(item: CodigoConducta) {
    if (!window.confirm(`¿Desactivar ${item.codigo}?`)) return;
    try {
      await desactivarCodigoConducta(item.id);
      setCatalogStatus('Código de conducta desactivado.');
      await loadCodigosConducta();
    } catch (err) {
      setCatalogStatus(err instanceof ApiError ? err.message : 'No se pudo desactivar el código de conducta.');
    }
  }

  return <section className="panel" style={{ gridColumn: '1 / -1' }}>
    <div className="class-card-head"><div><span>Administración</span><h3>Catálogo de conducta</h3></div></div>
    <form className="form-grid" onSubmit={saveCodigoConducta}>
      <label>Código<input value={nuevoCodigo} maxLength={10} placeholder="Ej.: N10" onChange={(event) => setNuevoCodigo(event.target.value.toUpperCase())} required /></label>
      <label>Descripción<input value={nuevaDescripcion} maxLength={255} placeholder="Descripción del rasgo" onChange={(event) => setNuevaDescripcion(event.target.value)} required /></label>
      <span className="admin-actions"><button className="button" type="submit">Agregar código</button></span>
    </form>
    <div className="admin-list">{codigosConducta.map((item) => <div key={item.id}><span><strong>{item.codigo}</strong> {item.descripcion}</span><button className="button danger" type="button" onClick={() => void disableCodigoConducta(item)}>Desactivar</button></div>)}</div>
    {catalogStatus && <p className="notice" role="status">{catalogStatus}</p>}
  </section>;
}
