# 03 — Arquitectura móvil (stack, módulos, banco, reloj, updates)

> Autor: `mobile-architect` · **Revisión 4 (cierre de diseño)**: alineada con el **contrato
> canónico `ARB1` de [`02` §3.7](02-credencial-y-amenazas.md)** (rev. 4). Todo lo que la tabla de
> propiedad de `02` §3.8.1 asigna a `02` (formato del token, bytes firmados, ES256 `R‖S`,
> base64url, UTF-8/JSON, campos y patrones, nombres reservados, límites, normalización de entrada,
> orden de verificación y códigos, esquema de `trusted-keys/`, vectores) **se referencia, no se
> repite aquí**: este documento ya no especifica esas reglas, solo cómo las implementa la app
> (§4, §6). Si algo de este documento parece contradecirlo, **prevalece `02` §3.7**. La rev. 3
> corregía las incidencias B1, B2, B3, I1–I5 e I7 de
> [`reviews/revision-final-diseno.md`](reviews/revision-final-diseno.md); la revisión 2 ya había
> sustituido a la 1, que partía de un organizador que corregía y firmaba, atestación de
> Keystore y banco cifrado por pregunta. Parte de [`01-requisitos.md`](01-requisitos.md) y
> [`02-credencial-y-amenazas.md`](02-credencial-y-amenazas.md). Fase de diseño: no hay código.
>
> **Prevalecen los ADR aprobados** ([0001](../adr/0001-credencial-sellada-y-verificador-publico.md):
> firma asimétrica con clave embebida en el build + verificador **PWA** pública, sin app Android
> de organizadores en el MVP; [0002](../adr/0002-banco-privado-en-build-y-apk.md): banco real fuera
> del repo público, inyectado en el build, dentro del APK y **sin cifrar ni ofuscar**). Las
> comparativas de este documento que mencionan checksum, `app-verifier` u ofuscación del banco
> son **históricas** y están marcadas como tales; **no son opciones vigentes**.
>
> Convención: **[H]** hecho verificado (fuente) · **[D]** decisión del usuario · **[S]**
> suposición/recomendación propia · **[PV]** pendiente de verificar en un prototipo (§12).
> Consultas web de esta revisión: 2026-11-19.

---

## 0. Resumen ejecutivo

1. **Criterio rector [D]**: simple, mantenible, útil para aprender, apto para miembros mayores,
   contexto de confianza social. Autoservicio: **la app corrige**, muestra aciertos/fallos,
   detalle de cada fallo y, cuando exista, una **explicación breve**. Banco, respuestas y
   corrección **dentro del APK**; sin organizador, sin backend, sin atestación.
2. **Stack: Android nativo, Kotlin + Jetpack Compose** (§2). Ya **no** lo justifica el Keystore
   (la rev. 1 sí): lo justifican accesibilidad para mayores, robustez ante proceso muerto,
   PDF/QR sin dependencias pesadas, APK pequeño y un solo lenguaje. **Alternativa seria:**
   Capacitor si el mantenedor prefiere JS (§2.3). iOS: no se persigue (§2.4).
3. **Arquitectura mínima** (§3–§4): **una sola app Android** (`app-candidate`, sin cámara ni
   lector) sobre dos módulos JVM puros (`core-exam`, `core-credential`). La **verificación**
   es una **PWA estática** (`web/verificador/`, ADR 0001), no una app Android. Una sola
   `Activity`, estado persistido en Room, la UI se reconstruye desde el estado (rotación,
   cierre y muerte del proceso son el mismo caso). **Regla de oro: no se avanza ni se finaliza
   sin haber persistido antes la respuesta** (§4.3, §4.5).
4. **Banco** (§5): **un único JSON versionado** (preguntas + respuestas + `explanation`
   opcional) en un **repo privado o carpeta local fuera de git**; el build lo valida y lo
   empaqueta **en claro** en el APK (sin cifrar ni ofuscar, ADR 0002). El repo público solo trae un
   banco **de muestra ficticio**. Nunca hay un fichero de banco servido por HTTP ni en git
   público. Banco nuevo = APK nuevo.
5. **Credencial** (§6): PDF A4 + QR generados en el móvil (`PdfDocument` + ZXing core). Token
   sellado con **ECDSA P-256** y clave inyectada en el build (ADR 0001), conforme al **contrato
   único Android↔PWA de `02` §3.7** (aquí solo se referencia, §6.2). **La clave embebida es
   extraíble por un técnico y forja credenciales indistinguibles; no es una garantía
   criptográfica fuerte** (§6.3).
6. **Reloj** (§8): `elapsedRealtime` con **tiempo restante persistido y reanclado
   atómicamente** tras un reinicio; política indulgente (el apagado no cuenta). La hora de
   pared es informativa y se etiqueta "del dispositiu".
7. **Actualizaciones** (§9): el APK se descarga de la web; aviso opcional con un `latest.json`
   público **sin datos secretos** (la app candidata pedirá `INTERNET` solo para eso, decisión
   abierta). La verificación de desarrolladores de Android (2027) condiciona `applicationId` y
   firma: hay que fijarlos antes de publicar. Compatibilidad honesta con PWA antiguas (§7).
8. **minSdk 26 / targetSdk 36**, sin permisos salvo `INTERNET` (opcional); **la app Android no
   pide `CAMERA`** (la cámara es de la PWA); catalán por defecto; edge-to-edge (§10).

### 0.1 Qué desaparece respecto a la rev. 1 y por qué

| Rev. 1 | Rev. 2 | Motivo |
|---|---|---|
| Invitación firmada, hoja de respuestas, aprovisionamiento del organizador, corrección externa | **Eliminado** | Sin organizador [D1]. |
| `core/grading` solo en el organizador; clave de respuestas fuera del APK | La corrección y las respuestas viven en el APK | [D2] |
| Android Keystore, Key Attestation, StrongBox, `BiometricPrompt`, minSdk 30 del organizador | **Eliminado** | Atestación fuera de alcance (02 §1.3). Un solo `minSdk`. |
| Cifrado por pregunta (O4b), `K_bank`, claves en QR de invitación | **Eliminado** | Pierde sentido con respuestas en el APK y feedback visible. |
| CBOR/COSE propio, base45, QR múltiple | Token texto simple (`02` §3.7.1) | Menos código y menos superficie; la PWA verifica con WebCrypto. |
| `FLAG_SECURE`, instantánea de preguntas cifrada, cadena de hashes de eventos | **Eliminado** | No aportan con este perfil; `FLAG_SECURE` estorba al feedback. |
| Banco dividido en `content` + `key` | Un único fichero | No hay clave que separar del APK; evita el problema de `bank_hash` (rev. intermedia B4). |
| App de organizadores que emite/firma | **PWA verificadora** pública y sin secretos; ninguna app Android de organizadores en el MVP | ADR 0001 |

### 0.2 Qué se conserva de la revisión intermedia (`reviews/revision-intermedia-01-03.md`)

| Hallazgo | Tratamiento aquí |
|---|---|
| B1 confidencialidad del texto | Aceptado y explicitado: el texto y las respuestas están en el APK y el feedback los revela (§5.1). Lo que se protege es el **fuente** (git/HTTP). |
| B3 `iat` no fiable | No se promete fecha fiable: "data del dispositiu" (§8, §6.3). |
| B4 `bank_hash` | Un artefacto, un hash calculado en el build privado; **no** se publica hash de respuestas. |
| I4 inconsistencias (`min(N, activas)`) | Validación **estricta en build**: falla si `num_questions > activas` (§5.3). |
| M1 comprobaciones de CI por nombre | Se complementan con un *canary* (§5.5). |
| M2 campos desconocidos | Sin campo `crit`: lo que el verificador deba entender exige `ARB2`; los miembros desconocidos se ignoran (`02` §3.7.5). |

---

## 1. Hechos y restricciones de partida

### 1.1 Del proyecto base [H]

- `supabase/functions/_shared/examLogic.ts`: barajado Fisher-Yates con `Math.random`
  (`:54-61`); corrección por **conjunto exacto** `sameOptionSet` (`:121-126`) y `finalizeExam`
  (`:140-173` aprox.): sin respuesta = fallo, sin crédito parcial; "aprobado" = `correct_count
  >= pass_threshold_count` (`isPassed`, `:175`). No devuelve feedback por pregunta.
- `assets/api.js:12` hace `fetch(.../functions/v1/${name})`: el JS del base es un **cliente
  fino** de las Edge Functions; la lógica (selección, corrección, reloj) está en el servidor.
  **Reutilizar el JS = reutilizar HTML/CSS**, no lógica. Hay que reescribir la lógica (~100
  líneas) en cualquier stack.
- `scripts/questions.sql` (75 filas, esquema antiguo `correct_option_index` singular, `type =
  'multiple_choice'` para preguntas de una respuesta, líneas 1–2) y `.gitignore:6` lo ignora.
  Pero el repo base público ya expone el banco actual (75 preguntas) con su índice de respuesta
  (hallazgo de `02` §5.2 [H, verificado por `trust-architect`]; `02` no reproduce ruta ni
  contenido para no servir de índice, y este documento tampoco). **Las 75 preguntas actuales
  deben tratarse como ya públicas.**

### 1.2 Entorno Android (consultado 2026-11-19; reverificar al empezar)

| Hecho | Fuente |
|---|---|
| Android 17 estable (API 37); Android 16 (API 36) es la versión más extendida (25,4 %); `minSdk` 28 cubre ≈97 % y Android 8 ≈99 % | composables.com/android-distribution-chart (oct. 2026), citado en la rev. 1 de este doc |
| Android 14+ rechaza **instalar** apps con `targetSdk` < 23/24 | bayton.org/android/android-minimum-targetsdk-matrix |
| **Verificación de desarrolladores**: desde 2026-09-30 en BR/ID/SG/TH; **global en 2027**, en dispositivos Android certificados. Vías: *full distribution* (verificar identidad), *limited distribution* (sin DNI, gratis, para aficionados) y *sideload de apps no registradas* con "flujo avanzado". Registrar un paquete exige subir un APK firmado con tu clave. | developer.android.com/developer-verification/guides y …/guides/limited-distribution |
| *Discrepancia a resolver en 05*: la guía general dice limited distribution = **hasta 20** dispositivos; la página específica dice **hasta 50** | las dos URLs anteriores |
| *Histórico, ya no aplica al MVP* (la lectura de QR es de la PWA, ADR 0001): ML Kit *barcode scanning* empaquetado ≈2,4 MB; *Google code scanner* delega en Play Services | developers.google.com/ml-kit/vision/barcode-scanning/android y …/code-scanner |
| Un APK es un ZIP; `assets/` se extrae con `unzip` sin decompilar | developer.android.com/topic/performance/reduce-apk-size (02 §1.2) |

### 1.3 Heredado de 01 y 02 que condiciona esta arquitectura

- 01 §3: examen secuencial por defecto, persistencia tras cada respuesta, comportamiento
  definido ante cierre/reinicio/cambio de hora; resultado con **fallos y detalle**; fecha "del
  dispositivo"; sin `FLAG_SECURE`.
- 01 §4.1: credencial con nombre, `credential_id`, fecha, `exam_version`, aprobado (nota
  opcional); sin DNI, email, foto, duración ni dispositivo.
- 02 §3.3/§7.1 + ADR 0001: token `payload + sello` ECDSA P-256 con clave de build (el checksum
  sin clave de 02 §7.3 es **histórico**, descartado); PDF/imagen como presentación; la PWA
  verificadora solo usa claves públicas y muestra `VÀLIDA` / `NO VÀLIDA` / `VERSIÓ
  DESCONEGUDA` / `FORMAT INCORRECTE`, nunca "autèntica".
