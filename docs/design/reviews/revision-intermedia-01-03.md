# Revisión intermedia de diseño: documentos 01–03

**Alcance:** `docs/design/01-requisitos.md`, `02-credencial-y-amenazas.md` y `03-arquitectura-movil.md`.  
**Estado:** revisión documental, sin ejecutar ataques ni prototipos. No se han modificado archivos.  
**Ruta del informe:** `docs/design/reviews/revision-intermedia-01-03.md`.

## Resumen y veredicto

**No recomiendo cerrar el diseño ni iniciar la implementación todavía.** La separación entre corrección y dispositivo del candidato es una buena dirección, pero las garantías de confidencialidad, emisión y revocación aún necesitan precisarse.

- **Bloqueantes para el cierre:** elección de seguridad pendiente; confidencialidad del texto incompatible con K1 sin cifrado; protocolo de emisión insuficientemente especificado; revocación basada en fechas que una clave comprometida puede falsificar; hash del banco imposible de verificar en la CI pública tal como está descrito.
- **Importantes:** límites exagerados de la atestación, reloj y control de intentos; recuperación y privacidad incompletas; incompatibilidades en el ciclo de vida.
- **Fuera de revisión:** `04-ux.md`, `05-distribucion.md` y ADRs aún no existen. Su ausencia concuerda con el carácter intermedio de este encargo, no es un defecto de esos documentos.
- **Recomendación condicional:** estudiar primero **organizador que corrige y firma**, más cifrado por pregunta si se mantiene la confidencialidad del texto; comparar con examen presencial en dispositivo del club. Ninguna opción se da por decidida.

## 1. Alcance y criterio de evidencia

- **[H] Hecho documental:** las instrucciones exigen ausencia de backend y confidencialidad tanto de preguntas como de respuestas; el nivel de seguridad lo decide el usuario (`AGENTS.md:13-18`).
- **[H]** El plan prevé decisiones con el usuario y ADRs antes de completar UX y distribución, seguidos de una revisión integral (`docs/design/README.md:7-18`).
- **[H]** La carpeta `docs/adr/` estaba vacía y en `docs/design/` solo estaban README y 01–03 en esta revisión.
- **[H]** El `.gitignore` local del proyecto base incluye `scripts/questions.sql` en la línea 6. Esto **no demuestra** que nunca se publicara en el historial, ni que otro archivo o copia no exponga preguntas. El README local del proyecto base contiene ejemplos de preguntas y respuestas (ejemplos del proyecto base); no se ha comprobado si coinciden con el banco real ni su historial público.
- **[A] Análisis del revisor:** conclusiones y escenarios derivados de los documentos; no equivalen a vulnerabilidades demostradas en una implementación.
- **[PV] Pendiente de verificar:** afirmaciones dependientes de dispositivos, servicios externos, despliegues Android, tamaño real de QR o pruebas criptográficas.

Las severidades indican prioridad **para cerrar el diseño**, no la existencia de una aplicación vulnerable.

## 2. Hallazgos clasificados

### Bloqueantes

#### B1. La recomendación principal no cumple toda la confidencialidad declarada

**Evidencia.** Se exige confidencialidad de preguntas y respuestas (`AGENTS.md:16-18`; `01-requisitos.md:131`). La opción C protege las respuestas, pero deja los textos en el APK (`01-requisitos.md:142`); K1 recomendado conserva esa propiedad (`02-credencial-y-amenazas.md:581-585`). El propio documento móvil reconoce la contradicción (`03-arquitectura-movil.md:524-528`).

**[A] Impacto.** Un repositorio privado evita publicar la fuente, pero no protege contenidos incluidos en el APK público. C tampoco evita que el candidato fotografíe o comparta las preguntas que se le muestran. Calificar el híbrido como “muy alta” confidencialidad (`01-requisitos.md:144`) sin estos límites puede inducir a error.

**Acción — 01, 02, 03 y futuro ADR.** Presentar separadamente:

