import { useCallback, useEffect, useState, type ChangeEvent, type FormEvent } from 'react';
import { Link, useLocation } from 'react-router-dom';
import AppShell from '../../components/AppShell';
import { createAdminRecord, deleteAssignment, getAdminCatalog, deleteAdminRecord, getMateriaEspecialidades, wipePlanillaSyncImports, type AdminCatalog } from '../../api/admin';
import { ApiError } from '../../api/client';
import { useSpecialty } from '../../context/SpecialtyContext';
import { normalizeSpecialty } from '../../theme/theme';
import AnimatedSelect from '../../components/AnimatedSelect';
import PasswordInput from '../../components/PasswordInput';

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
  const [wipeId, setWipeId] = useState('');
  const [wiping, setWiping] = useState(false);
  const [wipeResult, setWipeResult] = useState<string | null>(null);
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

    {/* Panel rápido de wipe de importaciones por planilla (solo administradores) */}
    <section className="panel">
      <h2>Wipe: importaciones Classroom</h2>
      <p>Ingresa el ID de la planilla para borrar tareas y notas importadas desde Classroom.</p>
      <label>
        Planilla ID
        <input type="number" value={wipeId} onChange={(e) => setWipeId(e.target.value)} />
      </label>
      <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
        <button className="button danger" disabled={wiping || !wipeId} onClick={async () => {
          if (!wipeId) return;
          if (!window.confirm('¿Confirmas borrar todas las tareas y notas importadas desde Classroom para la planilla ' + wipeId + '? Esta acción no se puede deshacer.')) return;
          try {
            setWiping(true);
            setWipeResult(null);
            const res = await wipePlanillaSyncImports(Number(wipeId));
            setWipeResult(res.message + ' (tareas borradas: ' + (res.deletedTasks ?? 0) + ', notas borradas: ' + (res.deletedGrades ?? 0) + ')');
            await load();
          } catch (err) {
            setWipeResult(err instanceof ApiError ? err.message : 'Error al ejecutar wipe');
          } finally { setWiping(false); }
        }}>Wipe planilla</button>
        <button className="button" onClick={() => { setWipeId(''); setWipeResult(null); }}>Limpiar</button>
      </div>
      {wiping && <div className="notice">Ejecutando wipe…</div>}
      {wipeResult && <div className="notice">{wipeResult}</div>}
    </section>
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
      <AnimatedSelect ariaLabel="Paleta del sistema" value={selectedId} onChange={(value) => changePalette(Number(value))} options={[{ value: 0, label: 'Institucional (predeterminada)' }, ...data.especialidades.map((specialty) => ({ value: specialty.id, label: specialty.nombre }))]} />
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
  const [especialidadIds, setEspecialidadIds] = useState<number[]>([]);
  const [materiaEspecialidadIds, setMateriaEspecialidadIds] = useState<number[]>([]);
  const { selectSpecialty } = useSpecialty();
  function numeric(name: string) { return form[name] ? Number(form[name]) : null; }
  function send(event: FormEvent) {
    event.preventDefault();
    if (section === 'materias') submit({ nombre: form.nombre, categoria: form.categoria, especialidadIds: especialidadIds });
    if (section === 'usuarios') submit({ ...form, nivel: numeric('nivel'), especialidadId: numeric('especialidadId') });
    if (section === 'asignaciones') submit({ profesorId: numeric('profesorId'), materiaId: numeric('materiaId'), cursoId: numeric('cursoId') });
    if (section === 'ingresantes') submit({ ...form, cursoId: numeric('cursoId'), ci: numeric('ci') });
  }
  const field = (name: string, label: string, type = 'text') => {
    const inputProps = { required: ['nombre', 'apellido', 'usuario'].includes(name), value: form[name] || '', onChange: (event: ChangeEvent<HTMLInputElement>) => setForm({ ...form, [name]: event.target.value }) };
    return <label>{label}{type === 'password' ? <PasswordInput {...inputProps} /> : <input type={type} {...inputProps} />}</label>;
  };
  const specialtyChanged = (value: string) => {
    const specialty = data.especialidades.find((item) => item.id === Number(value));
    if (specialty) selectSpecialty(specialty.nombre, specialty.id);
  };

  function handleCategoriaChange(value: string) {
    // If switching to 'especifico' while multiple especialidades are selected, ask before trimming
    if (value === 'especifico' && especialidadIds.length > 1) {
      const proceed = window.confirm('Al cambiar a "Específica" solo se conservará una especialidad. ¿Desea continuar y conservar la primera seleccionada?');
      if (!proceed) return;
      setEspecialidadIds([especialidadIds[0]]);
    }
    setForm({ ...form, categoria: value });
  }

  return (
    <form className="form-grid" onSubmit={send}>
      {section === 'materias' && (
        <>
          {field('nombre', 'Nombre')}
          <label>
            Categoría
            <AnimatedSelect ariaLabel="Categoría" value={form.categoria} onChange={handleCategoriaChange} options={[{ value: 'comun', label: 'Común' }, { value: 'especifico', label: 'Específica' }]} />
          </label>
          <Specialties data={data} form={form} setForm={setForm} onChange={specialtyChanged} especialidadIds={especialidadIds} setEspecialidadIds={setEspecialidadIds} categoria={form.categoria} />
        </>
      )}

      {section === 'usuarios' && (
        <>
          {field('nombre', 'Nombre')}
          {field('apellido', 'Apellido')}
          {field('usuario', 'Usuario')}
          {field('contrasenia', 'Contraseña', 'password')}
          <label>
            Nivel
            <AnimatedSelect ariaLabel="Nivel" value={form.nivel} onChange={(value) => setForm({ ...form, nivel: value })} options={[{ value: '1', label: 'Profesor' }, { value: '2', label: 'Evaluador' }, { value: '3', label: 'Administrador' }]} />
          </label>
          {field('correo', 'Correo', 'email')}
          <Specialties data={data} form={form} setForm={setForm} onChange={specialtyChanged} />
        </>
      )}

      {section === 'asignaciones' && (
        <>
          <Select label="Profesor" name="profesorId" items={data.usuarios.map((user) => ({ id: user.id, label: `${user.apellido}, ${user.nombre}` }))} form={form} setForm={setForm} />
          <Select label="Materia" name="materiaId" items={data.materias.map((subject) => ({ id: subject.id, label: subject.nombre }))} form={form} setForm={setForm} onValueChange={async (value) => {
            const id = Number(value);
            if (Number.isInteger(id) && id > 0) {
              try {
                const res = await getMateriaEspecialidades(id);
                setMateriaEspecialidadIds(res || []);
              } catch (err) {
                setMateriaEspecialidadIds([]);
              }
            } else setMateriaEspecialidadIds([]);
          }} />
          <Select label="Curso" name="cursoId" items={courseItems(data, materiaEspecialidadIds)} form={form} setForm={setForm} />
        </>
      )}

      {section === 'ingresantes' && (
        <>
          {field('nombre', 'Nombre')}
          {field('apellido', 'Apellido')}
          {field('ci', 'Cédula', 'number')}
          <Select label="Curso" name="cursoId" items={courseItems(data)} form={form} setForm={setForm} />
          {field('correoEncargado', 'Correo del encargado', 'email')}
          {field('correoEncargado2', 'Segundo correo', 'email')}
        </>
      )}

      <button className="button">Guardar</button>
    </form>
  );
}

