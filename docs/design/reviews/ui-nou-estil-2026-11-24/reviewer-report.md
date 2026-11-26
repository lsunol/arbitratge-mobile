No he escrito `docs/design/reviews/ui-nou-estil-2026-11-24/reviewer-report.md`: como oracle no edito ficheros. El informe completo va abajo para que lo guardes tú, tal cual. Veredicto: PASS en UI-01, UI-02 y UI-04; PASS condicionado en UI-03 y UI-05. No re-ejecuté los 175 tests.

---

# Informe de revisión independiente: nuevo estilo + densidad (UI-01..UI-05), 2026-11-24

**Revisor:** `oracle`, modelo `anthropic/claude-opus-5-5` (esfuerzo alto). Sustituye a `reviewer`: `github-copilot` no tiene créditos y el intento anterior falló con un 429.
**Alcance:** cambio sin commitear en `app-candidate` (UI) más los documentos asociados.
**Tests:** no los he vuelto a ejecutar. Son 175 en verde según el orquestador, después de que borrase `QuestionFitTest.kt` (ver D2).
**Confidencialidad:** el banco real solo se cita por IDs (q01..q10). Sus capturas están en `arbitratge-bank/work/qa/` y `qa/rv/`, fuera del repo.

## Veredicto por decisión

| ID | Veredicto | Prueba |
|---|---|---|
| UI-01 | **PASS** | `NavBar` (`QuestionScreen.kt` ~219-240): una sola fila, los dos botones con el mismo `Modifier.weight(1f).fillMaxHeight()` e `IntrinsicSize.Min`. Ya no hay rama de botón-flecha ni pesos distintos. Los tests `ui01*` (`ComposeScreensTest` ~685-711) comparan `boundsInRoot` con `assertEquals(width/height, ±1 px)` a escala 1.0, 1.3 y 2.0, en P1, en una pregunta intermedia y en la última. Con 1,4/1 fallarían. En capturas, 50/50 en claro y oscuro a 100 %, y con el banco real a 130 % y 200 %. En P1 «Anterior» sale deshabilitado con el mismo ancho. |
| UI-02 | **PASS** (con test débil, D3) | Insignia ámbar `#F5C443` con un disco oscuro y «!» (`ic_priority_high`). No tiene rol, no es clicable y no tiene estado seleccionable; el drawable `ic_check_box_outline` está borrado. Hay tests de que no tiene rol ni clic, de que el número de casillas es igual al de opciones, de que no ocupa todo el ancho y de que la ayuda va solo en el `contentDescription`. Visible en claro y oscuro con contraste correcto y no se confunde con una opción. |
| UI-03 | **PASS condicionado** | Cabecera de una fila más un progreso segmentado, tarjetas compactas sin bloque negro y opciones de 52-56 dp. Visualmente es claramente más compacta y actual. Quien decide si es «moderna» es el usuario: queda pendiente su visto bueno. |
| UI-04 | **PASS** | Banco real a 100 % (`q01..q10-100n`): ninguna pregunta llega a la barra; el peor caso (q06 y q08) deja holgura. A 130 % (`rv/q*-130`) tampoco hay scroll: un swipe deja la pantalla idéntica salvo 65 px del reloj en q06. A 200 % hay scroll en q05-q08, que está permitido; cabecera y barra se quedan fijas y en 50/50. Desde que se borró `QuestionFitTest` ya no hay test mecánico (D2). |
| UI-05 | **PASS condicionado** | Inici, Pregunta, Resultat y Credencial siguen los mockups: fondo `#F3F6F2`, tarjetas blancas con sombra suave, botones píldora, hero verde, anillo de resultado y cabecera de credencial con filete amarillo. Las desviaciones están justificadas por UI-01, UI-02 y UI-04 y por `spec-acceptada.md`: insignia con «!», opciones de 56 dp en vez de 64, sin X de salir, sin «Ajuda» y sin amarillo en el hero. **No verificado visualmente:** Resultat y Credencial a 130 % y 200 % (falta la captura, D1). |

## Comprobaciones visuales

- **Claro 100 % (banco de muestra):** inici, pregunta única/múltiple/V-F, resumen, normas, resultat aprovat/suspès y credencial. No he visto texto cortado ni solapes.
- **Oscuro 100 %:** inici, pregunta única y múltiple marcadas, resultat y credencial. El botón primario pasa a menta claro con texto oscuro y el contraste es correcto. La insignia se ve bien.
- **Banco real:** a 100 %, 130 % y 200 % las preguntas se ven bien, también a 200 % con scroll.
- **Captura que falta:** la carpeta `evidence/2026-11-24-nou-estil/clar-130/` está **vacía**. En `clar-200/` solo hay benvinguda, inici y normes.

## Regresiones y ámbito

