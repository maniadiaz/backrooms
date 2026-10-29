# Cómo jugar

**Objetivo:** escapar del Nivel 0 y de Zona de Carga con un equipo de una a cuatro personas.

## Partida

Al entrar aparecen en el lobby. El primer integrante es anfitrión y pulsa el botón sobre el bloque verde para iniciar. Quienes llegan después esperan la siguiente partida y pueden observar a sus compañeros.

En el Nivel 0 busquen tres estuches numerados de fusibles. Clic derecho para recogerlos; con cada fusible en la mano, clic derecho en el panel del mismo número. Los paneles están juntos cerca de la salida. En Zona de Carga se hace lo mismo con relés y controles de carga.

Reparar los tres habilita las paredes/compuertas anómalas oscuras. Mantener avance y carrera contra una durante dos segundos, con comida suficiente para correr. La pantalla se pone negra un segundo y pasa todo el equipo. El primer nivel conduce a un descanso de noventa segundos; el anfitrión puede pulsar el botón para salir antes. El segundo conduce al atrio de victoria y fuegos artificiales, y después al lobby.

No se puede romper ni construir libremente. Los objetos de misión solo se insertan en sus paneles. Un componente perdido por eliminación o desconexión puede recuperarse de su estuche; no se duplica si alguien todavía lo lleva o puede recuperarlo del limbo. Las copias sustituidas dejan de funcionar.

## Muertes y desconexiones

Primera y segunda muertes por nivel: limbo blanco, treinta segundos para encontrar el botón de salida. Si se acaba el tiempo, espectador. Tercera muerte: espectador directamente. Al completar un nivel revive todo el equipo y se reinicia su contador de muertes. Si no queda nadie activo ni con una oportunidad recuperable en limbo, derrota y lobby.

Desconectarse pierde el progreso personal. Al volver se espera en el lobby: los que siguen dentro pueden votar reiniciar o continuar sin ese participante. Cerrar/reabrir el mundo también empieza de cero. `/bedroom leave` devuelve el inventario externo protegido.

## Cordura y objetos

- Agua de Almendras: +30 de cordura y tres muslos de hambre.
- Barra nutritiva: cuatro muslos y regeneración breve.
- Bebida energética: treinta segundos de Velocidad II/Prisa I y luego -5 de cordura una sola vez.
- Vendas: tres corazones.
- Linterna: encender/apagar con usar objeto en la mano. La pila recarga 25%; consume energía encendida en la mano.
- Brújula anómala: se orienta a una salida al acercarse a menos de treinta bloques; fuera de ese rango gira.

Penumbra, monotonía y entidades cercanas afectan a la cordura individual con un tope ambiental. Bajo 50 hay viñeta/susurros; bajo 25, lentitud y ceguera breve; a cero, daño de pánico hasta recuperar cordura. Los compañeros próximos reducen la monotonía.

## Entidades

- Sabueso: mirarlo sin caminar lo hace dudar. Moverse, correr o darle la espalda provoca carga; un compañero que corre puede atraerlo.
- Smiler: cualquier mirada válida de un participante activo lo inmoviliza. Al perder todas avanza; ceguera y paredes anulan la observación. El contacto causa daño masivo y cordura a cero.
- Bacteria: mirar no la detiene. Detecta visión cercana o ruido, avisa y persigue. Para perderla, romper la visión y permanecer silencioso tras varios recodos; puede trepar de forma limitada por superficies en contacto. No atraviesa paredes ni navega por techos.

## Comandos y controles

- `/bedroom lobby`, `/bedroom status`, `/bedroom leave`.
- `/bedroom restart`: propone votación durante la partida. `/bedroom vote restart` o `/bedroom vote continue`; gana una mayoría estricta y hay treinta segundos.
- `/bedroom spectate`, `next`, `previous`, `off`. Cámaras con `]` y `[`; se pueden reasignar.
- Comer/beber/curarse: mantener usar objeto. Insertar componente: clic derecho en su panel. Correr: tecla de carrera asignada más avance.

Las pruebas administrativas requieren operador y `developmentCommands=true`; no son atajos habilitados en la configuración de distribución. El volumen, niebla y movimiento opcional de cámara se ajustan en `config/bedroom-client.toml`. Los apagones y la cordura son reglas del servidor.
