# Tests de la landing (Node >= 20, sin dependencias)

```bash
node web/test/site-static.mjs   # CSP, sin JS, contenido catalán, enlaces, aislamiento del verificador, estampado fail-closed
node web/test/browser-qa.mjs    # Chrome headless con emulación móvil (mobile:true): 320/390 × 100/130/200 % × claro/oscuro (+1280 escritorio)
node web/test/browser-qa.mjs --android [serial]   # EMULADOR/dispositivo Android real vía adb: font_scale nativa; genera emu-*.png y emu-qa-metadata.json
```

## Artefacto de Pages (`scripts/stamp-web.mjs`)

Exactamente un modo (`--preview` o `--production`). Salida: solo `<repo>/_site` (`.gitignore`) o un directorio **nuevo** bajo `os.tmpdir()`.

```bash
# Vista previa (hoy: sin release firmada ni claves de producción)
node scripts/stamp-web.mjs --preview --version 1.2.3 --version-code 102039 \
  --apk-url https://github.com/lsunol/arbitratge-mobile/releases/download/v1.2.3/<fichero>.apk \
  --sha256 <64 hex> [--cert-sha256 <64 hex>] [--published YYYY-MM-DD] [--bank-version <id>] \
  [--release-notes-url https://github.com/lsunol/arbitratge-mobile/releases/tag/v1.2.3] [--out _site]

# Producción (CI): exige además --cert-sha256, --published, --bank-version, --release-notes-url,
# --release-metadata y una lista de claves de producción no vacía
node scripts/fetch-release-metadata.mjs --tag v1.2.3 --out release.json   # API pública de GitHub + SHA-256 real del APK
node scripts/stamp-web.mjs --production --version 1.2.3 --version-code 102039 \
  --apk-url https://github.com/lsunol/arbitratge-mobile/releases/download/v1.2.3/<fichero>.apk --sha256 <hex> \
  --cert-sha256 <hex> --published YYYY-MM-DD --bank-version <id> \
  --release-notes-url https://github.com/lsunol/arbitratge-mobile/releases/tag/v1.2.3 --release-metadata release.json
```

- `--version` = X.Y.Z final canónico; `--version-code` = X·100000 + Y·1000 + Z·10 + 9 (05 §7.1; Y, Z ≤ 99; ≤ 2 100 000 000).
- `--bank-version` = `[0-9A-Za-z._+-]{1,24}`.
- `--production` exige `verificador/keys/*.json` idénticas byte a byte a `trusted-keys/*.json` (mismo conjunto); `--preview` no.
- Lista blanca de ficheros explícita (landing, `brand/`, `fonts/`, verificador fichero a fichero + claves validadas); sin enlaces simbólicos.
- Navegador manual: `python -m http.server 8092 -d _site`
- Pilot sin dominio propio: añade `--host lsunol.github.io/arbitratge-mobile` (landing y `latest.json` muestran esa URL; no se publica el `CNAME`)..
