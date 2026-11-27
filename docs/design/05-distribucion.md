# 05 — Distribución: repos, CI, firma del APK, publicación y verificador

> Autor: `release-engineer` · **Revisión 3 (cierre de diseño)**: alineada con el contrato canónico `ARB1` de `02` §3.7 (rev. 4), con la puerta de test G5 (cadena Android firma → PWA verifica) y sin referencias a la pregunta real de `03` (retiradas). · Estado: pendiente de la revisión final de `design-reviewer` y de las preguntas abiertas del usuario. **Rev. 3.1** (correcciones del orquestador, 2026-11-20): G1 excluye F02 de `BAD_FORMAT` y separa R01/K02 (solo release, G3); se retira la ruta directa a un fichero del banco y los avisos R-17 y pregunta 18; estado del repo local según la auditoría (sin commits).
> Sustituye a la rev. 2, cuyas incidencias de `reviews/revision-final-diseno.md` ya están incorporadas. Fase de diseño: **no hay código ni workflows implementados**; los
> ficheros YAML/JSON de este documento son esbozos de contrato, no artefactos.
>
> Entradas: `01-requisitos.md` (rev. 2), `02-credencial-y-amenazas.md` (rev. 4; contrato `ARB1` §3.7 canónico),
> `03-arquitectura-movil.md` (rev. 3), ADR 0001 (credencial PDF+QR firmada, verificador PWA
> estático, sin app de organizadores en MVP) y ADR 0002 (banco fuera del repo público e
> incluido en el APK), más `reviews/revision-completa-01-03-seguridad-proporcional.md` y `reviews/revision-final-diseno.md`.
>
> Convención: **[H]** hecho verificado (fichero:línea o URL) · **[D]** decisión aprobada ·
> **[S]** recomendación o suposición propia · **[PV]** pendiente de verificar al implementar ·
> **[ADR-n]** decisión ya cerrada en un ADR.

---

## 0. Resumen ejecutivo

1. **Dos repositorios en la org `cfbbarcelona`**: `arbitratge-mobile` **público** (código,
   metadatos, banco ficticio, claves públicas, web de descarga y PWA verificadora) y
   `arbitratge-bank` **privado** (banco real). Ningún banco real, clave privada ni keystore
   entra en el público ni en su historial [ADR-0002].
2. **Pipeline en GitHub Actions** con tres flujos: `ci` (sin secretos, válido también para PRs
   de forks), `release` (solo desde tag `vX.Y.Z`, en un *environment* protegido) y `pages`
   (publica web, PWA y `latest.json`). Artefactos: APK firmado con nombre estable, `SHA256SUMS`,
   SBOM y atestación de procedencia.
3. **Dos claves distintas que no deben confundirse**: (a) el **keystore de firma del APK**
   (identidad del editor ante Android; **nunca** dentro del APK) y (b) la **clave privada del
   sello de credenciales** (ECDSA P-256, **sí** dentro del APK candidato y extraíble, según
   [ADR-0001]). El digest que se publica y se fija en el pipeline es el del keystore; la
   verificación de credenciales usa la huella de la **clave pública** del sello (`kid`; contrato y puertas en §5.8).
4. **Publicación**: GitHub Releases (enlace a la versión concreta `releases/download/vX.Y.Z/…`, alias `latest` secundario) + GitHub Pages
   en un subdominio propuesto **`arbitratge.futbolbotons.cat`** (una sola web: descarga en
   `/` y verificador PWA en `/verificador/`). `latest.json` estático para avisos de versión, sin
   URL de APK ni datos sensibles.
5. **Coste**: todo en planes gratuitos previsiblemente (repos públicos, Releases, Pages,
   Actions en repo público). Riesgos reales: límites de Actions del repo privado si se usa la
   vía de CI con banco privado, límites soft de Pages, dependencia de una sola persona y de la
   verificación de desarrolladores de Android (2027) [H/PV, §8].
6. **Lo más urgente antes del primer push público** (§1.3): crear `.gitignore` (el repo no lo
   tiene), pasar la puerta de contenido (§1.3, B-3) sobre todo `docs/` y verificar que no hay
   historial con banco. El ejemplo de `03` §5.2 ya es ficticio (verificado); no queda acción sobre `03`.
7. **Fase 1 recomendada**: firma y banco **en local** del mantenedor con un script Node sin
   dependencias (misma secuencia que el pipeline), y pasar a firma en CI cuando el
   *environment* `release` tenga dos personas, revisión y reglas de tags (§4.8). Decisión del
   usuario (preguntas abiertas, nº 3).
8. **Versionado**: SemVer para la app, `versionCode` calculado monótono que distingue RC y
   versión final, `bank_version` `AAAA.MM.N` independiente y `exam_version` (la que va en la
   credencial) que solo sube cuando cambian reglas o significado de la credencial (§7).

---

## 1. Hechos de partida y restricciones

### 1.1 Hechos verificados

| # | Hecho | Fuente |
|---|---|---|
| H1 | El repo base `cfbbarcelona/arbitratge` es **público** (`private: false`) y su rama `main` contiene material del banco de preguntas (ruta no citada aquí). | [H] `docs/design/02` §5.2, verificado por `trust-architect` vía API de GitHub |
| H2 | El dominio del proyecto base es `arbitratge.futbolbotons.cat`; su `CNAME` contiene solo ese host. El base despliega en GitHub Pages desde `main` y **no tiene CI** (despliegue manual). | [H] `arbitratge/CNAME`; `arbitratge/AGENTS.md:135-136`, `:155-156`, `:17` |
| H3 | El `.gitignore` del base ignora ficheros de banco por nombre (línea 6) y no sirve tal cual: el de `arbitratge-mobile` lo sustituye (§2.4). | [H] `arbitratge/.gitignore:6` |
| H4 | El repo de arbitratge-mobile **no tiene `.gitignore`** en la raíz (listado: `.agents/ .claude/ .git/ .github/ .opencode/ .pi/ AGENTS.md CLAUDE.md docs/ scripts/`). | [H] `ls` de `arbitratge-mobile/` |
| H5 | `.git/config` no define remoto (`[remote …]` ausente) y no existe `.git/logs/HEAD`: **no hay commits ni remoto** en el repo local. Auditoría del orquestador (2026-11-20): `git log --all` no devuelve commits y todos los ficheros están *untracked*; no hay historial previo que reescribir. Esto no sustituye la inspección antes del primer push. | [H] `arbitratge-mobile/.git/config`; búsqueda de `.git/logs/HEAD` sin resultado |
| H6 | El ejemplo JSON de `03` §5.2 es **ficticio** (prefijo `[FICTICI]`, IDs de muestra). La referencia obsoleta a una pregunta real en ese documento queda retirada. No queda acción pendiente sobre `03`; la puerta B-3 se aplica a `docs/` en general. | [H] `03-arquitectura-movil.md` §5.2; `reviews/verificacion-correcciones-finales.md` B1 |
| H7 | Los artifacts de Actions de un repo público los puede descargar cualquiera con acceso de lectura; los secrets no se pasan a workflows de forks; `pull_request_target` con checkout del PR da acceso a secrets. | [H] `docs/design/02` §5.3 (docs.github.com citados allí) |
| H8 | Un secret de Actions tiene límite de **48 KB**. | [H] `docs/design/02` §5.3 |
| H9 | **Releases inmutables**: se pueden activar por repositorio; con la opción activa, el tag y los assets de una release publicada no pueden cambiarse; la recomendación oficial es crear la release como *draft*, adjuntar assets y publicar después. | [H] <https://docs.github.com/en/code-security/concepts/supply-chain-security/immutable-releases> y <https://docs.github.com/en/code-security/how-tos/secure-your-supply-chain/establish-provenance-and-integrity/prevent-release-changes> |
| H10 | Enlace estable a un asset de la última release: `https://github.com/{owner}/{repo}/releases/latest/download/{nombre-del-asset}`. La "última" excluye prereleases y drafts. | [H] <https://docs.github.com/en/repositories/releasing-projects-on-github/linking-to-releases> |
| H11 | Un dominio personalizado verificado para una organización solo puede publicar Pages desde repos de esa organización; la verificación evita *takeover* de subdominios. | [H] <https://docs.github.com/en/pages/configuring-a-custom-domain-for-your-github-pages-site/about-custom-domains-and-github-pages> y página de verificación de dominio de GitHub Pages |
| H12 | `apksigner rotate` crea un *lineage* de certificados; APK Signature Scheme v3 soporta rotación y su soporte de rotación arranca en Android 9 (API 28). | [H] <https://developer.android.com/tools/apksigner>; <https://source.android.com/docs/security/features/apksigning/v3> |
| H13 | `actions/attest-build-provenance` genera atestaciones SLSA de procedencia para artefactos de un workflow; en repos públicos la atestación queda en el registro público de Sigstore. | [H] <https://github.com/actions/attest-build-provenance/> y docs de Actions sobre *artifact attestations* [PV: confirmar detalle del registro al implementar] |

### 1.2 Restricciones que este documento respeta

- **Sin backend** y **solo hosting estático** (GitHub Pages / Releases) [`AGENTS.md`].
- **Banco fuera del repo público e inyectado al build**; queda dentro del APK [ADR-0002].
- **Credencial**: PDF+QR con sello ECDSA P-256 cuya privada está en el APK (extraíble); el
  verificador es PWA estática pública, sin secretos y offline [ADR-0001].
- **Sin segunda app nativa en el MVP** [ADR-0001]: el verificador es únicamente la PWA de
  `web/verificador/`. No hay `app-verifier` ni app de organizadores; una app nativa futura exigiría un
  ADR nuevo y no contendría banco ni keystore.
- **Seguridad proporcional** [D]: no se resiste a un técnico que extraiga el APK; sí hay que
  impedir fugas por error o por CI mal configurado.

### 1.3 Bloqueantes previos al primer push público (checklist)

| # | Acción | Motivo |
|---|---|---|
| B-1 | Crear `.gitignore` antes de cualquier `git add` (§2.4). | H4 |
| B-2 | **Ejemplo de `03` §5.2: ya ficticio** (verificado, H6). Sin acción sobre `03`; B-3 lo re-comprueba antes del push. | H6 |
| B-3 | **Puerta de contenido (bloqueante)**: ningún documento de `docs/`, ejemplo, vector ni workflow puede contener enunciados, opciones o respuestas identificables del banco real. El mantenedor la verifica antes del push con el canary (§3.4) y una revisión manual de `docs/` y de los ejemplos. Este documento solo cita IDs y nombres de ficheros. | Ejemplos o documentos futuros, incluida inspección editorial antes del primer push |
| B-4 | Confirmar con `git log --all` y `git count-objects` que el historial está vacío o limpio. **Estado (H5, auditoría 2026-11-20)**: sin commits y ficheros *untracked*; no hay historial que reescribir. Repetir la comprobación justo antes del primer push. Si hubiera commits con banco, **reescribir antes de publicar** (y asumir que el historial del base ya es público, H1). | ADR-0002, controles de entrega |
| B-5 | Ejecutar escaneo de secretos (`gitleaks` o equivalente) sobre el árbol y el historial. | §4.2 |
| B-6 | Decidir la licencia del código público (preguntas abiertas, nº 6). Sin licencia el código no es reutilizable legalmente. | Publicación de código |

