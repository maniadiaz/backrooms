# Etapa 6C — Bacteria

Entrega 0.6.2 para Minecraft Java 1.20.1 / Forge 47.4.28. Incorpora `bedroom:bacteria` y completa la primera implementación de las tres especies del bestiario.

## Detección y estados

Toda la IA, daño y selección de objetivo se ejecutan en el servidor. El cliente recibe el estado de animación mediante los datos sincronizados de la entidad.

`RECORRIDO → ALERTA → CAZA → BÚSQUEDA → RECORRIDO`

- Visión directa a doce bloques, con obstáculos reales. Mirarla no la inmoviliza.
- Carrera y movimientos de ataque detectables a quince bloques, incluso tras un recodo. Intentar golpear un bloque también registra ruido durante un segundo; no permite romperlo. Ese registro conserva la posición donde ocurrió, sin revelar las coordenadas actuales del jugador al moverse después en silencio.
- La primera detección emite un grito de estática/gemido y da una alerta de doce ticks, aproximadamente 0.6 segundos, antes de perseguir. Mantener visión o ruido no reinicia el aviso cada tick.
- Durante la caza actualiza la última posición por nueva visión o ruido. Al romper ambas evidencias navega hacia la posición recordada. No persigue las coordenadas actuales a través de paredes.
- Quince segundos sin evidencia dan paso a una búsqueda local de cinco segundos alrededor del punto recordado. Prueba rutas transitables hasta seis bloques de ese punto. Nueva evidencia permite recuperarlo; sin ella abandona el objetivo y vuelve al recorrido.
- Sin objetivo patrulla puntos cargados próximos, a un máximo de ocho bloques. Ninguna ruta fallida provoca teletransporte ni carga forzada de chunks.
- Los eliminados, espectadores, jugadores de limbo/descanso/lobby y quienes se desconectaron no generan objetivos válidos. Si pierde a uno puede atraerla otro participante activo.

Para esconderse hay que bloquear realmente la visión y permanecer silencioso. Correr a menos de quince bloques sigue siendo una pista aunque haya una pared. Los muebles de escondite interactivos todavía no existen.

## Movimiento, daño y ciclo de vida

Colisión de 0.75 × 1.45 bloques y navegación terrestre, comprobada en un pasillo en L de dos bloques de ancho y tres de alto. El modelo muestra torso bajo, huesos expuestos, extremidades desproporcionadas, cabeza sin rostro y cables articulados. Anima el arrastre, el movimiento de cables y el temblor de alerta.

Esta entrega se desplaza por el suelo. No incorpora navegación vertical, trepar paredes ni atravesarlas; esa capacidad real necesitaría una implementación y validación aparte.

Balance inicial: 36 puntos de vida, ocho puntos de daño base (cuatro corazones), treinta ticks entre golpes y velocidad de movimiento 0.28 con multiplicador 1.55 durante la caza. La dificultad, defensas y efectos de Minecraft modifican el daño. No cambia la cordura a cero por golpe: utiliza el drenaje de amenaza cercana ya existente, con su tope individual.

Los ataques usan el sistema normal de daño y conservan las dos oportunidades de limbo. Al morir o salir el objetivo pierde su seguimiento. No hay barra de jefe ni loot propio. La entidad dura como máximo tres minutos de ticks cargados; reinicio, lobby, descanso o cambio de nivel la retiran. Al cargar se comprueban ronda, nivel y tiempo guardado para descartar encuentros antiguos.

## Aparición y audio

El presupuesto común elige ahora una de tres especies con igual probabilidad por oportunidad. Mantiene frecuencia total, celdas exploradas, cordura, distancia mínima de 24 bloques, ubicación fuera de vista, ruta válida y enfriamiento de cinco minutos. Smiler conserva su requisito adicional de oscuridad. No se genera una criatura de cada especie a la vez: la presencia de cualquier `Monster` próximo bloquea otro candidato.

Cuatro sonidos originales mono a 32 kHz:

- Arrastre húmedo: un sonido corto como máximo cada dieciséis ticks mientras se mueve.
- Roce/tintineo de cables: un evento cada cinco segundos.
- Grito de alerta: una vez al adquirir o recuperar el objetivo desde recorrido/búsqueda.
- Impacto de ataque: al acertar un golpe, con el enfriamiento del combate.

El motor de Minecraft sitúa cada sonido en el mundo y realiza el paneo según la cámara, bajo el control de volumen de criaturas hostiles. Los sonidos cortos se cancelan al abandonar la escena. No se promete oclusión acústica por paredes. No se crean bucles nuevos cada tick.

`python scripts/generate_bacteria.py` reconstruye textura y sonidos propios con las dependencias de `scripts/audio-requirements.txt`. No usa grabaciones ajenas, voces reales ni recursos externos. El generador preserva los eventos de las otras especies; jugar solo requiere el jar.

## Validación y prueba manual

Compilación y 50 pruebas obligatorias aprobadas con `scripts/gradle.ps1 build runGameTestServer --console=plain`. Las nuevas pruebas comprueban visión/ruido, memoria del lugar de una interacción, alerta sin reiniciarse, búsqueda y recuperación, exclusión de eliminados, cambio de objetivo, daño/enfriamiento/limbo, ruta estrecha, presupuesto común de las tres especies, guardado/reinicio y recursos PNG/OGG mono empaquetados.

El cliente arrancó con recarga de recursos, registro y creación del modelo/renderizador y motor de sonido, sin errores de Bedroom en el registro. Se cerró ese cliente después de comprobar el arranque. Esta verificación no incluye ver la criatura moverse dentro de una partida ni escuchar sus sonidos jugando.

Con una partida activa y permisos de operador/trucos, usar `/summon bedroom:bacteria ~ ~ ~5` en un punto libre del Nivel 0. Ese comando administrativo omite deliberadamente las restricciones naturales de aparición. Fuera de una partida activa se descarta. `/bedroom restart` mantiene las votaciones del grupo.

Pendiente: revisión visual/auditiva jugando, calibrar la velocidad y las posibilidades de ocultarse en el laberinto real, y sesión de dos a cuatro clientes con Essential. Las pruebas automatizadas no verifican esa experiencia cooperativa ni sustituyen la revisión de las animaciones.

Instalar el mismo `bedroom-0.6.2.jar` en clientes y servidor. Esta entrega conserva el generador de los mundos 0.3–0.6.1. La siguiente etapa prevista es la 7: misiones y salida por No-Clip; Nivel 1 y final de celebración siguen pendientes.
