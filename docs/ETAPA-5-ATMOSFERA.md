# Etapa 5 — Atmósfera visual y sonora

Versión `0.5.0`, Minecraft Java 1.20.1, Forge 47.4.28.

## Incluido

- Zumbido fluorescente continuo, con fundamental de 60 Hz, armónicos y chisporroteos sutiles. Su volumen inicial es 35% de la categoría ambiente, con entrada gradual.
- Una única instancia de zumbido por cliente. Se retira al salir de la escena del nivel, desconectarse o cambiar de mundo, y se reconstruye tras recargar el motor de audio. No se inicia un bucle por lámpara ni por tick.
- Pisadas húmedas originales con cuatro variantes, asignadas a ambas moquetas mediante `ForgeSoundType`; no sustituyen los sonidos de otros bloques del juego.
- Pasos lejanos personales cada 45–90 segundos de nivel activo, emitidos desde una posición transitable a 10–18 bloques. No generan una entidad ni una fuente de ruido de jugador para la futura IA.
- Goteos cada 20–45 segundos cuando hay un punto válido bajo un panel de techo cercano, con sonido posicional y partículas de agua. No colocan agua ni alteran el suelo.
- Intentos de fallo eléctrico cada 20–40 segundos: selección de una lámpara cercana, chasquido y pérdida temporal de luz real. No se acumulan fallos simultáneos en el mismo entorno cercano.
- Recuperación de lámparas mediante ticks programados. Las fundidas permanentes siguen fundidas. Los fallos propios se limpian al abandonar el nivel o reiniciar; si un chunk se descarga, se recuperan al volver a procesar sus ticks.
- El ajuste inicial usa un apagón de dos a cuatro segundos. `rapidFlicker=true` agrega fases breves de apagado/encendido antes y después de ese apagón.
- Susurros no verbales y pasos falsos originales para cordura baja, reemplazando los sonidos vanilla provisionales. Conservan los umbrales y la ceguera momentánea existentes.
- Niebla de polvo amarillento a larga distancia: empieza aproximadamente a 32 bloques y termina a 96, limitada por la distancia de renderizado. Solo se aplica dentro del nivel; no sustituye la niebla de agua, lava, ceguera u oscuridad.
- Distorsión opcional por cordura baja mediante una rotación mínima de cámara; desactivada inicialmente. Solo cambia la presentación, sin modificar la mirada del jugador en el servidor.
- La música normal de Minecraft se detiene y se suprime mientras se está en la escena del laberinto; el resto del mundo conserva su comportamiento habitual.

El ambiente también acompaña a un espectador cuando su cámara está siguiendo a un participante activo dentro del nivel. No se aplica a su sala de espera ni provoca drenaje de cordura en el espectador. Los eventos periódicos solo se generan para jugadores activos, sin duplicar eventos por observadores.

## Audio y cordura

Los sonidos cercanos relevantes reinician el temporizador de monotonía del servidor. Las pisadas propias no lo reinician; las de un compañero cercano sí. Los goteos, fallos y pasos ambientales cercanos pueden interrumpir la monotonía. Las alucinaciones personales no reinician ese contador.

Esta regla hace que el drenaje por zumbido sea poco frecuente en zonas con eventos regulares: solo aplica cuando transcurren realmente tres minutos sin otros sucesos relevantes. Se mantiene la tasa acordada de 0.5 puntos por minuto; no se incrementa para compensar esa menor frecuencia.

Silenciar el juego o bajar el zumbido no altera los relojes ni las reglas de cordura. Los sonidos de espectadores no afectan la monotonía de los participantes. Se mantienen el tope ambiental y las demás reglas de etapa 4.

## Recursos originales

Diez archivos OGG Vorbis mono de 32 kHz, registrados en siete eventos. Incluyen un bucle de ocho segundos, cuatro variantes de paso y las frases/eventos de ambiente y cordura. Se sintetizan con osciladores, ruido filtrado y envolventes; no contienen grabaciones, muestras de otros juegos ni voces de personas.

