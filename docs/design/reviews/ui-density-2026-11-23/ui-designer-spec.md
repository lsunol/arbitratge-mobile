# Pantalla de pregunta moderna y compacta: spec visual

**Autor:** `ui-designer`, como consultor: propone, no implementa. **Fecha:** 2026-11-23.
**Alcance:** pantalla de pregunta (cabecera, reloj y progreso, enunciado, tarjetas de respuesta, barra Anterior/Següent) e insignia «Resposta múltiple».
**Decisiones vinculantes** (`docs/design/decisiones-usuario-ui.md`): UI-01, UI-02, UI-03 y UI-04. Esta spec las aplica tal como están escritas; lo que podría parecer que las toca va a §6 como pregunta.

**Base:**
- Capturas en `docs/design/reviews/evidence/2026-11-22-ui-despres/`: `11-pregunta-unica-marcada-100-clar`, `13-pregunta-multiple-marcada-100-clar` y `14-pregunta-certfals-buida-100-clar`, a 1080×2400 px (411×914 dp).
- Código en `app-candidate/src/main/kotlin/dev/lluissunol/arbitratge/ui/`:
  - `QuestionScreen.kt`;
  - `Components.kt`;
  - `Theme.kt`.
- Dirección «Paper i gespa» (`../ui-revamp-2026-11-22/ui-designer-spec.md`). Se mantienen la marca, Atkinson, la tinta como color de «marca de bolígrafo» y la gespa como color de acción.

## 1. Diagnóstico (por qué «massa espaiosa» y «gens moderna»)

| # | Problema | Evidencia |
|---|---|---|
| D1 | **La cabecera se come unos 150 dp antes del enunciado.** Ocupa 3 filas (pregunta y reloj de 48 dp, barra de 8 dp, «Respostes / Veure-les totes» de 48 dp) con paddings de 8 y 4, y después hay 12 + 12 dp de contenido. | `11-…-100-clar`: el enunciado empieza en y ≈ 190 dp · `QuestionScreen.kt:222-265`, `Components.kt:198` |
| D2 | **Escala tipográfica de cartel.** El enunciado va a 24/34 y las opciones a 20/28, así que con preguntas reales (enunciado ≤ 132 caracteres, opciones de 97/96) solo caben unas 29 letras por línea y las opciones ocupan 4 o 5 líneas. | `Theme.kt:221-224` |
| D3 | **«Caja pesada».** Las opciones sin marcar llevan un borde de 1,5 dp `#66736B` (5:1) y tienen 64 dp de mínimo; la marcada es un bloque negro macizo `#18241D` (D3 de la spec anterior, P3 «invertida»). | `13-…-100-clar` · `QuestionScreen.kt:400-441` |
| D4 | **La marca mueve el contenido.** El texto pasa de 400 a 600 y el padding se recalcula como `16 - bw` (borde de 1,5 a 3 dp): el texto se desplaza 1,5 dp y una opción larga puede ganar una línea al marcarla. Incumple *Stable Interaction States* de `pro-rules.md`. | `QuestionScreen.kt:417-433` |
| D5 | **«Resposta múltiple» parece una opción más.** Es una caja de contorno de radio 12 (la misma forma que las tarjetas) con un glyph de casilla, justo encima de las opciones. Incumple UI-02. | `13-…-100-clar` · `QuestionScreen.kt:367-381` |
| D6 | **Botones 1 : 1,4.** «Següent» pesa 1,4 y, al 200 %, «Anterior» queda como un icono de 56 dp. Incumple UI-01. | `11-…-100-clar` · `QuestionScreen.kt:185-198` |
| D7 | **Píldora del reloj gigante.** Tiene 48 dp de alto visual y fondo gris: compite con el enunciado. | `11-…-100-clar` · `QuestionScreen.kt:310-318` |
| D8 | **Cert/Fals:** dos cajas de 96 dp para una palabra cada una. | `14-…-100-clar` · `Theme.kt:144` |

## 2. Dirección: «Acta compacta»

Mismo papel y misma tinta, pero con densidad de formulario oficial moderno.

| Elemento | Cambio |
|---|---|
| Enunciado | Protagonista, 20 sp |
| Opciones | Tarjetas ligeras: filete fino de 3:1 y relleno solo al marcar |
| Selección | Borde de tinta de 2 dp + velo de tinta + indicador lleno, sin bloque negro |
| Insignia de aviso | El único color cálido de la pantalla (UI-02) |
| Barra inferior | 2 botones gemelos (UI-01) |

