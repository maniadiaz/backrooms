# Etapa 3 — Base del laberinto y entrada automática

Versión: `0.3.0`. Minecraft Java `1.20.1`, Forge `47.4.28`.

## Lobby automático

Al conectarse, los jugadores nuevos llegan al lobby sin comandos. Los cuatro primeros puestos disponibles forman el grupo. Una llegada adicional o durante una partida queda en espera dentro del lobby, con inventario externo guardado y sin poder iniciar ni intervenir en la partida activa.

Cuando la partida está en lobby y se libera un puesto, se admite a los jugadores en espera conectados. No se incorpora a nadie al nivel activo ni al descanso de otro grupo. La cola se guarda por UUID con inventarios externos separados; reconectar no vuelve a capturar el inventario vacío del lobby como inventario de origen.

Quien pertenecía a una partida conserva nivel, limbo, espectador o descanso al reconectar. Un regreso pendiente de una partida terminada restaura primero los datos externos y vuelve a dirigir al jugador al lobby. Las reapariciones normales fuera de una partida también llegan al lobby. Las muertes de partida siguen usando el limbo y no pasan por una reaparición vanilla.

`autoLobby=true` es el valor predeterminado de la configuración común. Desactivarlo evita la entrada automática de jugadores ajenos; no abandona partidas ni borra colas ya guardadas. `/bedroom leave` permite salir del modo hasta la próxima conexión o reaparición.

## Generación del Nivel 0

- Generador propio `bedroom:level_0` registrado mediante codec; hereda la infraestructura de terreno plano, pero sustituye la generación de bloques y consultas de altura.
- Generación por chunks, sin colocar bloques de chunks vecinos desde una tarea de generación.
- Distribución reproducible a partir de la semilla del mundo y coordenadas globales, también negativas.
- Celdas de 16 × 16 bloques con cinco variantes lógicas: sala abierta, columna solitaria, pasillos cruzados estrechos, paneles con recodos y paneles desplazados. No son cinco estructuras NBT prefabricadas: las variantes se calculan en código.
- Conexiones centrales garantizadas entre celdas; algunas divisiones desaparecen para formar espacios mayores. La red se extiende conforme se explora, dentro del límite del mundo de Minecraft.
- Suelo continuo en Y=64, tres bloques interiores en Y=65–67 y techo continuo en Y=68. Sin desniveles ni escaleras.
- Pasillos y pasos garantizados de dos bloques de ancho. Paneles de cuatro píxeles de grosor con colisión y modelo coherentes; las masas de separación de pasillos y columnas siguen siendo bloques completos.
- Papel tapiz amarillo, moqueta y parches húmedos agrupados junto a divisiones, techo modular, luminarias encendidas y algunas fundidas.
- Texturas originales de 32 × 32 y modelos regenerables con `scripts/generate_textures.py`; solo usa la biblioteca estándar de Python. No se copian texturas del juego de referencia.
- Sala de entrada segura en el centro de una celda. Las nuevas partidas comienzan en sectores distintos, alejados de las salas antiguas del prototipo.
- Pared anómala en una celda abierta por cada región de 128 × 128 bloques. Por ahora se activa con clic derecho para comprobar el recorrido completo hacia el lobby; la carrera de dos segundos, pantalla negra y no-clip corresponden a etapa 7.
- El descanso de desarrollo lleva a otro sector del Nivel 0; no se incluye Nivel 1.

## Arquitectura nueva

- `Level0Layout`: selección de variantes, divisiones, humedad, luminarias y salida mediante funciones de coordenadas y semilla, sin estado aleatorio compartido.
- `Level0Generator`: codec, construcción de chunks y columnas/alturas del terreno.
- `PanelBlock`: geometría de panel orientado y colisión de cuatro píxeles.
- `Level0GameTests`: conectividad, semillas, bordes de chunks y registro del generador.

## Verificación

La batería de Forge contiene catorce pruebas requeridas: las nueve anteriores, llegada de cinco jugadores con espera y devolución exacta de inventarios, reaparición/reconexión al limbo y tres pruebas de generación.

Resultado de la entrega: `BUILD SUCCESSFUL`, catorce pruebas requeridas aprobadas y ninguna fallida.

La prueba de conectividad recorre cien celdas por cada una de tres semillas, con coordenadas negativas y positivas. Comprueba que cada bloque transitable del área interior y cada centro de habitación sean alcanzables. Es una comprobación del algoritmo; no sustituye caminar manualmente las trescientas habitaciones.

Las pruebas de integración cargan el generador real en un mundo nuevo, verifican suelo y techo en ambos lados de bordes positivos y negativos, comparan geometría con el algoritmo, comprueban altura interior, colisión del panel, salida regional y serialización del codec. También comprueban repetibilidad y diferencias entre semillas.

El cliente de desarrollo se inició y cargó los nuevos atlas sin errores específicos de modelos o texturas del mod. Se inspeccionó una muestra de las texturas generadas. Falta verificar visualmente el recorrido en juego y realizar una sesión con dos a cuatro clientes reales.

Cada ejecución de `runGameTestServer` recrea exclusivamente `run-gametest/world`, dentro del proyecto. Esto evita falsos resultados por chunks guardados de una versión anterior. Las partidas personales del cliente en `run/saves` no se borran.

## Compatibilidad con mundos previos

Crear un mundo nuevo para esta versión. Un mundo guardado puede conservar el generador plano anterior en su configuración, y los chunks existentes no se regeneran automáticamente. El botón de inicio comprueba el generador y rechaza iniciar si sigue siendo el antiguo; no se borran ni convierten mundos personales.

Las nuevas partidas se sitúan lejos de las salas anteriores, pero no constituyen dimensiones aisladas por partida. Los sectores viejos permanecen en disco. La gestión de instancias y limpieza de regiones se revisará al añadir loot y entidades.

## Pendiente

- Recorrido visual y prueba multicliente, señalización final del lobby y ajustes artísticos del patrón de habitaciones.
- Enchufes decorativos y detalles adicionales de deterioro.
- Cordura, consumibles, inventarios de contenedores y equipo inicial: etapa 4.
- Parpadeos dinámicos, niebla, sonidos y goteos: etapa 5. Las lámparas actuales solo están encendidas o fundidas.
- Sabueso: etapa 6.
- Espacio no euclidiano, no-clip de salida y victoria de campaña: etapa 7. Este laberinto sigue siendo euclidiano.

No se afirma que el aspecto final reproduzca todavía la referencia: esta entrega establece geometría conectada, materiales propios y entrada desde el lobby.
