# Dirección visual «Paper i gespa»: revisión del look & feel de la app del candidato

**Autor:** `ui-designer`, en papel de consultor: propone, no implementa. **Fecha:** 2026-11-22.

**Base de la revisión:**
- Capturas ANTES en `docs/design/reviews/evidence/2026-11-22-ui-abans/`. Abiertas: 01, 02, 06, 11, 13, 20, 30, 31, 40, 42, 50, 50b, 51 y 60 en claro; 02, 11, 30, 42, 50b y 51 en oscuro; 02, 10 y 50 al 200 %.
- Código en `app-candidate/src/main/kotlin/dev/lluissunol/arbitratge/ui/*.kt` y `res/values*`.

**Marco:** se mantienen el símbolo, el nombre, Atkinson Hyperlegible Next/Mono, el flujo, ARB1, los ADR y el contrato del banco. Lo que se desvía de `docs/brand/ESPECIFICACIO-MARCA.md` va marcado **[DESV]** y se pregunta en §6.

**Evidencia DESPUÉS:** mismos nombres en `docs/design/reviews/evidence/<fecha>-ui-despres/`, generada con `scripts/qa/capture-ui.mjs --out`. Hay que añadir al flujo `escala` las capturas `11-…-200-clar` y `60-…-200-clar`, y lanzar `--scale 200 --theme fosc` para 02, 11, 30 y 50.

## 1. Diagnóstico: por qué no convence

En una frase: **es un kit de Material 3 bien ordenado con un logo encima.** La marca vive en una caja; lo demás son tarjetas blancas iguales sobre gris verdoso; aparece un azul que no es de la marca, y media pantalla queda vacía. Nada desentona, pero nada parece «el examen oficial d'àrbitre de la CFBB».

| # | Causa | Evidencia (captura · código) |
|---|---|---|
| D1 | **Kit de tarjetas de plantilla** (rasgo nº 4 de `frontend-design`). Cada dato es una tarjeta blanca de radio 20 con filete gris de 1 dp: cifras, estadísticas, último examen, filas. Como todo pesa igual, agrupar no comunica nada. | `02-…-100-clar` (3 tarjetas), `30` (2), `60` (2 + miniatura), `20`, `40` · `Components.kt:266-277`, `ScreensMain.kt:116-129`, `ScreensResult.kt:166-178`, `:358-382`, `ScreensOverview.kt:89-91` |
| D2 | **Acento roto.** La selección usa un azul `#2B5C8A` que no aparece en ningún otro sitio. La correcta no marcada sale amarilla (oliva o marrón en oscuro). Hay 5 tonos con significado donde debería haber un acento. | `11-…-100-clar`, `13`, `20`, `42-…-fosc` (`#5B4300`, `#8C1D18`) · `Theme.kt:109-116`, `ScreensResult.kt:466-470` |
| D3 | **Marca encerrada en una caja.** `HeroCard` flota con radio 28 y sus líneas de cal tachan el subtítulo («de Futbol»). Fuera del inicio, la app es un M3 neutro. | `01`, `02`, `60-…-100-clar` · `ScreensMain.kt:77-89`, `Components.kt:378-383` |
| D4 | **Vacíos de entre 1/3 y 1/2 de pantalla.** El contenido queda arriba y la acción, abajo, así que parece un *wireframe*. | `02-…-100-clar` (≈55 %), `01`, `30`, `61`, `11` |
| D5 | **Jerarquía plana y lavada.** El título de barra (22/600) es casi igual que el cuerpo (18). Hay etiquetas grises `#4A5750` por todas partes, también en «Pregunta 2 de 5», y un único ritmo de 12 dp. | `11-…-100-clar`, `20` (título pegado a la píldora), `40` · `Theme.kt:164-175`, `Components.kt:89-128` |
| D6 | **Resultado con aire de app de fitness.** Anillo gris con un arco mínimo, un 0 barrado que se lee «Ø» y 3 jerarquías de botón. | `31-…-100-clar`, `30-…-fosc` · `ScreensResult.kt:129-163` |
| D7 | **Iconos mezclados.** Unos rellenos (`ic_quiz`, `ic_flag`, `ic_check_box`) y otros de contorno (`ic_timer`, `ic_id_card`, `ic_menu_book`). Las normas tienen 8 iconos en círculos verdes y 1 en amarillo. El chip «Resposta múltiple» muestra una casilla **marcada** sin que haya nada seleccionado. | `06-…-100-clar`, `02`, `13` · `ScreensMain.kt:282-292`, `Components.kt:250-260`, `QuestionScreen.kt:348-367` |
| D8 | **Credencial con aspecto de componente, no de documento.** Las 2 columnas parten «reglament / sample-1» y falta el filete amarillo de la banda oficial (marca §8). La presentación usa el único botón de borde negro de toda la app. | `50-…-100-clar`, `51-…-fosc`, `50-…-200-clar` · `ScreensCredential.kt:79-131`, `:163-172` |
| D9 | **Oscuro = «casi negro y verde ácido»** (rasgo nº 2). Fondo `#0F1512`, botones menta `#86D7A4` y tarjetas `#151C18` que no se distinguen del fondo. | `02-…-100-fosc`, `30-…-fosc`, `11-…-fosc` · `Theme.kt:54-74` |
| D10 | **Al 200 % la barra fija se come el 25-30 % de la altura.** Tapa la 2.ª opción, la 3.ª cifra y el «Codi», y las cifras se convierten en 3 tarjetas gigantes. | `10-`, `02-`, `50-…-200-clar` · `Components.kt:104-118`, `ScreensMain.kt:148-153` |