1. confidencialidad del **banco fuente**;
2. confidencialidad del **texto no entregado**;
3. confidencialidad de las **respuestas correctas**;
4. exposición inevitable y acumulativa de lo mostrado.

Mantener C sin cifrar como alternativa **solo si el usuario acepta hacer públicos los textos**. Evaluar C + cifrado por pregunta y el dispositivo del club sin declarar que proporcionan secreto absoluto.

#### B2. El protocolo de corrección y emisión no define suficientes validaciones

**Evidencia.** K1 permite selección y cronómetro locales (`02-credencial-y-amenazas.md:581-583`); la hoja contiene IDs y respuestas (`03-arquitectura-movil.md:477`), y el organizador conserva nonces (`03-arquitectura-movil.md:348-350`). El contrato completo de validación de la hoja no está especificado.

**[A] Impacto.** Un cliente modificado puede fabricar una hoja con el nonce recibido, elegir preguntas conocidas o construir respuestas fuera del flujo previsto. El nonce demuestra referencia a una invitación, **no** que el examen se haya ejecutado correctamente. Si el corrector no valida unicidad de IDs, cantidad y pertenencia a la sesión, podrían aparecer ataques adicionales; su viabilidad depende de la implementación.

**Acción — 02 y 03.** Especificar y probar el protocolo extremo a extremo:

- invitación auténtica, identificador único, emisor, versión de banco/reglas, candidato o mecanismo de asociación, selección autorizada, límite y vigencia;
- hoja vinculada a esa invitación; IDs de preguntas y opciones válidos y únicos, cantidad, tratamiento de ausencias, incompatibilidades y tamaño máximo;
- corrección con el banco y reglas de la versión exacta; emisión **solo si** se cumple el umbral;
- resultado de suspenso o rechazo ligado a la sesión, y credencial aprobada ligada a la sesión e identidad correctas;
- consumo atómico del nonce y recuperación ante interrupción, reenvío o pérdida del estado.

La selección autorizada por el organizador y la validación del corrector reducen manipulación; **no prueban** por sí solas que el candidato contestó sin ayuda ni que siguió la navegación.

#### B3. Una fecha firmada por una clave comprometida no permite salvar las credenciales anteriores

**Evidencia.** El diseño propone mantener credenciales anteriores al compromiso basándose en `iat` (`02-credencial-y-amenazas.md:419`, `:560-561`).

**[A] Ataque.** Quien puede usar la clave comprometida firma hoy una credencial falsa con `iat` anterior al supuesto compromiso. La firma verifica y el verificador offline no dispone de una fecha de emisión independiente.

**Acción — 02, 03 y futuro ADR.** No prometer separación fiable por `iat`. Opciones:

- **Revocar todas** las credenciales del emisor comprometido y reemitir: simple y segura, pero perjudica a titulares legítimos.
- Conservar excepciones mediante **registro independiente previo al compromiso** o comprobación humana: menor impacto, más operación y privacidad.
- Aceptar el riesgo de fechas retroactivas: menos trabajo, menor seguridad.

Definir además política para listas antiguas, ausencia de lista, reloj del verificador, versiones monotónicas y rechazo de listas firmadas antiguas —*rollback*— cuando exista estado previo. Sin actualización, ningún verificador offline conoce revocaciones nuevas.

#### B4. El `bank_hash` y la comprobación de CI no encajan con la separación de secretos

**Evidencia.** `bank_hash` se define sobre contenido **más clave de respuestas más reglas** (`03-arquitectura-movil.md:469-470`), pero la CI pública debe verificarlo sin que la clave entre en el repositorio público (`03-arquitectura-movil.md:535-556`). El contenido puede estar cifrado, aunque se exige validar su JSON.

**[A] Impacto.** Con solo contenido público, la CI no puede recalcular un hash que incluye la clave privada. Tampoco puede validar el JSON cifrado sin acceso autorizado al claro. Publicar un hash de datos secretos puede ser un **oráculo de comprobación de conjeturas** si el secreto restante tiene poca entropía; no implica que recuperar todo el banco de 75 respuestas sea necesariamente práctico.

