// Estado de la sesión del verificador (sin DOM, para poder probarlo):
//   loading → ready → stale
// - La lista de claves se carga UNA vez por carga de página; hasta entonces no se puede verificar.
// - `stale` (una actualización cambió el worker): se bloquea todo, se borra lo mostrado y cualquier
//   comprobación que termine después se descarta. Solo sale de `stale` recargando la página.
export class VerifierState {
  constructor() {
    this.phase = "loading";
    this.keys = new Map();
    this.discarded = [];
    this.lastRaw = null;
    this.lastResult = null;
    this.onChange = null;
    this.run = 0;
  }

  get canVerify() {
    return this.phase === "ready";
  }

  /** Fin de la carga inicial de claves. Ignorada si ya no estamos en `loading` (p. ej. `stale`). */
  setKeys(keys, discarded = []) {
    if (this.phase !== "loading") return false;
    this.keys = keys;
    this.discarded = discarded;
    this.phase = "ready";
    this.onChange?.(this.phase);
    return true;
  }

  /** Una actualización activó otro worker: bloquear y olvidar todo lo mostrado. */
  enterStale() {
    if (this.phase === "stale") return;
    this.phase = "stale";
    this.run++; // invalida lo que esté en vuelo
    this.keys = new Map();
    this.lastRaw = null;
    this.lastResult = null;
    this.onChange?.(this.phase);
  }

  /** @returns {Promise<{status:'ok',res}|{status:'blocked'}|{status:'discarded'}>} */
  async verify(raw, verifyFn) {
    if (!this.canVerify) return { status: "blocked" };
    const mine = ++this.run;
    const res = await verifyFn(raw, this.keys);
    if (this.phase !== "ready" || mine !== this.run) return { status: "discarded" };
    this.lastRaw = raw;
    this.lastResult = res;
    return { status: "ok", res };
  }
}