**Lo que funciona y se queda:** Atkinson, el mínimo de 16 sp, los objetivos táctiles y el contraste del texto principal.

**El 0 barrado es propio de la fuente.** Revisé el GSUB de `atkinson_next.ttf` (`aalt, case, ccmp, frac, locl, ordn, pnum, sups, tnum`) y no ofrece un cero sin barra. Se acepta como rasgo de legibilidad; al quitar el anillo (I6) deja de leerse «Ø».

## 2. Dirección: «Paper i gespa»

El examen es un **acta**: papel con el sello del campo. Dos materiales y una tinta, cada color con un solo trabajo:

- **Gespa (`#0F3D26`): marca y acción.** Va a sangre como banda, nunca como tarjeta flotante, y es el color del botón primario. En la banda, el símbolo queda en el centro del campo: el cercle central y la línea de medio campo son concéntricos con el disco. Es el único ornamento.
- **Paper (`#FFFFFF`) y Cal (`#F2F5F3`): contenido.** El papel es el fondo; la cal, bloques sin borde con filetes. Solo llevan borde lo que se toca (opciones, filas) y los objetos (credencial, diálogos).
- **Tinta (`#18241D`): texto y «marca de bolígrafo».** Lo que el usuario marca se pinta en tinta: opción invertida y disco lleno. El azul desaparece y una opción marcada ya no se confunde con una correcta (UX F4).
- **Veredicto, solo en el repaso:** verde `#1D6F42` y rojo `#B3261E`, siempre con icono y texto.
- **Ámbar** solo para el aviso de tiempo. **Amarillo de marca** solo para el punto del símbolo y el filete de la credencial.
- **El botó como forma recurrente:** es el logo, el indicador de respuesta y el marcador del resultado.

**Por qué funciona para gente mayor:**
- Texto principal a 16:1 (AAA).
- Selección inequívoca: un bloque oscuro frente a uno blanco, sin depender del tono.
- Menos bordes que interpretar y títulos más grandes.
- Composición estable de una pantalla a otra.

**Pre-flight anti-slop (`ui-taste`) después del cambio:**
- Acentos: 1 (familia verde).
- Azules, degradados y emoji: 0.
- Radios: 12/16/20/28, más el círculo.
- Familia de iconos: 1 (contorno; el relleno se reserva al veredicto).

## 3. Tokens

Sustituyen a `Theme.kt:32-74`, `:100-147` y `:164-189`. Contrastes calculados con la fórmula WCAG 2.x (script Python, §5); falta medirlos en el dispositivo.

### 3.1 M3 claro

| Rol | Hex | Uso · contraste |
|---|---|---|
| `primary`/`onPrimary` | `#0F3D26`/`#FFFFFF` | Botón primario, progreso, foco, enlace-botón **[DESV-1]** · 12,2:1 |
| `primaryContainer`/`on…` | `#DCEBE1`/`#0F3D26` | «Ets aquí», aviso de credencial · 9,9:1 |
| `secondary`/`onSecondary` | `#1D6F42`/`#FFFFFF` | Iconos informativos · 6,2:1 sobre blanco |
| `secondaryContainer`/`on…` | `#E1EFE5`/`#0F3D26` | Reserva (deja de usarse en avisos) |
| `tertiary`/`onTertiary` | `#7A5900`/`#FFFFFF` | — |
| `tertiaryContainer`/`on…` | `#FFE08A`/`#2B1F00` | Aviso de 5 min y aviso «si surts» · 12,5:1 |
| `background`, `surface`/`on…` | `#FFFFFF`/`#18241D` | Papel y tinta · 16,0:1 |
| `surfaceVariant`/`onSurfaceVariant` | `#E3EAE5`/`#3F4B44` | Texto secundario (antes `#4A5750`) · 9,1:1 sobre blanco, 8,3:1 sobre cal |
| `surfaceContainerLowest`/`Low`/`—` | `#FFFFFF`/`#F7F9F7`/`#F2F5F3` | `surfaceContainer` = cal: `FactStrip`, «Per què?», pie de la credencial |
| `surfaceContainerHigh`/`Highest` | `#E9EEEA`/`#DEE5E0` | Reloj en reposo / pista de progreso (gespa sobre pista 9,6:1) |
| `surfaceBright`/`surfaceDim` | `#FFFFFF`/`#DCE3DE` | — |
| `outline`/`outlineVariant` | `#66736B`/`#D3DCD5` | Borde de lo tocable (5,0:1 sobre blanco, 4,5:1 sobre cal) / filetes decorativos |
| `error`/`onError` | `#B3261E`/`#FFFFFF` | Reloj a 1 min · 6,5:1 |
| `errorContainer`/`on…` | `#FCE4E1`/`#5C0E09` | Incorrecta marcada, «Fallada» · 11,4:1 |
| `inverseSurface`/`inverseOnSurface`/`inversePrimary` | `#18241D`/`#F2F5F3`/`#A6DDB8` | Snackbar |
| `surfaceTint`/`scrim` | `#0F3D26`/`#000000` | Sin elevación tonal; *scrim* α 0,6 |

