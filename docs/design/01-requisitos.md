# 01 — Requisitos de Arbitratge Mobile

> Autor: `product-analyst` · Revisión 3 (alineada con ADR 0001 y 0002 y con la revisión final) ·
> Estado: vigente para el resto de documentos de diseño. Fase de diseño: no hay código.
> Convención: **[H]** = hecho verificado (con fuente), **[D]** = decisión del usuario/ADR,
> **[S]** = suposición/recomendación mía (no cerrada), **[PV]** = pendiente de verificar con
> prototipo o pruebas.
> Los ADR `docs/adr/0001-credencial-sellada-y-verificador-publico.md` y
> `docs/adr/0002-banco-privado-en-build-y-apk.md` están aprobados y prevalecen sobre cualquier
> texto de este documento que los contradiga.

---

## 0. Resumen ejecutivo

1. **Qué es**: una app Android de **autoservicio y autoevaluación**. El candidato hace el
   examen solo, la app corrige, le muestra **cuántos fallos tuvo, el detalle de cada pregunta
   fallada** y, cuando exista el contenido editorial, **una explicación breve**. Si aprueba,
   la app emite una credencial. **No interviene ningún organizador durante el examen** [D].
2. **Objetivo pedagógico** (nuevo, central): el examen sirve para **aprender el reglamento**,
   no solo para certificar. Por eso mostrar las respuestas correctas tras el examen es una
   función, no una fuga. Esto es incompatible con "banco secreto" y es coherente con el nivel
   de seguridad elegido (§1.7).
3. **Seguridad rebajada de forma consciente** [D]: el adversario *no* es quien decompila o
   modifica el APK. Se acepta que banco, respuestas y lógica de corrección estén en el APK, y
   que la credencial **no resista a un candidato técnico**. Sí debe resistir **errores,
   edición visual casual y falsificación no técnica** (§4.2).
4. **Límite que sí se mantiene** [D]: el banco **no** se sube al historial de un repo público
   ni se sirve como fichero legible en una página HTTP. Se inyecta en la compilación desde
   repo/secret/artifact privado o desde local fuera de git (§1.7).
5. Las garantías que daba el backend (secreto de respuestas, tiempo en servidor, límite de
   intentos, fecha fiable, resultado no manipulable) **se renuncian explícitamente** o se
   dejan en "mejor esfuerzo" (§1.6, §3). Cada una queda documentada como riesgo residual
   aceptado (§1.8).
6. **Verificación por organizadores = optativa** [D, ADR 0001]: no es parte del examen ni
   obliga a personalizar la emisión. El verificador es una **PWA pública, instalable y offline**
   (tras cargar sus recursos), sin secretos y con solo la clave pública; **no hay app Android
   de organizadores en el MVP**. Que un organizador la use para una credencial concreta es
   optativo. Revocación: **fase 2**, sin backend por ella.

Criterio rector [D]: **simple, mantenible, útil para aprender, adecuado a miembros mayores y a
un contexto de confianza social.** No se añade seguridad que impida la autocorrección o exija
organizador.

---

## 1. Inventario del proyecto base

Ruta: `C:\Users\lluis\Visual Studio Code Projects\arbitratge`. Repo declarado:
`github.com/cfbbarcelona/arbitratge` (producción: arbitratge.futbolbotons.cat).
**[H] El repo base es público** (documentado en `02-credencial-y-amenazas.md`; ver también
`AGENTS.md` de este repo). Consecuencia: el banco actual del base ya fue publicado allí, pero
eso **no autoriza** a reproducir preguntas ni respuestas reales en este repo ni en su
documentación; la regla "banco fuera de repo público" (§1.7, ADR 0002) rige para este
proyecto.

### 1.1 Qué hace [H]

Plataforma de exámenes en línea para candidatos a árbitro del CFBB. Frontend HTML/CSS/JS
vanilla en GitHub Pages; backend Supabase (Postgres + Edge Functions Deno + Auth).
El administrador crea un examen (nombre + email del candidato), obtiene un enlace
`examen.html?id=<uuid>` y lo envía a mano; el candidato lo abre y responde. Sin SMTP.
Fuente: `AGENTS.md` del proyecto base ("Què és" y "Model de dades").

### 1.2 Modelo de datos [H]

Fuente: `supabase/migrations/20260802000000_init.sql` + `AGENTS.md` base.