**Acción — 03.** Separar identificador/hash público del artefacto distribuido y controles privados del contenido más respuestas. Definir bytes exactos y canonicalización, delimitación, versionado, clave/IV y datos autenticados del cifrado; verificar cada cosa en el entorno que realmente tiene acceso. Evitar exponer compromisos públicos de respuestas salvo necesidad y análisis específico.

#### B5. No existe aún un objetivo de seguridad aceptado

**Evidencia.** El nivel lo fija el usuario (`AGENTS.md:18`) y los tres documentos son borradores. No hay ADRs. Los niveles y costes de 02 son estimaciones, no una decisión aprobada (`02-credencial-y-amenazas.md:115-120`).

**Acción — 01, 02, 03 y futuros ADRs.** Mantener abiertas las alternativas y registrar amenazas, garantías y renuncias por separado. No tratar “medio” como aceptación implícita ni escoger stack, PKI o distribución definitiva antes de la decisión.

### Importantes

#### I1. La atestación se presenta con demasiada certeza

**Evidencia.** 02 deduce que la firma la pidió la app genuina en un SO no rooteado (`02-credencial-y-amenazas.md:275-278`) y presenta K2 como prueba de origen de la hoja (`:363`). También reconoce excepciones y límites (`:283-307`).

**[A]** La atestación vincula propiedades declaradas de una clave y del dispositivo en condiciones específicas; no prueba por sí sola el contenido, la ausencia de ayuda externa, la aleatoriedad, el estado continuado del sistema ni la corrección del flujo. La seguridad depende de validación externa, reto fresco, vinculación exacta de la clave atestada a la firma de la hoja, cadena, política de certificados y revocación.

**Acción — 02 y 03.** Reformular K2 como **evidencia adicional condicionada**, no garantía de examen íntegro. Prototipar solo si el nivel elegido justifica compatibilidad, operación y mantenimiento.

#### I2. La cota del organizador no prueba duración individual ni ausencia de trampas

**Evidencia.** Se rechazan hojas posteriores a `t0 + límite + margen` (`02-credencial-y-amenazas.md:476-481`); 03 la llama cota “fuerte” (`03-arquitectura-movil.md:49-51`).

**[A]** Acota la ventana entre entrega y recepción **si el reloj y el registro del organizador son fiables**, pero no demuestra cuándo empezó realmente el candidato, ni impide preparación previa, colaboración o varios dispositivos. Una entrega remota lenta puede penalizar un intento legítimo.

**Acción — 01–03.** Definir el evento de inicio, referencia temporal del organizador, márgenes y tratamiento de reloj/reinicio. Calificar la cota como límite de ventana, no prueba completa de tiempo de examen.

#### I3. Reintentos y replay solo están controlados dentro del ámbito del registro

**Evidencia.** El nonce se registra localmente (`02-credencial-y-amenazas.md:476-478`), pero se afirma que el organizador controla los intentos (`01-requisitos.md:176`; `02-credencial-y-amenazas.md:589-590`).

**[A]** Un registro por dispositivo no coordina automáticamente varios organizadores. Pérdida, reinstalación o restauración de estado antiguo pueden perder consumos; una nueva identidad o invitación puede permitir otro intento salvo controles operativos.

**Acción — 01–03.** Delimitar control por sesión, organizador y persona. Elegir entre registro local con procedimiento humano, intercambio manual de registros o aceptar límites débiles. No prometer unicidad global offline.

#### I4. Ciclo de vida y reglas inconsistentes

