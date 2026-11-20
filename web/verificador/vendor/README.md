# vendor/

## jsQR 1.4.0 — lector de QR (`jsQR.js`)

- Origen: paquete npm `jsqr@1.4.0` (<https://github.com/cozmo/jsQR>), fichero `dist/jsQR.js` **sin modificar**
  (build UMD de webpack; define `self.jsQR`).
- Licencia: Apache-2.0 → `LICENSE-jsQR.txt` (copia del paquete).
- Integridad del paquete (npm `dist.integrity`):
  `sha512-dxLob7q65Xg2DvstYkRpkYtmKm2sPJ9oFhrhmudT1dZvNFFTlroai3AWSpLey/w5vMcLBXRgOJsbXpdN9HzU/A==`
- SHA-256 de `jsQR.js`: `ebc40c8a15196236b2314db0856f72ca0b49980cd5413b8c852a7349f5fee0859`
- Sin `eval`, sin `new Function`, sin red. Se carga con `<script src="vendor/jsQR.js">` (CSP `script-src 'self'`).
- Se usa solo como alternativa cuando el navegador no tiene `BarcodeDetector` (o este no encuentra el QR).
- Último release del proyecto: 2020; es estable y sin dependencias. Vigilar la alternativa `zxing-wasm`
  si se detectan QR reales que no lee (ver `docs` de la PWA, riesgos).
