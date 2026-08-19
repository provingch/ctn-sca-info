export type PwaInstallStatus = 'installed' | 'ready' | 'ios-manual' | 'unavailable' | 'waiting';

export interface PwaInstallState {
  status: PwaInstallStatus;
  canInstall: boolean;
}

interface BeforeInstallPromptEvent extends Event {
  prompt(): Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed'; platform: string }>;
}

const listeners = new Set<() => void>();
let deferredPrompt: BeforeInstallPromptEvent | null = null;
let serviceWorkerRegistration: Promise<ServiceWorkerRegistration | null> | null = null;
let installSnapshot: PwaInstallState;

function isStandalone() {
  const navigatorWithStandalone = navigator as Navigator & { standalone?: boolean };
  return window.matchMedia('(display-mode: standalone)').matches || navigatorWithStandalone.standalone === true;
}

function isIos() {
  return /iPad|iPhone|iPod/.test(navigator.userAgent) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
}

function resolveInstallState(): PwaInstallState {
  if (isStandalone()) return { status: 'installed', canInstall: false };
  if (deferredPrompt) return { status: 'ready', canInstall: true };
  if (isIos()) return { status: 'ios-manual', canInstall: false };
  if (!('serviceWorker' in navigator)) return { status: 'unavailable', canInstall: false };
  return { status: 'waiting', canInstall: false };
}

function notifyListeners() {
  installSnapshot = resolveInstallState();
  listeners.forEach((listener) => listener());
}

installSnapshot = resolveInstallState();

window.addEventListener('beforeinstallprompt', (event) => {
  event.preventDefault();
  deferredPrompt = event as BeforeInstallPromptEvent;
  notifyListeners();
});

window.addEventListener('appinstalled', () => {
  deferredPrompt = null;
  notifyListeners();
});

export function subscribePwaInstall(listener: () => void) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function getPwaInstallSnapshot() {
  return installSnapshot;
}

export async function promptPwaInstall() {
  if (!deferredPrompt) return 'unavailable' as const;
  const prompt = deferredPrompt;
  deferredPrompt = null;
  await prompt.prompt();
  const choice = await prompt.userChoice;
  notifyListeners();
  return choice.outcome;
}

export function registerPwaServiceWorker(): Promise<ServiceWorkerRegistration | null> {
  if (!('serviceWorker' in navigator) || !import.meta.env.PROD) return Promise.resolve(null);
  if (!serviceWorkerRegistration) {
    serviceWorkerRegistration = navigator.serviceWorker.register('/sw.js', { scope: '/' })
      .then((registration) => {
        void registration.update();
        return registration;
      })
      .catch((error) => {
        console.error('No se pudo registrar el service worker de SCA.', error);
        return null;
      });
  }
  return serviceWorkerRegistration;
}
