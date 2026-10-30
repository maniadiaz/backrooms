# Bedroom — bestiario y contratos de implementación

> Rediseño visual 1.2.0: Sabueso encorvado, Bacteria humanoide bípeda de tres bloques y Smiler con cuerpo esférico negro. Las descripciones anteriores se conservan como diseño histórico; consultar ENEMIGOS-1.2.0.md para el aspecto actual.

Estado vigente: 1.0.0 con las tres entidades en ambos niveles y trepa limitada de Bacteria con colisión real. Véanse las entregas históricas ETAPA-6A-SABUESO.md, ETAPA-6B-SMILER.md, ETAPA-6C-BACTERIA.md y el informe vigente ENTREGA-FINAL.md. La revisión visual, auditiva y cooperativa dentro de una partida sigue pendiente. Los valores indicados como propuestas deben calibrarse jugando; no son requisitos ya confirmados.

## Reglas comunes

Son amenazas de evasión, sin obligación de combatir ni derrotarlas para avanzar. No se presupone una barra de jefe ni loot al matarlas. Su presencia debe seguir siendo escasa para conservar el aislamiento del Nivel 0.

- IA, selección de objetivo, daño, aparición y cambios de estado son responsabilidad del servidor. Modelos, animaciones y efectos visuales corresponden al cliente.
- Solo persiguen participantes activos del nivel. No atacan a jugadores en lobby, descanso, limbo, cola o espectador, y nunca aparecen en esas salas.
- Morir por una entidad utiliza el sistema de muertes/limbo existente. Ningún ataque teletransporta directamente al lobby ni evita las dos oportunidades de limbo.
- Un participante que se desconecta pierde su intento; la entidad pierde ese objetivo. Quien regresa y observa no provoca detección, mirada, ruido ni cordura de sus compañeros.
- El espectador que usa la cámara de otro jugador no cuenta como un segundo observador de la entidad. La cámara tampoco controla la mirada del jugador observado.
- Propuesta inicial: un encuentro peligroso simultáneo por grupo cercano, compartido entre las tres especies, con cinco minutos de descanso entre encuentros. No aplicar un límite independiente que permita tres cazadores juntos.
- Mantener la regla de aparición rara: puntos potenciales aproximadamente cada 30–40 habitaciones; cordura inferior a 40 habilita oportunidades adicionales, siempre sujetas al límite de encuentros. No invocar automáticamente al bajar del umbral en cada tick.
- Propuesta: aparecer fuera de la vista y al menos a 24 bloques de participantes activos, con espacio y ruta válidos. La zona de aparición del Smiler debe ser oscura; su audio comienza a ser perceptible al aproximarse a 20 bloques.
- La amenaza a menos de 15 bloques activa el drenaje de cordura de proximidad existente. No se suma una penalización por cada monstruo; se mantiene el tope ambiental de tres puntos cada diez segundos.
- Los modelos y colisiones deben caber en tres bloques de altura libre y pasillos de dos bloques. Brazos/cables decorativos no deben obligar a atravesar paredes.

## Smiler — la cara sonriente

### Referencia del usuario

Rostro flotante con sonrisa amplia, estática y luminosa; ojos blancos sin pupilas. El resto de su silueta desaparece en la oscuridad. Su aspecto debe transmitir una cara humana perturbadora, con recursos originales, adaptada al estilo y a las posibilidades de Minecraft.

Prefiere pasillos oscuros y luminarias fundidas. Avanza lentamente cuando no lo observan. Al mirarlo directamente se detiene o retrocede un poco; al apartar la mirada, perder la visión o retroceder hacia la oscuridad, se acerca agresivamente. No produce pasos.

### Estados y detección

`ACECHO → OBSERVADO → AVANCE → ATAQUE`

