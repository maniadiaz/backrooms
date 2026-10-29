# Etapa 2.1 — Prototipo de partidas

Versión: `0.2.1`, Minecraft Java `1.20.1`, Forge `47.4.28`.

## Implementado

- Una partida por servidor, de 1–4 participantes. Entrada voluntaria con `/bedroom lobby`, sin permisos de operador. El primer participante es anfitrión.
- Lobby cerrado con botón físico para iniciar. El servidor verifica anfitrión, capacidad, conexión del grupo y estado; pulsaciones repetidas no duplican la partida.
- Inventario externo, efectos, experiencia, salud, hambre, modo de juego y origen guardados aparte del inventario de partida. `/bedroom leave` los restaura; no exporta objetos de la partida.
- Nueva partida sin objetos ni progreso anterior, con un nuevo sector de prueba para no reutilizar modificaciones de la partida previa. El kit inicial se añadirá con los objetos de etapa 4.
- Primera y segunda muerte interceptadas antes de la muerte vanilla: sin drops ni pantalla de reaparición, traslado al limbo blanco y 600 ticks de oportunidad. Se preservan los objetos de partida, inaccesibles dentro del limbo.
- Laberinto blanco de recodos, independiente por puesto de jugador, con botón de salida. Volver a tiempo restaura inventario, salud y el estado activo; conserva el contador de muertes.
- Tercera muerte o vencimiento del limbo: modo espectador, cámara fijada al primer compañero activo conectado. Cuando no hay compañero activo, se mantiene al espectador en su zona de espera.
- Si todos están en limbo, continúa la oportunidad de rescate. Si nadie está activo y conectado ni tiene tiempo de limbo, derrota, limpieza de partida y regreso al lobby.
- Botón de salida del nivel provisional: el primer jugador activo devuelve a todo el grupo al lobby. Todavía no es la pared de falla real.
- `/bedroom testnext` para operadores permite verificar el avance: todos pasan al descanso, recuperan salud y reinician muertes. Después de 1800 ticks, o del botón del anfitrión, entran en otra sala provisional. No se afirma que exista Nivel 1.
- Relojes y estado mostrados por mensaje sobre la barra rápida, con autoridad del servidor. Las duraciones son segundos de juego a 20 TPS.
- Modo aventura y eventos de protección para participantes; también se protegen los bloques de la dimensión frente a jugadores no creativos y explosiones. Fuera de la dimensión, los jugadores ajenos a la partida mantienen sus reglas normales.
- Sin uso de bloques arbitrarios, interacción con entidades o recogida/lanzamiento de objetos fuera del estado activo. La etiqueta de contenedores `bedroom:containers` está vacía hasta implementar los contenedores propios. No hay excepción de misiones habilitada aún.
- Guardado NBT mediante `SavedData` en el Overworld: participantes, anfitrión, puestos estables, inventarios separados, estados, muertes, tiempos y ubicaciones. Guardado periódico de inventario y posición, además de guardado al desconectar.
- Reconectar en nivel o limbo recupera la ubicación si sigue siendo válida dentro de su sala, sin reiniciar vidas ni reloj. Si no es válida, se usa la entrada segura de esa sala.
- En servidor dedicado el tiempo de limbo sigue con el jugador desconectado. Los participantes activos desconectados no sostienen por sí solos una partida recuperable. El cierre normal del servidor pausa la resolución; guardar y salir de un mundo individual no se trata como derrota.
- Al cargar una partida guardada tras reiniciar el servidor, los relojes esperan a que reconecte el primer participante. Evita declarar derrota antes de que alguien tenga oportunidad de volver. Desde esa reconexión se retoman las reglas normales de desconexión.
- Los puestos desconectados se liberan al volver al lobby y sus inventarios externos quedan pendientes de devolución al reconectar. No bloquean futuras partidas. El anfitrión pasa a un participante conectado.

## Arquitectura

- `MatchData`: reglas y serialización de la partida, sin efectos visuales ni teletransportes.
- `MatchController`: eventos Forge, comandos, inventarios, botones, cambios de estado, protección y cámara.
- `MatchRooms`: construcción de salas provisionales y laberintos de limbo.
- `MatchGameTests`: pruebas de reglas y de integración de eventos con jugadores simulados de Forge.

Las decisiones complementarias del documento de diseño se han usado como base de implementación en los puntos anteriores. No se convierten por ello en requisitos confirmados por el usuario y pueden ajustarse.

## Pruebas automatizadas

Ejecutar `scripts/gradle.ps1 build runGameTestServer`.

Resultado de la verificación de esta entrega: compilación y reobfuscación correctas (`BUILD SUCCESSFUL`); nueve pruebas requeridas aprobadas, sin fallos.

La batería incluye nueve GameTests (el original de la sala y ocho de partidas):

1. Registro de dimensión, geometría de sala original y conservación de modificaciones.
2. Capacidad de cuatro, rechazo del quinto, permisos de anfitrión, inicio con dos, conexión del grupo y rechazo de doble inicio.
3. Dos regresos de limbo, tercera muerte a espectador y nuevo inicio limpio en solitario.
4. Equipo entero en limbo, superviviente que regresa y límite exacto de 600 ticks.
5. Fin de nivel, reanimación, rechazo de doble finalización, 1800 ticks de descanso e inicio anticipado.
6. Serialización de reloj, vidas, ubicación e inventarios; cambio de anfitrión, puestos estables y devolución pendiente de inventarios al liberar desconectados.
7. Entrada y salida con jugador simulado: inventario externo, XP, modo de juego y traslado a la dimensión.
8. Eventos Forge de muerte y clic en botón de limbo: inventario oculto/restaurado, contador conservado, tercera muerte a espectador, rotura bloqueada y raíz de comando accesible sin operador.
9. Búsqueda de ruta por el laberinto blanco: salida accesible, recorrido con desvíos y botón físico presente.

## Verificación manual pendiente

Los GameTests no sustituyen una sesión con cuatro clientes reales. Comprobar:

- Clics, HUD de cuenta atrás y controles desde un cliente normal sin permisos.
- Sincronización y cámara de espectador entre dos y cuatro clientes, incluido intentar abandonar la cámara con los controles vanilla.
- Cerrar y volver a abrir el mundo individual durante el limbo; desconectarse y reconectarse en un servidor dedicado durante los mismos 30 segundos.
- Cierre normal de servidor y reconexión conservando estados y objetos; devolución externa de un jugador desconectado durante el final.
- Inventario con armadura, mano secundaria, efectos y menús abiertos al morir o entrar/salir.
- Prohibición de colocar/romper y de usar mecanismos no autorizados, sin modificar las reglas de otros jugadores en el Overworld.
- Partida completa con derrota, reinicio y salida provisional; transición de prueba con participantes en limbo y espectando.

## Límites

El Nivel 0 sigue siendo una sala. El lobby, descanso y limbo tienen geometría provisional. Los mensajes de prototipo están en español; faltan señalización final y traducciones de esas instrucciones.

No se implementan todavía cordura, kit inicial, objetos de misión, contenedores, entidad, pared de falla, Nivel 1, lobby de festejo ni fuegos artificiales. El descanso entre niveles puede comprobarse mediante el comando de desarrollo, porque no hay segundo nivel real.

El regreso al mundo externo conserva el punto original y presupone que siga transitable. El guardado está pensado para cierres normales; no garantiza transacciones de inventario frente a cortes de energía. Los sectores de partidas anteriores permanecen en el mundo; la limpieza de regiones se revisará con la generación procedural.

Próxima etapa: generación conectada del Nivel 0 y recursos propios, conservando este gestor como base de la progresión.
