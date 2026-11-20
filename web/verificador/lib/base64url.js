// Base64url sin relleno (RFC 4648 §5), canónico: ARB1 (02 §3.7.1).
// No usa atob/Buffer: ninguno rechaza codificaciones no canónicas.
const ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
const REVERSE = new Int8Array(128).fill(-1);
for (let i = 0; i < 64; i++) REVERSE[ALPHABET.charCodeAt(i)] = i;

export function encodeBase64Url(bytes) {
  let out = "";
  let i = 0;
  for (; i + 2 < bytes.length; i += 3) {
    const n = (bytes[i] << 16) | (bytes[i + 1] << 8) | bytes[i + 2];
    out += ALPHABET[(n >> 18) & 63] + ALPHABET[(n >> 12) & 63] + ALPHABET[(n >> 6) & 63] + ALPHABET[n & 63];
  }
  const rest = bytes.length - i;
  if (rest === 1) {
    const n = bytes[i] << 16;
    out += ALPHABET[(n >> 18) & 63] + ALPHABET[(n >> 12) & 63];
  } else if (rest === 2) {
    const n = (bytes[i] << 16) | (bytes[i + 1] << 8);
    out += ALPHABET[(n >> 18) & 63] + ALPHABET[(n >> 12) & 63] + ALPHABET[(n >> 6) & 63];
  }
  return out;
}

/**
 * Decodifica base64url canónico. Devuelve Uint8Array o null si hay caracteres fuera del
 * alfabeto, longitud ≡ 1 (mod 4) o la codificación no es canónica (encode(decode(s)) !== s).
 */
export function decodeBase64UrlStrict(s) {
  if (typeof s !== "string") return null;
  const len = s.length;
  if (len % 4 === 1) return null;
  const vals = new Array(len);
  for (let i = 0; i < len; i++) {
    const c = s.charCodeAt(i);
    const v = c < 128 ? REVERSE[c] : -1;
    if (v < 0) return null;
    vals[i] = v;
  }
  const outLen = Math.floor((len * 6) / 8);
  const out = new Uint8Array(outLen);
  let o = 0;
  let i = 0;
  for (; i + 3 < len; i += 4) {
    const n = (vals[i] << 18) | (vals[i + 1] << 12) | (vals[i + 2] << 6) | vals[i + 3];
    out[o++] = (n >> 16) & 255;
    out[o++] = (n >> 8) & 255;
    out[o++] = n & 255;
  }
  const rest = len - i;
  if (rest === 2) {
    const n = (vals[i] << 18) | (vals[i + 1] << 12);
    out[o++] = (n >> 16) & 255;
  } else if (rest === 3) {
    const n = (vals[i] << 18) | (vals[i + 1] << 12) | (vals[i + 2] << 6);
    out[o++] = (n >> 16) & 255;
    out[o++] = (n >> 8) & 255;
  }
  return encodeBase64Url(out) === s ? out : null;
}
