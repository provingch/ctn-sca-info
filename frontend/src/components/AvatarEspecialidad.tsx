import React from 'react';
import CtnLogo from './CtnLogo';
import { iconUrlForSpecialtyName } from '../utils/specialtyIconData';

export default function AvatarEspecialidad({ name, size = 40, className = '' }: { name?: string | null; size?: number; className?: string }) {
  const url = iconUrlForSpecialtyName(name);
  const style: React.CSSProperties = { width: size, height: size, display: 'inline-grid', placeItems: 'center', borderRadius: '50%', overflow: 'hidden' };
  return (
    <span className={`avatar-specialty ${className}`} style={style} aria-hidden={!name} title={name ?? 'Especialidad'}>
      {url ? <img src={url} alt="" className="avatar-specialty-img" style={{ width: '72%', height: '72%', objectFit: 'contain' }} /> : <CtnLogo />}
    </span>
  );
}
