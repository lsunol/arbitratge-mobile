# Spec UX previa — pantalla de pregunta: densidad, 50/50, aviso múltiple (2026-11-23)

Rol: `ux-designer` (paso 1 de AGENTS.md). No se ha tocado código. Alcance: `QuestionScreen.kt`, `ScreenScaffold`
(Components.kt:110), `ArbDimens`/`ArbText` (Theme.kt:132, 196) y el aviso «Resposta múltiple».

## 0. Decisiones vinculantes (`docs/design/decisiones-usuario-ui.md`) — no se reinterpretan

| ID | Se aplica tal cual |
|---|---|
| UI-01 | Anterior y Següent: **50 % exacto**, mismo ancho y alto. Sin `weight` distinto, **tampoco a 200 %** (el test pide 100/130/200 %). |
| UI-02 | «Resposta múltiple» = insignia **rellena, color de aviso, con «!»**; nunca casilla ni contorno. |
| UI-03 | Moderna y **compacta**; «massa espaiosa» es defecto. |
| UI-04 | Peor caso real **sin scroll a 100 %** en ~360×800; scroll solo a 200 % como último recurso. |

**Causa raíz reconocida (hecho):** `ui-revamp-2026-11-22/ux-designer-spec.md` ~L10/L52 fijó Següent 1,4 / Anterior 1, y
el código lo implementa (`QuestionScreen.kt` `weight(if (st.canGoBack) 1.4f else 1f)`); además a ≥1,5× sustituye
Anterior por un `OutlinedIconButton` de 56 dp (≈ 18/82), lo que **también viola UI-01**. Esta spec revoca ambos puntos.
Ningún criterio de «jerarquía de acción principal» justifica pesos distintos: la jerarquía se da con el **estilo**
(Següent relleno, Anterior contorno), no con el ancho.

## 0.1 Consultas `ui-ux-pro-max` (registro)

SKILL.md y `references/pro-rules.md` leídos. Búsquedas (`scripts/search.py`):
1. `"mobile density compact vertical space no scroll" --domain ux` → sin resultado útil móvil (solo web). Se usa el checklist
   de pro-rules: objetivo táctil ≥48 dp, ritmo 4/8 dp, «scroll content not hidden behind fixed bars», Dynamic Type sin romper layout.
2. `"multi-select checkbox warning badge" --domain ux` → solo «Progress Indicators» / badges live: confirma que no hay
   regla que obligue a repetir un recuento en pantalla; el estado debe ser contextual, no un número suelto.
3. `"bottom navigation buttons equal width previous next" --domain ux` → sin patrón específico; aplica «Stable Interaction States»
   (pro-rules: sin cambios de bounds al pulsar) y «Disabled state clarity».
4. `"disabled button first step" --domain ux` → «Disabled States: reducir énfasis, no confundir con habilitado» y «Back Button: comportamiento predecible».
5. `"spacing touch target text scaling" --stack jetpack-compose` → «No hardcoded text style», Arrangement/Alignment declarativos.
Lectura: no hay hallazgo que contradiga las decisiones UI-01..04.

## 1. Estado actual medido (hecho, desde el código) y por qué no cabe

Presupuesto vertical actual a 100 %: cabecera ≈ 8+48(fila título/reloj)+4+8(barra)+4+48(fila «Respostes N de M»/«Veure-les totes»)+8 ≈ **128 dp**;
NavBar 56+2×10 = **76 dp** (+divisor); relleno de contenido 12+12; enunciado 24/34 sp, opciones 20/28 sp con mínimo 64 dp,
gap 12, V/F 96 dp, `single_hint`/`tf_hint` (~28 dp) y `MultiChip` (≈36 dp) + `multi_helper` (≈24). Con q-0006 (4 opciones, 97/96 car.)
a 252 dp de ancho útil de opción (≈24 car./línea) salen 4-5 líneas por opción → ~700 dp de contenido frente a ~480 disponibles. Hecho: no cabe.
(Anchos de carácter son estimaciones de Atkinson Next; **[PV]** medir en emulador.)

## 2. Especificación por decisión

