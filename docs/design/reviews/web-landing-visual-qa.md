# QA visual — landing `/` y enlace a `/verificador/`

## Re-verificación final (pwa-developer) — PASS en emulación móvil de escritorio; **emulador Android pendiente**

> Actualización: la ejecución anterior usaba `mobile:false` y no es válida como QA móvil. Ahora `web/test/browser-qa.mjs` usa `mobile:true` (deviceScaleFactor 2,75, toque, UA Android) con 320/390 × 100/130/200 % × claro/oscuro (`E/final-*.png`, `E/final-qa-metadata.json`, que declara `kind: desktop-chrome-mobile-emulation`). **La QA en emulador Android real se ejecuta con `node web/test/browser-qa.mjs --android [serial]`** (usa `adb`, `font_scale` nativa de Android 1.0/1.3/2.0 y genera `E/emu-*.png` + `E/emu-qa-metadata.json`); hasta que se ejecute, no hay evidencia de emulador ni de TalkBack.

Sustituye el veredicto «NO PASA» de más abajo (que describe el árbol anterior). Los puntos P0/P1/P2 se corrigieron
(botón apilado con el icono encima bajo 22,5 rem, dirección con `<wbr>` sin `overflow-wrap:anywhere`, chip «Exemple» en el
cuerpo del carnet, círculo de campo CSS, viñetas propias, botones con ancho máximo) y se cambió la leyenda del carnet a
la banda verde (encima del carnet) porque, posicionada debajo, al 200 % pisaba el titular «Què és?» (detectado por el
propio script; ya no ocurre).

- **Comando:** `node web/test/browser-qa.mjs` (Chrome headless por CDP, sin dependencias).
- **URL:** servidor efímero `127.0.0.1:<puerto>/` sobre la build estampada con `--preview` en un directorio nuevo bajo `os.tmpdir()`.
- **Navegador:** Chrome/158.0.8127.36.
- **Build:** sha256 `25fbb93ecc2ad0c52b0e579e5f6283adee8e77bda530ff4e6c25880dda7ba80d` (ruta + contenido de todos los ficheros del artefacto); base git `ff7704a34164` con cambios sin confirmar.
- **Ajustes:** `document.documentElement.style.fontSize = '200%'` (y 100 %); viewport sin emulación móvil (con emulación móvil Chrome ensancha el viewport al desbordar y lo oculta); anchos 320/360/390/1280; `prefers-color-scheme` claro y oscuro; `<details>` abiertos.
- **Comprobaciones por caso:** `scrollWidth <= innerWidth`, ningún elemento con borde derecho > `innerWidth`, ningún texto < 16 px, contraste, carnet sin pisar «Què és?». Resultado: **16/16 casos PASS** (todas las combinaciones de 320/360/390 × 100/200 % × claro/oscuro y 1280 × 100/200 % × claro/oscuro).
- **Evidencia:** `E/final-<ancho>-<claro|oscuro>-<100|200>.png` (página completa) y `E/final-qa-metadata.json` (ajustes, hash y resultados por caso).
- **No verificado:** zoom real del navegador/TalkBack en dispositivo físico (el 200 % es de texto raíz) [PV].

---

> Autor: `ui-designer` · Fecha: 2026-11-21 · Tipo: QA posterior a la implementación (sin editar código).
> Referencias: `web/index.html`, `web/styles.css`, `docs/design/reviews/web-landing-visual-spec.md`
> (en adelante «spec visual»), `docs/design/reviews/web-landing-ux-spec.md` («spec UX»),
> `docs/brand/ESPECIFICACIO-MARCA.md`.
> Evidencias: `docs/design/reviews/evidence/2026-11-21-landing/` (prefijo `E/` abajo).
> **Aviso:** el árbol de `web/` cambió durante esta QA: las capturas `E/*` son de las 21:39 y a las
> 21:52 ya había `minmax(0,1fr)` en `.pitch-grid` y un marcador `__PUBLISHED__`. Por eso cito
> **selectores**, no números de línea. Volví a comprobar P0 con el árbol de las 21:52.
> Skill `ui-ux-pro-max`: búsquedas `"text resize 200 reflow horizontal scroll overflow" --domain ux`
> (→ *Horizontal Scroll* High, *Long Token Wrapping* High: «let flex or grid text children shrink»,
> *Text Reflow* Critical) y `"long url wrapping monospace break" --domain ux` (→ *Long Token Wrapping*).

## Veredicto

