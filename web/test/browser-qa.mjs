// QA de la landing en un Chrome real por CDP (sin dependencias; Node >= 22). Dos modos:
//
//  1) EMULADOR / DISPOSITIVO ANDROID (válido como QA visual móvil):
//       node web/test/browser-qa.mjs --android [<serial>]
//     Requiere `adb` (env ADB o PATH) y un emulador/dispositivo con Chrome (com.android.chrome) ya pasado el primer
//     arranque. Sirve la build estampada en el host, hace `adb reverse` y `adb forward` al socket DevTools de Chrome y
//     fija la escala de fuente NATIVA de Android (`settings put system font_scale 1.0 / 1.3 / 2.0`, que Chrome aplica
//     al texto web) reiniciando Chrome en cada escala; restaura la escala original al terminar. Para los anchos 320 y
//     390 usa Emulation.setDeviceMetricsOverride con mobile:true sobre el Chrome del emulador.
//     Salida: `emu-*.png` + `emu-qa-metadata.json`.
//
//  2) EMULACIÓN MÓVIL EN ESCRITORIO (apoyo rápido, NO sustituye al emulador):
//       node web/test/browser-qa.mjs
//     Chrome headless de escritorio con Emulation.setDeviceMetricsOverride mobile:true (deviceScaleFactor 2.75), toque y
//     User-Agent de Android; el texto 130 %/200 % se simula con document.documentElement.style.fontSize (no es la
//     escala de fuente nativa de Android). Salida: `final-*.png` + `final-qa-metadata.json`.
//
// En ambos: estampa la build final (--preview) en un directorio nuevo bajo os.tmpdir(), la sirve con un servidor
// efímero, abre los <details> y comprueba scrollWidth <= innerWidth, innerWidth == ancho pedido (con mobile:true un
// desbordamiento ensancha el viewport y haría fallar esto), ningún elemento con borde derecho > innerWidth, texto >= 16 px,
// contraste y que el carnet de ejemplo no pise «Què és?». Casos: 320 y 390 × 100/130/200 % × claro/oscuro
// (+ 1280 de escritorio, mobile:false, solo en el modo 2 como referencia).
// NO verifica TalkBack ni un dispositivo físico.
// Opciones: --out <dir> (evidencia, por defecto docs/design/reviews/evidence/2026-11-21-landing) --chrome <ruta> (modo 2).
import { spawn, execFileSync } from "node:child_process";
import { createHash } from "node:crypto";
import { createServer } from "node:http";
import { existsSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, rmSync, statSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, extname, join, resolve, sep } from "node:path";
import { fileURLToPath } from "node:url";
import { stage } from "../../scripts/stamp-web.mjs";

const here = dirname(fileURLToPath(import.meta.url));
const repo = resolve(here, "..", "..");
const argv = process.argv.slice(2);
const arg = (k) => { const i = argv.indexOf("--" + k); return i >= 0 ? argv[i + 1] : undefined; };
const ANDROID = argv.includes("--android");
const SERIAL = ANDROID && argv[argv.indexOf("--android") + 1] && !argv[argv.indexOf("--android") + 1].startsWith("--") ? argv[argv.indexOf("--android") + 1] : undefined;
const outDir = resolve(arg("out") ?? join(repo, "docs/design/reviews/evidence/2026-11-21-landing"));
const PREFIX = ANDROID ? "emu" : "final";
const ADB = process.env.ADB ?? "adb";
const adb = (...a) => execFileSync(ADB, [...(SERIAL ? ["-s", SERIAL] : []), ...a], { encoding: "utf8" }).trim();
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

// 1. Build final (preview) en un directorio nuevo bajo tmp
const work = mkdtempSync(join(tmpdir(), "arb-qa-"));
const REPO = "lsunol/arbitratge-mobile";
const BUILD_ARGS = {
  mode: "preview", version: "1.2.3", versionCode: "102039", sha256: "ab".repeat(32), certSha256: "cd".repeat(32), published: "2027-03-14", bankVersion: "2027.01.0",
  apkUrl: `https://github.com/${REPO}/releases/download/v1.2.3/arbitratge.apk`, releaseNotesUrl: `https://github.com/${REPO}/releases/tag/v1.2.3`,
};
const site = join(work, "_site");
stage({ ...BUILD_ARGS, out: site });
const walk = (d) => readdirSync(d).flatMap((x) => (statSync(join(d, x)).isDirectory() ? walk(join(d, x)) : [join(d, x)]));
const h = createHash("sha256");
for (const f of walk(site).sort()) h.update(f.slice(site.length) + "\0").update(readFileSync(f));
const buildHash = h.digest("hex");
let gitHead = "desconocido";
try { gitHead = execFileSync("git", ["rev-parse", "HEAD"], { cwd: repo, encoding: "utf8" }).trim(); } catch { /* sin git */ }

