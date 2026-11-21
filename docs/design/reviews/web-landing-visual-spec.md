# Especificación visual — web de entrada `/` y enlace a `/verificador/`

> Autor: `ui-designer` · Estado: **propuesta previa a la implementación** (no implementada).
> Alcance: la página estática `web/index.html` (sin JS) servida en `/` de
> `arbitratge-app.futbolbotons.cat`, y cómo conecta con la PWA de `/verificador/`.
> No cambia ADR, ARB1 ni el flujo; lo que choca con `04`/`05` está en §11 como pregunta.
>
> Fuentes leídas: `AGENTS.md`; `docs/brand/ESPECIFICACIO-MARCA.md`;
> `docs/design/reviews/visual-review-app-m1.md` §2 (tokens de la app);
> `app-candidate/.../ui/Theme.kt:30-112` (tokens implementados); `web/verificador/index.html` y
> `styles.css`; `docs/design/04-ux.md` §6 y §7.8; `docs/design/05-distribucion.md` §6.3-6.4;
> captura `docs/design/reviews/evidence/2026-11-21/overview-100-light.png`; marca
> `docs/brand/png/arbitratge-marca-horitzontal-1200.png`. Skills: `ui-ux-pro-max` (SKILL +
> `references/pro-rules.md`) y `frontend-design`.

---

## 0. Resumen

**Dirección: «Centre del camp», versión documento impreso.** La página es una sola columna que
empieza en el césped y termina en papel: una banda de gespa `#0F3D26` con las líneas de cal del
centro del campo contiene la marca, el titular y **un único botón de tiza** (blanco sobre verde)
para descargar el APK. Debajo de la banda, **montada a caballo sobre el borde**, como un carnet
dejado sobre la mesa, está la credencial de ejemplo. Es el único elemento atrevido de la página
y la única superficie con sombra. Todo lo demás es plano, tranquilo y legible: neutros con tinte
verde, Atkinson Hyperlegible Next, texto alineado a la izquierda y secciones separadas por tono,
no por tarjetas.

Por qué no parece copiada de la app ni de una plantilla:

- La app usa la cabecera de marca como **tarjeta con márgenes** (`visual-review-app-m1.md` §4.10).
  La web usa la **banda a sangre** con la credencial cruzando su borde. Son la misma familia,
  pero no el mismo encuadre.
- No hay marcos de móvil de *stock*, degradados, emojis, rejilla de tarjetas iguales ni
  etiquetas en mayúsculas sobre cada título. El único número grande de la página es el de los
  pasos de instalación, que sí son una secuencia.
- La paleta sale de la marca, no de una búsqueda. La `--design-system` de Pro Max proponía rojo
  de equipo + Bebas Neue, o azul marino institucional. Las dos se descartan (§12).

---

## 1. Principios (lo que hace única esta página)

1. **Un único momento memorable:** la credencial sobre la línea de banda (§6). La banda verde y
   el botón de tiza la sirven; nada compite con ella.
2. **Dos superficies, como en el campo:** la gespa (arriba, identidad) y la cal o el papel
   (resto, lectura). No se introduce un tercer color de superficie.
3. **El disco es la única forma recurrente:** el símbolo, los números de los pasos y la
   insignia del verificador. Los rectángulos redondeados se reservan para controles y
   documentos.
4. **Las líneas de cal son el único ornamento**, decorativas, a 16 % de opacidad y solo en la
   banda. No imitan seguridad: sin guilloches, sellos ni *checks* (marca §8-§9; 02 §4/T5).
5. **Público mayor primero:** cuerpo de 18 px, botón de 64 px, ningún texto por debajo de 16 px
   fuera de la ilustración, y todo legible al 200 % de zoom sin desplazamiento horizontal.

---

## 2. Color

### 2.1 Tokens (los mismos hex que `Theme.kt`, con nombres CSS)

Regla: los componentes solo usan variables, nunca hex sueltos. Todos los valores son los de la
app (`Theme.kt:30-89`) o los de la marca (`ESPECIFICACIO-MARCA.md` §3), así que la coherencia
con el APK es literal.

| Token CSS | Claro | Oscuro | Rol M3 / marca | Uso en la web |
|---|---|---|---|---|
| `--pitch` | `#0F3D26` | `#0F3D26` | `BrandColors.Pitch` | Banda superior (igual en los dos temas) y banda del carnet |
| `--on-pitch` | `#FFFFFF` | `#FFFFFF` | — | Titular y marca sobre la banda |
| `--on-pitch-muted` | `rgb(255 255 255 / .88)` | igual | — | Entradilla y metadatos sobre la banda |
| `--pitch-line` | `rgb(255 255 255 / .16)` | igual | `PitchLine` | Cal decorativa |
| `--chalk-btn` | `#FFFFFF` | `#E2EAE4` | — | Botón principal sobre la banda (en oscuro, apagado para no deslumbrar) |
| `--chalk-btn-hover` | `#E8F3EC` | `#D3E6D9` | — | *Hover* del botón de tiza |
| `--chalk-btn-active` | `#D7EEDF` | `#C6EED3` | `primaryContainer` | Pulsado |
| `--bg` | `#F2F5F3` | `#0F1512` | `background` | Fondo general |
| `--surface` | `#FFFFFF` | `#151C18` | `surface` | Sección de instalación, desplegables |
| `--surface-2` | `#EEF2EF` | `#1A221D` | `surfaceContainer` | Sección de organizadores y pie |
| `--ink` | `#18241D` | `#E2EAE4` | `onBackground` | Texto principal |
| `--ink-muted` | `#4A5750` | `#BAC6BE` | `onSurfaceVariant` | Texto secundario |
| `--brand` | `#1D6F42` | `#86D7A4` | `primary` | Botón primario fuera de la banda, enlaces, borde del secundario |
| `--on-brand` | `#FFFFFF` | `#003920` | `onPrimary` | Texto del primario |
| `--brand-soft` | `#D7EEDF` | `#1A5235` | `primaryContainer` | Disco de número de paso, insignias |
| `--on-brand-soft` | `#0A3A20` | `#C6EED3` | `onPrimaryContainer` | Número dentro del disco |
| `--outline` | `#6B7770` | `#87938B` | `outline` | Borde de controles (≥ 3:1) |
| `--hairline` | `#C5CFC8` | `#3A453E` | `outlineVariant` | Divisores y bordes decorativos |
| `--warn` | `#FFE08A` | `#FFE08A` | `WarnAmber` | Aviso «atura't» (igual en los dos temas, como el temporizador de la app) |
| `--on-warn` | `#2B1F00` | `#2B1F00` | `OnWarnAmber` | |
| `--ref-card` | `#F2C230` | `#F2C230` | `RefCard` | **Solo** el punto del símbolo y el filete del carnet |
| `--paper` / `--paper-ink` / `--paper-muted` / `--paper-foot` | `#FFFFFF` / `#18241D` / `#4A5750` / `#EEF2EF` | **iguales** | `Paper*` | Carnet de ejemplo: siempre claro |
| `--focus` | `#18241D` | `#E2EAE4` | — | Anillo de foco sobre fondos claros u oscuros |
| `--focus-on-pitch` | `#FFFFFF` | `#FFFFFF` | — | Anillo de foco dentro de la banda |

