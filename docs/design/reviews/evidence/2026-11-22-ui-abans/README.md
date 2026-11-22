# Evidència visual ANTES (línia base) — app del candidat

Data: 2026-11-22. App debug `dev.lluissunol.arbitratge` amb `bank-sample` (ficticio), emulador API 36 `emulator-5554` (1080×2400, 420 dpi). Cap canvi de codi de l'app. Capturas con `android screen capture`; las observaciones salen de mirar cada PNG (no hay medidas más allá de lo visible).

Nombre: `NN-pantalla-estado-ESCALA-TEMA.png` (`ESCALA` = 100/130/200, `TEMA` = `clar`/`fosc`). Si la pantalla se desplaza, hay una segunda captura `NNb-…-final-…` con el final del scroll (solo existe si había contenido fuera de pantalla).

## Cobertura

| Conjunt | Estat |
|---|---|
| 100 % claro y oscuro (`--flow main`) | completo (todos los NN de la tabla) |
| 130 % claro | **parcial**: solo 01, 02, 06/06b, 10–15 (el script se cortó por timeout antes de 30/31/50/51/60) |
| 200 % claro | 02/02b, 10, 14, 30/30b, 31/31b, 50/50b |
| Temps esgotat / avís 1 min (33, 32, 34, 35) | **no capturado** (ver «No alcanzado») |

## Tabla fichero → pantalla/estado

Cada NN existe en `-100-clar` y `-100-fosc` salvo que se indique.

| NN | Nombre | Pantalla / estado |
|---|---|---|
| 01 (+01b) | benvinguda | Bienvenida tras `pm clear` (C1) |
| 02 (+02b) | inici-inicial | Inicio sin exámenes previos (C0) |
| 03 | nom-buit | «El teu nom», campo vacío |
| 04 | nom-error | «El teu nom» tras Continuar vacío (error) |
| 05 | nom-escrit-teclat | Nombre escrito, teclado abierto |
| 06 (+06b) | normes | «Abans de començar» con el nombre confirmado |
| 10 / 11 | pregunta-unica-buida / -marcada | Pregunta de una respuesta sin responder / con opción marcada |
| 12 / 13 | pregunta-multiple-buida / -marcada | Respuesta múltiple sin responder / con una marcada |
| 14 / 15 | pregunta-certfals-buida / -marcada | Cert o fals sin responder / con «Cert» marcado |
| 16 | pregunta-sortir-dialeg | Diálogo «Vols sortir de l'examen?» (tecla Atrás) |
| 17 | pregunta-temps-amagat | Temporizador oculto («Mostrar el temps») |
| 20 (+20b) | resum-parcial | «Preguntes de l'examen» con respuestas pendientes |
| 21 | finalitzar-dialeg-amb-buides | Diálogo «Vols avaluar l'examen?» con preguntas sin responder |
| 22 (+22b) | resum-complet | Resumen con las 5 respondidas |
| 23 | finalitzar-dialeg-complet | Diálogo avaluar con todo respondido |
| 30 (+30b) | resultat-aprovat | Resultado APROBADO (5 de 5) |
| 31 (+31b) | resultat-suspes | Resultado SUSPENDIDO (0 de 5, 1 sin respuesta + falladas) |
| 40 | repas-llista-fallades | Repaso, lista, filtro «Fallades» (examen suspendido) |
| 41 | repas-llista-totes | Repaso, lista, filtro «Totes» |
| 42 | repas-detall-fallada | Detalle de pregunta fallada con «Per què?» |
| 43 | repas-llista-aprovat-filtre-fallades | Repaso del examen aprobado con filtro «Fallades 0» (lista vacía) |
| 45 | repas-llista-totes-aprovat | Repaso del examen aprobado, «Totes» |
| 44 | repas-detall-encertada | Detalle de pregunta acertada |
| 50 (+50b) | credencial | «La teva credencial» (QR, campos) |
| 51 | credencial-presentacio | Modo «Mostrar a un organitzador» |
| 60 (+60b) | inici-amb-credencial | Inicio con credencial y último examen aprobado |
| 61 | inici-ultim-suspes | Inicio con último examen no aprobado (sin credencial) |

Las preguntas del examen se barajan (5 de 7 del banco de muestra): el contenido y orden de la pregunta en cada captura varía entre ejecuciones; el tipo sí está fijado por el nombre.

## Cómo reproducir

Requisitos: emulador `emulator-5554` en marcha, APK debug instalado y CLI `android` en `%USERPROFILE%\AppData\AndroidCLI\android.exe` (o variable `ANDROID_CLI`).