- 02 §5.3: controles obligatorios de CI para que el banco no se filtre (no artifacts, no logs, no
  `pull_request_target`, secrets no disponibles para forks).

---

## 2. Comparativa de stack

### 2.1 Qué pesa ahora

Sin Keystore/atestación, los requisitos técnicos reales son modestos: UI de formulario + test,
**persistencia robusta** (examen en curso), **PDF y QR** offline, **accesibilidad para
mayores** y **mantenimiento por una persona** (apoyada en agentes). La **lectura de QR** ya no
es un criterio: es de la PWA verificadora (ADR 0001), no de la app Android. Eso acerca las
cuatro opciones; el desempate lo dan accesibilidad, robustez, tamaño y un solo lenguaje.

Notación: ✔ directo · ~ posible con plugin/código nativo · ✘ no recomendable.

| Criterio | A. Kotlin + Compose | B. Flutter | C. Capacitor (WebView) | D. React Native |
|---|---|---|---|---|
| **Generar PDF + QR en el móvil** | ✔ `android.graphics.pdf.PdfDocument` (framework) + ZXing `core` (solo codificación) | ✔ `pdf` + `qr_flutter` | ✔ jsPDF/librerías QR; menos control de impresión | ✔ librerías de terceros |
| **Persistencia ante proceso muerto** | ✔ Room/SQLite transaccional | ✔ `drift`/`sqflite` | ~ plugin SQLite; `localStorage`/IndexedDB del WebView más frágil | ✔ SQLite |
| **Accesibilidad TalkBack / escala de fuente 200 % para mayores** | ✔ semántica de Compose de primera clase | ✔ razonable | ~ depende del WebView y del ARIA | ✔ razonable |
| **Fiabilidad en móviles viejos** | ✔ sin runtime extra | ✔ motor propio incluido | ~ depende del *System WebView* del dispositivo [S] | ✔ runtime propio |
| **Tamaño APK** [S, PV-S3] | ≈ 3–7 MB (R8) | ≈ 8–15 MB | ≈ 3–6 MB | ≈ 10–20 MB |
| **Mantenibilidad (1 persona)** | Un lenguaje; **skills disponibles** (§11); churn de AGP/Compose | Dart (+ Kotlin solo para casos raros) | JS ya conocido del base + Kotlin para plugins/nativo | La superficie más grande (npm) |
| **Reutilización del proyecto base** | Solo contenido (banco) y reglas | Idem | HTML/CSS/DOM del frontend (no la lógica, §1.1) | Idem A |
| **Compartir código con la PWA verificadora** | ✘ (otro lenguaje; se comparte **contrato + vectores**, `02` §3.7 y §3.7.11) | ✘ | ✔ mismo JS/WebCrypto | ~ |
| **iOS futuro** | ✘ (KMP sería la vía) | ✔ | ✔ | ✔ |

### 2.2 Lectura

- **PDF y QR** ya no son diferenciales: las cuatro lo hacen. En A, el PDF sale de una clase del
  framework sin dependencias; en C el control de la impresión (tamaño del QR en mm, márgenes)
  es menos directo [S].
- **Mayores y TalkBack**: el requisito más específico de este proyecto. Compose da semántica
  nativa (roles radio/checkbox, `liveRegion`, escala de fuente del sistema). En WebView
  funciona, pero depende del navegador del móvil, que en un móvil antiguo puede estar sin
  actualizar [S].
- **Robustez**: "recuperar el examen tras la muerte del proceso" es un caso de uso de primera
  clase (§4.4); A/B/D lo resuelven con una base de datos; en C el almacenamiento del WebView
  puede ser limpiado por el sistema en situaciones de poca memoria [S, PV].
- **Reutilizar el JS del base** (alternativa C) **no ahorra lógica**, solo HTML/CSS (§1.1).
  Su ventaja real sería compartir con la PWA ~100–150 líneas de verificación ECDSA/JSON:
  no justifica cambiar de stack, porque el contrato interoperable (`02` §3.7) y los vectores
  compartidos ya aseguran que dos implementaciones coincidan.

### 2.3 Recomendación

**A. Kotlin + Jetpack Compose**, un proyecto Gradle, Kotlin 2.x, AGP 9.x (skill `agp-9-upgrade`
disponible; versiones exactas al empezar). Dependencias mínimas (§4.6).

**Cuándo cambiaría a C (Capacitor) [S]:** si el mantenedor prefiere seguir en JS/TS y no quiere
aprender Kotlin, **y** acepta (1) validar TalkBack con el WebView de móviles reales de los
miembros y (2) hacer un `StorageAdapter` robusto sobre SQLite. Es una alternativa **legítima y más barata de aprender** si el criterio es
reutilizar el conocimiento web; no la recomiendo como primera opción por accesibilidad y
robustez, pero **no es un error**. **Flutter (B)** solo si iOS pasara a ser un requisito.

Una **PWA servida desde GitHub Pages queda descartada como app de examen**: el banco sería un
fichero HTTP público, contra [D2] y ADR 0002. (La PWA **sí** es la herramienta de
verificación aprobada por ADR 0001: no contiene banco ni secretos, solo claves públicas.)

### 2.4 iOS

No se persigue [D]. Obstáculo real: **distribución** (no hay "APK fuera de tienda" en iOS salvo
TestFlight, programa empresarial o mercados alternativos solo UE [S, no verificado]), no el
stack. Preparación sin coste: `core-exam` y `core-credential` son Kotlin/JVM puro con formatos
neutrales (JSON, ECDSA, texto). Si algún día hay iOS, se estudia KMP. La **PWA verificadora
es multiplataforma por naturaleza**: un organizador con iPhone podría usarla [PV: cámara,
instalación y modo sin conexión en Safari no probados] o, en su defecto, **leer el PDF a ojo**
(la credencial es legible, 01 §4.2.1).

---

## 3. Estructura del proyecto y verificación (PWA)

### 3.1 Módulos

```
arbitratge-mobile/                         (repo PÚBLICO)
├─ app-candidate/    com.<dominio>.arbitratge                 ÚNICO APK (público)
├─ core-exam/        Kotlin/JVM puro: modelo y validación del banco, selección, barajado,
│                    reglas, corrección + feedback, máquina de estados, reloj (interfaz)
├─ core-credential/  Kotlin/JVM puro: emisor y verificador del contrato `02` §3.7 (token,
│                    sello, verificación y códigos de resultado de `02` §3.7.8; la verificación
│                    en Kotlin sirve a la autoverificación al emitir, `02` §3.7.10, a las
│                    pruebas y al control de release)
├─ android-common/   tema Material 3, componentes, recursos (catalán), QR→Bitmap, utilidades
│                    de accesibilidad
├─ bank-sample/      banco FICTICIO (5–8 preguntas absurdas) + clave `dev` de desarrollo
├─ tools/            (opcional) tarea Gradle JavaExec que valida y empaqueta el banco
├─ trusted-keys/     claves PÚBLICAS de sello por `kid`, esquema de `02` §3.7.9 (no son secretas)
├─ test-vectors/     vectores compartidos Android↔PWA de `02` §3.7.11, clave `test`
└─ web/              página de descarga + `verificador/` (PWA estática, ADR 0001; detalle en 05 §6.7)

(repo PRIVADO — o carpeta local fuera de git)
arbitratge-bank/
├─ bank/bank.json             banco real: preguntas, respuestas, explicaciones
└─ migration/                 script Node de un solo uso (SQL → JSON); no es parte del build
```

Reglas (las impone Gradle, no la disciplina):

- `core-exam` y `core-credential` **no** dependen de `android.*` ni entre sí.
- La PWA (`web/verificador/`) es JS sin dependencia de Kotlin: **no comparte código**, comparte
  el **contrato** (`02` §3.7) y los **`test-vectors/`** (`02` §3.7.11); CI ejecuta ambas
  implementaciones.
  Por construcción la PWA no contiene banco ni lógica de corrección, solo claves públicas.
- Ni la clave privada del sello ni el banco real existen en el repo público; los inyecta el
  build (§5.4).
- Con `minSdk` 26, `java.time` está disponible sin *desugaring* [S].

### 3.2 Verificador/lector de QR: PWA (decisión cerrada por ADR 0001)

**Vigente:** el verificador es una **PWA estática pública** (`web/verificador/`), instalable y
utilizable sin conexión **una vez cargados todos sus recursos** [PV, ver §7], sin backend y sin
secretos. **No hay `app-verifier`, ni modo verificador dentro de `app-candidate`, ni segundo
APK en el MVP**; la app Android del candidato **no pide cámara** ni contiene lector. Que un
organizador use la PWA con una credencial concreta es **optativo** [D6] y no condiciona la
emisión. Revisitar una app Android de organizadores solo si el soporte móvil, la cámara o la
revocación de fase 2 lo justifican (ADR 0001).

> *Histórico (rev. 2, superado):* se compararon `app-verifier` separada, modo dentro del
> candidato, *flavor*, página estática y sin verificador. La rev. 2 recomendaba `app-verifier`;
> el ADR 0001 eligió la página estática en su forma PWA. No reabrir.

Implicaciones para esta arquitectura: (1) el contrato de firma/parseo debe ser **implementable
en JS/WebCrypto** y probado Android→PWA (`02` §3.7, §3.7.11); (2) la **cadena de claves públicas** llega a la
PWA por despliegue (`trusted-keys/` → `verificador/keys/`, 05 §6.7), no por APK; (3) la PWA
depende del dominio oficial y del service worker (05), no de Google Play ni de Gradle.

### 3.3 Qué hace el verificador (esbozo; la UX es de 04, la publicación de 05)

- Entradas: **cámara** (`getUserMedia` + decodificador QR vendido, 05 §6.7), **importar imagen**
  y **pegar texto**. Funcionamiento real offline, cámara y compatibilidad de navegador:
  **[PV]**, a probar en Android con Chrome/Edge antes de publicar (ADR 0001).
- Salida: uno de cuatro resultados (ADR 0001) con el mapeo de códigos de `02` §3.7.8:
  `VÀLIDA` / `NO VÀLIDA` / `VERSIÓ DESCONEGUDA` / `FORMAT INCORRECTE`; datos decodificados en
  letra grande (nombre, resultado, fecha "del dispositiu", `exv`, `cid` corto según `02` §3.7.5,
  `bv` si existe; qué datos y cómo se muestran en cada código: `02` §3.7.8 y `04`) y
  **comparación explícita con los datos visibles** del documento y de la persona. Recordatorio
  "Credencial emesa en autoservei. No és una garantia forta" y el aviso de ADR 0001.
- Rechazo estricto: `kid`, versión o algoritmo desconocidos **nunca se aceptan por defecto**
  (`02` §3.7.8). No hay campos críticos: lo que un verificador deba entender exige `ARB2`
  (`02` §3.7.5).
- **Firma correcta ≠ vigencia.** En el MVP **no se promete caducidad** y no se emite `exp`
  (§7.3; `exp` es nombre reservado en `v=1`, `02` §3.7.5). Si se introdujera, la PWA debe mostrar por separado "firma correcta" y "vigent", con
  la advertencia de que la fecha de emisión la pone el móvil del candidato y la de
  comprobación depende del reloj del dispositivo del verificador.

---

## 4. Arquitectura de `app-candidate`

### 4.1 Capas

```
UI (Compose; un único estado por pantalla)                    app-candidate, android-common
   ▲ StateFlow<UiState>         │ eventos
ViewModels (orquestan; sin reglas de negocio)
   ▲                            │
Dominio (Kotlin puro, test en JVM)                            core-exam, core-credential
   ▲ interfaces (puertos)       │
Plataforma Android                                            Room, PdfDocument, FileProvider, reloj
```