**Prohibido:** degradados de cualquier tipo, el lila de M3, el gris frío `#F4F6F8` y la tinta
`#1F2937` de la PWA actual, el azul `#0B57D0` como foco (no es de la marca) y el amarillo como
texto o como fondo de bloque.

### 2.2 Contraste (calculado con la fórmula WCAG; [PV] con herramienta en el navegador)

| Par | Ratio | Nivel |
|---|---|---|
| `--ink` sobre `--bg` / `--surface` (claro) | 14,6 / 16,0 | AAA |
| `--ink` sobre `--bg` / `--surface` (oscuro) | 15,1 / 14,1 | AAA |
| `--ink-muted` sobre `--surface` / `--bg` / `--surface-2` (claro) | 7,6 / 6,9 / 6,7 | AAA / AA / AA |
| `--ink-muted` sobre `--bg` / `--surface` / `--surface-2` (oscuro) | 10,5 / 9,8 / 9,2 | AAA |
| Blanco sobre `--pitch` (titular) | 12,2 | AAA |
| Blanco al 88 % sobre `--pitch` (entradilla) | 9,9 | AAA |
| `--pitch` sobre `--chalk-btn` (texto del botón, claro / oscuro) | 12,2 / 10,0 | AAA |
| `--brand` sobre `--bg` / `--surface` (enlaces, claro) | 5,6 / 6,2 | AA |
| `--brand` sobre `--bg` / `--surface` (oscuro) | 10,8 / 10,1 | AAA |
| `--on-brand` sobre `--brand` (claro / oscuro) | 6,2 / 7,6 | AA / AAA |
| `--on-brand-soft` sobre `--brand-soft` (claro / oscuro) | 10,5 / 7,2 | AAA |
| `--on-warn` sobre `--warn` | 12,5 | AAA |
| `--outline` sobre `--surface` / `--bg` (bordes de control) | 4,7 / 4,3 | ≥ 3:1 |
| `--ref-card` sobre `--pitch` (no es texto) | 7,3 | — |
| Estados (§6.2): blanco sobre `#185C37`/`#145530`; `#003920` sobre `#9BE0B3`/`#B4E8C6`; `--pitch` sobre `#E8F3EC`/`#D3E6D9`/`#C6EED3`; `--brand` sobre `--brand-soft` (claro / oscuro) | 8,0 / 8,9; 8,5 / 9,5; 10,8 / 9,4 / 9,7; 5,1 / 5,3 | AA o mejor |

El texto principal es AAA en los dos temas sin coste añadido. Los enlaces en claro se quedan en
AA (5,6:1) para no cambiar el verde de marca. Además van **siempre subrayados**, así que no
dependen solo del color.

### 2.3 Política de modo oscuro

- Sigue `prefers-color-scheme` (sin JS no hay interruptor). `<meta name="color-scheme"
  content="light dark">` y `color-scheme: light dark` en `:root`, para que los controles
  nativos (`<details>`, barras de desplazamiento) acompañen.
- **La banda de gespa es la misma en los dos temas.** En oscuro se separa poco del fondo
  (1,5:1), pero el contraste del contenido no cambia y las líneas de cal marcan la zona. No
  hace falta un borde.
- **El carnet de ejemplo se queda claro** (documento, como en la app, Q7 de la revisión M1). En
  oscuro se cambia la sombra por un borde de 1 px `--hairline`.
- **El botón de tiza se apaga en oscuro** (`#E2EAE4`) para evitar el deslumbramiento que se
  vio en la captura `19` de la app.
- Las capturas de la app cambian de tema con `<picture><source media="(prefers-color-scheme:
  dark)">` (§7).
- `<meta name="theme-color" content="#0F3D26">` (un solo valor): en Chrome Android la barra de
  direcciones continúa la banda. La PWA usa hoy `#1d6f42`; ver §10.

---

## 3. Tipografía

### 3.1 Familia y carga

- **Atkinson Hyperlegible Next** (OFL 1.1), la misma que el APK (`res/font/atkinson_next.ttf`).
  Se usa **Atkinson Hyperlegible Mono** solo para el SHA-256, la huella del certificado y la
  dirección del verificador, para que `0/O`, `8/B` y `1/l` no se confundan al compararlos.
- **Autoalojada** (no Google Fonts ni ningún tercero): `web/fonts/atkinson-next.woff2` y
  `web/fonts/atkinson-mono.woff2`, más `web/fonts/OFL.txt`.
  - Generarlas desde los TTF variables con `pyftsubset … --flavor=woff2`, conservando el eje
    `wght` y el subconjunto latino + catalán:
    `U+0000-00FF, U+0131, U+0140, U+013F, U+0152-0153, U+02BC, U+2013-2014, U+2018-201E,
    U+2022, U+2026, U+20AC`. El rango incluye `ŀ`/`Ŀ` (U+0140/013F) y el punt volat `·`
    (U+00B7, dentro de `00FF`).
  - Presupuesto: Next ≤ 45 KB y Mono ≤ 20 KB (este último puede reducirse a `0-9 A-F a-z . / : -`).
    [PV] Medir el tamaño real.
- `@font-face { font-display: swap; }` con `<link rel="preload" as="font" type="font/woff2"
  crossorigin>` **solo para Next**. Mono carga tarde porque está dentro de `<details>`.
- Fallback métrico para no mover el texto al cambiar de fuente (Pro Max: *Font Loading*):
  `@font-face { font-family: "Atkinson fallback"; src: local("Arial"), local("Roboto");
  size-adjust: …; ascent-override: …; descent-override: …; }`. Los valores se calculan en la
  implementación con una herramienta de métricas (capsize/fontaine) [PV]. Pila final:
  `"Atkinson Hyperlegible Next", "Atkinson fallback", system-ui, sans-serif`.
- **Choca con 04 §6.2 («sin webfonts»)** y con la CSP de 05 §6.3, que no tiene `font-src`.
  Hace falta `font-src 'self'` (Q2).

### 3.2 Escala (px en el móvil; `rem` en el código; base `html{font-size:100%}`)