| Tabla | Contenido | Observación |
|---|---|---|
| `questions` | `id`, `text`, `type`, `options` (jsonb), `correct_option_indexes` (int[]; en la init era `correct_option_index` singular), `active` | Banco maestro. Sin borrado: se desactiva con `active=false`. |
| `exams` | `candidate_name`, `candidate_email`, `status` (pending/in_progress/completed), `num_questions`, `pass_threshold_count`, `time_limit_minutes` (snapshot de `config`), `current_question_index`, `correct_count`, `started_at`, `finished_at` | **No se guarda % ni "aprobado"**: se calcula `correct_count >= pass_threshold_count` (`isPassed`). Duración = `finished_at − started_at`. |
| `exam_questions` | `exam_id`, `question_id`, `question_index`, `options_order` | Fija selección y barajado al crear el examen. |
| `exam_answers` | `exam_id`, `question_index`, `selected_option_indexes` (índices del orden **barajado**) | Una fila por pregunta; upsert. |
| `config` | fila única: `num_questions`=20, `time_limit_minutes`=30, `pass_threshold_count`=14 (defaults de la migración init) | Umbral = nº de aciertos, no porcentaje. |

Banco actual: `scripts/questions.sql` contiene **75 preguntas** (líneas 2–76, recuento
propio), mezcla de opción única (4 opciones) y verdadero/falso, en catalán, sobre el
reglamento unificado de Futbol amb Botons. Origen: Google Sheet "CFBB – Test Reglament de
Joc" (`TODO.md` base, ítem 3). En el seed `multiple_choice` se usa para preguntas de **una**
respuesta; la multi-respuesta real (≥2 correctas) llegó con
`20260808010000_multiple_choice_support.sql` (según `AGENTS.md` base; no leída en detalle).

### 1.3 Reglas de examen (`supabase/functions/_shared/examLogic.ts` y funciones) [H]

- **Selección**: `create-exam` toma las preguntas `active=true`, `shuffle` (Fisher-Yates con
  `Math.random`, no criptográfico) y corta a `min(num_questions, disponibles)`; el umbral se
  recorta a `min(pass_threshold_count, numQuestions)`.
- **Opciones barajadas** por pregunta y examen (`options_order`).
- **Tipos**: `single_choice`, `multiple_choice` (≥2 correctas), `true_false` (opciones fijas
  `["Vertader","Fals"]`).
- **Corrección** (`finalizeExam`): acierta si el **conjunto** de opciones marcadas (mapeadas
  al índice original) es **idéntico** al conjunto correcto (`sameOptionSet`). Sin crédito
  parcial. Sin responder = fallo.
- **Navegación estrictamente secuencial** (`submit-answer` rechaza `question_index ≠
  current_question_index`, 409): no se puede volver atrás ni saltar.
  *(Es el comportamiento del proyecto base. **Sustituido en este proyecto** por navegación libre:
  ver nota del 2026-11-20 en §1.3.)*
- **Tiempo**: cronómetro en servidor (`started_at + time_limit_minutes` vs `now()`); al
  expirar se finaliza con lo respondido. El cliente (`assets/exam.js`, `setInterval` l.134)
  solo pinta la cuenta atrás.
- **Reanudación**: `start-exam` idempotente; cerrar el navegador **no** pausa el reloj.
- **Resultado** (`resultPayload`): nombre, `correct_count`, `num_questions`,
  `pass_threshold_count`, `passed`, `duration_seconds`. **No incluye feedback por pregunta**:
  el base no revela cuáles fallaste ni la respuesta correcta.
- **Sin límite de intentos**: cada examen es un enlace nuevo creado por el admin.

### 1.4 Seguridad del proyecto base [H]

RLS sin políticas para `anon`/`authenticated`; respuestas correctas nunca salen antes de
responder; tiempo validado en servidor; funciones admin con JWT; registro público desactivado.
El `AGENTS.md` base declara "~10 candidats/mes, sense mesures anti-trampes": contexto de
*aquel* proyecto, no requisito de éste.

### 1.5 Idioma [H]

UI y mensajes en **catalán**; documentación de diseño en castellano.

### 1.6 Qué se reutiliza / adapta / desaparece

