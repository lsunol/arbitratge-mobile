# Revisión visual — app del candidato, hito 1

> Autor: `ui-designer` · Alcance: diseño visual de `app-candidate` (hito 1) · Estado: **propuesta**
> (no implementada). No cambia ADR, ARB1 ni el flujo de producto; lo que lo rozaría está en §8
> como pregunta.
>
> Fuentes: capturas reales `C:\msys64\tmp\shots\*.png` (1080×2400, ~411 dp de ancho); código
> `app-candidate/src/main/kotlin/dev/lluissunol/arbitratge/ui/*.kt`, `res/values*/`,
> `AndroidManifest.xml`; `docs/design/04-ux.md` §2, §3 y §8; marca base
> `arbitratge/assets/style.css` + `index.html` (no se ha leído el banco).

---

## 0. Diagnóstico en una frase

La app es **correcta en estructura y accesibilidad**, pero su aspecto es el de una plantilla de
Material 3 a medio configurar. Hay cinco causas concretas que hacen que parezca *cutre*, y todas
tienen arreglo barato:

1. **Restos del morado de M3 por defecto.** `Theme.kt:15-33` solo define 13 de los ~36 roles de
   color. El resto se queda con la paleta base de Google (lila/morado): la pista lila de la barra
   de progreso (`06`, `q-*`), el fondo lila de los diálogos (`07`, `q-finish-dialog`) y el
   segmento seleccionado lila del repaso (`13`). Es el defecto más visible y el más fácil de
   corregir.
2. **Iconos hechos con emojis y caracteres de texto.** `⏱ ❓ ✔ ➜ ⚠ 🔋 ✕` tienen tamaños,
   colores y líneas base distintos (`05-rules` es el peor caso; también el temporizador y la
   tarjeta de inicio de `12`).
3. **Sin identidad.** No hay icono de lanzador (el manifiesto no declara `android:icon`), ni
   marca, ni cabecera. Todo es texto suelto sobre gris, con una franja blanca de `TopAppBar`
   pegada encima (`01`–`05`, `10`, `12`–`15`).
4. **Falta de jerarquía.** El resultado y la credencial son listas de líneas `Etiqueta: valor`
   todas al mismo tamaño (`10`, `15`). El veredicto va en MAYÚSCULAS, con un icono de 24 dp
   que no escala con el texto (`18`). La credencial no parece un documento.
5. **Detalles de acabado.** El texto de las opciones queda arriba de la tarjeta y deja aire
   abajo (`06`, `q-single-selected`). Hay botones píldora con texto partido y alineado a la
   izquierda al 200 % (`18`). Los diálogos solo tienen botones de texto, sin una acción clara
   (`07`, `q-finish-dialog`, `20`).

**Dirección propuesta (§2): «Centre del camp».** Un documento oficial amable dibujado con la
geometría del terreno de juego. Superficies neutras con un ligero tinte verde; verde de césped
profundo como identidad; las líneas de cal del centro del campo como único adorno; un botó
(disco) como forma recurrente en el logo, el marcador del resultado y los indicadores de
respuesta; y Atkinson Hyperlegible, una tipografía diseñada para baja visión. Toda la audacia
visual va a la **credencial**, tratada como un carnet de verdad.

---

## 1. Problemas por pantalla (ordenados por impacto)

Leyenda: **[A]** impacto alto · **[M]** medio · **[B]** bajo. Cada fila indica la captura y el
`fichero:línea`.

### C1 Bienvenida — `01-welcome.png`

| # | Problema | Dónde |
|---|---|---|
| A | Un solo bloque de texto de 6 líneas y el 70 % de la pantalla vacía. Sin marca. Es la primera impresión de la app. | `ScreensMain.kt:42-47`; `strings.xml` `welcome_body` |
| A | Franja blanca del `TopAppBar` sobre el fondo gris `#F4F6F8`: dos tonos sin motivo. | `Components.kt:69-79` (contenedor por defecto = `surface`) |
| M | El wireframe de 04 §2.2 ya preveía viñetas («Es corregeix sola…», «No cal connexió…»), pero se implementó en prosa. | 04 §2.2 |

### C0 Inicio — `02-home-empty.png`, `12-home-passed.png` (`11-exit-dialog-or-back.png` es la misma pantalla)

| # | Problema | Dónde |
|---|---|---|
| A | No hay ninguna identidad CFBB. El título de la barra («Arbitratge CFBB») y el H1 se repiten en dos estilos distintos. | `ScreensMain.kt:61-67` |
| A | Los datos del examen van en una sola línea separada por puntos («5 preguntes · 5 minuts · Per aprovar: 3 encerts»): cuesta leerla y al 200 % se parte mal. | `ScreensMain.kt:67`; `home_summary` |
| A | La tarjeta «✔ Has aprovat · 4 de 5» usa el carácter `✔` en negro (no es un icono) y es una `OutlinedCard` gris genérica. No recuerda en nada a una credencial. | `ScreensMain.kt:69-80` |
| M | «Repassar l'últim examen» es un `TextButton` suelto con sangría distinta a la del resto del contenido. | `ScreensMain.kt:104-106` |
| M | El botón secundario «Tornar a fer l'examen» tiene borde gris y texto tinta: casi no se lee como botón. La web base usa borde y texto en verde de marca (`style.css` `button.secondary`). | `Components.kt:111-119` |

### C2 Nombre — `03-name-empty.png`, `04-name-filled.png`

| # | Problema | Dónde |
|---|---|---|
| M | El aviso «No es podran canviar després» es la información más importante de la pantalla, y va como cuerpo normal. | `ScreensMain.kt:113` |
| M | La etiqueta flotante del campo («Nom i cognoms», `04`) sale a ~12 sp porque M3 usa `bodySmall`, que no está definido en `AppTypography`. Rompe la norma de 04 §8.1 de que nada baje de 16 sp. | `Theme.kt:36-44` |
| B | El campo es transparente sobre el gris. Sobre blanco destacaría más. | `ScreensMain.kt:116-139` |

### C3 Normas — `05-rules.png`

| # | Problema | Dónde |
|---|---|---|
| A | Los iconos son emojis heterogéneos: ⏱ de 16 px, ❓ rojo a todo color, ✔ y ➜ negros y gruesos, ⚠ fino. Además, el texto de cada fila arranca en una x distinta. Es la pantalla más *cutre*. | `ScreensMain.kt:155-159`, `181-186` |
| M | La confirmación del nombre (un dato que queda sellado) no tiene contenedor; «Canviar» flota a la derecha. | `ScreensMain.kt:161-167` |
| M | El aviso de batería usa el emoji 🔋 y texto rojo plano. | `ScreensMain.kt:169-176` |
| **A (contenido)** | «No podràs tornar a una pregunta anterior» contradice la decisión del usuario del 2026-11-20 (se puede volver atrás, `AGENTS.md`). Ver Q5. | `strings.xml` `rules_noback` |

### C4 Pregunta — `06`, `q-truefalse`, `q-single-selected`, `q-multi-selected`, `20-question-fontscale200`

| # | Problema | Dónde |
|---|---|---|
| A | **Opción descentrada.** El `Row` va dentro de una `OutlinedCard` con `heightIn(56)` aplicado a la tarjeta y no a la fila. El contenido queda arriba y sobra aire abajo, como se ve en todas las capturas de pregunta. | `QuestionScreen.kt:246-272` |
| A | Radio y checkbox a 20 dp (el tamaño M3 por defecto): pequeños para un público mayor. El estado seleccionado depende sobre todo del relleno menta saturado `#B9EFCB`. | `QuestionScreen.kt:265`; `Theme.kt:17` |
| A | La barra de progreso es el `LinearProgressIndicator` por defecto: pista lila, hueco entre segmentos y punto final. No es la barra de 8 px de la marca base. | `QuestionScreen.kt:207-210` |
| A | El temporizador usa el emoji `⏱` (diminuto, y su tamaño depende del fabricante) y `⚠` como texto. El ámbar está escrito a mano en el código, fuera del tema. | `QuestionScreen.kt:158-162`, `201`, `224-233` |
| M | El enunciado (22 sp) y la cabecera «Pregunta 1 de 5» (24 sp) compiten, y gana la cabecera, que es la que menos importa. | `QuestionScreen.kt:178-182`, `88-92` |
| M | Al 200 % la cabecera se parte en «Pregunta 5 de / 5» junto al temporizador (`20`). | `QuestionScreen.kt:172-205` |
| M | La pista del tipo de pregunta («Tria una resposta.») es cuerpo plano. Lo único que distingue la múltiple es la forma de la casilla y una frase. | `QuestionScreen.kt:93-95` |
| B | La barra inferior no tiene separador; el contenido largo pasa por debajo sin pista visual. | `Components.kt:83-90` |

### C5 Diálogos — `07-next-without-answer`, `q-finish-dialog`, `17-result-failed` (en realidad es C5-b), `20`

