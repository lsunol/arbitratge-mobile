#!/usr/bin/env node
// Prepara el artefacto de GitHub Pages (_site/) a partir de web/ con los datos de una release.
// Falla cerrado: sin datos válidos de una release final no se genera nada.
//
// Uso (exactamente un modo: --preview o --production):
//   node scripts/stamp-web.mjs --preview|--production \
//     --version 1.2.3 --version-code 102039 \
//     --apk-url https://github.com/lsunol/arbitratge-mobile/releases/download/v1.2.3/<fichero>.apk \
//     --sha256 <64 hex> [--cert-sha256 <64 hex>] [--published YYYY-MM-DD] [--bank-version <id>] \
//     [--release-notes-url https://github.com/lsunol/arbitratge-mobile/releases/tag/v1.2.3] \
//     [--release-metadata release.json] (--production exige keys/*.json idénticas byte a byte a trusted-keys/*.json) [--download-page https://<CNAME>/] [--out _site] [--web web]
//
// - --production exige además --cert-sha256, --published, --bank-version, --release-notes-url,
//   --release-metadata (generado por scripts/fetch-release-metadata.mjs: release no borrador ni
//   prerelease, tag, nombre/URL del asset y SHA-256 descargado coinciden con los argumentos) y una
//   lista de claves de producción NO vacía. --preview admite que falten; lo que falte no se publica.
// - El APK solo puede ser el de la release final de ESTE repositorio (origin), con tag v<version>.
// - latest.json sigue docs/design/05 §6.5 (sin URL del APK; `download_page` fijo = https://<CNAME>/).
// - Lista blanca explícita (nombres fijos; no se copian directorios ni rutas del manifiesto), sin enlaces
//   simbólicos. Salida: solo `<repo>/_site` (vacío o con la marca .arbitratge-web-stamp) o un directorio
//   NUEVO bajo os.tmpdir() sin enlaces/uniones en su ruta.
// Sin dependencias; no escribe secretos.
import { createHash, createPublicKey } from "node:crypto";
import { cpSync, existsSync, lstatSync, mkdirSync, readFileSync, readdirSync, realpathSync, rmSync, statSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { basename, dirname, join, relative, resolve, sep } from "node:path";
import { fileURLToPath } from "node:url";
import { buildManifest } from "../web/verificador/tools/gen-manifest.mjs";

const repo = resolve(dirname(fileURLToPath(import.meta.url)), "..");
export const RELEASE_REPO = "lsunol/arbitratge-mobile"; // = origin; cambiar solo si el repositorio se mueve
export const MARKER = ".arbitratge-web-stamp";
export const MAX_VERSION_CODE = 2100000000; // límite de Android (05 §7.1)
const PLACEHOLDER = /__[A-Z0-9]+(?:_[A-Z0-9]+)*__|\{\{[^}]*\}\}/;
const KID_FILE = /^[a-z0-9]{1,16}\.json$/;
const SEMVER_FINAL = /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$/;
const BANK_VERSION = /^[0-9A-Za-z._+-]{1,24}$/;

// Listas blancas exactas (relativas a web/ y a web/verificador/).
export const LANDING_FILES = ["index.html", "styles.css", "CNAME", ".nojekyll"];
export const BRAND_FILES = ["brand/arbitratge-icona.svg", "brand/arbitratge-marca-negatiu-subtitol.svg", "brand/favicon.svg", "brand/favicon-32.png"];
export const FONT_FILES = ["fonts/atkinson-next.woff2", "fonts/atkinson-mono.woff2", "fonts/OFL-Atkinson-Next.txt", "fonts/OFL-Atkinson-Mono.txt"];
export const VERIFIER_FILES = [
  "index.html", "app.js", "styles.css", "manifest.webmanifest", "sw.js", "precache-manifest.json",
  "icons/apple-touch-icon.png", "icons/icon-192.png", "icons/icon-512.png", "icons/icon-maskable-512.png", "icons/icon.svg",
  "lib/arb1.js", "lib/base64url.js", "lib/camera-session.js", "lib/json-restricted.js", "lib/keys.js", "lib/offline.js", "lib/qr.js", "lib/text.js", "lib/verifier-state.js",
  "vendor/jsQR.js", "vendor/LICENSE-jsQR.txt", "keys/index.json",
]; // + keys/<kid>.json validadas y listadas en keys/index.json
const KEY_FIELDS = new Set(["kid", "alg", "spki", "sha256_spki", "status", "valid_from", "valid_until"]);
const SECRET_FIELD = /^(d|p|q|dp|dq|qi|priv|private|privatekey|private_key|secret|seed|pkcs8|pem|keystore|password|token)$/i;

