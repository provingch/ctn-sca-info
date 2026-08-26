import { useEffect, useState } from 'react';
import { ApiError } from '../../api/client';
import { createSala, deleteSala, getSalas, updateSala, type AdminCatalog, type SalaItem } from '../../api/admin';

export default function SalasPanel({ data, status }: { data: AdminCatalog; status: (message: string) => void }) {
  const [items, setItems] = useState<SalaItem[]>([]);
  const [name, setName] = useState('');
  const [specialty, setSpecialty] = useState('');
  const [editing, setEditing] = useState<number | null>(null);
  const load = () => getSalas().then(setItems).catch((reason) => status(reason instanceof ApiError ? reason.message : 'No se pudo cargar las salas.'));
  useEffect(() => { void load(); }, []);
  const save = async () => {
    try { const payload = { nombre: name.trim(), especialidadId: specialty ? Number(specialty) : null }; if (!payload.nombre) { status('El nombre de la sala es requerido.'); return; } if (editing) await updateSala(editing, payload); else await createSala(payload); setName(''); setSpecialty(''); setEditing(null); await load(); status('Sala guardada.'); }
    catch (reason) { status(reason instanceof ApiError ? reason.message : 'No se pudo guardar la sala.'); }
  };
  const grouped = new Map<string, SalaItem[]>();
  for (const item of items) { const key = item.especialidadNombre ?? 'Comunes'; grouped.set(key, [...(grouped.get(key) ?? []), item]); }
  return <div className="admin-summary-groups"><section className="panel admin-summary-section"><header className="admin-summary-heading"><div><span>Catálogo</span><h2>{editing ? 'Editar sala' : 'Nueva sala'}</h2></div></header><div className="form-grid salas-form"><label>Nombre<input value={name} maxLength={45} onChange={(event) => setName(event.target.value)} /></label><label>Especialidad / pabellón<select value={specialty} onChange={(event) => setSpecialty(event.target.value)}><option value="">Común</option>{data.especialidades.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></label><span className="admin-actions"><button className="button" type="button" onClick={() => void save()}>{editing ? 'Guardar cambios' : 'Agregar sala'}</button>{editing && <button className="button secondary" type="button" onClick={() => { setEditing(null); setName(''); setSpecialty(''); }}>Cancelar</button>}</span></div></section>{[...grouped.entries()].map(([group, rooms]) => <section className="panel admin-summary-section" key={group}><header className="admin-summary-heading"><h2>{group}</h2><strong>{rooms.length}</strong></header><div className="admin-list">{rooms.map((room) => <div key={room.id}><strong>{room.nombre}</strong><span className="admin-actions"><button className="button secondary" type="button" onClick={() => { setEditing(room.id); setName(room.nombre); setSpecialty(room.especialidadId ? String(room.especialidadId) : ''); }}>Editar</button><button className="button danger" type="button" onClick={() => { if (window.confirm(`¿Eliminar ${room.nombre}?`)) void deleteSala(room.id).then(load).catch((reason) => status(reason instanceof ApiError ? reason.message : 'No se pudo eliminar la sala.')); }}>Eliminar</button></span></div>)}</div></section>)}</div>;
}