| # | Problema | Dónde |
|---|---|---|
| A | El contenedor es lila (`surfaceContainerHigh` sin definir). | `Theme.kt` |
| A | Las dos acciones son `TextButton` con el mismo peso visual. No se distingue la acción segura de la irreversible, en contra de 04 §2.4 («el botón seguro es el primario»). En C5-c, «Finalitzar» y «Revisar» van uno al lado del otro, iguales. | `QuestionScreen.kt:109-149` |
| M | Al 200 % los botones se apilan alineados a la derecha y en dos líneas (`20`); el título sale en `headlineSmall` por defecto (24 sp, peso normal). | idem; `Theme.kt` |

### C8/C9 Resultado — `10-result`, `18-result-fontscale200`, `19-result-dark`

| # | Problema | Dónde |
|---|---|---|
| A | El veredicto en MAYÚSCULAS (peor para leer) con un `Icons.Check` de 24 dp que no escala: al 200 % el icono se queda diminuto (`18`). | `ScreensResult.kt:100-110` |
| A | Las cifras van como cinco líneas `Body` iguales («Encerts: 4 de 5», «Fallades: 1»…) y parecen un log. El número que importa no destaca. | `ScreensResult.kt:111-116` |
| A | Hay tres botones a ancho completo con el mismo peso. Al 200 % el secundario es una píldora de dos líneas con el texto a la izquierda (`18`). | `ScreensResult.kt:84-96`; `Components.kt:101-119` |
| M | En tema oscuro el botón primario es menta brillante de 56 dp a todo ancho y deslumbra (`19`). | `Theme.kt:26` |
| — | Falta una captura del suspendido (C9) real: `17` muestra el diálogo C5-b. | — |

### C10 Repaso — `13-review-list`, `14-review-detail`

| # | Problema | Dónde |
|---|---|---|
| A | El segmento seleccionado es lila. | `ScreensResult.kt:170-183`; `Theme.kt` |
| A | En el detalle, «La teva resposta» y «La resposta correcta» (24 sp) son más grandes que el enunciado. La jerarquía está invertida y la misma opción aparece dos veces (`14`). | `ScreensResult.kt:208-226` |
| M | Los elementos de la lista no tienen icono de estado ni chevron: no parecen pulsables ni dicen de un vistazo si están falladas. | `ScreensResult.kt:186-195` |
| M | Con un solo elemento, «‹ Anterior» y «Següent ›» aparecen como dos botones fantasma deshabilitados (`14`). Los `‹ ›` son caracteres de texto. | `ScreensResult.kt:156-161`; `btn_prev`/`btn_next_q` |

### C11 Credencial — `15-credential`, `16-presentation`

| # | Problema | Dónde |
|---|---|---|
| A | **No parece un documento.** Rótulo en mayúsculas y siete líneas `Etiqueta: valor` sobre gris, sin contenedor, marca ni estructura. El QR ocupa el resto, y la nota «Emesa en autoservei…» y el descargo quedan fuera de la pantalla. | `ScreensCredential.kt:41-58` |
| A | En la presentación, el «Tancar» es texto suelto arriba a la derecha (fuera del alcance del pulgar) y la composición es un QR con texto centrado debajo, sin identificar el documento. | `ScreensCredential.kt:70-74` |
| M | `"APTE · " + …removePrefix("Examen: ")` concatena cadenas a mano: frágil, y no es i18n. | `ScreensCredential.kt:78` |
| M | Los módulos del QR se dibujan en celdas fraccionarias con un solape de 0,5 px. Funciona, pero los bordes pueden salir antialiasados en algunas densidades. | `Components.kt:139-147` |
| B | Grises `#333333` escritos a mano en el código. | `ScreensCredential.kt:81-82` |

### Global

| # | Problema | Dónde |
|---|---|---|
| A | Sin icono de lanzador (sale el icono genérico de Android) y sin splash de marca. | `AndroidManifest.xml:9-17`; `res/` sin `mipmap*` |
| A | El tema de la ventana es `android:Theme.Material.Light` con un `windowBackground` escrito a mano: no es coherente con el tema de Compose. | `res/values*/themes.xml` |
| M | `Typography` incompleta: faltan `headlineSmall`, `bodySmall`, `labelMedium` y `labelSmall`, y los componentes M3 que los usan caen a 11-14 sp. | `Theme.kt:36-44` |
| M | Sin `Shapes` propias: todo son píldoras (botones) o 12 dp (tarjetas), sin jerarquía. | `Theme.kt:47-53` |

---

## 2. Dirección visual: «Centre del camp»

### 2.1 Idea y principios

- **Tema:** arbitrar *Futbol amb Botons*. El vocabulario visual sale de ese mundo: el **tauler**
  (campo verde con líneas de cal), el **botó** (disco con aro, la ficha del juego) y la
  **targeta groga** del árbitro. No hay balones, trofeos ni gamificación: 04 §8.1 pide un
  «documento oficial amable», no un juego.
- **Un solo elemento memorable: la credencial-carnet** (§4.8). Todo lo demás es tranquilo y
  disciplinado.
- **El disco como forma recurrente.** Logo = botó en el punto central; marcador del resultado =
  botó con aro de progreso; indicadores de respuesta = discos grandes. Así se da coherencia sin
  añadir decoración.
- **Las líneas de cal son el único ornamento:** medio campo y círculo central, a trazo de 1,5 dp
  y 16-20 % de opacidad, solo en la cabecera de inicio, en la banda de la credencial y en el
  icono. Son decorativas (sin semántica) y **no imitan elementos de seguridad**: nada de
  guilloches, hologramas ni sellos. Simular seguridad contradiría 02 §4 y T5.
- **Superficies planas.** Sin sombras salvo en la credencial (es un objeto) y en los diálogos.
  Las capas se separan por tono, no por elevación.
