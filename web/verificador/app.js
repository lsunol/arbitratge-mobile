// Verificador CFBB: interfaz. La lógica ARB1 está en lib/arb1.js (misma que usan los tests).
// Todo el texto dinámico se escribe con textContent (sin HTML dinámico): el contenido del token
// no es de fiar.
import { verifyToken, FORBIDDEN_NAME_CHARS_RE } from "./lib/arb1.js";
import { loadDeployedKeys } from "./lib/keys.js";
import { KeyStore, verifyCurrent } from "./lib/key-store.js";
import { decodeImageFile, startCamera } from "./lib/qr.js";
import { checkPrepared, getControllerInfo } from "./lib/offline.js";
import { CameraSession } from "./lib/camera-session.js";
import { T, VIEWS, CODE_VIEW } from "./lib/text.js";

const $ = (id) => document.getElementById(id);
const host = location.host || location.hostname || "(local)";
let appVersion = "?";
let prepared = { state: "unchecked" };
let deferredInstall = null;
let swReg = null;
let lastResult = null;
let camera = null;
let lastRaw = null; // solo en memoria, para repetir la comprobación si cambian las claves
const store = new KeyStore(() => loadDeployedKeys("keys/"));
store.onChange = onKeysChanged;
const camSession = new CameraSession(({ signal }) =>
  startCamera($("video"), onCameraFound, { signal, onNoQr: () => { $("cam-hint").textContent = `${T.cam_noqr} ${T.cam_noqr_alt}`; } }));
let current = null;

const standalone = () =>
  matchMedia("(display-mode: standalone)").matches || matchMedia("(display-mode: minimal-ui)").matches || navigator.standalone === true;

// ---------- DOM helpers
function el(tag, attrs, ...kids) {
  const n = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs || {})) {
    if (v === false || v == null) continue;
    if (k === "class") n.className = v;
    else n.setAttribute(k, v === true ? "" : v);
  }
  for (const k of kids) n.append(k);
  return n;
}
const clear = (n) => { while (n.firstChild) n.removeChild(n.firstChild); };
const ALL_FORBIDDEN = new RegExp(FORBIDDEN_NAME_CHARS_RE.source, "g");
/** Texto de un token NO fiable: los caracteres invisibles/bidi se muestran como «‹U+XXXX›». */
const visible = (s) =>
  String(s).replace(ALL_FORBIDDEN, (c) => `‹U+${c.charCodeAt(0).toString(16).toUpperCase().padStart(4, "0")}›`);

// ---------- navegación
const SCREENS = ["first", "home", "camera", "image", "text", "result", "install", "about"];
function render(id, { focus = true } = {}) {
  stopCamera();
  current = id;
  for (const s of SCREENS) $("screen-" + s).hidden = s !== id;
  if (focus) {
    const target =
      id === "result" ? $("result-head") : document.querySelector(`#screen-${id} h2`);
    target?.focus({ preventScroll: false });
    window.scrollTo(0, 0);
  }
  if (id === "home" || id === "install") refreshPrepared();
  if (id === "camera") resetCameraScreen();
  if (id === "image") { $("image-msg").textContent = ""; $("image-fail").hidden = true; }
  if (id === "text") updateCheckBtn();
  if (id === "about") fillAbout();
}
function go(id) {
  history.pushState({ s: id }, "");
  render(id);
}
window.addEventListener("popstate", (e) => {
  const s = e.state?.s;
  render(s && SCREENS.includes(s) && (s !== "result" || lastResult) ? s : "home");
});

