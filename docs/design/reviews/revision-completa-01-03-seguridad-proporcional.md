# Revisión completa 01–03: seguridad proporcional

**Alcance:** `docs/design/01-requisitos.md`, `02-credencial-y-amenazas.md` y `03-arquitectura-movil.md`, contrastados con `AGENTS.md`, el plan de diseño y el proyecto base. Esta revisión sustituye conceptualmente a la revisión intermedia; no presupone el modelo anterior de emisión supervisada.

**Veredicto:** el diseño recoge el autoservicio, el feedback pedagógico y las limitaciones de seguridad decididas. Hay **dos bloqueantes para publicar el diseño y fijar el contrato del MVP**: un ejemplo reproduce una pregunta real con respuesta y la detección de alteraciones se exige, pero la herramienta que la hace posible queda opcional o aplazada. No se propone volver al modelo estricto.

**Límites de la revisión:** no hay implementación ni pruebas ejecutadas. `docs/adr/` está vacío; `04-ux.md` y `05-distribucion.md` aún no existen. No se ha auditado el historial Git completo ni la configuración de una futura CI. Las citas de `01`, `02` y `03` son relativas a `docs/design/`.

## 1. Comprobación de las decisiones vigentes

| Decisión | Resultado |
|---|---|
| **Autoservicio sin organizador** | **Cumplida en el diseño.** La app corrige y emite si se aprueba; la verificación es posterior y optativa (`01-requisitos.md:172–201`; `03-arquitectura-movil.md:297–325`). |
| **Feedback para aprender** | **Incluido.** Se muestran número de fallos, respuesta marcada y correcta para cada fallo y explicación cuando exista; las preguntas sin responder cuentan como fallo (`01-requisitos.md:178–181`; `03-arquitectura-movil.md:278–294`). |
| **Firma embebida descrita honestamente** | **Cumplida con matices.** `02` y `03` advierten que la clave es extraíble y permite forjas indistinguibles; deben corregirse algunas afirmaciones demasiado generales sobre lo que detecta el sello (`02-credencial-y-amenazas.md:175–211`; `03-arquitectura-movil.md:563–577`). |
| **Banco fuera del repositorio público** | **El diseño de build lo prevé, pero existe una infracción puntual en el propio documento 03.** Además, el banco del proyecto base ya es público; no se puede afirmar confidencialidad retroactiva. Véase B1. |
| **Revocación en fase 2** | **Cumplida.** No se exige backend ni sincronización para el MVP (`01-requisitos.md:361–369`; `02-credencial-y-amenazas.md:469–484`; `03-arquitectura-movil.md:854–869`). Un identificador por sí solo no revoca nada. |
| **Suplantación aceptada** | **Cumplida.** La identidad es declarada y el préstamo del móvil queda como riesgo social (`01-requisitos.md:160–165`; `02-credencial-y-amenazas.md:111–115`). |
| **Repositorio público y posible fuente privada** | **Previstos**, con build local o inyección privada; la elección entre ambas vías sigue abierta (`03-arquitectura-movil.md:462–486`). |
| **Verificador opcional; sin decisión sobre app de emisión privada** | **Respetado para el examen y la emisión**, pero contradictorio con la verificación obligatoria de la credencial en `01`. Véase B2. |

## 2. Hallazgos bloqueantes

### B1. Un ejemplo del diseño contiene una pregunta real y su respuesta

