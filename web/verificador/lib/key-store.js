// Lista de claves vigente + generación. Garantías:
// - Solo aplica la carga MÁS RECIENTE: una carga antigua que termina tarde se descarta.
// - `generation` cambia cada vez que la lista se sustituye; las verificaciones en curso comparan
//   la generación y, si cambió (p. ej. una clave pasó a «compromised»), se repiten con la lista nueva.
export class KeyStore {
  /** @param load () => Promise<{keys:Map, discarded:string[]}> (puede lanzar) */
  constructor(load) {
    this.load = load;
    this.keys = new Map();
    this.discarded = [];
    this.generation = 0;
    this.seq = 0;
    this.pending = new Set();
    this.onChange = null;
  }

  refresh() {
    const mine = ++this.seq;
    const p = (async () => {
      let r;
      try {
        r = await this.load();
      } catch {
        r = { keys: new Map(), discarded: ["keys/index.json"] }; // sin lista fiable: nada se acepta
      }
      if (mine !== this.seq) return false; // llegó una carga más nueva: esta ya es obsoleta
      this.keys = r.keys;
      this.discarded = r.discarded;
      this.generation++;
      this.onChange?.();
      return true;
    })();
    this.pending.add(p);
    p.finally(() => this.pending.delete(p));
    return p;
  }

  /** Espera a que no quede ninguna carga en curso. */
  async ready() {
    while (this.pending.size) await Promise.all([...this.pending]);
  }
}

/**
 * Verifica con la lista vigente. Si la lista se sustituye (o hay una carga en curso) mientras se
 * verifica, el resultado se DESCARTA y se repite con la lista nueva.
 */
export async function verifyCurrent(store, raw, verify) {
  for (;;) {
    await store.ready();
    const gen = store.generation;
    const res = await verify(raw, store.keys);
    if (gen === store.generation && store.pending.size === 0) return res;
  }
}
