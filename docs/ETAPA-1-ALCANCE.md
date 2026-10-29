# Bedroom — alcance de la primera versión

Estado: diseño actualizado con las reglas del usuario para partidas cooperativas. Los valores de balance identificados como propuestas siguen siendo ajustables. Este documento no representa funcionalidades implementadas.

Objetivo técnico: Minecraft Java 1.20.1, Forge 47.4.28.

## 1. Experiencia objetivo

Crear una experiencia de exploración y supervivencia de terror inspirada en la referencia descrita por el usuario: un laberinto de oficinas amarillas donde el aislamiento, la desorientación y los sonidos generan más tensión que el combate.

El recorrido de una partida será: reunir de uno a cuatro jugadores en un lobby, iniciar con un botón, explorar el Nivel 0, administrar recursos y cordura, sobrevivir y localizar la salida. Entre niveles habrá un descanso de 90 segundos, que se puede omitir con un botón. Una derrota de todo el equipo devuelve al lobby y reinicia la partida; completar la campaña lleva a un lobby de festejo con fuegos artificiales.

Los recursos visuales, sonidos, modelos y código serán propios o tendrán licencia compatible con su distribución. La referencia define la atmósfera; no se presupone acceso a recursos del juego original.

## 2. Requisitos y decisiones propuestas

Las descripciones del usuario y su tabla de objetos constituyen los requisitos de referencia. Las decisiones de esta sección completan vacíos de diseño y pueden ajustarse antes de implementar el sistema correspondiente.

| Tema | Regla para V1 |
| --- | --- |
| Modalidad | De 1 a 4 jugadores por partida; un jugador y cooperativo en servidor dedicado. Cordura individual. |
| Entrada | Lobby con botón de inicio que incorpora al grupo al Nivel 0. Se descarta el objeto de acceso y su receta. |
| Inicio | Propuesta de equipo: cordura al 100%, una linterna al 100% y un Agua de Almendras por participante al empezar cada partida. No se duplica al reconectarse. |
| Muerte | Primera y segunda muerte de cada nivel: limbo blanco con laberinto y 30 segundos para salir. Tercera muerte: espectador directo. Si vence el tiempo en el limbo, espectador. Confirmado por el usuario. |
| Derrota | Si todo el equipo muere, termina la partida: todos al lobby y progreso de partida desde cero. El caso de varios jugadores simultáneamente en limbo se concreta en el documento de partidas. |
| Fin de nivel | Los eliminados reviven, se reinicia el contador de muertes por nivel y todos avanzan juntos. |
| Descanso | Entre niveles, 90 segundos de espera o botón para empezar inmediatamente. Sin drenaje ni enemigos durante el descanso. |
| Construcción | No romper ni colocar bloques; únicamente se pueden colocar objetos autorizados de misiones en sus puntos válidos. Interactuar con contenedores y botones sigue permitido. |
| Salida provisional | Mientras no exista Nivel 1, completar Nivel 0 devuelve al lobby. |
| Victoria final | Al completar todos los niveles de la campaña terminada, lobby de festejo y fuegos artificiales. |
| Hambre y combate | Hambre vanilla; sin aparición natural de criaturas vanilla en Nivel 0. El Sabueso no deja loot. |
| Camas | No permiten dormir ni establecer reaparición en las zonas de partida. |
| Guardado | Persistir partida, participantes, muertes, estados, temporizadores, inventarios y cordura; reconectarse no reinicia el limbo ni concede vidas. |
| Administración | Comandos de desarrollo y rescate separados de la progresión normal. |

Las reglas de partida no deben modificar las reglas del resto del mundo. El detalle de estados, casos límite y propuestas pendientes está en [Partidas y lobby](PARTIDAS-Y-LOBBY.md).

## 3. Contenido incluido en V1

### 3.1 Nivel 0

- Dimensión propia, sin clima ni luz natural.
- Suelo horizontal continuo y tres bloques de espacio libre entre suelo y techo.
- Salas rectangulares, pasillos, recodos en L y espacios abiertos conectados.
- Sin escaleras, ventanas, puertas tradicionales ni salidas evidentes.
- Paredes delgadas, papel amarillo ocre desgastado, moqueta marrón amarillenta y techo modular blanco grisáceo.
- Columnas ocasionales, enchufes decorativos y humedad agrupada cerca de esquinas.
- Generación por semilla y por zonas, con conexiones transitables y entrada segura.
- Sensación de extensión indefinida dentro de los límites del mundo de Minecraft.

La geometría imposible se limitará en V1 a una anomalía de recorrido controlada, añadida después de estabilizar el laberinto convencional. No se incluye un sistema general de portales visibles o renderizado de espacios superpuestos.

