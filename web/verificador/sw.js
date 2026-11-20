// Service worker del verificador (05 §6.7, 04 §5.8–5.9).
// - Precachea TODO lo listado en precache-manifest.json, comprobando el SHA-256 de cada fichero.
//   Si algo falla, la instalación falla (no queda una versión a medias) y se reintenta en la
//   próxima visita.
// - Caché con nombre = versión. Cache-first, solo del mismo origen.
// - Actualización: la versión nueva se instala en segundo plano y queda «en espera»; la página
//   avisa y solo se activa cuando el usuario lo pide (mensaje SKIP_WAITING). Nunca a mitad de
//   una verificación.
// Cambiar VERSION cada vez que cambie cualquier fichero (tools/gen-manifest.mjs lo lee de aquí).
const VERSION = "0.1.4";
const CACHE = "verificador-" + VERSION;
const MANIFEST = "precache-manifest.json";

const hex = (buf) => Array.from(new Uint8Array(buf), (b) => b.toString(16).padStart(2, "0")).join("");

async function precache() {
  const mres = await fetch(new Request(MANIFEST, { cache: "reload" }));
  if (!mres.ok) throw new Error("manifest " + mres.status);
  const manifest = await mres.clone().json();
  if (manifest.version !== VERSION || manifest.cache !== CACHE) throw new Error("versión del manifiesto distinta de sw.js");
  const cache = await caches.open(CACHE);
  try {
    await Promise.all(
      manifest.files.map(async (f) => {
        const res = await fetch(new Request(f.path, { cache: "reload" }));
        if (!res.ok) throw new Error(f.path + " " + res.status);
        const digest = hex(await crypto.subtle.digest("SHA-256", await res.clone().arrayBuffer()));
        if (digest !== f.sha256) throw new Error(f.path + " hash");
        await cache.put(f.path, res);
      }),
    );
    await cache.put(MANIFEST, mres); // el manifiesto va el último: su presencia = precache completo
  } catch (e) {
    await caches.delete(CACHE);
    throw e;
  }
}

self.addEventListener("install", (event) => {
  event.waitUntil(precache());
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    (async () => {
      for (const k of await caches.keys()) {
        if (k.startsWith("verificador-") && k !== CACHE) await caches.delete(k);
      }
      await self.clients.claim();
    })(),
  );
});

self.addEventListener("message", (event) => {
  const type = event.data && event.data.type;
  if (type === "SKIP_WAITING") self.skipWaiting();
  // La página pregunta qué worker la controla: «preparat» se liga a su versión y su caché.
  if (type === "GET_INFO" && event.ports && event.ports[0]) event.ports[0].postMessage({ version: VERSION, cache: CACHE });
  // Caché borrada o incompleta con el SW ya activo: volver a descargarlo todo (con red).
  if (type === "PRECACHE") event.waitUntil(precache().catch(() => {}));
});

self.addEventListener("fetch", (event) => {
  const req = event.request;
  if (req.method !== "GET") return;
  const url = new URL(req.url);
  if (url.origin !== self.location.origin) return;
  event.respondWith(
    (async () => {
      const cache = await caches.open(CACHE);
      const hit = await cache.match(req, { ignoreSearch: true });
      if (hit) return hit;
      if (req.mode === "navigate") {
        const index = await cache.match(new URL("index.html", self.registration.scope).href);
        if (index) return index;
      }
      try {
        return await fetch(req);
      } catch {
        return Response.error();
      }
    })(),
  );
});
