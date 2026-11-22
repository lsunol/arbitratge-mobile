# UX y percepción de calidad — app del candidato (revisión look&feel)

Autor: ux-designer. Foco: jerarquía, tarea principal, microcopy, estados, consistencia, densidad, accesibilidad para mayores. **No** propongo paleta ni tipografía (lo cubre `ui-designer`); uso los tokens vigentes de `Theme.kt` (roles M3) y solo marco cuando un color es problema de usabilidad.
Evidencia = `docs/design/reviews/evidence/2026-11-22-ui-abans/` (abreviado `E/`). Leídas: 02, 10, 20, 30, 31, 42, 06 (claro 100 %), 42 (oscuro 100 %), 10 y 50 (claro 200 %) y README completo. No abiertas (me baso en README): 01, 03–05, 16–17, 21–23, 40–41, 43–45, 51, 60–61. **TalkBack, estados de tiempo (32–35) y errores no existen en la línea base**: lo marco abajo.

## 1. Diagnóstico: por qué no convence

Hechos verificados en captura/código; los juicios son míos.

1. **Pantallas "de formulario" con la mitad vacía y el contenido flotando.** `E/02-inici-inicial-100-clar.png`: hero + 3 teselas + un párrafo gris suelto arriba; ~50 % de pantalla vacío hasta el botón. Lo mismo en `E/30-resultat-aprovat-100-clar.png` (~1/3 vacío) y `E/10-…-100-clar.png`. El CTA anclado abajo (`ScreenScaffold`, `Components.kt:106-119`) está bien; el problema es que el **contenido no tiene composición**: lista de cosas sin agrupar, sin foco. Efecto: sensación de plantilla sin acabar.
2. **Datos crípticos donde hay sitio de sobra.** Teselas «5 / preg.», «5 / min», «3 / encerts» (`02`, strings `home_fact_*`, `ScreensMain.kt:116-160`): "3 encerts" no dice "para aprobar"; abreviaturas a 18 sp cuando hay ancho libre vertical. El texto completo ya existe (`home_fact_*_full`, `home_facts_a11y`) pero se esconde.
3. **Demasiados bloques con el mismo peso → no hay "lo importante".**
   - `E/06-normes-100-clar.png`: 8 reglas con el mismo icono-círculo, mismo cuerpo, mismo interlineado; la única crítica («Si surts de l'app, el temps continua passant») solo se diferencia por un círculo amarillo. Parece un manual, no una preparación.
   - `E/10-…-100-clar.png`: cabecera con **3 niveles** (título+cronómetro / barra / «Respostes: x de 5 + Veure-les totes») antes del enunciado; el enunciado empieza al 22 % de la altura. «Pregunta 2 de 5» en gris (`ArbText.progressLabel`, `onSurfaceVariant`) es el ancla de orientación pero se ve como metadato.
   - `E/42-…-clar.png`: **triple redundancia** del veredicto: chip «Fallada» + banner «La teva resposta no era la correcta.» (`review_wrong_single`) + etiquetas «Incorrecta · la vas marcar» en cada opción.
4. **Gramática de banners/colores ambigua.** El mismo contenedor verde claro + icono «i» sirve para aviso neutro (nombre), consejo (31: «Repassa les teves fallades…»), éxito (30: «credencial preparada») y veredicto (42). Un mayor no sabe si es bueno, malo o informativo.
5. **Semántica de color confusa en el repaso.** La correcta no marcada va en **amarillo/ámbar con borde discontinuo** (`E/42-…-clar.png`; en oscuro marrón oliva, `E/42-…-fosc.png`). Ámbar = "aviso", pero es "esta era la buena". Y la correcta marcada va en verde: la misma idea ("correcta") con dos colores. Además la selección en el examen es azul (`ArbColors.selection`, `Theme.kt:109`) —decisión previa válida (UX F4)— pero el círculo "contestada" del resumen (`E/20-…`) usa también ese azul: un azul sin función en una app verde se lee como resto de otro tema.
6. **Jerarquía de botones inconsistente.** `E/31-…-clar.png`: relleno / contorno / texto plano a 3 alturas distintas; «Tornar a l'inici» (texto) pesa visualmente menos que una etiqueta. `E/21`/`16`: bien. En `E/10`: «Anterior» (contorno) y «Següent» (relleno) a **50/50** — la acción principal no tiene más peso; «Anterior» deshabilitado en Q1 es un contorno pálido que "parece botón pero no hace nada" (pro-rules, *Disabled state clarity*).
7. **Resultado con datos mal modelados.** `E/31`: «Fallades 5 / 1 sense resposta» junto a «0 de 5» y «Mínim per aprovar: 3» y «Et van faltar 3 encerts»: tres frases dicen lo mismo y la tesela "Fallades" mezcla falladas+vacías (5 de 5 con un anillo en 0). Título en dos líneas («No has aprovat, de / moment»), tono de "castigo". Las teselas dependen de `IntrinsicSize.Max` (`ScreensResult.kt:236`) y se desigualan.
8. **Resumen (`E/20`)**: título «Preguntes de l'examen» y píldora «4 min» **pegados** (misma fila, sin separación); cada fila ocupa 4 líneas («Pregunta 3 · Sense resposta» / «Ets aquí» / enunciado×2); estado codificado casi solo con círculo azul vs vacío; borde negro grueso de «actual» es el elemento más pesado de la pantalla aunque no es el más importante (lo importante son las pendientes).
9. **200 % es donde se rompe la percepción de calidad** (público mayor). `E/10-…-200-clar.png`: el cronómetro cae a línea propia, «Respostes:/2 de 5» se parte, la barra inferior apilada (Següent encima de Anterior, `QuestionScreen.kt:167-171`) ocupa ~25-30 % y la 2.ª opción queda **parcialmente tapada**; `E/50-credencial-200-clar.png`: cabecera de 4 líneas con logo a la izquierda, campos en columna, **el QR no aparece en la primera pantalla** y «Codi» queda cortado; botón «Mostrar a un organitzador» en 2 líneas con icono a un lado y texto centrado (desalineado). 51 y 60 a 200 % no se pudieron capturar (README).
10. **Detalles de alineación que restan "acabado"**: «Canviar» en `E/06` empieza más a la derecha (x≈120) que el nombre (x≈80) por padding del TextButton; en `E/02` la línea del campo atraviesa «de Futbol» del subtítulo (legibilidad del único texto de la cabecera); sombras/bordes de tarjeta de 1 dp casi invisibles en oscuro.
11. **Contraste**: por tokens (`Theme.kt` `onSurfaceVariant` claro #4A5750 sobre #F2F5F3 ≈ 7:1 estimado; oscuro #BAC6BE sobre #0F1512 ≈ 10:1; no medido con herramienta) **no es un fallo AA**; lo que se percibe como "bajo contraste" es **peso 400/16 sp en gris** para etiquetas que sí importan. Se arregla con rol y tamaño, no con color nuevo.

Lo que ya funciona y se conserva: botones ≥56 dp, tipografía ≥16 sp en todo rol (`arbTypography`, `Theme.kt:~165`), enunciado 24 sp (`ArbText.question`), selección con doble codificación (borde+indicador), persistencia/navegación.

## 2. Propuesta priorizada (impacto/esfuerzo)

Nomenclatura: tokens = existentes de `Theme.kt` salvo indicación; dp sobre `ArbDimens` (`Theme.kt:121`). «Verif.» = qué debe verse en la captura DESPUÉS (misma nomenclatura de fichero, en carpeta de evidencia "després"). Todas las verif. valen para claro y oscuro salvo indicación.

### P1 — Alto impacto / bajo esfuerzo

**P1.1 Resultado: un solo bloque de datos + título corto + CTA coherente** (`ScreensResult.kt:166-260`)
- Título: aprobado «Has aprovat» (se mantiene); suspenso **«No has aprovat»** (1 línea a 100 %; `headlineLarge`, `onBackground`). Subtítulo único (`bodyLarge`, `onSurfaceVariant`): suspenso «Calien %1$d encerts i n'has tingut %2$d.» (nuevo string); aprobado «Mínim per aprovar: %1$d» (como hoy). **Eliminar** `result_missing` («Et van faltar…») por duplicado.
- Sustituir las 2 `StatTile` por **una tarjeta** `surfaceContainerLow` + borde `outlineVariant` 1 dp, `large` shape, padding 16 dp, filas «etiqueta … valor» (`bodyLarge` / valor `titleMedium`→600). Filas: «Encerts», «Fallades» (solo falladas con respuesta), «Sense resposta» (**solo si >0**), «Temps». Fila: `Row(SpaceBetween)`; a ≥1,5× pasa a `Column` (etiqueta encima). Sin `IntrinsicSize`.
- Aprobado: el banner «credencial preparada» pasa a **tarjeta de siguiente paso** reutilizando `CredentialMini` (`ScreensMain.kt:166`) con nombre y «APTE», encima del CTA «Veure la credencial». Rellena el hueco con la tarea que sigue. Suspenso: el consejo pasa a texto de apoyo `bodyMedium` `onSurfaceVariant` sin icono ni contenedor (no es un aviso).
- Botones (ambos estados): primario 56 dp, secundario contorno 56 dp, y «Tornar a l'inici» como `TextButton` **min 56 dp, ancho completo**, gap 8 dp (sustituye los alturas ad hoc). Sin cambios de flujo.
- 200 %: disco 168 dp (de 176/200 actuales) para ganar altura; filas apiladas; botones ancho completo; el scroll nunca deja un elemento tapado por la barra: `bottom` inset ≥ altura de barra (ya hay `Scaffold` padding).
- Verif.: `31-resultat-suspes-100-*` título en **una** línea, sin la frase «Et van faltar», una sola tarjeta de datos con 4 filas; `30-resultat-aprovat-100-*` sin vacío >15 % entre tarjeta y botones, con mini-credencial; `31-…-200-clar` y `30-…-200-clar` sin elemento parcialmente tapado ni etiqueta de botón cortada.

**P1.2 Repaso: veredicto una sola vez, correcta siempre verde** (`ScreensResult.kt:405-500`)
- Quitar el banner `review_wrong_single` (y `review_both`/similares solo si no aportan; ver pregunta 3). Mantener chip de estado (Fallada/Encertada/Sense resposta, `StatusBadge`).
- Opciones: etiqueta **solo en 2 casos**, no en las neutras: marcada incorrecta → «La teva resposta» + icono ✕ (contenedor `errorContainer`, borde `error` 2 dp); correcta (marcada o no) → icono ✓ + contenedor `ArbColors.correctContainer`, borde `ArbColors.correct` 2 dp, texto «Resposta correcta» (si además fue marcada: «La teva resposta · correcta»). **Sin amarillo**; correcta no marcada se distingue por borde discontinuo + texto, no por otro color. Desviación respecto a diseño previo: se elimina el ámbar (`tertiaryContainer`) del repaso; marcado como cambio de decisión de UX F-previa; no toca contrato.
- Orden: sin cambios (orden del examen).
- Bloque «Per què?»: contenedor `surfaceContainerHigh`, icono libro 24 dp, título `titleSmall`, cuerpo `bodyLarge` (18 sp). Hoy 16 sp en tarjeta casi invisible (`E/42-…-fosc`).
- Verif.: `42-repas-detall-fallada-100-clar/fosc` sin banner, 2 opciones con etiqueta, 1 de ellas en verde con ✓, ninguna amarilla; `44-…-encertada` coherente (verde + «La teva resposta · correcta»).

**P1.3 Pantalla de pregunta: cabecera simple, acción principal clara** (`QuestionScreen.kt:106-250`)
- Cabecera fija: **fila 1** «Pregunta 2 de 5» (`titleMedium` 600, **`onBackground`**, no gris) + cronómetro a la derecha; **barra de progreso** 8 dp; fila 2 «1 contestada» a la izquierda y «Veure-les totes» a la derecha, **ambas `bodyLarge`**, la primera `onSurfaceVariant`, la segunda `TextButton` con icono lista 20 dp, min 48 dp. Strings: `answered_progress` → «%1$d contestades de %2$d» (plural cuidado) opcional. Separación fila 1/2: 8 dp.
- A ≥1,5× (`isExtraLargeText`): fila 1 = «3 de 5» (a11y mantiene el texto completo, `contentDescription` ya existente) + cronómetro **en la misma fila**; la fila 2 sigue en el contenido (ya lo hace). Elimina la línea extra del cronómetro.
- Opciones: gap **12 dp** (hoy 8); resto igual. Cert/Fals: `optionMinTf` 80→**96 dp** (aprovecha el vacío, objetivo mayor).
- Barra inferior: **Següent con peso 1,4 y Anterior 1**; en Q1, **no mostrar Anterior deshabilitado**: Següent a ancho completo. En última pregunta usar `btn_review_finish` («Revisar i finalitzar») en lugar de «Revisar» (ya existe, `strings.xml:72`).
- A ≥1,5×: fila única `[IconButton Anterior 56×56 (flecha, contentDescription «Pregunta anterior») | Següent weight 1]` en lugar de apilar. Recupera ~70 dp.
- Verif.: `10-pregunta-unica-buida-100-clar` enunciado empieza ≥8 % más arriba que hoy (y=~418 → ≤380 en la escala de 900 px), Següent claramente más ancho que Anterior; `10-…-200-clar`: cronómetro en la fila del título, barra inferior de **una** fila, las 2 primeras opciones íntegras sin solapes; `10-…-100-clar` Q1 sin botón Anterior pálido.

**P1.4 Resumen de preguntas** (`ScreensOverview.kt`)
- Título «Preguntes» (1 palabra) y píldora de tiempo con `Spacer` ≥12 dp (el título pasa a `weight(1f)` con `maxLines=1, ellipsis`; a 200 % el tiempo baja a una fila propia bajo el título).
- Una sola línea resumen en lugar de dos: «2 contestades · en falten 3» (`titleMedium`, 600).
- Fila: **línea 1** estado como chip (`labelLarge`, ≥16 sp): «Contestada» (icono ✓ en círculo `primary`/`onPrimary` 28 dp — **sustituye el azul**, el verde ya significa "hecho" en el resto) / «Sense resposta» (círculo contorno `outline` 2 dp); «Pregunta 3 · Ets aquí» en un único chip para la actual; **línea 2–3** enunciado (`bodyLarge`, máx. 2 líneas). Fila actual: borde `primary` 2 dp (no negro 3 dp) + etiqueta; filas sin respuesta con texto `onSurface` (peso normal), contestadas `onSurfaceVariant`. Gap 12 dp.
- Barra inferior: «Anar a la primera sense resposta» solo si hay pendientes; si no, solo «Avaluar» (menos ruido).
- Verif.: `20-resum-parcial-100-clar/fosc` título y píldora con ≥12 dp de separación, filas ≤3 líneas, ningún azul; `22-resum-complet` sin botón «Anar a la primera».

### P2 — Alto impacto / esfuerzo medio

**P2.1 Inicio con composición por estado** (`ScreensMain.kt:77-236`)
- C0 (sin examen): hero (se mantiene; recortar la línea de campo a la mitad derecha o `PitchLine` alpha 0.16→0.08 y que no pase por el subtítulo; contenido de la cabecera alineado a la misma sangría 20 dp que el resto) → título de sección «Què t'espera» (`titleMedium` 600) → **tarjeta única** (`surfaceContainerLow`, `outlineVariant` 1 dp, `large`) con 3 filas icono 24 dp + texto completo (`bodyLarge`): «5 preguntes», «5 minuts», «Per aprovar cal encertar-ne 3» (reutilizar `home_fact_*_full`; unifica con `RulesScreen`). Quitar las 3 teselas. «Encara no has fet cap examen…» → `bodyMedium` `onSurfaceVariant` dentro de la tarjeta, al pie. CTA anclado.
- Con historial (60/61): orden: **estado del último examen primero** (chip aprobado/no aprobado + «3 de 5 · 22/11/2026»), luego credencial si existe (`CredentialMini`), luego «Repassar l'últim examen» como `TextButton` con icono 24 dp (hoy enlace sin icono). CTA anclado «Tornar a fer l'examen».
- 200 %: hero compacta (título 1 línea; subtítulo ≤2); tarjeta de filas en columna **sin cortar** («3» sin etiqueta en `E/02-…-200-clar` desaparece porque las filas llevan el texto completo).
- Verif.: `02-inici-inicial-100-*` sin teselas ni texto «preg.», sin línea cruzando el subtítulo, espacio vacío ≤30 %; `02-…-200-clar` las 3 filas con su texto completo visibles al scroll 0 o con scroll nítido (`02b`); `60`/`61` primer bloque = último resultado.

**P2.2 Normas (3 niveles en lugar de 8 iguales)** (`ScreensMain.kt:282-330`)
- Nivel 1 (tarjeta compacta igual que Home): «5 preguntes · 5 minuts · encerts per aprovar 3».
- Nivel 2 (4 reglas, icono 24 dp sin círculo, `bodyLarge`): avançar/enrere, múltiple, sense resposta = fallada, desat automàtic.
- Nivel 3 (**tarjeta de aviso**, `tertiaryContainer`/`onTertiaryContainer` existentes; texto ≥7:1 ya cumplido): «Si surts de l'app, el temps continua passant.» con icono 24 dp. Es el único elemento de color cálido de la pantalla.
- Tarjeta «Nom a la credencial»: nombre + «Canviar» en `Row(SpaceBetween)`, «Canviar» `TextButton` con `contentPadding` 0 a la izquierda; ambos alineados a x=16 dp de la tarjeta; objetivo 48 dp.
- Verif.: `06-normes-100-clar` ≤3 bloques visuales distintos, aviso destacado, «Canviar» alineado; `06-…-200-clar` (a añadir) sin texto cortado.

**P2.3 Credencial: QR primero, detalles después** (`ScreensCredential.kt`)
- Orden: cabecera → nombre → «APTE» (chip `primaryContainer`, `titleMedium`) → **QR** → detalles (Examen, Data del dispositiu, Codi) → aviso «Emesa en autoservei…».
- Cabecera: a ≥1,5× apilada en columna (logo 40 dp arriba, «Credencial d'aptitud» 1 título, subtítulo `bodyMedium` bajo, 2 líneas máx.); a 100 % como hoy.
- «Mostrar a un organitzador»: icono 24 dp izquierda **y texto alineado a la izquierda del icono** en `Row` con `weight(1f)`; permitir 2 líneas a 200 %; padding vertical 12 dp (altura mínima 56). Alternativa de copy en preguntas.
- Campos: «Examen» no debe romper «reglament sample-1»; `softWrap` con `Modifier.fillMaxWidth()` y `maxLines=2` (valor técnico en `AtkinsonMono`).
- `E/51`: el único botón con borde negro → contorno `PaperInk` 1,5 dp + texto `PaperInk` y 56 dp (coherente con el papel).
- Verif.: `50-credencial-100-clar` QR visible sin scroll o al menos ≥70 % visible; `50-…-200-clar` cabecera ≤3 líneas y **QR visible** tras un scroll corto (`50b`), «Codi» no tapado; `51-credencial-presentacio-*` botón alineado.

### P3 — Medio impacto / bajo esfuerzo

**P3.1 Banners con gramática** (`Components.kt`, un `InfoBanner(kind)`): neutro (`surfaceContainerHigh`, icono «i»), éxito (`primaryContainer`, ✓), aviso (`tertiaryContainer`, ⚠), error (`errorContainer`, `error` icono). Radio `medium` 14 dp, padding 16 dp, texto `bodyLarge`. Aplicar: nombre (neutro), credencial lista (éxito), temps/ bateria baixa (aviso), guardado/QR error (error). Verif.: en `03-nom-buit`, `04-nom-error`, `30`, banner `33` (cuando se capture): cada tipo con color+icono distinto.
**P3.2 Estados no demostrados → criterios obligatorios antes de cerrar** (no existen en E/): `33` aviso 1 min, `32` «S'ha acabat el temps», `34`, `35`; `BuildError`/`RecoveryError`/`StateError`/error de guardado/batería baja. Especificación UX: todo error = mensaje en lenguaje llano (qué pasó + qué hacer) + **un** botón primario (`Tornar-ho a provar`) + un secundario si aplica; `liveRegion=Assertive` en error de guardado; tiempo agotado: pantalla de transición con icono, «S'ha acabat el temps», «Ja hem avaluat les teves respostes.» y un solo CTA «Veure el resultat» (ya en `TimeUpScreen`, `ScreensResult.kt:78`). Aviso de 1 min: banner `tertiaryContainer` (`WarnAmber`), icono ⚠, texto «Et queda 1 minut», botón «Tancar l'avís» ≥48 dp. Verif.: capturas 33, 32, 34, 35, y mínimo una por pantalla de error forzada con test/ADB.
**P3.3 Bienvenida (01)**: filas con icono-círculo 48 dp `primaryContainer`, texto `bodyLarge`→`titleMedium` 20 sp, gap 20 dp, hero (mismo componente que Home) reduciendo el hueco; el botón «Entesos» → **«Començar»**? (ver preguntas) — «Entesos» no dice la acción. Verif.: `01-benvinguda-100-*` hueco ≤25 %.
**P3.4 Radio/chip «Resposta múltiple»** (`MultiChip`, `QuestionScreen.kt:347`): el chip lleva una casilla ✔ marcada aunque no haya selección (README 12): cambiar icono a «casillas» **sin check** (`library_add_check`→ outline) o quitarlo y dejar texto `titleSmall` «Marca totes les que siguin correctes.» sobre fondo `secondaryContainer`. Verif.: `12-pregunta-multiple-buida-100-*` sin ✔ marcado visible.

### P4 — Opcional/posterior
Háptico leve (10 ms) en selección/guardado (guía *Haptic Feedback*); transición de 150-200 ms entre preguntas (respetando `Animator duration scale`=0); indicador «Desat» sutil (ya descartado por el usuario; no reponer). TalkBack: hacer [PV] explícito (`QuestionScreen.kt:~200-210`): ver pregunta 5.

## 2b. Reglas transversales (aplican a todo)
- Cuerpo: `bodyLarge` 18 sp por defecto en texto que el usuario debe leer; `bodyMedium` 16 solo secundario. Etiquetas de estado/contador ≥16 sp con `onSurfaceVariant` (≥4.5:1; texto principal `onSurface` ≥7:1 → AAA ya cumplido).
- Espaciado: 8/12/16/24 dp; separación entre secciones 24 dp, dentro de sección 12 dp; gutter 20 dp se mantiene.
- Un único nivel de énfasis por pantalla: **1 relleno primario**, 1 contorno, texto lo demás. Nunca dos rellenos.
- Estado pulsado/focus: ripple M3 + focus ring 2 dp `primary` (visible en oscuro; verif. por teclado/Switch Access en emulador).
- A ≥1,5× de fuente: ningún `Row` con 2 `weight` de botones; layouts alternativos explícitos (arriba) en vez de truncar; ningún elemento accionable tapado por la barra fija.
- Textos largos del banco: nunca truncar enunciados/opciones; en resumen máx. 2 líneas con `…` y semántica con el texto completo.

## 3. Consultas `ui-ux-pro-max` registradas
Cargados `SKILL.md` y `references/pro-rules.md` (secciones iconos, interacción, contraste claro/oscuro, layout/spacing). Consultas ejecutadas con `scripts/search.py`:
1. `--domain ux "exam quiz timer progress feedback"` → *Progress Indicators* (barra "paso N de M": se mantiene) y *Haptic Feedback* (opcional P4).
2. `--domain ux "empty state call to action"` → *Empty States* («mensaje útil + acción», no pantalla en blanco): P2.1 (inicio C0/estado), P1.4.
3. `--domain ux "senior accessibility large text touch"` → *Text Reflow and Spacing* (Crítico: no recortar texto al 200 %), *Touch Spacing* (≥8 dp entre objetivos): P1.3 (gap 12 dp, fila de iconos 56 dp), 2b.
4. `--domain ux "button hierarchy primary secondary"` → *Back Button* (no romper Atrás: se mantiene el diálogo de salida) + *Heading Hierarchy* (el título semántico `heading()` ya existe, mantener).
5. `--domain ux "error recovery message"` → *Error Recovery* (siguiente paso claro), *Error Messages* (anunciar: `liveRegion`), *Error Placement* (error junto al campo, ya en 04): P3.2.
6. `--stack jetpack-compose "list card navigation accessibility"` → *Event-based navigation*, *Content description*, *Semantics*: P1.3/P1.4 (nuevos controles icono con `contentDescription`; filas con `semantics`).
Pro-rules aplicadas: *Disabled state clarity* (Q1 sin «Anterior» pálido), *Touch target minimum* (todo ≥48 dp; icono Anterior 56), *Stable Interaction States*, *Scroll and fixed element coexistence* (barra inferior), *Consistent Icon Sizing* (24 dp inline / 40–48 badge), *Contextual Semantics* (iconos decorativos junto a texto ocultos de TalkBack).
Skills `ui-taste` y `mobile-android-design`: criterios de composición (una acción principal, no tres jerarquías de botones, menos contenedores anidados; navegación M3: back en top bar, un FAB/CTA anclado) coherentes con lo anterior; no hay imágenes generadas.

## 4. Preguntas para el usuario (producto/copy, no las decido yo)
1. **«Entesos»** en la bienvenida: ¿cambiarlo por «Començar»/«Continuar»? (No toca flujo; es copy.)
2. **«Mostrar a un organitzador»** (a 200 % ocupa 2 líneas): ¿aceptas «Mostrar el QR» o «Ensenyar la credencial»?
3. **Repaso**: ¿elimino también los mensajes `review_both`/`review_wrong_single` por completo o prefieres mantener una frase única de resumen (p. ej. «Vas marcar 1 d'incorrecta(es)…») solo en preguntas de múltiple? Recomendado: quitar en única/cert-fals, conservar en múltiple.
4. **Ámbar en repaso** (P1.2): ¿apruebas dejar de usar ámbar para "correcta no marcada"? Es desviación de UX F-previa, no de ADR ni marca.
5. **Resultado**: ¿«Tornar a l'inici» como texto de ancho completo (recomendado) o icono × en la top bar? Cambia dónde está la salida de la pantalla final.
6. **Última pregunta**: ¿«Revisar i finalitzar» (largo; 2 líneas a 200 %) o mantener «Revisar»? Recomiendo la larga.
7. ¿Se puede capturar ya el flujo `--flow temps` en la línea base (E/33, 32, 34, 35) y forzar pantallas de error? Sin ellas no puedo certificar P3.2 ni dar PASS visual en esos puntos.
8. **TalkBack**: ninguna de las propuestas se da por verificada hasta probar en dispositivo [PV]: ¿se dispone de dispositivo físico para la validación final? El orden de lectura de cabecera/filas nuevas (P1.3/P1.4) debe probarse.

## 5. Riesgos / límites
- No he abierto las 84 capturas (según instrucciones); lo no visto lo cito del README, marcado.
- Contrastes de tokens estimados por cálculo mental, no por herramienta; deben verificarse con un checker en implementación.
- Medidas «≥8 % más arriba», «≤30 % vacío» son criterios de aceptación aproximados sobre captura 1080×2400; el implementador/QA debe medirlas con la captura, no con tests de semántica (regla del reviewer).
