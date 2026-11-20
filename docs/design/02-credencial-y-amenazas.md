# 02 — Credencial, modelo de amenazas y confidencialidad del banco

> Autor: `trust-architect` · **Revisión 4 (cierre de diseño)**. Fija el **contrato `ARB1`
> canónico** (§3.7) que sustituye a las variantes de la revisión 3 de este documento y de
> `03` §6.2, resuelve el bloqueante de contrato y la incidencia de pegado de
> [`reviews/verificacion-correcciones-finales.md`](reviews/verificacion-correcciones-finales.md),
> añade la tabla de propiedad (§3.8) y retira de §5.2 los punteros identificables al banco
> real. Se basa en los ADR aprobados
> [`0001`](../adr/0001-credencial-sellada-y-verificador-publico.md) (sello asimétrico con clave
> embebida + verificador PWA pública) y [`0002`](../adr/0002-banco-privado-en-build-y-apk.md)
> (banco privado inyectado en el build, sin ofuscación inicial). **Los ADR prevalecen** sobre
> este documento; **este documento prevalece sobre `03`, `04` y `05` en todo lo que §3.8
> asigna a `02`**. Las comparativas anteriores a los ADR se conservan solo como contexto y van
> rotuladas **[Histórico — no seleccionada]**. Parte de [`01-requisitos.md`](01-requisitos.md)
> (§1.7, §1.8, §3.2, §4.1–§4.3). Fase de diseño: no hay código.
>
> Convención: **[H]** hecho verificado (URL o `fichero:línea`) · **[D]** decisión del
> usuario o ADR aprobado · **[S]** suposición, estimación o recomendación propia · **[PV]**
> pendiente de verificar o medir (normalmente en un prototipo o en dispositivos).
> En §3.7, **DEBE / NO DEBE / PUEDE** son normativos para la app y la PWA.

---

## 0. Resumen ejecutivo

1. **Perfil decidido [D]**: autoservicio y autocorrección sin organizador; banco, respuestas,
   lógica de corrección y clave de sello **dentro del APK**; suplantación aceptada como riesgo
   social; sin revocación en el MVP. La credencial debe resistir **errores, edición visual
   casual y falsificación no técnica**, y **no** se pretende que resista a un candidato técnico
   (`AGENTS.md`; ADR 0001).
2. **Consecuencia que hay que decir claro**: sin backend y con emisión en el móvil del
   candidato, **cualquier secreto de emisión está en el APK público y es extraíble**. La firma con
   clave embebida **no es una garantía criptográfica fuerte**: es un **sello de integridad frente
   a la edición casual** que convierte la falsificación en un acto técnico y deliberado. Una vez
   alguien extrae la clave (o invoca la función de firma), puede forjar credenciales ilimitadas e
   indistinguibles de las legítimas y compartir esa capacidad (p. ej. un "generador").
3. **Ninguna marca de integridad sirve sin un verificador independiente**: si nadie escanea el QR
   con su propio dispositivo, el organizador solo ve un PDF/imagen que cualquiera puede retocar.
   La comparación **texto visible ↔ datos que muestra el verificador** es lo que detecta la
   edición visual.
4. **Decisión vigente [D, ADR 0001]**: **QR con token firmado ECDSA P-256 (ES256) con clave
   privada incorporada en el build** + **verificador PWA estática y pública, sin secretos**, que
   solo contiene claves públicas y funciona offline una vez cargada [PV: probar]. Resultados de la
   PWA: `VÀLIDA`, `NO VÀLIDA`, `VERSIÓ DESCONEGUDA`, `FORMAT INCORRECTE`. **No hay app Android de
   organizadores ni checksum sin clave en el MVP.**
5. **Contrato `ARB1` canónico (§3.7)**: texto `ARB1.<payload_b64>.<firma_b64>`; se firman los
   **bytes ASCII de `ARB1.<payload_b64>`**; ES256 con firma **`R‖S` de 64 bytes**; base64url
   **sin relleno y canónico**; payload **JSON restringido UTF-8** (solo textos y enteros, sin
   duplicados); campos y patrones cerrados; **sin campo `crit`**: todo cambio que el verificador
   deba entender exige `ARB2`; normalización de entrada única para escaneo, imagen y pegado; orden
   de verificación con **códigos estables** mapeados a los cuatro resultados de ADR 0001. App
   (Kotlin/JCA) y PWA (WebCrypto) **DEBEN** pasar los mismos vectores (§3.7.11) antes de publicar:
   la interoperabilidad no está demostrada hasta entonces [PV].
6. **Banco [D, ADR 0002]**: fuente fuera del repo público, inyectada en el build e incluida en el
   APK **sin cifrado ni ofuscación inicial**. Se acepta que sea extraíble. **Hallazgo [H]**: el
   banco actual (75 preguntas **con índice de respuesta correcta**) ya es público en el repo base;
   la confidencialidad del fuente solo puede proteger contenido nuevo o reescrito y las
   explicaciones (§5.2). Este documento no contiene ni señala contenido del banco real.
7. **Fase 2 (§8)**, separada: revocación sin backend (lista de denegación en la PWA), registro del
   club, política de caducidad y, solo si cambia el perfil de amenaza, emisión con clave fuera del
   APK, que **rompe el autoservicio**. Una app Android de organizadores solo se revisitaría si lo
   justifican el soporte móvil, la cámara o la revocación (ADR 0001).

---

## 1. Activos, supuestos y fuera de alcance

### 1.1 Activos

| ID | Activo | Qué se espera de él en este perfil |
|---|---|---|
| A1 | **Credencial** (PDF/imagen + QR) | Detectar errores y edición/falsificación no técnica. |
| A2 | **Material de sello** (clave privada ES256 por `kid`) | Que no esté en git ni en logs/artifacts; se acepta que sea extraíble del APK. |
| A3 | **Fuente del banco** (preguntas, respuestas, explicaciones) | Fuera del historial git público, Pages, documentación, logs y artifacts (ADR 0002). |
| A4 | **Banco empaquetado** en el APK | Se acepta que sea extraíble; **sin ofuscación inicial** (ADR 0002). |
| A5 | **Clave de firma del APK** | Que solo el mantenedor pueda publicar actualizaciones (detalle en `05`). Distinta de A2. |
| A6 | **Datos personales** del candidato | Minimización (`01` §4.1): nombre, `cid`, resultado, fechas. |
| A7 | **Lista de claves públicas de confianza** (`kid → clave`) en la PWA | Integridad: que la PWA oficial solo acepte las claves publicadas por el proyecto. |

### 1.2 Supuestos verificados

