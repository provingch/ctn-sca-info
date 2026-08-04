(function () {
  const CONSENT_KEY = 'SIA_COOKIE_CONSENT';
  const banner = document.getElementById('cookieConsent');
  const acceptBtn = document.getElementById('acceptCookies');

  if (!banner || !acceptBtn) {
    return;
  }

  function readCookie(name) {
    const match = document.cookie.match(new RegExp('(?:^|; )' + name.replace(/([.$?*|{}()[\]\\/+^])/g, '\\$1') + '=([^;]*)'));
    return match ? decodeURIComponent(match[1]) : null;
  }

  function readStoredConsent() {
    try {
      const stored = window.localStorage.getItem(CONSENT_KEY);
      if (stored === 'accepted') {
        return stored;
      }
    } catch (error) {
      // Ignore storage access issues and fall back to cookies.
    }

    const cookieValue = readCookie(CONSENT_KEY);
    return cookieValue === 'accepted' ? cookieValue : null;
  }

  function writeStoredConsent() {
    const value = 'accepted';
    try {
      window.localStorage.setItem(CONSENT_KEY, value);
    } catch (error) {
      // Ignore storage access issues and keep the cookie fallback.
    }

    document.cookie = `${CONSENT_KEY}=${encodeURIComponent(value)}; path=/; max-age=31536000; SameSite=Lax`;
  }

  function hideBanner() {
    banner.classList.add('hidden');
  }

  function showBanner() {
    banner.classList.remove('hidden');
  }

  function hasConsent() {
    return readStoredConsent() === 'accepted';
  }

  function shouldShowBanner() {
    const rememberInput = document.querySelector('input[name="rememberMe"]');
    return rememberInput && rememberInput.checked && !hasConsent();
  }

  hideBanner();

  const rememberInput = document.querySelector('input[name="rememberMe"]');

  function updateBannerVisibility() {
    if (shouldShowBanner()) {
      showBanner();
      return;
    }

    hideBanner();
  }

  updateBannerVisibility();

  const loginForm = document.querySelector('.login-form');
  if (loginForm) {
    loginForm.addEventListener('submit', function (event) {
      if (rememberInput && rememberInput.checked && !hasConsent()) {
        event.preventDefault();
        showBanner();
      }
    });
  }

  acceptBtn.addEventListener('click', function () {
    writeStoredConsent();
    hideBanner();
  });
})();