```bash
./gradlew :app-candidate:assembleDebug
adb install -r app-candidate/build/outputs/apk/debug/app-candidate-debug.apk

node scripts/qa/capture-ui.mjs --flow main --theme clar            # 100 %, claro  (~5 min)
node scripts/qa/capture-ui.mjs --flow main --theme fosc            # 100 %, oscuro
node scripts/qa/capture-ui.mjs --flow escala --scale 130 --theme clar   # 130 %: 01,02,06,10-15,30,31,50,51,60
node scripts/qa/capture-ui.mjs --flow escala --scale 200 --theme clar   # 200 %: 02,10,14,30,31,50
node scripts/qa/capture-ui.mjs --flow temps --theme clar|fosc      # avís 1 min (33), temps esgotat (32), resultat (34) ~6 min
node scripts/qa/capture-ui.mjs --flow fora  --theme clar           # 33 y temps esgotat mentre eres fora (35) ~6 min
```

Opciones: `--flow main|escala|temps|fora`, `--theme clar|fosc`, `--scale 100|130|200`, `--out <carpeta>` (por defecto esta carpeta; para la evidencia DESPUÉS usa otra carpeta con los mismos nombres). El script hace `pm clear`, fija `cmd uimode night` y `font_scale`, y al acabar los restaura a claro y 1.0. Archivos: `scripts/qa/capture-ui.mjs` (flujos) y `scripts/qa/lib.mjs` (adb/CLI, lectura de `android layout`, banco de muestra para saber qué opción es correcta).

