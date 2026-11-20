// Estado «preparat» (04 §5.8): solo si TODOS los ficheros de precache-manifest.json están en la
// caché de esa versión y su SHA-256 coincide. Instalada ≠ preparada; activar el SW tampoco basta.

const hex = (buf) => Array.from(new Uint8Array(buf), (b) => b.toString(16).padStart(2, "0")).join("");

/**
 * @returns {Promise<{state:'ready'|'incomplete'|'unsupported'|'error', version?:string, missing?:string[]}>}
 */
export async function checkPrepared() {
  if (!("caches" in globalThis) || !("serviceWorker" in navigator) || !globalThis.isSecureContext) {
    return { state: "unsupported" };
  }
  let manifest;
  try {
    const r = await fetch("precache-manifest.json");
    if (!r.ok) return { state: "incomplete", missing: ["precache-manifest.json"] };
    manifest = await r.json();
  } catch {
    return { state: "incomplete", missing: ["precache-manifest.json"] };
  }
  const version = manifest.version;
  try {
    if (!(await caches.has(manifest.cache))) return { state: "incomplete", version, missing: ["(caché)"] };
    const cache = await caches.open(manifest.cache);
    const missing = [];
    if (!(await cache.match("precache-manifest.json"))) missing.push("precache-manifest.json");
    for (const f of manifest.files) {
      const hit = await cache.match(f.path, { ignoreSearch: true });
      if (!hit) {
        missing.push(f.path);
        continue;
      }
      const digest = hex(await crypto.subtle.digest("SHA-256", await hit.clone().arrayBuffer()));
      if (digest !== f.sha256) missing.push(f.path);
    }
    const reg = await navigator.serviceWorker.getRegistration();
    if (!reg?.active) missing.push("(service worker)");
    return missing.length ? { state: "incomplete", version, missing } : { state: "ready", version };
  } catch {
    return { state: "error", version };
  }
}
