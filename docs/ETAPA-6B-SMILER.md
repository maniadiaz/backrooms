# Etapa 6B — Smiler

Entrega 0.6.1 para Minecraft Java 1.20.1 / Forge 47.4.28. Amplía la etapa 6A con `bedroom:smiler`; Bacteria sigue pendiente.

## Movimiento, mirada y combate

El Smiler es una amenaza controlada por el servidor con navegación terrestre y colisiones normales. El rostro flota visualmente sobre el suelo; no usa navegación de fantasma, teletransporte ni vuelo a través de paneles. Su colisión mide 0.65 × 1.8 bloques y cabe en los pasillos de dos bloques y bajo el techo de tres.

1. Acecha lentamente hacia la posición inicial del participante. La adquisición visual y las miradas se limitan a 20 bloques.
2. La mirada de cualquier participante activo lo inmoviliza, incluso si ese compañero camina. Se exige línea de visión, cono estrecho y ausencia de ceguera. Las cámaras de espectador, los eliminados y quienes esperan en lobby no influyen.
3. Al perder todas las miradas hay un margen de siete ticks, aproximadamente 0.35 segundos. Después avanza agresivamente si ya lo habían observado. Recuperar una mirada válida vuelve a detenerlo inmediatamente en la lógica del servidor.
4. Retroceder respecto a la propia mirada en oscuridad puede activar el avance si nadie lo está observando. La observación de un compañero tiene prioridad. Minecraft no incorpora un parpadeo voluntario: aquí se utilizan la pérdida de mirada y la ceguera de cordura.
5. Tras un obstáculo solo conserva la última posición vista. Quince segundos sin evidencia hacen que abandone ese objetivo; no conoce las coordenadas actuales a través de paredes.

El contacto sin observadores ni margen de protección provoca 16 puntos de daño base y pone la cordura a cero si el jugador sigue activo tras el golpe. Hay 30 ticks entre ataques. La dificultad y las defensas normales de Minecraft modifican el daño; ocho corazones base pueden resultar letales según esas condiciones.

El anfitrión puede activar `smilerLethal=true` en `config/bedroom-common.toml`: el golpe pasa a 1000 puntos base. Ambos modos usan daño normal y el sistema de muertes existente. Si el ataque ya envió al jugador al limbo, no vuelve a poner su cordura a cero después de la transición. El Agua de Almendras sigue recuperando cordura y deteniendo el daño de pánico.

El encuentro expira a los tres minutos de ticks cargados de la entidad. Reiniciar, entrar al lobby/descanso o cambiar de nivel lo elimina en su siguiente tick. Los datos guardados conservan ronda, nivel y tiempo transcurrido para descartar encuentros antiguos al cargar.

## Aparición compartida

Cada oportunidad del sistema existente elige con igual probabilidad entre Hound y Smiler. No duplica la frecuencia total: conserva celdas exploradas, umbral de cordura, margen inicial de un minuto y enfriamiento de cinco minutos. Si se elige Smiler y no existe un lugar oscuro válido, no se fuerza otra especie en ese intento.

Además de los requisitos comunes —chunk cargado, suelo sólido, ruta válida, sin colisiones, fuera de vista y al menos a 24 bloques de todos los participantes activos— Smiler necesita luz de bloque de siete o menos a la altura de los ojos. Solo aparece en el área del laberinto, nunca en salas seguras. Puede entrar posteriormente en una zona iluminada; la luz no es un escudo de combate.

El mismo presupuesto comprueba amenazas `Monster` alrededor del jugador de referencia y del candidato. Un Hound bloquea la aparición de Smiler y viceversa; la cordura por proximidad ya reconoce ambas especies sin sumar una penalización por cada una.

## Rostro y sonido

La textura original de 64 × 64 contiene únicamente ojos y sonrisa sobre transparencia. El renderizado emisivo conserva el brillo en oscuridad y respeta la profundidad de las paredes. El plano mira hacia la cámara y oscila levemente en altura. Es una interpretación de estilo píxel para Minecraft; no se presenta como un rostro humano fotorrealista.

- Movimiento sin pisadas.
- Siseo OGG mono en un único bucle por entidad y cliente. Comienza a 20 bloques y sube gradualmente de volumen y tono al acercarse, con valores acotados. Un margen de salida de 22 bloques evita reinicios continuos en el borde.
- Risa distorsionada cada cuatro segundos cuando hay un participante activo a menos de cinco bloques; estallido al acertar un ataque.
- Posición y paneo mediante el motor de Minecraft, bajo el volumen de criaturas hostiles. No se promete oclusión acústica por paredes. El silencio del cliente no altera la cordura ni la IA.
- El siseo lógico interrumpe la monotonía de participantes dentro de 20 bloques. Al desaparecer, cambiar de dimensión, salir de la escena, silenciar criaturas hostiles o recargar el audio se detienen los bucles; al volver pueden iniciarse otra vez.

`python scripts/generate_smiler.py` reconstruye la cara y los tres sonidos originales, sin grabaciones externas. Usa las dependencias de `scripts/audio-requirements.txt` y preserva el manifiesto de sonidos de las otras especies. Los OGG mono a 32 kHz están incluidos en el jar; jugar no requiere Python.

## Verificación y prueba manual

Ejecutar `scripts/gradle.ps1 build runGameTestServer --console=plain`. La suite añade observadores cooperativos, exclusión de espectadores, ceguera y obstáculos, margen de mirada, búsqueda por última posición, daño/contacto/enfriamiento, variante letal con limbo, lectura de encuentros antiguos, luz real propagada, navegación en L, presupuesto compartido y validación de la transparencia y de las cabeceras mono de los OGG.

Resultado de esta entrega: compilación correcta y 43 pruebas obligatorias aprobadas. El cliente arrancó con registro del renderizador, recarga de recursos y motor de sonido, sin errores de Bedroom en el registro. Se cerró el cliente de prueba después de comprobar el arranque. La cara se inspeccionó como PNG; su presentación dentro de una partida sigue pendiente.

En una partida activa, con trucos u operador, `/summon bedroom:smiler ~ ~ ~5` permite probarlo en un punto libre. El comando administrativo omite deliberadamente las restricciones naturales de oscuridad y frecuencia. Fuera de una partida activa se descarta. Para comprobar mirada, detenerse y apuntar a la cara; apartar la cámara permite que avance. `/bedroom restart` mantiene las votaciones del equipo.

Pendientes de una partida real: revisar orientación y brillo del rostro, localizar el siseo al girar, ajustar velocidad/dificultad y probar de dos a cuatro clientes mediante Essential. La compilación, el arranque del cliente y las pruebas del servidor no sustituyen esa revisión visual, auditiva y cooperativa.

Instalar el mismo `bedroom-0.6.1.jar` en todos los clientes y el servidor. No cambia el generador ni exige crear un mundo nuevo de las etapas 0.3–0.6. La siguiente entrega es Bacteria, etapa 6C.