| Rol | Móvil | ≥ 60rem | Peso | Interlineado | Uso |
|---|---|---|---|---|---|
| `--t-display` | 34 | 48 | 800 | 1,12 | H1 de la banda |
| `--t-h2` | 26 | 30 | 700 | 1,25 | Títulos de sección |
| `--t-h3` | 20 | 20 | 700 | 1,35 | Título de paso o de bloque |
| `--t-lead` | 20 | 22 | 400 | 1,5 | Entradilla de la banda |
| `--t-body` | 18 | 18 | 400 | 1,55 | Cuerpo |
| `--t-btn` | 20 (principal) / 18 | igual | 700 / 600 | 1,25 | Botones |
| `--t-small` | 16 | 16 | 400 | 1,5 | Metadatos, notas y pie (**mínimo**) |
| `--t-mono` | 17 | 18 | 500 | 1,5 | SHA-256, huella y dirección |
| Ilustración (solo carnet) | 14-22 | igual | 400-700 | — | Texto con `aria-hidden` dentro del ejemplo (mínimo 14 px, marca §4) |

- El display usa `clamp(2.125rem, 1.6rem + 2.2vw, 3rem)`. El mínimo y el máximo van en `rem`
  para que el zoom del navegador los escale.
- `text-wrap: balance` en H1 y H2, y `text-wrap: pretty` en el cuerpo (mejora progresiva).
- Minúscula de frase en todo. **Ninguna etiqueta en mayúsculas ni con espaciado ancho**
  (antipatrón de `frontend-design`). El único texto en mayúsculas es el valor «APTE» dentro del
  carnet, que es dato del documento (`cred_value_apte`).
- Medida: párrafos con `max-inline-size: 38rem` (~68 caracteres a 18 px).
- Alineación: todo a la izquierda, también la banda en escritorio. No se centra nada salvo el
  número dentro de su disco.

---

## 4. Formas, espaciado y retícula

### 4.1 Radios (los de `ArbShapes`)

| Token | px | Uso |
|---|---|---|
| `--r-xs` | 6 | Chip «Exemple» del carnet |
| `--r-sm` | 10 | Caja de la dirección, bloque del hash |
| `--r-md` | 14 | Botones, desplegables, aviso «atura't» |
| `--r-lg` | 20 | Carnet, capturas |
| `--r-full` | 50 % | Discos: símbolo, números de paso, insignia del verificador |

Sin píldoras en los botones (al 200 % se deforman: lección de la app).

### 4.2 Espaciado (ritmo de 4/8)

```
--s1: 4px   --s2: 8px   --s3: 12px  --s4: 16px  --s5: 24px
--s6: 32px  --s7: 48px  --s8: 64px  --s9: 96px
--gutter: 20px (base) · 32px (≥ 40rem) · 48px (≥ 60rem)
--section-y: 56px (base) · 88px (≥ 60rem)
--wrap: 72rem   (contenedor de página)
--measure: 38rem (columna de lectura)
```

- Entre el título de sección y el contenido: `--s5`. Entre párrafos: `--s4`. Entre bloques de
  una misma sección: `--s6`.
- Puntos de corte: **base (≥ 320 px)**, **40rem** (más margen lateral) y **60rem** (banda en
  dos columnas y tres capturas en fila). Con `min-width` en `rem`, al ampliar la fuente o el
  zoom se vuelve a la disposición de móvil, que es lo que se quiere.
- Elevación: 0 en todo **salvo el carnet**:
  `box-shadow: 0 1px 2px rgb(15 61 38 / .14), 0 12px 32px -8px rgb(15 61 38 / .28)`. Es una
  sombra teñida de verde, no el gris genérico `rgba(0,0,0,.1)`.

---

## 5. Estructura de la página

### 5.1 Esquema y landmarks

```html
<body>
  <svg hidden> <symbol id="i-download">…</symbol> … </svg>   <!-- sprite de iconos, §8 -->
  <header class="pitch">                   <!-- banda; contiene el <h1> -->
  <main>
    <section id="app"          aria-labelledby="h-app">        Què és
    <section id="installar"    aria-labelledby="h-install">    Com instal·lar-la + <details> avançada
    <section id="verificar"    aria-labelledby="h-verify">     Per als organitzadors
  </main>
  <footer>                                  <!-- contacto, privacidad, dirección -->
</body>
```

Sin navegación superior ni cabecera fija: la página es corta y una barra fija tapa el foco
(Pro Max *Focus Not Obscured*).

### 5.2 Móvil (≈ 375-411 px)

```
┌───────────────────────────────────────┐  ← --pitch, a sangre; cal decorativa
│ (●) Arbitratge                        │    marca negativa con subtítulo, alto 44
│     Futbol amb Botons            ╎ ◜  │
│                                  ╎(   │
│ Examen d'àrbitre de Futbol       ╎ ◟  │    H1 --t-display 34/800 blanco
│ amb Botons                            │
│ Fes l'examen al mòbil. Es corregeix   │    --t-lead blanco 88 %
│ sola i, si aproves, tens una          │
│ credencial.                           │
│ ┌───────────────────────────────────┐ │
│ │ ⤓  Descarrega l'app (APK)         │ │    botón de tiza, 64 px, ancho completo
│ └───────────────────────────────────┘ │
│ Versió 1.0.0, 12 MB. Android 8.0 o    │    --t-small blanco 88 %
│ superior.                             │
│ Ets organitzador? Comprova una        │    enlace de ancla a #verificar (subrayado)
│ credencial                            │
│                                       │    padding-bottom 88
│  ┌─────────────────────────────────┐  │
├──│▓▓ (●) Credencial d'aptitud [Ex.]│──┤  ← el carnet cruza el borde (−64 px)
   │━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━│     filete --ref-card 2 px
   │ Anna Prova Puig                 │
   │ Resultat  APTE        ┌──────┐  │
   │ Codi 1291-82BC        │  QR  │  │
   │                       └──────┘  │
   │ Emesa en autoservei.            │
   └─────────────────────────────────┘
    Exemple amb dades inventades.          ← figcaption, --t-small --ink-muted
  --bg
  Què és                                   ← H2
  [captura]  Fes l'examen al mòbil          fila: captura 120 px + texto
             Preguntes del reglament…
  [captura]  Sabràs què has fallat
  [captura]  Mostra la credencial
├───────────────────────────────────────┤
  --surface
  Com instal·lar-la                        ← H2
  (1) Toca «Descarrega l'app (APK)». …     ← disco 40 px + texto; filete vertical
   │
  (2) Obre el fitxer descarregat. …
   │
  (3) Toca «Instal·lar».
  ┌───────────────────────────────────┐
  │ ⚠ Si el mòbil diu que l'app és    │    aviso --warn
  │   una amenaça, atura't. …         │
  └───────────────────────────────────┘
  Les pantalles canvien segons la marca…
  ┌───────────────────────────────────┐
  │ ⤓  Descarrega l'app (APK)         │    primario verde (repetición), 56 px
  └───────────────────────────────────┘
  ┌───────────────────────────────────┐
  │ Comprovació avançada          ⌄   │    <details>
  └───────────────────────────────────┘
├───────────────────────────────────────┤
  --surface-2
  Per als organitzadors                    ← H2
  (QR) Comprova una credencial al
       mòbil, sense instal·lar res més.
  ┌───────────────────────────────────┐
  │ arbitratge-app.futbolbotons.cat/  │    caja de dirección, Mono
  │ verificador/                      │
  └───────────────────────────────────┘
  ┌───────────────────────────────────┐
  │   Obre el verificador             │    secundario con borde
  └───────────────────────────────────┘
  ⓘ Instal·la'l i obre'l sempre des de la icona…
  Aquesta marca ajuda a detectar canvis casuals; no és una prova…
├───────────────────────────────────────┤
  pie (--surface-2, divisor --hairline)
  CFBB · contacte · Privacitat ⌄ · Adreça d'aquesta pàgina
```

