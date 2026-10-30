# Backrooms 1.4.0 — diez vidas, recuperación y guardado

## Diez vidas para toda la partida

Cada participante comienza con diez vidas. Cada muerte consume una y el contador no se reinicia al pasar de Nivel 0 a Darkrooms o Zona de Carga. Las primeras nueve muertes dan una oportunidad de recuperación. La décima deja al jugador como espectador para el resto del intento, aunque el equipo complete otro nivel. Empezar una partida desde cero restablece diez vidas.

Se conserva la regla previa de fracaso de recuperación: si el jugador no encuentra la puerta a tiempo, espera como espectador hasta que el equipo termine ese nivel. Entonces revive si todavía tiene vidas. Si nadie sigue activo ni tiene una recuperación pendiente, la partida termina y vuelve al lobby. El checkpoint manual se conserva incluso tras esa derrota.

## Recuperación dentro del mismo mapa

Ya no se envía al laberinto blanco. El jugador se queda dentro de la misma dimensión y arena, sin inventario visible y con tres minutos para localizar una puerta personal. Las entidades no lo seleccionan como objetivo y no recibe daño durante esta fase. Visión nocturna temporal permite recorrer también Darkrooms sin la linterna del inventario guardado.

Una búsqueda de caminos sobre los bloques cargados elige una posición libre y conectada, normalmente hasta 64 pasos de recorrido desde el punto de muerte. No se coloca la puerta al extremo de un mapa de 500 × 500. La puerta se representa solo para su propietario y no añade obstáculos al mapa de sus compañeros. Al caminar hacia ella, el servidor valida la cercanía y devuelve al jugador a su último punto guardado, o a la entrada del nivel si no guardó uno para ese nivel. Recupera su inventario y conserva la vida gastada.

El latido es mono y posicional, proyectado en dirección a la puerta para percibir izquierda/derecha. Su volumen y frecuencia aumentan al acercarse. Respeta los controles de sonido de Minecraft. El contador de tres minutos y las vidas se muestran en el HUD.

## Guardar y continuar

Cada mundo tiene **un slot manual de checkpoint**, separado de los datos de la partida en vivo. El anfitrión guarda para el equipo:

- Usar el terminal verde marcado **GUARDAR**, situado junto a la entrada y en el sector central de cada nivel.
- Abrir **Esc → Checkpoint del mundo → Guardar checkpoint**. El botón vuelve al juego para que el servidor integrado procese el guardado.
- Usar `/backrooms save`.

No se guarda durante la pantalla de transición ni mientras alguien está dentro del casillero de la misión de escondite. El guardado se escribe en `data/backrooms_checkpoint.dat` del mundo e incluye nivel, dificultad, misiones, secuencia de colores, inventarios, salud, cordura, carga de linterna, vidas gastadas y puntos seguros de los integrantes presentes. Si alguien está recuperándose, también conserva su posición, puerta y tiempo restante. Es un guardado de progreso de campaña y jugadores; no es una copia completa para deshacer todos los cambios de bloques y contenedores del mundo.

Después de guardar se puede cerrar el mundo normalmente. Al abrirlo, se llega al lobby:

- **Botón verde:** empezar desde cero (`/backrooms start`).
- **Botón azul:** continuar el último checkpoint (`/backrooms continue`).
- Desde el menú de pausa del lobby también están **Empezar desde cero** y **Último checkpoint**.

Solo el anfitrión elige la partida. Los miembros del lobby deben estar conectados. Se restauran los datos de quienes tienen el mismo UUID en el guardado; un nuevo compañero puede incorporarse al cargar desde el lobby y recibe equipo inicial. No se sustituye el inventario externo actual de Minecraft por una copia antigua del checkpoint. Las criaturas del intento anterior se retiran antes de continuar. Un contador de rondas conserva un máximo histórico para que una nueva partida no reutilice por accidente una arena de un intento posterior al guardado cargado.

Empezar desde cero, perder, volver al lobby o cerrar el mundo no borra el slot manual. Volver a guardar lo reemplaza. Sin un checkpoint explícito se mantienen las reglas anteriores de reinicio al cerrar o desconectarse; los invitados que regresan a una partida activa esperan en el lobby y se conserva el voto de reinicio.

## Puzle de la Nota #2

El fallo de interacción procedía de aceptar solo el bloque inferior del pilar: a la altura normal de la vista, se seleccionaba el bloque superior y no ocurría nada. Ahora ambas alturas son interactivas y muestran el color y número. Se ignora la mano secundaria y se agrupan clics repetidos sobre el mismo pilar en una ventana de medio segundo, para que un gesto no cuente dos veces. Cada color aceptado confirma el progreso y el HUD indica el siguiente. Se conserva Azul → Amarillo → Rojo → Verde y el requisito de leer la Nota #2 tras completar la misión anterior.

El almacenamiento y su compuerta tienen tres bloques de profundidad. Acertar elimina las tres capas del acceso; no queda una pared oculta detrás.

## Hambre, coordenadas y estructura

La comida queda fija en veinte puntos para participantes y observadores del mod; no es necesario comer ni se acumula agotamiento. Se mantiene saturación cero para no añadir curación acelerada por saturación alta. El Agua de Almendras sigue siendo un recurso de cordura y las barras conservan su regeneración opcional. Se retiran las barras de la entrega inicial porque ya no se requieren para alimentarse.

El cliente oculta el panel de depuración/coordenadas F3 mientras el HUD de partida está visible, incluido el lobby, y se retiran las coordenadas numéricas de `/backrooms objective`. La orientación usa dirección y distancia.

Las paredes procedurales pasan a bloques sólidos y se ensanchan a tres bloques; se conservan los cruces de circulación de dos bloques de ancho. El perímetro, los muros del almacén, el almacenamiento y la puerta blindada también se refuerzan. Suelo y techo tienen tres capas. Se conservan las alturas interiores: Nivel 0 y Darkrooms de tres bloques, y Zona de Carga con luminarias a cinco bloques del suelo. La geometría nueva se aplica al generar arenas nuevas; no se reescriben masivamente los chunks antiguos del mundo.

## Validación

81 GameTests: nueve recuperaciones y décima muerte, temporizador de tres minutos, vidas entre niveles, recuperación en la misma arena, puerta alcanzable, escritura del checkpoint a disco, carga desde lobby, inventario/salud/misiones/vidas, guardado durante recuperación, limpieza de varias criaturas, interacción real con la parte superior de los pilares, duplicados de manos, hambre, tres capas de suelo/techo y tabiques, rutas de misión y progresión por ambas salidas. Se conservan las pruebas de inventarios externos y campaña con cuatro perfiles simulados.

El arranque del cliente comprueba carga de recursos. La presentación del menú, la puerta, los latidos, F3 y una sesión de varios clientes Essential todavía deben revisarse jugando. El protocolo pasa a 8: todos los clientes y el servidor deben instalar 1.4.0. Los checkpoints manuales se introducen en esta versión; no existe un guardado previo automático de una partida 1.3.0. No hace falta borrar el mundo.
