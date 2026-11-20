// Servidor estático para pruebas de navegador. NO es parte del despliegue.
//   node web/verificador/test/serve-test-build.mjs [--port 8080] [--prod] [--sw-version X.Y.Z]
// Por defecto sirve una COPIA de web/verificador con la lista de confianza de PRUEBA
// (test-vectors/trusted-keys-test.json → keys/test.json y keys/testx.json) y el manifiesto
// regenerado, para poder verificar tokens de prueba en el navegador. Con --prod sirve la
// carpeta tal cual (lista de producción, hoy vacía). La carpeta web/verificador no se modifica.
import { createServer } from "node:http";
import { cpSync, mkdtempSync, readFileSync, writeFileSync, rmSync, existsSync, statSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, extname, join, normalize } from "node:path";
import { fileURLToPath } from "node:url";
import { buildManifest } from "../tools/gen-manifest.mjs";

const here = dirname(fileURLToPath(import.meta.url));
const src = join(here, "..");
const MIME = {
  ".html": "text/html; charset=utf-8", ".js": "text/javascript; charset=utf-8", ".mjs": "text/javascript; charset=utf-8",
  ".css": "text/css; charset=utf-8", ".json": "application/json; charset=utf-8",
  ".webmanifest": "application/manifest+json", ".png": "image/png", ".svg": "image/svg+xml", ".txt": "text/plain; charset=utf-8",
};

export function prepareTestBuild({ version } = {}) {
  const dir = mkdtempSync(join(tmpdir(), "verificador-test-"));
  cpSync(src, dir, { recursive: true, filter: (p) => !/[\\/](test|tools)([\\/]|$)/.test(p.slice(src.length)) });
  const entries = JSON.parse(readFileSync(join(src, "..", "..", "test-vectors", "trusted-keys-test.json"), "utf8"));
  for (const e of entries) writeFileSync(join(dir, "keys", `${e.kid}.json`), JSON.stringify(e, null, 2) + "\n");
  writeFileSync(join(dir, "keys", "index.json"), JSON.stringify({ files: entries.map((e) => `${e.kid}.json`) }));
  if (version) {
    const sw = readFileSync(join(dir, "sw.js"), "utf8").replace(/const VERSION = "[^"]+";/, `const VERSION = "${version}";`);
    writeFileSync(join(dir, "sw.js"), sw);
  }
  writeFileSync(join(dir, "precache-manifest.json"), JSON.stringify(buildManifest(dir), null, 2) + "\n");
  return dir;
}

export function serve(root, port = 0) {
  const server = createServer((req, res) => {
    const u = new URL(req.url, "http://x");
    let p = normalize(decodeURIComponent(u.pathname)).replace(/^([\\/])+/, "");
    let f = join(root, p);
    if (!f.startsWith(root)) { res.writeHead(403).end(); return; }
    if (existsSync(f) && statSync(f).isDirectory()) f = join(f, "index.html");
    if (!existsSync(f)) { res.writeHead(404).end("not found"); return; }
    res.writeHead(200, { "Content-Type": MIME[extname(f)] ?? "application/octet-stream", "Cache-Control": "no-cache" });
    res.end(readFileSync(f));
  });
  return new Promise((ok) => server.listen(port, "127.0.0.1", () => ok(server)));
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const args = process.argv.slice(2);
  const port = Number(args[args.indexOf("--port") + 1]) || 8080;
  const prod = args.includes("--prod");
  const vi = args.indexOf("--sw-version");
  const root = prod ? src : prepareTestBuild({ version: vi >= 0 ? args[vi + 1] : undefined });
  const server = await serve(root, port);
  console.log(`${prod ? "PRODUCCIÓ" : "PROVA (claus de test)"}: http://localhost:${server.address().port}/  (${root})`);
  process.on("SIGINT", () => { if (!prod) rmSync(root, { recursive: true, force: true }); process.exit(0); });
}
