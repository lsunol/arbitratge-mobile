# ADR 0002 — Banco privado durante el desarrollo, incorporado al APK

- **Estado:** aprobado
- **Fecha:** 2026-11-20

## Contexto

Las preguntas, respuestas y explicaciones se consideran contenido sensible. El repositorio
principal debe ser público al menos para distribuir la web y las releases. El examen es
offline y autocorregido, por lo que el artefacto instalado necesita el banco completo.

Un repositorio privado protege el código fuente del banco, pero no los datos una vez incluidos
en un APK descargable. El banco del proyecto base ya contiene contenido publicado con
anterioridad, por lo que no puede presumirse confidencialidad retroactiva de esas preguntas.

## Opciones

1. Banco real versionado en el repo público.
2. Banco fuera del APK y descarga bajo demanda: añade servidor/conectividad y complejidad.
3. Banco fuera del repo público, inyectado en el build e incluido en el APK.
4. Cifrado/ofuscación del banco dentro del APK con la clave también embebida.

## Decisión

El repositorio público contendrá únicamente código, metadatos y un **banco ficticio mínimo
para pruebas**. El banco real:

- no estará en el repositorio público ni en su historial, GitHub Pages, documentación,
  ejemplos, logs o artifacts accesibles;
- podrá vivir en un segundo repositorio privado, en un package/artifact privado o en una
  carpeta local excluida de git;
- se inyectará durante el build local o desde un secreto/token de CI;
- quedará integrado en el APK candidato para que el examen funcione sin conexión.

No se cifra/ofusca inicialmente dentro del APK: al estar la clave o lógica de desbloqueo en
el mismo artefacto, añade complejidad sin proteger frente al adversario técnico que ya está
fuera del perfil del MVP. La explicación educativa será opcional por pregunta.

## Consecuencias

### Positivas

- El repo público no ofrece las preguntas/respuestas mediante navegador o API de Git.
- La app es completamente offline.
- Puede compilarse con un banco ficticio para desarrollo y contribuciones públicas.
- El banco privado puede evolucionar sin rediseñar la app.

### Limitaciones aceptadas

- Quien reciba el APK público puede extraer o decompilar su contenido.
- El contenido mostrado en exámenes anteriores puede copiarse y compartirse.
- No se recupera la confidencialidad del banco que ya estaba publicado en el proyecto base.

### Controles de entrega

- Añadir validación de esquema, IDs únicos, respuestas válidas y ausencia del banco real en
  artefactos públicos.
- Revisar el árbol e historial destinados a publicación antes del primer push público.
- Tratar las explicaciones nuevas como contenido privado igual que preguntas/respuestas.
- Separar claramente la fuente privada, el banco ficticio y el APK final.
- Definir en el diseño de distribución cómo CI accede al banco privado sin filtrar tokens.
