interface ConnectionStateProps {
  active: boolean;
  title: string;
  detail?: string;
}

export default function ConnectionState({ active, title, detail }: ConnectionStateProps) {
  return <div className={`connection-state ${active ? 'connected' : ''}`} role="status">
    <i aria-hidden="true" />
    <div>
      <strong>{title}</strong>
      {detail && <span>{detail}</span>}
    </div>
  </div>;
}