const bad = (m) => { throw new Error("stamp-web: " + m); };
const isHex64 = (s) => typeof s === "string" && /^[0-9a-fA-F]{64}$/.test(s);

export function validate(o) {
  const { mode, version, versionCode, apkUrl, sha256, certSha256, published, releaseNotesUrl, bankVersion } = o;
  if (mode !== "preview" && mode !== "production") bad("indica exactamente un modo: --preview o --production");
  const v = SEMVER_FINAL.exec(version ?? "");
  if (!v) bad("--version debe ser una versión final canónica X.Y.Z (sin ceros a la izquierda, -rc, prerelease ni +build)");
  const [X, Y, Z] = [+v[1], +v[2], +v[3]];
  if (Y > 99 || Z > 99) bad("05 §7.1: MINOR y PATCH deben ser ≤ 99");
  const expected = X * 100000 + Y * 1000 + Z * 10 + 9; // versión final: s = 9
  if (expected > MAX_VERSION_CODE) bad(`versionCode ${expected} supera el límite de Android (${MAX_VERSION_CODE})`);
  if (!/^[1-9]\d*$/.test(String(versionCode ?? ""))) bad("--version-code ausente o no es un entero canónico");
  if (+versionCode !== expected) bad(`--version-code ${versionCode} no coincide con la fórmula de 05 §7.1 para ${version} (${expected})`);
  const tag = "v" + version;
  const prefix = `https://github.com/${RELEASE_REPO}/releases/download/${tag}/`;
  if (typeof apkUrl !== "string" || !apkUrl.startsWith(prefix) || !/^[\w.-]+\.apk$/.test(apkUrl.slice(prefix.length))) {
    bad(`--apk-url debe ser exactamente ${prefix}<fichero>.apk (repositorio ${RELEASE_REPO}, tag ${tag}; sin latest)`);
  }
  if (!isHex64(sha256)) bad("--sha256 ausente o no son 64 caracteres hex");
  if (certSha256 !== undefined && !isHex64(certSha256)) bad("--cert-sha256 no son 64 caracteres hex");
  if (published !== undefined) {
    const d = new Date(published + "T00:00:00Z");
    if (!/^\d{4}-\d{2}-\d{2}$/.test(published) || Number.isNaN(+d) || d.toISOString().slice(0, 10) !== published) bad("--published debe ser una fecha real YYYY-MM-DD");
  }
  if (bankVersion !== undefined && !BANK_VERSION.test(bankVersion)) bad("--bank-version debe cumplir [0-9A-Za-z._+-]{1,24}");
  if (releaseNotesUrl !== undefined && releaseNotesUrl !== `https://github.com/${RELEASE_REPO}/releases/tag/${tag}`) {
    bad(`--release-notes-url debe ser https://github.com/${RELEASE_REPO}/releases/tag/${tag}`);
  }
  if (mode === "production") {
    if (certSha256 === undefined) bad("--production exige --cert-sha256");
    if (published === undefined) bad("--production exige --published");
    if (releaseNotesUrl === undefined) bad("--production exige --release-notes-url");
    if (bankVersion === undefined) bad("--production exige --bank-version");
    if (o.releaseMetadata === undefined) bad("--production exige --release-metadata (scripts/fetch-release-metadata.mjs)");
  }
}

