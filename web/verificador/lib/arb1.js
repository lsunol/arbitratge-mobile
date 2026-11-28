// Verificador ARB1 (02 §3.7, canónico). JS plano + WebCrypto; sin dependencias.
// Debe devolver el mismo código que core-credential (Kotlin) para el mismo texto: test-vectors/.
import { decodeBase64UrlStrict } from "./base64url.js";
import { parseRestrictedJson } from "./json-restricted.js";

export const MAX_INPUT = 2048; // unidades UTF-16, antes de normalizar
export const MAX_TOKEN = 1024;
export const SUPPORTED_VERSION = 1;

/** Resultado de ADR 0001 para cada código (02 §3.7.8). */
export const RESULT_OF_CODE = Object.freeze({
  OK: "VÀLIDA",
  IS_URL: "FORMAT INCORRECTE",
  BAD_FORMAT: "FORMAT INCORRECTE",
  BAD_PAYLOAD: "FORMAT INCORRECTE",
  BAD_FIELDS: "FORMAT INCORRECTE",
  UNKNOWN_VERSION: "VERSIÓ DESCONEGUDA",
  UNKNOWN_KID: "NO VÀLIDA",
  KID_NOT_ACCEPTED: "NO VÀLIDA",
  BAD_SIGNATURE: "NO VÀLIDA",
});

// Orden del grupo N de P-256 (para el control explícito 1 ≤ r,s ≤ n−1, 02 §3.7.3).
const P256_N = 0xffffffff00000000ffffffffffffffffbce6faada7179e84f3b9cac2fc632551n;

const STRIP_RE = /[\t\n\r \u00a0\u200b\ufeff]/g;

/** §3.7.7. Devuelve la cadena normalizada o null (⇒ BAD_FORMAT). */
export function normalizeInput(raw) {
  if (typeof raw !== "string" || raw.length > MAX_INPUT) return null;
  const s = raw.replace(STRIP_RE, "");
  return s.length === 0 ? null : s;
}

/** Plegado de mayúsculas solo ASCII (nada de toLowerCase Unicode). */
function startsWithHttp(s) {
  const head = s.slice(0, 8).replace(/[A-Z]/g, (c) => String.fromCharCode(c.charCodeAt(0) + 32));
  return head.startsWith("http://") || head.startsWith("https://");
}

const PREFIX_RE = /^ARB([1-9][0-9]{0,2})\./;
const B64_PAYLOAD_RE = /^[A-Za-z0-9_-]{2,683}$/;
const B64_SIG_RE = /^[A-Za-z0-9_-]{86}$/;
const KID_RE = /^[a-z0-9]{1,16}$/;
const CID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;
const EXV_RE = /^[0-9A-Za-z._-]{1,16}$/;
const IAT_RE = /^([0-9]{4})-([0-9]{2})-([0-9]{2})$/;
const BV_RE = /^[0-9A-Za-z._+-]{1,24}$/;
const APP_RE = /^[0-9A-Za-z._+-]{1,24}$/;
const RESERVED = ["exp", "nbf", "crit", "alg"];

/** Conjunto F de §3.7.5.1 (lista fija, independiente de la versión de Unicode). */
export const FORBIDDEN_NAME_CHARS_RE =
  /[\u0000-\u001f\u007f-\u009f\u00a0\u00ad\u061c\u1680\u180e\u2000-\u200f\u2028-\u202f\u205f-\u206f\u3000\ufeff\ufff9-\ufffb]/;

function validIat(s) {
  const m = IAT_RE.exec(s);
  if (!m) return false;
  const y = Number(m[1]);
  const mo = Number(m[2]);
  const d = Number(m[3]);
  if (mo < 1 || mo > 12 || d < 1) return false;
  const leap = (y % 4 === 0 && y % 100 !== 0) || y % 400 === 0;
  const dim = [31, leap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31][mo - 1];
  return d <= dim;
}

function validNom(nom) {
  if (typeof nom !== "string") return false;
  const cps = [...nom].length;
  if (cps < 1 || cps > 80) return false;
  if (nom.startsWith(" ") || nom.endsWith(" ") || nom.includes("  ")) return false;
  return !FORBIDDEN_NAME_CHARS_RE.test(nom);
}

/** Código corto de §3.7.5: primeros 8 hex del cid en mayúsculas como XXXX-XXXX. */
export function shortCode(cid) {
  const h = cid.slice(0, 8).toUpperCase();
  return `${h.slice(0, 4)}-${h.slice(4)}`;
}