- `01-requisitos.md:250` promete completar el examen iniciado con su versión; `03-arquitectura-movil.md:705-708` lo invalida si la migración es incompatible. **Acción:** presentar la invalidación como excepción y definir restitución del intento.
- `01-requisitos.md:71-73` describe recortar preguntas y umbral si faltan activas; `03-arquitectura-movil.md:473-476` exige que haya suficientes; sus pruebas vuelven a proponer `min(N, activas)` (`:796-798`). **Acción:** elegir validación estricta o recorte, sin cambiar silenciosamente el umbral.
- `03-arquitectura-movil.md:340-346` consume abandonos sin enviar hoja. **[A]** En K1 el organizador puede quedar esperando sin saber que terminó. Definir cancelación, caducidad y recuperación sin reabrir el intento.
- El ajuste de reinicio `clamp(..., monoDesdeArranque, cap)` (`03-arquitectura-movil.md:625-635`) no define qué pasa si la cota inferior supera `cap`; además, un tope de cinco minutos puede ignorar una interrupción más larga. `BOOT_COUNT` y sus supuestos están pendientes de prueba (`:595`). **Acción:** definir política y casos de prueba; no describirla como defensa completa ante un dispositivo hostil.

#### I5. Recuperación, pérdida de claves y degradación a QR estático

**Evidencia.** La credencial debe sobrevivir a cambio de móvil con exportación (`01-requisitos.md:174-175`, `:315-316`), mientras K3 puede perder la clave del titular y pasar a modo estático (`03-arquitectura-movil.md:710-712`).

**[A]** Si el verificador acepta silenciosamente el modo estático, una captura vuelve a ser utilizable, anulando la propiedad anti-captura que motivó K3.

**Acción — 01–03 y futuros 04/05.** Definir recuperación tras pérdida del móvil candidato y organizador, reprovisión, reemisión, copia del registro y revocación. Si hay presentación dinámica, decidir explícitamente si el modo estático se rechaza, se acepta con advertencia o exige revisión humana.

#### I6. Privacidad y accesibilidad requieren política operativa

**Evidencia.** 01 afirma que, sin backend, el responsable no almacena nada (`01-requisitos.md:266-268`), pero 03 propone registros y datos locales del organizador (`03-arquitectura-movil.md:359-365`). El QR contiene nombre en claro (`02-credencial-y-amenazas.md:390-394`).

**[A]** La ausencia de servidor no elimina el tratamiento de datos locales, exportaciones, mensajería o registros. Una foto no es automáticamente dato biométrico de categoría especial por el mero hecho de existir; depende del tratamiento y finalidad.

**Acción — 01–04.** Revisar con la persona responsable de protección de datos: responsable, finalidad, base jurídica, información al candidato, conservación y borrado, mensajería remota, menores, pérdida y derechos. Definir adaptación de tiempo, alternativa a cámara/QR, lectura por TalkBack y pruebas con personas. El texto alternativo Base45 ayuda al transporte, pero por sí solo no hace comprensible el contenido de un QR (`03-arquitectura-movil.md:772-776`).

#### I7. Distribución e instalación necesitan verificación independiente

**Evidencia.** 03 propone comprobación de versión por invitación y afirma efectos concretos del registro de desarrolladores (`03-arquitectura-movil.md:657-695`). Las fuentes y despliegues de 2026–2027 no se han vuelto a consultar en esta revisión.

**[A]** La firma de Android protege actualizaciones frente a una clave distinta, pero en la **primera instalación** no identifica por sí sola al editor legítimo. Un hash publicado junto a un APK en un canal comprometido tampoco es una segunda fuente independiente.

**Acción — futuros 04/05.** Revalidar normativa y fechas oficiales al preparar la distribución; documentar identidad del editor, autenticidad del primer APK, custodia y copia del keystore, compromiso, pérdida, rotación compatible y reinstalación. Explicar que un candidato offline no puede descubrir por sí solo una versión retirada.

### Menores

#### M1. Pruebas de ausencia de secretos demasiado concretas

La CI propuesta busca nombres como `bank.key*` o una clase `Grader` (`03-arquitectura-movil.md:246-250`, `:553-557`). **[A]** Es útil como regresión, no prueba exhaustiva: los datos podrían aparecer con otro nombre, transformados o en recursos. Complementar con separación de artefactos, revisión del grafo y análisis del APK final.

