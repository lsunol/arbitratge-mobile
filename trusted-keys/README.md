# trusted-keys/

Lista pública de claves de verificación ARB1 (`docs/design/02` §3.7.9). **Vacía a propósito**: aún no hay
clave de sello de producción. Cada clave será un fichero `<kid>.json`
(`kid`, `alg: "ES256"`, `spki`, `sha256_spki`, `status`, `valid_from`) cuyo nombre coincide con el `kid`.

- Solo claves **públicas**. Nunca privadas, PKCS#8 ni material de firma.
- **Nunca** `test`, `testx` ni `dev` (viven en `test-vectors/`).
- La PWA (`web/verificador/keys/`) y el APK (G2) se comprueban contra estos ficheros; el despliegue copia
  estos ficheros a `web/verificador/keys/` y los publica antes del APK que los usa (§3.7.9, `05` §5.5).
