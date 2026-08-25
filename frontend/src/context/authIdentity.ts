interface IdentityOwner {
  especialidadId?: number | null;
}

interface IdentitySpecialty {
  id: number;
  nombre: string;
}

export function resolveIdentitySpecialty(level: number, owner: IdentityOwner, specialties: IdentitySpecialty[]) {
  if (level === 3 && !Object.hasOwn(owner, 'especialidadId')) {
    throw new Error('El perfil no informó el alcance administrativo.');
  }
  const id = owner.especialidadId ?? null;
  const name = id === null ? null : specialties.find((specialty) => specialty.id === id)?.nombre ?? null;
  return { id, name };
}
