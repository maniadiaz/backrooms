# Etapa 3.1 — HUD, comida y reinicio

Versión `0.3.1` para Minecraft Java 1.20.1 y Forge 47.4.28.

## Cambios pedidos

- Panel persistente en el lado izquierdo, centrado verticalmente, con fondo oscuro y acento amarillo. `BEDROOM: ACTIVE` (o el estado correspondiente) aparece encima de `MUERTES n / 3`. Los tres indicadores cambian de color según las muertes.
- El mismo panel muestra el reloj del limbo o descanso, nivel/grupo y estado de espera. Se retira el texto periódico sobre la barra rápida. Se respeta F1 para ocultar la interfaz.
- Paquete Forge de servidor a cliente con estado individual, muertes y segundos restantes. Actualización inmediata en cambios de estado y cada segundo durante la partida; no depende de variables del anfitrión local.
- El panel desaparece al abandonar Bedroom o desconectarse. Los clientes deben actualizarse a esta versión por el nuevo canal de red.
- Anuncios centrales con título, subtítulo y color según evento: lobby, inicio, limbo, regreso, espectador, descanso, nivel completado, derrota y reinicio. Usan los paquetes de títulos vanilla: aparición de 12 ticks, permanencia de 50 y desvanecimiento de 22. No se repiten cada segundo.
- Objeto `bedroom:nutrition_bar`, con icono original de envoltorio. Al comer restaura ocho puntos de hambre (cuatro muslos), sin superar 20, y da Regeneración I durante 100 ticks. Cada consumo gasta una barra.
- El botón de inicio entrega tres barras por participante al comenzar una partida nueva. No se vuelven a entregar al reconectar ni al regresar del limbo. Los objetos de la partida se eliminan al reiniciar, y el siguiente inicio entrega la ración nueva.
- `/bedroom restart` vuelve al lobby y limpia muertes, relojes, nivel y objetos de partida. Conserva los inventarios externos. También funciona durante limbo, espectador y descanso, y elimina la espera de reconexión de una partida guardada.
- El reinicio admite invitados en espera cuando haya espacio, hasta cuatro participantes. No empieza una nueva partida ni admite jugadores directamente al nivel activo; después hay que pulsar el botón del lobby.
- El comando está autorizado para el anfitrión de partida, el dueño del mundo integrado (incluido compartirlo con Essential), un operador o la consola. Los invitados normales no pueden reiniciar. No depende de habilitar comandos de desarrollo.

## Essential

No se agrega Essential como dependencia de compilación ni se incluye su jar. Bedroom utiliza el servidor integrado y la red de Forge. Según la [guía oficial de Essential](https://essential.gg/wiki/start-hosting), los jugadores de un mundo modificado deben tener los mismos mods y versiones.

Instalar Bedroom 0.3.1 y una versión de Essential para Forge 1.20.1 en los clientes, manteniendo las versiones del grupo alineadas. Quien entre después del inicio esperará en el lobby. El dueño o anfitrión puede usar `restart` para reunir al grupo antes de volver a comenzar. El límite de Bedroom sigue siendo cuatro jugadores activos, aunque haya más personas en el mundo compartido.

No se ha probado una conexión real mediante Essential; esto describe la arquitectura y el procedimiento previsto. No se garantiza compatibilidad con todas sus versiones o con otros mods de interfaz.

## Verificación ejecutada

`scripts/gradle.ps1 build runGameTestServer` terminó con compilación/reobfuscación correctas y diecisiete pruebas requeridas aprobadas.

Las tres pruebas nuevas comprueban:

1. Reinicio con anfitrión, compañero y llegada tardía: permisos del comando, regreso al lobby, incorporación del invitado, limpieza de inventario de partida y devolución exacta de objetos externos.
2. Consumo real del objeto: recuperación de ocho puntos de hambre, gasto de una unidad, efecto de regeneración y límite de hambre. La protección de suelo y paredes permite el uso normal de comida al mirarlos, sin permitir activar esos bloques.
3. Codificación/decodificación de paquetes del HUD, datos individuales de limbo/muertes, estado de espera y ocultación fuera del modo de juego.

El servidor GameTest cargó el mod sin clases exclusivas de cliente. El cliente de desarrollo inició, cargó los recursos y conectó a un mundo integrado de prueba, con entrada al lobby sin errores del nuevo canal en los registros.

Se intentó revisar la interfaz mediante control de la ventana, pero la aprobación de acceso agotó su tiempo. No se afirma que el panel o las animaciones hayan sido inspeccionados visualmente. Quedan pendientes la revisión a distintas escalas de GUI, la comida durante una partida y una sesión multicliente con Essential.

El entorno visual de prueba se mantiene separado en `run-hud/`, ignorado por Git. Se puede iniciar con `scripts/gradle.ps1 runClient -PqaWorld=BedroomHUD` si existe un mundo de prueba con ese nombre en `run-hud/saves`.

## Alcance

Este cambio adelanta la barra nutritiva para que la comida tenga una vía de recuperación en el prototipo. No completa la etapa 4: faltan cordura, agua de almendras, otros consumibles, linterna y contenedores con loot. La ración inicial es limitada y aún no hay reposición mediante contenedores.

No cambia el generador de 0.3.0: los mundos creados con esa versión pueden seguir usándose. Un mundo anterior que todavía conserve el generador plano requiere uno nuevo.