### 3.2 M3 oscuro «nit de camp»

El fondo es verde noche, no negro, y los botones son de cal, no menta.

| Rol | Hex | Contraste |
|---|---|---|
| `primary`/`onPrimary` | `#E3F1E7`/`#0F3D26` | 10,5:1 |
| `primaryContainer`, `secondaryContainer`/`on…` | `#1B4A31`/`#D5EEDD` | 8,3:1 |
| `secondary`/`onSecondary` | `#9FD8B1`/`#0A3A20` | 11,0:1 sobre el fondo |
| `tertiary`/`tertiaryContainer`/`onTertiaryContainer` | `#F0C64A`/`#FFE08A`/`#2B1F00` | El ámbar es el mismo en los dos temas |
| `background`/`onBackground` | `#0C1A12`/`#E9F0EA` | 15,5:1 |
| `surface`/`onSurface` | `#12241A`/`#E9F0EA` | 14,0:1 |
| `surfaceVariant`/`onSurfaceVariant` | `#223C2D`/`#BCCBC0` | 10,6:1 sobre el fondo, 8,0:1 sobre `High` |
| `surfaceContainerLowest`/`Low`/`—`/`High`/`Highest` | `#08130D`/`#0F2017`/`#152A1E`/`#1B3325`/`#223C2D` | Tinta sobre `High`: 11,7:1 |
| `surfaceBright`/`surfaceDim` | `#2A4535`/`#0C1A12` | — |
| `outline`/`outlineVariant` | `#8DA194`/`#2C4536` | 6,5:1 sobre el fondo, 5,9:1 sobre `surface` |
| `error`/`onError` · `errorContainer`/`on…` | `#FFB4AB`/`#690005` · `#4E1A16`/`#FFDAD6` | 9,6:1 · 11,0:1 |
| `inverseSurface`/`inverseOnSurface`/`inversePrimary` | `#E9F0EA`/`#18241D`/`#0F3D26` | — |

### 3.3 Tokens propios

En `ArbColors` (`Theme.kt:100-116`), `selection*` pasa a llamarse `mark*` y se añaden `wrong*`.

| Token | Claro | Oscuro |
|---|---|---|
| `mark`/`onMark` | `#18241D`/`#FFFFFF` (16:1) | `#E9F0EA`/`#0C1A12` (15,5:1) |
| `correct`/`correctContainer`/`onCorrectContainer` | `#1D6F42`/`#DDF0E3`/`#0A3A20` (10,8:1; borde 5,2:1) | `#8FD6A6`/`#163F27`/`#CFEFDA` (9,6:1; borde 7,0:1) |
| `wrong*` | = `error`/`errorContainer`/`onErrorContainer` | ídem |

En `BrandColors` (`Theme.kt:80-96`):

| Token | Valor | Notas |
|---|---|---|
| `Pitch` | `#0F3D26` | — |
| `Chalk` | `#FFFFFF` | Líneas a α 0,18, decorativas, 1,7:1 |
| `PitchSubtitle` | `#E2E8E5` | 9,9:1 |
| `RefCard` | `#F2C230` | Solo punto y filete |
| `PaperMuted` | `#3F4B44` | — |
| `WarnAmber` | igual | — |

Se eliminan `PitchLine` y el azul.

### 3.4 Tipografía (Next; Mono solo para el código y mm:ss)

