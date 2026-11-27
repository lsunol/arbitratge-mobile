// Comprobaciones estáticas del despliegue: manifiesto al día, CSP, sin inline/red externa,
// lista de claves de producción sin claves de prueba, contraste de los resultados.
// Uso: node web/verificador/test/check-static.mjs
import assert from "node:assert/strict";
import { readFileSync, readdirSync, existsSync } from "node:fs";
import { createHash } from "node:crypto";
import { mkdtempSync, mkdirSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { buildManifest } from "../tools/gen-manifest.mjs";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const read = (p) => readFileSync(join(root, p), "utf8");
let n = 0;
const t = (name, fn) => {
  try { fn(); n++; } catch (e) { console.error("FALLA:", name, "\n", e.message); process.exitCode = 1; }
};

t("precache-manifest.json al día (ejecuta tools/gen-manifest.mjs y sube VERSION si cambia algo)", () => {
  assert.deepEqual(JSON.parse(read("precache-manifest.json")), buildManifest(root));
});
t("ficheros de texto con LF (los hashes dependen de los bytes; ver .gitattributes)", () => {
  for (const f of JSON.parse(read("precache-manifest.json")).files.map((x) => x.path).filter((x) => !x.endsWith(".png"))) {
    assert.ok(!readFileSync(join(root, f)).includes(13), `${f} tiene CR`);
  }
});
t("el manifiesto no incluye ficheros ocultos (.gitattributes)", () => {
  for (const f of JSON.parse(read("precache-manifest.json")).files) assert.ok(!/(^|\/)\./.test(f.path), f.path);
});
t("vendor/jsQR.js: procedencia (SHA-256 fijado = README = manifiesto)", () => {
  const PINNED = "bc40c8a15196236b2314db0856f72ca0b49980cd5413b8c852a7349f5fee0859"; // jsqr@1.4.0 dist/jsQR.js
  const actual = createHash("sha256").update(readFileSync(join(root, "vendor/jsQR.js"))).digest("hex");
  assert.equal(actual, PINNED);
  const m = /SHA-256 de `jsQR\.js`: `([0-9a-f]{64})`/.exec(read("vendor/README.md"));
  assert.ok(m, "README sin SHA-256 de 64 hex");
  assert.equal(m[1], actual);
  assert.equal(JSON.parse(read("precache-manifest.json")).files.find((f) => f.path === "vendor/jsQR.js").sha256, actual);
});
t("gen-manifest salta directorios y ficheros ocultos", () => {
  const d = mkdtempSync(join(tmpdir(), "mf-"));
  mkdirSync(join(d, ".git")); mkdirSync(join(d, "a", ".cache"), { recursive: true });
  writeFileSync(join(d, "sw.js"), 'const VERSION = "9.9.9";');
  for (const f of [".git/x.js", "a/.cache/y.js", ".gitattributes", "z.js", "a/ok.js"]) writeFileSync(join(d, f), "x");
  assert.deepEqual(buildManifest(d).files.map((f) => f.path), ["a/ok.js", "z.js"]);
});
t("versión de sw.js = manifiesto", () => {
  assert.equal(/const VERSION = "([^"]+)"/.exec(read("sw.js"))[1], JSON.parse(read("precache-manifest.json")).version);
});
t("CSP de 05 §6.7 literal", () => {
  const expected =
    "default-src 'none'; script-src 'self'; style-src 'self'; img-src 'self' data: blob:; media-src 'self' blob:; manifest-src 'self'; worker-src 'self'; connect-src 'self'; base-uri 'none'; form-action 'none'; object-src 'none'";
  assert.ok(read("index.html").includes(`<meta http-equiv="Content-Security-Policy" content="${expected}">`));
});
t("sin scripts/estilos inline ni manejadores on*", () => {
  const html = read("index.html");
  for (const m of html.matchAll(/<script\b[^>]*>/g)) assert.match(m[0], /\bsrc="/, m[0]);
  assert.ok(!/<style\b/i.test(html));
  assert.ok(!/\sstyle\s*=/i.test(html));
  assert.ok(!/\son[a-z]+\s*=/i.test(html));
});
t("sin red externa ni APIs peligrosas en el código propio", () => {
  const own = ["index.html", "app.js", "sw.js", "styles.css", "manifest.webmanifest", ...readdirSync(join(root, "lib")).map((f) => `lib/${f}`)];
  for (const f of own) {
    const s = read(f);
    assert.ok(!/(src|href)="https?:/.test(s), `${f}: recurso externo`);
    assert.ok(!/url\(\s*['"]?https?:/.test(s), `${f}: url() externa`);
    assert.ok(!/XMLHttpRequest|WebSocket|sendBeacon|EventSource|\beval\(|new Function|innerHTML|document\.write|importScripts/.test(s), `${f}: API no permitida`);
    for (const m of s.matchAll(/\bfetch\(([^)]*)\)/g)) assert.ok(!/https?:/.test(m[1]), `${f}: fetch externo`);
  }
});
t("keys/: lista de producción sin claves de prueba", () => {
  const idx = JSON.parse(read("keys/index.json"));
  for (const f of idx.files) assert.ok(existsSync(join(root, "keys", f)), f);
  const files = readdirSync(join(root, "keys")).filter((f) => f.endsWith(".json") && f !== "index.json");
  assert.deepEqual([...files].sort(), [...idx.files].sort(), "index.json debe listar exactamente los ficheros");
  for (const f of files) {
    const e = JSON.parse(read(`keys/${f}`));
    assert.ok(!["test", "testx", "dev"].includes(e.kid), `${f}: clave de prueba en producción`);
    assert.equal(`${e.kid}.json`, f);
    assert.ok(!("pkcs8" in e) && !JSON.stringify(e).includes("PRIVATE"), `${f}: material privado`);
  }
});
t("manifiesto web: iconos existen y están precacheados", () => {
  const wm = JSON.parse(read("manifest.webmanifest"));
  assert.equal(wm.lang, "ca");
  assert.equal(wm.display, "standalone");
  const files = new Set(JSON.parse(read("precache-manifest.json")).files.map((f) => f.path));
  for (const i of wm.icons) assert.ok(files.has(i.src), i.src);
  for (const need of ["index.html", "app.js", "styles.css", "vendor/jsQR.js", "manifest.webmanifest", "lib/arb1.js", "keys/index.json"]) {
    assert.ok(files.has(need), `falta en el precache: ${need}`);
  }
});
t("contraste texto blanco sobre colores de resultado ≥ 4,5:1", () => {
  const lum = (hex) => {
    const c = [1, 3, 5].map((i) => parseInt(hex.slice(i, i + 2), 16) / 255).map((v) => (v <= 0.03928 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4));
    return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2];
  };
  const css = read("styles.css");
  for (const name of ["r-valid", "r-invalid", "r-unknown", "r-format"]) {
    const hex = new RegExp(`--${name}:\\s*(#[0-9a-f]{6})`, "i").exec(css)[1];
    const ratio = 1.05 / (lum(hex) + 0.05);
    assert.ok(ratio >= 4.5, `${name} ${hex} ${ratio.toFixed(2)}`);
    console.log(`  contraste ${name} ${hex}: ${ratio.toFixed(2)}:1`);
  }
});
t("UI-07: textos retirados del verificador no existen", () => {
  const all = ["index.html", "app.js", "lib/text.js"].map(read).join("\n");
  for (const bad of ["Adreça que estàs fent servir", "Obre sempre aquest verificador", "Credencial emesa en autoservei", "coincideix amb el document", "Comprovat en aquest dispositiu amb les claus", "L'adreça és correcta"]) {
    assert.ok(!all.includes(bad), `no debe existir: ${bad}`);
  }
});

if (!process.exitCode) console.log(`static: ${n} comprobaciones OK`);
