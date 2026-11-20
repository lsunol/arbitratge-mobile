# Fase de diseño — plan de trabajo

## Orden

1. **`product-analyst`** → `01-requisitos.md`
   Lee el proyecto base y define requisitos, flujo y reglas del examen offline.
2. En paralelo, ambos a partir de `01-requisitos.md`:
   - **`trust-architect`** → `02-credencial-y-amenazas.md` (la decisión más crítica)
   - **`mobile-architect`** → `03-arquitectura-movil.md`
   Se coordinan a través del usuario/orquestador: el esquema de credencial condiciona el
   stack (APIs nativas de Android Keystore / Key Attestation, generación de PDF, QR…).
3. **Punto de decisión con el usuario:** esquema de credencial y stack → ADRs.
4. En paralelo:
   - **`ux-designer`** → `04-ux.md` (incluye app candidata, PWA verificadora pública y web de descarga)
   - **`release-engineer`** → `05-distribucion.md` (repo público + fuente privada del banco, CI y PWA)
5. **`design-reviewer`** revisa todo (01–05 + ADRs). Se corrigen los hallazgos
   bloqueantes y, si hace falta, se repite la revisión.
6. **Cierre de diseño** con el usuario.

## Decisiones ya aprobadas por el usuario

- [`docs/adr/0001-credencial-sellada-y-verificador-publico.md`](../adr/0001-credencial-sellada-y-verificador-publico.md) — aprobado
- [`docs/adr/0002-banco-privado-en-build-y-apk.md`](../adr/0002-banco-privado-en-build-y-apk.md) — aprobado

Revisión final del diseño: [`reviews/revision-final-diseno.md`](reviews/revision-final-diseno.md).

**No queda ninguna decisión pendiente** sobre app de organizadores (descartada en el MVP;
se usa la PWA verificadora) ni sobre checksum/ofuscación del banco.

No hace falta app Android de organizadores en el MVP: se usará una PWA estática verificadora,
instalable como acceso directo en Android.

## Entregables

| Fichero | Autor | Estado |
|---|---|---|
| `01-requisitos.md` | product-analyst | revisado; solo pendiente de validaciones/prototipos |
| `02-credencial-y-amenazas.md` | trust-architect | revisado; solo pendiente de validaciones/prototipos |
| `03-arquitectura-movil.md` | mobile-architect | revisado; solo pendiente de validaciones/prototipos |
| `04-ux.md` | ux-designer | revisado; solo pendiente de validaciones/prototipos |
| `05-distribucion.md` | release-engineer | revisado; solo pendiente de validaciones/prototipos |
| `../adr/` | orquestador + usuario | — |

## Implementación — revisión de agentes completada

La flota se revisó al cerrar el diseño. No hace falta un `test-engineer` separado en esta
fase: cada implementador es responsable de sus tests. La configuración vigente está en
`AGENTS.md` y `.pi/settings.json`.

Agentes propios activos:

- `android-developer`: Kotlin/Compose, `core-exam`, persistencia, PDF/QR, accesibilidad.
- `credential-engineer`: único propietario del contrato ARB1 y sus vectores compartidos.
- `pwa-developer`: PWA verificadora y web estática de descarga.
- `bank-curator`: banco privado, validación, migración y explicaciones.
- `release-engineer`: CI, firma, Releases, Pages y controles de fugas.

Agentes consultivos conservados: `trust-architect`, `ux-designer`. Los agentes de diseño
retirados se conservan en `docs/agents-archive/` únicamente como historial. La revisión de
cada cambio se delegará al `reviewer` integrado (modelo independiente) y las decisiones
difíciles al `oracle` integrado.

Antes de implementar hay que crear el repositorio remoto, añadir el `.gitignore` bloqueante,
inspeccionar el árbol publicable y hacer el primer commit.