| Elemento del base | Destino | Notas |
|---|---|---|
| Banco de preguntas (75, catalán) | **Reutiliza** (contenido) | Migrar a fichero (JSON) con `id` estable, versión y metadatos; **añadir campo opcional `explanation`** (contenido editorial posterior). Mantener fuera de repo público (§1.7). Revisión de contenido pendiente (duplicados temáticos entre preguntas). |
| Tipos de pregunta | **Reutiliza** | Misma semántica de conjunto exacto. |
| `num_questions`, `time_limit_minutes`, `pass_threshold_count` | **Reutiliza**; pasan a ser parámetros de una *versión de examen* empaquetada | Defaults 20/30/14. |
| Barajado de preguntas y opciones | **Adapta** | `SecureRandom` o `kotlin.random` sembrado correctamente; la criptografía no es crítica aquí, basta con que no sea predecible por descuido. |
| Corrección (`sameOptionSet`) | **Adapta** | Se ejecuta en el móvil del candidato; **además devuelve el detalle por pregunta**. |
| **Feedback por pregunta** (marcada, correcta, explicación) | **Nuevo** | Requisito [D] y núcleo pedagógico. |
| ~~Navegación secuencial sin retroceso~~ → **Navegación libre** | **Adapta (decidido 2026-11-20)** | **Decisión del usuario, 2026-11-20:** el candidato puede ir a cualquier pregunta (Anterior/Següent y resumen de preguntas) y cambiar o vaciar cualquier respuesta hasta finalizar. Sin respuesta = fallo. Se **persiste cada selección al instante** (persistir antes de navegar se mantiene). Nada finaliza el examen salvo «Finalitzar» confirmado (tras el resumen) o el tiempo agotado: responder la última pregunta ya no finaliza. Sustituye el «secuencial sin retroceso» anterior. |
| Snapshot de configuración por examen | **Adapta** | Se fija en `exam_version` (reglas); ver §4.1 para su relación con `bank_version` (contenido). |
| Cronómetro | **Adapta, pierde garantía** | Local, mejor esfuerzo (§3.3). |
| Reanudación tras cierre | **Adapta** | Persistencia local (§3.2). |
| `candidate_email` | **Desaparece** | Sin backend/SMTP no hay uso; minimización. |
| Edge Functions `create-exam`, `get-exam-state`, `start-exam`, `submit-answer` | **Desaparecen** como servicio; la lógica se reimplementa en la app | |
| Panel admin, Auth, `requireAdmin`, RLS | **Desaparecen** | Sustituidos por el flujo del mantenedor (§2.3). |
| Registro de exámenes/resultados (`exams`, `exam_answers`) | **Desaparece** | Sin registro central; el único "registro" es lo que tiene el candidato. Podría guardarse histórico **local** del candidato (útil para aprender; pregunta 5). |
| Enlace único `?id=` como control de acceso | **Desaparece** | No hay control de acceso: cualquiera con el APK puede examinarse, cuantas veces quiera (aceptado, §1.8). |
| Web (`index.html`) | **Reduce** a página de descarga del APK | Según AGENTS.md de este repo. |

Garantías **perdidas** al quitar el backend y que se **aceptan** [D]: secreto de respuestas,
tiempo fiable, límite de intentos, fecha fiable, resultado no manipulable por el titular.

### 1.7 Banco de preguntas: dónde vive y qué se protege

**Decisión [D]**: el banco, las respuestas y la lógica de corrección **van dentro del APK**
(imprescindible para autocorrección offline con feedback). Lo que **sí** se exige:

- **Nunca** en el historial git de un repo público, ni como fichero/endpoint HTTP legible en
  GitHub Pages u otro hosting.
- Se inyecta **antes de compilar**, por una de estas vías (la elección es de `mobile-architect`
  / `release-engineer`): (a) repo privado + CI que clona y compila; (b) secret/artifact
  privado de GitHub Actions; (c) fichero local fuera de git y compilación local.
- Debe existir **un repositorio público** (Pages + Actions) [D]; puede coexistir con un repo
  privado o con un mecanismo privado de build. El repo público contiene código, no banco.

**Precisión importante (tensión a tener presente) [H/S]**: el APK se publica en GitHub
Releases, es decir, es un *asset HTTP público*, y un APK es un ZIP: cualquiera que lo
descargue puede extraer los ficheros de `assets/` con `unzip` **sin necesidad de decompilar**.
Por tanto la regla "el banco no está en un asset HTTP público" debe leerse como **"no se
publica como fichero independiente ni en git"**; el banco empaquetado dentro del APK queda
expuesto a quien lo abra. **[D, ADR 0002]** Es un riesgo aceptado: el banco real se inyecta en
el build, va dentro del APK público y **no se cifra ni ofusca inicialmente** (una clave en el
mismo APK no protegería frente a extracción técnica y añadiría mantenimiento).

### 1.8 Riesgo residual aceptado (explícito)

| # | Riesgo | Por qué se acepta | Mitigación blanda |
|---|---|---|---|
| R1 | Quien abra el APK obtiene preguntas y respuestas | Objetivo pedagógico + nivel de seguridad rebajado [D]; los candidatos son miembros de una comunidad de confianza. Además, ver respuestas aprendiéndolas es pedagógicamente neutro o positivo. | Banco grande y rotación editorial; no publicar el banco en git/HTTP como fichero. |
| R2 | Un candidato técnico puede forjar una credencial o editar una existente | Perfil de seguridad proporcional [D] | Etiquetar la credencial como "emitida en autoservicio" y no como certificación fuerte (§4.2). |
| R3 | Si la credencial contiene clave privada en el APK, esta es extraíble | Ver §4.3: se documenta sin ambigüedad | Rotación por versión de app; no presentar como garantía criptográfica. |
| R4 | Suplantación: otra persona hace el examen en el móvil del titular | Un móvil pertenece a una persona y puede cederlo; riesgo social, no técnico [D] | Ninguna técnica; el nombre en la credencial es declarado. |
| R5 | Tiempo, fecha e intentos manipulables (hora del sistema, reinstalar, datos borrados) | Sin servidor ni organizador no hay fuente fiable [D] | Reloj monotónico y avisos (§3.2); la fecha se marca "autodeclarada". |
| R6 | Sin revocación | Fase 2 [D]; no justifica backend | Caducidad en la credencial (a decidir) y versión de examen (§4.2). |
| R7 | Compartir el APK por canales no oficiales (versión alterada) | Solo afecta a quien lo instala | Publicar hash/huella en la web de descarga. |