**Densidad adaptable sin scroll (UI-04):**
- Hay dos niveles tipográficos, **N** (normal) y **C** (compacto). El nivel se elige **por pregunta** midiendo si el contenido cabe.
- El scroll solo aparece si ni el nivel C cabe; en la práctica, a partir del 130-200 % o con preguntas más largas que el perfil actual.

## 3. Tokens

### 3.1 Color: solo se añade, no cambia ningún rol existente salvo uno

Contrastes calculados con la fórmula WCAG 2.x (script Python; no medidos en pantalla).

| Token (nuevo en `ArbColors`) | Claro | Oscuro | Contraste |
|---|---|---|---|
| `optionOutline` (filete de la opción sin marcar) | `#87938C` | `#6B8173` | 3,19:1 sobre `#FFFFFF` · 3,88:1 sobre `surface` `#12241A` (≥ 3:1, WCAG 1.4.11) |
| `markContainer` (velo de la opción marcada) | `#E2EAE5` | `#24412F` | Texto `onSurface` encima: 13,1:1 · 9,7:1 |
| `mark` (borde de 2 dp y el indicador lleno; ya existe) | `#18241D` | `#E9F0EA` | 13,6:1 sobre `markContainer` · 9,8:1 |
| `warnBadge` / `onWarnBadge` (UI-02) | `#F5C443` / `#2B1F00` | ídem (mismo ámbar) | Texto 9,9:1 (AAA) · la insignia sobre el fondo: 1,63:1 en claro y 10,97:1 en oscuro |
| **Cambia:** `LightScheme.secondaryContainer` | `#E1EFE5` → `#D4E7DA` | `#1B4A31` (sin cambios) | `onSecondaryContainer` `#0F3D26`: 9,47:1 · `#D5EEDD`: 8,26:1 |

**Sobre `warnBadge`:**
- Es más saturado que el `#FFE08A` del aviso de tiempo, por dos motivos: que se lea como «insignia» y no como «banner», y que no se confunda con la píldora del reloj a 5 min.
- La insignia no es interactiva, así que WCAG 1.4.11 no le exige un borde de 3:1. Lo que da la información es el texto (9,9:1).

**Sobre `secondaryContainer`:** hoy no se usa en `ui/` (`grep secondaryContainer`). Pasa a ser el relleno del botón tonal «Anterior» (§3.5).

### 3.2 Tipografía

Son estilos nuevos en `ArbText`; **no se tocan** `question`, `option` ni `progressLabel`, que usa también el repaso (`ScreensResult.kt:421, 443, 511`).

| Token | sp / interlínea / peso | Uso |
|---|---|---|
| `qHeader` | 16/22/700 | «Pregunta 2 de 5» y «Veure-les totes» |
| `questionN` · `questionC` | 20/26/600 · 18/24/600 | Enunciado |
| `optionN` · `optionC` | 18/24/400 · 16/22/400 | Opción, **sin cambiar el peso al marcar** (D4) |
| `optionTf` (cambia; solo lo usa `QuestionScreen`) | 22/30/600 → **20/26/600** | Cert/Fals |
| `badge` | 16/22/700 | «Resposta múltiple» |
| Pista (`bodyMedium` del tema) | 16/24/400, `onSurfaceVariant` (9,1:1) | Las tres pistas («Tria una resposta.», etc.); hoy la de la única y la de V/F van en `bodyLarge` 18/28 |
| Reloj | `labelMedium` 16/22/600; «mm:ss» en `ArbText.timer` | — |

Ningún estilo baja de 16 sp (04 §8.1).

### 3.3 Medidas: objeto nuevo `QDimens`, solo para la pantalla de pregunta

| Token | Valor | Token | Valor |
|---|---|---|---|
| `gutter` | **16** (hoy 20) | `headerRow` | 48 |
| `progress` | 4 (hoy 8) | `contentTop` | 8 |
| `badgeGap` (insignia → enunciado) | 6 | `qHintGap` | 4 |
| `hintOptionsGap` | 12 | `optionGap` N/C | 8 / 8 (≥ 8 entre objetivos táctiles) |
| `optionVPad` N/C | 10 / 8 | `optionHPad` | 12 a cada lado |
| `optionMin` N/C | **56 / 52** | `optionMinTf` | 64 |
| `indicator` | 24 (hoy 28) | `indicatorGap` | 12 |
| `navButton` | 52 (mínimo) | `navVPad` / `navGap` / `navHPad` | 8 / 8 / 10 |
| `radius` opción y botón | 12 (`shapes.medium`, sin cambios) | `badgeRadius` | 14 (píldora con 28 dp de alto) |

