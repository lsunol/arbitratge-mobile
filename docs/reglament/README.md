# Reglament de joc

Font per generar i justificar les preguntes de l'examen. Només es guarda el **Markdown** de cada
versió (el `.docx` rebut no es versiona).

Cada versió viu a `docs/reglament/<etiqueta>/`, on l'etiqueta és la **data de la versió**
(`AAAA-MM-DD`). Aquesta etiqueta és el valor de `rulebook_version` de cada pregunta i el que l'app
mostra/emmagatzema al camp de reglament.

| Fitxer | Què és |
|---|---|
| `CURRENT` | Etiqueta de la versió vigent |
| `<etiqueta>/reglament.md` | Text del reglament (capítols, articles, subapartats), sense canvis de redacció |
| `<etiqueta>/rules.json` | Unitats citables (`art-N`, `art-N.M`), generat a partir del Markdown |

## Versions

| Etiqueta | Estat | Notes |
|---|---|---|
| `2026-11-28` | **provisional** (Revisió novembre 2026) | Netejat respecte del Word: sense ressaltats de canvis (`<mark>`), sense la nota de treball final («Capítol nomes perl porter 3 articles») ni el número de pàgina. |

## Nova versió

1. Crea `docs/reglament/<AAAA-MM-DD>/reglament.md` (converteix el Word i revisa capítols/articles).
2. `node scripts/reglament-units.mjs <etiqueta>` (genera `rules.json`; revisa els avisos).
3. Actualitza `CURRENT` i la taula de dalt.
4. No esborris versions antigues mentre hi hagi preguntes aprovades que les citin; cada pregunta
   guarda la seva `rulebook_version`.

## Anomalies conegudes del text (2026-11-28)

El parser les tolera, però convé corregir-les a la font:
- Art. 10: subapartats numerats `11.1–11.4` (haurien de ser `10.x`) → ref `art-10.1…4`.
- Art. 11: subapartats numerats `5.1–5.3` (haurien de ser `11.x`) → ref `art-11.1…3`.
- Art. 16: salta de `16.2` a `16.4` (no hi ha `16.3`).

El reglament és públic; **les preguntes, respostes i explicacions no** (ADR 0002): es generen i
es guarden al banc privat, fora d'aquest repositori (ordre `/genera-preguntes`).