// Une los argumentos con los metadatos de la release publicada (asset, tag, estado y SHA-256 descargado).
export function verifyReleaseMetadata(meta, { version, apkUrl, sha256, published, releaseNotesUrl }) {
  const keys = (o, ks, w) => {
    if (!o || typeof o !== "object" || Array.isArray(o) || Object.keys(o).sort().join() !== [...ks].sort().join()) bad(`release-metadata: ${w} con campos inesperados`);
  };
  keys(meta, ["schema", "repo", "tag", "draft", "prerelease", "html_url", "published_at", "asset"], "raíz");
  keys(meta.asset, ["name", "browser_download_url", "size", "sha256"], "asset");
  if (meta.schema !== 1) bad("release-metadata: schema");
  if (meta.repo !== RELEASE_REPO) bad("release-metadata: repositorio distinto");
  if (meta.tag !== "v" + version) bad("release-metadata: tag distinto de v" + version);
  if (meta.draft !== false || meta.prerelease !== false) bad("release-metadata: la release es borrador o prerelease");
  if (meta.html_url !== `https://github.com/${RELEASE_REPO}/releases/tag/v${version}`) bad("release-metadata: html_url inesperado");
  if (meta.asset.browser_download_url !== apkUrl || meta.asset.name !== apkUrl.slice(apkUrl.lastIndexOf("/") + 1)) bad("release-metadata: el asset no coincide con --apk-url");
  if (!/^[0-9a-f]{64}$/.test(meta.asset.sha256) || meta.asset.sha256 !== sha256.toLowerCase()) bad("release-metadata: el SHA-256 del asset descargado no coincide con --sha256");
  if (!Number.isInteger(meta.asset.size) || meta.asset.size <= 0) bad("release-metadata: size");
  if (typeof meta.published_at !== "string" || !/^\d{4}-\d{2}-\d{2}T/.test(meta.published_at)) bad("release-metadata: published_at");
  if (published !== undefined && meta.published_at.slice(0, 10) !== published) bad("--published no coincide con la fecha de publicación de la release");
  if (releaseNotesUrl !== undefined && releaseNotesUrl !== meta.html_url) bad("--release-notes-url no coincide con la release");
}

const noLink = (p, what) => {
  if (!existsSync(p)) bad(`falta ${what}`);
  if (lstatSync(p).isSymbolicLink()) bad(`enlace simbólico no permitido: ${what}`);
};
// Comprueba que ningún componente de la ruta (desde la raíz) es un enlace simbólico/unión.
function safeFile(root, rel) {
  if (rel.split("/").some((x) => x === ".." || x === "." || x === "")) bad(`ruta no permitida: ${rel}`);
  let p = root;
  for (const part of rel.split("/")) {
    p = join(p, part);
    noLink(p, rel);
  }
  if (!statSync(p).isFile()) bad(`no es un fichero: ${rel}`);
  return p;
}

function checkKeyEntry(entry, name) {
  if (entry === null || typeof entry !== "object" || Array.isArray(entry)) bad(`clave ${name}: no es un objeto`);
  for (const k of Object.keys(entry)) {
    if (SECRET_FIELD.test(k)) bad(`clave ${name}: campo privado/secreto «${k}» prohibido`);
    if (!KEY_FIELDS.has(k)) bad(`clave ${name}: campo no permitido «${k}»`);
  }
  if (entry.kid !== name.slice(0, -5) || !/^[a-z0-9]{1,16}$/.test(entry.kid)) bad(`clave ${name}: kid debe coincidir con el nombre del fichero`);
  if (/^(test|testx|dev)/.test(entry.kid)) bad(`clave ${name}: kid de prueba no permitido`);
  if (entry.alg !== "ES256") bad(`clave ${name}: alg`);
  if (!["active", "retired", "compromised"].includes(entry.status)) bad(`clave ${name}: status`);
  if (!/^\d{4}-\d{2}-\d{2}$/.test(entry.valid_from ?? "")) bad(`clave ${name}: valid_from`);
  if (entry.valid_until !== undefined && !/^\d{4}-\d{2}-\d{2}$/.test(entry.valid_until)) bad(`clave ${name}: valid_until`);
  if (typeof entry.spki !== "string" || !/^[A-Za-z0-9_-]+$/.test(entry.spki)) bad(`clave ${name}: spki`);
  const der = Buffer.from(entry.spki, "base64url");
  if (der.length === 0 || der.toString("base64url") !== entry.spki) bad(`clave ${name}: spki no es base64url canónico (bits finales o relleno)`);
  if (!/^[0-9a-f]{64}$/.test(entry.sha256_spki ?? "")) bad(`clave ${name}: sha256_spki`);
  if (createHash("sha256").update(der).digest("hex") !== entry.sha256_spki) bad(`clave ${name}: la huella no coincide con spki`);
  let pub;
  try { pub = createPublicKey({ key: der, format: "der", type: "spki" }); } catch { bad(`clave ${name}: spki no es una clave pública válida`); }
  if (pub.asymmetricKeyType !== "ec" || pub.asymmetricKeyDetails?.namedCurve !== "prime256v1") bad(`clave ${name}: no es EC P-256`);
}

