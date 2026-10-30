# Renombre a Backrooms — 1.0.1

Nombre correcto del mod: **Backrooms**. Nuevo repositorio: <https://github.com/maniadiaz/backrooms>.

El destino estaba vacío. Se trasladan la rama `main`, todos sus commits y todas las etiquetas con sus objetos originales, sin reescribir ni perder el historial de las etapas. El repositorio anterior queda como remoto `bedroom-legacy`; `origin` apunta al nuevo. Los documentos y etiquetas antiguos conservan su carácter histórico.

Cambian el nombre del listado de mods, HUD, anuncios, categoría de controles, documentación vigente, artefactos y paquetes. Se añade `/backrooms` como alias completo y principal en las instrucciones. `/bedroom` sigue disponible, incluidos los comandos de administración cuando están habilitados.

Se mantienen `modId=bedroom`, nombres de registros y dimensiones `bedroom:*`, paquetes Java, claves NBT, datos guardados y configuraciones `bedroom-common.toml`/`bedroom-client.toml`. Cambiarlos sin una migración de registros haría que Minecraft no reconociese contenido de mundos anteriores; no es necesario para que el mod se presente como Backrooms.

La versión pasa a 1.0.1 y genera `backrooms-1.0.1.jar`, `backrooms-1.0.1-client.zip` y `backrooms-1.0.1-server.zip`. Hay que retirar el jar Bedroom anterior y dejar una sola copia del mod.

La validación añade una prueba de uso real de `/backrooms lobby` y `/backrooms status`, seguida de `/bedroom leave`, verificando la conservación del inventario externo y la compatibilidad del comando anterior. Se mantiene toda la suite y la comprobación de integridad de paquetes. La revisión visual y Essential pendientes de la entrega 1.0.0 no se dan por realizadas con este cambio de nombre.

Resultado: compilación correcta y 63 pruebas obligatorias aprobadas. Los paquetes vuelven a comprobar versión y nombre visible del jar, presencia del identificador heredado, CRC y hashes internos.
