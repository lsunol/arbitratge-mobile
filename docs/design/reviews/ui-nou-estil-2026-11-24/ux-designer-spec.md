# UX — consultoría previa UI-05 (estilo Claude Design) · 2026-11-24

Autor: `ux-designer`. Prevalece `docs/design/decisiones-usuario-ui.md`: UI-01..UI-04 mandan sobre UI-05. Solo diseño; no se toca código.

## 0. Hechos verificados

- Mockups (`new-designs/*/UI-*.dc.html`, 390×844): fondo `#F3F6F2`, tarjetas `#FFF` sin borde, sombra `0 1px 2px / 0 8px 24px rgba(15,61,38,.06/.07)`, radio 24/28, botones píldora 56-58 px, Atkinson Hyperlegible Next, verde `#0F3D26`/`#1D6F42`, amarillo `#F2C230`, iconos en recuadro `#E4F0E8` 44 px (r14), chips `#E4F0E8`.
- La marca **ya coincide**: `Theme.kt:33-85` usa `#0F3D26`, `#1D6F42`, `#F2C230` y Atkinson Next. La refactorización es de **forma** (superficies, radios, sombras, píldoras, progreso), no de paleta/tipografía.
- Estado actual: cabecera de pregunta de una fila con «N de M» + reloj + icono de resumen (`QuestionScreen.kt:~255-300`); sin barra de progreso (spec-acceptada, UI-03/04). «Sortir» cierra la app y el tiempo sigue corriendo (`ExamViewModel.kt:66`); no hay botón X en pantalla, solo `BackHandler` (`ArbitratgeRoot.kt:17`). Resultado: bloque de veredicto sangrado + `FactStrip`; suspenso sin rojo (04 §2.8).
- 04-ux §tono (l.1318): «sin exclamaciones, **salvo felicitación al aprobar**» → «Has aprovat!» **no choca**; el copy actual (`passed_title` = «Has aprovat», 04:1388) puede ganar la «!».

## 1. Consultas `ui-ux-pro-max` (SKILL.md + `references/pro-rules.md` leídos)

| Consulta | Resultado aplicado |
|---|---|
| `progress indicator step segmented --domain ux` | Progreso «Step 2 of 4» válido, pero debe ser información, no solo color → exponer «Pregunta N de M» en texto/semántica. |
| `exit confirmation abandon flow --domain ux` | Acciones irreversibles/destructivas requieren confirmación → un botón X «Sortir» exige diálogo. |
| `touch target spacing small screen --domain ux` | 48 dp Android, ≥8 dp entre dianas → botones 52 dp válidos; gap 12 de mockup OK. |
| `dark mode contrast shadow elevation --domain ux` | Contraste 4.5:1; la sombra suave no se ve en oscuro → elevar por tono (`surfaceContainer`), no por sombra. |
| `scaffold card elevation --stack jetpack-compose` | Sin resultados (0). Se usa criterio general M3 (fallback declarado). |

## 2. Pantalla Inici (`HomeScreen` en `ScreensMain.kt`, `ScreensOverview` solo si comparte tarjetas)

