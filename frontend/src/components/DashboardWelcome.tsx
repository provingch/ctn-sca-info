import { useAuth } from '../context/AuthContext';
import PageBanner from './PageBanner';

/** Same welcome banner as the professor home, using only the signed-in identity. */
export default function DashboardWelcome({ title, subtitle, specialty }: { title: string; subtitle?: string; specialty?: string | null }) {
  const { user } = useAuth();
  const firstName = user?.displayName?.trim().split(/\s+/)[0];
  const date = new Intl.DateTimeFormat('es-PY', { weekday: 'long', day: 'numeric', month: 'long' }).format(new Date());
  return <PageBanner title={firstName ? `Hola, ${firstName}` : 'Hola'} context={[title, subtitle, date].filter(Boolean).join(' · ')} specialty={specialty} />;
}