- **Texto en minúscula de frase en todo** (adiós a «HAS APROVAT» y «CREDENCIAL D'APTITUD»):
  las mayúsculas sostenidas se leen peor, sobre todo con baja visión.

### 2.2 Paleta — tema claro (por defecto)

Base: el verde de marca `#1D6F42` (CFBB, `style.css:7`) se mantiene como `primary`. El gris
frío `#F4F6F8` y la tinta pizarra `#1F2937` de la web base se sustituyen por neutros con
un ligero tinte verde, para que el conjunto suene a césped y no a Tailwind.

| Rol M3 | Hex | Uso | Contraste (calculado) |
|---|---|---|---|
| `primary` | `#1D6F42` | Botón primario, selección, enlaces | blanco sobre él 6,7:1 · sobre `background` 6,1:1 |
| `onPrimary` | `#FFFFFF` | | |
| `primaryContainer` | `#D7EEDF` | Opción seleccionada, aprobado | |
| `onPrimaryContainer` | `#0A3A20` | | 10,5:1 sobre `primaryContainer` |
| `inversePrimary` | `#86D7A4` | | |
| `secondary` | `#3F5F4B` | Iconos neutros, segmentos | |
| `onSecondary` | `#FFFFFF` | | |
| `secondaryContainer` | `#D3E6D9` | Segmento seleccionado, *chips* de instrucción, pista de progreso alternativa | |
| `onSecondaryContainer` | `#0F2A1A` | | >10:1 |
| `tertiary` | `#7A5900` | Avisos (ámbar «targeta groga») | |
| `onTertiary` | `#FFFFFF` | | |
| `tertiaryContainer` | `#FFE08A` | Temporizador a 5 min, aviso de batería | |
| `onTertiaryContainer` | `#2B1F00` | | 12,5:1 |
| `background` | `#F2F5F3` | Fondo de pantalla | |
| `onBackground` | `#18241D` | Texto principal (tinta verde-negra) | 14,7:1 sobre `background`, 16:1 sobre blanco (AAA) |
| `surface` | `#FFFFFF` | Tarjetas, opciones, diálogos | |
| `onSurface` | `#18241D` | | 16:1 |
| `surfaceVariant` | `#DFE7E1` | | |
| `onSurfaceVariant` | `#4A5750` | Texto secundario | 7,6:1 sobre blanco (AAA), 6,9:1 sobre `background` |
| `surfaceTint` | `#1D6F42` | (no se usa elevación tonal) | |
| `inverseSurface` | `#2B332E` | *Snackbar* | |
| `inverseOnSurface` | `#EEF2EF` | | |
| `error` | `#B3261E` | Errores del sistema, temporizador a 1 min | blanco sobre él 6,5:1 |
| `onError` | `#FFFFFF` | | |
| `errorContainer` | `#FBE3E0` | Opción incorrecta marcada, aviso de 1 min | |
| `onErrorContainer` | `#5C0E09` | | 11,2:1 |
| `outline` | `#6B7770` | Borde de opción no seleccionada, controles | 4,7:1 sobre blanco (≥ 3:1 de componente) |
| `outlineVariant` | `#C5CFC8` | Divisores, bordes decorativos | (decorativo) |
| `scrim` | `#000000` | | |
| `surfaceBright` | `#FFFFFF` | | |
| `surfaceDim` | `#D8DFDA` | | |
| `surfaceContainerLowest` | `#FFFFFF` | Diálogos | |
| `surfaceContainerLow` | `#F7F9F7` | | |
| `surfaceContainer` | `#EEF2EF` | Pie de la credencial, *callouts* neutros | |
| `surfaceContainerHigh` | `#E8EDE9` | Temporizador en reposo | |
| `surfaceContainerHighest` | `#E1E8E3` | Pista de la barra de progreso | |

**Colores de marca fuera de los roles M3** (`object BrandColors`):

| Token | Hex | Uso |
|---|---|---|
| `pitch` | `#0F3D26` | Banda de la cabecera de inicio y de la credencial; fondo del icono y del splash. Blanco encima: 12,3:1 |
| `pitchLine` | `#FFFFFF` al 18 % | Líneas de cal (decorativas) |
| `chalk` | `#F2F5F3` | Disco del botó en el logo |
| `refCard` | `#F2C230` | Targeta groga del logo (solo en la marca) |
| `qrInk` / `qrPaper` | `#000000` / `#FFFFFF` | QR, siempre (04 §3.1.4) |
| `paperInk` | `#18241D` | Texto de la credencial (que es siempre clara) |

### 2.3 Paleta — tema oscuro

Criterio: un verde-negro profundo en lugar del gris neutro actual, y un primario menta algo
menos luminoso para que el botón de ancho completo no deslumbre (`19`).

| Rol | Hex | | Rol | Hex |
|---|---|---|---|---|
| `primary` | `#86D7A4` | | `onPrimary` | `#003920` (7,6:1) |
| `primaryContainer` | `#1A5235` | | `onPrimaryContainer` | `#C6EED3` (7,2:1) |
| `inversePrimary` | `#1D6F42` | | `secondary` | `#B5CCBC` |
| `onSecondary` | `#213528` | | `secondaryContainer` | `#2E4637` |
| `onSecondaryContainer` | `#D3E6D9` | | `tertiary` | `#F0C64A` |
| `onTertiary` | `#3F2E00` | | `tertiaryContainer` | `#5B4300` |
| `onTertiaryContainer` | `#FFE08A` (7,3:1) | | `background` | `#0F1512` |
| `onBackground` | `#E2EAE4` (15:1) | | `surface` | `#151C18` |
| `onSurface` | `#E2EAE4` | | `surfaceVariant` | `#3A453E` |
| `onSurfaceVariant` | `#BAC6BE` (9,6:1) | | `inverseSurface` | `#E2EAE4` |
| `inverseOnSurface` | `#2B332E` | | `error` | `#FFB4AB` |
| `onError` | `#690005` | | `errorContainer` | `#8C1D18` |
| `onErrorContainer` | `#FFDAD6` | | `outline` | `#87938B` |
| `outlineVariant` | `#3A453E` | | `scrim` | `#000000` |
| `surfaceBright` | `#333B36` | | `surfaceDim` | `#0F1512` |
| `surfaceContainerLowest` | `#0B100D` | | `surfaceContainerLow` | `#151C18` |
| `surfaceContainer` | `#1A221D` | | `surfaceContainerHigh` | `#222B26` (diálogos) |
| `surfaceContainerHighest` | `#2C3530` | | `surfaceTint` | `#86D7A4` |

Excepciones deliberadas en oscuro:

- **La credencial-carnet sigue siendo clara** (papel blanco con banda `pitch`). Es un documento,
  el QR va sobre blanco de todos modos (04 §3.1.4) y así coincide con el PDF.
- **El temporizador a 5 min usa el ámbar claro `#FFE08A`/`#2B1F00` en los dos temas.** Un
  aviso tiene que destacar, y una píldora ámbar sobre fondo oscuro destaca más que el
  contenedor ámbar oscuro.
- Sin color dinámico (04 §8.1).

### 2.4 Tipografía

**Familia: Atkinson Hyperlegible Next** (Braille Institute, SIL OFL 1.1; variable `wght`
200–800, ~115 KB), empaquetada en el APK en `res/font/`. Funciona sin conexión y no descarga
nada. **Para el temporizador y el código de la credencial: Atkinson Hyperlegible Mono** (OFL,
variable).

Por qué esta fuente y no Roboto ni una pila del sistema:

- Está **diseñada para lectores con baja visión**: distingue letras que se confunden (`I l 1`,
  `0 O`, `B 8`, `rn m`). Es exactamente el público de la app.
- El código corto `1291-82BC` mezcla `8` y `B`, y el organizador lo lee en voz alta o lo
  compara a ojo. La versión Mono diferencia `8`/`B` y `0`/`O`, y sus cifras tabulares evitan
  que el temporizador «baile» cada segundo.
- Tiene carácter propio sin ser decorativa: la app deja de parecer «Android por defecto».

Sustituye a «Roboto» de 04 §8.1 (pregunta Q1). Los tamaños mínimos de 04 §8.1 se respetan o se
superan; ningún rol baja de 16 sp.

| Rol M3 | sp / interlineado | Peso | Uso |
|---|---|---|---|
| `displayLarge` | 44 / 52 | 700 | Número de aciertos dentro del disco de resultado |
| `displayMedium` | 40 / 48 | 700 | (reserva) |
| `displaySmall` | 34 / 42 | 700 | Veredicto: «Has aprovat» / «No has aprovat, de moment» |
| `headlineLarge` | 30 / 38 | 800 | Marca «Arbitratge» en la cabecera de inicio |
| `headlineMedium` | 28 / 36 | 700 | Nombre en la credencial (04 §8.1: 28/700) |
| `headlineSmall` | 24 / 32 | 600 | Títulos de sección en el contenido; título de diálogo (rol por defecto de M3) |
| `titleLarge` | 22 / 28 | 600 | Título de la barra superior |
| `titleMedium` | 20 / 28 | 400 | Texto de las opciones de respuesta (04: 20/400) |
| `titleSmall` | 18 / 26 | 600 | Títulos de tarjeta, valores de campo |
| `bodyLarge` | 18 / 27 | 400 | Cuerpo (04: 18; interlineado 1,5) |
| `bodyMedium` | 16 / 24 | 400 | Secundario |
| `bodySmall` | **16** / 24 | 400 | Igual que `bodyMedium`: evita que la etiqueta del `TextField` caiga a 12 sp |
| `labelLarge` | 18 / 24 | 600 | Botones (se baja de 20 a 18: al 200 % evita el corte de `18-result-fontscale200`) |
| `labelMedium` | 16 / 20 | 500 | *Chips*, segmentos, insignias |
| `labelSmall` | **16** / 20 | 500 | Mínimo absoluto |

Estilos propios (fuera de `Typography`, en `object ArbText`):

- `question`: 24 / 33, peso 600. **El enunciado pasa a ser lo más grande del examen** (04 decía
  22; el cambio invierte la jerarquía rota de `06`, donde mandaba la cabecera).
- `progressLabel`: 18 / 24, peso 600, `onSurfaceVariant`: «Pregunta 3 de 20».
- `timer`: Mono 20 / 24, peso 600, `fontFeatureSettings = "tnum"`.
- `code`: Mono 20 / 28, peso 600, `letterSpacing = 1.sp`: «1291-82BC».

Texto siempre alineado a la izquierda (04 §8.1). Solo se centran el modo presentación y el
contenido del disco de resultado.

### 2.5 Formas

```kotlin
val ArbShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),   // insignias, casilla de opción múltiple (7 dp, ver §4.1)
    small      = RoundedCornerShape(10.dp),  // = radio de marca de la web base (style.css:10)
    medium     = RoundedCornerShape(14.dp),  // botones, opciones, campos
    large      = RoundedCornerShape(20.dp),  // tarjetas, credencial
    extraLarge = RoundedCornerShape(24.dp),  // diálogos, cabecera de inicio, bottom sheet
)
```

- **Botones en rectángulo redondeado de 14 dp, no en píldora.** Con dos líneas al 200 % la
  píldora se deforma (`18`); el rectángulo aguanta. Además se lee más «documento» y menos
  «app de juego».
- Píldora (`CircleShape`) solo para el temporizador y los *chips* de una línea.
- Círculo perfecto = el «botó»: logo, disco de resultado, indicadores de opción única, insignias
  de estado.

### 2.6 Espaciado, tamaños y elevación

```kotlin
object ArbDimens {
    val gutter = 20.dp          // margen lateral de pantalla
    val s4 = 4.dp; val s8 = 8.dp; val s12 = 12.dp; val s16 = 16.dp; val s24 = 24.dp; val s32 = 32.dp
    val sectionGap = 24.dp      // entre bloques de contenido
    val itemGap = 12.dp         // entre tarjetas/opciones
    val touchMin = 48.dp        // cualquier control
    val buttonMin = 56.dp       // botones de acción
    val optionMin = 64.dp       // tarjeta de opción (true_false: 72.dp)
    val indicator = 28.dp       // radio/casilla dentro de la opción
    val iconInline = 24.dp      // icono junto a texto (escalado, ver nota)
    val iconBadge = 40.dp       // círculo contenedor de icono
    val progressHeight = 8.dp
    val dividerThin = 1.dp
}
```

- **Iconos que acompañan a texto escalan con la fuente**:
  `val s = with(LocalDensity.current) { 24.sp.toDp() }.coerceAtMost(48.dp)`. Así se evita el
  icono enano de `18`.
- **Elevación:** nivel 0 en todo. Excepciones: credencial `shadowElevation = 3.dp`; diálogos
  `6.dp`. Ninguna superficie usa `tonalElevation` (en M3 tiñe).
- **Separador inferior:** 1 dp `outlineVariant` sobre la barra de acciones, solo cuando el
  contenido se puede desplazar (`scrollState.canScrollForward`).

### 2.7 Iconografía

- **Material Symbols Rounded**, peso 500 (algo más grueso, mejor con baja visión), relleno 0 en
  reposo y 1 en los estados (`check_circle`, `cancel`). Lo redondeado conversa con el disco.
- Se **copian como `VectorDrawable`** en `res/drawable/ic_*.xml` (Apache 2.0, ~1 KB cada uno)
  en lugar de añadir `material-icons-extended`. Juego cerrado:
  `timer, visibility_off, visibility, warning, error, info, check, check_circle, cancel, close,
  remove, chevron_left, chevron_right, arrow_back, quiz, flag, edit, battery_alert, menu_book,
  logout, wifi_off, task_alt, id_card, qr_code_2, expand_more`.
- **Ningún emoji ni carácter dingbat en la UI** (`⏱ ❓ ✔ ➜ ⚠ 🔋 ✕ ‹ ›`). Fuera de
  `strings.xml` y fuera del código.
- Iconos informativos con `contentDescription`; decorativos (los que acompañan a un texto que
  ya lo dice) con `null`.

### 2.8 Marca: «Xapa al punt central»

**El logo** es un botó (disco claro con aro verde) colocado sobre el punto central de un campo
(medio campo y círculo de cal), con una **targeta groga** asomando por arriba a la derecha.
Lectura: *el árbitro de Futbol amb Botons*.

`res/drawable/ic_launcher_background.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#0F3D26" android:pathData="M0,0h108v108h-108z"/>
    <!-- línia de mig camp -->
    <path android:strokeColor="#2EFFFFFF" android:strokeWidth="1.5" android:pathData="M54,0V108"/>
    <!-- cercle central r=30 -->
    <path android:strokeColor="#2EFFFFFF" android:strokeWidth="1.5"
        android:pathData="M24,54a30,30 0 1,0 60,0a30,30 0 1,0 -60,0"/>
</vector>
```

`res/drawable/ic_launcher_foreground.xml` (dentro de la zona segura de 66 dp):

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <!-- botó -->
    <path android:fillColor="#F2F5F3" android:pathData="M33,54a21,21 0 1,0 42,0a21,21 0 1,0 -42,0"/>
    <path android:strokeColor="#1D6F42" android:strokeWidth="2.5"
        android:pathData="M38,54a16,16 0 1,0 32,0a16,16 0 1,0 -32,0"/>
    <!-- targeta groga, girada 12° -->
    <group android:rotation="12" android:pivotX="65" android:pivotY="37">
        <path android:fillColor="#F2C230" android:strokeColor="#0F3D26" android:strokeWidth="1.2"
            android:pathData="M61.5,30h7a1.5,1.5 0 0 1 1.5,1.5v11a1.5,1.5 0 0 1 -1.5,1.5h-7a1.5,1.5 0 0 1 -1.5,-1.5v-11a1.5,1.5 0 0 1 1.5,-1.5z"/>
    </group>
</vector>
```

- `res/mipmap-anydpi/ic_launcher.xml` y `ic_launcher_round.xml`: `<adaptive-icon>` con
  `background`, `foreground` y `<monochrome>` (iconos temáticos de Android 13: el mismo
  *foreground* en un solo color). Con `minSdk 26` no hacen falta PNG heredados.
- En el manifiesto: `android:icon="@mipmap/ic_launcher"` y
  `android:roundIcon="@mipmap/ic_launcher_round"`.
- **Marca dentro de la app**: `ic_brand_mark_on_dark.xml` (el *foreground* anterior, viewport
  48×48 reescalado) para usar sobre `pitch`, y `ic_brand_mark_on_light.xml` (disco `#1D6F42`,
  aro `#F2F5F3`, la misma tarjeta) para usar sobre papel.