### 5.3 Escritorio (≥ 60rem)

```
┌──────────────────────────────────────────────────────────────────────────┐
│ (●) Arbitratge                                              ╎    ◜‾‾◝    │
│     Futbol amb Botons                                       ╎  (      )  │
│                                                             ╎    ◟__◞    │
│ Examen d'àrbitre de Futbol            ┌────────────────────────────┐     │
│ amb Botons                            │▓▓ (●) Credencial d'aptitud │     │
│ Fes l'examen al mòbil…                │━━━━━━━━━━━━━━━━━━━━━━━━━━━━│     │
│ [ ⤓ Descarrega l'app (APK) ]          │ Anna Prova Puig            │     │
│ Versió 1.0.0, 12 MB. Android 8.0…     │ …                   [QR]   │     │
│ Ets organitzador? …                   │ Emesa en autoservei.       │     │
└───────────────────────────────────────│────────────────────────────│─────┘
                                        └────────────────────────────┘
  Què és
  ┌────────┐ ┌────────┐ ┌────────┐      tres columnas: captura 16rem + título + texto
  │captura │ │captura │ │captura │
  └────────┘ └────────┘ └────────┘
  …resto en columna de 38rem alineada con el titular (no centrada en la ventana)
```

- La banda tiene dos columnas: `grid-template-columns: minmax(0, 1fr) minmax(20rem, 24rem)` y
  `gap: var(--s8)`. El carnet va en la columna derecha, alineado abajo, y sobresale 64 px por
  debajo de la banda.
- El botón de tiza deja de ocupar todo el ancho: `inline-size: auto`, `min-inline-size: 20rem`.
- Las secciones de texto comparten el borde izquierdo con el titular: misma `--wrap` y el mismo
  `--gutter`. Las columnas de lectura no se centran en la ventana.

---

## 6. Componentes

### 6.1 Banda de gespa (`header.pitch`)

- Fondo `--pitch`, `padding: var(--s6) var(--gutter) var(--s9)` (abajo 96 px para el solape) y
  `padding-top` con `max(var(--s6), env(safe-area-inset-top))`.
- **Líneas de cal**: un `<svg aria-hidden="true" focusable="false">` en línea, con
  `position:absolute; inset:0` y `pointer-events:none` (no se puede usar `data:` en CSS porque
  la CSP solo permite `img-src 'self'`). Es la geometría de `pitchLines()` de la app: línea
  vertical en `x = 80 %` y círculo de radio `0,85·alto` centrado en `(80 %, 50 %)`, trazo de 2 px
  `--pitch-line`, con `vector-effect: non-scaling-stroke`. Se recorta con `overflow:hidden` en
  la banda (el carnet sale de la banda porque está fuera de ese contenedor, §6.3).
- **Marca**: `<img src="img/arbitratge-marca-negatiu-subtitol.svg" alt="Arbitratge, Futbol amb
  Botons" width="179" height="44">` (44 px de alto en móvil y 56 px desde 60rem; ancho
  ≥ 160 px, marca §6). Se copia de `docs/brand/svg/marca/`. Área de seguridad = X = H/4
  alrededor: 11 px como mínimo entre la marca y el H1, aunque se usa `--s6`.
- **H1** (`home_title`, ya canónico en la app): «Examen d'àrbitre de Futbol amb Botons».
- **Entradilla** (propuesta, la aprueba `ux-designer`): «Fes l'examen al mòbil. Es corregeix
  sola i, si aproves, obtens una credencial que un organitzador pot comprovar.»
- **Metadatos**: una frase, no una cadena con puntos medios: «Versió {v}, {mb} MB, publicada el
  {data}. Requereix Android 8.0 o superior.» La estampa CI. Va en `--t-small`, blanco al 88 %,
  y se asocia al botón con `aria-describedby`.
- **Ancla para organizadores**: «Ets organitzador? Comprova una credencial» → `#verificar`.
  Enlace de texto blanco subrayado (`text-underline-offset: 3px`), en una línea propia y con
  `min-block-size: 48px` de zona táctil (`display:inline-flex; align-items:center`). Lleva a la
  sección, **no** a la PWA: el enlace a la PWA está solo en su sección, junto al aviso.

### 6.2 Botones

| Variante | Dónde | Fondo / texto | Borde | Alto mín. | Padding | Tipo |
|---|---|---|---|---|---|---|
| **Tiza** (principal) | Banda | `--chalk-btn` / `--pitch` | — | **64 px** | 18 × 24 | `--t-btn` 20/700 |
| **Primario** | Después de los pasos | `--brand` / `--on-brand` | — | 56 px | 14 × 20 | 18/600 |
| **Secundario** | Verificador | transparente / `--brand` | 2 px `--brand` | 56 px | 14 × 20 | 18/600 |
| Enlace | En el texto | — / `--brand` (en la banda, blanco) | subrayado 1 px, offset 3 px | 48 px de zona si va solo | — | heredado |

- Son `<a>` (descargas y navegación), nunca `<button>` sin JS. Icono de 24 px a la izquierda,
  `gap: var(--s3)`, contenido centrado y `text-align:center`. Al 200 % el texto puede ocupar
  dos líneas: sin altura fija, solo `min-block-size`.
- Anchura: 100 % en móvil; `auto` con `min-inline-size: 20rem` desde 40rem.
- Estados:
  - `:hover` (solo con `@media (hover:hover)`): tiza → `--chalk-btn-hover`; primario → `#185C37`
    en claro y `#9BE0B3` en oscuro; secundario → fondo `--brand-soft`.
  - `:active`: tiza → `--chalk-btn-active`; primario → `#145530` (en oscuro, `#B4E8C6`).
  - `:focus-visible`: `outline: 3px solid var(--focus); outline-offset: 3px`; en la banda,
    `--focus-on-pitch`. Así queda un aro blanco separado del botón por 3 px de verde, que se
    distingue del botón blanco.
  - Transición: solo `background-color` a 120 ms y `ease-out`; ninguna con
    `prefers-reduced-motion: reduce`. Sin `transform` ni sombras que cambien al pulsar.
- `href` del APK: la URL **versionada** de la release (05 §6.3, I5) con el atributo `download`.
  El tamaño va en los metadatos, no en el botón.
