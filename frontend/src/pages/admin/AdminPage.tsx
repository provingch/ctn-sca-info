import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Link, useLocation } from 'react-router-dom';
import AppShell from '../../components/AppShell';
import { createAdminRecord, deleteAssignment, getAdminCatalog, type AdminCatalog } from '../../api/admin';
import { ApiError } from '../../api/client';
import { useSpecialty } from '../../context/SpecialtyContext';
import { normalizeSpecialty } from '../../theme/theme';

const modules = [
  { path: '/admin/materias', key: 'materias', title: 'Materias', detail: 'Catálogo, categorías y especialidades' },
  { path: '/admin/usuarios', key: 'usuarios', title: 'Usuarios', detail: 'Altas, roles y datos de acceso' },
  { path: '/admin/asignaciones', key: 'asignaciones', title: 'Asignaciones', detail: 'Profesor, materia y curso' },
  { path: '/admin/ingresantes', key: 'ingresantes', title: 'Ingresantes', detail: 'Carga de nuevos alumnos' },
];

export default function AdminPage() {
  const location = useLocation();
  const selected = modules.find((module) => module.path === location.pathname);
  const [data, setData] = useState<AdminCatalog | null>(null);
  const [status, setStatus] = useState('');
  const load = useCallback(async () => {
    try {
      setData(await getAdminCatalog());
    } catch (error) {
      setStatus(error instanceof ApiError ? error.message : 'No se pudo cargar el catálogo.');
    }
  }, []);

  useEffect(() => { void load(); }, [load]);

  if (!data) {
    return <AppShell title={selected?.title || 'Panel general'}><section className="panel">{status || 'Cargando…'}</section></AppShell>;
  }

  return <AppShell title={selected?.title || 'Panel general'}>
    <AdminToolbar data={data} showBack={Boolean(selected)} />
    {status && <div className="notice" role="status">{status}</div>}
    {!selected
      ? <div className="card-grid">{modules.map((module) => <Link className="nav-card" to={module.path} key={module.path}><span>Gestionar</span><h2>{module.title}</h2><p>{module.detail}</p><strong>Abrir →</strong></Link>)}</div>
      : <AdminModule module={selected} data={data} reload={load} status={setStatus} />}
  </AppShell>;
}

function AdminToolbar({ data, showBack }: { data: AdminCatalog; showBack: boolean }) {
  const { id, name, selectSpecialty, resetSpecialty } = useSpecialty();
  const selectedId = id ?? data.especialidades.find((item) => normalizeSpecialty(item.nombre) === normalizeSpecialty(name))?.id ?? 0;

  function changePalette(value: number) {
    const specialty = data.especialidades.find((item) => item.id === value);
    if (specialty) selectSpecialty(specialty.nombre, specialty.id);
    else resetSpecialty();
  }

  return <div className="toolbar filters admin-toolbar">
    {showBack && <Link className="button secondary" to="/admin">← Panel general</Link>}
    <label className="inline-filter">Paleta del sistema
      <select value={selectedId} onChange={(event) => changePalette(Number(event.target.value))}>
        <option value="0">Institucional (predeterminada)</option>
        {data.especialidades.map((specialty) => <option key={specialty.id} value={specialty.id}>{specialty.nombre}</option>)}
      </select>
    </label>
  </div>;
}

function AdminModule({ module, data, reload, status }: {
  module: typeof modules[number]; data: AdminCatalog; reload: () => Promise<void>; status: (message: string) => void;
}) {
  async function submit(payload: unknown) {
    try {
      await createAdminRecord(module.key, payload);
      status('Registro creado.');
      await reload();
    } catch (error) {
      status(error instanceof ApiError ? error.message : 'No se pudo guardar.');
    }
  }

  return <>
    <section className="panel"><h2>Crear registro</h2><CreateForm section={module.key} data={data} submit={submit} /></section>
    <section className="panel"><h2>{module.title} existentes</h2><AdminList section={module.key} data={data} reload={reload} status={status} /></section>
  </>;
}

