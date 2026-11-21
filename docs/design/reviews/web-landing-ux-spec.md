# Spec UX previa — web pública de dos partes (`/` descarga + `/verificador/`)

Fecha: 2026-11-21 · Autor: `ux-designer` · Estado: propuesta para aceptar antes de implementar `web/index.html`.
No toca ARB1 ni ADRs. Idioma de la UI: catalán; documento: castellano.

## 0. Fuentes y búsquedas Pro Max

Leído: `AGENTS.md`, SKILL + `references/pro-rules.md` de `ui-ux-pro-max`, ADR 0001, `04-ux.md` §5.7, §6, §7.8, §8, `05-distribucion.md` §6.1–6.4, y `web/verificador/` (`index.html`, `manifest.webmanifest`, `styles.css` tokens).

Búsquedas ejecutadas (`search.py`):
1. `"app download landing page single CTA install" --domain product` → útil solo el patrón «Hero + Install» (Open Source Landing); estilo/paleta propuestos descartados (marca CFBB manda).
2. `"mobile landing page older users large text CTA" --domain ux` → resultado off-topic salvo *Back Button* (no romper el historial: usar enlaces normales, sin JS de navegación). Sin coincidencia verificada para «usuarios mayores»; se aplica 04 §8.2 como fallback.
3. `"accessible disclosure details summary" --domain ux` → off-topic (error summary/ARIA labels); se usa `<details>` nativo con foco visible (fallback documentado).
4. `"landing hero cta static html" --stack html-tailwind` → sin valor (Tailwind no aplica; HTML+CSS en línea).
5. `--design-system "download app trust landing"` → patrón Conference/Swiss, paleta azul/ámbar: **rechazada**; se conserva Swiss-minimal solo como criterio (rejilla simple, mucho aire).

## 1. Decisión de arquitectura de información y conflictos con docs vigentes

IA confirmada por el usuario: `/` = landing/descarga con CTA APK; enlace a `/verificador/` para cualquiera que verifique.

