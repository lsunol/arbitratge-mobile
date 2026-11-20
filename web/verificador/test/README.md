# Tests del verificador (Node >= 20, sin dependencias)

```bash
node web/verificador/test/run-vectors.mjs   # G1 (JS): test-vectors/cases.json, código exacto por caso (178/178)
node web/verificador/test/unit.mjs          # base64url, JSON restringido, normalización, carga de claves
node web/verificador/test/check-static.mjs  # manifiesto al día, CSP, sin inline/red externa, keys/ sin claves de prueba
```

Navegador: `node web/verificador/test/serve-test-build.mjs --port 8091` sirve una **copia** con las claves de
PRUEBA (`test-vectors/`) y el manifiesto regenerado (`--sw-version 0.1.1` para probar la actualización;
`--prod` sirve la carpeta real, con la lista de producción vacía). `fixtures/` = QR de casos públicos.

`test/` y `tools/` no forman parte del precache; el despliegue (`pages.yml`) debe copiar solo los ficheros
del manifiesto (+ `precache-manifest.json`, `sw.js`, `vendor/LICENSE-jsQR.txt`).
