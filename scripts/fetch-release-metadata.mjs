#!/usr/bin/env node
// Genera el JSON de metadatos que `scripts/stamp-web.mjs --release-metadata` compara con sus argumentos:
// consulta la release PUBLICADA en GitHub (API pública, sin credenciales; GITHUB_TOKEN opcional solo para
// evitar límites), descarga el asset .apk y calcula su SHA-256 real. Vincula tag, estado, asset y hash.
//
// Uso: node scripts/fetch-release-metadata.mjs --tag v1.2.3 [--asset <fichero>.apk] --out release.json
// Sin dependencias (Node >= 20, fetch global). No lo ejecutan los tests (usan `buildMetadata` con datos simulados).
import { createHash } from "node:crypto";
import { writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";

export const RELEASE_REPO = "lsunol/arbitratge-mobile"; // = origin; mantener igual que scripts/stamp-web.mjs

const bad = (m) => { throw new Error("fetch-release-metadata: " + m); };

/** Valida la respuesta de la API y construye el JSON de metadatos (función pura, probada sin red). */
export function buildMetadata(api, { tag, assetName, sha256, size }) {
  if (!/^v(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$/.test(tag ?? "")) bad("tag debe ser vX.Y.Z final");
  if (!api || typeof api !== "object" || api.tag_name !== tag) bad("la release no corresponde al tag");
  if (api.draft !== false || api.prerelease !== false) bad("la release es borrador o prerelease");
  if (typeof api.published_at !== "string") bad("la release no está publicada");
  const prefix = `https://github.com/${RELEASE_REPO}/releases/download/${tag}/`;
  const assets = (api.assets ?? []).filter((a) => a && a.name === assetName);
  if (assets.length !== 1) bad(`debe haber exactamente un asset llamado ${assetName}`);
  const [a] = assets;
  if (a.browser_download_url !== prefix + assetName || !/^[\w.-]+\.apk$/.test(assetName)) bad("URL del asset inesperada");
  if (a.state !== undefined && a.state !== "uploaded") bad("el asset no está subido");
  if (!Number.isInteger(size) || size <= 0 || (a.size !== undefined && a.size !== size)) bad("el tamaño descargado no coincide con el de la API");
  if (!/^[0-9a-f]{64}$/.test(sha256)) bad("sha256");
  return {
    schema: 1, repo: RELEASE_REPO, tag, draft: false, prerelease: false,
    html_url: `https://github.com/${RELEASE_REPO}/releases/tag/${tag}`, published_at: api.published_at,
    asset: { name: assetName, browser_download_url: prefix + assetName, size, sha256 },
  };
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const args = Object.fromEntries(process.argv.slice(2).reduce((acc, x, i, a) => (x.startsWith("--") ? [...acc, [x.slice(2), a[i + 1]]] : acc), []));
  try {
    if (!args.tag || !args.out) bad("uso: --tag vX.Y.Z [--asset fichero.apk] --out release.json");
    const headers = { accept: "application/vnd.github+json", "user-agent": "arbitratge-web-stamp", ...(process.env.GITHUB_TOKEN ? { authorization: `Bearer ${process.env.GITHUB_TOKEN}` } : {}) };
    const r = await fetch(`https://api.github.com/repos/${RELEASE_REPO}/releases/tags/${args.tag}`, { headers });
    if (!r.ok) bad(`la API respondió ${r.status}`);
    const api = await r.json();
    const apks = (api.assets ?? []).filter((x) => /\.apk$/.test(x?.name ?? ""));
    const assetName = args.asset ?? (apks.length === 1 ? apks[0].name : bad("indica --asset: hay 0 o varios .apk"));
    const asset = (api.assets ?? []).find((x) => x.name === assetName) ?? bad("asset no encontrado");
    const d = await fetch(asset.browser_download_url, { headers: { "user-agent": "arbitratge-web-stamp" }, redirect: "follow" });
    if (!d.ok) bad(`la descarga respondió ${d.status}`);
    const h = createHash("sha256");
    let size = 0;
    for await (const chunk of d.body) { h.update(chunk); size += chunk.length; }
    writeFileSync(args.out, JSON.stringify(buildMetadata(api, { tag: args.tag, assetName, sha256: h.digest("hex"), size }), null, 2) + "\n");
    console.log(`Metadatos escritos en ${args.out}`);
  } catch (e) {
    console.error(e.message);
    process.exit(1);
  }
}
