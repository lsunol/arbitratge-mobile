#!/usr/bin/env node
// Segmenta el reglament (Markdown) en unitats citables amb ID estable: art-N i art-N.M.
// Sense dependències. Ús:
//   node scripts/reglament-units.mjs [docs/reglament/reglament-de-joc-2026-11.md] [sortida.json]
// Per defecte escriu docs/reglament/rules.json (format de la skill rulebook-exam-questions:
// [{ "ref", "title", "chapter", "text" }]).
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const src = process.argv[2] ?? path.join(root, "docs/reglament/reglament-de-joc-2026-11.md");
const dst = process.argv[3] ?? path.join(root, "docs/reglament/rules.json");

const lines = fs.readFileSync(src, "utf8").replace(/\r\n/g, "\n").split("\n");
const units = [];
let chapter = "";
let current = null;
let article = null;

const flush = () => {
  if (current) {
    current.text = current.text.join("\n").replace(/\n{3,}/g, "\n\n").trim();
    if (current.text) units.push(current);
  }
  current = null;
};

for (const line of lines) {
  const ch = line.match(/^## (.+)$/);
  if (ch) { flush(); chapter = ch[1].trim(); article = null; continue; }
  const art = line.match(/^### Article\s+(\d+)\s*[-–—]?\s*(.*)$/i);
  if (art) {
    flush();
    article = art[1];
    current = { ref: `art-${article}`, title: art[2].trim(), chapter, text: [] };
    continue;
  }
  // Subapartats "4.1 Títol" o "4.3. Títol" (també apartats que són el mateix text de la norma).
  const sub = article && line.match(new RegExp(`^${article}\\.(\\d+)\\.?\\s+(.+)$`));
  if (sub) {
    flush();
    current = { ref: `art-${article}.${sub[1]}`, title: sub[2].trim(), chapter, text: [sub[2].trim()] };
    continue;
  }
  if (current) current.text.push(line);
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