### 3.2 Iluminación y audio

- Fluorescentes encendidos, parpadeantes y fundidos.
- Zumbido continuo con volumen base del 35%, ajustable.
- Pisadas húmedas sobre la moqueta.
- Pasos distantes cada 45–90 segundos por jugador, sin implicar necesariamente una entidad real.
- Goteos y chasquidos de lámparas localizados.
- Niebla tenue a distancia; viñeta y eventos auditivos según cordura.
- Ajustes de intensidad de parpadeos y efectos de pantalla, sin alterar las reglas de supervivencia.

El temporizador de monotonía se reinicia con eventos ambientales distintos del zumbido y sonidos relevantes de entidades o interacciones cercanas. Las pisadas propias y los sonidos de interfaz no lo reinician. Silenciar el audio en opciones no elimina el drenaje: se calcula por eventos del mundo.

### 3.3 Cordura

Valor individual entre 0 y 100, con decimales; nunca sale de ese intervalo.

| Condición | Regla |
| --- | --- |
| Penumbra | Pierde 1 punto cada 10 segundos acumulados en zonas con luz de bloque de 7 o menos a la altura de los ojos. |
| Monotonía | Tras 180 segundos sin otros eventos sonoros relevantes, pierde 0.5 puntos por minuto mientras continúa esa condición, incluso en zonas iluminadas. Se mantiene este valor; subirlo a 1 sería un ajuste de balance futuro. |
| Entidad cercana | A menos de 15 bloques, añade 1 punto cada 10 segundos. No se multiplica por número de entidades. |
| Coincidencia | Los drenajes ambientales se suman con un máximo de 3 puntos por cada 10 segundos; cálculo individual y con decimales. |
| Energética | Resta 5 puntos una sola vez al vencer: penalización puntual separada del tope de drenaje ambiental. |
| Compañía (opcional) | Propuesta configurable: otro jugador activo a menos de 10 bloques reduce solo el drenaje de zumbido a la mitad. Los espectadores no cuentan. |
| Menos de 40 | Puede habilitar aparición de entidad, respetando límites de encuentros. |
| Menos de 50 | Viñeta gradual y susurros ocasionales. |
| Menos de 25 | Lentitud I, episodios breves de ceguera y alucinaciones de pasos. |
| Igual a 0 | Daño de pánico de 1 punto de vida cada 2 segundos, hasta recuperar cordura o morir. |
| Recuperación | Agua de Almendras: +30 puntos. No existe regeneración pasiva de cordura en Nivel 0. |

Propuesta inicial para ceguera: 1 segundo por evento, con descanso aleatorio de 20–40 segundos. Los efectos propios del mod se retiran al superar el umbral; no se eliminan efectos aplicados por otros sistemas.

El drenaje ambiental solo se aplica durante el nivel activo; no en lobby, limbo, descanso ni modo espectador. Con las tres fuentes actuales, el máximo normal es 2 + 0.5/6 = 2.08333 puntos por 10 segundos. La implementación integra las tasas por tiempo de servidor y limita la suma a 0.3 puntos por segundo, evitando acumular descuentos atrasados en un golpe.

Las alucinaciones personales no dañan ni generan enemigos físicos. La interfaz muestra la cordura y permite verificar sus cambios durante desarrollo.

### 3.4 Objetos

En Minecraft un corazón equivale a 2 puntos de vida y un muslo a 2 puntos de hambre.

| Objeto | Efecto V1 |
| --- | --- |
| Agua de Almendras | +30 cordura, +6 hambre; elimina lentitud y ceguera causadas por cordura y efectos leves de hambre, náusea y debilidad. No cura todos los efectos negativos indiscriminadamente. |
| Barra nutritiva | +8 hambre y Regeneración I durante 5 segundos. |
| Energética Juice | Velocidad II y Prisa I durante 30 segundos; al vencer, resta 5 puntos de cordura una sola vez. |
| Linterna | Iluminación portátil direccional al activarse en mano principal o secundaria; batería persistente de 0–100%. |
| Batería AAA | Recarga 25 puntos de una linterna, sin superar 100; se consume solo si aporta carga. |
| Vendas | Recuperan 6 puntos de vida, sin superar la salud máxima; uso de 2 segundos. |
| Brújula anómala | Dentro de 30 bloques señala la pared de falla más cercana; fuera de alcance gira de forma errática. |

Decisiones de balance inicial: linterna completa dura 20 minutos encendida; apagada o guardada no consume. Usar una batería recarga primero la linterna sostenida y después la primera linterna con carga incompleta del inventario.