---

## 2. Actores y casos de uso

### 2.1 Candidato

| ID | Caso de uso | Notas |
|---|---|---|
| CU-C1 | **Obtener la app**: web de descarga → APK → instalar (orígenes desconocidos) | Instrucciones muy claras en catalán, con capturas, pensando en usuarios mayores. Hash publicado. |
| CU-C2 | **Identificarse**: introducir nombre y apellidos | Declarado, no verificado (R4). |
| CU-C3 | **Iniciar examen**: ver normas (tiempo, nº preguntas, umbral, navegación libre; sin respuesta = fallo), aceptar y empezar | Sin código de invitación ni organizador. |
| CU-C4 | **Hacer el examen** | §3. |
| CU-C5 | **Ver resultado y feedback**: nº de aciertos y **fallos**, umbral, aprobado/suspendido, tiempo; **detalle de cada pregunta fallada** (lo que marcó, la respuesta correcta) y, si existe, **explicación breve** | Núcleo pedagógico [D]. Si la pregunta no tiene explicación, se muestra solo la respuesta correcta. |
| CU-C6 | **Obtener credencial** si aprueba: guardada en la app y exportable (imagen/PDF con QR) | Emisión automática por la app (§4.3). |
| CU-C7 | **Mostrar credencial** (en pantalla, sin red) o imprimirla | Debe poder verse siempre desde la app y desde el fichero exportado. |
| CU-C8 | **Reintentar** tras suspenso (o repetir para practicar) | Sin límite técnico; no se puede imponer (R5). Política de producto abierta (pregunta 4). |
| CU-C9 | **Recuperar credencial** perdida | Sin backend no hay reenvío. Sirve la copia exportada; si aprueba de nuevo, se emite otra credencial con nuevo id. |
| CU-C10 | **Repasar errores** de intentos anteriores (histórico local) | **Opcional [S]**, coherente con el objetivo pedagógico (pregunta 5). |

### 2.2 Organizador (verificador) — optativo

La verificación **no forma parte del examen** ni condiciona la emisión: la credencial se emite
igual con o sin verificador [D]. El verificador aprobado es la **PWA pública** de ADR 0001
(clave pública, sin secretos, offline tras cargar sus recursos [PV: instalación, primera carga
completa, cámara y modo avión por probar]). No hay segunda app Android en el MVP.

| ID | Caso de uso | Notas |
|---|---|---|
| CU-O1 | **Comprobar una credencial** con la PWA, sin red tras cargarla: ver nombre, fecha, resultado y versión, y un indicador de integridad ("sin alteraciones" / "alterada o no reconocida") | Entrada por cámara, imagen o texto; la PWA no envía contenido a ninguna parte. |
| CU-O2 | **Comprobación visual** (sin app): leer el PDF/imagen impreso o mostrado | **Mínimo viable**: la credencial debe ser comprensible a simple vista por una persona (§4.2). |
| CU-O3 | Ver **motivo** de invalidez (versión no soportada, datos alterados, caducada) en catalán | |
| CU-O4 | **Formato de verificación**: PWA pública [D, ADR 0001]; sin app Android de organizadores en el MVP | Cerrado. La PWA solo contiene clave pública; no cubre PWA clonadas ni claves comprometidas (limitación aceptada, §4.3). |

**Eliminado respecto a la revisión anterior**: emitir invitaciones, corregir QR de respuestas
y firmar credenciales por parte del organizador (opción C); ya no son casos de uso.

### 2.3 Mantenedor (CFBB / administrador del banco)

| ID | Caso de uso | Notas |
|---|---|---|
| CU-M1 | **Editar el banco** (añadir/modificar/desactivar preguntas) en el repo/mecanismo **privado** | Sustituye al panel admin. Desactivar, no borrar. |
| CU-M2 | **Añadir explicaciones breves** a las preguntas (trabajo editorial posterior) | La app debe funcionar sin ellas (campo opcional). |
| CU-M3 | **Cambiar reglas** (nº preguntas, tiempo, umbral) | Cada cambio = nueva versión de examen reflejada en la credencial. |
| CU-M4 | **Publicar versión**: CI construye (con el banco inyectado desde la fuente privada), firma el APK y publica en Releases | Detalle en 03/05. |
| CU-M5 | **Retirar versiones antiguas** / indicar versión mínima | Sin servidor la app no puede saberlo; solo vía publicación y comunicación (pregunta 7). |
| CU-M6 | **Revisión de calidad del banco** (duplicados, ambigüedades, claves de respuesta) | Hechos del base: preguntas casi duplicadas y alguna con respuesta discutible (a detallar en la revisión editorial, fuera del repositorio público). Con feedback visible, **un error en la clave se vuelve más visible y más dañino**: la revisión pasa a ser requisito de calidad. |
| CU-M7 | **Rotar el material de firma** al publicar una app nueva si procede, publicando antes la clave pública nueva en la PWA | Ver §4.3 y ADR 0001. |

