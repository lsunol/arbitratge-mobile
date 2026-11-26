# Spec visual: nuevo estilo de los mockups de Claude Design (UI-05) — `ui-designer`, 2026-11-24

Consultoría previa (paso 1 de `AGENTS.md`). **No implementa.** Orden de prioridad: `decisiones-usuario-ui.md`
(**UI-01..UI-04 mandan sobre UI-05**) → `ui-density-2026-11-23/spec-acceptada.md` → esta spec. Fuentes leídas:
los 3 mockups enteros (`new-designs/*/UI-*.dc.html`, valores de los `style=""`), `Theme.kt`, `Components.kt`,
`BrandComponents.kt`, `QuestionScreen.kt`, `ScreensMain.kt`, `ScreensResult.kt`, `ScreensCredential.kt`, `ScreensOverview.kt`,
`docs/brand/ESPECIFICACIO-MARCA.md` §3. **No hay capturas reales** en esta consulta: las medidas se han calculado con las
métricas de `atkinson_next.ttf` (fontTools) y deben comprobarse con capturas en la QA posterior.

**Dirección en una frase:** «Centre del camp» en luz de día. El fondo pasa a cal verdosa `#F3F6F2`, todo lo que se
toca o se lee como bloque es una **tarjeta blanca sin borde con sombra verde muy difusa**, las acciones son **píldoras** y
la gespa `#0F3D26` queda para lo que pesa: el botón principal, el hero de Inici y la banda de la credencial. Hay un solo
acento: el verde `#1D6F42`, que se usa en la selección, el progreso y los iconos. El ámbar solo aparece en los avisos.

---

## 1. Tokens Compose

### 1.1 Colores: cambios en `LightScheme` / `DarkScheme` (`Theme.kt:31-75`)

| Rol M3 | Claro actual → **nuevo** | Oscuro (sin cambios salvo que se indique) | Por qué |
|---|---|---|---|
| `background` | `#FFFFFF` → **`#F3F6F2`** | `#0C1A12` (igual) | El fondo del mockup. Sin él, la tarjeta blanca no se ve. |
| `onBackground` / `onSurface` | `#18241D` (igual) | `#E9F0EA` | 14,7:1 sobre `#F3F6F2` (AAA). |
| `onSurfaceVariant` | `#3F4B44` → **`#4A5A50`** | `#BCCBC0` | Texto secundario del mockup: 6,72:1 sobre el fondo y 7,32:1 sobre blanco (AA; AAA sobre blanco). |
| `primaryContainer` | `#DCEBE1` → **`#E4F0E8`** | `#1B4A31` | Recuadro de icono y píldora del reloj (mockup). `#0F3D26` sobre él: 10,45:1. |
| `surface` | `#FFFFFF` (igual) | `#12241A` → **`#152A1E`** | En oscuro la tarjeta tiene que separarse del fondo por tono, porque la sombra no se ve. |
| resto | igual | igual | `primary #0F3D26`, `secondary #1D6F42`, `tertiaryContainer`, `error*`, `outline #66736B`, `outlineVariant #D3DCD5`. |

### 1.2 `ArbColors`: tokens nuevos o modificados (`Theme.kt:95-132`)

| Token | Claro | Oscuro | Uso |
|---|---|---|---|
| `card` *(nuevo)* | `#FFFFFF` | `#152A1E` | Fondo de toda tarjeta (opción, hero blanco, resultado, filas tocables). |
| `cardBorder` *(nuevo)* | `Color.Transparent` | `#2C4536` (1 dp) | En oscuro no hay sombra: el filete de 1 dp sustituye la separación. |
| `tile` *(nuevo)* | `#F3F6F2` (= fondo) | `#0C1A12` (= fondo) | Recuadros de dato dentro de una tarjeta blanca (mockup Resultat). |
| `iconTile` / `onIconTile` *(nuevo)* | `#E4F0E8` / `#1D6F42` | `#1B4A31` / `#9FD8B1` | Recuadro de icono de 44 dp. Contraste: 5,27:1 en claro y 6,25:1 en oscuro. |
| `select` / `onSelect` *(nuevo)* | `#1D6F42` / `#FFFFFF` | `#9FD8B1` / `#0A3A20` | Borde e indicador de la opción marcada **en el examen**. 5,48:1 sobre `selectContainer`; en oscuro, 7,54:1. |
| `selectContainer` *(nuevo)* | `#EAF4EE` | `#1E3B2A` | Velo de la opción marcada. La tinta sobre él da 14,3:1. |
| `optionRing` *(nuevo)* | **`#738A7C`** | `#7E978A` | Anillo o casilla sin marcar. El `#8AA394` del mockup da 2,71:1 y **no llega al 3:1 de WCAG 1.4.11**; `#738A7C` da 3,71:1. |
| `progressDone` / `progressTodo` *(nuevo)* | `#1D6F42` / `#CFDCD3` | `#9FD8B1` / `#2C4536` | Progreso segmentado (§3.2). |
| `buttonOutline` *(nuevo)* | `#B9C9BE` | `#3A5747` | Borde de 1,5 dp de «Anterior» y de los botones secundarios (mockup). |
| `heroLine` *(nuevo)* | `#1D6F42` | `#1D6F42` | Líneas de camp del hero (decorativas, 1,7:1 a propósito). |
| `shadowTint` *(nuevo)* | `#0F3D26` | — | Color de las dos sombras (§3.1). |
| `mark`, `markContainer` | igual | igual | **Solo en el repaso** (canal «lo que marcaste»), para no confundirlo con `correct` (ver Q5). |
| `optionOutline` | se deja de usar en `OptionCard` | — | Si queda sin uso, se borra. |
| `warnBadge` / `onWarnBadge` | `#F5C443` / `#2B1F00` (**igual, UI-02**) | igual | 9,9:1. **No** se adopta `#FFF4CC` del mockup (§2.2). |