Todos los objetivos táctiles miden ≥ 48 dp: opciones 52-64, botones 52 y filas de cabecera 48.

## 4. Componentes

### 4.1 Cabecera: 104 dp en lugar de ~150

```
┌ status bar ───────────────────────────────────┐
│ Pregunta 2 de 5                     (⏱ 5 min) │ fila A · 48 dp
│ ▬▬▬▬▬▬▬▬▬▬▬▬▬────────────────────────────────  │ progreso 4 dp (4 dp por encima)
│ Respostes: 2 de 5            Veure-les totes › │ fila B · 48 dp
└───────────────────────────────────────────────┘
```

**Fila A:**
- `qHeader` en `onBackground`. Semántica, `FocusRequester` y anuncio sin cambios.

**Reloj** (`TimerPill`):
- Píldora **visual de 36 dp**, centrada en una caja clicable de 48 dp (objetivo táctil intacto). El *ripple* se recorta a la píldora.
- Padding horizontal 12; icono de 18 dp y 6 dp hasta el texto.
- El fondo en reposo pasa a `surfaceContainer` (`#F2F5F3`, más ligero); los estados ámbar y rojo no cambian.

**Progreso:**
- `LinearProgressIndicator` de 4 dp, `StrokeCap.Round`.
- Pista `surfaceContainerHighest`; la gespa sobre la pista da 9,55:1.

**Fila B:**
- Recuento en `bodyMedium` `onSurfaceVariant`.
- `TextButton` «Veure-les totes» en `qHeader` `primary`, con un chevron `KeyboardArrowRight` de 20 dp al final y `contentPadding(start = 12, end = 0)`.
- Ancho en el peor caso: «Respostes: 30 de 30» (146 dp) + «Veure-les totes ›» (139 dp) = 293 ≤ 328 ✓.

**Con `fontScale ≥ 1,5`:** se queda el comportamiento actual (la fila B baja al contenido).

### 4.2 Insignia «Resposta múltiple» (UI-02)

```
 ╭──────────────────────────╮
 │ (●!)  Resposta múltiple  │  28 dp alto · relleno #F5C443 · sin borde
 ╰──────────────────────────╯
```

**Composable `MultiAnswerBadge()`** (sustituye a `MultiChip`, `QuestionScreen.kt:367-381`):
- Contenedor: `Row` con `background(warnBadge, RoundedCornerShape(14.dp))`, `heightIn(min = 28.dp)` y padding `start 4 / end 12 / vertical 3`. Ocupa **solo el ancho de su contenido** (`wrapContentWidth`), nunca `fillMaxWidth`.
- **Icono «!»:** un disco de 20 dp (`CircleShape`) relleno de `onWarnBadge` `#2B1F00` y, dentro, el drawable **existente** `R.drawable.ic_priority_high` (glyph «!» de Material Symbols *priority_high*) a 14 dp, teñido de `warnBadge` (9,9:1). El disco escala con `14.sp.toScaledDp()` hasta un máximo de 32 dp. **No se crea ningún asset.**
- Separación: 6 dp entre el disco y el texto.
- Texto: `q_type_multi` en `ArbText.badge`, `onWarnBadge`, `maxLines = 2`. Al 200 % y con 360 dp de ancho no cabe en una línea (336 dp de 328): pasa a 2 líneas y el radio de 14 deja de ser una píldora completa. Así no se trunca (*Compact Label Overflow*).
- **Posición: «antetítulo» encima del enunciado**, con `badgeGap` de 6. La ayuda `multi_helper` va debajo del enunciado, en el mismo sitio que «Tria una resposta.» (§6 Q1).
- **Semántica:**
  - sin `Role` ni `clickable`, sin *ripple*;
  - `Modifier.semantics(mergeDescendants = true) {}` y el icono con `contentDescription = null`; TalkBack lee «Resposta múltiple»;
  - `testTag("multi_badge")` en el contenedor y `testTag("multi_badge_icon")` en el disco.
