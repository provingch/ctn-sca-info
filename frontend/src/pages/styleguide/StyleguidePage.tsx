import { useState } from 'react';
import AnimatedSelect from '../../components/AnimatedSelect';
import AppShell from '../../components/AppShell';
import ConnectionState from '../../components/ui/ConnectionState';
import ContentState from '../../components/ui/ContentState';
import GradeChip from '../../components/ui/GradeChip';
import SectionHeading from '../../components/ui/SectionHeading';

const sampleOptions = [
  { value: 'primera', label: 'Primera etapa' },
  { value: 'segunda', label: 'Segunda etapa' },
];

export default function StyleguidePage() {
  const [stage, setStage] = useState('primera');

  return <AppShell title="Sistema de diseño" subtitle="Referencia viva de componentes, estados y patrones compartidos de SCA">
    <div className="styleguide-page">
      <section className="panel styleguide-intro">
        <p className="eyebrow">Referencia interna</p>
        <h2>Una sola base visual para todo el sistema</h2>
        <p>Estos ejemplos usan los mismos tokens y componentes que las pantallas reales. También funcionan con las paletas por especialidad y con ambos temas.</p>
      </section>

      <section className="panel styleguide-section">
        <SectionHeading number="01" title="Colores semánticos" detail="Los colores expresan propósito; no se agregan valores aislados por pantalla." />
        <div className="design-token-grid">
          <Token className="primary" name="Primario" token="--accent" />
          <Token className="surface" name="Superficie" token="--paper" />
          <Token className="success" name="Éxito" token="--success" />
          <Token className="warning" name="Advertencia" token="--warning" />
          <Token className="danger" name="Error" token="--danger" />
        </div>
      </section>

      <section className="panel styleguide-section">
        <SectionHeading number="02" title="Acciones y etiquetas" detail="Variantes existentes para acciones primarias, secundarias y destructivas." />
        <div className="styleguide-row">
          <button className="button" type="button">Acción principal</button>
          <button className="button secondary" type="button">Acción secundaria</button>
          <button className="button danger" type="button">Acción destructiva</button>
          <button className="button" type="button" disabled>Acción no disponible</button>
          <span className="badge">Estado general</span>
        </div>
        <div className="styleguide-grade-row" aria-label="Escala visual de notas">
          {[5, 4, 3, 2, 1].map((grade) => <GradeChip key={grade} grade={grade} label={`Nota ${grade}`} />)}
        </div>
      </section>

      <section className="panel styleguide-section">
        <SectionHeading number="03" title="Controles de formulario" detail="Controles con etiquetas visibles, foco consistente y estados deshabilitados legibles." />
        <div className="styleguide-form-grid">
          <label>Nombre del alumno<input placeholder="Ej. Lucas Mathias" /></label>
          <label>Etapa<AnimatedSelect ariaLabel="Etapa de ejemplo" value={stage} options={sampleOptions} onChange={setStage} /></label>
          <label>Curso no disponible<input value="Seleccione una especialidad primero" disabled readOnly /></label>
        </div>
      </section>

      <section className="panel styleguide-section">
        <SectionHeading number="04" title="Estados del sistema" detail="Mensajes reutilizables para conexión, carga, ausencia de datos y errores." />
        <div className="styleguide-status-grid">
          <ConnectionState active title="Cuenta conectada" detail="La sincronización está disponible." />
          <ConnectionState active={false} title="Sin conexión" detail="Todavía no existe una vinculación." />
        </div>
        <div className="styleguide-content-states">
          <ContentState compact tone="loading" title="Cargando información…" detail="Esperá un momento." />
          <ContentState compact title="Sin resultados" detail="No encontramos elementos para mostrar." />
          <ContentState compact tone="error" title="No se pudo cargar" detail="Intentá nuevamente." />
        </div>
      </section>

      <section className="panel styleguide-section styleguide-guidelines">
        <SectionHeading number="05" title="Reglas de uso" detail="Criterios mínimos para mantener consistencia y accesibilidad." />
        <ul>
          <li>Usar variables de <code>index.css</code> para colores, radios y sombras.</li>
          <li>Mostrar siempre una etiqueta visible y un estado de foco en controles.</li>
          <li>Reservar el color rojo para errores o acciones destructivas.</li>
          <li>Usar <code>ContentState</code> para carga, vacío y error, y <code>GradeChip</code> para notas.</li>
          <li>Verificar cada cambio en tema claro, oscuro y en anchos de 320 px en adelante.</li>
        </ul>
      </section>
    </div>
  </AppShell>;
}

function Token({ className, name, token }: { className: string; name: string; token: string }) {
  return <article className={`design-token design-token--${className}`}>
    <i aria-hidden="true" />
    <strong>{name}</strong>
    <code>{token}</code>
  </article>;
}
