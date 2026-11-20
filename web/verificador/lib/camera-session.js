// Ciclo de vida de la cámara: cada `begin()` tiene su propio AbortController (token de generación).
// `cancel()` (cambio de pantalla, app en segundo plano) invalida los arranques pendientes: si el
// permiso o el stream llegan después, se paran y no se muestra nada.
export class CameraSession {
  /** @param startFn ({signal}) => Promise<{stop(), …}> */
  constructor(startFn) {
    this.startFn = startFn;
    this.ctrl = null;
    this.active = null;
    this.pending = false;
  }

  /** @returns {Promise<{status:'live',cam}|{status:'error',error}|{status:'stale'}>} */
  async begin() {
    this.cancel();
    const ctrl = new AbortController();
    this.ctrl = ctrl;
    this.pending = true;
    try {
      const cam = await this.startFn({ signal: ctrl.signal });
      if (ctrl.signal.aborted) {
        cam?.stop?.();
        return { status: "stale" };
      }
      this.pending = false;
      this.active = cam;
      return { status: "live", cam };
    } catch (error) {
      if (ctrl.signal.aborted) return { status: "stale" };
      this.pending = false;
      return { status: "error", error };
    }
  }

  /** Hay un arranque en curso o una cámara abierta. */
  get busy() {
    return this.pending || this.active !== null;
  }

  cancel() {
    this.ctrl?.abort();
    this.ctrl = null;
    this.active?.stop?.();
    this.active = null;
    this.pending = false;
  }

  /** La cámara terminó por sí sola (QR encontrado): suelta la referencia sin parar nada más. */
  release() {
    this.active = null;
    this.ctrl = null;
  }
}