---

## 3. Flujo del examen offline (autoservicio)

### 3.1 Paso a paso

0. **Precondición**: app instalada con la versión de examen vigente embebida (sin red tras la
   instalación).
1. **Bienvenida/normas** (catalán, texto grande): nº de preguntas, tiempo, umbral, "puedes ir
   adelante y atrás y cambiar las respuestas hasta finalizar" (decisión 2026-11-20), "las
   preguntas sin respuesta cuentan como falladas", "las respuestas se guardan solas", "no cierres
   la app", aviso de que cambiar la hora no ayuda.
2. **Identificación**: nombre y apellidos (+ datos de §4.1 que se decidan).
3. **Preparación** al pulsar "Començar":
   - generar `exam_id` aleatorio (UUID v4);
   - seleccionar N preguntas del banco activo y barajar opciones;
   - registrar `t_start_monotonic = SystemClock.elapsedRealtime()` y `t_start_wall`
     (informativa);
   - **persistir inmediatamente** el estado. El intento queda iniciado.
4. **Bucle de preguntas**: una por pantalla; "Següent" fija la respuesta (persistida en cada
   paso). Contador visible (tiempo restante, "pregunta i de N"). Avisos a 5 y 1 min.
5. **Fin**: al responder la última o al agotarse el tiempo (sin responder = fallo).
6. **Corrección local**: algoritmo `sameOptionSet`; produce, por pregunta: respuesta marcada,
   correcta, acierto/fallo.
7. **Resultado y feedback** [D]: aciertos, **número de fallos**, umbral, aprobado/suspendido,
   tiempo. Lista de **preguntas falladas** con detalle y, si existe, **explicación breve**.
   Tras ver el feedback puede mostrarse también el resto de preguntas (acertadas) — decisión
   editorial abierta (pregunta 4).
8. **Credencial** (si aprueba): emitida por la app localmente (§4.3), guardada en
   almacenamiento privado de la app y exportable.
9. **Cierre**: el examen pasa a `completed`; no se reabre ni se edita. Se puede iniciar uno
   nuevo.

### 3.2 Casos límite del ciclo de vida

Como no hay organizador ni servidor, **ninguna incidencia se persigue**: el criterio es
"hacer lo razonable y no frustrar al usuario". Esto es deliberado, por el perfil de usuarios.

| Situación | Comportamiento recomendado [S] | Justificación |
|---|---|---|
| **Cierre de la app / muerte por el SO** | El examen **continúa corriendo**; al reabrir se restaura en la pregunta actual con el tiempo restante; si ya expiró, se finaliza. | Igual que el base. Persistencia tras cada respuesta, escritura atómica. |
| **Llamada, notificación, cambio de app** | No pausa. Sin penalización. | Sin supervisión no hay revisión. |
| **Pantalla apagada / Doze** | El tiempo sigue contando con `elapsedRealtime` (incluye suspensión). | `uptimeMillis` se detiene en suspensión. |
| **Reinicio / batería agotada** | **Política única indulgente [D]:** el **tiempo restante** se persiste (al empezar y tras cada respuesta/evento) y, tras un reinicio, se **reancla** al nuevo arranque (nuevo `elapsedRealtime` de partida = restante persistido). El tiempo apagado **no cuenta**. Sin servidor ni sanción. Si el reinicio no se puede detectar de forma fiable (p. ej. `BOOT_COUNT` no disponible), se asume que pudo haberlo y se reancla igualmente. | Un usuario mayor con batería baja no debe perder un examen; el abuso es R5, aceptado. **[PV]** prototipo: reinicio antes de responder, reinicios repetidos y reinicio durante la recuperación; el reanclado debe ser atómico con la persistencia. |
| **Cambio de hora del sistema** | El cómputo usa **solo `elapsedRealtime`** (y el restante persistido), por lo que no afecta. Sin detección ni sanción (opcional: avisar). | Simple; la mayoría de casos son accidentales (zona horaria, NTP). |
| **Fecha de emisión** | La hora de pared **no es fiable**; la credencial marca la fecha como **"fecha del dispositivo"** (autodeclarada). | Honestidad con el verificador (§4.2). |
| **Rotación / multiventana** | El estado no se pierde. | |
| **Capturas de pantalla** | **No** se bloquean por defecto. Con objetivo pedagógico y banco en el APK, `FLAG_SECURE` no aporta y molesta (p. ej. capturar el feedback para estudiar). | Cambio respecto a la revisión anterior. |
| **Desinstalar/borrar datos y repetir** | Permitido; intentos ilimitados (R5). | |
| **Actualización de la app en pleno examen** | El examen en curso se termina con la versión con que empezó (`exam_version` en el estado). | |
| **Root/emulador/hooking** | Fuera de alcance [D]. | |
| **Otra persona hace el examen** | Aceptado (R4). | |

