---
description: "Genera N preguntes d'examen noves a partir del reglament, sense repetir les del banc"
mode: agent
---
<!-- GENERADO por scripts/sync-agents.mjs desde .agents/commands/. No editar. -->

Genera **${input:arg1}** preguntes noves per al banc d'Arbitratge Mobile a partir del reglament.
Abast opcional (articles, capítol o tema; si és buit, tot el reglament prioritzant el que
menys cobert estigui): ${input:arguments}

Segueix la skill `rulebook-exam-questions` (llegeix el seu `SKILL.md` sencer abans de començar)
amb aquesta configuració del projecte:

1. **Rol i confidencialitat.** Actua amb les regles de `bank-curator` (`AGENTS.md` i
   `.agents/agents/bank-curator.md`). Si el teu harness té subagents, delega la generació a
   `bank-curator` i la verificació (§5.2 de la skill) a `reviewer`. Al xat només recomptes,
   IDs i articles; mai el text de preguntes o respostes. No escriguis res del banc dins
   d'aquest repositori públic.
2. **Reglament:** la versió vigent és l'etiqueta (data AAAA-MM-DD) de `docs/reglament/CURRENT`; usa
   `docs/reglament/<etiqueta>/rules.json` (unitats `art-N`/`art-N.M`, ja segmentades) i
   `docs/reglament/<etiqueta>/reglament.md`. `rulebook_version` = aquesta etiqueta.
3. **Banc privat:** carpeta `$ARB_BANK_DIR` o, si no està definida,
   `C:\Users\lluis\projects\arbitratge-bank` (fora del repo públic). Si no existeix, pregunta
   abans de crear-la (amb `bank/`, `drafts/`, `work/` i `git init`, sense remot).
4. **No repetir.** Abans de generar, construeix l'índex de preguntes existents a partir de:
   `<banc>/bank/bank.json` (si existeix) i tots els `<banc>/drafts/drafts-*.json` (incloses les
   descartades). Cap pregunta nova pot avaluar el mateix fet normatiu que una existent amb la
   mateixa resposta; reformular-la no la fa nova. Calcula la cobertura per article (`ref`) i
   reparteix les ${input:arg1} preguntes cap als articles menys coberts dins l'abast.
5. **Barreja de tipus:** aproximadament 60 % `single_choice`, 20 % `multiple_choice`
   (2–3 correctes de 4–5) i 20 % `true_false`, tret que l'abast demani una altra cosa.
6. **IDs:** el següent `q-NNNN` lliure considerant banc i esborranys.
7. **Sortida:** `<banc>/drafts/drafts-<AAAA-MM-DD>.json` i `<banc>/drafts/review-<AAAA-MM-DD>.md`.
   Valida amb
   `node ~/.agents/skills/rulebook-exam-questions/scripts/validate-drafts.mjs <drafts> --rules docs/reglament/<etiqueta>/rules.json [--bank <banc>/bank/bank.json]`
   fins que surti `OK`, i fes la verificació independent (§5.2) abans de donar-ho per acabat.
8. **No incorporis res al `bank.json`** sense l'aprovació explícita de l'usuari (§6 de la skill).

Acaba amb: preguntes generades per tipus i per article, resultat del validador, resultat del
fact-check (ok/fix/drop), IDs, ruta del fitxer de revisió i articles encara sense cobertura.
