const ASSET_VERSION = '0.6.5';
const CACHE_NAME = `ctn-cache-v${ASSET_VERSION}`;
const CORE_ASSETS = [
  './offline.html',
  `./styles/ctn-theme.css?v=${ASSET_VERSION}`,
  `./scripts/sca-theme.js?v=${ASSET_VERSION}`,
  './images/ctn-logo.svg',
  './images/ctn-logo-2.svg',
  './icons/pwa/icon-192.png',
  './icons/pwa/icon-512.png'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(CORE_ASSETS)).catch(() => undefined)
  );
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys
        .filter((key) => key !== CACHE_NAME)
        .map((key) => caches.delete(key))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  const { request } = event;
  if (request.method !== 'GET') return;

  const isSameOrigin = request.url.startsWith(self.location.origin);
  if (!isSameOrigin) return;

  if (request.mode === 'navigate' || request.destination === 'document') {
    event.respondWith(networkFirst(request, './offline.html'));
    return;
  }

  if (request.destination === 'style') {
    event.respondWith(networkFirst(request));
    return;
  }

  if (request.destination === 'script') {
    event.respondWith(staleWhileRevalidate(request));
    return;
  }

  if (request.destination === 'image' || request.destination === 'font') {
    event.respondWith(cacheFirst(request));
    return;
  }

  event.respondWith(networkFirst(request));
});

async function networkFirst(request, fallbackUrl) {
  try {
    const response = await fetch(request);
    if (response && response.ok) {
      const cache = await caches.open(CACHE_NAME);
      cache.put(request, response.clone());
    }
    return response;
  } catch (error) {
    const cached = await caches.match(request);
    if (cached) {
      return cached;
    }
    if (fallbackUrl) {
      const fallback = await caches.match(fallbackUrl);
      if (fallback) {
        return fallback;
      }
    }
    throw error;
  }
}

async function staleWhileRevalidate(request) {
  const cache = await caches.open(CACHE_NAME);
  const cached = await cache.match(request);
  const networkPromise = fetch(request)
    .then((response) => {
      if (response && response.ok) {
        cache.put(request, response.clone());
      }
      return response;
    })
    .catch(() => undefined);

  if (cached) {
    eventWaitUntilSafe(networkPromise);
    return cached;
  }

  const networkResponse = await networkPromise;
  if (networkResponse) {
    return networkResponse;
  }

  const offline = await caches.match('./offline.html');
  return offline;
}

async function cacheFirst(request) {
  const cache = await caches.open(CACHE_NAME);
  const cached = await cache.match(request);
  if (cached) {
    return cached;
  }

  const response = await fetch(request);
  if (response && response.ok) {
    cache.put(request, response.clone());
  }
  return response;
}

function eventWaitUntilSafe(promise) {
  promise.catch(() => undefined);
}

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

