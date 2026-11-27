#!/usr/bin/env node
// Captura reproducible de la app candidata en el emulador (bank-sample, ficticio).
//   node scripts/qa/capture-ui.mjs --flow main   --theme clar|fosc [--out <dir>]   (escala 100 %)
//   node scripts/qa/capture-ui.mjs --flow escala --scale 130|200 --theme clar        (inici, pregunta, resultat, credencial)
//   node scripts/qa/capture-ui.mjs --flow pregunta --scale 100|130|200 --theme clar|fosc   (només pregunta única/múltiple/V-F, buida i marcada: 10-15)
//   node scripts/qa/capture-ui.mjs --flow temps  --theme clar|fosc                   (avís d'1 min + temps esgotat; ~5 min)
//   node scripts/qa/capture-ui.mjs --flow fora   --theme clar                        (temps esgotat mentre eres fora; ~5 min)
// Opciones: --max-minutes N (por defecto 15; el script se aborta solo al superarlo).
// Requisitos: emulator-5554 arrancado (u otro serial en ANDROID_SERIAL), APK debug instalado (./gradlew :app-candidate:assembleDebug; adb install -r ...).
// Deja siempre font_scale 1.0 y modo claro al terminar. No toca código de la app.
//
// PARAR UNA CAPTURA: el script escribe su PID en `<out>/.capture-ui.pid`. Para pararlo usa SOLO ese PID
// (`taskkill //PID <pid> //T //F` en Git Bash o `Stop-Process -Id <pid>` en PowerShell) y después
// `adb -s emulator-5554 shell settings put system font_scale 1.0; ... cmd uimode night no`.
// NUNCA `taskkill /IM node.exe`, `killall node` ni `pkill node`: el harness del agente (pi, opencode…) también es
// Node y lo matarías junto con la sesión.
import { join, dirname } from 'node:path';
import { mkdirSync, writeFileSync, rmSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import * as L from './lib.mjs';

const arg = (k, d) => { const i = process.argv.indexOf('--' + k); return i > 0 ? process.argv[i + 1] : d; };
const flow = arg('flow', 'main');
const theme = arg('theme', 'clar');
const scale = arg('scale', '100');
const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const out = arg('out', join(root, 'docs', 'design', 'reviews', 'evidence', '2026-11-22-ui-abans'));
const NAME = 'Maria Pujol Vila';
const TF = { t: 'Cert', f: 'Fals' };
// A escala != 100 sólo se capturan estos estados.
const SCALE_ALLOW = new Set(['01', '02', '06', '10', '11', '12', '13', '14', '15', '30', '31', '50', '51', '60']);

const maxMinutes = Number(arg('max-minutes', '15'));
mkdirSync(out, { recursive: true });
const pidFile = join(out, '.capture-ui.pid');
writeFileSync(pidFile, String(process.pid));
console.log(`capture-ui: pid ${process.pid}, dispositiu ${L.SERIAL}, límit ${maxMinutes} min`);
const restore = () => {
  try { L.setScale('1.0'); L.setTheme('clar'); } catch { /* dispositiu no disponible */ }
  rmSync(pidFile, { force: true });
};
const watchdog = setTimeout(() => {
  console.error(`capture-ui: superat el límit de ${maxMinutes} min; s'atura.`);
  restore();
  process.exit(2);
}, maxMinutes * 60_000);
for (const sig of ['SIGINT', 'SIGTERM']) process.on(sig, () => { restore(); process.exit(130); });

const { shoot, shootScroll } = L.makeShooter(out, `${scale}`, theme);
const ALLOW200 = new Set(['01', '02', '06', '10', '11', '12', '13', '14', '15', '30', '31', '50', '60']); // 11 i 60 inclosos a 200 % (revisió 2026-11-22)
// El resultat ja no té títol «Resultat»: el veredicte (encapçalament) el substitueix.
const RESULT = /^(Has aprovat!?|No has aprovat)$/;
const ok = (nn) => scale === '100' || (scale === '200' ? ALLOW200 : SCALE_ALLOW).has(nn.replace(/b$/, ''));
let quiet = false; // flujo 'pregunta': solo capturas de la pantalla de pregunta (10-15)
const S = (nn, name) => { if (!quiet && ok(nn)) shoot(nn, name); };
const SS = (nn, name) => { if (!quiet && ok(nn)) shootScroll(nn, name); };
const SQ = (nn, name) => { if (ok(nn)) shoot(nn, name); };
const wait = (ms) => L.sleep(ms);

/** Botón dentro de un diálogo (ventana sin título). */
async function dialogBtn(re) {
  const t0 = Date.now();
  for (;;) {
    const n = L.layout().find((x) => x.win === '' && re.test(L.lab(x)));
    if (n) { L.tapNode(n); await wait(500); return; }
    if (Date.now() - t0 > 8000) throw new Error('Diàleg: no apareix ' + re);
    await wait(300);
  }
}
function currentQuestion(nodes) {
  const q = L.bank.questions.find((b) => nodes.some((n) => n.text === b.text));
  return { q, type: L.questionTypeFromNodes(nodes) };
}
const TYPE_NN = { single: ['10', '11', 'unica'], multi: ['12', '13', 'multiple'], tf: ['14', '15', 'certfals'] };

async function tapOption(text) {
  // Desplaça de mica en mica (a 200 % la llista no cap): primer cap avall, després cap amunt.
  const dirs = [...Array(4).fill('down'), ...Array(8).fill('up')];
  for (let i = 0; i <= dirs.length; i++) {
    const n = L.layout().find((x) => x.text === text && !x['off-screen'] && JSON.parse(x.center)[1] > 450 && JSON.parse(x.center)[1] < 2150);
    if (n) { L.tapNode(n); await wait(500); return; }
    if (i === dirs.length) break;
    if (dirs[i] === 'down') L.sh('input', 'swipe', '540', '1500', '540', '1000', '300');
    else L.sh('input', 'swipe', '540', '1000', '540', '1500', '300');
    await wait(400);
  }
  throw new Error('Opció no trobada: ' + text);
}
const optText = (q, id) => (q.type === 'true_false' ? TF[id] : q.options.find((o) => o.id === id).text);
function chooseTexts(q, policy) {
  if (policy === 'none') return [];
  if (policy === 'right') return q.correct.map((id) => optText(q, id));
  // 'wrong': una opció incorrecta; en múltiples, només UNA de correcta (resposta parcial)
  if (q.type === 'multiple_choice') return [optText(q, q.correct[0])];
  return [optText(q, q.options.map((o) => o.id).find((id) => !q.correct.includes(id)))];
}

async function toWelcome() {
  L.stop(); L.clearApp(); L.launch();
  await L.waitFor(/Et donem la benvinguda/);
  await wait(600);
}
async function welcomeToRules() {
  SS('01', 'benvinguda');
  await L.tap(/^Continuar$/);
  await L.waitFor(/Encara no has fet cap examen/); await wait(400);
  SS('02', 'inici-inicial');
  await L.tap(/^Començar l'examen$/);
  await L.waitFor(/El teu nom/); await wait(500);
  S('03', 'nom-buit');
  await L.tap(/^Continuar$/); await L.waitFor(/Escriu el teu nom per continuar/); await wait(400);
  S('04', 'nom-error');
  await L.tap(/^Nom i cognoms$/); L.sh('input', 'text', NAME.replace(/ /g, '%s')); await wait(700);
  S('05', 'nom-escrit-teclat');
  await L.tap(/^Continuar$/);
  await L.waitFor(/Abans de començar/); await wait(500);
  SS('06', 'normes');
}
/** Inicia l'examen des de les normes i recorre les 5 preguntes per llegir-ne els tipus. */
async function beginAndScan() {
  await L.tap(/^Començar ara$/);
  await L.waitFor(/Pregunta 1 de 5/); await wait(500);
  const seen = [];
  for (let i = 0; i < 5; i++) {
    seen.push(currentQuestion(L.layout()).type);
    if (i < 4) await L.tap(/^Següent$/);
  }
  for (let i = 0; i < 4; i++) await L.tap(/^(Pregunta anterior|Anterior)$/);
  return seen;
}

/** Recorre les 5 preguntes. policy: right | wrong | none. rich: captures extra (sortir, temps amagat, resum parcial). */
async function answerAll(policy, { rich = false, skipLast = false, shots = true } = {}) {
  const marked = new Set();
  const empty = new Set();
  for (let i = 0; i < 5; i++) {
    await L.waitFor(new RegExp(`Pregunta ${i + 1} de 5`)); await wait(300);
    let { q, type } = currentQuestion(L.layout());
    if (rich && i === 0) {
      L.back(); await L.waitFor(/Vols sortir de l'examen/); await wait(400);
      S('16', 'pregunta-sortir-dialeg');
      await dialogBtn(/Continuar l'examen/); await wait(300);
    }
    if (rich && i === 1) {
      const tn = L.layout().find((x) => /amagar el temps|^\d+ min$/.test(L.lab(x)));
      if (tn) {
        L.tapNode(tn); await wait(600);
        S('17', 'pregunta-temps-amagat');
        const again = L.layout().find((x) => /Mostrar el temps|mostrar el temps|Temps amagat/.test(L.lab(x)));
        if (again) { L.tapNode(again); await wait(500); }
      }
    }
    if (rich && i === 2) {
      await L.tap(/^Veure el resum: /); await L.waitFor(/^Preguntes$/); await wait(500);
      SS('20', 'resum-parcial');
      await L.tap(/^Avaluar$/); await L.waitFor(/Vols avaluar l'examen/); await wait(400);
      S('21', 'finalitzar-dialeg-amb-buides');
      await dialogBtn(/Tornar a l'examen/);
      await L.tap(/Anar a la primera sense resposta/);
      await L.waitFor(/Pregunta 3 de 5/); await wait(400);
      ({ q, type } = currentQuestion(L.layout()));
    }
    const [nE, nM, tn] = TYPE_NN[type];
    if (!empty.has(type)) { empty.add(type); if (shots) S(nE, `pregunta-${tn}-buida`); }
    const last = i === 4;
    if (!(skipLast && last && marked.has(type))) {
      for (const t of chooseTexts(q, policy)) await tapOption(t);
      if (policy !== 'none' && !marked.has(type)) { marked.add(type); await wait(300); if (shots) S(nM, `pregunta-${tn}-marcada`); }
    }
    await L.tap(last ? /^Revisar$/ : /^Següent$/);
  }
  await L.waitFor(/^Preguntes$/); await wait(500);
}
async function finish(nnOverview, nnDialog) {
  if (nnOverview) SS(nnOverview, 'resum-complet');
  await L.tap(/^Avaluar$/); await L.waitFor(/Vols avaluar l'examen/); await wait(400);
  if (nnDialog) S(nnDialog, 'finalitzar-dialeg-complet');
  await dialogBtn(/^Avaluar$/);
  await L.waitFor(RESULT, 15000); await wait(900);
}
async function retakeToQuestions() {
  await L.tap(/^Tornar a fer l'examen$|^Començar l'examen$/);
  await L.waitFor(/El teu nom|Abans de començar|Pregunta 1 de 5/);
  await wait(400);
  if (L.findIn(L.layout(), /^El teu nom$/)) { await L.tap(/^Continuar$/); await L.waitFor(/Abans de començar/); }
  if (L.findIn(L.layout(), /Abans de començar/)) await L.tap(/^Començar ara$/);
  await L.waitFor(/Pregunta 1 de 5/); await wait(400);
}
async function passedAndCredential(rich) {
  await finish(rich ? '22' : null, rich ? '23' : null);
  SS('30', 'resultat-aprovat');
  await L.tap(/^Veure la credencial$/); await L.waitFor(/Mostrar a un organitzador/); await wait(900);
  SS('50', 'credencial');
  await L.tap(/^Mostrar a un organitzador$/); await L.waitFor(/Tancar/); await wait(900);
  S('51', 'credencial-presentacio');
  await L.tap(/^Tancar$/); await wait(600);
  await L.tap(/^Tornar a l'inici$/);
  await L.waitFor(/Últim examen/); await wait(500);
  SS('60', 'inici-amb-credencial');
}

async function main() {
  L.setScale(scale === '100' ? '1.0' : scale === '130' ? '1.3' : '2.0');
  L.setTheme(theme);
  await wait(1200);
  if (flow === 'main') {
    for (let attempt = 0; ; attempt++) {
      await toWelcome();
      await welcomeToRules();
      const seen = await beginAndScan();
      console.log('Tipus de les preguntes:', seen.join(','));
      if (['single', 'multi', 'tf'].every((t) => seen.includes(t)) || attempt >= 12) break;
      console.log('  falta algun tipus: es reinicia');
    }
    // R1: examen SUSPÈS
    await answerAll('wrong', { rich: true, skipLast: true });
    await finish(null, null);
    SS('31', 'resultat-suspes');
    await L.tap(/^Repassar les \d+ preguntes fallades|^Repassar la pregunta fallada/); await L.waitFor(/^Repàs$/); await wait(600);
    SS('40', 'repas-llista-fallades');
    await L.tap(/^Totes \d+/); await wait(700);
    SS('41', 'repas-llista-totes');
    await L.tap(/^Pregunta 1 de 5 · /); await L.waitFor(/Correcta · |Incorrecta · |No la vas respondre/); await wait(500);
    SS('42', 'repas-detall-fallada');
    L.back(); await wait(500); L.back(); await L.waitFor(RESULT); await wait(500);
    await L.tap(/^Tornar a l'inici$/); await L.waitFor(/Últim examen/); await wait(500);
    SS('61', 'inici-ultim-suspes');
    // R2: examen APROVAT + credencial
    await retakeToQuestions();
    await answerAll('right', { shots: false });
    await passedAndCredential(true);
    try { await L.tap(/^Repassar l'últim examen$/); } catch { // la fila queda per sota de la barra fixa: es desplaça
      L.sh('input', 'swipe', '540', '1700', '540', '700', '300'); await wait(500); await L.tap(/^Repassar l'últim examen$/);
    }
    await L.waitFor(/^Repàs$/); await wait(600);
    SS('43', 'repas-llista-aprovat-filtre-fallades');
    await L.tap(/^Totes \d+/); await wait(700);
    SS('45', 'repas-llista-totes-aprovat');
    const row = L.layout().find((n) => /^Pregunta 1 de 5 · /.test(L.lab(n)));
    if (row) { L.tapNode(row); await wait(700); SS('44', 'repas-detall-encertada'); L.back(); await wait(400); }
    L.back();
  } else if (flow === 'pregunta') {
    for (let attempt = 0; ; attempt++) {
      quiet = true;
      await toWelcome();
      await welcomeToRules();
      const seen = await beginAndScan();
      console.log('Tipus de les preguntes:', seen.join(','));
      if (['single', 'multi', 'tf'].every((t) => seen.includes(t)) || attempt >= 12) break;
      console.log('  falta algun tipus: es reinicia');
    }
    quiet = false;
    await answerAll('right');
  } else if (flow === 'escala') {
    await toWelcome();
    await welcomeToRules();
    await beginAndScan();
    await answerAll('right');
    await passedAndCredential(false);
    await retakeToQuestions();
    for (let i = 0; i < 4; i++) await L.tap(/^Següent$/);
    await L.tap(/^Revisar$/); await L.waitFor(/^Preguntes$/); await wait(400);
    await finish(null, null);
    SS('31', 'resultat-suspes');
  } else if (flow === 'temps' || flow === 'fora') {
    await toWelcome();
    await welcomeToRules();
    await L.tap(/^Començar ara$/);
    await L.waitFor(/Pregunta 1 de 5/);
    const t0 = Date.now();
    const at = async (sec) => { const d = sec * 1000 - (Date.now() - t0); if (d > 0) await wait(d); };
    await at(4 * 60 + 5);
    await L.waitFor(/Només queda 1 minut/, 20000); await wait(300);
    S('33', 'pregunta-avis-1-minut');
    if (flow === 'temps') {
      await at(5 * 60 + 6);
      await L.waitFor(/S'ha acabat el temps/, 30000); await wait(800);
      S('32', 'temps-esgotat');
      await L.tap(/^Veure el resultat$/); await L.waitFor(RESULT, 15000); await wait(600);
      S('34', 'resultat-temps-esgotat');
    } else {
      L.sh('input', 'keyevent', 'KEYCODE_HOME');
      await wait(1500);
      // Mata el procés en segon pla (només aquest paquet): en reobrir, el temps ja és esgotat i el ViewModel recupera amb away = true.
      L.sh('am', 'kill', L.PKG);
      await at(5 * 60 + 20);
      L.launch();
      await L.waitFor(/S'ha acabat el temps/, 30000); await wait(800);
      S('35', 'temps-esgotat-fora');
    }
  }
}
try { await main(); } finally { clearTimeout(watchdog); restore(); }