### 3.3 Cálculo de tiempo [S]

`restante = restante_persistido − (elapsedRealtime_ahora − ancla)`, donde `ancla` es el
`elapsedRealtime` del último anclaje (inicio del examen o reinicio del dispositivo). Se
persiste el `restante` calculado en cada respuesta/evento de ciclo de vida, y al detectar un
nuevo arranque se fija un ancla nueva con ese restante. La hora de pared es solo informativa y
nunca decide el tiempo. Tiempo por examen (no por pregunta), como en el base. Es **mejor
esfuerzo**, indulgente y sin sanción: sirve para ritmo y consistencia con el reglamento, no
como control de integridad ni como reloj fiable frente al candidato.

**Validación de reglas [S]** (para `03`): `N > 0`, tiempo límite positivo y
`1 ≤ umbral ≤ N`; IDs de opción únicos por pregunta. Una versión que no cumpla no se empaqueta.

---

## 4. Datos del candidato y de la credencial

### 4.1 Datos y minimización (RGPD)

Principios: minimización (art. 5.1.c), limitación de finalidad. No hay copia central (no hay
backend), lo cual reduce la exposición: los datos solo están en el móvil del titular y en los
ficheros que él exporte. No soy asesor legal; contexto: club pequeño, comunidad de confianza.

| Dato | ¿En la credencial? | Observaciones |
|---|---|---|
| Nombre y apellidos | **Sí** | Necesario para que el organizador reconozca a la persona. |
| **DNI/NIE** | **No** (recomendación) | Alto riesgo si se difunde; un hash es reversible por fuerza bruta. El organizador cotejará con documento si hace falta. Ver pregunta 3. |
| Fecha de nacimiento / edad | No | Salvo requisito federativo (pregunta 3). |
| Email / teléfono / foto | **No** | Sin uso funcional; la foto añade carga RGPD y no se justifica con el perfil de riesgo [D]. |
| `credential_id` (UUID aleatorio) | **Sí** | Referencia sin datos personales; sirve a una futura revocación (fase 2) sin cambiar el formato. |
| Fecha de emisión (marcada "fecha del dispositivo") | **Sí** | §3.2. |
| Fecha de caducidad | **Opcional** (pregunta 4) | Única "revocación" natural sin backend. |
| `exam_version` (versión de **reglas**) | **Sí** | Saber contra qué reglas (N, tiempo, umbral, significado de la puntuación) se aprobó. Solo cambia cuando cambian las reglas o su significado. |
| `bank_version` (versión del **contenido** del banco) | **[S] Opcional**, pública y no sensible | Dos APK con preguntas o claves distintas pueden compartir `exam_version`; sin `bank_version` el verificador no sabe qué banco se usó. Si no se incluye, la trazabilidad es solo local (en la app). No sustituir por hashes de respuestas. Decisión residual (pregunta 7). |
| Resultado: aprobado; aciertos/N | Aprobado: **sí**; nota: **opcional** | Mínimo necesario = "aprobado". |
| Duración, dispositivo, identificador de instalación | **No** | Sin utilidad con este perfil de riesgo; evitan identificadores innecesarios. |
| Identificador del emisor / versión de la app | **Sí** | Permite saber qué app emitió y qué material de firma aplicó. |
| Respuestas individuales / feedback | **No** | Solo local en la app; no viajan en la credencial. |

### 4.2 Requisitos de la credencial desde el punto de vista del organizador

*(El esquema es de `trust-architect`. Aquí solo qué debe cumplir, ajustado al nivel de
seguridad elegido [D].)*

**Obligatorios (MVP):**

1. **Legible por una persona sin herramientas**: nombre, resultado, fecha, versión, claramente
   presentados en catalán (PDF/imagen y pantalla). Es la verificación básica.
2. **Integridad casual**: detectar **errores y edición visual no técnica** (cambiar un nombre o
   una nota en un PDF/imagen con un editor común, copiar/pegar parcial, captura retocada). La
   credencial lleva un **QR/código con los datos y un sello de integridad** comprobable por la
   app/verificador; si el texto visible no coincide con el QR, se detecta.
3. **Coherencia texto ↔ QR**: lo que se ve debe poder compararse con lo que codifica el QR.
4. **Verificable sin red** y sin cuenta, en pocos segundos.
5. **Versión de examen y emisor** presentes, con mensajes de motivo de invalidez en catalán.
6. **Honestidad sobre el origen**: debe indicar **"emitida en autoservicio por la app, sin
   supervisión"** y que no equivale a certificación con garantía criptográfica fuerte. El
   organizador debe saber que se apoya en la confianza social.
