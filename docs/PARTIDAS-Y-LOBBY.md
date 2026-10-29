# Partidas, lobby y progresión

Estado: diseño de referencia. El prototipo 0.2.1 implementa el gestor de partidas con salas provisionales; véase [implementación, pruebas y pendientes](ETAPA-2.1-PARTIDAS.md). Cordura, misiones y victoria de campaña siguen pendientes. Las reglas explícitas del usuario sustituyen las propuestas anteriores de acceso por objeto y reaparición libre en Nivel 0.

## Reglas confirmadas

- Una partida admite de uno a cuatro jugadores y se inicia presionando un botón en el lobby.
- No se pueden romper ni colocar bloques. Solo se pueden colocar objetos específicos de misiones.
- Primera y segunda muerte de cada nivel: traslado a un laberinto blanco llamado limbo, con 30 segundos para encontrar la salida.
- Tercera muerte del mismo nivel: espectador directo, sin nuevo limbo. Esta interpretación fue confirmada expresamente por el usuario.
- Si no se encuentra la salida del limbo a tiempo, el jugador queda como espectador de sus compañeros.
- Al completar un nivel, todos reviven y pasan al siguiente. Hay 90 segundos de descanso o un botón para comenzar de inmediato.
- Si muere todo el equipo, derrota y regreso al lobby; la siguiente partida empieza desde cero.
- Mientras no exista Nivel 1, la salida del Nivel 0 devuelve al lobby.
- Completar todos los niveles de la campaña terminada lleva a un lobby de festejo con fuegos artificiales.
- La cordura es individual; los drenajes ambientales se suman con un máximo de 3 puntos cada 10 segundos. La energética descuenta 5 puntos una sola vez al vencer.

## Decisiones complementarias propuestas

Estas reglas completan casos no especificados. Son la base propuesta de implementación, no confirmaciones del usuario.

| Caso | Propuesta |
| --- | --- |
| Todos en limbo | Conservar la partida mientras exista un participante en limbo con tiempo restante. Derrota cuando no quede nadie activo ni con oportunidad de regresar. En solitario se puede intentar el limbo. La lectura literal de derrota al morir el último jugador haría imposible usarlo en solitario. |
| Completar nivel | La primera salida válida de un participante activo completa el nivel para todo el equipo; cancela limbos pendientes y rescata espectadores. No se exige que todos encuentren la salida. |
| Inicio desde lobby | El anfitrión de la partida usa el botón; se admite comenzar con 1–4 participantes, se congela la lista al iniciar y los clics repetidos no crean partidas duplicadas. |
| Inicio tras descanso | El anfitrión o un jugador activo designado al desconectarse aquel puede omitir los 90 segundos con el botón. |
| Entrada tardía | Un jugador que no pertenecía al grupo espera en el lobby; no entra en mitad de un nivel. No se incluye gestión de varias partidas simultáneas en V1. |
| Desconexión | Guardar el estado; reconectarse no concede vidas ni reinicia temporizadores. En servidor activo, el tiempo del limbo y del descanso sigue corriendo. Un integrante desconectado fuera de limbo no mantiene por sí solo una partida recuperable. |
| Cierre del servidor | Guardar tiempos restantes; al reabrir se reanuda, sin descontar tiempo fuera de línea. |
| Inventario al morir | Guardar inventario de partida sin soltarlo al suelo; queda inaccesible en limbo y espectador y se devuelve al revivir. Evita duplicación y pérdida de objetos obligatorios de misión. |
| Regreso del limbo | Salud completa, cordura mínima de 30 si estaba por debajo, hambre e inventario conservados; posición segura del nivel. No reiniciar el contador de muertes. |
| Inicio de otro nivel | Todos activos con salud y cordura al 100%, muertes en cero e inventario conservado. Reiniciar peligros y temporizadores del nivel anterior. No volver a entregar el kit inicial. |
| Nueva partida | Inventario de partida vacío y nuevo kit inicial; cordura y salud completas, muertes en cero, objetivos y loot reiniciados y una nueva zona o semilla. El inventario externo al modo de juego se guarda por separado y no se destruye. |
| Límite exacto del limbo | Salir requiere tiempo restante mayor que cero. Si la salida ocurre al mismo instante que el vencimiento, el vencimiento tiene prioridad. |
| Salida y muerte simultáneas | Una salida ya validada resuelve el nivel una sola vez; después se ignoran eventos atrasados del nivel anterior. La validación requiere que el jugador siga activo. |
| Festejo | Efecto visual de fuegos artificiales sin daño ni destrucción, con botón de regreso al lobby principal. |

Estas propuestas deben poder revisarse antes de implementar el sistema correspondiente. No requieren cambios en las reglas del Overworld ajenas a la partida.

## Estados previstos para el código

Partida:

`LOBBY → STARTING → LEVEL_ACTIVE → INTERMISSION → LEVEL_ACTIVE`

Ramas de salida:

- `LEVEL_ACTIVE → DEFEAT → LOBBY` cuando no hay participantes recuperables.
- `LEVEL_ACTIVE → PROTOTYPE_COMPLETE → LOBBY` mientras no exista siguiente nivel.
- `LEVEL_ACTIVE → VICTORY → CELEBRATION_LOBBY` al terminar una campaña definida como completa.