- **Eliminar** `res/drawable/ic_check_box_outline.xml`, porque solo lo usa `MultiChip`. El test comprueba que dentro de `multi_badge` no hay ningún nodo con su id ni con `Role.Checkbox`.

**Por qué no se puede confundir con una opción:**

| Rasgo | Insignia | Tarjeta de opción |
|---|---|---|
| Forma | Píldora de 28 dp de alto, ancho del contenido | Rectángulo de radio 12, ancho completo, ≥ 52 dp de alto |
| Color | Ámbar macizo, el único color cálido de la pantalla | Blanco con filete gris, o velo de tinta |
| Glyph izquierdo | Disco **lleno** oscuro con «!» | Aro **hueco** (radio) o cuadrado hueco (casilla) |
| Texto | 16 sp/700, tinta marrón | 16-18 sp/400 |
| Posición | Encima del enunciado, separada de la lista por todo el enunciado y la ayuda | Agrupadas debajo, con un ritmo de 8 dp |
| Interacción | No responde al toque ni tiene rol | *Ripple* y `Role.RadioButton` o `Role.Checkbox` |

### 4.3 Tarjeta de respuesta: selección ligera y estable

**Sin marcar:**
- Fondo `surface`, borde de **1 dp** `optionOutline`.
- Indicador de 24 dp con un aro de 2 dp `outline` (`#66736B`, 4,97:1).
- Texto `onSurface` 400.

**Marcada:**
- Fondo `markContainer`, borde de **2 dp** `mark`.
- Indicador:
  - radio: aro de 2 dp `mark` con punto de 12 dp `mark`;
  - casilla: cuadrado de radio 5 relleno de `mark` con ✓ (`Icons.Outlined.Check`) de 18 dp en `onMark`.
- Texto en el **mismo peso** (400) y color `onSurface`.

**Estabilidad (D4):**
- El borde se dibuja con `Modifier.border`, que no ocupa layout, y el **padding es fijo** (`optionHPad`/`optionVPad`), sin el `- bw` actual.
- Al marcar no cambia ni el tamaño ni la posición del texto. El test `ComponentsTest.kt:169-170` ya lo comprueba; se amplía a opciones de 3 líneas.

**No depende solo del color:**
- el grosor del borde (×2);
- el relleno del indicador (aro → punto, o ✓);
- el rol `selected`.

**Movimiento:**
- Color de fondo y borde con `animateColorAsState`; ancho del borde con `animateDpAsState`. Todo `tween(120)`, o `snap()` si se reducen las animaciones; se mantiene `reduceMotion()`.
- Al pulsar, *ripple* `onSurface` α 0,10.

**Disposición:**
- `Row(verticalAlignment = Top)` y el indicador con `padding(top = max(0, (lineHeight − 24)/2))`, para alinearlo con la primera línea.
- `heightIn(min = optionMin[N|C])` y el contenido centrado en vertical cuando ocupa una sola línea.
- Con `saveError`, α 0,38 (sin cambios).

**Cert/Fals:** dos tarjetas **lado a lado** (`Row` con `weight(1f)` cada una y 8 dp de separación), de 64 dp de alto, con el indicador y el texto centrados en `optionTf`. `Modifier.selectableGroup()` en la fila. Ahorra unos 140 dp frente a hoy. Pendiente de `ux-designer` (§6 Q3); si se rechaza, apiladas a 64 dp.

### 4.4 Barra Anterior/Següent (UI-01)

```
│ [   Anterior   ]  [   Següent    ] │  2 × 160 dp a 360 dp · 52 dp de alto · 8 dp de separación
```

**Estructura:**
- `Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = spacedBy(8.dp))`.
- Los dos botones llevan `Modifier.weight(1f).fillMaxHeight().heightIn(min = 52.dp)`. **El mismo `weight` en las dos escalas de fuente**: desaparece la rama `isExtraLargeText()` con el `OutlinedIconButton` de 56 dp (`QuestionScreen.kt:181-193`).
- El intrínseco está justificado: es una sola fila de 2 hijos y el aviso «evitar intrínsecos» de la búsqueda de Compose apunta a listas.

**Jerarquía solo por estilo, nunca por tamaño:**

| Botón | Estilo | Colores |
|---|---|---|
| «Següent» | `Button` **relleno** | `primary` `#0F3D26` / `onPrimary`: 12,2:1 (oscuro `#E3F1E7`/`#0F3D26`: 10,5:1) |
| «Anterior» | `FilledTonalButton` | `secondaryContainer` `#D4E7DA` / `onSecondaryContainer` `#0F3D26`: 9,47:1 (oscuro `#1B4A31`/`#D5EEDD`: 8,26:1) |