function courseItems(data: AdminCatalog, allowedEspecialidadIds?: number[]) {
  if (allowedEspecialidadIds && allowedEspecialidadIds.length > 0) {
    const allowedNames = data.especialidades.filter((e) => allowedEspecialidadIds.includes(e.id)).map((e) => e.nombre);
    return data.cursos.filter((course) => allowedNames.includes(course.especialidad)).map((course) => ({ id: course.id, label: `${course.nivel}° ${course.seccion} · ${course.especialidad}` }));
  }
  return data.cursos.map((course) => ({ id: course.id, label: `${course.nivel}° ${course.seccion} · ${course.especialidad}` }));
}

function Specialties({ data, form, setForm, onChange, especialidadIds, setEspecialidadIds, categoria }: FormProps & { data: AdminCatalog; onChange: (value: string) => void; especialidadIds?: number[]; setEspecialidadIds?: (ids: number[]) => void; categoria?: string }) {
  // When used for materia creation (setEspecialidadIds provided):
  // - if categoria === 'especifico' -> allow only one specialty (single select)
  // - if categoria === 'comun' -> allow multi-selection (checkboxes)
  if (setEspecialidadIds) {
    const ids = especialidadIds || [];
    if (categoria === 'especifico') {
      // single-select: update the array to contain only the selected id
      return (
        <label>
          Especialidad
          <AnimatedSelect
            ariaLabel="Especialidad"
            value={ids[0] || ''}
            onChange={(value) => {
              const n = Number(value);
              setEspecialidadIds(Number.isInteger(n) && n > 0 ? [n] : []);
              onChange?.(value);
            }}
            options={[{ value: '', label: 'Seleccione…' }, ...data.especialidades.map((s) => ({ value: s.id, label: s.nombre }))]}
          />
        </label>
      );
    }

    return (
      <fieldset className="checkbox-list">
        <legend>Especialidades</legend>
        {data.especialidades.map((s) => (
          <label key={s.id}>
            <input
              type="checkbox"
              checked={ids.includes(s.id)}
              onChange={(e) => {
                const next = e.target.checked ? [...ids, s.id] : ids.filter((x) => x !== s.id);
                setEspecialidadIds(next);
                onChange?.(String(s.id));
              }}
            />
            {s.nombre}
          </label>
        ))}
      </fieldset>
    );
  }

  // Default usage (e.g., user creation) -> single select
  return <Select label="Especialidad" name="especialidadId" items={data.especialidades.map((specialty) => ({ id: specialty.id, label: specialty.nombre }))} form={form} setForm={setForm} optional onValueChange={onChange} />;
}