- Mirada válida: participante activo, línea de visión sin obstáculos, dirección de los ojos dentro de un cono estrecho y sin ceguera.
- Mirarlo detiene el avance; el retroceso leve es una variante de animación/movimiento, sin atravesar la geometría.
- Perder la mirada durante un breve margen configurable permite avanzar. Propuesta: 0.35 segundos para evitar oscilaciones por un cambio mínimo de cámara o latencia.
- Minecraft no tiene parpadeo voluntario por defecto. En la primera implementación, apartar la mirada y la ceguera breve de cordura representan esa pérdida de visión. No se añade un sistema de cerrar los ojos sin diseñarlo por separado.
- Retroceder a oscuras se detecta mediante movimiento opuesto a la mirada y luz de bloque baja, no mediante un supuesto estado de miedo del cliente.
- No atraviesa paredes. Flotación visual y navegación respetan pasillos y paneles; no usar navegación de fantasma que ignore colisiones.
- En cooperativo, propuesta: una mirada válida de cualquier participante activo lo detiene. No puede atacar atravesando a quien está observándolo; se calibrará para evitar que un compañero lo inmovilice sin riesgo indefinidamente.

El usuario permite dos alternativas de ataque: muerte inmediata o daño masivo y cordura a cero. **Propuesta inicial de balance:** ocho corazones de daño base y cordura a cero, con comprobación de contacto y enfriamiento. La variante letal quedaría configurable; no se presenta como elección confirmada. En ambos casos se respeta la lógica de limbo.

### Audio espacial

- Movimiento: silencioso, sin evento de pisada heredado.
- Entre 15 y 20 bloques: silbido/siseo tenue.
- Al acercarse: estática aguda progresiva.
- A menos de cinco bloques: siseo intenso y risa breve, ahogada y procesada.
- Propuesta de eventos: `smiler_hiss`, `smiler_laugh`, `smiler_attack`.

## Bacteria — carne, cables y alambres

### Referencia del usuario

Criatura esbelta, contorsionada y sin rostro definido, formada por carne deteriorada, tendones, huesos, cableado y tubos oxidados. Se mueve agachada o arrastrándose; la animación debe mostrar extremidades largas y contacto incómodo con suelo y paredes.

Es una cazadora activa. Detectar ruido o visión cercana provoca un grito seco y una persecución rápida. Se pierde al romper la detección mediante varios recodos o escondites válidos.

### Estados y detección

`RECORRIDO → ALERTA → CAZA → BÚSQUEDA → RECORRIDO`

- Propuesta: ruido de carrera o interacción fuerte hasta 15 bloques; visión directa hasta 12 bloques.
- La alerta emite un único grito al cambiar de estado, sin repetirlo cada tick durante la caza.
- Navega a través del laberinto y memoriza la última posición detectada. No conoce automáticamente la posición actual del jugador tras una pared.
- Perder visión no termina instantáneamente la persecución: busca en la última posición y recodos cercanos. Propuesta: abandonar la caza tras 15 segundos sin nueva evidencia y finalizar la búsqueda después de un intervalo adicional configurable.
- Un escondite cuenta si bloquea realmente la detección y el jugador permanece silencioso. Todavía no existen muebles de escondite interactivos; no prometer esa interacción en la primera entrega.
- Arrastrarse por paredes puede empezar como animación junto a ellas. Desplazamiento real vertical requiere navegación y colisión específicas, además de pruebas en recodos; se implementará como capacidad separada, sin fingirla mediante teletransporte.
- Velocidad, daño y resistencia son parámetros pendientes de balance. Debe existir una posibilidad real de escapar mediante distancia y rutas; no se fija una persecución inevitable que dure para siempre.

### Audio espacial

- Movimiento: arrastre húmedo, crujido y roce metálico de cables.
- Lejos: tintineo metálico y zumbido tenue.
- Alerta: estallido corto de estática y gemido gutural.
- Cerca: arrastre pesado, claramente localizable delante, detrás o al otro lado de un recodo.
- Propuesta de eventos: `bacteria_drag`, `bacteria_metal`, `bacteria_alert`, `bacteria_attack`.

## Hound — el sabueso

### Referencia del usuario

Cuadrúpedo canino demacrado, piel gris o pálida, costillas marcadas y patas largas. Cabeza vagamente humana, ojos vacíos y mandíbula grande con dientes humanos deteriorados. Puede levantarse brevemente en una animación, siempre respetando el techo.

Se detiene y gruñe cuando se le sostiene la mirada. Duda mientras el jugador lo mira sin desplazarse; caminar o correr provoca una carga rápida. La rotación de cámara necesaria para mantener la mirada no cuenta como caminar.