---

## 2. Estructura del repo y estrategia de ramas/tags

### 2.1 Repositorios

| Repo | Visibilidad | Contenido | Quién accede | Motivo |
|---|---|---|---|---|
| `cfbbarcelona/arbitratge-mobile` | **Público** | App candidato, `core-*`, `android-common`, `bank-sample/` (ficticio), `trusted-keys/` (públicas), `test-vectors/`, `web/` (descarga + PWA), `scripts/`, `docs/`, workflows | Público | Pages y Releases lo exigen; [ADR-0002] permite código público |
| `cfbbarcelona/arbitratge-bank` | **Privado** | `bank/bank.json` (banco real con respuestas y explicaciones), migración SQL→JSON de un solo uso, validador y *canary* | Mantenedores | Fuente de verdad del banco [ADR-0002] |
| *(opcional)* carpeta local `%USERPROFILE%\arbitratge-bank\` con `git init` | Local | Igual que el privado, sin remoto | Mantenedor | Vía L de `03` §5.4; alternativa válida en fase 1 |

**Motivo de la separación frente a "monorepo con carpeta privada"**: una carpeta privada dentro
de un repo público no existe (todo el árbol es público). Por eso el banco vive en otro repo o
fuera de git.

**Alternativas descartadas:** (i) un solo repo privado para todo + distribución por Releases de
un repo público aparte: duplica el pipeline sin ventaja, y Pages desde un repo privado requiere
plan de pago [PV]; (ii) monorepo con el banco cifrado (02 B3): descartado: al
exponer el cifrado en el historial público.

### 2.2 Árbol del repo público `arbitratge-mobile`

```
arbitratge-mobile/                         PÚBLICO
├─ .github/
│  ├─ workflows/ci.yml                     PR y push: sin secretos
│  ├─ workflows/release.yml                tag v*.*.*: environment "release"
│  ├─ workflows/pages.yml                  publica web/ + latest.json
│  ├─ workflows/security.yml               gitleaks programado + canary (solo con secreto)
│  ├─ CODEOWNERS                           .github/, scripts/, trusted-keys/, test-vectors/, web/verificador/, docs/adr/
│  ├─ dependabot.yml                       actualiza actions (pinned por SHA) y Gradle
│  └─ pull_request_template.md
├─ app-candidate/                          APK público (examen + credencial)
├─ (sin app nativa verificadora en el MVP; ADR-0001)
├─ core-exam/  core-credential/            JVM puro, sin android.*
├─ android-common/
├─ bank-sample/bank-sample.json            banco FICTICIO (sample=true)
├─ trusted-keys/s1.json …                  claves PÚBLICAS del sello por kid
├─ test-vectors/                           tokens de prueba (kid=test, rechazados en release)
├─ web/
│  ├─ index.html                           página de descarga (sin JS)
│  ├─ privacitat.html                      aviso de privacidad (catalán)
│  ├─ verificador/                         PWA estática (ver §6.7)
│  ├─ CNAME                                arbitratge.futbolbotons.cat
│  └─ .nojekyll
├─ scripts/
│  ├─ release.mjs                          secuencia de release local (Node, sin dependencias)
│  ├─ validate-bank.mjs                    validador del esquema (también en CI)
│  ├─ check-public-tree.mjs                escaneo: sin PEM/PKCS#8/keystores/canary
│  └─ latest-json.mjs                      genera latest.json
├─ docs/
│  ├─ design/  adr/                        documentos y decisiones
│  ├─ release-runbook.md                   procedimiento paso a paso
│  └─ SIGNING.md                           SOLO digests públicos y procedimiento (sin contraseñas)
├─ bank.lock                               bank_version y commit del repo privado (sin hashes de contenido)
├─ .gitignore  README.md  SECURITY.md  LICENSE(pendiente, pregunta abierta 6)
```

### 2.3 Árbol del repo privado `arbitratge-bank`

```
arbitratge-bank/                           PRIVADO
├─ bank/bank.json                          banco real (schema 1)
├─ bank/README.md                          formato y reglas editoriales (sin contenido)
├─ migration/questions-sql-to-json.mjs     un solo uso (no forma parte del build)
├─ checks/canary.txt → NO SE VERSIONA      el canary vive como secreto (§3.4), no en el repo
└─ .github/workflows/validate.yml          valida esquema e IDs en push; sin artifacts, sin secretos
```

### 2.4 Qué NUNCA entra en el repo público (lista de control)

- Banco real, explicaciones reales, `bank.dat` de release, cualquier JSON de preguntas con
  `sample: false`.
- Claves privadas del sello (`*.pk8`, PEM `-----BEGIN PRIVATE KEY-----`), keystores (`*.jks`,
  `*.keystore`, `*.p12`, `*.pfx`), contraseñas, tokens, deploy keys privadas.
- Logs o capturas de CI con contenido del banco.
- Fotos o datos de candidatos (tampoco en `test-vectors/`: los tokens de prueba usan nombres
  ficticios).

`.gitignore` propuesto (bloqueante B-1):

```gitignore
# Secretos y material de firma
*.jks
*.keystore
*.p12
*.pfx
*.pk8
*.pem
*.key
keys/private/
local.properties
.env
.env.*

# Banco real: solo se versiona la muestra
bank/
bank-*.json
!bank-sample/bank-sample.json