- **Una sola `Activity`** (`launchMode="singleTask"`) y un **`Stage` raíz derivado del estado
  persistido**, no un grafo de navegación con pila: al abrir, rotar o tras muerte del proceso,
  la pantalla se **recompone desde la base de datos** (§4.4).
- **Inyección manual** con un `AppContainer` (el grafo es pequeño); Hilt es válido, decisión de
  implementación.
- **Puertos** (interfaces) que dan testabilidad sin añadir capas: `ExamClock`
  (`monotonicMs()`, `wallMs()`, `bootCount(): Int?` — nulo si no es fiable), `Rng`
  (`SecureRandom` en producción, semilla fija en pruebas), `SessionStore` (**las pruebas
  inyectan uno que falla**, §11), `CredentialSealer` (§6.4), `CredentialExporter`.
- Dominio: **`ExamEngine`** (puro): `start(bank, rules, rng) → Session` (**valida las reglas
  al arrancar**, §5.3; si no son válidas, no hay examen), `answer(session, i, ids,
  remainingAtTap) → Session`, `grade(session) → ExamResult`.

### 4.2 Qué se muestra tras corregir (modelo de feedback) [D1]

```
ExamResult { correct, wrong, unanswered, total, threshold, passed, elapsedMs,
             items: [QuestionResult] }
QuestionResult { questionId, text, options(en el orden mostrado), selectedIds, correctIds,
                 isCorrect, explanation? , ref? }
```

- **Fallos = total − aciertos** (las sin responder cuentan como fallo; se muestra el desglose).
- La pantalla de resultado muestra: aciertos/fallos/umbral/aprobado, tiempo, y **cada pregunta
  fallada** con lo que marcó y **siempre la respuesta correcta**; el **motivo** (`explanation`)
  solo **cuando está disponible**. Los textos de la app (04) no deben prometer "el porqué" de
  cada fallo: prometen la respuesta correcta y, cuando exista, su explicación [I2].
- La pantalla de resultado se pinta **solo desde estado ya persistido** (§4.3): nunca se
  muestra un "Aprovat/Suspens" definitivo que no esté guardado. El resto de preguntas (acertadas) se ofrece con "Veure-les totes" — por
  defecto **fallos primero**; el resto es decisión editorial (01 pregunta 4).
- `explanation` y `ref` son opcionales: **la app funciona igual sin ellas** (trabajo editorial
  posterior).
- El feedback se calcula desde `snapshot + answers` guardados: es **determinista y
  recalculable** (se puede reabrir el último resultado sin estado extra).

### 4.3 Máquina de estados del examen

```
 [Home] → [Briefing] → [Identifying: nombre] → [Ready: nombre confirmado] → [InProgress(0)]

 [InProgress(i)] ── respuesta i GUARDADA, i<N-1 ──► [InProgress(i+1)]
 [InProgress(*)] ── última respuesta GUARDADA ─────► [Finished(Completed)]
 [InProgress(*)] ── tiempo agotado (y nada pendiente) ► [Finished(TimedOut)]
 [InProgress(*)] ── abandonar (confirmado Y guardado) ► [Abandoned] → [Home]

 [Finished] (FINISHED + correct_count escritos en UNA transacción)
     ├─ no aprobado ─► [Result(Failed)] ── "Tornar a l'inici" ─► [Home]
     └─ aprobado    ► [Result(Passed)] persistido ─► emisión inmediata y automática (sella con el nombre congelado) ─► [Credential]

 Ante un fallo de guardado, en cualquier punto de escritura, se PERMANECE en el estado
 actual (con aviso y reintento); jamás se avanza. Ver "Fallos de persistencia".
```

**Invariante [B3]:** *lo que la pantalla da por confirmado (pregunta siguiente, fin, resultado,
credencial) ya está en la base de datos.* **Nunca se avanza, ni se finaliza, ni se corrige,
ni se emite sin haber persistido antes.** La respuesta marcada pero no guardada es solo
estado de pantalla.

| Transición | Guardia | Efecto (transacción **antes** de pintar el nuevo estado) |
|---|---|---|
| `Ready → InProgress(0)` ("Començar ara") | nombre válido según `02` §3.7.5.1 (§6.1) y **confirmado por el usuario en `Ready`** (C3 de 04, con "Canviar" que vuelve a `Identifying`) | Crea sesión: `session_id`, **`cid` (UUID) ya generado**, `bank_version`, `exam_version`, reglas (validadas, §5.3), **instantánea de las N preguntas con su orden barajado**, **`candidate_name` (congelado e inmutable desde aquí)**, reloj inicial (§8.2: `remaining_ms_saved = limit_ms`, `anchor_mono`, `anchor_boot`), `start_wall`. **El intento queda iniciado.** |
| `InProgress(i) → InProgress(i+1)` ("Següent") | `remainingAtTap ≥ 0` e `i == actual` | **Una sola transacción:** fila `answer(session, i, ids, remaining_at_tap)` (clave única `(session, i)` ⇒ idempotente) + `current_index = i+1` + punto de control del reloj (§8.2). La UI avanza **solo tras el *commit*** |
| `InProgress(N−1) → Finished(Completed)` | igual | **Una sola transacción:** `answer` + `state = FINISHED` + `finish_reason` + `correct_count` (calculado por `grade` desde la instantánea) |
| `InProgress → Finished(TimedOut)` | `remaining ≤ 0` (en el tick, al guardar y al reanudar) **y no hay respuesta pendiente de guardar con `remaining_at_tap ≥ 0`** | **Una sola transacción:** `state = FINISHED(TimedOut)` + `correct_count`. `finished_wall` es solo informativo |
| `InProgress → Abandoned` | el usuario confirma "Abandonar" | `state = ABANDONED` (terminal), sin resultado ni credencial. Si no se puede guardar, se sigue en `InProgress` con aviso |
| `Finished → Result` | estado `FINISHED` **ya persistido** | Solo lectura: se muestra lo guardado (`correct_count`; ítems recalculados de `snapshot + answers`) |
| `Result(Passed) → Credential` (automática, sin pantalla ni paso intermedio del usuario) | estado `FINISHED` aprobado **ya persistido** y **no existe fila `credential`** para el `cid` | Ejecuta **inmediatamente** la **emisión de `02` §3.7.10** (construir y serializar el payload con `candidate_name` de la sesión, sellar, **autoverificar con `OK`**) en memoria y, en una transacción, inserta `credential` (`token` exacto, `name` = `candidate_name`, `iat`, `kid`, `exv`, `bv`). Si el sellado, la autoverificación (código ≠ `OK`, error interno) o el *insert* fallan: se sigue en `Result(Passed)` con "Encara no s'ha pogut crear la credencial" y reintento (04 §2.10), y **no queda credencial a medias** |
| (recuperación) | sesión `FINISHED` aprobada **sin** fila `credential` | Al reabrir se muestra `Result(Passed)` y se **reintenta la emisión automáticamente** con el mismo `candidate_name`; el `cid` ya está reservado en la sesión ⇒ reintento **idempotente** |

**Nombre y sellado [I2] (alineado con 04 §2.3).** El nombre se pide en `Identifying` (con el
aviso "Aquest nom apareixerà a la credencial") y se **confirma o cambia en `Ready`** (C3 de 04)
**antes de `Ready → InProgress`**. Al pulsar "Començar ara" se **congela** en
`session.candidate_name` y es **inmutable**: no hay `ConfirmName` tras aprobar, ni edición
posterior; cambiarlo exige repetir el examen (nueva sesión, nuevo `cid`). La emisión es
**inmediata** una vez persistido el resultado aprobado, sin intervención del usuario ni de un
organizador, y usa exactamente ese nombre [S].

**Fallos de persistencia [B3].** Son un estado de primera clase, no una excepción silenciosa:

