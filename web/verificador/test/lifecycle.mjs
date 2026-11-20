// Pruebas de ciclo de vida (sin navegador): cámara, «preparat» y carga independiente de claves.
// Uso: node web/verificador/test/lifecycle.mjs
import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { CameraSession } from "../lib/camera-session.js";
import { startCamera } from "../lib/qr.js";
import { checkPrepared } from "../lib/offline.js";
import { loadDeployedKeys, buildTrustedKeys } from "../lib/keys.js";
import { KeyStore, verifyCurrent } from "../lib/key-store.js";
import { verifyToken } from "../lib/arb1.js";

let n = 0;
const t = async (name, fn) => {
  try { await fn(); n++; } catch (e) { console.error("FALLA:", name, "\n", e); process.exitCode = 1; }
};

// ---------- cámara
function fakeMedia() {
  const calls = { stopped: 0, requested: 0 };
  const pending = [];
  const mediaDevices = {
    getUserMedia() {
      calls.requested++;
      return new Promise((resolve, reject) => pending.push({ resolve, reject }));
    },
  };
  const makeStream = () => {
    const track = { stop: () => calls.stopped++, getCapabilities: () => ({}) };
    return { getTracks: () => [track], getVideoTracks: () => [track] };
  };
  return { mediaDevices, pending, calls, makeStream };
}
const fakeVideo = () => ({ srcObject: null, readyState: 0, videoWidth: 0, setAttribute() {}, play: async () => {} });
const startFn = (m) => ({ signal }) =>
  startCamera(fakeVideo(), () => {}, { signal, mediaDevices: m.mediaDevices, createCanvas: () => ({ getContext: () => ({}) }) });

await t("cámara: permiso pendiente + navegación => el stream concedido después se para", async () => {
  const m = fakeMedia();
  const s = new CameraSession(startFn(m));
  const p = s.begin();
  assert.equal(s.busy, true);
  s.cancel(); // el usuario sale de la pantalla
  m.pending[0].resolve(m.makeStream()); // el permiso llega tarde
  assert.deepEqual(await p, { status: "stale" });
  assert.equal(m.calls.stopped, 1);
  assert.equal(s.active, null);
  assert.equal(s.busy, false);
});
await t("cámara: permiso denegado tras cancelar => 'stale', no error visible", async () => {
  const m = fakeMedia();
  const s = new CameraSession(startFn(m));
  const p = s.begin();
  s.cancel();
  const e = new Error("x"); e.name = "NotAllowedError";
  m.pending[0].reject(e);
  assert.deepEqual(await p, { status: "stale" });
});
await t("cámara: dos arranques seguidos => solo el último queda vivo", async () => {
  const m = fakeMedia();
  const s = new CameraSession(startFn(m));
  const p1 = s.begin();
  const p2 = s.begin();
  m.pending[0].resolve(m.makeStream());
  m.pending[1].resolve(m.makeStream());
  assert.equal((await p1).status, "stale");
  assert.equal((await p2).status, "live");
  assert.equal(m.calls.stopped, 1); // el del primero
  s.cancel();
  assert.equal(m.calls.stopped, 2);
});
await t("cámara: arranque normal, error real y cancelación de la cámara viva", async () => {
  const m = fakeMedia();
  const s = new CameraSession(startFn(m));
  let p = s.begin();
  m.pending[0].resolve(m.makeStream());
  const r = await p;
  assert.equal(r.status, "live");
  assert.equal(s.busy, true);
  s.cancel(); // pasar a segundo plano
  assert.equal(m.calls.stopped, 1);
  assert.equal(s.busy, false);
  p = s.begin();
  const e = new Error("x"); e.name = "NotReadableError";
  m.pending[1].reject(e);
  const r2 = await p;
  assert.equal(r2.status, "error");
  assert.equal(r2.error.name, "NotReadableError");
});
await t("cámara: señal ya abortada no pide el permiso", async () => {
  const m = fakeMedia();
  const ctrl = new AbortController();
  ctrl.abort();
  await assert.rejects(startCamera(fakeVideo(), () => {}, { signal: ctrl.signal, mediaDevices: m.mediaDevices }), { name: "AbortError" });
  assert.equal(m.calls.requested, 0);
});