### 2.1 UI-04 + UI-03 — presupuesto vertical (100 %, 360×800, útil 700 dp)

Cambios (todos medibles):

| Elemento | Hoy | Nuevo |
|---|---|---|
| Gutter horizontal de esta pantalla | 20 | **16 dp** (nuevo parámetro `gutter` en `ScreenScaffold`; el resto de pantallas no cambia) |
| Cabecera | 3 filas, 128 dp | **1 fila, 56 dp** (4 + 48 + 4). Contiene: «Pregunta N de M» (16 sp/22, w700) · píldora del reloj (alto visual 40, zona táctil 48) · **icono-botón «resum»** 48×48 |
| Barra de progreso (`LinearProgressIndicator`) | 8 dp + gap | **Se quita** (redundante con «Pregunta N de M»). Alternativa solo si el usuario la quiere: 3 dp pegada al borde inferior de la cabecera, +4 dp |
| Fila «Respostes N de M» + TextButton «Veure-les totes» | 48 + gap | **Se fusiona**: el botón pasa a icono (lista/checklist, 48×48) en la fila de cabecera con `contentDescription` «Veure resum: N de M respostes». El recuento deja de verse en la pregunta (sigue en el resum y en TalkBack). *Pérdida señalada: ver §6-P2.* |
| `single_hint` («Tria una resposta.») y `tf_hint` | ~28 dp | **Se eliminan** (el control radio/Cert-Fals ya lo comunica; TalkBack anuncia rol). V/F conserva solo las dos opciones |
| `multi_helper` + `MultiChip` | ≈60 dp | **Una sola insignia de 32 dp** (§2.3) |
| Enunciado | 24 sp / 34 | **20 sp / 26, w600** (`ArbText.question`) |
| Opciones | 20/28, mín 64, relleno v 12, h 16 | **16 sp / 22, mín 48, relleno v 8 / h 12**, gap entre opciones **8 dp**, indicador **24 dp** (era 28), separación indicador-texto 12→10 |
| Opción V/F (`optionTf`) | 22/30, mín 96 | **18 sp / 24, mín 56 dp**, apiladas, gap 8 |
| Relleno de contenido | top 12, bottom 12 | **top 4, bottom 8** (la cabecera ya aporta 4) |
| NavBar | botones 56 + 2×10 = 76 | botones **48 dp** + 2×8 = **64 dp** |
| Divisor sobre la barra | si `canScrollForward` | igual (en 100 % no debe aparecer; es una señal de test) |

Ancho útil (360 − 2×16 = 328 dp). Texto del enunciado: ≈34 car./línea. Texto de opción: 328 − 24 (relleno) − 24 (indicador) − 10 = 270 dp ≈ 33 car./línea.

**Presupuesto dp a dp** (útil 700 dp):

| Bloque | q-0006 peor caso | Caso largo 200/120, 4 opc. |
|---|---|---|
| Cabecera | 56 | 56 |
| Relleno superior | 4 | 4 |
| Enunciado (hasta 132 car. → 5 líneas × 26 = 130 · hasta 200 car. → 6 líneas × 26 = 156) | 130 | 156 |
| Gap enunciado→insignia | 8 | 8 |
| Insignia múltiple (solo MC) | 32 | 32 |
| Gap →opciones | 12 | 12 |
| Opciones (líneas×22 + 16; mín 48): q-0006 = 97→3-4 l (104), 96→3-4 l (104), 41→2 l (60), 26→1 l (48) | 316 + 3×8 = 340 | 120→4 l 104, 90→3 l 82, 60→2 l 60, 30→1 l 48 = 294 + 24 = 318 |
| Relleno inferior | 8 | 8 |
| NavBar | 64 | 64 |
| **Total** | **654** | **658** |
| **Holgura sobre 700** | **46 dp** | **42 dp** |

Para una pregunta single (sin insignia) se recuperan 40 dp (32+8). V/F: 56+8+56 = 120 dp de opciones, sobra mucho.

