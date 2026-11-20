# Verificación de correcciones finales

**Veredicto global: FAIL.** La pregunta literal de B1 se sustituyó, pero queda contenido identificable del banco real en `01`. B2 y B3 están corregidos en lo esencial; sin embargo, `02` y `03` especifican contratos de firma incompatibles, por lo que la verificación prevista por ADR 0001 no está coherentemente definida.

**Alcance:** comprobación documental de B1–B3 y de la alineación de `01–05` con ADR 0001/0002. No se han ejecutado pruebas ni auditado el historial Git.

## Resultado por bloqueante

### B1 — Banco real en documentación: **FAIL**

- **Corregido:** el ejemplo concreto de `03` es ahora ficticio (`docs/design/03-arquitectura-movil.md:473–510`).
- **Pendiente:** `01` conserva una descripción identificable de otra pregunta real junto con su respuesta (`docs/design/01-requisitos.md:220`; contraste con un generador histórico del banco). ADR 0002 excluye el banco real de documentación y ejemplos (`docs/adr/0002-banco-privado-en-build-y-apk.md:25–29`).
- **Acción:** retirar esa referencia sin reproducirla en el informe y revisar el árbol e historial destinados a publicarse, como pedía B1 (`docs/design/reviews/revision-final-diseno.md:59–63`).
- **Limpieza:** `05` todavía afirma que la pregunta original sigue en `03` y solicita sustituirla (`docs/design/05-distribucion.md:41–43, 65, 91`); actualizar esas referencias tras la revisión.

### B2 — Decisiones cerradas presentadas como abiertas: **PASS, con limpieza menor**

Las decisiones sustantivas ya se reflejan: PWA pública para el MVP, sin app nativa de organizadores, sello asimétrico y banco sin ofuscación inicial (`docs/design/01-requisitos.md:36–39, 160, 349–350`; `docs/design/02-credencial-y-amenazas.md:36–47`; `docs/design/03-arquitectura-movil.md:235–241`; `docs/design/05-distribucion.md:77–82, 733–736`).

Queda una frase obsoleta en `01` que llama a las opciones «a evaluar», aunque la tabla está rotulada histórica y su resultado está decidido (`docs/design/01-requisitos.md:349–370`). Conviene eliminarla. La incompatibilidad técnica restante de la PWA se detalla más abajo; no exige reabrir el ADR.

### B3 — Avance sin guardar: **PASS documental**

`03` exige persistir antes de avanzar o finalizar, mantener la selección visible y ofrecer reintento o abandono (`docs/design/03-arquitectura-movil.md:337–340, 365–385`). `04` ya prescribe lo mismo y contempla fallos al guardar resultado y credencial (`docs/design/04-ux.md:345–348, 534–536`).

**Pendiente de validación:** probar esos fallos y la recuperación tras muerte del proceso; la corrección comprobada es documental, no una prueba de implementación.

## Incidencias restantes de alineación con los ADR

1. **Bloqueante — contrato de verificación incompatible.** `02` firma el prefijo y el payload codificado (`docs/design/02-credencial-y-amenazas.md:238–247`); `03` firma únicamente los bytes JSON decodificados (`docs/design/03-arquitectura-movil.md:690–696`). También discrepan `res`, `iat`, extensiones críticas y límite de entrada (`02:284–304`; `03:710–734`). **Consecuencia inferida:** una app y una PWA implementadas según sus respectivos documentos rechazarían credenciales legítimas. **Acción:** adoptar un único contrato técnico en `02–05` y validarlo con vectores Android → PWA. Esto concreta ADR 0001; no cambia su decisión.

2. **Importante — pegado y preparación offline de la PWA.** `04` elimina espacios y saltos interiores al pegar (`docs/design/04-ux.md:862–864`), mientras `02` los rechaza (`docs/design/02-credencial-y-amenazas.md:254–255`). Además, `04` vincula en un estado la instalación con funcionamiento offline (`04:786–788`), aunque `05` exige que todos los recursos estén cacheados (`docs/design/05-distribucion.md:711`). **Acción:** alinear la normalización con el contrato único y mostrar preparación offline solo tras comprobarla. La instalación, cámara y modo avión siguen pendientes de prueba antes de publicar la PWA (`docs/adr/0001-credencial-sellada-y-verificador-publico.md:73–74`).

**Recordatorio para el cierre del diseño:** antes de implementar, revisar qué agentes harán falta para esa fase (`docs/design/README.md:44–54`).

## Preguntas abiertas para el usuario

Ninguna sobre B1–B3 ni sobre los ADR aprobados. La alineación indicada puede efectuarse sin reabrir esas decisiones.