7. **Tamaño**: cabe en un QR escaneable en pantalla de móvil y en papel A4.
8. **Independiente de la app del candidato**: sigue siendo comprobable aunque desinstale, si
   guardó el PDF/imagen.
9. **Accesible**: legible por personas mayores (tamaño de letra, contraste).

**Deseables:**
10. **Caducidad** y/o versión mínima aceptada, si el club lo decide (pregunta 4).
11. **Campo `credential_id`** presente desde el MVP para no romper formato cuando exista la
    revocación (fase 2).

**Explícitamente NO exigidos** [D]: resistencia a un atacante que decompila el APK; vinculación
a dispositivo; *attestation*; anti-copia/préstamo; revocación; resultado verificado por tercero;
fecha fiable.

### 4.3 Clave privada en el APK: advertencia obligatoria y decisión

**[D, ADR 0001]** La credencial es un PDF/imagen con **QR firmado por una clave privada
incorporada al APK**; el verificador es la PWA pública con la clave pública. No se usa
checksum sin clave como alternativa de MVP. La tabla siguiente es **histórica** (comparativa
previa a la decisión) y se conserva solo como contexto.

**Si la app emite credenciales firmadas, la clave privada (o material equivalente: secreto
HMAC, keystore) debe estar en el APK o ser derivable de él. Un atacante técnico puede
extraerla y forjar credenciales con apariencia válida.** Esto es inherente a "sin backend +
emisión en autoservicio" y **no es una garantía criptográfica fuerte**. Debe documentarse así
en `02`, en la ayuda de la app y en la web de descarga; no se debe describir como "firma
digital segura" sin esta salvedad. Ocultarla (ofuscación, cifrado con otra clave en el mismo
APK) retrasa pero no impide.

Opciones consideradas (decisión cerrada en ADR 0001, ver abajo):

| Opción | Qué da | Qué no da | Complejidad |
|---|---|---|---|
| **a) PDF/imagen + QR con los datos y un checksum/hash simple (sin clave)** | Detecta errores y ediciones visuales; trivial de implementar y verificar (incluso en una página estática); no hay clave que extraer | Cualquiera que entienda el formato puede recalcular el checksum y forjar | Mínima |
| **b) Token con firma asimétrica (p. ej. Ed25519) con la clave privada en el APK; clave pública en el verificador** | Detecta edición casual; un forjador debe extraer la clave del APK (barrera técnica pequeña pero real); posible rotación por versión | Tras extraer la clave, forja ilimitada; hay que gestionar y no commitear la clave privada (CI/secret) | Media |
| **c) Token con HMAC/secreto compartido embebido en candidato y verificador** | Verificación simple | Ambos binarios contienen el secreto: aún más expuesto; sin ventaja sobre (b) | Baja-media |
| **d) Firma con clave en un servicio externo** | Sin clave en el APK | **Requiere backend/red/organizador**: descartado [D] | — |

**Resultado [D]**: se adopta la opción **(b)** (firma asimétrica; verificador solo con clave
pública), según ADR 0001. Las opciones (a), (c) y (d) quedan descartadas para el MVP. El
esquema concreto (algoritmo, bytes firmados, `kid`) es de `02`/`trust-architect`. La
advertencia de arriba se mantiene: el sello es extraíble del APK y no prueba que el examen se
ejecutara correctamente.

---

## 5. Requisitos no funcionales

| Área | Requisito | Origen |
|---|---|---|
| **Offline** | Examen, corrección, feedback y emisión **100 % sin red** tras la instalación de la app; la PWA verificadora funciona sin red tras cargar sus recursos [PV]. Ninguna llamada de red obligatoria en la app. | AGENTS.md, ADR 0001 |
| **Idioma** | Catalán en toda la UI, errores, normas y credencial. | AGENTS.md |
| **Usuarios** | Miembros del club, incluidos **mayores**: flujos lineales y mínimos, letra grande, lenguaje llano, pocos pasos, sin jerga de seguridad. Instalación del APK explicada paso a paso. | [D] |
| **Android mínimo** | Propuesta: **API 26 (Android 8.0)**, o API 24 si el parque incluye móviles viejos. Decisión final con `mobile-architect` y usuario (pregunta 9). | S |
| **Tamaño** | APK < ~30 MB (el banco de texto pesa <100 KB; lo domina el framework). | S |
| **Accesibilidad** | TalkBack (etiquetas en todo), objetivos táctiles ≥48 dp, contraste WCAG AA, escala de fuente del sistema, tiempo no solo con color/sonido, avisos anunciados por TalkBack. Tiempo extendido: sin organizador, solo como opción/versión de examen (pregunta 10). | S, WCAG 2.2 |
| **Robustez** | Persistencia tras cada respuesta (escritura atómica); soportar muerte del proceso en cualquier instante. | S |
| **Privacidad** | Sin analítica ni trackers; sin permiso de Internet si es posible; datos solo locales. | RGPD |
| **Integridad del binario** | APK firmado; hash publicado en la descarga. | `mobile-architect`/`release-engineer` |
| **Mantenibilidad** | Banco como fichero versionado con `id` estables y esquema validado (incluye `explanation` opcional); lógica de selección/corrección pura y con tests; **simplicidad ante todo** [D]. | S |
| **Confidencialidad del fuente del banco** | Fuera de git público y de HTTP público como fichero; inyección en CI/local (§1.7). | [D] |
| **Seguridad** | Nivel proporcional y rebajado conscientemente (§1.8, §4.2). **No** añadir controles que impidan la autocorrección o exijan organizador. | [D] |

