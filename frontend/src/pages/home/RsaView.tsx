import { useCallback, useEffect, useState } from 'react';
import { ApiError } from '../../api/client';
import AnimatedSelect from '../../components/AnimatedSelect';
import { useToast } from '../../context/toast';
import { getMisAsignaciones, type AsignacionCompleta } from '../../api/planCurricular';
import { getPlanilla, resolvePlanilla, saveRsa, type RsaToleranciaUnidad } from '../../api/academics';

type RsaActual = { puntos: number; toleranciaValor: number; toleranciaUnidad: RsaToleranciaUnidad } | null;

function unique<T>(items: T[], key: (item: T) => string | number): T[] {
  return Array.from(new Map(items.map((item) => [key(item), item])).values());
}

function errorMessage(error: unknown, fallback: string) {
  return error instanceof ApiError ? error.message : fallback;
}

function etapaActual(): '1' | '2' {
  const hoy = new Date();
  return hoy < new Date(hoy.getFullYear(), 5, 22) ? '1' : '2';
}

function describirTolerancia(valor: number, unidad: RsaToleranciaUnidad) {
  return unidad === 'PORCENTAJE' ? `${valor}% de las clases dadas` : `${valor} ${valor === 1 ? 'falta' : 'faltas'}`;
}

/** Devuelve el mensaje de error del formulario, o '' si la configuración es válida. */
function validarConfig(puntos: string, tolerancia: string, unidad: RsaToleranciaUnidad) {
  const p = Number(puntos);
  if (puntos.trim() === '' || !Number.isInteger(p) || p <= 0) return 'Los puntos de RSA deben ser un número entero mayor a 0.';
  const v = Number(tolerancia);
  if (tolerancia.trim() === '' || !Number.isFinite(v) || v < 0) return 'La tolerancia debe ser un número mayor o igual a 0.';
  if (unidad === 'CANTIDAD' && !Number.isInteger(v)) return 'Con unidad "cantidad de faltas" la tolerancia debe ser un número entero.';
  if (unidad === 'PORCENTAJE' && v > 100) return 'El porcentaje de tolerancia no puede superar 100.';
  return '';
}

