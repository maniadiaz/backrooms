# Backrooms 1.1.0 — mapas limitados y más tensión

El cambio responde a la prueba de juego: linterna demasiado plana, poca niebla y presión, paneles difíciles de localizar y encuentros que no aparecían durante la exploración normal.

## Elegir dificultad

En el lobby, el anfitrión o un operador usa `/backrooms difficulty facil`. Opciones:

| Comando final | Mapa, contando el perímetro | Multiplicador de cordura | Primer intento de encuentro | Intervalo mínimo entre encuentros |
|---|---|---|---|---|
| `principiante` | 100 × 100 | 1 | 90 s | 150 s |
| `facil` | 150 × 150 | 1.5 | 60 s | 120 s |
| `normal` | 250 × 250 | 2 | 45 s | 90 s |
| `dificil` | 350 × 350 | 2.5 | 35 s | 70 s |
| `hardcore` | 500 × 500 | 3 | 25 s | 50 s |

Fácil es el valor inicial. La elección se guarda y solo cambia en el lobby, para todo el equipo y ambos niveles. El tamaño cuenta las paredes exteriores; por ejemplo, Fácil tiene 148 × 148 bloques interiores. Un perímetro sólido cierra los pasillos; el servidor también devuelve al recinto a cualquier participante activo que salga de sus límites. El generador de fondo se conserva para no invalidar mundos anteriores. Una nueva partida utiliza otra zona.

500 × 500 tiene aproximadamente once veces la superficie de 150 × 150. Está reservado para partidas largas en Hardcore; no se ha medido todavía su duración con jugadores reales. El terreno interior sigue cargándose por chunks, sin construir todo el cuadrado de golpe.

## Paneles y orientación

Los fusibles/relés se distribuyen dentro del tamaño elegido sobre pasillos conectados. Los tres paneles están juntos en una sala de control despejada, iluminada y próxima a la pared de salida. El HUD muestra `CONTROL`, dirección relativa y distancia horizontal. `/backrooms objective` da orientación y coordenadas del siguiente panel pendiente. Las instrucciones al recoger un componente mencionan ese comando. La brújula toma como referencia la salida de esa sala; mantiene su comportamiento errático fuera de 30 bloques.

## Linterna, niebla y sonidos

La linterna deja de ser un sprite plano: tiene cuerpo, empuñadura, anillos, reflector, lente e interruptor en 3D, con transformaciones para primera y tercera persona y ambas manos. Encenderla muestra un cono tenue de polvo iluminado que parte de la mano, visible también en linternas de otros jugadores. Los rayos se recortan contra bloques. La luz real usa nivel 14 y se actualiza cada dos ticks; la representación del cono es independiente de la iluminación de bloques, sin exigir shaders externos. No es un sistema físico de sombras volumétricas.

La niebla comienza aproximadamente a seis bloques y termina a 28 por defecto. La nueva clave `horrorFogDistance` sustituye el valor visual antiguo de 96 bloques sin exigir borrar configuraciones. Se puede ajustar entre 12 y 96; `fog=false` sigue desactivándola.

Se regeneraron todos los OGG ambientales y de entidades con más presencia, conservando audio mono espacial, transiciones y los controles de volumen de Minecraft. Aumentan pasos, goteos, fallos eléctricos, susurros y el zumbido. Las voces de Sabueso y Bacteria incorporan saturación y mayor cuerpo. No se ha afirmado una audición ni una valoración subjetiva de miedo por parte del agente.

## Cordura y amenazas

Por segundo se suma: ambiente constante `0.035`, penumbra `0.16`, entidad cercana `0.18`, más el triple de la monotonía anterior. Se multiplica por dificultad y se limita a `0.3 × multiplicador` por segundo, equivalente a un máximo de 3/4.5/6/7.5/9 puntos cada diez segundos. En Nivel 1 el multiplicador aumenta otro 15%. Estar acompañado sigue reduciendo solo la monotonía. La penalización de la energética es de cinco puntos una vez, fuera del tope; el agua recupera treinta. Los umbrales se conservan y las alucinaciones se repiten cada 10–25 segundos bajo 50.

Los encuentros ya no exigen explorar 30–40 salas o caer bajo 40 de cordura. Transcurrido el período inicial se buscan hasta 96 posiciones a 18–34 bloques, dentro del recinto, a 12 o más de todos los participantes y fuera de su visión. Cada posición necesita suelo, espacio y una ruta real hacia el jugador. Si Smiler no encuentra oscuridad, se prueban las otras especies. Un fallo vuelve a intentarse tras cinco segundos; no consume cinco minutos. Sigue habiendo como máximo una amenaza por grupo cercano. Las entidades empiezan investigando la última posición del jugador, sin conocer continuamente sus movimientos tras paredes. El daño y la velocidad aumentan con dificultad; Nivel 1 añade daño y reduce quince segundos el intervalo.

Las pisadas falsas continúan siendo ambiente; no se presentan como prueba de que existe una entidad. La suite incluye un encuentro mediante el mismo programador usado en producción, que comprueba una entidad real, el período de gracia, los límites y el presupuesto compartido.

## Actualización y validación

Sustituir el jar anterior por `backrooms-1.1.0.jar` en todos los clientes y en el servidor. Mantener un solo jar del mod. El protocolo cambia a 4; versiones mezcladas se rechazan. Desde el lobby elegir dificultad e iniciar una nueva partida; `/backrooms restart` mantiene las reglas de votación durante una partida existente. No se eliminan mundos ni inventarios externos.

La revisión automatizada comprende 68 GameTests: incluye la campaña de cuatro perfiles simulados, un encuentro real del programador, límites, escalado, permisos, orientación relativa y búsqueda de rutas hacia todos los objetivos para las cinco dificultades y ambos niveles. La comprobación gráfica del nuevo modelo y cono y el balance con amigos siguen requiriendo prueba en el cliente; no equivalen a los GameTests.
