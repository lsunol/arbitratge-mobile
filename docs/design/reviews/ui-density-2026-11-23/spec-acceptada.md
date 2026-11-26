# Especificación aceptada: densidad y decisiones UI-01..UI-04 (2026-11-23)

Reconcilia `ux-designer-spec.md` y `ui-designer-spec.md`. Manda `docs/design/decisiones-usuario-ui.md`; después esta página;
después las specs. Alcance: SOLO pantalla de pregunta + los tokens que necesita (sin tocar otras pantallas salvo lo
inevitable, que se declara).

| Tema | Decisión del orquestador (reversible, se informa al usuario) |
|---|---|
| UI-01 | Dos botones `weight(1f)` idénticos, misma altura, a TODAS las escalas de fuente; se elimina la rama icono-de-flecha de ≥1,5×. Següent relleno, Anterior tonal (jerarquía solo por estilo). En P1: **Anterior visible y deshabilitado** (opción A de UX) para conservar el 50/50. |
| Última pregunta | Etiqueta **«Revisar»** (una línea) en vez de «Revisar i finalitzar». |
| UI-02 | Insignia en píldora ámbar `#F5C443` / texto `#2B1F00`, igual en claro y oscuro, disco oscuro con «!» (`ic_priority_high`), sin casilla ni borde de opción; borrar `ic_check_box_outline.xml` si queda sin uso. Texto de ayuda largo solo para TalkBack (`contentDescription`). |
| Cabecera | Una sola fila (~56 dp): «Pregunta N de M» + reloj + icono de resumen. **Se quita** la barra de progreso y la fila «Respostes N de M / Veure resum» (el resumen sigue accesible por el icono, con descripción a11y). |
| Pistas | Se quitan `single_hint` y `tf_hint` visibles (se conservan para a11y). |
| Medidas | Gutter 16 dp; tarjetas de respuesta mín. 48 dp, gap 8 dp; enunciado 20 sp y opciones 18 sp, con reducción automática a 18/16 sp si la pregunta no cabe (mín. 16 sp, 04 §8.1); NavBar ~64 dp; botones 52 dp. V/F: 64 dp mín. |
| Selección | Estilo suave del `ui-designer` (borde tinta 2 dp + fondo suave + indicador lleno), sin cambiar pesos ni padding al marcar (la tarjeta no salta). Se elimina el bloque negro. |
| 130 %/200 % | Scroll solo en el cuerpo; cabecera y NavBar fijas y 50/50 siempre. |
| Alcance | UI-01 también en los botones Anterior/Següent del detalle de repaso si usan el mismo componente; si no, se declara. |

Criterio de cierre (de decisiones-usuario-ui.md): tests mecánicos UI-01/UI-02 + capturas de las 10 preguntas del banco real
SIN scroll a 100 % (guardadas fuera del repo) + capturas con banco de muestra en el repo.
