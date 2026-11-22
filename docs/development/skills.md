# Skills de agentes en Arbitratge Mobile

Revisión: 2026-11-22. Fuente de verdad de qué skill usa cada agente: el frontmatter `skills:` de
`.agents/agents/*.md`. `node scripts/sync-agents.mjs` lo propaga a pi (frontmatter) y a **todos**
los harnesses (sección «Skills asignadas» con la ruta del `SKILL.md`), y avisa si alguna skill no
está instalada en la máquina.

## Dónde viven

| Ámbito | Original | Cómo lo ve cada harness |
|---|---|---|
| Proyecto | `.agents/skills/<nombre>/` (versionado, fijado en `skills-lock.json`) | `.claude/skills/` y `.pi/skills/` son enlaces (ignorados en git) creados con `node scripts/link-project-skills.mjs`; opencode y Copilot leen `.agents/skills/` / `.claude/skills/` |
| Usuario | `~/.agents/skills/<nombre>/` | junctions en `~/.pi/agent/skills/`, `~/.claude/skills/`, `~/.copilot/skills/` (ver `~/AGENTS.md`) |

Los subagentes de pi **no** leen `~/.agents/skills/` directamente: necesitan el junction en
`~/.pi/agent/skills/`.

## Catálogo y asignación

| Skill | Ámbito | Origen · evidencia | Agentes |
|---|---|---|---|
| `ui-ux-pro-max` | proyecto | nextlevelbuilder/ui-ux-pro-max-skill · 465K inst. · MIT (ver `skills-ui-ux-pro-max.md`) | android-developer, ui-designer, ux-designer, pwa-developer, reviewer (pi) |
| `ui-taste` | proyecto | uizze.sh · 1,1M inst. · Apache-2.0 · web/iOS; en Android solo playbooks genéricos; el MCP de pago es opcional y no se usa | android-developer, ui-designer, ux-designer, pwa-developer |
| `frontend-design` | proyecto (idéntica a la global) | anthropics/skills · 1,2M inst. · 199K ★ | ui-designer, pwa-developer |
| `mobile-android-design` | usuario | wshobson/agents · 27K inst. · 43K ★ | android-developer, ui-designer, ux-designer |
| `edge-to-edge` | usuario | android/skills (Google) · 4,1K inst. · 10,7K ★ | android-developer |
| `testing-setup` | usuario | android/skills (Google) · 3,4K inst. | android-developer |
| `compose-ui-testing-patterns` | usuario | chrisbanes/skills · 2,7K inst. | android-developer, reviewer (pi) |
| `chrome-devtools-axi` | usuario | kunchenguid/chrome-devtools-axi | pwa-developer |
| `gh-axi` | usuario | kunchenguid/gh-axi | release-engineer |
| `lluissunol-apk-signing` | usuario (propia) | `~/.agents/skills` | release-engineer, android-developer |
| `rulebook-exam-questions` | usuario (propia) | `~/.agents/skills` | bank-curator |
| `android-cli` | proyecto | android/skills (Google) · 3,8K inst. · 10,7K ★ · aprobada 2026-11-22; solo `SKILL.md`. Snyk: riesgo medio (instrucciones `curl … | bash`) | android-developer, release-engineer |
| `agp-9-upgrade` | usuario | android/skills | ninguno: el proyecto ya usa AGP 9.5.1 |

`credential-engineer` y `trust-architect` no tienen skills asignadas: no se encontró ninguna con
evidencia suficiente para ES256/WebCrypto/ARB1 (las búsquedas solo devuelven skills de CTF con
< 50 instalaciones).

Precedencia: AGENTS.md, ADR, identidad «Centre del camp» (`docs/brand/`) y especificaciones de
`ui-designer`/`ux-designer` mandan sobre las recomendaciones genéricas de cualquier skill.

## Descartadas

- `designed-by-ai/skills@design-mobile-apps` (1,6M inst., 40 ★): requiere `SLEEK_API_KEY` (servicio
  de pago sleek.design) y genera React Native/HTML, no Compose. Estaba en `skills-lock.json` sin
  instalar; se retiró del lock.
- `callstackincubator/agent-skills@github-actions` y `wshobson/agents@github-actions-templates`:
  React Native y Docker/Kubernetes genéricos; no aportan sobre los requisitos de `release-engineer`
  (SHA fijado, permisos mínimos, entornos protegidos).
- `android/skills@jetpack-compose-performance`, `navigation-3`, `adaptive`: sin necesidad actual.

## Candidatas no aprobadas (2026-11-22)

`android/skills@android-intent-security`, `android/skills@r8-analyzer` y
`addyosmani/web-quality-skills@accessibility`: propuestas y no aprobadas; no instalar sin nueva aprobación.

## `android-cli`: requisito y límites

La skill documenta el CLI `android` de Google. **Instalado el 2026-11-22** (v1.1.16919734, aprobado por el usuario) en `%USERPROFILE%\AppData\AndroidCLI/android.exe`, añadido al PATH de usuario; **no** se ejecutó `android init`. Su
instalador de Windows (`dl.google.com/android/cli/latest/windows_x86_64/install.cmd`) descarga
`android.exe` en `%USERPROFILE%\AppData\AndroidCLI`, lo añade al PATH de usuario y lo inicializa;
`android init` también instala «skills por defecto». Por eso los agentes no ejecutan `android init`/`android skills add` sin aprobación del usuario y usan
`--no-metrics`; en máquinas sin el CLI, usan `adb` y `emulator` del SDK. Si se instala, revisar dónde deja skills y moverlas a la
estructura de `~/AGENTS.md`.

## Máquina nueva

```bash
node scripts/link-project-skills.mjs   # enlaces de las skills del proyecto
node scripts/sync-agents.mjs           # regenera agentes y avisa de skills globales que falten
```

Las globales se instalan en `~/.agents/skills/` (p. ej. `npx skills add android/skills@edge-to-edge -g`,
moviendo el resultado a `~/.agents/skills/` si el CLI lo deja en un directorio de harness) y se
enlazan con junctions según `~/AGENTS.md`.