// ---------- estado sin conexión (V0 / V8)
function offlineText() {
  switch (prepared.state) {
    case "ready": return T.v_offline_ready;
    case "unchecked": return T.install_state_checking;
    case "unsupported": return T.install_state_unsupported;
    default: return navigator.onLine ? T.install_state_wait : T.v_offline_notready;
  }
}
function paintPrepared() {
  $("offline-label").hidden = navigator.onLine;
  $("offline-state").textContent = offlineText();
  $("install-state").textContent = prepared.state === "ready" ? T.install_state_ok : offlineText();
  $("install-done").hidden = !standalone();
  $("install-guide").hidden = standalone();
  $("btn-install").hidden = prepared.state === "ready" && standalone();
}
let pollTimer = null;
async function refreshPrepared(poll = 0) {
  prepared = await checkPrepared();
  paintPrepared();
  clearTimeout(pollTimer);
  if (prepared.state === "incomplete" && navigator.onLine && poll < 15) {
    if (poll === 0) {
      const reg = await navigator.serviceWorker?.getRegistration?.();
      reg?.active?.postMessage({ type: "PRECACHE" }); // caché borrada con el SW activo
    }
    pollTimer = setTimeout(() => refreshPrepared(poll + 1), 2000); // descargando…
  }
}
addEventListener("online", () => { paintPrepared(); refreshPrepared(); });
addEventListener("offline", () => { paintPrepared(); refreshPrepared(); });

// ---------- instalación
addEventListener("beforeinstallprompt", (e) => {
  e.preventDefault();
  deferredInstall = e;
  $("btn-install-native").hidden = false;
  maybeInstallBanner();
});
addEventListener("appinstalled", () => { deferredInstall = null; $("install-banner").hidden = true; paintPrepared(); });
async function doInstall() {
  if (!deferredInstall) return go("install");
  deferredInstall.prompt();
  await deferredInstall.userChoice.catch(() => {});
  deferredInstall = null;
  $("btn-install-native").hidden = true;
  $("install-banner").hidden = true;
}
function maybeInstallBanner() {
  if (standalone() || !navigator.onLine) return;
  let until = 0;
  try { until = Number(localStorage.getItem("installDismissedUntil") || 0); } catch { /* sin almacenamiento */ }
  $("install-banner").hidden = Date.now() < until;
  $("btn-install-banner").textContent = deferredInstall ? "Instal·lar" : "Com fer-ho";
}

// ---------- actualización del SW (04 §5.9): no se activa a mitad de una verificación
let updating = false;
function showUpdate(worker) {
  $("update-text").textContent = T.update_pwa;
  $("btn-update-apply").textContent = "Actualitzar";
  $("update-banner").hidden = false;
  $("btn-update-apply").onclick = () => {
    updating = true;
    worker.postMessage({ type: "SKIP_WAITING" });
  };
}
// Otra pestaña ha activado una versión nueva: aquí el JS es el antiguo pero el worker (y sus claves)
// ya son los nuevos. Se recargan las claves YA (p. ej. una clave marcada «compromised») y la página
// se recarga sola solo en una pantalla sin nada en curso; si no, se avisa y decide el usuario.
let hadController = false;
async function onControllerChange() {
  if (updating) { location.reload(); return; }
  if (!hadController) { // primera instalación: el worker toma el control, no es una actualización
    hadController = true;
    await loadVersion();
    refreshPrepared();
    return;
  }
  await reloadKeys();
  refreshPrepared();
  const idle = current === "home" || current === "first";
  if (idle) { location.reload(); return; }
  $("update-text").textContent = T.update_other_tab;
  $("btn-update-apply").textContent = "Recarregar";
  $("update-banner").hidden = false;
  $("btn-update-apply").onclick = () => location.reload();
}
async function registerSw() {
  if (!("serviceWorker" in navigator) || !isSecureContext) return;
  hadController = !!navigator.serviceWorker.controller;
  try {
    swReg = await navigator.serviceWorker.register("sw.js");
  } catch { return; }
  navigator.serviceWorker.addEventListener("controllerchange", onControllerChange);
  if (swReg.waiting && navigator.serviceWorker.controller) showUpdate(swReg.waiting);
  swReg.addEventListener("updatefound", () => {
    const nw = swReg.installing;
    nw?.addEventListener("statechange", () => {
      if (nw.state === "installed" && navigator.serviceWorker.controller) showUpdate(nw);
      if (nw.state === "activated") refreshPrepared();
    });
  });
  if (navigator.onLine) swReg.update().catch(() => {});
  refreshPrepared();
}

