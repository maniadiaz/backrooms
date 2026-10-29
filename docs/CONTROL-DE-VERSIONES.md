# Control de versiones por etapa

Remoto: https://github.com/maniadiaz/bedroom-mod.git

## Flujo

La rama principal es `main`. Cada etapa se guarda en uno o varios commits pequeños y descriptivos. Al terminar una etapa y verificar sus entregables, se crea una etiqueta anotada `etapa-N`.

La etiqueta identifica el estado exacto de esa entrega. No se mueve ni se reemplaza si después hay correcciones: estas reciben nuevos commits y, si se necesita identificar otra entrega, una etiqueta como `etapa-1.1`.

Antes de cada commit, revisar `git status` y `git diff`. Agregar únicamente los archivos de la tarea; no incluir mundos, datos de ejecución, credenciales ni resultados de compilación.

## Ejemplo de cierre de etapa

```powershell
git status
git diff
git add docs/ETAPA-1-ALCANCE.md
git commit -m "docs(etapa-1): definir alcance de la primera version"
git tag -a etapa-1 -m "Etapa 1: alcance y criterios de aceptacion"
git push origin main
git push origin etapa-1
```

El ejemplo de commit solo aplica cuando hay cambios nuevos. Si la etapa ya está guardada, no se repite el commit ni se recrea la etiqueta.

## Convención de mensajes

- `docs(etapa-1): ...` para documentación y decisiones.
- `build(etapa-2): ...` para configuración del proyecto y compilación.
- `feat(etapa-3): ...` para funcionalidades nuevas.
- `fix(etapa-N): ...` para correcciones.

Las etapas con código deben indicar en su documentación qué se verificó y qué queda pendiente antes de etiquetarlas. El wrapper Gradle, sus archivos jar y los recursos fuente del mod sí se incluyen en Git.