const fold = (p) => (process.platform === "win32" || process.platform === "darwin" ? p.toLowerCase() : p);
// Salida permitida: exactamente <repo>/_site (vacío o con marca) o un directorio NUEVO bajo os.tmpdir().
function checkOut(out, web) {
  if (out === web || web.startsWith(out + sep) || out.startsWith(web + sep) || fold(out) === fold(repo) || fold(repo).startsWith(fold(out) + sep)) bad("--out no puede solaparse con --web ni con el repositorio");
  const parent = dirname(out);
  if (fold(parent) === fold(repo)) {
    if (basename(out) !== "_site") bad("en el repositorio la salida solo puede ser `_site`");
    const st = lstatSync(out, { throwIfNoEntry: false });
    if (st) {
      if (st.isSymbolicLink() || !st.isDirectory()) bad("--out existe y no es un directorio normal (enlace/unión/fichero)");
      if (fold(realpathSync.native(out)) !== fold(join(realpathSync.native(repo), "_site"))) bad("--out: alias o unión no permitidos");
      if (!existsSync(join(out, MARKER)) && readdirSync(out).length > 0) bad(`--out existe, no está vacío y no lleva la marca ${MARKER}: no se toca`);
    }
    return;
  }
  const lexRoot = resolve(tmpdir());
  if (!fold(out).startsWith(fold(lexRoot) + sep)) bad("--out debe ser `<repo>/_site` o un directorio nuevo bajo el directorio temporal del sistema");
  if (lstatSync(out, { throwIfNoEntry: false })) bad("--out ya existe: fuera del repositorio solo se admiten directorios nuevos");
  const parts = relative(lexRoot, parent).split(sep).filter(Boolean);
  let cur = lexRoot;
  for (const part of parts) {
    cur = join(cur, part);
    const st = lstatSync(cur, { throwIfNoEntry: false });
    if (!st || st.isSymbolicLink() || !st.isDirectory()) bad(`--out: componente de la ruta ausente, enlace o unión: ${part}`);
  }
  if (fold(realpathSync.native(parent)) !== fold(join(realpathSync.native(lexRoot), ...parts))) bad("--out: alias o unión en la ruta");
}