// ---------- claus
function paintKeys() {
  $("keys-warning").hidden = store.keys.size > 0;
  const ids = [...store.keys.keys()].sort().join(", ") || "cap";
  $("foot-version").textContent = `Versió ${appVersion} · claus: ${ids}`;
}
async function reloadKeys() {
  await loadVersion();
  await store.refresh();
}
// La lista de claves se ha sustituido (p. ej. una clave ahora «compromised»). Lo que se muestre se
// calculó con la lista anterior: si hay un resultat en pantalla se repite con la nueva y se avisa;
// si no, se descarta para que «enrere» no pueda mostrar un resultat caducat.
async function onKeysChanged() {
  paintKeys();
  if (lastRaw === null) return;
  if (current === "result") {
    const raw = lastRaw;
    const res = await verifyCurrent(store, raw, verifyToken);
    if (current === "result" && lastRaw === raw) showResult(res, { replace: true, notice: T.keys_changed });
  } else {
    lastResult = null;
    lastRaw = null;
  }
}
async function loadVersion() {
  try {
    const info = await getControllerInfo(); // versión del worker que controla la página
    appVersion = info?.version || (await (await fetch("precache-manifest.json")).json()).version || "?";
  } catch { /* sin manifiesto */ }
}

// ---------- resultado
function dl(rows, big) {
  const d = el("dl", { class: "data" });
  for (const [k, v, isBig] of rows) {
    d.append(el("dt", {}, k), el("dd", { class: isBig && big ? "big" : "" }, v));
  }
  return d;
}
const fmtDate = (iat) => iat.split("-").reverse().join("/");

function showResult(res, { replace = false, notice = null } = {}) {
  lastResult = res;
  const cv = CODE_VIEW[res.code];
  const view = VIEWS[cv.view];
  const head = $("result-head");
  head.className = "verdict " + view.cls;
  $("result-icon").textContent = view.icon;
  $("result-word").textContent = view.word;
  $("result-sub").textContent = view.sub;
  head.setAttribute("aria-label", view.aria);
  const body = $("result-body");
  clear(body);
  if (notice) body.append(el("p", { class: "notice", role: "status" }, notice));

  if (res.code === "OK") {
    const f = res.fields;
    const rows = [
      [T.d_name, f.nom, true],
      [T.d_result, f.res],
      [T.d_exam, `reglament v${f.exv}`],
      [T.d_date, fmtDate(f.iat)],
      [T.d_code, res.shortCode],
      [T.d_orig, T.d_orig_value],
    ];
    if (f.bv) rows.push([T.d_bank, f.bv]);
    body.append(dl(rows, true));
    body.append(el("p", {}, T.compare_hint));
    body.append(
      el("div", { class: "checks" },
        el("label", {}, el("input", { type: "checkbox" }), T.check_name_doc),
        el("label", {}, el("input", { type: "checkbox" }), T.check_name_person)),
    );
    body.append(el("p", { class: "notice" }, el("span", { "aria-hidden": "true" }, "⚠ "), T.r_valid_warn));
    body.append(el("p", { class: "here" }, T.v_checked_here(appVersion)));
  } else {
    if (cv.reason) body.append(el("p", { class: "lead" }, cv.reason));
    if (!cv.replaceBody && view.body) body.append(el("p", {}, view.body));
    if (view.todo) {
      body.append(el("p", {}, "Què pots fer:"));
      body.append(el("ul", {}, ...view.todo.map((t) => el("li", {}, t))));
    }
    if (res.untrusted) body.append(untrustedBlock(res.untrusted));
    if (res.code === "UNKNOWN_VERSION") {
      body.append(el("button", { type: "button", class: "btn", id: "btn-update-check" }, T.btn_update_pwa));
      body.append(el("p", { id: "update-msg", role: "status", "aria-live": "polite" }));
    }
  }
  const again = el("button", { type: "button", class: "btn primary" }, res.code === "OK" ? T.btn_again : T.btn_retry_v);
  again.addEventListener("click", () => go("home"));
  body.append(el("div", { class: "actions" }, again));
  body.append(el("p", { class: "small addr" }, `Adreça que estàs fent servir: ${host}`));
  body.querySelector("#btn-update-check")?.addEventListener("click", checkUpdate);
  if (replace) render("result");
  else go("result");
}

