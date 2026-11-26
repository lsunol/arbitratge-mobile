# Especificación aceptada: nuevo estilo (mockups Claude Design) — 2026-11-24

Orden de prioridad: `docs/design/decisiones-usuario-ui.md` (UI-01..UI-05) > esta página > `ui-designer-spec.md` (tokens y
Compose) > `ux-designer-spec.md`. Mockups: `new-designs/*/UI-*.dc.html` (valores en `style=""`).

Se adoptan los valores por defecto propuestos por ambos consultores. En concreto:

| Tema | Decisión |
|---|---|
| Estilo global | Fondo `#F3F6F2`; tarjetas blancas sin borde con sombra suave (`dropShadow`/`shadow`; en oscuro sin sombra, elevación tonal + filete 1 dp); radios opciones 18, tarjetas 24, hero/resultado 28; botones píldora (58 dp primarios; Anterior/Següent 56); iconos en recuadro `#E4F0E8` radio 14. Tokens claro y oscuro de `ui-designer-spec.md` §1. Atkinson Next se mantiene. |
| UI-01..UI-04 | Siguen mandando: Anterior/Següent con `weight(1f)` idéntico a todas las escalas (se ignora el `width:167px` del mockup); insignia múltiple ámbar con «!» (no la pálida con casillas del mockup); opciones 56 dp (52 compacto), NO 64; las 10 preguntas reales deben seguir sin scroll a 100 % (360×800 dp). |
| Pregunta | Cabecera actual (texto N de M + reloj + icono resumen) + **progreso segmentado** de 6 dp bajo ella (solo si M ≤ 12 y fontScale < 1,5; si no, nada o barra continua fina); sin botón X «Sortir», sin repetir «Pregunta N de M» sobre el enunciado; selección en verde `#1D6F42` con fondo `#EAF4EE` y casilla/radio relleno (contraste del borde sin marcar ≥ 3:1: `#738A7C`). |
| Inici | Hero verde `#0F3D26` radio 28 con círculos decorativos y 3 cifras (preguntes/minuts/per aprovar); tarjeta blanca con 3 filas de icono + título + subtítulo (textos del mockup en catalán, ya coinciden con la app o se adaptan); CTA «Començar l'examen» píldora 58 dp. Se omite «Ajuda» y el enlace «Llegir les normes» si no existe ya en el flujo; el eyebrow del hero NO va en amarillo (usar `#D7E6DC`). Debe caber a 360×800 dp a 100 %; a 130/200 % se permite scroll. |
| Resultat | Tarjeta blanca radio 28 con anillo de aciertos (sustituye al marcador de botones; sin punto amarillo), título, y 3 recuadros (encerts/errades/temps) en `#F3F6F2`; «Has aprovat!» con exclamación permitido (04-ux: felicitación); suspenso sin rojo ni exclamación. |
| Credencial | Cabecera verde con logo + filete amarillo 4 dp (solo credencial), nombre, código, fecha. Se quita el QR de 64 px decorativo de la mini-credencial (no escaneable). Botón «Mostrar a un organitzador» primario + «Repassar les respostes» texto, sin cambiar el flujo. |
| Coherencia | Pantallas que comparten componentes (benvinguda, normes, nom, resum, repàs) reciben solo la coherencia mínima de `ui-designer-spec.md` §4 (fondo/tarjetas visibles sobre el nuevo fondo). No se añaden pantallas ni botones nuevos (flujo de producto intacto). |
| Contraste/tipografía | Textos secundarios ≥ 16 sp si el mockup usaba 13-15 px; `#4A5A50` para texto secundario. |

Criterio de cierre: tests de UI-01/UI-02 intactos y en verde; capturas ANTES (git stash o las de 2026-11-22/23) y DESPUÉS de Inici,
Pregunta (única, múltiple, V/F) y Resultat/Credencial en claro y oscuro 100 %, y 130/200 % de Pregunta/Inici/Resultat, con
banco de muestra en `docs/design/reviews/evidence/2026-11-24-nou-estil/`; las 10 preguntas reales sin scroll a 100 %
(capturas privadas).
