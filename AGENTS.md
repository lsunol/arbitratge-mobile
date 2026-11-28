# AGENTS.md — Arbitratge Mobile (CFBB)

Fuente única de instrucciones para cualquier agente de IA (Claude Code, pi, opencode,
GitHub Copilot…). `CLAUDE.md` solo reenvía aquí.

## Qué es

App móvil (Android primero, distribuida como APK) para examinar a candidatos a árbitro
de **Futbol amb Botons** y, si aprueban, emitirles una **credencial verificable** por un
organizador. Más una **web mínima**: un HTML simple en GitHub Pages que sirve de punto de
entrada para pasar el enlace a los aspirantes y descargar el APK. Nada más.

- **Sin backend.** Todo el examen ocurre en el móvil. Hosting estático (GitHub Pages /
  GitHub Releases) solo para distribuir el APK.
- **Autoservicio y autocorrección:** no interviene un organizador durante el examen. La app
  corrige, muestra el número de fallos y feedback por pregunta. Una explicación breve del
  porqué puede añadirse como trabajo editorial posterior.
- **Perfil de seguridad proporcional:** no se persiguen garantías contra candidatos técnicos
  o dispositivos comprometidos. El riesgo relevante es uso casual/falsificación visual y
  errores, no ataques avanzados; la app puede contener el banco y la lógica de corrección en
  el APK. Esto **no autoriza** subir preguntas/respuestas al historial git público ni a una
  página HTTP legible; se mantienen fuera del repositorio público (secreto/artifact/segundo repo).
- **Verificador:** PWA pública, instalable y offline tras preparar su caché (ADR 0001). No hay
  app Android de organizadores en el MVP. La suplantación se acepta como riesgo social. La
  revocación y la sincronización que pueda requerir quedan para fase 2, si se justifican.
- **Proyecto base** (web + Supabase, misma finalidad):
  - Local: `C:\Users\lluis\Visual Studio Code Projects\arbitratge`
  - Repo: https://github.com/cfbbarcelona/arbitratge
  - Producción (antiguo; desde 2026-11-26 ese dominio es de este proyecto): https://arbitratge.futbolbotons.cat/
  - Reutilizable: banco de preguntas (`scripts/questions.sql`, migraciones en
    `supabase/migrations/`), tipos de pregunta (`single_choice`, `multiple_choice`,
    `true_false`), reglas de examen (`num_questions`, `time_limit_minutes`,
    `pass_threshold_count`, opciones barajadas) en `supabase/functions/_shared/examLogic.ts`.
- **Idioma:** UI y textos para el usuario en **catalán**. Documentación de diseño en
  castellano.
- El proyecto tendrá al menos un repositorio público para GitHub Pages/Actions. El banco
  de preguntas no debe quedar rastreable en su historial ni en archivos públicos.
- **Dominio (2026-11-26):** `arbitratge.futbolbotons.cat` ya apunta a ESTE proyecto (GitHub Pages de `lsunol/arbitratge-mobile`; web de descarga en `/`, verificador en `/verificador/`). Publicación: `pages.yml` (manual, tag + bank_version). El proyecto base ya no lo sirve.
- Ojo: el `AGENTS.md` del proyecto base dice "~10 candidats/mes, sense mesures
  anti-trampes". Es contexto de **aquel** proyecto, no un requisito de este.

## Fase actual: PREPARACIÓN DE IMPLEMENTACIÓN

La fase de diseño documental está cerrada en
[`docs/design/reviews/cierre-diseno.md`](docs/design/reviews/cierre-diseno.md). Las decisiones
vigentes están en `docs/adr/0001-*` y `0002-*`. No se cambia un ADR ni ARB1 sin aprobación
explícita del usuario.

Antes del primer push quedan por preparar `.gitignore`, repositorio remoto, estructura Android/
PWA/CI y el contenido publicable. Las pruebas [PV] indicadas en el diseño se ejecutan durante
la implementación; no son requisitos ya demostrados.

## Flota de agentes

Originales en `.agents/agents/*.md` (frontmatter portable + prompt). **No edites las
copias de cada harness**: edita el original y ejecuta