**Evidencia.** `03-arquitectura-movil.md:417–427` incluye un ejemplo JSON con enunciado, opciones y respuesta que coincide con la primera pregunta de un generador histórico del banco del proyecto base. La [API de GitHub](https://api.github.com/repos/cfbbarcelona/arbitratge) confirma que el repositorio base es público y su generador histórico del banco es accesible. Esto contradice la intención de mantener el banco real fuera del repo público (`01-requisitos.md:133–144`; `03-arquitectura-movil.md:388–396`).

**Alcance preciso.** Se ha identificado **una pregunta reproducida en el documento**, no el banco completo en este repositorio. La exposición del banco base ya existía: no debe confundirse con una fuga nueva completa ni presentarse como reversible. No se ha verificado el historial Git completo de este proyecto ni que el documento actual esté publicado.

**Acción antes de publicar:** sustituir el ejemplo por contenido inequívocamente ficticio, revisar los demás ejemplos y el historial destinado a publicarse, e impedir que preguntas, respuestas o explicaciones reales entren en commits, Pages, logs o artifacts independientes. La fuente privada puede contener las 75 preguntas ya públicas para reutilizarlas, pero no recupera su secreto.

**Documentos:** `03`; controles posteriores en `05`.

### B2. Integridad obligatoria frente a verificación aplazada

**Evidencia.** `01-requisitos.md:304–318` exige detectar edición visual y verificar sin red. `02-credencial-y-amenazas.md:25–31` reconoce correctamente que, sin un verificador independiente y comparación del texto con el QR, el sello no detecta esa edición. Sin embargo, `03-arquitectura-movil.md:219–238` recomienda el verificador en una fase posterior y contempla lectura solo visual.

**Consecuencia.** Una credencial emitida automáticamente puede llevar sello sin que nadie compruebe su integridad. La lectura a ojo permite entenderla, **no garantiza que no se haya retocado**. El problema no obliga a introducir organizador durante el examen.

**Opciones:**

1. **Recomendada:** mantener emisión completamente autónoma y disponer de una herramienta de comprobación offline sencilla e independiente para quien decida verificar. La comprobación no condiciona el examen ni la emisión. Si se decide una UI de verificación, respetar la separación de apps decidida.
2. Emitir primero y posponer la herramienta, documentando expresamente que **hasta que se use no se detecta la edición**; no afirmar entonces que el MVP ya ofrece detección operativa.

La elección del sello y de una UI no está cerrada. Lo bloqueante es que el contrato, las recomendaciones y los textos no prometan a la vez verificación efectiva y ausencia de medio de verificación.

**Documentos:** `01`, `02`, `03`; después, `04` y ADR.

## 3. Hallazgos importantes

### I1. El checksum se presenta como si resistiera toda falsificación no técnica

`02-credencial-y-amenazas.md:122–146` y `:160–173` califican el checksum como suficiente frente al perfil casual. **Un checksum sin clave no autentica al emisor**: protege frente a cambios accidentales o a quien cambia datos sin recalcularlo. Al existir el algoritmo público, una herramienta preparada por otra persona puede permitir recalcularlo sin conocimientos técnicos. Lo mismo ocurre con una firma embebida una vez difundido un generador basado en la clave extraída (`02:199–204`).

**Acción:** expresar la protección como barrera condicionada, no como garantía por categoría de usuario. Para cambios visuales, exigir escaneo en herramienta confiable y comparación humana; para forjas con herramientas difundidas, declarar el riesgo residual aceptado. Afecta a `01:341–349`, `02:122–173` y `03:584–599`.

### I2. Recuperación del reloj tras reinicio incompleta y contradictoria

`01-requisitos.md:254–257` recomienda estimar con hora de pared y un tope; `03-arquitectura-movil.md:669–679` propone conservar el último tiempo restante sin contar el apagado ni aplicar tope. Esta segunda opción es compatible con la indulgencia y el riesgo aceptado, pero `03:656–667` sigue usando un `start_mono` del arranque anterior: no especifica cómo se reancla el reloj tras reiniciar.

**Acción recomendada:** al recuperar, persistir atómicamente el tiempo restante y un nuevo origen monotónico del arranque actual; inicializar el restante al comenzar y definir recuperación cuando no exista `boot_count` fiable. Ensayar reinicio antes de responder, tras varias respuestas, reinicios repetidos y muerte de proceso durante la recuperación. No añadir sanciones ni un servidor.

### I3. Contratos de build y firma no corresponden a todas las opciones

`03-arquitectura-movil.md:473–476` exige `ARB_SEAL_KEY_FILE` para todo release, aunque `03:584–599` contempla checksum sin clave. Además, `02:509–515` habla de firma y lectura QR como necesidades, aunque son recomendaciones todavía no decididas.

**Acción:** condicionar la clave y la tabla de claves al esquema firmado; con checksum, compilar sin secreto de sello. Mantener el fallo de build si falta el banco real y distinguir **clave del sello embebida y extraíble** de **keystore de firma del APK, que nunca debe distribuirse dentro de él**. Afecta a `02`, `03` y futuro `05`.

### I4. Validación del banco y emisión: faltan límites de funcionamiento

`03-arquitectura-movil.md:454–459` valida relaciones entre preguntas y umbral, pero no especifica límites inferiores: número de preguntas o tiempo cero/negativo, umbral cero, opciones duplicadas o selección inválida. Una configuración defectuosa podría provocar un examen vacío o un aprobado incorrecto sin ataque técnico.

**Acción:** validar reglas positivas, `1 ≤ umbral ≤ N ≤ preguntas activas`, IDs únicos y respuestas válidas; probar que suspenso, abandono y fallo de persistencia no emiten credencial. En recuperación de `FINISHED`, repetir corrección de forma determinista y emitir solo si se ha confirmado el aprobado (`03:315–325`).

### I5. Compatibilidad y fecha: promesas excesivas

- `03-arquitectura-movil.md:596–600` dice que pasar de checksum a firma no rompe verificadores porque aceptan N y N−1. **Un verificador antiguo no puede conocer automáticamente un formato futuro**; el actualizado puede aceptar formatos anteriores si se implementa expresamente. La regla N/N−1 también puede dejar fuera credenciales aún vigentes (`03:627–639`).
- `03:575–578` dice que la fecha no decide validez, pero `03:240–249` contempla comprobar caducidad; `02:390–398` reconoce que la fecha inicial procede del candidato y es manipulable.

**Acción:** especificar comportamiento de versión desconocida y conservación de formatos/claves anteriores mientras corresponda. Si se decide caducidad, explicar que se compara con el reloj del verificador, pero que una expiración calculada desde la fecha del candidato puede estar manipulada. No equivale a revocación.

### I6. Pérdida del móvil y firma del APK necesitan procedimiento concreto

`03-arquitectura-movil.md:344–349` desactiva el backup y confía en exportar PDF/imagen; `01-requisitos.md:182–184` contempla una copia o repetir el examen. No se garantiza que el usuario haya guardado la copia ni que pueda recuperarse sin ella.

`03:704–706` identifica la firma del APK como necesaria para actualizar, y `02-credencial-y-amenazas.md:423–431` pide copia de seguridad, pero falta un procedimiento operativo. Sin la clave de firma del APK, no debe prometerse una actualización normal sobre la instalación existente; las posibilidades concretas de rotación dependen de la versión y configuración y deben verificarse.

**Acción:** explicar en catalán cómo guardar y recuperar la copia, qué se pierde al desinstalar y que repetir emite otra credencial. Para distribución, documentar custodia y restauración probada del keystore y contraseñas, y un plan de reinstalación/comunicación si se pierde. No confundirlo con rotar la clave extraíble del sello.

### I7. Privacidad y accesibilidad requieren criterios de aceptación

`01-requisitos.md:277–296` minimiza datos; `03-arquitectura-movil.md:333–349` guarda nombre, respuestas y credencial localmente. El QR permite leer los datos y, si se activa la consulta de actualizaciones, GitHub recibe la IP (`03:710–722`). No se detallan borrado desde la app, información de tratamiento, conservación de copias compartidas ni situación de menores.

**Acción:** preparar una nota de privacidad comprensible en catalán: responsable y finalidad cuando correspondan, datos locales y exportados, borrado y límites de recuperar copias compartidas, y consulta de versión opcional. Revisar la situación de menores y obligaciones aplicables sin afirmar cumplimiento legal automático.

Las propuestas de TalkBack, escala y contraste son buenas (`03:777–791`), pero deben probarse también en **feedback, exportación, QR y temporizador**, con personas mayores y dispositivos reales. Definir la adaptación de tiempo como opción de producto, sin exigir un organizador.

### I8. Controles de publicación aún no son evidencia de que no haya fugas

`03-arquitectura-movil.md:494–508` propone `.gitignore`, escáner y *canary*. Son útiles, pero un canary solo detecta su propio valor donde se busque; no demuestra que otro texto del banco, un fichero renombrado, un commit antiguo o una transformación no se haya filtrado.

**Acción:** antes de publicar, inspeccionar historial destinado al repo público, árbol, configuración de Pages, logs, cachés y artifacts; usar solo datos ficticios en CI pública de contribuciones. Si se usa build local, no convertir CI privada en requisito artificial. La auditoría y su resultado pertenecen al futuro `05`.

## 4. Hallazgos menores y simplificaciones

- **M1 — Estado documental.** `01-requisitos.md:43–47` aún pregunta si el repo base es público; `02:280–297` ya lo verifica. Actualizar `01` con el hecho y retirar esa pregunta resuelta. `docs/design/README.md:20–28` todavía marca 01–03 como pendientes.
- **M2 — Ofuscación del banco.** `02:300–313` y `03:488–492` recomiendan gzip y AES-GCM con clave embebida. **Recomendación:** omitirlo inicialmente; añade código y mantenimiento sin proteger contra extracción. Si se conserva, llamarlo únicamente obstáculo de lectura superficial, nunca confidencialidad.
- **M3 — Coste del verificador.** `03:229–235` estima aproximadamente un 10 % del esfuerzo; no hay medición. Tratarlo como estimación, incluyendo mantenimiento de segunda app, claves, cámara, pruebas y distribución.
- **M4 — Preparación prematura.** `03:854–869` prevé interfaces de política de confianza para fase 2 y `03:327–329` un campo de práctica. No son necesarios para entregar autocorrección. Mantener formatos versionados e identificador simple es razonable; aplazar interfaces y campos que no tengan uso real en MVP.
- **M5 — Attestation.** `02:239` afirma que Key Attestation prueba app íntegra en dispositivo no rooteado. Esa formulación es demasiado amplia. Corregirla o eliminarla: no es requisito del MVP ni una solución propuesta para restaurar el modelo estricto.
- **M6 — Ejemplos y tamaños.** Los tamaños de APK y QR son estimaciones, no resultados (`03:128–137`, `:555–561`). Validar QR con nombres largos y caracteres catalanes, pantalla pequeña, impresión y captura antes de fijar límites y mensajes.

## 5. Red team concreto de la credencial

La columna «viable» describe el diseño propuesto, **no una prueba de una app existente**. La detección presupone que se use un verificador confiable, independiente del móvil del candidato, y que se comparen sus datos con el documento.

| Intento | Resultado y razón |
|---|---|
| Cambiar el nombre o la nota visibles en PDF/imagen sin tocar el QR | **Viable sin detección si nadie verifica.** Con escaneo y comparación, la discrepancia se detecta. El sello no examina automáticamente el aspecto completo del PDF. |
| Pegar el QR válido de otra persona en un documento con otro nombre | **Viable sin comparación; detectable** al comparar el nombre decodificado con el visible. Mostrar la credencial completa de otra persona o prestar el móvil sigue siendo riesgo social aceptado. |
| Leer el QR, cambiar datos y generar otro sin conservar el sello | **Viable generar el QR; detectable** por un verificador de checksum o firma correctamente implementado. Sin verificador, pasa por inspección visual. |
| Recalcular checksum para otros datos | **Viable:** no hay clave; basta implementar o usar una herramienta que implemente el formato público. Una persona no técnica también podría usar una herramienta ya preparada. |
| Extraer clave de sello del APK y emitir una credencial sin aprobar | **Viable para un técnico** en la opción firmada: puede extraer o invocar el material de firma y generar tokens válidos. Después puede compartir un generador. Riesgo aceptado; no prueba de aprobación real. |
| Modificar la app para que apruebe o llame al emisor directamente | **Viable para un técnico**, fuera del nivel de protección decidido. La firma de la credencial no demuestra que la corrección se ejecutó correctamente. |
| Extraer preguntas y respuestas | **Viable:** el APK público contiene el banco; si se empaqueta como archivo visible puede extraerse como ZIP. El feedback también revela respuestas de fallos. Ambas consecuencias están aceptadas. |
| Cambiar fecha o alargar caducidad con el reloj del candidato | **Viable:** la fecha de emisión no es de una fuente fiable. El reloj del verificador permite comparar una expiración declarada, no validar cómo se obtuvo. |
| Repetir hasta aprobar o pedir a otra persona que responda | **Viable y aceptado:** no hay límite fiable ni comprobación de identidad. No se recomienda añadir controles. |
| Enseñar una pantalla falsa «válida» o un verificador web falso | **Viable** si se confía en la pantalla o enlace del candidato. Mitigación proporcional: comprobar en herramienta propia y oficial, sin tratar un QR como prueba por sí mismo (`02:103–119`, `:365–375`). |
| Distribuir APK alterado como descarga inicial | **Viable** mediante un canal engañoso. La firma Android ayuda a impedir que un APK de otra clave actualice una instalación existente, pero no autentica por sí sola una primera instalación; un hash publicado en la misma página comprometida tampoco es una fuente independiente. Recomendar canal oficial y comunicación clara, sin prometer protección absoluta. |

## 6. Recomendación proporcional para continuar

1. Corregir B1 antes de publicar y B2 antes de cerrar las afirmaciones del MVP.
2. Conservar **autocorrección, intentos libres y feedback**; no introducir organizador, backend, atestación ni controles contra el candidato técnico.
3. Elegir entre **checksum** —menor gestión, sin autenticación— y **firma embebida** —barrera adicional limitada, con gestión de claves y forja posible tras extracción—. Ambas requieren comprobación y comparación para detectar edición visual; ninguna demuestra por sí sola que la persona aprobó.
4. Priorizar pruebas de corrección, persistencia, reinicio, versión del banco, exportación y accesibilidad sobre mecanismos de seguridad más complejos.
5. Completar posteriormente `04`, `05` y los ADR compartidos. Al cerrar la fase de diseño, recordar la revisión de los agentes necesarios para implementación (`AGENTS.md:45–47`).

## Preguntas abiertas para el usuario

*Cuestiones residuales registradas, sin reabrir las decisiones ya tomadas:*

- Sello con checksum o firma embebida, y alcance práctico del verificador independiente.
- Política de caducidad y tratamiento de versiones antiguas.
- Build local inicial o fuente privada inyectada en CI; procedimiento de custodia del keystore.
- Adaptaciones de tiempo, navegación y conservación del historial local.
- Datos personales, posible participación de menores y procedimiento de privacidad aplicable.