- **Splash** (`values-v31/themes.xml`): `android:windowSplashScreenBackground = #0F3D26` y
  `android:windowSplashScreenAnimatedIcon = @drawable/ic_launcher_foreground`. En
  `values/themes.xml` y `values-night/themes.xml`, `windowBackground` igual al `background` de
  cada tema (`#F2F5F3` / `#0F1512`).
- **Wordmark**: «Arbitratge» en `headlineLarge` (800) blanco sobre `pitch`, y debajo «Examen
  d'àrbitre de Futbol amb Botons» en `bodyLarge` blanco al 88 %. Si el club tiene un logo
  oficial, va en la credencial y en el PDF junto a la *xapa* (Q2).

**Patrón de cal** (Compose, `Modifier.pitchLines()` mediante `drawBehind`): línea vertical en
`x = 0.80·ancho` y círculo de radio `0.85·alto` centrado en `(0.80·ancho, alto/2)`. Trazo
1,5 dp `Color.White.copy(alpha = 0.16f)`, recortado al contenedor y sin semántica
(`clearAndSetSemantics {}` en la capa decorativa).

---

## 3. Lista priorizada de cambios (impacto ÷ esfuerzo)

Cada punto es una tarea para `android-developer`. **P0** = poco esfuerzo y mucho impacto, en el
orden en que conviene hacerlos.

| P | # | Cambio | Ficheros | Esfuerzo |
|---|---|---|---|---|
| **P0** | 1 | `lightColorScheme`/`darkColorScheme` **completos** (§2.2-2.3, los ~36 roles), `ArbShapes` y la `Typography` completa (§2.4, aún con Roboto si la fuente no está aprobada). Elimina el lila de golpe. | `Theme.kt` | XS |
| P0 | 2 | Barra superior del color de `background` (`TopAppBarDefaults.topAppBarColors(containerColor = background, scrolledContainerColor = surfaceContainer)` + `pinnedScrollBehavior`), título `titleLarge` 22/600. Divisor inferior condicional. | `Components.kt:66-91` | XS |
| P0 | 3 | Botones: forma `shapes.medium`; `labelLarge` 18 sp centrado (`textAlign = Center`); secundario con borde 1,5 dp `primary` y texto `primary`; nuevo `TertiaryButton` (TextButton de 48 dp) para «Tornar a l'inici». | `Components.kt:100-119`, `ScreensResult.kt:95`, `ScreensCredential.kt:44` | XS |
| P0 | 4 | `OptionCard` nueva (§4.1): contenido centrado, indicador de 28 dp, borde de 3 dp al seleccionar. | `QuestionScreen.kt:243-273` | S |
| P0 | 5 | Iconos vectoriales en lugar de los emojis (§2.7): normas, temporizador, avisos, tarjeta de inicio, batería, `✕`, `‹ ›`. | `ScreensMain.kt:73-74,155-186`, `QuestionScreen.kt:201,226,232`, `strings.xml` `btn_prev`/`btn_next_q` | S |
| P0 | 6 | `ArbDialog` (§4.4): fondo blanco/`surfaceContainerHigh`, acciones apiladas a ancho completo, segura = rellena. Sustituye los 4 `AlertDialog`. | `QuestionScreen.kt:107-150` | S |
| P0 | 7 | Minúscula de frase: quitar los `.uppercase()` del veredicto y de la credencial. | `ScreensResult.kt:107`, `ScreensCredential.kt:47` | XS |
| P0 | 8 | Icono adaptativo + monocromo, splash y `windowBackground` (§2.8). | `res/drawable`, `res/mipmap-anydpi`, `res/values*/themes.xml`, `AndroidManifest.xml` | S |
| **P1** | 9 | Fuente Atkinson Hyperlegible Next + Mono (§5.2), una vez resuelta Q1. | `res/font`, `Theme.kt`, licencia | S |
| P1 | 10 | Cabecera de pregunta + temporizador + barra de progreso de marca (§4.2-4.3), con disposición adaptativa al 200 %. | `QuestionScreen.kt:154-213` | M |
| P1 | 11 | Credencial-carnet (§4.8) y modo presentación (§4.9). | `ScreensCredential.kt` | M |
| P1 | 12 | Resultado con disco-marcador y cifras en tarjetas (§4.5). | `ScreensResult.kt:73-136` | M |
| P1 | 13 | Inicio con cabecera de marca, cifras del examen y tarjeta de estado (§4.10). | `ScreensMain.kt:51-108`, `strings.xml` (cadenas nuevas) | M |
| P1 | 14 | Repaso: lista con insignias de estado y detalle con tarjetas de resultado por opción (§4.6-4.7; el detalle depende de Q3). | `ScreensResult.kt:142-243` | M |
| **P2** | 15 | Bienvenida con cabecera y 4 filas con icono (§4.11; separa `welcome_body`). | `ScreensMain.kt:42-48`, `strings.xml` | S |
| P2 | 16 | Normas con `RuleRow` y `NameConfirmCard`; nombre con aviso destacado y vista previa del carnet. | `ScreensMain.kt:110-187` | S |
| P2 | 17 | Mapa de preguntas (§4.12) cuando se implemente volver atrás (decisión del 2026-11-20). | `QuestionScreen.kt`, `ExamViewModel.kt` | M |
| P2 | 18 | Movimiento: fundido de 150 ms entre preguntas; el aro del resultado se dibuja en 600 ms (un solo momento orquestado); todo desactivado si la escala de animación es 0. | `QuestionScreen.kt`, `ScreensResult.kt` | S |
| P2 | 19 | QR con módulos en píxeles enteros (§4.8). | `Components.kt:135-151` | XS |
| P2 | 20 | Error, carga y corrección: icono de 48 dp en un círculo de `errorContainer`/`primaryContainer` + título + cuerpo; marca de 64 dp sobre el indicador circular en la carga. | `ScreensMain.kt:32-40,196-203`, `ScreensResult.kt:44-71` | XS |

