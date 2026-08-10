import { useEffect, useState } from 'react';
import { getProfile, type ProfileResponse } from '../../api/profile';
import { ApiError } from '../../api/client';

export default function ProfilePage() {
  const [data, setData] = useState<ProfileResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    getProfile()
      .then((res) => {
        if (!cancelled) setData(res);
      })
      .catch((err) => {
        if (!cancelled) setError(err instanceof ApiError ? err.message : 'Error al cargar el perfil.');
      });
    return () => {
      cancelled = true;
    };
  }, []);

  if (error) return <p className="form-error">{error}</p>;
  if (!data) return <p>Cargando…</p>;

  return (
    <div>
      <h1>Perfil</h1>
      <p>{data.profileOwner.fullName}</p>
      <p>{data.profileRoleLabel}</p>
      <p>{data.profileAccessDescription}</p>
    </div>
  );
}