```bash
node scripts/sync-agents.mjs
```

que genera `.claude/agents/`, `.pi/agents/`, `.opencode/agent/` y `.github/agents/`.

| Agente | Modelo (pi) | Rol |
|---|---|---|
| `android-developer` | sonnet-5-5 · medium | App Kotlin/Compose: estados, Room, reloj, feedback, PDF/QR, accesibilidad y tests |
| `credential-engineer` | opus-5-5 · high | Propietario único de ARB1, Kotlin/JCA, WebCrypto y vectores G1/G3/G5 |
| `pwa-developer` | sonnet-5-5 · medium | PWA verificadora, web de descarga, offline, cámara, imagen y navegador |
| `bank-curator` | sonnet-5-5 · medium | Banco privado: migración, validación, explicaciones y muestra ficticia; sin fuga de contenido |
| `release-engineer` | sonnet-5-5 · medium | CI, firma, Actions, Releases, Pages, secretos y protección del repo público |
| `trust-architect` | opus-5-5 · high | Consultor de cambios al contrato o confianza; no implementa salvo aprobación |
| `ux-designer` | sonnet-5-5 · medium | Consultor de implementación frente a UX, catalán y accesibilidad |
| `ui-designer` | opus-5-5 · high | Consultor de diseño visual móvil (identidad, color, tipografía, componentes); propone, no implementa |

Los originales de diseño retirados quedan en `docs/agents-archive/` y no se cargan como
subagentes. Los agentes integrados de pi usan `.pi/settings.json`: `scout` Haiku/low,
`worker` Sonnet/medium, `reviewer` GPT-6.1 Terra/high (con `ui-ux-pro-max` y
`compose-ui-testing-patterns`) y `oracle` Opus/high.

**Skills:** cada agente declara las suyas en el frontmatter `skills:` del original y el sync las
propaga a todos los harnesses. Catálogo, evidencia, ubicación y descartes en
[`docs/development/skills.md`](docs/development/skills.md). Skills del proyecto en `.agents/skills/`
(enlaces con `node scripts/link-project-skills.mjs`); no se instala ninguna nueva sin aprobación.

Flujo habitual: scout/worker → implementador especializado → `reviewer` independiente →
corrección → tests. No cambies ARB1 o un ADR dentro de una tarea de implementación.

**Decisiones del usuario sobre UI (vinculantes):** [`docs/design/decisiones-usuario-ui.md`](docs/design/decisiones-usuario-ui.md). Prevalecen sobre cualquier spec de agente; cada una tiene comprobación mecánica y el `reviewer` da FAIL si falta. Se registran en el mismo turno en que el usuario las pide. Las capturas con el banco real nunca van al repo.

**Tareas que afectan a la UI (obligatorio):** (0) cargar la skill `ui-ux-pro-max`, leer
`references/pro-rules.md` y ejecutar búsquedas pertinentes al stack Jetpack Compose/UX; `ux-designer`,
`ui-designer` y `android-developer` la usan y registran las consultas; (1) `ux-designer` y `ui-designer`
revisan/especifican **en paralelo** ANTES de implementar y se acepta su resultado; (2) implementa
el implementador (`android-developer` o `pwa-developer`); (3) después `reviewer` independiente **y** QA visual
con capturas de dispositivo; (4) corrección y tests. La QA visual rutinaria se hace primero en emulador
(normal/130 %/200 %, claro y oscuro); el dispositivo físico queda para validación final. Ninguna
tarea de UI se da por cerrada sin las dos revisiones de diseño previas, el checklist de `ui-ux-pro-max` y
la QA posterior.

Checklist mínimo de `reviewer`: banco real/secretos fuera del repo y logs; ARB1 canónico;
persistencia antes de avanzar; catalán/accesibilidad; tests; permisos/secretos de CI; cambios
limitados al alcance. Si falla la cuota de Copilot, registrar un informe con el modelo usado.

