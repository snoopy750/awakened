// Offline-Cache. Die Version wird beim Veröffentlichen ersetzt, damit Updates erkannt werden.
const VERSION = 'dev';
const CACHE = `awakened-${VERSION}`;
const FILES = [
  './', 'index.html', 'style.css', 'manifest.webmanifest',
  'js/app.js', 'js/game.js', 'js/domain.js', 'js/version.js',
  'fonts/rajdhani_medium.ttf', 'fonts/rajdhani_semibold.ttf', 'fonts/rajdhani_bold.ttf',
  'sounds/level_up.m4a', 'sounds/quest_clear.m4a', 'sounds/system_alert.m4a',
  'icons/icon.svg', 'icons/icon-180.png', 'icons/icon-192.png', 'icons/icon-512.png',
];

self.addEventListener('install', (e) => {
  e.waitUntil(caches.open(CACHE).then((c) => c.addAll(FILES.map((f) => new Request(f, { cache: 'reload' })))));
});

self.addEventListener('activate', (e) => {
  e.waitUntil(caches.keys().then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k))))
    .then(() => self.clients.claim()));
});

self.addEventListener('message', (e) => { if (e.data === 'SKIP_WAITING') self.skipWaiting(); });

self.addEventListener('fetch', (e) => {
  const url = new URL(e.request.url);
  if (e.request.method !== 'GET' || url.origin !== location.origin) return;
  e.respondWith(
    caches.match(e.request, { ignoreSearch: true }).then((hit) => hit || fetch(e.request).catch(() =>
      e.request.mode === 'navigate' ? caches.match('index.html') : Response.error())),
  );
});
