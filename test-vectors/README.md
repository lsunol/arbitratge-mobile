# Vectores compartidos ARB1

Vectores **públicos y ficticios** del contrato `ARB1` (`docs/design/02-credencial-y-amenazas.md`
§3.7, canónico; casos de §3.7.11). Los usan el verificador Kotlin de `core-credential` (JVM y,
cuando exista, dispositivo/emulador API 26+) y la PWA verificadora (WebCrypto en Node y en
navegador). Puertas G1/G5 de `docs/design/05-distribucion.md` §5.8.

Ningún contenido del banco de preguntas; nombres inventados; ninguna clave de producción.

## Ficheros

| Fichero | Contenido |
|---|---|
| `cases.json` | Casos: entrada exacta y código/resultado esperado |
| `trusted-keys-test.json` | Lista de confianza **de prueba**: `kid=test` (`active`) y `kid=testx` (`compromised`) |
| `test-keys.json` | Claves de prueba `test`, `testx` y `other` (S06) con **privada PKCS#8**. Públicas por diseño (`02` §3.7.9, `05` §5.5). **Nunca** en `trusted-keys/` ni en un build de release |

Todos los ficheros son JSON UTF-8 con solo caracteres ASCII: todo lo que no es ASCII imprimible
va como escape `\uXXXX` (pares sustitutos para fuera del BMP). Un `JSON.parse` normal sirve para
**leer estos ficheros** (no para el payload de un token: véase más abajo).

### `cases.json`

```json
{
  "format": "arb1-test-vectors/2",
  "trustedKeys": "trusted-keys-test.json",
  "cases": [
    {
      "id": "V01",            // único y estable: "G" o "G-variante" (p. ej. "F04-lowercase", "S04-r-zero")
      "group": "V01",         // id de §3.7.11 (o X01… para extras); = id antes del primer "-"
      "description": "…",     // en catalán, informativa
      "input": "ARB1.…",      // texto EXACTO que se pasa al verificador (antes de normalizar)
      "code": "OK",           // código exacto de §3.7.8
      "result": "VÀLIDA",     // resultado de ADR 0001 que corresponde al código
      "fields": { … },        // solo si code == "OK": campos decodificados literales
      "shortCode": "9071-7B65" // solo si code == "OK": primeros 8 hex de cid, §3.7.5
    }
  ]
}
```

- `fields` contiene `v` (entero), `kid`, `cid`, `nom`, `res`, `exv`, `iat`, `orig` y, si están en el
  token, `ok`, `tot`, `dur` (enteros) y `bv`, `app`. Los miembros desconocidos se ignoran y **no** aparecen. `nom` es el texto
  tras resolver escapes y **sin** normalizar (V11 está en NFD a propósito).
- Criterio (G1/G5): igualdad **exacta** de `code` en cada caso; para `OK`, igualdad literal de
  `fields`. Cero casos sin ejecutar.
- Grupos: todos los públicos de §3.7.11 (V01–V14, F01–F09, U01, P01–P09, K01, K03, S01–S06,
  C01–C09). V12–V14, C09, `P04-ok-decimal`, `P04-dur-negative` y X12–X14 cubren los campos
  opcionales de resultado `ok`/`tot`/`dur` (ampliación 2026-11-26, `02` §3.7.0); el generador los
  añade **al final** para que los 178 casos anteriores no cambien ni un byte. **R01 y K02 no están**: son exclusivos de release (G3) con la lista de producción.
- Cada variante enumerada en §3.7.11 tiene su id estable (p. ej. `C01-missing-iat`, `C08-alg`,
  `F06-plus`, `P07-kid-dash`, `S04-r-n`). `TestVectorsTest` contiene la lista completa de ids y
  códigos de forma independiente del generador: quitar, renombrar o cambiar de código un caso
  hace fallar las pruebas. Una implementación JS debería comprobar lo mismo (todos los ids
  ejecutados y ninguno ausente).
- Grupo `X01…X14`: extras del mismo contrato (límites exactos y precedencias: entrada de 2048,
  payload de 512 bytes, 16 miembros, `ARB1000.`, `ARB10.`, escapes en nombres de miembro, límites
  de `ok`/`tot`/`dur`, payload con `nom` de 80 caracteres de 3 bytes y todos los opcionales al
  máximo…). No
  añaden reglas; deben pasar igual.