- Sin borde: el tono es más ligero que el contorno de 2 dp actual.
- Los dos con radio 12 y `contentPadding(horizontal = 10, vertical = 8)`, sin iconos (el ancho no da: §6 Q4).

**Etiqueta:**
- `labelLarge` 18/24/700, centrada.
- `maxLines = 2`, `autoSize = TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = 18.sp)` y `overflow = Clip`. Nunca elipsis ni palabras partidas.
- Medido con la fuente: «finalitzar» a 16 sp × 2 = 135 dp ≤ 140 ✓ al 200 %. «Revisar i finalitzar» a 18 sp ocupa 155 dp y en 360 dp pasa a 2 líneas: **los dos botones crecen a la vez** (64 dp) por `IntrinsicSize.Min`.

**En la pregunta 1** solo se muestra «Següent». UI-01 aplica «cuando ambos se muestran»: hoy ocupa todo el ancho. Hay una alternativa en §6 Q5.

## 5. Presupuesto vertical (360 × 800 dp, 100 %, útil = 700 dp)

**Supuestos:**
- 800 dp − barra de estado (24-32) − navegación (24 con gestos / 48 con 3 botones) ≈ 720-744 dp. Se presupuestan **700** (el caso conservador).
- Líneas calculadas con las métricas reales de `atkinson_next.ttf` (fontTools, avance sin kerning, así que es conservador) sobre 300 muestras de `docs/reglament/rules.json`. Se toma el **p95** de líneas. No se ha leído el banco real.
- Ancho del enunciado: 328 dp. Ancho de texto de la opción: 328 − 12 − 24 − 12 − 12 = **268 dp**.

**Coste fijo:** cabecera 104 + `contentTop` 8 + inferior 8 + barra (8 + 52 + 8) 68 = **188 dp**. Quedan **512 dp** de contenido.

| Caso | Nivel | Antetítulo | Enunciado | Pista | Opciones (alto de cada tarjeta) | Total contenido | ¿Cabe? |
|---|---|---|---|---|---|---|---|
| Típico: única, enunciado de 80, 4 × 40 | N | — | 3 × 26 = 78 | 4 + 24 + 12 | 68 × 4 + 3 × 8 = 296 | 414 | ✓ (98 de margen) |
| Real peor: múltiple, 132 · 97/96/41/26 | N | 34 | 5 × 26 = 130 | 4 + 48 + 12 | 116 + 116 + 68 + 56 + 24 = 380 | 608 | ✗ → C |
| ídem | **C** | 34 | 4 × 24 = 96 | 4 + 48 + 12 | 82 + 82 + 60 + 52 + 24 = 300 | **494** | ✓ (18; con el enunciado en 5 líneas, −6 → hace falta Q2 o 1 línea de pista) |
| Real peor, única | C | — | 96 | 4 + 24 + 12 | 300 | 436 | ✓ |
| Real 2: 132 · 69/57/54/30, única | N | — | 130 | 40 | 92 × 3 + 56 + 24 = 356 | 526 | ✗ → C |
| ídem | C | — | 96 | 40 | 82 + 60 + 60 + 52 + 24 = 278 | 414 | ✓ |
| 5 opciones (132 · 69/57/54/30/30), única | C | — | 96 | 40 | 306 + 32 = 338 | 474 | ✓ |
| 5 opciones, múltiple | C | 34 | 96 | 4 + 48 + 12 | 338 | 532 | ✗ (−20); **✓ con la ayuda en 1 línea (508)**: §6 Q2 |
| V/F (enunciado de 132) | N | — | 130 | 40 | 64 lado a lado | 234 | ✓ |
| Futuro: 200 · 120/120/60/30, única | C | — | 6 × 24 = 144 | 40 | 104 + 104 + 60 + 52 + 24 = 344 | 528 | ✗ (−16) → scroll |
| Futuro, múltiple | C | 34 | 144 | 64 | 344 | 586 | ✗ (−74) → scroll; límite editorial en §6 Q6 |

**Alturas de tarjeta:**
- N: líneas × 24 + 20; mínimo 56.
- C: líneas × 22 + 16; mínimo 52.

