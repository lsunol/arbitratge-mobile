#!/usr/bin/env node
// Crea enlaces project-locales a las skills originales en .agents/skills/ sin copiar.
// Idempotente y usable desde cualquier harness desde la raíz del repo.
import fs from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
// Todas las skills originales del proyecto (cada subcarpeta de .agents/skills con SKILL.md).
const skills = fs.readdirSync(path.join(root, ".agents", "skills"), { withFileTypes: true })
  .filter((d) => d.isDirectory() && fs.existsSync(path.join(root, ".agents", "skills", d.name, "SKILL.md")))
  .map((d) => d.name)
  .sort();
const harnessDirs = [".claude/skills", ".pi/skills"];

function windowsJunction(link, target) {
  const escaped = (s) => s.replaceAll("'", "''");
  execFileSync("powershell.exe", [
    "-NoProfile", "-NonInteractive", "-Command",
    `$ErrorActionPreference='Stop'; New-Item -ItemType Junction -Path '${escaped(link)}' -Target '${escaped(target)}' | Out-Null`,
  ], { stdio: "inherit" });
}

for (const dir of harnessDirs) {
  fs.mkdirSync(path.join(root, dir), { recursive: true });
  for (const name of skills) {
    const target = path.join(root, ".agents", "skills", name);
    const link = path.join(root, dir, name);
    if (!fs.existsSync(target)) throw new Error(`Falta la skill original: ${target}`);
    // Solo sustituye enlaces/junctions: nunca borra una carpeta real (sería una copia a revisar).
    let st = null;
    try { st = fs.lstatSync(link); } catch {}
    if (st && !st.isSymbolicLink()) {
      const real = fs.realpathSync(link);
      if (real !== fs.realpathSync(target)) throw new Error(`${link} es una carpeta real, no un enlace: revísala a mano`);
    }
    if (st) {
      // Quita solo el enlace (unlink; en Windows un junction de directorio puede requerir rmdir).
      try { fs.unlinkSync(link); } catch { fs.rmdirSync(link); }
    }
    try {
      fs.symlinkSync(target, link, process.platform === "win32" ? "junction" : "dir");
    } catch (error) {
      if (process.platform !== "win32") throw error;
      windowsJunction(link, target);
    }
    console.log(`${path.relative(root, link)} -> ${path.relative(root, target)}`);
  }
}
