# ADR 0001 — Credencial sellada y verificador público

- **Estado:** aprobado
- **Fecha:** 2026-11-20

## Contexto

La app de examen es autoservicio, sin backend y corrige en el móvil del candidato. Se
acepta un perfil de seguridad proporcional: no se intenta impedir que un candidato técnico
extraiga o modifique el APK. Sí se quiere evitar edición visual casual, facilitar la
comprobación por el organizador y no exigir personal durante el examen.

En esta fase no se usa TOTP, Play Integrity, Key Attestation ni una clave de emisión
custodiada únicamente por organizadores. Una revocación sincronizada queda para fase 2.

## Opciones

1. PDF o texto con QR sin sello: legible, pero se puede regenerar sin limitación.
2. Checksum sin clave: detecta cambios accidentales o casuales mientras no se recalcule.
3. QR/PDF con firma digital cuya clave privada está dentro del APK.
4. Corrección o firma por un organizador: descartada por su coste operativo.

## Decisión

La app del candidato emitirá una credencial legible como PDF/imagen con un **QR sellado
mediante firma digital asimétrica**, usando una clave incorporada durante el build. El
usuario/organizador podrá comprobarla con una herramienta independiente que solo contiene
la clave pública correspondiente.

El verificador será inicialmente una **PWA estática y pública**, alojada en GitHub Pages o
en el mismo hosting estático:

- una página móvil sencilla para abrir/importar el QR o introducir su contenido;
- manifiesto de instalación y service worker para permitir instalarla como acceso directo
  y usarla offline una vez cargados sus recursos;
- comparación explícita entre los datos del QR y los datos visibles de la credencial;
- resultado claro: `VÀLIDA`, `NO VÀLIDA`, `VERSIÓ DESCONEGUDA` o `FORMAT INCORRECTE`;
- sin backend y sin secretos.

Una app Android de organizadores queda descartada para el MVP, al no aportar valor suficiente
frente a la PWA. Podrá revisitarse si el soporte móvil, el escaneo de cámara o los requisitos
futuros de revocación lo justifican.

## Consecuencias

### Positivas

- El examen no necesita organizador ni conexión.
- La verificación es accesible desde un navegador y puede quedar instalada como icono.
- La PWA solo necesita la clave pública, por lo que ser pública no compromete por sí sola
  la verificación.
- El PDF sigue siendo legible aunque falle la comprobación automática.

### Limitaciones aceptadas

- La clave privada incorporada al APK **es extraíble por un atacante técnico**. Con ella se
  pueden fabricar tokens válidos indistinguibles de los emitidos por la app.
- La firma no demuestra que la persona realizó correctamente el examen ni que la app no fue
  modificada.
- Sin verificación independiente, una edición visual de la credencial no se detecta.
- Un verificador falso o una pantalla falsificada pueden engañar si el organizador no usa la
  dirección/PWA oficial.
- No hay revocación ni unicidad global en el MVP. Una fecha de emisión no es revocación ni
  prueba fiable de tiempo.

### Operación

- Mantener versionado el formato y las claves públicas aceptadas.
- Conservar de forma segura las claves privadas solo para generar el sello durante el build
  y la clave de firma Android, que es distinta.
- Diseñar el texto de privacidad y un aviso corto: «Aquesta marca ajuda a detectar canvis
  casuals; no és una prova contra un dispositiu modificat o una clau extraïda».
- Antes de hacer pública la PWA, probar instalación/offline en Android, Chrome/Edge, cámara,
  importación de imagen y pegado de texto.

## Enmienda 2026-11-26 (aprobada por el usuario)

El aviso anti-suplantación por dirección (pantalla «Abans de començar», tarjeta «Adreça que estàs fent servir», aviso de la
pantalla de inicio y línea en cada resultado) se **retira** de la PWA verificadora: se considera desproporcionado para un club
y ruido para quien verifica. Se mantiene el riesgo de suplantación como aceptado (riesgo social) y «Sobre els límits» sigue
explicando que «vàlida» no significa «autèntica». Solo queda, discreta, la dirección y versión en el pie de la pantalla de inicio.
Ver `docs/design/decisiones-usuario-ui.md` (UI-07).

## Enmienda 2026-11-26 (b): resultado del examen en la credencial (aprobada por el usuario)

El token `ARB1` (`v = 1`) admite tres campos **opcionales** firmados con el resto del payload: `ok`
(aciertos, 0–99), `tot` (preguntas del examen, 1–99; junto con `ok` o ninguno, `ok ≤ tot`) y `dur`
(duración en segundos, 0–86400). Es una ampliación compatible: no cambia la gramática, la firma, los
límites ni los códigos de los vectores existentes; los tokens sin ellos siguen siendo válidos y un
verificador anterior los ignora como miembros desconocidos (no los muestra). No se crea `ARB2`.
`dur` procede del reloj del dispositivo y es informativa, como la fecha de emisión. Contrato y
vectores: `docs/design/02-credencial-y-amenazas.md` §3.7.0, §3.7.5, §3.7.6 y §3.7.11.

## Enmienda 2026-11-28: resultado obligatorio e intentos suspendidos (aprobada por el usuario)

`ok`, `tot`, `dur`, `bv` (versión del banco de preguntas) y `app` (`versionName` de la app) pasan a ser **obligatorios** y se añade el campo obligatorio `fail` (entero
0–99): número de exámenes suspendidos antes de este. Un examen cuenta como suspendido cuando el
candidato llegó a obtener nota y a ver errores y soluciones (examen terminado, también por tiempo);
no cuentan los abandonados ni la práctica. El contador sale de la base de datos local del móvil, así
que reinstalar la app lo reinicia: es informativo, como `dur` e `iat`. `app` baja de 32 a 24
caracteres para que el payload máximo siga ≤ 512 bytes. Sigue siendo `ARB1`/`v = 1`: no existe
ninguna credencial emitida a terceros, así que la incompatibilidad con tokens anteriores no tiene
efecto real. La PWA muestra ahora también `fail` («Intents fallits») como una fila más del carnet; la revisión de diseño de esa fila queda pendiente.