| Elemento del mockup | Veredicto | Resolución |
|---|---|---|
| Fondo `#F3F6F2`, tarjetas blancas sin borde + sombra, r24 | **Adoptar** | Tokens `surfaceBackground`/`cardSurface`; sombra solo en claro; oscuro = `surfaceContainer` sin sombra. |
| Cabecera logo+«Arbitratge» + botón «Ajuda» circular 44 | **Adoptar logo/nombre**; ayuda **solo si existe destino** | El botón sin destino es una diana muerta (viola «disabled clarity»). Opción: abre «Llegir les normes» (ya existe `RulesScreen`). Pregunta abierta Q1. |
| Hero verde r28 con círculos y 3 métricas (preguntes/minuts/per aprovar) | **Adoptar con matiz** | Altura 236 px fija **no**: `wrap` con mínimo ~190 dp; las 3 métricas son `RulesFactStrip` ya existente (datos reales de `RulesSummary`, no «5/5/3»). A 200 % las métricas pasan a columna 1×3 o conservan 3×1 con etiquetas de 2 líneas; el círculo decorativo se recorta. Contraste texto `#D7E6DC` sobre `#0F3D26` ≥ 7:1. |
| Lista de 3 beneficios con icono en recuadro | **Adoptar parcialmente** (UI-03 compacta) | Mantener las 3 filas si caben en 360×800 sin scroll a 100 %; a ≥130 % es la zona que hace scroll (único scroll aceptable; botón principal queda fijo abajo). Textos de los beneficios son contenido nuevo → ver microcopy §6. «Funciona sense internet» debe ser cierto (lo es: sin backend). |
| Texto «Encara no has fet cap examen…» | Adoptar (estado vacío) | Variante con último intento/credencial = tarjeta de resultado previo (ya existe); no mostrar ambos. |
| CTA píldora 58 px «Començar l'examen →» + enlace «Llegir les normes» 44 px | **Adoptar** | Botón fijo abajo (`pinBottom`), 52-58 dp. Enlace «Llegir les normes» a ≥48 dp de alto (mockup da 44 → subir a 48). |
| Con credencial: «Veure credencial» primario + «Tornar a fer l'examen» | Mantener estructura actual | Mockup no lo cubre; restyle solo de superficies. |

## 3. Pantalla Pregunta (`QuestionScreen.kt`)

| Elemento | Veredicto | Resolución |
|---|---|---|
| **Progreso segmentado** (5 barras de 6 px) | **Conflicto con UI-03/04 (barra quitada por espacio)**; el segmentado es compatible solo si **sustituye** al texto de cabecera sin añadir fila | Propuesta: misma fila de ~56 dp: `[texto «N de M»] [segmentos flex] [reloj] [resumen]`. Con ≤12 preguntas, segmentos de 4 dp de alto/gap 4 dp; con M>12 pasa a **barra continua** (segmentos <8 dp son ruido). A ≥1,5× fuente o ancho escaso: ocultar segmentos, queda «N de M» (decorativos = `clearAndSetSemantics`). Respondida=`#1D6F42`, actual=anillo/mayor altura, pendiente=`#CFDCD3`; no solo color (actual distinguible por altura). Si el usuario lo juzga «espacio perdido», retirar segmentos (decide el usuario, Q2). |
| Texto «Pregunta 2 de 5» duplicado sobre el enunciado | **Rechazar** | Duplica la cabecera y consume ~28 dp (viola UI-04). Queda solo el de cabecera (heading a11y). |
| Botón X «Sortir de l'examen» (44 px, izquierda) | **Chocar**: semántica de «Sortir» = cerrar la app con el reloj corriendo; añadir X 48 dp roba ancho al reloj a 200 % y confunde con «abandonar» | Resolución: **no añadir X por defecto**; el sistema ya da Atrás/gesto. Si el usuario lo quiere: X abre diálogo «Vols sortir? El temps continua» [Sortir] [Continuar], pero solo ≤1,3× de fuente; a mayores escalas se oculta (queda Atrás). Q3. |
| Enunciado 25 px / opciones 17 px, gap 12, padding de cuerpo 24 | **Mandan UI-04**: se conserva la escala 20 sp/18 sp con reducción automática y gutter 16, gap 8 | Solo se adoptan forma y color; no tamaños que añadan altura. |
| Insignia «Pots marcar més d'una» (`#FFF4CC`, r12, **sin «!»**) | **Chocar con UI-02** | Gana UI-02: píldora ámbar `#F5C443` con disco «!» (como hoy). Se puede tomar el radio r12 del mockup solo si se mantiene el relleno ámbar y el icono. Texto: «Resposta múltiple». |
| Opciones tipo tarjeta con casilla/caja izquierda (`boxStyle`, pressed) | **Adoptar parcial** | Tarjetas blancas r16-20 sin sombra (en listas largas, el filet 1 dp `optionOutline` ≥3:1 sigue siendo la frontera: sin borde, el 1,2:1 blanco/`#F3F6F2` **no** cumple 3:1 de límite de control). Seleccionada: borde 2 dp + fondo suave + indicador relleno (spec-acceptada). Opción **mín. 52 dp**, no 64 dp: 64 dp × 5 opciones = 320 dp + gaps rompe UI-04. 64 dp solo en V/F (ya). |
| Pista visible de selección/«hint» | Rechazar (spec-acceptada); solo `contentDescription`. |
| **Anterior/Següent 50/50**, píldora 56 px (borde 1,5 px / relleno) | **Adoptar forma, manda UI-01** | `weight(1f)` ambos, misma altura (52-56 dp) a 100/130/200 %. El mockup trae `width:167px` residual en Següent → ignorar. En P1: Anterior **visible y deshabilitado** (spec-acceptada) para no romper el 50/50. Icono de flecha dentro del botón: opcional y desaparece a ≥1,5× antes que reducir texto; nunca cambiar el reparto. Última pregunta: «Revisar». |
| Reloj-chip `#E4F0E8` 40 px | Adoptar (ya es píldora 36/48) | Estados 5 min ámbar / 1 min rojo + icono se mantienen (no solo color). Falta el icono de resumen en el mockup: **se conserva** (04 §resumen, necesario para volver a preguntas). |

