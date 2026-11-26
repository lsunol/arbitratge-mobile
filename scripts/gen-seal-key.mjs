#!/usr/bin/env node
// Genera un par de claves de sello ARB1 (ECDSA P-256 / ES256).
//   · Privada  -> PKCS#8 DER, FUERA del repo (por defecto ~/.arbitratge-seal/<kid>.der).
//   · Pública  -> trusted-keys/<kid>.json (SPKI base64url + huella + estado). Esta sí va a git.
// Uso:  node scripts/gen-seal-key.mjs <kid> [--priv-dir <dir>] [--valid-from YYYY-MM-DD]
// Node >= 20, sin dependencias. Nunca sobrescribe una clave existente. No imprime la privada.
import { createHash, generateKeyPairSync } from "node:crypto";
import { chmodSync, existsSync, mkdirSync, writeFileSync } from "node:fs";
import { homedir } from "node:os";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const repo = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const args = process.argv.slice(2);
const kid = args[0];
const opt = (name, def) => { const i = args.indexOf(name); return i >= 0 ? args[i + 1] : def; };
const privDir = resolve(opt("--priv-dir", join(homedir(), ".arbitratge-seal")));
const validFrom = opt("--valid-from", new Date().toISOString().slice(0, 10));

const fail = (m) => { console.error("ERROR:", m); process.exit(1); };
if (!kid || !/^[a-z0-9]{1,16}$/.test(kid)) fail("kid obligatorio, ^[a-z0-9]{1,16}$ (p. ej. s1)");
if (/^(test|testx|dev)/.test(kid)) fail("kid de prueba no permitido (test, testx, dev)");
if (!/^\d{4}-\d{2}-\d{2}$/.test(validFrom)) fail("--valid-from debe ser YYYY-MM-DD");
if (resolve(privDir).startsWith(repo)) fail("la clave privada no puede vivir dentro del repo");

const privPath = join(privDir, `${kid}.der`);
const pubPath = join(repo, "trusted-keys", `${kid}.json`);
if (existsSync(privPath) || existsSync(pubPath)) fail(`ya existe ${kid} (no se sobrescribe; usa otro kid)`);

const { privateKey, publicKey } = generateKeyPairSync("ec", { namedCurve: "P-256" });
const pkcs8 = privateKey.export({ format: "der", type: "pkcs8" });
const spki = publicKey.export({ format: "der", type: "spki" });
const entry = {
  kid,
  alg: "ES256",
  spki: spki.toString("base64url"),
  sha256_spki: createHash("sha256").update(spki).digest("hex"),
  status: "active",
  valid_from: validFrom,
};

mkdirSync(privDir, { recursive: true });
writeFileSync(privPath, pkcs8, { flag: "wx", mode: 0o600 });
try { chmodSync(privPath, 0o600); } catch { /* Windows: se hereda la ACL del perfil de usuario */ }
writeFileSync(pubPath, JSON.stringify(entry, null, 2) + "\n", { flag: "wx" });

console.log(`Privada : ${privPath}  (PKCS#8 DER; NO subir a git, hacer copia de seguridad)`);
console.log(`Pública : ${pubPath}`);
console.log(`Huella SHA-256 SPKI: ${entry.sha256_spki}`);
