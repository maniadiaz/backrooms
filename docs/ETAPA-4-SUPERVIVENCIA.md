# Etapa 4 — Supervivencia y nuevas reglas de reconexión

Versión `0.4.0`, Minecraft 1.20.1, Forge 47.4.28. Los documentos de entregas anteriores describen su comportamiento histórico; las reglas de esta entrega sustituyen la recuperación de partidas al reconectar.

## Reconectar, votar y observar

Desconectarse descarta el inventario, cordura, muertes y progreso personal de esa partida. El inventario externo al modo de juego se protege por separado. Al regresar se llega al lobby; si los compañeros siguen jugando, no se recupera un puesto activo ni se teletransporta al nivel.

El regreso de un participante propone votar. Durante una partida cualquier participante conectado también puede proponer con `/bedroom restart`. Solo votan los integrantes conectados de la partida, incluidos sus eliminados; quienes esperan en el lobby no votan. Las opciones son `/bedroom vote restart` y `/bedroom vote continue`, con botones clicables en el chat.

Hace falta más de la mitad de los votos posibles para reiniciar. La votación dura 30 segundos de juego; empate, imposibilidad de lograr mayoría o tiempo agotado significan continuar. Si alguien se desconecta, deja de formar parte del electorado. Solo hay una votación simultánea. Reiniciar reúne al grupo y admite la cola hasta cuatro puestos, sin iniciar otra partida automáticamente. La consola conserva su capacidad administrativa de reinicio; en lobby la acción directa queda para anfitrión, dueño u operador.

Quien espera puede usar `/bedroom spectate`, `/bedroom spectate next`, `/bedroom spectate previous` y `/bedroom spectate off`. Las teclas `]` y `[` cambian de cámara y se pueden reasignar. Los eliminados también pueden cambiar entre compañeros activos; no recuperan un puesto jugando. Al morir/desconectarse el objetivo, se elige otro disponible. No hay interacción ni inventario de partida mientras se observa.

Cerrar y reabrir un mundo no actúa como punto de guardado de la partida: el servidor vuelve al lobby. Los datos externos de jugadores desconectados se devuelven al regresar. Esta política también cubre migración de partidas guardadas por versiones anteriores.

## Cordura

Valor individual de 0 a 100 con decimales. Se actualiza únicamente durante el nivel activo:

- Penumbra: un punto cada diez segundos, con luz de bloque a la altura de los ojos de 7 o menos.
- Monotonía: 0.5 puntos por minuto tras tres minutos sin interacciones/sonidos relevantes. Es un temporizador lógico; el zumbido ambiental definitivo corresponde a etapa 5. Consumir y abrir suministros reinicia el contador.
- Amenaza: un punto cada diez segundos si hay un monstruo a menos de 15 bloques. No se multiplica por cantidad. Se integrará el Sabueso en etapa 6.
- Las tasas se suman y se limitan a 0.3 puntos por segundo (tres por diez segundos). No se descuentan lotes acumulados de golpe.
- Compañero activo a menos de diez bloques: reduce solo monotonía a la mitad si `companyHelps=true`, valor inicial de configuración.
- Menos de 50: bordes oscuros y audio personal provisional. Menos de 25: Lentitud I, ceguera de un segundo y sonidos falsos cada 20–40 segundos. Los sonidos usan recursos vanilla provisionales hasta etapa 5.
- Cero: un punto de daño de pánico cada dos segundos hasta recuperarse o morir. El Agua de Almendras detiene el daño.

No se drena cordura en lobby, limbo, descanso o espectador. Al salir del limbo se conserva un mínimo de 30; el siguiente nivel restaura 100. La penalización de energética es un evento separado del tope y su reloj se pausa fuera del nivel activo.

## Suministros

