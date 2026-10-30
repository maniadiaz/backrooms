# Instalar Backrooms 1.4.0

Minecraft Java 1.20.1 · Forge 47.4.28 · Java 17 de 64 bits · 1–4 participantes.

Actualización desde Bedroom: retirar el jar antiguo y dejar solo `backrooms-1.4.0.jar`; no instalar ambos a la vez. El identificador interno continúa siendo `bedroom`, por lo que se conservan bloques, objetos, dimensiones y datos de los mundos. Los archivos `bedroom-common.toml` y `bedroom-client.toml` conservan sus nombres. Los comandos `/backrooms` y `/bedroom` apuntan a las mismas funciones.

## Cliente y Essential

1. Crear un perfil/directorio de juego separado para el modpack. Instalar Forge **47.4.28** para **1.20.1** desde [la página oficial](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html). Usar Java 17.
2. Extraer `backrooms-1.4.0-client.zip` dentro de ese directorio: deben quedar `mods/backrooms-1.4.0.jar` y los archivos de `config/`. Es un paquete de directorio para instalación manual, no un archivo de importación de CurseForge/Modrinth.
3. Si van a invitarse con Essential, descargar por separado su versión para **Forge 1.20.1** desde [Essential](https://essential.gg/download). Instalar las mismas versiones de mods en todos los clientes. No mezclar Forge/Fabric ni dejar dos versiones de Backrooms dentro de `mods/`.
4. Iniciar el perfil, crear o abrir un mundo y aparecer en el lobby. El anfitrión comparte ese mundo e invita a sus amigos siguiendo la [guía oficial de Essential](https://essential.gg/en/wiki/start-hosting). No hace falta un servidor dedicado para este método.

Los ZIP no incluyen Minecraft, Forge, Java ni Essential. Para actualizar un mundo anterior, hacer una copia de seguridad, reemplazar el jar antiguo y reiniciar. La dimensión de Zona de Carga se carga desde el datapack del mod. Los mundos que todavía usaban el generador plano de la etapa 2 requieren un mundo nuevo; el inicio detecta ese caso.

Las configuraciones incluidas desactivan comandos de desarrollo y parpadeo rápido, mantienen el No-Clip normal, cordura, apariciones y dificultad de entidades. Si se copian sobre una instalación existente sustituyen sus opciones: usar un perfil nuevo o conservar la configuración propia.

## Servidor dedicado opcional

1. En una carpeta vacía, instalar el servidor oficial de Forge 1.20.1-47.4.28 con Java 17. El instalador genera `run.bat`/`run.sh` y sus bibliotecas.
2. Extraer `backrooms-1.4.0-server.zip` en esa carpeta. Contiene el mod, configuración común y `server.properties.example`, sin mundos personales ni instaladores.
3. Iniciar con el script generado por Forge, leer y aceptar personalmente el EULA de Minecraft si se está de acuerdo, y volver a iniciar. Ajustar memoria desde `user_jvm_args.txt`; el ejemplo de configuración propone cuatro conexiones, dificultad normal y distancias moderadas.
4. Todos los clientes deben tener Minecraft/Forge/Backrooms en las mismas versiones. Essential es la alternativa para compartir un mundo integrado, no un requisito del servidor dedicado.

## Integridad y solución de problemas

`SHA256SUMS.txt` contiene las sumas de los ZIP y del jar. Cada ZIP incluye `backrooms-manifest.json` con versiones y hashes internos. En PowerShell se puede comparar con `Get-FileHash -Algorithm SHA256`.

Si el HUD o la conexión fallan tras actualizar, revisar que todos retiraron jars antiguos y usan la misma versión; el protocolo de Backrooms es 3 desde 0.7.0. Si el mundo no encuentra una dimensión, reiniciar tras actualizar y revisar el log; no eliminar mundos para resolverlo sin una copia de seguridad.

La campaña y cuatro participantes simulados están probados automáticamente. La sesión con clientes reales y el transporte de Essential sigue siendo una validación manual pendiente; no se presenta como compatibilidad comprobada.

Actualización desde 1.3.0: comenzar una partida nueva desde el lobby para generar la estructura reforzada y crear el primer checkpoint manual. En partidas ya guardadas con 1.4.0 se puede continuar usando el botón azul. Los fusibles antiguos del Nivel 0 quedan retirados de la progresión. No borrar mundos; los registros antiguos siguen presentes para compatibilidad. Todos deben actualizar porque el protocolo cambia a 8.

Guardado 1.4.0: el anfitrión debe usar el terminal GUARDAR, Esc → Checkpoint del mundo → Guardar checkpoint o /backrooms save antes de cerrar. En el lobby, botón azul o /backrooms continue carga el último guardado del mundo. El verde o /backrooms start empieza desde cero. Los checkpoints manuales se introducen en esta versión; no se puede recuperar retrospectivamente una partida anterior que nunca se guardó.