| Rol | sp / interlínea / peso | Uso |
|---|---|---|
| `displayLarge` | 56/64/700 `tnum` | Número del marcador |
| `headlineLarge` | 34/40/800 | «Arbitratge» en la banda |
| `headlineMedium` | 30/38/700 | Veredicto |
| `headlineSmall` | 28/36/700 | **Título de pantalla en el contenido**, nombre en la credencial, valor de `FactStrip` |
| `titleLarge` | 24/32/700 | Diálogo, banda de la credencial |
| `titleMedium` | 20/28/600 | Valores de la credencial, campo de nombre |
| `titleSmall` | 18/26/700 | Titular de fila, «Per què?» |
| `bodyLarge` | 18/28/400 | Cuerpo |
| `bodyMedium`/`bodySmall` | 16/24/400 | Secundario, en `#3F4B44` |
| `labelLarge` | 18/24/**700** | Botones |
| `labelMedium`/`labelSmall` | 16/22/600 | Metadatos, chips |

En `ArbText`: `question` 24/34/600; `option` 20/28/400 (**600 cuando está marcada**); `optionTf` 22/30/600; `progressLabel` 18/24/**700 en tinta**; `timer` y `code` no cambian. Ningún rol baja de 16 sp.

### 3.5 Formas, espacio y medidas (*shape lock*)

| Elemento | Forma |
|---|---|
| Controles (botones, opciones, campo, filas tocables) | Radio 12 |
| Bloques de cal | 20 |
| Diálogos | 28 |
| Credencial | 16 (un documento tiene esquinas pequeñas) |
| Bandas | 0 |
| Reloj y chips de estado | Píldora (solo estos) |
| Discos | Círculo |

`Shapes(extraSmall 8, small 12, medium 12, large 20, extraLarge 28)`.

**Ritmo vertical:** 8 dentro de un componente, **16** dentro de un grupo (nuevo valor por defecto de `ScreenScaffold.spacing`) y **32** entre grupos. Margen lateral: 20.

**Mínimos** (todos con `toScaledDp`): táctil 48 · botón 56 · opción 64 (V/F 80) · fila 72 · indicador 28 · icono 24 (estado 28).

### 3.6 Iconografía

- Una sola familia: *Material Symbols Outlined*, peso 400, 24 dp. El relleno se reserva al veredicto (`check_circle`, `cancel`).
- Sustituciones:
  - `ic_quiz` → `help`;
  - `ic_flag` → `outlined_flag`;
  - `ic_check_box` → `checklist` en las normas y `check_box_outline_blank` en el chip;
  - los `Icons.Default.*` informativos → `Icons.Outlined.*`.
- Color: `secondary` para los informativos, `onSurfaceVariant` para los de navegación.
- Sin círculo de fondo en las listas (fuera `IconBadge`).

## 4. Intervenciones por impacto y esfuerzo

| # | Intervención | Impacto/esfuerzo |
|---|---|---|
| I1 | Tokens y tema oscuro | Alto/S |
| I2 | Selección en tinta | Alto/S |
| I3 | Banda «Centre del camp» | Alto/M |
| I4 | Fuera el kit de tarjetas | Alto/M |
| I5 | Títulos grandes | Medio/S |
| I6 | Resultado y marcador | Alto/M |
| I7 | Repaso en dos canales | Medio/S |
| I8 | Credencial-documento | Alto/M |
| I9 | Botones y barra al 200 % | Medio/S |
| I10 | Iconografía | Bajo/S |

Orden de implementación: I1 → I2 → I5 → I10 → I3 → I4 → I7 → I6 → I8 → I9.

**I1 · Tokens y tema oscuro «nit de camp».** Ficheros: `Theme.kt:32-189` y `res/values*/arbitratge_colors.xml` (las barras del sistema y el *splash* usan `background`).
- Aplicar §3.
- Actualizar el test `allRoles()`.
- Diálogos (`Components.kt:322-325`): `surfaceContainerLowest` en claro, `surfaceContainerHigh` en oscuro; radio 28; *scrim* α 0,6.

*Aceptación:*
- `…-despres/02-…-100-fosc`: fondo `#0C1A12` (cuentagotas ±2) y botón primario `#E3F1E7` con texto en gespa; sin rastro de `#86D7A4`.
- `11-…-100-clar`: fondo `#FFFFFF` y «Respostes: …» en `#3F4B44`.

**I2 · Selección en tinta («marca de bolígrafo»).** Ficheros: `QuestionScreen.kt:348-428` y `ScreensOverview.kt:89-110`.
- **`OptionCard`:**
  - sin marcar: `surface`, borde 1,5 dp `outline`, texto `onSurface` 400;
  - **marcada:** fondo `mark`, borde 3 dp `mark`, texto `onMark` **600**.
  - El borde se dibuja hacia dentro, así que la tarjeta no salta de tamaño. El color cambia con `tween(120)`, o al instante si se reducen las animaciones. Con `saveError`, α 0,38. La semántica no cambia.
- **`Indicator`** (28 dp):
  - sin marcar: aro de 2,5 dp `outline`;
  - radio marcado: aro `onMark` con punto de 14 dp;
  - casilla marcada: cuadrado de radio 6 relleno `onMark`, con ✓ de 20 dp en `mark`.
