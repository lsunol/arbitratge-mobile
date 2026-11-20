# Verificación final del diseño

**Veredicto global: FAIL.** El contrato `ARB1` tiene una fuente canónica en `02`, pero persisten contradicciones en `04` y en las puertas de prueba de `05`. La preparación offline está definida mediante caché completa, aunque algunos textos aún prometen funcionamiento sin esa condición.

**Alcance:** revisión documental de `01–05`, los ADR aprobados y los hallazgos B1–B3. Se contrastaron los ejemplos con el generador local del banco base. No se modificaron los ADR ni se ejecutaron pruebas de implementación, dispositivos o historial Git.

| Comprobación | Resultado |
|---|---|
| 1. Contenido real identificable en `01–05` | **PASS**, limitado a la revisión documental y al banco base disponible. |
| 2. Contrato único en `02`; sin contradicciones en `03–05` | **FAIL** |
| 3. Pegado y normalización | **PASS documental** |
| 4. «Listo offline» coincide con caché | **FAIL estricto**: regla alineada, promesas incondicionales pendientes. |
| 5. B1–B3 de la revisión final | **FAIL global**: B1 sigue sin evidencia completa; B2 y B3 pasan documentalmente. |

## 1. Contenido del banco — PASS limitado

La referencia identificable señalada anteriormente en `01` se ha sustituido por una descripción general (`docs/design/01-requisitos.md:220`). El ejemplo de `03` es ficticio (`docs/design/03-arquitectura-movil.md:485–530`). En la lectura y contraste manual de `01–05` no se identificó una reproducción de pregunta o respuesta del banco base disponible.

**Límites:** esto no prueba la ausencia de contenido de una eventual base de producción distinta, ni audita todo el árbol o historial. Los wireframes de preguntas de `04` no están marcados como ficticios; conviene sustituirlos por ejemplos inequívocamente ficticios o verificarlos editorialmente (`docs/design/04-ux.md:293–328, 498–516`).

Además, `05` identifica directamente el generador del banco base (`docs/design/05-distribucion.md:60`), mientras `02` declara que no debe señalarse su ubicación (`docs/design/02-credencial-y-amenazas.md:654–663`). Es un **puntero**, no contenido de una pregunta; debe retirarse para cumplir esa regla documental sin reproducirlo en este informe.

## 2. Contrato único y consumidores — FAIL

**Comprobado:** `02` establece la propiedad normativa de `ARB1` (`docs/design/02-credencial-y-amenazas.md:239–250, 574–583`) y `03` remite a ese contrato para los bytes, campos y códigos (`docs/design/03-arquitectura-movil.md:693–725`). La antigua discrepancia sobre qué bytes firmar está corregida en `03`.

**Contradicciones restantes:**

1. **Bloqueante — caducidad y código corto en UX.** `ARB1` reserva `exp`, no emite caducidad y define el código corto como ocho dígitos hexadecimales (`docs/design/02-credencial-y-amenazas.md:349, 358–368`). Los wireframes y el comportamiento de `04` muestran caducidad y un código no hexadecimal como si fueran datos de la credencial (`docs/design/04-ux.md:585–586, 623–624, 637–639, 681–682, 905–906, 924–927`).  
   **Acción:** hacer que las pantallas y ejemplos de `ARB1` respeten `02`; dejar la caducidad futura claramente fuera del MVP.

2. **Bloqueante — resultado esperado imposible en G1.** `02` asigna a F02 el código `IS_URL` (`docs/design/02-credencial-y-amenazas.md:559–560`), pero `05` exige tanto `F01–F09 BAD_FORMAT` como `F02 IS_URL` (`docs/design/05-distribucion.md:570`).  
   **Acción:** excluir F02 del rango `BAD_FORMAT` y usar los resultados exactos de `02`.

3. **Importante — significado de clave retirada.** `02` acepta claves `retired` y rechaza `compromised` (`docs/design/02-credencial-y-amenazas.md:452, 511–512`); `04` equipara un `kid` retirado con `KID_NOT_ACCEPTED` (`docs/design/04-ux.md:981–983`).  
   **Acción:** corregir el texto de `04` para distinguir ambos estados.

