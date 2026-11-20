# Revisión final integral del diseño

**Alcance:** `docs/design/01–05`, los ADR 0001 y 0002, el plan y las dos revisiones anteriores.  
**Método:** revisión documental y análisis adversarial; no hay implementación ni se han ejecutado ataques, compilaciones o pruebas en dispositivos.  
**Ruta:** `docs/design/reviews/revision-final-diseno.md`.

## Resumen y veredicto

**No recomiendo cerrar todavía el diseño como especificación lista para implementar.** Las decisiones aprobadas son coherentes entre sí y no hace falta reabrirlas, pero siguen presentes tres bloqueantes:

1. Una pregunta real y su respuesta aparecen en `03`; hay que retirarlas antes de publicar el repositorio.
2. `01–03` mantienen recomendaciones y preguntas que contradicen los ADR aprobados sobre la PWA, la firma y la ofuscación del banco.
3. UX permite continuar cuando falla el guardado, mientras arquitectura exige persistir antes de avanzar; esto puede perder respuestas y alterar el resultado.

La PWA está descrita en `04` y `05`, pero su funcionamiento real offline y su interoperabilidad con Android siguen **pendientes de prueba**. Las limitaciones de la firma y del banco extraíble están aceptadas, no son defectos que deban «corregirse» volviendo al modelo supervisado.

**Recordatorio para el cierre:** antes de implementar, revisar qué agentes harán falta para esa fase (`docs/design/README.md:39–51`).

## 1. Criterio y decisiones vigentes

- **[H] Hecho documental:** lo que figura en los archivos citados.
- **[A] Análisis:** consecuencia o riesgo deducido de esos documentos; no es una vulnerabilidad demostrada en una aplicación.
- **[PV] Pendiente de verificar:** depende de código, configuración, publicación o dispositivos aún inexistentes o no examinados.

Los ADR prevalecen sobre propuestas anteriores:

- **ADR 0001:** PDF/imagen con QR firmado por una clave privada incorporada al APK; verificador **PWA pública, instalable y offline** tras cargar sus recursos, sin secretos; ninguna app Android de organizadores en el MVP (`docs/adr/0001-credencial-sellada-y-verificador-publico.md:24–42`).
- **ADR 0002:** banco real fuera del repo público, inyectado en el build e incluido en el APK público; sin ofuscación inicial (`docs/adr/0002-banco-privado-en-build-y-apk.md:24–37`).
- **Autoservicio:** corrección local, feedback por fallo y ninguna intervención de organizador durante el examen (`AGENTS.md:15–25`).
- **Perfil proporcional:** no se exige resistencia frente a extracción, modificación del dispositivo o suplantación; revocación en fase 2 (`AGENTS.md:19–28`; `docs/adr/0001-credencial-sellada-y-verificador-publico.md:54–63`).

**Alcance de la palabra “verificación”:** la PWA forma parte del diseño inicial aprobado; que un organizador la use para una credencial concreta es posterior y no condiciona la emisión.

## 2. Comprobación integral de coherencia

| Cadena de requisitos | Resultado |
|---|---|
| **Examen → resultado → feedback** | El flujo local muestra fallos, respuesta marcada y correcta, con explicación opcional (`docs/design/01-requisitos.md:172–184`; `docs/design/03-arquitectura-movil.md:278–294`; `docs/design/04-ux.md:480–518`). Persistencia y microcopy necesitan corrección: B3 e I2. |
| **Aprobado → credencial** | Arquitectura emite tras corregir y aprobar, y contempla recuperación idempotente (`docs/design/03-arquitectura-movil.md:317–322`). La confirmación del nombre y las validaciones del banco no están completamente alineadas: I2. |
| **Credencial → PWA** | `04` y `05` describen la PWA aprobada, con clave pública, entradas por cámara/imagen/texto y cuatro resultados (`docs/design/04-ux.md:715–740`; `docs/design/05-distribucion.md:660–679`). `01–03` siguen recomendando o preguntando por otra solución: B2. |
| **Banco privado → build → APK público** | El principio está recogido: el banco fuente se protege, pero el APK lo contiene y permite extraerlo (`docs/adr/0002-banco-privado-en-build-y-apk.md:24–35, 48–50`; `docs/design/05-distribucion.md:245–255`). Un ejemplo real incumple la regla documental: B1. |
| **Firma del sello → firma del APK** | `05` distingue correctamente ambas claves (`docs/design/05-distribucion.md:451–464`). El sello es extraíble; el keystore Android no debe incluirse en el APK. La interoperabilidad y la correspondencia de claves necesitan pruebas: I3. |
| **Distribución → primera instalación** | Se prevén Releases, SHA-256, huella y custodia (`docs/design/05-distribucion.md:416–429, 467–485`). La primera instalación no queda autenticada si el canal de descarga y su hash se sustituyen juntos: I5. |