Jugador:

- `ACTIVE → LIMBO` en muerte 1 o 2.
- `LIMBO → ACTIVE` al salir antes del vencimiento.
- `LIMBO → SPECTATING` al vencer el plazo.
- `ACTIVE → SPECTATING` en muerte 3.
- `ACTIVE/LIMBO/SPECTATING → INTERMISSION` al completar un nivel con sucesor.
- Cualquier estado de partida → lobby al resolverse una derrota o final provisional.

El servidor es autoridad de participantes, botones, muertes, objetivos y relojes. El cliente solo muestra estado, tiempo y efectos. Usar ticks de juego del servidor para los 30 y 90 segundos (600 y 1800 ticks a 20 TPS); no reiniciar contadores por recarga de interfaz o reconexión.

Los espectadores quedan limitados a observar compañeros y no pueden abrir contenedores, usar botones, revelar objetivos mediante interacciones, colocar objetos de misión ni activar salidas. El modo espectador vanilla por sí solo no constituye toda esta política.

## Cordura: cálculo acordado

| Fuente | Tasa |
| --- | --- |
| Penumbra | 1 punto / 10 s; no aplica en zonas iluminadas. |
| Zumbido monótono | 0.5 puntos / minuto después de 3 minutos sin otros sonidos relevantes. Aplica también con luz. |
| Entidad a menos de 15 bloques | 1 punto / 10 s adicional; no se multiplica por cantidad de entidades. |
| Energética vencida | 5 puntos en un único evento, fuera del límite ambiental. |
| Agua de Almendras | Recupera 30 puntos, máximo 100. |

Opcional propuesto: compañero activo a menos de 10 bloques reduce a la mitad solo la tasa de zumbido; configurable y activado inicialmente para probar el cooperativo. Mantener la tasa base de 0.5/minuto; no adoptar aún la alternativa de 1/minuto.

Integración de tasas: `min(penumbra + zumbido + entidad, 0.3 puntos/segundo)`. Aplicar con decimales según el tiempo de juego; no redondear el zumbido a cero ni acumularlo para descontarlo de golpe. Las tres fuentes actuales suman como máximo aproximadamente 2.08333 puntos por 10 segundos; el tope permite futuras fuentes.

No hay drenaje ambiental ni daño de cordura en lobby, limbo, descanso o espectador. Pausar durante esos estados el tiempo restante de la energética para no aplicar un descuento oculto; reanudar al volver al nivel activo. Al reiniciar la partida, limpiar los efectos y penalizaciones anteriores.

Se mantienen viñeta y susurros bajo 50; lentitud, ceguera intermitente y pasos falsos bajo 25; daño continuo a 0; posibilidad de aparición bajo 40. Los límites de aparición y descansos de encuentros siguen aplicándose.

## Protección e interacción

Bloquear rotura y colocación en las zonas del modo de juego, incluidos lobby, limbo y descanso. Las excepciones administrativas no deben permitir a jugadores normales eludir estas reglas.

Los objetos de misión se validarán en el servidor mediante una lista explícita de objetos, objetivo, nivel y ubicación permitidos. No basta con nombrar un objeto como objeto de misión. Una colocación exitosa actualiza el objetivo de forma atómica, sin duplicar ni consumir dos veces el objeto. Las misiones y sus objetos concretos siguen pendientes de diseño; esta excepción no agrega todavía una campaña de misiones a V1.

## Nueva etapa 2.1

Antes del laberinto procedural, implementar con salas provisionales:

1. Gestor de una partida, participantes de 1–4 y lobby con botón.
2. Estados de jugadores, contador de muertes y transferencia de inventario de partida.
3. Limbo blanco de prueba con salida y temporizador de 30 segundos.
4. Espectadores, derrota colectiva y reinicio al lobby.
5. Fin de nivel de prueba, reanimación y descanso de 90 segundos con botón.
6. Persistencia, reconexión, límites de interacción y sincronización de relojes.

La etapa 3 sustituirá las salas por los laberintos definitivos. La etapa 4 implementará cordura y recursos sobre estos estados. La etapa 7 conectará la salida real con la progresión y el final provisional.

## Comprobaciones necesarias

- Inicio con 1, 2 y 4 jugadores; quinto rechazado y doble clic sin duplicar partida.
- Primera y segunda muerte con retorno del limbo; tercera con espectador inmediato.
- Salida a los 29 segundos y vencimiento exacto a los 30 segundos.
- Todos en limbo simultáneamente, un jugador saliendo y todos venciendo.
- Eliminación total, nuevo inicio sin progreso ni loot residual y sin borrar inventarios externos.
- Fin de nivel mientras otro jugador está en limbo o espectando; revivir a todos una sola vez.
- Descanso de 90 segundos y botón anticipado sin dos inicios.
- Reconexión, cierre/reapertura y desconexión del anfitrión sin reiniciar vidas ni duplicar inventarios.
- Prohibición de romper/colocar, excepción válida de misión y rechazo de usos fuera de objetivo.
- Cordura individual, suma de fuentes, tope, compañía opcional y penalización puntual independiente.
- Final provisional hacia lobby distinguible de victoria de campaña con fuegos artificiales.