export default function RsaView() {
  const { showToast } = useToast();
  const [asignaciones, setAsignaciones] = useState<AsignacionCompleta[] | null>(null);
  const [errorAsignaciones, setErrorAsignaciones] = useState('');
  const [especialidadId, setEspecialidadId] = useState('');
  const [curso, setCurso] = useState('');
  const [seccion, setSeccion] = useState('');
  const [materiaId, setMateriaId] = useState('');
  const [etapa, setEtapa] = useState<string>(etapaActual);

  const [planillaId, setPlanillaId] = useState<number | null>(null);
  const [actual, setActual] = useState<RsaActual>(null);
  const [cargando, setCargando] = useState(false);
  const [errorPlanilla, setErrorPlanilla] = useState('');

  const [activar, setActivar] = useState(false);
  const [puntos, setPuntos] = useState('');
  const [tolerancia, setTolerancia] = useState('');
  const [unidad, setUnidad] = useState<RsaToleranciaUnidad>('CANTIDAD');
  const [guardando, setGuardando] = useState(false);

  useEffect(() => {
    let active = true;
    getMisAsignaciones()
      .then((list) => { if (active) setAsignaciones(list); })
      .catch((err) => { if (active) setErrorAsignaciones(errorMessage(err, 'No se pudieron cargar tus asignaciones.')); });
    return () => { active = false; };
  }, []);

  const lista = asignaciones ?? [];
  const especialidades = unique(lista, (item) => item.especialidadId);
  const especialidadEfectiva = especialidades.length === 1 ? String(especialidades[0].especialidadId) : especialidadId;
  const deLaEspecialidad = lista.filter((item) => String(item.especialidadId) === especialidadEfectiva);
  const cursos = unique(deLaEspecialidad, (item) => item.cursoOrdinal);
  const secciones = curso ? unique(deLaEspecialidad.filter((item) => item.cursoOrdinal === curso), (item) => item.seccion) : [];
  const materias = curso && seccion ? deLaEspecialidad.filter((item) => item.cursoOrdinal === curso && item.seccion === seccion) : [];
  const asignacion = materias.find((item) => item.materiaId === Number(materiaId));
  const cursoRealId = asignacion?.cursoRealId ?? null;
  const materiaSeleccionada = asignacion?.materiaId ?? null;

  const aplicarPlanilla = useCallback((header: { rsaPuntos: number | null; rsaToleranciaValor: number | null; rsaToleranciaUnidad: RsaToleranciaUnidad | null }) => {
    const vigente: RsaActual = header.rsaPuntos != null && header.rsaToleranciaValor != null && header.rsaToleranciaUnidad != null
      ? { puntos: header.rsaPuntos, toleranciaValor: header.rsaToleranciaValor, toleranciaUnidad: header.rsaToleranciaUnidad }
      : null;
    setActual(vigente);
    setActivar(vigente != null);
    setPuntos(vigente ? String(vigente.puntos) : '');
    setTolerancia(vigente ? String(vigente.toleranciaValor) : '');
    setUnidad(vigente ? vigente.toleranciaUnidad : 'CANTIDAD');
  }, []);

  // Con curso+materia+etapa resueltos se busca (o crea) la planilla y se lee su RSA actual.
  useEffect(() => {
    setPlanillaId(null);
    setActual(null);
    setErrorPlanilla('');
    if (cursoRealId == null || materiaSeleccionada == null) return;
    let active = true;
    setCargando(true);
    resolvePlanilla(cursoRealId, materiaSeleccionada, Number(etapa))
      .then(async ({ planillaId: id }) => {
        const detalle = await getPlanilla(id);
        if (!active) return;
        setPlanillaId(id);
        aplicarPlanilla(detalle.planilla);
      })
      .catch((err) => { if (active) setErrorPlanilla(errorMessage(err, 'No se pudo cargar la planilla.')); })
      .finally(() => { if (active) setCargando(false); });
    return () => { active = false; };
  }, [cursoRealId, materiaSeleccionada, etapa, aplicarPlanilla]);

  const errorFormulario = activar ? validarConfig(puntos, tolerancia, unidad) : '';
  const puedeGuardar = planillaId != null && !guardando && (activar ? errorFormulario === '' : actual != null);

  async function guardar(desactivar = false) {
    if (planillaId == null) return;
    const activarAhora = activar && !desactivar;
    if (activarAhora && errorFormulario) return;
    setGuardando(true);
    try {
      await saveRsa(planillaId, activarAhora
        ? { puntos: Number(puntos), toleranciaValor: Number(tolerancia), toleranciaUnidad: unidad }
        : { puntos: null });
      const detalle = await getPlanilla(planillaId);
      aplicarPlanilla(detalle.planilla);
      showToast(activarAhora ? 'RSA guardado. El TP de la planilla se amplió con sus puntos.' : 'RSA desactivado para esta planilla.', { tone: 'success' });
    } catch (err) {
      showToast(errorMessage(err, 'No se pudo guardar la configuración de RSA.'), { tone: 'error' });
    } finally {
      setGuardando(false);
    }
  }

  return <div className="two-column">
    <div className="class-card" style={{ gridColumn: '1 / -1' }}>
      <div className="class-card-head"><h3>RSA (Rasgos Socioacadémicos)</h3></div>
      <p style={{ margin: 0, color: 'var(--muted)' }}>
        Opcional, por planilla. Cada código de conducta (N1–N8) asignado a un alumno en la etapa cuenta como una falta; por cada falta que supere la tolerancia se descuenta 1 punto del RSA.
        Los puntos de RSA se suman al TP de la planilla y a la nota de cada alumno.
      </p>
    </div>

    <section className="class-card">
      <h4 style={{ margin: '0 0 8px' }}>Planilla</h4>
      {errorAsignaciones ? <div className="notice error">{errorAsignaciones}</div>
        : asignaciones === null ? <p>Cargando asignaciones…</p>
        : lista.length === 0 ? <p>No tenés asignaciones disponibles.</p>
        : <div className="class-grid">
          {especialidades.length > 1 && <div className="class-field"><label>Especialidad</label><AnimatedSelect ariaLabel="Especialidad" value={especialidadId} onChange={(value) => { setEspecialidadId(value); setCurso(''); setSeccion(''); setMateriaId(''); }} options={[{ value: '', label: 'Seleccione especialidad' }, ...especialidades.map((item) => ({ value: item.especialidadId, label: item.especialidadNombre }))]} /></div>}
          <div className="class-field"><label>Curso</label><AnimatedSelect ariaLabel="Curso" value={curso} disabled={!especialidadEfectiva} onChange={(value) => { setCurso(value); setSeccion(''); setMateriaId(''); }} options={[{ value: '', label: 'Seleccione curso' }, ...cursos.map((item) => ({ value: item.cursoOrdinal, label: item.cursoOrdinal }))]} /></div>
          <div className="class-field"><label>Sección</label><AnimatedSelect ariaLabel="Sección" value={seccion} disabled={!curso} onChange={(value) => { setSeccion(value); setMateriaId(''); }} options={[{ value: '', label: 'Seleccione sección' }, ...secciones.map((item) => ({ value: item.seccion, label: item.seccion }))]} /></div>
          <div className="class-field"><label>Materia</label><AnimatedSelect ariaLabel="Materia" value={materiaId} disabled={!seccion} onChange={setMateriaId} options={[{ value: '', label: 'Seleccione materia' }, ...materias.map((item) => ({ value: item.materiaId, label: item.materiaNombre }))]} /></div>
          <div className="class-field"><label>Etapa</label><AnimatedSelect ariaLabel="Etapa" value={etapa} onChange={setEtapa} options={[{ value: '1', label: 'Etapa 1' }, { value: '2', label: 'Etapa 2' }]} /></div>
        </div>}
      {asignacion && cursoRealId == null && <div className="notice" style={{ marginTop: 12 }}>Ese curso todavía no existe para la promoción actual, así que no tiene planilla.</div>}
    </section>

    <section className="class-card">
      <h4 style={{ margin: '0 0 8px' }}>Configuración</h4>
      {!asignacion || cursoRealId == null ? <p style={{ color: 'var(--muted)' }}>Elegí curso, sección, materia y etapa para configurar el RSA de esa planilla.</p>
        : errorPlanilla ? <div className="notice error">{errorPlanilla}</div>
        : cargando || planillaId == null ? <p>Cargando planilla…</p>
        : <>
          <p role="status" style={{ margin: '0 0 12px' }}>
            {actual
              ? <><strong>RSA activo:</strong> {actual.puntos} {actual.puntos === 1 ? 'punto' : 'puntos'}, tolerancia de {describirTolerancia(actual.toleranciaValor, actual.toleranciaUnidad)}.</>
              : <><strong>RSA desactivado</strong> para esta planilla.</>}
          </p>
          <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer', marginBottom: 12 }}>
            <input type="checkbox" checked={activar} disabled={guardando} onChange={(event) => setActivar(event.target.checked)} />
            <span>Activar RSA en esta planilla</span>
          </label>
          <div className="class-grid">
            <div className="class-field"><label htmlFor="rsa-puntos">Puntos de RSA</label><input id="rsa-puntos" type="number" min={1} step={1} inputMode="numeric" value={puntos} disabled={!activar || guardando} onChange={(event) => setPuntos(event.target.value)} /></div>
            <div className="class-field"><label htmlFor="rsa-tolerancia">Tolerancia</label><input id="rsa-tolerancia" type="number" min={0} step={unidad === 'CANTIDAD' ? 1 : 0.5} inputMode="decimal" value={tolerancia} disabled={!activar || guardando} onChange={(event) => setTolerancia(event.target.value)} /></div>
            <div className="class-field"><label>Unidad de la tolerancia</label><AnimatedSelect ariaLabel="Unidad de la tolerancia" value={unidad} disabled={!activar || guardando} onChange={(value) => setUnidad(value as RsaToleranciaUnidad)} options={[{ value: 'CANTIDAD', label: 'Cantidad de faltas' }, { value: 'PORCENTAJE', label: '% de las clases dadas' }]} /></div>
          </div>
          {errorFormulario && <div className="notice error" style={{ marginTop: 12 }}>{errorFormulario}</div>}
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 16 }}>
            {actual && <button type="button" className="button secondary" disabled={guardando} onClick={() => void guardar(true)}>Desactivar RSA</button>}
            <button type="button" className="button" disabled={!puedeGuardar} onClick={() => void guardar()}>{guardando ? 'Guardando…' : 'Guardar'}</button>
          </div>
        </>}
    </section>
  </div>;
}