`BrandColors`: `PaperChip` `#DCEBE1` → `#E4F0E8`. Los demás no cambian; el carnet sigue siendo papel blanco en los dos temas.

### 1.3 Formas (*shape lock*, `Theme.kt:195-201`)

La regla, que es la que se audita: **las acciones son píldoras; las filas tocables miden 18; las tarjetas, 24; el hero y el
bloque de resultado, 28; los recuadros dentro de una tarjeta, 16; los recuadros de icono, 14; las insignias, 12; la casilla, 8; el
documento (credencial), 16.**

```kotlin
val ArbShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),   // casilla del indicador (24 dp)
    small = RoundedCornerShape(12.dp),       // insignia «Resposta múltiple», chips
    medium = RoundedCornerShape(16.dp),      // recuadros de dato, TextField, Callout pequeño
    large = RoundedCornerShape(24.dp),       // tarjetas (ArbCard, InfoBanner, FactStrip, CredentialMini)
    extraLarge = RoundedCornerShape(28.dp),  // hero, tarjeta de resultado, diálogos
)
object ArbShape {
    val option = RoundedCornerShape(18.dp)   // OptionCard, ActionRow, ReviewItem, OverviewRow, ResolvedOption
    val iconTile = RoundedCornerShape(14.dp)
    val document = RoundedCornerShape(16.dp) // credencial = documento, no "card" de UI
    val pill = RoundedCornerShape(29.dp)     // botones: radio FIJO (no 50 %), así a 200 % con 2 líneas no se convierte en un óvalo
}
```

### 1.4 Medidas

| Token | Actual → **nuevo** | Nota |
|---|---|---|
| `ArbDimens.cta` *(nuevo)* | **58 dp** | Botón principal o secundario a todo el ancho (Inici, Resultat, Credencial). |
| `ArbDimens.buttonMin` | 56 (igual) | Diálogos y botones secundarios. |
| `QDimens.navButton` | 52 → **56** | Píldora del mockup. Cabe en el presupuesto UI-04 (§2.2). |
| `QDimens.navGap` | 8 → **12** | Mockup; los dos botones siguen con `weight(1f)` (UI-01). |
| `QDimens.optionGap` | 8 → **10 (N) / 8 (C)** | El mockup usa 12; con 12 no cabe en C. |
| `QDimens.optionHPad` | 12 → **14** | Con radio 18 y padding 12, el texto queda pegado a la curva. Ancho de texto: 264 dp (antes 268). |
| `QDimens.optionMinN / C / Tf` | 56 / 52 / 64 (**igual**) | **No 64 dp** (el mockup usa 64): rompería UI-04. Mínimo que respeta los 700 dp: **52 dp** en C. |
| `QDimens.badgeRadius` | 14 → **12** | Mockup. |
| `ArbDimens.iconTile` *(nuevo)* | 44 dp; icono de 22 | Mockup. |
| `ArbDimens.progressSeg` *(nuevo)* | 6 dp de alto, radio 3, separación 4 | El mockup usa 6 de separación con 5 segmentos; con 10 queda mejor 4. |
| Margen lateral | Inici, Resultat y Credencial **20** (igual); Pregunta **16** (igual, UI-04) | El 24 del mockup está pensado para 390 dp; en 360 dp quitaría líneas de texto. |
| Padding de tarjeta | **20** (mockup 22); separación entre filas **16** | Ritmo de 4/8 dp. |

### 1.5 Tipografía (Atkinson Hyperlegible Next, la misma del mockup)

Regla dura (04 §8.1): **nada por debajo de 16 sp**. Los textos de 13, 14 y 15 px del mockup suben a 16.

