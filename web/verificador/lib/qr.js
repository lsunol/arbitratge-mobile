// Lectura de QR: BarcodeDetector nativo si existe; si no (o no encuentra nada), jsQR vendido
// (vendor/jsQR.js, global `jsQR`). Todo local: ninguna imagen sale del dispositivo.

export const hasBarcodeDetector = () => typeof globalThis.BarcodeDetector === "function";

let detector = null;
async function nativeDetect(source) {
  if (!hasBarcodeDetector()) return null;
  try {
    detector ??= new globalThis.BarcodeDetector({ formats: ["qr_code"] });
    const found = await detector.detect(source);
    const raw = found?.[0]?.rawValue;
    return typeof raw === "string" && raw.length > 0 ? raw : null;
  } catch {
    detector = null;
    return null;
  }
}

function jsqrDecode(imageData) {
  const fn = globalThis.jsQR;
  if (typeof fn !== "function") return null;
  const r = fn(imageData.data, imageData.width, imageData.height, { inversionAttempts: "attemptBoth" });
  return r && typeof r.data === "string" && r.data.length > 0 ? r.data : null;
}

/** Lee un QR de una imagen (File/Blob). Devuelve el texto o null si no hay QR. Lanza si no es imagen. */
export async function decodeImageFile(file) {
  const bmp = await loadBitmap(file);
  try {
    const native = await nativeDetect(bmp);
    if (native) return native;
    const w0 = bmp.width;
    const h0 = bmp.height;
    const maxSide = Math.max(w0, h0);
    // jsQR es lento y frágil con fotos enormes: se prueban varios tamaños.
    const sides = [...new Set([Math.min(maxSide, 1600), Math.min(maxSide, 1024), Math.min(maxSide, 640)])];
    const canvas = document.createElement("canvas");
    const ctx = canvas.getContext("2d", { willReadFrequently: true });
    for (const side of sides) {
      const k = side / maxSide;
      canvas.width = Math.max(1, Math.round(w0 * k));
      canvas.height = Math.max(1, Math.round(h0 * k));
      ctx.fillStyle = "#fff"; // transparencias (PNG) sobre blanco
      ctx.fillRect(0, 0, canvas.width, canvas.height);
      ctx.drawImage(bmp, 0, 0, canvas.width, canvas.height);
      const text = jsqrDecode(ctx.getImageData(0, 0, canvas.width, canvas.height));
      if (text) return text;
      await new Promise((r) => setTimeout(r, 0)); // deja respirar la UI
    }
    return null;
  } finally {
    bmp.close?.();
  }
}

async function loadBitmap(file) {
  if (typeof createImageBitmap === "function") {
    try {
      return await createImageBitmap(file);
    } catch {
      /* cae al <img> */
    }
  }
  const url = URL.createObjectURL(file);
  try {
    const img = new Image();
    img.decoding = "async";
    img.src = url;
    await img.decode();
    return img; // HTMLImageElement ya decodificado; width/height = tamaño natural al no estar en el DOM
  } finally {
    URL.revokeObjectURL(url);
  }
}

/**
 * Escáner de cámara. `onFound(text)` se llama una vez. Devuelve {stop, canTorch, toggleTorch}.
 * Lanza el error de getUserMedia (NotAllowedError, NotFoundError, NotReadableError…).
 * `signal` (AbortSignal): si se aborta mientras el permiso está pendiente o el stream arranca,
 * se paran las pistas que se concedan después y se lanza un error `AbortError`.
 * `mediaDevices` y `createCanvas` son inyectables para pruebas.
 */
export async function startCamera(video, onFound, opts = {}) {
  const { onNoQr, noQrMs = 10000, signal, mediaDevices = globalThis.navigator?.mediaDevices } = opts;
  const createCanvas = opts.createCanvas ?? (() => document.createElement("canvas"));
  const aborted = () => {
    const e = new Error("aborted");
    e.name = "AbortError";
    return e;
  };
  if (signal?.aborted) throw aborted();
  if (!mediaDevices?.getUserMedia) {
    const e = new Error("unsupported");
    e.name = "NotSupportedError";
    throw e;
  }
  const stream = await mediaDevices.getUserMedia({
    audio: false,
    video: { facingMode: { ideal: "environment" }, width: { ideal: 1280 }, height: { ideal: 720 } },
  });
  // Limpieza propia de ESTE arranque, registrada nada más obtener el stream: así una cancelación
  // mientras video.play() sigue pendiente ya lo para. Solo se vacía el <video> si todavía apunta a
  // este stream (otro arranque posterior puede estar usándolo).
  let stopped = false;
  let timer = null;
  const release = () => {
    for (const tr of stream.getTracks()) tr.stop();
    if (video.srcObject === stream) video.srcObject = null;
  };
  function stop() {
    if (stopped) return;
    stopped = true;
    if (timer !== null) clearInterval(timer);
    release();
  }
  if (signal?.aborted) {
    stop();
    throw aborted();
  }
  signal?.addEventListener("abort", stop, { once: true });
  const abortSignalled = new Promise((res) => signal?.addEventListener("abort", res, { once: true }));
  video.srcObject = stream;
  video.setAttribute("playsinline", "");
  video.muted = true;
  await Promise.race([Promise.resolve(video.play()).catch(() => {}), abortSignalled]);
  if (signal?.aborted) {
    stop();
    throw aborted();
  }
  const canvas = createCanvas();
  const ctx = canvas.getContext("2d", { willReadFrequently: true });
  let busy = false;
  const t0 = Date.now();
  let warned = false;
  const tick = async () => {
    if (stopped || busy) return;
    if (video.readyState < 2 || !video.videoWidth) return;
    busy = true;
    try {
      let text = await nativeDetect(video);
      if (!text) {
        const k = Math.min(1, 800 / video.videoWidth);
        canvas.width = Math.round(video.videoWidth * k);
        canvas.height = Math.round(video.videoHeight * k);
        ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
        text = jsqrDecode(ctx.getImageData(0, 0, canvas.width, canvas.height));
      }
      if (text && !stopped) {
        stop();
        onFound(text);
        return;
      }
      if (!warned && onNoQr && Date.now() - t0 > noQrMs) {
        warned = true;
        onNoQr();
      }
    } finally {
      busy = false;
    }
  };
  timer = setInterval(tick, 200);
  const track = stream.getVideoTracks()[0];
  const canTorch = !!track?.getCapabilities?.().torch;
  let torchOn = false;
  return {
    stop,
    canTorch,
    async toggleTorch() {
      torchOn = !torchOn;
      await track.applyConstraints({ advanced: [{ torch: torchOn }] });
      return torchOn;
    },
  };
}
