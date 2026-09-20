import type { ReactNode } from 'react';

export type LauncherOption = {
  key: string;
  icon: ReactNode;
  title: string;
  description: string;
  onSelect: () => void;
  badges?: ReactNode;
};

export default function LauncherCards({ options, className = 'launcher-cards' }: { options: LauncherOption[]; className?: string }) {
  return (
    <div className={className}>
      {options.map((option) => (
        <button type="button" key={option.key} className="launcher-card" aria-label={option.title} onClick={option.onSelect}>
          <div className="launcher-card-head">
            <span className="launcher-card-icon">{option.icon}</span>
            <h2>{option.title}</h2>
            <span className="launcher-card-arrow" aria-hidden="true">→</span>
          </div>
          <p>{option.description}</p>
          {option.badges !== undefined && <div className="launcher-badges">{option.badges}</div>}
        </button>
      ))}
    </div>
  );
}