- [H] Un APK es un archivo ZIP; contiene `assets/`, `res/`, `classes*.dex`, etc.
  ("An APK file consists of a ZIP archive that contains all the files that comprise your app",
  <https://developer.android.com/topic/performance/reduce-apk-size>). Leer un fichero de
  `assets/` no exige descompilar; leer constantes del código exige un descompilador común
  (jadx/apktool) [S, conocimiento estándar].
- [H] El APK se publica en GitHub Releases, es decir, en una URL HTTP pública (`AGENTS.md`).
- [D] Existe al menos un repo **público** (Pages + Actions); el código de la app y de la PWA es
  legible por cualquiera: **el formato de la credencial es público**.
- [D] No hay organizador durante el examen; la verificación es optativa y posterior.

### 1.3 Fuera de alcance del MVP (explícito)

Resistencia a decompilación, extracción de claves, repackaging o hooking; detección de
root/emulador; atestación; vinculación al dispositivo o al titular; control de intentos,
tiempo o fecha fiables; revocación; certificación con valor legal; cifrado u ofuscación del
banco. (Coherente con `01` §4.2, ADR 0001 y ADR 0002.)

---

## 2. Modelo de amenazas (perfil proporcional)

### 2.1 Tipos de atacante considerados

| Perfil | Capacidad | ¿Debe resistirse? |
|---|---|---|
| **E — Error** | Errores de transcripción, PDF mal impreso, captura recortada, versión antigua | **Sí** |
| **C — Casual no técnico** | Editor de PDF/imágenes, generador de QR en web, leer un QR con la cámara del móvil, copiar la credencial de otro | **Sí** |
| **L — Lector de código** | Lee el repo público, entiende el formato, sabe calcular un hash con una web o un script | Sí (lo cubre la firma, ADR 0001) |
| **T — Técnico** | Descompila el APK, extrae claves y banco, parchea la app, usa root/emulador | **No** [D] |
| **S — Social** | Otra persona hace el examen; préstamo del móvil | **No** (riesgo social aceptado) [D] |

### 2.2 Catálogo de amenazas y tratamiento

| ID | Amenaza | Perfil | Tratamiento en MVP |
|---|---|---|---|
| T1 | Editar el **texto visible** del PDF/imagen (nombre, resultado, fecha) | C | **Detectar**: la PWA muestra los datos del QR; si no coinciden con el texto, alterada. |
| T2 | **Regenerar el QR** con otros datos (leer el QR propio o ajeno, cambiar el nombre, generar un QR nuevo en una web) | C | **Detectar**: la firma no se puede recalcular sin la clave privada (§3.3). |
| T3 | Pegar el QR de **otra persona** en un documento con mi nombre | C | **Detectar** con la PWA (nombre del QR ≠ texto). Sin verificación, no se detecta. |
| T4 | Mostrar una **captura** o la credencial de otro como propia | C/S | **No se detecta técnicamente**; el nombre se coteja con la persona (social). |
| T5 | Mostrar una **pantalla falsa** "VÀLIDA" en su propio móvil | C | **Procedimiento**: verificar siempre en el dispositivo del organizador; la app del candidato nunca muestra `VÀLIDA` (`04`). |
| T6 | **Error de lectura** o documento dañado | E | Corrección de errores del QR (Reed-Solomon, ISO/IEC 18004 [S, estándar no re-leído]) + `FORMAT INCORRECTE` con motivo. |
| T7 | Credencial de una **versión de formato o examen antigua o retirada** | E | Versión en el prefijo `ARB<n>` y `v`, y `exv` en el payload; política de aceptación de versiones en fase 2 (§8.3). |
| T8 | Recalcular un **código de integridad** leyendo el repo público | L | Lo frena la firma: la clave privada no está en git (§3.3). |
| T9 | **Extraer la clave** de sello del APK y forjar | T | **No se resiste** [D]. Documentado (§4). Un `kid` nuevo limita el daño futuro, no el pasado. |
| T10 | **Parchear la app** para aprobar siempre, o leer las respuestas del APK | T | **No se resiste** [D]. Leer respuestas es más fácil que parchear y ya es posible (§5). |
| T11 | **Manipular el reloj** para la fecha de emisión o una caducidad derivada | C | **Se acepta**: la fecha se etiqueta "data del dispositiu" (`01` §3.2). Ver §6.6. |
| T12 | Repetir el examen hasta aprobar (borrar datos, reinstalar) | C | **Se acepta** [D]; coherente con el fin pedagógico. |
| T13 | Otra persona hace el examen | S | **Se acepta** [D]. |
| T14 | **APK falso** distribuido por un tercero (phishing, credenciales con otra clave) | C/T | Descarga desde el canal oficial; SHA-256 del APK y huella del certificado de firma (`05` §5.7). La PWA rechaza `kid` desconocidos (§3.7.8, `UNKNOWN_KID`). |
| T15 | **Verificador falso** (web que siempre dice "VÀLIDA") | C/T | El organizador usa la PWA oficial **instalada en su dispositivo**; el QR no es una URL (§6.4); dominio e icono no son prueba infalible. |
| T16 | Difusión de una **herramienta de forja** tras extraer la clave | T→C | El riesgo real de la clave embebida. **Sin detección técnica en MVP**; fase 2: `kid` nuevo + lista de claves retiradas. |
| T17 | Lectura del QR por un tercero (ve nombre y resultado) | C | Se acepta: el QR no se cifra; datos mínimos (`01` §4.1). |
| T18 | Fuga del **fuente del banco** por git, logs, artifacts o cachés de CI públicos | — | **Control obligatorio** (§5.4, ADR 0002, `05`). |
| T19 | **Discrepancia de interpretación** entre app y PWA (JSON ambiguo, firma en otro formato, bytes firmados distintos, `kid` con otra clave) | E | **Un único contrato** (§3.7), tabla de propiedad (§3.8) y vectores compartidos (§3.7.11). |

### 2.3 Niveles dentro de este perfil

| Nivel | Resiste a | No resiste a | Coste relativo [S] | Estado |
|---|---|---|---|---|
| **N0 — Solo legible** (texto + QR con datos, sin código) | E (parcial) | C: T2 trivial con un generador de QR | Mínimo | No cumple el requisito |
| **N1 — Checksum sin clave** | E, C | L, T | Mínimo (+ verificador) | [Histórico — no seleccionada] |
| **N2 — Firma con clave embebida** | E, C, L | T (y T16 si un T comparte la forja) | Bajo (+ custodia de una clave por `kid`) | **Seleccionado [D, ADR 0001]** |
| **N3 — Clave fuera del APK** (organizador, dispositivo del club, atestación) | E, C, L, buena parte de T | S | Alto y **rompe el autoservicio** | Fase 2 o nunca (§8.4) |

---

## 3. Alternativas para la credencial

> **Estado [D, ADR 0001]:** seleccionada la opción **C (firma asimétrica con clave embebida)**
> con verificador **PWA pública**. §3.0–§3.6 son la evaluación que llevó al ADR y se conservan
> como contexto; las opciones distintas de C están **[Histórico — no seleccionada]**. §3.7 y
> §3.8 son **normativos**.

### 3.0 Tabla comparativa

| Opción | Estado | Qué garantiza | Qué **no** garantiza | Coste de implementación [S] | Experiencia del organizador | Dependencia externa |
|---|---|---|---|---|---|---|
| **A. PDF/QR sin marca de integridad** | No seleccionada | Legibilidad; con verificador, detecta edición del texto visible (T1) | T2: cualquiera regenera el QR | Mínimo | Leer el papel o escanear y comparar | Ninguna |
| **B. QR con checksum sin clave** | [Histórico — no seleccionada] | T1, T2, T3 frente a no técnicos | Quien lea el repo recalcula el código (L); no autentica al emisor | Mínimo; sin secretos | Escanear y comparar | Ninguna |
| **C. Firma asimétrica con clave privada embebida** (ECDSA P-256) + **PWA pública** | **Seleccionada [D]** | B + resistencia a L; verificación solo con clave pública | Nada frente a T: extracción; forja ilimitada e indistinguible; no separa legítimas de forjadas tras el compromiso | Bajo: un par de claves por `kid`, librería estándar en ambos lados | PWA instalada: escanear/importar/pegar y comparar; cuatro resultados | Ninguna (navegador con WebCrypto) |
| **C'. HMAC con secreto embebido** | [Histórico — no seleccionada] | Como C frente a C/L | Peor que C: todo verificador puede forjar | Bajo | Igual que C | Ninguna |
| **D. PDF con firma PAdES generada en el móvil** | [Histórico — no seleccionada] | Integridad del fichero PDF | Con clave embebida = C; autofirmada; no protege capturas/impresiones | Medio-alto | Avisos de "identidad desconocida" en lectores PDF | Opcional CA de pago [PV] |
| **E. C + app Android de organizadores** | [Histórico — no seleccionada para el MVP, ADR 0001] | Igual que C + escaneo nativo | Lo mismo que C frente a T | Bajo-medio (segundo APK) | App propia | Ninguna |

### 3.1 A — QR/PDF sin firma (no seleccionada)

Un QR de texto plano se lee con la cámara nativa del móvil; cambiar el nombre y generar un QR
nuevo en cualquier web es una operación **no técnica** (T2). Solo sirve como **capa legible** que
acompaña al sello.

### 3.2 B — Checksum sin clave [Histórico — no seleccionada]

Construcción evaluada [S]: `codi = base32(SHA-256("ARB1" || payload))[0..N]`. Frenaba al perfil C
sin secretos, pero cualquiera que lea el repo público recalcula el código (L) y no autentica al
emisor. ADR 0001 eligió la firma; **el MVP no implementa checksum** ni como alternativa ni como
modo de compatibilidad.

### 3.3 C — Firma asimétrica con clave privada embebida (seleccionada)

- **Construcción**: la app firma los **bytes exactos** que transporta el token (sin
  re-serializar) con la clave privada de su `kid`; el QR lleva `prefijo + payload + firma`; la PWA
  comprueba con la clave pública de ese `kid`. El formato normativo es el **contrato `ARB1` de
  §3.7**.
- **Algoritmo** [H]: en Android, `SHA256withECDSA` está disponible desde API 11 y `Ed25519`
  solo desde API 33 en la JCA de la plataforma
  (<https://developer.android.com/reference/java/security/Signature>). Con `minSdk` 26
  (`01` §5), **ECDSA P-256 no requiere dependencias**. En WebCrypto, ECDSA con P-256 y SHA-256
  está definido en la especificación (<https://w3c.github.io/webcrypto/>, operaciones ECDSA);
  Ed25519 solo en versiones recientes de navegador
  (<https://caniuse.com/mdn-api_subtlecrypto_verify_ed25519>). Se usa **ES256** (ECDSA P-256 +
  SHA-256, RFC 7518 §3.4).
- **Tamaño** [S/PV]: payload obligatorio ≈ 150 B (ejemplo de §3.7.5) → token ≈ 295 caracteres;
  con `bv` y `app` ≈ 350. QR versión ≈ 10–14 con corrección M; escaneable en pantalla y A4.
  Medir en prototipo.
- **Detecta**: T1, T2, T3 (perfil C) y T8 (perfil L): leer el repo no basta, porque la clave
  privada no está en git.
- **No detecta — sin ambigüedad**:
  - **Extracción de la clave (T9)**. La clave privada está en el APK público. Como fichero se saca
    con `unzip`; como constante, con un descompilador; ofuscada (R8, troceada, cifrada con otra
    clave del mismo APK, código nativo) se reconstruye igualmente en memoria al usarla, o basta
    con **llamar a la función de firma** de la app desde un entorno instrumentado. Estimación [S]:
    de minutos a pocas horas para una persona con experiencia en Android.
  - **Tras la extracción**, las credenciales forjadas son **criptográficamente idénticas** a las
    legítimas. La fecha de emisión la escribe quien firma, así que `iat` no separa unas de otras
    (hallazgo B3 de `reviews/revision-intermedia-01-03.md`).
  - **Difusión (T16)**: un técnico puede publicar la clave o un "generador" y la forja pasa a ser
    casual para todos hasta que se retire el `kid`.
  - Parcheo de la app para aprobar siempre (T10): la firma será "válida" porque la emite la
    propia app.
- **Lo que aporta frente a B**: resiste al perfil L; convierte la falsificación en un **acto
  técnico y deliberado**; permite retirar `kid` en fase 2; admite en el futuro un emisor más
  fuerte con otro `kid` sin cambiar el modelo de verificación.
- **Cómo debe nombrarse**: "segell d'integritat" / "sello de integridad", **nunca** "firma
  digital segura", "certificado" ni "garantía criptográfica". Ver §4.

### 3.4 C' — HMAC con secreto compartido [Histórico — no seleccionada]

El verificador necesita el mismo secreto que el emisor: todo verificador sería una máquina de
forjar y, siendo la PWA pública, el secreto quedaría publicado. Estrictamente peor que C.

### 3.5 D — PDF con firma digital (PAdES) [Histórico — no seleccionada]

Con clave embebida tiene las mismas limitaciones que C; autofirmada, ningún lector la reconoce;
para que un lector la marque como confiable haría falta una CA de una lista de confianza
(AATL/EUTL) [S; requisitos PV]; y no protege capturas ni impresiones. El PDF es solo
**contenedor de presentación** (texto + QR); la verificación se hace con el QR.

### 3.6 Alternativas descartadas para el MVP (por exigir lo que el perfil excluye)

| Alternativa | Por qué no en MVP | Fuente |
|---|---|---|
| **QR caducable tipo TOTP** | TOTP exige secreto compartido entre quien genera y quien verifica (RFC 6238 §3, R2 y R5): la PWA pública debería llevar el secreto → cualquiera forja; además el código no dice nada del resultado del examen. | [H] <https://www.rfc-editor.org/rfc/rfc6238> |
| **QR dinámico firmado / desafío del organizador** | Sirve contra capturas reenviadas (T4), que se aceptan [D]; añade un paso con el móvil del candidato presente. | [S] |
| **Android Keystore + Key Attestation** | Una cadena válida hasta la raíz de Google indica que la clave está en hardware que Google considera seguro y que tiene las propiedades declaradas en el certificado; puede incluir los *digests* del certificado de firma de la app según el sistema operativo y el estado de arranque (`verifiedBootState`, `deviceLocked`). **No prueba** que la app se ejecutara sin modificaciones en tiempo de ejecución, que el dispositivo no esté rooteado ni que el examen se hiciera correctamente: los datos aportados por la plataforma solo son fiables si el SO cumple el modelo de seguridad de Android, `Verified` describe el arranque y no el estado posterior, y las claves de atestación pueden revocarse por extracción o mal manejo. Además: cadena grande para un QR, dispositivos sin soporte o con raíces de fabricante, y **no** protege el banco (las respuestas están en el APK). ADR 0001 lo excluye del MVP. | [H] <https://developer.android.com/privacy-and-security/security-key-attestation>, <https://source.android.com/docs/security/features/keystore/attestation> (consultados en la revisión 3) |
| **Play Integrity** | Descifrar el veredicto exige servidor ("Never decrypt tokens or expose keys within your client app") y, para descifrado propio, app publicada en Google Play; un APK instalado fuera de Play no obtiene licencia. | [H] <https://developer.android.com/google/play/integrity/setup>, <https://developer.android.com/google/play/integrity/verdicts> (consultados en la revisión 1) |
| **Organizador corrige y firma / dispositivo del club** | Clave fuera del APK, pero exige organizador por emisión: contradice el autoservicio [D]. | `01` §0.1; ADR 0001 opción 4 |

### 3.7 Contrato `ARB1` (canónico)

#### 3.7.0 Estado y precedencia

- **Único contrato** de credencial para la app (emisión y autoverificación en Kotlin/JCA,
  `core-credential`) y la PWA (WebCrypto). **Sustituye** a la revisión 3 de este apartado y a
  `03` §6.2 en todo lo que difiera (lista de pasajes afectados en §3.8.2). Concreta ADR 0001; no
  cambia ninguna decisión.
- Es una **especificación de diseño**: ninguna interoperabilidad está demostrada hasta que los
  vectores de §3.7.11 pasen en Android real (API 26+) y en la PWA [PV].
- Cualquier cambio posterior a este apartado que altere bytes, campos o resultados exige
  **nueva versión de formato** (`ARB2`, §3.7.5) o, si es aclaratorio, actualizar los vectores en
  el mismo cambio.

#### 3.7.1 Gramática del texto

```
token        = prefijo "." payload-seg "." firma-seg
prefijo      = "ARB" version          ; "ARB" en mayúsculas exactas; sin plegado de caja
version      = "1"                    ; en v1. Forma general reconocida: %x31-39 0*2DIGIT
payload-seg  = 2*683 b64url           ; base64url sin relleno de 1–512 bytes
firma-seg    = 86 b64url              ; base64url sin relleno de exactamente 64 bytes
b64url       = ALPHA / DIGIT / "-" / "_"
```

- El token es **ASCII puro**, sin espacios ni saltos de línea, y **no** es una URL (§6.4).
- Longitud máxima de un token `ARB1`: 4 + 1 + 683 + 1 + 86 = **775** caracteres; el verificador
  admite hasta 1024 (§3.7.6).
- **Base64url** [H, <https://www.rfc-editor.org/rfc/rfc4648>]: alfabeto de RFC 4648 §5, **sin
  `=`** (RFC 4648 §3.2 permite omitirlo si la especificación lo dice; esta lo dice). Se rechaza
  cualquier carácter fuera del alfabeto (§3.3) y toda codificación **no canónica**: decodificar y
  volver a codificar **DEBE** reproducir exactamente el segmento (bits sobrantes a cero, §3.5; una
  longitud `≡ 1 (mod 4)` es inválida). Nota [S]: `java.util.Base64` (Android API 26+ [H,
  <https://developer.android.com/sdk/api_diff/26/changes/pkg_java.util>]) y los decodificadores
  habituales de JS no garantizan este rechazo; por eso la regla normativa es la comparación tras
  re-codificar.

#### 3.7.2 Bytes firmados

```
entrada_firmada = bytes ASCII de  prefijo "." payload-seg
                = el texto del token desde el primer carácter hasta, sin incluir, el segundo "."
```

- Se firma **el texto transportado**, no el JSON decodificado ni re-serializado: no hay
  canonicalización y el verificador nunca reconstruye nada a partir de campos.
- Como el prefijo forma parte de la entrada firmada, cambiar `ARB1` por otra versión invalida la
  firma. El patrón es análogo al *JWS Signing Input* de RFC 7515 §5.1 con el prefijo en lugar de
  una cabecera JOSE [S].
- **Supersede `03` §6.2.2** ("se firman los bytes del payload decodificados"): una firma calculada
  así da `BAD_SIGNATURE` (vector S05).
- ES256 aplica SHA-256 internamente: **NO DEBE** pre-calcularse el hash.
  Kotlin: `Signature.getInstance("SHA256withECDSA")` + `update(entrada_firmada)`.
  PWA: `crypto.subtle.verify({name: "ECDSA", hash: "SHA-256"}, clave, firma64, entrada_firmada)`.

#### 3.7.3 Algoritmo y representación de la firma

- **Único algoritmo: ES256** = ECDSA sobre P-256 (secp256r1) con SHA-256 (RFC 7518 §3.4).
- El algoritmo **no viaja en el token** ni se acepta de él: lo fija la versión de formato y la
  entrada de confianza del `kid` (`alg: "ES256"`, §3.7.9). Sin confusión de algoritmos.
- **Representación transportada**: 64 bytes `R‖S`, cada mitad de 32 bytes *big-endian* **con
  ceros a la izquierda** (RFC 7518 §3.4, <https://www.rfc-editor.org/rfc/rfc7518#section-3.4>).
  Es lo que exige WebCrypto: su verificación ECDSA devuelve `false` si la firma no mide `2·n`
  bytes (64 en P-256) y toma `r` y `s` de cada mitad [H, <https://w3c.github.io/webcrypto/>].
- **Conversión en Android**: `SHA256withECDSA` produce la firma en **DER**
  (`SEQUENCE { r INTEGER, s INTEGER }`, RFC 3279 §2.2.3) [H para la JCA estándar,
  <https://docs.oracle.com/en/java/javase/26/docs/specs/security/standard-names.html>; PV en el
  proveedor de Android]. El emisor **DEBE** convertir DER → `R‖S`: cada `INTEGER` tiene 1–33 bytes
  de contenido; si tiene 33, el primero **DEBE** ser `0x00` y se descarta; luego se rellena a la
  izquierda con ceros hasta 32. Cualquier otra forma es un error de emisión (no se emite). El
  nombre `SHA256withECDSAinP1363Format` existe en JDK recientes, pero **no se debe depender** de él
  en Android [PV].
- **Sin requisito *low-S***: el verificador acepta `s` y `n−s` (WebCrypto no lo restringe). Por
  la maleabilidad de ECDSA [S, propiedad conocida], **la firma NO DEBE usarse como identificador**:
  el identificador es `cid`.
- Valores `r` o `s` fuera de `[1, n−1]` ⇒ la verificación falla ⇒ `BAD_SIGNATURE` (vector S04).

#### 3.7.4 Payload: UTF-8 y JSON restringido

**Bytes**: 1–512 bytes, **UTF-8 estricto** (sin secuencias *overlong*, sin sustitutos codificados,
sin bytes sueltos [S, RFC 3629 §3]) y **sin BOM** (`EF BB BF` inicial ⇒ rechazo).
Implementación [H/S]: en la PWA, `new TextDecoder("utf-8", {fatal: true, ignoreBOM: true})` lanza
error ante datos mal formados y **conserva** el BOM en la salida para poder rechazarlo (con
`ignoreBOM: false`, el valor por defecto, lo eliminaría en silencio) [H,
<https://developer.mozilla.org/en-US/docs/Web/API/TextDecoder/TextDecoder>,
<https://developer.mozilla.org/en-US/docs/Web/API/TextDecoder/ignoreBOM>]; en Kotlin, un
`CharsetDecoder` con `CodingErrorAction.REPORT`, **nunca** `String(bytes, UTF_8)`, que sustituye
en silencio [S].

**JSON restringido** (subconjunto de RFC 8259 [H, <https://www.rfc-editor.org/rfc/rfc8259>]):

- El texto JSON es **un objeto**; se admiten espacios insignificantes de RFC 8259 al verificar
  (el emisor no los escribe).
- Cada valor es **texto** o **entero**. Entero = `0` o `[1-9][0-9]{0,8}` (sin signo, sin
  fracción ni exponente). `true`, `false`, `null`, arrays y objetos ⇒ rechazo.
- Textos: escapes de RFC 8259 §7 admitidos; `\uXXXX` con pares sustitutos válidos; un sustituto
  aislado ⇒ rechazo.
- **Sin nombres duplicados**, comparados **tras resolver escapes** (`"nom"` y `"n\u006fm"` son
  duplicados). RFC 8259 §4: con duplicados "the behavior of software that receives such an
  object is unpredictable". `JSON.parse` conserva el último sin avisar [S, comportamiento
  habitual], así que la PWA **DEBE** usar un analizador que detecte duplicados; para esta
  gramática basta un analizador propio pequeño en cada lenguaje [S].
- Como máximo **16 miembros**.

#### 3.7.5 Campos `v = 1`, extensiones y versionado

| Campo | Tipo y patrón exacto | Oblig. | Se valida | Uso / visualización |
|---|---|---|---|---|
| `v` | Entero `1`; **DEBE** ser igual a la versión del prefijo | Sí | Antes de la firma | Versión de formato |
| `kid` | Texto `^[a-z0-9]{1,16}$` | Sí | Antes de la firma | Selecciona la clave (§3.7.9). No se muestra como dato del titular |
| `cid` | Texto `^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$` (UUID v4 en minúsculas) | Sí | Tras la firma | **Código corto** visible = primeros 8 hex en mayúsculas como `XXXX-XXXX` (mismo en PDF y PWA) |
| `nom` | Texto; reglas de §3.7.5.1 | Sí | Tras la firma | Nombre visible |
| `res` | Exactamente `"APTE"` | Sí | Tras la firma | Resultado (solo se emiten aprobados) |
| `exv` | Texto `^[0-9A-Za-z._-]{1,16}$` | Sí | Tras la firma | Versión de reglas. Esquema de valores: `05` §7.2 (producción: entero como texto, p. ej. `"1"`) |
| `iat` | Texto `^[0-9]{4}-[0-9]{2}-[0-9]{2}$` y **fecha gregoriana válida** (mes 01–12, día válido, bisiestos); sin hora ni zona; sin rango | Sí | Tras la firma | Fecha **local** del reloj del dispositivo al emitir; se muestra como "data del dispositiu"; **no** decide validez |
| `orig` | Exactamente `"autoservei"` | Sí | Tras la firma | Origen de la emisión |
| `bv` | Texto `^[0-9A-Za-z._+-]{1,24}$` | No | Tras la firma (si existe) | `bank_version` pública y no sensible. Si la app lo emite es decisión residual de `01`/`03`/`05`; el contrato admite ambos casos |
| `app` | Texto `^[0-9A-Za-z._+-]{1,32}$` (`versionName`) | No | Tras la firma (si existe) | Diagnóstico; no se muestra en el resultado principal |

**Nombres reservados en `v=1`**: `exp`, `nbf`, `crit`, `alg`. La app `v=1` **NO DEBE** emitirlos;
su presencia en un token con firma válida ⇒ `BAD_FIELDS`. Así una futura caducidad no puede
colarse en `v=1` y ser ignorada por verificadores antiguos.

**Miembros desconocidos** (cualquier otro nombre): se **ignoran** y no se muestran, sujetos a la
gramática y a los límites. Permiten añadir datos informativos sin cambiar de versión.

**Extensiones críticas — regla única**: **no hay campo `crit`**. Todo cambio que un verificador
**deba** entender (nuevo campo obligatorio, nuevo significado, caducidad, otro algoritmo u otra
representación) exige **nueva versión de formato** (`ARB2`, `v = 2`). Un verificador que no la
conoce responde `VERSIÓ DESCONEGUDA` por el prefijo, sin analizar el resto. **Supersede** el
mecanismo `crit` y el código `UNKNOWN_CRITICAL` de `03` §6.2.3/§6.2.5.

##### 3.7.5.1 Reglas de `nom`

Comprobadas por la app al emitir y por el verificador tras la firma:

1. 1–80 **puntos de código** (no unidades UTF-16: en JS `[...nom].length`, en Kotlin
   `codePointCount`).
2. Ni empieza ni termina por U+0020 y no contiene dos U+0020 seguidos.
3. No contiene ningún carácter del conjunto **F** (lista fija, independiente de la versión de
   Unicode de cada plataforma): U+0000–U+001F, U+007F–U+009F, U+00A0, U+00AD, U+061C, U+1680,
   U+180E, U+2000–U+200F, U+2028–U+202F, U+205F–U+206F, U+3000, U+FEFF, U+FFF9–U+FFFB
   (controles, espacios no ASCII, invisibles y controles bidireccionales que permitirían mostrar
   un nombre engañoso).

Solo la app, al construir el nombre desde lo que teclea el usuario: sustituye por U+0020 los
caracteres U+0009, U+000A, U+000D, U+00A0, U+1680, U+2000–U+200A, U+202F, U+205F y U+3000;
colapsa espacios; recorta; aplica **NFC**; y comprueba 1–3 (si falla, no emite y lo explica,
`04`). El verificador **no** comprueba NFC: las tablas Unicode de ICU en Android y del navegador
pueden diferir y el nombre ya está protegido por la firma; la NFC se prueba en el emisor.

##### 3.7.5.2 Serialización del emisor y ejemplo ficticio

Recomendada (no afecta a la verificación, que opera sobre bytes): orden `v, kid, cid, nom, res,
exv, iat, orig, bv, app`; sin espacios; caracteres no ASCII en UTF-8 directo; solo se escapan
`"` y `\`. Obligatorio: cumplir §3.7.4 y §3.7.5.

Ejemplo **ficticio** (151 bytes):

```json
{"v":1,"kid":"s1","cid":"3f0c1e9a-5b7d-4c2e-9a10-6d8b2f4e7c01","nom":"Anna Exemple Puig","res":"APTE","exv":"1","iat":"2026-11-20","orig":"autoservei"}
```

→ `payload-seg` de 202 caracteres; token de 4 + 1 + 202 + 1 + 86 = **294** caracteres.

#### 3.7.6 Límites

| Límite | Valor | Si se supera |
|---|---|---|
| Entrada bruta antes de normalizar (unidades UTF-16, `length` en JS y Kotlin) | ≤ 2048 | `BAD_FORMAT` |
| Token `ARB1` normalizado | ≤ 1024 caracteres (máximo legítimo 775) | `BAD_FORMAT` |
| Payload decodificado | 1–512 bytes | `BAD_FORMAT` |
| Firma decodificada | exactamente 64 bytes | `BAD_FORMAT` |
| Miembros del objeto | ≤ 16 | `BAD_PAYLOAD` |
| Entero | ≤ 9 dígitos | `BAD_PAYLOAD` |
| `nom` | 1–80 puntos de código | `BAD_FIELDS` |

Supersede los 1024/2048 divergentes de la revisión 3 y de `03` §6.2.4: 2048 es el límite de
**entrada**, 1024 el del **token**.

#### 3.7.7 Normalización de la entrada (escaneo, imagen y pegado)

Una **única función** para todos los canales de la PWA (cámara, imagen importada, texto pegado):

1. Si la entrada bruta supera 2048 unidades UTF-16 ⇒ `BAD_FORMAT`.
2. Eliminar **todas** las apariciones, en cualquier posición, de: U+0009, U+000A, U+000D, U+0020,
   U+00A0, U+200B, U+FEFF.
3. **Nada más**: ni cambio de mayúsculas, ni normalización Unicode, ni eliminación de guiones, ni
   sustitución de caracteres parecidos.
4. Resultado vacío ⇒ `BAD_FORMAT`.

Ninguno de esos caracteres pertenece al alfabeto del token, de modo que quitarlos no puede
convertir un texto en un token distinto del que se imprimió, y la firma decide igualmente. Esto
**supersede** la regla de la revisión 3 ("espacio interno ⇒ `FORMAT INCORRECTE`") y **coincide**
con `04` §5.5 (impresión del token en varias líneas). Obligación del emisor (`03` §6.5, `04`): si
el token se muestra o imprime como texto partido, solo con saltos de línea, **nunca** con guion
de división (el `-` es parte del alfabeto).

#### 3.7.8 Verificación y códigos de resultado

Orden fijo; el **primer** fallo determina el código. La PWA y el verificador Kotlin de
`core-credential` (autoverificación y pruebas) **DEBEN** devolver el mismo código para el mismo
texto.

| Paso | Fase | Comprobación | Código si falla |
|---|---|---|---|
| 1 | Entrada | Normalización de §3.7.7 | `BAD_FORMAT` |
| 2 | Entrada | Empieza por `http://` o `https://` (sin distinguir mayúsculas) | `IS_URL` |
| 3 | Envoltorio | Empieza por `ARB` + versión `[1-9][0-9]{0,2}` + `.` (ni `ARB0`, ni `ARB01`, ni minúsculas) | `BAD_FORMAT` |
| 4 | Envoltorio | Versión soportada (MVP: solo `1`). **No se analiza el resto**: un formato futuro puede tener otra estructura | `UNKNOWN_VERSION` |
| 5 | Envoltorio | Token ≤ 1024; exactamente 3 segmentos; gramática de §3.7.1; base64url canónico; payload 1–512 bytes; firma 64 bytes | `BAD_FORMAT` |
| 6 | Payload, antes de la firma | UTF-8 estricto sin BOM; JSON restringido (§3.7.4) sin duplicados, ≤ 16 miembros; `v` entero igual a la versión del prefijo; `kid` presente y con su patrón | `BAD_PAYLOAD` |
| 7 | Clave | `kid` presente en la lista de confianza de **esta** PWA | `UNKNOWN_KID` |
| 8 | Clave | Estado del `kid` aceptado (`active` o `retired`); `compromised` no lo está | `KID_NOT_ACCEPTED` |
| 9 | Firma | ES256 verifica `entrada_firmada` (§3.7.2) con la clave del `kid` | `BAD_SIGNATURE` |
| 10 | Campos, tras la firma | Obligatorios presentes; tipos, patrones y valores de §3.7.5; reglas de `nom`; sin nombres reservados | `BAD_FIELDS` |
| 11 | — | Todo correcto | `OK` |

Por qué este orden [S]: los campos semánticos se validan **después** de la firma para que una
alteración por terceros que deje el JSON bien formado se informe como `NO VÀLIDA` y para no
interpretar contenido no autenticado más allá de `v` y `kid`. `BAD_FIELDS` solo puede aparecer
con una firma válida: indica un emisor defectuoso o una forja con la clave; en ambos casos la
credencial no se acepta.

**Correspondencia con los resultados de ADR 0001** (los textos en catalán y el diseño de cada
pantalla son de `04`):

| Código | Resultado | Motivo que `04` debe transmitir | Datos del token |
|---|---|---|---|
| `OK` | `VÀLIDA` | Segell correcte amb una clau coneguda + "comprova que coincideixen amb el document i la persona" + aviso de §4 | Se muestran como datos del sello |
| `IS_URL` | `FORMAT INCORRECTE` | És un enllaç, no una credencial | — |
| `BAD_FORMAT` | `FORMAT INCORRECTE` | No és un codi de credencial o s'ha llegit malament | — |
| `BAD_PAYLOAD` | `FORMAT INCORRECTE` | Codi danyat o mal llegit | — |
| `BAD_FIELDS` | `FORMAT INCORRECTE` | Contingut no conforme | Solo como "dades NO fiables", si `04` lo ofrece |
| `UNKNOWN_VERSION` | `VERSIÓ DESCONEGUDA` | Versió de credencial més nova: actualitza el verificador | — |
| `UNKNOWN_KID` | `NO VÀLIDA` | Clau no reconeguda; si el verificador és antic, actualitza'l | Solo como "dades NO fiables" |
| `KID_NOT_ACCEPTED` | `NO VÀLIDA` | Clau retirada per seguretat | Solo como "dades NO fiables" |
| `BAD_SIGNATURE` | `NO VÀLIDA` | Dades alterades o segell incorrecte | Solo como "dades NO fiables" |

- **Nunca** se acepta por defecto un `kid`, una versión o un algoritmo desconocidos.
- `kid` desconocido con versión soportada ⇒ siempre `UNKNOWN_KID` (`NO VÀLIDA`), nunca
  `VERSIÓ DESCONEGUDA`: una PWA no puede distinguir una clave nueva legítima de una inventada.
  `VERSIÓ DESCONEGUDA` solo se produce por el prefijo (paso 4). Esto resuelve la pregunta 2 de
  `04` y supersede la propuesta de `04` §5.6 V6 de mostrar `VERSIÓ DESCONEGUDA` por `kid` futuro
  o "campo crítico".
- La app del candidato usa este verificador solo **internamente** (autoverificación, §3.7.10):
  **nunca** muestra `VÀLIDA` (T5).

#### 3.7.9 Claves de confianza (`kid → clave pública`)

Esquema de cada entrada `trusted-keys/<kid>.json` (repo público; ubicación, revisión y despliegue
en `05`):

```json
{
  "kid": "s1",
  "alg": "ES256",
  "spki": "<base64url sin relleno del DER SubjectPublicKeyInfo>",
  "sha256_spki": "<SHA-256 del DER SPKI, 64 caracteres hex en minúsculas>",
  "status": "active",
  "valid_from": "AAAA-MM-DD"
}
```

- `spki`: DER `SubjectPublicKeyInfo` (RFC 5280 §4.1) con `id-ecPublicKey` y `secp256r1`, punto sin
  comprimir (RFC 5480) [S, estándares no re-leídos]; ≈ 91 bytes [S]. Se codifica en **base64url
  sin relleno** para reutilizar el decodificador del token. **Supersede** `spki_pem` de `03`
  §6.2.6 y `05` §5.5.
- La PWA importa con `crypto.subtle.importKey("spki", der, {name: "ECDSA", namedCurve: "P-256"},
  false, ["verify"])` y, al cargar la lista, comprueba que `kid` coincide con el nombre del
  fichero y con `^[a-z0-9]{1,16}$`, que `alg == "ES256"` y que `SHA-256(spki) == sha256_spki`. Una
  entrada incoherente se descarta (y la puerta G1 de `05` impide publicarla).
- `status`: `active` y `retired` ⇒ aceptado; `compromised` ⇒ `KID_NOT_ACCEPTED`. Cualquier otro
  valor ⇒ entrada descartada. `valid_from` es **informativo**: no decide validez, porque `iat` no
  es fiable (§3.3).
- La PWA lleva una **copia** de la lista en su despliegue; nunca descarga claves ni las toma del
  token.
- **Claves de prueba**: `kid=test` (y `dev` en builds de depuración) viven solo en `test-vectors/`
  y en builds de depuración. La lista de producción **NO DEBE** contenerlas; en producción dan
  `UNKNOWN_KID`.
- **Correspondencia privada↔pública** (requisito; puertas G2/G3 de `05` §5.8): la clave pública
  derivada de la privada inyectada en el build **DEBE** coincidir **byte a byte** con `spki` de la
  entrada de su `kid` en el commit etiquetado, y un token de autocomprobación firmado con ella
  **DEBE** dar `OK` en el código de la PWA a publicar.
- La huella del pie de la PWA es **solo informativa**: una página sustituida puede sustituir a la
  vez claves y huella. La referencia de confianza es `trusted-keys/` en el tag y la copia del
  mantenedor fuera de línea.
- **Orden de publicación**: la entrada pública nueva se despliega en la PWA **antes** que el APK
  que la usa (`05` §5.5, §6.7).
- **Clave privada en el APK**: material PKCS#8 DER incorporado durante el build; **el sello de cada
  credencial se genera en tiempo de ejecución** en el móvil (aclara la frase operativa de ADR 0001).
  La representación concreta es de `05` §4.3.1/G4 [PV]; ninguna representación la oculta (§3.3).

#### 3.7.10 Emisión (app)

1. Construir el nombre según §3.7.5.1 (confirmado por el usuario, `03` §6.1) y la fecha `iat` como
   fecha local del dispositivo.
2. Construir el payload con los campos de §3.7.5 (sin nombres reservados) y serializarlo **una
   vez** a bytes UTF-8 (§3.7.5.2); comprobar ≤ 512 bytes.
3. `payload-seg` = base64url sin relleno; `entrada_firmada` = ASCII de `ARB1.` + `payload-seg`.
4. Firmar con ES256 y convertir DER → `R‖S` (§3.7.3); `token = entrada_firmada + "." + firma-seg`.
5. **Autoverificar** el texto completo con el verificador de §3.7.8 y la entrada de confianza de su
   `kid` incluida en la app; si el código no es `OK`, **no se emite** (error interno, sin
   credencial a medias).
6. Guardar el **texto exacto** del token; nunca re-firmar ni reconstruirlo desde campos. El QR, el
   PDF y la pantalla usan ese texto y el mismo `nom` (fuente única).

#### 3.7.11 Vectores compartidos (condición previa a publicar)

`test-vectors/` (repo público, nombres ficticios, **ningún contenido del banco**):
`trusted-keys-test.json` con `kid=test` (`active`) y `kid=testx` (`compromised`), y un fichero de
casos con `id`, `input` (texto exacto, con escapes si hace falta), `code` y `result` esperados.
Ejecución (puertas G1/G3/G5, `05` §5.8): los vectores públicos corren en CI/G5; R01 y K02 son exclusivos de release/G3. En el contexto correspondiente, el verificador Kotlin en JVM **y en dispositivo o emulador API
26+** (el proveedor JCA de Android no es el del JDK [PV]), y la PWA en navegador y en Node con
WebCrypto [PV]. Cadena real **Android firma → PWA verifica**: los casos V01–V05 los genera el
emisor de producción, instrumentado con `kid=test`, en Android.

| Grupo | Casos mínimos | Código |
|---|---|---|
| **Válidos** | V01 solo obligatorios, nombre ASCII · V02 acentos y `l·l` en UTF-8 directo · V03 el mismo nombre con escapes `\u00XX` · V04 `nom` de 80 puntos de código con un carácter fuera del BMP · V05 con `bv` y `app` · V06 con miembro desconocido · V07 JSON con espacios insignificantes · V08 `r` o `s` con ceros a la izquierda · V09 `s` alto (`n−s` de V01) · V10 V01 con espacios, tabuladores, saltos, U+00A0 y U+200B intercalados y exteriores · V11 `nom` en NFD firmado por un emisor de prueba | `OK` |
| **Entrada y envoltorio** | F01 vacío o solo espacios · F03 entrada > 2048 · F04 `arb1.`, `ARB0.`, `ARB01.`, `ARB1` sin punto · F05 2 o 4 segmentos · F06 con `=`, con `+` o `/`, con carácter no ASCII · F07 base64url no canónico en payload y en firma · F08 firma de 63 o 65 bytes, firma DER · F09 payload vacío o de 513 bytes, token > 1024 | `BAD_FORMAT` |
| | F02 `https://…` y `HTTP://…` | `IS_URL` |
| **Versión** | U01 `ARB2.` + resto válido · `ARB2.xyz` · `ARB999.` | `UNKNOWN_VERSION` |
| **Payload** | P01 UTF-8 inválido (*overlong*, sustituto codificado, byte suelto) · P02 BOM · P03 no objeto, mal formado, coma final · P04 valor anidado, array, `true`/`null`, decimal, exponente, negativo, entero de 10 dígitos · P05 nombre duplicado, también por escape · P06 `\ud800` aislado · P07 falta `v` o `kid`, `v` como texto, `v=2` en `ARB1`, `kid` con mayúsculas o `-` · P08 17 miembros · P09 un byte del payload alterado que rompe el JSON | `BAD_PAYLOAD` |
| **Claves** | K01 `kid` inexistente · K02 `kid=test` contra la lista de producción | `UNKNOWN_KID` |
| | K03 `kid=testx` | `KID_NOT_ACCEPTED` |
| **Firma** | S01 un carácter de `nom` cambiado (JSON válido) · S02 un bit de la firma cambiado · S03 payload de un token con la firma de otro · S04 `r = 0`, `s = 0` o `r ≥ n` · **S05 firma sobre los bytes JSON decodificados (contrato antiguo de `03`)** · S06 firmado con otra clave P-256 y `kid=test` | `BAD_SIGNATURE` |
| **Campos** (firmados con `test`) | C01 falta cada obligatorio · C02 `res` = `"apte"` o `"APTA"` · C03 `orig` distinto · C04 `cid` en mayúsculas o no v4 · C05 `iat` = `2026-02-30`, con hora o entero · C06 `nom` vacío, con espacio inicial/final, doble espacio, U+0007, U+202E o 81 puntos de código · C07 `exv`, `bv` o `app` fuera de patrón · C08 con `exp`, `nbf`, `crit` o `alg` | `BAD_FIELDS` |
| **Release** | R01 token de autocomprobación firmado con la clave inyectada (§3.7.9) | `OK` en la PWA a publicar |

Emisor (pruebas unitarias de `03`, además de V01–V05): entrada con NFD, espacios múltiples y
U+00A0 ⇒ `nom` NFC normalizado; conversión DER → `R‖S` con `INTEGER` de 31, 32 y 33 bytes.

### 3.8 Propiedad del contrato y alineación

#### 3.8.1 Tabla de *ownership*

| Tema | Dueño normativo | Implementa / consume | Qué **no** decide el dueño |
|---|---|---|---|
| Decisiones de fondo (sello asimétrico, clave embebida, PWA pública, cuatro resultados, banco privado sin ofuscación) | **ADR 0001 / 0002** | Todos | — (no se reabren) |
| Gramática, bytes firmados, ES256, `R‖S`, base64url, UTF-8/JSON, campos y patrones, nombres reservados, versionado y extensiones, límites, normalización de entrada, orden de verificación, códigos y su correspondencia con los resultados | **`02` §3.7** | `03` (`core-credential`: emisor y verificador Kotlin), PWA (`05` §6.7), `04` | Textos, pantallas, valores concretos de `exv`/`bv`/`kid` |
| Esquema de la entrada `trusted-keys/<kid>.json`, estados aceptados y regla de correspondencia privada↔pública | **`02` §3.7.9** | `05` (ubicación, CODEOWNERS, despliegue, puertas G1–G4) | Custodia y generación |
| Casos de prueba y resultados esperados | **`02` §3.7.11** | `03` genera los válidos en Android; `05` los ejecuta en CI (G1) | Herramientas de CI |
| Textos en catalán por código, diseño de pantallas, qué datos se muestran y cómo ("dades NO fiables"), formato visible de la fecha | **`04`** | PWA, app | Qué código corresponde a cada caso |
| Implementación de la emisión (UI del nombre, persistencia, DER → `R‖S`, renderizado del QR y del token sin guion de división) | **`03`** §6 | — | Bytes, campos y patrones |
| Representación de la clave privada en el APK; generación, custodia y rotación de claves; publicación de `trusted-keys/` y de la PWA; puertas G1–G4 | **`05`** §4.3.1, §5.5, §5.8, §6.7 | `03` | Esquema de la entrada y criterio de aceptación de las puertas |
| Significado de `exv` y `bv`; esquema de valores (`exv` entero como texto, `bv` `AAAA.MM.N`, `kid` = `s<MAJOR>`) | **`03` §5.2 / `05` §7.2–§7.3** | `02` (solo patrón sintáctico) | Patrón sintáctico admitido |
| Si la credencial incluye `bv` | **Decisión residual** (`01` p. 7, `03` p. 3, `05` p. 16) | — | El contrato admite ambos casos |
| Caducidad, revocación, versiones aceptadas | **Fase 2** (`02` §8) | — | En `v=1` no hay `exp` (nombre reservado) |

#### 3.8.2 Pasajes de otros documentos que este contrato supersede

Alineación **editorial** pendiente en esos documentos; mientras tanto, prevalece §3.7.

| Documento | Pasaje | Divergencia | Regla canónica |
|---|---|---|---|
| `03` §6.2.2 | Bytes firmados | Firma los bytes JSON decodificados | ASCII de `ARB1.<payload-seg>` (§3.7.2; vector S05) |
| `03` §6.2.3 | Campos | `res "apte"`; `iat` entero Unix; `kid` con `-`; `app ≤ 16`; campo `crit` | §3.7.5 (`"APTE"`, fecha `AAAA-MM-DD`, `[a-z0-9]`, `≤ 32`, sin `crit`) |
| `03` §6.2.4 | Límites | Texto ≤ 2048 como límite del token | Entrada ≤ 2048, token ≤ 1024 (§3.7.6) |
| `03` §6.2.5, §7.2 | Orden y códigos | 3 segmentos antes de la versión; `UNKNOWN_CRITICAL`; tipos tras la firma sin código propio | §3.7.8 (versión antes de segmentos; `BAD_FIELDS`; sin `UNKNOWN_CRITICAL`) |
| `03` §6.2.6, `05` §5.5 | Entrada de confianza | `spki_pem` | `spki` en base64url sin relleno (§3.7.9) |
| `03` §6.2.7, `05` §5.8 G1, `05` §6.7 | Vectores | "Campo crítico desconocido" | §3.7.11 (C08 y U01) |
| `04` §5.5 | Pegado | — | **Compatible** con §3.7.7 (la divergencia era de la revisión 3 de `02`) |
| `04` §5.6 V4 | Línea "Caducitat" | `v=1` no lleva `exp` | Sin línea de caducidad en `v=1` |
| `04` §5.6 V4 | Código corto `7F3A-92KQ` | No es hexadecimal | Primeros 8 hex de `cid` en mayúsculas, `XXXX-XXXX` (§3.7.5) |
| `04` §5.6 V6, p. 2 | `VERSIÓ DESCONEGUDA` por `kid` futuro o crítico; referencia a "`02` §6.3 punto 4" | — | Solo por prefijo; `kid` desconocido ⇒ `UNKNOWN_KID` (§3.7.8) |
| `05` §5.8 G3 | Firma de un mensaje aleatorio | Compatible | Además, R01 (§3.7.11) con un token conforme |

---

## 4. Advertencia obligatoria sobre la clave embebida

Aviso corto aprobado en ADR 0001 (en la credencial, la PWA y la web de descarga):

> «Aquesta marca ajuda a detectar canvis casuals; no és una prova contra un dispositiu modificat
> o una clau extraïda.»

Texto ampliado para la ayuda de la app y de la PWA [S]:

> "La credencial porta un segell d'integritat generat per l'app. Serveix per detectar errors i
> modificacions casuals del document. **No és una garantia criptogràfica forta**: com que
> l'examen és en autoservei i sense servidor, una persona amb coneixements tècnics podria
> extreure la clau de l'app i generar credencials falses que semblarien vàlides. La credencial
> es basa en la confiança dins del club."

Consecuencias de diseño:

1. La credencial lleva `orig = "autoservei"` y el PDF lo dice en texto visible.
2. La PWA usa las etiquetas de ADR 0001 (`VÀLIDA`, `NO VÀLIDA`, `VERSIÓ DESCONEGUDA`,
   `FORMAT INCORRECTE`). `VÀLIDA` significa "segell correcte amb una clau coneguda", **no**
   "autèntica" ni "certificada": siempre va acompañada de la instrucción de comparar datos y del
   aviso corto (`04`).
3. Ningún documento de diseño, ADR ni texto de usuario llamará a esto "firma digital segura".

---

## 5. Confidencialidad del banco de preguntas

### 5.1 La tensión, sin rodeos

- El banco, las respuestas y la corrección **están en el APK** [D, ADR 0002]; el APK es un ZIP
  público [H]. Quien lo descargue puede extraerlo. **No hay forma de impedirlo sin backend**, y
  se acepta.
- Además, la app **muestra la respuesta correcta** de cada pregunta fallada [D]: repetir el
  examen revela el banco igualmente. Con 20 de 75 preguntas por examen, la probabilidad de que
  una pregunta no haya salido en *k* exámenes es (55/75)^k: tras 5 intentos se ha visto ~79 % del
  banco; tras 10, ~95 % [S, cálculo]. **Es una característica pedagógica, no una fuga.**
- Lo que se protege [D, ADR 0002] es el **fuente**: que el banco real no aparezca en el
  repositorio público ni en su historial, Pages, documentación, ejemplos, logs o artifacts
  accesibles. La única excepción explícita es el **APK final**, que lo contiene por diseño.

### 5.2 Hallazgo: el banco actual ya es público [H]

- El repo base `cfbbarcelona/arbitratge` es **público** (`"private": false`,
  <https://api.github.com/repos/cfbbarcelona/arbitratge>).
- Su rama principal contiene, en un script generador, las preguntas del banco actual con el
  **índice de la respuesta correcta**; el volcado SQL del banco sí está excluido por su
  `.gitignore`, pero el generador no. Verificado [H] en la revisión 2 de este documento contra el
  repo local y GitHub. **A propósito, este documento no reproduce la ruta, las líneas, los
  commits, las URL directas ni ningún contenido**, para no servir de índice del banco real
  (ADR 0002); el mantenedor conoce la ubicación.
- **Implicación** (aceptada en ADR 0002): no se recupera la confidencialidad de esas preguntas.
  La regla del fuente protege **preguntas nuevas o reescritas y las explicaciones**. Que el
  contenido ya fuera público **no autoriza** a reproducirlo ni a señalarlo en la documentación de
  este proyecto, ni en ejemplos o vectores.

### 5.3 Opciones evaluadas para el banco

> **Estado [D, ADR 0002]:** fuente fuera del repo público (repo privado, artifact privado o
> carpeta local excluida de git), inyectado en el build e incluido en el APK **sin cifrado ni
> ofuscación inicial**. La vía operativa inicial (local o CI privada) la concreta `05`.

| # | Opción | Estado | Comentario |
|---|---|---|---|
| B1 | Fuente en repo privado; CI lo lee con un token de solo lectura | Permitida por ADR 0002 | Token de un solo repo como secret; controles de §5.4. |
| B2 | Fuente como secret de Actions | [Histórico — no seleccionada] | Límite de **48 KB** por secret [H, <https://docs.github.com/en/actions/reference/security/secrets>]; edición incómoda. |
| B3 | Banco cifrado en el repo público + clave en secret | [Histórico — no seleccionada] | Si la clave se filtra, el historial cifrado queda legible para siempre; contradice ADR 0002. |
| B4 | Compilación local del mantenedor con el banco fuera de git | Permitida por ADR 0002 | Simple; depende de una máquina y una persona. |
| B5 | Codificación/cifrado ligero del banco dentro del APK | [Histórico — no seleccionada; ADR 0002] | La clave o la lógica de desbloqueo estaría en el mismo APK: añade complejidad sin proteger frente al adversario técnico. **No se implementa en el MVP.** |
| B6 | Banco separado del APK como paquete descargable | [Histórico — no seleccionada] | Sería un asset HTTP público independiente o exigiría servidor. |
| B7 | Rotación/ampliación editorial del banco por versión | Trabajo editorial, no control técnico | Única vía para tener contenido no publicado previamente (§5.2). |

### 5.4 Controles de CI y publicación (detalle en `05`)

- [H] En un repo público, los **artifacts** de Actions los descarga cualquiera con acceso de
  lectura (<https://docs.github.com/en/actions/how-tos/manage-workflow-runs/download-workflow-artifacts>).
  **No subir nunca el banco ni ficheros intermedios como artifact.**
- Los **logs** de un repo público son públicos: no imprimir el banco ni listar directorios que lo
  contengan. El enmascarado de secrets no aplica a contenido derivado.
- [H] Los secrets **no se pasan** a workflows lanzados desde forks
  (<https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets>).
  No usar `pull_request_target` con checkout de código del PR
  (<https://docs.github.com/en/actions/reference/security/securely-using-pull_request_target>).
- No guardar el banco en la **caché** de Actions [PV: alcance de las cachés respecto a forks].
- Limitar quién puede modificar workflows (cualquiera con escritura puede exfiltrar secrets).
- Los `test-vectors/` y ejemplos de la credencial usan solo datos ficticios y nunca contenido del
  banco (§3.7.11).

---

## 6. Verificación por organizadores

### 6.1 Qué necesita el verificador

| Esquema | Qué lleva el verificador | ¿Secreto? | Si se copia el verificador |
|---|---|---|---|
| **C (firma, seleccionada)** | Lista de claves **públicas** por `kid` (§3.7.9) | **No** | Nada: con claves públicas no se puede firmar. **Puede ser pública.** |
| B (checksum) [Histórico] | El algoritmo | No | Nada nuevo: ya es público. |
| C' (HMAC) [Histórico] | El secreto | Sí | Cualquiera forja. |

Conclusión [D, ADR 0001]: el verificador es una **PWA pública sin secretos**; no necesita
distribución restringida ni clave de activación. Lo único que debe protegerse es su
**integridad** (que la PWA oficial lleve la lista de claves correcta, §3.7.9) y que el organizador
use la **dirección oficial** (T15).

### 6.2 Formas de verificador

| Forma | Estado | Pros | Contras |
|---|---|---|---|
| **V3. PWA estática pública** (cámara/imagen/texto + WebCrypto, instalable, offline tras cargar) | **Seleccionada [D, ADR 0001]** | Sin APK adicional; vale en Android y otros sistemas; solo claves públicas | Offline, cámara e instalación **pendientes de prueba** [PV]; riesgo de páginas clonadas (T15); una PWA no actualizada desconoce `kid` nuevos |
| V0. Solo visual (sin herramienta) | Práctica posible, no detecta | Cero coste | No detecta T2/T3 |
| V1. App Android de organizadores separada | [Histórico — no seleccionada para el MVP] | Escaneo nativo, offline | Segundo APK; ADR 0001 la descarta para el MVP; revisable en el futuro |
| V2. "Modo verificador" en la app del candidato | [Histórico — no seleccionada] | Un solo APK | Riesgo T5; la app del candidato necesitaría cámara |

### 6.3 Qué muestra la PWA

1. Uno de los cuatro resultados de ADR 0001 con su motivo en catalán, según el código de §3.7.8.
2. Solo con `OK` (`VÀLIDA`): **los datos decodificados del QR** en letra grande (nombre,
   resultado, "data del dispositiu", `exv`, código corto de `cid`, `bv` si existe) e instrucción
   "Comprova que coincideixen amb el document i amb la persona".
3. Aviso corto fijo de §4.
4. Nunca acepta `kid`, versión o algoritmo desconocidos (§3.7.8).

### 6.4 QR como payload, no como URL

El QR contiene el token `ARB1…`, **no** una URL [S, adoptado en `04` §5.7]. Un QR-URL permitiría
abrir el verificador con la cámara nativa (el fragmento `#…` no se envía al servidor, RFC 3986
§3.5, <https://www.rfc-editor.org/rfc/rfc3986#section-3.5>), pero facilitaría T15: un falsificador
imprime un QR que apunta a su propio dominio con una página que siempre dice "VÀLIDA". La PWA
trata cualquier URL como `FORMAT INCORRECTE` con aviso (`IS_URL`, §3.7.8 paso 2).

### 6.5 Opción ligera: "lista del club" (fase 2 o práctica sin software)

El candidato envía su PDF a la secretaría; esta lo verifica una vez con la PWA y lo apunta en la
lista de árbitros del club (hoja privada). Aporta un control humano y una "revocación" trivial
(borrar de la lista); cuesta un paso manual y un tratamiento de datos personales. **No** requiere
cambiar la emisión. No meter la lista con nombres en la PWA ni en Pages públicos.

### 6.6 Revocación y caducidad en el MVP

- **Revocación: no hay** [D]. `cid` y `kid` se incluyen desde `v=1` para no romper el formato; el
  estado `compromised` de §3.7.9 solo se usaría ante una fuga, y la política es de fase 2.
- **Caducidad: sin política cerrada.** `exp` es un **nombre reservado** en `v=1` (§3.7.5): la app
  no lo emite y la PWA no promete vigencia. Si se introduce, exige **`ARB2`**, debe separar
  **firma correcta** de **vigencia**, y asumir que la fecha de emisión procede del reloj del
  candidato (T11) y la comparación, del reloj del verificador. Alternativa evaluada: vigencia por
  versión de reglas (`exv` aceptadas en la PWA, fase 2).

---

## 7. Diseño vigente

### 7.1 Sello con clave embebida + PWA pública (N2 + V3) [D, ADR 0001 y 0002]

| Pieza | Diseño |
|---|---|
| **Token** | Contrato `ARB1` de §3.7 (ES256 sobre `ARB1.<payload-seg>`, `R‖S`, base64url sin relleno y canónico, JSON restringido UTF-8). |
| **Payload** | `v`, `kid`, `cid`, `nom`, `res="APTE"`, `exv`, `iat` (fecha local del dispositivo), `orig="autoservei"`; opcionales `bv`, `app`; reservados `exp`, `nbf`, `crit`, `alg`. Sin DNI, email ni foto (`01` §4.1). |
| **Clave privada** | Por `kid`; fuera de git (máquina del mantenedor o secret de CI según `05`); **incorporada al APK en el build** y **extraíble** (§3.3, §4). Sin ofuscación como medida de seguridad. |
| **Claves públicas** | `trusted-keys/` en el repo público con el esquema de §3.7.9 y copia en la PWA. No son secretas; su correspondencia con la privada se comprueba en la release. |
| **Documento** | PDF/imagen A4 y pantalla: texto grande en catalán + QR + "Emesa en autoservei" + aviso corto. |
| **Verificación** | PWA pública, sin secretos, instalable y offline tras cargar [PV]; uso optativo y posterior; se verifica en el dispositivo del organizador. |
| **Banco** | Fuente privada (B1 o B4), inyectado en el build, **sin cifrado/ofuscación** (ADR 0002); controles de §5.4; contenido nuevo o reescrito para salir de las 75 ya públicas (B7). |

- **Nivel conseguido**: resiste E, C y L. No resiste T (ni la difusión de una forja, T16). Se
  declara así (§4).
- **Complejidad**: baja. Un par de claves por `kid`, ECDSA de plataforma en Android, WebCrypto en
  la PWA, un analizador JSON restringido en cada lado, una batería de vectores compartidos y dos
  comprobaciones de release.

### 7.2 Gestión de claves

| Clave / secreto | Dónde vive | Rotación | Si se filtra |
|---|---|---|---|
| **Sello de credenciales** (privada ES256) por `kid` | Máquina del mantenedor / secret de CI (`05`); **y dentro del APK publicado** | `kid` nuevo por versión mayor de la app o ante sospecha de abuso (`05` §7.3) | Ya se asume extraíble. Al detectar abuso: `kid` nuevo en la siguiente versión; el antiguo puede marcarse `compromised` (§3.7.9); las credenciales del `kid` viejo **no se pueden separar** en legítimas/forjadas; política en fase 2. |
| **Claves públicas** | `trusted-keys/` y PWA | Se añaden con cada `kid`, antes que el APK que las usa | No aplica (públicas); lo relevante es su integridad (§3.7.9). |
| **Claves de prueba** (`kid=test`/`testx`/`dev`) | `test-vectors/` (públicas, incluida la privada de test) | — | Ninguna consecuencia: no están en la lista de producción. |
| **Token de lectura del repo del banco** (B1, si se usa) | Secret de Actions | Anual o al cambiar mantenedores | Lectura del banco fuente: revocar el token. |
| **Keystore de firma del APK** | Fuera del repo, con copia de seguridad (`05`) | Larga; APK Signature Scheme v3 permite rotación [PV] | Un tercero puede publicar actualizaciones aceptadas por Android. **La clave más crítica**; detalle en `05`. |

Regla: **ningún secreto en ningún repo**: ni la privada del sello de producción, ni el keystore,
ni tokens. *Secret scanning* y `.gitignore` para `*.jks`, `*.keystore`, `*.p12`, `*.pem`, `*.pk8`
y la carpeta local del banco.

### 7.3 Alternativa más simple: checksum sin clave [Histórico — no seleccionada]

Evaluada en revisiones anteriores como opción sin secretos (N1). ADR 0001 eligió la firma; no
forma parte del MVP ni se mantiene como modo alternativo.

### 7.4 Matriz: qué detecta cada opción

| Ataque | A sin marca | B checksum [Histórico] | **C firma + PWA (vigente)** | C + PWA + lista del club (fase 2) |
|---|---|---|---|---|
| Error / documento dañado (E) | Parcial | Sí | Sí | Sí |
| Editar texto visible (T1) | Solo con verificador | Sí* | Sí* | Sí* |
| Regenerar el QR con otros datos (T2) | **No** | Sí | Sí | Sí |
| QR de otro en mi documento (T3) | Solo con verificador | Sí* | Sí* | Sí* |
| Recalcular leyendo el repo (L) | No | **No** | Sí | Sí |
| Extraer clave y forjar (T9) | — | — | **No** | Parcial (la secretaría conoce a la gente) |
| Herramienta de forja difundida (T16) | — | No | **No** (hasta retirar el `kid`) | Parcial |
| App parcheada que siempre aprueba (T10) | No | No | **No** | Parcial |
| Captura/credencial de otro (T4) | No | No | No | Parcial |
| Otra persona hizo el examen (T13) | No | No | No | No |
| Pantalla "VÀLIDA" del candidato (T5) | — | Procedimiento | Procedimiento | Procedimiento |

\* Siempre que el organizador escanee con **su** PWA oficial y compare los datos mostrados con el
documento.

---

## 8. Fase 2 (separada, no forma parte del MVP)

### 8.1 Revocación sin backend

- **Lista de denegación** (`cid` y `kid` retirados, `exv` no aceptadas) incluida en la PWA y
  actualizada **publicando una versión nueva**.
- Variante: la misma lista como JSON **firmado** en Pages, con versión creciente anti-*rollback*;
  una PWA sin actualizar no conoce revocaciones nuevas.
- Limitación: con la clave embebida, revocar un `cid` solo sirve contra credenciales legítimas
  conocidas; no frena la forja, que generará `cid` nuevos.
- Si se añaden códigos de resultado (p. ej. `cid` denegado), se definen en este documento y se
  mapean a uno de los cuatro resultados de ADR 0001.

### 8.2 Registro del club

La "lista del club" de §6.5 formalizada: hoja privada o lista firmada distribuida solo a
organizadores. Es la medida de mayor valor frente a T9/T16 sin backend. Coste: proceso manual y
tratamiento de datos.

### 8.3 Caducidad y versiones aceptadas

Política de vigencia por fecha o por versión de reglamento; lista de `exv` aceptadas en la PWA
(§6.6). Una caducidad en el token exige `ARB2` (§3.7.5).

### 8.4 Emisión fuerte (solo si cambia el perfil de amenaza)

La única vía sin backend es sacar la clave del APK del candidato: **organizador que firma**,
**examen en un dispositivo del club** o **Key Attestation** del móvil del candidato. Esta última
solo aportaría que la clave vive en hardware que Google considera seguro y la información de
paquete y arranque que declare la plataforma (§3.6); no prueba la ejecución correcta del examen
ni protege el banco. Las dos primeras exigen un organizador y **rompen el autoservicio** [D]. El
`kid` permite añadir otro emisor con el mismo formato; si cambia el algoritmo o el significado,
con `ARB2`.

### 8.5 App Android de organizadores

Descartada para el MVP (ADR 0001). Solo se revisitaría si el soporte de la PWA en móviles, el
escaneo con cámara o una futura revocación lo justificaran; en ese caso usaría el mismo contrato
§3.7 y las mismas claves públicas.

---

## 9. Impacto en otros documentos

La lista detallada de pasajes superados está en §3.8.2 y la propiedad de cada tema en §3.8.1.
Resumen:

- **`03-arquitectura-movil.md`**: §6.2 pasa a remitir a `02` §3.7 (o se alinea con él); la emisión
  sigue §3.7.10; sin `app-verifier`, checksum ni ofuscación del banco (ADR 0001/0002).
- **`04-ux.md`**: textos por código de §3.7.8; sin línea de caducidad en `v=1`; código corto de
  `cid` hexadecimal; `kid` desconocido ⇒ `NO VÀLIDA` con indicación de actualizar; pegado
  conforme a §3.7.7 (ya lo es).
- **`05-distribucion.md`**: `trusted-keys/` con el esquema de §3.7.9 (`spki` en base64url);
  vectores de §3.7.11 en G1 (sin "campo crítico desconocido"); R01 en las puertas de release.
- **ADR**: 0001 y 0002 aprobados y sin cambios. El contrato §3.7 puede elevarse a ADR cuando pase
  los vectores en Android real y en la PWA [S].

---

## 10. Riesgos residuales (diseño vigente)

1. **Clave del sello extraíble** del APK público: un técnico puede forjar credenciales
   indistinguibles y difundir la capacidad (T9, T16). Aceptado [D]; mitigación social y, en fase
   2, retirada de `kid` + registro del club.
2. **Banco y respuestas extraíbles** del APK y aprendibles con el propio feedback [D].
3. **Las 75 preguntas actuales ya son públicas** con su respuesta en el repo base (§5.2).
4. **Sin verificación no hay detección**: si los organizadores no escanean, el sello es decorativo.
5. **Pantalla o verificador falsos** (T5, T15): mitigado solo por procedimiento y por usar la PWA
   oficial instalada.
6. **Fecha** de emisión dependiente del reloj del candidato (T11); sin caducidad en `v=1`.
7. **Fuga del fuente por CI** (logs, artifacts, cachés, workflows modificados): depende de los
   controles de §5.4 y `05`.
8. **Interoperabilidad no demostrada** entre JCA en Android y WebCrypto hasta pasar §3.7.11 en
   dispositivo y navegador reales.
9. **Deriva documental**: hasta que `03`, `04` y `05` se alineen (§3.8.2), un implementador que lea
   solo esos documentos implementaría el contrato antiguo; el vector S05 y G1 lo detectarían.
10. **PWA desactualizada u offline sin todos los recursos**: puede rechazar `kid` o versiones
    nuevas legítimas [PV].
11. **Riesgo de comunicación**: que alguien presente `VÀLIDA` como "certificado oficial seguro".
    Mitigado por el aviso de §4.
12. **[PV] pendientes**: tamaño y legibilidad del QR; salida DER del proveedor JCA de Android;
    analizador JSON restringido en ambos lados; WebCrypto en Node para CI y rechazo de `r`/`s`
    fuera de rango; representación de la clave en el APK; alcance de las cachés de Actions;
    rotación del keystore con APK Signature Scheme v3.

---

## Preguntas abiertas para el usuario

*(Solo residuales; ninguna bloquea el contrato §3.7. Ya están decididos, entre otros:
autoservicio, perfil de seguridad, sello con clave embebida, PWA pública sin app Android de
organizadores, banco sin ofuscación, suplantación aceptada y revocación en fase 2. Los detalles
técnicos que la revisión 3 dejaba a confirmar —`iat` como fecha `AAAA-MM-DD` y `kid` desconocido
⇒ `NO VÀLIDA`— quedan fijados en §3.7 como corrección técnica.)*

1. **Caducidad**: ¿sin caducidad (por defecto en `v=1`), por fecha o por versión de reglamento?
   Cualquier caducidad en el token exigirá `ARB2`.
2. **Repo base**: ¿se retira del repositorio público del proyecto base el script generador que
   contiene el banco, sabiendo que el historial persiste? ¿Hay plan editorial de preguntas nuevas
   o reescritas?
3. **Fase 2**: ¿interesa el registro del club (§6.5/§8.2) como práctica social, aunque sea en una
   hoja de cálculo?