4. **Importante — contexto de los vectores de release.** R01 debe firmarse con la clave inyectada y verificarse con la PWA a publicar (`docs/design/02-credencial-y-amenazas.md:520–522, 567`). Sin embargo, G1 lo incluye en `ci` y release, y G5 exige el fichero completo con verificadores de test (`docs/design/05-distribucion.md:570, 576–581`), mientras `ci` no recibe secretos (`docs/design/05-distribucion.md:312, 320`).  
   **Acción:** separar los vectores públicos de test de R01 y K02, indicando para cada uno lista de confianza, clave y fase de ejecución; reservar R01 de producción a la puerta de release.

5. **Importante — emisión y nombre.** `03` exige confirmar o cambiar el nombre tras aprobar antes de sellar (`docs/design/03-arquitectura-movil.md:344, 363–370`); `04` lo congela antes del examen y describe emisión inmediata (`docs/design/04-ux.md:273–279, 456–458`).  
   **Acción:** alinear el flujo de `04` con la transición vigente de `03`, o acordar una única transición sin cambiar los ADR.

## 3. Pegado y normalización — PASS documental

`02` fija una función común para cámara, imagen y texto: límite previo de 2048 unidades UTF-16, eliminación de los siete caracteres enumerados y ninguna otra transformación (`docs/design/02-credencial-y-amenazas.md:419–434`). `04` reproduce esas mismas reglas y exige partir el token sin guiones de división (`docs/design/04-ux.md:863–874`).

**Pendiente de prueba:** vectores con caracteres interiores y exteriores, guiones legítimos, entrada vacía, límite de longitud y los tres canales.

## 4. Preparación offline — FAIL estricto

La regla principal sí coincide: `04` solo muestra «preparat» cuando todos los recursos están en caché, independientemente de la instalación (`docs/design/04-ux.md:785–788, 1076–1105`); `05` exige el manifiesto completo (`docs/design/05-distribucion.md:724`).

No obstante, `04` también afirma que estando offline «todo sigue funcionando» sin condicionar esa frase al estado preparado (`docs/design/04-ux.md:789`), y el texto visible de la guía promete funcionamiento posterior sin conexión tras hacerlo una vez (`docs/design/04-ux.md:1083–1084`). **Acción:** condicionar ambas frases a la comprobación de preparación y explicar que la caché puede desaparecer.

La coincidencia documental no demuestra funcionamiento real: probar cierre y reapertura en modo avión, caché incompleta o borrada, cámara, imagen y pegado en Chrome/Edge Android.

## 5. B1–B3 de la revisión final

- **B1 — FAIL para cierre completo.** Los ejemplos detectados en `01` y `03` ya no reproducen la pregunta señalada; sin embargo, B1 también pedía revisar árbol, historial y salidas destinadas a publicación (`docs/design/reviews/revision-final-diseno.md:59–63`). Esa revisión no se ha acreditado aquí. `05` conserva además avisos obsoletos sobre `01` (`docs/design/05-distribucion.md:867, 896`).
- **B2 — PASS documental.** Los documentos presentan como decisiones vigentes la PWA, la firma asimétrica y el banco sin ofuscación inicial; las alternativas se marcan históricas (`docs/design/01-requisitos.md:348–370, 419–420`; `docs/design/03-arquitectura-movil.md:760–763`).
- **B3 — PASS documental.** Arquitectura exige persistir antes de avanzar o finalizar y UX ya impide continuar con una respuesta no guardada (`docs/design/03-arquitectura-movil.md:350–360, 375–397`; `docs/design/04-ux.md:345–349`). Falta probar fallos de guardado y muerte del proceso.

## Riesgos y cierre

Las contradicciones anteriores impiden dar **PASS** al diseño, sin necesidad de reabrir los ADR. Siguen pendientes la interoperabilidad Android–PWA, la preparación offline real y la auditoría previa a publicación. La clave de sello y el banco son extraíbles del APK por decisión aceptada; una firma válida no demuestra que se haya aprobado el examen.

Antes de implementar, debe recordarse revisar qué agentes harán falta para esa fase (`docs/design/README.md:44–54`).

## Preguntas abiertas para el usuario

Ninguna necesaria para corregir estos hallazgos; las decisiones aprobadas no se reabren.