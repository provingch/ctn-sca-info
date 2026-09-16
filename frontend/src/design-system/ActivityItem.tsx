import { Icon, type IconName } from './Icon';
const eventIcons: Record<ActivityItemProps['type'], IconName> = { class: 'book', grades: 'document', review: 'check', profile: 'user' };
export interface ActivityItemProps {
  type: 'class' | 'grades' | 'review' | 'profile';
  text: string;
  detail?: string;
  /** ISO 8601, with timezone for an instant. Display text supplied by caller. */
  dateTime: string;
  timestamp: string;
  href?: string;
}
/** Place inside ul/ol; href makes the text a real keyboard-accessible link. */
export function ActivityItem({ type, text, detail, dateTime, timestamp, href }: ActivityItemProps) {
  return <li className="ds-activity"><span className="ds-event-icon"><Icon name={eventIcons[type]} /></span><div className="ds-activity-copy">{href ? <a className="ds-link" href={href}>{text}</a> : <strong>{text}</strong>}{detail && <p className="ds-secondary ds-small">{detail}</p>}</div><time className="ds-tertiary ds-small" dateTime={dateTime}>{timestamp}</time></li>;
}
