# Rediseño de enemigos — Backrooms 1.2.0

Modelos originales inspirados en las tres referencias del usuario, construidos con geometría nativa de Minecraft, sin copiar las imágenes como texturas ni añadir dependencias de animación.

## Sabueso

Cuadrúpedo encorvado, con joroba, columna y costillas marcadas, piel pálida moteada y cabeza cubierta por una estructura oscura. Tiene cejas inclinadas, cuencas vacías, colmillos y dientes individuales, mandíbula articulada, patas con rodillas y tres garras por pie. La mandíbula se abre más durante la carga y el trote se convierte en una zancada más amplia. Al sostenerle la mirada inclina ligeramente la cabeza y muestra los dientes. El estado de animación se sincroniza desde el servidor, también para invitados por Essential.

Su colisión, daño y reglas de mirada conservan el balance anterior: la petición de mayor agresividad se aplica al diseño y la animación.

## Bacteria

Pasa de cuadrúpedo bajo a humanoide bípedo de aproximadamente tres bloques. La colisión mide 0.75 × 2.95 bloques: cinco centímetros de margen permiten navegar bajo el techo del Nivel 0 sin atravesarlo. La silueta tiene columna expuesta, vértebras, clavículas, costillas abiertas, pelvis, cabeza irregular y cresta asimétrica. Brazos largos con codos, cables laterales, palmas y cuatro dedos de dos segmentos; piernas delgadas con rodillas y pies.

Usa materiales marrón verdoso, hueso envejecido y cables oscuros en un atlas UV de 128 × 128. Camina erguida, balancea los brazos e inclina el torso durante la persecución. Mantiene sus estados de alerta y búsqueda sincronizados. La trepa ahora comprueba espacio para toda su caja de colisión elevada, para impedir que la nueva altura penetre un techo bajo.

La suite comprueba tanto una ruta en L bajo techo de tres bloques como movimiento real bloqueado por un hueco de solo dos bloques.

## Smiler

Tiene un cuerpo negro tridimensional, de superficie oscura irregular, construido como esfera de 24 segmentos y 12 anillos. Flota y pulsa muy levemente. La cara continúa orientándose hacia la cámara: ojos redondos completamente blancos, sonrisa roja y dos filas de dientes blanco amarillento, con separaciones y longitudes irregulares. La cara usa una capa emisiva transparente de 256 × 256, independiente del material negro opaco del cuerpo; así se distingue en la oscuridad sin iluminar todo el cuerpo.

Conserva su IA, colisión y daño. El cuerpo visual mide aproximadamente 1.45 bloques de ancho y cabe en los pasillos principales de dos bloques. No se añadió un efecto de destellos ni cambios rápidos de tamaño.

## Regeneración y comprobación

`python scripts/generate_enemy_designs.py` reconstruye únicamente los materiales de los tres enemigos mediante biblioteca estándar. Los generadores anteriores de audio vuelven a aplicar esos materiales al terminar, por lo que no restauran las texturas antiguas. No se modificaron los sonidos en esta entrega.

La compilación y los 70 GameTests comprueban navegación, colisiones, estados, ataques, reglas de partida y recursos empaquetados, incluidos el material negro de Smiler, sonrisa roja y dientes amarillentos. El arranque del cliente se utiliza para detectar errores al cargar las capas de modelos; eso no equivale a revisar visualmente un encuentro completo ni una sesión con varios clientes.

El protocolo cambia a 6. Todos los clientes y el servidor deben usar 1.2.0 para compartir la nueva altura y los estados de animación. No hace falta eliminar mundos.
