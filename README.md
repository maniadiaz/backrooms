# Bedroom

Proyecto de mod de terror y exploración para Minecraft Java 1.20.1 con Forge 47.4.28.

Estado actual: prototipo de la etapa 2.1 con lobby, partidas de 1–4 jugadores, limbo, espectadores y descanso. Todavía no incluye el laberinto procedural del Nivel 0 ni cordura. Véase [validación y pendientes](docs/ETAPA-2.1-PARTIDAS.md).

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

El jar se genera en `build/libs/bedroom-0.2.1.jar`. Instalar en cliente y servidor con Minecraft 1.20.1 y Forge 47.4.28.

## Probar una partida

1. Ejecutar `/bedroom lobby`. Hasta cuatro jugadores pueden unirse; este comando no requiere operador. No se envía automáticamente al lobby a quienes entran al servidor.
2. El primer participante es el anfitrión. Presiona el botón sobre el bloque verde al fondo del lobby para iniciar.
3. En la sala del nivel, el botón verde representa la salida provisional y devuelve al equipo al lobby.
4. Para probar la muerte, un operador participante puede usar `/bedroom testdeath`. Las primeras dos llevan a un laberinto blanco: encontrar y presionar su botón de salida antes de 30 segundos. La tercera lleva a espectador.
5. Para probar el avance entre niveles, usar `/bedroom testnext` como operador activo: todos reviven en la sala de descanso. Esperar 90 segundos o presionar el botón como anfitrión. La sala siguiente también es provisional; no representa el Nivel 1.
6. `/bedroom status` muestra el estado. `/bedroom leave` permite salir y recuperar el inventario, experiencia y modo de juego externos.

Los botones son interacciones normales: no requieren permisos de operador. Si todos quedan eliminados, el equipo vuelve al lobby y el siguiente inicio crea una partida limpia. Mientras alguien conserve tiempo en el limbo, todavía puede rescatar la partida. No hay kit inicial hasta implementar sus objetos en la etapa 4.

## Sala de prueba

Crear un mundo con comandos permitidos y ejecutar:

- `/bedroom enter`: construir la sala una sola vez y entrar al Nivel 0.
- `/bedroom exit`: regresar a la posición y dimensión guardadas antes de entrar.
- `/bedroom rescue`: volver al spawn del Overworld en caso de emergencia.
- `/give @s bedroom:wallpaper`: obtener el bloque provisional de pared.

Los comandos requieren permisos de operador de nivel 2. Pueden desactivarse en `run/config/bedroom-common.toml` mediante `developmentCommands=false`, reiniciando después.

Esta sala antigua permanece disponible para desarrollo fuera de una partida. La entrada `enter` se bloquea para participantes; `exit` y `rescue` equivalen a abandonar la partida si perteneces a una. Fuera de ella conservan su comportamiento anterior. El retorno externo presupone que el lugar de origen sigue siendo transitable.

Las salas usan recursos vanilla provisionales. La colocación de objetos de misiones y los contenedores propios llegarán con sus sistemas; por ahora se bloquean las colocaciones y las interacciones con bloques ajenos a los botones de partida. El diseño completo está en [Partidas y lobby](docs/PARTIDAS-Y-LOBBY.md).

Para el servidor de desarrollo, revisar el EULA que genera Minecraft y aceptarlo personalmente si se está de acuerdo antes de volver a ejecutar `runServer`.

Referencia técnica: [documentación oficial de Forge 1.20.1](https://docs.minecraftforge.net/en/1.20.1/gettingstarted/).