## 4. Resultat (`ScreensResult.kt`) y Credencial (`ScreensCredential.kt`)

| Elemento | Veredicto | Resolución |
|---|---|---|
| Tarjeta blanca r28 con anillo 5/5 + «Has aprovat!» + 3 stats | **Adoptar con matices** | Anillo de progreso (decorativo, `contentDescription` único «N de M encerts») sustituye al `ButtonsMarker`/bloque sangrado; el título sigue siendo `heading` + `liveRegion polite`. **Suspenso** (no hay mockup): misma tarjeta, anillo en tinta/`#4A5A50` sin rojo (04 §2.8), título «No has aprovat, de moment». Falta definirlo → ver Q4. |
| «Has aprovat!» con «!» | **Compatible** | 04:1318 permite exclamación en la felicitación; actualizar `passed_title` a «Has aprovat!». Prohibido añadir «!» al suspenso. |
| «Calien 3 encerts per aprovar» | **Cuidado** | El umbral de la app se expresa en errores/aciertos según `r.threshold`; usar el dato real (`result_min`), no literal. Mantener «errades» como cifra principal (AGENTS: «número de fallos»). Si hay «sense resposta», 4.ª celda o 2 líneas (ya existe `FactCell`). |
| Stats r16 (encerts/errades/temps) | Adoptar | A 200 % pasan de 3×1 a apiladas 1×3 (altura libre porque es scrollable). |
| Mini-credencial con cabecera verde y banda `#F2C230`, nombre, código FB15-AED4, fecha, QR 64 px | **Adoptar sin QR falso** | Un QR de 64 dp no se escanea y confunde al organizador: usar un icono de «escaneable» decorativo o quitarlo; el QR real vive solo en `PresentationScreen` (fondo blanco, brillo, quiet zone, 04 §3). El código corto se mantiene. Texto y nombre largo: `maxLines=2` + elipsis solo a partir de 3 líneas; a 200 % el icono pasa debajo. |
| CTA «Mostrar a un organitzador» (píldora 58) + «Repassar les respostes» (texto 44 px) | **Adoptar**; enlace a 48 dp | Fallos: «Repassar» pasa a botón primario si suspenso (como hoy). «Tornar a l'inici» no aparece en el mockup: **se conserva** (necesario). |
| Pantalla de presentación (QR) | Fuera del mockup | No tocar la lógica: blanco, QR máx., caducidad visible; solo adoptar radios/cabecera. |
| Aviso pendiente de credencial (`cred_pending`) | Fuera del mockup | Mantener `InfoBanner` ERROR con reintento; en la tarjeta nueva, aparecer **en lugar** de la mini-credencial, no debajo. |

