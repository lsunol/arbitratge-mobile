// Analizador propio del JSON restringido de ARB1 (02 §3.7.4). No usa JSON.parse.
// Entrada: cadena ya decodificada de UTF-8 estricto (no puede contener sustitutos aislados
// literales, pero sí se comprueban por si el llamador pasa otra cosa).
// Devuelve un Map nombre → (string | number), o null si el texto no cumple la gramática.

export const MAX_MEMBERS = 16;

const WS = new Set([0x20, 0x09, 0x0a, 0x0d]);
const SIMPLE_ESCAPES = { '"': '"', "\\": "\\", "/": "/", b: "\b", f: "\f", n: "\n", r: "\r", t: "\t" };

export function parseRestrictedJson(text) {
  if (typeof text !== "string") return null;
  const n = text.length;
  let i = 0;

  const skipWs = () => {
    while (i < n && WS.has(text.charCodeAt(i))) i++;
  };
  const isHex = (c) => (c >= 0x30 && c <= 0x39) || (c >= 0x41 && c <= 0x46) || (c >= 0x61 && c <= 0x66);

  // Devuelve la cadena o null (error). Deja i tras la comilla de cierre.
  const parseString = () => {
    if (text[i] !== '"') return null;
    i++;
    let out = "";
    while (i < n) {
      const c = text.charCodeAt(i);
      if (c === 0x22) {
        i++;
        return out;
      }
      if (c < 0x20) return null;
      if (c === 0x5c) {
        i++;
        if (i >= n) return null;
        const e = text[i];
        if (e === "u") {
          if (i + 4 >= n) return null;
          for (let k = 1; k <= 4; k++) if (!isHex(text.charCodeAt(i + k))) return null;
          const cu = parseInt(text.slice(i + 1, i + 5), 16);
          i += 5;
          if (cu >= 0xd800 && cu <= 0xdbff) {
            // Debe seguir \uDC00-\uDFFF
            if (text[i] !== "\\" || text[i + 1] !== "u") return null;
            for (let k = 2; k <= 5; k++) if (!isHex(text.charCodeAt(i + k))) return null;
            const lo = parseInt(text.slice(i + 2, i + 6), 16);
            if (lo < 0xdc00 || lo > 0xdfff) return null;
            i += 6;
            out += String.fromCharCode(cu, lo);
          } else if (cu >= 0xdc00 && cu <= 0xdfff) {
            return null;
          } else {
            out += String.fromCharCode(cu);
          }
        } else if (Object.prototype.hasOwnProperty.call(SIMPLE_ESCAPES, e)) {
          out += SIMPLE_ESCAPES[e];
          i++;
        } else {
          return null;
        }
      } else if (c >= 0xd800 && c <= 0xdfff) {
        // sustituto literal: solo válido si forma par
        if (c <= 0xdbff) {
          const lo = text.charCodeAt(i + 1);
          if (!(lo >= 0xdc00 && lo <= 0xdfff)) return null;
          out += text[i] + text[i + 1];
          i += 2;
        } else {
          return null;
        }
      } else {
        out += text[i];
        i++;
      }
    }
    return null; // sin cierre
  };

  // Entero: 0 | [1-9][0-9]{0,8}. Cualquier otro inicio numérico, o seguido de . e E, es error.
  const parseInteger = () => {
    const start = i;
    const c0 = text.charCodeAt(i);
    if (c0 === 0x30) {
      i++;
    } else if (c0 >= 0x31 && c0 <= 0x39) {
      i++;
      while (i < n && text.charCodeAt(i) >= 0x30 && text.charCodeAt(i) <= 0x39) i++;
      if (i - start > 9) return null;
    } else {
      return null;
    }
    // Lo que sigue debe ser delimitador (espacio, coma o llave); cualquier otro carácter es error.
    if (i < n) {
      const c = text.charCodeAt(i);
      if (!(WS.has(c) || c === 0x2c || c === 0x7d)) return null;
    }
    return Number(text.slice(start, i));
  };

  skipWs();
  if (text[i] !== "{") return null;
  i++;
  const members = new Map();
  skipWs();
  if (text[i] === "}") {
    i++;
  } else {
    for (;;) {
      skipWs();
      const name = parseString();
      if (name === null) return null;
      skipWs();
      if (text[i] !== ":") return null;
      i++;
      skipWs();
      let value;
      if (text[i] === '"') value = parseString();
      else value = parseInteger();
      if (value === null) return null;
      if (members.has(name)) return null; // duplicado tras resolver escapes
      members.set(name, value);
      if (members.size > MAX_MEMBERS) return null;
      skipWs();
      if (text[i] === ",") {
        i++;
        continue;
      }
      if (text[i] === "}") {
        i++;
        break;
      }
      return null;
    }
  }
  skipWs();
  if (i !== n) return null;
  return members;
}