| Objeto | Uso |
| --- | --- |
| Agua de Almendras | Beber: +30 cordura y seis puntos de hambre; retira efectos propios de cordura baja y hambre, náusea/debilidad. |
| Barra nutritiva | Ocho puntos de hambre (cuatro muslos) y Regeneración I durante cinco segundos. |
| Energética Juice | Velocidad II y Prisa I durante 30 segundos activos; al vencer pierde cinco de cordura una sola vez. Otra bebida renueva ese único plazo. |
| Vendas | Uso de dos segundos, recupera seis puntos de vida; no se inicia a salud completa. |
| Pilas AAA | Recarga 25% de una linterna, primero sostenida y luego inventario. No consume si no hay una descargada. |
| Linterna | Clic derecho enciende/apaga. Carga completa: 20 minutos de uso activo en mano. Guardada, apagada o fuera del nivel no consume. |
| Brújula anómala | Aguja hacia la salida regional más cercana a menos de 30 bloques; gira fuera de alcance. |

Al iniciar una partida nueva se entregan tres barras, un Agua de Almendras y una linterna completa. Regresar del limbo no entrega otro kit. Reiniciar elimina los recursos de la partida anterior.

La linterna aproxima un haz direccional mediante tres rayos y puntos de luz temporales hasta ocho bloques. Comprueba colisiones y no coloca luz detrás de paredes ni sustituye bloques sólidos. No es un foco renderizado mediante shaders: la propagación alrededor de cada punto sigue las reglas de Minecraft. Se actualiza cada medio segundo, se retira al apagar/guardar/salir y los puntos huérfanos tienen limpieza programada al cargarse. Queda pendiente medir el coste con cuatro jugadores.

Los iconos, modelos y texturas son originales y reproducibles con el script del proyecto. La brújula usa dieciséis orientaciones de aguja. Las animaciones de beber/vendar son provisionales.

## Cajas y archivadores

Caja de cartón y archivador metálico con inventario compartido de 27 espacios y apariencia abierta tras usarlo. El contenido se genera una sola vez, no por jugador. Se guarda el inventario resultante y retirar todo no repone objetos al reabrir o cargar.

La generación reserva un contenedor por grupo de doce habitaciones en cada franja; sus posiciones varían de manera que los consecutivos quedan separados por diez a catorce celdas a lo largo de la franja. La celda del contenedor se abre para garantizar acceso.

Tres tiradas independientes, una unidad por tirada:

| Tirada | Probabilidad | Pesos |
| --- | --- | --- |
| Básica | 100% | Agua 40, barra 45 |
| Utilidad | 50% | Baterías 35, energética 25, linterna 15 |
| Especial | 15% | Vendas 20, brújula 5 |

Resultado: uno a tres objetos y media teórica de 1.65. La tabla está en `data/bedroom/loot_tables/chests/supplies.json`. Los contenedores solo pueden abrirse por participantes activos.

En mundos de versiones anteriores, las cajas solo aparecen en chunks generados por esta versión. Se puede empezar una partida nueva en otro sector o crear un mundo nuevo para una prueba limpia. No se convierten chunks ya existentes.

## Verificación y pendientes

Resultado de esta entrega: compilación y reobfuscación correctas (`BUILD SUCCESSFUL`), veinticuatro pruebas GameTest requeridas aprobadas y ninguna fallida. La prueba de pánico usa un jugador de servidor con la protección inicial de aparición ya vencida, y verifica el daño real y su cese tras consumir Agua de Almendras.

Las pruebas cubren los sistemas anteriores y añaden: tasas y tope de cordura; compañía; penalización única de energética; efectos bajos y recuperación de pánico; mayoría, empate y vencimiento de votación; selección circular de cámaras; desconexión sin recuperar progreso; consumibles y batería; 4000 tiradas de loot y persistencia de una caja vacía; frecuencia de colocación y entidades de inventario generadas; proyección de luz, bloqueo por pared y retirada.

La prueba de cámaras verifica la selección de objetivos; no sustituye una sesión con clientes reales. Quedan pendientes la revisión visual del HUD, modelos y aguja, el cambio de cámaras con Essential, sesiones con cuatro jugadores y rendimiento de iluminación. No se declara compatibilidad verificada con Essential.

El cliente de desarrollo inició y cargó los atlas de modelos/texturas sin errores específicos de los nuevos recursos. No se realizó inspección visual en juego en esta entrega.

No incluye todavía el Sabueso, audio ambiental original, parpadeo dinámico, niebla, misiones, Nivel 1, geometría imposible ni el no-clip de salida. La siguiente etapa es la atmósfera visual y sonora.