export function stage(o) {
  const web = resolve(o.web ?? join(repo, "web"));
  const out = resolve(o.out ?? join(repo, "_site"));
  validate(o);
  const { mode, version, versionCode, apkUrl, sha256, certSha256, published, releaseNotesUrl, bankVersion } = o;
  checkOut(out, web); // antes de tocar nada
  if (o.releaseMetadata !== undefined) {
    let meta;
    try { meta = JSON.parse(readFileSync(o.releaseMetadata, "utf8")); } catch { bad("--release-metadata ilegible o no es JSON"); }
    verifyReleaseMetadata(meta, { version, apkUrl, sha256, published, releaseNotesUrl });
  }

  const host = readFileSync(safeFile(web, "CNAME"), "utf8").trim();
  if (!/^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+$/.test(host)) bad("web/CNAME no contiene un único dominio válido");
  const downloadPage = `https://${host}/`;
  if (o.downloadPage !== undefined && o.downloadPage !== downloadPage) bad(`--download-page debe ser ${downloadPage}`);

  const vroot = join(web, "verificador");
  noLink(vroot, "verificador/");
  for (const f of VERIFIER_FILES) safeFile(vroot, f);
  // Claves de producción
  const idx = JSON.parse(readFileSync(join(vroot, "keys/index.json"), "utf8"));
  if (!idx || typeof idx !== "object" || !Array.isArray(idx.files) || Object.keys(idx).some((k) => k !== "files")) bad("keys/index.json debe ser {\"files\": [...]}");
  if (new Set(idx.files).size !== idx.files.length) bad("keys/index.json repite ficheros");
  const keyEntries = [];
  for (const f of idx.files) {
    if (typeof f !== "string" || !KID_FILE.test(f)) bad(`clave no permitida en producción: ${f}`);
    const e = JSON.parse(readFileSync(safeFile(vroot, "keys/" + f), "utf8"));
    checkKeyEntry(e, f);
    keyEntries.push(e);
  }
  if (mode === "production" && !idx.files.length) bad("--production: la lista de claves de producción está vacía");
  if (mode === "production" && !keyEntries.some((e) => e.status === "active")) bad("--production: ninguna clave activa");
  if (mode === "production") {
    // Los bytes públicos desplegados deben ser EXACTAMENTE los aprobados en trusted-keys/ (mismo conjunto, mismos bytes).
    const td = resolve(o.trustedKeysDir ?? join(repo, "trusted-keys"));
    noLink(td, "trusted-keys/");
    if (!statSync(td).isDirectory()) bad("trusted-keys/ no es un directorio");
    const approved = readdirSync(td).filter((f) => KID_FILE.test(f)).sort();
    if (!approved.length) bad("--production: trusted-keys/ no contiene claves aprobadas");
    const deployed = [...idx.files].sort();
    if (approved.join() !== deployed.join()) {
      const miss = approved.filter((f) => !deployed.includes(f)), extra = deployed.filter((f) => !approved.includes(f));
      bad(`--production: keys/index.json no coincide con trusted-keys/ (faltan: ${miss.join(",") || "-"}; sobran: ${extra.join(",") || "-"})`);
    }
    for (const f of deployed) {
      if (!readFileSync(safeFile(vroot, "keys/" + f)).equals(readFileSync(safeFile(td, f)))) bad(`--production: keys/${f} difiere byte a byte de trusted-keys/${f}`);
    }
  }
  const vfiles = [...VERIFIER_FILES, ...idx.files.map((f) => "keys/" + f)];
  // El precache declarado debe ser exactamente la lista blanca (menos los ficheros que no precachea) y estar al día.
  const declared = JSON.parse(readFileSync(join(vroot, "precache-manifest.json"), "utf8"));
  const allowed = new Set(vfiles);
  for (const f of declared.files) if (!allowed.has(f.path)) bad(`fichero del verificador no permitido (fuera de la lista blanca): ${f.path}`);
  if (JSON.stringify(declared) !== JSON.stringify(buildManifest(vroot))) bad("verificador/precache-manifest.json desactualizado (ejecuta web/verificador/tools/gen-manifest.mjs)");
  const landing = [...LANDING_FILES, ...BRAND_FILES, ...FONT_FILES];
  for (const f of ["latest.json", ...landing]) safeFile(web, f);

  // Escritura
  if (existsSync(out)) rmSync(out, { recursive: true, force: true }); // solo <repo>/_site con marca o vacío (checkOut)
  mkdirSync(out, { recursive: true });
  writeFileSync(join(out, MARKER), "generado por scripts/stamp-web.mjs; se puede borrar y regenerar\n");
  const put = (root, rel, dest = rel) => {
    mkdirSync(dirname(join(out, dest)), { recursive: true });
    cpSync(join(root, rel), join(out, dest));
  };
  for (const f of landing.filter((f) => f !== "index.html")) put(web, f);
  for (const f of vfiles) put(vroot, f, "verificador/" + f);

  const sub = (s) => s.replaceAll("__VERSION__", version).replaceAll("__APK_URL__", apkUrl).replaceAll("__SHA256__", sha256.toLowerCase()).replaceAll("__HOST_WBR__", host.replaceAll(".", ".<wbr>")).replaceAll("__HOST__", host);
  const optional = (html, name, value, subs) => {
    const re = new RegExp(`[ \\t]*<!--${name}-->([\\s\\S]*?)<!--/${name}-->\\n?`);
    if (!re.test(html)) bad(`index.html sin bloque ${name}`);
    if (value === undefined) return html.replace(re, "");
    let inner = re.exec(html)[1];
    for (const [k, v] of Object.entries(subs)) inner = inner.replaceAll(k, v);
    return html.replace(re, inner);
  };
  let html = readFileSync(join(web, "index.html"), "utf8");
  html = optional(html, "CERT", certSha256, { __CERT_SHA256__: certSha256?.toLowerCase() });
  html = optional(html, "PUBLISHED", published, { __PUBLISHED__: published });
  html = optional(html, "NOTES", releaseNotesUrl, { __RELEASE_NOTES_URL__: releaseNotesUrl });
  writeFileSync(join(out, "index.html"), sub(html));

  // latest.json (05 §6.5): sin URL del APK
  const tpl = JSON.parse(readFileSync(join(web, "latest.json"), "utf8"));
  const latest = { schema: tpl.schema, app: tpl.app, versionName: version, versionCode: +versionCode };
  if (bankVersion !== undefined) latest.bank_version = bankVersion;
  if (published !== undefined) latest.published = published;
  latest.apk_sha256 = sha256.toLowerCase();
  if (certSha256 !== undefined) latest.cert_sha256 = certSha256.toLowerCase();
  latest.download_page = downloadPage;
  if (releaseNotesUrl !== undefined) latest.release_notes_url = releaseNotesUrl;
  writeFileSync(join(out, "latest.json"), JSON.stringify(latest, null, 2) + "\n");

  // Comprobaciones finales
  const m = PLACEHOLDER.exec(readFileSync(join(out, "index.html"), "utf8"));
  if (m) bad(`index.html conserva un marcador sin sustituir: ${m[0]}`);
  if (/apk_url|\.apk/.test(readFileSync(join(out, "latest.json"), "utf8"))) bad("latest.json no puede contener la URL del APK");
  for (const f of ["sw.js", "manifest.webmanifest", "service-worker.js"]) if (existsSync(join(out, f))) bad(`la raíz no puede tener ${f} (scope del verificador)`);
  const expectedFiles = new Set([MARKER, "index.html", "latest.json", ...landing, ...vfiles.map((f) => "verificador/" + f)]);
  for (const p of readdirSync(out, { recursive: true })) {
    const rel = p.split(sep).join("/");
    const st = lstatSync(join(out, p));
    if (st.isSymbolicLink()) bad(`enlace simbólico en el artefacto: ${rel}`);
    if (st.isFile() && !expectedFiles.has(rel)) bad(`fichero inesperado en el artefacto: ${rel}`);
  }
  return { out, host, mode };
}

const VALUE_OPTS = {
  version: "version", "version-code": "versionCode", "apk-url": "apkUrl", sha256: "sha256", "cert-sha256": "certSha256", published: "published",
  "bank-version": "bankVersion", "release-notes-url": "releaseNotesUrl", "release-metadata": "releaseMetadata", "download-page": "downloadPage", out: "out", web: "web",
};
if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const a = process.argv.slice(2);
  const o = {};
  for (let i = 0; i < a.length; i++) {
    const k = a[i];
    if (k === "--preview" || k === "--production") { if (o.mode) { console.error("Indica un solo modo"); process.exit(2); } o.mode = k.slice(2); continue; }
    const key = k.startsWith("--") ? VALUE_OPTS[k.slice(2)] : undefined;
    if (!key || a[i + 1] === undefined) { console.error("Argumento no válido: " + k); process.exit(2); }
    o[key] = a[++i];
  }
  try {
    const r = stage(o);
    console.log(`Artefacto (${r.mode}) listo en ${r.out} (host ${r.host})`);
  } catch (e) {
    console.error(e.message);
    process.exit(1);
  }
}
