import './RsaView.css';
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
  const [etapa, setEtapa] = useState('');

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
    setCargando(false);
    if (cursoRealId == null || materiaSeleccionada == null || !etapa) return;
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
    if (planillaId == null || guardando) return;
    const activarAhora = activar && !desactivar;
    if (activarAhora && errorFormulario) return;
    setGuardando(true);
    try {
      await saveRsa(planillaId, activarAhora
        ? { puntos: Number(puntos), toleranciaValor: Number(tolerancia), toleranciaUnidad: unidad }
        : { puntos: null });
      const detalle = await getPlanilla(planillaId);
      aplicarPlanilla(detalle.planilla);
      showToast(activarAhora ? 'RSA guardado. El total de puntos de la planilla se amplió con sus puntos.' : 'RSA desactivado para esta planilla.', { tone: 'success' });
    } catch (err) {
      showToast(errorMessage(err, 'No se pudo guardar la configuración de RSA.'), { tone: 'error' });
    } finally {
      setGuardando(false);
    }
  }

  return <div className="rsa-view">
    <header className="rsa-intro">
      <div><h3>Configurá los rasgos socioacadémicos</h3><p><strong>Opcional, por planilla.</strong> Elegí dónde querés aplicar el RSA.</p></div>
      <details className="rsa-help"><summary>¿Cómo funciona el RSA?</summary>
        <ul>
          <li><strong>Qué cuenta:</strong> cada código de conducta asignado a un alumno durante la etapa cuenta como una falta de RSA; no es lo mismo que una ausencia.</li>
          <li><strong>Cuánto descuenta:</strong> la tolerancia es la cantidad de faltas sin descuento. Cada falta adicional resta 1 punto de RSA, sin bajar de cero.</li>
          <li><strong>Dónde se suma:</strong> una planilla reúne las calificaciones de una materia, un curso y una etapa. El máximo de RSA amplía su TP (total de puntos); cada alumno suma el RSA que conserva a su puntaje.</li>
          <li><strong>Quién administra los códigos:</strong> Coordinación Pedagógica. Vos configurás los puntos y la tolerancia de cada planilla.</li>
        </ul>
        <p>Ejemplo: con 5 puntos de RSA, tolerancia de 2 faltas y 3 faltas registradas, el alumno conserva 4 puntos.</p>
      </details>
    </header>
    <section className="class-card rsa-selection" aria-labelledby="rsa-selection-title">
      <h4 id="rsa-selection-title">1. Elegí la planilla</h4>
      {errorAsignaciones ? <div className="notice error">{errorAsignaciones}</div>
        : asignaciones === null ? <p>Cargando asignaciones…</p>
        : lista.length === 0 ? <p>No tenés asignaciones disponibles.</p>
        : <div className="rsa-selector-grid">
          {especialidades.length > 1 && <div className="class-field"><label>Especialidad</label><AnimatedSelect ariaLabel="Especialidad" value={especialidadId} disabled={guardando} onChange={(value) => { setEspecialidadId(value); setCurso(''); setSeccion(''); setMateriaId(''); setEtapa(''); }} options={[{ value: '', label: 'Elegí una especialidad' }, ...especialidades.map((item) => ({ value: item.especialidadId, label: item.especialidadNombre }))]} /></div>}
          <div className="class-field"><label>Curso</label><AnimatedSelect ariaLabel="Curso" value={curso} disabled={!especialidadEfectiva || guardando} describedBy="rsa-curso-help" onChange={(value) => { setCurso(value); setSeccion(''); setMateriaId(''); setEtapa(''); }} options={[{ value: '', label: 'Elegí un curso' }, ...cursos.map((item) => ({ value: item.cursoOrdinal, label: item.cursoOrdinal }))]} /><small id="rsa-curso-help">{especialidadEfectiva ? 'Curso de tu asignación' : 'Primero elegí una especialidad'}</small></div>
          <div className="class-field"><label>Sección</label><AnimatedSelect ariaLabel="Sección" value={seccion} disabled={!curso || guardando} describedBy="rsa-seccion-help" onChange={(value) => { setSeccion(value); setMateriaId(''); setEtapa(''); }} options={[{ value: '', label: 'Elegí una sección' }, ...secciones.map((item) => ({ value: item.seccion, label: item.seccion }))]} /><small id="rsa-seccion-help">{curso ? 'Secciones de este curso' : 'Primero elegí un curso'}</small></div>
          <div className="class-field"><label>Materia</label><AnimatedSelect ariaLabel="Materia" value={materiaId} disabled={!seccion || guardando} describedBy="rsa-materia-help" onChange={(value) => { setMateriaId(value); setEtapa(''); }} options={[{ value: '', label: 'Elegí una materia' }, ...materias.map((item) => ({ value: item.materiaId, label: item.materiaNombre }))]} /><small id="rsa-materia-help">{seccion ? 'Materias de esta sección' : 'Primero elegí una sección'}</small></div>
          <div className="class-field"><label>Etapa</label><AnimatedSelect ariaLabel="Etapa" value={etapa} disabled={!asignacion || guardando} describedBy="rsa-etapa-help" onChange={setEtapa} options={[{ value: '', label: 'Elegí una etapa' }, { value: '1', label: 'Etapa 1' }, { value: '2', label: 'Etapa 2' }]} /><small id="rsa-etapa-help">{asignacion ? 'Período de evaluación' : 'Primero elegí una materia'}</small></div>
        </div>}
      {asignacion && cursoRealId == null && <div className="notice" style={{ marginTop: 12 }}>Ese curso todavía no existe para la promoción actual, así que no tiene planilla.</div>}
    </section>

    {asignacion && cursoRealId != null && etapa && <section className="class-card rsa-config" aria-labelledby="rsa-config-title">
      <h4 id="rsa-config-title">2. Configurá el RSA</h4>
      <p className="rsa-context">{asignacion.especialidadNombre} · {curso} {seccion} · {asignacion.materiaNombre} · Etapa {etapa}</p>
      {errorPlanilla ? <div className="notice error">{errorPlanilla}</div>
        : cargando || planillaId == null ? <p>Cargando planilla…</p>
        : <>
          <p role="status" style={{ margin: '0 0 12px' }}>
            {actual
              ? <><strong>RSA activo:</strong> {actual.puntos} {actual.puntos === 1 ? 'punto' : 'puntos'}, tolerancia de {describirTolerancia(actual.toleranciaValor, actual.toleranciaUnidad)}.</>
              : <><strong>RSA desactivado</strong> para esta planilla.</>}
          </p>
          <label className="checkbox-label" style={{ gap: 8, cursor: 'pointer', marginBottom: 12 }}>
            <input type="checkbox" checked={activar} disabled={guardando} onChange={(event) => setActivar(event.target.checked)} />
            <span>Activar RSA en esta planilla</span>
          </label>
          <div className="class-grid">
            <div className="class-field"><label htmlFor="rsa-puntos">Puntos de RSA</label><input id="rsa-puntos" type="number" min={1} step={1} inputMode="numeric" value={puntos} disabled={!activar || guardando} onChange={(event) => setPuntos(event.target.value)} /></div>
            <div className="class-field"><label htmlFor="rsa-tolerancia">Tolerancia sin descuento</label><input id="rsa-tolerancia" aria-describedby="rsa-tolerancia-help" type="number" min={0} step={unidad === 'CANTIDAD' ? 1 : 0.5} inputMode="decimal" value={tolerancia} disabled={!activar || guardando} onChange={(event) => setTolerancia(event.target.value)} /><small id="rsa-tolerancia-help">{unidad === 'CANTIDAD' ? 'Cantidad de faltas permitidas antes de descontar puntos.' : 'Porcentaje de clases dadas convertido a faltas y redondeado al entero más cercano.'}</small></div>
            <div className="class-field"><label>Unidad de la tolerancia</label><AnimatedSelect ariaLabel="Unidad de la tolerancia" value={unidad} disabled={!activar || guardando} onChange={(value) => setUnidad(value as RsaToleranciaUnidad)} options={[{ value: 'CANTIDAD', label: 'Cantidad de faltas' }, { value: 'PORCENTAJE', label: '% de las clases dadas' }]} /></div>
          </div>
          {errorFormulario && <div className="notice error" style={{ marginTop: 12 }}>{errorFormulario}</div>}
          <div className="rsa-actions">
            {actual && <button type="button" className="button secondary" disabled={guardando} onClick={() => void guardar(true)}>Desactivar RSA</button>}
            <button type="button" className="button rsa-save" disabled={!puedeGuardar} onClick={() => void guardar()}>{guardando ? 'Guardando…' : 'Guardar configuración'}</button>
          </div>
        </>}
    </section>}
  </div>;
}