// 2. Servidor estático efímero
const MIME = { ".html": "text/html; charset=utf-8", ".css": "text/css; charset=utf-8", ".js": "text/javascript", ".json": "application/json", ".svg": "image/svg+xml", ".png": "image/png", ".woff2": "font/woff2", ".txt": "text/plain", ".webmanifest": "application/manifest+json" };
const server = createServer((req, res) => {
  let p = decodeURIComponent(new URL(req.url, "http://x").pathname);
  if (p.endsWith("/")) p += "index.html";
  const f = resolve(site, "." + p);
  if (!f.startsWith(site + sep) || !existsSync(f) || !statSync(f).isFile()) { res.writeHead(404).end("no"); return; }
  res.writeHead(200, { "content-type": MIME[extname(f)] ?? "application/octet-stream" }).end(readFileSync(f));
});
await new Promise((r) => server.listen(0, "127.0.0.1", r));
const SPORT = server.address().port;
const URL_ = `http://127.0.0.1:${SPORT}/`;

// 3. Navegador + CDP
let chromeProc, devtoolsPort, originalFontScale, deviceInfo = {};
const cleanup = () => {
  try { chromeProc?.kill(); } catch {}
  if (ANDROID) {
    try { adb("shell", "settings", "put", "system", "font_scale", originalFontScale && originalFontScale !== "null" ? originalFontScale : "1.0"); } catch {}
    try { adb("reverse", "--remove", `tcp:${SPORT}`); } catch {}
    try { adb("forward", "--remove", `tcp:${devtoolsPort}`); } catch {}
  }
  server.close();
  setTimeout(() => rmSync(work, { recursive: true, force: true }), 500);
};
const CHROME = [arg("chrome"), process.env.CHROME, "C:/Program Files/Google/Chrome/Application/chrome.exe", "C:/Program Files (x86)/Google/Chrome/Application/chrome.exe", "/usr/bin/google-chrome", "/usr/bin/chromium", "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"].find((p) => p && existsSync(p));
const waitJson = async (url) => { for (let i = 0; i < 60; i++) { try { return await (await fetch(url)).json(); } catch { await sleep(250); } } return undefined; };

async function connect() {
  const ver = await waitJson(`http://127.0.0.1:${devtoolsPort}/json/version`);
  if (!ver) { cleanup(); console.error("No hay DevTools de Chrome"); process.exit(2); }
  let target;
  if (ANDROID) {
    const list = await waitJson(`http://127.0.0.1:${devtoolsPort}/json`);
    target = list?.find((t) => t.type === "page") ?? (await (await fetch(`http://127.0.0.1:${devtoolsPort}/json/new?about:blank`, { method: "PUT" })).json());
  } else target = await (await fetch(`http://127.0.0.1:${devtoolsPort}/json/new?about:blank`, { method: "PUT" })).json();
  const ws = new WebSocket(target.webSocketDebuggerUrl);
  await new Promise((r, j) => { ws.onopen = r; ws.onerror = j; });
  let id = 0; const pending = new Map(); const waiters = [];
  ws.onmessage = (m) => {
    const d = JSON.parse(m.data);
    if (d.id && pending.has(d.id)) { const { ok, ko } = pending.get(d.id); pending.delete(d.id); d.error ? ko(new Error(JSON.stringify(d.error))) : ok(d.result); }
    else if (d.method) for (const w of waiters.splice(0)) w(d);
  };
  const send = (method, params = {}) => new Promise((ok, ko) => { const i = ++id; pending.set(i, { ok, ko }); ws.send(JSON.stringify({ id: i, method, params })); });
  const once = (method) => new Promise((r) => { const f = (d) => (d.method === method ? r(d) : waiters.push(f)); waiters.push(f); });
  const evaluate = async (expression) => {
    const r = await send("Runtime.evaluate", { expression, returnByValue: true, awaitPromise: true });
    if (r.exceptionDetails) throw new Error(JSON.stringify(r.exceptionDetails.exception?.description ?? r.exceptionDetails));
    return r.result.value;
  };
  await send("Page.enable");
  return { ver, ws, send, once, evaluate };
}

