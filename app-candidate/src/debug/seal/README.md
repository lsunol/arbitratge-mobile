# Clau de sello `kid=dev` (NOMÉS DEBUG, pública per disseny)

- `dev-seal-key.der`: clau privada P-256 en PKCS#8 DER. **Pública per disseny** (ADR 0001/0002: la
  clau de sello viatja dins de l'APK). Només s'empaqueta a les variants `debug`.
- `dev-trusted.json`: entrada de confiança `trusted-keys/<kid>.json` (02 §3.7.9) corresponent.
  Una prova (`DevSealKeyTest`) comprova que correspon a la privada.
- La PWA verificadora oficial **no ha de confiar mai en `dev`**: no s'ha de copiar a
  `trusted-keys/` ni a `verificador/keys/`. Una credencial `kid=dev` surt «NO VÀLIDA» (UNKNOWN_KID).
- Es va generar una sola vegada amb `KeyPairGenerator("EC", secp256r1)`. No es regenera.
- Nom amb extensió `.der` perquè `.gitignore` bloqueja `*.pk8`/`*.key`; a l'APK és `assets/seal/seal-key.der`.