Pasos de cada estado (lo que hace `--flow main`):
1. `pm clear` + lanzar → 01. Tap «Entesos» → 02. «Començar l'examen» → 03; «Continuar» vacío → 04; tocar campo, escribir «Maria Pujol Vila» → 05; «Continuar» → 06.
2. «Començar ara»; recorre las 5 preguntas con «Següent» para leer los tipos; si falta algún tipo (única/múltiple/cert-fals) se reinicia desde `pm clear`.
3. Examen 1 (SUSPENSO): Atrás en la pregunta 1 → 16 (y «Continuar l'examen»); en la pregunta 2 toca el cronómetro → 17; en la pregunta 3 «Veure-les totes» → 20, «Avaluar» → 21, «Tornar a l'examen» y «Anar a la primera sense resposta». En cada tipo, primera vez: 10/12/14 antes de responder y 11/13/15 tras responder (opción incorrecta; en múltiple, una sola correcta); la última pregunta se deja sin responder si su tipo ya estaba capturado. «Revisar» → «Avaluar» (resumen) → «Avaluar» (diálogo) → 31. «Repassar les 5 preguntes fallades» → 40; «Totes» → 41; primera fila → 42; Atrás ×2; «Tornar a l'inici» → 61.
4. Examen 2 (APROBADO): «Començar l'examen» (nombre se mantiene; si aparece «El teu nom», «Continuar»; «Començar ara»), responde todo bien (según `bank-sample.json`); «Revisar» → 22; «Avaluar» → 23; «Avaluar» → 30; «Veure la credencial» → 50; «Mostrar a un organitzador» → 51; «Tancar»; «Tornar a l'inici» → 60; «Repassar l'últim examen» → 43; «Totes» → 45; primera fila → 44.

## No alcanzado

- **33/32/34/35 (aviso de 1 min, «S'ha acabat el temps», resultado por tiempo, tiempo agotado estando fuera)**: el flujo está escrito (`--flow temps|fora`) pero no se ha ejecutado (cada uno exige esperar ~5 min reales; se agotó el tiempo de la tarea). Se requiere ejecutarlo para la línea base.
- Aviso de 5 min: al ser el examen de muestra de 5 min no aparece banner de 5 min (solo el contador «Et queden 5 minuts»).
- Credencial pendiente / «Creant la credencial…», «Corregint l'examen…», pantallas de error (`BuildError`, `RecoveryError`, `StateError`), error de guardado, bateria baixa, tarjeta de credencials pendents: requieren fallos inyectados, no alcanzables con la app debug normal.
- 130 %: faltan 30, 31, 50, 51, 60 (ejecutar `--flow escala --scale 130`).
- 200 %: 51 y 60 no capturados (el flujo falló en «Mostrar a un organitzador» a 200 %; no requeridos). No se capturó 200 % oscuro (fuera de alcance).
- TalkBack: no probado. Dispositivo físico: no usado.

## Observaciones objetivas (solo lo visible en las capturas; sin propuestas)

Generales
- Paleta: fondo verde grisáceo muy claro (claro) / casi negro verdoso (oscuro), cabecera de inicio verde oscuro con motivo de línea de campo, botón primario verde relleno, secundario con borde verde, enlaces en verde.
- Los textos de tamaño pequeño (etiquetas «Respostes:», «Pregunta N · estado», «Incorrecta · la vas marcar», «Nom a la credencial») son visiblemente más pequeños y de menor contraste (gris sobre fondo claro) que el cuerpo.
- Las preguntas llevan el prefijo «[FICTICI]» (datos de muestra); no es parte del diseño.

01 Bienvenida
- Zona entre la lista de 4 puntos y el botón «Entesos» vacía (~1/3 de la pantalla); el contenido queda arriba, el botón abajo.
- El título «Et donem la benvinguda» y los puntos están alineados a la izquierda, pero la cabecera verde tiene el contenido en otra sangría respecto a los iconos de la lista.

02 Inicio inicial
- Las tres tarjetas (5 preg. / 5 min / 3 encerts) usan etiquetas abreviadas; debajo, un texto gris suelto («Encara no has fet cap examen…») sin contenedor. Más de la mitad inferior de la pantalla está vacía hasta el botón.
- El motivo de línea de campo de la cabecera cruza el subtítulo «Examen d'àrbitre de Futbol amb Botons» (una línea curva pasa por «de Futbol»).

03–05 Nombre
- Mucho espacio vacío entre el aviso «No es podran canviar després.» y el botón. En 04 el borde y la etiqueta del campo pasan a rojo; el mensaje de error es de cuerpo pequeño y rojo sobre fondo claro. En 05 el teclado cubre el texto bajo el botón pero el botón queda visible.
- El aviso informativo es un contenedor verde claro con icono «i», estilo distinto al de los campos.

06 Normas
- Lista de 8 reglas con iconos distintos (cronómetro, interrogante, bandera, flechas, casilla, guion, check, salida); varias reglas ocupan dos líneas; el icono de «Si surts de l'app…» es amarillo, el único de ese color. Tarjeta «Nom a la credencial» al final con «Canviar».

10–15 Preguntas
- Cabecera: «Pregunta N de 5» a la izquierda, cronómetro en píldora a la derecha, barra de progreso verde fina, «Respostes: x de 5» y «Veure-les totes» (enlace verde) en la misma fila.
- Opciones: tarjetas blancas con borde fino gris y radio/casilla a la izquierda; la seleccionada pasa a fondo azul claro con borde azul oscuro (el azul no aparece en el resto de la paleta, que es verde). En múltiple, casillas cuadradas; el chip «Resposta múltiple» lleva una casilla marcada aunque no haya selección.
- Cert/Fals: dos tarjetas anchas con el radio a la izquierda; mucho espacio vacío (~40 %) entre la última opción y los botones inferiores.
- Botones «Anterior» (borde) y «Següent» (relleno) ocupan el mismo ancho; «Anterior» deshabilitado en pregunta 1 en gris muy pálido.
- Las opciones usan tamaño de letra menor que el enunciado; el enunciado en negrita.

16–17 Diálogos/tiempo
- El diálogo de salida es una tarjeta blanca con icono, título, texto y dos botones (primario «Continuar l'examen», secundario «Sortir»); el fondo se oscurece. En 17 la píldora del tiempo cambia a «Mostrar el temps» con icono tachado y ocupa más ancho, desplazando el título.
- En el resumen (20) y su diálogo, el título «Preguntes de l'examen» y la píldora «4 min» quedan pegados (casi sin separación).

20–23 Resumen
- Lista de tarjetas con círculo (relleno azul = contestada, vacío = sin respuesta); la fila actual lleva borde negro grueso y «Ets aquí». Los enunciados se truncan con «…» en 2–3 líneas (p. ej. «pesa més que una pedra de mo…»). Dos botones fijos abajo sobre la lista, con línea separadora; con 5 preguntas la última fila queda cortada tras los botones en el resumen parcial (hay 20b para el final).
- El círculo «contestada» es azul, otra vez distinto del verde de la marca.

30–31 Resultado
- Anillo con la nota (5 / 0) y «de 5»; el título del suspenso ocupa dos líneas («No has aprovat, de moment»). Tarjetas «Fallades» y «Temps» a mitad de ancho cada una; en suspenso, «5 / 1 sense resposta» añade una tercera línea solo en una de las tarjetas. Los botones inferiores («Veure la credencial» o «Repassar…», «Tornar-ho a provar», «Tornar a l'inici») quedan pegados abajo con un hueco grande por encima en 30 (la pantalla de aprobado tiene ~1/3 vacío).
- El anillo del suspendido es gris con un pequeño arco verde en la parte inferior izquierda; el texto «0» queda en negrita grande.
- «Tornar-ho a provar» en el resultado es un botón de borde, «Tornar a l'inici» es un texto verde sin borde: tres jerarquías de botón en una pantalla.

40–45 Repaso
- Selector segmentado «Fallades N / Totes N» con check en el activo. Filas con icono circular (✕ rojo, – gris) y texto de estado pequeño. Enunciados de hasta 3 líneas sin truncar.
- Detalle (42/44): los colores de opciones son rojo claro (incorrecta marcada), amarillo con borde discontinuo (correcta no marcada), verde (correcta marcada), blanco (incorrecta no marcada). La correcta no marcada en amarillo difiere de la correcta marcada en verde. En oscuro, el amarillo es marrón oliva y el rojo granate saturado, con texto blanco de bajo tamaño.
- En 43 (filtro «Fallades 0») la lista queda vacía aunque la pestaña activa es la de fallades.
- Bloque «Per què?» en gris claro, sin diferenciarse mucho del fondo; el botón «Següent» ocupa el ancho completo y no tiene variante «anterior» visible.

50–51 Credencial
- Cabecera verde con logotipo y «Credencial d'aptitud / Àrbitre de Futbol amb Botons · CFBB»; el nombre grande; campos en dos columnas (Resultat / Examen, Data del dispositiu / Codi) con «reglament sample-1» envuelto en dos líneas; QR grande. Contenido más largo que la pantalla (50b muestra el aviso «Emesa en autoservei…» al final).
- Botón primario «Mostrar a un organitzador» y enlace «Tornar a l'inici» fijos abajo. 51 es una pantalla blanca (también en tema oscuro, según 51-fosc) con el QR sobre fondo blanco, textos centrados negros, botón «Tancar» con borde negro (no verde) — es el único botón con borde negro.

60–61 Inicio con historial
- Con credencial: tarjeta de credencial (mini cabecera verde, nombre, «Tens la credencial d'aptitud.») y tarjeta «Últim examen…» con enlace verde sin icono; los dos botones inferiores (primario, secundario) más un hueco vacío en medio.
- Sin credencial (61): la tarjeta «Últim examen: 0 de 5 encerts (no aprovat)» + enlace, y el resto de la pantalla vacía hasta «Començar l'examen».

Tema oscuro
- Mismos elementos; los botones primarios son verde menta claro con texto oscuro; las tarjetas pierden el fondo blanco (gris verdoso oscuro, borde fino poco contrastado). El texto secundario (gris) sobre fondo oscuro es de bajo contraste visible (p. ej. «Respostes: 1 de 5», «Incorrecta · no la vas marcar»). El círculo «contestada» pasa a azul claro.

130 % (claro, parcial)
- 01, 02, 06 y 10–15 se mantienen en una pantalla con textos algo mayores; ver los ficheros `…-130-clar.png` y `06b` para el final del scroll de normas.

200 % (claro)
- 02: cabecera del tema en dos líneas («Examen d'àrbitre de / Futbol amb Botons»); las tres tarjetas de datos pasan a apilarse en vertical y la tercera queda cortada por el botón inferior fijo (se ve «3» sin etiqueta); el resto se ve con scroll (02b).
- 10 / 14: la píldora del cronómetro pasa a una línea propia bajo «Pregunta N de 5»; «Respostes: 2 de 5» se parte en dos líneas junto a «Veure-les totes»; el enunciado y las opciones ocupan casi toda la altura y la 2ª opción de 10 queda parcialmente tapada por la barra de botones inferior fijos (Següent/Anterior apilados, ahora uno encima del otro).
- 30 / 31: las tarjetas «Fallades» y «Temps» se apilan a ancho completo; el aviso «La teva credencial ja està preparada.» queda parcialmente tapado por la barra de botones inferior en 30; en 31 la tarjeta «Fallades» queda bajo otra tarjeta cortada. El botón «Repassar les 5 preguntes fallades» ocupa dos líneas.
- 50: la cabecera de la credencial ocupa 4 líneas («Credencial / d'aptitud / Àrbitre de Futbol amb / Botons · CFBB») con el logotipo a la izquierda; «Mostrar a un organitzador» parte en dos líneas; el campo «Codi» queda tapado por la barra de botones fija en la primera vista.
- En todas las pantallas de 200 % la barra inferior de botones fija ocupa ~25–30 % de la altura y reduce el área desplazable.

Nota de verificación: las afirmaciones de disposición se basan en las capturas del emulador adjuntas; TalkBack y dispositivo físico no verificados.