// ---------- «preparat» ligado al worker que controla
const sha = (b) => createHash("sha256").update(b).digest("hex");
function buildCache(version, { omit = [] } = {}) {
  const files = { "index.html": "<html>", "app.js": "x" };
  const manifest = { version, cache: "verificador-" + version, files: Object.entries(files).map(([p, c]) => ({ path: p, sha256: sha(c) })) };
  const store = new Map(Object.entries(files).map(([p, c]) => [p, new Response(c)]));
  store.set("precache-manifest.json", new Response(JSON.stringify(manifest)));
  for (const o of omit) store.delete(o);
  return { name: "verificador-" + version, store };
}
const envFor = (caches, controllerVersion, { controller = true } = {}) => {
  const map = new Map(caches.map((c) => [c.name, c.store]));
  return {
    isSecureContext: true,
    subtle: globalThis.crypto.subtle,
    serviceWorker: { controller: controller ? {} : null },
    getInfo: async () => ({ version: controllerVersion, cache: "verificador-" + controllerVersion }),
    caches: {
      has: async (k) => map.has(k),
      open: async (k) => ({
        match: async (p) => { const r = map.get(k)?.get(p); return r ? r.clone() : undefined; },
      }),
    },
  };
};
await t("preparat: caché del controlador completa => ready", async () => {
  assert.equal((await checkPrepared(envFor([buildCache("1.0.0")], "1.0.0"))).state, "ready");
});
await t("preparat: worker nuevo en espera con caché completa + caché del controlador BORRADA => incomplete", async () => {
  const waiting = buildCache("1.1.0"); // completa, pero es del worker en espera
  const env = envFor([waiting], "1.0.0"); // el controlador es 1.0.0 y su caché no existe
  const r = await checkPrepared(env);
  assert.equal(r.state, "incomplete");
  assert.equal(r.version, "1.0.0");
});
await t("preparat: caché del controlador incompleta o con manifiesto ajeno => incomplete", async () => {
  assert.equal((await checkPrepared(envFor([buildCache("1.0.0", { omit: ["app.js"] }), buildCache("1.1.0")], "1.0.0"))).state, "incomplete");
  assert.equal((await checkPrepared(envFor([buildCache("1.0.0", { omit: ["precache-manifest.json"] }), buildCache("1.1.0")], "1.0.0"))).state, "incomplete");
  const c = buildCache("1.0.0");
  c.store.set("app.js", new Response("alterado")); // hash distinto
  assert.equal((await checkPrepared(envFor([c], "1.0.0"))).state, "incomplete");
});
await t("preparat: sin controlador, sin respuesta del worker o sin API", async () => {
  assert.equal((await checkPrepared(envFor([buildCache("1.0.0")], "1.0.0", { controller: false }))).state, "incomplete");
  const e = envFor([buildCache("1.0.0")], "1.0.0");
  e.getInfo = async () => null;
  assert.equal((await checkPrepared(e)).state, "incomplete");
  assert.equal((await checkPrepared({ caches: null, serviceWorker: {}, isSecureContext: true })).state, "unsupported");
});