Límite garantizado: cabe sin scroll si `altura(enunciado) + altura(opciones) ≤ 568 dp` (700 − 56 − 4 − 8 − 64) en single, ≤ 528 dp en MC
(restando 40 dp de insignia+gap) . Casos que **no** caben (se aceptan y se informan, UI-04): 4 opciones todas de ~120 car. con enunciado de
200 (≈ 456 + 156 = 612) o 5 opciones largas. Solución de último recurso permitida: scroll; **no** bajar de 16 sp en opciones ni de 48 dp táctiles,
ni quitar el nav fijo. Si el usuario quiere cubrir también esos casos, la única palanca restante es 15 sp (viola mínimo 16 sp de legibilidad móvil: no recomendado).

Moderno sin gastar altura (UI-03; detalle visual lo cierra `ui-designer`): jerarquía por peso/color en vez de aire; tarjetas tonales con borde
de 1,5 dp que mantiene ≥3:1 (pro-rules «Icon/boundary contrast»); sin sombras ni márgenes decorativos. «Moderno» lo valida el usuario sobre captura.

### 2.2 UI-01 — Anterior / Següent 50 %

- `Row(Modifier.fillMaxWidth(), spacedBy(8.dp))` con **dos hijos `weight(1f)`** y altura única: `Modifier.height(IntrinsicSize.Min)` en la Row y `fillMaxHeight()` en ambos botones, de modo que si uno envuelve a 2 líneas el otro crece igual. Anchos a 100 %: (328−8)/2 = **160 dp** cada uno.
- Jerarquía por estilo: Següent `PrimaryButton` (relleno), Anterior `SecondaryButton` (contorno 2 dp). Mismo alto mín 48 dp, misma forma.
- Etiquetas: 16 sp w600, `maxLines = 1` a 100 %; relleno horizontal 12 dp (8 dp a ≥130 %). A 130 % «Següent»/«Anterior» caben (≈70-80 dp de texto en 144 dp).
- **Se elimina la rama `isExtraLargeText()` del `NavBar`** (botón-icono 56 dp). A 200 % siguen dos botones 50/50; texto 32 sp ≈ 128 dp en 144 dp útiles → una línea; si no cabe, 2 líneas (nunca partir palabra; altura igual por IntrinsicSize). El nav fijo a 200 % ocupa ≈ 64–96 dp (<14 % de 700).
- Última pregunta: la etiqueta de Següent **«Revisar i finalitzar» (20 car.) no cabe en 160 dp a 16 sp en una línea**. Propuesta: **«Revisar»** (el destino es el resum, donde está «Finalitzar»); alternativa «Finalitzar». Es decisión de copy → §6-P3. Si se mantiene la cadena larga, envuelve a 2 líneas y Anterior iguala altura (nav 72 dp; sigue cabiendo, holgura baja a 38 dp).
- Estados al pulsar: sin cambio de bounds (pro-rules «Stable Interaction States»).

### 2.3 UI-02 — insignia «Resposta múltiple»

- Solo en `MULTIPLE_CHOICE`, **debajo del enunciado**, alineada al inicio, **no a todo el ancho** (ancho = contenido), alto **32 dp**, forma `shapes.extraSmall` (8 dp, rectangular redondeada; **no píldora** para distinguirla de las opciones —12 dp, a todo el ancho— y del reloj —píldora—).
- **Relleno** `BrandColors.WarnAmber`, texto/icono `BrandColors.OnWarnAmber` (ya existen, Components TimerPill). Sin borde.
- Contenido: icono **«!»** (círculo con exclamación relleno, 18 dp; p. ej. `Icons.Filled.PriorityHigh`/`ic_error` filled; NO `ic_check_box_outline`) + texto «Resposta múltiple» 14 sp w600. Relleno interno 10 dp h, 6 dp v.
- No es interactiva, sin ripple, sin `selectable/toggleable`. Texto de ayuda visible **eliminado** (`multi_helper`); la información pasa a la semántica: `contentDescription` = «Resposta múltiple: pot haver-hi més d'una resposta correcta. Marca-les totes.» y el icono `null` (decorativo).
- Contraste texto/fondo ≥4.5:1 en claro y oscuro (medir; si WarnAmber/OnWarnAmber no lo cumple en oscuro se ajusta el token, no el requisito).
- **Riesgo señalado:** la píldora del reloj usa el mismo ámbar a 5 min (`TimerPill`). Mitigación: forma distinta, posición distinta (cuerpo vs. cabecera), icono distinto. Si en captura se confunden, `ui-designer` elige otro tono de aviso (no el rojo de error).
- Las opciones múltiples siguen con casilla cuadrada (`OptionKind.CHECK`); eso es la única «casilla» de la pantalla, así que la insignia no puede confundirse con una opción (test UI-02).