| `ArbText` nuevo | Valor | Dónde |
|---|---|---|
| `brandWord` | 20/26/700, `primary` | «Arbitratge» en la fila de marca. |
| `heroOverline` | 16/20/700, `#D7E6DC` (9,47:1 sobre gespa) | «Examen d'àrbitre». En minúscula de frase: las mayúsculas pierden legibilidad para gente mayor (ver Q1 sobre el amarillo). |
| `heroTitle` | 28/32/700, blanco, `widthIn(max = 220.dp)` | «Futbol amb Botons» en 2 líneas, como el mockup. |
| `factValue` | 24/28/700, `tnum` | Cifras del hero y del resultado. |
| `factLabel` | 16/20/400 | «preguntes», «minuts»… |
| `featureTitle` | 16/22/700 | Filas de la tarjeta de características. |
| `qProgress` | 16/22/700, `secondary #1D6F42` (5,66:1) | «Pregunta 2 de 5» encima del enunciado. |
| `score` | 30/36/700, `tnum`, `primary` | «5/5» dentro del anillo. |
| `link` | 16/22/700, `secondary` | Botones terciarios («Tornar a l'inici», «Repassar…»). |
| `timer` | Atkinson Next 16/22/**700** (antes 600) | Mockup. La tipografía mono se mantiene solo para mm:ss. |

`questionN/C` 20/18 y `optionN/C` 18/16 **no cambian** (UI-04). El 25 px del enunciado del mockup no cabe en el presupuesto.

---

## 2. Cambios por pantalla

### 2.1 Inici (`ScreensMain.kt:142-195`, mockup `UI-Inici`)

1. **Se quita la `BrandBand` a sangre** (`header =`). La barra de estado queda sobre el fondo, con iconos oscuros, y en esta pantalla deja de llamarse a `LightStatusBarIcons`.
2. **`BrandTopRow`** (componente nuevo, como `topBar`): `StatusInset` + `Row` de 56 dp con padding horizontal de 20 y vertical de 8/4. Lleva `BrandMark(36.dp)` (sin `onPitch`), 10 dp de separación y `brandWord`. El botón «Ajuda» del mockup **no se añade** (Q2).
3. **`HeroCard`** (nuevo, primer elemento del contenido). Forma `extraLarge` (28), fondo `BrandColors.Pitch`, padding horizontal 16 y vertical 20, `heightIn(min = 220.dp)` (no 236 fijo) y `Arrangement.SpaceBetween`.
   - Decoración (`drawBehind`, dentro del clip): círculo de r 88 dp centrado en (ancho − 50 dp, alto/2), trazo de 2 dp `heroLine`; línea vertical en x = cx de arriba abajo; punto de r 6 dp. Es el «centre del camp» de la marca y sustituye a `centreLines`, que dibujaba cal α 0,18. La capa no tiene semántica.
   - Texto: `heroOverline` y, 6 dp debajo, `heroTitle` con `heading()`. Cadenas nuevas `hero_overline` = «Examen d'àrbitre» y `hero_title` = «Futbol amb Botons» (copy para `ux-designer`).
   - Datos: 3 recuadros con `weight(1f)`, separación 6, fondo `Color(0xFFF2F5F3).copy(alpha = .12f)` (equivale a `#2A533F`), radio 16, padding horizontal 8 y vertical 12. El valor va en `factValue` blanco y la etiqueta en `factLabel` `#D7E6DC` (6,75:1). Se mantienen las etiquetas completas actuales (`home_fact_*_full`).
   - **Medido a 360 dp:** el texto dispone de 76 dp por recuadro y «preguntes» ocupa 71 dp → cabe. «encerts per aprovar» baja a 3 líneas, como ya pasa hoy en `FactStrip`. **Regla:** si `isLargeText()` es cierto, o si la palabra más ancha (`rememberTextMeasurer`) supera el ancho disponible, los datos pasan a filas «valor · etiqueta» dentro de un único panel α 0,12 (el patrón de `FactStrip` a 130 %). Así nunca se parte una palabra. Para TalkBack, el bloque se lee como uno solo: `home_facts_a11y`.
   - Oscuro: igual, con un filete de 1 dp de cal α 0,24 (la gespa sobre `#0C1A12` da 1,46:1; es el mismo recurso que usa `BrandBand` hoy).
4. **Estado vacío** (sin historial y sin credencial): tarjeta `ArbCard` (§2.5) con 3 `FeatureRow` (nuevo). Cada fila lleva `iconTile` de 44 dp con radio 14, 14 dp de separación, `featureTitle` y debajo `bodyMedium` `onSurfaceVariant`, alineados al centro vertical. Iconos existentes: `ic_wifi_off`, `ic_menu_book` e `ic_id_card`. El copy del mockup («Funciona sense internet / Es corregeix al moment, al teu mòbil.», «Aprens de cada error / …», «Credencial si aproves / …») lo valida `ux-designer`. Debajo va `home_empty`, centrado, en 16/22 `onSurfaceVariant`.
5. **Con historial:** el orden actual se mantiene (UX P2.1). El estado del último examen pasa a una tarjeta con `iconTile` (✓ / i). `CredentialMini` adopta el estilo nuevo (§2.3, punto 4). `ActionRow` pasa a fila blanca con sombra, radio 18, **sin borde**, con `iconTile` a la izquierda y chevron.
6. **Botón principal:** `PrimaryButton` en píldora de 58 dp con flecha → final (22 dp, solo si `!isLargeText()`). El enlace «Llegir les normes» **no se añade** (Q2). Con credencial: «Veure la credencial» (principal) + «Tornar a fer l'examen» (secundario, §2.5).
7. Presupuesto a 360×800 y 100 %: barra de estado 24 + fila de marca 56 + 12 + hero 220 + 16 + tarjeta 216 + 16 + texto 44 + barra inferior 82 + navegación 24 ≈ **710 de 800**. Cabe sin scroll.

### 2.2 Pregunta (`QuestionScreen.kt`, mockup `UI-Pregunta`)

| # | Elemento | Cambio | Restricción |
|---|---|---|---|
| P1 | Cabecera (`QuestionTopBar`, :232-279) | Una sola fila de 48 dp + 4/4, sin cambios de alto: **[progreso segmentado `weight(1f)`] 12 dp [TimerPill] 4 dp [botón de resumen]**. El texto «Pregunta N de M» **sale de la cabecera** y va al contenido (P2). El `FocusRequester`, `heading()`, `contentDescription = a11y` y el anuncio se mueven con él. | La «X Sortir» del mockup **no se añade** (Q4); salir sigue siendo con el gesto Atrás y el diálogo EXIT. |
| P1b | Con `isExtraLargeText()` | Los segmentos pasan a una segunda línea a todo el ancho (6 dp, 8 dp por encima), para que el reloj grande no los deje con menos de 96 dp. | — |
| P1c | Botón de resumen | Círculo blanco (`card`) de 44 dp con sombra de tarjeta, dentro de una zona táctil de 48 dp. `ic_checklist` de 22 dp en `primary`. La `contentDescription` y el `OVERVIEW_TAG` no cambian. | ≥ 48 dp de toque. |
| P1d | `TimerPill` (:284-326) | Normal: `primaryContainer #E4F0E8` / `#0F3D26`, 40 dp de alto (zona táctil de 48), padding horizontal 14, icono de 18 y texto a 700. Con aviso de 5 min (ámbar) y de 1 min (error), igual que ahora. | Se mantienen «N min» y mm:ss solo en el último minuto (decisión de producto; el mockup muestra «4:12»). |
| P2 | Antetítulo | `Text(progress, qProgress)` encima del enunciado. Orden de la columna: antetítulo → **4 dp** → enunciado → **8 dp** → insignia. Con lletra extra-gran, `progress_short`. | Coste: +26 dp, incluido en el presupuesto. |
| P3 | Enunciado | Sin cambios (`questionN/C`, 600). | UI-04. |
| P4 | Insignia «Resposta múltiple» (:375-396) | **Se mantiene** el ámbar `#F5C443` con el disco «!» (UI-02). Solo cambia: radio **12**, `heightIn(min = 32.dp)`, padding inicial 6, final 12 y vertical 4. | El mockup pone `#FFF4CC` con un **icono de dos casillas**: incumple UI-02 dos veces (glifo de casilla y aviso sin color). Desviación obligatoria. |
| P5 | `OptionCard` (:417-455) | **Sin marcar:** `card` + `softShadow(option)` (§3.1), sin borde en claro (`cardBorder` 1 dp en oscuro). **Marcada:** `selectContainer` + borde de **2 dp `select`** y **sin sombra**: la tarjeta «se hunde», sin desplazamiento de layout porque la sombra se dibuja fuera. Forma `ArbShape.option` (18). Padding horizontal 14, vertical 10 (N) / 8 (C). Mínimos 56 / 52 / 64. **La sombra va ANTES del `clip`** en la cadena de modifiers. | La animación de 120 ms y `reduceMotion()` se mantienen. Se anima solo el color, no el grosor del borde: el borde de 0 → 2 dp se dibuja con `border` y no mueve el texto. |
| P6 | `Indicator` (:461-479) | Casilla de 24 dp con `extraSmall` (8). Sin marcar: borde de 2 dp `optionRing`. Marcada: relleno `select` + ✓ de 18 dp `onSelect` (trazo 3, como el mockup). Radio: anillo `optionRing`; marcado, anillo `select` + punto de 12 dp `select`. | 3,71:1 o más (1.4.11). La marca no depende solo del color: también cambian el relleno, el ✓ o el punto y el borde de la tarjeta. |
| P7 | `NavBar` (:179-188) | **Anterior** = `OutlinedButton`, fondo `card`, borde de 1,5 dp `buttonOutline`, texto e icono `primary`, `ArbShape.pill`, 56 dp, ← de 20 dp a la izquierda. **Següent** = `Button` `primary`, la misma forma y alto, → de 20 dp a la derecha. Las flechas solo con `!isLargeText()` y **en los dos botones a la vez**, para que la altura sea idéntica. Anterior deshabilitado en P1: fondo transparente, borde `buttonOutline` α 0,5 y texto `onSurface` α 0,38. | **UI-01:** `weight(1f)` en los dos botones y `IntrinsicSize.Min` (se justifica pese a la regla de `ui-ux-pro-max`: es una sola fila con dos hijos y UI-01 exige la misma altura). Ancho a 360 dp: 158 cada uno; «Anterior» + icono = 116 incluido el padding ✓. |
| P8 | Fondo | Lo hereda de `background`: la barra inferior y la cabecera usan `MaterialTheme.colorScheme.background` (ya lo hacen). | — |

**Presupuesto UI-04 recalculado** (360×800, 700 dp útiles; mismas muestras que en `ui-density/ui-designer-spec.md` §5):
el coste fijo es cabecera 56 + 8 + 8 + barra (8 + 56 + 8) = **144** → **556 dp** de contenido.

| Caso | Contenido | Margen |
|---|---|---|
| Típica, única, nivel N, 4 × 1 línea | 26 + 78 + 12 + (4 × 56 + 3 × 10) = 370 | +186 ✓ |
| Peor caso real, múltiple, nivel C | 26 + 96 + 8 + 32 + 12 + 300 = 474 | **+82 ✓** |
| 5 opciones, múltiple, nivel C | 26 + 96 + 8 + 32 + 12 + 338 = 512 | **+44 ✓** |
| Futuro (200 caracteres), múltiple, nivel C | 26 + 144 + 8 + 32 + 12 + 344 = 566 | −10 → scroll (ya pasaba antes) |

El margen sale de la fila de 48 dp que quitó `spec-acceptada`. **Con estos números no caben** opciones de 64 dp, una separación
de 12 entre opciones ni un enunciado de 25 sp. **[PV]** Hay que repetir las capturas de las preguntas reales a 100 %, fuera del repo.

### 2.3 Resultat (`ScreensResult.kt:192-260`, mockup `UI-Resultat`)

1. **Se quita `VerdictBlock` a sangre** y `ButtonsMarker`. Barra superior: `StatusInset`. Barra de estado con iconos oscuros. Margen lateral de 20 y separación de 16.
2. **`ResultCard`**: `card`, forma de 28, `softShadow(card)`, padding superior 24, laterales 20 e inferior 20. Columna centrada con 14 de separación:
   - **`ScoreRing`** (Canvas): diámetro `132.dp * fontScale.coerceIn(1f, 1.6f)`, trazo de 12 dp, `StrokeCap.Round` y arco desde −90°. Si aprueba: pista `iconTile #E4F0E8` y arco `secondary #1D6F42`. Si suspende: pista `outlineVariant` y arco `onSurfaceVariant` (**sin rojo**, 04 §2.8). Con `correct == 0` no se dibuja arco, porque la punta redonda pintaría un punto. Animación del barrido 0 → valor en 600 ms con `FastOutSlowInEasing`; con `reduceMotion()`, estático. El «5/5» va dentro, en `score`. El **punto amarillo** del mockup no se pone (Q1).
   - Título `passed_title` / `failed_title` en `headlineSmall` 28/36/700, `primary` si aprueba y `onSurface` si no. Con `heading()` + `liveRegion Polite` (los mismos que hoy).
   - Subtítulo `result_min` en 16/22 `onSurfaceVariant`. El copy del mockup, «Calien 3 encerts per aprovar», lo decide `ux-designer`.
   - **Recuadros de dato**: `Row` con separación 8; recuadros `weight(1f)`, fondo `tile`, radio 16, padding horizontal 10 y vertical 12; `factValue` (22→24) + `factLabel` `onSurfaceVariant`. Contenido: **[encerts (`secondary`), errades, temps]** si no hay preguntas sin respuesta y **[errades, sense resposta, temps]** si las hay, para tener siempre 3 recuadros. «resposta» ocupa unos 60 dp y caben 68 ✓. Con `isLargeText()`, filas (como `FactStrip`). Ver Q7.
   - Accesibilidad: el anillo y los recuadros con `clearAndSetSemantics { contentDescription = score_a11y + spoken }`, como hoy.
3. Suspendido: `failed_next` dentro de la tarjeta, debajo de los recuadros, en `onSurfaceVariant`.
4. **`CredentialMini`** con el estilo nuevo (`ScreensMain.kt:89-110`), que también usa Inici. `card`, forma `large` (24), `softShadow(card)` y `clip`.
   - Cabecera: `Row` de gespa con padding 16/18. Lleva `BrandMark(36.dp, onPitch = true)` y, a 12 dp, una columna con `cred_title` en 18/24/700 blanco y `cred_subtitle` en 16/20 `#D7E6DC`. Debajo, un **filete de 4 dp `RefCard`** (uso de marca ya aprobado).
   - Cuerpo: padding 18 y una fila con dos partes:
     - Columna con 4 de separación: nombre en 20/26/700 tinta; «Codi» + `shortCode` en `AtkinsonMono` 16/700, `letterSpacing` 1 sp; fecha `dd/MM/yyyy` en 16 `onSurfaceVariant`.
     - Recuadro de 64 dp, radio 14, fondo `tile`, con el icono QR de 32 dp en tinta. Es decorativo y **no es el QR real**. Requiere el asset nuevo `ic_qr_code.xml` (Material Symbols «qr_code_2», Apache 2.0).
   - La tarjeta no es interactiva: la acción está en el botón principal.
5. **Botones:** principal en píldora de 58 dp con icono QR. Se mantienen el destino y el texto actuales, «Veure la credencial» (Q3). Revisar errores es un secundario en píldora (§2.5) e «Inici» va como enlace (`link`). Se conserva la jerarquía actual de 3 niveles. El mockup muestra 2 porque tiene 0 errores.
6. Presupuesto aproximado a 360×800: 24 + 12 + tarjeta 332 + 16 + mini 168 + 16 + 3 botones 202 ≈ 770. Cabe justo; si no, hace scroll (UI-04 no aplica aquí).

### 2.4 Credencial (`ScreensCredential.kt:91-145`; sin mockup propio, se aplica coherencia)

1. Documento: `Surface` blanco con forma `ArbShape.document` (**16**, no 24). Un documento tiene las esquinas pequeñas; un DNI de 85 mm tiene 3 mm de radio, que en 320 dp son unos 12 dp. Así se distingue de las tarjetas de UI. Cambios: **se quita el borde `PaperLine`** en claro y se cambia `shadowElevation = 2.dp` por `softShadow(card)`. En oscuro, papel blanco sin sombra (ya contrasta).
2. `CredentialBand` (filete amarillo de 4 dp) **sin cambios**. El QR, `CardRow` y el pie no cambian (ARB1 intacto). El chip «APTE» pasa a `PaperChip #E4F0E8`.
3. Botones: `PrimaryButton` en píldora de 58 dp «Mostrar a un organitzador» con **icono QR** en lugar de `ic_id_card` (como el mockup). «Tornar a l'inici» como enlace.
4. Presentación: «Tancar» pasa a `OutlinedButton` en píldora de 56 dp (borde de 2 dp de gespa, sin cambios). El resto, igual.

### 2.5 Componentes comunes (`Components.kt`, `BrandComponents.kt`)

| Componente | Cambio |
|---|---|
| `PrimaryButton` | `shape = ArbShape.pill`, `heightIn(min = ArbDimens.cta)`, padding horizontal 24. Icono opcional **a la derecha** (parámetro `trailingIcon`) para la flecha. |
| `SecondaryButton` | Fondo `card`, borde de 1,5 dp `buttonOutline`, contenido `primary`, píldora de 56/58. Deshabilitado como Anterior (P7). |
| `TertiaryButton` | Estilo de enlace: texto `link`, color `secondary`, 48 dp de mínimo y forma de píldora para el *ripple*. |
| `ArbCard` | `card`, forma `large` (24), **sin borde** + `softShadow(card)`; en oscuro, `cardBorder`. Padding 20 y separación 16. |
| `InfoBanner` NEUTRAL | Pasa a `card` + sombra + icono `onSurfaceVariant`. `surfaceContainer` sobre el fondo nuevo quedaría invisible (1,0:1). SUCCESS, WARNING y ERROR siguen como contenedores de color, con radio 24. |
| `FactStrip` | Si va directamente sobre el fondo (Normes), contenedor `card` + sombra. Las celdas no cambian. |
| `ActionRow`, `ReviewItem`, `OverviewRow` | `card` + `softShadow(option)`, forma de 18, **sin borde** de 1,5 dp `outline`. El icono de la izquierda va en `iconTile` (`ActionRow`). En `OverviewRow`, la fila actual pasa de un borde de 3 dp `primary` a **2 dp `select` + `selectContainer`**, el mismo lenguaje que la opción marcada. |
| `ArbDialog` | Sin cambios (28). Los botones pasan a píldora automáticamente. |

---

## 3. Sombra suave y progreso segmentado en Compose

### 3.1 Sombra (CSS → Compose)

Mockup: tarjeta `0 1px 2px rgba(15,61,38,.06), 0 8px 24px rgba(15,61,38,.07)`; opción `… 0 6px 18px rgba(15,61,38,.06)`.
Con el BOM `2026.11.01` está disponible `Modifier.dropShadow(shape, Shadow)` (Compose UI ≥ 1.9), que es un *box-shadow*
real: con color, desplazamiento y desenfoque, y sin las limitaciones de `elevation`.

```kotlin
enum class ShadowLevel { Card, Option }

@Composable
fun Modifier.softShadow(shape: Shape, level: ShadowLevel = ShadowLevel.Card): Modifier {
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) return this // en oscuro: sin sombra (se usa cardBorder)
    val tint = MaterialTheme.arb.shadowTint
    val far = if (level == ShadowLevel.Card) Shadow(radius = 24.dp, offset = DpOffset(0.dp, 8.dp), color = tint.copy(alpha = .07f))
              else Shadow(radius = 18.dp, offset = DpOffset(0.dp, 6.dp), color = tint.copy(alpha = .06f))
    return this
        .dropShadow(shape, Shadow(radius = 2.dp, offset = DpOffset(0.dp, 1.dp), color = tint.copy(alpha = .06f)))
        .dropShadow(shape, far)
}
// Orden: Modifier.softShadow(shape).clip(shape).background(card, shape).border(...)  ← la sombra SIEMPRE antes del clip.
```

- **Calibración [PV]:** el *blur* de CSS corresponde a 2σ y la implementación de Android no lo traduce 1:1. Se empieza con los valores del mockup y se ajusta el `radius` (×0,8-1,0) comparando la captura con el mockup a la misma escala. **Se comprueba también en API 26**, que es el `minSdk`. Hay que confirmar la firma exacta (`Shadow(radius, color, spread, offset)`) en la versión resuelta del BOM.
- **Alternativa** si `dropShadow` no está disponible o renderiza mal en API 26: `Modifier.shadow(elevation = 6.dp, shape, ambientColor = tint, spotColor = tint)`. Da una sombra algo más dura y abajo; es aceptable.
- Solo hay **2 niveles** (Card y Option). La búsqueda `"elevation shadow depth" --domain style` marca *risk:high* de accesibilidad para el estilo de capas: no se superponen tarjetas ni se anima la sombra (salvo quitarla al marcar).
- Rendimiento: como máximo unas 8 sombras por pantalla. Es despreciable.

### 3.2 Progreso segmentado

Un solo `Canvas` es más barato que N `Box` y se adapta a cualquier `total`:

```kotlin
@Composable
fun SegmentedProgress(current: Int, total: Int, modifier: Modifier = Modifier) {
    val arb = MaterialTheme.arb
    val done by animateColorAsState(arb.progressDone, if (reduceMotion()) snap() else tween(200), label = "seg")
    Canvas(modifier.height(6.dp).clearAndSetSemantics { }) { // decorativo: «Pregunta N de M» ya lo dice (y es heading)
        val gap = 4.dp.toPx(); val r = CornerRadius(3.dp.toPx())
        val seg = (size.width - gap * (total - 1)) / total
        if (seg < 6.dp.toPx()) { // > ~20 preguntas: barra continua
            drawRoundRect(arb.progressTodo, cornerRadius = r)
            drawRoundRect(done, size = Size(size.width * (current + 1) / total, size.height), cornerRadius = r)
        } else for (i in 0 until total) drawRoundRect(
            if (i <= current) done else arb.progressTodo,
            topLeft = Offset(i * (seg + gap), 0f), size = Size(seg, size.height), cornerRadius = r,
        )
    }
}
```

- Se rellena según la **posición** (`index`), como en el mockup («Pregunta 2 de 5» = 2 segmentos), no según las respuestas contestadas. Lo contestado ya está en el resumen.
- El segmento pendiente `#CFDCD3` sobre el fondo da 1,30:1. Es aceptable porque la información está en el texto «Pregunta N de M», con lo que el 1.4.11 no aplica. El segmento hecho da 5,66:1.
- Con RTL no hace falta nada (la app es solo LTR en catalán). Si algún día hiciera falta, se aplicaría `scale(-1, 1)` con `LayoutDirection.Rtl`.

---

## 4. Alcance y riesgos en otras pantallas (coherencia mínima)

| Pantalla | Cambio mínimo propuesto | Esfuerzo | Riesgo |
|---|---|---|---|
| **Todas** | Les llega automáticamente por los tokens: fondo `#F3F6F2`, botones en píldora y formas `medium` 16 / `large` 24. | Ya incluido | **Alto si no se hacen capturas:** todo lo que usa `surfaceContainer` directamente sobre el fondo desaparece (sale 1,0:1). Hay que revisar `TimerPill` oculto, `FactStrip`, `InfoBanner` NEUTRAL y el detalle de repaso «Per què?». |
| Benvinguda (`ScreensMain.kt:76-88`) | Se sustituye `BrandBand` por `BrandTopRow` + `HeroCard` sin datos (`welcome_title`) + `ArbCard` con 4 `FeatureRow` (`welcome_b1..b4`). Es la primera pantalla de la app y no puede contradecir a Inici. | M | Después, `BrandBand` se usa solo en `ErrorScreen`: se migra también y se borran `BrandBand`, `isBandBehindStatusBar`/`STATUS_CAP_TAG` si quedan sin uso. Hay tests que los referencian y hay que actualizarlos. |
| Normes (`:271-301`) | `FactStrip` → `card`; las 4 `InfoRow` → **una** `ArbCard` con `FeatureRow` de solo título (icono en `iconTile` de 40). Aviso ámbar, radio 24. `NameCard` → `card` sin borde + sombra. | S-M | `collectionInfo(6,1)` se mantiene. |
| Nom (`:198-233`) | `OutlinedTextField`: contenedor `card` y forma 16; borde con foco `select`. `InfoBanner` NEUTRAL → tarjeta. | S | — |
| Resum (`ScreensOverview.kt`) | `OverviewRow` → tarjeta de 18 con sombra (§2.5). Los botones de abajo, en píldora. | S | El disco de tinta «contestada» (`mark`) se mantiene: **no** es una selección. |
| Repaso: lista y detalle | `ReviewItem` → tarjeta de 18 con sombra. `ResolvedOption`: **el radio está fijado a 12 en `drawBehind` (`ScreensResult.kt:497`)** y hay que pasarlo a 18. Las tarjetas sin color llevan sombra; las de veredicto (verde/rojo) no. Los botones Anterior/Següent del detalle pasan a píldora con `weight(1f)` cuando se ven los dos (ya lo hacen). | S | **Pendiente de la tarea anterior:** el detalle sigue usando `ArbText.question`/`option` (24/20), que es más grande que la pregunta. Se deja fuera del alcance y se declara. |
| TimeUp, Grading, Error, Loading | Solo tokens. | — | — |
| Diálogos | Solo tokens: los botones pasan a píldora. | — | — |

**Tests afectados** (para `android-developer`): los que buscan la banda o `STATUS_CAP_TAG`, `VerdictBlock`/`ButtonsMarker`
y la cabecera con el texto «Pregunta N de M» (ahora en el contenido, con el mismo `heading`/foco). **Los tests de UI-01
(anchos iguales a 100/130/200 %) y UI-02 (icono «!», sin casilla) tienen que seguir en verde sin tocarlos.**

---

## 5. Lista priorizada (impacto / esfuerzo)

| # | Cambio | Impacto | Esfuerzo |
|---|---|---|---|
| 1 | Tokens §1 (fondo, `ArbColors` nuevos, formas, `softShadow`, botones en píldora) | Muy alto: toda la app cambia de aspecto | S |
| 2 | Pregunta §2.2 (opción blanca con sombra y selección verde, cabecera segmentada, reloj, NavBar en píldora) | Muy alto (la pantalla más usada) | M |
| 3 | Inici §2.1 (fila de marca, `HeroCard`, `FeatureRow`) | Alto | M |
| 4 | Resultat §2.3 (`ResultCard` + `ScoreRing` + recuadros + `CredentialMini` nuevo + `ic_qr_code`) | Alto | M |
| 5 | Coherencia §4 (Benvinguda, Normes, Resum, Repaso, Nom) | Medio: evita una app «a medias» | M |
| 6 | Credencial §2.4 | Medio | S |

---

## 6. Preguntas (no se implementan sin respuesta; el valor por defecto se aplica si no hay respuesta)

| # | A quién | Pregunta | Por defecto |
|---|---|---|---|
| Q1 | Usuario (marca) | La marca reserva el amarillo `#F2C230` al punto del símbolo y al filete de la credencial. El mockup lo usa además en el **antetítulo del hero** y en un **punto del anillo de resultado**. ¿Se amplía su uso? (Sobre gespa da 7,31:1, así que la legibilidad no es el problema.) | No: antetítulo en `#D7E6DC` y anillo sin punto. Si se acepta, cambiar el color es una línea. |
| Q2 | Usuario / `ux-designer` (flujo) | El mockup de Inici añade un botón **«Ajuda»** y un enlace **«Llegir les normes»**: son entradas nuevas. | No se añaden. |
| Q3 | Usuario / `ux-designer` (flujo) | En Resultat, el principal del mockup es **«Mostrar a un organitzador»** (iría directo a la presentación). | Se mantiene «Veure la credencial» → pantalla de credencial, con el icono QR. |
| Q4 | Usuario | ¿Se añade la **«X» de salir** visible en la cabecera de la pregunta, como en el mockup? Para un público mayor aumenta el riesgo de tocarla sin querer, aunque hay diálogo de confirmación. | No: salir sigue siendo con el gesto Atrás y el diálogo; ese hueco lo ocupa el botón de resumen (a la derecha). |
| Q5 | `ux-designer` | La selección en el examen pasa de **tinta** (UX F4: «el verde es el veredicto») al **verde `#1D6F42`** del mockup. En el examen no se muestra ningún veredicto. En el repaso, «lo que marcaste» sigue en tinta (`mark`). | Verde (lo pide UI-05), con el token `select` separado de `correct`. |
| Q6 | `ux-designer` (copy) | Cadenas nuevas o cambiadas: `hero_overline`/`hero_title`, las 3 `FeatureRow`, «Has aprovat**!**», «Calien N encerts per aprovar» y el texto de la insignia («Pots marcar més d'una» del mockup, frente a «Resposta múltiple»; el «!» y el ámbar se quedan por UI-02). | Las actuales, más las nuevas imprescindibles de Inici. |
| Q7 | `ux-designer` | Recuadros de Resultat: [encerts, errades, temps] / [errades, sense resposta, temps]. | Así. |
| Q8 | Usuario | El anillo sustituye al marcador de **botones** (discos), que era una metáfora de la marca. | Anillo (es el mockup). |

## 7. Conformidad con el mockup: desviaciones justificadas

| Mockup | Spec | Motivo |
|---|---|---|
| Opción de 64 dp, separación 12, enunciado 25 px | 56/52, separación 10/8, 20/18 sp adaptable | **UI-04** (§2.2, presupuesto) |
| Insignia `#FFF4CC` con glifo de casillas | Ámbar `#F5C443` + disco «!» | **UI-02** |
| Anterior/Següent de 167 px fijo | `weight(1f)` × 2 | **UI-01** |
| Textos de 13, 14 y 15 px | 16 sp | 04 §8.1 (público mayor) |
| Casilla `#8AA394` | `#738A7C` | WCAG 1.4.11 (3:1) |
| Margen lateral 24 | 16 en Pregunta y 20 en el resto | Ancho de 360 dp y UI-04 |
| Hero de 236 fijo | Mínimo 220, crece con el texto | 130/200 % |
| Antetítulo en mayúsculas y amarillo | Minúscula de frase, `#D7E6DC` | Legibilidad y marca (Q1) |
| Ayuda, «Llegir les normes», «X», CTA de presentación | No se añaden | Flujo de producto (Q2-Q4) |
| Sombras | `softShadow` en claro; filete en oscuro | Sombra invisible en oscuro |

## 8. Verificación que se exige en la QA (UI-05)

Capturas ANTES y DESPUÉS de Inici, Pregunta (única, múltiple, Cert/Fals, P1 con Anterior deshabilitado) y Resultat (aprobado y suspendido)
junto al mockup. En claro y oscuro, a 100/130/200 %, en `emulator-5554` a 360×800. Las preguntas reales a 100 %, fuera del repo
(UI-04). Contraste medido en las capturas para `optionRing`, el texto secundario y los enlaces. TalkBack [PV]: el foco va al
antetítulo «Pregunta N de M» al cambiar de pregunta y no se lee dos veces.

## 9. Registro de `ui-ux-pro-max`

Leídos: `SKILL.md`, `references/pro-rules.md` (estados estables sin desplazamiento, paridad de contraste claro/oscuro, bordes
visibles en los dos temas, tokens semánticos, ritmo de 8 dp, 48 dp de toque) y `ui-taste/reference/anti-slop-craft.md`
(un solo acento, *shape lock*, una sola familia de grises).

| Consulta | Resultado | Aplicación |
|---|---|---|
| `"soft shadow card elevation" --stack jetpack-compose` | 0 resultados | Reintento ↓ |
| `"shadow elevation" --stack jetpack-compose` | 0 resultados | Sin coincidencias verificadas: §3.1 usa la API de Compose como alternativa general, marcada [PV] |
| `"segmented progress indicator steps" --stack jetpack-compose` / `"progress" --stack jetpack-compose` | 0 / 0 | Reintento por dominio ↓ |
| `"progress stepper multi-step" --domain ux` | *Progress Indicators: Step 2 of 4 indicator* | Segmentos + texto «Pregunta N de M» (§3.2) |
| `"dark mode elevation tonal surface" --domain ux` | Fuera de tema (Maps); descartado | Reintento ↓ |
| `"dark mode contrast" --domain ux` | *Color Contrast 4.5:1*, *Contrast Readability* | Tabla de contraste en §1.1-1.2 |
| `"Material theme color scheme" --stack jetpack-compose` | *Design system: Material3 tokens*; *Dark mode support: colorScheme* | Todo en `ArbColors`/`ArbShape`, sin hex en las pantallas |
| `"intrinsic size" --stack jetpack-compose` | *Avoid intrinsic size unless necessary* (High) | Se mantiene `IntrinsicSize.Min` solo en NavBar (UI-01, 2 hijos) |
| `"elevation shadow depth" --domain style` | *dimensional-layering*, accesibilidad *risk:high* | Solo 2 niveles de sombra, sin solapamientos (§3.1) |
| `"button height touch target"`, `"font scaling text overflow"` `--stack jetpack-compose` | 0 / 0 | Se aplican las reglas de `pro-rules.md` (48 dp; palabras sin partir, medidas con fontTools) |