**NO PASA todavía.** Las capturas entregadas muestran un bloqueante de accesibilidad:
desbordamiento horizontal con el texto al 200 %. En el árbol actual está **corregido en parte**:
ya no desborda a 360 y 390 px, pero a 320 px el CTA parte «Descarrega/r». Queda además un defecto
mayor en la dirección del verificador. El resto es pulido. La dirección visual sí está conseguida: la banda de gespa, el botón de tiza y el carnet
sobre la línea de banda se leen como propios de Arbitratge/CFBB, no como plantilla. Tras
corregir P0 y P1 bastan nuevas capturas para dar PASS, sin necesidad de rediseñar.

Lo que está bien (verificado en captura):
- **Jerarquía del hero a 390 px** (`E/390-light-fold.png`): marca → H1 de 2 líneas → entradilla →
  CTA de tiza de 64 px, todo dentro del primer pantallazo. El CTA es claramente la acción
  dominante. El enlace al verificador queda subordinado (texto subrayado, sin botón), como pide la
  spec UX §8.
- **Carnet de ejemplo sin seguridad simulada**: sin QR, sin *check*, sin «VÀLIDA» ni sellos. Lleva
  el chip «Exemple», la leyenda «dades inventades» y «Emesa en autoservei» dentro del documento.
  No llevar QR es más prudente que la spec, que preveía un QR de texto plano. Correcto.
- **Fuente**: Atkinson Next y Mono cargan (formas de `Q`, `à`, `l·l` en «instal·lar», y `0`
  barrado en «8.0»). Los pesos 800/700/600/400 se distinguen.
- **Pasos** con disco + filete vertical; aviso «Atura't» en ámbar con icono (`E/390-light-full`,
  tramo 2). Desplegables de 56 px y botón primario repetido de 56 px.
- **Oscuro** (`E/390-dark-full.png`): la banda se mantiene, el CTA de tiza aparece apagado
  (`#E2EAE4`) y el papel del carnet sigue siendo claro. Los botones primario y contorno en menta
  tienen buen contraste.
- **Sección de organizadores**: insignia QR, dirección en caja Mono, botón de contorno secundario,
  aviso anti-enlace y «Límits» en un desplegable. La jerarquía es correcta.
- **Enlace a `/verificador/`**: navega en la misma pestaña y el verificador carga
  (`E/verificador-offline-from-landing-build.png`).

---

## Defectos, por prioridad

### P0-1 · Desbordamiento horizontal con texto al 200 % (BLOQUEANTE en las capturas; parcialmente corregido en el árbol)

- **Estado a las 21:52:** con `.pitch-grid { grid-template-columns: minmax(0, 1fr) }` y
  `min-inline-size: 0` en los hijos, a 360 y 390 px ya no hay recorte
  (`E/qa-ui-text200-320-360-390-after-minmax.png`). **A 320 px el CTA se parte a mitad de
  palabra: «Descarrega / r l'app / (APK)»** (misma captura, columna 1). Falta el paso 3 de abajo
  (*container query*), y hay que repetir las capturas oficiales.

- **Evidencia:** en `E/320-text200-fold.png` aparecen cortados «de Futbo**l**», «sense inte…»,
  «aconsegueixes u…», «credencial d'àrbit…» y el CTA «Descarrega…». Lo he reproducido a 320, **360
  y 390 px** con `html{font-size:200%}` (copia temporal fuera del repo): ver
  `E/qa-ui-text200-320-360-390.png`. Falla también a 390 px, que es el ancho de referencia.
- **Causa (en las capturas):** `.pitch-grid { display:grid }` no declaraba columnas en móvil, así
  que la columna implícita era `auto` y crecía hasta el *min-content* de sus hijos. Esos mínimos
  son el botón de tiza (icono + «Descarregar» a 40 px + *padding*, unos 340 px; `.btn`,
  `.btn-chalk`) y la cabecera del carnet (icono + «Credencial» + chip «Exemple»; `.card-top`,
  `.chip`). Todo el hero se ensanchaba más que el *viewport* y se recortaba.