- **`MultiChip`:** transparente, borde 1,5 dp `onSurface`, icono `check_box_outline_blank`.
- **Resumen:** «contestada» es un disco `mark` de 28 dp y «sense resposta», un aro `outline`. «Ets aquí» pasa a un borde de 3 dp en `primary` con píldora `primaryContainer` (sustituye al borde negro).

*Aceptación:*
- `…-despres/11` y `13-…-100-clar`: la opción marcada es un bloque `#18241D` con texto blanco; **ningún píxel** `#2B5C8A`/`#E3EEF9`.
- `11-…-fosc`: bloque `#E9F0EA`.
- `20`: discos en tinta y fila actual con borde en gespa.
- `12`: la casilla del chip, vacía.
- `11-…-200-clar` (captura nueva): la opción marcada se lee entera.

**I3 · Banda «Centre del camp» (sustituye a `HeroCard`).** Ficheros: `ScreensMain.kt:71-112` y `:188-200`; en `Components.kt:378-383`, `pitchLines` se sustituye por `centreLines(markCenter, markRadius)`.

`BrandBand(title, subtitle, minHeightFraction)`:
- **Caja:** a todo el ancho, sin esquinas y por detrás de la barra de estado. Mientras está a la vista, los iconos de esa barra van en claro (`isAppearanceLightStatusBars = false`) y se restauran al salir. Fondo `Pitch`; padding: arriba *inset* + 24, abajo 28, a los lados 20.
- **Composición con `fontScale < 1,5`:** `Row` con `BrandMark` de 64 dp (`onPitch`), 28 dp de separación y una columna con «Arbitratge» (`headlineLarge`, en blanco) y el subtítulo (`bodyLarge`, `#E2E8E5`).
- **`centreLines`:** cercle central de r 44 dp concéntrico con el símbolo y línea de medio campo vertical que pasa por su centro, en dos tramos. Color cal α 0,18, trazo 1,5 dp, sin semántica. Ninguna línea cruza el texto: el círculo termina en x = 96 y el texto empieza en x = 112.
- **Con `fontScale ≥ 1,5`:** columna (símbolo, 16 dp, texto) y solo el tramo superior de la línea.
- **Altura:** en el inicio, con `fontScale < 1,3`, mínimo el 34 % de la ventana y contenido centrado; así el vacío de D4 se llena de marca. Por encima de 1,3, la altura la marca el contenido. En la bienvenida no hay mínimo.
- **Tema oscuro:** filete inferior de 1 dp en cal α 0,24, porque la banda sobre el fondo solo da 1,46:1.

*Aceptación:*
- `…-despres/01`, `02` y `60` (claro y oscuro): banda de borde a borde y barra de estado verde con iconos blancos; con zoom, ninguna línea toca una letra.
- `02-…-200-clar`: símbolo arriba y «Arbitratge» sin partir.

**I4 · Fuera el kit de tarjetas.** Ficheros: `ScreensMain.kt:92-162`, `:211-218` y `:282-330`; `ScreensResult.kt:166-178` y `:358-382`; `ScreensOverview.kt:89-91`; `Components.kt:250-277`.
- **`FactStrip(cells)`** sustituye a `FactTiles` y `StatTile`.
  - Forma: un solo bloque `surfaceContainer`, radio 20, sin borde. Celdas `weight(1f)` dentro de `Row(IntrinsicSize.Max)`, con padding de 16 y un divisor vertical de 1 dp `outlineVariant` entre ellas.
  - Texto: valor en `headlineSmall` 700 `tnum`; etiqueta en `bodyMedium` `onSurfaceVariant`, hasta 2 líneas y sin elipsis.
  - Inicio: siempre las cadenas `*_full` ya existentes («preguntes», «minuts», «encerts per aprovar»), sin «preg.» ni iconos.
  - Resultado: «Fallades» (con «1 sense resposta» debajo) y «Temps».
  - Con `fontScale ≥ 1,3`: filas con el valor a la izquierda (mínimo 56 dp), la etiqueta a la derecha y divisores horizontales. La semántica fusionada no cambia.
- **`ActionRow` (último examen):**
  - Contenedor: toda la fila tocable, 72 dp, `surface`, borde 1,5 dp `outline`, radio 12, `Role.Button`.
  - Contenido: icono `menu_book` en `secondary`, titular «Repassar l'últim examen», soporte `last_passed`/`last_failed` y chevron.
