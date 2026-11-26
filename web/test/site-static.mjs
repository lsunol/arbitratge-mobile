// Comprobaciones estáticas de la landing «/» y de su aislamiento del verificador.
// Uso: node web/test/site-static.mjs   (Node >= 20, sin dependencias)
import assert from "node:assert/strict";
import { cpSync, existsSync, lstatSync, mkdtempSync, readFileSync, readdirSync, rmSync, rmdirSync, statSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join, relative, resolve, sep } from "node:path";
import { fileURLToPath } from "node:url";
import { spawnSync } from "node:child_process";
import { createHash, generateKeyPairSync } from "node:crypto";
import { mkdirSync, symlinkSync } from "node:fs";
import { buildManifest } from "../verificador/tools/gen-manifest.mjs";
import { buildMetadata } from "../../scripts/fetch-release-metadata.mjs";
import { stage, BRAND_FILES, FONT_FILES, LANDING_FILES, MARKER } from "../../scripts/stamp-web.mjs";

const web = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const read = (p) => readFileSync(join(web, p), "utf8");
let n = 0;
const t = (name, fn) => {
  try { fn(); n++; } catch (e) { console.error("FALLA:", name, "\n ", e.message); process.exitCode = 1; }
};
const html = read("index.html");
const css = read("styles.css");
const tmp = mkdtempSync(join(tmpdir(), "site-"));
process.on("exit", () => rmSync(tmp, { recursive: true, force: true }));
const walk = (d) => readdirSync(d).flatMap((x) => (statSync(join(d, x)).isDirectory() ? walk(join(d, x)) : [join(d, x)]));

