import React from 'react';
import ScaLogo from './ScaLogo';
import { iconUrlForSpecialtyName } from '../utils/specialtyIconData';

export default function AvatarEspecialidad({ name, size = 40, className = '' }: { name?: string | null; size?: number; className?: string }) {
  const url = iconUrlForSpecialtyName(name);
  const style: React.CSSProperties = { width: size, height: size, display: 'inline-grid', placeItems: 'center', overflow: 'hidden' };
  return (
    <span className={`avatar-specialty ${className}`} style={style} aria-hidden={!name} title={name ?? 'Especialidad'}>
      {url ? (
        <span
          className="avatar-specialty-mask"
          role="img"
          aria-label={`Especialidad ${name}`}
          style={{
            width: '72%',
            height: '72%',
            display: 'block',
            WebkitMaskImage: `url(${url})`,
            maskImage: `url(${url})`,
            WebkitMaskRepeat: 'no-repeat',
            maskRepeat: 'no-repeat',
            WebkitMaskSize: 'contain',
            maskSize: 'contain',
            backgroundColor: 'currentColor'
          }}
        />
      ) : (
        <ScaLogo />
      )}
    </span>
  );
}
