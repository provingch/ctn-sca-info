import { useState, type InputHTMLAttributes } from 'react';

export default function PasswordInput(props: Omit<InputHTMLAttributes<HTMLInputElement>, 'type'>) {
  const [visible, setVisible] = useState(false);
  const actionLabel = visible ? 'Ocultar contraseña' : 'Mostrar contraseña';

  return <div className="password-field">
    <input {...props} type={visible ? 'text' : 'password'} />
    <button
      className="password-toggle"
      type="button"
      onClick={() => setVisible((value) => !value)}
      aria-label={actionLabel}
      aria-pressed={visible}
      title={actionLabel}
    >
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7Z" />
        <circle cx="12" cy="12" r="3" />
        {visible && <path d="M4 4l16 16" />}
      </svg>
    </button>
  </div>;
}
