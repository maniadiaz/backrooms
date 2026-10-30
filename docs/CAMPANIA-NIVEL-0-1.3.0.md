# Las Habitaciones Amarillas — campaña 1.3.0

> Actualización 1.4.0: las cinco misiones siguen vigentes, pero las reglas de vidas, recuperación y guardado descritas al final de este documento han sido sustituidas por REGLAS-Y-CHECKPOINTS-1.4.0.md.

El usuario eligió **sustituir** los tres fusibles del Nivel 0 por las cinco misiones. Los relés del Nivel 1 continúan. Los identificadores antiguos de fusibles se conservan únicamente para cargar mundos e inventarios anteriores; no se generan ni son objetivos de la campaña nueva.

## Misión 1: llegada y primer escondite

Encontrar y usar la Nota #1 sobre la moqueta. Beber Agua de Almendras después de leerla y terminar con más de 70 de cordura. El fallo eléctrico y el aviso anuncian un Smiler real; se busca un lugar cercano con suelo, espacio y ruta, sin colocarlo encima del jugador. Usar el casillero metálico y permanecer dentro 200 ticks activos, diez segundos. Salir antes cancela el contador. Solo cabe un participante; basta que uno complete esta prueba cooperativa.

El participante escondido es invisible y queda excluido de los objetivos de Sabueso, Smiler y Bacteria y de la selección de encuentros. El escondite se libera al salir, morir o abandonar la partida. Al completar los diez segundos se retira el Smiler de la prueba y se recupera la posibilidad de ser detectado. Se desbloquea el valor numérico de cordura y un mapa mental básico en el HUD: cuadrícula de sectores con posición del jugador y notas descubiertas. No muestra las paredes del laberinto. Estado y muertes permanecen visibles.

## Misión 2: columnas y colores

Leer la Nota #2 y pulsar los cuatro pilares en orden **Azul → Amarillo → Rojo → Verde**. El progreso es compartido. Un error reinicia la secuencia, emite ruido y atrae una Bacteria real; se reutiliza una existente de esa especie para no apilar criaturas pulsando repetidamente.

La solución elimina físicamente la compuerta vertical de la sala de almacenamiento, antes sellada con paredes. Allí están la Nota #3 y la caja de herramientas. Los controles son bloques de misión protegidos, no bloques que se deban romper.

## Misión 3: traje Hazmat

Recoger la llave inglesa en almacenamiento. Usarla en la taquilla amarilla de seguridad del este, después de leer la Nota #3, para obtener casco, traje y botas. Se pueden recoger las piezas faltantes si el inventario estaba lleno o se perdió el equipo. La herramienta no se consume y cada jugador puede conseguir su propio conjunto.

Equipar las tres piezas completa la misión para el grupo. La protección se calcula por persona, por objetos realmente equipados y por la partida actual; una pieza guardada en el inventario no protege. El traje cubre visualmente torso y piernas desde la ranura del pecho, además del casco y las botas. La ropa amarilla utiliza una textura propia.

## Misión 4: zona radiactiva

Localizar la Nota #4 y la consola central en el sector de suelo verdoso. Hay partículas verdes, niebla verde mientras se permanece dentro y un contador Geiger sintetizado, mono y posicional. Sin las tres piezas equipadas se reciben tres puntos de daño mágico por segundo. El conjunto completo evita exclusivamente la radiación, no las mordidas ni otros peligros.

La presencia en el sector atrae un Sabueso, con velocidad base un 15% mayor. Solo la consola usada por alguien con el traje completo, y tras leer la Nota #4, desbloquea la puerta blindada y entrega su llave. Volver a usarla recupera una llave perdida. Los objetos están vinculados al número de partida, por lo que no se pueden usar llaves de intentos antiguos.

## Misión 5: decisión final

Leer la Nota #5 después de activar la consola. Ambas rutas exigen las tareas anteriores:

- **Puerta blindada:** usar la llave frente a la puerta de acero, tres bloques de ancho y tres de alto. Pantalla negra de un segundo y descanso de noventa segundos antes de **Nivel 1: Zona de Carga**.
- **Pared Glitch:** mantener **agacharse (Shift por defecto) y avance** contra una pared anómala durante dos segundos continuos. Se mantiene la instrucción directa del usuario anterior al documento; correr no activa la salida. Pantalla negra y descanso antes de **Nivel 0.1: Darkrooms**.

Todo el equipo revive al pasar y se reinician las muertes y cordura como antes. El grupo comparte el destino elegido por la primera salida activada; no se divide en dos campañas simultáneas. Se otorga el logro de finalización del Nivel 0 a los participantes conectados, incluidos quienes esperan ser revividos.

Darkrooms es una dimensión real con el mismo tamaño de dificultad y conectividad básica, paredes oscuras y fluorescentes fundidos. En esta entrega es un desvío de exploración: localizar su pared de salida y atravesarla con Shift + avance conduce, tras el descanso, a Zona de Carga. Esta continuación se eligió porque el documento define su entrada, pero no misiones propias ni una salida posterior. El Nivel 1 conserva sus tres relés y conduce al festejo final.

## Orientación y cooperación

`/backrooms journal` muestra la misión y las notas ya descubiertas por el grupo. `/backrooms objective` añade dirección y distancia al siguiente punto útil. El HUD muestra misión 1–5, objetivo, dirección, dificultad y mapa mental al desbloquearlo. La orientación de equipo de protección también tiene en cuenta el inventario de cada jugador para dirigirlo a la caja de herramientas o a la taquilla.

Las salas se colocan proporcionalmente en los mapas de 100/150/250/350/500. Se despejan sus interiores y sus accesos conectan con los pasillos del generador. La sala de radiación puede quedar próxima a la pared Glitch en Principiante; la pared se restaura después de despejar las salas para no borrarla accidentalmente.

Progreso, notas y secuencia se guardan en el estado compartido. Reiniciar limpia las misiones. Los escondites en curso no sobreviven a una desconexión y el intento no tiene checkpoints al cerrar el mundo, igual que antes.

## Validación y actualización

La suite conserva la campaña de cuatro perfiles simulados usando la puerta blindada y los relés, añade la ruta completa a Darkrooms y después a Zona de Carga, escondite continuo/cancelación, radiación real con protección individual, una Bacteria real por error de colores, persistencia y reinicio. También comprueba rutas sobre el mapa Principiante generado y la compuerta de almacenamiento antes/después de resolver el puzle, además de límites geométricos en las cinco dificultades.

La revisión visual de notas, traje, casilleros, mapa mental y niebla verde, y la prueba con varios clientes Essential, se deben realizar jugando. El arranque del cliente y las pruebas del servidor no sustituyen esa revisión. El protocolo cambia a 7; instalar la misma versión 1.3.0 en todos los clientes y servidor y comenzar una partida nueva. No es necesario borrar el mundo.
