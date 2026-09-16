export type IconName = 'book' | 'check' | 'document' | 'user' | 'bell' | 'plus' | 'arrow' | 'empty';
const paths: Record<IconName, string> = {
  book: 'M12 5v15M12 5C8 2 3 3 3 3v15s5-1 9 2c4-3 9-2 9-2V3s-5-1-9 2Z',
  check: 'm5 12 4 4L19 6',
  document: 'M14 3H5v18h14V8l-5-5Zm0 0v5h5M8 12h8M8 16h6',
  user: 'M16 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0ZM4 21v-3a8 8 0 0 1 16 0v3',
  bell: 'M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4',
  plus: 'M12 5v14M5 12h14',
  arrow: 'M4 12h16m-6-6 6 6-6 6',
  empty: 'M4 4h16v16H4ZM4 13h5l2 3h2l2-3h5',
};
export function Icon({ name }: { name: IconName }) {
  return <svg className="ds-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" focusable="false"><path d={paths[name]} /></svg>;
}