// --- CSP y ausencia de JS ---
t("CSP exacta: sin script-src, fuentes y estilos solo 'self'", () => {
  const m = /<meta http-equiv="Content-Security-Policy" content="([^"]+)">/.exec(html);
  assert.ok(m, "sin CSP");
  const d = Object.fromEntries(m[1].split(";").map((x) => x.trim().split(/\s+/)).map(([k, ...v]) => [k, v.join(" ")]));
  assert.deepEqual(d, { "default-src": "'none'", "style-src": "'self'", "font-src": "'self'", "img-src": "'self'", "base-uri": "'none'", "form-action": "'none'", "object-src": "'none'" });
});
t("sin JavaScript: ni <script>, ni on*=, ni javascript:, ni style= en línea", () => {
  assert.ok(!/<script/i.test(html));
  assert.ok(!/\son[a-z]+\s*=/i.test(html));
  assert.ok(!/javascript:/i.test(html));
  assert.ok(!/\sstyle\s*=/i.test(html), "style= en línea lo bloquea style-src 'self'");
  assert.ok(!/<style/i.test(html));
  assert.ok(!/@import|expression\(/.test(css));
});
t("sin recursos externos (ni http:// ni https:// salvo canonical/og:url con __HOST__)", () => {
  for (const m of html.matchAll(/https?:\/\/[^\s"'<>]+/g)) assert.match(m[0], /^https:\/\/__HOST__\/$/, m[0]);
  for (const m of css.matchAll(/url\(\s*["']?([^"')]+)/g)) assert.ok(!/^(https?:)?\/\//.test(m[1]) && !m[1].startsWith("data:"), m[1]);
});

// --- Aislamiento de scope / manifest ---
t("la landing no enlaza manifest ni registra service worker", () => {
  assert.ok(!/rel=["']?manifest/i.test(html));
  assert.ok(!/serviceWorker|manifest\.webmanifest|sw\.js/i.test(html + css));
  assert.ok(!existsSync(join(web, "sw.js")) && !existsSync(join(web, "manifest.webmanifest")));
});
t("el verificador sigue acotado a /verificador/ (scope relativo, sw sin scope ampliado)", () => {
  const mf = JSON.parse(read("verificador/manifest.webmanifest"));
  assert.equal(mf.scope, "./"); assert.equal(mf.start_url, "./"); assert.equal(mf.id, "./");
  const app = read("verificador/app.js");
  assert.match(app, /serviceWorker\.register\("sw\.js"\)/);
  assert.ok(!/Service-Worker-Allowed|scope:\s*["']\//.test(app + read("verificador/sw.js")));
  assert.match(read("verificador/index.html"), /rel="manifest" href="manifest\.webmanifest"/);
});
t("el precache del verificador solo contiene rutas propias (nada de la landing ni de fuera)", () => {
  const files = JSON.parse(read("verificador/precache-manifest.json")).files.map((f) => f.path);
  for (const f of files) assert.ok(!f.startsWith("/") && !f.includes(".."), f);
  assert.ok(!files.some((f) => /fonts\/|brand\//.test(f)));
  assert.ok(!/\.\.\//.test(read("verificador/index.html")) && !/\.\.\//.test(read("verificador/styles.css")), "el verificador no depende de la landing");
});
t("la landing no usa verificador/styles.css y viceversa", () => {
  assert.ok(!/verificador\/styles\.css/.test(html));
  assert.ok(!/href="(\.\.\/|\/)styles\.css"/.test(read("verificador/index.html")));
  assert.ok(!/verificador\//.test(css));
});

// --- Contenido ---
t("estructura: lang ca, un h1, skip link, landmarks, sin target=_blank", () => {
  assert.match(html, /<html lang="ca">/);
  assert.equal((html.match(/<h1[ >]/g) || []).length, 1);
  assert.match(html, /<a class="skip" href="#contingut">Salta al contingut<\/a>/);
  for (const tag of ["<header", "<main", "<footer"]) assert.ok(html.includes(tag), tag);
  assert.ok(!/target=/i.test(html));
  assert.match(html, /name="viewport" content="width=device-width, initial-scale=1">/);
  assert.ok(!/user-scalable|maximum-scale/.test(html));
});
t("textos catalans exigits (spec UX/visual, 04 §7.8)", () => {
  for (const s of [
    "Examen d'àrbitre de Futbol amb Botons",
    "Fes l'examen al mòbil, sense internet. Si l'aproves, aconsegueixes una credencial d'àrbitre.",
    "Descarregar l'app (APK)",
    "Només per a Android 8.0 o superior.",
    "Si no tens un mòbil Android, aquesta app no funcionarà.",
    "Vols comprovar una credencial?", "Obre el verificador", "Obrir el verificador", "Verificar una credencial",
    "Per a organitzadors i qualsevol persona que rebi una credencial d'àrbitre del CFBB. Es comprova al teu navegador; no s'envia cap dada.",
    "Si algú t'ha enviat un enllaç al verificador, no l'obris: entra des d'aquesta pàgina o escriu l'adreça tu mateix.",
    "Aquesta marca ajuda a detectar canvis casuals; no és una prova contra un dispositiu modificat o una clau extreta.",
    "Com instal·lar-la", "Si el mòbil o Play Protect diuen que l'app és una amenaça, és nociva o ha estat bloquejada",
    "Comprovació avançada", "SHA-256 de l'APK", "Privacitat", "Aquesta pàgina no té formularis, galetes ni analítica pròpia.",
    "Descarrega l'app només des de l'adreça que t'ha donat el CFBB.", "Exemple amb dades inventades. Representació simplificada de la credencial que obtens si aproves.",
  ]) assert.ok(html.includes(s), "falta: " + s);
  assert.ok(!html.includes("Així és la credencial"));
  for (const bad of ["segur", "oficial", "garantit", "VÀLIDA", "✓", "✔"]) assert.ok(!html.includes(bad), "no debe aparecer: " + bad);
});
t("enlaces: verificador ≥2 relativos, CTA APK con marcador, el resto locales o ancla", () => {
  assert.ok((html.match(/href="verificador\/"/g) || []).length >= 2);
  assert.ok((html.match(/href="__APK_URL__"/g) || []).length >= 1);
  for (const m of html.matchAll(/href="([^"]+)"/g)) assert.ok(/^(#|verificador\/$|__APK_URL__$|__RELEASE_NOTES_URL__$|brand\/|fonts\/|styles\.css$|https:\/\/__HOST__\/$)/.test(m[1]), m[1]);
});
t("privacidad en <details>; sin contacto inventado", () => {
  assert.match(html, /<details class="disc">\s*<summary><span>Privacitat/);
  assert.ok(!/mailto:|tel:|\b[\w.-]+@[\w-]+\.[a-z]{2,}/i.test(html));
});
t("sin afirmaciones reales: ni fecha, ni tamaño, ni hash en el código fuente; marcadores esperados", () => {
  assert.ok(!/\b\d+(,\d+)? ?MB\b/.test(html));
  assert.ok(!/\b20\d\d-\d\d-\d\d\b|\b\d{1,2}\/\d{1,2}\/20\d\d\b/.test(html));
  assert.ok(!/\b[0-9a-f]{40,}\b/i.test(html));
  assert.deepEqual([...new Set(html.match(/__[A-Z0-9_]+__/g))].sort(), ["__APK_URL__", "__CERT_SHA256__", "__HOST_WBR__", "__HOST__", "__PUBLISHED__", "__RELEASE_NOTES_URL__", "__SHA256__", "__VERSION__"]);
});
t("referencias locales existen (href/src/url()) y licencias OFL presentes", () => {
  const refs = [...html.matchAll(/(?:href|src)="([^"#][^"]*)"/g)].map((m) => m[1]).filter((r) => !r.startsWith("__") && !r.startsWith("https:") && !r.endsWith("/"));
  refs.push(...[...css.matchAll(/url\("([^"]+)"\)/g)].map((m) => m[1]));
  assert.ok(refs.length >= 6);
  for (const r of refs) assert.ok(existsSync(join(web, r)), "no existe " + r);
  assert.ok(existsSync(join(web, "verificador/index.html")));
  for (const f of ["fonts/OFL-Atkinson-Next.txt", "fonts/OFL-Atkinson-Mono.txt"]) assert.match(read(f), /SIL OPEN FONT LICENSE/);
});
t("CNAME de un solo dominio, .nojekyll, latest.json con marcadores", () => {
  assert.equal(read("CNAME"), "arbitratge.futbolbotons.cat\n");
  assert.ok(existsSync(join(web, ".nojekyll")));
  assert.deepEqual(JSON.parse(read("latest.json")), { schema: 1, app: "candidate", versionName: "__VERSION__", versionCode: "__VERSION_CODE__", apk_sha256: "__SHA256__", download_page: "https://__HOST__/" });
});
t("CSS: foco visible, reduced-motion, modo oscuro, sin 100vh", () => {
  assert.match(css, /:focus-visible/); assert.match(css, /prefers-reduced-motion/); assert.match(css, /prefers-color-scheme: dark/);
  assert.ok(!/100vh/.test(css));
});

// --- Estampado (CI) ---
const REPO = "lsunol/arbitratge-mobile";
const GOOD = {
  mode: "preview", version: "1.2.3", versionCode: "102039",
  apkUrl: `https://github.com/${REPO}/releases/download/v1.2.3/arbitratge.apk`, sha256: "ab".repeat(32),
};
const FULL = { ...GOOD, certSha256: "CD".repeat(32), published: "2027-03-14", releaseNotesUrl: `https://github.com/${REPO}/releases/tag/v1.2.3` };
let nOut = 0;
const newOut = () => { const d = join(tmp, "o" + ++nOut); mkdirSync(d); return join(d, "_site"); };
const files = (out) => walk(out).map((p) => relative(out, p).split(sep).join("/")).sort();
const copyWeb = (name) => { const w = join(tmp, name + "-web"); cpSync(web, w, { recursive: true }); return w; };
// Clave pública de prueba generada al vuelo (solo en tmp; no se versiona)
const fakeKey = (kid, over = {}) => {
  const { publicKey } = generateKeyPairSync("ec", { namedCurve: "prime256v1" });
  const der = publicKey.export({ type: "spki", format: "der" });
  return { kid, alg: "ES256", spki: der.toString("base64url"), sha256_spki: createHash("sha256").update(der).digest("hex"), status: "active", valid_from: "2027-01-01", ...over };
};
const withKeys = (name, entries, listed = entries.map((e) => e.kid + ".json")) => {
  const w = copyWeb(name);
  for (const f of readdirSync(join(w, "verificador/keys"))) if (f !== "index.json" && f.endsWith(".json")) rmSync(join(w, "verificador/keys", f)); // quita las claves reales de la copia
  for (const e of entries) writeFileSync(join(w, "verificador/keys", e.kid + ".json"), JSON.stringify(e));
  writeFileSync(join(w, "verificador/keys/index.json"), JSON.stringify({ files: listed }));
  writeFileSync(join(w, "verificador/precache-manifest.json"), JSON.stringify(buildManifest(join(w, "verificador")), null, 2) + "\n");
  return w;
};

t("stamp preview: sin marcadores, latest.json según 05 §6.5, sin URL del APK, lista blanca exacta", () => {
  const out = newOut();
  const r = stage({ ...GOOD, out });
  assert.equal(r.host, "arbitratge.futbolbotons.cat");
  const o = readFileSync(join(out, "index.html"), "utf8");
  assert.ok(!/__[A-Z0-9_]+__|\{\{|<!--\/?(CERT|PUBLISHED|NOTES)/.test(o));
  assert.ok(o.includes(`href="${GOOD.apkUrl}"`) && o.includes("Versió 1.2.3. Només") && o.includes(GOOD.sha256));
  assert.ok(!o.includes("Empremta") && !o.includes("Notes de la versió") && !o.includes("Publicada"), "lo que falta no se publica");
  assert.deepEqual(JSON.parse(readFileSync(join(out, "latest.json"), "utf8")), { schema: 1, app: "candidate", versionName: "1.2.3", versionCode: 102039, apk_sha256: GOOD.sha256, download_page: "https://arbitratge.futbolbotons.cat/" });
  assert.ok(!/apk_url|\.apk/.test(readFileSync(join(out, "latest.json"), "utf8")));
  const fl = files(out);
  assert.ok(fl.includes(MARKER));
  for (const f of ["sw.js", "manifest.webmanifest"]) assert.ok(!fl.includes(f), "raíz: " + f);
  assert.ok(!fl.some((f) => /(^|\/)(test|tools|fixtures|test-vectors)\//.test(f) || /README/.test(f)));
  assert.deepEqual(fl.filter((f) => f.startsWith("verificador/keys/")), ["verificador/keys/index.json", "verificador/keys/s1.json"]); // clave de producción real
  assert.deepEqual(fl.filter((f) => !f.startsWith("verificador/")), [...new Set([MARKER, ...LANDING_FILES, ...BRAND_FILES, ...FONT_FILES, "latest.json"])].sort());
  for (const f of JSON.parse(readFileSync(join(out, "verificador/precache-manifest.json"), "utf8")).files) assert.ok(fl.includes("verificador/" + f.path), f.path);
});
t("stamp con --host de Pages (sin dominio propio): landing y latest.json con esa URL, sin CNAME; otros hosts se rechazan", () => {
  const out = newOut();
  const r = stage({ ...GOOD, host: "lsunol.github.io/arbitratge-mobile", out });
  assert.equal(r.host, "lsunol.github.io/arbitratge-mobile");
  const o = readFileSync(join(out, "index.html"), "utf8");
  assert.ok(o.includes('href="https://lsunol.github.io/arbitratge-mobile/"') && !o.includes("futbolbotons"));
  assert.equal(JSON.parse(readFileSync(join(out, "latest.json"), "utf8")).download_page, "https://lsunol.github.io/arbitratge-mobile/");
  assert.ok(!files(out).includes("CNAME"), "el CNAME no se publica sin dominio propio");
  for (const h of ["evil.github.io/arbitratge-mobile", "lsunol.github.io/otro", "example.com"]) assert.throws(() => stage({ ...GOOD, host: h, out: newOut() }), /--host solo puede ser/, h);
});
t("stamp con cert, fecha y notas: la landing los muestra y latest.json los incluye", () => {
  const out = newOut();
  stage({ ...FULL, out });
  const o = readFileSync(join(out, "index.html"), "utf8");
  assert.ok(o.includes("cd".repeat(32)) && o.includes("Publicada el 2027-03-14.") && o.includes(`href="${FULL.releaseNotesUrl}"`) && o.includes("Notes de la versió"));
  assert.ok(!/__[A-Z0-9_]+__|<!--\/?(CERT|PUBLISHED|NOTES)/.test(o));
  const l = JSON.parse(readFileSync(join(out, "latest.json"), "utf8"));
  assert.equal(l.cert_sha256, "cd".repeat(32)); assert.equal(l.published, "2027-03-14"); assert.equal(l.release_notes_url, FULL.releaseNotesUrl);
});
t("stamp falla cerrado ante datos inválidos (modo, versión, versionCode, URL, repo, prerelease, hash, fecha, notas)", () => {
  const f = (over, re) => assert.throws(() => stage({ ...FULL, ...over, out: newOut() }), re);
  f({ mode: undefined }, /modo/);
  f({ version: undefined }, /versión final/);
  f({ version: "1.2.3-rc.1", versionCode: "102032", apkUrl: `https://github.com/${REPO}/releases/download/v1.2.3-rc.1/a.apk`, releaseNotesUrl: `https://github.com/${REPO}/releases/tag/v1.2.3-rc.1` }, /versión final/);
  f({ version: "1.2.3+build" }, /versión final/);
  f({ versionCode: undefined }, /version-code/);
  f({ versionCode: "103009" }, /fórmula/);
  f({ apkUrl: undefined }, /apk-url/);
  f({ apkUrl: `https://github.com/${REPO}/releases/latest/download/arbitratge.apk` }, /apk-url/);
  f({ apkUrl: `http://github.com/${REPO}/releases/download/v1.2.3/a.apk` }, /apk-url/);
  f({ apkUrl: "https://github.com/cfbbarcelona/arbitratge-mobile/releases/download/v1.2.3/a.apk" }, /apk-url/);
  f({ apkUrl: "https://github.com/lsunol/otro-repo/releases/download/v1.2.3/a.apk" }, /apk-url/);
  f({ apkUrl: `https://github.com/${REPO}/releases/download/v9.9.9/a.apk` }, /apk-url/);
  f({ apkUrl: `https://github.com/${REPO}/releases/download/v1.2.3/a.apk?x=1` }, /apk-url/);
  f({ apkUrl: `https://github.com/${REPO}/releases/download/v1.2.3/sub/a.apk` }, /apk-url/);
  f({ sha256: undefined }, /sha256/);
  f({ sha256: "abc" }, /sha256/);
  f({ certSha256: "zz" }, /cert-sha256/);
  f({ published: "2027-02-30" }, /published/);
  f({ published: "14/03/2027" }, /published/);
  f({ releaseNotesUrl: "https://example.com/notes" }, /release-notes-url/);
  f({ downloadPage: "https://otro.example/" }, /download-page/);
});
const metaFile = (name, over = {}, assetOver = {}) => {
  const m = {
    schema: 1, repo: REPO, tag: "v1.2.3", draft: false, prerelease: false, html_url: `https://github.com/${REPO}/releases/tag/v1.2.3`, published_at: "2027-03-14T10:00:00Z",
    asset: { name: "arbitratge.apk", browser_download_url: GOOD.apkUrl, size: 1234, sha256: GOOD.sha256, ...assetOver }, ...over,
  };
  const p = join(tmp, name + ".json"); writeFileSync(p, JSON.stringify(m)); return p;
};
// Copia "aprobada" de trusted-keys/ para los tests (mismos bytes que los desplegados salvo que se pida otra cosa)
const trusted = (name, entries) => {
  const d = join(tmp, name + "-trusted"); mkdirSync(d);
  for (const e of entries) writeFileSync(join(d, e.kid + ".json"), JSON.stringify(e));
  return d;
};
const PROD = () => ({ ...FULL, mode: "production", bankVersion: "2027.01.0", releaseMetadata: metaFile("meta-ok") });
t("--production exige cert, fecha, bank-version, notas, release-metadata y claves no vacías; --preview las admite vacías", () => {
  for (const k of ["certSha256", "published", "releaseNotesUrl", "bankVersion", "releaseMetadata"]) assert.throws(() => stage({ ...PROD(), [k]: undefined, out: newOut() }), /production exige/, k);
  assert.throws(() => stage({ ...PROD(), web: withKeys("vacia", []), out: newOut() }), /lista de claves de producción está vacía/);
  stage({ ...FULL, mode: "preview", out: newOut() });
  const k1 = fakeKey("p1");
  const w = withKeys("prodok", [k1]);
  const out = newOut();
  stage({ ...PROD(), web: w, trustedKeysDir: trusted("tk-ok", [k1]), out });
  assert.deepEqual(files(out).filter((x) => x.startsWith("verificador/keys/")), ["verificador/keys/index.json", "verificador/keys/p1.json"]);
  assert.equal(JSON.parse(readFileSync(join(out, "latest.json"), "utf8")).bank_version, "2027.01.0");
  const k2 = fakeKey("p2", { status: "retired" });
  assert.throws(() => stage({ ...PROD(), web: withKeys("retired", [k2]), trustedKeysDir: trusted("tk-ret", [k2]), out: newOut() }), /ninguna clave activa/);
});
t("--production liga las claves desplegadas a trusted-keys/ (mismo conjunto y bytes); --preview no lo exige", () => {
  const k = fakeKey("p1"), other = fakeKey("p9");
  const w = withKeys("bind", [k]);
  const run = (dir) => () => stage({ ...PROD(), web: w, trustedKeysDir: dir, out: newOut() });
  // clave válida recién generada pero sin fuente aprobada
  assert.throws(run(join(tmp, "no-existe-trusted")), /falta trusted-keys/);
  assert.throws(run(trusted("tk-empty", [])), /no contiene claves aprobadas/);
  // fuente aprobada con otra clave (mismo kid, otros bytes)
  assert.throws(run(trusted("tk-diff", [fakeKey("p1")])), /difiere byte a byte/);
  // mismos datos, distinta serialización
  const d = trusted("tk-ws", [k]); writeFileSync(join(d, "p1.json"), JSON.stringify(k, null, 2));
  assert.throws(run(d), /difiere byte a byte/);
  // conjunto distinto: sobra en trusted-keys (no desplegada) / sobra en el despliegue (no aprobada)
  assert.throws(run(trusted("tk-extra", [k, other])), /faltan: p9\.json/);
  assert.throws(run(trusted("tk-other", [other])), /faltan: p9\.json; sobran: p1\.json/);
  const w2 = withKeys("bind2", [k, other]);
  assert.throws(() => stage({ ...PROD(), web: w2, trustedKeysDir: trusted("tk-one", [k]), out: newOut() }), /sobran: p9\.json/);
  // positivo con los mismos bytes
  stage({ ...PROD(), web: w2, trustedKeysDir: trusted("tk-two", [k, other]), out: newOut() });
  // preview: no necesita trusted-keys (ni existente ni coincidente)
  const out = newOut();
  stage({ ...FULL, mode: "preview", web: w, trustedKeysDir: join(tmp, "no-existe-trusted"), out });
  assert.deepEqual(files(out).filter((x) => x.startsWith("verificador/keys/")), ["verificador/keys/index.json", "verificador/keys/p1.json"]);
  // sin trustedKeysDir explícito se usa <repo>/trusted-keys (real): el despliegue de prueba (p1) no coincide con las claves aprobadas
  assert.throws(() => stage({ ...PROD(), web: w, out: newOut() }), /no coincide con trusted-keys/);
});
t("bank-version: formato exacto [0-9A-Za-z._+-]{1,24}; opcional en preview y se emite si se da", () => {
  for (const b of ["", "a b", "x".repeat(25), "ñ", "a/b", "a;b"]) assert.throws(() => stage({ ...GOOD, bankVersion: b, out: newOut() }), /bank-version/, JSON.stringify(b));
  const out = newOut(); stage({ ...GOOD, bankVersion: "2027.01.0-rc+1_x", out });
  assert.equal(JSON.parse(readFileSync(join(out, "latest.json"), "utf8")).bank_version, "2027.01.0-rc+1_x");
});
t("release-metadata: borrador/prerelease, tag, repo, asset, URL, hash, fecha y notas deben coincidir", () => {
  const f = (over, assetOver, re, extra = {}) => assert.throws(() => stage({ ...FULL, mode: "preview", releaseMetadata: metaFile("m" + ++nOut, over, assetOver), out: newOut(), ...extra }), re);
  stage({ ...FULL, releaseMetadata: metaFile("mgood"), mode: "preview", out: newOut() });
  f({ draft: true }, {}, /borrador o prerelease/);
  f({ prerelease: true }, {}, /borrador o prerelease/);
  f({ tag: "v1.2.4" }, {}, /tag distinto/);
  f({ repo: "cfbbarcelona/arbitratge-mobile" }, {}, /repositorio distinto/);
  f({}, { name: "otro.apk" }, /asset no coincide/);
  f({}, { browser_download_url: `https://github.com/${REPO}/releases/download/v1.2.3/otro.apk` }, /asset no coincide/);
  f({}, { sha256: "00".repeat(32) }, /SHA-256 del asset/);
  f({}, { size: 0 }, /size/);
  f({ published_at: "2027-03-15T00:00:00Z" }, {}, /published no coincide/);
  f({ html_url: "https://github.com/x/y/releases/tag/v1.2.3" }, {}, /html_url/);
  f({ extra: 1 }, {}, /campos inesperados/);
  assert.throws(() => stage({ ...FULL, mode: "preview", releaseMetadata: join(tmp, "no-existe.json"), out: newOut() }), /ilegible/);
});
t("fetch-release-metadata.buildMetadata (sin red): liga tag, estado, asset y hash", () => {
  const api = { tag_name: "v1.2.3", draft: false, prerelease: false, published_at: "2027-03-14T10:00:00Z", assets: [{ name: "arbitratge.apk", size: 1234, state: "uploaded", browser_download_url: GOOD.apkUrl }] };
  const args = { tag: "v1.2.3", assetName: "arbitratge.apk", sha256: GOOD.sha256, size: 1234 };
  const m = buildMetadata(api, args);
  stage({ ...FULL, mode: "preview", releaseMetadata: (() => { const p = join(tmp, "built.json"); writeFileSync(p, JSON.stringify(m)); return p; })(), out: newOut() });
  assert.throws(() => buildMetadata({ ...api, draft: true }, args), /borrador/);
  assert.throws(() => buildMetadata({ ...api, prerelease: true }, args), /prerelease/);
  assert.throws(() => buildMetadata({ ...api, tag_name: "v1.2.4" }, args), /tag/);
  assert.throws(() => buildMetadata(api, { ...args, assetName: "x.apk" }), /exactamente un asset/);
  assert.throws(() => buildMetadata(api, { ...args, size: 99 }), /tamaño/);
  assert.throws(() => buildMetadata({ ...api, assets: [{ ...api.assets[0], browser_download_url: "https://evil.example/arbitratge.apk" }] }, args), /URL del asset/);
  assert.throws(() => buildMetadata(api, { ...args, tag: "v1.2.3-rc.1" }), /tag/);
});
t("versión canónica y versionCode (05 §7.1): ceros, rc, límites de componentes y de Android", () => {
  const code = (x, y, z) => String(x * 100000 + y * 1000 + z * 10 + 9);
  const mk = (x, y, z) => ({ version: `${x}.${y}.${z}`, versionCode: code(x, y, z), apkUrl: `https://github.com/${REPO}/releases/download/v${x}.${y}.${z}/a.apk`, releaseNotesUrl: undefined, published: undefined, certSha256: undefined });
  for (const v of ["01.2.3", "1.02.3", "1.2.03", "1.2", "1.2.3.4", "v1.2.3", "1.2.3-rc.1", " 1.2.3", "1.2.3\n", "-1.2.3", "1.2.x"]) {
    assert.throws(() => stage({ ...GOOD, version: v, out: newOut() }), /versión final canónica/, v);
  }
  assert.throws(() => stage({ ...GOOD, ...mk(1, 100, 0), out: newOut() }), /≤ 99/);
  assert.throws(() => stage({ ...GOOD, ...mk(1, 0, 100), out: newOut() }), /≤ 99/);
  assert.throws(() => stage({ ...GOOD, ...mk(21000, 0, 0), out: newOut() }), /límite de Android/);
  for (const c of ["0102039", "+102039", "102039.0", "1e5", "0x18F37", " 102039"]) assert.throws(() => stage({ ...GOOD, versionCode: c, out: newOut() }), /entero canónico|fórmula/, c);
  for (const [x, y, z] of [[0, 0, 0], [0, 1, 0], [1, 99, 99], [20999, 99, 99]]) stage({ ...GOOD, ...mk(x, y, z), out: newOut() });
  assert.equal(code(20999, 99, 99), "2099999999");
});
t("claves: campos privados, forma, kid de prueba, huella, no listadas o inexistentes se rechazan", () => {
  const run = (name, entries, re, listed) => assert.throws(() => stage({ ...GOOD, web: withKeys(name, entries, listed), out: newOut() }), re);
  run("k1", [{ ...fakeKey("p1"), d: "AAAA" }], /campo privado/);
  run("k2", [{ ...fakeKey("p1"), privateKey: "x" }], /campo privado/);
  run("k3", [{ ...fakeKey("p1"), extra: 1 }], /campo no permitido/);
  run("k4", [fakeKey("test")], /kid de prueba/);
  run("k5", [fakeKey("testx")], /kid de prueba/);
  run("k6", [fakeKey("p1", { sha256_spki: "00".repeat(32) })], /huella/);
  run("k7", [fakeKey("p1", { alg: "RS256" })], /alg/);
  run("k8", [fakeKey("p1", { spki: "AAAA", sha256_spki: createHash("sha256").update(Buffer.from("AAAA", "base64url")).digest("hex") })], /spki no es una clave pública/);
  run("k9", [fakeKey("p1")], /falta keys.p3/, ["p3.json"]);
  run("k10", [], /falta/, ["p4.json"]);
  run("k11", [fakeKey("p1")], /repite/, ["p1.json", "p1.json"]);
  run("k12", [fakeKey("p5")], /fuera de la lista blanca\): keys\/p5\.json/, []); // fichero de clave presente pero no listado
});
t("stamp falla cerrado: marcador sobrante, bloque opcional ausente, verificador ausente, manifiesto viejo, brand ausente", () => {
  const mk = (name, mutate) => { const w = copyWeb(name); mutate(w); return () => stage({ ...GOOD, web: w, out: newOut() }); };
  assert.throws(mk("ph", (w) => writeFileSync(join(w, "index.html"), read("index.html").replace("<title>", "<title>__OTRO__ "))), /marcador sin sustituir: __OTRO__/);
  assert.throws(mk("blk", (w) => writeFileSync(join(w, "index.html"), read("index.html").replace("<!--NOTES-->", ""))), /sin bloque NOTES/);
  assert.throws(mk("nov", (w) => rmSync(join(w, "verificador", "sw.js"))), /sw\.js/);
  assert.throws(mk("old", (w) => writeFileSync(join(w, "verificador/styles.css"), read("verificador/styles.css") + "\n/* x */\n")), /desactualizado/);
  assert.throws(mk("brand", (w) => rmSync(join(w, "brand/favicon.svg"))), /falta brand\/favicon\.svg/);
});
t("lista blanca: ficheros extra no se copian; enlaces simbólicos se rechazan", () => {
  const w = copyWeb("extra");
  writeFileSync(join(w, "brand/secret.pem"), "x"); writeFileSync(join(w, "fonts/extra.ttf"), "x"); writeFileSync(join(w, "notes.md"), "x");
  const out = newOut(); stage({ ...GOOD, web: w, out });
  assert.ok(!files(out).some((f) => /secret|extra\.ttf|notes\.md/.test(f)));
  const w2 = copyWeb("link");
  let linked = true;
  try { rmSync(join(w2, "brand/favicon.svg")); symlinkSync(join(w2, "index.html"), join(w2, "brand/favicon.svg")); } catch {
    try { cpSync(join(w2, "brand"), join(tmp, "brand-real"), { recursive: true }); rmSync(join(w2, "brand"), { recursive: true }); symlinkSync(join(tmp, "brand-real"), join(w2, "brand"), "junction"); } catch { linked = false; }
  }
  if (linked) assert.throws(() => stage({ ...GOOD, web: w2, out: newOut() }), /enlace simbólico/);
  else console.log("  (symlinks no disponibles en este sistema: prueba omitida)");
});
t("claves: SPKI debe ser base64url canónico (bits finales distintos de cero se rechazan antes de validar P-256)", () => {
  const k = fakeKey("p1");
  assert.equal(Buffer.from(k.spki, "base64url").toString("base64url"), k.spki);
  const A = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
  const last = k.spki.at(-1), i = A.indexOf(last);
  const alt = A[i ^ 1]; // mismos bytes decodificados, otros bits finales
  assert.equal(Buffer.from(k.spki.slice(0, -1) + alt, "base64url").toString("hex"), Buffer.from(k.spki, "base64url").toString("hex"), "mismo contenido");
  assert.throws(() => stage({ ...GOOD, web: withKeys("canon", [{ ...k, spki: k.spki.slice(0, -1) + alt }]), out: newOut() }), /canónico/);
  assert.throws(() => stage({ ...GOOD, web: withKeys("canon2", [{ ...k, spki: k.spki + "=" }]), out: newOut() }), /spki/);
});
t("lista blanca del verificador: un fichero extra (p. ej. questions.json) se rechaza, con o sin manifiesto regenerado", () => {
  for (const regen of [true, false]) {
    const w = copyWeb("q" + regen);
    writeFileSync(join(w, "verificador/questions.json"), "[]");
    if (regen) writeFileSync(join(w, "verificador/precache-manifest.json"), JSON.stringify(buildManifest(join(w, "verificador")), null, 2) + "\n");
    assert.throws(() => stage({ ...GOOD, web: w, out: newOut() }), regen ? /fuera de la lista blanca\): questions\.json/ : /desactualizado|lista blanca/);
  }
  const w2 = copyWeb("lib-extra");
  writeFileSync(join(w2, "verificador/lib/extra.js"), "export {}");
  writeFileSync(join(w2, "verificador/precache-manifest.json"), JSON.stringify(buildManifest(join(w2, "verificador")), null, 2) + "\n");
  assert.throws(() => stage({ ...GOOD, web: w2, out: newOut() }), /fuera de la lista blanca\): lib\/extra\.js/);
  // fichero extra fuera del manifiesto: nunca se copia
  const w3 = copyWeb("noncopy");
  writeFileSync(join(w3, "verificador/questions.json"), "[]");
  const real = buildManifest;
  const out = newOut();
  try { stage({ ...GOOD, web: w3, out }); } catch { /* desactualizado: también es rechazo */ }
  assert.ok(!existsSync(join(out, "verificador/questions.json")));
});
t("--out: solo <repo>/_site o directorio NUEVO bajo tmp; nada ajeno, nada con enlaces/uniones", () => {
  const repoRoot = resolve(web, "..");
  const base = join(tmp, "own"); mkdirSync(base);
  const docs = join(base, "docs"); mkdirSync(docs); writeFileSync(join(docs, "keep.txt"), "no borrar");
  assert.throws(() => stage({ ...GOOD, out: docs }), /directorio temporal|ya existe/);
  assert.equal(readFileSync(join(docs, "keep.txt"), "utf8"), "no borrar");
  // marcado pero existente fuera del repo: rechazado igualmente
  const marked = join(base, "marked"); mkdirSync(marked); writeFileSync(join(marked, MARKER), "x"); writeFileSync(join(marked, "keep.txt"), "x");
  assert.throws(() => stage({ ...GOOD, out: marked }), /ya existe/);
  assert.ok(existsSync(join(marked, "keep.txt")));
  // el repo: solo _site; docs/ marcado o no, jamás
  const docsRepo = join(repoRoot, "docs");
  const before = readdirSync(docsRepo).length;
  assert.throws(() => stage({ ...GOOD, out: docsRepo }), /solo puede ser `_site`/);
  assert.throws(() => stage({ ...GOOD, out: join(repoRoot, "_SITE") }), /solo puede ser `_site`/);
  assert.throws(() => stage({ ...GOOD, out: join(repoRoot, "otra") }), /solo puede ser `_site`|directorio temporal/);
  assert.equal(readdirSync(docsRepo).length, before);
  assert.throws(() => stage({ ...GOOD, out: web }), /solapar/);
  assert.throws(() => stage({ ...GOOD, out: join(web, "_x") }), /solapar/);
  assert.throws(() => stage({ ...GOOD, out: repoRoot }), /solapar/);
  assert.throws(() => stage({ ...GOOD, out: dirname(repoRoot) }), /solapar|temporal/);
  assert.throws(() => stage({ ...GOOD, out: join(tmp, "no-padre", "x", "_site") }), /ausente|temporal/);
  assert.throws(() => stage({ ...GOOD, out: join(dirname(tmpdir()), "_site") }), /temporal/); // fuera de tmp
  // unión/enlace en la ruta (portable: junction en Windows, symlink dir en POSIX)
  const real = join(base, "real"); mkdirSync(real);
  const link = join(base, "link");
  let linked = true;
  try { symlinkSync(real, link, "junction"); } catch { linked = false; }
  if (linked) {
    assert.throws(() => stage({ ...GOOD, out: join(link, "_site") }), /enlace o unión|alias/);
    assert.ok(!existsSync(join(real, "_site")), "no escribe a través de la unión");
  } else console.log("  (enlaces no disponibles: prueba de unión omitida)");
  // nuevo directorio normal bajo tmp: permitido y marcado
  const ok = join(base, "nuevo-ok", "_site"); mkdirSync(dirname(ok));
  stage({ ...GOOD, out: ok });
  assert.ok(existsSync(join(ok, MARKER)));
  assert.throws(() => stage({ ...GOOD, out: ok }), /ya existe/); // no se sobrescribe fuera del repo
});
t("--out <repo>/_site: unión o directorio ajeno no vacío rechazados; con marca se regenera", () => {
  const repoRoot = resolve(web, "..");
  const site = join(repoRoot, "_site");
  if (existsSync(site) || lstatSync(site, { throwIfNoEntry: false })) { console.log("  (_site ya existe en el repo: prueba omitida)"); return; }
  const target = join(tmp, "unionTarget"); mkdirSync(target); writeFileSync(join(target, "keep.txt"), "x");
  let linked = true;
  try { symlinkSync(target, site, "junction"); } catch { linked = false; }
  try {
    if (linked) { assert.throws(() => stage({ ...GOOD, out: site }), /enlace\/unión|directorio normal/); assert.ok(existsSync(join(target, "keep.txt"))); }
  } finally { if (linked) rmdirSync(site); }
  mkdirSync(site); writeFileSync(join(site, "ajeno.txt"), "x");
  try {
    assert.throws(() => stage({ ...GOOD, out: site }), /no lleva la marca/);
    assert.ok(existsSync(join(site, "ajeno.txt")));
  } finally { rmSync(site, { recursive: true, force: true }); }
  try {
    stage({ ...GOOD, out: site }); writeFileSync(join(site, "stale.txt"), "x");
    stage({ ...GOOD, out: site });
    assert.ok(!existsSync(join(site, "stale.txt")) && existsSync(join(site, MARKER)));
  } finally { rmSync(site, { recursive: true, force: true }); }
});
t("CLI: un solo modo, argumentos desconocidos rechazados, --out ajeno rechazado, README documenta los argumentos", () => {
  const run = (args) => spawnSync(process.execPath, [join(web, "..", "scripts", "stamp-web.mjs"), ...args], { encoding: "utf8" });
  assert.equal(run(["--preview", "--production"]).status, 2);
  assert.equal(run(["--preview", "--nope", "x"]).status, 2);
  assert.notEqual(run(["--preview", "--out", join(tmp, "cli-ajeno")]).status, 0);
  assert.equal(run(["--version", "1.2.3"]).status, 1);
  const readme = read("test/README.md");
  for (const flag of ["--preview", "--production", "--version-code", "--bank-version", "--release-metadata", "--cert-sha256", "--published", "--release-notes-url", "fetch-release-metadata"]) assert.ok(readme.includes(flag), flag);
});

console.log(`site-static: ${n} comprobaciones correctas${process.exitCode ? " (CON FALLOS)" : ""}`);