- Texto del botón: «Descarrega l'app (APK)» (05 §6.3). 04 §7.8 dice «Descarregar l'app (APK)»:
  lo unifica `ux-designer` (Q7). El paso 1 tiene que repetir exactamente el mismo texto.

### 6.3 Credencial de ejemplo (el elemento memorable)

Se construye en **HTML+CSS**, no como imagen: es nítida a cualquier zoom, sigue el tema y pesa
poco. Es ilustración: `<figure>` con el contenido del carnet en un `<div aria-hidden="true">`,
y la `<figcaption>` visible es lo que se anuncia.

```
┌─────────────────────────────────────────────┐ radio 20; borde 1 px --hairline; sombra (§4.2)
│▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓│ banda --pitch 60 px, padding 0 16
│▓ (●32) Credencial d'aptitud     [Exemple] ▓│ título 16/700 blanco; chip, §abajo
│▓       Àrbitre de Futbol amb Botons · CFBB▓│ 14/400 blanco 88 %
│━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━│ filete 2 px --ref-card (= banda A4: 30 mm + 1,2 mm)
│ Anna Prova Puig                             │ 22/28/700 --paper-ink; padding 16
│                                             │
│ Resultat            ┌──────────┐            │ etiqueta 14 --paper-muted
│ APTE                │          │            │ valor 16/700 --paper-ink
│ Data del dispositiu │   QR     │            │ QR 104 px, módulos negros sobre blanco,
│ 20/11/2026          │  104 px  │            │ zona de silencio de 4 módulos, sin logo
│ Codi                │          │            │
│ 1291-82BC (Mono)    └──────────┘            │
│─────────────────────────────────────────────│
│ Emesa en autoservei.                        │ pie --paper-foot, 14/400 --paper-muted, 10 × 16
└─────────────────────────────────────────────┘
  Exemple amb dades inventades. Així és la credencial que obtens si aproves.   ← figcaption
```

- Ancho `min(100%, 22rem)` en móvil y `24rem` desde 60rem. En móvil se alinea a la izquierda
  con el `--gutter`, no se centra.
- **Solape**: el `<figure>` va justo después de `</header>` con `margin-block-start: -64px`,
  `position: relative` y `z-index: 1`. Desde 60rem, en cambio, va dentro de la rejilla de la
  banda, alineado abajo, con `margin-block-end: -64px`, y la banda tiene `overflow: visible`
  (las líneas de cal se recortan con su propio contenedor).
- Símbolo de 32 px: `img/arbitratge-icona.svg` (el disco sobre la banda verde deja ver la cal,
  el botó y el punto amarillo). Sin rotarlo ni cambiarle los colores (marca §9).
- **Chip «Exemple»**: 14/700, blanco, `padding: 2px 8px`, `--r-xs`, borde de 1 px blanco al
  60 %. Evita que alguien tome el ejemplo por una credencial real.
- **QR del ejemplo**: SVG estático (`img/qr-exemple.svg`, con `shape-rendering: crispEdges`)
  que codifica **texto plano**: «EXEMPLE: aquesta no és una credencial d'àrbitre.». **No es un
  token ARB1** ni lleva ningún sello. Si un organizador lo escanea con el verificador, verá el
  resultado de formato no reconocido, que es correcto. Lo genera `pwa-developer` o
  `credential-engineer` una sola vez.
- **T5 / 02 §4 (obligatorio)**: sin *check*, sin «VÀLIDA», sin sellos ni hologramas. La banda
  verde es de marca. «Emesa en autoservei» va dentro del documento.
- En oscuro: el papel sigue siendo blanco y la sombra se sustituye por un borde de 1 px
  `--hairline`. Hay que asegurarse de que la sombra **no** se aplica en oscuro, porque sobre
  `#0F1512` parece una mancha.
- Los textos del carnet repiten las cadenas de la app (`cred_title`, `cred_subtitle`,
  `cred_field_*`, `cred_value_apte`, `cred_orig`). El nombre ficticio es el mismo de la
  revisión M1.

### 6.4 «Què és» — tres filas con captura

No es una secuencia de pasos, así que **no lleva números**. Son tres filas con un título en H3,
dos líneas de texto y una captura real de la app.

| # | H3 (propuesta) | Texto (propuesta, basada en `dl_what`) | Captura |
|---|---|---|---|
| a | Fes l'examen al mòbil | Preguntes del reglament de Futbol amb Botons, amb temps. Funciona sense internet. | C0 Inici o C3 Normes (§7) |
| b | Sabràs què has fallat | En acabar, veus el resultat, les preguntes fallades i la resposta correcta. | C8 Resultat aprovat |
| c | Mostra la credencial | Si aproves, la credencial queda al mòbil i la pots ensenyar a un organitzador. | C11 Credencial, recortada **por encima del QR** |

- Móvil: `display:flex; gap: var(--s4); align-items:flex-start` con la captura a la izquierda
  (`flex: 0 0 7.5rem`). Al 200 % o en pantallas de menos de 22rem se pasa a columna
  (`flex-wrap: wrap`; el texto con `flex: 1 1 12rem`).
- Desde 60rem: rejilla de 3 columnas, `gap: var(--s6)`, captura arriba a `max-inline-size:
  16rem`.
- Separación entre filas: `--s6`. No son tarjetas: van sobre `--bg` sin contenedor.

### 6.5 Pasos de instalación (`ol.steps`)

Aquí sí hay secuencia y los números aportan información.

- `<ol>` con `list-style:none` y `counter-reset`. Cada `<li>` es una rejilla
  `grid-template-columns: 40px 1fr; column-gap: var(--s4)`.
- **Número = el botó**: disco de 40 px, `--brand-soft`, cifra 18/700 `--on-brand-soft`,
  centrada. Debajo, un filete vertical de 2 px `--hairline` que une los discos (la línea de cal
  de la página clara), con `aria-hidden` (es un pseudo-elemento).
- Texto del paso: `dl_step1..3` (04 §7.8), en `--t-body`. La acción que se nombra va entre
  «cometes» y en negrita 600 («Instal·lar»), como la ve el usuario.
- Separación entre pasos: `--s5`.
- **Aviso «atura't»** (`dl_threat`): bloque `--warn`/`--on-warn`, `--r-md`, padding 16 × 20,
  `display:grid; grid-template-columns: 24px 1fr; gap: var(--s3)`, icono `warning` de 24 px
  (decorativo, porque el texto ya empieza por «Si el mòbil…») y primera frase en 700. Es el
  ámbar de los avisos de la app, igual en los dos temas. No es `role="alert"`: es contenido
  estático.
- `dl_unexpected`, `dl_vary` y `dl_why` en `--t-small` `--ink-muted`, debajo del aviso.
- Botón **primario** repetido (§6.2), para que quien lee los pasos no tenga que volver a subir.

