# Bedroom — alcance de la primera versión

Estado: propuesta de diseño lista para revisión. No representa funcionalidades implementadas.

Objetivo técnico: Minecraft Java 1.20.1, Forge 47.4.28.

## 1. Experiencia objetivo

Crear una experiencia de exploración y supervivencia de terror inspirada en la referencia descrita por el usuario: un laberinto de oficinas amarillas donde el aislamiento, la desorientación y los sonidos generan más tensión que el combate.

El recorrido de una partida será: entrar al Nivel 0, explorar, encontrar suministros, administrar cordura y luz, sobrevivir a encuentros poco frecuentes y localizar una pared de falla para escapar hacia una zona de llegada del Nivel 1.

Los recursos visuales, sonidos, modelos y código serán propios o tendrán licencia compatible con su distribución. La referencia define la atmósfera; no se presupone acceso a recursos del juego original.

## 2. Requisitos y decisiones propuestas

Las descripciones del usuario y su tabla de objetos constituyen los requisitos de referencia. Las decisiones de esta sección completan vacíos de diseño y pueden ajustarse antes de implementar el sistema correspondiente.

| Tema | Decisión propuesta para V1 |
| --- | --- |
| Modalidad | Un jugador y cooperativo en servidor dedicado. Cordura e inventario individuales. |
| Entrada | Usar un objeto de acceso, obtenido mediante una receta sencilla, inicia una transición al Nivel 0. Un comando de desarrollo permite probar la entrada sin fabricar el objeto. La receta se define en etapa 2. |
| Inicio | Cordura al 100%; una linterna cargada al 100% y un Agua de Almendras, entregadas una sola vez por jugador y mundo. |
| Regreso | La salida normal del Nivel 0 conduce a una zona segura pequeña del Nivel 1. Esa zona incluye un punto explícito de regreso al mundo de origen. |
| Construcción | No se pueden romper ni colocar bloques en Nivel 0 en supervivencia. Abrir contenedores y usar objetos sí está permitido. Creativo y herramientas administrativas quedan disponibles para desarrollo. |
| Muerte | Reaparición en una sala segura del Nivel 0, cordura restaurada al 100%. Se conserva el inventario dentro de esta dimensión; no se repone el equipo inicial. |
| Hambre y combate | Se mantiene hambre vanilla. Sin aparición natural de criaturas vanilla dentro del Nivel 0. El Sabueso no deja loot. |
| Descanso | Las camas no permiten dormir ni establecer reaparición dentro del Nivel 0. |
| Guardado | Salir de la partida conserva posición, inventario, cordura, batería y penalizaciones pendientes. |
| Escape administrativo | Comando para devolver a un jugador a una posición segura, sin ser parte de la progresión normal. |

La conservación de inventario, protección de bloques y reaparición se aplican al Nivel 0, sin cambiar las reglas del resto del mundo.

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
| Monotonía | Tras 180 segundos sin otros eventos sonoros relevantes, pierde 0.5 puntos por minuto mientras continúa esa condición. |
| Coincidencia | Ambos drenajes se acumulan. No hay un tercer drenaje por el simple hecho de estar junto a una lámpara. |
| Menos de 50 | Viñeta gradual y susurros ocasionales. |
| Menos de 25 | Lentitud I, episodios breves de ceguera y alucinaciones de pasos. |
| Igual a 0 | Daño de pánico de 1 punto de vida cada 2 segundos, hasta recuperar cordura o morir. |
| Recuperación | Agua de Almendras: +30 puntos. No existe regeneración pasiva de cordura en Nivel 0. |

Propuesta inicial para ceguera: 1 segundo por evento, con descanso aleatorio de 20–40 segundos. Los efectos propios del mod se retiran al superar el umbral; no se eliminan efectos aplicados por otros sistemas.

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
- Pantalla negra de 1 segundo y traslado a una posición previamente validada de la zona de llegada del Nivel 1.
- Inventario y cordura se conservan; no se arrastran entidades perseguidoras.
- Protección contra activación repetida durante la transición.

El Nivel 1 de V1 es únicamente una zona de llegada segura y un regreso al mundo de origen. No es un segundo nivel completo.

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
| Entrada y reglas | Entrar, morir, reaparecer y regresar funcionan; el equipo inicial no puede duplicarse mediante reentrada. Las reglas de Nivel 0 no cambian otros mundos. |
| Generación | Recorrer al menos 100 habitaciones en cada una de 3 semillas sin huecos al exterior, cortes de conexión ni puntos de entrada atrapados. |
| Persistencia | Cerrar y volver a abrir conserva estado de jugador, loot abierto, batería y penalización de energética. |
| Cordura | Con herramientas de desarrollo se verifican drenajes y umbrales, incluidos valores exactos 50, 25 y 0; el agua detiene el daño al recuperar cordura. |
| Loot | Verificar mediante una muestra automatizada las tres tiradas, sus pesos y el límite de 1–3 objetos; reabrir no vuelve a generarlos. |
| Audio y efectos | Los bucles no se duplican al cambiar de dimensión o reconectarse; los efectos personales no afectan a otros jugadores. |
| Entidad | Comprobar cada transición de estado, obstáculos entre mirada y entidad, escapes por recodos y comportamiento con dos jugadores. |
| Salida | Probar activación interrumpida, activación completa y destino seguro; no pierde inventario ni encadena teletransportes. |
| Partida completa | Completar entrada → suministros → desgaste → encuentro → salida en una partida de prueba sin comandos de rescate. |
| Rendimiento | Registrar hardware, distancia de renderizado, FPS y tiempo de tick durante exploración y encuentros. Objetivo inicial: sostener 20 TPS en la configuración de prueba declarada; los objetivos de FPS se fijan con el hardware disponible. |

Las pruebas funcionales se ejecutarán tanto en un jugador como con dos clientes conectados a servidor dedicado cuando corresponda.

## 6. Secuencia de implementación

1. Etapa 2: proyecto compilable, registros, dimensión de prueba y comandos.
2. Etapa 3: bloques, biblioteca de habitaciones y generación conectada.
3. Etapa 4: cordura, objetos, contenedores, persistencia y validación de linterna.
4. Etapa 5: recursos finales, sonidos, iluminación y efectos de pantalla.
5. Etapa 6: Sabueso completo y balance del encuentro.
6. Etapa 7: pared de falla, anomalía espacial controlada y llegada al Nivel 1.
7. Etapa 8: partidas completas, correcciones, rendimiento y empaquetado.

Cada etapa entrega una compilación comprobable. Se conservan recursos provisionales cuando permitan validar primero el comportamiento.

## 7. Estado al cerrar la etapa 1

- Alcance de V1 documentado.
- Requisitos de referencia y decisiones propuestas diferenciados.
- Reglas de supervivencia, botín, entidad y transición especificadas.
- Exclusiones y criterios de aceptación definidos.
- No se ha creado ni compilado código del mod en esta etapa.

Este documento constituye la base de trabajo. Las decisiones propuestas pueden revisarse con el usuario; no se presentan como decisiones que ya haya aprobado explícitamente.