**Algoritmo de nivel** (`android-developer`):
1. Un `SubcomposeLayout` (o `BoxWithConstraints` + `rememberTextMeasurer`) mide el contenido de la pregunta en N con alto libre.
2. Si supera `maxHeight` del área de scroll, recompone en C.
3. Clave: `(st.index, fontScale, ancho, alto)`.
4. **El aviso de tiempo no entra en el cálculo** (`TimeBannerCard`), para que no salte el nivel a media pregunta. Mientras se ve, puede aparecer scroll; hay una alternativa en §6 Q7.
5. Si C tampoco cabe, se mantiene C y se hace scroll, con el separador superior de la barra que ya existe.

## 6. Preguntas (no se implementan sin respuesta)

| # | A quién | Pregunta | Propuesta por defecto |
|---|---|---|---|
| Q1 | `ux-designer` | ¿Insignia como antetítulo, **encima** del enunciado? Hoy va debajo; el cambio es solo de orden visual. | Sí. Si no, debajo del enunciado con 8 dp por arriba y 12 por abajo. |
| Q2 | `ux-designer` | `multi_helper` → «Marca totes les correctes.» (1 línea, 26 caracteres) | Sí: hace falta para que quepan las múltiples de 5 opciones y el peor caso real con el enunciado en 5 líneas (UI-04). |
| Q3 | `ux-designer` | ¿Cert/Fals lado a lado? | Sí. |
| Q4 | `ux-designer` | En 360 dp, «Revisar i finalitzar» pasa a 2 líneas (los 2 botones miden 64). ¿Se acepta, o una etiqueta más corta? | Aceptar las 2 líneas. |
| Q5 | `ux-designer` | En la P1, ¿«Següent» en la mitad derecha (misma posición en todas las preguntas) en lugar de a todo el ancho? | No cambiarlo sin confirmación. |
| Q6 | Usuario / `bank-curator` | Límite editorial para el banco futuro: con 4 opciones, enunciado ≤ 160 caracteres y opción ≤ 100; con 5 opciones, opción ≤ 70. Más largo implica scroll al 100 %. | Adoptarlo como regla de validación del banco. |
| Q7 | `ux-designer` | ¿El aviso de 5/1 min **sustituye temporalmente a la fila B** (48 dp, mismo alto) en lugar de empujar el contenido? | Opcional; no aplicar sin respuesta. |
| Q8 | Usuario | Esto abandona la selección «invertida» (bloque negro, P3 de la spec del 22-11) en favor de la «suave» que ya se ofrecía como alternativa. | Suave: es más moderna y no pesa. |

## 7. Impacto en otras pantallas (acotación del alcance)

| Cambio | Afecta a | Riesgo / decisión |
|---|---|---|
| `ArbColors` + `optionOutline`, `markContainer`, `warnBadge`, `onWarnBadge` | Solo a quien los use (pregunta) | Nulo |
| `LightScheme.secondaryContainer` `#D4E7DA` | Componentes M3 tonales; hoy no se usa ninguno | Bajo: revisar 06/20/30 con capturas |
| `ArbText.optionTf` 20/26 | Solo `QuestionScreen` | Nulo |
| Estilos nuevos `qHeader`, `question*`, `option*`, `badge`; **no** se tocan `question`, `option` ni `progressLabel` | Repaso (`ScreensResult.kt:421-511`) sin cambios | Queda una incoherencia visual entre la pregunta y el detalle del repaso. Propuesta posterior: llevar `QDimens`/`option*` al repaso (42/44) en otra tarea |
| `QDimens.gutter` 16 | Solo si `ScreenScaffold` recibe un parámetro `gutter` (por defecto 20) | Nulo. Si el usuario quiere compactar toda la app, se baja `ArbDimens.gutter` a 16 en otra tarea, con capturas de todas las pantallas |
| `TimerPill` (36 visual / 48 táctil, fondo `surfaceContainer`) | También el resumen (`ScreensOverview.kt:57`) | Deseable y coherente; capturas 20/22 |
| `NavBar` 50/50 | Solo la pregunta | El repaso tiene su propio Anterior/Següent (`ScreensResult.kt:323-340`). **¿UI-01 aplica también ahí?** Si sí, mismo componente `NavPair`. Pregunta para el usuario |
| Eliminar `ic_check_box_outline.xml` | Solo `MultiChip` | Nulo |

## 8. Aceptación (mecánica y visual)

