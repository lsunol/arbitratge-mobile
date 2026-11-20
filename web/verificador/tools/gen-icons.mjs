// Genera los iconos PNG/SVG (marcador de posición: escudo verde con ✔). Sin dependencias.
// Uso: node web/verificador/tools/gen-icons.mjs
import { deflateSync } from "node:zlib";
import { mkdirSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const out = join(dirname(fileURLToPath(import.meta.url)), "..", "icons");
mkdirSync(out, { recursive: true });

const crcTable = Array.from({ length: 256 }, (_, n) => {
  let c = n;
  for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
  return c >>> 0;
});
const crc32 = (buf) => {
  let c = 0xffffffff;
  for (const b of buf) c = crcTable[(c ^ b) & 255] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
};
function chunk(type, data) {
  const len = Buffer.alloc(4);
  len.writeUInt32BE(data.length);
  const td = Buffer.concat([Buffer.from(type, "ascii"), data]);
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(td));
  return Buffer.concat([len, td, crc]);
}
function png(size, rgba) {
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(size, 0);
  ihdr.writeUInt32BE(size, 4);
  ihdr[8] = 8; ihdr[9] = 6; // 8 bits RGBA
  const raw = Buffer.alloc((size * 4 + 1) * size);
  for (let y = 0; y < size; y++) {
    raw[y * (size * 4 + 1)] = 0;
    rgba.copy(raw, y * (size * 4 + 1) + 1, y * size * 4, (y + 1) * size * 4);
  }
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk("IHDR", ihdr), chunk("IDAT", deflateSync(raw, { level: 9 })), chunk("IEND", Buffer.alloc(0))]);
}

const GREEN = [0x1d, 0x6f, 0x42];
const WHITE = [255, 255, 255];
const distSeg = (px, py, ax, ay, bx, by) => {
  const dx = bx - ax, dy = by - ay;
  const t = Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)));
  return Math.hypot(px - (ax + t * dx), py - (ay + t * dy));
};
// Coordenadas en [0,1]. `scale` reduce el dibujo (zona segura de los iconos maskable).
function shade(x, y, { rounded, scale }) {
  const inBg = !rounded || (() => {
    const r = 0.18;
    const cx = Math.min(Math.max(x, r), 1 - r), cy = Math.min(Math.max(y, r), 1 - r);
    return Math.hypot(x - cx, y - cy) <= r;
  })();
  if (!inBg) return null;
  const u = (x - 0.5) / scale + 0.5, v = (y - 0.5) / scale + 0.5;
  // anillo
  const ring = Math.abs(Math.hypot(u - 0.5, v - 0.5) - 0.31) <= 0.045;
  // marca ✔
  const w = 0.05;
  const check = distSeg(u, v, 0.33, 0.5, 0.45, 0.62) <= w || distSeg(u, v, 0.45, 0.62, 0.68, 0.37) <= w;
  return ring || check ? WHITE : GREEN;
}
function render(size, opts) {
  const buf = Buffer.alloc(size * size * 4);
  const SS = 3;
  for (let y = 0; y < size; y++) {
    for (let x = 0; x < size; x++) {
      let r = 0, g = 0, b = 0, a = 0;
      for (let sy = 0; sy < SS; sy++) {
        for (let sx = 0; sx < SS; sx++) {
          const c = shade((x + (sx + 0.5) / SS) / size, (y + (sy + 0.5) / SS) / size, opts);
          if (c) { r += c[0]; g += c[1]; b += c[2]; a++; }
        }
      }
      const i = (y * size + x) * 4;
      if (a) { buf[i] = r / a; buf[i + 1] = g / a; buf[i + 2] = b / a; buf[i + 3] = Math.round((255 * a) / (SS * SS)); }
    }
  }
  return png(size, buf);
}

writeFileSync(join(out, "icon-192.png"), render(192, { rounded: true, scale: 1 }));
writeFileSync(join(out, "icon-512.png"), render(512, { rounded: true, scale: 1 }));
writeFileSync(join(out, "icon-maskable-512.png"), render(512, { rounded: false, scale: 0.78 }));
writeFileSync(join(out, "apple-touch-icon.png"), render(180, { rounded: false, scale: 0.9 }));
writeFileSync(
  join(out, "icon.svg"),
  `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect width="100" height="100" rx="18" fill="#1d6f42"/><circle cx="50" cy="50" r="31" fill="none" stroke="#fff" stroke-width="9"/><path d="M33 50 45 62 68 37" fill="none" stroke="#fff" stroke-width="10" stroke-linecap="round" stroke-linejoin="round"/></svg>\n`,
);
console.log("iconos generados en", out);