### Modelo supervisado anterior

**[H]** Los flujos vigentes de examen y emisión son autónomos. `03` identifica expresamente como eliminados la invitación, la corrección externa, la atestación y el cifrado por pregunta (`docs/design/03-arquitectura-movil.md:48–58`). La revisión intermedia sí proponía un organizador que corrigiera y firmara, pero era una evaluación del modelo anterior (`docs/design/reviews/revision-intermedia-01-03.md:1–14`).

**[A]** Esas propuestas históricas no deben recuperarse como requisitos. El problema actual no es que `01–03` exijan supervisión del examen, sino que sus recomendaciones vigentes sobre **verificador nativo y alternativas de sello** no se han actualizado tras los ADR.

## 3. Hallazgos bloqueantes

### B1. Una pregunta real aparece en la documentación destinada al repositorio público

**Evidencia.** El ejemplo de `docs/design/03-arquitectura-movil.md:417–427` reproduce una pregunta y su respuesta presentes en el proyecto base, un generador histórico del banco. La revisión anterior y `05` ya señalaron el hecho (`docs/design/reviews/revision-completa-01-03-seguridad-proporcional.md:24–32`; `docs/design/05-distribucion.md:83–91`). ADR 0002 excluye el banco real de la documentación pública (`docs/adr/0002-banco-privado-en-build-y-apk.md:26–33`).

**Impacto.** [A] Hay una infracción puntual verificable; no se ha demostrado una nueva publicación del banco completo ni se ha auditado el historial Git de este proyecto. Que el contenido del proyecto base ya fuera público no autoriza a reproducirlo en este repositorio.

**Acción — `03`, `05`, publicación.**

1. Sustituir el ejemplo por uno inequívocamente ficticio.
2. Revisar los demás ejemplos, árbol **e historial que se pretenda publicar**, Pages, logs y artifacts independientes.
3. Mantener el APK final como excepción explícita: contiene el banco por decisión aprobada.

### B2. Los documentos anteriores a los ADR siguen presentando decisiones cerradas como abiertas

**Evidencia.**

- `01` dice que aún no se ha decidido la UI de verificación y plantea una segunda app (`docs/design/01-requisitos.md:31–33, 187–198`).
- `02` recomienda una app de organizadores y checksum como alternativa, pide publicar dos APK y vuelve a preguntar por ambos asuntos (`docs/design/02-credencial-y-amenazas.md:35–40, 403–413, 512–522, 548–560`).
- `03` recomienda `app-verifier` y contempla checksum en su arquitectura (`docs/design/03-arquitectura-movil.md:25–36, 219–236, 584–599`).
- El ADR fija firma asimétrica y PWA, y descarta la app Android de organizadores para el MVP (`docs/adr/0001-credencial-sellada-y-verificador-publico.md:24–42`).
- `02` y `03` siguen recomendando codificación/cifrado ligero del banco (`docs/design/02-credencial-y-amenazas.md:308`; `docs/design/03-arquitectura-movil.md:488–492`), mientras ADR 0002 dice que no se cifra/ofusca inicialmente (`docs/adr/0002-banco-privado-en-build-y-apk.md:35–37`).

**Impacto.** [A] Una persona que implemente por separado requisitos, seguridad o arquitectura puede construir la solución equivocada, añadir un segundo APK o omitir el verificador aprobado.

**Acción — `01`, `02`, `03`, `05`, `README`.** Marcar las comparativas como históricas, sustituir sus recomendaciones vigentes por referencias a los ADR y eliminar preguntas ya resueltas. La PWA pertenece al MVP; usarla en una comprobación individual sigue siendo optativo. En `05`, dejar la app nativa únicamente como eventual fase posterior, no como decisión pendiente del MVP (`docs/design/05-distribucion.md:694–713, 873–877`). Actualizar también los estados aún “pendiente” del plan (`docs/design/README.md:29–36`).