- **Corrección** (en este orden):
  1. ✅ Ya en el árbol: `.pitch-grid { …; grid-template-columns: minmax(0, 1fr); }` en la regla
     base. La regla de `≥ 60rem` la sobrescribe.
  2. `.btn > span { min-inline-size: 0; }`, para que el texto pueda encogerse dentro del `flex`.
     Con `minmax` ya no desborda, pero hoy es este `span` el que acaba partiendo «Descarregar».
  3. Para que el CTA no parta «Descarregar» por la mitad cuando el texto es grande, el icono se
     coloca **encima** del texto según el ancho medido en `em`:
     ```css
     .hero { container-type: inline-size; }
     @container (max-width: 19em) {           /* 342 px a 100 %; siempre en móvil al 200 % */
       .btn-chalk { flex-direction: column; gap: 4px; padding: 14px 12px; }
     }
     ```
     Las unidades `em` de la *container query* siguen el tamaño de fuente, así que el cambio se
     activa al ampliar el texto y no solo al estrechar la pantalla. A 320 px con el texto al 100 %
     también se activa, y queda mejor que el icono pegado a dos líneas de `E/320-light-fold.png`.
  4. Cabecera del carnet: ver P1-2. Quitar el chip de esa fila elimina el otro mínimo rígido.
- **Aceptación:** capturas a 360 y 390 px con el texto al 200 %, en claro y oscuro, sin recortes ni
  desplazamiento horizontal (`document.documentElement.scrollWidth === innerWidth`).

### P1-1 · La dirección del verificador se parte a mitad de palabra: «verifi / cador/»

- **Evidencia:** `E/390-light-full.png` y `E/390-dark-full.png`, tramo de la caja
  «Adreça del verificador»: `arbitratge-` / `app.futbolbotons.cat/verifi` / `cador/`. A 320 px se
  parte bien (`E/320-light-full.png`, tramo 4) y a 1280 px cabe en una sola línea. **Esta caja
  sirve para comparar o teclear la dirección**: un corte dentro de «verificador» y un salto tras
  `arbitratge-` (que parece un guion de partición) son justo el tipo de error que el aviso
  intenta evitar.
- **Causa:** `.addr { overflow-wrap:anywhere }` y `code.mono { overflow-wrap: anywhere }`. Con
  `anywhere`, Chrome llena la línea y no respeta el `<wbr>` de `.addr` en `index.html`. La guía de Pro Max permite `anywhere` como protección contra el
  desbordamiento, no como forma de partir una dirección que hay que leer.
- **Corrección:**
  - Partir la dirección **solo después de `.` y `/`**, mediante segmentos atómicos:
    ```html
    <p class="addr"><code class="mono"><span class="seg">arbitratge-app.</span><span class="seg">futbolbotons.cat/</span><span class="seg">verificador/</span></code></p>
    ```
    ```css
    .addr, .addr code { overflow-wrap: break-word; }  /* sustituye a anywhere en .addr */
    .addr .seg { display: inline-block; }              /* no se parte salvo que no quepa ni sola */
    ```
    A 390 px saldría `arbitratge-app.` / `futbolbotons.cat/` / `verificador/`, o dos líneas si el
    ancho lo permite. Los segmentos van sin espacios entre ellos en el HTML, así que al copiar la
    dirección sale continua [PV: comprobarlo copiando y pegando en Chrome Android].
  - El *host* lo estampa CI desde `CNAME` (`scripts/stamp-web.mjs`, función `sub`). Hay que añadir un marcador
    (p. ej. `__HOST_SEG__`) que genere los `<span class="seg">` separando después de cada `.`. Es
    tarea de `release-engineer`/`pwa-developer`; no cambia el contrato.
  - `.addr` en Mono 18/600, como dice la spec visual §6.7 (hoy son 16 px), si
    con la partición nueva cabe en tres líneas a 320 px (unos 13 px por carácter; el segmento más
    largo tiene 17 caracteres, unos 220 px de 246 disponibles). Si no cabe, se queda en 16.
  - Lo mismo, con menos urgencia, en la dirección dentro del texto de «Com instal·lar-la»
    (`p.official`, partida como `arbitratge-` / `app…` en `E/390-light-full`, tramo 1) y en el
    pie (`footer code.mono`).

### P1-2 · Cabecera del carnet saturada en móvil: el chip la convierte en 5 líneas

- **Evidencia:** `E/qa-ui-320-card-top-crop.png` (recorte de `E/320-light-full.png`): «Credencial /
  d'aptitud / Àrbitre de Futbol / amb Botons · / CFBB». A 390 px quedan 3 líneas, y la tercera
  empieza con un huérfano «· CFBB» (`E/390-light-fold.png`, y≈1448). En escritorio queda «CFBB»
  suelto (`E/1280-light-fold.png`). La cabecera verde llega a ser más alta que el cuerpo del
  documento, que es la parte memorable.