### 2.4 Escala de fuente 130 % / 200 %

- **130 %** (fontScale ≥1,3): mismo layout; cabecera usa «N de M» corto (`progress_short`) para no envolver; insignia/opciones se escalan; scroll tolerado solo en el peor caso, pero se **intenta** que q-0006 no haga scroll (estimación: enunciado 6 l×34 = 204; opciones ≈ 4×(5 l)… probablemente sí scrollea; documentar).
- **200 %**: scroll del cuerpo permitido (UI-04). Cabecera **fija** (1 fila, crece a ~72 dp; reloj sin icono; «N de M»), nav **fijo** 50/50 (no se mueve al final del contenido), insignia y texto crecen. `scrollKey = st.index` se mantiene. Divisor sobre el nav visible al poder hacer scroll. Nada recortado, ningún texto truncado con elipsis.
- En todas las escalas: objetivos ≥48 dp (a 200 % crecen solos), sin scroll horizontal.

## 3. Primera pregunta sin «Anterior» (UI-01)

UI-01 dice «cuando ambos se muestran»: ocultar en P1 no lo incumple literalmente, pero hay que elegir qué ocupa el hueco.

| Opción | Descripción | Pros | Contras / riesgo |
|---|---|---|---|
| **A (recomendada)** | «Anterior» **visible y deshabilitado** en P1; ambos 50/50 siempre | Layout idéntico en todas las preguntas; «Següent» no cambia de posición/ancho (memoria muscular); cumple UI-01 sin excepción ni caso especial; fácil de testear | Control inerte visible (pro-rules «Disabled state clarity»: debe verse inactivo, α 0,38, semántica `disabled`, TalkBack «No disponible»). Algunos usuarios lo percibirán como ruido |
| B | Ocultar «Anterior»; Següent a ancho completo (comportamiento actual) | Sin control muerto | Següent cambia de ancho/posición entre P1 y P2 (salto del objetivo táctil); P1 es la única pantalla con otro layout |
| C | Ocultar «Anterior»; Següent en la mitad **derecha** (50 %), hueco a la izquierda | Posición estable de Següent; 50 % se conserva | Hueco vacío raro, parece un fallo de maquetación |

Recomiendo **A**; riesgo asumido: control inerte. La decisión es del usuario (§6-P1). Hasta que la dé, se implementa A y B queda como cambio de una línea.

## 4. Accesibilidad (se mantiene)

- Foco de entrada y anuncio «Pregunta N de M. Tipus. Estat» por pregunta: sin cambios (`QuestionTopBar` LaunchedEffect) — **[PV] TalkBack en dispositivo**.
- La cabecera conserva `heading()`; el icono «resum» con nombre descriptivo y recuento; enunciado `heading()`.
- Radio/Checkbox nativo, estado no solo por color (peso + control + rol).
- Disabled de Anterior en P1 con `enabled=false` real.
- Orden de foco = visual: cabecera → enunciado → insignia → opciones → Anterior → Següent.
- Reducción de movimiento: se conserva `reduceMotion()`.

## 5. Microcopy (catalán)

| Clave | Texto |
|---|---|
| progress | «Pregunta %1$d de %2$d» (≥130 %: «%1$d de %2$d») |
| a11y botón resum | «Veure resum: %1$d de %2$d respostes» |
| badge múltiple (visible) | «Resposta múltiple» |
| badge múltiple (a11y) | «Resposta múltiple: pot haver-hi més d'una resposta correcta. Marca-les totes.» |
| nav | «Anterior» · «Següent» · última: «Revisar» (propuesta; hoy «Revisar i finalitzar») |
| eliminados de la UI visible | `single_hint`, `tf_hint`, `multi_helper`, `answered_progress` (este último se reutiliza en a11y y resumen) |

