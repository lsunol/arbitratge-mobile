#!/usr/bin/env node
// Captures «de producte» de l'app candidata per al portfolio i la web (/web): examen complet amb un nom FICTICI.
//   ARB_BANK_FILE=<banc de pilot, fora del repo> ANDROID_SERIAL=<serial> node scripts/qa/capture-promo.mjs --out <dir> [--theme clar|fosc]
// Requisits: l'APK instal·lat és la build que du aquest mateix banc (release de pilot). ESBORRA les dades de l'app (pm clear).
// El banc només es llegeix per saber quina opció és la correcta; no s'imprimeix mai cap text de pregunta.
import { join } from 'node:path';
import { mkdirSync } from 'node:fs';
import * as L from './lib.mjs';

const arg = (k, d) => { const i = process.argv.indexOf('--' + k); return i > 0 ? process.argv[i + 1] : d; };
const out = arg('out');
const theme = arg('theme', 'clar');
if (!out || !process.env.ARB_BANK_FILE) { console.error('Cal --out i ARB_BANK_FILE'); process.exit(1); }
const NAME = arg('name', 'Jordi Casals Vidal');
const N = L.bank.rules?.num_questions ?? L.bank.questions.length;
const wait = (ms) => L.sleep(ms);
mkdirSync(out, { recursive: true });
const { shoot, shootScroll } = L.makeShooter(out, '100', theme);
const S = shoot, SS = shootScroll;
const TF = { t: 'Cert', f: 'Fals' };
const optText = (q, id) => (q.type === 'true_false' ? TF[id] : q.options.find((o) => o.id === id).text);

async function dialogBtn(re) {
  const t0 = Date.now();
  for (;;) {
    const n = L.layout().find((x) => x.win === '' && re.test(L.lab(x)));
    if (n) { L.tapNode(n); await wait(500); return; }
    if (Date.now() - t0 > 8000) throw new Error('Diàleg: no apareix ' + re);
    await wait(300);
  }
}
const typeKey = (nodes) => ({ single: ['10', 'unica'], multi: ['12', 'multiple'], tf: ['14', 'certfals'] })[L.questionTypeFromNodes(nodes)];
async function tapOption(text) {
  for (let i = 0; i < 12; i++) {
    const n = L.layout().find((x) => x.text === text && !x['off-screen'] && JSON.parse(x.center)[1] > 450 && JSON.parse(x.center)[1] < 2150);
    if (n) { L.tapNode(n); await wait(450); return; }
    if (i < 4) L.sh('input', 'swipe', '540', '1500', '540', '1000', '300'); else L.sh('input', 'swipe', '540', '1000', '540', '1500', '300');
    await wait(400);
  }
  throw new Error('Opció no trobada');
}
const wrongTexts = (q) => (q.type === 'multiple_choice' ? [optText(q, q.options.map((o) => o.id).find((id) => !q.correct.includes(id)))] : [optText(q, q.options.map((o) => o.id).find((id) => !q.correct.includes(id)))]);

async function answerAll(policy, shots) {
  const seen = new Set();
  for (let i = 0; i < N; i++) {
    await L.waitFor(new RegExp(`Pregunta ${i + 1} de ${N}`)); await wait(300);
    const nodes = L.layout();
    const q = L.bank.questions.find((b) => nodes.some((n) => n.text === b.text));
    if (!q) throw new Error('Pregunta ' + (i + 1) + ' no trobada al banc');
    const tk = typeKey(nodes);
    const first = tk && !seen.has(tk[1]);
    if (shots && (first || i === 0)) { S(i === 0 ? '10' : tk[0], 'pregunta-' + (tk?.[1] ?? 'x') + '-buida'); }
    const texts = policy === 'right' ? q.correct.map((id) => optText(q, id)) : wrongTexts(q);
    for (const t of texts) await tapOption(t);
    if (shots && first) { seen.add(tk[1]); await wait(300); S(String(+tk[0] + 1), 'pregunta-' + tk[1] + '-marcada'); }
    await L.tap(i === N - 1 ? /^Revisar$/ : /^Següent$/);
  }
  await L.waitFor(/^Preguntes$/); await wait(500);
}
async function evaluate() {
  await L.tap(/^Avaluar$/); await L.waitFor(/Vols avaluar l'examen/); await wait(400);
  await dialogBtn(/^Avaluar$/);
  await L.waitFor(/^(Has aprovat!?|No has aprovat)$/, 15000); await wait(900);
}

L.setTheme(theme); L.setScale('1.0');
L.stop(); L.clearApp(); L.launch();
await L.waitFor(/Et donem la benvinguda/); await wait(600);
S('01', 'benvinguda');
await L.tap(/^Continuar$/); await L.waitFor(/Encara no has fet cap examen/); await wait(400);
SS('02', 'inici');
await L.tap(/^Començar l'examen$/); await L.waitFor(/El teu nom/); await wait(500);
await L.tap(/^Nom i cognoms$/); L.sh('input', 'text', NAME.replace(/ /g, '%s')); await wait(700);
S('05', 'nom-escrit');
L.back(); await wait(500); // tanca el teclat
await L.tap(/^Continuar$/); await L.waitFor(/Abans de començar/); await wait(500);
SS('06', 'normes');
await L.tap(/^Començar ara$/);
await answerAll('wrong', false); // primer intent: suspès (captura del resultat i la repesca)
SS('20', 'resum');
await evaluate();
SS('31', 'resultat-suspes');
await L.tap(/Veure les respostes|Revisar les respostes|Repassar/).catch(() => {});
await wait(600);
S('40', 'repas');
await L.tap(/^Pregunta 1 de .*Fallada/); await wait(900);
SS('42', 'repas-detall');
L.back(); await wait(500); L.back(); await wait(600);
await L.tap(/^Tornar-ho a provar$/);
await L.waitFor(/El teu nom|Abans de començar|Pregunta 1 de/); await wait(400);
if (L.findIn(L.layout(), /^El teu nom$/)) { await L.tap(/^Continuar$/); await L.waitFor(/Abans de començar/); }
if (L.findIn(L.layout(), /Abans de començar/)) await L.tap(/^Començar ara$/);
await answerAll('right', true);
SS('22', 'resum-complet');
await evaluate();
SS('30', 'resultat-aprovat');
await L.tap(/^Veure la credencial$/); await L.waitFor(/Mostrar a un organitzador/); await wait(1200);
SS('50', 'credencial');
await L.tap(/^Mostrar a un organitzador$/); await L.waitFor(/Tancar/); await wait(900);
S('51', 'credencial-presentacio');
await L.tap(/^Tancar$/); await wait(600);
await L.tap(/^Tornar a l'inici$/); await L.waitFor(/Últim examen/); await wait(500);
SS('60', 'inici-amb-credencial');
L.setTheme('clar');
console.log('fet');