- **Corrección:**
  - Sacar el chip de `.card-top` y colocarlo en el cuerpo, arriba a la derecha, a la altura del
    nombre: `.card-body { position: relative; }` y
    `.chip { position: absolute; inset-block-start: 14px; inset-inline-end: 16px; color:
    var(--paper-ink); border: 1px solid var(--paper-muted); background: var(--paper); }`, con
    `.card-name { padding-inline-end: 6.5rem; }`. Sigue siendo una etiqueta de 14/700 sobre
    papel y se ve antes que el nombre, así que no pierde su función de aviso.
  - En `.card-top span`: `Àrbitre de Futbol amb Botons&nbsp;· CFBB`, con `&nbsp;` antes del punto
    medio para que «· CFBB» nunca empiece línea. Mejor aún: `<span class="nowrap">· CFBB</span>`.
- **Aceptación:** a 320 y 390 px, título en 1-2 líneas y subtítulo en 1-2 líneas, sin huérfanos.

### P2-1 · El borde del carnet desaparece sobre la banda en oscuro

- **Evidencia:** `E/390-dark-full.png`, tramo 0, y≈1330: la cabecera verde del carnet se funde con
  la banda y el carnet parece empezar en el filete amarillo. En oscuro se quita la sombra
  (`--card-shadow: none`) y el borde pasa a `--hairline` `#3A453E`, con un contraste de 1,3:1
  sobre `#0F3D26`.
- **Corrección:** el borde del carnet es una propiedad del **papel**, no del tema. Añadir
  `--paper-edge: #C5CFC8` (igual en los dos temas) y usar `border: 1px solid var(--paper-edge)`
  en `.card`. En claro no cambia nada.

### P2-2 · Las líneas de cal se deforman: el círculo central es una elipse

- **Evidencia:** en `E/1280-light-fold.png` la elipse es muy achatada (unos 1070 × 520 px). En
  `E/390-light-fold.png` es un arco alargado en vertical. Es así porque
  `preserveAspectRatio="none"` sobre `<ellipse rx=42 ry=42>` en `.pitch-lines`. Un círculo central
  deformado deja de leerse como campo, y es el único ornamento de la página.
- **Corrección:** dejar la línea en el SVG estirable y sacar el círculo a un SVG cuadrado propio:
  ```html
  <svg class="pitch-circle" viewBox="0 0 100 100" aria-hidden="true" focusable="false"><circle cx="50" cy="50" r="49" vector-effect="non-scaling-stroke"/></svg>
  ```
  ```css
  .pitch-circle { position:absolute; inset-inline-start:80%; inset-block-start:50%;
    block-size:min(170%, 150vw); aspect-ratio:1; inline-size:auto; max-inline-size:none;
    translate:-50% -50%; fill:none; stroke:var(--pitch-line); stroke-width:2; }
  ```
  Va dentro de `.pitch-lines`, que ya tiene `overflow:hidden`. Así queda un radio de 0,85 × la
  altura de la banda, que es la geometría de la spec visual §6.1, sin deformarse.

### P2-3 · Desfases menores de ritmo y alineación

| # | Dónde (captura) | Defecto | Corrección |
|---|---|---|---|
| a | `E/390-light-full`, tramo 2: discos 1-3 | El número queda unos 6 px por debajo del centro de la primera línea (disco de 40 px frente a una línea de 28 px) | `.steps li::before { inset-block-start: -6px; }` y `.steps li:not(:last-child)::after { inset-block: 38px 4px; }` |
| b | `E/1280-light-fold.png`, `E/1280-light-full` (tramos 1-2) | Los tres botones ocupan los 608 px de la columna. La spec visual §6.2 pide `auto` con un mínimo de 20rem: en un bloque `flex`, `inline-size:auto` lo estira | En `@media (min-width: 40rem)`, `.btn { inline-size: fit-content; }`. Prioridad baja: en escritorio un CTA ancho no perjudica al público mayor |
| c | `E/390-light-fold.png`, bajo el CTA | Tres bloques de texto de 16 px con el mismo peso y separación (metadatos, «3 passos de sota», verificador). Los dos primeros hablan de la descarga y aparecen tan separados como el tercero, que va dirigido a otro público | `#dl-meta { margin-block-end: 8px; }`, para agrupar metadatos y pista de pasos, y conservar los 24 px antes de `.hero-verify`. Salen dos grupos: descarga y organizador |
| d | `E/390-light-full`, tramo 1: «Què és?» | Viñetas por defecto del navegador: es el único punto que parece plantilla | `.bullets li::marker { color: var(--brand); }`. Opcional: un disco de 8 px `--brand` con `list-style:none` y `::before` (la marca usa el disco como forma recurrente) |
| e | `E/390-light-full`, tramo 3 | Cuatro párrafos grises de 16 px seguidos entre el aviso y el segundo CTA: muro de texto | Solo en lo visual: separarlos 8 px en lugar de 16 (`.warn ~ .small { margin-block-end: 8px }`). Agruparlos en un desplegable es decisión de contenido de `ux-designer` |