- **Respuesta no guardada:** la selección se **mantiene visible** y **no se pasa a la
  siguiente pregunta ni se finaliza**. Se reintenta de forma automática un par de veces con
  espera corta; si siguen los fallos se muestra, en català y sin jerga, un aviso con
  **"Torna-ho a provar"** (p. ej. "No hem pogut desar la teva resposta. Comprova que el
  mòbil té espai lliure i torna-ho a provar"; el texto definitivo es de 04). **Queda
  prohibido** un aviso no modal que deje continuar sin guardar (corrige la propuesta de 04
  señalada en `reviews/revision-final-diseno.md` B3).
- **El tiempo no se congela, pero la respuesta no se pierde por culpa del almacenamiento:**
  `remaining_at_tap` se captura **al tocar**, no al escribir; una respuesta tocada con tiempo
  restante ≥ 0 es válida aunque se guarde unos segundos después (mientras la sesión siga
  `IN_PROGRESS`).
- **Si no se consigue guardar** (se agota el tiempo o el usuario desiste): estado
  `StorageError` con **reintentar** y **abandonar**. **No se corrige ni se emite credencial
  basada en datos incompletos**, y no se presenta un resultado definitivo. "Abandonar" cierra
  la sesión (`ABANDONED`) solo si el guardado lo permite; si ni eso, el estado queda como
  estaba y se recupera al reabrir.
- **Fallo al guardar el resultado** (`FINISHED` + `correct_count`) **o al emitir**
  (`credential`): igual; el resultado o la credencial no se muestran como existentes hasta
  que el *commit* tiene éxito. Fallar al **exportar** el PDF/PNG es independiente: el token
  ya está guardado y se reintenta la exportación.
- **Muerte del proceso con respuesta sin guardar:** al reabrir, la pregunta `i` vuelve a estar
  sin responder (es el estado persistido) y el reloj se recupera del último punto de control
  (§8.2).

**Otras reglas de la máquina:**

- **Un solo examen activo**: restricción única en BD (`state = IN_PROGRESS`).
- Estados terminales no se reabren. **Sin límite de intentos** (aceptado, 01 R5).
- **Solo se emite credencial en `Finished` con `correct_count ≥ umbral` persistido**: no hay
  credencial en suspenso, abandono, tiempo agotado sin aprobar ni con error de persistencia
  (tests en §11).
- **Navegación**: secuencial sin retroceso por defecto (01 §1.3). El motor lo parametriza
  (`sequential` en las reglas): relajarlo por fin pedagógico no cambia la arquitectura.
- **Modo práctica** (01 pregunta 5): la sesión tiene `kind = EXAM | PRACTICE`; `PRACTICE` no
  emite credencial. No se construye hasta que se decida; el campo cuesta nada.

### 4.4 Persistencia local

Almacenamiento privado de la app, sin permisos. **Room/SQLite** (tres tablas):

| Tabla | Contenido |
|---|---|
| `session` | `id`, `kind`, `state`, `cid` (reservado al empezar), `candidate_name` (congelado al crear la sesión; inmutable), `bank_version`, `exam_version`, `rules` (JSON), `snapshot` (JSON de las N preguntas **incluyendo respuestas y explicaciones**, con orden barajado), `current_index`, **reloj:** `limit_ms`, `remaining_ms_saved`, `anchor_mono`, `anchor_boot` (§8.2); informativos: `start_wall`, `finished_wall`; `finish_reason`, `correct_count` |
| `answer` | `session_id`, `q_index`, `selected_ids`, `remaining_at_tap_ms` (PK `(session_id, q_index)`) |
| `credential` | `cid`, `session_id`, `token` (texto exacto del QR), `name`, `iat` (texto `AAAA-MM-DD`, `02` §3.7.5), `kid`, `exv`, `bv`, `pdf_exported` |

- **La instantánea de preguntas va en la sesión** (≈20–40 KB): el examen y su feedback no
  dependen del banco del APK, así que **actualizar el APK en pleno examen o consultar un
  intento antiguo no se rompe** (01 §3.2). Sin cifrado: el banco ya está en el APK.
- **Historial local** (útil para aprender, 01 CU-C10): se conservan las últimas ~20 sesiones
  terminadas; es consulta, no registro fiable (borrar datos lo pierde).
- **Credenciales**: se guarda el **token tal cual** (nunca se re-serializa); la UI lo renderiza
  con el parser. La copia portable es el PDF exportado (§6.5): **`allowBackup=false`** y
  `dataExtractionRules` vacías (evita clonar estado por `adb backup`; la recuperación es el
  PDF/imagen guardado, 01 CU-C9). Alternativa de producto: backup en la nube de la credencial
  (más cómodo para cambiar de móvil, más superficie) — pregunta abierta.
- **Reglas de robustez**: (1) *persistir y luego avanzar* (invariante B3, §4.3: ningún
  avance, fin, resultado ni credencial sin *commit* previo); (2) escrituras en transacción, WAL;
  `synchronous=FULL` es barato con ≈20 escrituras [S, PV]; (3) migraciones de Room probadas
  con `MigrationTestHelper`; (4) nada crítico vive solo en un `ViewModel`/`SavedStateHandle`.

### 4.5 Robustez ante cierre, rotación y proceso muerto

| Situación | Comportamiento |
|---|---|
| Rotación / multiventana / plegable | Se recrea la `Activity`; estado en BD + `StateFlow`. No se bloquea orientación (accesibilidad). |
| Muerte del proceso / *swipe-away* | Al arrancar, el `Router` lee la sesión: si `IN_PROGRESS` reancla/evalúa el reloj (§8.2) y reanuda en la pregunta persistida o finaliza por tiempo. Una respuesta tocada y no guardada se pierde (la pregunta reaparece sin responder); nunca se da por guardada. |
| Cierre voluntario | El tiempo **sigue corriendo** (cálculo perezoso desde el último punto de control, sin servicio ni alarmas ni notificaciones). Al volver se restaura. |
| Pantalla apagada / Doze | `elapsedRealtime` incluye el sueño [H, doc. `SystemClock`]. `FLAG_KEEP_SCREEN_ON` mientras hay examen (evita bloqueos involuntarios). |
| Llamada, notificación, cambio de app | Sin penalización ni registro (no hay revisor). |
| Reinicio / batería agotada | Política indulgente con **reanclaje atómico** del reloj (§8.3). |
| Fallo al guardar (disco lleno, error de BD) | Se permanece en la pantalla con aviso y reintento; no se avanza (§4.3). |
| Actualización del APK | La sesión se termina con su propia instantánea (§4.4). |
| Doble toque / reintento | Idempotente por clave única de `answer`. |
| Batería < 20 % al empezar | Aviso en `Briefing`. |

### 4.6 Dependencias (mínimas, cada una justificada)

| Necesidad | Propuesta | Alternativa |
|---|---|---|
| UI | Compose + Material 3 (BOM) | — |
| Persistencia | Room + KSP | SQLite directo |
| JSON | `kotlinx.serialization` | Moshi |
| Firma/verificación | **JCA de Android/JDK** (`Signature SHA256withECDSA`, `KeyFactory EC` con PKCS#8) | — |
| QR generar | ZXing `core` (solo codificación; Apache-2.0) | Generador propio |
| PDF | `android.graphics.pdf.PdfDocument` (framework) | — |
| Compartir/guardar | `FileProvider`, `ACTION_CREATE_DOCUMENT`, *share sheet* (sin permisos) | — |

Sin analítica, sin *crash reporters*, sin SDK de terceros con red (01 §5). **Sin CameraX ni ML
Kit**: la lectura de QR es de la PWA (ADR 0001).

---

## 5. Banco de preguntas

### 5.1 Qué se protege y qué no (sin rodeos)

- **Está en el APK** y el APK es un ZIP público: quien lo descargue lo extrae. **Aceptado
  [D2]**, y además la app **enseña** las respuestas de los fallos [D1]; con 20 de 75 preguntas
  por examen, tras ~5 intentos se ha visto ≈79 % del banco (02 §5.1). Es pedagogía, no fuga.
- **Lo que sí se exige [D2]**: el banco **no** está en el historial de un repo público ni como
  fichero/URL HTTP independiente (Pages, Releases como `bank.json`, artifacts de Actions).
- Las 75 preguntas actuales **ya son públicas** (§1.1): la confidencialidad del fuente solo
  vale para **preguntas nuevas, reescritas y explicaciones**. El repo privado sigue siendo la
  fuente de verdad única del banco completo.

### 5.2 Formato: un único JSON versionado

**Ejemplo único y ficticio** (es una versión reducida del banco de muestra de `bank-sample/`; el contenido
es absurdo a propósito y **no procede del banco real ni del proyecto base**. El banco real
nunca se reproduce en la documentación, ADR 0002):

```json
{
  "schema": 1,
  "bank_version": "0.0.0-sample",
  "locale": "ca",
  "sample": true,
  "rules": {
    "exam_version": "sample-1",
    "num_questions": 2,
    "time_limit_minutes": 5,
    "pass_threshold_count": 1,
    "shuffle_questions": true,
    "shuffle_options": true,
    "sequential": true
  },
  "questions": [
    {
      "id": "sample-0001",
      "type": "single_choice",
      "status": "active",
      "text": "[FICTICI] Quantes potes té un unicorn de proves?",
      "options": [
        { "id": "a", "text": "Quatre (resposta fictícia)" },
        { "id": "b", "text": "Set (resposta fictícia)" }
      ],
      "correct": ["a"],
      "explanation": "[FICTICI] Text d'exemple; no és cap norma real."
    },
    {
      "id": "sample-0002",
      "type": "true_false",
      "status": "active",
      "text": "[FICTICI] Un núvol de mostra pesa més que una pedra de mostra.",
      "options": [
        { "id": "t", "text": "Vertader" },
        { "id": "f", "text": "Fals" }
      ],
      "correct": ["f"]
    }
  ]
}
```

(El banco real añade `explanation`/`ref`/`tags` a discreción y un campo privado `canary`,
§5.5. Toda cadena de un banco de muestra empieza por `[FICTICI]`, para que sea imposible
confundirla con contenido real en capturas, logs o PDF de pruebas.)

Decisiones de diseño:

1. **Opciones con `id` estable** (`a`,`b`,…), no por índice: barajar no afecta al contrato y
   el feedback habla de IDs (en el base hay que mapear índices barajados ↔ originales con
   `options_order`).
2. **`id` de pregunta inmutable** (patrón `^[a-z0-9-]{1,32}$`; el banco real usa `q-0001`… y
   el de muestra `sample-0001`…); no se borra, se pasa a `status: "retired"` (equivale a
   `active=false` del base).
3. **Textos como `string`**, con `locale` a nivel de banco (solo `ca` hoy). Más simple que
   `{"ca": …}` por campo; si algún día hay otro idioma, se publica otro fichero.
4. **`explanation` y `ref` opcionales**, texto plano con saltos de línea (sin HTML).
5. **`sample`**: `true` solo en `bank-sample`; el build de *release* **falla** si el banco es de
   muestra.
6. **Tipos** de 01: `single_choice` (1 correcta), `multiple_choice` (≥2), `true_false`
   (opciones fijas "Vertader"/"Fals"). Corrección por **conjunto exacto** (`sameOptionSet`).
7. **Un solo fichero** (contenido + respuestas): no hay clave que separar del APK. Esto elimina
   el problema de un `bank_hash` sobre secretos (rev. intermedia B4): si se quiere mostrar
   `bank_version` + `hash` corto en la pantalla "Quant a", se calcula en el build privado sobre
   el artefacto final y **no se publica hash de respuestas**.
8. **Significado de `bank_version` y `exam_version` [I7]** (no son lo mismo):

   | Identificador | Qué identifica | Cuándo cambia | Dónde viaja |
   |---|---|---|---|
   | `bank_version` (p. ej. `AAAA.MM.n`) | El **contenido concreto** del banco empaquetado: preguntas, respuestas, explicaciones, retiradas. Etiqueta **pública y no sensible** (no es hash de respuestas) | Con **cualquier** cambio de contenido, incluida una corrección de clave | Sesión (`bank_version`), `latest.json`, "Quant a"; en la credencial **solo si se incluye `bv`** (§6.1) |
   | `exam_version` | El **significado del resultado**: reglas (N, tiempo, umbral, tipos admitidos, criterio de corrección, navegación) | **Solo** si cambian las reglas o el significado de "aprobado"; una corrección de texto **no** la cambia | Sesión y credencial (`exv`) |

   Consecuencia explícita: **dos APK con preguntas distintas pueden emitir el mismo `exv`**, y
   el verificador no puede deducir qué banco se usó **salvo que la credencial lleve `bv`**.
   Recomendación [S]: incluir `bv` (campo opcional de `02` §3.7.5, ≈12–16 bytes) para que la
   trazabilidad por contenido no dependa de nadie; si el usuario prefiere no ponerlo, esa
   trazabilidad queda **solo local** (sesión y "Quant a") y debe decirse así. Decisión
   residual, ver preguntas abiertas.

### 5.3 Validación (build y tests)

Falla el build (y la carga de la app en ejecución, antes de permitir un examen) si:

- **Estructura:** JSON no cumple `schema`; `id` de pregunta duplicado o fuera del patrón;
  `bank_version` o `exam_version` vacíos **o que no cumplan el patrón sintáctico de `bv` y
  `exv` de `02` §3.7.5** (porque viajarán a la credencial; el esquema de valores es de `05`
  §7.2–§7.3); texto de pregunta u opción vacío.
- **Opciones:** **`id` de opción únicos dentro de cada pregunta**; ≥2 opciones;
  `true_false` sin exactamente 2 opciones fijas; `single_choice`/`true_false` ⇒ ≠1 correcta;
  `multiple_choice` ⇒ <2 correctas; `correct` con duplicados o `correct ⊄` ids de opciones;
  pregunta activa sin `correct`.
- **Reglas [I1]:** `num_questions` entero **`> 0`**; `time_limit_minutes` entero **`> 0`**;
  **`1 ≤ pass_threshold_count ≤ num_questions`**; `num_questions ≤ nº de activas`
  (**estricto, sin recorte silencioso** del umbral, rev. intermedia I4); cotas de cordura
  (p. ej. `num_questions ≤ 200`, `time_limit_minutes ≤ 240`) [S] para detectar errores de
  tecleo.
- **Release:** `sample=true`; banco ausente (§5.4).

Imprime **solo recuentos e IDs**, nunca texto (§5.5). Casos de fallo cubiertos con bancos de
prueba ficticios (§11).

### 5.4 Cómo entra en el APK sin pasar por git público

```
Fuente de verdad                         Build (local o CI)                    Salida
────────────────                         ──────────────────                    ──────
arbitratge-bank/bank/bank.json ──► tarea Gradle "packBank" ───────────────►  assets/bank.json
(repo PRIVADO o carpeta local)      · valida (§5.3)                           (en claro, en el APK;
                                    · copia tal cual; quita solo `canary`     NO en git)
bank-sample/bank-sample.json ──►    (debug y PRs de forks: muestra ficticia)
```

- **Contrato del build**: variables de entorno/propiedades `ARB_BANK_FILE`, `ARB_SEAL_KEY_FILE`
  (y las del keystore del APK, 05). *Release* **sin** esas entradas ⇒ falla. *Debug* y CI de
  PRs usan `bank-sample` y una clave `kid=dev` que la PWA oficial de producción rechaza
  (`kid` no incluido en `verificador/keys/`).
- **Vía de inyección (decisión compartida con 05):**

| Vía | Descripción | Pros | Contras |
|---|---|---|---|
| **L — local** (02 B4) | El mantenedor compila en su máquina con el banco en una carpeta fuera de git y sube el APK a Releases | Cero infraestructura; el banco nunca sale de la máquina | Depende de una máquina/persona; build no reproducible en CI |
| **P — repo privado + CI** (02 B1) | El workflow del repo público hace *checkout* del repo privado con una *deploy key*/PAT de solo lectura (secret) y compila | Reproducible; coherente con Pages+Actions | Superficie de CI (§5.5); hay que vigilar workflows y logs |
| S — secret de Actions con el JSON | El banco en un secret | Sin repo privado | **Límite de 48 KB** por secret [H, 02 §5.3]; con explicaciones se supera; edición incómoda |
| ✘ — banco cifrado en el repo público (02 B3) | Texto cifrado versionado | — | Contradice "no en historial público": si la clave se filtra, el histórico queda legible. Descartada |

**Recomendación [S]: empezar por L (misma interfaz de build) y pasar a P cuando el flujo esté
estable.** La interfaz Gradle es idéntica, así que el cambio es solo de `release.yml`.

- **Sin cifrado ni ofuscación [D, ADR 0002]:** el banco se empaqueta **en claro**
  (`assets/bank.json`; el ZIP del APK ya lo comprime). Cifrar o codificar con una clave que
  viaja en el mismo APK solo añade mantenimiento sin proteger frente a quien extrae el APK. La
  protección que sí se mantiene es la del **fuente** (repo privado, no en git público, §5.5). La
  propuesta anterior de "ofuscación ligera por higiene" (02 B5, rev. 2 de este documento) queda
  **retirada**. Consecuencia aceptada: el banco es extraíble con `unzip`.

### 5.5 Controles de CI y del repo (para que el banco no se filtre; detalle en 05)

1. `.gitignore` con lista **de permitidos** para `bank-*.json` (solo `bank-sample.json`), y
   `*.pk8`, `*.pem`, `*.jks`, `/private/`.
2. **Ningún artifact** de Actions con banco, `assets/bank.json` ni salida intermedia; solo el
   APK final (público de todos modos, **excepción explícita aprobada**: contiene banco y clave
   de sello). Los artifacts de repos públicos los descarga cualquiera [H, 02 §5.3]. En la CI
   pública de contribuciones se usan **solo datos ficticios**.
3. **Logs**: `packBank` imprime solo recuentos/IDs; no `cat`, no `ls -R`; los secrets
   enmascarados no cubren contenido derivado.
4. Los secrets no llegan a workflows de forks [H]; **no usar `pull_request_target`** con
   *checkout* de código del PR; limitar quién puede editar workflows.
5. **Canary (control complementario, no secreto):** el banco privado lleva un campo `canary`
   con un texto aleatorio que `packBank` **no incluye** en el APK; un paso de CI (con ese valor
   como secret) hace `grep` en el árbol público y falla si aparece. Marca coincidencias por
   nombre de fichero distinto o transformaciones triviales (rev. intermedia M1), pero **no
   detecta una pregunta copiada sin el marcador** ni prueba ausencia de fugas. Si alguna vez
   se incluyera en el banco empaquetado, también sería extraíble del APK.
6. Escáner de secretos (p. ej. `gitleaks`) en ambos repos y *secret scanning* de GitHub.

### 5.6 Migración desde `scripts/questions.sql`

1. **Script Node sin dependencias, de un solo uso** (en el repo privado, no en el build):
   parsea las tuplas `('texto', 'tipo', '[opciones]'::jsonb, índice)` respetando el escape SQL
   `''` y el sufijo `::jsonb`. Formato regular y acotado (75 filas) [H en cuanto a la forma,
   `questions.sql:1-6`].
2. Aplica la regla de la migración del base `20260808010000_multiple_choice_support.sql`:
   `multiple_choice` con **un** índice ⇒ `single_choice`; `true_false` se mantiene. No existen
   en el seed preguntas de ≥2 correctas: habría que redactarlas aparte [S: coherente con
   01 §1.2].
3. Asigna `id` `q-0001…` **una vez** y los congela; `id` de opción `a,b,c,d`;
   `correct_option_index` → `correct`.
4. Valida con §5.3 y comprueba recuento = 75.
5. **Conciliar con la BD de producción** (el seed puede estar desactualizado: el admin editaba
   preguntas desde el panel) **antes** de dar el JSON por bueno [PV].
6. **Revisión de calidad previa a publicar** (01 CU-M6): preguntas casi duplicadas y alguna
   clave discutible. **Con feedback visible, un error en la clave se ve y daña más**: es
   requisito de calidad, no tarea cosmética.
7. El script **no se ejecuta en esta fase** (diseño); su salida va al repo/carpeta **privada**.

### 5.7 Actualizar el banco

**Banco nuevo = APK nuevo** (sin paquete descargable): un solo artefacto firmado, sin gestión
de paquetes locales, y evita servir el banco por HTTP (**un paquete descargable sería justo el
"fichero público" prohibido**, 02 B6). Un error de clave corregido llega por el aviso de
versión (§9.2). La sesión en curso termina con su instantánea (§4.4).

---

## 6. Credencial: emisión, QR y PDF

### 6.1 Qué hace la app

Al persistirse el resultado de un aprobado (`Result(Passed) → Credential`, inmediata, §4.3): ejecuta la
**emisión de `02` §3.7.10** (payload con los campos de `02` §3.7.5, serialización única,
sello ES256, autoverificación; sin DNI, email, foto ni duración, `01` §4.1), guarda el **texto
exacto del token** y genera (a) la **pantalla** de credencial, (b) **PDF A4** y (c) **imagen**
(PNG) exportables. No se emiten `exp`, `nbf`, `crit` ni `alg` (nombres reservados, `02` §3.7.5).
El contrato de bytes, campos y códigos es **`02` §3.7**; aquí solo se concreta lo que es de
la app (§6.2).

**Nombre:** la app lo construye desde lo que teclea el usuario y lo valida con las reglas de
**`02` §3.7.5.1** (sustituciones, colapso de espacios, recorte, **NFC**, 1–80 puntos de código,
conjunto de caracteres prohibidos); `03` no las repite. Si no cumple, no se emite y se explica
al usuario (`04`). Esa **misma cadena** va al token y al PDF/pantalla (fuente única: no hay dos
nombres que puedan divergir). El PDF debe envolver nombres largos y con acentos sin truncarlos
(prueba de §11).

### 6.2 Contrato interoperable Android ↔ PWA (token y firma)

> **Estado:** **cerrado en `02` §3.7 (contrato `ARB1` canónico, rev. 4)**. Esta sección
> **sustituye** a la rev. 3 de `03` §6.2 (6.2.1–6.2.7) y **no repite ninguna regla**: si un
> documento cita `03` §6.2.x, la regla vigente es la de `02` indicada en la tabla. La
> interoperabilidad Kotlin/JCA ↔ WebCrypto sigue **[PV]** hasta que los vectores de `02` §3.7.11
> pasen en Android real (API 26+) y en la PWA.

**Correspondencia de los antiguos apartados con el contrato vigente** (solo punteros; los bytes,
campos, patrones y códigos viven únicamente en `02`):

| Antiguo `03` | Tema | Regla vigente (`02`) |
|---|---|---|
| §6.2.1 | Envoltorio y base64url | §3.7.1 (gramática, base64url canónico) |
| §6.2.2 | Bytes firmados, algoritmo, firma `R‖S` | §3.7.2 (bytes firmados), §3.7.3 (ES256, DER → `R‖S`) |
| §6.2.3 | Payload y campos | §3.7.4 (UTF-8/JSON restringido), §3.7.5 (campos, reservados, versionado), §3.7.5.1 (`nom`) |
| §6.2.4 | Límites y normalización de la entrada | §3.7.6, §3.7.7 |
| §6.2.5 | Orden de verificación y códigos | §3.7.8 (9 códigos + `OK`; correspondencia con los 4 resultados de ADR 0001) |
| §6.2.6 | `kid → clave pública`, control de release | §3.7.9 (esquema, estados, correspondencia privada↔pública); puertas G1–G4 y representación de la clave privada: `05` §4.3.1, §5.5, §5.8 |
| §6.2.7 | Vectores | §3.7.11 |
| (nuevo) | Emisión | §3.7.10 (la implementa §4.3 y §6.1 de este documento) |

**Qué implementa `03` (sin redefinirlo):**

- `core-credential` expone el emisor (`02` §3.7.10) y un verificador Kotlin que devuelve **el mismo
  código** que la PWA para el mismo texto (`02` §3.7.8). El verificador Kotlin se usa solo
  **internamente** (autoverificación al emitir, pruebas, control de release): la app del candidato
  **nunca** muestra `VÀLIDA` (`02` §3.7.8, T5).
- Detalles de implementación que el contrato deja a la plataforma: conversión DER → `R‖S` en el
  emisor (`02` §3.7.3); decodificación UTF-8 estricta con `CharsetDecoder`/`REPORT` y analizador
  JSON restringido propio (`02` §3.7.4); comparación tras re-codificar para base64url canónico
  (`02` §3.7.1).
- La entrada de confianza del `kid` de la app (`trusted-keys/<kid>.json`, `02` §3.7.9) la usa la
  autoverificación; su correspondencia con la clave privada inyectada es una puerta de release
  (`02` §3.7.9, `05` §5.8).
- **Tamaño del QR** [S, PV-S1]: las estimaciones vigentes están en `02` §3.3 (token ≈ 295
  caracteres sin `bv`/`app`, ≈ 350 con ellos; QR versión ≈ 10–14, corrección M). Se miden en el
  spike S1 (§12); si resultara demasiado denso, se reducen los opcionales `bv`/`app` antes de
  plantear `ARB2`.
- **Vectores**: `03` genera los casos V01–V05 de `02` §3.7.11 con el emisor de producción
  instrumentado con `kid=test` en dispositivo o emulador API 26+ (el proveedor JCA de Android no
  es el del JDK); `05` los ejecuta en CI (G1).

### 6.3 Advertencia obligatoria sobre la clave embebida [D8]

> Si la app **firma** credenciales, la **clave privada está en el APK público** (o es
> derivable de él). Una persona con conocimientos técnicos puede **extraerla** —de un
> `assets/` con `unzip`, de una constante con un descompilador, o llamando a la función de
> firma de la app— y **fabricar credenciales ilimitadas e indistinguibles de las legítimas**
> (incluida la fecha, que escribe quien firma). Ofuscar, cifrar con otra clave del mismo APK o
> usar código nativo solo retrasa. **No es una garantía criptográfica fuerte**: es un **sello de
> integridad frente a errores y edición casual**, que convierte la falsificación en un acto
> técnico deliberado. Debe aparecer en la ayuda de la app, en la web de descarga y en la
> PWA verificadora (texto de 02 §4), y **nunca** describirse como "firma digital segura",
> "certificado" ni "autèntica".

Aviso corto aprobado (ADR 0001): «*Aquesta marca ajuda a detectar canvis casuals; no és una
prova contra un dispositiu modificat o una clau extreta*».

Consecuencias de diseño: la PWA muestra uno de los cuatro resultados de ADR 0001 (`VÀLIDA`
significa "firma correcta con una clave conocida", no "autèntica"); la fecha es "data del
dispositiu" y no se usa para decidir validez (rev. intermedia B3); la credencial lleva
`orig="autoservei"` y el PDF lo dice en texto visible.

### 6.4 Sello: decisión vigente (ADR 0001) y encaje técnico

**Vigente:** firma asimétrica **ECDSA P-256** con la clave incorporada en el build. **El
checksum sin clave ("L1") y el PDF+QR sin sello ("L0") son opciones históricas, descartadas
por el ADR 0001; HMAC ("L3") estaba descartado desde 02 §3.4. No se mantienen como
alternativas del MVP** ni se prevé un "modo checksum" en el código.

- La interfaz `CredentialSealer { seal(entradaFirmada: ByteArray): ByteArray /* 64 B R‖S */ }`
  existe por **testabilidad** (clave `test`/`dev` en pruebas) y por rotación de `kid`. Recibe
  **los bytes que define `02` §3.7.2** (ASCII de `ARB1.<payload-seg>`, **no** los bytes del JSON
  decodificado: la rev. 3 de este documento lo tenía al revés; vector S05) y devuelve la firma
  ya en `R‖S` (`02` §3.7.3). La verificación en Kotlin (`core-credential`) sirve a la
  autoverificación, a las pruebas y al control de release (§6.2).
- **Coste real:** ≈ 40 líneas con JCA + conversión DER→`R‖S` + **un par de claves por `kid`**
  (la privada inyectada en el build, §5.4, **va en el APK** §6.3; la pública, en
  `trusted-keys/`).
- **Cuándo se sella:** el material de firma se incorpora en el build y el sello de **cada
  credencial se genera en ejecución**, en el móvil del candidato (`02` §3.7.9, último punto,
  que aclara ADR 0001 §Operación). La representación de la clave privada en el APK es de `05`
  §4.3.1/G4 [PV].
- Rotación por `kid` y gancho para una futura revocación por `kid`/`cid` (§13).
- La clave privada se lee de la entrada del build (`ARB_SEAL_KEY_FILE`, §5.4), **nunca** del
  repo.

### 6.5 PDF y QR

- **QR**: ZXing `core` → matriz → `Bitmap` (pantalla) y dibujado vectorial en el PDF; corrección
  **M**, margen de silencio de 4 módulos, fondo blanco. Pantalla: brillo máximo temporal
  mientras se muestra.
- **PDF A4** con `PdfDocument`: título "Credencial d'àrbitre — Futbol amb Botons", **nombre** y
  **"APTE/A"** grandes (≥ 18–24 pt), fecha "(data del dispositiu)", `exv`, código corto de `cid` (formato `XXXX-XXXX`, `02` §3.7.5), QR de
  **≥ 6–7 cm** (legible impreso en A4) [S, PV-S1], texto fijo "Emesa en autoservei. No és una
  garantia forta" y las instrucciones de verificación con la **dirección oficial de la PWA**
  (dominio definido en 05; no se imprime una URL que acepte el QR como entrada, 05 §6.7).
  **El texto visible es la verificación básica** (01 §4.2.1). Pensar en **impresión en blanco y negro**.
- Si pantalla o PDF muestran el **token como texto** (p. ej. para pegarlo en la PWA), se parte
  **solo con saltos de línea, nunca con guion de división** (el `-` forma parte del alfabeto del
  token); la PWA elimina esos espacios y saltos al normalizar la entrada (`02` §3.7.7). El texto
  mostrado es el token guardado, sin reconstruirlo (`02` §3.7.10 paso 6).
- El PDF es **presentación**, no contenedor de firma (02 §3.5): no se usa PAdES.
- **Exportar/compartir** sin permisos de almacenamiento: `ACTION_CREATE_DOCUMENT` (guardar) y
  *share sheet* con `FileProvider`. También PNG de la credencial.
- La credencial **no usa `FLAG_SECURE`**: se debe poder capturar y compartir (la captura sigue
  siendo verificable por el sello, 02 §7.4).
- Mostrar una credencial **sin red** desde la app y desde el fichero exportado.

---

## 7. Compatibilidad entre versiones de la app y de la PWA verificadora

### 7.1 Credenciales emitidas

Se guardan como **token firmado** (nunca se re-serializa ni se re-firma); el PDF/PNG se puede
regenerar desde el token y el nombre guardados, pero el **QR es siempre el mismo token**.

### 7.2 Compatibilidad honesta entre APK y PWA [I4]

- **Regla vigente (sustituye a "N y N−1")**: la PWA **verifica todos los formatos `v` y todas
  las claves `kid` cuyas credenciales sigan siendo aceptables**. Mientras no exista una
  política explícita de retirada o caducidad (decisión residual), **ninguna credencial emitida
  con un formato o clave anteriores deja de verificarse**, salvo `kid` marcado `compromised`
  en `trusted-keys/`. Retirar un formato o un `kid` es una **decisión de producto explícita**,
  con aviso en català, no un efecto lateral de actualizar la PWA.
- **Una PWA antigua no puede conocer un formato ni un `kid` futuros.** Los declara
  `VERSIÓ DESCONEGUDA` (solo por prefijo de versión, `UNKNOWN_VERSION`) o `NO VÀLIDA` con la
  pista "pots haver d'actualitzar el verificador" (`UNKNOWN_KID`; ambos códigos, en `02`
  §3.7.8): **fallo operativo, no forja**, y
  **nunca se acepta por defecto**. La solución es actualizar la PWA, no relajar el rechazo.
- **Orden de publicación:** publicar la clave pública nueva (PWA desplegada) **antes** de
  distribuir el APK que emite con ella. Los verificadores **offline ya instalados** no se
  actualizan solos hasta tener conexión: hay que avisar de que se actualicen antes de usar
  credenciales nuevas (comunicación del club, 05).
- **"Instalada" no es "lista sin conexión"** [PV]: instalar el acceso directo no prueba que
  *todos* los recursos (JS, decodificador QR, claves, iconos) estén en caché, y el navegador o
  el usuario pueden borrarla. La PWA debe comprobar que el precache está completo antes de
  declarar "preparada" (04/05), y se prueba el arranque real en modo avión tras cerrar y
  reabrir (Android Chrome/Edge) antes de publicarla. Debe mostrar su versión y los `kid` que
  conoce (05 §6.7).

### 7.3 Caducidad y vigencia [I4]

**No se promete caducidad**: el MVP **no emite `exp`** y `exv` no invalida por sí sola. Si el
producto la introduce más adelante: (1) la PWA separa visualmente **"firma correcta"** de
**"vigent"**; (2) se avisa de que `iat` lo escribe el móvil del candidato (manipulable) y de
que la comparación usa el reloj del dispositivo del verificador (también manipulable); (3) un
formato con `exp` exigiría **`ARB2`**: `exp` es nombre reservado en `v=1` y no hay campos
críticos (`02` §3.7.5, §6.6), de modo que un verificador antiguo nunca lo ignoraría en silencio
(respondería `VERSIÓ DESCONEGUDA`); habría que comunicarlo. La política de vigencia y de versiones aceptadas es
fase 2 (§13).

### 7.4 Sesión y claves

- **Sesión en curso al actualizar**: la sesión lleva su instantánea de preguntas y reglas; una
  migración de Room la conserva. Si el esquema de sesión fuera incompatible (caso excepcional),
  se cierra como `Abandoned(update)` **con aviso en catalán** y se permite reintentar; es un
  riesgo a minimizar con migraciones probadas, no una política.
- **Claves**: la lista `kid → clave pública` se publica con cada despliegue de la PWA
  (`trusted-keys/` → `verificador/keys/`, repo público). Una `kid` `compromised` se rechaza;
  las credenciales de una `kid` comprometida **no se pueden separar** en legítimas/forjadas por
  fecha (02 §3.3, rev. intermedia B3): decisión de política en fase 2.

---

## 8. Temporizador y reloj

### 8.1 Fuentes de tiempo [H salvo indicación]

| Fuente | Característica | Uso |
|---|---|---|
| `SystemClock.elapsedRealtime()` | ms desde el arranque, **incluye sueño profundo**; monotónica; se reinicia al reiniciar el móvil | **Única base del cómputo del examen** |
| `SystemClock.uptimeMillis()` | **no** cuenta el sueño | No usar |
| `System.currentTimeMillis()` | Hora de pared; el usuario puede cambiarla | Solo informativa: `start_wall`, `finished_wall`, fecha de la credencial ("data del dispositiu") |
| `Settings.Global.BOOT_COUNT` (API 24) | Contador de arranques | Detectar **reinicio**; puede no estar disponible o no ser fiable [S: comportamiento por fabricante, PV-S5] |

### 8.2 Modelo: tiempo restante persistido con ancla

**Una sola política, indulgente** (01 §3.2 y 04 deben alinearse con ella; la estimación por
reloj de pared con tope queda **descartada**). Se persisten, en `session`:

- `limit_ms` (de las reglas validadas, §5.3, siempre > 0);
- `remaining_ms_saved`: tiempo restante válido **en el instante del ancla**;
- `anchor_mono`: `elapsedRealtime` en ese instante; `anchor_boot`: arranque (`BOOT_COUNT`) en
  ese instante.

```
Al crear la sesión (misma transacción que el inicio, §4.3):
    remaining_ms_saved = limit_ms ; anchor_mono = ahora_mono ; anchor_boot = arranque_actual
    (así, un reinicio ANTES de la primera respuesta ya tiene ancla válida)

restante(ahora) = remaining_ms_saved − (ahora_mono − anchor_mono)        [mismo arranque]
```

- **Punto de control** (en la misma transacción de cada respuesta, y además en `onStop` y cada
  ~30 s con la app abierta [S]): se escribe `remaining_ms_saved = restante(ahora)` y
  `anchor_mono = ahora_mono` **a la vez** (una fila, una transacción). Así nunca coexisten un
  restante nuevo con un ancla vieja.
- Se calcula **siempre desde la fuente**, no se decrementa un contador (las pausas y la deriva
  del `delay` no lo corrompen). Tiempo **por examen**, como en el base (01 §3.3). El contador
  visible refresca con `LaunchedEffect`+`delay` y relee la fuente.
- **Fin por tiempo:** `restante ≤ 0` (se evalúa en el tick, al guardar y al reanudar). Una
  respuesta se acepta si su `remaining_at_tap ≥ 0`, medido **al tocar** (§4.3), aunque la
  escritura ocurra después; `finished_wall` es informativo.
- `remaining_ms_saved` se limita a `[0, limit_ms]` al escribirlo.

### 8.3 Reinicio y cambio de hora (política indulgente [S], coherente con 01 §3.2)

- **Cambio de hora de pared** sin reinicio: no afecta (solo se usa `elapsedRealtime`). No se
  detecta ni se sanciona.
- **Reinicio** (arranque distinto del `anchor_boot`): `elapsedRealtime` ya no es comparable.
  **Reanclaje atómico e idempotente**, en una sola transacción al reabrir la app: se escribe
  `anchor_mono = ahora_mono` y `anchor_boot = arranque_actual` **dejando `remaining_ms_saved`
  intacto**. El tiempo con el móvil apagado **no cuenta** (tampoco el transcurrido entre el
  último punto de control y el apagado: de ahí los puntos de control periódicos, que acotan la
  indulgencia). Un usuario mayor con la batería agotada no pierde el examen.
  - Si el proceso muere **durante** el reanclaje, la transacción no se aplicó y se repite al
    abrir de nuevo (mismo resultado).
  - **Reinicios repetidos** (antes de responder, entre respuestas o durante la recuperación):
    cada uno reancla desde el `remaining_ms_saved` persistido; no hay acumulación de error ni
    sanción.
  - Quien reinicie a propósito para ganar tiempo puede hacerlo: **riesgo aceptado**
    (01 R5). No se añade sanción ni se pretende un reloj fiable frente al candidato.
- **Si `BOOT_COUNT` no es fiable o no se puede leer** [PV-S5]: se trata como reinicio (y se
  reancla, cobrando 0) si `ahora_mono < anchor_mono` (imposible en el mismo arranque) **o** si
  la diferencia entre el avance de la hora de pared y el de `elapsedRealtime` desde el ancla
  supera una tolerancia (p. ej. 2 min) — en ese caso la hora de pared solo sirve de **indicio**
  para no cobrar tiempo dudoso, nunca para cobrar más. Peor caso: un reinicio no detectado
  que cobra como mucho el tiempo de arranque nuevo; lo aceptamos por proporcionalidad.
- No se promete tiempo "fiable": sirve para ritmo y coherencia con el reglamento, no como
  control de integridad.

### 8.4 Qué se registra

Por sesión: `start_wall`, `finished_wall`, duración (mostrada al candidato en el resultado,
**no** incluida en la credencial, 01 §4.1), motivo de fin (`Completed | TimedOut | Abandoned`)
y `remaining_at_tap_ms` de cada respuesta. **Nada más**: sin cadena de hashes, sin conteo de pérdidas de
foco ni de reinicios (no hay revisor que los use).

### 8.5 Presentación y accesibilidad del tiempo

- Contador legible `mm:ss` en texto grande; `liveRegion=Polite` **solo** a los 5 min, 1 min y
  10 s (no cada segundo); acción accesible "Quant de temps queda?"; no se transmite solo con
  color ni sonido.
- **Tiempo ampliado** (01 pregunta 10): sin organizador, solo como ajuste visible en el
  *Briefing* (`time_factor` guardado en la sesión) o como `exam_version` aparte. Decisión de
  producto abierta; la arquitectura lo admite sin cambios.

---

## 9. Actualizaciones del APK fuera de Play Store

### 9.1 Reglas de Android [H general; detalle en 05]

Una actualización requiere el mismo `applicationId`, **la misma firma** y `versionCode` mayor;
la clave de firma es la **identidad irrecuperable** de la app (copia de seguridad obligatoria,
05; rotación posible con APK Signature Scheme v3 [PV]). Sin instalador propio en v1: la
descarga y el instalador del sistema; **no** se pide `REQUEST_INSTALL_PACKAGES`.

**Alcance honesto de la firma Android [I5]:** protege la **actualización** de una instalación
existente (Android rechaza un APK con otra firma), pero **no autentica la primera
instalación**: si se sustituyen a la vez el enlace de descarga y el hash/huella publicados
en la misma página, el candidato no lo detecta. No se promete autenticidad absoluta; se
recomienda comunicar la dirección oficial y la huella también por un canal conocido del club
(05/04). La página de descarga debe enlazar a un **asset de versión concreta** asociado al
hash mostrado, y actualizar ambos **a la vez**; el `latest.json` (abajo) debe ser coherente
con ese mismo asset (05). Una *prerelease* pública **no** es un canal restringido: si
contuviera banco real o la clave de sello de producción, se asumiría desde su publicación
que cualquiera puede obtenerlos (pruebas con banco real: solo locales/privadas, §5.4).

### 9.2 Aviso de nueva versión (sin backend)

- **`latest.json` estático** en GitHub Pages (**público y sin datos sensibles**):
  `{ "app": "candidate", "versionCode": 12, "versionName": "1.3.0", "minSupportedVersionCode": 10,
  "bank_version": "2027.01.0", "notes": {"ca": "…"} }`. **No lleva la URL del APK**: el botón
  "Descarrega la nova versió" abre con `ACTION_VIEW` la **página de descarga fija**
  compilada en la app. Así un `latest.json` alterado no puede redirigir a un APK ajeno y no hace
  falta firmarlo; el instalador del sistema sigue siendo la defensa real (rechaza APK con otra
  firma).
- **`INTERNET` en el candidato: opción recomendada [S]**, solo para esa consulta (HTTPS, sin
  identificadores ni datos del usuario, fallo silencioso, desactivable). Razón: un **error en la
  clave de respuestas** se corrige únicamente publicando versión, y los mayores no vuelven a la
  web solos. Coste: se pierde el argumento "la app no tiene Internet" y la consulta expone la IP
  a GitHub. **Alternativa sin `INTERNET`**: el aviso es solo la web y la comunicación del club
  (pregunta abierta).
- La **PWA** se actualiza por su propio *service worker* (05 §6.7), no por `latest.json` del
  APK; avisa de nueva versión o de nuevas claves cuando hay conexión (§7.2).

### 9.3 Verificación de desarrolladores de Android (impacto en el diseño) [H §1.2]

- Desde 2027, en dispositivos certificados, un paquete **no registrado** por un desarrollador
  verificado exige el "flujo avanzado" (fricción grave para un candidato mayor).
- **`app-candidate` (público, único APK):** conviene *full distribution* (verificación de
  identidad; una organización debe verificar su sitio con Search Console:
  *futbolbotons.cat* sería el candidato natural) o aceptar el flujo avanzado y documentarlo en
  la web. *Limited distribution* (gratis, sin DNI, para aficionados **sin intención
  comercial**) es una alternativa si el club encaja en sus términos; el límite de
  dispositivos (**20 vs 50**, §1.2) a aclarar en 05 y es relevante porque el candidato es
  público. [PV]
- **Efecto en la arquitectura:** `applicationId` y clave de firma pasan a ser **datos
  registrados**: se fijan **antes** de la primera publicación (cambiarlos ⇒ re-registro y no
  hay actualización). **Un solo APK = un solo registro** (la PWA no se registra). Todo esto va
  a `05` y a la web de descarga. Las fechas y límites citados son **[PV]**: reverificar al
  publicar.

### 9.4 Compatibilidad entre versiones

Ver §7. Resumen: credenciales inmutables; la PWA verifica todos los formatos y claves
vigentes (no solo "N y N−1"); una PWA antigua rechaza lo desconocido y pide actualizarse;
sesiones autosuficientes; `applicationId` y firma estables.

---

## 10. minSdk/targetSdk, permisos, edge-to-edge, accesibilidad, i18n

### 10.1 SDK

| | Valor | Motivo |
|---|---|---|
| `minSdk` | **26** (Android 8.0) | ≈99 % de dispositivos [H, §1.2]; `java.time` sin *desugaring*; iconos adaptativos. Bajar a 24 (+≈0,3–0,4 %) es barato si algún miembro tiene un móvil antiguo (*desugaring* automático). Ya no hay razón técnica para `minSdk` 30. |
| `targetSdk` | **36** al empezar; subir a 37 tras probar | El comportamiento depende de `targetSdk`; edge-to-edge obligatorio con ≥35; un `targetSdk` bajo **impide instalar** en Android 14/15 [H]. Play no aplica, pero fija qué se considera "actual". |
| `compileSdk` | 37 (o el último estable) | APIs recientes sin cambiar el comportamiento |

### 10.2 Permisos

| Permiso | App del candidato (única app Android) |
|---|---|
| `CAMERA` | **No** (no hay lector en la app; la cámara es de la PWA verificadora, que la pide el navegador) |
| `INTERNET` | Solo si se acepta el aviso de versión (§9.2) |
| Almacenamiento, notificaciones, ubicación, contactos, `REQUEST_INSTALL_PACKAGES`, `QUERY_ALL_PACKAGES` | No |

Manifiesto: `allowBackup=false`, `dataExtractionRules` vacías, `debuggable=false` en *release*,
`exported=false` salvo la actividad principal; `usesCleartextTraffic=false`. **Esto corrige la
desviación de la rev. 1**: con el perfil actual el candidato **no necesita cámara** (no hay
invitación que escanear); tras el ADR 0001 tampoco existe app Android verificadora (la cámara es de la PWA).

### 10.3 Edge-to-edge

Obligatorio con `targetSdk` ≥ 35: `enableEdgeToEdge()` antes de `setContent`; insets con
`Scaffold`/`WindowInsets.safeDrawing`; `adjustResize` en la pantalla de nombre (la IME no debe
tapar el campo ni el botón); botón principal siempre visible sobre la barra de navegación.
**Aplicar la skill `edge-to-edge` al implementar.**

### 10.4 Accesibilidad (público mayor)

- Texto ≥ 18 sp por defecto; escala de fuente del sistema hasta 200 % **sin truncar**
  (scroll); contraste WCAG AA; objetivos táctiles ≥ 48 dp; una pregunta por pantalla con un
  único botón principal ("Següent") grande y fijo.
- Semántica Compose: `Role.RadioButton` (única) / `Role.Checkbox` (múltiple); enunciado
  "Selecciona totes les correctes" cuando proceda; orden de foco = orden visual; teclado y
  *Switch Access*.
- **Correcta/incorrecta nunca solo con color**: icono + texto ("Correcta"/"Incorrecta"/"Sense
  respondre").
- Confirmación antes de salir del examen y antes de dar por terminado; sin gestos obligatorios
  (sin *swipe*); sin animaciones que distraigan; tiempo sin presión visual agresiva.
- El **aviso de fallo de guardado** (§4.3) es accesible: se anuncia (`liveRegion`), no depende
  del color, y "Torna-ho a provar" es alcanzable con TalkBack y teclado.
- Pruebas: Accessibility Scanner + pruebas de semántica (skill `compose-ui-testing-patterns`),
  **y con 2–3 personas mayores reales** (TalkBack, escala de fuente, contraste, temporizador,
  feedback, exportación) antes de publicar [S]. La PWA se prueba aparte (04/05).

### 10.5 i18n (catalán)

- **Recursos por defecto en catalán** (`values/strings.xml` = `ca`): cualquier configuración de
  idioma del móvil cae en catalán [01 §1.5]. `androidResources.localeFilters` para limitar a
  `ca`. Añadir otros idiomas = `values-xx`.
- `plurals` con formas `one/other`; fechas con `Locale("ca")` ("19 de novembre de 2026").
- Contenido del banco en un idioma por fichero (§5.2); PDF y credencial toman sus cadenas de
  recursos.
- Mensajes de verificación como **catálogo con claves estables = los códigos de `02` §3.7.8**
  (no se define aquí una lista propia): `core-credential` y la PWA devuelven **códigos**, no
  texto; la PWA tiene sus propias cadenas en catalán (04).

---

## 11. Pruebas y herramientas

- **`core-*` en JVM**: JUnit + pruebas de propiedades — selección sin repeticiones y tamaño
  exacto N; barajado reproducible con `Rng` fijo; corrección por conjunto exacto igual que
  `sameOptionSet`; `multiple_choice` ≥2; sin responder = fallo; feedback por pregunta;
  validación del banco (§5.3, con casos de fallo: `N = 0`, tiempo ≤ 0, umbral 0 o > N,
  `N` > activas, ids de opción repetidos, `exam_version`/`bank_version` fuera del patrón de
  `02` §3.7.5); **emisor** según `02` §3.7.11 ("Emisor"): entrada con NFD, espacios múltiples y
  U+00A0 ⇒ `nom` NFC normalizado; conversión DER→`R‖S` con `INTEGER` de 31, 32 y 33 bytes;
  el verificador Kotlin devuelve el código esperado de **todos** los casos de `02` §3.7.11.
- **Contrato Android ↔ PWA** (`02` §3.7.11): Android firma → PWA/Node WebCrypto verifica, con
  los vectores válidos e inválidos de ese apartado; V01–V05 generados en dispositivo/emulador
  API 26+; correspondencia "la privada inyectada corresponde a la pública publicada" y caso R01
  (`02` §3.7.9, §3.7.11; puertas G1–G3 de `05` §5.8).
- **Reloj**: `ExamClock` falso: reinicio **antes de responder**, reinicios **repetidos**,
  reinicio **durante la recuperación** (muerte del proceso a mitad del reanclaje), `BOOT_COUNT`
  ausente o no fiable, salto de hora de pared, Doze; el restante nunca aumenta ni supera
  `limit_ms`.
- **Persistencia (B3)**: `SessionStore` que falla al guardar una respuesta, al finalizar
  (`FINISHED` + `correct_count`) y al insertar la credencial: comprobar que **no se avanza, no
  se corrige y no se emite**, que la selección sigue visible, que el reintento funciona sin
  duplicar filas, y que `Abandonar` no deja la sesión en un estado inconsistente.
- **Emisión**: tests que impiden credencial en **suspenso, abandono, tiempo agotado sin
  aprobar y error de persistencia**; recuperación "aprobado sin credencial" → reintento de emisión
  automático e idempotente (mismo `cid`, mismo nombre); nombre inmutable desde `Ready → InProgress`.
- **Proceso muerto**: instrumentación `am kill` tras cada respuesta y comprobar que el estado
  coincide; migraciones Room con `MigrationTestHelper`.
- **Compose**: pruebas de semántica/accesibilidad y *screenshot tests* (skills `testing-setup` y
  `compose-ui-testing-patterns`; `mobile-android-design` para Material 3).
- **Banco**: el **banco de muestra ficticio** es el que usan los tests y la CI pública; el
  banco real solo se valida en el build privado y **nunca** se copia a tests, ejemplos, logs ni
  documentación.
- **QR/PDF**: vectores de QR de referencia; prueba manual de lectura desde pantalla, papel A4 y
  captura (§12 S1).
- **Herramientas**: validación/empaquetado del banco en **Kotlin/JVM** (reutiliza `core-exam`);
  migración SQL→JSON en Node (script de un solo uso, sin criptografía).

---

## 12. Riesgos, supuestos y prototipos

| # | Riesgo / supuesto | Tratamiento |
|---|---|---|
| R1 | Verificación de desarrolladores (2027): fricción de instalación | Decidir cuenta/registro y fijar `applicationId`/firma antes de publicar (§9.3, 05) |
| R2 | Banco y respuestas extraíbles del APK; 75 preguntas ya públicas | Aceptado [D2]; el valor del repo privado es para contenido nuevo y explicaciones |
| R3 | Clave de sello extraíble ⇒ forja posible | Aceptado (ADR 0001); documentar sin ambigüedad (§6.3). Sin "modo checksum" alternativo |
| R4 | Fuga del banco por CI/logs/artifacts/workflows | Controles §5.5; empezar por build local (vía L) |
| R5 | Error en la clave de respuestas ahora **visible** al candidato | Revisión editorial previa; aviso de versión (§9.2) |
| R6 | Churn de AGP/Compose/KSP para una persona | Dependencias mínimas; ventanas de actualización fijas; skill `agp-9-upgrade` |
| R7 | Usuarios mayores no instalan/entienden el APK | Web de descarga con pasos y capturas (04/05); prueba con usuarios reales |
| R8 | QR demasiado denso para móviles/impresoras corrientes | S1; reducir opcionales `bv`/`app` antes de plantear `ARB2` (`02` §3.3, §3.7.6) |
| R9 | PWA: offline real, cámara y caché borrada no probados; PWA antigua desconoce `kid`/`v` nuevos | **[PV]** S7; rechazo explícito y orden de publicación (§7.2) |
| R10 | Divergencia entre implementación Kotlin y JS del contrato | Contrato único `02` §3.7 + `test-vectors/` en CI (`02` §3.7.11) |
| R11 | Pantalla "guardado" que deja continuar sin persistir (propuesta de 04) | Invariante B3 (§4.3); 04 debe alinearse |

**Spikes recomendados antes de cerrar (≈1 día cada uno [S]):**

| ID | Qué medir |
|---|---|
| S1 | Tamaño y legibilidad del QR (≈295–350 caracteres, versión ≈10–14, `02` §3.3) en pantalla pequeña, A4 impreso (B/N) y captura; mínimo de cm en el PDF |
| S2 | Generar el PDF con `PdfDocument` en 3–4 móviles (incluido antiguo) y compartirlo/guardarlo; abrir en visores comunes |
| S3 | Tamaño real del APK (Compose + R8, ABI filtradas) |
| S4 | Flujo de instalación con verificación de desarrolladores en un dispositivo certificado con un APK real |
| S5 | `BOOT_COUNT`/`elapsedRealtime` tras reinicio y Doze en 2–3 fabricantes; comportamiento de `remaining_ms_saved` |
| S6 | CI: *checkout* del repo privado con deploy key y compilación sin que el banco aparezca en logs/caché/artifacts |
| S7 | **Android firma → PWA verifica** con los vectores de `02` §3.7.11; analizador JSON restringido con detección de duplicados en JS; PWA instalada, primera carga completa, **modo avión tras cerrar y reabrir**, cámara, imagen y texto en Android Chrome/Edge (con 05) |

---

## 13. Extensión futura sin diseñar backend ahora (fase 2)

Nada de esto se implementa en el MVP; solo se **garantiza que no habrá que romper formatos**.

| Extensión | Gancho ya presente | Cómo se haría (sin servidor propio) |
|---|---|---|
| **Revocación** | `cid` (UUID aleatorio) y `kid` en el token; `v` | Lista de denegación (`cid`/`kid` retirados, `exv` no aceptadas) empaquetada en la **PWA** y actualizada **publicando versión**; variante: JSON firmado en Pages que la PWA descarga si hay red (con versión creciente anti-*rollback*). Limitación: con clave embebida no frena la forja (nuevos `cid`) (02 §8.1). Interfaz prevista (PWA y `core-credential`): `TrustPolicy { isKidAccepted, isCidDenied, isExamVersionAccepted }`, en MVP con implementación trivial |
| **Registro del club** | Nombre y `cid` en la credencial | Práctica social (hoja privada de la secretaría); nada de nombres en un APK ni en Pages |
| **Caducidad / versiones aceptadas** | `exp` (nombre reservado en `v=1`: exige `ARB2`, `02` §3.7.5) y `exv` | Política de vigencia en la PWA separando "firma correcta" de "vigent" (§7.3; 02 §8.3) |
| **Emisión fuerte** (clave fuera del APK) | `kid` y `v` | Solo si cambia el perfil de amenaza; exige organizador y rompe el autoservicio (02 §8.4) |
| **Paquete de banco descargable** | `bank_version`, formato JSON único | Contradice [D2] si es público; solo con un canal privado, y no se plantea ahora |
| **Modo práctica** | `kind` en `session` | UI y política (01 pregunta 5) |

---

## Preguntas abiertas para el usuario

*(Solo residuales. **Ya decididos y no reabiertos:** autoservicio, banco en el APK sin
cifrar/ofuscar (ADR 0002), firma asimétrica con clave embebida y verificador PWA público, sin
app Android de organizadores (ADR 0001), seguridad proporcional, suplantación aceptada,
revocación en fase 2, repo público y verificación optativa.)*

1. **Stack**: ¿confirmas Kotlin + Compose? Si prefieres seguir en JS/TS, Capacitor es una
   alternativa legítima (§2.3); si iOS pasara a ser requisito, Flutter.
2. ~~Contrato de firma~~ **Cerrada**: el contrato está fijado en `02` §3.7 (rev. 4: ES256
   `R‖S`, `spki`, `UNKNOWN_KID` ⇒ `NO VÀLIDA`, `res` = `"APTE"`). Queda solo su validación
   con los vectores (`02` §3.7.11, spike S7). Se conserva la numeración por las referencias
   cruzadas.
3. **`bank_version` en la credencial (`bv`, §5.2 punto 8)**: ¿se incluye (recomendado) o la
   trazabilidad por contenido queda solo local?
4. **Inyección del banco**: ¿empezar con compilación local (vía L) y pasar a repo privado + CI
   (vía P) más adelante? (compartida con `05`).
5. **Aviso de versión**: ¿aceptas `INTERNET` en la app del candidato solo para consultar
   `latest.json` (recomendado), o prefieres cero Internet y avisos solo por la web/club?
6. **Cuenta de desarrollador (Android)**: ¿quién es titular —persona o CFBB con `futbolbotons.cat`
   verificado—? ¿Aceptas el "flujo avanzado" para el candidato si no se verifica? ¿Qué
   `applicationId` definitivo (p. ej. `cat.futbolbotons.arbitratge`)? No puede cambiarse
   después sin re-registrar. ¿Se aclara el límite 20 vs 50 de *limited distribution*?
7. **Reloj**: ¿conforme con la política única indulgente de §8.2–8.3 (restante persistido,
   reanclaje atómico, el apagado no cuenta; sustituye a la estimación por hora de pared de 01)?
   ¿Y tiempo ampliado como ajuste visible de la app (§8.5), sin organizador obligatorio?
8. **Backup de la credencial**: ¿basta el PDF/imagen exportado (`allowBackup=false`) o se quiere
   restauración automática al cambiar de móvil?
9. **Contenido editorial**: ¿se muestran siempre las acertadas además de las falladas? ¿Navegación
   sin retroceso o con revisión? ¿Modo práctica sin credencial? (01 preguntas 4–5.)
10. **Conciliación del banco** con la BD de producción antes de migrar, y **qué hacer con las 75
    preguntas ya públicas** en el repo base (¿se retira el fichero, sabiendo que el historial
    persiste?).
11. **`minSdk` 26** (propuesto): ¿algún miembro con un móvil anterior a Android 8? Si sí, bajar a
    24.
12. **Mantenimiento**: ¿ventanas trimestrales de actualización de AGP/Compose y empezar con los
    spikes S1–S7?
13. **Caducidad y aceptación de versiones antiguas** (§7.2–7.3): mientras no se decida, no se
    emite `exp` y la PWA verifica todos los formatos y claves vigentes. ¿Se quiere una política
    de retirada? ¿Qué significa `VÀLIDA` si algun día hay caducidad?
14. **Dominio oficial de la PWA** y canal independiente del club para comunicar su dirección y la
    huella del APK (05).
15. **Nombre** (cerrado en la alineación con 04 §2.3): se confirma y congela antes de
    `Ready → InProgress`; no hay `ConfirmName` tras aprobar y la emisión es inmediata. Solo
    se reabre si el usuario quiere permitir corregir el nombre después de aprobar (exigiría
    reintroducir un paso previo al sellado).

---

## Contratos que otros documentos deben seguir (resumen de la rev. 3)

| Incidencia | Qué debe reflejar |
|---|---|
| B3 | `04`: no continuar con una respuesta sin guardar; aviso con reintento; sin credencial con datos incompletos (§4.3) |
| I1 | `01`/`04`: política de reloj única de §8.2–8.3; límites de reglas de §5.3 |
| I2 | `04`: confirmación y congelación del nombre antes de empezar el examen (sin paso tras aprobar); textos que prometen la respuesta correcta y el motivo **cuando exista** |
| I3 | `02`/`05`: contrato `02` §3.7 (canónico) y correspondencia privada↔pública de `02` §3.7.9 / puertas de `05` §5.8 (la antigua `03` §6.2 solo remite) |
| I4/I5 | `04`/`05`: "instalada" ≠ "lista offline"; clave pública antes del APK; sin promesa de caducidad; autenticidad de la primera instalación no absoluta; enlace a asset de versión concreta |
| I7 | `01`/`05`: significado de `bank_version` y `exam_version` (§5.2 punto 8) |
