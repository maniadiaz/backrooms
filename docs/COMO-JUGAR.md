# Cómo jugar — Backrooms 1.4.0

Uno a cuatro participantes. Al entrar aparecen en el lobby. El primer integrante es anfitrión. El botón verde inicia desde cero y el azul carga el último checkpoint guardado. También se puede elegir desde Esc en el lobby. Quienes lleguen tarde esperan o miran las cámaras de sus compañeros.

## Dificultad

Antes de empezar, el anfitrión usa `/backrooms difficulty facil`. Opciones: `principiante` 100 × 100, `facil` 150 × 150 (inicial), `normal` 250 × 250, `dificil` 350 × 350 y `hardcore` 500 × 500. El mapa tiene límites y la elección queda fija durante la partida. Subir dificultad aumenta pérdida de cordura, frecuencia, daño y velocidad de las entidades.

## Nivel 0: cinco misiones

1. **Primer escondite:** encontrar y usar la Nota #1 en la moqueta. Beber Agua de Almendras y quedar por encima del 70% de cordura. Tras el aviso del zumbido, usar el casillero y permanecer dentro diez segundos. Salir antes cancela el contador. Esto desbloquea el contador de cordura y el mapa mental del sector.
2. **Colores:** leer la Nota #2 y pulsar los cuatro pilares en orden **Azul → Amarillo → Rojo → Verde**. Equivocarse atrae Bacteria y reinicia la secuencia. Acertar abre la compuerta de almacenamiento.
3. **Hazmat:** leer la Nota #3, recoger la llave inglesa en la caja de herramientas y usarla en la taquilla amarilla del este. Equipar casco, traje y botas. Cada jugador necesita las tres piezas puestas para estar protegido.
4. **Radiación:** buscar la Nota #4, entrar al sector verdoso con el traje y usar la consola maestra. Cuidado con el Sabueso. La consola entrega la llave y desbloquea la puerta blindada. Si se pierde una llave o pieza, puede recuperarse en su fuente.
5. **Elegir salida:** leer la Nota #5. Usar la llave en la puerta blindada lleva al **Nivel 1: Zona de Carga**. Atravesar la pared Glitch lleva al **Nivel 0.1: Darkrooms**. Ambas requieren las misiones anteriores; todo el equipo toma la primera ruta activada.

Las notas se leen con usar bloque. `/backrooms journal` muestra las ya descubiertas; `/backrooms objective` orienta al siguiente destino. El mapa mental es una cuadrícula básica de sectores y notas conocidas, no un plano de las paredes. El progreso de misión es compartido; salud, cordura, comida y equipo son individuales.

En la radiación hay partículas y niebla verdes y un contador Geiger. Sin el traje completo se reciben tres puntos de daño por segundo. El traje evita la radiación, pero no los ataques. Guardarlo en el inventario no protege.

## Darkrooms y Zona de Carga

Darkrooms es un desvío oscuro con fluorescentes fundidos. Usar la linterna, localizar la pared de salida y atravesarla conduce a Zona de Carga. No tiene otra cadena de fusibles.

En Zona de Carga, encontrar tres relés numerados y usar cada uno en su panel correspondiente, juntos cerca de la salida. Reparar los tres habilita la compuerta anómala. Esta salida termina la campaña y lleva al atrio de victoria con fuegos artificiales.

Para atravesar cualquier pared anómala habilitada: mirar hacia ella, estar en contacto y mantener **agacharse (Shift por defecto) y avance durante dos segundos continuos**. Si cambiaste la tecla de agacharse, usa la asignada. Soltar una acción o perder contacto reinicia la carga. No hace falta correr ni tener comida. Sigue una pantalla negra de un segundo.

Entre niveles revive todo el equipo, se reinician muertes y cordura y hay noventa segundos de descanso; el anfitrión puede pulsar el botón para empezar antes. Al terminar el festejo se vuelve al lobby.

## Guardar, muertes y protección

