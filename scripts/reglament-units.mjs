#!/usr/bin/env node
// Segmenta el reglament (Markdown) en unitats citables amb ID estable: art-N i art-N.M.
// Sense dependències. Ús:
//   node scripts/reglament-units.mjs [etiqueta]      (per defecte, el contingut de docs/reglament/CURRENT)
// Llegeix  docs/reglament/<etiqueta>/reglament.md
// Escriu   docs/reglament/<etiqueta>/rules.json  ([{ "ref", "title", "chapter", "text" }], format de la
// skill rulebook-exam-questions).
// L'etiqueta és la data de la versió (AAAA-MM-DD) i és el valor del camp `rulebook_version`.
//
// Tolerant als nivells d'encapçalament (#…#####), als **negretes** i a <mark>. Si un subapartat
// porta un número d'article diferent de l'article actual (p. ex. «5.1» dins l'article 11) es
// numera segons l'article actual i s'avisa per stderr.
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const base = path.join(root, "docs/reglament");
const tag = process.argv[2] ?? fs.readFileSync(path.join(base, "CURRENT"), "utf8").trim();
const src = path.join(base, tag, "reglament.md");
const dst = path.join(base, tag, "rules.json");

const clean = (s) => s.replace(/<\/?mark>/g, "").replace(/\*\*/g, "").trim();
const lines = fs.readFileSync(src, "utf8").replace(/\r\n/g, "\n").split("\n");
const units = [];
const warnings = [];
let chapter = "";
let current = null;
let article = null;
let lastSub = 0;

const flush = () => {
  if (current) {
    current.text = current.text.join("\n").replace(/\n{3,}/g, "\n\n").trim();
    if (current.text) units.push(current);
  }
  current = null;
};

for (const raw of lines) {
  const hm = raw.match(/^(#{1,6})\s+(.*)$/);
  const heading = hm ? clean(hm[2]) : null;

  if (heading) {
    const ch = heading.match(/^CAP[ÍI]TOL\s+\d+.*$/i);
    if (ch) { flush(); chapter = heading; article = null; continue; }
    const art = heading.match(/^Article\s+(\d+)\s*[-–—]?\s*(.*)$/i);
    if (art) {
      flush();
      article = art[1];
      lastSub = 0;
      current = { ref: `art-${article}`, title: art[2].trim(), chapter, text: [] };
      continue;
    }
  }
  // Subapartats «4.1 Títol» / «4.3. Títol» (encapçalament o línia).
  const candidate = heading ?? clean(raw);
  const sub = article && candidate.match(/^(\d+)\.(\d+)\.?\s+(.+)$/);
  if (sub) {
    flush();
    if (sub[1] !== article) warnings.push(`«${sub[1]}.${sub[2]}» dins l'article ${article} → art-${article}.${sub[2]}`);
    lastSub = Number(sub[2]);
    current = { ref: `art-${article}.${sub[2]}`, title: sub[3].trim(), chapter, text: [sub[3].trim()] };
    continue;
  }
  if (current) current.text.push(clean(raw) === "" ? "" : raw.replace(/<\/?mark>/g, ""));
}
flush();

const refs = new Set();
for (const u of units) {
  if (refs.has(u.ref)) throw new Error(`ref duplicada: ${u.ref}`);
  refs.add(u.ref);
}
fs.writeFileSync(dst, JSON.stringify(units, null, 2) + "\n");
const arts = new Set(units.map((u) => u.ref.split(".")[0]));
console.log(`${path.relative(root, dst)}: ${units.length} unitats, ${arts.size} articles`);
for (const w of warnings) console.error(`avís: ${w}`);