Con los P0 (≈ 1 jornada) desaparece la sensación de *cutre*. Los P1 le dan identidad.

---

## 4. Especificación de componentes

Todas las medidas en dp/sp; los colores por rol (§2.2-2.3). La semántica que ya está bien en el
código (roles, `stateDescription`, `heading`, `liveRegion`) **se conserva tal cual**.

### 4.1 `OptionCard` (por tipo de pregunta)

```
┌──────────────────────────────────────────────┐  ← Surface, shape 14 dp, minHeight 64 dp
│  (◯)  [FICTICI] Text de l'opció que pot      │     padding 16 h / 14 v, gap 14 dp
│       ocupar dues línies                     │     Row(verticalAlignment = CenterVertically)
└──────────────────────────────────────────────┘
```

| Propiedad | Sin seleccionar | Seleccionada |
|---|---|---|
| Fondo | `surface` (#FFFFFF / #151C18) | `primaryContainer` (#D7EEDF / #1A5235) |
| Borde | 1,5 dp `outline` | **3 dp `primary`** |
| Texto | `titleMedium` 20/28, `onSurface` | igual, `onPrimaryContainer`; **sin cambiar el peso** (evita que el texto se recoloque) |
| Pulsado | *ripple* `primary` al 12 % | — |

- **Toda la `Surface` es el objetivo táctil**: `selectable`/`toggleable` sobre la `Surface`, no
  sobre el `Card` con un `Row` dentro (eso causa el descentrado actual).
- **Indicador propio de 28 dp** (Canvas o `Box`), `onClick = null` y sin semántica propia (la
  pone la tarjeta):
  - `single_choice` y `true_false`: círculo de 28 dp con aro de 2,5 dp (`outline` → `primary`);
    seleccionado, disco interior de 14 dp `primary`.
  - `multiple_choice`: cuadrado de 28 dp con radio de 7 dp y aro de 2,5 dp `outline`;
    seleccionado, relleno `primary` + `ic_check` de 20 dp `onPrimary`. La forma cuadrada frente
    a la redonda sigue marcando el tipo (04 §2.4).
- `true_false`: mismas tarjetas apiladas (no lado a lado: al 200 % «Vertader» no cabe en media
  anchura), `minHeight 72 dp`, texto 22 sp / 400. **Sin iconos ✓/✗** junto a «Vertader» y
  «Fals»: parecerían corrección.
- Separación entre tarjetas: 12 dp. Transición de color y borde: `animateColorAsState` en
  120 ms (0 ms si la escala de animación es 0).
- Mientras se guarda (`saving`): no se atenúa salvo que dure más de 300 ms, para evitar
  parpadeos.

**`InstructionChip`** (encima de las opciones, en lugar del `Body` de `QuestionScreen.kt:93-95`):
`Row`, fondo `secondaryContainer`, forma `RoundedCornerShape(12.dp)`, padding 12 h / 8 v, icono
de 20 dp (`radio_button_checked` en única; `check_box` en múltiple) y texto `labelMedium` 16/500
`onSecondaryContainer`. No es interactivo.

**Contador de la múltiple** (barra inferior): icono `check_box` de 20 dp + «2 respostes
marcades» en `bodyMedium` `onSurfaceVariant`; conserva el `liveRegion`.

### 4.2 Cabecera de pregunta y progreso

```
 Pregunta 3 de 20                     ┌─────────────┐
                                      │ ⏲  18:42    │  ← TimerPill
                                      └─────────────┘
 ████████████░░░░░░░░░░░░░░░░░░░░░░░░                  ← 8 dp, radio 4
```

- Contenedor: `background`, insets superiores y horizontales (como ahora), padding 20 h, 8 top,
  12 bottom.
- «Pregunta 3 de 20»: `ArbText.progressLabel` (18/600 `onSurfaceVariant`). Es `heading` +
  `liveRegion` (se conserva). El enunciado debajo, en `ArbText.question` 24/33/600.
- **Disposición adaptativa**: si `LocalDensity.current.fontScale >= 1.5f`, columna (etiqueta y
  temporizador debajo, alineado a la izquierda); si no, `Row` con `SpaceBetween`. Corrige `20`.
- **Barra**: `LinearProgressIndicator(progress, Modifier.height(8.dp), color = primary,
  trackColor = surfaceContainerHighest, strokeCap = StrokeCap.Round, gapSize = 0.dp,
  drawStopIndicator = {})`. Sigue con `clearAndSetSemantics {}`. Es la barra de 8 px de
  `style.css` (`.progress-bar`).
- Cuando se pueda volver atrás (§4.12), la barra pasa a medir **contestadas/total**, no la
  posición.

### 4.3 `TimerPill` y avisos de tiempo

| Estado | Fondo | Texto/icono | Icono |
|---|---|---|---|
| Normal | `surfaceContainerHigh` | `onSurface` | `ic_timer` |
| ≤ 5 min | `#FFE08A` (los dos temas) | `#2B1F00` | `ic_warning` |
| ≤ 1 min | `error` | `onError` | `ic_warning` |
| Oculto | `surfaceContainerHigh` | `onSurfaceVariant` | `ic_visibility` + «Mostrar el temps» |

- Píldora `CircleShape`, `minHeight 48 dp`, padding 14 h / 10 v, icono de 22 dp (escalado),
  separación de 8 dp, texto `ArbText.timer` (Mono 20/600 con cifras tabulares). **Sin
  parpadeo**; el color cambia una sola vez en cada umbral (04 §2.4).
- La `contentDescription` y la ausencia de `liveRegion` se quedan como están.
- **`TimeBannerCard`**: forma `large` (20 dp), padding 16, fondo `tertiaryContainer` (5 min) o
  `errorContainer` (1 min), icono `ic_warning` de 24 dp, texto `bodyLarge` y cierre
  `IconButton` de 48 dp con `ic_close` («Tancar l'avís»). Desaparecen los colores escritos a
  mano de `QuestionScreen.kt:217`.

### 4.4 `ArbDialog`

```
┌────────────────────────────────────┐  ← Surface, radio 24 dp, padding 24 dp
│  ⚠                                 │     icono 32 dp (opcional, color del tipo)
│  No has marcat cap resposta.       │     headlineSmall 24/32/600
│  Si continues, comptarà com a      │     bodyLarge 18/27, onSurfaceVariant
│  fallada…                          │
│                                    │     24 dp
│  ┌──────────────────────────────┐  │
│  │      Marcar una resposta     │  │  ← PrimaryButton (acción SEGURA)
│  └──────────────────────────────┘  │     8 dp
│  ┌──────────────────────────────┐  │
│  │     Continuar igualment      │  │  ← SecondaryButton (irreversible)
│  └──────────────────────────────┘  │
└────────────────────────────────────┘
```

- `BasicAlertDialog` + `Surface(shape = extraLarge, color = surfaceContainerLowest` (claro,
  blanco) / `surfaceContainerHigh` (oscuro), `shadowElevation = 6.dp)`. Ancho:
  `fillMaxWidth()` con márgenes de 24 dp.
- **Acciones siempre apiladas a ancho completo**: al 200 % no se parten (`20`) y el orden
  vertical deja clara la jerarquía.
- Contenido con `verticalScroll` para fuentes grandes.
- Asignación (texto actual, sin cambiar el *microcopy*):

| Diálogo | Icono | Primario (seguro) | Secundario |
|---|---|---|---|
| C5-a salir | `logout`, `secondary` | «Continuar» | «Sortir» |
| C5-b sin respuesta | `warning`, `tertiary` | «Marcar una resposta» | «Continuar igualment» |
| C5-c finalizar | `flag`, `primary` | ver **Q4** (propuesta: «Finalitzar») | «Revisar» |
| Error al guardar | `error`, `error` | «Tornar-ho a provar» | «Sortir de l'examen» |

### 4.5 Resultado (C8 aprobado / C9 suspendido)

```
 ╭──────────╮
 │ ╭──────╮ │   ← ScoreDisc 120 dp: aro de progreso + aro interior (el «botó»)
 │ │  4   │ │      44 sp/700 ; «de 5» 18 sp
 │ │ de 5 │ │
 │ ╰──────╯ │
 ╰──────────╯
 Has aprovat                         ← displaySmall 34/42/700, heading
 Mínim per aprovar: 3                ← bodyLarge onSurfaceVariant

 ┌───────────────┐ ┌───────────────┐
 │ Fallades      │ │ Temps         │ ← StatTile
 │ 1             │ │ 1 min 46 s    │
 └───────────────┘ └───────────────┘

 ┌───────────────────────────────────┐
 │ [▬] La teva credencial ja està    │ ← StatusCallout (mini carnet 40×28)
 │     preparada.                    │
 └───────────────────────────────────┘
```

- **`ScoreDisc`**: 120 dp (crece con `fontScale`, con un mínimo de 120 y diámetro =
  texto + 32 dp). El aro exterior de 8 dp es un arco con `sweep = aciertos/total`, y una
  **marca de umbral** (un tic de 3 dp en `threshold/total`) indica dónde se aprueba. El aro
  interior de 1,5 dp a 10 dp del borde es el canto del botó.
  - Aprobado: relleno `primaryContainer`, arco `primary`, pista `outlineVariant`, tic
    `onPrimaryContainer`.
  - Suspendido (04 §2.8, sin rojo): relleno `surfaceContainerHigh`, arco `outline`, tic
    `primary` (enseña la meta, no el fallo).
  - Semántica: `clearAndSetSemantics { contentDescription = "4 encerts de 5" }`. El veredicto
    sigue siendo el `heading`.
  - El único movimiento orquestado de la app: el arco se dibuja en 600 ms al entrar (estático
    si la escala de animación es 0).
- Veredicto en minúscula de frase. **Se elimina el `Icons.Check`/`Info` de 24 dp**: el disco
  lleva la carga visual.
- **`StatTile`**: `Surface` `surface`, forma `large`, padding 16, `weight(1f)` dentro de un
  `FlowRow` (al 200 % se apilan). Etiqueta `bodyMedium` `onSurfaceVariant`, valor
  `headlineSmall` 24/600. Se muestran «Fallades» y «Temps»; los aciertos ya están en el disco.
  Suspendido: añade «Et van faltar 3 encerts» como tercera tarjeta.
- **`StatusCallout`**: forma `large`, padding 16, fila con un glifo de 40 dp y texto
  `bodyLarge`.
  - Credencial lista: glifo de mini carnet (rectángulo 40×28 con banda `pitch` arriba) +
    `passed_next`.
  - Pendiente: fondo `errorContainer` + `ic_error` + `cred_pending`.
  - Suspendido: fondo `secondaryContainer` + `ic_info` + `failed_next`.
- Barra inferior: primario + secundario (contorno) + «Tornar a l'inici» como `TertiaryButton`
  centrado. Tres pesos para tres prioridades.

### 4.6 Repaso — lista

- Filtro `SegmentedButton`: se queda; con la paleta nueva el seleccionado es `secondaryContainer`
  verde-gris. Altura mínima de 56 dp y etiqueta `labelMedium` 16 sp (antes `bodyLarge`
  forzado).
- **`ReviewItem`**: `Surface(onClick)`, forma `large`, `surface`, borde 1 dp `outlineVariant`,
  padding 16, `minHeight 72 dp`. `Row` con:
  - `StatusBadge` de 40 dp: fallada → `errorContainer` + `ic_close` `onErrorContainer`; sin
    responder → `surfaceContainerHighest` + `ic_remove`; acertada → `primaryContainer` +
    `ic_check`.
  - Columna: «Pregunta 2 de 5 · Fallada» en `labelMedium` 16/500 (`onSurfaceVariant`) y el
    enunciado en `bodyLarge`, sin truncar.
  - `ic_chevron_right` de 24 dp `onSurfaceVariant`.
- El estado se dice con texto + icono + color (04 §8.2). El elemento entero es un solo nodo de
  TalkBack con rol `Button`.

### 4.7 Repaso — detalle (pendiente de Q3)

**Propuesta recomendada (B): la pregunta con sus opciones resueltas**, como `.option-item--correct`
y `--incorrect` de la web base:

| Caso | Fondo | Borde | Icono 28 dp | Etiqueta (16/500) |
|---|---|---|---|---|
| Correcta y marcada | `primaryContainer` | 2 dp `primary` | `check_circle` relleno `primary` | «Correcta, la vas marcar» |
| Correcta no marcada | `surface` | 2 dp `primary` **discontinuo** (8 on / 6 off) | `check_circle` contorno `primary` | «Correcta, no la vas marcar» |
| Incorrecta marcada | `errorContainer` | 2 dp `error` | `cancel` relleno `error` | «Incorrecta, la vas marcar» |
| Incorrecta no marcada | `surface` | 1 dp `outlineVariant` | (hueco de 28 dp) | — |

- Encima: el enunciado en `ArbText.question` y una insignia de estado (como en §4.6). La barra
  superior dice solo «Pregunta 2 de 5»; la insignia lleva el estado, así el título no se parte.
- «Per què?» (solo si hay explicación): `callout` con fondo `surfaceContainer`, forma `large`,
  `ic_menu_book`, título `titleSmall` y cuerpo `bodyLarge`.
- Navegación: si la lista tiene un solo elemento, **no hay barra**. Si tiene más, «Anterior»
  (secundario, `ic_chevron_left`) y «Següent» (primario, `ic_chevron_right`) con `weight(1f)`.
  En los extremos, el botón que no aplica se oculta en lugar de deshabilitarse (los
  deshabilitados al 38 % confunden, `14`).
- **Alternativa (A)** si Q3 dice que no: mantener las secciones de 04 §2.9, con encabezados en
  `titleSmall` 18/600 (por debajo del enunciado) y los elementos como las tarjetas de la tabla.

### 4.8 Credencial-carnet (C11) — el elemento memorable

```
┌────────────────────────────────────────────┐ ← Surface papel #FFFFFF (también en oscuro)
│▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓│   radio 20, borde 1 dp #C5CFC8, sombra 3 dp
│▓ (xapa) Credencial d'aptitud      ╎   ◜‾◝ ▓│ ← banda pitch #0F3D26, min 72 dp, líneas de cal
│▓        Àrbitre de Futbol amb     ╎  (   )▓│   título 20/600 blanco; subtítulo 16 blanco 88 %
│▓        Botons · CFBB             ╎   ◟_◞ ▓│
│────────────────────────────────────────────│
│  Anna Prova Puig                           │ ← headlineMedium 28/36/700 paperInk
│                                            │
│  Resultat          Examen                  │ ← etiqueta bodyMedium #4A5750
│  APTE              reglament sample-1      │   valor titleSmall 18/600 #18241D
│  Data del dispositiu   Codi                │
│  20/11/2026            1291-82BC           │ ← código en ArbText.code (Mono)
│                                            │
│  ┌──────────────────────────────────────┐  │
│  │                QR                    │  │ ← blanco, zona de silencio de 4 módulos
│  └──────────────────────────────────────┘  │
│────────────────────────────────────────────│
│ ⓘ Emesa en autoservei. La comprova un      │ ← pie surfaceContainer (#EEF2EF)
│   organitzador amb el verificador oficial. │   bodyMedium 16, #4A5750
│   No és un certificat oficial…             │
└────────────────────────────────────────────┘
```

- Tarjeta de ancho = pantalla − 2×20 dp. Padding del cuerpo: 20 dp. Campos en una cuadrícula de
  2 columnas (`FlowRow` con `maxItemsInEachRow = 2`; al 200 % pasan a una columna). Cada campo
  es un `Column` con `semantics(mergeDescendants = true)`.
- **Etiquetas y valores separados** (no «Codi: 1291-82BC» en una línea): hacen falta 4 cadenas
  de etiqueta nuevas («Resultat», «Examen», «Data del dispositiu», «Codi») y los valores sin
  prefijo. Las cadenas actuales se mantienen para TalkBack, PDF y presentación si se prefiere.
- **T5 / 02 §4 (obligatorio)**: sin check, sin «VÀLIDA», sin sellos, guilloches ni hologramas.
  La banda verde es de marca (la misma que en la cabecera de inicio), no un indicador de
  validez. «Emesa en autoservei» queda **dentro** del documento, en el pie, así que no se pierde
  bajo el pliegue como en `15`.
- **En oscuro, la credencial sigue clara** (§2.3): sobre el fondo `#0F1512` se ve como un carnet
  físico; la sombra se cambia por un borde de 1 dp `#3A453E`.
- **QR**: `QrCode` con módulos en píxeles enteros:
  `cellPx = floor(minDimPx / (n + 8))`, origen = `(minDimPx − cellPx·(n+8))/2` y relleno blanco
  del lado entero. Siempre `#000`/`#FFF`, **sin logo dentro del QR** (no se toca el token ni la
  corrección de errores de ARB1). Tamaño = ancho interior de la tarjeta.
- Acciones (barra inferior): «Mostrar a un organitzador» (primario, `ic_qr_code_2`) y «Tornar a
  l'inici» (terciario). Compartir, PDF e imprimir irán aquí cuando existan (04 §3.2).

### 4.9 Modo presentación

```
 (xapa 32) Credencial d'aptitud          ← 56 dp, titleSmall 18/600 #000

        ┌──────────────────────┐
        │        QR            │         ← min(ancho − 32 dp, alto × 0,55), centrado
        └──────────────────────┘
         Anna Prova Puig                  ← headlineMedium 28/700 #000, centrado
         Resultat: APTE                   ← bodyLarge #000 (cadena cred_result)
         Examen: reglament sample-1       ← bodyLarge #000 (cred_exam, SIN removePrefix)
         Codi 1291-82BC                   ← ArbText.code #000
         Emesa en autoservei              ← titleSmall
         Mantén la pantalla encesa…       ← bodyMedium #3D3D3D (10:1)
         Per verificar-la: obre…
 ┌────────────────────────────────────┐
 │  ✕  Tancar                         │  ← OutlinedButton 56 dp, borde 1,5 dp #000, abajo
 └────────────────────────────────────┘
```

- Fondo `#FFFFFF` a pantalla completa, iconos de sistema oscuros. El brillo y el
  `KEEP_SCREEN_ON` se quedan como están.
- «Tancar» pasa **abajo, a ancho completo** (zona del pulgar; el atrás del sistema sigue
  cerrando). Es un cambio de posición, no de flujo.
- Se elimina la concatenación `"APTE · " + …removePrefix(…)` (`ScreensCredential.kt:78`): dos
  líneas con las cadenas existentes.

### 4.10 Inicio (C0)

```
 ┌──────────────────────────────────────────┐ ← HeroCard: pitch, radio 24, márgenes 16 h,
 │ (xapa 48)                       ╎  ◜‾◝   │   8 dp bajo la barra de estado; padding 24;
 │ Arbitratge                      ╎ (   )  │   min 168 dp; líneas de cal (§2.8)
 │ Examen d'àrbitre de Futbol amb  ╎  ◟_◞   │   headlineLarge blanco + bodyLarge blanco 88 %
 │ Botons                                   │
 └──────────────────────────────────────────┘
 ┌──────────┐ ┌──────────┐ ┌──────────┐
 │ ? 5      │ │ ⏲ 5      │ │ ⚑ 3      │      ← FactTile ×3 (FlowRow)
 │preguntes │ │ minuts   │ │encerts per│        icono 24 primary, valor 24/700,
 │          │ │          │ │ aprovar  │        etiqueta 16 onSurfaceVariant; radio 16
 └──────────┘ └──────────┘ └──────────┘
 [Tarjeta de estado]
```

- **Sin `TopAppBar` en Inicio**: la cabecera la sustituye (se acaba la duplicidad
  «Arbitratge CFBB» + H1). La cabecera es una tarjeta con márgenes, no una banda a sangre, así
  la barra de estado conserva iconos oscuros sin tener que cambiar su apariencia por pantalla.
  Si llega la Ayuda (C13, «ⓘ»), irá como `IconButton` de 48 dp blanco en la esquina superior
  derecha de la cabecera.
- `FactTile`: sustituye la línea `home_summary`. Hacen falta cadenas o *plurals* nuevas para
  cada etiqueta (pregunta para `ux-designer`, no cambia el contenido).
- **Tarjeta de estado**:
  - Vacío: `bodyLarge` `onSurfaceVariant` («Encara no has fet cap examen…»), sin contenedor.
  - **Con credencial**: **miniatura del carnet** (§4.8 a escala: banda `pitch` de 48 dp con la
    xapa de 24 dp y «Credencial d'aptitud», nombre en `titleSmall` y debajo `last_passed`). No es
    pulsable; la acción es el primario de abajo («Veure la credencial»), para no duplicar
    objetivos.
  - Último suspendido: `Surface` `surface`, forma `large`: `last_failed` + `TertiaryButton`
    «Repassar l'últim examen» dentro de la tarjeta (alineado con su contenido).
  - Credenciales pendientes: `StatusCallout` `errorContainer` (§4.5) con su botón.

### 4.11 Bienvenida (C1), Nombre (C2), Normas (C3)

- **C1**: `HeroCard` (§4.10) arriba; «Et donem la benvinguda» en `headlineSmall`; 4
  `BenefitRow`: insignia de 40 dp `primaryContainer` con icono de 24 dp `onPrimaryContainer`
  + `bodyLarge`, `minHeight 56`, separación de 12. Iconos: `task_alt` (es corregeix sola),
  `menu_book` (veuràs les fallades i la resposta correcta), `id_card` (credencial),
  `wifi_off` (no cal internet). El texto ya está en 04 §2.2; solo hay que separarlo en 4
  cadenas.
- **C2**: `OutlinedTextField` con `shape = medium`, contenedor `surface` (blanco),
  `textStyle` 20 sp; la etiqueta queda en 16 sp gracias a `bodySmall`. «No es podran canviar
  després» como `InfoCallout` (`secondaryContainer`, `ic_info`, `bodyLarge`). Opcional (P2):
  vista previa del nombre en la miniatura del carnet («Així sortirà a la credencial»), que se
  actualiza al escribir y es decorativa para TalkBack.
- **C3**: `RuleRow` = insignia de 40 dp (`primaryContainer`; la regla de salir usa
  `tertiaryContainer`) + `bodyLarge`, `minHeight 56`. Iconos: `timer`, `quiz`, `flag`
  (umbral), `chevron_left` tachado → **depende de Q5**, `logout`. `NameConfirmCard`: `Surface`
  `surface`, forma `large`, padding 16; «Nom a la credencial» en `bodyMedium`
  `onSurfaceVariant`, nombre en `headlineSmall` y `TertiaryButton` «Canviar» con `ic_edit` a
  48 dp. Batería baja: `callout` `tertiaryContainer` + `ic_battery_alert`.

### 4.12 Mapa de preguntas (cuando se implemente volver atrás)

La decisión del usuario del 2026-11-20 permite volver a preguntas anteriores; la UI actual aún
no lo hace. Diseño visual para cuando se implemente (el flujo lo deciden `ux-designer` y
`android-developer`):

- **Acceso**: la etiqueta «Pregunta 3 de 20 ⌄» se convierte en un control de 48 dp
  (`expand_more`) que abre un `ModalBottomSheet` «Totes les preguntes».
- **Hoja**: título `headlineSmall`; leyenda (contestada / sense contestar / actual) con las
  mismas muestras; `LazyVerticalGrid(GridCells.Adaptive(64.dp))` (5 columnas a 411 dp, 3-4 al
  200 %); padding de 24.
- **`QuestionTile`**: mínimo 56×56, forma `medium`, número en 20/600.

| Estado | Fondo | Borde | Extra |
|---|---|---|---|
| Contestada | `primaryContainer` | — | punto de 8 dp `primary` abajo |
| Sin contestar | `surface` | 1,5 dp `outline` | — |
| Actual | (la de su estado) | **3 dp `onSurface`** | — |

  TalkBack: «Pregunta 3, contestada» / «…, sense contestar» / «…, actual». Rol `Button`.
- **Barra inferior**: `Row` con «Anterior» (secundario, `ic_chevron_left`, `weight(1f)`) y
  «Següent» (primario, `weight(1.6f)`). En la pregunta 1 «Anterior» no aparece. Si
  `fontScale >= 1.5`, se apilan con «Següent» arriba.
- **Barra de progreso** = contestadas/total.
- El texto que hoy niega volver atrás (Q5) tiene que cambiar antes de publicar este mapa.

---

## 5. Tokens listos para implementar

### 5.1 `Theme.kt` (esqueleto)

```kotlin
private val Light = lightColorScheme(
    primary = Color(0xFF1D6F42), onPrimary = Color.White,
    primaryContainer = Color(0xFFD7EEDF), onPrimaryContainer = Color(0xFF0A3A20),
    inversePrimary = Color(0xFF86D7A4),
    secondary = Color(0xFF3F5F4B), onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3E6D9), onSecondaryContainer = Color(0xFF0F2A1A),
    tertiary = Color(0xFF7A5900), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE08A), onTertiaryContainer = Color(0xFF2B1F00),
    background = Color(0xFFF2F5F3), onBackground = Color(0xFF18241D),
    surface = Color.White, onSurface = Color(0xFF18241D),
    surfaceVariant = Color(0xFFDFE7E1), onSurfaceVariant = Color(0xFF4A5750),
    surfaceTint = Color(0xFF1D6F42),
    inverseSurface = Color(0xFF2B332E), inverseOnSurface = Color(0xFFEEF2EF),
    error = Color(0xFFB3261E), onError = Color.White,
    errorContainer = Color(0xFFFBE3E0), onErrorContainer = Color(0xFF5C0E09),
    outline = Color(0xFF6B7770), outlineVariant = Color(0xFFC5CFC8), scrim = Color.Black,
    surfaceBright = Color.White, surfaceDim = Color(0xFFD8DFDA),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF7F9F7),
    surfaceContainer = Color(0xFFEEF2EF), surfaceContainerHigh = Color(0xFFE8EDE9),
    surfaceContainerHighest = Color(0xFFE1E8E3),
)
// Dark: valores de §2.3 con la misma estructura (los 36 roles, sin omitir ninguno).

object BrandColors {
    val Pitch = Color(0xFF0F3D26); val PitchLine = Color.White.copy(alpha = 0.16f)
    val Chalk = Color(0xFFF2F5F3); val RefCard = Color(0xFFF2C230)
    val Paper = Color.White; val PaperInk = Color(0xFF18241D); val PaperMuted = Color(0xFF4A5750)
    val WarnAmber = Color(0xFFFFE08A); val OnWarnAmber = Color(0xFF2B1F00)
}
```

Test recomendado (barato): un test de Robolectric que recorra **todos** los getters de
`ColorScheme` en los dos temas y falle si alguno coincide con `lightColorScheme()` o
`darkColorScheme()` sin argumentos. Así se blinda que no vuelva el lila.

### 5.2 Fuente

1. Descargar `AtkinsonHyperlegibleNext[wght].ttf` y `AtkinsonHyperlegibleMono[wght].ttf` de
   `github.com/google/fonts` (`ofl/atkinsonhyperlegiblenext`, `ofl/atkinsonhyperlegiblemono`)
   en la fase de implementación. Renombrarlos a `res/font/atkinson_next.ttf` y
   `res/font/atkinson_mono.ttf` (minúsculas, sin corchetes).
2. Copiar los `OFL.txt` a `app-candidate/src/main/assets/licenses/` y citarlos en «Sobre» (C13)
   cuando exista.
3. Compose (`minSdk 26` admite fuentes variables):

```kotlin
@OptIn(ExperimentalTextApi::class)
private fun next(w: Int) = Font(R.font.atkinson_next, FontWeight(w),
    variationSettings = FontVariation.Settings(FontVariation.weight(w)))
val Atkinson = FontFamily(next(400), next(500), next(600), next(700), next(800))
val AtkinsonMono = FontFamily(/* igual con R.font.atkinson_mono, 400 y 600 */)
```

4. **[PV]** Comprobar en un dispositivo los glifos catalanes: `à è é í ï ò ó ú ü ç`, el punt
   volat `l·l` (U+00B7) y `ŀ` (U+0140), además de `’` y `«»`.

### 5.3 Resumen de medidas

| Componente | Medidas |
|---|---|
| Margen de pantalla | 20 dp; entre bloques 24; entre tarjetas 12 |
| Botón primario/secundario | min 56 dp, radio 14, padding 20 h / 14 v, `labelLarge` 18/600 centrado; secundario con borde 1,5 dp `primary` |
| Botón terciario | min 48 dp, `labelLarge` `primary` |
| Barra superior | color `background`, título 22/600, icono de 24 dp en un objetivo de 48 |
| Barra inferior | padding 20 h / 12 v + insets, separación 8, divisor 1 dp condicional |
| Opción | min 64 (T/F 72), radio 14, padding 16/14, indicador 28, borde 1,5 → 3 |
| Temporizador | min 48, píldora, padding 14/10, icono 22, Mono 20/600 |
| Progreso | 8 dp, extremo redondeado, sin hueco ni punto final |
| Diálogo | radio 24, padding 24, botones apilados de 56 |
| Tarjetas | radio 20, padding 16, borde 1 dp `outlineVariant` |
| Credencial | radio 20, banda min 72, cuerpo 20, sombra 3 |
| Cabecera de inicio | radio 24, padding 24, min 168, márgenes 16 |
| Insignias | 40 dp de círculo, icono de 24 |
| Disco de resultado | 120 dp, aro de 8, aro interior de 1,5 a 10 dp |

---

## 6. Accesibilidad (comprobación del diseño)

- **Contraste:** todos los pares de texto de §2.2-2.3 calculados ≥ 4,5:1, y casi todos ≥ 7:1
  (AAA). La excepción es `primary` como texto sobre blanco (6,7:1, AA); si se quiere AAA en los
  enlaces, usar `#17633A` (7,3:1) solo como color de texto. Componentes: borde de opción
  `outline` 4,7:1; seleccionada, 3 dp `primary` 6,7:1. **[PV]** con Accessibility Scanner en
  los dos temas: los valores están calculados a mano.
- **Objetivos táctiles:** todos ≥ 48 dp; acciones ≥ 56; opciones ≥ 64.
- **200 %:** sin alturas fijas; cabecera de pregunta, cifras del resultado, campos de la
  credencial y botones del diálogo se reorganizan (§4.2, §4.4, §4.5, §4.8). Iconos junto a texto
  escalados. Repetir las capturas `18`/`20` y añadir: credencial, inicio y repaso al 200 %.
- **TalkBack:** no cambia nada de lo que ya funciona. Todo lo nuevo decorativo (cal, aro
  interior, marca en cabeceras) va sin semántica; el disco del resultado se fusiona en una sola
  descripción.
- **No solo color:** opción = forma del indicador + borde 3 dp + relleno; repaso = icono +
  etiqueta + color; temporizador = icono + color + avisos de texto.
- **Movimiento:** solo transiciones ≤ 150 ms y un único arco de 600 ms; todo a 0 si
  `Settings.Global.ANIMATOR_DURATION_SCALE == 0`.

---

## 7. Capturas que faltan para validar

Resultado suspendido real (C9; `17` es un diálogo); avisos de 5 y 1 min y temporizador oculto;
error al guardar; tiempo agotado (C6); errores del nombre; credencial y presentación en
oscuro; inicio con último suspendido y con pendientes; repaso con varias preguntas y con
explicación. Después de implementar los P0/P1, repetir el juego completo en claro, oscuro y al
200 %.

---

## 8. Preguntas abiertas (para el usuario / `ux-designer`)

| # | Pregunta | Propuesta por defecto |
|---|---|---|
| **Q1** | ¿Se cambia la fuente de Roboto (04 §8.1) a **Atkinson Hyperlegible Next/Mono** empaquetada (~230 KB, OFL, sin descargas)? | **Sí.** Hasta entonces se aplica todo lo demás con Roboto. |
| **Q2** | ¿Hay un **logo oficial de CFBB** en vectorial y con permiso de uso? ¿Cómo se desarrolla «CFBB» en la credencial? (La web base dice «Federació de Clubs de Futbol Botons».) | La xapa como icono de la app; el logo oficial, si existe, en la banda de la credencial y en el PDF. Subtítulo actual hasta que se confirme. |
| **Q3** | Detalle del repaso: ¿opciones resueltas en contexto (B, §4.7) en lugar de las dos secciones de 04 §2.9? Muestra también las opciones incorrectas no marcadas. | **B.** Si no, A. |
| **Q4** | C5-c: 04 se contradice (el texto dice «botón seguro = primario» y el *wireframe* pone `[ Finalitzar ]`). ¿Cuál va como primario? | **«Finalitzar»** primario: es la intención del usuario, que ya ha pulsado «Finalitzar l'examen». |
| **Q5** | **Textos que contradicen la decisión de volver atrás** (2026-11-20): `rules_noback`, `noanswer_body` («…i no podràs tornar-hi») y el modelo de C5-b/C5-c. Hay que reescribirlos al implementar la navegación (§4.12). Hoy la app afirma algo que ya no es política. | Pasarlo a `ux-designer` con la tarea de navegación. |
| Q6 | Cadenas nuevas o separadas: 4 viñetas de bienvenida, 3 etiquetas de las cifras de inicio, 4 etiquetas de campo de la credencial. | Aprobar en bloque con `ux-designer` (no cambia contenido). |
| Q7 | ¿La credencial se queda **clara en tema oscuro**? | Sí (documento, QR sobre blanco, igual que el PDF). |

---

## 9. Riesgos residuales

- **Contrastes calculados a mano** con la fórmula WCAG: verificar con herramienta (§6).
- **Glifos catalanes** de la fuente nueva sin comprobar en un dispositivo (§5.2.4).
- **Banda verde en la credencial**: un organizador con prisa podría leer «verde = válida» (T5).
  Lo mitigan el mismo verde en toda la marca, la ausencia de checks y sellos, y «Emesa en
  autoservei» dentro del documento. No queda eliminado.
- **Tamaño del APK**: +~230 KB de fuentes y +~25 KB de iconos vectoriales; asumible.
- **Sin prueba con usuarios mayores** (04 §8.3): las medidas son hipótesis razonadas hasta
  probarlas con 3-5 miembros del club.
