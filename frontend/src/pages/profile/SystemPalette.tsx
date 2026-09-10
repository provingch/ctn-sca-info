import AnimatedSelect from '../../components/AnimatedSelect';
import { useSpecialty } from '../../context/SpecialtyContext';
import { normalizeSpecialty } from '../../theme/theme';
import type { ProfileSpecialtyDto } from '../../api/profile';

export default function SystemPalette({ especialidades }: { especialidades: ProfileSpecialtyDto[] }) {
  const { id, name, selectSpecialty, resetSpecialty } = useSpecialty();
  const selectedId = id ?? especialidades.find((item) => normalizeSpecialty(item.nombre) === normalizeSpecialty(name))?.id ?? 0;
  return <section className="panel" aria-labelledby="system-palette-title">
    <h2 id="system-palette-title">Apariencia del sistema</h2>
    <p className="muted-copy" id="system-palette-help">Elegí la paleta que se aplicará al navegar por el sistema en esta sesión.</p>
    <div className="form-grid"><label>Paleta del sistema
      <AnimatedSelect ariaLabel="Paleta del sistema" describedBy="system-palette-help" value={selectedId} onChange={(value) => {
        const specialty = especialidades.find((item) => item.id === Number(value));
        if (specialty) selectSpecialty(specialty.nombre, specialty.id);
        else resetSpecialty();
      }} options={[{ value: 0, label: 'Institucional (predeterminada)' }, ...especialidades.map((specialty) => ({ value: specialty.id, label: specialty.nombre }))]} />
    </label></div>
  </section>;
}
