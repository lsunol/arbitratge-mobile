// Lista de confianza kid → clave pública (02 §3.7.9). Solo claves públicas.
import { decodeBase64UrlStrict } from "./base64url.js";

const KID_RE = /^[a-z0-9]{1,16}$/;
const HEX64_RE = /^[0-9a-f]{64}$/;
export const STATUSES = ["active", "retired", "compromised"];

function toHex(buf) {
  return Array.from(new Uint8Array(buf), (b) => b.toString(16).padStart(2, "0")).join("");
}

/**
 * Valida y carga entradas. Una entrada incoherente se descarta; un kid repetido se descarta
 * entero (no se elige ninguna).
 * @param {object[]} entries entradas con el esquema de 02 §3.7.9
 * @param {string[]|null} names nombres de fichero sin extensión, en paralelo a `entries`
 *   (en producción `kid` debe coincidir con el nombre del fichero). null = no se comprueba.
 * @returns {Promise<{keys: Map<string,{kid:string,status:string,key:CryptoKey}>, discarded: string[]}>}
 */
export async function buildTrustedKeys(entries, names = null, subtle = globalThis.crypto.subtle) {
  const discarded = [];
  const candidates = [];
  for (let idx = 0; idx < entries.length; idx++) {
    const e = entries[idx];
    const label = names?.[idx] ?? `#${idx}`;
    try {
      if (e === null || typeof e !== "object" || Array.isArray(e)) throw new Error("no es un objeto");
      if (typeof e.kid !== "string" || !KID_RE.test(e.kid)) throw new Error("kid");
      if (names && names[idx] !== e.kid) throw new Error("kid != nombre de fichero");
      if (e.alg !== "ES256") throw new Error("alg");
      if (!STATUSES.includes(e.status)) throw new Error("status");
      if (typeof e.sha256_spki !== "string" || !HEX64_RE.test(e.sha256_spki)) throw new Error("sha256_spki");
      if (typeof e.spki !== "string") throw new Error("spki");
      const der = decodeBase64UrlStrict(e.spki);
      if (!der || der.length === 0) throw new Error("spki no canónico");
      if (toHex(await subtle.digest("SHA-256", der)) !== e.sha256_spki) throw new Error("huella SPKI");
      const key = await subtle.importKey("spki", der, { name: "ECDSA", namedCurve: "P-256" }, false, ["verify"]);
      candidates.push({ kid: e.kid, status: e.status, key });
    } catch (err) {
      discarded.push(`${label}: ${err.message}`);
    }
  }
  const count = new Map();
  for (const c of candidates) count.set(c.kid, (count.get(c.kid) || 0) + 1);
  const keys = new Map();
  for (const c of candidates) {
    if (count.get(c.kid) > 1) {
      if (!discarded.some((d) => d === `${c.kid}: kid repetido`)) discarded.push(`${c.kid}: kid repetido`);
    } else {
      keys.set(c.kid, c);
    }
  }
  return { keys, discarded };
}

/** Carga en el navegador: keys/index.json = {"files":["s1.json", …]} (copia de trusted-keys/). */
export async function loadDeployedKeys(base = "keys/", fetchFn = fetch) {
  const idx = await (await fetchFn(base + "index.json")).json();
  const files = Array.isArray(idx.files) ? idx.files : [];
  const entries = [];
  const names = [];
  for (const f of files) {
    if (typeof f !== "string" || !/^[a-z0-9]{1,16}\.json$/.test(f)) continue;
    const r = await fetchFn(base + f);
    if (!r.ok) continue;
    entries.push(await r.json());
    names.push(f.slice(0, -5));
  }
  return buildTrustedKeys(entries, names);
}