#### M2. Formato y extensiones de protocolo

`03-arquitectura-movil.md:699-703` propone ignorar campos desconocidos. **[A]** Eso puede ser peligroso si un campo desconocido cambia una condición de seguridad. Distinguir extensiones opcionales de críticas y rechazar las críticas no reconocidas; especificar algoritmos admitidos y validación estricta.

#### M3. Descripción imprecisa de la protección de claves

“No exportable” reduce extracción de material de clave bajo las garantías del dispositivo, pero no impide necesariamente **usar** una clave desde un dispositivo comprometido o desbloqueado (`02-credencial-y-amenazas.md:237-238`, `:419`). Evitar “sin consecuencias” para el APK de organizadores (`:422`): sin secretos incorporados su publicación no expone automáticamente el banco, pero puede facilitar análisis de superficie.

## 3. Red team: ataques concretos

Los siguientes resultados son **análisis condicionales**, no ataques ejecutados.

| Ataque | Viabilidad y explicación |
|---|---|
| Editar nombre o fecha dentro de un QR firmado K1 | **No viable bajo las premisas criptográficas**: cambiar bytes firmados invalida la firma, siempre que el verificador compruebe correctamente firma, clave de confianza y campos. |
| Editar texto visible del PDF sin cambiar el QR | **Viable como engaño visual**: el texto exterior no queda protegido por la firma del QR. El organizador debe leer y comparar lo que obtiene de su propio verificador, no confiar en el texto impreso (`01-requisitos.md:309-310`; `02-credencial-y-amenazas.md:170-174`). |
| Crear un QR “APTE” con clave inventada | **No viable** si el verificador rechaza emisores desconocidos y la lista de confianza es auténtica y actual. **Viable** si se sustituye el verificador o su raíz de confianza. |
| Extraer clave emisora común del APK | **Viable si está embebida**, incluso ofuscada. K1 aprovisionado aparte evita ese vector; no basta con hacer privado el repositorio (`02-credencial-y-amenazas.md:146-158`, `:409-420`). |
| Extraer respuestas del APK público | **Viable** en corrección local con respuestas incluidas; cifrado con clave incluida solo añade dificultad. En K1 **no por ese vector** si realmente no se empaquetan respuestas; siguen posibles la filtración desde organizadores, la inferencia por resultados y la colusión. |
| Extraer texto de preguntas | **Viable** si está en claro en APK. Con O4b, un candidato que recibe las claves puede recuperar y compartir las preguntas autorizadas; no debería poder descifrar las restantes sin otras claves, suponiendo implementación correcta. Las exposiciones se acumulan (`02-credencial-y-amenazas.md:503-512`, `:530`). |
| Usar un código de desbloqueo global | **Viable** descifrar todo el banco tras obtener ese código y el APK. Las claves por pregunta limitan el subconjunto, no evitan que se comparta lo recibido. |
| Enviar una hoja inventada sin haber seguido el examen | **Viable en K1 como creación de mensaje**, pues la hoja procede de dispositivo controlado por candidato. Si las respuestas son correctas y el corrector no puede distinguir su origen, puede emitirse una credencial. La validación de sesión de B2 reduce manipulaciones, pero no elimina ayuda externa. |
| Aprobar con respuestas de otra persona o con conocimientos previos | **Viable** incluso con firma y corrección correctas; la firma certifica el resultado procesado por el emisor, no la ausencia de ayuda. Supervisión presencial reduce el riesgo. |
| Reutilizar la misma hoja/nonce | **No viable** en el mismo organizador si el consumo se persiste y comprueba correctamente. **Potencialmente viable** tras pérdida o restauración de estado, o en otro organizador sin coordinación; depende de la política implementada. |
| Compartir una captura de credencial estática | **Viable**: la firma seguirá siendo válida. El cotejo de identidad mitiga la suplantación; no elimina coincidencias de nombre, fallo humano o documentos falsos. |
| Reenviar o retransmitir un desafío dinámico | Una captura anterior no responde a un **nonce nuevo** bien implementado; **relay en directo** y préstamo del móvil pueden seguir siendo viables. Exigir nonce único, sesión y caducidad local del desafío. |
| Enseñar una pantalla falsa “VÀLIDA” | **Viable** si el organizador confía en el móvil del candidato. Mitigación operativa: escanear en dispositivo o verificador de confianza propio (`02-credencial-y-amenazas.md:452-459`). |
| Robar un móvil de organizador | **Condicional**: Keystore puede dificultar la extracción de clave, pero un móvil desbloqueado o comprometido puede permitir usarla y acceder al banco. Revocar y recuperar son procesos indispensables. |
| Firmar una credencial con fecha anterior tras comprometer al emisor | **Viable** si se conserva validez usando solo `iat`; véase B3. |
| Entregar una lista antigua firmada | **Viable si no hay anti-rollback o política de frescura**, incluso sin poder falsificar firmas. Un verificador nunca actualizado no sabe por sí mismo que existe una lista posterior. |
| Instalar o distribuir APK falso | **Viable como phishing** si el usuario acepta una primera instalación de origen no auténtico; no equivale a actualizar una instalación legítima con firma distinta. La verificación de primera descarga requiere un canal de confianza. |