### 6.6 Desplegables (`<details>`): «Comprovació avançada» y «Privacitat»

- Contenedor: `--surface` (o `--bg` si la sección es `--surface`), borde de 1 px `--hairline`
  y `--r-md`.
- `summary`: `min-block-size: 56px`, padding 16 × 20, 18/600 `--ink`, `display:flex;
  justify-content:space-between; align-items:center`. Se oculta el marcador nativo
  (`list-style:none` y `::-webkit-details-marker{display:none}`) y se pone el icono
  `expand_more` de 24 px, que gira 180° con `[open]` (150 ms; sin transición si se pide reducir
  el movimiento). El foco va sobre el `summary` (§6.2).
- **Hash y huella**: `<dl>`. Etiqueta en `--t-small` `--ink-muted`. Valor en `<code>` Mono
  `--t-mono`, sobre `--bg`, `--r-sm`, padding 12, con `overflow-wrap:anywhere` y
  `user-select:all`: un toque largo selecciona el valor entero, sin JS. CI lo estampa **agrupado
  de 4 en 4 caracteres** con espacios finos (U+2009) para compararlo a ojo, en un `<span>` por
  grupo o, sin marcado extra, con `word-spacing`. [PV] Comprobar que al copiar no se arrastran
  los espacios. Si se arrastran, mostrarlo sin agrupar.
- **Sin botón «Copiar»** (`dl_copy` exige JS): la nota `dl_adv_note` basta.

### 6.7 Sección de organizadores (`#verificar`)

Objetivo visual: que el organizador **reconozca la dirección** y se lleve el verificador a su
pantalla de inicio, no que se acostumbre a pulsar enlaces.

```
 Per als organitzadors                                   H2
 (◎ qr_code_2)  Comprova una credencial des del          insignia 48 px --brand-soft
                navegador del mòbil. Un cop preparat,     + --t-body
                funciona sense connexió.
 Adreça oficial del verificador                          etiqueta --t-small --ink-muted
 ┌──────────────────────────────────────────────┐
 │ arbitratge-app.futbolbotons.cat/verificador/ │        caja: --surface, borde 1 px --outline,
 └──────────────────────────────────────────────┘        --r-sm, padding 12×16, Mono 18/600
 ┌──────────────────────────────────────────────┐
 │            Obre el verificador               │        secundario (§6.2)
 └──────────────────────────────────────────────┘
 (i) Instal·la'l a la pantalla d'inici i, després,      --t-small --ink-muted, icono info 20
     obre'l sempre des de la icona o escrivint
     l'adreça, no des d'enllaços rebuts per missatge.
 Aquesta marca ajuda a detectar canvis casuals; no és una prova contra un dispositiu
 modificat o una clau extreta.                           --t-small (ADR 0001 / 05 §6.3)
```

- Fondo `--surface-2` a sangre: separa por tono el público «organizador» del «candidato».
- La dirección va en una caja **de texto, no de enlace** (`<p>` con `<code>`) y se puede
  seleccionar. Es el patrón de la PWA (`.host-big`, `index.html:44`) y prepara al organizador
  para compararla.
- **Variante A (recomendada, la que pide el usuario):** botón secundario «Obre el verificador»
  con `href="/verificador/"` (ruta relativa al mismo origen, **sin** `target="_blank"`). Se usa
  el secundario y no el primario porque en esta página solo hay una acción principal, que es
  descargar.
- **Variante B (si se mantiene 04 §6.1 M1):** sin botón; solo la caja con la dirección y la
  frase «Escriu aquesta adreça al navegador». El resto es igual. El CSS no cambia.
- Insignia: icono `qr_code_2` en un disco de 48 px `--brand-soft`, decorativo. **No** se usa el
  icono actual de la PWA (`web/verificador/icons/icon.svg`): es un *check* en un círculo, y la
  marca §9 prohíbe los *checks* de seguridad (Q5).

### 6.8 Pie

- Fondo `--surface-2` (sigue la sección anterior), separado por un divisor de 1 px
  `--hairline`, padding `--s7 --gutter`, texto `--t-small` `--ink-muted`.
