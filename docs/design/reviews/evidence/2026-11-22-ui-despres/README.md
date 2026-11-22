# Evidencia DESPUÉS — revisión look&feel de la app del candidato (2026-11-22)

Capturas del emulador `emulator-5554` (1080×2400, `bank-sample` ficticio) con el APK debug de la fase 3, generadas con
`scripts/qa/capture-ui.mjs` y **los mismos nombres** que `../2026-11-22-ui-abans/` (más los sufijos `b-…-final` cuando la
pantalla hace scroll, y `-fosc`/`-200-…` nuevos).

| Flujo | Comando | Resultado |
|---|---|---|
| main claro y oscuro (100 %) | `--flow main --theme clar\|fosc` | `*-100-clar.png`, `*-100-fosc.png` |
| escala 130 % claro | `--flow escala --scale 130 --theme clar` | `*-130-clar.png` |
| escala 200 % claro | `--flow escala --scale 200 --theme clar` (incluye 11 y 60) | `*-200-clar.png` |
| escala 200 % oscuro | `--flow escala --scale 200 --theme fosc` | `*-200-fosc.png` (02, 11, 30, 50 obligatorias; el flujo guarda también 01, 06, 10, 14, 31, 60) |
| temps claro | `--flow temps --theme clar` | `33`, `32`, `34` (ver la nota de «temps» abajo) |
| fora claro | `--flow fora --theme clar` | `35` (el script mata el proceso en segundo plano con `am kill` para que la recuperación marque `away = true`) |

Diferencias de ficheros respecto a ANTES: ya no existe `02b-…-200-clar` porque el inicio cabe en una pantalla a 200 %
(sin barra fija ni scroll); `31-…-130-clar` y `30-…-130-clar` se añaden; `20b`/`40b`/`41b`… son las mitades inferiores.

## Comparación ANTES → DESPUÉS por pantalla

Leyenda: **Visto** = abierta y revisada por el implementador; **NV** = «no verificado visualmente» (existe la captura pero
no se ha inspeccionado, o el aspecto no se puede afirmar).

| Pantalla | ANTES | DESPUÉS (fase 3) | Estado |
|---|---|---|---|
| 30 Resultado aprobado | Título «Resultat», anillo de progreso, 2 teselas, banner «credencial preparada» | Bloque de gespa a sangre: «Has aprovat», «5 de 5 encerts», 5 discos blancos y la línea del mínimo tras el 3.º; `FactStrip` Incorrectes/Temps (sin «Sense resposta» si 0); `CredentialMini` sobre «Veure la credencial»; «Tornar a l'inici» texto a todo el ancho 56 dp | Visto 100 % y 200 % claro (30b rehecha tras la corrección de la barra de estado); oscuro NV |
| 31 Resultado suspendido | Título en 2 líneas, anillo, «Et van faltar…», banner | Bloque `surfaceContainer` sin rojo, «No has aprovat» en 1 línea, 5 aros y línea tras el 3.º, `FactStrip` Incorrectes/Sense resposta/Temps, consejo como texto, 3 botones a ancho completo | Visto 100 % claro, 100 % fosc y 200 % fosc |
| 40/41/43/45 Listas de repaso | Icono en cercle, radio 20 | Icono ✕/✓/– de 28 dp sin cercle, radio 12 | Visto 40 claro; resto NV |
| 42 Detalle de fallada | Banner verde «La teva resposta no era la correcta», correcta no marcada en ámbar | Sin banner (respuesta única), indicador de tinta a la izquierda + icono relleno al final, correcta no marcada con borde discontinuo verde, incorrecta en rojo, sin ámbar | Visto 42 claro (el 42 oscuro NV) |
| 44 Detalle de acierto | Verde primario | `correctContainer` + disco de tinta + ✓; «Per què?» con filete `primary` de 4 dp | Visto 44 oscuro |
| 50 Credencial | Cabecera con línies pitch, 2 columnas, QR al final | Documento I8: banda con filete amarillo, nombre → «APTE» → QR → detalles (una columna) → aviso | Visto 100 % y 200 % claro (símbolo encima a 200 %); oscuro NV |
| 50 «Mostrar a un organitzador» a 200 % | Icono y texto desalineados | 2 líneas con el texto junto al icono | Visto `50b-…-200-clar` |
| 51 Presentación | Borde negro en «Tancar», textos `#3D3D3D` | Banda y filete a sangre, «Tancar» con borde gespa, textos `#3F4B44` | Visto (clar 100 %); capturas 51 rehechas tras quitar las líneas de campo bajo la barra de estado: la banda queda limpia bajo la hora (visto 100 % claro; 100 % fosc y 130 % claro rehechas) |
| 60/61 Inicio con historial | — | `CredentialMini` con filete amarillo de 3 dp y símbolo de 28 dp | Visto 60 claro 100 %; el resto NV |
| 32/33/34 Tiempo | No existían | Aviso de 1 min como `InfoBanner` de aviso con «Tancar l'avís»; pantalla de tiempo agotado con un único CTA | Ver sección «Tiempo» |