### Estados y detección

`INACTIVO → ACECHO → DUDA → CARGA → BÚSQUEDA`

- Mantener los criterios propuestos de ruido a 15 bloques y contacto visual directo.
- La mirada necesita línea de visión y objetivo activo; no funciona a través de paredes ni desde cámaras de espectador.
- **Actualización de diseño:** se elimina la regla anterior que provocaba caza tras tres segundos de mirada sostenida. La descripción más reciente mantiene su duda mientras se lo observa.
- El desplazamiento del jugador durante el encuentro, la carrera o darle la espalda pueden disparar la carga. Propuesta: un umbral pequeño de desplazamiento horizontal acumulado evita activar por vibraciones de posición o latencia.
- En cooperativo, se evalúa el comportamiento de cada participante detectado: que uno lo mire no garantiza protección si otro corre cerca y se vuelve un objetivo válido. No cambiar de objetivo a un espectador.
- Mantener como punto de partida seis a ocho puntos de daño base por golpe y al menos 1.5 segundos entre ataques. La velocidad de carga se ajustará en pasillos reales.
- Perderlo mediante varios recodos conduce a búsqueda, con memoria de la última posición; no aparece delante del jugador para compensar una ruta fallida.

### Audio espacial

- Movimiento: garras y pisadas rápidas sobre moqueta, jadeos húmedos y saliva.
- Acecho: gemidos que recuerdan a un lamento humano, sin diálogos ni voces de personas reales como requisito.
- Lejos: respiración tenue y rasguños ocasionales.
- Cerca: jadeos y lamentos claros. El paso a gruñido furioso anuncia la carga; el ataque añade un chillido o aullido seco.
- Propuesta de eventos: `hound_steps`, `hound_breath`, `hound_lure`, `hound_growl`, `hound_charge`, `hound_attack`.

## Contrato de audio para las tres entidades

Los archivos posicionales serán OGG mono para que Minecraft sitúe la fuente en el mundo. No grabar izquierda/derecha dentro del archivo: la orientación de la cabeza y la posición de la entidad deben determinar el paneo del motor de sonido.

Separar movimiento, proximidad y cambio de estado. No iniciar un nuevo bucle cada tick; detener o sustituir el anterior al desaparecer, cambiar de dimensión o terminar el encuentro. Los sonidos de amenaza se originan en la entidad, y las alucinaciones de cordura siguen siendo eventos personales distintos.

La atenuación debe conservar las pistas lejanas y permitir localizar a la criatura al acercarse. El incremento de tensión no debe depender únicamente de subir el volumen sin límite; combinar capas de sonido y timbre, con controles de volumen de criaturas hostiles. No afirmar oclusión acústica real por paredes: el motor vanilla no garantiza esa simulación; cualquier filtro propio requerirá implementación y pruebas.

## Orden de trabajo y criterios de aceptación

1. Etapa 5: preparar el ambiente y la infraestructura de eventos/bucles de audio, con recursos originales o de licencia compatible.
2. Etapa 6A: Hound, para validar navegación, ruido, mirada, carga y conexión con limbo/cordura.
3. Etapa 6B: Smiler, con rostro emisivo, detección de observadores y desplazamiento bloqueado por paredes.
4. Etapa 6C: Bacteria, animaciones de arrastre, búsqueda por última posición y persecución; navegación vertical real, si se añade, se valida aparte.

Cada entrega debe compilar y cargar en cliente y servidor dedicado. Verificar aparición fuera de salas seguras, rutas por paneles y recodos, transiciones sin spam de audio, mirada con/sin obstáculo y ceguera, contacto/daño con limbo, desconexión del objetivo, límites de encuentros y limpieza al reiniciar. Probar además con dos a cuatro clientes: varias miradas, cambio de objetivo, espectadores sin influencia y localización del sonido al girar.

El catálogo amplía el alcance previo de una sola especie a tres. La etapa 5 prepara el ambiente; las etapas 6A, 6B y 6C incorporan sucesivamente al Sabueso, Smiler y Bacteria. Las tres comparten ahora un único presupuesto de aparición. Su equilibrio final requiere una sesión de juego real antes de considerar cerrada la revisión cooperativa.