- **Listas del repaso y el resumen:** se queda el borde, ahora de radio 12. El icono de estado mide 28 dp, sin círculo: ✕ `wrong`, ✓ `correct`, – `onSurfaceVariant`.
- **Bienvenida y normas:**
  - Iconos: fuera `IconBadge`; icono de contorno de 24 dp en `secondary`, alineado con la primera línea. 16 dp de separación hasta el texto (en tinta) y 16 dp entre filas.
  - «Si surts de l'app…» sale de la lista y pasa a un `Callout` `tertiaryContainer` con `warning`; `collectionInfo` pasa a (7,1).
  - «Nom a la credencial»: borde 1,5 dp, radio 12, el nombre en `titleLarge` y «Canviar» en la misma fila.

*Aceptación:*
- `…-despres/02-…-100-clar`: un bloque de cal con 3 columnas y 2 filetes, sin abreviaturas.
- `02-…-200-clar`: 3 filas visibles, sin barra encima.
- `06`: sin círculos; aviso ámbar aparte.
- `60`: fila con chevron.
- `40`: iconos sin círculo.

**I5 · Títulos grandes y jerarquía.** Fichero: `Components.kt:135-163`.
- **Barra (56 dp):** solo el botón de volver (48 dp) y, en el resumen, el reloj.
- **Título:** primer elemento del contenido, en `headlineSmall` en tinta, con `heading()`, 8 dp arriba y 16 abajo. Se desplaza con el contenido, así que al 200 % no ocupa espacio fijo. Pantallas: nombre, normas, resumen, repaso y credencial.
- **Resultado:** el veredicto sustituye a «Resultat» como encabezado (a confirmar por `ux-designer`).
- **Pregunta:** «Pregunta N de M» en `progressLabel`, en tinta.

*Aceptación:*
- `06-…-100-clar`: título de 28 sp bajo la flecha.
- `20`: la píldora «4 min» en la barra y el título debajo, sin tocarse.
- `11`: «Pregunta 2 de 5» en ≈`#18241D`.

**I6 · Resultado: veredicto y marcador de botons (sustituye al anillo).** Fichero: `ScreensResult.kt:129-270`.
- **Bloque de veredicto:** a sangre como en I3, pero sin símbolo ni líneas.
  - Fondo: aprobado, `Pitch` con texto blanco; suspendido, `surfaceContainer` con texto en tinta y **sin rojo** (04 §2.8).
  - Contenido, en este orden:
    1. el veredicto en `headlineMedium`, con `heading()` y `liveRegion` *polite*;
    2. el número en `displayLarge`, seguido de «de 5» (`score_of`) y «encerts» (`home_fact_pass`);
    3. el marcador;
    4. «Mínim per aprovar: 3».
- **Marcador:**
  - Disposición: `FlowRow` con N discos de 24 dp (máximo 32 al escalar), separados 8 dp, como mucho 10 por fila. Primero van los aciertos; el detalle de cada pregunta queda para el repaso.
  - Discos: acierto, disco lleno (blanco sobre gespa, o gespa sobre cal: 11:1); fallo o sin respuesta, aro de 2 dp (blanco α 0,7 sobre gespa, o `outline`).
  - Mínimo: una línea vertical de 2 × 32 dp después del disco `threshold`.
  - Accesibilidad y movimiento: decorativo (`clearAndSetSemantics`); se mantiene `score_a11y`. Los discos se llenan uno a uno, 60 ms cada uno y 600 ms en total como máximo; quieto si se reducen las animaciones.
- **Debajo:** `FactStrip` con Fallades y Temps, el aviso de la credencial en `primaryContainer` o, si suspende, «Repassa…» sobre `surfaceContainer`. Los botones no cambian (son flujo); llevan los estilos de I9.

*Aceptación:*
- `…-despres/30-…-100-clar`: banda verde con «Has aprovat», «5 de 5 encerts» y 5 discos blancos; sin anillo; menos de un 15 % de hueco.
- `31`: bloque de cal con 5 aros y una línea después del 3.º, sin rojo.
- Oscuro: equivalente.
- 200 %: el aviso no queda tapado.

**I7 · Repaso en dos canales, sin amarillo.** Fichero: `ScreensResult.kt:405-499`. Dos canales independientes:
- **Lo que marcaste:** a la izquierda, el mismo indicador del examen (lleno en `mark` o aro `outline`).
- **El veredicto:** contenedor, icono relleno de 28 dp al final y la etiqueta existente.

| Caso | Contenedor y borde | Icono | Etiqueta |
|---|---|---|---|
| Correcta, marcada | `correctContainer`, 2 dp `correct` | `check_circle` | `onCorrectContainer` |
| Correcta, no marcada | `surface`, 2 dp **discontinuo** `correct` (8/6) | `check_circle` | **700** `correct` (6,2:1) |
| Incorrecta, marcada | `wrongContainer`, 2 dp `wrong` | `cancel` | — |
| Incorrecta, no marcada | `surface`, 1 dp `outlineVariant` | — | `onSurfaceVariant` |

