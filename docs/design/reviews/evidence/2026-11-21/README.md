# Evidència visual — 21-11-2026

Captures d'un emulador Android API 36, pantalla 1080×2400, con la build debug que incorporaba:

- navegación libre con `←/→`;
- barra de progreso por **posición de pregunta**, no por número de respuestas;
- ningún estado visible «Resposta desada»;
- chip compacto «Resposta múltiple» y ayuda «Marca-les totes»;
- controles a la izquierda;
- `Cert / Fals`;
- botones iguales al 100 %, reflow al 200 %;
- marca «Centre del camp» y Atkinson Hyperlegible.

| Fichero | Estado |
|---|---|
| `question-single-100-light.png` | Pregunta de respuesta única, fuente 100 %, tema claro |
| `question-multiple-100-light.png` | Pregunta múltiple, 100 %, claro |
| `question-true-false-130-light.png` | Cert/Fals, 130 %, claro |
| `question-true-false-200-light.png` | Cert/Fals, 200 %, claro; los controles de navegación quedan apilados |
| `question-multiple-100-dark.png` | Pregunta múltiple, 100 %, oscuro, versión final con `Marca-les totes` |
| `overview-100-light.png` | Resumen de preguntas, 100 %, claro |

Datos visibles: todos ficticios, de `bank-sample/`. No contienen banco real ni datos personales reales.

Validación manual observada:

- la barra avanza/retrocede al navegar, independientemente del recuento de respuestas;
- «Resposta desada» no aparece debajo de las opciones;
- el chip múltiple no domina la pantalla;
- a 200 %, el recuento/resumen pasa al contenido desplazable para no ocupar cabecera fija;
- los controles múltiples y los radios están a la izquierda;
- el modo oscuro mantiene legibles bordes, texto, timer, etiqueta y botones;
- las preguntas reales largas pueden crecer las filas y el contenido usa un único scroll.

**No probado aquí:** TalkBack real, cámara física (no aplica a la app candidata), instalación final de release
ni Android API 26. TalkBack sigue [PV].
