const CACHE_NAME = 'ctn-portal-v2';
const CORE_ASSETS = [
  './',
  './offline.html',
  './styles/ctn-theme.css',
  './scripts/sca-theme.js',
  './images/ctn-logo.svg',
  './images/ctn-logo-2.svg',
  './icons/pwa/icon-192.png',
  './icons/pwa/icon-512.png'
];
const HTML_PREFIXES = ['https://', 'http://'];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(CORE_ASSETS)).catch(() => undefined)
  );
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) => Promise.all(keys.filter((key) => key !== CACHE_NAME).map((key) => caches.delete(key))))
  );
  event.waitUntil(self.clients.claim());
});

self.addEventListener('fetch', (event) => {
  const { request } = event;
  if (request.method !== 'GET') return;

  const isSameOrigin = request.url.startsWith(self.location.origin);
  const shouldCache = isSameOrigin && (request.destination === 'document' || request.destination === 'script' || request.destination === 'style' || request.destination === 'image');

  if (request.mode === 'navigate') {
    event.respondWith(
      fetch(request)
        .then((response) => {
          const copy = response.clone();
          if (response.ok) {
            caches.open(CACHE_NAME).then((cache) => cache.put('./', copy));
          }
          return response;
        })
        .catch(() => caches.match('./offline.html').then((fallback) => fallback || caches.match('./')))
    );
    return;
  }

  event.respondWith(
    caches.match(request).then((cached) => {
      if (cached) {
        return cached;
      }
      return fetch(request).then((response) => {
        const copy = response.clone();
        if (response.ok && shouldCache) {
          caches.open(CACHE_NAME).then((cache) => cache.put(request, copy));
        }
        return response;
      }).catch(() => {
        if (request.destination === 'document') {
          return caches.match('./offline.html');
        }
        return caches.match('./offline.html');
      });
    })
  );
});

self.addEventListener('push', (event) => {
  const payload = event.data && event.data.text ? event.data.text() : '{}';
  let notification = { title: 'SCA', body: 'Tienes un mensaje nuevo.' };
  try {
    notification = JSON.parse(payload);
  } catch (ignored) {
    // ignore malformed payloads
  }
  const title = notification.title || 'SCA';
  const body = notification.body || 'Tienes un mensaje nuevo.';
  const url = notification.url || self.registration.scope;
  const iconUrl = new URL('icons/pwa/icon-192.png', self.registration.scope);
  event.waitUntil(self.registration.showNotification(title, {
    body,
    icon: iconUrl.toString(),
    data: { url }
  }));
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const targetUrl = event.notification.data && event.notification.data.url ? event.notification.data.url : self.registration.scope;
  const urlToOpen = new URL(targetUrl, self.registration.scope);
  event.waitUntil(clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientList) => {
    for (const client of clientList) {
      if (client.url === urlToOpen.href && 'focus' in client) {
        return client.focus();
      }
    }
    if (clients.openWindow) {
      return clients.openWindow(urlToOpen.href);
    }
    return null;
  }));
});