function CreateForm({ section, data, submit }: { section: string; data: AdminCatalog; submit: (payload: unknown) => void }) {
  const [form, setForm] = useState<Record<string, string>>({ categoria: 'comun', nivel: '1' });
  const { selectSpecialty } = useSpecialty();
  function numeric(name: string) { return form[name] ? Number(form[name]) : null; }
  function send(event: FormEvent) {
    event.preventDefault();
    if (section === 'materias') submit({ nombre: form.nombre, categoria: form.categoria, especialidadIds: numeric('especialidadId') ? [numeric('especialidadId')] : [] });
    if (section === 'usuarios') submit({ ...form, nivel: numeric('nivel'), especialidadId: numeric('especialidadId') });
    if (section === 'asignaciones') submit({ profesorId: numeric('profesorId'), materiaId: numeric('materiaId'), cursoId: numeric('cursoId') });
    if (section === 'ingresantes') submit({ ...form, cursoId: numeric('cursoId'), ci: numeric('ci') });
  }
  const field = (name: string, label: string, type = 'text') => <label>{label}<input type={type} required={['nombre', 'apellido', 'usuario'].includes(name)} value={form[name] || ''} onChange={(event) => setForm({ ...form, [name]: event.target.value })} /></label>;
  const specialtyChanged = (value: string) => {
    const specialty = data.especialidades.find((item) => item.id === Number(value));
    if (specialty) selectSpecialty(specialty.nombre, specialty.id);
  };

  return <form className="form-grid" onSubmit={send}>
    {section === 'materias' && <>{field('nombre', 'Nombre')}<label>Categoría<select value={form.categoria} onChange={(event) => setForm({ ...form, categoria: event.target.value })}><option value="comun">Común</option><option value="especifico">Específica</option></select></label><Specialties data={data} form={form} setForm={setForm} onChange={specialtyChanged} /></>}
    {section === 'usuarios' && <>{field('nombre', 'Nombre')}{field('apellido', 'Apellido')}{field('usuario', 'Usuario')}{field('contrasenia', 'Contraseña', 'password')}<label>Nivel<select value={form.nivel} onChange={(event) => setForm({ ...form, nivel: event.target.value })}><option value="1">Profesor</option><option value="2">Evaluador</option><option value="3">Administrador</option></select></label>{field('correo', 'Correo', 'email')}<Specialties data={data} form={form} setForm={setForm} onChange={specialtyChanged} /></>}
    {section === 'asignaciones' && <><Select label="Profesor" name="profesorId" items={data.usuarios.map((user) => ({ id: user.id, label: `${user.apellido}, ${user.nombre}` }))} form={form} setForm={setForm} /><Select label="Materia" name="materiaId" items={data.materias.map((subject) => ({ id: subject.id, label: subject.nombre }))} form={form} setForm={setForm} /><Select label="Curso" name="cursoId" items={courseItems(data)} form={form} setForm={setForm} /></>}
    {section === 'ingresantes' && <>{field('nombre', 'Nombre')}{field('apellido', 'Apellido')}{field('ci', 'Cédula', 'number')}<Select label="Curso" name="cursoId" items={courseItems(data)} form={form} setForm={setForm} />{field('correoEncargado', 'Correo del encargado', 'email')}{field('correoEncargado2', 'Segundo correo', 'email')}</>}
    <button className="button">Guardar</button>
  </form>;
}

function courseItems(data: AdminCatalog) {
  return data.cursos.map((course) => ({ id: course.id, label: `${course.nivel}° ${course.seccion} · ${course.especialidad}` }));
}

function Specialties({ data, form, setForm, onChange }: FormProps & { data: AdminCatalog; onChange: (value: string) => void }) {
  return <Select label="Especialidad" name="especialidadId" items={data.especialidades.map((specialty) => ({ id: specialty.id, label: specialty.nombre }))} form={form} setForm={setForm} optional onValueChange={onChange} />;
}

type FormProps = { form: Record<string, string>; setForm: (value: Record<string, string>) => void };
function Select({ label, name, items, form, setForm, optional, onValueChange }: FormProps & { label: string; name: string; items: Array<{ id: number; label: string }>; optional?: boolean; onValueChange?: (value: string) => void }) {
  return <label>{label}<select required={!optional} value={form[name] || ''} onChange={(event) => { setForm({ ...form, [name]: event.target.value }); onValueChange?.(event.target.value); }}><option value="">Seleccione…</option>{items.map((item) => <option key={item.id} value={item.id}>{item.label}</option>)}</select></label>;
}

function AdminList({ section, data, reload, status }: { section: string; data: AdminCatalog; reload: () => Promise<void>; status: (message: string) => void }) {
  if (section === 'materias') return <div className="admin-list">{data.materias.map((subject) => <div key={subject.id}><strong>{subject.nombre}</strong><span>{subject.categoria}</span></div>)}</div>;
  if (section === 'usuarios') return <div className="admin-list">{data.usuarios.map((user) => <div key={user.id}><strong>{user.apellido}, {user.nombre}</strong><span>{user.usuario} · nivel {user.nivel}</span></div>)}</div>;
  if (section === 'ingresantes') return <div className="admin-list">{data.alumnos.map((student) => <div key={student.id}><strong>{student.apellido}, {student.nombre}</strong><span>{data.cursos.find((course) => course.id === student.cursoId)?.especialidad || 'Curso'} · CI {student.ci || '—'}</span></div>)}</div>;
  async function remove(id: number) {
    if (!window.confirm('¿Eliminar esta asignación?')) return;
    try { await deleteAssignment(id); status('Asignación eliminada.'); await reload(); }
    catch (error) { status(error instanceof ApiError ? error.message : 'No se pudo eliminar.'); }
  }
  return <div className="admin-list">{data.asignaciones.map((assignment) => <div key={assignment.id}><span><strong>{assignment.profesor}</strong><small>{assignment.materia} · {assignment.curso}</small></span><button className="button danger" onClick={() => remove(assignment.id)}>Eliminar</button></div>)}</div>;
}