Checklist adicional de `reviewer` para cambios de UI: (a) constan las revisiones previas de `ux-designer` y
`ui-designer`, y que `ui-ux-pro-max` se consultó con búsquedas registradas; (b) **ninguna afirmación de medidas o disposición (anchos 50/50, saltos de línea, 200 %/escala de
fuente, alineación, truncados, contraste real) es válida sin una captura de dispositivo** (o emulador) adjunta: los
tests de semántica Robolectric no demuestran el aspecto; si no hay captura, el informe lo marca «no verificado
visualmente» y no puede dar PASS en ese punto; (c) textos en catalán y contrato del banco intacto (los cambios de
etiqueta son solo de presentación); (d) TalkBack/lectores de pantalla quedan fuera de alcance (decisión del usuario, 2026-11-24): no se prueban ni se exigen; se mantienen contraste, tamaño táctil y escala de fuente.

## Convenciones

- Ningún secreto (keystores, claves privadas, contraseñas, tokens) en el repo. Nunca. La clave
de sello ARB1 existe en el APK final por ADR 0001, pero su material de build sigue fuera de git.
- Decisiones que afecten a varios documentos → ADR en `docs/adr/NNNN-titulo.md`.
- Un solo agente escribe en un directorio a la vez; para trabajo paralelo, usa worktrees o
  separa los módulos. Nunca dejes a dos subagentes editando el mismo árbol.
- `bank-curator` es el único agente autorizado para leer el banco real. Nunca imprime su
  contenido: solo recuentos, versiones e IDs técnicos. Para crear preguntas o explicaciones
  a partir del reglamento usa la skill global `rulebook-exam-questions` (trabajo en la
  carpeta privada del banco, nunca en este repo).
- Reglamento en `docs/reglament/<etiqueta>/` (público; la etiqueta es la fecha AAAA-MM-DD de la versión, vigente en `docs/reglament/CURRENT`; `reglament.md` + `rules.json` con unidades citables). La etiqueta es el valor de `rulebook_version` de las preguntas y del campo de reglamento de la app. Preguntas nuevas:
  comando `/genera-preguntes <N> [abast]` (original en `.agents/commands/`, sincronizado por
  `scripts/sync-agents.mjs`).
- **Navegación del examen (decisión del usuario, 2026-11-20):** el candidato puede volver a
  preguntas anteriores y cambiar respuestas hasta finalizar; sustituye el "secuencial sin
  retroceso" de `01` §1.3 / `03` §4.3. La persistencia antes de avanzar se mantiene.
- Paquete y `applicationId`: `dev.lluissunol.arbitratge` (decidido). La firma de release
  usa el keystore compartido `dev.lluissunol` (skill global `lluissunol-apk-signing`).
- **Actualizar el móvil (decisión del usuario, 2026-11-24):** cuando pida «actualiza el móvil», instala **siempre la build
  release de revisión, no la debug**. Procedimiento (todo fuera del repo, en `arbitratge-bank/work/`, hermano de este repo):
  `ARB_BANK_FILE=<work>/review-bank.json ARB_SEAL_KEY_FILE=<work>/review-seal.der ARB_SEAL_KID=review ./gradlew :app-candidate:assembleRelease --no-build-cache`,
  luego `zipalign -p 4` y `apksigner sign --ks <work>/review.jks --ks-pass file:<work>/ks.pass` → `<work>/arbitratge-review.apk`,
  verificar con `apksigner verify` e `adb -s <serial> install -r`. Nunca imprimir contraseñas ni contenido del banco. La debug
  (otra firma) da `INSTALL_FAILED_UPDATE_INCOMPATIBLE`; no desinstales sin permiso (borra los datos).
- Cada tarea termina con pruebas ejecutadas, riesgos/pendientes y rutas tocadas.
- **Nunca mates procesos por nombre** (`taskkill /IM node.exe`, `killall node`, `pkill node`): pi y
  opencode corren en Node y se mataría la sesión. Solo por PID (p. ej. `.capture-ui.pid` de
  `scripts/qa/capture-ui.mjs`). Con un móvil físico conectado, la QA apunta siempre a `emulator-5554`.