---

## 6. Fuera de alcance (explícito)

- **Backend de cualquier tipo**, sincronización, notificaciones push, analítica.
- **Intervención de un organizador en el examen**: invitación, corrección, firma externa.
- **Panel de administración** o gestión de usuarios/organizadores.
- **Registro central** de candidatos, resultados o credenciales.
- **Revocación** de credenciales (fase 2; sin backend por ella).
- **Garantías contra candidatos técnicos**: protección frente a decompilación/modificación del
  APK, extracción de claves, *attestation*, detección de root/emulador, anti-hooking,
  ofuscación como medida de seguridad.
- **Control de suplantación** (verificación de que quien examina es el titular), proctoring,
  biometría, KYC, foto.
- **Control de tiempo, fecha o intentos fiable**; límite de reintentos.
- **Anti-captura** (`FLAG_SECURE`) por defecto.
- **iOS**, Google Play, web móvil de examen.
- **Pagos, inscripción, certificados con valor legal/federativo oficial.**
- **Preguntas abiertas/de redacción**: solo los tipos del base.
- **Multi-club / multi-idioma** (salvo decisión posterior).
- **Redacción de las explicaciones** (trabajo editorial, posterior).
- **Esquema criptográfico** (doc 02), **stack** (03), **pantallas** (04), **CI/distribución** (05).
- **App Android de organizadores / segunda app verificadora**: fuera del MVP (ADR 0001); la
  verificación es la PWA. Una app nativa sería, como mucho, fase posterior.
- **Checksum sin clave como alternativa de credencial**: descartado (ADR 0001).
- **Cifrado/ofuscación inicial del banco**: descartado (ADR 0002).

---

## 7. Dependencias hacia otros documentos

- → `02-credencial-y-amenazas.md`: §4.2 (requisitos, ajustados al nivel rebajado), §4.3
  (decisión de firma asimétrica y advertencia obligatoria), §1.8 (riesgos aceptados), §3.2
  (qué no es fiable), `exam_version` vs `bank_version` (§4.1).
- → `03-arquitectura-movil.md`: persistencia, reloj monotónico, formato del banco con
  `explanation`, inyección privada del banco en la compilación, ausencia de `FLAG_SECURE`.
- → `04-ux.md`: pantalla de resultado con **detalle de fallos y explicación**, accesibilidad
  para mayores, verificación (PWA) como flujo opcional.
- → `05-distribucion.md`: repo público (Pages + Actions) + fuente privada del banco, firma
  del APK, hash publicado; cuidado de que el banco no entre en logs/artifacts públicos de CI.

---

## Preguntas abiertas para el usuario

*(Solo cuestiones residuales; se han eliminado las ya contestadas: autoservicio, nivel de
seguridad, suplantación, revocación, repo base público, extracción del banco del APK, PWA
como verificador, firma asimétrica. La numeración conserva la de revisiones previas.)*

3. **Datos de identidad**: ¿DNI/NIE en la credencial? (recomendado: no). ¿Edad mínima o
   menores, con consentimiento parental?
4. **Políticas de producto**: ¿caduca la credencial (cuánto) y qué pasa con versiones
   antiguas? ~~¿Navegación sin retroceso o con revisión?~~ (**resuelto 2026-11-20: navegación libre**; ver §1.3). ¿Se muestran
   también las acertadas? ¿Hay reintento libre o espera entre intentos (solo informativa)?
5. **Modo práctica / histórico local**: ¿se desea poder repetir exámenes solo para estudiar
   y consultar errores anteriores sin emitir credencial?
7. **Actualización del banco y `bank_version`**: ¿basta con publicar un APK nuevo, sin
   paquete descargable? ¿Hay que comunicar una versión mínima? ¿Se incluye `bank_version` en
   la credencial o se mantiene solo como trazabilidad local?
9. **Versión mínima de Android** (propongo API 26) y, dado el público mayor, ¿qué móviles
   se esperan?
10. **Accesibilidad**: ¿tiempo extendido u otras adaptaciones como opción de la app, y quién
    decide su uso en ausencia de organizador?
11. **Volumen**: ¿sigue valiendo ~10 candidatos/mes para dimensionar? (Contexto del base,
    no requisito necesariamente vigente.)
12. **Idiomas**: ¿solo catalán o castellano para la verificación?
