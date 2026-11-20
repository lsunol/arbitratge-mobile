# Cierre de diseño — verificación documental

**Veredicto: PASS documental.**

No se reabren los ADR y no se han ejecutado todavía pruebas de implementación.

## Comprobaciones finales

- **Contrato ARB1:** existe una única definición canónica en `02` y `03–05` remiten a ella.
- **G5:** usa únicamente casos públicos con la lista de confianza de test. R01/K02 quedan reservados a G3 y a la lista/clave de release.
- **Wireframes:** los ejemplos de `04` están identificados como ficticios y no reproducen contenido identificable del banco base disponible.
- **B1–B3:** resueltos documentalmente:
  - no quedan las referencias identificables detectadas en los ejemplos vigentes;
  - PWA, firma embebida y banco sin ofuscación inicial están alineados con los ADR;
  - una respuesta no guardada impide avanzar o finalizar.
- **Git:** auditado el 2026-11-20: no había commits ni historial previo; los ficheros estaban sin seguimiento. Debe repetirse la inspección inmediatamente antes del primer push.
- Las revisiones históricas ya no exponen la ruta o el nombre del generador real del banco.

## Decisiones aprobadas

- `docs/adr/0001-credencial-sellada-y-verificador-publico.md`: PDF/imagen + QR firmado; verificador PWA pública, instalable y usable offline tras preparar su caché.
- `docs/adr/0002-banco-privado-en-build-y-apk.md`: fuente real fuera del repositorio público; inclusión en el APK sin ofuscación inicial.

## Fuera del cierre documental — [PV implementación]

Antes de publicar habrá que probar:

- interoperabilidad Android firma → PWA verifica;
- todos los vectores y códigos ARB1;
- cámara, imagen y pegado de texto;
- instalación PWA, primera carga, caché completa/incompleta/borrada y modo avión;
- reinicios, fallos de guardado, muerte del proceso y exportación de credenciales;
- accesibilidad con usuarios mayores;
- release, actualización, hash y restauración del keystore.

Este PASS no acredita todavía que todo el árbol público esté libre de contenido sensible: se requiere otra inspección justo antes del primer push.

## Siguiente fase

Antes de implementar, hay que **revisar qué agentes nuevos hacen falta para implementación** según `docs/design/README.md`.

## Preguntas abiertas para el usuario

Ninguna necesaria para cerrar esta fase documental.