Beber otra energética renueva la duración, sin acumular amplificadores ni múltiples penalizaciones: habrá un único descuento pendiente de 5 puntos. Su tiempo restante se guarda al desconectarse y continúa al volver a jugar. Los consumibles de hambre pueden usarse con la barra llena cuando aporten otro efecto útil.

La linterna direccional es un requisito de la experiencia, pero su técnica de iluminación y su coste se validarán en etapa 4; no se considera cumplido con un objeto que solo tenga textura luminosa.

### 3.5 Contenedores y distribución

Cajas de cartón selladas y archivadores metálicos; abrir una caja cambia su apariencia a abierta. El contenido se determina una sola vez y es compartido entre jugadores.

- Una colocación de contenedor cada intervalo aleatorio de 10–15 habitaciones generadas, escogiendo una habitación apta.
- Sin reposición automática de loot.
- Cada tirada entrega una unidad de un objeto; máximo tres objetos por contenedor.

| Tirada | Activación | Pesos relativos dentro de la tirada |
| --- | --- | --- |
| Básica | 100% | Agua 40; barra 45 |
| Utilidad | 50% | Batería 35; energética 25; linterna 15 |
| Especial | 15% | Vendas 20; brújula 5 |

Los números de la imagen se interpretan como pesos dentro de cada grupo, no como porcentajes globales. Las tiradas son independientes: resulta una media de 1.65 objetos por contenedor.

### 3.6 Sabueso

Una sola entidad original en V1. Estados: inactivo, acecho, retroceso y caza.

- En generación se reserva aproximadamente un punto potencial cada 30–40 habitaciones; no equivale a una aparición obligatoria.
- Cordura inferior a 40 permite un encuentro adicional, sujeto a los mismos límites.
- Nunca aparece dentro del campo visual, en salas seguras ni a menos de 24 bloques de un jugador.
- Límite inicial: un Sabueso activo por grupo de jugadores cercanos, agrupados a 64 bloques; descanso mínimo de 5 minutos entre encuentros por grupo.
- Correr o romper bloques a menos de 15 bloques permite detectarte. La rotura se contempla para creativo, administración o futuras zonas donde esté permitida.
- Cordura inferior a 30 permite iniciar acecho si ya existe un Sabueso cercano.
- Mirarlo requiere línea de visión y apuntar hacia su cuerpo dentro de un cono estrecho; no basta con tenerlo en pantalla.
- Al mirarlo se detiene y gruñe. Mantener la mirada durante 3 segundos continuos provoca caza, como en el diagrama de referencia.
- Correr o darle la espalda durante el encuentro también provoca caza.
- Ataque: 6–8 puntos de daño base antes de armadura y resistencias, con al menos 1.5 segundos entre golpes.
- La caza termina si pierde detección y línea de visión durante 15 segundos. A continuación vuelve a acechar; no se teletransporta frente al jugador.

La interacción de miradas se evalúa por objetivo. En cooperativo, otro jugador puede provocar la caza aunque uno lo esté mirando; la mirada no garantiza inmovilización permanente.

Velocidad y resistencia se calibrarán con el laberinto real. Debe ser posible escapar usando recodos y distancia; no se exige matar a la entidad.

### 3.7 Salida y transición

- Pared de falla con tono ligeramente oscuro y anomalía visual sutil.
- Una salida garantizada por región inicial de 128 × 128 bloques; su posición se escoge en una pared accesible desde la red de habitaciones. Densidad ajustable tras pruebas.
- Activación: mantener carrera e intento de avanzar hacia la pared durante 2 segundos continuos, estando en contacto frontal.
- Interrumpir el intento reinicia el contador. No se exige desplazamiento real mientras existe colisión.
- Pantalla negra de 1 segundo y resolución de fin de nivel para el equipo.
- Si existe el siguiente nivel, reviven todos y pasan a su zona segura de descanso de 90 segundos; un botón permite comenzar antes.
- Mientras no exista Nivel 1, regreso al lobby, sin presentarlo como final de la campaña.
- Cuando se complete la lista definitiva de niveles, traslado al lobby de festejo con fuegos artificiales.
- No se arrastran entidades perseguidoras; la política propuesta de inventario y recuperación se detalla en el documento de partidas.
- Protección contra activación repetida durante la transición.

V1 incluye solo el Nivel 0 y retorno provisional al lobby. El sistema de progresión admite niveles futuros y distingue el final provisional del final real de campaña.

## 4. Fuera del alcance de V1