- Los casos V01, V02, V04, V05, V06, V08, V12, V13, V14 y X14 los produce el **emisor de producción**
  (`CredentialIssuer`) con `kid=test` en la JVM; las peticiones de V01–V05 y V12 salen de
  `Arb1ChainCases` (fuente única compartida con G5). El V03 fijo se construye con la clave de
  prueba, pero su payload es **idéntico byte a byte** al que produce el emisor instrumentado
  `CredentialIssuer.withNonAsciiEscapes` con el mismo `cid` (lo comprueba `Arb1ChainCasesTest`).
  V11 (NFD) no lo puede producir el emisor (aplica NFC) y se construye con la clave de prueba.
  Los demás se construyen para forzar cada caso.
- Todos los casos de payload/clave/campos llevan una firma **correcta** con la clave de prueba
  (salvo donde el caso es la propia firma), de modo que un verificador que se salte un paso o lo
  ordene mal devuelve otro código.

### `trusted-keys-test.json`

Un **array** de entradas con exactamente el esquema de `02` §3.7.9:
`kid`, `alg` (`"ES256"`), `spki` (DER SubjectPublicKeyInfo P-256 sin comprimir en base64url sin
relleno, 91 bytes), `sha256_spki` (64 hex minúsculas), `status`, `valid_from` (informativo).
En producción cada entrada es un fichero `trusted-keys/<kid>.json` y además `kid` debe coincidir
con el nombre del fichero; aquí, al ser un solo fichero, esa comprobación no aplica.

Validación al cargar (entrada incoherente ⇒ se descarta): `kid` `^[a-z0-9]{1,16}$`,
`alg == "ES256"`, `spki` base64url canónico, `SHA-256(spki) == sha256_spki`, `status` ∈
{`active`, `retired`, `compromised`}, importación P-256 correcta. Un `kid` repetido se descarta
entero (no se elige ninguno).

### `test-keys.json`

`{ "keys": [ { "name", "kid"?, "status"?, "pkcs8", "spki" } ] }`, todo en base64url sin relleno.
`other` no tiene `kid`: es una clave P-256 que no está en ninguna lista (S06).
WebCrypto: `importKey("pkcs8", der, {name:"ECDSA", namedCurve:"P-256"}, false, ["sign"])`. El
PKCS#8 lo generó la JCA (sin clave pública embebida); Node/Chromium lo importan. La PWA **no**
necesita estas privadas para G1 (solo `spki`).

## G5: cadena Android firma → PWA verifica (V01–V05 y V12)

`05` §5.8 G5. No se guarda aquí: se genera en cada ejecución, en `$RUNNER_TEMP` o equivalente.

1. Un APK **de test** (nunca `release`) con `core-credential` y la clave `test` de
   `test-keys.json` (`JcaEs256Sealer(pkcs8)`) en emulador/dispositivo **API 26+**.
2. La prueba instrumentada, con `@OptIn(Arb1TestInstrumentation::class)`, recorre
   `Arb1ChainCases.cases` (V01–V05 y V12) y llama a `case.issue(sealer, "test", trustedTest) { cid }`:
   V01, V02, V04, V05 y V12 usan el constructor de producción de `CredentialIssuer`; V03 usa
   `CredentialIssuer.withNonAsciiEscapes` (mismo emisor, misma validación, sello y
   autoverificación; solo cambia la serialización a escapes `\uXXXX`). Cada emisión debe dar
   `Issued` (autoverificación `OK`).
3. Verificadores: (a) `CredentialVerifier` en el propio dispositivo; (b) la PWA de test en Node
   (WebCrypto); (c) la PWA en navegador headless [PV]. Todos con `trusted-keys-test.json`.
4. Criterio: los tres devuelven `OK` y los campos de `case.expected("test", cid)`. Como ECDSA no es
   determinista, **no** se compara el texto del token. Los logs muestran solo ids y códigos.

`Arb1TestInstrumentation` es un `@RequiresOptIn(level = ERROR)`: usar `withNonAsciiEscapes` o
`Arb1ChainCases` sin `@OptIn` explícito no compila, de modo que el código normal de la app no
puede llegar a ellos por descuido. La revisión de código/CI debe rechazar cualquier `@OptIn` de
esa anotación fuera de pruebas.

## Notas para una implementación JS/WebCrypto (paridad con Kotlin)

Orden y códigos: `02` §3.7.8; el primer fallo decide. Puntos donde es fácil divergir:

1. **Normalización (§3.7.7)**: si `input.length > 2048` ⇒ `BAD_FORMAT`; quitar U+0009, U+000A,
   U+000D, U+0020, U+00A0, U+200B, U+FEFF en cualquier posición; nada más; vacío ⇒ `BAD_FORMAT`.
