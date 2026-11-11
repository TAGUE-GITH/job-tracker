// Service worker de Postulyn : rend l'application installable et ouvre l'interface meme hors connexion.
// Les appels a l'API ne sont jamais mis en cache (donnees toujours a jour).
const CACHE = 'postulyn-v1'

self.addEventListener('install', (event) => {
  event.waitUntil(caches.open(CACHE).then((cache) => cache.addAll(['/', '/favicon.svg', '/icon-192.png'])))
  self.skipWaiting()
})

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((cles) => Promise.all(cles.filter((c) => c !== CACHE).map((c) => caches.delete(c))))
      .then(() => self.clients.claim()),
  )
})

self.addEventListener('fetch', (event) => {
  const requete = event.request
  const url = new URL(requete.url)
  if (requete.method !== 'GET' || url.origin !== self.location.origin || url.pathname.startsWith('/api/')) return

  // Pages : le reseau d'abord, la derniere version en cache si hors connexion
  if (requete.mode === 'navigate') {
    event.respondWith(
      fetch(requete)
        .then((reponse) => {
          const copie = reponse.clone()
          caches.open(CACHE).then((cache) => cache.put('/', copie))
          return reponse
        })
        .catch(() => caches.match('/')),
    )
    return
  }

  // Fichiers du build (noms uniques) et icones : le cache d'abord
  if (url.pathname.startsWith('/assets/') || /\.(png|svg|ico|webmanifest)$/.test(url.pathname)) {
    event.respondWith(
      caches.match(requete).then((enCache) => enCache || fetch(requete).then((reponse) => {
        if (reponse.ok) {
          const copie = reponse.clone()
          caches.open(CACHE).then((cache) => cache.put(requete, copie))
        }
        return reponse
      })),
    )
  }
})
