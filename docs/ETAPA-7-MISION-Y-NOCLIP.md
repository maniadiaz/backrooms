# Etapa 7 — Tres fusibles y salida por No-Clip

> Actualización 1.1.1: este documento describe la implementación histórica con carrera. La salida actual requiere agacharse (Shift) y avanzar durante dos segundos, sin mínimo de hambre. Ver COMO-JUGAR.md.

Entrega 0.7.0 para Minecraft Java 1.20.1 / Forge 47.4.28. El usuario confirmó la misión cooperativa de tres fusibles y tres paneles.

## Objetivo compartido

Cada partida coloca tres estuches numerados y tres paneles, con texturas propias y señal de estado. Los estuches están separados dentro del primer sector de misión de 128 × 128 bloques; los paneles quedan juntos cerca de su salida. Las ubicaciones ocupan solo una de las dos líneas de los corredores abiertos del generador, sin cerrar sus conexiones. Los inicios posteriores usan el sector de la nueva ronda; no se depende de generar chunks nuevos para crear los objetos y funciona en mundos del generador 0.3 o posterior.

Pulsar un estuche entrega su fusible, numerado del uno al tres. Con él en la mano, pulsar el panel del mismo número lo consume y repara ese panel. El equipo comparte el progreso; cualquier integrante activo puede recoger o instalar. Se muestra `PANELES n / 3` en el HUD izquierdo y anuncios centrales al reparar/habilitar la salida. No se permite instalar desde lobby, limbo, espera o espectador, a distancia ni en una copia de panel colocada fuera de su ubicación de partida.

Los fusibles son objetos de misión; no habilitan la colocación general de bloques. Solo se modifica el estado del panel validado por el servidor. Se conserva la prohibición de romper/colocar bloques y se impide tirar fusibles al suelo. Los contenedores existentes pueden usarse para transferir objetos.

Cada objeto queda asociado a ronda, nivel, número y emisión. Un fusible incorrecto, de otra ronda o sustituido no sirve. La primera instalación es definitiva para ese panel; repetir clics o usar una copia no aumenta el progreso ni consume más objetos.

## Recuperación y reinicios

Si un fusible todavía lo lleva un participante activo o está en el inventario recuperable de alguien en limbo, el estuche no entrega otra copia. Al quedar eliminado definitivamente o desconectarse el portador, se puede volver al estuche y solicitarlo. También se puede recuperar un fusible abandonado en un contenedor: la nueva emisión invalida la anterior, de modo que no permite completar varios paneles con copias.

La comprobación considera los inventarios reales, el objeto llevado por el cursor de un menú y las instantáneas de limbo. El progreso instalado y las emisiones se guardan durante la ronda. Reiniciar, perder o cerrar/reabrir el mundo sigue la política de comenzar desde cero; la nueva misión no acepta los objetos antiguos. La transición de prueba a otro nivel reinicia también los objetivos.

## Activación de la salida

Reparar los tres paneles habilita las paredes anómalas existentes. Se necesita mirar hacia una, estar realmente en contacto con ella y mantener avance con intención de carrera durante cuarenta ticks consecutivos, aproximadamente dos segundos. El jugador debe estar activo, vivo, sin vuelo/espectador y con más de seis puntos de hambre, el umbral normal para correr. Soltar avance, perder contacto, apartar la mirada, cambiar de pared o dejar de cumplir las condiciones reinicia la carga.

Minecraft cancela automáticamente la carrera al chocar con una pared sólida. El cliente conserva la intención de carrera mientras se mantenga avance después de correr, o al mantener las teclas de avance y carrera. Envía un estado sencillo con latidos cada diez ticks. El servidor no recibe una orden de completar: verifica pared, contacto, estado, misión, hambre y cuarenta ticks. Si deja de recibir intención válida durante más de quince ticks, la carga no continúa. Los mensajes no permiten acelerar el reloj del servidor.

Al terminar se aplica una pantalla negra de veinte ticks al equipo y se programa la transición. Durante ese segundo el equipo está protegido del daño. Al acabar, un participante todavía activo confirma la transición; si no queda ninguno se cancela esa finalización. No se cambia globalmente la colisión ni se permite caminar a través de otras paredes.

Hasta que exista el Nivel 1, el destino es el lobby para todo el equipo. Las salidas por clic derecho y el antiguo botón del área activa dejan de completar la partida. `/bedroom testnext` conserva su función administrativa de probar descanso/avance y omite intencionadamente la misión; no representa una salida jugable ni el Nivel 1.

## Recursos, compatibilidad y verificación

`python scripts/generate_mission.py` crea doce texturas/modelos de bloques numerados, sus variantes de estado y el icono de fusible. No requiere bibliotecas externas. El protocolo Forge de Bedroom pasa de `2` a `3` por los nuevos campos del HUD y mensajes de intención/transición: todos los jugadores y el anfitrión deben usar `bedroom-0.7.0.jar`.

Compilación correcta y 56 pruebas obligatorias aprobadas con `scripts/gradle.ps1 build runGameTestServer --console=plain`. Las seis nuevas pruebas cubren instalación/consumo/idempotencia, rechazo de panel incorrecto, reserva de inventario de limbo, recuperación/invalidez de copias, persistencia/reinicio, carga continua, contacto real, hambre/estado, corredores abiertos y codificación/decodificación del HUD. Se mantienen las cincuenta pruebas previas.

El cliente arrancó y recargó las nuevas variantes de modelos/texturas y el motor de audio sin errores de Bedroom ni modelos de misión ausentes en el registro. Se cerró ese cliente de prueba después del arranque; no se verificó visualmente una partida completa ni la transición en pantalla.

Pendiente de una partida real: distribución y facilidad para encontrar estuches/paneles, legibilidad del HUD y números, respuesta de las teclas de carrera al chocar, presentación de la pantalla negra y finalización con dos a cuatro clientes mediante Essential. La suite del servidor no sustituye esa revisión de interfaz ni una prueba de red real.

La siguiente etapa es construir el Nivel 1 y conectar la progresión real, aprovechando la recuperación del equipo y el descanso de noventa segundos existentes. El lobby final de festejo sigue pendiente.