function untrustedBlock(members) {
  const rows = [];
  const names = { nom: T.d_name, res: T.d_result, exv: T.d_exam, iat: T.d_date, cid: T.d_code, orig: T.d_orig, kid: "Clau" };
  for (const [k, label] of Object.entries(names)) {
    if (members.has(k)) rows.push([label, visible(members.get(k))]);
  }
  const d = el("details", { class: "untrusted" }, el("summary", {}, T.d_untrusted_btn));
  d.append(el("p", { class: "lead" }, T.d_untrusted + "."));
  d.append(dl(rows, false));
  return d;
}

async function checkUpdate() {
  const msg = $("update-msg");
  if (!swReg) { msg.textContent = T.update_unavailable; return; }
  if (!navigator.onLine) { msg.textContent = T.update_offline; return; }
  try {
    await swReg.update();
    if (swReg.waiting || swReg.installing) {
      msg.textContent = T.update_found;
      if (swReg.waiting) showUpdate(swReg.waiting);
    } else msg.textContent = T.update_none;
  } catch { msg.textContent = T.update_offline; }
}

async function process(raw) {
  lastRaw = raw;
  // Siempre con la lista vigente; si cambia durante la comprobación, se descarta y se repite.
  const res = await verifyCurrent(store, raw, verifyToken);
  if (lastRaw !== raw) return; // otra comprobación más reciente (o claves cambiadas sin resultat)
  showResult(res);
}

// ---------- V3 text
function updateCheckBtn() {
  const empty = $("token-input").value.length === 0;
  $("btn-check").disabled = empty;
  $("paste-hint").hidden = !empty;
  $("token-input").setAttribute("aria-describedby", empty ? "paste-hint" : "");
}
$("token-input").addEventListener("input", updateCheckBtn);
$("btn-check").addEventListener("click", () => process($("token-input").value));
$("btn-clip").addEventListener("click", async () => {
  $("clip-msg").textContent = "";
  try {
    const t = await navigator.clipboard.readText();
    $("token-input").value = t;
    updateCheckBtn();
    $("token-input").focus();
  } catch { $("clip-msg").textContent = T.clip_failed; }
});
$("btn-clip").hidden = !navigator.clipboard?.readText;

// ---------- V2 imatge
$("btn-pick").addEventListener("click", () => $("file-input").click());
$("file-input").addEventListener("change", async () => {
  const f = $("file-input").files?.[0];
  $("file-input").value = "";
  if (!f) return;
  $("image-fail").hidden = true;
  $("image-msg").textContent = T.img_reading;
  let text = null;
  try { text = await decodeImageFile(f); } catch { text = null; }
  if (text === null) {
    $("image-msg").textContent = `${T.img_noqr} ${T.img_whatsapp}`;
    $("image-fail").hidden = false;
    return;
  }
  $("image-msg").textContent = "";
  process(text);
});