type FormProps = { form: Record<string, string>; setForm: (value: Record<string, string>) => void };
function Select({ label, name, items, form, setForm, optional, onValueChange }: FormProps & { label: string; name: string; items: Array<{ id: number; label: string }>; optional?: boolean; onValueChange?: (value: string) => void }) {
  return <label>{label}<AnimatedSelect ariaLabel={label} name={name} required={!optional} value={form[name] || ''} placeholder="Seleccione…" options={items.map((item) => ({ value: item.id, label: item.label }))} onChange={(value) => { setForm({ ...form, [name]: value }); onValueChange?.(value); }} /></label>;
}

function AdminList({ section, data, reload, status }: { section: string; data: AdminCatalog; reload: () => Promise<void>; status: (message: string) => void }) {
  if (section === 'materias') {
    return (
      <div className="admin-list">
        {data.materias.map((subject) => (
          <div key={subject.id}>
            <strong>{subject.nombre}</strong>
            <span>{subject.categoria}</span>
            <button className="button danger" onClick={async () => {
              if (!window.confirm('¿Eliminar esta materia? Esta acción fallará si existen planillas que la referencian.')) return;
              try { await deleteAdminRecord('materias', subject.id); status('Materia eliminada.'); await reload(); }
              catch (error) { status(error instanceof ApiError ? error.message : 'No se pudo eliminar.'); }
            }}>Eliminar</button>
          </div>
        ))}
      </div>
    );
  }

  if (section === 'usuarios') {
    return (
      <div className="admin-list">
        {data.usuarios.map((user) => (
          <div key={user.id}>
            <strong>{user.apellido}, {user.nombre}</strong>
            <span>{user.usuario} · nivel {user.nivel}</span>
            <button className="button danger" onClick={async () => {
              if (!window.confirm('¿Eliminar este usuario?')) return;
              try { await deleteAdminRecord('usuarios', user.id); status('Usuario eliminado.'); await reload(); }
              catch (error) { status(error instanceof ApiError ? error.message : 'No se pudo eliminar.'); }
            }}>Eliminar</button>
          </div>
        ))}
      </div>
    );
  }

  if (section === 'ingresantes') {
    return (
      <div className="admin-list">
        {data.alumnos.map((student) => (
          <div key={student.id}>
            <strong>{student.apellido}, {student.nombre}</strong>
            <span>{data.cursos.find((course) => course.id === student.cursoId)?.especialidad || 'Curso'} · CI {student.ci || '—'}</span>
            <button className="button danger" onClick={async () => {
              if (!window.confirm('¿Eliminar este ingresante?')) return;
              try { await deleteAdminRecord('ingresantes', student.id); status('Ingresante eliminado.'); await reload(); }
              catch (error) { status(error instanceof ApiError ? error.message : 'No se pudo eliminar.'); }
            }}>Eliminar</button>
          </div>
        ))}
      </div>
    );
  }

  async function remove(id: number) {
    if (!window.confirm('¿Eliminar esta asignación?')) return;
    try { await deleteAssignment(id); status('Asignación eliminada.'); await reload(); }
    catch (error) { status(error instanceof ApiError ? error.message : 'No se pudo eliminar.'); }
  }

  return <div className="admin-list">{data.asignaciones.map((assignment) => <div key={assignment.id}><span><strong>{assignment.profesor}</strong><small>{assignment.materia} · {assignment.curso}</small></span><button className="button danger" onClick={() => remove(assignment.id)}>Eliminar</button></div>)}</div>;
}