## 6. Preguntas abiertas para el usuario

- **P1.** Primera pregunta: ¿A (Anterior deshabilitado visible, recomendado), B (oculto, Següent ancho completo) o C (Següent a la derecha con hueco)?
- **P2.** Para ganar ~48 dp se quita la fila «Respostes N de M»: ahora el recuento solo se ve en el resum (icono en cabecera). ¿Aceptas perder el recuento visible en cada pregunta?
- **P3.** Última pregunta: «Revisar» / «Finalitzar» en lugar de «Revisar i finalitzar» (no cabe al 50 % en una línea). ¿Qué prefieres? (si quieres mantener el texto largo, envolverá a 2 líneas).
- **P4.** ¿Se elimina la barra de progreso (propuesto) o la quieres fina de 3 dp?
- **P5.** El texto de ayuda de «múltiple» pasa a TalkBack; visualmente solo queda «! Resposta múltiple». ¿Basta?

## 7. Comprobaciones que QA debe hacer (con capturas; bloqueantes: FAIL si falta alguna)

Banco de muestra en repo; **real solo en `arbitratge-bank/work/qa/`** (nunca en el repo; UI-04 regla 4). Emulador `emulator-5554`, 360×800, claro y oscuro.

1. **UI-01 (mecánico):** test `boundsInRoot` Anterior vs Següent: `width` y `height` iguales ±1 px en 100 %, 130 %, 200 %, en P1, intermedia y última; sin `weight` distinto (grep). Captura de cada escala.
2. **UI-01 P1:** captura de P1 con la opción de §3 elegida; en A, `Anterior` `isNotEnabled` y bounds = Següent.
3. **UI-02:** test: el aviso contiene icono «!» y ningún glyph/recurso de casilla; no tiene rol Checkbox/Radio/Selectable. Captura de pregunta múltiple (claro y oscuro) mostrando la insignia junto a las casillas sin posible confusión; comprobar que no se parece al reloj ámbar (forzar aviso 5 min).
4. **UI-04:** capturas a 100 % de **todas** las preguntas del banco real (privadas) más las sintéticas: q-0006 (97/96/41/26), q-0008 (69/57/54/30), 53/41/38/26, V/F, y dos sintéticas 200/120 con 4 y 5 opciones. Medir: `canScrollForward == false` en cada una (test que lo afirme) o listarla en el informe con la causa. Esperado: todas las reales actuales sin scroll; sintética 4×120 posiblemente con scroll (documentar).
5. **UI-03:** capturas completas de la pantalla en 100 % para que el usuario juzgue «moderno/compacto»; medir cabecera ≤56 dp, nav ≤64 dp, relleno v. de opción 8 dp, gap 8 dp.
6. **Escalas:** 130 % y 200 %, claro/oscuro: sin texto recortado ni elipsis, sin scroll horizontal, nav visible y fijo, cabecera con «N de M», scroll vertical funcional y que cada pregunta empieza arriba.
7. **Táctil:** todos los controles ≥48×48 dp (opciones, nav, icono resum, píldora reloj) vía `assertHeightIsAtLeast(48.dp)`.
8. **Contraste:** insignia ≥4.5:1, borde de opciones ≥3:1, texto deshabilitado coherente, en claro y oscuro (medido, no supuesto).
9. **Reloj/avisos:** con banner de 5 min y 1 min la pregunta mayor sigue sin scroll (el banner suma ~64 dp: **verificar**; si desborda, el banner pasa a superponerse sobre la cabecera o se acepta scroll puntual; reportar).
10. **TalkBack [PV] en dispositivo:** orden de foco, anuncio de cabecera una sola vez, insignia leída completa, «Anterior» deshabilitado anunciado como no disponible, icono resum con recuento.
11. **Regresión:** persistencia antes de avanzar, navegación libre, diálogos, `ExamDialogs`, banco/contrato intactos.
12. **Informe:** marcar «no verificado visualmente» todo lo sin captura; las estimaciones de ancho de carácter de esta spec (33-34 car./línea) son **suposición** hasta medirlas.