// ---------- V1 càmera
function resetCameraScreen() {
  $("cam-rationale").hidden = false;
  $("cam-live").hidden = true;
  $("cam-error").hidden = true;
  $("btn-torch").hidden = true;
}
function camError(msg, retry) {
  $("cam-rationale").hidden = true;
  $("cam-live").hidden = true;
  $("cam-error").hidden = false;
  $("cam-error-text").textContent = msg;
  $("btn-cam-retry").hidden = !retry;
}
function onCameraFound(text) {
  camSession.release();
  camera = null;
  navigator.vibrate?.(60);
  process(text);
}
async function beginCamera() {
  $("cam-rationale").hidden = true;
  $("cam-error").hidden = true;
  $("cam-live").hidden = false;
  $("cam-hint").textContent = T.cam_hint;
  const r = await camSession.begin();
  if (r.status === "stale") return; // el usuario salió mientras se pedía el permiso: no se muestra nada
  if (current !== "camera") { camSession.cancel(); return; }
  if (r.status === "live") {
    camera = r.cam;
    $("btn-torch").hidden = !camera.canTorch;
    return;
  }
  camera = null;
  const e = r.error;
  if (e?.name === "NotAllowedError" || e?.name === "SecurityError") camError(T.cam_denied, false);
  else if (e?.name === "NotReadableError" || e?.name === "AbortError" || e?.name === "TrackStartError") camError(T.cam_busy, true);
  else camError(T.cam_unsupported, false);
}
function stopCamera() { camSession.cancel(); camera = null; }
$("btn-cam-continue").addEventListener("click", beginCamera);
$("btn-cam-retry").addEventListener("click", beginCamera);
$("btn-cam-cancel").addEventListener("click", () => history.back());
$("btn-torch").addEventListener("click", async () => { try { await camera?.toggleTorch(); } catch { $("btn-torch").hidden = true; } });
// App en segundo plano: se invalida cualquier arranque pendiente y se para el stream. Al volver se
// ofrece reintentar (nunca se reanuda sola una cámara que el usuario ya no tenía a la vista).
document.addEventListener("visibilitychange", () => {
  if (!document.hidden || current !== "camera") return;
  const wasBusy = camSession.busy;
  stopCamera();
  if (wasBusy) camError(T.cam_paused, true);
});

// ---------- sobre
function fillAbout() {
  $("about-version").textContent = `Versió del verificador: ${appVersion}.`;
  const ids = [...store.keys.entries()].map(([k, v]) => `${k} (${v.status === "active" ? "activa" : v.status === "retired" ? "retirada" : "no acceptada"})`);
  $("about-keys").textContent = `Claus de confiança: ${ids.join(", ") || "cap"}.` + (store.discarded.length ? ` ${T.keys_discarded(store.discarded.length)}` : "");
}

// ---------- cableado general
$("btn-camera").addEventListener("click", () => go("camera"));
$("btn-image").addEventListener("click", () => go("image"));
$("btn-text").addEventListener("click", () => go("text"));
$("btn-install").addEventListener("click", () => go("install"));
$("btn-about").addEventListener("click", () => go("about"));
$("btn-check-ready").addEventListener("click", async () => {
  $("install-state").textContent = T.install_state_checking;
  await refreshPrepared();
  swReg?.update().catch(() => {});
});
$("btn-install-native").addEventListener("click", doInstall);
$("btn-install-banner").addEventListener("click", doInstall);
$("btn-install-dismiss").addEventListener("click", () => {
  $("install-banner").hidden = true;
  try { localStorage.setItem("installDismissedUntil", String(Date.now() + 7 * 864e5)); } catch { /* ignore */ }
});
document.querySelectorAll("[data-back]").forEach((b) => b.addEventListener("click", () => history.back()));
document.querySelectorAll("[data-goto]").forEach((b) => b.addEventListener("click", () => go(b.dataset.goto)));
document.querySelectorAll("[data-host]").forEach((n) => { n.textContent = host; });
$("btn-first-ok").addEventListener("click", () => {
  try { localStorage.setItem("firstVisitDone", "1"); } catch { /* ignore */ }
  history.replaceState({ s: "home" }, "");
  render("home");
});

// ---------- arranque
reloadKeys();
let seen = false;
try { seen = localStorage.getItem("firstVisitDone") === "1"; } catch { /* ignore */ }
const first = !standalone() && !seen;
history.replaceState({ s: first ? "first" : "home" }, "");
render(first ? "first" : "home", { focus: false });
maybeInstallBanner();
registerSw();
