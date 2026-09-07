// Jobsite Spanish is a single static file with zero runtime network
// dependencies - no APIs, no CDN, nothing dynamic. This worker exists
// only to satisfy Android's installability requirements for the TWA
// build and to let the very first shell request succeed offline.
// Bump CACHE_NAME whenever index.html / manifest.json / icons change,
// so installed clients pick up the new files instead of the old ones
// forever - there is no build tooling here to do that automatically.
const CACHE_NAME = 'jobsite-spanish-v1';
const PRECACHE_URLS = [
  './',
  './index.html',
  './manifest.json',
  './icons/icon-192.png',
  './icons/icon-512.png'
];

self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => cache.addAll(PRECACHE_URLS))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys()
      .then(keys => Promise.all(keys.filter(k => k !== CACHE_NAME).map(k => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', event => {
  if (event.request.method !== 'GET') return;
  event.respondWith(
    caches.match(event.request).then(cached =>
      cached || fetch(event.request).catch(() => caches.match('./index.html'))
    )
  );
});
