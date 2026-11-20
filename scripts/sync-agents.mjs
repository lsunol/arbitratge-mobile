#!/usr/bin/env node
// Genera las definiciones de agentes de cada harness a partir de los originales
// portables en .agents/agents/*.md. Sin dependencias. Uso: node scripts/sync-agents.mjs
//
// Frontmatter portable admitido en los originales:
//   name:        identificador (kebab-case)
//   description: cuándo usar el agente (una línea)
//   access:      read-only | docs | write | private-write
//                private-write no carga herramientas web; sirve al banco privado
//   model:       provider/modelo explícito (obligatorio)
//   thinking:    off | minimal | low | medium | high | xhigh
//   skills:      lista CSV de skills ya instaladas; se propaga a pi
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const srcDir = path.join(root, ".agents", "agents");

const TOOLS = {
  claude: {
    "read-only": "Read, Grep, Glob, WebSearch, WebFetch",
    docs: "Read, Grep, Glob, WebSearch, WebFetch, Write, Edit",
    write: "Read, Grep, Glob, WebSearch, WebFetch, Write, Edit, Bash",
    "private-write": "Read, Grep, Glob, Write, Edit, Bash",
  },
  // web_search/fetch_content/get_search_content: pi-web-access, cargado en los hijos vía
  // subagents.defaultSubagentOnlyExtensions (settings de usuario).
  pi: {
    "read-only": "read, grep, find, ls, web_search, fetch_content, get_search_content",
    docs: "read, grep, find, ls, write, edit, web_search, fetch_content, get_search_content",
    write: "read, grep, find, ls, write, edit, bash, web_search, fetch_content, get_search_content",
    "private-write": "read, grep, find, ls, write, edit, bash",
  },
  copilot: {
    "read-only": ["read", "search", "web"],
    docs: ["read", "search", "web", "edit"],
    write: ["read", "search", "web", "edit", "execute"],
    "private-write": ["read", "search", "edit", "execute"],
  },
  opencode: {
    "read-only": { write: false, edit: false, bash: false },
    docs: { write: true, edit: true, bash: false },
    write: { write: true, edit: true, bash: true },
    "private-write": { write: true, edit: true, bash: true },
  },
};

function parse(file) {
  const raw = fs.readFileSync(file, "utf8").replace(/\r\n/g, "\n");
  const m = raw.match(/^---\n([\s\S]*?)\n---\n?([\s\S]*)$/);
  if (!m) throw new Error(`${file}: falta el frontmatter`);
  const meta = {};
  for (const line of m[1].split("\n")) {
    const kv = line.match(/^([\w-]+):\s*(.*)$/);
    if (kv) meta[kv[1]] = kv[2].trim();
  }
  for (const k of ["name", "description", "access", "model", "thinking"]) {
    if (!meta[k]) throw new Error(`${file}: falta '${k}' en el frontmatter`);
  }
  if (!TOOLS.claude[meta.access]) throw new Error(`${file}: access inválido '${meta.access}'`);
  return { meta, body: m[2].trimStart() };
}

const q = (s) => JSON.stringify(s); // YAML acepta cadenas JSON
const claudeModel = (model) => model.includes("claude-opus") ? "opus" : model.includes("claude-haiku") ? "haiku" : "sonnet";
const copilotModel = (model) => model.includes("claude-opus") ? "gpt-6.1-terra" : model.includes("claude-haiku") ? "claude-haiku-5.5" : model.includes("claude-sonnet") ? "claude-sonnet-5.5" : model.replace(/^github-copilot\//, "");
const HEADER = "<!-- GENERADO por scripts/sync-agents.mjs desde .agents/agents/. No editar. -->\n\n";

const targets = [
  {
    dir: ".claude/agents",
    file: (n) => `${n}.md`,
    fm: ({ name, description, access, model }) =>
      `name: ${name}\ndescription: ${q(description)}\ntools: ${TOOLS.claude[access]}\nmodel: ${claudeModel(model)}`,
  },
  {
    dir: ".pi/agents",
    file: (n) => `${n}.md`,
    fm: ({ name, description, access, model, thinking, skills }) =>
      `name: ${name}\ndescription: ${q(description)}\ntools: ${TOOLS.pi[access]}\nmodel: ${model}\nthinking: ${thinking}` +
      (skills ? `\nskills: ${skills}` : "") +
      `\ninheritProjectContext: true\ninheritSkills: true`,
  },
  {
    dir: ".opencode/agent",
    file: (n) => `${n}.md`,
    fm: ({ description, access, model }) =>
      `description: ${q(description)}\nmode: subagent\nmodel: ${model}\ntools:\n` + 
      Object.entries(TOOLS.opencode[access]).map(([k, v]) => `  ${k}: ${v}`).join("\n"),
  },
  {
    dir: ".github/agents",
    file: (n) => `${n}.agent.md`,
    fm: ({ name, description, access, model }) =>
      `name: ${name}\ndescription: ${q(description)}\ntools: ${q(TOOLS.copilot[access])}\nmodel: ${copilotModel(model)}`,
  },
];

const sources = fs.readdirSync(srcDir).filter((f) => f.endsWith(".md")).sort();
const agents = sources.map((f) => parse(path.join(srcDir, f)));

for (const t of targets) {
  const dir = path.join(root, t.dir);
  fs.mkdirSync(dir, { recursive: true });
  const expected = new Set(agents.map((a) => t.file(a.meta.name)));
  // Borra copias generadas de agentes que ya no existen (solo las que llevan la cabecera).
  for (const f of fs.readdirSync(dir)) {
    const p = path.join(dir, f);
    if (!expected.has(f) && fs.readFileSync(p, "utf8").includes("GENERADO por scripts/sync-agents.mjs")) {
      fs.unlinkSync(p);
    }
  }
  for (const a of agents) {
    fs.writeFileSync(path.join(dir, t.file(a.meta.name)), `---\n${t.fm(a.meta)}\n---\n${HEADER}${a.body}`);
  }
  console.log(`${t.dir}: ${agents.length} agentes`);
}
