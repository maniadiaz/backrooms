# Etapa 2 — Base técnica

Versión del mod: `0.2.0`. Minecraft Java `1.20.1`, Forge `47.4.28`, ForgeGradle `6.0.54`, Gradle `8.8`, JDK `17`.

## Implementado

- Wrapper Gradle del MDK oficial, con su jar versionado.
- Identificador `bedroom`, metadatos Forge y separación de código común sin dependencias de clases del cliente.
- Registro diferido de bloque `bedroom:wallpaper` y su objeto. Textura vanilla provisional y nombres en español de México e inglés.
- Dimensión `bedroom:level_0` definida mediante datos, sin luz natural; bioma vacío sin spawns naturales.
- Sala de prueba de 15 × 15 bloques interiores, suelo en Y=64, espacio libre en Y=65–67 y techo en Y=68.
- Comandos de operador `/bedroom enter`, `/bedroom exit` y `/bedroom rescue`.
- Guardado de dimensión, posición y orientación de regreso en datos persistentes del jugador. Reentrar desde Nivel 0 no sobrescribe el origen.
- Marcador de construcción para no reconstruir la sala en cada visita.
- Configuración común para habilitar comandos de desarrollo y script PowerShell para elegir el JDK local.
- Prueba GameTest que carga la dimensión en entorno de servidor y comprueba la sala.

## Validación ejecutada

- `build`: compilación, recursos, empaquetado y reobfuscación completados correctamente.
- `runGameTestServer`: una prueba requerida aprobada; comprobó dimensión cargada, ausencia de luz de cielo, altura interior, paredes, suelo, luminaria y conservación de una modificación al solicitar de nuevo la sala.
- `runClient`: arranque con Forge 47.4.28; inicialización de audio y atlas de texturas sin errores de carga específicos del mod. Esto no sustituye una prueba visual dentro del mundo.

El entorno GameTest no equivale a una partida cooperativa con clientes conectados. No se han comprobado manualmente entrada, salida y regreso tras reconexión con un jugador real.

## Comprobación manual pendiente

1. Ejecutar `scripts/gradle.ps1 runClient` y crear un mundo creativo con comandos permitidos.
2. Anotar posición inicial y ejecutar `/bedroom enter`.
3. Revisar suelo, paredes, techo y altura; obtener el bloque con `/give @s bedroom:wallpaper`.
4. Modificar un bloque de la sala, ejecutar otra vez `enter` y verificar que la modificación permanece.
5. Ejecutar `/bedroom exit` y comprobar dimensión, posición y orientación originales.
6. Entrar de nuevo, guardar, cerrar y volver a abrir el mundo; comprobar el regreso.
7. Ejecutar `/bedroom rescue` y comprobar el regreso al spawn del Overworld.
8. Repetir con dos clientes en servidor dedicado y verificar que cada uno conserva su propio origen. Comprobar que un jugador sin permisos no puede ejecutar los comandos.

## Límites de esta entrega

Solo la sala tiene techo y paredes; el resto de la dimensión es una base plana provisional. No es todavía un laberinto. Las paredes son bloques completos; su geometría delgada y sus texturas propias corresponden a etapa 3.

No hay lobby de partidas, limbo, espectadores gestionados, cordura, equipo inicial, audio propio, linterna, contenedores, Sabueso ni pared de falla. No se han implementado las reglas especiales de muerte y construcción de V1.

El regreso exacto presupone que no se haya obstruido el punto original. El rescate usa el spawn del Overworld definido por el administrador. Estas herramientas son de desarrollo; las transiciones de supervivencia necesitarán validación de destinos.

La propuesta anterior de un objeto de acceso y su receta queda descartada: el usuario definió entrada desde un lobby para 1–4 jugadores mediante botón. El nuevo gestor de partidas se desarrollará en la etapa 2.1; los comandos actuales permanecen como herramientas de desarrollo.

## Procedencia

El wrapper y la configuración inicial proceden del [MDK oficial de Forge 47.4.28](https://maven.minecraftforge.net/net/minecraftforge/forge/1.20.1-47.4.28/forge-1.20.1-47.4.28-mdk.zip). El proyecto usa las [instrucciones oficiales para Forge 1.20.1](https://docs.minecraftforge.net/en/1.20.1/gettingstarted/).

Los modelos provisionales referencian texturas del juego instalado; no se incluyen copias de los archivos de Minecraft. El wrapper Gradle conserva sus avisos de licencia originales. El código propio se mantiene bajo todos los derechos reservados mientras el propietario no elija otra licencia.