- **«La teva resposta no era la correcta» pasa a neutro:** `surfaceContainer` con `info` en tinta. Hoy es verde y parece un acierto (`42-…-100-clar`).
- **«Per què?»:** bloque `surfaceContainer` de radio 12 con un **filete izquierdo de 4 dp `primary`**, como una cita del reglamento.
- **Chip «Fallada»:** `wrongContainer`.

*Aceptación:*
- `…-despres/42` (claro y oscuro): sin amarillo ni oliva; la opción marcada lleva el disco de tinta; la correcta no marcada sale en blanco (`#12241A` en oscuro) con borde discontinuo verde; «Per què?» con filete verde.
- `44`: la correcta marcada sobre verde claro, con disco de tinta.

**I8 · Credencial como documento, y modo presentación.** Ficheros: `ScreensCredential.kt:47-175` y la miniatura en `ScreensMain.kt:166-185`.
- **Documento:** blanco en los dos temas (Q7), radio 16. En claro, borde de 1 dp `#C5CFC8` y elevación 2; en oscuro, sin borde ni elevación.
- **Banda:** `Pitch`, mínimo 88 dp, con un **filete `RefCard` de 4 dp** abajo. Es la proporción de la banda A4 de la marca (§8), un uso permitido. Lleva `BrandMark` de 48 dp con `centreLines`, «Credencial d'aptitud» en `titleLarge` blanco y `cred_subtitle` en `#E2E8E5`. Con `fontScale ≥ 1,5`, el símbolo va encima.
- **Cuerpo:**
  - Nombre en `headlineSmall`.
  - Formulario de **una sola columna**: filas etiqueta | valor separadas por filetes `outlineVariant`.
    - Por debajo de 1,3: etiqueta en `bodyMedium` `#3F4B44` a la izquierda y valor en `titleMedium` alineado al final, sin `maxLines`.
    - A partir de 1,3: la etiqueta encima del valor.
  - Código en Mono; el QR no cambia; pie `PaperFoot` en `#3F4B44`.
- **Miniatura:** filete de 3 dp, símbolo de 28 dp.
- **Presentación (`51`):** página blanca con la misma banda y filete arriba, a sangre. QR al 90 %, nombre en `headlineSmall` y textos en `#3F4B44` (en vez de `#3D3D3D`). «Tancar» pasa a `SecondaryButton` con borde gespa e icono `close`, y desaparece el borde negro.

*Aceptación:*
- `…-despres/50-…-100-clar`: filete amarillo visible, una sola columna y «reglament sample-1» en una línea.
- `50b-…-fosc`: documento blanco sobre verde noche.
- `50-…-200-clar`: símbolo encima y ningún campo tapado.
- `51` (claro y oscuro): banda verde y «Tancar» con borde gespa.

**I9 · Botones y barra inferior.** Ficheros: `Components.kt:89-128` y `:166-210`; `QuestionScreen.kt:167-183`.
- **Botones:**
  - Primario: `primary`/`onPrimary`, radio 12, mínimo 56 dp, `labelLarge` 700, padding 20×14.
  - Secundario: borde de **2 dp** `primary`.
  - Terciario: texto `primary` 700, mínimo 48 dp.
  - Secundario desactivado: borde y texto `onSurface` α 0,38 (hoy «Anterior» casi no se ve en `11-…-fosc`).
- **Barra al 200 %:** con `fontScale ≥ 1,5`, en bienvenida, inicio, normas, resultado y credencial, las acciones bajan al **final del contenido**, con 32 dp por encima. Se hace con un `ScreenScaffold(pinBottom = false)` nuevo.
  - Siguen fijas en la pregunta, el resumen, el detalle del repaso y el nombre (por el teclado).
  - En la pregunta, el padding vertical baja a 10 dp.
  - Lo valida `ux-designer`, porque el botón principal queda por debajo del pliegue.

*Aceptación:*
- `…-despres/02`, `30` y `50-…-200-clar`: sin barra fija y con los botones en `…b-…-final-200-clar`.
- `10-…-200`: la 2.ª opción se ve más que en ANTES.

**I10 · Iconografía unificada.** Se sustituyen `res/drawable/ic_quiz.xml`, `ic_flag.xml` e `ic_check_box*.xml` por *Material Symbols Outlined* (Apache 2.0), y los `Icons.Default.*` informativos por sus versiones de contorno.

*Aceptación:* en `…-despres/01`, `02`, `06` y `13`, todos los iconos de contorno con el mismo trazo; solo `42`/`44` llevan iconos rellenos (✓ y ✕).

**Accesibilidad del diseño resultante:**

| Aspecto | Valor |
|---|---|
| Texto principal | 16,0:1 en claro y 15,5:1 en oscuro (AAA) |
| Texto secundario | ≥ 8:1 |
| Bordes de control | ≥ 4,5:1 |
| Selección | No depende del color: relleno + peso 600 + indicador lleno + rol `selected` |
| Veredicto | Icono + texto + color |
| Objetivos táctiles | ≥ 48 dp (opciones ≥ 64 dp) |
| 200 % | Cubierto en I3, I4, I8 e I9 |
| TalkBack | Semántica sin cambios **[PV]** |

