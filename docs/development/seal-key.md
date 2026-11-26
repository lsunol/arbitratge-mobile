# Clave de sello ARB1 de producción

Par ECDSA P-256 (ES256) del ADR 0001. Sirve **solo** para sellar credenciales y verificarlas:

| Parte | Qué hace | Dónde vive | ¿En git? |
|---|---|---|---|
| **Privada** (PKCS#8 DER) | El APK la lleva embebida y **firma** cada credencial emitida | `~/.arbitratge-seal/<kid>.der` (Windows: `C:\Users\lluis\.arbitratge-seal\`) | **Nunca** |
| **Pública** (SPKI) | La PWA verificadora y el APK **verifican** con ella, según el `kid` de la credencial | `trusted-keys/<kid>.json` → copiada a `web/verificador/keys/` en el despliegue | Sí |

No es la clave de firma del APK (ese es el keystore `dev.lluissunol`, skill `lluissunol-apk-signing`)
ni tiene relación con el banco de preguntas.

Clave actual: **`kid = s1`**, creada el 2026-11-24, huella SPKI
`35fd2d522cc5bbb84136dff6e5d3b72381d8ff89042374c80be792beefef20bf`.

## Cómo se generó

```bash
node scripts/gen-seal-key.mjs s1 --valid-from 2026-11-24
```

El script (`scripts/gen-seal-key.mjs`, Node ≥ 20, sin dependencias):

1. Genera un par P-256 con `crypto.generateKeyPairSync`.
2. Escribe la privada en `~/.arbitratge-seal/s1.der` (PKCS#8 DER). Se niega a escribir dentro del repo.
3. Escribe `trusted-keys/s1.json` (`kid`, `alg: ES256`, `spki` base64url, `sha256_spki`, `status: active`, `valid_from`).
4. Nunca sobrescribe una clave existente ni imprime la privada. Rechaza kids `test`, `testx`, `dev*`.

Opciones: `--priv-dir <dir>` (otra carpeta privada) y `--valid-from YYYY-MM-DD`.

## Cómo se usa

- **Build release del APK** (variables de entorno, ver `app-candidate/build.gradle.kts`):
  `ARB_SEAL_KEY_FILE=~/.arbitratge-seal/s1.der ARB_SEAL_KID=s1` (junto con `ARB_BANK_FILE`).
- **Despliegue de la web:** `stamp-web --production` exige que `web/verificador/keys/` sea idéntico
  a `trusted-keys/` (se regenera con `web/verificador/tools/gen-manifest.mjs`).
- La pública debe estar publicada en la PWA **antes** de distribuir un APK que firme con ese `kid`.

## Copia de seguridad (obligatoria)

La privada no está en ningún otro sitio. Si se pierde no se puede emitir con `s1`; habría que crear un
`kid` nuevo (`s2`) y publicar un APK nuevo. Las credenciales ya emitidas siguen verificando mientras
`s1.json` siga en `trusted-keys/`. Guarda una copia cifrada fuera del equipo (gestor de contraseñas
o disco cifrado). Nunca por email, chat, issue ni commit.

## Rotación o compromiso

1. `node scripts/gen-seal-key.mjs s2` (nuevo `kid`).
2. Publicar `s2.json` en la PWA y, después, un APK con `ARB_SEAL_KID=s2`.
3. Si `s1` se compromete: cambiar su `status` a `compromised` en `trusted-keys/s1.json` (la PWA
   rechazará lo firmado con él). Si solo se retira: `retired`. Cambios en `trusted-keys/` pasan
   por revisión (CODEOWNERS).
