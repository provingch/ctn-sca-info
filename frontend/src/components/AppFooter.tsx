import { NavLink } from 'react-router-dom';

export default function AppFooter() {
  return (<footer className="app-footer">
        <div className="app-footer-inner">
          <div className="app-footer-brand" aria-hidden="true">
            <span><strong>Colegio Técnico Nacional</strong></span>
          </div>
          <span><NavLink to="/privacidad">Privacidad</NavLink> · <NavLink to="/terminos">Términos</NavLink> · <NavLink to="/acerca-de">Acerca de</NavLink></span>
        </div>
      </footer>);
}
