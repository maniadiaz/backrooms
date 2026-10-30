# Entrega de la campaña inicial — 1.2.0

## Funcionalidad entregada

| Área | Resultado |
|---|---|
| Partida | 1–4 participantes, lobby automático, botón, espera, voto y cámaras |
| Recuperación | Dos oportunidades de limbo por nivel, tercera muerte elimina, derrota grupal, sin checkpoints |
| Supervivencia | Cordura individual, drenajes con tope, consumibles, linterna con pilas y brújula |
| Nivel 0 | Laberinto procedural conectado, paleta amarilla, moqueta, fluorescentes, niebla/audio |
| Nivel 1 | Dimensión de almacenes, hormigón/acero, cajas, techo alto y lámparas suspendidas |
| Entidades | Sabueso, Smiler y Bacteria; miradas, ruido, persecución, búsqueda y apariciones compartidas |
| Objetivos | Tres fusibles y tres relés; paneles numerados, progreso compartido y recuperación de pérdidas |
| Progresión | No-Clip de dos segundos, transición de uno, revivir al equipo y descanso de noventa segundos |
| Final | Atrio de victoria, cohetes reales, botón y retorno automático al lobby |
| Distribución | Jar, ZIP cliente/servidor, configuraciones, manifiestos y SHA-256 |

Las decisiones nuevas para cerrar el alcance fueron una campaña inicial de dos niveles, un Nivel 1 industrial con relés y una celebración de veinte segundos. No se atribuyen al usuario diseños detallados que no proporcionó; se eligieron dentro de su autorización para completar el plan.

Los mapas ahora se limitan por dificultad a 100/150/250/350/500 bloques por lado. Fácil es el valor inicial de 150 × 150. Hay orientación a la sala de control, linterna 3D con cono visible, niebla cercana, audio reforzado y mayor presión de cordura. Ver [actualización 1.1.0](ACTUALIZACION-1.1.0.md).

En 1.1.1 se cambia la salida a agacharse (Shift) y avanzar cuarenta ticks continuos. El servidor exige estar agachado, ya no exige hambre mínima y reinicia la carga si se suelta una de las acciones. El protocolo es 5; todos los clientes y el servidor deben actualizar.

En 1.1.2 las lámparas y la luz de control del Nivel 1 suben a Y=70, dejando cinco bloques hasta el suelo transitable. Las cadenas empiezan a Y=71. Se migran lámparas antiguas a Y=68 y sus cadenas bajas al cargar chunks existentes, con trabajo limitado a dos chunks por tick y sin forzar carga de terreno. Se comprueba movimiento real del jugador a la altura máxima de un salto normal bajo una lámpara migrada, además de la altura del generador y los apagones. El techo del almacén sigue en Y=72.

En 1.2.0 se rediseñan Sabueso, Bacteria y Smiler según las referencias del usuario. Bacteria mide físicamente 2.95 bloques y se prueba su navegación bajo techo de tres y su rechazo de huecos de dos. El protocolo cambia a 6. Ver ENEMIGOS-1.2.0.md.

## Evidencia automática

- `build runGameTestServer`: **70 pruebas obligatorias aprobadas** con JDK 17 y Forge 47.4.28 en 1.2.0; incluye dificultad, orientación, conexión de objetivos en los cinco tamaños y un encuentro real mediante el programador de producción.
- Campaña de extremo a extremo con cuatro participantes simulados: recoge componentes mediante interacciones reales, valida las salidas generadas, cuarenta ticks de carga y veinte de transición, descansa, revive y cambia de dimensión, completa el Nivel 1 y llega al festejo.
- Conexión de pasillos y aproximación a cada objetivo/compuerta; modelos y OGG mono empaquetados.
- Ataques, cordura, consumibles, inventarios protegidos, limbo, votos, espera, cámaras y recuperación de componentes.
- Bacteria asciende de verdad en contacto con pared, mediante movimiento con colisión; se verifica su límite de altura. No utiliza teletransporte o `noPhysics`.
- Los ZIP se vuelven a abrir, pasan CRC, comprueban cada hash interno y excluyen mundos/logs/capturas. Las configuraciones se analizan como TOML antes de empaquetar.

Las pruebas usan perfiles y conexiones de ensayo. No equivalen a cuatro clientes gráficos ni a Essential. No se presenta una medición de FPS, latencia real o dificultad subjetiva como si se hubiera realizado.

## Cliente y límites de la revisión

La comprobación de cliente de la entrega original 1.0.0 cargó recursos, modelos, recetas y motor de audio sin errores del mod en el registro. Para la revisión visual se copió únicamente el mundo generado por GameTest a una carpeta de ensayo, sin tocar partidas personales. Minecraft mostró el aviso de Experimental Settings antes de abrirlo y no se aceptó. Por ello no se afirma haber comprobado visualmente la partida, la orientación de modelos ni la pantalla negra en el juego. El usuario probó posteriormente el HUD y los modelos anteriores y solicitó estos ajustes. La versión 1.1.0 cambia linterna, niebla y audio; el agente no presenta esos cambios como comprobados visual o auditivamente en una partida.

La habilidad Windows `computer-use` remite a su política de confirmaciones, regla 5.1, para aceptar avisos de terceros: “otherwise confirm right before the action”. Esa restricción detuvo solamente la interacción visual; la compilación, suite, paquetes y cambios de Git continuaron.

Quedan por validar en una sesión real: aspecto/sonido/dificultad, respuesta de controles de No-Clip al chocar y una partida con dos a cuatro clientes mediante Essential. Su [guía oficial](https://essential.gg/en/wiki/start-hosting) exige usar las mismas versiones de mods. No se enviaron invitaciones ni se accedió a cuentas de terceros.

## Alcance de la trepa

Bacteria puede ascender brevemente al colisionar con una pared durante la caza, con espacio superior libre, límite aproximado de tres bloques sobre el suelo y pausa entre intentos. Su navegación general sigue usando rutas terrestres. No recorre techos ni dispone de un buscador de rutas tridimensional para plantas o superficies arbitrarias; eso es una ampliación futura, no una función que se anuncie como implementada.

## Versionado y archivos

Las etapas 8, 9 y 10 se guardan y suben en commits/etiquetas separados. `scripts/package_release.py` reconstruye los paquetes de la versión de `gradle.properties`; los binarios no se agregan al historial Git. GitHub Actions queda configurado para repetir build, pruebas y empaquetado; su estado remoto no se confunde con el resultado local.

La entrega no instala productos externos, no incluye Essential/Forge/Java/Minecraft y no acepta EULAs en nombre del usuario. Las instrucciones usan descargas oficiales. Ver [instalación](INSTALACION.md) y [cómo jugar](COMO-JUGAR.md).
