import { useEffect, useState, type FormEvent } from 'react';
import { Link, useLocation } from 'react-router-dom';
import AppShell from '../../components/AppShell';
import { createAdminRecord, deleteAssignment, getAdminCatalog, type AdminCatalog } from '../../api/admin';
import { ApiError } from '../../api/client';

const modules = [
  { path: '/admin/materias', key: 'materias', title: 'Materias', detail: 'Catálogo, categorías y especialidades' },
  { path: '/admin/usuarios', key: 'usuarios', title: 'Usuarios', detail: 'Altas, roles y datos de acceso' },
  { path: '/admin/asignaciones', key: 'asignaciones', title: 'Asignaciones', detail: 'Profesor, materia y curso' },
  { path: '/admin/ingresantes', key: 'ingresantes', title: 'Ingresantes', detail: 'Carga de nuevos alumnos' },
];
export default function AdminPage() {
  const location = useLocation();
  const selected = modules.find((m) => m.path === location.pathname);
  if (!selected) return <AppShell title="Panel general"><div className="card-grid">{modules.map((m) => <Link className="nav-card" to={m.path} key={m.path}><span>Gestionar</span><h2>{m.title}</h2><p>{m.detail}</p><strong>Abrir →</strong></Link>)}</div></AppShell>;
  return <AdminModule module={selected} />;
}

function AdminModule({ module }: { module: typeof modules[number] }) {
  const [data, setData] = useState<AdminCatalog | null>(null); const [status, setStatus] = useState('');
  const load = () => getAdminCatalog().then(setData).catch((e) => setStatus(e instanceof ApiError ? e.message : 'No se pudo cargar el catálogo.'));
  useEffect(() => { void load(); }, []);
  async function submit(payload: unknown) { try { await createAdminRecord(module.key, payload); setStatus('Registro creado.'); await load(); } catch (e) { setStatus(e instanceof ApiError ? e.message : 'No se pudo guardar.'); } }
  if (!data) return <AppShell title={module.title}><section className="panel">{status || 'Cargando…'}</section></AppShell>;
  return <AppShell title={module.title}><div className="toolbar"><Link className="button secondary" to="/admin">← Panel general</Link></div>{status && <div className="notice">{status}</div>}<section className="panel"><h2>Crear registro</h2><CreateForm section={module.key} data={data} submit={submit} /></section><section className="panel"><h2>{module.title} existentes</h2><AdminList section={module.key} data={data} reload={load} status={setStatus} /></section></AppShell>;
}