# Salidas de build y configuraciones locales de agentes
build/
.gradle/
**/build/
.claude/settings.local.json
.pi/**/local*
.opencode/**/local*
```

Además, **escaneo de árbol** en CI (`scripts/check-public-tree.mjs`) que falla si encuentra
cabeceras PEM/PKCS#8, nombres de keystores o la cadena *canary* (§3.4), y **gitleaks** en el
historial (§4.2).

### 2.5 Ramas, PR, tags y prereleases

| Elemento | Regla | Ejemplo |
|---|---|---|
| Rama principal | `main`, protegida: PR obligatorio, checks `ci` en verde, sin *force push* ni borrado | — |
| Ramas de trabajo | `feat/*`, `fix/*`, `docs/*`, `bank/*` (solo en el repo privado) | `feat/feedback-resultat` |
| Integración | PR con *squash merge* (historial lineal) | — |
| Release | Tag anotado `vX.Y.Z` **sobre un commit de `main`** | `v1.3.0` |
| Prerelease | Tag `vX.Y.Z-rc.N` → GitHub *prerelease* **pública** (no es «latest»). Lleva solo banco de muestra y sello de prueba; **nunca** banco real ni clave de producción (§4.3, §10) | `v1.3.0-rc.1` |
| Hotfix | Rama `release/X.Y` solo si hay que parchear una versión anterior; normalmente no | — |
| Tags del banco | En el repo privado: `bank-AAAA.MM.N` | `bank-2027.01.0` |

**Reglas de tags** (ruleset de GitHub): crear y mover tags `v*` solo para mantenedores; prohibir
borrado y actualización de tags `v*` publicados. Una release publicada con inmutabilidad (H9) no
se puede rehacer: el arreglo es **una versión nueva**.

### 2.6 Ajustes del repo público (lista de configuración)

| Ajuste | Valor propuesto |
|---|---|
| Visibilidad | Público |
| Permisos por defecto de `GITHUB_TOKEN` | Solo lectura; cada workflow pide lo suyo (`permissions:` explícito) |
| Actions en PRs de colaboradores externos | Requerir aprobación para ejecutar workflows de forks |
| Secret scanning + *push protection* | Activados |
| Releases inmutables | Activados (H9) — crear releases siempre como *draft* |
| Pages | Fuente: *GitHub Actions*; dominio personalizado verificado a nivel de organización (§6.1) |
| Environments | `release` (reviewers, solo tags `v*` de `main`), `github-pages` (automático) |
| 2FA | Obligatorio para todos los miembros con escritura; ≥2 *owners* en la org [S] |
| Acciones de terceros | Pinned por SHA de commit; Dependabot las actualiza |
| Dependabot | Actualizaciones de `github-actions` y Gradle |
| CODEOWNERS | `.github/`, `scripts/`, `trusted-keys/`, `test-vectors/`, `web/verificador/`, `docs/adr/` (el gate G5 depende de ellos) |

---

## 3. Banco de preguntas: cómo entra en el build sin tocar git público

### 3.1 Vías de inyección (compartidas con `03` §5.4)

| Vía | Descripción | Ventajas | Inconvenientes | Recomendación |
|---|---|---|---|---|
| **L — local** | `scripts/release.mjs` lee `ARB_BANK_FILE` desde la carpeta privada; el APK sale a Releases con `gh` o a mano | Cero secretos en GitHub; el banco no sale de la máquina; coste nulo | Depende de una persona y su equipo; build no reproducible desde CI | **Fase 1** |
| **P — repo privado + CI** | `release.yml` hace *checkout* de `arbitratge-bank` con **deploy key de solo lectura** en el environment `release`, a `$RUNNER_TEMP` | Reproducible desde tag; provenance; coherente con Pages+Actions | Superficie de CI (§4.5); el banco pasa por un runner; la clave de deploy es un secret | **Fase 2**, cuando el environment esté endurecido |
| S — secret con el JSON | Banco en un secret de Actions | Sin repo privado | Límite de 48 KB (H8) y edición incómoda | Descartada |
| Banco cifrado en el repo público | Texto cifrado versionado | — | Historial público permanente (02 B3) | Descartada |

**Plan**: misma interfaz Gradle (`ARB_BANK_FILE`) en ambas vías; cambiar de L a P solo cambia
el origen del fichero en el workflow.

### 3.2 Contrato de build (interfaz)

| Variable (entorno) | Obligatoria en release | Uso |
|---|---|---|
| `ARB_BANK_FILE` | Sí | Ruta a `bank.json` real fuera del árbol del repo |
| `ARB_BANK_EXPECT_VERSION` | Sí | `bank_version` que debe tener el fichero (de `bank.lock`); evita empaquetar un banco distinto del previsto |
| `ARB_SEAL_KEY_FILE` | Sí, si se usa L2 de `03` §6.4 | PKCS#8 de la clave del sello del `kid` activo |
| `ARB_SEAL_KID` | Sí, si L2 | `kid` activo (p. ej. `s1`) |
| `ARB_KEYSTORE_FILE`, `ARB_KEYSTORE_PASS`, `ARB_KEY_ALIAS`, `ARB_KEY_PASS` | Sí | Firma Android (§5) |
| `ARB_BANK_FILE` ausente o `sample=true` | — | **El build de release falla**. Solo `debug` y CI pública usan `bank-sample` |

### 3.3 `bank.lock` (público, sin contenido)

```json
{
  "bank_version": "2027.01.0",
  "private_commit": "<sha del commit del repo privado>",
  "exam_version": "1"
}
```

No contiene hashes del contenido (evita un oráculo sobre respuestas, revisión B4). Lo modifica
un PR en el repo público; el release falla si `ARB_BANK_EXPECT_VERSION` no coincide.

### 3.4 Canary (detección de fugas hacia lo público)

- El repo privado contiene una cadena aleatoria (`canary`) dentro de `bank.json` (campo
  `canary`, que `packBank` elimina antes de empaquetar, 03 §5.4) y su valor exacto se guarda **solo como secreto** en
  `arbitratge-bank` y en el environment de `arbitratge-mobile` (para el escaneo, no para el build).
- `security.yml` y `release.yml` buscan la cadena en el árbol público, en `web/`, en la salida de
  Pages y en los logs del job. **Si aparece, el job falla.**
- **El APK no se escanea con el canary**: contiene el banco por diseño (ADR-0002). Su control es
  otro (§4.3, paso 8).
- Limitación [S]: el canary detecta copias literales o transformaciones triviales, no
  paráfrasis. Por eso el control principal es la separación de repos y artefactos (rev. intermedia M1).

### 3.5 Validación del banco

- En `ci` (muestra) y en el repo privado (banco real) con el mismo validador (`validate-bank.mjs`,
  reutilizando la lógica de `core-exam` cuando sea posible): esquema `schema: 1`, IDs únicos y
  estables, opciones con IDs únicos, `correct ⊆ opciones`, tipos (`single_choice` 1 correcta,
  `multiple_choice` ≥2, `true_false` con dos opciones fijas), `num_questions ≤` preguntas
  activas, `pass_threshold_count ≤ num_questions`, `sample=false` en release.
- Imprime **solo** recuentos y IDs, nunca texto.

---

## 4. Pipeline de CI (GitHub Actions)

### 4.1 Flujos

| Workflow | Disparador | Permisos (`GITHUB_TOKEN`) | Secretos | Salidas |
|---|---|---|---|---|
| `ci.yml` | `pull_request`, `push` a `main` | `contents: read` | **Ninguno** | Log; artifact de APK **debug con banco de muestra** (opcional) |
| `security.yml` | Semanal y manual | `contents: read` | Canary (solo lectura) | Fallo si hay fuga |
| `release.yml` | Tag `v*.*.*` en `main`; `environment: release` con revisión | `contents: read`, `id-token: write`, `attestations: write` | Environment `release` (§5.5) | APK firmado, SHA256SUMS, SBOM, atestaciones, release en *draft* → publicada; Pages desplegado al final del mismo workflow |
| `pages.yml` | `workflow_call` desde `release.yml` tras publicar; `workflow_dispatch` con `tag` (re-despliegue) | `pages: write`, `id-token: write`, `contents: read` | Ninguno | Sitio de Pages |

**Nunca** `pull_request_target` con *checkout* del PR; nunca `workflow_run` que consuma artefactos
de PRs de forks.

### 4.2 `ci.yml` (sin secretos; se ejecuta también en forks)

1. *Checkout* sin credenciales persistentes (`persist-credentials: false`).
2. Verificación del wrapper de Gradle (`gradle/actions/wrapper-validation`) [PV: nombre exacto].
3. JDK LTS requerido por AGP 9 [PV: versión mínima al implementar].
4. `validate-bank.mjs bank-sample/bank-sample.json` (y `check-public-tree.mjs`).
5. `./gradlew test lint` para `core-*`, `android-common` y `app-candidate` (JVM, sin emulador).
   Pruebas instrumentadas (`connectedAndroidTest`, Compose) en *job* manual o nocturno, por coste; **excepto G5** (§5.8), que es bloqueante en `release.yml`.
6. Pruebas de la PWA con el runner de Node integrado y sin dependencias: vectores de
   `test-vectors/` (generados desde la implementación Android, no a mano; códigos exactos, §5.8 G1 y G5; R01 y K02 solo en G3 de `release.yml`), comprobación de CSP, enlaces, ausencia de
   `http(s)://` externos y tamaño máximo del sitio.
7. `gitleaks` sobre el árbol y el historial del PR.
8. Build **debug** con `bank-sample` y firma debug. Si se sube artifact, solo ese APK, por ruta
   explícita (nunca `build/**`).

Caché: `actions/cache` solo para `~/.gradle` en `ci`. **No** se usa caché en `release` (§4.5).

### 4.3 `release.yml` (objetivo: firma en CI; desde tag)

Esbozo (no implementado):

```yaml
name: release
on:
  push:
    tags: ['v[0-9]+.[0-9]+.[0-9]+', 'v[0-9]+.[0-9]+.[0-9]+-rc.[1-8]']   # rc: solo build público de muestra (§2.5)
permissions: { contents: read }
concurrency: { group: release-${{ github.ref }}, cancel-in-progress: false }
jobs:
  build:
    runs-on: ubuntu-24.04            # imagen fija, no "latest"
    environment: release             # reviewers + solo tags de main
    permissions: { contents: write, id-token: write, attestations: write }
    steps:
      - uses: actions/checkout@<sha>           # persist-credentials: false
      # 1. el tag debe apuntar a un commit alcanzable desde main
      # 2. banco privado: deploy key -> $RUNNER_TEMP/bank (vía P) o se omite (vía L)
      # 3. secretos a ficheros en $RUNNER_TEMP con permisos 600; nunca echo
      # 4. ./gradlew :app-candidate:assembleRelease --no-build-cache --no-daemon
      # 5. apksigner verify + comparación con el digest del keystore (§5.4)
      # 5b. clave del sello (§5.8; solo tags finales): puertas G2 y G3
      # 5c. G5 cadena Android firma -> PWA verifica (§5.8): job gate-chain, bloqueante antes de publicar
      # 6. check-public-tree + canary sobre el árbol
      # 7. SBOM + SHA256SUMS + atestaciones
      # 8. inspección del APK (§4.3.1)
      # 9. release en draft con assets; publicar (inmutable) al final
      # 10. limpieza de $RUNNER_TEMP en if: always()
```

Pasos en orden, con su control:

| # | Paso | Control de fallo |
|---|---|---|
| 1 | Comprobar que el tag es `vX.Y.Z` o `vX.Y.Z-rc.N` y que su commit está en `main` | Falla si el tag viene de otra rama |
| 2 | Comprobar `versionName`/`versionCode` (§7.2) y que `bank.lock` coincide con `bank_version` del repo privado | Falla si no coincide |
| 3 | Obtener el banco real (vía P) y validarlo en modo release (`sample=false`). Solo en tags finales; en `-rc.N` se usan banco de muestra y sello de prueba, y se aborta si hay `ARB_BANK_FILE` o `ARB_SEAL_KEY_FILE` de producción (§2.5) | Falla si es muestra o no valida (tags finales), o si hay material de producción en un RC |
| 4 | Materializar el keystore y la clave del sello desde secretos a ficheros temporales | Sin `echo`; máscara de secretos automática |
| 5 | `assembleRelease` con todas las variables; sin caché ni *build scans* (`--no-build-cache`, sin `--scan`) | Falla si falta cualquier variable de release |
| 6 | `apksigner verify --verbose --print-certs` | Falla si v2/v3 no válidas, si hay v1, o si el digest del certificado **no** coincide con el fijado en `docs/SIGNING.md` (digest del **keystore Android**, no de la clave del sello) |
| 6b | Clave del sello (§5.8, solo tags finales): puertas G2 y G3 con la clave inyectada; no imprime la clave | Falla si no corresponde a `sha256_spki` de `trusted-keys/` o si la firma de prueba no verifica |
| 6c | G5 — cadena Android firma → PWA verifica (§5.8), con la PWA de test y `kid=test`; bloquea la publicación | Falla ante cualquier código distinto del esperado, caso no ejecutado, o si el emisor no obtiene `OK` en su autoverificación |
| 7 | Inspección del APK (§4.3.1) | Falla ante cualquier hallazgo |
| 8 | SBOM CycloneDX (plugin Gradle [PV]) y `SHA256SUMS.txt` | — |
| 9 | Atestación de procedencia para el APK y el SBOM (`actions/attest-build-provenance` [PV]) | — |
| 10 | Crear release **draft** con assets; verificar que los assets están; publicar | Si falla la subida, no se publica |
| 11 | Desplegar Pages (`pages.yml` vía `workflow_call`, §4.4) con los valores del **mismo tag** y el `latest.json` generado | Pages solo apunta a un asset existente y con el hash de ese tag |
| 12 | Prueba de humo *sin secretos*: descargar `releases/download/vX.Y.Z/…` y comparar SHA-256 con `SHA256SUMS.txt`, `latest.json` y la web desplegada; el alias `latest/download` se comprueba aparte | Alerta si no coincide con el tag |

#### 4.3.1 Inspección del APK (paso 7)

- `applicationId`, `versionName`, `versionCode` y `minSdkVersion` esperados (`aapt2 dump badging`
  o `apkanalyzer` [PV]).
- Sin keystore Android (`*.jks`, `*.keystore`, `*.p12`, `*.pfx`) ni ficheros `*.pk8` o PEM en el APK. **La clave del sello sí está en el APK** (ADR-0001), en la representación que fije §5.8; por tanto la inspección **no** concluye que la clave no esté. Su presencia se comprueba con la puerta G4, no por el nombre de un fichero [PV].
- En tags finales, sin marcador `sample: true` en `assets/`; el número de preguntas coincide con `bank_version`
  esperado.
- En tags finales, sin `kid=test`, `testx` ni `dev` (el verificador también los rechazaría). En `-rc.N` el APK lleva «PRUEBA» en su nombre y no emite credenciales de producción [PV: nombre del asset].
- **No** se busca el canary dentro del APK (el banco va dentro por diseño, §3.4).

### 4.4 `pages.yml`

1. Construye `_site/` a partir de `web/`: copia `trusted-keys/*.json` a `verificador/keys/`,
   recibe el **tag** de la release (entrada de `workflow_call` o `workflow_dispatch`) y genera `latest.json`
   y los valores estampados (versión, SHA-256 del APK versionado, huella del keystore) solo a partir de
   `SHA256SUMS.txt` y metadatos de **ese** tag, nunca de «la última release» consultada en el momento;
   estampa `index.html` mediante plantilla, y copia `CNAME` y `.nojekyll`. Un `push` a `main` que solo
   toque `web/**` no despliega: para publicar cambios de `web/` se relanza con el tag vigente.
2. Comprueba que **no** hay en `_site/` ningún fichero distinto de la lista blanca (§6.2). En particular rechaza cualquier clave `test*` en `verificador/keys/` y cualquier build de test de la PWA (el perfil de test de G5 solo existe en `$RUNNER_TEMP`).
3. `actions/upload-pages-artifact` + `actions/deploy-pages` [PV: versiones].

### 4.5 Controles de seguridad del pipeline (para que no haya fugas)

| Riesgo | Control |
|---|---|
| PR de fork roba secretos | Los forks no reciben secretos (H7); requerir aprobación de workflows de externos |
| PR modifica `release.yml` para exfiltrar | `CODEOWNERS` en `.github/`; revisión obligatoria; el environment `release` solo admite tags de `main` |
| Caché envenenada desde PR | `release` sin caché (`--no-build-cache`, sin `actions/cache`) |
| Artifacts con banco | Solo artifacts por ruta explícita y de muestra; ningún `build/**`, ningún `bank/**` (§2.4) |
| Logs con banco | Sin `cat`/`ls -R` de carpetas de banco; los scripts imprimen recuentos; `set +x` en pasos con secretos |
| Build scans o telemetría | Desactivados en release (`--no-scan` / sin `gradle-enterprise`) |
| Acciones de terceros comprometidas | Pinned por SHA; Dependabot; mínimos de `permissions:` |
| Deploy key del banco reutilizada | Deploy key **solo lectura** de un único repo, un environment, rotación anual |
| Un solo mantenedor puede aprobar su propio release | Con una persona, `prevent self-review` no se puede activar; riesgo residual aceptado hasta tener segunda persona (§9) |
| Tag movido | Ruleset de tags + releases inmutables (H9) |
| OIDC/atestación mal usado | Solo `id-token: write` en `release`/`pages`; ningún token de nube |

### 4.6 Artefactos y checksums

| Fichero | Dónde | Público | Contenido |
|---|---|---|---|
| `arbitratge-candidat.apk` | Release (nombre **estable**, para `latest/download`) | Sí | APK firmado de la última versión |
| `arbitratge-candidat-vX.Y.Z.apk` | Release | Sí | Copia versionada (histórico y reversiones) |
| `SHA256SUMS.txt` | Release | Sí | SHA-256 de todos los assets (formato `sha256sum`) |
| `sbom.cdx.json` | Release | Sí | Dependencias (CycloneDX); **no** incluye el banco |
| Atestaciones | Registro de atestaciones del repo | Sí | Procedencia: repo, commit, workflow, digest del APK |
| `latest.json` | Pages | Sí | §6.5 |
| *Artifacts de CI* | Actions | Sí (públicos) | Solo APK debug de muestra en PRs |

Verificación del lado del usuario (para el mantenedor y el organizador técnico): `sha256sum`
contra `SHA256SUMS.txt`; `apksigner verify --print-certs` contra el digest publicado; `gh attestation
verify` [PV: sintaxis] para la procedencia.

### 4.7 SBOM y procedencia (opcional, recomendado)

- **SBOM**: útil para auditar dependencias (ML Kit, Compose, ZXing, Room) sin coste. No añade
  riesgo si no contiene el banco ni rutas privadas [S].
- **Atestación**: prueba que el APK lo generó el workflow de *ese* repo y commit. **No** prueba
  el contenido del banco ni que el examen sea correcto [S]. Coste cero en repos públicos [PV].
- Ni SBOM ni atestación sustituyen a la firma Android ni al sello: son capas de auditoría.

### 4.8 Fase 1 local: `scripts/release.mjs`

Misma secuencia que los pasos 1–12, ejecutada por el mantenedor con Node y sin dependencias,
usando `ARB_*` desde ficheros fuera de cualquier repo (`%USERPROFILE%\.arbitratge-keys\`). Sube
los assets con `gh release create --draft` y publica al final. Se ejecuta en Windows (equipo del
mantenedor); los ficheros de claves **no** deben estar dentro de `C:\Users\lluis\projects\…`.
Ventajas e inconvenientes: §3.1 y §8.

---

## 5. Firma del APK

### 5.1 Dos claves, dos funciones (no confundir)

| | **Keystore de firma Android** | **Clave privada del sello de credenciales** [ADR-0001] |
|---|---|---|
| Qué firma | El **APK** (identidad del editor ante Android) | Los **tokens** de credencial (`payload + sello`) |
| Algoritmo [S] | RSA 4096 (compatibilidad amplia) o EC P-256 [PV] | ECDSA P-256 (ES256); la JCA devuelve DER y el emisor lo convierte a `r‖s` de 64 bytes (02 §3.7.3) |
| ¿Dentro del APK candidato? | **Nunca** (lo impone Android; la firma está en el APK pero **no** la clave) | **Sí**; extraíble por un técnico (02 §3.3, 03 §6.3) |
| Lo que se publica | **Digest SHA-256 del certificado** (`apksigner verify --print-certs`) en `docs/SIGNING.md` y en la web | **Huella SHA-256 de la clave pública (SPKI)** por `kid` en `trusted-keys/` |
| Rotación | `apksigner rotate` + lineage (API ≥ 28 [H12]); o nuevo `applicationId` | Nuevo `kid` por versión mayor (02 §7.2) |
| Si se pierde | No hay actualizaciones con el mismo `applicationId` (§5.6) | No se pueden emitir credenciales nuevas con ese `kid`; las existentes siguen verificando |
| Si se filtra | Un tercero puede firmar actualizaciones aceptadas por Android: incidente grave (§5.6) | Forja de credenciales de ese `kid`; ya asumido como extraíble |
| Dónde vive | Fuera de repos, con dos copias cifradas (§5.2); en CI solo como secret del environment `release` si se pasa a F2 | Igual; en CI, secret del environment `release` |

### 5.2 Keystore de firma: generación, custodia y prueba de restauración

**Generación** [S]: en el equipo del mantenedor (o en una máquina limpia), **una sola vez**, con
`keytool -genkeypair` en formato PKCS12 (`-keyalg RSA -keysize 4096 -validity` ~ 30 años [S]).
Dos contraseñas distintas (almacén y clave), generadas con gestor de contraseñas. Nada de esto se
escribe en el repo.

**Custodia** (mínimo recomendado [S]):

1. **Copia A**: gestor de contraseñas con adjunto cifrado (keystore + contraseñas en la misma entrada, o
   contraseñas separadas) [PV: el gestor elegido debe admitir adjuntos].
2. **Copia B**: contenedor cifrado (p. ej. VeraCrypt [PV]) en un medio **físicamente distinto** (USB o
   disco fuera de casa), con el mismo contenido.
3. **Opcional** (decisión del usuario, pregunta abierta 5): tercera copia en poder de una segunda persona de la
   CFBB, en sobre o en su propio gestor.
4. Registro público en `docs/SIGNING.md`: alias, fecha de creación, algoritmo y **digest SHA-256 del
   certificado**; **nunca** contraseñas ni nombres de ficheros de la copia.
5. **Prueba de restauración** al menos una vez al año: restaurar la copia B en una carpeta limpia,
   firmar un APK de prueba y comprobar que el digest coincide con `SIGNING.md`. [S]

**Acceso**: máximo dos personas. Si se pasa a F2, el keystore solo existe en el environment
`release` (secrets: `ARB_KEYSTORE_B64`, `ARB_KEYSTORE_PASS`, `ARB_KEY_PASS`), y la copia de
seguridad sigue siendo fuera de GitHub.

### 5.3 Esquemas de firma

- **Firmar con v2 y v3** (APK Signature Scheme). v3 es necesario para rotación [H12]. Con
  `minSdk` 26 no hace falta v1 (JAR): **desactivar v1** en la configuración de firma (AGP lo permite;
  nombre exacto de la opción al implementar [PV]). v4 no aplica (instalación incremental) [S].
- `apksigner verify --verbose` debe mostrar v2 y v3 como verificados y **no** v1 en release.
- Certificado: un solo *signer*. El *lineage* solo se crea si hay rotación.

### 5.4 Pin del digest en el pipeline

`docs/SIGNING.md` contiene el digest publicado (`SHA-256 certificado = …`). El paso 6 de §4.3
**compara** el digest del APK recién firmado con ese valor y **falla** si no coincide. Esto evita
firmar con un keystore equivocado (p. ej. uno de pruebas) y convierte el digest en un contrato
público verificable. Cambiar el digest es un cambio deliberado: exige PR con revisión y una nota
en la release explicando por qué (rotación).

### 5.5 Clave del sello de credenciales (solo si se elige L2 de `03` §6.4)

- **Generación**: en la máquina del mantenedor, PKCS#8 de P-256, **una por `kid`**; la pública se
  exporta a `trusted-keys/<kid>.json` (repo público) con `kid`, `alg`, `spki` (DER SPKI en base64url sin relleno), `sha256_spki`,
  `valid_from` (informativo) y `status: active | retired | compromised` (esquema de 02 §3.7.9).
- **Custodia**: como el keystore (dos copias cifradas). En CI, secret del environment `release`
  (`ARB_SEAL_KEY_PKCS8_B64` + `ARB_SEAL_KID`) solo en fase F2.
- **Huella que se publica y se fija en el verificador**: `sha256_spki` (huella de la clave pública).
  Es la misma huella que comprueba el verificador al cargar `trusted-keys/`. **No** es el digest del
  keystore.
- **Rotación**: un `kid` nuevo por versión mayor de la app o ante sospecha de abuso (02 §7.2). La app
  nueva usa el `kid` nuevo; el verificador acepta los `kid` con `status: active` o `retired` según
  política (fase 2 para la revocación, 02 §8.1). Publicar la clave pública nueva **antes** de emitir con ella.
- **Clave de pruebas** `kid=test`: clave pública y privada **de test**, en `test-vectors/`, que
  el release rechaza y el verificador de producción no acepta. Es una clave de prueba, no un secreto;
  mismo criterio que `02` §3.7.9 y §7.2: `kid=testx` (`compromised`) también en `test-vectors/`; `dev` solo en builds de depuración. El material de prueba es fijo y público por diseño; no se generan claves al vuelo.
- **Prohibido** que la privada del sello pase por `docs/`, `test-vectors/`, logs o artifacts.

### 5.6 Escenarios de pérdida o compromiso

| Escenario | Efecto | Respuesta recomendada |
|---|---|---|
| **Keystore perdido, con copias** | Ninguno inmediato | Restaurar desde copia; prueba anual lo asegura |
| **Keystore perdido, sin copias** | No se pueden publicar actualizaciones con el mismo `applicationId` (Android rechaza otra firma) | Opción A: nuevo `applicationId` (app nueva; los candidatos reinstalan y pierden el examen en curso y sus datos locales; **las credenciales ya emitidas siguen siendo verificables** porque su validez depende del sello, no del keystore). Opción B: mantener la versión existente sin actualizaciones. Comunicar en la web y al club |
| **Contraseña del keystore perdida, fichero bien** | Igual que el anterior | Gestor de contraseñas; no hay recuperación de contraseña de PKCS12 [S] |
| **Keystore comprometido** | Un atacante puede firmar actualizaciones que Android acepta para el mismo `applicationId` | Tratar como incidente: aviso en la web; nuevo `applicationId` (la rotación con lineage exige la clave antigua, así que no sirve tras un compromiso [S]); revocar la confianza en la web de descarga; instrucciones de desinstalación. Riesgo para instalaciones existentes |
| **Clave del sello perdida** | No se emiten credenciales con ese `kid` | Nuevo `kid` en la siguiente versión + verificador actualizado; las credenciales antiguas siguen verificando |
| **Clave del sello filtrada** | Forja de credenciales de ese `kid` (ya asumido, 02 §3.3) | Nuevo `kid`; marcar el antiguo `compromised` en `trusted-keys/` y en el verificador; no se pueden separar legítimas de forjadas por fecha (02 §3.3, rev. intermedia B3) |
| **Token de deploy key del banco filtrado** | Lectura del banco privado | Revocar la deploy key, regenerar, auditar accesos del repo privado |
| **Canary aparece en lo público** | Fuga parcial del banco | Pipeline detenido; retirar del árbol; evaluar la exposición (rotar banco o retirar preguntas) |

### 5.7 Relación con `trust-architect` (digest de certificado)

- El término «huella del certificado de firma» que `02` usa en T14 (§2.2) corresponde a **este documento**
  y significa el **digest del keystore Android** (§5.1). Sirve para que un usuario o un organizador técnico
  confirme que el APK descargado está firmado por el editor esperado. **No** verifica credenciales ni
  sustituye a `sha256_spki`.
- La clave que `02` y `03` llaman "clave de emisión" o "clave del sello" **no** es ese certificado:
  es la clave privada ECDSA que sí va dentro del APK (ADR-0001). Su huella es la `sha256_spki` de
  `trusted-keys/`, que es la que pinea el verificador.
- Ninguno de los dos digests protege la primera instalación frente a una descarga falsa si la
  página que los muestra también está comprometida (rev. completa I7). Conviene publicarlos en la
  web **y** en el repo, y comunicarlos por un canal del club.

---

### 5.8 Contrato de firma del sello y puertas de publicación (I3)

Para distribución basta fijar **qué se comprueba antes de publicar**. El contrato técnico (bytes exactos
firmados, UTF-8, algoritmo y representación de la firma `r‖s`, JSON estricto sin campos duplicados,
límites de tamaño, ausencia de campos críticos (todo cambio exige `ARB2`) y correspondencia `kid → clave pública`) pertenece a `02` §3.7 (canónico; `03` §6.2 queda superado, `02` §3.8.2);
ya está en `02` §3.7 y §3.7.11; los vectores deben pasar en Android real y en la PWA antes de publicar [PV]. Aquí solo se fijan las
puertas del pipeline:

| Puerta | Qué comprueba | Cuándo | Falla si |
|---|---|---|---|
| G1 — Vectores públicos (código exacto) | Los casos públicos de `test-vectors/` (§3.7.11 de `02`) se verifican con la PWA de test (WebCrypto, Node) y con el verificador Kotlin en JVM. Coincidencia **exacta** del código por caso: V01–V11 `OK`; F01 y F03–F09 `BAD_FORMAT`; F02 `IS_URL`; U01 `UNKNOWN_VERSION`; P01–P09 `BAD_PAYLOAD`; K01 `UNKNOWN_KID`; K03 `KID_NOT_ACCEPTED`; S01–S06 `BAD_SIGNATURE`; C01–C08 `BAD_FIELDS`. Todos contra la lista de **test** (`kid=test`/`testx`), sin claves ni listas de producción y sin secretos. **R01 y K02 no están aquí**: van solo en G3 | `ci` y `release.yml` (mismos casos públicos) | Cualquier código distinto, caso no ejecutado u `OK` inesperado |
| G2 — Correspondencia | SPKI de la clave privada inyectada → SHA-256 = `sha256_spki` de `trusted-keys/<kid>.json` | Solo tags finales | No coincide (sello de otra clave) |
| G3 — Firma de prueba y R01/K02 (solo release) | Firma un mensaje aleatorio con la privada inyectada y lo verifica con la pública publicada. Además (`02` §3.8.2), con la **lista de confianza de producción** (`trusted-keys/`) y un build de la PWA de producción generado en `$RUNNER_TEMP` (sin desplegar, así no depende de Pages): el token R01 firmado con la clave inyectada debe dar `OK` y `kid=test` (K02) debe dar `UNKNOWN_KID`. Usa el secreto del environment `release`; no corre en `ci`; no guarda ni imprime mensaje, firma ni token | Solo tags finales (`release.yml`) | No verifica, R01 ≠ `OK` o K02 ≠ `UNKNOWN_KID` |
| G4 — Representación | La clave del sello aparece solo en la representación que fije el contrato (§4.3.1); sin keystore Android ni PEM/PKCS#8 legible como fichero | Solo tags finales | Aparece en otra forma o junto a nombres prohibidos |
| **G5 — Cadena Android firma → PWA verifica** (contrato exacto) | El emisor de producción firma casos con `kid=test` en un APK de test en emulador o dispositivo **API 26**; la PWA de test (Node WebCrypto, y navegador headless cuando exista [PV]) y el verificador Kotlin del propio dispositivo devuelven para cada caso **el mismo código** que el fichero de casos. Detalle en el bloque siguiente | `ci` nocturno/manual y **obligatoria en `release.yml`** (job `gate-chain`, antes de publicar) | Cualquier discrepancia, caso no ejecutado, o autoverificación del emisor distinta de `OK` |

**G5 en detalle** (cadena Android firma → PWA verifica; contrato `02` §3.7, sin variantes):

- **Quién firma**: el emisor de `core-credential` (§3.7.10 de `02`) compilado en un APK **de test** (nunca `release`) con la clave `kid=test` de `test-vectors/`. Genera V01–V05 de §3.7.11. Como ECDSA no es determinista, **no se compara el texto del token**: se compara el resultado de verificación y los campos decodificados.
- **Quién verifica**: (a) el verificador Kotlin en el dispositivo API 26; (b) la PWA de test en Node con WebCrypto; (c) la PWA de test en navegador headless [PV: disponibilidad en el runner]. Los tres devuelven **exactamente** el código de §3.7.8 que indica el caso; para `OK`, los campos decodificados (`nom` tras NFC, `iat`, `cid`, `exv`…) coinciden literalmente con los del caso.
- **Casos**: solo el subconjunto público de `G1` (V01–V11, F01–F09, U01, P01–P09, K01/K03, S01–S06 y C01–C08), usando la lista de confianza de test. **R01 y K02 quedan expresamente fuera de G5** y solo se comprueban en G3 con la clave/lista de producción. Ningún caso del subconjunto elegido se omite ni cambia de código sin cambiar `02` (§3.7.0).
- **Criterio**: igualdad exacta de código por caso en los tres verificadores. Un `OK` que en un verificador sea `BAD_FIELDS` es una diferencia. Cero casos sin ejecutar.
- **Autoverificación**: el emisor debe obtener `OK` con su propio verificador antes de devolver un token (`02` §3.7.10, paso 5); si no, el caso falla.
- **Dónde y cuándo**: `ci.yml` (nocturno o manual, por coste de emulador) y **obligatorio en `release.yml`** antes de publicar. Si el runner no puede ejecutar un emulador API 26 [PV: KVM], la alternativa de fase 1 es ejecutar G5 en el dispositivo del mantenedor con `scripts/release.mjs` y adjuntar a la release solo el recuento de casos y los códigos. Esa alternativa **no** es una puerta automática y queda como pregunta abierta 17.
- **Material**: casos y tokens son ficticios (`kid=test`), pero se mantienen en `$RUNNER_TEMP`; los logs solo muestran recuentos y códigos. Ningún token se sube como artifact.

Reglas:

- **Dos pines distintos, sin sustituirse**: el digest del **keystore Android** (§5.4, `docs/SIGNING.md`) y la
  `sha256_spki` del **sello** (§5.5, `trusted-keys/`). Ninguno verifica al otro.
- **Presencia del sello en el APK**: la clave del sello sí está en el APK candidato (ADR-0001, clave extraíble
  por diseño). Su representación no es una barrera de confidencialidad: no se añade cifrado ni ofuscación
  como control de seguridad (perfil proporcional). La inspección de §4.3.1 no debe leerse como ausencia de esa clave.
- **Material**: la privada del sello nunca se imprime ni va a `docs/`, `test-vectors/`, logs o artifacts.
  G2 y G3 usan `$RUNNER_TEMP` y dejan solo el resultado.
- **Orden**: un `kid` nuevo se publica en `trusted-keys/` y se despliega en la PWA antes de emitir con él (§5.5, §6.7).
- Antes de la primera release pública, G1–G5 deben estar probadas [PV].

---

## 6. Publicación: GitHub Releases, GitHub Pages y verificador

### 6.1 Dominio y Pages

| Decisión | Propuesta [S] | Alternativas |
|---|---|---|
| **Subdominio** | **`arbitratge.futbolbotons.cat`** (descarga en `/`, verificador en `/verificador/`) | `descarrega.futbolbotons.cat` (si se prefiere lenguaje llano); `verifica.futbolbotons.cat` solo con **repo de Pages separado** (un repo solo sirve un dominio personalizado) |
| Estado del dominio (2026-11-26) | **Activo**: el usuario apuntó el DNS (Hostalia) a este repo. Dominio definitivo **`arbitratge.futbolbotons.cat`** (`arbitratge-app.` descartado). El proyecto base ya no lo sirve | — |
| Fase piloto | Terminada: se publicó brevemente en `https://lsunol.github.io/arbitratge-mobile/` con `stamp-web --host`. Ahora `pages.yml` publica por defecto con el dominio propio (`custom_domain=true`); `--host` queda para pruebas en github.io | — |
| Repo que sirve Pages | `lsunol/arbitratge-mobile` (propietario: usuario personal, no la org `cfbbarcelona`) | `cfbbarcelona/arbitratge-mobile` si se transfiere |
| DNS | Registro **CNAME** `arbitratge` → `lsunol.github.io` (cuenta personal) configurado y resolviendo. Ajuste en Pages: dominio personalizado `arbitratge.futbolbotons.cat` + HTTPS forzado. Un mismo dominio solo lo sirve un sitio de Pages | — |
| Verificación de dominio | Opcional en cuenta personal: verificar `futbolbotons.cat` en los ajustes de Pages del usuario (TXT de desafío) para evitar *takeover* (H11) | — |
| HTTPS | Forzar HTTPS en Pages cuando emita el certificado (H11) | — |
| `CNAME` | En `web/CNAME` (`arbitratge.futbolbotons.cat`); se copia al artefacto solo cuando se publica con ese dominio | — |

Sin cambios en el proyecto base. El DNS de `futbolbotons.cat` lo gestiona el usuario [supuesto].

### 6.2 Contenido publicado en Pages (lista blanca)

```
_site/
├─ index.html                 descarga (única página de entrada)
├─ styles.css, brand/, fonts/ landing (Atkinson autoalojada, OFL)
├─ latest.json                versión actual (§6.5)
├─ verificador/
│  ├─ index.html              PWA
│  ├─ app.js                  lógica (sin dependencias de build)
│  ├─ sw.js                   service worker (§6.7)
│  ├─ manifest.webmanifest
│  ├─ icons/                  192, 512, maskable
│  ├─ vendor/…                lector QR vendido, con licencia (§6.7)
│  └─ keys/*.json             trusted-keys (solo claves públicas)
├─ CNAME
└─ .nojekyll
```

**No** se publica: banco en ningún formato, `bank-sample` (ya está en el repo, no hace falta
en Pages), SBOM, `test-vectors/` con tokens de prueba (se usan en CI, no en producción), APK
(se enlaza desde Releases; ver §6.4 sobre no duplicarlo), ficheros de `docs/`.

### 6.3 Web de descarga: un único HTML simple

- **Un solo `index.html`** (+ `styles.css`, `brand/`, `fonts/` propios), sin JavaScript. Cabecera `Content-Security-Policy`
  como `<meta>`: `default-src 'none'; style-src 'self'; font-src 'self'; img-src 'self'; base-uri 'none';
  form-action 'none'; object-src 'none'` (no hay scripts, así que no hace falta `script-src`; el verificador tiene su CSP propia, §6.7). Pages no
  permite cabeceras propias (`frame-ancestors` no se puede fijar con meta; aceptable sin secretos) [PV].
- **Contenido en catalán**, texto grande, pasos numerados (04 define el detalle visual):
  1. Botón "Descarrega l'app (APK)" → **URL versionada** de Releases (`…/releases/download/vX.Y.Z/…`, §6.4), cuyo SHA-256 y huella son los estampados. El alias `latest` no es el botón principal (I5).
  2. Aviso: el móvil pedirá permitir instal·lació d'orígens desconeguts; cómo activarlo y cómo
     volver a desactivarlo.
  3. **SHA-256 del APK** y **huella del certificado de firma** (valores estampados en la plantilla
     en el despliegue), con instrucción de comparación.
  4. Enlace a la **versión** (`versionName`, fecha) y a notas de versión.
  5. Enlace al **verificador** (`/verificador/`) y advertencia de [ADR-0001]: "Aquesta marca ajuda a
     detectar canvis casuals; no és una prova contra un dispositiu modificat o una clau extreta."
  6. Aviso de privacidad en un `<details>` del mismo HTML (decisión: sin `privacitat.html`; qué datos se guardan, que no hay servidor). Contacto del club pendiente.
  7. Contacto del club (decisión del usuario).
- **Plantilla** (implementada): marcadores `__VERSION__`, `__APK_URL__`, `__SHA256__`, `__HOST__` (de `web/CNAME`) y bloques opcionales `<!--CERT-->` (`__CERT_SHA256__`), `<!--PUBLISHED-->` (`__PUBLISHED__`) y `<!--NOTES-->` (`__RELEASE_NOTES_URL__`), que estampa `node scripts/stamp-web.mjs --preview|--production --version X.Y.Z --version-code N --apk-url … --sha256 … [--cert-sha256 …] [--published YYYY-MM-DD] [--release-notes-url …]` hacia `_site/`.
  - `--production` **exige** cert, fecha, `--bank-version` (emitido como `bank_version` en `latest.json`), notas, `--release-metadata` (generado por `scripts/fetch-release-metadata.mjs`: release publicada, no borrador/prerelease, tag, asset, URL y SHA-256 descargado ligados a los argumentos) y una lista de claves de producción no vacía y válida (SPKI base64url canónico) **idéntica byte a byte a `trusted-keys/`** (mismo conjunto de ficheros y mismos bytes; la correspondencia con la clave de firma de la release es otra puerta, G2/G3) (`verificador/keys`, solo claves públicas ES256). `--preview` (hoy, sin release firmada ni claves) admite que falten; lo que falta **no se publica**, nunca se inventa.
  - Solo versión final `X.Y.Z`; `versionCode` debe cumplir la fórmula de §7.1; el APK debe ser exactamente `https://github.com/lsunol/arbitratge-mobile/releases/download/vX.Y.Z/<fichero>.apk` (el repositorio real es `origin`; si se traslada a `cfbbarcelona`, cambiar `RELEASE_REPO` en el script y este documento).
  - Falla si queda un marcador, falta el verificador o el precache está desactualizado. Lista blanca explícita de ficheros (verificador fichero a fichero; un `questions.json` o `lib/extra.js` se rechaza), sin enlaces simbólicos; la salida solo puede ser `<repo>/_site` (vacío o con la marca `.arbitratge-web-stamp`) o un directorio nuevo bajo el temporal del sistema, sin enlaces/uniones.
  - Estampado fuera de la web: el CI debe además comprobar HTTP 200 del APK (§4.4). No hay página «descàrrega no disponible»: ante un fallo no se despliega.

### 6.4 Enlaces estables

| Qué | URL | Cambia? |
|---|---|---|
| Web de entrada (para pasar a los aspirantes) | `https://arbitratge.futbolbotons.cat/` | No |
| Verificador | `https://arbitratge.futbolbotons.cat/verificador/` | No |
| Alias de la última versión (secundario) | `https://github.com/cfbbarcelona/arbitratge-mobile/releases/latest/download/arbitratge-candidat.apk` (H10) | Cambia con cada release publicada y puede adelantarse al despliegue de Pages. **No** es la fuente del hash mostrado (I5) |
| APK de una versión concreta | `…/releases/download/vX.Y.Z/arbitratge-candidat-vX.Y.Z.apk` | No |
| Metadatos de versión | `https://arbitratge.futbolbotons.cat/latest.json` | Se regenera en cada release |

**Decisión sobre el APK en Pages** [S]: se recomienda **enlazar desde Releases y no copiar el APK
a Pages**. Motivos: no se duplica el tráfico contra el límite soft de Pages; el dominio de GitHub
es reconocible; un único lugar de publicación del binario. Coste: el enlace de descarga sale de
`github.com`, lo que se explicará en la web. Alternativa: copiar el APK a Pages para tener una URL
del propio dominio (más cómodo, más tráfico y dos fuentes que mantener).

### 6.5 `latest.json`

```json
{
  "schema": 1,
  "app": "candidate",
  "versionName": "1.3.0",
  "versionCode": 103009,
  "minSupportedVersionCode": 103000,
  "bank_version": "2027.01.0",
  "published": "2027-03-14",
  "apk_sha256": "<hex>",
  "cert_sha256": "<hex>",
  "download_page": "https://arbitratge.futbolbotons.cat/",
  "notes": { "ca": "Correccions en el feedback de resultats." }
}
```

- `versionCode` según la fórmula de §7.1 (`1.3.0` → 103009). Solo describe versiones finales; las RC no aparecen.
- Campos opcionales añadidos por `stamp-web`: `release_notes_url` (enlace a la release; la landing lo muestra). `minSupportedVersionCode` **no lo emite aún** el script (los fijará el pipeline de release cuando existan).
- **Público y sin datos sensibles**. No contiene URL del APK: la app abre `download_page` (fija en
  el código), así un `latest.json` manipulado no redirige a otro binario. El instalador de Android
  rechaza un APK con otra firma (límite real, 02/rev. completa §B).
- `notes` **sin textos de preguntas**; solo descripción de cambios.
- `minSupportedVersionCode` solo se sube si una versión anterior es insegura o incorrecta (p. ej.
  clave de respuestas errónea). La app puede mostrar aviso y no bloquear el examen [S, 03 §9.2].

### 6.6 Aviso de nueva versión en la app

- Requiere `INTERNET` solo para leer `latest.json` (decisión de `03` §9.2 / pregunta 5). Si se
  acepta: HTTPS, sin identificadores, fallo silencioso, desactivable. Si no: el aviso es solo la web
  y el club.
- Desde el punto de vista de distribución, **no cambia nada del pipeline**: `latest.json` se genera
  igual.

### 6.7 PWA verificadora estática (público, sin secretos)

**Requisitos de [ADR-0001]**: instalable, offline una vez cargada, sin backend, sin secretos, con
resultado `VÀLIDA` / `NO VÀLIDA` / `VERSIÓ DESCONEGUDA` / `FORMAT INCORRECTE` (los textos definitivos
los fija 04; aquí, los códigos de `core-credential`).

| Aspecto | Diseño [S] |
|---|---|
| Tecnología | HTML + JS sin dependencias de build (ES modules) y **WebCrypto** `ECDSA P-256` para verificar. Sin bundler: menos cadena de suministro |
| Lógica compartida | Parser y verificación con **los mismos vectores** que `core-credential` (`test-vectors/`). CI ejecuta ambos. Sin compartir código: dos implementaciones, misma prueba |
| Claves | `verificador/keys/*.json` copiadas desde `trusted-keys/` en el despliegue. Solo claves públicas; la app rechaza `kid` desconocido |
| Lectura de QR | (a) cámara con `getUserMedia` + decodificador QR **vendido** (copia local con su licencia; sin CDN) y (b) importar imagen y (c) pegar texto. `BarcodeDetector` solo como mejora si existe [PV: soporte por navegador] |
| Red | Ninguna llamada de datos. `connect-src 'self'` en CSP. Sin analítica, sin fuentes externas |
| CSP | `<meta>`: `default-src 'none'; script-src 'self'; style-src 'self'; img-src 'self' data: blob:; media-src 'self' blob:; manifest-src 'self'; worker-src 'self'; connect-src 'self'; base-uri 'none'; form-action 'none'; object-src 'none'`. `getUserMedia` no lo rige la CSP. Sin `eval` ni inline |
| Instalación | `manifest.webmanifest` con `lang: ca`, `display: standalone`, iconos 192/512 y maskable |
| Offline | `sw.js` precachea la lista completa generada en el despliegue (HTML, JS, CSS, vendor, keys, iconos), con un manifiesto de hashes por fichero. Nombre de caché = versión del verificador (`verificador-1.2.0`). Actualización: aviso «nova versió disponible» y recarga (detalle 04). **Preparada** solo si todos los ficheros del manifiesto están en caché; «instal·lada» no basta [PV: API del service worker]. Prueba: inicio en modo avión tras cerrar y reabrir el navegador, y tras borrar la caché (qué mensaje se muestra) |
| Compatibilidad y versiones antiguas (I4) | Una PWA antigua no conoce `kid` ni formatos nuevos y debe **rechazarlos**, nunca aceptarlos por defecto. Orden: primero `trusted-keys/` y la PWA desplegadas; después el APK que emite con el `kid` nuevo (§5.5). Los `kid` no se retiran sin política de revocación (fase 2, 02 §8.1). **Caducidad**: el MVP no la define y ninguna pantalla ni documento la promete; si se añade, separar firma válida de vigencia (la fecha del candidato y el reloj del verificador son manipulables) |
| Versión visible | Pie de página con `versionName` y `kid` aceptados (transparencia para el organizador) |
| Dominio | Solo `arbitratge.futbolbotons.cat` (el organizador debe usar la dirección oficial; 02 T15). Sin enlaces que acepten QR como URL (02 §6.4) |
| Pruebas | Vectores de §3.7.11 de `02` con código exacto (G1) y cadena Android→PWA (G5); prueba manual en Android con Chrome/Edge: instalación, offline en modo avión, cámara, importar imagen, pegar texto (condición de ADR-0001) |
| Riesgo | Un verificador falso que diga siempre "VÀLIDA" (02 T15): mitigado por procedimiento (verificar en el móvil del organizador, dominio oficial), no técnicamente |

**Aviso**: la PWA es pública y sin secretos; si se filtrara su código, nadie puede firmar nada
(solo tiene claves públicas) [02 §6.1].

### 6.8 Separación de assets: público, banco de ejemplo y banco privado

| Categoría | Ejemplos | Dónde vive | Visible públicamente | Entra en el APK |
|---|---|---|---|---|
| **Público** | Código, `web/`, PWA, `trusted-keys/`, `latest.json`, docs, `bank-sample/` | Repo público, Pages, Releases | Sí | Código sí; muestra no (solo en debug) |
| **Banco de ejemplo (ficticio)** | `bank-sample/bank-sample.json` (preguntas inventadas, `sample=true`) | Repo público | Sí | Solo en debug |
| **Banco privado real** | `bank/bank.json` con preguntas, respuestas y explicaciones | Repo privado o carpeta local | **No** | Sí, en release (ADR-0002) |
| **Claves** | Keystore, PKCS#8 del sello | Fuera de repos; copias cifradas; secrets solo en `release` (F2) | **No** | Solo el sello (extraíble) |
| **Tokens de prueba** | `test-vectors/` (kid=test) | Repo público | Sí | No (el release los rechaza) |

### 6.9 App de organizadores y distribución no pública (fuera del MVP)

- **[ADR-0001] descarta la app Android de organizadores para el MVP.** El verificador es la PWA pública de
  §6.7, que solo contiene claves públicas y, por eso, no necesita canal restringido. **No se propone ninguna
  segunda app nativa ni una distribución privada para ella**; las opciones de publicación restringida que se
  describían antes quedan retiradas.
- Si se revisa en el futuro, requerirá un ADR nuevo. Restricción mínima para cualquier app nativa verificadora:
  no depende de `bank-*` ni de `core-exam`; no contiene keystore ni claves privadas; CI comprueba el grafo de
  dependencias y el APK (sin `assets/bank*`). Su canal (público o restringido) se decidirá entonces.
- Control equivalente para la PWA: el paso de despliegue de Pages (§4.4, paso 2) rechaza cualquier fichero de
  banco en `_site/`.

---

## 7. Versionado

### 7.1 App (SemVer y `versionCode`)

- **`versionName`** = SemVer `MAJOR.MINOR.PATCH` (tag `vX.Y.Z`). Pre-releases `-rc.N`.
  - MAJOR: cambio incompatible de formato de credencial o de la política de sesión (raro).
  - MINOR: funciones nuevas o cambio visible.
  - PATCH: correcciones, textos, banco (sin cambio de reglas).
- **`versionCode`** monótono estrictamente creciente, calculado en Gradle (no a mano):

  ```
  versionCode = X·100000 + Y·1000 + Z·10 + s
  s = N para `rc.N` (N = 1..8) ; s = 9 para la versión final (s = 0 no se usa)
  ```

  Con `Y, Z ≤ 99`, el orden es el de SemVer y **cualquier RC es anterior a su final**, lo que permite
  actualizar sobre un RC sin desinstalar. Ejemplos con la fórmula: `1.3.0` = 1·100000 + 3·1000 + 0·10 + 9 = **103009**; `1.3.0-rc.2` = 1·100000 + 3·1000 + 0·10 + 2 = **103002**; `1.3.1-rc.1` = **103011** (monótono). Como `Y·1000 + Z·10 + s ≤ 999`, no hay colisiones entre componentes. Pruebas de la fórmula, monotonía y transición RC → final antes de la primera release [PV].
  Límite de Android: 2 100 000 000 (no es problema) [PV: tipo del campo al implementar].
- **Control** en `release.yml` (paso 2): el `versionCode` nuevo debe ser mayor que el de la última
  release publicada; si no, falla.
- **`applicationId`**: fijo para siempre (cambiarlo = app nueva). **Decidido (2026-11-20, usuario)**:
  `dev.lluissunol.arbitratge` (namespace de paquetes Kotlin `dev.lluissunol.arbitratge.*`). Cualquier `applicationId` pasa por el
  registro de desarrolladores de Android (§8.3).
- **Versión de la PWA**: la misma `versionName` que la última release del candidato, o versión propia
  si la PWA se publica sin release de APK (decisión menor, pregunta abierta 11).

### 7.2 Banco de preguntas

| Campo | Significado | Cómo cambia |
|---|---|---|
| `bank_version` `AAAA.MM.N` (p. ej. `2027.01.0`) | Versión del **contenido** del banco | Cualquier cambio de contenido: `N` (o `MM` si cambia el mes); tag `bank-…` en el repo privado |
| `exam_version` (`exv`, entero como texto, `"1"`) | Versión de **reglas y significado** de la credencial (nº preguntas, tiempo, umbral, tipos) | Solo si cambian reglas o el sentido de la credencial (02 §7, 03 §5.2) |
| `status` por pregunta | `active` / `retired` | Retirar, nunca borrar (IDs estables) |

- Cada release declara **una** `bank_version` (en `bank.lock`, `latest.json` y notas de versión).
- **Qué identifica cada campo** (I7): `exam_version` (`exv`) identifica reglas y significado de la credencial; **no** identifica el contenido. Dos APK con bancos distintos pueden compartir `exv`.
- `bank_version` es pública, no sensible y no revela respuestas. Propuesta [S]: incluirla en el payload firmado junto a `exv` (campo a fijar en `03` §5.2), para saber qué banco se usó sin publicar hashes de respuestas. Alternativa: trazabilidad solo local (registro en el dispositivo y en el repo privado). Pregunta abierta 16.
- El release comprueba que `bank.lock`, `ARB_BANK_EXPECT_VERSION`, `latest.json` y las notas declaran la **misma** cadena (§3.2, §4.3 paso 2).
- Corregir una clave de respuesta = nueva `bank_version` **y** nueva versión PATCH de la app. Es la
  única vía de corrección (no hay servidor): la comunicación al club la hace la web.
- `exam_version` aparece en la credencial (`exv`); un cambio de reglas debe aparecer en las notas.

### 7.3 Sello (kid)

- `kid` = `s<MAJOR de la app>` (p. ej. `s1`); si hay dos claves en la misma versión mayor (rotación por
  sospecha), `s1b`. El `kid` **no** cambia el `v` del token (02 §7.2; `v` es el formato).
- Un `kid` nuevo requiere actualizar `trusted-keys/` en el repo público y desplegar la PWA antes de
  emitir con él.

---

## 8. Coste, límites y dependencias

### 8.1 Coste económico

| Concepto | Previsión | Verificación |
|---|---|---|
| Repos públicos (código, Releases, Pages) | 0 € | [H/PV: planes de GitHub] |
| Actions en repo público (runners estándar) | 0 € | [PV: condiciones vigentes] |
| Repo privado del banco | 0 € en plan gratuito, con minutos de Actions limitados si hay CI de validación/compilación | [PV] |
| Dominio `futbolbotons.cat` y DNS | Ya disponible del club (H2 lo sugiere) | Confirmar quién lo gestiona |
| Registro de desarrollador Android (verificación 2027) | Coste y forma: **no verificado** | [PV] |
| Sigstore/atestaciones | 0 € | [H: uso gratuito en repos públicos; PV] |
| Herramientas (Node, JDK, Gradle, apksigner) | 0 € | — |

### 8.2 Límites técnicos a vigilar

| Límite | Valor [PV: confirmar al implementar] | Impacto |
|---|---|---|
| Tamaño de secret de Actions | 48 KB (H8) | Banco no puede ir en secret; sí las claves PKCS#8 en Base64 |
| Asset de release | Muy superior al tamaño del APK previsto (< 30 MB, 01 §5) | Ninguno |
| Pages: tamaño soft del sitio | ~1 GB [PV] | Holgado sin copiar APK |
| Pages: ancho de banda soft | ~100 GB/mes [PV] | Por eso el APK se enlaza desde Releases (§6.4) |
| Retención de artifacts | 90 días por defecto [PV] | No usar artifacts como canal de distribución |
| Tiempo de un job | Decenas de minutos de margen [PV] | Ninguno previsible |

### 8.3 Dependencias externas

| Dependencia | Riesgo | Mitigación |
|---|---|---|
| GitHub (Actions, Releases, Pages) | Indisponibilidad o cambio de condiciones | Copia local del APK firmado y del `SHA256SUMS`; `scripts/release.mjs` permite publicar desde el equipo del mantenedor; la app no depende de GitHub salvo para el aviso de versión |
| **Verificación de desarrolladores de Android (2027)** | Un APK no registrado en dispositivos certificados exige el "flujo avanzado" (fricción para mayores) | Registrar `applicationId` antes de la primera publicación; según el tipo (*full* o *limited*), revisar requisitos. **Discrepancia a aclarar**: `03` §1.2 recoge límite de 20 dispositivos (*limited*) en una guía y 50 en otra [H según `03`; PV] |
| Google Play Protect | Puede avisar de "app de origen desconocido" al instalar | Instrucciones en la web; no hay forma de evitarlo fuera de Play [PV, S] |
| Proveedor del decodificador QR (PWA) | Licencia y mantenimiento | Vendido en el repo con su licencia; sin CDN |
| Android/AGP/JDK | Churn de versiones | Versiones fijadas; ventana trimestral de actualización (`03` §12) |
| Cuenta del mantenedor | Un solo punto de fallo | 2FA; ≥2 owners; documentación de recuperación (§9) |

---

## 9. Riesgos residuales

| # | Riesgo | Severidad | Tratamiento |
|---|---|---|---|
| R-1 | Fuga del banco por error humano (commit, captura, logs) | Alta | Bloqueantes §1.3; `.gitignore`; canary; escaneo en CI y pre-push |
| R-2 | Banco extraíble del APK (ADR-0002) | Aceptada | Documentada; no se intenta ocultar |
| R-3 | Keystore comprometido → actualizaciones falsas para el mismo `applicationId` | Alta (poco probable) | Dos copias cifradas; acceso ≤2 personas; digest pineado; plan de nuevo `applicationId` |
| R-4 | Keystore perdido sin copia | Alta (impacto), baja (probabilidad) | Copias y prueba anual de restauración |
| R-5 | Un solo mantenedor puede publicar y aprobar | Media | Riesgo aceptado mientras haya una persona; segunda persona como criterio de salida de F1 |
| R-6 | Pipeline de CI mal configurado (secretos a PR, tag movido) | Media | §4.5; rulesets; releases inmutables |
| R-7 | Actions o Pages cambian condiciones o límites | Media | Plan B de publicación local (§4.8); seguimiento en cada release |
| R-8 | Verificación de desarrolladores de Android: instalación bloqueada o con fricción | Alta para el usuario final | Registrar pronto; web con instrucciones; prueba S4 de `03` |
| R-9 | Descarga de un APK falso desde un canal que suplanta la web | Media | Digest del certificado y SHA-256 en la web y en el repo; comunicación del club. **No es protección absoluta** (rev. completa I7) |
| R-10 | Verificador falso que dice "VÀLIDA" | Media | Dominio oficial, verificación en el móvil del organizador (02 T5/T15) |
| R-11 | Clave del sello extraída y forja (ya aceptado) | Aceptada | Documentado; rotación por `kid` |
| R-12 | Pages sirve un `latest.json` falso (aviso engañoso) | Baja | No hay URL de APK en el JSON; el instalador verifica la firma |
| R-13 | Un release con `bank_version` equivocada | Media | `bank.lock` + `ARB_BANK_EXPECT_VERSION`; prueba de humo de §4.3 |
| R-14 | Licencia no definida para código público | Media | Decidir antes del primer push (pregunta abierta 6) |
| R-15 | PWA sin caché completa, o verificador antiguo ante `kid`/formato nuevos (I4) | Media | Indicador de preparación [PV]; orden de publicación de §6.7; prueba en modo avión; no prometer caducidad |
| R-16 | `latest` y la página muestran versiones distintas durante un despliegue (I5) | Media | Botón a URL versionada; Pages en el mismo workflow tras publicar; prueba de humo (§4.3, paso 12) |
| R-18 | Interoperabilidad Android↔PWA no demostrada (`02` §3.7.11) | Alta hasta pasar G5 | G5 bloqueante en `release.yml`; sin G5 en verde no hay publicación |

---

## 10. Runbook de publicación (resumen)

1. PR de versión: `versionName`, `bank.lock`, notas (sin textos de preguntas), `docs/SIGNING.md` si
   cambió el digest (raro). `ci` en verde; revisión.
2. Merge a `main`. Tag `vX.Y.Z-rc.1` → **prerelease pública** con banco de muestra y sello de prueba: valida
   pipeline, descarga, hashes y G1/G5 (cadena Android→PWA), **no** el examen con banco real (§2.5, I5). Las pruebas de examen completo y
   credencial en 2–3 móviles (incluido uno antiguo) se hacen con un APK de build local con banco real, entregado
   por canal directo del mantenedor y **no** subido a GitHub ni a Pages (pregunta abierta 13).
3. Si OK, tag `vX.Y.Z` → release en draft → publicar. `latest.json` y Pages se actualizan en el mismo workflow, con el tag de esa release (§4.3, §4.4).
4. Verificación posterior: descarga desde la web, comparar SHA-256 y `apksigner verify`, instalar.
5. Comunicación al club con el enlace estable y el aviso de versión.
6. **Rollback**: no hay downgrade en Android (`versionCode` menor se rechaza). Se publica una versión
   PATCH nueva con la corrección. Si la versión es insegura, `minSupportedVersionCode` + aviso.

---

## 11. Impacto en otros documentos y ADRs propuestos

| Documento | Cambio pedido |
|---|---|
| `03-arquitectura-movil.md` | §5.2: ya ficticio (B-2 cerrado). §6.2: alinear con `02` §3.7 (editorial, `02` §3.8.2). §9.2: remitir a §6.5 de este documento. §1.2: aclarar la discrepancia 20/50 dispositivos |
| `02-credencial-y-amenazas.md` | T14: referir la huella del keystore a este documento y la `sha256_spki` a `trusted-keys/` (§5.7) |
| `04-ux.md` | Pantallas de descarga y verificador: textos de §6.3 y §6.7, advertencia de [ADR-0001] |
| `01-requisitos.md` | Quitar la pregunta 1 (repo base público) como resuelta (H1), según la revisión completa M1 |
| `03-arquitectura-movil.md` (I3, I7) | Payload: decidir si incluye `bank_version` (§7.2); fijar el contrato de firma y sus vectores antes de implementar (§5.8) |

**ADRs propuestos** (no escritos aquí; los cierra el usuario):

- **ADR-0003 — Repositorios y distribución**: repo público `arbitratge-mobile`, privado `arbitratge-bank`,
  Pages en `arbitratge.futbolbotons.cat`, Releases como canal del APK.
- **ADR-0004 — Firma del APK y clave del sello**: separación de claves (§5.1), F1 vs F2, política de rotación
  y digest pineado.

---

## Preguntas abiertas para el usuario

1. **Organización y repos**: ¿confirmas `cfbbarcelona/arbitratge-mobile` (público) y
   `cfbbarcelona/arbitratge-bank` (privado)? ¿Tienes permisos de *owner* en la org para la verificación
   de dominio, los environments y los rulesets?
2. **Subdominio**: ¿`arbitratge.futbolbotons.cat` (recomendado) u otro nombre? ¿Quién gestiona el DNS
   de `futbolbotons.cat`?
3. **Inyección del banco y firma en fase 1**: ¿empezamos con firma y banco **en local** (recomendado) y
   pasamos a CI con environment protegido cuando haya segunda persona? ¿O quieres CI desde el principio?
4. **Enlace del APK**: ¿enlazar desde Releases (recomendado) o copiar el APK a Pages para tener URL propia?
5. **Keystore**: ¿tipo de clave (RSA 4096 recomendado por compatibilidad, o EC P-256)? ¿Aceptas una tercera
   copia en poder de una segunda persona de la CFBB? ¿Quién custodia las copias?
6. **Licencia del código público**: ¿qué licencia (p. ej. MIT, Apache-2.0 o "todos los derechos reservados")?
   Hace falta antes del primer push.
7. **`applicationId` definitivo**: ¿`cat.futbolbotons.arbitratge` para el candidato? Sin app nativa verificadora en el MVP (ADR-0001) no se reserva un segundo identificador. Es irreversible sin
   re-registro.
8. **Verificación de desarrolladores de Android**: ¿la cuenta la titula una persona o la CFBB? ¿Full o
   *limited distribution*? ¿Se acepta el flujo avanzado para aspirantes si no se verifica? (Confirmar el
   límite de 20 o 50 dispositivos.)
9. **Aviso de versión** (`03` §9.2): ¿`INTERNET` solo para `latest.json` o cero permisos de red?
10. **Orden de publicación de la PWA**: ¿se publica en la primera release pública o tras probar instalación y modo avión en Android (ADR-0001, §6.7)? Recomendación: después de esas pruebas, sin cambiar el alcance.
11. **Política de versiones de la PWA**: ¿misma versión que el APK o versión propia?
12. **Contacto y aviso legal**: ¿qué correo o canal del club aparece en la web de descarga y en el
    aviso de privacidad? (Lo redacta el usuario o el asesor del club.)
13. **Pruebas con banco real**: las prereleases públicas no llevan banco real (I5). ¿Se prueba con un APK de build local entregado por canal directo del mantenedor (recomendado) o con una prerelease en un repositorio privado (más complejo)?
14. **Dudas residuales de este documento** (no bloquean el diseño): (a) comportamiento exacto de Pages con
    `Cache-Control` y de *releases* inmutables con el flujo *draft*; (b) condiciones de Actions en repos
    públicos y de Pages/Releases en el momento de implementar; (c) nombre de la opción de AGP para desactivar
    v1; (d) si el gestor de contraseñas elegido admite adjuntos cifrados; (e) licencia y vigencia del
    decodificador QR elegido; (f) si `attest-build-provenance` en repo público publica datos más allá de
    repo/commit/digest.
15. **Representación de la clave del sello en el APK** (§5.8): ¿formato concreto que fija el contrato y prueba de correspondencia con `trusted-keys/` en release? Sin cifrado ni ofuscación como control de seguridad.
16. **`bank_version` en la credencial** (§7.2, I7): ¿se incluye en el payload firmado (recomendado) o se mantiene solo en local? `02` §3.7.5 admite ambos casos; es decisión residual (`02` §3.8.1). Afecta a `03` §5.2 y a la PWA.
17. **G5 en CI** (§5.8): ¿se acepta que `release.yml` dependa de un emulador API 26 en el runner (minutos de Actions y KVM [PV]), o se usa la alternativa local del mantenedor con resumen adjunto a la release? Esta última **no** es una puerta automática.