// ---------- carga independiente de claves
const root = join(dirname(fileURLToPath(import.meta.url)), "..", "..", "..", "test-vectors");
const tk = JSON.parse(readFileSync(join(root, "trusted-keys-test.json"), "utf8"));
const respond = (body, ok = true) => ({ ok, status: ok ? 200 : 404, json: async () => { if (body instanceof Error) throw body; return body; } });
const fetchWith = (files) => async (url) => {
  const name = url.replace(/^keys\//, "");
  if (!(name in files)) return respond(null, false);
  return respond(files[name]);
};
await t("claves: un fichero malo no invalida el resto", async () => {
  const files = {
    "index.json": { files: ["test.json", "testx.json", "bad.json", "gone.json", "Evil.json", "mismatch.json"] },
    "test.json": tk[0],
    "testx.json": tk[1],
    "bad.json": new SyntaxError("JSON mal formado"),
    "mismatch.json": { ...tk[0], kid: "otro" }, // kid != nombre de fichero
  };
  const r = await loadDeployedKeys("keys/", fetchWith(files));
  assert.deepEqual([...r.keys.keys()].sort(), ["test", "testx"]);
  assert.equal(r.discarded.length, 4); // bad, gone (404), Evil (nombre), mismatch
  assert.ok(r.discarded.some((d) => d.startsWith("bad.json")));
  assert.ok(r.discarded.some((d) => d.startsWith("gone.json")));
  assert.ok(r.discarded.some((d) => d.startsWith("mismatch")));
});
await t("claves: fetch que lanza => solo esa entrada; index ilegible => lanza", async () => {
  const f = async (url) => {
    if (url.endsWith("testx.json")) throw new TypeError("network");
    return fetchWith({ "index.json": { files: ["test.json", "testx.json"] }, "test.json": tk[0] })(url);
  };
  const r = await loadDeployedKeys("keys/", f);
  assert.deepEqual([...r.keys.keys()], ["test"]);
  assert.equal(r.discarded.length, 1);
  await assert.rejects(loadDeployedKeys("keys/", fetchWith({})));
});
await t("claves: lista vacía de producción", async () => {
  const r = await loadDeployedKeys("keys/", fetchWith({ "index.json": { files: [] } }));
  assert.equal(r.keys.size, 0);
  assert.equal(r.discarded.length, 0);
});

// ---------- cámara: carreras con play() pendiente y <video> compartido
const tick = () => new Promise((r) => setImmediate(r));
function controllableVideo() {
  const v = { srcObject: null, readyState: 0, videoWidth: 0, setAttribute() {}, plays: [] };
  v.play = () => new Promise((resolve) => v.plays.push(resolve));
  return v;
}
const startOn = (video, m) => ({ signal }) =>
  startCamera(video, () => {}, { signal, mediaDevices: m.mediaDevices, createCanvas: () => ({ getContext: () => ({}) }) });
const trackedStream = () => {
  let stopped = 0;
  const track = { stop: () => stopped++, getCapabilities: () => ({}) };
  return { stream: { getTracks: () => [track], getVideoTracks: () => [track] }, stopped: () => stopped };
};

await t("cámara: cancelar con play() pendiente para el stream YA y no espera a play()", async () => {
  const m = fakeMedia();
  const video = controllableVideo();
  const s = new CameraSession(startOn(video, m));
  const p = s.begin();
  const A = trackedStream();
  m.pending[0].resolve(A.stream);
  await tick(); // el stream ya está asignado y play() sigue pendiente
  assert.equal(video.plays.length, 1);
  assert.equal(video.srcObject, A.stream);
  s.cancel();
  assert.equal(A.stopped(), 1, "pistas paradas sin esperar a play()");
  assert.equal(video.srcObject, null);
  assert.deepEqual(await p, { status: "stale" }); // play() nunca se resolvió
});
await t("cámara: A obsoleto que llega DESPUÉS de B vivo no toca el <video> ni la pista de B", async () => {
  const m = fakeMedia();
  const video = controllableVideo();
  const s = new CameraSession(startOn(video, m));
  const pA = s.begin();
  const pB = s.begin(); // cancela A
  const A = trackedStream();
  const B = trackedStream();
  m.pending[1].resolve(B.stream);
  await tick();
  video.plays[0]();
  const rB = await pB;
  assert.equal(rB.status, "live");
  m.pending[0].resolve(A.stream); // A llega tarde
  assert.deepEqual(await pA, { status: "stale" });
  assert.equal(A.stopped(), 1);
  assert.equal(B.stopped(), 0, "la pista de B sigue activa");
  assert.equal(video.srcObject, B.stream, "la vista previa de B sigue");
  s.cancel();
  assert.equal(B.stopped(), 1);
  assert.equal(video.srcObject, null);
});
await t("cámara: A con play() pendiente, B empieza; al resolver play() de A no se pisa a B", async () => {
  const m = fakeMedia();
  const video = controllableVideo();
  const s = new CameraSession(startOn(video, m));
  const pA = s.begin();
  const A = trackedStream();
  m.pending[0].resolve(A.stream);
  await tick(); // A asignado, play pendiente
  const pB = s.begin(); // cancela A: se libera su stream
  const B = trackedStream();
  m.pending[1].resolve(B.stream);
  await tick();
  assert.equal(video.srcObject, B.stream);
  video.plays[0](); // play() de A se resuelve tarde
  video.plays[1]();
  assert.deepEqual(await pA, { status: "stale" });
  assert.equal((await pB).status, "live");
  assert.equal(video.srcObject, B.stream);
  assert.equal(B.stopped(), 0);
  assert.equal(A.stopped(), 1);
  s.cancel();
});

// ---------- claves: cargas fuera de orden y verificación en curso
const casesJson = JSON.parse(readFileSync(join(root, "cases.json"), "utf8")).cases;
const V01 = casesJson.find((c) => c.id === "V01").input;
const deferred = () => { let resolve; const promise = new Promise((r) => (resolve = r)); return { promise, resolve }; };
const keysWithStatus = async (status) => (await buildTrustedKeys([{ ...tk[0], status }])).keys;

await t("KeyStore: una carga antigua que termina tarde no pisa a la nueva", async () => {
  const gates = [deferred(), deferred()];
  let call = 0;
  const store = new KeyStore(async () => gates[call++].promise);
  const p1 = store.refresh(); // arranque (lento)
  const p2 = store.refresh(); // controllerchange (más nuevo, rápido)
  gates[1].resolve({ keys: await keysWithStatus("compromised"), discarded: [] });
  assert.equal(await p2, true);
  gates[0].resolve({ keys: await keysWithStatus("active"), discarded: [] });
  assert.equal(await p1, false); // descartada
  assert.equal(store.keys.get("test").status, "compromised");
  assert.equal(store.generation, 1);
});
await t("KeyStore: una carga que falla deja la lista vacía (nada se acepta)", async () => {
  const store = new KeyStore(async () => { throw new Error("red"); });
  await store.refresh();
  assert.equal(store.keys.size, 0);
});
await t("verificación en curso + cambio de claves => se descarta y se repite (compromised => KID_NOT_ACCEPTED)", async () => {
  const store = new KeyStore(async () => ({ keys: await keysWithStatus("active"), discarded: [] }));
  await store.refresh();
  const gate = deferred();
  let calls = 0;
  const slowVerify = async (raw, keys) => {
    const mine = ++calls;
    const res = await verifyToken(raw, keys); // se calcula con la lista que tenía al empezar
    if (mine === 1) await gate.promise; // y se queda «en vuelo»
    return res;
  };
  const p = verifyCurrent(store, V01, slowVerify);
  await tick();
  store.load = async () => ({ keys: await keysWithStatus("compromised"), discarded: [] });
  await store.refresh(); // actualización: la clave pasa a compromised
  gate.resolve();
  const res = await p;
  assert.equal(res.code, "KID_NOT_ACCEPTED");
  assert.equal(calls, 2);
});
await t("verificación: sin cambios devuelve el resultado normal; carga en curso se espera", async () => {
  const gate = deferred();
  const store = new KeyStore(async () => { await gate.promise; return { keys: await keysWithStatus("active"), discarded: [] }; });
  store.refresh();
  const p = verifyCurrent(store, V01, verifyToken);
  await tick();
  gate.resolve();
  assert.equal((await p).code, "OK");
});
await t("claves: entradas del índice que no son texto no vacían la lista", async () => {
  const evil = { toString: null, valueOf: null };
  const files = {
    "index.json": { files: [evil, 7, null, ["a"], "test.json"] },
    "test.json": tk[0],
  };
  const r = await loadDeployedKeys("keys/", fetchWith(files));
  assert.deepEqual([...r.keys.keys()], ["test"]);
  assert.equal(r.discarded.length, 4);
});

if (!process.exitCode) console.log(`lifecycle: ${n} pruebas OK`);