**Precisión del ADR:** su operación dice conservar claves “solo para generar el sello durante el build” (`docs/adr/0001-credencial-sellada-y-verificador-publico.md:67–70`). Conviene aclarar que **el material se incorpora en el build y el sello de cada credencial se genera durante la ejecución**; no cambia la decisión.

### B3. UX permite avanzar sin guardar una respuesta que arquitectura exige persistir

**Evidencia.** `03` especifica transacción antes de avanzar y persistencia de la respuesta (`docs/design/03-arquitectura-movil.md:315–318, 350–353`). `04` propone que, tras fallar dos veces el guardado, se muestre un aviso no modal y el candidato continúe (`docs/design/04-ux.md:335–339`).

**Impacto.** [A] Si se pasa a la siguiente pregunta sin persistir, una muerte de proceso o un fallo de almacenamiento puede perder una respuesta o generar un resultado distinto del presentado. No requiere adversario técnico.

**Acción — `03`, `04`.** No confirmar el avance ni finalizar con una respuesta no guardada. Mantenerla visible, ofrecer reintento y explicar el problema en catalán; si no se puede recuperar, permitir abandonar o reintentar sin emitir una credencial basada en datos incompletos. Probar también el fallo al guardar el resultado y al emitir.

## 4. Hallazgos importantes

### I1. Recuperación del reloj tras reinicio y límites de reglas

`01` propone estimar el tiempo con reloj de pared y un tope (`docs/design/01-requisitos.md:253–257`); `03` propone conservar el último tiempo restante sin contar el apagado (`docs/design/03-arquitectura-movil.md:659–675`). La segunda política es proporcional, pero no especifica cómo reanclar atómicamente el inicio monotónico en el nuevo arranque. La revisión previa ya lo señaló (`docs/design/reviews/revision-completa-01-03-seguridad-proporcional.md:57–61`).

**Acción — `01`, `03`, `04`.** Documentar una sola política indulgente: inicializar y persistir el tiempo restante, reanclarlo tras reinicio y definir qué hacer si `BOOT_COUNT` no es fiable. Probar reinicios antes de responder, repetidos y durante la recuperación. No añadir sanción ni pretender un reloj fiable frente al candidato.

Además, la validación enumerada en `03` no explicita `N > 0`, tiempo positivo ni `1 ≤ umbral ≤ N` (`docs/design/03-arquitectura-movil.md:454–459`). **Acción:** añadir esos límites, IDs de opciones únicos y tests que impidan emitir en suspenso, abandono o error de persistencia.

### I2. Confirmación del nombre y feedback educativo

`04` propone confirmar o cambiar el nombre **antes** de emitir (`docs/design/04-ux.md:268–270, 447–449`), pero `03` emite inmediatamente al corregir un aprobado (`docs/design/03-arquitectura-movil.md:321–322`). Los textos de bienvenida prometen además ver “por qué” se ha fallado (`docs/design/04-ux.md:224, 1205`), aunque las explicaciones son opcionales (`docs/design/04-ux.md:507–518`).

**Acción — `03`, `04`.** Confirmar el nombre antes de iniciar el examen o definir una transición explícita antes del sellado, sin intervención externa. Cambiar el texto educativo para prometer siempre la respuesta correcta y ofrecer el motivo **cuando esté disponible**.

### I3. Contrato interoperable de firma y confianza

`03` propone ECDSA P-256, firma sobre bytes exactos y conversión DER a `r‖s` (`docs/design/03-arquitectura-movil.md:548–557, 602–606`). `05` prevé WebCrypto y vectores compartidos (`docs/design/05-distribucion.md:665–670`), pero son propuestas, no resultados medidos.

**Acción — `02`, `03`, `05`.** Fijar antes de implementar un contrato único: bytes firmados, algoritmo permitido, representación de firma, UTF-8, límites de entrada, tratamiento de JSON malformado o campos duplicados, extensiones críticas y correspondencia `kid → clave pública`. Probar **Android firma → PWA verifica** con vectores válidos e inválidos y comprobar en release que la privada inyectada corresponde a la pública publicada. No confiar solo en una huella calculada desde datos que una misma página pueda sustituir.

