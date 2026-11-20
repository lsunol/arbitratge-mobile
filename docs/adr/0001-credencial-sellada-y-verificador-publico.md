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
