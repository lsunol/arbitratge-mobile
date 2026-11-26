# keys/ — lista de confianza desplegada (solo claves públicas)

- `index.json` = `{ "files": ["<kid>.json", …] }`. Cada `<kid>.json` es una copia **byte a byte** de
  `trusted-keys/<kid>.json` (esquema de `docs/design/02` §3.7.9). El despliegue (`pages.yml`) las copia aquí.
- Hoy contiene la clave de producción `s1` (ver `docs/development/seal-key.md`). Con la lista
  vacía el verificador respondería `NO VÀLIDA` (`UNKNOWN_KID`) a todo y avisaría en pantalla.
- **Nunca** `test`, `testx`, `dev` ni otras claves de prueba aquí (R01/K02 y G3 lo comprueban; `test/check-static.mjs`
  también). Las claves de prueba viven en `test-vectors/` y solo las usan los tests y `test/serve-test-build.mjs`.
- El nombre de fichero debe ser el `kid` (`^[a-z0-9]{1,16}$`); si no coincide, la entrada se descarta.
- Tras cambiar este directorio: `node web/verificador/tools/gen-manifest.mjs` (regenera `precache-manifest.json`) y
  subir la versión en `sw.js`.
