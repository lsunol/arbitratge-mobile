// G1 (lado JS): ejecuta test-vectors/cases.json con la lista de confianza de PRUEBA.
// Uso: node web/verificador/test/run-vectors.mjs   (Node >= 20, sin dependencias)
import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { verifyToken, RESULT_OF_CODE } from "../lib/arb1.js";
import { buildTrustedKeys } from "../lib/keys.js";

const root = join(dirname(fileURLToPath(import.meta.url)), "..", "..", "..", "test-vectors");
const cases = JSON.parse(readFileSync(join(root, "cases.json"), "utf8"));
const trustedEntries = JSON.parse(readFileSync(join(root, cases.trustedKeys), "utf8"));
const { keys, discarded } = await buildTrustedKeys(trustedEntries); // un solo fichero: sin nombres
if (discarded.length || keys.size !== 2) {
  console.error("Lista de confianza de prueba inesperada:", discarded);
  process.exit(1);
}

const sortKeys = (o) => Object.fromEntries(Object.entries(o).sort(([a], [b]) => (a < b ? -1 : 1)));

let pass = 0;
const failures = [];
const perCode = new Map();
const seen = new Set();
for (const c of cases.cases) {
  if (seen.has(c.id)) failures.push(`id duplicado: ${c.id}`);
  seen.add(c.id);
  const r = await verifyToken(c.input, keys);
  let ok = r.code === c.code && r.result === c.result && RESULT_OF_CODE[c.code] === c.result;
  if (ok && c.code === "OK") {
    ok = JSON.stringify(sortKeys(r.fields)) === JSON.stringify(sortKeys(c.fields)) && r.shortCode === c.shortCode;
  }
  if (ok && c.code !== "OK" && r.fields) ok = false;
  const row = perCode.get(c.code) ?? { total: 0, pass: 0 };
  row.total++;
  if (ok) {
    pass++;
    row.pass++;
  } else failures.push(`${c.id}: esperado ${c.code}, obtenido ${r.code}`);
  perCode.set(c.code, row);
}
// Grupos obligatorios de 02 §3.7.11 (R01 y K02 son solo de release / G3).
const required = [
  "V01","V02","V03","V04","V05","V06","V07","V08","V09","V10","V11",
  "F01","F02","F03","F04","F05","F06","F07","F08","F09","U01",
  "P01","P02","P03","P04","P05","P06","P07","P08","P09","K01","K03",
  "S01","S02","S03","S04","S05","S06","C01","C02","C03","C04","C05","C06","C07","C08",
];
const groups = new Set(cases.cases.map((c) => c.group));
for (const g of required) if (!groups.has(g)) failures.push(`grupo ausente: ${g}`);
for (const g of ["R01", "K02"]) if (groups.has(g)) failures.push(`${g} no debe estar en los vectores públicos`);

console.log(`Casos: ${pass}/${cases.cases.length}`);
for (const [code, { total, pass: p }] of [...perCode].sort()) console.log(`  ${code.padEnd(17)} ${p}/${total}`);
if (failures.length || pass !== cases.cases.length) {
  console.error("FALLOS:\n" + failures.join("\n"));
  process.exit(1);
}
console.log("OK");