## 5. Consultas registradas

**`ui-ux-pro-max`.** Leídos `SKILL.md` y `references/pro-rules.md`. Las búsquedas se lanzan desde la raíz con `python .agents/skills/ui-ux-pro-max/scripts/search.py …`.

| Consulta | Resultado | Aplicación |
|---|---|---|
| `"official exam government accessible elderly" --domain style` | *Accessible & Ethical*, *Inclusive Design* (7:1, símbolos además de color) | AAA y doble codificación (I2, I7) |
| `"civic official document trustworthy green" --domain color` | Paletas de gobierno en azul | **Descarto** el color (la marca fija el verde); me quedo con la idea de «gris de documento + un acento» |
| `"sports club editorial" --domain typography` | Barlow Condensed / Newsreader | **Descartado:** se queda Atkinson |
| `"quiz answer option selected state" --domain ux` | Fuera de tema | Reintento: `"color not only indicator"` → *Color Only* (High) → I2, I7 |
| `"sticky footer obscures content" --domain ux` | *Sticky Navigation* | I9 |
| `"dynamic type large text scaling" --domain ux` | Fuera de tema | Reintento: `"text resize zoom 200%"` → *Text Reflow* (Critical) → I3, I4, I8, I9 |
| `"theme color scheme dark mode tokens" --stack jetpack-compose` | `colorScheme` y tokens centralizados | §3 |
| `"typography font scale" --stack jetpack-compose` | `MaterialTheme.typography` | §3.4 |
| `"referee exam app for older adults official credential" --design-system -p Arbitratge` | Estilo *Swiss* + azul/naranja + Outfit | **Descarto** color y fuente (genéricos); me quedo con el principio *Swiss*: rejilla, filetes y espacio |

Del checklist de `pro-rules` se aplican: un estilo de icono por nivel, tamaños de icono como tokens, el tema oscuro comprobado aparte, el *scrim* medido y barras que no tapan contenido.

**Otras skills:**
- **`ui-taste`** (sin generar imágenes):
  - `anti-slop-craft.md`: un acento, una familia de grises, *shape lock*, *pre-flight*.
  - `polish.md`: primero los estados que confunden (I2, I7) y se conserva lo que funciona.
  - `distill.md`: quitar contenedores (I4).
  - `mobile-controls.md`: controles nativos con su rol.
- **`mobile-android-design`:** roles M3 completos, `Shapes` y `FlowRow`.
- **`frontend-design`:** detectados los rasgos de plantilla nº 4 (D1) y nº 2 (D9). Un solo elemento memorable: la banda, que se repite en el inicio, el resultado y la credencial.

## 6. Preguntas para el usuario

| # | Pregunta | Propuesta por defecto |
|---|---|---|
| P1 **[DESV-1]** | La marca reserva la gespa `#0F3D26` para fondo, disco y banda. ¿Puede ser también el **relleno del botón primario**? `#1D6F42` pasaría a iconos y textos secundarios. | **Sí:** es más oficial, da 12,2:1 y une marca y acción. Si no, `primary` = `#1D6F42` y el resto sigue igual. |
| P2 **[DESV-2]** | ¿Filete amarillo también **bajo la banda del inicio**? La marca lo reserva a la credencial. | **No:** que siga siendo la firma de la credencial. |
| P3 | ¿Selección **invertida en tinta**, o la variante suave (blanco, borde de tinta de 3 dp y disco lleno)? | **Invertida**, a validar con 3-5 personas mayores (04 §8.3). Pasar a la suave cambia solo 2 tokens. |
| P4 | ¿Se cambia el **anillo** del resultado (aprobado en `visual-review-app-m1.md` §4.5) por el marcador de botons? | **Sí.** |
| P5 | ¿Fondo claro de **papel blanco**? Hoy es cal `#F2F5F3`. | **Sí**, con la cal en los bloques. Si deslumbra, `#F7F9F7`. |
| P6 (`ux-designer`) | (a) ¿Quitar el título «Resultat»? (b) ¿Acciones no fijas al 200 % fuera del examen? | Sí a las dos. |

**Cadenas:** no hace falta ninguna nueva. Se reutilizan `*_full`, `score_of`, `home_fact_pass`, `q_state_here` y `btn_review_last`.

**Riesgos residuales:**
- Los contrastes están calculados, no medidos en pantalla.
- Ni la selección invertida ni el marcador con más de 30 discos se han probado con usuarios.
- Falta revisar en el emulador cómo queda la barra de estado sobre la banda.
- La identidad es provisional hasta que la apruebe la CFBB.
- TalkBack **[PV]**.
