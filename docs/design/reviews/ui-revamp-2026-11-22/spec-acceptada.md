# Especificación aceptada: revisión de look&feel de la app del candidato (2026-11-22)

**Orquestador:** reconcilia las dos revisiones previas, hechas en paralelo según el paso (1) de AGENTS.md:

- [`ui-designer-spec.md`](ui-designer-spec.md): dirección «Paper i gespa». Intervenciones I1-I10.
- [`ux-designer-spec.md`](ux-designer-spec.md): UX y percepción de calidad. Propuestas P1-P4.

Motivo: al usuario no le gusta el *look&feel* de la app. Línea base en
`docs/design/reviews/evidence/2026-11-22-ui-abans/`.

**Fuera de alcance, sin cambios:** ADR, ARB1, contrato del banco, flujo de producto (pantallas y orden), semántica de TalkBack.

**Base de la especificación:** la **especificación visual** es la de `ui-designer` (§3 tokens, §4 I1-I10). Los ajustes de UX de `ux-designer` se aplican donde se indica abajo. Si entran en conflicto, manda esta página.

## 1. Decisiones del orquestador

Se toman los valores por defecto recomendados. Todas son reversibles y se informan al usuario.

| Pregunta | Decisión |
|---|---|
| UI P1 [DESV-1]: gespa `#0F3D26` como `primary` (botón primario) | **Sí**, desviación de marca marcada; si el usuario o CFBB no la aceptan, `primary=#1D6F42` |
| UI P2: filete amarillo bajo la banda del inicio | **No** (exclusivo de la credencial) |
| UI P3: selección | **Invertida en tinta** |
| UI P4: anillo del resultado | **Se sustituye** por el marcador de botons |
| UI P5: fondo claro | **Papel `#FFFFFF`**, cal en los bloques |
| UI P6a / UX: título «Resultat» | Se sustituye por el veredicto como encabezado |
| UI P6b: acciones no fijas al 200 % fuera del examen | **Sí** (welcome, inicio, normas, resultado, credencial). Fijas en pregunta, resumen, detalle de repaso y nombre |
| UX Q1: «Entesos» | **«Continuar»** |
| UX Q2: «Mostrar a un organitzador» | **Se mantiene**; 2 líneas a 200 %, con icono y texto alineados |
| UX Q3: banners `review_wrong_single`/`review_both` | Se quitan en respuesta única y Cert/Fals. En múltiple se conserva una frase neutra (`surfaceContainer`, icono `info`) |
| UX Q4: ámbar en el repaso | **Se elimina** (canal veredicto verde/rojo, I7) |
| UX Q5: «Tornar a l'inici» en el resultado | Botón de texto a todo el ancho, mín. 56 dp |
| UX Q6: última pregunta | «Revisar i finalitzar» (`btn_review_finish`) |
| UX Q7: estados de tiempo | Se capturan en la evidencia DESPUÉS (`--flow temps`) |
| UX Q8: TalkBack | **[PV]** en dispositivo físico; no se declara verificado |

## 2. Reconciliación por pantalla

| Pantalla | Se aplica |
|---|---|
| Tema global | I1 (tokens claro/oscuro «nit de camp»), I5 (título en el contenido), I9 (botones), I10 (iconos de contorno) y UX P3.1 (`InfoBanner(kind)`: neutro/éxito/aviso/error con los tokens de §3) |
| Bienvenida 01 | Banda I3 sin altura mínima; filas de I4 (icono de contorno sin círculo), cuerpo `bodyLarge`. Botón «Continuar». Hueco ≤ 25 % |
| Inicio 02/60/61 | Banda I3 + `FactStrip` I4 con etiquetas completas (no la tarjeta de filas de UX). Con historial, el orden es de UX P2.1: 1) estado del último examen, 2) `CredentialMini` si existe, 3) `ActionRow` «Repassar l'últim examen» |
| Nombre 03-05 | I5 + `InfoBanner` neutro/error |
| Normas 06 | Tres niveles de UX P2.2: `FactStrip`, 4-7 reglas con icono de contorno sin círculo, `Callout` ámbar «Si surts…». Tarjeta del nombre de I4 con «Canviar» alineado |
| Pregunta 10-17 | I2 (selección en tinta, `MultiChip` vacío) + UX P1.3: cabecera de 2 filas, «Pregunta N de M» en tinta 700, separación 12 dp, Cert/Fals 96 dp. Següent con peso 1,4 y Anterior 1; en Q1 no hay Anterior. Con `fontScale ≥ 1,5`, fila única `[IconButton Anterior 56 | Següent]` y cronómetro en la fila del título. Botones I9 |
| Resumen 20-23 | UX P1.4 (título «Preguntes» separado del reloj ≥ 12 dp, una línea de resumen, filas ≤ 3 líneas, «Anar a la primera sense resposta» solo si hay pendientes) con los indicadores de I2 (disco de tinta = contestada, aro = sin respuesta, fila actual con borde `primary` 3 dp y píldora `primaryContainer`) |
| Diálogos 16/21/23 | Tokens I1 (radio 28, *scrim* 0,6), un único botón relleno |
| Resultado 30/31 | Bloque de veredicto + marcador de botons de I6. Datos de UX P1.1: título suspendido «No has aprovat» en 1 línea; se elimina «Et van faltar…»; `FactStrip` con Fallades (solo falladas con respuesta), «Sense resposta» solo si > 0, y Temps. Aprobado: `CredentialMini` como siguiente paso sobre «Veure la credencial». Suspendido: consejo como texto de apoyo, sin banner. Botones: primario, secundario y texto a ancho completo, todos de 56 dp |
| Repaso 40-45 | Listas I4 (icono de 28 dp sin círculo, radio 12). Detalle I7 (dos canales, sin ámbar, «Per què?» con filete `primary`) y regla de banners de §1 |
| Credencial 50/51 | Documento I8 (banda con filete amarillo, una sola columna). Orden de UX P2.3: nombre → «APTE» → QR → detalles → aviso. Presentación de I8 («Tancar» con borde gespa) |
| Tiempo 32-35 | `InfoBanner` de aviso para 1 min; pantalla de tiempo agotado con un único CTA (UX P3.2) |

## 3. Criterios de aceptación (solo valen con captura)

Son los de cada intervención (I1-I10 de `ui-designer`, P1-P3 de `ux-designer`) que no contradicen §2, comprobados en `docs/design/reviews/evidence/2026-11-22-ui-despres/` con **los mismos nombres** que ANTES.

**Capturas obligatorias en DESPUÉS:**

- `--flow main` claro y oscuro (100 %).
- `--flow escala` 130 y 200 % en claro. A 200 % también 11 y 60 (añadirlas al flujo).
- 200 % oscuro de 02, 11, 30 y 50.
- `--flow temps` en claro (32-35).

Sin captura, un punto no puede darse por PASS: queda «no verificado visualmente».

**Regresiones prohibidas:**

- Ningún texto truncado ni tapado por la barra a 200 %.
- Objetivos táctiles ≥ 48 dp.
- Tests existentes en verde.
- Sin cambios de semántica ni de contrato.
- Sin azul `#2B5C8A` ni ámbar en el repaso.
- Sin banco real.