- **Ámbito:** solo hay cambios en `app-candidate/src/main/.../ui/*`, `strings.xml`, drawables, los tests de UI y `scripts/qa/capture-ui.mjs`. No se tocan `core-exam`, `core-credential`, ARB1, el ViewModel ni el flujo. En la credencial solo hay refactor visual: helpers extraídos y la forma del documento.
- **Accesibilidad:** las pistas («Tria una resposta», ayuda de múltiple) pasan a `contentDescription`, y el resumen tiene descripción con el recuento. TalkBack en dispositivo queda **[PV]**.
- **Catalán:** los textos nuevos están bien. Cambios visibles para el usuario: «Has aprovat!» y «Revisar», ya decididos en la spec.
- **Secretos y banco real:** busqué en el repo, incluidos los ficheros sin seguimiento, los textos del banco real. Solo hay 2 coincidencias, y están en el reglamento público (`docs/reglament/*`), que es lo esperado porque la opción cita el artículo. No hay `review-bank`, `review-seal`, claves privadas ni `storePassword` fuera de la documentación.

## Defectos priorizados

1. **D1 (media): falta evidencia a 130 % y 200 % en el repo.** No hay capturas de pregunta, resultat y credencial a 130 % (la carpeta está vacía) ni de pregunta, resultat y credencial a 200 %. Es obligatorio según `spec-acceptada.md`. Las capturas privadas del banco real cubren la pregunta, pero Resultat y Credencial siguen **no verificadas** a esas escalas.
2. **D2 (media): UI-04 ya no tiene test mecánico.** Se borró `QuestionFitTest.kt` porque su test de contraste (9 opciones deben desbordar) fallaba al cambiar el nivel compacto. UI-04 depende ahora solo de las capturas. Propuesta: recuperar el test de 5 opciones (ese pasaba) y rehacer el de contraste con más opciones, o quitar el test de contraste.
3. **D3 (baja): el test de UI-02 no comprueba que el icono sea «!».** Comprueba que existe el nodo `MULTI_BADGE_ICON_TAG`, pero seguiría pasando con cualquier otro icono. Basta con fijar el recurso con una semántica o etiqueta propia, o comprobar el drawable.
4. **D4 (baja): a 200 %, al bajar hasta las últimas opciones la insignia de múltiple sale de pantalla,** y no hay ninguna indicación de que se puede hacer scroll. Está permitido, pero conviene un degradado o pista de scroll.
5. **D5 (baja): puntuación en el `contentDescription` del enunciado.** Se concatena «…?. Tria una resposta.» con «?.». Mejor unir con un espacio o un salto.
6. **D6 (baja): Inici a 100 %.** La etiqueta «encerts per aprovar» ocupa 3 líneas en la tercera cifra del hero; el mockup pone «per aprovar». En Resultat aprovat, la mini-credencial queda medio tapada por las acciones fijas: se lee «Codi…» a medias. Es aceptable porque hay scroll, pero se ve menos cuidado.
7. **D7 (baja, repo):** `new-designs/` no tiene seguimiento en la raíz e incluye un `.zip`, React del vendor y la fuente de Google Fonts. Hay que decidir si se mueve a `docs/design/` (solo los `.dc.html` y el README) o si se ignora, para no commitear ~200 KB de vendor sin querer.

## Preguntas abiertas para el usuario

- Si da por buena la apariencia de UI-03 y UI-05 (capturas en `evidence/2026-11-24-nou-estil/` y las privadas).
- Etiqueta «Revisar» frente a «Revisar i finalitzar», y «Has aprovat!» con exclamación.
- Si quiere los botones «Ajuda» y X «Sortir» de los mockups (ahora omitidos para no cambiar el flujo).
- Si UI-01 también aplica a los botones Anterior/Següent del detalle de repaso.

**Veredicto global: PASS condicionado.** UI-01, UI-02 y UI-04 cumplen con pruebas. Para un PASS limpio hace falta cerrar D1 y D2 y que el usuario dé el visto bueno a UI-03 y UI-05.

---

Notas para el orquestador:
- Hice capturas propias del banco real a 130 % y 200 % en `C:\Users\lluis\projects\arbitratge-bank\work\qa\rv\`: `q01..q10-130|200.png`, y las `-sw` después del swipe.
- `rv/run.mjs` y `rv/run2.mjs` son mis scripts de captura y también están ahí. Las dos carpetas son privadas.
- El emulador queda con `font_scale 1.0`, tema claro y la app de revisión con un examen a medias.

No hace falta pasarle nada a `worker`, salvo que decidas cerrar D1, D2 y D3 antes del commit. En ese caso, encárgaselo a `android-developer`: capturas a 130 % y 200 % con `capture-ui.mjs --flow escala` y `--flow pregunta`, recuperar el test de UI-04 y fijar el icono «!» en el test de UI-02.