- Contenido: `dl_official` («Descarrega l'app només des de l'adreça que t'ha donat el CFBB.»),
  «Adreça d'aquesta pàgina: arbitratge-app.futbolbotons.cat» (estampada por CI, en Mono),
  contacto del CFBB (pendiente, 05 Q12) con icono `mail` y `<details>` «Privacitat»
  (`dl_privacy` + `dl_privacy_body`) o un enlace a `privacitat.html` según Q3.
- La marca no se repite en el pie: ya está arriba. Solo el texto «CFBB · Futbol amb Botons».

---

## 7. Capturas de la app

- **Sin marco de dispositivo.** Nada de biseles, *notch* ni móviles 3D de *stock*. Cada captura
  es una «pantalla» desnuda: `border-radius: var(--r-lg)`, borde de 1 px `--hairline`, fondo
  `--bg`, sin sombra (superficies planas) y `aspect-ratio: 9 / 16` con `object-fit: cover;
  object-position: top`.
- **Origen**: emulador 1080 × 2400, fuente al 100 %, banco de muestra (`bank-sample`), build de
  depuración. Se recortan la barra de estado y la píldora de navegación. Se hace un juego en
  claro y otro en oscuro.
- **Prohibido en una captura pública:** el banco real (ADR 0002) y cualquier QR que el
  verificador de producción pueda dar por válido. Por eso la credencial (c) se recorta por
  encima del QR. Las capturas de preguntas se evitan: mostrarían «[FICTICI] …» del banco de
  muestra, que en una página pública parece de broma, o arriesgarían contenido real. Solo se
  incluirían si `bank-curator` aprueba una pregunta pública y creíble.
- Formato: WebP de calidad ~80, a 480 px y 720 px de ancho (`srcset` 1x/2x para 120 px y
  240 px de visualización), ≤ 40 KB cada una. Con `width`/`height` declarados (evita el CLS),
  `loading="lazy"` y `decoding="async"`.

```html
<figure class="shot">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="img/app-resultat-fosc-480.webp 480w, img/app-resultat-fosc-720.webp 720w" sizes="(min-width: 60rem) 16rem, 7.5rem">
    <img src="img/app-resultat-clar-480.webp" srcset="img/app-resultat-clar-480.webp 480w, img/app-resultat-clar-720.webp 720w" sizes="(min-width: 60rem) 16rem, 7.5rem"
         width="480" height="853" loading="lazy" decoding="async"
         alt="Pantalla de resultat de l'app: 4 encerts de 5 i el missatge «Has aprovat».">
  </picture>
</figure>
```

- `alt` descriptivo y breve. Las capturas cuentan algo que el texto de al lado solo resume, así
  que **no** son decorativas.
- Se regeneran en cada cambio visual de la app. Mientras no existan, el bloque se publica sin
  capturas (las filas funcionan solo con texto).

---

## 8. Iconografía

- **Material Symbols Rounded**, peso 500, relleno 0 y tamaño óptico 24: la misma familia que el
  APK (`visual-review-app-m1.md` §2.7). Licencia Apache 2.0. Se copian como `<symbol>` en un
  sprite `<svg hidden>` al principio del `<body>` y se usan con `<svg class="i"
  aria-hidden="true" focusable="false"><use href="#i-download"/></svg>`. Funciona sin JS y la
  CSP lo permite.
- Juego cerrado: `download`, `task_alt`, `menu_book`, `id_card`, `wifi_off`, `warning`, `info`,
  `qr_code_2`, `expand_more`, `mail`.
- Tamaños: `--i-md: 24px` (botones, avisos, desplegables) y `--i-sm: 20px` (notas). En `em`
  dentro del texto (`1.2em`), para que escalen con el zoom. `fill: currentColor`.
- Todos los iconos acompañan a un texto que ya lo dice: siempre `aria-hidden`.
- **Ningún emoji ni dingbat**: sustituyen a `⬇ ⚠ ▸ ⓘ` del *wireframe* de 04 §6.3 y no deben
  copiarse de la PWA (`📷 🖼 📋 💡 ⚠ ⓘ ←`).

---

## 9. Estados y casos límite

| Estado | Qué se ve | Quién lo genera |
|---|---|---|
| Normal | Botones de descarga + metadatos estampados | CI (`pages.yml`) |
| **Descarga no disponible** (faltan el hash o la release; fallo cerrado de 05 §6.3) | En lugar de **cada** botón de descarga: bloque de 64 px de alto mínimo, `--surface` (en la banda: blanco al 10 % con borde de 2 px blanco al 60 %), icono `info` + `dl_unavailable`. El hueco del botón no cambia, así no se descoloca el carnet. Sin metadatos ni `<details>` de hash | CI, con plantilla alternativa |
| Visitante sin Android | No se detecta (sin JS). Frase fija bajo los metadatos: `dl_not_android` («Si no tens un mòbil Android, aquesta app no funcionarà.»), `--t-small` | Estático |
| Fuente que no carga | Fallback métrico (§3.1); la maquetación aguanta | — |
| Zoom 200 % / 400 % (320 CSS px) | Una columna; capturas encima del texto; el hash se parte (`overflow-wrap:anywhere`); botones de dos líneas centradas; **sin desplazamiento horizontal** | — |
| Modo de alto contraste de Windows (`forced-colors: active`) | Los botones conservan un borde de 2 px `ButtonText` (añadir `border: 2px solid transparent` a todos los botones para que aparezca); la banda pierde el fondo y se ve igual de bien | — |
| Impresión (`@media print`) | Sin la banda ni las capturas; los pasos, el hash y la dirección en negro sobre blanco. Es barato y útil para quien imprime las instrucciones | — |
| Enlace a `#verificar` | `scroll-margin-top: var(--s5)` en las secciones; `scroll-behavior: smooth` solo con `prefers-reduced-motion: no-preference` | — |

**Movimiento:** ninguno no solicitado. Sin animaciones de entrada ni *parallax*. Solo existen
la transición de color del botón (120 ms) y el giro del chevron (150 ms), y las dos se anulan
con `reduce`.

---

## 10. Conexión visual con `/verificador/`

La PWA es hoy **anterior a «Centre del camp»**: pila del sistema, `#F4F6F8`/`#1F2937`, cabecera
`#1d6f42`, radios de 10, foco azul `#0b57d0`, iconos emoji (`index.html:65-67,71,88`) y un icono
con *check* (`icons/icon.svg`). Al pasar de la landing a la PWA se nota el salto de identidad.

La landing **no** se rebaja a ese estilo. Se propone una tarea posterior para `pwa-developer`
(con revisión `ux-designer` + `ui-designer`, fuera de este alcance):

1. Sustituir los tokens de `styles.css:2-30` por los de §2.1 (mismos nombres de rol). Cabecera
   `.top` en `--pitch` y `theme-color` `#0F3D26`. Los colores de veredicto
   (`--r-valid/invalid/unknown/format`) **no se tocan** sin revisar sus contrastes y su
   semántica.
2. Sustituir los emojis por el mismo sprite de §8 (`photo_camera`, `image`, `content_paste`,
   `flashlight_on`, `warning`, `info`, `arrow_back`).
3. Un icono de PWA nuevo, sin *check*: por ejemplo, el disco de marca con un marco de QR en
   cal. Lo decide el usuario o CFBB (Q5).
4. ¿Atkinson en la PWA? Añadiría ~45 KB al *precache* y exigiría `font-src 'self'`. Se
   recomienda que sí, por la legibilidad de la dirección y del código (Q2).

Mientras tanto, la coherencia se apoya en lo que **sí** comparten: verde de marca, botones de
56 px con radio 10-14, cuerpo de 18 px y la dirección en monoespaciada grande (`.host-big`). La
caja de dirección de §6.7 repite el patrón de la PWA a propósito.

---

## 11. Preguntas abiertas (para el usuario / `ux-designer` / `release-engineer`)

| # | Pregunta | Propuesta por defecto |
|---|---|---|
| **Q1** | **Enlace landing → verificador.** 04 §6.1 (mapa M1) dice «**No**» (anti-suplantación §5.7); 05 §6.3 punto 5 dice que sí y el usuario lo pide. ¿Se actualiza 04 §6.1? | **Variante A** (§6.7): enlace de mismo origen, con la dirección visible en texto y el aviso «obre'l des de la icona». Si se mantiene M1, variante B sin cambiar el CSS. Hay que actualizar 04 §6.1 (`ux-designer`). |
| **Q2** | **Webfonts.** 04 §6.2 dice «sin webfonts»; se propone Atkinson **autoalojada**, sin terceros. Exige `font-src 'self'` en la CSP de 05 §6.3. | Sí, autoalojada. Actualizar 04 §6.2 y la CSP (`release-engineer`). |
| Q3 | Privacidad: `<details>` dentro de la página (04 §6.1) o `privacitat.html` (05 §6.2). | El diseño admite las dos. Se recomienda el `<details>`: una sola página. |
| Q4 | Imagen `og:image` para la vista previa de WhatsApp (04 §6.2: «sin imagen pesada»). | Opcional: PNG de 1200×630 ≤ 60 KB con la banda y la marca negativa, sin texto de llamada. Facilita reconocer el enlace oficial, pero un clon también lo puede copiar. |
| Q5 | El icono actual de la PWA es un *check* (marca §9 lo prohíbe en contexto de seguridad). ¿Se rediseña? | Sí, en la tarea de §10. |
| Q6 | La identidad es **provisional, pendiente de aprobación de CFBB** (marca, cabecera). Publicarla en una página pública la fija de hecho. | Publicar igualmente (solo es visual y se puede cambiar), o esperar a la aprobación si CFBB lo pide. Decide el usuario. |
| Q7 | Textos nuevos o unificados: entradilla, 3 títulos de «Què és», metadatos en frase, «Descarrega» frente a «Descarregar», textos del verificador y pie del carnet. | `ux-designer` los aprueba en bloque. No cambian el contenido de 04 §7.8. |

---

## 12. Consultas a `ui-ux-pro-max` (registradas)

Se leyeron `SKILL.md` y `references/pro-rules.md`. Este último tiene alcance de app nativa;
aquí se aplicaron sus reglas transversales: iconos SVG de una familia, contraste en los dos
temas, tokens semánticos, ritmo de 4/8 y medida de lectura. Se ejecutó desde la raíz del repo
con `python .pi/skills/ui-ux-pro-max/scripts/search.py`:

| # | Consulta | Resultado | Uso |
|---|---|---|---|
| 1 | `"app download landing page sports club official" --design-system` | *Hero-Centric* + *Vibrant & Block-based*, rojo `#DC2626`, Bebas Neue | **Descartado**: no encaja (juvenil y de afición; contradice la marca). Se reintentó con un enfoque más acotado. |
| 2 | `"accessible government service older adults" --design-system` | *Minimal Single Column*, estilo *Accessible & Ethical*, **Atkinson Hyperlegible**; evitar degradados lila/rosa y movimiento | **Adoptado**: patrón de una columna con una sola CTA y la tipografía. Su paleta azul marino se descarta en favor de la marca. |
| 3 | `"web font loading fallback layout shift" --domain ux` | `font-display: swap` + fallback similar; reservar espacio | §3.1 (fallback métrico, *preload*) |
| 4 | `"dark mode prefers color scheme contrast" --domain ux` | 4,5:1, no solo color | §2.2-2.3 |
| 5 | `"decorative icon aria hidden" --domain icons` | Semántica por contexto; una familia por superficie | §8 |
| 6 | `"focus visible keyboard link" --domain ux` | Anillo visible en todo; foco no tapado | §6.2, §5.1 (sin cabecera fija) |
| 7 | `"zoom 200 percent reflow text resize" --domain ux` | Alturas por contenido, sin recortes, `min(100%, 65ch)` | §3.2, §6.4, §9 |
| 8 | `"responsive images srcset picture aspect ratio" --stack html-tailwind` | Reservar espacio con `aspect-ratio`/dimensiones | §7 (aplicado en CSS plano, sin Tailwind) |
| 9 | `"details summary disclosure accessible" --domain ux` | Fuera de tema (resumen de errores de formulario) | **Sin coincidencia verificada**: §6.6 usa la pauta general del `<details>/<summary>` nativo. |
| 10 | `"reduced motion animation" --domain ux` | `prefers-reduced-motion`; evitar el movimiento excesivo | §9 |

`frontend-design`: se revisó el plan frente a sus rasgos de página generada. No hay crema con
serif y terracota, ni fondo negro con acento ácido, ni kit de tarjetas SaaS, ni etiquetas en
mayúsculas sobre cada título, ni metadatos «A · B · C» (§6.1), ni `→` en los botones. Se
corrigieron dos cosas: (1) la primera idea de un pie también en gespa se quitó, para gastar la
audacia en un solo sitio; (2) los metadatos de 04 §7.8 (`Versió {v} · {mb} MB · {data}`) se
proponen como una frase.

---

## 13. Lista de implementación (para `pwa-developer`), por impacto ÷ esfuerzo

| P | Cambio | Esfuerzo |
|---|---|---|
| P0 | `web/index.html` con la estructura de §5.1, CSS en línea con los tokens de §2.1/§3.2/§4 en `:root` y `@media (prefers-color-scheme: dark)` | S |
| P0 | Banda de gespa con la cal en SVG en línea, marca negativa, H1, entradilla, botón de tiza y metadatos (§6.1-6.2) | S |
| P0 | Pasos, aviso «atura't», primario repetido y `<details>` de hash y huella (§6.5-6.6) | S |
| P0 | Estado «no disponible» en la plantilla de CI (§9), coordinado con `release-engineer` | XS |
| P1 | Carnet de ejemplo en HTML/CSS con solape y `img/qr-exemple.svg` (§6.3) | M |
| P1 | Atkinson Next/Mono `woff2` en subconjunto, `OFL.txt`, *preload*, fallback métrico y `font-src 'self'` (§3.1) | S |
| P1 | Sección de organizadores, variante A o B según Q1 (§6.7) | XS |
| P1 | Sprite de iconos (§8) | XS |
| P2 | Capturas claro/oscuro en WebP (§7), cuando las produzca la QA de la app | S |
| P2 | `@media print` y `forced-colors` (§9) | XS |
| P2 | Alineación de la PWA (§10), como tarea aparte | M |

---

## 14. QA visual exigida después (AGENTS.md)

- Capturas de Chrome Android (emulador) a **360, 411 y 768 px**, y de escritorio a 1280 px; en
  **claro y oscuro**; con el zoom del navegador al **100 %, 130 % y 200 %**, más 320 CSS px
  (equivale al 400 %). Hay que comprobar: el solape del carnet sin tapar texto, la ausencia de
  desplazamiento horizontal, el hash partido, los botones de dos líneas centrados y el foco
  visible en todos los elementos con teclado.
- Contraste medido con herramienta (DevTools/axe) en los dos temas; los valores de §2.2 están
  calculados a mano.
- TalkBack en dispositivo físico [PV]: orden de lectura (banda → carnet como una figura con su
  leyenda → secciones). Los iconos y la cal no se anuncian; el carnet se anuncia solo por la
  `figcaption`.
- Glifos catalanes con la fuente subconjuntada: `à è é í ï ò ó ú ü ç l·l ŀ ’ « »` [PV].
- Peso total de la página sin capturas ≤ 100 KB (HTML+CSS ~15 KB, fuentes ~65 KB, SVG ~10 KB)
  [PV].

---

## 15. Riesgos residuales

- **Q1 abierto:** si se mantiene M1, el enlace al verificador no puede publicarse. La variante
  B no cambia el diseño.
- **Webfont contra 04 §6.2:** hasta que se acepte Q2, la página tendría que salir con la pila
  del sistema. El diseño aguanta, pero pierde parte de su carácter y la distinción `8/B` del
  hash.
- **El carnet de ejemplo puede tomarse por real** o escanearse. Lo mitigan el chip «Exemple»,
  la leyenda y un QR de texto plano que no es ARB1. No queda eliminado.
- **Identidad provisional** (Q6): publicarla la consolida antes de que CFBB la apruebe.
- **Salto visual con la PWA** hasta que se haga la tarea de §10.
- Contrastes, peso de las fuentes y métricas del fallback **calculados o estimados, no
  medidos**. Ninguna afirmación de disposición vale sin capturas (checklist de `reviewer`).