function CreateForm({ section, data, submit }: { section: string; data: AdminCatalog; submit: (p: unknown) => void }) {
  const [form, setForm] = useState<Record<string, string>>({ categoria: 'comun', nivel: '1' });
  function send(e: FormEvent) { e.preventDefault(); const numeric = (name: string) => form[name] ? Number(form[name]) : null; if (section === 'materias') submit({ nombre: form.nombre, categoria: form.categoria, especialidadIds: numeric('especialidadId') ? [numeric('especialidadId')] : [] }); if (section === 'usuarios') submit({ ...form, nivel: numeric('nivel'), especialidadId: numeric('especialidadId') }); if (section === 'asignaciones') submit({ profesorId: numeric('profesorId'), materiaId: numeric('materiaId'), cursoId: numeric('cursoId') }); if (section === 'ingresantes') submit({ ...form, cursoId: numeric('cursoId'), ci: numeric('ci') }); }
  const field = (name: string, label: string, type = 'text') => <label>{label}<input type={type} required={['nombre','apellido','usuario'].includes(name)} value={form[name] || ''} onChange={(e) => setForm({ ...form, [name]: e.target.value })} /></label>;
  return <form className="form-grid" onSubmit={send}>{section === 'materias' && <>{field('nombre', 'Nombre')}<label>Categoría<select value={form.categoria} onChange={(e) => setForm({ ...form, categoria: e.target.value })}><option value="comun">Común</option><option value="especifico">Específica</option></select></label><Specialties data={data} form={form} setForm={setForm} /></>}{section === 'usuarios' && <>{field('nombre','Nombre')}{field('apellido','Apellido')}{field('usuario','Usuario')}{field('contrasenia','Contraseña','password')}<label>Nivel<select value={form.nivel} onChange={(e) => setForm({ ...form, nivel: e.target.value })}><option value="1">Profesor</option><option value="2">Evaluador</option><option value="3">Administrador</option></select></label>{field('correo','Correo','email')}<Specialties data={data} form={form} setForm={setForm} /></>}{section === 'asignaciones' && <><Select label="Profesor" name="profesorId" items={data.usuarios.map((u) => ({ id: u.id, label: `${u.apellido}, ${u.nombre}` }))} form={form} setForm={setForm} /><Select label="Materia" name="materiaId" items={data.materias.map((m) => ({ id: m.id, label: m.nombre }))} form={form} setForm={setForm} /><Select label="Curso" name="cursoId" items={data.cursos.map((c) => ({ id: c.id, label: `${c.nivel}° ${c.seccion} · ${c.especialidad}` }))} form={form} setForm={setForm} /></>}{section === 'ingresantes' && <>{field('nombre','Nombre')}{field('apellido','Apellido')}{field('ci','Cédula','number')}<Select label="Curso" name="cursoId" items={data.cursos.map((c) => ({ id: c.id, label: `${c.nivel}° ${c.seccion} · ${c.especialidad}` }))} form={form} setForm={setForm} />{field('correoEncargado','Correo del encargado','email')}{field('correoEncargado2','Segundo correo','email')}</>}<button className="button">Guardar</button></form>;
}
function Specialties({ data, form, setForm }: FormProps & { data: AdminCatalog }) { return <Select label="Especialidad" name="especialidadId" items={data.especialidades.map((e) => ({ id: e.id, label: e.nombre }))} form={form} setForm={setForm} optional />; }
type FormProps = { form: Record<string,string>; setForm: (value: Record<string,string>) => void };
function Select({ label, name, items, form, setForm, optional }: FormProps & { label: string; name: string; items: Array<{id:number;label:string}>; optional?: boolean }) { return <label>{label}<select required={!optional} value={form[name] || ''} onChange={(e) => setForm({ ...form, [name]: e.target.value })}><option value="">Seleccione…</option>{items.map((i) => <option key={i.id} value={i.id}>{i.label}</option>)}</select></label>; }

function AdminList({ section, data, reload, status }: { section: string; data: AdminCatalog; reload: () => Promise<void>; status: (s:string) => void }) {
  if (section === 'materias') return <div className="admin-list">{data.materias.map((m) => <div key={m.id}><strong>{m.nombre}</strong><span>{m.categoria}</span></div>)}</div>;
  if (section === 'usuarios') return <div className="admin-list">{data.usuarios.map((u) => <div key={u.id}><strong>{u.apellido}, {u.nombre}</strong><span>{u.usuario} · nivel {u.nivel}</span></div>)}</div>;
  if (section === 'ingresantes') return <div className="admin-list">{data.alumnos.map((a) => <div key={a.id}><strong>{a.apellido}, {a.nombre}</strong><span>{data.cursos.find((c) => c.id === a.cursoId)?.especialidad || 'Curso'} · CI {a.ci || '—'}</span></div>)}</div>;
  async function remove(id: number) { if (!window.confirm('¿Eliminar esta asignación?')) return; try { await deleteAssignment(id); status('Asignación eliminada.'); await reload(); } catch (e) { status(e instanceof ApiError ? e.message : 'No se pudo eliminar.'); } }
  return <div className="admin-list">{data.asignaciones.map((a) => <div key={a.id}><span><strong>{a.profesor}</strong><small>{a.materia} · {a.curso}</small></span><button className="button danger" onClick={() => remove(a.id)}>Eliminar</button></div>)}</div>;
}
