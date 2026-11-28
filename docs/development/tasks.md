# Tareas pendientes

Registro de tareas acordadas con el usuario. Marcar `[x]` y anotar fecha/commit al cerrar.
Toda tarea que toque la UI sigue el flujo de `AGENTS.md` (ux-designer + ui-designer antes, reviewer + QA visual después),
salvo que el usuario diga expresamente que se difiere (ver T-04).

## T-01 · Compartir y descargar la credencial como documento verificable (2026-11-28) — implementado, falta QA

**Decisión:** formato **PNG**, no PDF: la PWA lee QR desde imágenes (jsQR) pero no abre PDF, así que un PDF no sería verificable directamente. Un PDF sigue siendo posible como extra (con el mismo QR) si el usuario lo quiere.

- [x] Al aprobar, el candidato puede **compartir** (p. ej. por WhatsApp) y **descargar** su credencial.
- [x] Formato: imagen o **PDF** (el usuario cree que el PDF es más profesional; decidir y registrar en un ADR si cambia el contrato).
- [~] **Requisito duro** (cubierto por test; falta prueba manual con la PWA y WhatsApp): lo que se descargue debe poder verificarse con el verificador (PWA). Es decir, el documento tiene
      que llevar el token ARB1 de forma legible por la PWA: QR (cámara/imagen) y/o texto del token. Comprobar que la PWA
      lee el QR desde una imagen y desde un PDF (si se elige PDF, ¿la PWA acepta PDF o solo imagen? Si no, el PDF debe
      incluir el QR y el usuario lo captura/escanea).
- [x] Android: `FileProvider` + `ACTION_SEND`/SAF; sin permisos de almacenamiento amplios.
- [x] Nada del banco ni secretos en el documento; solo los datos de la credencial (nombre, resultado, `ok/tot`, `dur`, `fail`, fecha, código corto, QR).
- [x] Tests: el documento generado se decodifica y verifica (`OK`) con `core-credential` y con la PWA.

## T-02 · Pantalla de credencial: sello y botón (2026-11-28) — implementado, falta QA visual

- [x] Sello «APTE»: quitar la segunda línea («àrbitre cfbb»); dejar solo «APTE» **centrado** dentro del sello.
- [x] Sustituir el botón «Tornar a l'inici» por **«Descarregar»**, que descarga el documento de T-01 (depende de T-01).
- [x] Registrar la decisión (UI-09 registrada; falta la captura de emulador y la revisión de diseño) en `docs/design/decisiones-usuario-ui.md` (UI-09) con su comprobación mecánica y captura de emulador.
- [ ] Ojo: sin «Tornar a l'inici» el usuario solo sale con el gesto/botón atrás del sistema (ya era así en UI-08(a)); confirmar que es aceptable.

## T-03 · Nuevo banco de preguntas y nuevas reglas del examen (2026-11-28) — banco listo, falta regenerar el APK

- [x] Banco nuevo en `C:\Users\lluis\projects\arbitratge-bank` (192 preguntas). Los APK pasan a generarse con este banco.
- [x] Reglas del examen (en `arbitratge-bank/bank/bank.json`: `num_questions=40`, `pass_threshold_count=35`, `time_limit_minutes=30`, `exam_version` subida a `2026-11-2`; copia previa en `bank/history/`): **40 preguntas**, aprobado con **35 aciertos**, **30 minutos**.
- [x] Verificado (`packReleaseBank`: 192 preguntas activas, N=40, 30 min, umbral 35, shuffle activo; sin imprimir contenido; solo recuentos/IDs, como `bank-curator`): que el banco valida (`BankValidator`),
      que `num_questions=40`, `pass_threshold_count=35`, `time_limit_minutes=30`, `shuffle_questions` y `shuffle_options` en `true`,
      y que `ok ≤ tot ≤ 99`, `bank_version` cumple `^[0-9A-Za-z._+-]{1,24}$`.
- [ ] Regenerar la build release de revisión con este banco (procedimiento de «actualizar el móvil» de `AGENTS.md`); no antes de cerrar los cambios de UI.
- [ ] Comprobar que la UI (textos, resumen, temporizador) se comporta con 40 preguntas y 30 min (escala de fuente 130/200 %).

## T-04 · Revisión de diseño de la fila «Intents fallits» del verificador (2026-11-28)

- [ ] La fila `fail` se añadió sin pasar por ux-designer/ui-designer (decisión del usuario: se revisará al final, con los demás cambios de UI).
- [ ] Comprobar UI-07: el carnet VÀLIDA sigue cabiendo sin scroll en ~360×780 dp a 100 % con las 4 filas (Data, Encerts, Temps, Intents fallits, Codi).
- [ ] Captura de emulador adjunta.

## T-05 · Mostrar `fail` también en la app del candidato (opcional, 2026-11-28)

- [ ] Decidir si la pantalla de credencial/PDF de la app muestra «Intents fallits» (relacionado con T-01).
