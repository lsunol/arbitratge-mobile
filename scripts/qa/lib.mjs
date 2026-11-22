// Utilidades QA para el emulador (Node sin dependencias). Usa `adb` y el CLI `android`.
// Dispositivo: `ANDROID_SERIAL` (por defecto `emulator-5554`). Siempre se pasa explícito (`adb -s`,
// `android --device`) para no actuar sobre un móvil físico conectado a la vez.
// Cada llamada externa lleva timeout: un comando colgado falla solo, sin tener que matar procesos.
import { execFileSync } from 'node:child_process';
import { readFileSync, mkdirSync } from 'node:fs';
import { homedir } from 'node:os';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

export const PKG = 'dev.lluissunol.arbitratge';
const ANDROID = process.env.ANDROID_CLI ?? join(homedir(), 'AppData', 'AndroidCLI', 'android.exe');
const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
export const bank = JSON.parse(readFileSync(join(root, 'bank-sample', 'bank-sample.json'), 'utf8'));

export const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
export const SERIAL = process.env.ANDROID_SERIAL || 'emulator-5554';
const CMD_TIMEOUT = 60_000;
/** Espera síncrona sin lanzar procesos. */
export const sleepSync = (ms) => Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, ms);
const dec = new TextDecoder('windows-1252'); // el CLI `android` en Windows emite cp1252

export function adb(...args) {
  return execFileSync('adb', ['-s', SERIAL, ...args], { encoding: 'utf8', maxBuffer: 64e6, timeout: CMD_TIMEOUT });
}
export function sh(...args) { return adb('shell', ...args); }

export function layout() {
  const buf = execFileSync(ANDROID, ['--no-metrics', 'layout', '--device', SERIAL], { maxBuffer: 64e6, timeout: CMD_TIMEOUT });
  const wins = JSON.parse(dec.decode(buf));
  const out = [];
  const walk = (n, w) => {
    out.push({ ...n, win: w });
    (n.content ?? []).forEach((c) => walk(c, w));
    (n.children ?? []).forEach((c) => walk(c, w));
  };
  for (const w of wins) if (w.type !== 'SYSTEM' && !/QWERTY/.test(w['window-title'] ?? '')) walk(w, w['window-title']);
  return out.filter((n) => n.bounds && !n.hidden);
}

const label = (n) => [n.text, n['content-desc']].filter(Boolean).join(' | ');
export const lab = label;
export function findIn(nodes, re) {
  return nodes.find((n) => !n['off-screen'] && label(n) && re.test(label(n)));
}
export async function waitFor(re, timeout = 8000) {
  const t0 = Date.now();
  for (;;) {
    const nodes = layout();
    const n = findIn(nodes, re);
    if (n) return { n, nodes };
    if (Date.now() - t0 > timeout) throw new Error(`No apareix ${re}`);
    await sleep(400);
  }
}
export function tapNode(n) {
  const [x, y] = JSON.parse(n.center);
  sh('input', 'tap', String(x), String(y));
}
export async function tap(re, timeout) {
  // Con letra al 200 % las acciones no fijas quedan al final del contenido (I9): si no aparece, se desplaza.
  let found = null;
  for (let i = 0; i < 5 && !found; i++) {
    try { found = (await waitFor(re, i < 4 ? 2500 : (timeout ?? 8000))).n; }
    catch (e) { if (i === 4) throw e; swipeUp(); await sleep(400); }
  }
  tapNode(found);
  await sleep(450);
}
export const tapXY = (x, y) => sh('input', 'tap', String(x), String(y));
export const back = () => sh('input', 'keyevent', 'KEYCODE_BACK');
export const swipeUp = () => sh('input', 'swipe', '540', '1900', '540', '500', '350');
export const swipeDown = () => sh('input', 'swipe', '540', '500', '540', '1900', '350');

export function setTheme(t) { sh('cmd', 'uimode', 'night', t === 'fosc' ? 'yes' : 'no'); }
export function setScale(s) { sh('settings', 'put', 'system', 'font_scale', String(s)); }
export function clearApp() { sh('pm', 'clear', PKG); }
export function launch() { sh('am', 'start', '-n', `${PKG}/.MainActivity`); }
export function stop() { sh('am', 'force-stop', PKG); }

/** Captura con nombre `${nn}-${name}-${scale}-${theme}.png`. */
export function makeShooter(outDir, scaleTag, themeTag) {
  mkdirSync(outDir, { recursive: true });
  const taken = [];
  const shoot = (nn, name) => {
    const f = `${nn}-${name}-${scaleTag}-${themeTag}.png`;
    execFileSync(ANDROID, ['--no-metrics', 'screen', 'capture', '--device', SERIAL, '-o', join(outDir, f)], { timeout: CMD_TIMEOUT });
    taken.push(f);
    console.log('  📷', f);
    return f;
  };
  /** Captura y, si hay contenido desplazable fuera de pantalla, una segunda al final (`b`). */
  const shootScroll = (nn, name) => {
    shoot(nn, name);
    const sc = layout().some((n) => (n.interactions ?? []).includes('SCROLLABLE'));
    if (sc) {
      swipeUp(); swipeUp();
      sleepSync(500);
      shoot(nn + 'b', name + '-final');
      swipeDown(); swipeDown();
    }
  };
  return { shoot, shootScroll, taken };
}

// --- Banco de muestra (público, ficticio) ---
export const qByText = (text) => bank.questions.find((q) => q.text === text);
export function questionTypeFromNodes(nodes) {
  if (findIn(nodes, /. Cert o fals./)) return 'tf';
  if (findIn(nodes, /. Resposta múltiple./)) return 'multi';
  if (findIn(nodes, /. Una resposta./)) return 'single';
  return null;
}
