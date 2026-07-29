(function () {
  function normalizeSpecialty(value) {
    return String(value || '').trim().toLowerCase();
  }

  function getBodySpecialty() {
    return normalizeSpecialty(document.body ? document.body.getAttribute('data-specialty') : '');
  }

  function applyThemeFromAttr() {
    const html = document.documentElement;
    const body = document.body;
    const theme = html.getAttribute('data-theme') || (body && body.classList.contains('dark-mode') ? 'dark' : 'light');
    html.setAttribute('data-theme', theme);
    body && body.classList.toggle('dark-mode', theme === 'dark');
  }

  function getToggleIcon(isDark) {
    return isDark
      ? '<svg viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M12 3.75a.75.75 0 0 1 .75.75v1.5a.75.75 0 0 1-1.5 0V4.5A.75.75 0 0 1 12 3.75Zm0 14.5a.75.75 0 0 1 .75.75v1.5a.75.75 0 0 1-1.5 0v-1.5A.75.75 0 0 1 12 18.25Zm8.25-6.25a.75.75 0 0 1 .75.75.75.75 0 0 1-.75.75h-1.5a.75.75 0 0 1 0-1.5h1.5Zm-14.5 0a.75.75 0 0 1 .75.75.75.75 0 0 1-.75.75h-1.5a.75.75 0 0 1 0-1.5h1.5Zm10.6-4.6a.75.75 0 0 1 1.06 0l1.06 1.06a.75.75 0 1 1-1.06 1.06l-1.06-1.06a.75.75 0 0 1 0-1.06Zm-9.4 9.4a.75.75 0 0 1 1.06 0l1.06 1.06a.75.75 0 1 1-1.06 1.06l-1.06-1.06a.75.75 0 0 1 0-1.06Zm0-9.4a.75.75 0 0 1 0 1.06l-1.06 1.06a.75.75 0 0 1-1.06-1.06l1.06-1.06a.75.75 0 0 1 1.06 0Zm9.4 9.4a.75.75 0 0 1 0 1.06l-1.06 1.06a.75.75 0 0 1-1.06-1.06l1.06-1.06a.75.75 0 0 1 1.06 0ZM12 7.5a4.5 4.5 0 1 0 0 9 4.5 4.5 0 0 0 0-9Z"/></svg>'
      : '<svg viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M21 12.79A9 9 0 1 1 11.21 3a7 7 0 0 0 9.79 9.79Z"/></svg>';
  }

  function ensureThemeToggle() {
    const existing = document.querySelector('.theme-toggle-button');
    if (existing) return existing;
    const themeItem = document.querySelector('.ctn-theme-item');
    if (themeItem) {
      const button = document.createElement('button');
      button.type = 'button';
      button.className = 'theme-toggle-button';
      button.setAttribute('aria-pressed', 'false');
      button.title = 'Cambiar tema';
      themeItem.appendChild(button);
      return button;
    }
    return null;
  }

  function applyTheme(isDark) {
    document.documentElement.setAttribute('data-theme', isDark ? 'dark' : 'light');
    document.body.classList.toggle('dark-mode', isDark);
    const toggle = ensureThemeToggle();
    if (toggle) {
      toggle.setAttribute('aria-pressed', String(isDark));
      toggle.innerHTML = getToggleIcon(isDark);
      toggle.setAttribute('aria-label', isDark ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro');
    }
  }

  function ensureSessionDropdown() {
    const button = document.getElementById('sessionButton');
    if (!button) return;
    const dropdown = button.closest('.dropdown');
    const menu = document.getElementById('sessionMenu');
    if (!dropdown || !menu) return;

    let mobileToggle = dropdown.querySelector('.session-mobile-toggle');
    if (!mobileToggle) {
      mobileToggle = document.createElement('button');
      mobileToggle.type = 'button';
      mobileToggle.className = 'session-mobile-toggle';
      mobileToggle.setAttribute('aria-controls', 'ctnSessionPanel');
      mobileToggle.setAttribute('aria-expanded', 'false');
      mobileToggle.innerHTML = '<span class="session-mobile-toggle__icon" aria-hidden="true"></span><span class="session-mobile-toggle__label">Sesión</span>';
      dropdown.appendChild(mobileToggle);
    }

    let mobilePanel = document.getElementById('ctnSessionPanel');
    let mobileBackdrop = document.getElementById('ctnSessionPanelBackdrop');
    if (!mobilePanel || !mobileBackdrop) {
      mobileBackdrop = document.createElement('div');
      mobileBackdrop.id = 'ctnSessionPanelBackdrop';
      mobileBackdrop.className = 'session-menu-panel-backdrop';
      mobileBackdrop.setAttribute('aria-hidden', 'true');
      document.body.appendChild(mobileBackdrop);

      mobilePanel = document.createElement('aside');
      mobilePanel.id = 'ctnSessionPanel';
      mobilePanel.className = 'session-menu-panel';
      mobilePanel.setAttribute('role', 'dialog');
      mobilePanel.setAttribute('aria-modal', 'true');
      mobilePanel.setAttribute('aria-label', 'Menú de sesión');

      const header = document.createElement('div');
      header.className = 'session-menu-panel__header';
      header.innerHTML = '<span class="session-menu-panel__title">Sesión</span><button type="button" class="session-menu-panel__close" aria-label="Cerrar menú de sesión">×</button>';
      mobilePanel.appendChild(header);

      const panelMenu = menu.cloneNode(true);
      panelMenu.className = 'dropdown-menu session-menu-panel__menu';
      panelMenu.removeAttribute('id');
      mobilePanel.appendChild(panelMenu);
      document.body.appendChild(mobilePanel);
    }

    const panelCloseButton = mobilePanel.querySelector('.session-menu-panel__close');
    const panelLinks = mobilePanel.querySelectorAll('a');

    function closeMenu() {
      dropdown.classList.remove('open');
      button.setAttribute('aria-expanded', 'false');
    }

    function closeMobileMenu() {
      mobilePanel.classList.remove('is-open');
      mobileBackdrop.classList.remove('is-visible');
      document.body.classList.remove('session-panel-open');
      button.setAttribute('aria-expanded', 'false');
      mobileToggle.setAttribute('aria-expanded', 'false');
    }

    function openMobileMenu() {
      mobilePanel.classList.add('is-open');
      mobileBackdrop.classList.add('is-visible');
      document.body.classList.add('session-panel-open');
      button.setAttribute('aria-expanded', 'true');
      mobileToggle.setAttribute('aria-expanded', 'true');
    }

    button.addEventListener('click', function (event) {
      event.preventDefault();
      event.stopPropagation();
      if (window.matchMedia('(max-width: 767px)').matches) {
        openMobileMenu();
        return;
      }
      const isOpen = dropdown.classList.toggle('open');
      button.setAttribute('aria-expanded', String(isOpen));
    });

    mobileToggle.addEventListener('click', function (event) {
      event.preventDefault();
      event.stopPropagation();
      openMobileMenu();
    });

    if (panelCloseButton) {
      panelCloseButton.addEventListener('click', function (event) {
        event.preventDefault();
        closeMobileMenu();
      });
    }

    if (mobileBackdrop) {
      mobileBackdrop.addEventListener('click', closeMobileMenu);
    }

    panelLinks.forEach(function (link) {
      link.addEventListener('click', closeMobileMenu);
    });

    document.addEventListener('click', function (event) {
      const target = event.target;
      const isMobileView = window.matchMedia('(max-width: 767px)').matches;
      if (isMobileView) {
        const shouldClose = mobilePanel.classList.contains('is-open')
          && !mobilePanel.contains(target)
          && !mobileToggle.contains(target)
          && !button.contains(target);
        if (shouldClose) {
          closeMobileMenu();
        }
        return;
      }
      if (!dropdown.contains(target)) closeMenu();
    });

    document.addEventListener('keydown', function (event) {
      if (event.key === 'Escape') {
        if (mobilePanel.classList.contains('is-open')) {
          closeMobileMenu();
        } else {
          closeMenu();
        }
      }
    });
  }

  const initial = (() => {
    const saved = localStorage.getItem('ctnDarkMode');
    if (saved === 'true') return true;
    if (saved === 'false') return false;
    return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
  })();

  applyTheme(initial);
  const toggle = ensureThemeToggle();
  if (toggle) {
    toggle.addEventListener('click', function () {
      const next = document.documentElement.getAttribute('data-theme') !== 'dark';
      applyTheme(next);
      localStorage.setItem('ctnDarkMode', String(next));
    });
  }

  document.addEventListener('DOMContentLoaded', function () {
    const body = document.body;
    if (body) {
      const specialty = getBodySpecialty();
      if (specialty) {
        body.setAttribute('data-specialty', specialty);
      }
    }
    applyThemeFromAttr();
    ensureSessionDropdown();
  });
})();
