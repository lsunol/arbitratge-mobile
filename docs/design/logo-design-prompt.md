# Prompt per a Claude Design — identitat i logotip d'Arbitratge Mobile

> Còpia aquest prompt a Claude Design. La identitat proposada és provisional fins que hi hagi
> aprovació del club i logo oficial de CFBB (si n'hi ha).

---

Dissenya el sistema visual mínim de marca i l'icona adaptativa per a **Arbitratge Mobile**, l'app
Android que permet que una persona candidata prepari i faci l'examen per ser àrbitre de Futbol
amb Botons, sigui corregida localment i, si aprova, obtingui una credencial amb QR. El públic és
majoritàriament major; la confiança, la claredat i la llegibilitat són més importants que l'efecte
tecnològic.

## Direcció visual: «Centre del camp»

Construeix la marca al voltant de la geometria del futbol amb botons:

- Un **disc / botó circular** com a forma principal i immediatament reconeixible.
- Línies de cal estil centre del camp (cercle central i ratlla), usades amb molta mesura.
- Un petit to **groc targeta / xiulet arbitral** com a accent, no com a color dominant.
- Aspecte institucional, calent i serè; no videojoc, no app de fitness, no logo «tech» genèric,
  no escut heràldic complicat, no pilota de futbol convencional i no simular cap grau de seguretat
  criptogràfica.

Paleta de base:

- **Verd de gespa profund:** `#0F3D26` (fons/marca)
- **Verd principal accessible:** `#1D6F42`
- **Cal:** `#F2F5F3` / `#FFFFFF`
- **Tinta:** `#18241D`
- **Accent groc targeta:** `#F2C230`

Tipografia de suport si apareix text en alguna peça editorial: **Atkinson Hyperlegible Next**
(SIL OFL 1.1). Per al símbol principal, prefereixo una construcció sense text perquè funcioni
correctament a mides petites.

## Conceptes que ha de comunicar

1. **Futbol amb Botons:** disc circular, camp i moviment tàctil.
2. **Arbitratge i joc net:** ordre, línies, simetria, xiulet o targeta (com a accent secundari).
3. **Formació:** confiança, claredat i aprenentatge; no autoritat agressiva.
4. **Catalanitat:** subtil, sense bandera ni folclore obligatori.
5. **Credencial:** la marca ha de funcionar en una banda de carnet, però sense segell, cadenes o
   checks que facin creure que la credencial és infalsificable.

## Entregables obligatòris

1. **Icona principal en SVG vectorial** sobre fons transparent.
2. **Icona adaptativa Android** separada en dues capes:
   - `foreground.svg`: símbol segur dins el 66 % central, sense tocar els límits.
   - `background.svg` / color de fons.
   - Mostrar i provar les màscares cercle, quadrat arrodonit, squircle i gota.
3. **Icona petita / favicon** (16, 32, 48 px): versió simplificada, optimitzada per no perdre
   el detall.
4. **Marca horitzontal**: símbol + «Arbitratge» (subtítol opcional: «Futbol amb Botons»).
5. **Versions monocromes**: positiu negre, negatiu blanc i una tinta verda sobre transparent.
6. **PNG exportats**: 192×192, 512×512, 1024×1024; i captures a mida de 48 dp sobre pantalla
   clara i fosca.
7. **Plantilla de banda per a la credencial A4**: una capçalera o banda estreta, sense QR dins
   del logo. Ha de funcionar també a escala de grisos.
8. **Fitxer font de text o especificació** de la marca: nom, color, espaiat mínim, ús incorrecte,
   proporcions i àrea de seguretat.

No rasteritzis el vector principal. Els SVGs han de poder viure dins d'un repositori i ser
convertits a Android VectorDrawable/recursos Web; per tant, evita filtres, màscares complexes,
text convertit de manera complicada, gradients i efectes de brillantor.

## Límits i criteris d'acceptació

- Reconeixible a 24×24 px.
- Pot confondre's com a xapa/botó, camp central o xiulet; no com una app bancària ni de joc.
- Línies i formes continuen llegibles al 200 % de zoom i en escala de grisos.
- Contrastos adequats; el símbol clar sobre `#0F3D26` ha de tenir almenys 4,5:1.
- No fer servir imatges, logos o tipografies amb llicència desconeguda.
- No incloure fotografies, mapes, banderes, escuts ni el nom d'una federació si no s'ha donat logo
  oficial.
- Per al nom escrit, idioma català. Espai previ i posterior a «Arbitratge» coherent.

## Procés de resposta

Presenta primer **3 rutes visualment diferents** en un moodboard (no variacions de la mateixa):

A. «Botó al centre»: disc dominant amb línies de camp.
B. «Xapa i cal»: botó inclinat/geomètric sobre camp mínim.
C. «Xiulet de joc»: forma més abstracta, amb referència a arbitratge.

Per a cada ruta, mostra: símbol gran, simbol 24 px, icona Android en les 4 màscares, monocrom,
capçalera de credencial, captura simulada de la icona dins un launcher Android. Explica en una
frase què comunica i quins són els riscos.

Després, recomana una ruta i redueix-la a **una** proposta final amb les variants i entregables
anteriors. Prefereixo decidir només després de veure les 3 rutes.

Per evitar malentesos, etiqueta explícitament el resultat com a:
**«Identitat provisional d'Arbitratge Mobile — pendent d'aprovació de CFBB»**.