## 5. 130 %, 200 % y oscuro

- **130 %**: layout idéntico salvo hero Inici (métricas pueden partir etiqueta). Pregunta sin scroll en el banco de muestra; segmentos de progreso visibles solo si caben ≥4 dp por segmento.
- **200 %**: scroll solo en el cuerpo; cabecera y NavBar fijas, 50/50 intactos, sin icono en botones; segmentos y X ocultos; «N de M» en forma corta (`progress_short`). Tarjetas con padding reducido (16 dp) y círculos decorativos recortados. Ya hoy no se rompe la insignia (2 líneas).
- **Oscuro**: no inventar paleta paralela. Fondo `#101A14`-familia ya definida en `Theme.kt`; tarjetas = `surfaceContainer` (más claras), sin sombra; recuadro de icono `#E4F0E8` → `primaryContainer` oscuro; ámbar de insignia igual en ambos temas (UI-02); hero verde ya oscuro: borde sutil `#1D6F42` para que no se funda con el fondo. Verificar con contraste ≥4.5:1 (texto) y ≥3:1 (bordes de opción).
- Fuentes: no cargar Google Fonts (mockup lo hace por red); la app ya empaqueta Atkinson Next (`Theme.kt:209`) → sin red, coherente con «funciona sense internet».

## 6. Microcopy (catalán) a confirmar

| Clave | Texto |
|---|---|
| `passed_title` | Has aprovat! |
| `failed_title` | No has aprovat, de moment (sin «!») |
| `home_benefit_offline` / sub | Funciona sense internet / Es corregeix al moment, al teu mòbil. |
| `home_benefit_review` / sub | Aprens de cada error / Veuràs la resposta correcta de cada pregunta. |
| `home_benefit_cred` / sub | Credencial si aproves / La pots mostrar a un organitzador. |
| `btn_help` (si Q1) | Ajuda · desc: «Llegir les normes» |
| `btn_exit_exam` (si Q3) | Sortir de l'examen |

## 7. Riesgos

1. **Altura**: hero 236 px + 3 filas + CTA ≈ 790 px: en 360×800 dp a 100 % el Inici **no** cabe con todo → acortar hero (190) o scroll en Inici (UI-04 se limita a la pantalla de pregunta; aun así UI-03). Medir con captura.
2. Segmentos de progreso vs presupuesto de cabecera a 200 % (mitigado: se ocultan).
3. Borde de opciones: quitar el filet para «ser fiel» **incumple 3:1**; no hacerlo.
4. Sombras en oscuro/baja gama: coste de render de 24 dp blur; usar `shadow` M3 simple o elevación tonal.
5. El mockup no contempla suspenso, error, tiempo agotándose, V/F, pendiente de credencial, ni estados de carga: hay que reestilizar sin inventar jerarquías nuevas.
6. QR de 64 dp en tarjeta puede inducir a escanear la mini-credencial (confusión social/04 §3).
7. Verificación: ninguna medida es válida sin capturas emulador (UI-05 exige ANTES/DESPUÉS junto al mockup; banco real nunca en repo).

## Preguntas abiertas para el usuario

1. ¿El botón «Ajuda» del Inici abre «Llegir les normes» o se elimina?
2. ¿Quieres el progreso segmentado aunque ocupe ancho de la cabecera (se oculta a ≥1,5×)? ¿O prefieres seguir sin barra?
3. ¿Quieres el botón X «Sortir» en la cabecera de pregunta? Sería un diálogo de confirmación; el comportamiento actual es salir con el tiempo corriendo.
4. ¿Apruebas «Has aprovat!» con exclamación y el suspenso sin rojo ni exclamación?
5. ¿Se acepta quitar el QR de 64 px de la mini-credencial (decorativo engañoso)?