if (ANDROID) {
  try { deviceInfo = { serial: SERIAL ?? adb("get-serialno"), model: adb("shell", "getprop", "ro.product.model"), android: adb("shell", "getprop", "ro.build.version.release"), sdk: adb("shell", "getprop", "ro.build.version.sdk"), emulator: adb("shell", "getprop", "ro.kernel.qemu") === "1" || /emulator|sdk_gphone/i.test(adb("shell", "getprop", "ro.product.model") + adb("shell", "getprop", "ro.product.name")), density: adb("shell", "wm", "density"), size: adb("shell", "wm", "size") }; }
  catch (e) { cleanup(); console.error("adb: no hay dispositivo/emulador accesible (" + String(e.message).split("\n")[0] + ")"); process.exit(2); }
  originalFontScale = adb("shell", "settings", "get", "system", "font_scale");
  devtoolsPort = 9222;
  adb("reverse", `tcp:${SPORT}`, `tcp:${SPORT}`);
} else {
  if (!CHROME) { cleanup(); console.error("No se encuentra Chrome/Chromium (usa --chrome o CHROME)"); process.exit(2); }
  devtoolsPort = 9300 + Math.floor(Math.random() * 500);
}

async function startBrowser(fontScale) {
  if (ANDROID) {
    adb("shell", "settings", "put", "system", "font_scale", String(fontScale));
    adb("shell", "am", "force-stop", "com.android.chrome");
    await sleep(800);
    adb("shell", "am", "start", "-a", "android.intent.action.VIEW", "-d", "about:blank", "com.android.chrome");
    await sleep(2500);
    adb("forward", `tcp:${devtoolsPort}`, "localabstract:chrome_devtools_remote");
  } else if (!chromeProc) {
    chromeProc = spawn(CHROME, [`--headless=new`, `--remote-debugging-port=${devtoolsPort}`, `--user-data-dir=${join(work, "profile")}`, "--no-first-run", "--disable-gpu", "--hide-scrollbars", "about:blank"], { stdio: "ignore" });
  }
  return connect();
}