/** Paso 10: devuelve el objeto de campos o null (⇒ BAD_FIELDS). `m` es el Map del payload. */
function validateFields(m) {
  for (const r of RESERVED) if (m.has(r)) return null;
  const str = (k, re) => {
    const v = m.get(k);
    return typeof v === "string" && re.test(v) ? v : null;
  };
  const cid = str("cid", CID_RE);
  const exv = str("exv", EXV_RE);
  const iat = str("iat", IAT_RE);
  if (cid === null || exv === null || iat === null || !validIat(iat)) return null;
  const nom = m.get("nom");
  if (!validNom(nom)) return null;
  if (m.get("res") !== "APTE" || m.get("orig") !== "autoservei") return null;
  const fields = { v: m.get("v"), kid: m.get("kid"), cid, nom, res: "APTE", exv, iat, orig: "autoservei" };
  // Resultado obligatorio (§3.7.5): enteros JSON en rango; ok <= tot.
  const int = (k, min, max) => {
    const v = m.get(k);
    return Number.isInteger(v) && v >= min && v <= max ? v : null;
  };
  const ok = int("ok", 0, 99);
  const tot = int("tot", 1, 99);
  const dur = int("dur", 0, 86400);
  const fail = int("fail", 0, 99);
  if (ok === null || tot === null || dur === null || fail === null || ok > tot) return null;
  fields.ok = ok;
  fields.tot = tot;
  fields.dur = dur;
  fields.fail = fail;
  const bv = str("bv", BV_RE);
  const app = str("app", APP_RE);
  if (bv === null || app === null) return null;
  fields.bv = bv;
  fields.app = app;
  return fields;
}

function signatureInRange(sig) {
  let r = 0n;
  let s = 0n;
  for (let i = 0; i < 32; i++) {
    r = (r << 8n) | BigInt(sig[i]);
    s = (s << 8n) | BigInt(sig[32 + i]);
  }
  return r >= 1n && r < P256_N && s >= 1n && s < P256_N;
}

const fail = (code, extra) => ({ code, result: RESULT_OF_CODE[code], ...extra });

/**
 * Verifica un texto (§3.7.8). `trusted`: Map kid → {status, key:CryptoKey} (ver keys.js).
 * Devuelve {code, result, fields?, shortCode?, untrusted?}.
 * `untrusted` (solo si el payload se llegó a analizar): Map con los miembros leídos, que la UI
 * puede mostrar únicamente como «dades no fiables».
 */
export async function verifyToken(rawInput, trusted, subtle = globalThis.crypto.subtle) {
  // 1. Normalización
  const input = normalizeInput(rawInput);
  if (input === null) return fail("BAD_FORMAT");
  // 2. URL
  if (startsWithHttp(input)) return fail("IS_URL");
  // 3-4. Prefijo y versión (sin mirar el resto si la versión no es la soportada)
  const pm = PREFIX_RE.exec(input);
  if (!pm) return fail("BAD_FORMAT");
  if (Number(pm[1]) !== SUPPORTED_VERSION) return fail("UNKNOWN_VERSION");
  // 5. Envoltorio
  if (input.length > MAX_TOKEN) return fail("BAD_FORMAT");
  const parts = input.split(".");
  if (parts.length !== 3 || parts[0] !== "ARB1") return fail("BAD_FORMAT");
  const [, payloadSeg, sigSeg] = parts;
  if (!B64_PAYLOAD_RE.test(payloadSeg) || !B64_SIG_RE.test(sigSeg)) return fail("BAD_FORMAT");
  const payload = decodeBase64UrlStrict(payloadSeg);
  const sig = decodeBase64UrlStrict(sigSeg);
  if (!payload || !sig || payload.length < 1 || payload.length > 512 || sig.length !== 64) return fail("BAD_FORMAT");
  // 6. Payload
  let text;
  try {
    text = new TextDecoder("utf-8", { fatal: true, ignoreBOM: true }).decode(payload);
  } catch {
    return fail("BAD_PAYLOAD");
  }
  if (text.charCodeAt(0) === 0xfeff) return fail("BAD_PAYLOAD");
  const members = parseRestrictedJson(text);
  if (members === null) return fail("BAD_PAYLOAD");
  const v = members.get("v");
  const kid = members.get("kid");
  if (v !== SUPPORTED_VERSION || typeof kid !== "string" || !KID_RE.test(kid)) return fail("BAD_PAYLOAD");
  // 7-8. Clave
  const entry = trusted?.get(kid);
  if (!entry) return fail("UNKNOWN_KID", { untrusted: members });
  if (entry.status !== "active" && entry.status !== "retired") return fail("KID_NOT_ACCEPTED", { untrusted: members });
  // 9. Firma
  let ok = false;
  if (signatureInRange(sig)) {
    try {
      const signed = new TextEncoder().encode("ARB1." + payloadSeg); // ASCII
      ok = await subtle.verify({ name: "ECDSA", hash: "SHA-256" }, entry.key, sig, signed);
    } catch {
      ok = false;
    }
  }
  if (!ok) return fail("BAD_SIGNATURE", { untrusted: members });
  // 10. Campos
  const fields = validateFields(members);
  if (fields === null) return fail("BAD_FIELDS", { untrusted: members });
  return { code: "OK", result: "VÀLIDA", fields, shortCode: shortCode(fields.cid) };
}