## 4. Proporcionalidad y opciones

El nivel es **pendiente**, no “medio” por defecto. Los nombres siguientes describen perfiles de riesgo para ayudar a comparar; no son decisiones del usuario.

| Opción | Nivel al que podría encajar | Ventajas | Costes y renuncias |
|---|---|---|---|
| **Clave y respuestas en APK; banco público** | Básico frente a errores o curiosidad casual | Autoservicio y operación simple | Contradice la confidencialidad declarada; un técnico puede extraer secretos y falsificar. Solo reconsiderable si el usuario cambia expresamente el requisito. |
| **B cifrado con clave entregada al candidato y corrección local** | Intermedio para secreto temporal en reposo, **débil para resultado** | No expone el banco antes de entregar claves | Las respuestas entregadas al dispositivo son extraíbles; una clave global expone todo; la corrección local puede manipularse. |
| **C/K1 recortado: organizador corrige y firma** | Medio para autenticidad y secreto de respuestas | Sin clave emisora ni respuestas en APK candidato; verificación offline | Textos públicos sin cifrado; organizador por emisión; no demuestra examen íntegro ni evita colusión. |
| **C/K1 + cifrado por pregunta** | Medio/alto para limitar descarga previa del texto | Mantiene respuestas fuera del candidato y limita texto entregado | Más claves y logística; el candidato puede compartir lo visto; selección, disponibilidad y QR deben probarse. |
| **C + atestación y desafío** | Alto, si se justifican adversarios técnicos y suplantación | Evidencia adicional del dispositivo y posesión | Compatibilidad, listas de confianza, operación y UX; no elimina relay, ayuda externa, fallos de sistema ni colusión. |
| **Dispositivo del club supervisado** | Alto **en control operativo**, sujeto a custodia y supervisión | Menos protocolo entre dispositivos; respuestas fuera del móvil del candidato | Presencia física y dispositivo administrado; fotos, mala supervisión, compromiso del dispositivo o colusión siguen siendo posibles. |
| **K5 autoservicio atestado** | Especializado, alta complejidad con garantías limitadas | Sin organizador por emisión | No conserva secreto de respuestas si corrige localmente; compatibilidad y verificación complejas. |

**Simplificaciones recomendadas, condicionadas al riesgo aceptado:**

