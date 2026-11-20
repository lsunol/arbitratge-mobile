# Reglament de joc

Font per generar i justificar les preguntes de l'examen.

| Fitxer | Què és |
|---|---|
| `reglament-de-joc-2026-11.docx` | Original rebut (Revisió novembre 2026, **versió provisional**) |
| `reglament-de-joc-2026-11.md` | Conversió a text (capítols `##`, articles `###`), sense canvis de redacció |
| `rules.json` | Unitats citables (`art-N`, `art-N.M`) per a la skill `rulebook-exam-questions` |

Regenerar `rules.json` després d'editar el Markdown:

```bash
node scripts/reglament-units.mjs
```

Quan arribi una versió nova: afegeix el `.docx` amb la data al nom, converteix-lo a `.md`
(revisa capítols/articles a mà), regenera `rules.json` i actualitza `rulebook_version` a
les preguntes noves. No esborris versions antigues mentre hi hagi preguntes que les citin.

El reglament és públic; **les preguntes, respostes i explicacions no** (ADR 0002): es generen i
es guarden al banc privat, fora d'aquest repositori (ordre `/genera-preguntes`).

Nota: la conversió conserva tal qual una nota final de l'original («Capítol nomes perl porter
3 articles»), que sembla un comentari de treball de l'esborrany.
