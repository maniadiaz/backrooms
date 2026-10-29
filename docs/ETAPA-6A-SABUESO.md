# Etapa 6A — Sabueso

Entrega 0.6.0 para Minecraft Java 1.20.1 / Forge 47.4.28.

## Comportamiento

El servidor controla `bedroom:hound`, un cuadrúpedo original con costillas, patas largas, ojos hundidos y mandíbula abierta. Su colisión mide 0.7 × 1.35 bloques. El modelo usa una textura de 64 × 64 y animación de patas/cabeza; es una primera versión para ajustar su aspecto jugando.

- Detecta participantes activos por visión, carrera o movimientos de ataque/interacción a 15 bloques. Cordura inferior a 30 permite seleccionar un participante próximo para acechar, sin conocer su posición tras paredes.
- La mirada directa necesita línea de visión, un cono estrecho y ausencia de ceguera. Si el objetivo lo observa sin caminar, se detiene y gruñe. Girar la cámara no cuenta como caminar. La mirada no vence a los tres segundos.
- Acumula un desplazamiento horizontal de más de 0.05 bloques para distinguir movimiento de pequeñas vibraciones. Caminar, correr o darle la espalda con contacto visual inicia la carga. Un compañero que corre cerca puede atraerlo aunque otro lo observe.
- Navegación terrestre con colisión y búsqueda por última posición conocida. No actualiza esa posición a través de paredes salvo ruido próximo. Tras 15 segundos sin evidencia deja de perseguir.
- Siete puntos de daño base (3.5 corazones), con 30 ticks entre mordidas. Usa el daño normal de Minecraft y conserva las dos oportunidades de limbo.
- Los jugadores en lobby, limbo, descanso, espera o espectador no pueden ser objetivos. Reiniciar/cambiar de nivel elimina la entidad en su siguiente tick cargado. Las entidades guardadas conservan la identificación de la ronda para descartarse si ya expiró. Cada encuentro dura como máximo tres minutos activos de la entidad.

## Aparición y presupuesto común

Se cuentan las celdas de 16 × 16 distintas visitadas por cada participante: la primera oportunidad llega entre 30 y 40, o por cordura inferior a 40. Hay un margen inicial de un minuto. Se comprueba una vez por segundo, con hasta 24 candidatos en chunks cargados. Esto representa habitaciones lógicas del generador, no una detección arquitectónica de cada sala abierta.

El candidato queda a 28–44 bloques del jugador de referencia, al menos a 24 de todos los participantes activos y sin línea de visión desde ellos. Debe estar sobre suelo sólido, libre de colisiones y con ruta terrestre válida. No se generan chunks ni se atraviesan paredes para forzar una aparición.

El presupuesto es compartido entre especies: cualquier `Monster` a menos de 96 bloques impide otro encuentro. Una aparición reserva cinco minutos a participantes dentro de 128 bloques del jugador de referencia. Los intentos sin ubicación válida también esperan cinco minutos. El enfriamiento se conserva durante el limbo y dentro de la misma ronda; lobby/reinicio lo limpia. No hay apariciones en las salas seguras.

## Audio y recursos

Seis eventos originales: pasos con garras, jadeo, lamento no verbal, gruñido, carga y aullido de ataque. Son OGG mono a 32 kHz; el motor de Minecraft proporciona posición y atenuación, bajo el volumen de criaturas hostiles. Los avisos de estado tienen un enfriamiento de dos segundos para evitar spam al alternar la mirada. Al salir de la escena se cancelan sus sonidos cortos.

Los recursos se reconstruyen con `python scripts/generate_hound.py`, usando las dependencias de `scripts/audio-requirements.txt`. No hay grabaciones ajenas, voces reales ni dependencia de Python al jugar. El generador ambiental preserva estos nuevos eventos.

## Verificación y prueba manual

Ejecutar `scripts/gradle.ps1 build runGameTestServer --console=plain`. Resultado: compilación correcta, 37 pruebas obligatorias aprobadas y arranque del cliente con recarga de recursos/modelos y motor de sonido, sin errores de Bedroom en el registro. El cliente de prueba se cerró tras esa comprobación. La suite incluye mirada/obstáculos/ceguera, reacción sostenida, IA real con mirada y carrera, exclusión de eliminados, reinicio, navegación por una L de dos bloques y mordida letal con entrada al limbo y candidatos de aparición con ruta/distancia/presupuesto compartido, además de las pruebas anteriores.

Para verlo sin esperar una aparición, iniciar una partida normal y, con permisos de operador/trucos, usar `/summon bedroom:hound ~ ~ ~5` en un punto libre del Nivel 0. Este comando de Minecraft es una prueba administrativa: omite deliberadamente el presupuesto de aparición. Fuera de una partida activa el Sabueso se descarta. Para repetir el intento, `/bedroom restart` conserva el sistema de votaciones.

Pendiente: revisión visual y auditiva dentro de una partida, equilibrio de velocidad/daño en el laberinto real y prueba de dos a cuatro clientes con Essential. Las pruebas automatizadas y la carga del cliente no verifican esas experiencias. Smiler y Bacteria siguen pendientes de etapas 6B y 6C.

Instalar el mismo `bedroom-0.6.0.jar` en todos los clientes y el servidor. Los mundos de 0.3–0.5 conservan su generador; hacer copia de seguridad antes de actualizar un mundo compartido.
