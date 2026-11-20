// Estado «preparat» (04 §5.8): solo si TODOS los ficheros del manifiesto del worker que CONTROLA
// la página están en SU caché (nombre = su versión) y su SHA-256 coincide. Instalada ≠ preparada;
// la caché completa de un worker en espera (versión nueva) tampoco cuenta: si el worker activo
// perdió su caché, sin red la página no arrancaría.

const hex = (buf) => Array.from(new Uint8Array(buf), (b) => b.toString(16).padStart(2, "0")).join("");
const MANIFEST = "precache-manifest.json";

/** Pregunta al worker que controla la página su {version, cache}. null si no responde. */
export function getControllerInfo(container = globalThis.navigator?.serviceWorker, timeoutMs = 2000) {
  return new Promise((resolve) => {
    const ctl = container?.controller;
    if (!ctl || typeof MessageChannel === "undefined") return resolve(null);
    const ch = new MessageChannel();
    const timer = setTimeout(() => { ch.port1.close(); resolve(null); }, timeoutMs);
    ch.port1.onmessage = (e) => {
      clearTimeout(timer);
      ch.port1.close();
      const d = e.data;
      resolve(d && typeof d.version === "string" && typeof d.cache === "string" ? d : null);
    };
    ctl.postMessage({ type: "GET_INFO" }, [ch.port2]);
  });
}

/**
 * @param env {caches, serviceWorker, subtle, isSecureContext, getInfo} — inyectable para pruebas.
 * @returns {Promise<{state:'ready'|'incomplete'|'unsupported'|'error', version?:string, missing?:string[]}>}
 */
export async function checkPrepared(env = {}) {
  const cachesApi = env.caches ?? globalThis.caches;
  const container = env.serviceWorker ?? globalThis.navigator?.serviceWorker;
  const subtle = env.subtle ?? globalThis.crypto?.subtle;
  const secure = env.isSecureContext ?? globalThis.isSecureContext;
  if (!cachesApi || !container || !secure) return { state: "unsupported" };
  try {
    // Solo cuenta el worker que controla esta página.
    if (!container.controller) return { state: "incomplete", missing: ["(service worker)"] };
    const info = await (env.getInfo ?? (() => getControllerInfo(container)))();
    if (!info) return { state: "incomplete", missing: ["(service worker)"] };
    if (!(await cachesApi.has(info.cache))) return { state: "incomplete", version: info.version, missing: ["(caché)"] };
    const cache = await cachesApi.open(info.cache);
    // El manifiesto se lee de la caché del controlador, no de la red ni de otra caché.
    const mres = await cache.match(MANIFEST);
    if (!mres) return { state: "incomplete", version: info.version, missing: [MANIFEST] };
    const manifest = await mres.clone().json();
    if (manifest.version !== info.version || manifest.cache !== info.cache) {
      return { state: "incomplete", version: info.version, missing: [MANIFEST] };
    }
    const missing = [];
    for (const f of manifest.files) {
      const hit = await cache.match(f.path, { ignoreSearch: true });
      if (!hit) { missing.push(f.path); continue; }
      if (hex(await subtle.digest("SHA-256", await hit.clone().arrayBuffer())) !== f.sha256) missing.push(f.path);
    }
    return missing.length
      ? { state: "incomplete", version: info.version, missing }
      : { state: "ready", version: info.version };
  } catch {
    return { state: "error" };
  }
}
