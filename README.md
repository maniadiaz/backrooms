# Bedroom

Proyecto de mod de terror y exploración para Minecraft Java 1.20.1 con Forge 47.4.28.

Estado actual: base técnica de la etapa 2 implementada y compilada. Proyecto Forge y sala de prueba; todavía no incluye el laberinto procedural ni las mecánicas de supervivencia. Véase [validación y pendientes](docs/ETAPA-2-BASE-TECNICA.md).

El alcance, las reglas propuestas y los criterios de aceptación están en [Etapa 1 — Alcance](docs/ETAPA-1-ALCANCE.md).

Repositorio: [maniadiaz/bedroom-mod](https://github.com/maniadiaz/bedroom-mod).

El flujo de commits y etiquetas por etapa está en [Control de versiones](docs/CONTROL-DE-VERSIONES.md).

## Desarrollo

Usar JDK 17 de 64 bits y ejecutar desde la raíz:

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
.\gradlew.bat runServer
```

En este equipo se descargó un JDK 17 en `.tools/java` (ignorado por Git). El script `scripts/gradle.ps1` lo selecciona automáticamente si existe:

```powershell
.\scripts\gradle.ps1 build
.\scripts\gradle.ps1 runClient
.\scripts\gradle.ps1 runGameTestServer
```

El jar se genera en `build/libs/bedroom-0.2.0.jar`. Instalar en cliente y servidor con Minecraft 1.20.1 y Forge 47.4.28.

## Sala de prueba

Crear un mundo con comandos permitidos y ejecutar:

- `/bedroom enter`: construir la sala una sola vez y entrar al Nivel 0.
- `/bedroom exit`: regresar a la posición y dimensión guardadas antes de entrar.
- `/bedroom rescue`: volver al spawn del Overworld en caso de emergencia.
- `/give @s bedroom:wallpaper`: obtener el bloque provisional de pared.

Los comandos requieren permisos de operador de nivel 2. Pueden desactivarse en `run/config/bedroom-common.toml` mediante `developmentCommands=false`, reiniciando después.

La sala usa recursos vanilla provisionales. El retorno exacto presupone que el lugar de origen sigue siendo transitable; `rescue` usa el spawn definido por el administrador. Todavía no hay entrada de supervivencia, conservación especial de inventario al morir ni protección de construcción.

La entrada de juego será desde un lobby para 1–4 jugadores mediante botón. El diseño actualizado incluye limbo de 30 segundos, dos oportunidades de regreso por nivel, espectadores, derrota colectiva y descansos de 90 segundos. Estos sistemas todavía no están implementados: véase [Partidas y lobby](docs/PARTIDAS-Y-LOBBY.md).

Para el servidor de desarrollo, revisar el EULA que genera Minecraft y aceptarlo personalmente si se está de acuerdo antes de volver a ejecutar `runServer`.

Referencia técnica: [documentación oficial de Forge 1.20.1](https://docs.minecraftforge.net/en/1.20.1/gettingstarted/).