- Otros niveles completos, múltiples especies, jefes y combate especializado.
- Campaña narrativa, misiones, voces y cinemáticas extensas.
- Portales con vista en tiempo real y geometría no euclidiana general.
- Destrucción o construcción libre en Nivel 0.
- Compatibilidad garantizada con todos los shaders, mods gráficos y otros mods de dimensiones.
- Ediciones Bedrock, Fabric, NeoForge y otras versiones de Minecraft.
- Modpack grande con dependencias adicionales antes de validar el mod principal.

## 5. Criterios de aceptación

| Sistema | Evidencia necesaria para considerarlo terminado |
| --- | --- |
| Instalación | El jar carga en cliente y servidor dedicado con las versiones fijadas, sin errores de registro ni acceso a clases exclusivas del cliente desde el servidor. |
| Entrada y reglas | Lobby acepta 1–4 participantes y rechaza un quinto; un botón inicia una única partida. Equipo inicial sin duplicación. Protección de bloques y excepción de misión comprobadas sin afectar otros mundos. |
| Muerte y limbo | Primera y segunda muerte permiten limbo durante 30 segundos; tercera envía a espectador. Verificar salida a tiempo, vencimiento, derrota colectiva y reinicio limpio. |
| Cooperativo y progreso | Revivir a todo el grupo al completar nivel; reiniciar muertes; descanso de 90 segundos o inicio anticipado; espectadores no modifican el mundo. |
| Generación | Recorrer al menos 100 habitaciones en cada una de 3 semillas sin huecos al exterior, cortes de conexión ni puntos de entrada atrapados. |
| Persistencia | Cerrar y volver a abrir conserva estado de jugador, loot abierto, batería y penalización de energética. |
| Cordura | Verificar cada fuente y su suma, tope de 3/10 s con una fuente de prueba adicional, energética de -5 separada, compañía opcional y límites exactos 50, 40, 25 y 0. Sin drenaje fuera del nivel activo. |
| Loot | Verificar mediante una muestra automatizada las tres tiradas, sus pesos y el límite de 1–3 objetos; reabrir no vuelve a generarlos. |
| Audio y efectos | Los bucles no se duplican al cambiar de dimensión o reconectarse; los efectos personales no afectan a otros jugadores. |
| Entidad | Comprobar cada transición de estado, obstáculos entre mirada y entidad, escapes por recodos y comportamiento con dos jugadores. |
| Salida | Verificar activación interrumpida y completa, transición única del grupo, retorno provisional al lobby y victoria final de campaña con festejo. |
| Partida completa | Completar entrada → suministros → desgaste → encuentro → salida en una partida de prueba sin comandos de rescate. |
| Rendimiento | Registrar hardware, distancia de renderizado, FPS y tiempo de tick durante exploración y encuentros. Objetivo inicial: sostener 20 TPS en la configuración de prueba declarada; los objetivos de FPS se fijan con el hardware disponible. |

Las pruebas funcionales se ejecutarán tanto en un jugador como con dos clientes conectados a servidor dedicado cuando corresponda.

## 6. Secuencia de implementación

1. Etapa 2: proyecto compilable, registros, dimensión de prueba y comandos.
2. Etapa 2.1 (nuevo prerrequisito): gestor de partidas de 1–4 jugadores, lobby, botón, estados de jugador, limbo, espectadores, derrota y descanso; empezar con salas provisionales.
3. Etapa 3: bloques, biblioteca de habitaciones y generación conectada; laberinto blanco del limbo.
4. Etapa 4: cordura, objetos, contenedores, persistencia y validación de linterna.
5. Etapa 5: recursos finales, sonidos, iluminación y efectos de pantalla.
6. Etapa 6: Sabueso completo y balance del encuentro.
7. Etapa 7: pared de falla, anomalía espacial controlada, transición de equipo y regreso provisional al lobby; victoria final preparada para campaña futura.
8. Etapa 8: partidas completas, correcciones, rendimiento y empaquetado.

Cada etapa entrega una compilación comprobable. Se conservan recursos provisionales cuando permitan validar primero el comportamiento.

## 7. Estado al cerrar la etapa 1

- Alcance de V1 documentado.
- Requisitos de referencia y decisiones propuestas diferenciados.
- Reglas de supervivencia, botín, entidad y transición especificadas.
- Exclusiones y criterios de aceptación definidos.
- No se ha creado ni compilado código del mod en esta etapa.

Este documento incorpora las reglas explícitas del usuario. Las decisiones complementarias identificadas como propuestas no se presentan como aprobadas. La etiqueta etapa-1 conserva el diseño inicial; esta revisión no mueve etiquetas anteriores.