**Escala y emulador:**
- Capturas en un emulador a **360×800 dp**: `adb -s emulator-5554 shell wm size 1080x2400 && wm density 480`. Escenas 10-15 a 100, 130 y 200 %, en claro y en oscuro.
- UI-04 se valida con el banco real **fuera del repo** (`arbitratge-bank/work/qa/`), con la lista de preguntas que hacen scroll y el porqué.

**Comprobaciones:**

| Decisión | Comprobación |
|---|---|
| UI-01 | Test con `boundsInRoot` de «Anterior» y «Següent»: el mismo `width` ±1 px y el mismo `height` ±1 px a 1,0, 1,3 y 2,0. Captura `11-…` a 100 y 200 %. |
| UI-02 | Test: `multi_badge` contiene `multi_badge_icon`, sin `Role.Checkbox` ni el drawable de casilla, y no es clicable (`assertHasNoClickAction`). Captura `12/13-…-100-clar` y `-fosc`. |
| UI-03 | La fija el usuario con las capturas. *Pre-flight* anti-slop: 1 acento (gespa), 1 color cálido (la insignia), radios 12/14/píldora, iconos de contorno salvo el «!» de aviso. |
| Estabilidad | Test: el alto de `OptionCard` con 3 líneas es igual marcada y sin marcar. |
| Contraste | Medir con cuentagotas sobre la captura: `optionOutline` ≥ 3:1, el texto de la insignia ≥ 7:1 y el borde de la marcada ≥ 3:1, en los dos temas. |

## 9. Consultas registradas

Se leyeron `ui-ux-pro-max/SKILL.md` y `references/pro-rules.md`. Comando: `python .agents/skills/ui-ux-pro-max/scripts/search.py …`, desde la raíz.

| Consulta | Resultado | Aplicación |
|---|---|---|
| `"selectable card radio selected state" --stack jetpack-compose` | 0 resultados | Reintento → |
| `"intrinsic size layout" --stack jetpack-compose` | *Avoid intrinsic size unless necessary* | Se usa solo en la fila de 2 botones (§4.4) |
| `"design system theming" --stack jetpack-compose` | *Material3 tokens*, *No hardcoded text style* | Tokens en `ArbColors`/`ArbText`/`QDimens` (§3) |
| `"semantics accessibility" --stack jetpack-compose` | `Modifier.semantics`, `contentDescription` | Insignia sin rol, icono decorativo (§4.2) |
| `"equal width buttons row weight"`, `"badge chip warning"`, `"font scale large text"` (`--stack jetpack-compose`) | 0 resultados | Se recurrió a las de dominio `ux` |
| `"modern compact mobile exam" --domain style` | *Material 3 Expressive*: píldoras, contenedores tonales, *state layers* | Botón tonal, insignia en píldora y *ripple*. **Descartado:** botones en píldora (rompe el *shape lock*; afectaría a todas las pantallas) y el violeta |
| `"selected state color not only" --domain ux` | *Color Only* (High) | Borde ×2 + indicador lleno + rol (§4.3) |
| `"badge chip label wraps" --domain ux` | *Compact Label Overflow* (High) | `maxLines = 2` sin truncar (§4.2) |
| `"touch target spacing density" --domain ux` | *Touch Target Size* 48 dp, *Touch Spacing* ≥ 8 | Mínimos de 52 y 48; 8 dp entre tarjetas y entre botones |
| `"text resize zoom 200%" --domain ux` | *Text Reflow* (Critical) | Sin alturas fijas; `autoSize` solo de 18 a 16 sp |
| `"warning exclamation alert" --domain icons` | *warning-circle* (Phosphor) | Mismo concepto con el `ic_priority_high` de Material ya presente (una sola familia) |

**Checklist de `pro-rules` aplicado:**
- estados de pulsación sin cambiar los límites (D4);
- iconos de una sola familia;
- contraste comprobado por tema;
- barras que no tapan contenido;
- ritmo 4/8.

**Otras skills:**
- **`ui-taste`:** `anti-slop-craft` (un acento, *shape lock*), `distill` (fuera la caja de la insignia y el bloque negro) y `mobile-controls` (pocas opciones excluyentes → controles segmentados: Cert/Fals lado a lado).
- **`mobile-android-design`:** `FilledTonalButton`, `selectableGroup`, `minimumInteractiveComponentSize` y `TextAutoSize`.