Cada jugador tiene **diez vidas para toda la partida**, sin reponerlas al pasar de nivel. Las primeras nueve muertes dan tres minutos dentro del mismo mapa para buscar una puerta personal. Seguir los latidos: se oyen desde la dirección de la puerta y se aceleran al acercarse. Caminar hacia ella devuelve al último punto guardado del nivel, o a la entrada si no existe. Se recuperan los objetos, pero no la vida gastada. Las entidades no atacan durante esta recuperación.

Al agotar los tres minutos se espera como espectador hasta completar el nivel, si quedan vidas. La décima muerte elimina hasta el final del intento. Si todos quedan sin oportunidad, derrota y lobby; el checkpoint manual no se borra.

**Antes de cerrar:** el anfitrión usa el terminal verde **GUARDAR**, Esc → **Checkpoint del mundo → Guardar checkpoint** o `/backrooms save`. Guarda progreso, inventarios, salud, cordura, vidas y posiciones seguras en el mundo. Al regresar al lobby, azul o `/backrooms continue` recupera el slot; verde o `/backrooms start` empieza desde cero. Es un slot manual del mundo: volver a guardar reemplaza el anterior. No se guarda durante una transición ni un escondite de la misión 1.

Desconectarse de una partida activa sigue llevando al lobby de espera al volver; el equipo puede votar reiniciar. Para continuar el guardado se reúne el grupo en el lobby. `/backrooms leave` restaura el inventario externo protegido. No se puede romper ni construir libremente. F3 no muestra coordenadas durante la partida; la orientación usa dirección y distancia.

## Supervivencia

La comida permanece llena: **no necesitas comer**. El Agua de Almendras se usa por cordura y las barras por su regeneración opcional.

- Agua de Almendras: +30 de cordura y tres muslos de hambre.
- Barra nutritiva: cuatro muslos y regeneración breve.
- Bebida energética: treinta segundos de Velocidad II/Prisa I y luego -5 de cordura una vez.
- Vendas: tres corazones.
- Linterna 3D: usar para encender/apagar en cualquiera de las manos. Consume batería encendida; las pilas recargan 25%. La luz y el haz respetan paredes.
- Brújula anómala: se orienta hacia la pared de salida a menos de treinta bloques; fuera gira de forma errática.

El ambiente drena algo de cordura incluso con luz. Oscuridad, monotonía y entidades cercanas aumentan la pérdida hasta el tope de dificultad. Bajo 50 hay viñeta/susurros; bajo 25, lentitud y ceguera breve; a cero, daño de pánico. Los compañeros próximos reducen la monotonía.

## Entidades

- **Sabueso:** cuadrúpedo encorvado con cabeza oscura y garras. Mirarlo sin caminar lo hace dudar; correr, moverse o darle la espalda provoca carga.
- **Smiler:** esfera negra con ojos blancos, sonrisa roja y dientes amarillentos. Mirarlo lo inmoviliza; paredes y ceguera anulan esa observación. El contacto causa daño masivo y cordura a cero. El casillero protege durante el escondite.
- **Bacteria:** humanoide esquelético de unos tres bloques, brazos y dedos largos. Mirarla no la detiene. Persigue por visión y ruido; perderla requiere varios recodos y silencio. No atraviesa paredes ni techos.

## Comandos y controles

- `/backrooms start`, `/backrooms save`, `/backrooms continue`.
- `/backrooms lobby`, `/backrooms status`, `/backrooms leave`.
- `/backrooms journal`, `/backrooms objective`.
- `/backrooms restart`: propone votación durante la partida; `/backrooms vote restart` o `/backrooms vote continue`. Mayoría estricta, treinta segundos.
- `/backrooms spectate`, `next`, `previous`, `off`; cámaras con `]` y `[` reasignables.
- Comer/beber/curarse: mantener usar objeto. Notas, pilares, casilleros, herramientas y consola: usar bloque.

Las pruebas administrativas requieren operador y `developmentCommands=true`; la distribución las desactiva. Volumen, niebla y cámara se ajustan en `config/bedroom-client.toml`. Actualizar todos a la misma versión y dejar un solo jar del mod.
