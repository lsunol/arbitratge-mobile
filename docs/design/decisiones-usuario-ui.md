# Decisiones del usuario sobre la UI (REGISTRO VINCULANTE)

Lo que el usuario ha pedido **prevalece sobre cualquier spec de agente** (`ux-designer`, `ui-designer`, orquestador)
y sobre `04-ux.md`. Un agente NO puede reinterpretar, suavizar o "mejorar" estas decisiones (p. ej. sustituir un
50/50 por 1,4/1 "porque la acción principal debe pesar más"). Si cree que una decisión es mala, lo **consulta** al
usuario; no la incumple. Cada fila lleva una **comprobación mecánica** que debe pasar antes de dar la tarea por cerrada.

Los modelos de `github-copilot` no se usan (créditos agotados, 2026-11-24): `reviewer` se sustituye por `oracle`/`worker` (Anthropic).

Origen del registro (2026-11-23): el usuario repitió varias veces estas peticiones y volvieron a incumplirse porque
solo vivían en el chat. Las decisiones sobre la UI se añaden aquí **en el mismo turno** en que se reciben.

| ID | Decisión (palabras del usuario, resumidas) | Comprobación obligatoria |
|---|---|---|
| UI-01 | **«Anterior» y «Següent» ocupan exactamente el 50 % cada uno** (mismo ancho, misma altura) cuando ambos se muestran. | Test que compara `boundsInRoot` de ambos (`width` iguales ±1 px) en 100 %, 130 % y 200 % + captura. Prohibido `weight` distinto. |
| UI-02 | **«Resposta múltiple» lleva icono de exclamación «!»** y es una **insignia de aviso con color** (relleno, tono de aviso), nunca una casilla/checkbox ni un recuadro de contorno que parezca una opción más. | Test: el aviso tiene icono `!` y NO contiene glyph de casilla; captura donde se vea que no se confunde con una opción. |
| UI-03 | La UI debe verse **moderna** (no «de plantilla») y **compacta**: «massa espaiosa» es un defecto. | Captura en dispositivo/emulador; el usuario es el árbitro del «moderno». |
| UI-04 | Con las **preguntas reales** (enunciados y opciones largos, hasta 4-5 opciones) la pantalla de pregunta **no debe obligar a hacer scroll** a escala de fuente 100 % en un móvil normal (~360×800 dp). Scroll solo como último recurso (200 %). | Capturas de TODAS las preguntas del banco real a 100 % (fuera del repo, ver abajo) mostrando que caben sin scroll; el informe lista cuáles hacen scroll y por qué. |
| UI-05 | (2026-11-24) Refactorizar la UI (Inici, Pregunta, Resultat i credencial) para que tenga **el estilo de los diseños de Claude Design** en `new-designs/` (fondo `#F3F6F2`, tarjetas blancas sin borde con sombra suave y radio 24-28, botones píldora, progreso segmentado, hero verde con círculos, iconos en recuadro `#E4F0E8`, etc.). **No** sustituye a UI-01..UI-04: ante conflicto (p. ej. alturas de 64 dp, insignia sin «!», hints visibles) mandan UI-01..UI-04 y se adapta el estilo. | Capturas ANTES/DESPUÉS de las 3 pantallas junto al mockup (claro; oscuro y 130/200 % sin romper) y tabla de conformidad por pantalla con las desviaciones justificadas. |
| UI-06 | (2026-11-24) **TalkBack/accesibilidad para lectores de pantalla no es un requisito**: se retira del roadmap y de las pruebas en dispositivo. Siguen vigentes contraste, objetivos táctiles y escala de fuente. | No se exige prueba TalkBack ni se marca [PV]. |

## Reglas de proceso (para que no se vuelva a incumplir)

1. Los agentes **leen este fichero antes** de especificar, implementar o revisar UI. Las specs lo citan por ID.
2. `reviewer` y QA visual dan **FAIL** si falta la prueba de alguna fila (mecánica o captura). «Cumple por diseño» no vale.
3. Una nueva petición de UI del usuario se registra aquí **antes** de implementarla.
4. Capturas con **preguntas reales** (UI-04): nunca dentro de este repo público. Se guardan en la carpeta privada
   del banco (`arbitratge-bank/work/qa/`). En el repo solo capturas con el banco de muestra.