La inspección de `05` prohíbe archivos `.pk8` en el APK (`docs/design/05-distribucion.md:383–391`); eso es compatible con el ADR solo si se define cómo se incorpora el material de firma en otra representación. **No debe interpretarse como ausencia de la clave de sello:** sigue siendo recuperable del APK.

### I4. PWA offline, versiones antiguas y caducidad

`04` y `05` prevén precache del sitio, lector QR y claves (`docs/design/04-ux.md:725–737, 1053–1067`; `docs/design/05-distribucion.md:665–678`). No existe aún evidencia de instalación, primera carga completa, cámara o funcionamiento en modo avión. Una caché puede ser borrada y una PWA sin actualizar puede desconocer una clave nueva.

`03` limita la compatibilidad a formato actual y anterior (`docs/design/03-arquitectura-movil.md:627–640`), sin una política que garantice que una credencial anterior aún aceptada siga verificándose. `04` propone mostrar `VÀLIDA` para una credencial caducada junto a un aviso (`docs/design/04-ux.md:898–901`); la política de caducidad no está cerrada.

**Acción — `03`, `04`, `05`.**

- No presentar “instalada” como prueba suficiente de que **todos** los recursos están cacheados. Comprobar la preparación y probar inicio real en modo avión tras cerrar y reabrir.
- Publicar la clave pública nueva antes del APK que la usa; explicar que los verificadores offline antiguos deben actualizarse.
- Mantener la compatibilidad con los formatos y claves cuya aceptación siga vigente; un verificador antiguo no puede conocer automáticamente un formato futuro.
- Mientras no haya política de caducidad, no prometerla. Si se incorpora, separar claramente **firma correcta** de **vigencia**, y recordar que la fecha del candidato es manipulable y la del verificador también depende de su reloj.

### I5. Distribución: autenticidad, metadatos y prereleases

**[H]** El diseño publica hash y huella junto a la descarga (`docs/design/05-distribucion.md:597–607`), y reconoce que no protegen una primera instalación si también se compromete la página (`docs/design/05-distribucion.md:542–546`).

**[A]** Una actualización con otra clave es rechazada por Android, pero esto no autentica por sí solo la **primera** instalación. Además, `releases/latest/download` puede cambiar al publicarse una release antes de que Pages actualice el hash estampado (`docs/design/05-distribucion.md:379–381, 395–398, 613–618`): durante ese intervalo, el enlace y el metadato pueden referirse a versiones distintas.

**Acción — `04`, `05`.** Comunicar la dirección y huella también por un canal conocido del club; no prometer autenticidad absoluta. Para evitar discrepancias, recomendar que la página publicada enlace a un asset **de versión concreta** asociado al hash mostrado, y solo actualice ambos juntos. Mantener `latest` si se documenta y se prueba su sincronización.

El runbook propone publicar una prerelease con banco real para pruebas en dispositivos de test (`docs/design/05-distribucion.md:824–828`). [A] En un repositorio público, una prerelease publicada no debe tratarse como canal restringido aunque no sea `latest`; además, el borrador del flujo solo describe tags finales (`docs/design/05-distribucion.md:338–345, 368–370`).

**Acción:** distinguir explícitamente prueba privada/local y prerelease pública; si el RC contiene banco real o clave de sello de producción, asumir desde su publicación que cualquiera puede obtenerlos. Definir y probar el flujo de tags RC antes de utilizarlo.

### I6. Error en ejemplos de `versionCode`

La fórmula de `05` es `X·100000 + Y·1000 + Z·10 + s`, pero sus ejemplos asignan `130009` y `130002` a `1.3.0` y `1.3.0-rc.2` (`docs/design/05-distribucion.md:723–731`). **[A]** Con esa fórmula los valores son, respectivamente, **103009** y **103002**. `latest.json` también muestra `130009` para `1.3.0` (`docs/design/05-distribucion.md:630–634`).

**Acción — `05`.** Corregir ejemplos y metadatos o modificar deliberadamente la fórmula; probar monotonía, límites de componentes y transición RC → final antes de publicar. Un `versionCode` incorrecto puede impedir una actualización normal.

### I7. Versión del banco y resultado no identifican lo mismo

`01` describe `exam_version` como versión de banco/reglas (`docs/design/01-requisitos.md:115, 289–295`). `05` distingue `bank_version` de `exam_version`, y solo incrementa esta última cuando cambian reglas o significado (`docs/design/05-distribucion.md:740–752`). El payload propuesto incluye `exv`, pero no necesariamente `bank_version` (`docs/design/03-arquitectura-movil.md:541–545`).

