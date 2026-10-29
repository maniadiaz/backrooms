# Etapa 10 — Cierre, comprobación y paquetes

Versión 1.0.0 de la campaña inicial de dos niveles. Se conserva la mecánica acordada de lobby, muertes, limbo, cordura y desconexiones. Las fases pendientes se ejecutaron consecutivamente y se guardaron en Git sin pedir una nueva instrucción de continuación.

Se añadió la trepa limitada de Bacteria con colisión real y animación sincronizada, respetando techo/altura y evitando intentos continuos. La prueba física específica eleva el total a 62 pruebas obligatorias aprobadas. No se anuncia navegación por techos ni por superficies arbitrarias.

Se generaron paquetes cliente y servidor, configuración recomendada, manifiestos internos con hashes y SHA-256 de los entregables. Se verifican el contenido del jar, su versión, TOML, CRC y hashes de cada ZIP. Se incluyen instrucciones de instalación y juego y avisos de dependencias. No se distribuyen binarios ajenos ni mundos/logs/capturas.

El workflow de GitHub prepara Java 17 y Python, construye, ejecuta GameTest y publica artefactos de compilación. El resultado local está comprobado; el remoto se informa solo después de consultarlo.

La revisión visual de la partida quedó detenida por el aviso de mundo experimental y la política de confirmación de la habilidad de escritorio. Essential requiere una sesión real con amigos, que no se simula ni se afirma haber realizado. Detalle de evidencia y límites en [entrega final](ENTREGA-FINAL.md).