const QA = readFileSync(join(here, "browser-qa.js"), "utf8").split("\n").filter((l) => !l.startsWith("//")).join(" ");
const UA = "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";
const DSF = 2.75;
const CASES = [];
for (const pct of [100, 130, 200]) for (const scheme of ["light", "dark"]) for (const w of [320, 390]) CASES.push({ w, pct, scheme, mobile: true });
if (!ANDROID) for (const scheme of ["light", "dark"]) for (const pct of [100, 200]) CASES.push({ w: 1280, pct, scheme, mobile: false });
mkdirSync(outDir, { recursive: true });
for (const f of readdirSync(outDir)) if (new RegExp(`^${PREFIX}-.*\\.(png|json)$`).test(f)) rmSync(join(outDir, f));
const results = [];
let failed = false, browserVersion, userAgent;
let session, sessionScale;
for (const c of CASES) {
  if (!session || (ANDROID && sessionScale !== c.pct)) {
    if (session) { try { session.ws.close(); } catch {} }
    session = await startBrowser(c.pct / 100); sessionScale = c.pct; browserVersion = session.ver.Browser;
  }
  const { send, once, evaluate } = session;
  await send("Emulation.setDeviceMetricsOverride", { width: c.w, height: c.mobile ? 800 : 800, deviceScaleFactor: c.mobile ? (ANDROID ? 0 : DSF) : 1, mobile: c.mobile });
  if (c.mobile && !ANDROID) { await send("Emulation.setTouchEmulationEnabled", { enabled: true, maxTouchPoints: 5 }); await send("Emulation.setUserAgentOverride", { userAgent: UA }); }
  if (!c.mobile) { await send("Emulation.setTouchEmulationEnabled", { enabled: false }); await send("Emulation.setUserAgentOverride", { userAgent: "" }); }
  await send("Emulation.setEmulatedMedia", { features: [{ name: "prefers-color-scheme", value: c.scheme }] });
  const loaded = once("Page.loadEventFired");
  await send("Page.navigate", { url: URL_ });
  await loaded;
  await evaluate("document.fonts.ready.then(() => true)");
  // En Chrome Android, font_scale nativa no siempre afecta al CSS rem de una página web fija.
  // Se combina con el CSS root explícito para demostrar el reflow 130/200 % también en el navegador del emulador.
  const rootPx = await evaluate(`(() => { document.documentElement.style.fontSize = '${c.pct}%'; document.querySelectorAll('details').forEach(d => d.open = true); return parseFloat(getComputedStyle(document.documentElement).fontSize); })()`);
  await sleep(200);
  userAgent = await evaluate("navigator.userAgent");
  const q = JSON.parse(await evaluate(QA));
  const gap = await evaluate("(() => { const c = document.querySelector('.card-fig .card').getBoundingClientRect(), h = document.getElementById('h-app').getBoundingClientRect(); return Math.round(h.top - c.bottom); })()");
  const ok = q.sw <= q.w && q.w === c.w && q.over.length === 0 && q.small.length === 0 && q.contrast.length === 0 && gap > 0;
  const m = await send("Page.getLayoutMetrics");
  const shot = await send("Page.captureScreenshot", { format: "png", captureBeyondViewport: true, clip: { x: 0, y: 0, width: c.w, height: Math.ceil(m.cssContentSize.height), scale: 1 } });
  const file = `${PREFIX}-${c.w}-${c.scheme}-${c.pct}.png`;
  writeFileSync(join(outDir, file), Buffer.from(shot.data, "base64"));
  results.push({ ...c, innerWidth: q.w, scrollWidth: q.sw, rootFontPx: rootPx, pageHeight: Math.ceil(m.cssContentSize.height), overflowRight: q.over, textUnder16px: q.small, contrastFailures: q.contrast, cardToHeadingGapPx: gap, pass: ok, screenshot: file });
  if (!ok) failed = true;
  console.log(`${ok ? "PASS" : "FALLA"} ${c.mobile ? "móvil" : "escritorio"} ${c.w}px ${c.pct}% ${c.scheme}: scrollWidth ${q.sw} <= innerWidth ${q.w}, raíz ${rootPx}px, hueco carnet→«Què és?» ${gap}px`);
}
const meta = {
  tool: "web/test/browser-qa.mjs", command: ANDROID ? `node web/test/browser-qa.mjs --android${SERIAL ? " " + SERIAL : ""}` : "node web/test/browser-qa.mjs",
  kind: ANDROID ? "android-emulator-or-device-chrome" : "desktop-chrome-mobile-emulation (NO es un emulador Android)",
  androidEmulatorUsed: ANDROID, device: ANDROID ? deviceInfo : undefined,
  url: URL_ + (ANDROID ? " (vía adb reverse; servidor efímero en el host)" : " (servidor efímero 127.0.0.1)"), browser: browserVersion, userAgent,
  buildHash: { algorithm: "sha256 de (ruta + 0x00 + contenido) de todos los ficheros del artefacto estampado, ordenados", sha256: buildHash, gitHead, uncommittedChanges: true },
  stampArgs: { ...BUILD_ARGS, sha256: "ab×32 (ficticio)", certSha256: "cd×32 (ficticio)" },
  settings: {
    viewport: ANDROID ? "Emulation.setDeviceMetricsOverride mobile:true (deviceScaleFactor del dispositivo) sobre el Chrome del emulador" : `Emulation.setDeviceMetricsOverride mobile:true, deviceScaleFactor ${DSF}, toque y User-Agent Android (casos móviles); 1280 con mobile:false solo como referencia de escritorio`,
    textScaling: ANDROID ? "Android settings font_scale 1.0/1.3/2.0 + root CSS font-size 100/130/200 % (se restaura font_scale al terminar)" : "document.documentElement.style.fontSize = 100/130/200 % (simulación; no es font_scale nativa de Android)",
    colorScheme: "Emulation.setEmulatedMedia prefers-color-scheme", detailsOpen: true,
    checks: "scrollWidth <= innerWidth; innerWidth == ancho pedido; sin elementos con right > innerWidth; sin texto < 16 px; contraste; el carnet de ejemplo no pisa «Què és?»",
  },
  notVerified: ["TalkBack", "dispositivo físico", "zoom de página de Chrome (solo escala de texto)"],
  results, pass: !failed,
};
writeFileSync(join(outDir, `${PREFIX}-qa-metadata.json`), JSON.stringify(meta, null, 2) + "\n");
console.log(failed ? "QA CON FALLOS" : `QA OK: ${results.length} casos; metadatos en ${join(outDir, PREFIX + "-qa-metadata.json")}`);
try { session.ws.close(); } catch {}
cleanup();
process.exit(failed ? 1 : 0);
