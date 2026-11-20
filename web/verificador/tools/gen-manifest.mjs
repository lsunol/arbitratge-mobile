// Genera precache-manifest.json: lista de ficheros del despliegue con su SHA-256.
// Uso: node web/verificador/tools/gen-manifest.mjs [directorio]   (por defecto, web/verificador)
// La versión se lee de sw.js (const VERSION). Cambia VERSION cuando cambie cualquier fichero.
import { createHash } from "node:crypto";
import { readFileSync, readdirSync, statSync, writeFileSync } from "node:fs";
import { dirname, join, relative, sep } from "node:path";
import { fileURLToPath } from "node:url";

const dir = process.argv[2] ?? join(dirname(fileURLToPath(import.meta.url)), "..");
// Fuera del precache: el propio sw.js (lo gestiona el navegador), el manifiesto, tests, herramientas
// documentación/licencias y ficheros ocultos (.gitattributes…).
const EXCLUDE = [/(^|\/)\.[^/]+$/, /^sw\.js$/, /^precache-manifest\.json$/, /^test\//, /^tools\//, /(^|\/)README\.md$/, /^vendor\/LICENSE/];

export function buildManifest(root) {
  const sw = readFileSync(join(root, "sw.js"), "utf8");
  const m = /const VERSION = "([^"]+)";/.exec(sw);
  if (!m) throw new Error("sw.js no define VERSION");
  const files = [];
  const walk = (d) => {
    for (const name of readdirSync(d).sort()) {
      const p = join(d, name);
      if (statSync(p).isDirectory()) walk(p);
      else {
        const rel = relative(root, p).split(sep).join("/");
        if (EXCLUDE.some((re) => re.test(rel))) continue;
        files.push({ path: rel, sha256: createHash("sha256").update(readFileSync(p)).digest("hex") });
      }
    }
  };
  walk(root);
  return { version: m[1], cache: "verificador-" + m[1], files };
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const manifest = buildManifest(dir);
  writeFileSync(join(dir, "precache-manifest.json"), JSON.stringify(manifest, null, 2) + "\n");
  console.log(`precache-manifest.json: versión ${manifest.version}, ${manifest.files.length} ficheros`);
}