2. **`IS_URL`**: prefijo `http://`/`https://` con plegado de mayúsculas **solo ASCII** (en JS,
   `/^https?:\/\//i` sin flag `u`, o comparar tras pasar A–Z a minúsculas). No usar plegado
   Unicode (`ſ`, `K` Kelvin).
3. **Prefijo**: `^ARB[1-9][0-9]{0,2}\.`; versión ≠ 1 ⇒ `UNKNOWN_VERSION` **sin mirar el resto**
   (`ARB999.` es `UNKNOWN_VERSION`; `ARB1000.` es `BAD_FORMAT`).
4. **Envoltorio**: token ≤ 1024; exactamente 3 segmentos separados por `.`; payload-seg de 2–683
   caracteres y firma de 86; base64url **canónico** comprobando que `encode(decode(seg)) === seg`
   (longitud ≡ 1 mod 4 inválida); payload 1–512 bytes; firma 64 bytes. `Buffer.from(…,
   "base64url")`/`atob` **no** rechazan lo no canónico.
5. **UTF-8**: `new TextDecoder("utf-8", {fatal: true, ignoreBOM: true})`; si lanza o el primer
   carácter es U+FEFF ⇒ `BAD_PAYLOAD`.
6. **JSON**: analizador propio (no `JSON.parse`, que acepta duplicados y más tipos): un objeto;
   espacios RFC 8259 (solo espacio, tab, LF, CR); valores texto o entero `0|[1-9][0-9]{0,8}`;
   controles < U+0020 sin escapar ⇒ rechazo; `\uXXXX` con pares válidos, sustituto aislado ⇒
   rechazo; duplicados comparados tras resolver escapes; ≤ 16 miembros. Luego `v` entero == 1 y
   `kid` texto `^[a-z0-9]{1,16}$` ⇒ si no, `BAD_PAYLOAD`.
7. **Clave**: `kid` ausente de la lista ⇒ `UNKNOWN_KID`; `compromised` ⇒ `KID_NOT_ACCEPTED`
   (antes de mirar la firma: caso X09).
8. **Firma**: `crypto.subtle.verify({name:"ECDSA", hash:"SHA-256"}, key, firma64,
   ascii("ARB1." + payloadSeg))`. Sin pre-hash; `s` alto se acepta (V09). `r`/`s` fuera de
   `[1, n−1]` ⇒ `BAD_SIGNATURE` (S04; el verificador Kotlin lo comprueba explícitamente antes de
   llamar a la JCA).
9. **Campos (tras la firma)**: patrones de §3.7.5 como coincidencia de **cadena completa**
   (`^…$` en JS; ojo con `m`); `iat` fecha gregoriana con años 0000–9999 y bisiesto
   `(y%4==0 && y%100!=0) || y%400==0`; `nom` según §3.7.5.1 contando **puntos de código**
   (`[...nom].length`) y el conjunto F fijo; nombres reservados `exp`, `nbf`, `crit`, `alg` ⇒
   `BAD_FIELDS`. El verificador **no** comprueba NFC. `ok`/`tot`/`dur`: `Number.isInteger` y
   rango (0–99, 1–99, 0–86400); `ok` y `tot` juntos o ninguno, `ok <= tot`; un texto ⇒
   `BAD_FIELDS` (decimales y negativos ya son `BAD_PAYLOAD` en el paso 6).

## Regenerar

```bash
./gradlew :core-credential:generateTestVectors
```

- Lee las claves fijas de `test-keys.json` (no se generan claves al vuelo) y reescribe
  `trusted-keys-test.json` y `cases.json`.
- La aleatoriedad de ECDSA sale de un `SHA1PRNG` con semilla fija: con el mismo JDK, regenerar
  **no cambia ningún byte** (`git diff` vacío). Otro JDK puede producir firmas distintas, igual de
  válidas; si cambian, se revisan y se commitean junto con el cambio que lo motivó.
- Antes de escribir, el generador verifica cada caso con el verificador Kotlin y falla si algún
  código o campo no coincide. Solo imprime recuentos.
- Crear claves nuevas (solo una vez en la vida del proyecto, borrando antes `test-keys.json`):
  `./gradlew :core-credential:generateTestVectors -PinitKeys`. Cambiarlas invalida todos los
  vectores y cualquier copia en la PWA.
- Cualquier cambio de casos que altere códigos exige cambiar antes `02` §3.7 (§3.7.0).

Las pruebas JVM (`./gradlew :core-credential:test`) ejecutan todos los casos (`TestVectorsTest`).
