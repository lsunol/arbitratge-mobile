# Revisión de la flota para implementación

## Decisión

Se usará una flota mínima de **cinco agentes propios de implementación**, dos consultivos del
diseño y cuatro agentes integrados de pi. No se añade `test-engineer`: cada implementador
es responsable de sus propios tests y `reviewer` valida su presencia y alcance.

| Agente | Tier | Modelo · razonamiento | Herramientas/skills | Motivo |
|---|---|---|---|---|
| `android-developer` | T2 | `anthropic/claude-sonnet-5-5` · medium | write/bash; `mobile-android-design`, `edge-to-edge`, `compose-ui-testing-patterns`, `testing-setup` | Compose, Room, estados, reloj, feedback, PDF/QR y accesibilidad requieren contexto Android estable |
| `credential-engineer` | T1 | `anthropic/claude-opus-5-5` · high | write/bash; sin web | Un solo dueño para ARB1 evita divergencias entre Kotlin/JCA, WebCrypto y los vectores G1/G3/G5 |
| `pwa-developer` | T2 | `anthropic/claude-sonnet-5-5` · medium | write/bash; `frontend-design`, `chrome-devtools-axi` | Stack web/PWA con pruebas reales de cámara, caché, instalación y modo avión |
| `bank-curator` | T2 | `anthropic/claude-sonnet-5-5` · medium | write/bash, **sin herramientas web** | Contenido privado sensible; migración semántica y explicaciones en catalán. No imprime banco real |
| `release-engineer` | T2 | `anthropic/claude-sonnet-5-5` · medium | write/bash; `gh-axi` | Los workflows, secretos y releases pueden filtrar contenido; se eleva desde el Haiku documental |
| `trust-architect` | T1 consultivo | `anthropic/claude-opus-5-5` · high | solo diseño/lectura | Único agente que puede asesorar cambios aprobados de ARB1, claves o modelo de confianza |
| `ux-designer` | T2 consultivo | `anthropic/claude-sonnet-5-5` · medium | diseño/lectura | Comprueba la implementación contra UX, catalán y accesibilidad |

## Agentes integrados

| Agente | Modelo · razonamiento | Uso |
|---|---|---|
| `scout` | `anthropic/claude-haiku-5-5` · low | Reconocimiento barato y acotado |
| `worker` | `anthropic/claude-sonnet-5-5` · medium | Tareas genéricas, scripts o estructura sin dominio propio |
| `reviewer` | `github-copilot/gpt-6.1-terra` · high | Revisión independiente de otra familia que los implementadores |
| `oracle` | `anthropic/claude-opus-5-5` · high | Segunda opinión y decisiones complejas |

Se eliminan de la flota activa `product-analyst`, `mobile-architect` y `design-reviewer`.
Sus originales se conservan en `docs/agents-archive/` como historial y fuera del ámbito de
descubrimiento de pi. Sus copias generadas han sido retiradas por `sync-agents.mjs`.

## Reglas de ejecución

- Flujo: reconocimiento → implementador especializado → `reviewer` → corrección → tests.
- Un solo escritor por directorio; para fan-out usar worktrees o módulos separados.
- `credential-engineer` es el único propietario de ARB1. Los demás consumen su contrato.
- `bank-curator` es el único autorizado para leer el banco real y nunca reproduce contenido.
- Ningún secreto, token o keystore entra en git, logs o artifacts públicos.
- Si se propone cambiar un ADR o ARB1, parar y pedir aprobación al usuario.

## Validación

`node scripts/sync-agents.mjs` generó 7 agentes activos por harness.
`check-agents.mjs` revisó 28 definiciones y encontró **0 agentes sin modelo**.

## Coste relativo

- Alto y acotado: `credential-engineer`, `oracle` y `trust-architect`.
- Medio habitual: Android, PWA, banco, release, UX, `worker` y `researcher`.
- Bajo: reconocimiento con `scout`.
- Revisión independiente: cuota de Copilot con GPT-6.1 Terra. No hay fallback automático;
  cualquier cambio de modelo debe quedar registrado.

## Estado

Esta es la última tarea de diseño solicitada. El siguiente paso operativo es crear el
`.gitignore`, configurar el repositorio/remoto, inspeccionar el árbol publicable y realizar
el primer commit/push; no se ha realizado aquí.
