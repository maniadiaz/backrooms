# Bedroom

Proyecto de mod de terror y exploración para Minecraft Java 1.20.1 con Forge 47.4.28.

Estado actual: versión 0.3.1, con HUD fijo a la izquierda, mensajes centrales, reinicio de partida y barras nutritivas. Incluye lobby automático, laberinto procedural, partidas de 1–4 jugadores, cola, limbo y espectadores. Cordura y el resto de consumibles siguen pendientes. Véase [validación y pendientes](docs/ETAPA-3.1-HUD-Y-REINICIO.md).

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

El jar se genera en `build/libs/bedroom-0.3.1.jar`. Instalar la misma versión en cliente y servidor con Minecraft 1.20.1 y Forge 47.4.28. Los mundos de 0.3.0 conservan su generador; para mundos anteriores con el generador plano, crear un mundo nuevo.

## Probar una partida

1. Entrar al mundo o servidor: aparecerás automáticamente en el lobby. Hasta cuatro jugadores forman el grupo; los demás esperan allí para la siguiente oportunidad. `/bedroom lobby` permite volver si saliste del modo de juego y no requiere operador.
2. El primer participante es el anfitrión. Presiona el botón sobre el bloque verde al fondo del lobby para iniciar.
3. Explorar el laberinto. Hay una pared anómala más oscura por cada región de 128 × 128 bloques: presionarla con clic derecho completa el nivel y devuelve al equipo al lobby. El no-clip de correr contra ella llegará en etapa 7.
4. Para probar la muerte, un operador participante puede usar `/bedroom testdeath`. Las primeras dos llevan a un laberinto blanco: encontrar y presionar su botón de salida antes de 30 segundos. La tercera lleva a espectador.
5. Para probar el avance entre niveles, usar `/bedroom testnext` como operador activo: todos reviven en la sala de descanso. Esperar 90 segundos o presionar el botón como anfitrión. El destino es otro sector del Nivel 0 para pruebas; no representa el Nivel 1.
6. `/bedroom status` muestra el estado. `/bedroom leave` permite salir y recuperar el inventario, experiencia y modo de juego externos.
7. `/bedroom restart` devuelve al grupo al lobby, limpia la partida y admite a los invitados en espera si hay puestos libres. Lo pueden usar el anfitrión de la partida, el dueño del mundo o un operador, sin depender de los comandos de desarrollo. No inicia automáticamente otra partida.

Los botones son interacciones normales: no requieren permisos de operador. Si todos quedan eliminados, el equipo vuelve al lobby y el siguiente inicio crea una partida limpia. Mientras alguien conserve tiempo en el limbo, todavía puede rescatar la partida. Cada inicio nuevo entrega tres barras nutritivas: mantener pulsado usar objeto para comer y recuperar cuatro muslos de hambre, además de Regeneración I durante cinco segundos. El resto del kit inicial llegará en etapa 4.

El panel izquierdo muestra `BEDROOM: ACTIVE` encima de las muertes, además de los tiempos de limbo/descanso cuando corresponda. Los anuncios centrales aparecen al entrar, morir, regresar, completar, perder o reiniciar, con entrada y salida gradual.

## Jugar mediante Essential

El dueño comparte el mundo usando Essential; Bedroom utiliza el servidor integrado de Minecraft y sus paquetes Forge, sin una dependencia directa de Essential. Todos deben instalar las mismas versiones de mods, incluido `bedroom-0.3.1.jar`, y una versión de Essential para Forge 1.20.1. Véase la [guía oficial de hosting de Essential](https://essential.gg/wiki/start-hosting).

Los invitados que llegan tarde permanecen en el lobby hasta el siguiente inicio. Para incorporarlos antes, el anfitrión usa `/bedroom restart` y vuelve a pulsar el botón de inicio. El límite sigue siendo cuatro participantes. La compatibilidad práctica con Essential está pendiente de una sesión real con amigos; las pruebas automatizadas se ejecutaron con Forge.

Reconectar durante una partida recupera el estado del jugador, incluido su reloj de limbo. La llegada de alguien nuevo no lo incorpora a una partida activa. La cola y los inventarios externos se guardan. La entrada automática también se aplica a reapariciones normales fuera de una partida; puede desactivarse con `autoLobby=false` en `config/bedroom-common.toml`.

## Sala de prueba

Crear un mundo con comandos permitidos y ejecutar:

- `/bedroom enter`: construir la sala una sola vez y entrar al Nivel 0.
- `/bedroom exit`: regresar a la posición y dimensión guardadas antes de entrar.
- `/bedroom rescue`: volver al spawn del Overworld en caso de emergencia.
- `/give @s bedroom:wallpaper`: obtener el bloque provisional de pared.

Los comandos requieren permisos de operador de nivel 2. Pueden desactivarse en `run/config/bedroom-common.toml` mediante `developmentCommands=false`, reiniciando después.

Esta sala antigua permanece disponible para desarrollo fuera de una partida. La entrada `enter` se bloquea para participantes; `exit` y `rescue` equivalen a abandonar la partida si perteneces a una. Fuera de ella conservan su comportamiento anterior. El retorno externo presupone que el lugar de origen sigue siendo transitable.

El Nivel 0 usa bloques y texturas originales, con modelos simples. Lobby, limbo y descanso conservan salas provisionales. La colocación de objetos de misiones y los contenedores propios llegarán con sus sistemas; por ahora se bloquean las colocaciones y las interacciones no autorizadas. El diseño completo está en [Partidas y lobby](docs/PARTIDAS-Y-LOBBY.md).

`python scripts/generate_textures.py` reconstruye las texturas y modelos propios de forma reproducible, sin dependencias externas de Python.

Las pruebas de integración usan `run-gametest/`, separado del cliente. Cada ejecución borra únicamente el mundo de prueba generado en `run-gametest/world` para no reutilizar chunks de un generador anterior; no guardar partidas personales en esa carpeta.

Para el servidor de desarrollo, revisar el EULA que genera Minecraft y aceptarlo personalmente si se está de acuerdo antes de volver a ejecutar `runServer`.

Referencia técnica: [documentación oficial de Forge 1.20.1](https://docs.minecraftforge.net/en/1.20.1/gettingstarted/).
