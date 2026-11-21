# Skill UI/UX instalada en el proyecto

Fecha de revisión: 2026-11-21.

## Evidencia previa a usarla

El usuario instaló la skill `nextlevelbuilder/ui-ux-pro-max-skill@ui-ux-pro-max` con `npx skills`.
La elección se verificó en la página pública de skills.sh y en el repositorio de origen:

- página de la skill: https://www.skills.sh/nextlevelbuilder/ui-ux-pro-max-skill/ui-ux-pro-max
- repositorio: https://github.com/nextlevelbuilder/ui-ux-pro-max-skill
- **463,5k** instalaciones de la skill en skills.sh;
- **cerca de 984k** instalaciones acumuladas del paquete;
- **152k+** estrellas del repositorio;
- licencia del repositorio: **MIT**;
- contenido local revisado: 80 estilos, 194 paletas/perfiles, 74 parejas tipográficas,
  130 reglas UX, 105 iconos, 22 stacks y scripts sin dependencias para consultar los CSV.

El contenido también es suficientemente específico para este proyecto (stack Jetpack Compose,
app Android nativa y problemas de accesibilidad, texto largo, targets táctiles, modo oscuro y
alineación de controles).

## Ubicación y enlaces entre harnesses

- Original en: `.agents/skills/ui-ux-pro-max/`.
- `skills-lock.json` fija origen y hash de `SKILL.md`.
- En esta máquina, `.claude/skills/ui-ux-pro-max` y `.pi/skills/ui-ux-pro-max` son junctions
  al original. No hay una segunda copia.
- opencode consume `.claude/skills/`.
- Para GitHub Copilot, el original portable sigue disponible en `.agents/skills/`; si el host
  requiere enlace de usuario, usar la misma regla que `AGENTS.md` global para `~/.copilot/skills/`.
- En un checkout limpio, recrear los enlaces de proyecto con:

  ```bash
  node scripts/link-project-skills.mjs
  ```

  El script es idempotente: elimina solo los enlaces/dirigidos conocidos (`ui-ux-pro-max`) y los
  vuelve a crear sin copiar. opencode seguirá viendo `.claude/skills/`.

Los enlaces/junctions locales se ignoran en git. El original (incluido su `LICENSE` MIT) y el lock
sí se versionan.

## Adaptación del paquete instalado

El paquete venía pensado para un plugin con `${CLAUDE_PLUGIN_ROOT}`. Para que funcione en todos
los harnesses desde la raíz del repo, sus ejemplos pasan a usar:

```bash
python .agents/skills/ui-ux-pro-max/scripts/search.py "consulta" --domain ux
python .agents/skills/ui-ux-pro-max/scripts/search.py "consulta" --stack jetpack-compose
```

Se conservaron los datos y scripts de ejecución, pero no los tests del upstream que referencian
assets/scripts no incluidos en esta instalación.

## Uso obligatorio en Arbitratge Mobile

Antes de opinar o implementar cualquier tarea que cambie UI, los agentes `ux-designer`,
`ui-designer` y `android-developer` deben:

1. leer `SKILL.md` y `references/pro-rules.md`;
2. ejecutar una búsqueda UX y, cuando aplique, otra `--stack jetpack-compose`;
3. registrar las consultas y lo aplicado en su informe;
4. verificar las reglas críticas: contraste 4,5:1, controles ≥48 dp, 8 dp entre controles,
   reflow con texto grande, safe areas, foco/semántica, modo claro/oscuro;
5. después de implementar, aportar capturas de emulador en 100 %, 130 % y 200 %, claro/oscuro;
6. dejar TalkBack [PV] hasta probarlo en un dispositivo físico.

Esta skill no sustituye el criterio de `ux-designer` ni `ui-designer`; aporta un sistema de reglas
y datos locales consultables y repetibles. Las decisiones visuales siguen el diseño «Centre del
camp» y la identidad provisional ya aprobada.