**Impacto.** [A] Dos APK con preguntas o respuestas distintas pueden emitir la misma `exv`; el verificador no puede deducir qué banco concreto se usó.

**Acción — `01`, `03`, `04`, `05`.** Definir el significado de ambos identificadores. Si se necesita identificar el contenido, incluir una `bank_version` pública y no sensible en la credencial o mantener explícitamente esa trazabilidad solo local. No publicar hashes de respuestas como sustituto sin necesidad.

### I8. Privacidad, accesibilidad y recuperación

`04` afirma que los datos “només són al teu mòbil” y “no s'envien enlloc” (`docs/design/04-ux.md:539–540, 1330–1335`), pero permite compartir el PDF y contempla una consulta opcional de actualización; `03` reconoce que esta expone la IP a GitHub (`docs/design/03-arquitectura-movil.md:718–722`). El backup está desactivado y la recuperación depende de una copia exportada o de repetir el examen (`docs/design/03-arquitectura-movil.md:344–349`; `docs/design/01-requisitos.md:183–184`).

**Acción — `01`, `04`, `05`.**

- Informar en catalán de datos locales, QR legible, copias exportadas, destinatarios elegidos por el usuario y posible consulta de versión. Definir responsable, finalidad, conservación, borrado y tratamiento de menores con asesoramiento apropiado; no declarar cumplimiento RGPD automático.
- Explicar cómo guardar la credencial, qué se pierde al desinstalar o perder el móvil y que repetir emite otra.
- Probar TalkBack, escala de fuente, contraste, teclado, temporizador, feedback, exportación y PWA con personas y dispositivos reales (`docs/design/04-ux.md:1459–1483`). Registrar una adaptación de tiempo como cuestión de producto, sin organizador obligatorio.

### I9. Los controles de CI no demuestran por sí solos ausencia de fugas

`05` propone canary, escáner, exclusión de cachés y artifacts por rutas concretas (`docs/design/05-distribucion.md:282–292, 401–413`). Si el canary se incluye en el banco empaquetado, [A] también puede extraerse del APK público; sigue sirviendo como **marcador de coincidencia**, no como secreto ni prueba exhaustiva de ausencia de fugas. No detecta una pregunta copiada sin el marcador.

**Acción — `05`.** Mantener separación de fuente, revisión de árbol e historial, logs, cachés y artifacts; usar datos ficticios en CI pública de contribuciones. Tratar el canary como control complementario. El APK público contiene banco y clave de sello por diseño; keystore Android y tokens de acceso no deben estar en él.

### I10. Instrucciones de instalación demasiado genéricas ante alertas de seguridad

`04` indica pulsar “Instal·la igualment” si aparece un aviso de Play Protect (`docs/design/04-ux.md:1125–1139, 1410–1413`).

**Acción — `04`, `05`.** Distinguir el permiso normal para instalar desde una fuente externa de una alerta concreta de seguridad: recomendar comprobar origen y, ante una advertencia inesperada o de amenaza, detenerse y consultar al club. Revalidar las pantallas y reglas Android vigentes en el momento de publicar; las fechas y límites citados en `03` y `05` permanecen **[PV]**, no son pruebas de instalación.

## 5. Hallazgos menores

| ID | Hallazgo y acción sugerida | Documentos |
|---|---|---|
| M1 | La web de descarga no enlaza a la PWA según UX, pero distribución sí prevé enlace y página de privacidad. Unificar el mapa de enlaces y la advertencia sin ampliar la web innecesariamente. | `docs/design/04-ux.md:1080–1087`; `docs/design/05-distribucion.md:597–606` |
| M2 | La PWA propone mostrar el host como ayuda, pero un clon puede imitarlo. Conservar la advertencia explícita y no vender el icono o el texto de host como autenticación. | `docs/design/04-ux.md:980–1000` |
| M3 | Guardar mediante selector no garantiza que el destino sea “Descàrregues”. Mostrar “Desat” o el destino realmente elegido. | `docs/design/04-ux.md:634–640` |
| M4 | `02` afirma que Key Attestation prueba app íntegra en dispositivo no rooteado. Es una simplificación excesiva y no es requisito del MVP; corregir o retirar la frase. | `docs/design/02-credencial-y-amenazas.md:235–241` |
| M5 | `01` todavía pregunta si el repo base es público; `02` documenta su publicación previa. Actualizar el hecho sin reproducir preguntas. | `docs/design/01-requisitos.md:43–47`; `docs/design/02-credencial-y-amenazas.md:280–297` |
| M6 | La recomendación de SBOM, atestaciones, segundo APK futuro y varias capas de auditoría se mezcla con el mínimo de release. Mantenerlas opcionales salvo justificación operativa. | `docs/design/05-distribucion.md:23–27, 431–440` |