1. Para pocos emisores, comparar una **lista firmada de claves públicas de organizadores** con certificados jerárquicos. Mantener un procedimiento seguro de actualización y revocación en ambas opciones (`02-credencial-y-amenazas.md:612-615`).
2. Empezar sin atestación ni desafío dinámico **solo si** el usuario acepta sus riesgos; no construirlos preventivamente como requisito universal.
3. Considerar APK con banco actualizado por release antes de un sistema de paquetes descargables, si la cadencia y los casos offline lo permiten (`03-arquitectura-movil.md:573-580`).
4. Preferir una biblioteca mantenida y evaluada para CBOR/COSE y conversiones de firma frente a un subconjunto criptográfico propio (`03-arquitectura-movil.md:402-404`). **[A]** Menos dependencias no equivale automáticamente a menor riesgo.
5. Evitar cadena de hashes de eventos como prueba de honestidad del candidato: su propio documento ya reconoce que no resiste control del dispositivo (`03-arquitectura-movil.md:359`). Conservar solo los eventos necesarios para la política aceptada y privacidad.
6. No asignar un banco “grande” como garantía por número arbitrario de preguntas: 300+ es una estimación editorial de los borradores, no un umbral de seguridad demostrado.

## 5. Validaciones pendientes antes del cierre

1. **Contrato y pruebas adversariales:** hojas con preguntas repetidas, desconocidas o de otra versión; nonce reutilizado; duplicación durante caída; firmas y emisores desconocidos; mensajes truncados; campos críticos nuevos.
2. **Tiempo y recuperación:** cambio de hora, reinicio largo y corto, muerte de proceso, actualización con sesión activa, pérdida de organizador y restauración de datos antiguos.
3. **Credencial y confianza:** fechas retroactivas tras compromiso, lista antigua, reloj incorrecto, raíz retirada, pérdida de móvil del titular y decisión explícita sobre presentación estática.
4. **Banco:** conciliación del seed con la base vigente, historial y copias existentes; contenido y respuestas no deben aparecer en artefactos públicos si la opción elegida los protege.
5. **Interoperabilidad:** tamaño y lectura reales de invitación, hoja y credencial; QR desde pantalla, papel e imagen; accesibilidad y flujos sin cámara. Los tamaños de 02/03 son estimaciones pendientes, no resultados medidos.
6. **Distribución:** confirmar documentación oficial Android vigente, autenticidad de primera instalación y recuperación del keystore cuando exista `05-distribucion.md`.
7. **Revisión completa:** incorporar 04, 05 y ADRs, corregir bloqueantes y repetir revisión. Al cerrar el diseño, recordar al usuario revisar qué agentes harán falta para implementación (`docs/design/README.md:31-33`).

## Preguntas abiertas para el usuario

1. ¿Qué adversarios y consecuencias deben cubrirse: curiosidad casual, candidato técnico, suplantación, ayuda externa o colusión con organizador? ¿Qué nivel y riesgos residuales acepta?
2. ¿La confidencialidad exige impedir la descarga de todo el texto antes del examen, además de mantener las respuestas fuera del candidato? ¿Se acepta la difusión acumulativa de las preguntas mostradas?
3. ¿Puede intervenir un organizador en cada sesión, presencialmente o en remoto? Si son presenciales, ¿es aceptable un dispositivo supervisado del club?
4. ¿Debe limitarse el número de intentos por persona entre varios organizadores? ¿Se acepta un registro operativo local o intercambio manual, con sus límites y tratamiento de datos?
5. ¿Cómo deben tratarse reinicios, entregas tardías, abandono, actualización incompatible y adaptaciones de tiempo sin perjudicar injustamente al candidato?
6. Tras compromiso de un emisor, ¿se prefiere revocar todas sus credenciales y reemitir, o mantener excepciones mediante un registro independiente y revisión humana?
7. ¿Qué debe ocurrir si la lista de revocación está desactualizada o el reloj no es fiable: bloquear, advertir o derivar a revisión?
8. ¿Basta cotejar la identidad humana, o interesa prueba dinámica de posesión? Si se pierde el móvil, ¿puede aceptarse una credencial estática o debe reemitirse?
9. ¿Qué información, conservación y recuperación necesita el club para cumplir sus obligaciones de privacidad, incluidos menores y comunicaciones remotas?
10. ¿Quién custodiará las claves, verificará la primera distribución y mantendrá el banco? ¿Cuántos organizadores y dispositivos habrá?