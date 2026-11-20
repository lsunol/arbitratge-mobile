# Arbitratge Mobile

Aplicació Android offline per examinar i formar candidats a àrbitre de **Futbol amb Botons**,
juntament amb una PWA pública per verificar les credencials i una pàgina mínima de descàrrega
del APK.

## Estat

La fase de **disseny documental** està tancada. Encara no hi ha codi d'aplicació implementat.

- Context i instruccions per als agents: [`AGENTS.md`](AGENTS.md)
- Pla i entregables: [`docs/design/README.md`](docs/design/README.md)
- Decisions d'arquitectura:
  - [ADR 0001 — credencial i verificador públic](docs/adr/0001-credencial-sellada-y-verificador-publico.md)
  - [ADR 0002 — banc privat al build](docs/adr/0002-banco-privado-en-build-y-apk.md)
- Tancament del disseny: [`docs/design/reviews/cierre-diseno.md`](docs/design/reviews/cierre-diseno.md)

## Principis fixats

- Examen completament local, autoservei i sense backend.
- UI, missatges i contingut per a l'usuari en català.
- Correcció local amb feedback educatiu per pregunta.
- PDF/imatge amb QR signat segons el contracte `ARB1`.
- PWA verificadora pública, instal·lable i sense secrets.
- Banc real fora del repositori públic durant el desenvolupament, injectat al build i present
  dins de l'APK final.
- GitHub Pages i GitHub Releases per distribuir el web, la PWA i l'APK.

## Privacitat i seguretat

No afegiu mai claus privades, keystores, tokens, respostes, preguntes reals ni explicacions del
banc al repositori. `.gitignore`, controls de CI i la revisió humana s'han de mantenir abans de
cada publicació.

## Propers passos

1. Crear l'estructura Gradle/Android.
2. Implementar el contracte `ARB1` i els vectors compartits.
3. Implementar la PWA verificadora i la web de descàrrega.
4. Automatitzar CI, signatura i publicació.
5. Executar les proves [PV] indicades al tancament del disseny.