**Conflicto a registrar (no es ADR):** `04` §6.1 (mapa M1) decía «Web de descarga → PWA: **No**». `05` §6.3 punto 5 ya incluía un enlace al verificador. La decisión del usuario sustituye a 04 §6.1. Consecuencias y mitigación:
- El enlace sale **de la dirección oficial a otra ruta del mismo origen**, no de un mensaje externo: no empeora T15, siempre que el verificador siga mostrando V10 («compara l'adreça») y no se presente `/` como autenticador de nada.
- Hay que **actualizar 04 §6.1 y §7.8 (claves `dl_*`) y la nota de 05** en el mismo PR que implemente (pendiente de aprobación del usuario; yo no edito esos docs aquí).
- Textos: la landing **no** dice «verificador oficial/seguro»; dice dónde está y para quién es.

Segundo conflicto menor: 04 §6.1 pide privacidad en `<details>` dentro del HTML; 05 §6.2/6.3 prevé `privacitat.html`. **Recomendación:** `<details>` en el mismo HTML (el usuario pide una página simple; cero ruta extra). Si el responsable legal exige página propia, enlazarla desde el mismo bloque.

## 2. Principios de la página

- Una sola acción primaria visible: **Descarregar l'app**. Secundaria, claramente subordinada: **Verificar una credencial** (otro público: organizadores/terceros).
- Dos audiencias, dos «puertas» etiquetadas por rol, no por tecnología: «Sóc aspirant» / «Vull verificar una credencial».
- Sin JS necesario, sin analítica, sin cookies, sin webfonts, sin imágenes pesadas (logo SVG inline). Pila del sistema.
- Tono: llano, tranquilo, sin urgencia ni «segur/100 % garantit».

## 3. Navegación

No hay barra de menú ni hamburguesa (una página corta). Orden de contenido = orden de lectura = orden de foco.

```
[Skip link «Salta al contingut» (visible al foco)]
<header>  Logo CFBB + «Arbitratge CFBB»      (no es enlace/menu)
<nav aria-label="Seccions">  enlaces de ancla en una línea, solo ≥ 640 px (opcional):
        Instal·lar · Verificar · Privacitat
<main>
  1 Hero + CTA
  2 Què és (3 puntos)
  3 Com instal·lar-la (pasos)
  4 Comprovació avançada (details)
  5 Verificar una credencial (tarjeta)
<footer> adreça, privacitat (details), contacte, versió
```

- En móvil (<640 px) no hay nav: la página es lineal; el enlace a verificador aparece **dos veces** (tarjeta §3.5 y línea breve bajo el hero), ver §8.
- Ancla `#instal·lar` (id sin acento: `#installar`) para enlaces desde WhatsApp/ayuda de la app. Enlaces a `/verificador/` son `<a href="verificador/">` relativos, **misma pestaña**, sin `target=_blank` (back predecible).

## 4. Secciones, wireframe y microcopy (catalán)

Mobile 360 px, texto base 18 px, ancho de lectura ≤ 40 em, botones ≥ 56 px alto, separación ≥ 16 px.

```
┌───────────────────────────────────────┐
│ [logo] Arbitratge CFBB                │  header
├───────────────────────────────────────┤
│ Examen d'àrbitre de Futbol amb Botons │  h1 (28/32 px, 700)
│ Fes l'examen al mòbil, sense internet │  lead 20 px
│ i, si aproves, aconsegueix la teva    │
│ credencial d'àrbitre.                 │
│                                       │
│ ┌───────────────────────────────────┐ │
│ │ ⬇  Descarregar l'app (APK)        │ │  CTA primario, ancho completo
│ └───────────────────────────────────┘ │
│ Versió {v} · {mb} MB · {data}         │
│ Només Android 8.0 o superior          │
│ ⓘ No tens Android? Aquesta app no     │  (visible siempre; JS opcional lo
│   funcionarà al teu mòbil.            │   destaca si UA no es Android)
│                                       │
│ Vols comprovar una credencial d'un    │  enlace de texto subrayado, 48 px
│ àrbitre? → Obre el verificador        │  (SECUNDARIO, tras la CTA)
├───────────────────────────────────────┤
│ Què és?                               │  h2
│ • Examen del reglament, al mòbil.     │
│ • Es corregeix sola: veus què has     │
│   fallat i quina era la correcta.     │
│ • Si aproves, tens una credencial     │
│   que pots mostrar o enviar.          │
│ • Funciona sense internet.            │
├───────────────────────────────────────┤
│ Com instal·lar-la                     │  h2
│ [aviso fort] Descarrega l'app només   │
│ des d'aquesta adreça: {host}          │
│ 1 … 2 … 3 …  (textos dl_step1-3)      │
│ ⚠ dl_threat   · dl_unexpected         │
│ dl_vary · dl_why · dl_update          │
├───────────────────────────────────────┤
│ ▸ Comprovació avançada                │  details cerrado
├───────────────────────────────────────┤
│ Verificar una credencial              │  h2, tarjeta con borde
│ Per a organitzadors i qualsevol       │
│ persona que rebi una credencial.      │
│ Es comprova al teu navegador, sense   │
│ enviar cap dada.                      │
│ [ Obrir el verificador ]  (secundario │
│   outline, 56 px)                     │
│ ⓘ Si algú t'ha enviat un enllaç al    │
│   verificador, no el facis servir:    │
│   escriu l'adreça tu mateix o obre-la │
│   des d'aquesta pàgina.               │
├───────────────────────────────────────┤
│ footer                                │
└───────────────────────────────────────┘
```

### 4.1 Hero y CTA
| Clave | Texto |
|---|---|
| web_title | Arbitratge CFBB — Examen d'àrbitre de Futbol amb Botons |
| web_lead | Fes l'examen al mòbil, sense internet. Si l'aproves, aconsegueixes una credencial d'àrbitre. |
| dl_btn | Descarregar l'app (APK) |
| dl_meta | Versió {v} · {mb} MB · {data} |
| dl_req | Només per a Android 8.0 o superior. |
| dl_not_android | Si no tens un mòbil Android, aquesta app no funcionarà. |
| web_verify_hint | Vols comprovar una credencial? **Obre el verificador** |

CTA `aria-label` = texto visible + « (fitxer APK, {mb} MB)». El archivo se anuncia como descarga: atributo `download` no obligatorio.

### 4.2 Què és (reutiliza `dl_what` partido en 4 viñetas; mismo contenido, mejor escaneabilidad para mayores).
Viñetas, no párrafo largo; máx. 2 líneas cada una.

### 4.3 Instalación
Reutilizar tal cual `dl_how_title`, `dl_step1–3`, `dl_threat`, `dl_unexpected`, `dl_vary`, `dl_why`, `dl_update` de `04` §7.8 (ya revisados I10). Cambios de presentación: pasos como `<ol>` con número grande (32 px) en círculo, un paso por tarjeta; `dl_threat` en caja ámbar con icono SVG «stop» + texto («Atura't»), **no** solo color. Capturas reales Android 12–15 como mejora posterior [PV]; con `alt` descriptivo y `width/height` fijos (CLS).
Aviso dirección: `web_official`: «Descarrega l'app només des de l'adreça que t'ha donat el CFBB. Adreça d'aquesta pàgina: {host}.» (host escrito por CI desde CNAME; sin JS. Aclarar que **no autentica**, igual que 04 §5.7).

### 4.3b Mobile / mayores: flujo recomendado
1. Abre enlace en WhatsApp → Chrome. Ve título + CTA sin scroll (hero ≤ 1 pantalla a 360×640).
2. Toca CTA → Chrome avisa («este tipo de archivo puede dañar») → pasos ya explicados abajo; **añadir bajo el botón** una línea: «Després de descarregar, segueix els 3 passos de sota» + enlace a `#installar`. Evita que un mayor se quede en la descarga sin saber qué hacer.
3. Tras descargar, el usuario vuelve a la página: el paso 2 es la guía. Sin popups, sin modales, sin temporizadores.
4. Un solo scroll vertical; sin acordeones en los pasos (solo en «Comprovació avançada» y «Privacitat»).

### 4.4 Comprovació avançada (`<details>`, cerrado)
`dl_adv_title`, `dl_sha`, `dl_copy`, `dl_adv_note`, `dl_cert`. SHA y huella en `<code>` monoespaciado con `word-break:break-all`, tamaño ≥ 16 px, **seleccionables**. Botón «Copiar» solo con JS (`hidden` por defecto; si se muestra, `aria-live=polite` «Copiat»). Sin JS: el usuario selecciona el texto. Añadir línea «Com comprovar-ho» (una frase: «Compara aquest valor amb el que mostra una eina SHA-256 al teu dispositiu o ordinador»), y la advertencia de ADR 0001 **solo** en la sección del verificador/privacitat, no aquí (no mezclar).

Placeholders de CI (fallo cerrado, 05 §6.3):
| Placeholder | Uso |
|---|---|
| `{{APP_VERSION}}` `{{APK_MB}}` `{{PUBLISHED}}` | meta bajo el CTA |
| `{{APK_URL}}` | `href` del CTA (URL versionada de Releases, no `latest`) |
| `{{APK_SHA256}}` `{{CERT_SHA256}}` | bloque avanzado |
| `{{HOST}}` | aviso de dirección |
| `{{CONTACT}}` | footer/privacidad |
Si **cualquiera** de `APK_URL/APK_SHA256/APP_VERSION` queda sin sustituir (`{{…}}` literal), CI debe fallar el despliegue; el HTML de «no disponible» (§7) lo genera CI como variante de plantilla, nunca se publica una plantilla cruda con llaves visibles.

### 4.5 Sección «Verificar una credencial»
| Clave | Texto |
|---|---|
| web_vf_title | Verificar una credencial |
| web_vf_body | Per a organitzadors i qualsevol persona que rebi una credencial d'àrbitre del CFBB. Es comprova al teu navegador; no s'envia cap dada. |
| web_vf_btn | Obrir el verificador |
| web_vf_warn | Si algú t'ha enviat un enllaç al verificador, no l'obris: entra des d'aquesta pàgina o escriu l'adreça tu mateix. |
| web_vf_install | Pots instal·lar-lo com a icona al mòbil i fer-lo servir sense connexió (el verificador t'explicarà com). |
No prometer «oficial», «segur» ni mostrar candado/sello.

### 4.6 Footer
- `web_footer_host`: «Adreça d'aquesta pàgina: {host}»
- `dl_privacy` en `<details>` «Privacitat» (cuerpo `dl_privacy_body`; una frase añadida: el verificador comprueba en el dispositivo y no sube la credencial — verificar con pwa-developer, [PV] que no hay red de datos).
- Contacto `{contacte}` (pregunta abierta). Texto breve ADR: «Aquesta marca ajuda a detectar canvis casuals; no és una prova contra un dispositiu modificat o una clau extreta.» → en `<details>` «Límits de la verificació», debajo de la sección de verificador (no en el hero), para ser honestos sin asustar.

## 5. Diseño visual (alineado con 04 §8 y `web/verificador/styles.css`)

- Tokens: reutilizar los del verificador (`--bg #f4f6f8`, `--fg #1f2937`, `--brand #1d6f42`, `--brand-dark #145530`, `--muted #4b5563`, `--focus #0b57d0`; oscuro: bg `#111827`, brand `#7bd6a0`). Copiar los valores en el CSS en línea (la landing no depende de `verificador/styles.css`; CSP `style-src 'unsafe-inline'`).
- CTA: fondo `--brand`, texto `--on-brand`, radio 10 px, alto ≥ 56 px, ancho completo en móvil / máx. 420 px en escritorio; estado `:active` con oscurecimiento sin mover el layout; `:focus-visible` anillo 3 px `--focus` + offset 2 px.
- Secundaria (verificador): outline 2 px `--brand`, texto `--brand`, mismo alto → jerarquía por peso visual, no por tamaño.
- Iconos: SVG inline `aria-hidden="true"` con texto visible. Sin emoji estructurales (los ⬇/⚠ de wireframes son placeholders).
- Espaciado 8/16/24/40; secciones separadas por 40 px.
- Tipografía: pila del sistema; h1 28–32 px, h2 22–24, cuerpo 18, secundario ≥ 16; interlineado 1,5; alineación izquierda.
- Oscuro por `prefers-color-scheme`; probar contraste de cada par en ambos temas (CTA oscuro: texto `#0b1f14` sobre `#7bd6a0`).
- Movimiento: ninguno obligatorio; `scroll-behavior:smooth` solo bajo `prefers-reduced-motion: no-preference`.

## 6. Responsive

| Ancho | Comportamiento |
|---|---|
| 320–479 | 1 columna, CTA ancho completo, gutters 16 px, hash/huella con `break-all` sin scroll horizontal |
| 480–767 | Igual, gutters 24 px |
| 768+ | Contenedor centrado `max-width: 40rem` (lectura ≤ 40 em); hero + CTA centrados a la izquierda del contenedor; nav de anclas visible; tarjeta del verificador conserva ancho de contenedor (no 2 columnas: dos «puertas» lado a lado confundirían) |
| Zoom 200 % / fuente grande | Sin alturas fijas; nada se trunca; CTA crece en alto; sin `vh` en hero. Viewport sin `user-scalable=no` |
| Landscape móvil | Contenido en flujo normal; sin elementos sticky (evitar tapar contenido) |

No usar CTA sticky: tapa contenido con zoom/teclado y es innecesario en una página corta.

## 7. Estados, errores y fallbacks

| Estado | Comportamiento / texto |
|---|---|
| Sin JS | Página completa y funcional (ya sin JS). Botón «Copiar» oculto |
| Release no publicado / plantilla sin datos | CTA sustituido por tarjeta: «La descàrrega no està disponible ara mateix. Torna-ho a provar més tard.» + contacte. Bloque avanzado oculto. El verificador sigue enlazado |
| No Android (iOS/escritorio) | Aviso permanente `dl_not_android`; con JS mínimo opcional (sin dependencias) puede subir el aviso sobre el CTA. Nunca ocultar el CTA (puede abrirse desde un PC para pasar el enlace) |
| Descarga bloqueada por el navegador | Cubierto en `dl_step1`; añadir: «Si no es descarrega, torna-ho a provar o escriu al CFBB» |
| Alerta de amenaza Play Protect | `dl_threat` (detenerse) |
| Enlace APK 404 | Responsabilidad de CI: comprobar HTTP 200 de `{{APK_URL}}` antes de desplegar (`05` §4.4) y cabecera HTTP no controlable; texto de «no disponible» como arriba |
| Nueva versión | Misma página es la fuente; `dl_update` (con [PV] sobre conservar datos) |
| Enlace a `/verificador/` caído | Es parte del mismo sitio; si 404, es un fallo de despliegue → CI comprueba que `verificador/index.html` existe |
| Navegador viejo | HTML/CSS básico, sin `:has`, sin `clamp()` crítico; fallbacks `font-size`/`max-width` |
| Dominio no coincide con el comunicado | La página solo muestra `{host}` y la regla; no pretende detectar nada (04 §5.7) |

## 8. Ubicación del enlace al verificador

- **Posición 1:** línea de texto bajo meta del CTA (descubrible para quien llega a verificar, sin competir con el CTA). Enlace de texto subrayado, 48 px de alto táctil.
- **Posición 2:** tarjeta «Verificar una credencial» antes del footer (explicación + botón secundario + aviso).
- **Nunca** como botón del mismo color/tamaño que el CTA principal. En la app del candidato **no** hay enlace al verificador (04 §6.1 se mantiene para ese sentido).
- No abrir en nueva pestaña. El verificador, al instalarse como PWA con `scope: ./`, queda en `/verificador/` y no captura `/`.

## 9. Accesibilidad (WCAG 2.2 AA)

- `<html lang="ca">`; un `h1`, `h2` por sección, orden lógico; landmarks `header/nav/main/footer`; skip link.
- Contraste ≥ 4,5:1 texto, ≥ 3:1 bordes/iconos en ambos temas; verificar con herramienta (axe/Lighthouse) [PV].
- Enlaces distinguibles sin color (subrayado). Texto de enlace descriptivo («Obre el verificador», no «clica aquí»).
- Objetivos ≥ 48 px, separación ≥ 8 px; foco visible 3 px; foco no oculto por nada sticky.
- Orden de foco = visual. `<details>/<summary>` nativos (teclado y lector).
- Estados como texto, no solo color (ámbar + «Atura't»).
- TalkBack/Chrome: CTA anuncia «Descarregar l'app (APK), enllaç»; pasos como lista numerada (se anuncia «llista de 3 elements»); hash con `aria-label` no necesario (leer carácter a carácter es incómodo: ofrecer «Copiar»).
- `prefers-reduced-motion`; sin parpadeos.
- Texto ≥ 16 px; zoom 200 % sin pérdida (`reflow` a 320 CSS px).
- [PV] prueba con 3–5 usuarios mayores (04 §8.3): instalación y comprensión de las dos puertas.

## 10. SEO, metadatos y solapamiento con el manifiesto del verificador

`/` (landing):
- `<title>Arbitratge CFBB — Examen d'àrbitre de Futbol amb Botons</title>`; `<meta name="description">` ≤ 155 car.: «Descarrega l'app d'examen d'àrbitre de Futbol amb Botons del CFBB per a Android. Es corregeix sola i funciona sense internet.»
- `og:title`, `og:description`, `og:type=website`, `og:url` (URL canónica), `og:locale=ca_ES`; **sin imagen** (o SVG/PNG ≤ 100 KB si se quiere previsualización en WhatsApp: WhatsApp no renderiza SVG; decidir [pregunta]).
- `<link rel="canonical">`, `theme-color #1d6f42`, `color-scheme`, `viewport` sin límite de zoom.
- Indexación: público; `robots` permitido. Para **verificador** puede añadirse `<meta name="robots" content="noindex">` (herramienta, evita que un buscador lo lance como resultado y favorezca clones) — decidir con `pwa-developer` [recomendación].
- Favicon: reutilizar `verificador/icons/icon.svg` (no copiar; ruta relativa `verificador/icons/icon.svg`) o copia propia.

Manifiesto / SW:
- **La landing NO enlaza ningún `manifest.webmanifest`** y **no registra SW**: si lo hiciera, Chrome ofrecería instalar la landing y se pisaría el icono del verificador.
- Manifiesto del verificador: `scope`, `start_url`, `id` = `./` relativo a `/verificador/`. Con la landing en `/` no hay solape de scope. **Comprobar** que `sw.js` del verificador tiene scope `/verificador/` (no `/`), que no intercepta `/` y que la caché precache no incluye `/index.html` de la landing.
- Si el usuario abre la landing con el verificador instalado: no hay conflicto; la navegación de la landing a `/verificador/` abre la PWA o el navegador según el sistema (aceptable) [PV Android].
- Nombres distintos en pantalla de inicio: «Verificador CFBB» (short_name ya definido); la app APK se llama «Arbitratge CFBB» → no confundibles.
- CSP de la landing: `default-src 'none'; style-src 'unsafe-inline'; img-src 'self' data:; base-uri 'none'; form-action 'none'; object-src 'none'`. Si se añade el JS opcional (copiar/Android), usar fichero `'self'` con `script-src 'self'` y no inline; si la página debe ser **un único HTML**, prescindir de JS o usar hash CSP. Recomendación: **sin JS** (copiar = selección manual) para cumplir 05 §6.3 («sin JavaScript»).

## 11. Checklist de aceptación (para el `reviewer` y QA)

- [ ] Hero + CTA visibles sin scroll a 360×640; CTA ≥ 56 px, ancho completo.
- [ ] Un solo `<h1>`; skip link funcional; orden de foco correcto.
- [ ] Enlaces a `/verificador/` en dos sitios, secundarios, sin `target=_blank`, con aviso anti-enlace-recibido.
- [ ] Sin manifest ni SW en `/`; scope verificador solo `/verificador/`.
- [ ] Plantilla: ningún `{{…}}` visible; variante «no disponible» generada por CI.
- [ ] SHA-256 y huella en `<code>`, seleccionables, sin scroll horizontal a 320 px.
- [ ] Claro/oscuro, 130 %/200 % texto, landscape: **captura de dispositivo/emulador** adjunta (AGENTS.md: sin captura = «no verificado visualmente»).
- [ ] Contraste verificado con herramienta en ambos temas [PV].
- [ ] CSP aplicada y sin recursos externos/analítica/cookies.
- [ ] Textos en catalán idénticos a las claves de §4 y 04 §7.8; cambios de 04 §6.1 aprobados.
- [ ] TalkBack real en Chrome Android [PV].
- [ ] Instalación APK real en Android 12–15 con capturas [PV].

## 12. Riesgos

- Introducir el enlace al verificador desde la landing normaliza «abrir enlaces de verificador»; se mitiga con aviso, V10 y el principio «la dirección oficial es la que comunica el club», pero un clon puede copiar landing y enlace. Riesgo social aceptado (ADR 0001).
- Instrucciones de instalación cambian por fabricante y por la verificación de desarrolladores de 2027: re-probar en cada release.
- Previsualización de WhatsApp sin imagen puede quedar pobre (decisión de producto).
- Con CTA a Releases (github.com) el usuario sale del dominio del club: explicarlo en `dl_why`.

## Preguntas abiertas para el usuario

1. ¿Aceptas sustituir la regla de `04` §6.1 («landing no enlaza a la PWA») por el enlace secundario descrito, con el aviso anti-suplantación?
2. ¿Privacidad en `<details>` del mismo HTML (recomendado) o `privacitat.html` separada (05)?
3. ¿Contacto del club (email/teléfono/web) para footer y privacidad?
4. ¿Quieres imagen Open Graph (≤ 100 KB, PNG) para WhatsApp, o solo texto?
5. ¿`noindex` en `/verificador/` (recomendado) y landing indexable?
6. ¿Dominio definitivo (`arbitratge-app.futbolbotons.cat`) confirmado para mostrar `{host}` y `og:url`?
7. ¿Landing 100 % sin JS (recomendado, copiar manual) o permitir JS mínimo para «Copiar»/detección Android?