### P3 · Coherencia de textos visibles (no es un defecto visual; se pasa a `ux-designer`)

- El paso 1 dice «Toca «**Descarregar**»» y el botón, «Descarregar l'app (APK)» (`ol.steps li:1`
  frente a `.btn-chalk`). La spec visual §6.2 pedía que el paso repitiera exactamente el texto del botón.
- «**Obre** el verificador» (`.hero-verify`) frente a «**Obrir** el verificador» (`.btn-outline`). Las
  dos formas vienen de la spec UX (`web_verify_hint` y `web_vf_btn`), pero se ven a poca distancia.

---

## Salto visual con `/verificador/` (fuera del alcance de la landing)

`E/verificador-offline-from-landing-build.png`: cabecera `#1d6f42`, fuente del sistema, fondo
oscuro azul pizarra (no el verde neutro de la landing) y otro `theme-color`. Al pasar de la
landing al verificador cambia la identidad, como ya anticipaba la spec visual §10. **No bloquea
esta tarea.** Queda como P2 aparte para `pwa-developer` (tokens, Atkinson, `theme-color`
`#0F3D26`, icono sin *check*), con su propia revisión de diseño.

## Huecos de evidencia (para la siguiente ronda de QA)

- No hay capturas a **360 y 411 px**, ni al **130 %**, ni de 320 y 1280 px en oscuro, ni del
  200 % a 360/390 px en el emulador real (la reproducción de P0 es con un `font-size` raíz, no
  con la escala de texto de Android) [PV].
- No hay captura de **`:focus-visible`** (aro blanco en la banda, oscuro fuera), de los
  desplegables abiertos (`hash` partido en `Comprovació avançada`) ni de la **impresión**.
- TalkBack y el orden de lectura (banda → figura con su leyenda → secciones) quedan [PV] en
  dispositivo físico.

## Lista para `pwa-developer`, por impacto ÷ esfuerzo

| P | Cambio | Ficheros | Esfuerzo |
|---|---|---|---|
| P0 | (`minmax` ya hecho) `min-inline-size:0` en el `span` del botón y *container query* del CTA; nuevas capturas al 200 % | `.btn`, `.btn-chalk`, `.hero` | XS |
| P1 | Dirección en segmentos `.seg` + `overflow-wrap:break-word`; marcador CI `__HOST_SEG__` | `.addr`, `p.official`, pie, `code.mono`, `stamp-web.mjs` | S |
| P1 | Chip al cuerpo del carnet; `&nbsp;· CFBB` | `.card-top`, `.chip`, `.card-body`, `.card-name` | XS |
| P2 | `--paper-edge` en el carnet | `:root`, `.card` | XS |
| P2 | Círculo de cal con su propio SVG cuadrado | `.pitch-lines` | XS |
| P2 | Alineación de discos, `fit-content` en escritorio, agrupar metadatos, `::marker` y ritmo de las notas | `styles.css` | XS |
| — | Nuevas capturas: 360/390/411 × 100/130/200 %, claro y oscuro, foco y desplegable abierto | `E/` | S |

## Riesgos residuales

- Al 200 %, el H1 ocupa 5 líneas a 68 px y el CTA queda por debajo del primer pantallazo
  (`E/320-text200-fold.png`). Es aceptable, porque limitar el crecimiento del H1 con `vw`
  rozaría WCAG 1.4.4. Si molesta, se decide con `ux-designer`.
- La leyenda del carnet usa `position:absolute` (`.card-fig figcaption`) y el hueco se reserva con
  `6rem`. Con 5 líneas o más al 200 % en 320 px podría montarse sobre «Què és?» [PV en la próxima
  captura].
- Copiar la dirección partida en segmentos `inline-block` debe dar una cadena continua [PV].
- La identidad visual sigue siendo provisional hasta que CFBB la apruebe (spec visual Q6).
- Desde las 21:52 la leyenda es más larga («Representació simplificada…», 3 líneas a 390 px), lo
  que aumenta el riesgo anterior. Mi reproducción del 200 % usa un `font-size` raíz en Chrome de
  escritorio, no la escala de texto de Android [PV en el emulador].