### Corrección de la barra de estado a 200 % (fase 3b)

`ScreenScaffold` con `header` dibuja ahora un **fondo fijo** de la altura de `WindowInsets.statusBars` por encima del scroll:
color de la banda (gespa / `surfaceContainer`) mientras la banda sigue tapando la barra, color de pantalla cuando ya ha salido,
y los iconos de la barra se adaptan a ese mismo estado. Visto: `30b`/`31b`/`60b` a 200 % claro y fosc ya **no** dibujan texto
bajo la hora (el contenido queda recortado limpiamente por el fondo opaco, como bajo una barra de app). Test de lógica:
`ComponentsTest.statusCapKeepsTheBandColourOnlyWhileTheBandCoversTheStatusBar` (no mide píxeles).

### Textos del resultado (ux-designer, opción A)

`FactStrip`: «Incorrecta/Incorrectes» + «Sense resposta» + «Temps»; el botón de repaso pasa a plural (`btn_review_n`); filtre
«Fallades N» sin cambios. En `34` (tiempo agotado) el bloque ahora dice «0 Incorrectes · 5 Sense resposta» junto a
«Repassar les 5 preguntes fallades»: ya no se contradicen. Marcador `31` a 100 % claro: «0 de 5 encerts».
`FactStrip` en modo filas: columna de valores de ancho común (visto `30b`, `31b`, `60b` a 200 %: «Temps» alineado).

### Lo que sigue sin verificarse visualmente

- **TalkBack** y orden de lectura: **[PV]** en dispositivo físico (semántica nueva: `liveRegion` en veredicto y bloque de credencial, FactStrip fusionado «N incorrectes, M sense resposta. Temps: …»).
- Contraste medido con herramienta: valores de la especificación, no medidos en pantalla.
- Animación de los discos y reducción de animaciones: no comprobada en dispositivo.
- Capturas no abiertas una a una: la mayoría de `*-fosc` a 100 %, `41`, `43`, `45`, `61`, `130 %` (salvo 51), `10`/`14` a 200 %.
- Huecos verticales en 30/31/32 a 100 %: aceptados (la especificación no fija un límite).

## Tiempo (`--flow temps`, claro, 100 %) y «fora»

- `33-pregunta-avis-1-minut-100-clar.png` — **Visto.** Aviso de 1 min como `InfoBanner` de aviso con «Tancar l'avís».
- `32-temps-esgotat-100-clar.png` — título, icono, «Has respost 0 de 5 preguntes.» y un único botón «Veure el resultat».
- `34-resultat-temps-esgotat-100-clar.png` — **Visto (rehecha).** Resultado suspendido: 0 Incorrectes, 5 Sense resposta, 5:00.
- `35-temps-esgotat-fora-100-clar.png` — **Visto.** Aviso ámbar con ⚠ «El temps ja s'havia acabat mentre eres fora.» sobre el texto de resumen y un único CTA.
- `51-credencial-presentacio-130-clar.png` — **Visto.** Banda limpia bajo la hora; a 130 % el último párrafo («Per comprovar-la…») queda parcialmente tapado por el botón «Tancar» fijo hasta hacer scroll (el contenido es desplazable; no es un truncado, pero conviene comprobarlo en dispositivo).