El audio mono permite el paneo por posición del motor de Minecraft. El zumbido es relativo al oyente y no posicional. Los pasos/goteos/fallos se colocan en el mundo; no se graba izquierda o derecha dentro del archivo. No se implementa oclusión acústica real por paredes.

Todos los recursos están incluidos en el jar. Para regenerarlos:

```powershell
python -m pip install -r scripts/audio-requirements.txt
python scripts/generate_audio.py
```

En este equipo las dependencias de generación están en `.tools/audio-python`, fuera de Git. No son dependencias de ejecución de Minecraft. El script valida decodificación, duración, un canal, frecuencia y muestras finitas; escribe un informe de picos/RMS en `.tools/audio-report.json`.

`scripts/generate_textures.py` conserva los subtítulos existentes y las variantes de fallo de fluorescentes al reconstruir las texturas. No se requiere regenerar los audios cada vez que se compila.

## Configuración

En `config/bedroom-client.toml`, por cliente:

| Opción | Inicial | Efecto |
| --- | --- | --- |
| `humVolume` | 0.35 | Ganancia del zumbido; también respeta los controles de ambiente y volumen general de Minecraft. |
| `fog` | true | Niebla tenue del laberinto. |
| `fogDistance` | 96 | Distancia final de niebla, entre 48 y 192 bloques; no amplía la distancia de renderizado. |
| `cameraRoll` | 0 | Balanceo visual opcional por cordura baja, hasta 0.5 grados. |

En `config/bedroom-common.toml`, del servidor/anfitrión:

| Opción | Inicial | Efecto |
| --- | --- | --- |
| `lampFailures` | true | Permite los fallos temporales de lámparas. |
| `rapidFlicker` | false | Añade la secuencia breve de parpadeos; apagado mantiene un único apagón sostenido. |

Las lámparas son bloques compartidos y su estado lumínico es autoridad del servidor. El parpadeo real no tiene un ajuste distinto por cliente.

## Pruebas ejecutadas

Compilación/reobfuscación correctas (`BUILD SUCCESSFUL`), treinta GameTests requeridos aprobados, ninguno fallido. Se conservan las veinticuatro pruebas anteriores y se añaden seis:

1. Relojes reproducibles y límites de 45–90 segundos para pasos lejanos durante una hora simulada.
2. Fases de apagado/encendido, emisión real, recuperación y distinción entre fallos temporales y lámparas fundidas.
3. Relojes ambientales que comienzan en nivel activo y se detienen en lobby/limbo.
4. Limpieza de fallos al salir y rechazo de fallos superpuestos en la misma zona.
5. Registro de pisadas y empaquetado del manifiesto con todos los OGG requeridos.
6. Monotonía que distingue pasos propios, compañeros cercanos, sonidos fuera de alcance y jugadores en limbo.

La generación de audio decodificó los diez OGG y verificó que fueran mono de 32 kHz. El cliente de desarrollo inició el motor de audio y cargó modelos y manifiesto sin errores específicos de los recursos del mod. Hubo avisos del entorno de desarrollo por registros abiertos por otro cliente y avisos vanilla; no se presentan como una comprobación auditiva.

Quedan pendientes una escucha en juego con auriculares, revisión visual de niebla/parpadeos a distintas configuraciones y prueba con dos a cuatro clientes reales mediante Essential. También falta medir el coste conjunto de iluminación dinámica y fallos de lámparas con cuatro jugadores. No se declara verificación multicliente ni compatibilidad con todos los shaders/mods gráficos.

## Compatibilidad y siguiente entrega

No cambia la distribución del laberinto ni los identificadores de los bloques. Los fluorescentes existentes usan su fase normal por defecto; no hace falta generar un mundo nuevo para obtener los sonidos y efectos. Todos los clientes deben actualizar al mismo jar.

El bestiario sigue siendo un diseño: no hay monstruos todavía. La siguiente entrega es etapa 6A, Hound, empezando por navegación por el laberinto, ruido, mirada, duda, carga y ataques que respeten el limbo y los espectadores.
