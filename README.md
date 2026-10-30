# Backrooms 1.4.0

Campaña cooperativa de terror para **Minecraft Java 1.20.1**, **Forge 47.4.28** y **Java 17**, con uno a cuatro participantes.

Repositorio: [maniadiaz/backrooms](https://github.com/maniadiaz/backrooms). Renombrado desde Bedroom conservando todos sus commits y etiquetas. El identificador técnico `bedroom`, sus dimensiones, datos guardados y archivos de configuración se conservan para mantener la compatibilidad. El nombre visible y los paquetes son **Backrooms**. `/backrooms` es el comando principal; `/bedroom` sigue funcionando.

La campaña recorre el laberinto amarillo del Nivel 0 y los almacenes de Zona de Carga, con un desvío opcional por la dimensión Nivel 0.1: Darkrooms. Incluye lobby automático, espera/espectadores, votaciones, limbo, cordura individual, consumibles, linterna, loot, tres entidades, misiones cooperativas, salida por No-Clip, descanso y victoria con fuegos artificiales.

**Nuevo en 1.4.0:** diez vidas para toda la partida, recuperación de tres minutos en el mismo mapa con puerta y latidos, checkpoints manuales persistentes y elección desde el lobby. Se corrigen los pilares, se fija la comida, se oculta F3 y se refuerza la estructura a tres bloques. [Reglas y uso del guardado](docs/REGLAS-Y-CHECKPOINTS-1.4.0.md).

**Nuevo en 1.3.0:** cinco misiones de notas, escondite, colores, traje Hazmat y radiación sustituyen a los fusibles del Nivel 0. La puerta blindada lleva a Zona de Carga; Shift + avance contra la pared Glitch lleva a Darkrooms. [Campaña y decisiones técnicas](docs/CAMPANIA-NIVEL-0-1.3.0.md).

**Nuevo en 1.2.0:** rediseño de los tres enemigos: Sabueso encorvado con mandíbula y garras, Bacteria humanoide bípeda de tres bloques y Smiler como esfera negra con sonrisa roja, dientes amarillentos y ojos blancos. [Detalles y límites de validación](docs/ENEMIGOS-1.2.0.md).

**Nuevo en 1.1.2:** las lámparas del Nivel 1 suben dos bloques, con actualización de las zonas existentes al cargarlas, para correr y saltar sin chocar.

**Nuevo en 1.1.1:** la salida se activa con agacharse (Shift) y avance durante dos segundos, sin necesitar comida para correr.

**Nuevo en 1.1.0:** mapas limitados por dificultad (100/150/250/350/500 bloques por lado), orientación a los paneles en el HUD, linterna 3D con haz visible, niebla a 28 bloques, audio reforzado y cordura más exigente. Los encuentros son por tiempo y vuelven a intentar pronto si no encuentran espacio. [Cambios y balance detallados](docs/ACTUALIZACION-1.1.0.md).

## Jugar e instalar

- [Instalación de cliente, Essential y servidor dedicado](docs/INSTALACION.md).
- [Controles, reglas, objetos y entidades](docs/COMO-JUGAR.md).
- [Informe de entrega, pruebas y límites de validación](docs/ENTREGA-FINAL.md).

Compilar produce `build/libs/backrooms-1.4.0.jar`. El empaquetador crea `dist/backrooms-1.4.0-client.zip`, `dist/backrooms-1.4.0-server.zip` y `dist/SHA256SUMS.txt`. Son paquetes de directorio para instalación manual; no incluyen Minecraft, Forge, Java ni Essential. Todos deben actualizar a la misma versión del mod.

Al entrar se llega al lobby. El anfitrión pulsa el botón del bloque verde. Encontrar la Nota #1, beber Agua de Almendras, esconderse diez segundos, resolver Azul-Amarillo-Rojo-Verde, conseguir el traje Hazmat completo y activar la consola radiactiva. Leer la Nota #5 y elegir puerta con llave hacia Zona de Carga o Shift + avance contra una pared Glitch hacia Darkrooms. Darkrooms vuelve a conectar con Zona de Carga por su salida. Pantalla negra de un segundo, recuperación del equipo y descanso de noventa segundos o botón para salir antes. En Zona de Carga se recuperan tres relés. Su compuerta conduce al atrio de festejo y después al lobby.

Hay diez vidas por jugador para toda la partida, sin reinicio al cambiar de nivel. Cada muerte recuperable da tres minutos dentro del mismo mapa para buscar una puerta siguiendo latidos. Agotar las diez vidas elimina del intento; agotar la recuperación espera al siguiente nivel. El terminal GUARDAR, Esc → Checkpoint del mundo → Guardar checkpoint o `/backrooms save` guardan misiones, inventarios, vidas y punto de regreso. Al reabrir se entra al lobby: verde empieza desde cero y azul carga el último guardado. Los checkpoints son manuales y persisten al perder o reiniciar. `/backrooms leave` conserva el inventario externo protegido.

El Sabueso reacciona a mirada/movimiento, Smiler a observadores válidos y Bacteria a ruido/visión con búsqueda por última posición y trepa limitada. Comparten un presupuesto de encuentros raros. Las cajas y archivadores contienen comida y suministros; los componentes de misión perdidos pueden recuperarse sin dejar la partida bloqueada.

## Comandos

`/backrooms start`, `/backrooms save`, `/backrooms continue`, `/backrooms lobby`, `/backrooms status`, `/backrooms leave`, `/backrooms restart`, `/backrooms vote restart`, `/backrooms vote continue`, `/backrooms spectate` y sus variantes `next`, `previous`, `off`. Las cámaras también se cambian con `]` y `[`, reasignables. El HUD permanece a la izquierda con estado encima de muertes.

En el lobby: `/backrooms difficulty principiante|facil|normal|dificil|hardcore` (elegir una opción, sin barras), solo anfitrión u operador. Fácil inicia en 150 × 150. Durante la partida: `/backrooms objective` orienta al siguiente objetivo y `/backrooms journal` muestra las notas descubiertas.

Los comandos `testdeath`/`testnext` y la sala de desarrollo requieren operador y `developmentCommands=true`; el paquete de distribución los desactiva. Los botones y las misiones normales no requieren operador.

## Desarrollo y comprobaciones

Usar JDK 17 y Gradle wrapper 8.8. En este equipo, `scripts/gradle.ps1` selecciona el JDK local de `.tools/java` si existe.

```powershell
.\scripts\gradle.ps1 build runGameTestServer --console=plain
python scripts/package_release.py
```

En Linux/CI: `bash gradlew build runGameTestServer --console=plain`. El empaquetador utiliza Python 3.11 o posterior, biblioteca estándar. Los recursos generados ya están versionados; compilar no requiere herramientas de síntesis de audio.

Las 81 pruebas obligatorias verifican reglas, inventarios, geometría, entidades, cinco misiones, escondites, radiación, bifurcación por Darkrooms, red/HUD, dificultad, orientación, encuentros por tiempo y una campaña completa con cuatro participantes simulados. Se verifica la conexión de objetivos dentro de los cinco tamaños en ambos niveles. También se verifican checkpoint en disco, recuperación, diez vidas, hambre fija, pilares y estructura de tres bloques. Esto no equivale a cuatro clientes ni a una sesión Essential. Quedan pendientes la revisión visual/auditiva de estos cambios y la validación de red con amigos; el informe distingue esas comprobaciones de las automatizadas.

Las pruebas usan exclusivamente `run-gametest/`: cada ejecución borra su mundo de ensayo para evitar geometría antigua. No guardar partidas personales allí. Mundos, logs, caches, `.tools/`, `build/` y `dist/` se excluyen de Git. El workflow de GitHub recompila, ejecuta la suite y empaqueta artefactos; su resultado remoto se revisa por separado del resultado local.

## Recursos y configuración

Texturas y modelos originales mediante `generate_textures.py`, `generate_mission.py` y `generate_warehouse.py`. Los sonidos mono propios y las pieles de entidades se regeneran con `generate_audio.py`, `generate_hound.py`, `generate_smiler.py` y `generate_bacteria.py`; para estos últimos instalar `scripts/audio-requirements.txt` en desarrollo. Jugar no requiere Python. Los generadores preservan eventos de las otras especies.

`bedroom-common.toml`: entrada automática, ayuda de compañía, apagones, parpadeo rápido y variante letal de Smiler. `bedroom-client.toml`: volumen del zumbido, niebla y movimiento opcional de cámara. El paquete deja el movimiento de cámara y parpadeo rápido desactivados. Bajar volumen no cambia la cordura; los apagones sí cambian la luz real.

[Avisos de recursos y dependencias](THIRD_PARTY_NOTICES.md). Se conserva la licencia `All Rights Reserved` del proyecto.

## Entregas por fase

- [Etapa 1: alcance original](docs/ETAPA-1-ALCANCE.md).
- [Etapa 7: fusibles y No-Clip](docs/ETAPA-7-MISION-Y-NOCLIP.md).
- [Etapa 8: Nivel 1 y progresión](docs/ETAPA-8-NIVEL-1.md).
- [Etapa 9: victoria](docs/ETAPA-9-VICTORIA.md).
- [Etapa 10: cierre 1.0.1](docs/ETAPA-10-ENTREGA.md).
- [Bestiario](docs/BESTIARIO.md), [partidas y lobby](docs/PARTIDAS-Y-LOBBY.md), [control de versiones](docs/CONTROL-DE-VERSIONES.md).

Los documentos de etapas anteriores describen sus versiones históricas; el estado vigente es esta entrega y su informe final. La campaña inicial termina tras dos niveles. No incluye navegación de entidades por techos, campaña narrativa extensa, voces de actores ni niveles adicionales no diseñados.