## 6. Red team de la credencial

**Resultado = análisis documental, no ataque ejecutado.** Para detectar una discrepancia se presupone que el organizador usa **su propia PWA oficial** y compara sus datos con el documento y la persona.

| Ataque concreto | Viabilidad y razón |
|---|---|
| Editar nombre, resultado o fecha visibles en el PDF sin cambiar el QR | **Viable.** El sello protege el contenido del QR, no todo el aspecto del PDF. Es detectable solo si se escanea y compara; sin verificación puede pasar inadvertido. |
| Sustituir el QR por el de otra persona y mantener otro nombre visible | **Viable como edición; detectable con comparación.** Mostrar la credencial completa ajena o prestar el móvil sigue siendo un riesgo social aceptado. |
| Cambiar el payload del QR conservando la firma | **No viable bajo las premisas criptográficas y con implementación correcta:** la verificación de los bytes firmados falla. Esto no cubre una PWA falsa ni una clave comprometida. |
| Generar un QR nuevo con una clave inventada | **No viable ante la PWA oficial** si rechaza `kid` y claves desconocidos. Puede engañar a quien use un verificador falso o no verifique. |
| Extraer o invocar la clave de sello incluida en el APK y firmar “APTE” sin aprobar | **Viable para un técnico y aceptado.** La clave o su capacidad de firma está en el dispositivo del candidato. Las credenciales generadas pueden ser indistinguibles de las legítimas; un generador compartido puede hacer la forja accesible a terceros. |
| Modificar la app para que apruebe o llame directamente al firmador | **Viable para un técnico y aceptado.** Una firma válida no demuestra que se ejecutó correctamente el examen. |
| Extraer preguntas y respuestas del APK | **Viable y aceptado.** El APK público contiene el banco; si se almacena como archivo legible, puede extraerse sin decompilar. El feedback también revela respuestas de preguntas falladas. |
| Repetir el examen, borrar datos o pedir a otra persona que lo responda | **Viable y aceptado.** No hay límite fiable de intentos ni vínculo fuerte entre identidad y examinando. |
| Manipular la fecha de emisión o una caducidad derivada | **Viable.** La fecha procede del dispositivo del candidato. Comparar una expiración con el reloj del verificador no demuestra que la fecha inicial sea verdadera. |
| Enseñar una pantalla falsa “VÀLIDA” o una PWA clonada | **Viable** si se confía en el móvil o enlace del candidato. La mitigación es usar la PWA oficial instalada en el dispositivo del organizador; el dominio y el icono no son prueba infalible. |
| Distribuir un APK falso para la primera instalación | **Viable mediante suplantación del canal.** La firma Android protege la actualización de una instalación existente frente a otra firma, no identifica por sí sola el primer APK. |
| Usar un formato o `kid` nuevo en una PWA offline antigua | **Viable como fallo operativo, no como forja:** debe rechazarse o declararse desconocido, nunca aceptarse por defecto. Requiere actualización del verificador. |
| Distinguir credenciales legítimas de forjadas tras extraer la clave usando `iat` | **No viable de forma fiable:** quien dispone de la clave puede firmar una fecha anterior. Rotar la clave no separa retroactivamente ambos grupos. |

Fuentes principales de las limitaciones aceptadas: `docs/adr/0001-credencial-sellada-y-verificador-publico.md:54–63` y `docs/design/02-credencial-y-amenazas.md:175–205`.

## 7. Proporcionalidad y simplificaciones

El nivel de seguridad **sí está fijado**: errores y falsificación visual casual, no resistencia a un candidato técnico (`AGENTS.md:19–25`). Por tanto:

1. **Mantener la firma embebida aprobada**, con su advertencia y PWA; no sustituirla por checksum ni presentarla como prueba fuerte de aprobación.
2. **No implementar cifrado/ofuscación del banco inicialmente**, conforme al ADR 0002. Su clave o lógica estaría en el APK y añadiría mantenimiento sin proteger contra extracción técnica.
3. **No construir app Android de organizadores, atestación, TOTP, proctoring, cifrado por pregunta ni registro de intentos** para este MVP.
4. Priorizar tests de corrección, persistencia, interoperabilidad Android–PWA, QR real, instalación, accesibilidad y release. Aplazar SBOM, atestaciones de procedencia, políticas de confianza extensibles y campos sin uso real si no resuelven una necesidad concreta.
5. Conservar solo los controles de distribución proporcionados: separación de secretos y banco fuente, revisión de publicación, firma Android, copia restaurable del keystore, canal oficial y pruebas de actualización.

## 8. Estado de las revisiones anteriores

| Hallazgo previo | Estado en esta revisión |
|---|---|
| **Intermedia B1/B2/B5:** confidencialidad fuerte, protocolo supervisado y nivel de seguridad sin decidir | **Superados por las decisiones posteriores**, no por una implementación probada. No recuperar organizador corrector ni confidencialidad del APK como requisitos. |
| **Intermedia B3 / revisión completa I5:** fecha de emisión y compromiso de clave | **Limitación aceptada y bien documentada**, pero sigue siendo riesgo residual; no usar `iat` para distinguir forjas. |
| **Intermedia B4:** hash del banco y separación de entornos | **Tratamiento conceptual simplificado** mediante banco único y build privado; pendiente de comprobar controles reales de publicación. |
| **Revisión completa B1:** pregunta real en `03` | **Sin resolver: B1 actual.** |
| **Revisión completa B2:** detección sin herramienta disponible | **Resuelto como decisión por ADR 0001**, pero **sin alinear** `01–03`: B2 actual. La PWA aún no está probada. |
| **Revisión completa I2/I4:** reloj y límites de validación | **Pendientes: I1 actual.** |
| **Revisión completa I6/I7/I8:** pérdida, privacidad, accesibilidad y fugas | **Parcialmente detallados en `04–05`**, pero requieren acciones y pruebas: I8–I9 actuales. |
| **Revisión completa M2/M5:** ofuscación y descripción de attestation | **Pendientes de limpieza documental:** B2 y M4 actuales. |

## 9. Validación mínima antes de publicar

1. **Antes del primer push público:** corregir B1 y revisar árbol e historial previsto; no incluir banco real, claves privadas, keystore ni tokens fuera del APK permitido.
2. **Antes de implementar:** corregir B2–B3 y fijar contratos de emisión, persistencia, reloj y firma.
3. **Antes de publicar APK y PWA:** pruebas de aprobado/suspenso/abandono, respuestas múltiples y sin responder, fallos de guardado, reinicios, actualización con sesión activa y recuperación tras pérdida.
4. **PWA:** vectores Android → WebCrypto; firma alterada, `kid` desconocido, versión futura y formato incorrecto; instalación, primera carga completa, modo avión, cámara, imagen y texto en Android Chrome/Edge. Ensayar también QR impreso y pantalla con nombres largos y acentos.
5. **Distribución:** restaurar copia del keystore y verificar su huella; comprobar descarga, hash, versión y actualización desde una instalación anterior. Ensayar prerelease y sincronización de Pages/Releases.
6. **Accesibilidad y privacidad:** pruebas reales con usuarios, revisión del aviso de tratamiento y comprobación de que ningún contenido de credencial se envía desde la PWA.

## Preguntas abiertas para el usuario

*Dudas residuales registradas; no se solicita respuesta en esta revisión y no reabren los ADR aprobados:*

- Política de caducidad y aceptación de versiones antiguas; significado visible de `VÀLIDA` si se introduce caducidad.
- Vía inicial de build local o CI privada, custodios y procedimiento de recuperación del keystore.
- Dominio oficial, contacto del club y canal independiente para comunicar la huella del APK.
- Adaptaciones de tiempo, navegación e historial; alcance del repaso de preguntas acertadas.
- Necesidad de identificar `bank_version` en la credencial y tratamiento de menores o copias compartidas.
- Posibilidad de organizar pruebas de accesibilidad y de la PWA con miembros del club.