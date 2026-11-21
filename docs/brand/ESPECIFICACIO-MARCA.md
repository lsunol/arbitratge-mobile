# Arbitratge — especificació de marca

> **Identitat provisional d'Arbitratge Mobile — pendent d'aprovació de CFBB**
> Cap fitxer d'aquest paquet és definitiu fins que CFBB l'aprovi.

Direcció visual: **«Centre del camp»** (ruta A, «Botó al centre»).

## 1. Nom

| Ús | Forma |
|---|---|
| Nom de l'app | **Arbitratge Mobile** |
| Marca escrita | **Arbitratge** |
| Subtítol opcional | **Futbol amb Botons** |

- Sempre en català i amb majúscula inicial. Dins la marca no s'escriu «ARBITRATGE» tot en majúscules.
- No s'hi afegeix el nom ni el logotip de cap federació sense el logotip oficial i el permís corresponent.

## 2. Símbol

Construcció sobre una graella de 64 u (u = 1/64 del diàmetre del disc):

| Element | Mida | Color |
|---|---|---|
| Disc de gespa | Ø 64 u | `#0F3D26` |
| Ratlla de mig camp (vertical, fins a la banda) | gruix 3 u | `#F2F5F3` |
| Cercle central | r 19 u, gruix 3 u | `#F2F5F3` |
| Botó | Ø 27 u, vora de 6 u, interior de gespa | `#F2F5F3` |
| Punt central (accent) | Ø 9 u | `#F2C230` |

- El símbol es dibuixa al 86 % del disc (graella interior de 64 u escalada a 55 u).
- Formes simples: només cercles i traços, sense filtres, màscares, degradats ni text. Es converteixen directament a VectorDrawable.
- **Monocroms** (`svg/mono/`): símbol sense disc, en una sola tinta (negre `#000000`, blanc `#FFFFFF` o verd `#1D6F42`) sobre transparent. La ratlla es redueix a dos trams.

## 3. Color

| Nom | Hex | Ús | Contrast de referència |
|---|---|---|---|
| Verd de gespa profund | `#0F3D26` | Fons de marca, disc, banda | cal sobre gespa **11,2:1** |
| Verd principal accessible | `#1D6F42` | Text secundari, tinta única, enllaços | sobre blanc **6,2:1** · sobre cal **5,6:1** |
| Cal | `#F2F5F3` / `#FFFFFF` | Traços del símbol, fons clars | tinta sobre cal **14,6:1** |
| Tinta | `#18241D` | Text principal | sobre blanc **16,0:1** |
| Groc targeta | `#F2C230` | Només el punt central i el filet de la credencial | sobre gespa **7,3:1** |

Totes les combinacions de símbol superen el 4,5:1 exigit. El groc no s'usa mai per a text sobre fons clar (1,6:1).

Equivalències en escala de grisos (luminància relativa): gespa `#353535`, verd `#616161`, groc `#C8C8C8`, cal `#F4F4F4`.

## 4. Tipografia

- **Atkinson Hyperlegible Next** (SIL OFL 1.1).
- Marca: Bold. Subtítol: Regular, espaiat +6 %.
- Als SVG de marca el text està convertit a un sol traçat per línia, sense dependència de font.
- Interfície: text de cos mínim de 16 px; etiquetes de 14 px.

## 5. Proporcions i àrea de seguretat

- **H** = alçada del símbol. **X = H/4**.
- Alçada de majúscula d'«Arbitratge» ≈ 0,4 H (cos de 38 u per a H = 64 u).
- Separació símbol → mot = **X**.
- Àrea de seguretat = **X** per tots els costats, també després del mot: l'espai abans i després d'«Arbitratge» és sempre el mateix.
- Amb subtítol, el bloc de text queda centrat verticalment respecte al símbol.

## 6. Mides mínimes

| Peça | Mínim |
|---|---|
| Icona (disc) | 24 px. Per sota, el favicon simplificat |
| Favicon simplificat | 16–32 px (`svg/favicon/favicon.svg`, sense cercle central) |
| Marca horitzontal | 160 px d'amplada o 35 mm |
| Banda de credencial | 210 × 31,2 mm (no es reescala per sota d'A5) |

## 7. Icona adaptativa Android

- `android/background.svg`: gespa `#0F3D26` amb la ratlla de mig camp a sang (108 × 108 dp).
- `android/foreground.svg`: cercle central, botó i punt, tot dins el cercle segur de 66 dp. Res toca els límits.
- `android/monochrome.svg`: capa per a icones temàtiques (Android 13+).
- Recursos llestos: `android/res/drawable/ic_launcher_{foreground,background,monochrome}.xml` i `android/res/mipmap-anydpi-v26/ic_launcher{,_round}.xml`.
- Provat amb màscares cercle, quadrat arrodonit, squircle i gota (vegeu el llenç).
- La ratlla viu al fons perquè, amb el paral·laxi del launcher, no se separi del símbol.

## 8. Credencial A4

- `credencial/credencial-banda-a4.svg`: banda de 210 × 31,2 mm (30 mm de gespa + filet groc d'1,2 mm).
- `credencial/credencial-banda-a4-grisos.svg`: la mateixa en escala de grisos.
- El QR va al cos del document, **mai dins del logotip**.
- La banda no porta segells, cadenats, hologrames simulats ni «checks»: la marca no ha de suggerir que la credencial és infalsificable.

## 9. Usos incorrectes

- Girar, inclinar, deformar o estirar el símbol.
- Canviar els colors de la paleta o fer del groc el color dominant.
- Afegir ombres, brillantors, degradats o filtres.
- Posar el QR, text o altres elements dins el símbol.
- Afegir segells, cadenats o «checks» de seguretat.
- Col·locar el símbol sobre fons sense contrast suficient (per exemple, sobre `#1D6F42`) o sobre fotografies.
- Fer servir banderes, escuts, pilotes convencionals o el nom d'una federació sense logotip oficial.

## 10. Fitxers

```
svg/arbitratge-icona.svg                  icona principal (disc), fons transparent
svg/mono/arbitratge-simbol-{negre,blanc,verd}.svg
svg/marca/arbitratge-marca-horitzontal[-subtitol].svg
svg/marca/arbitratge-marca-negatiu[-subtitol].svg
svg/marca/arbitratge-marca-{negre,blanc,verd}.svg
svg/favicon/favicon.svg · favicon.ico (16/32/48)
android/foreground.svg · background.svg · monochrome.svg
android/res/...                           VectorDrawable i adaptive-icon
credencial/credencial-banda-a4[-grisos].svg
png/arbitratge-icona-{192,512,1024}.png   disc, fons transparent
png/ic_launcher-adaptiu-{192,512,1024}.png quadrat a sang (Play Store: 512)
png/favicon-{16,32,48}.png
png/arbitratge-marca-horitzontal-1200.png
png/captura-48dp-{clar,fosc}.png          xxhdpi (×3)
```
