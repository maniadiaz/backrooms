# Etapa 8 — Zona de Carga y progresión

La ampliación autorizada completa una campaña inicial de dos niveles, sin inventar niveles adicionales indefinidos. Versión 0.8.0: el Nivel 0 conduce al descanso de 90 segundos y después a la dimensión `bedroom:level_1`.

Zona de Carga tiene generación procedural propia, hormigón seco/húmedo, acero corrugado, almacenes, cajas apiladas, techo a Y72 y lámparas suspendidas a Y68. Los corredores conectados se mantienen libres para jugadores y entidades. Cada región de 128 bloques tiene una compuerta anómala accesible.

Se recuperan tres relés de carga numerados y se insertan en sus controles. Se conservan la recuperación de componentes perdidos, el progreso compartido y el No-Clip de dos segundos. Los fusibles del Nivel 0 no sirven en el Nivel 1. Los objetos de misión obsoletos se retiran al restaurar inventarios; consumibles y linterna continúan entre niveles.

Todos los miembros del equipo, incluidos quienes estaban eliminados o en limbo, vuelven con muertes a cero y cordura completa al descanso. El anfitrión puede omitirlo con el botón. Espectadores en espera pueden seguir observando entre dimensiones sin participar. Morir, cordura, loot, lámparas, brújula, linterna, protección de bloques y encuentros funcionan en ambos niveles.

Las pruebas nuevas comprueban la dimensión/generador, conexión de corredores y objetivos, cuatro participantes simulados reviviendo y entrando realmente al Nivel 1, y la misión de relés rechazando fusibles antiguos. Esto no representa cuatro clientes ni una sesión Essential. La etapa siguiente añade el lobby de victoria y el cierre de campaña.

Resultado: compilación correcta y 59 pruebas obligatorias aprobadas. La prueba de conectividad detectó un encierro de cajas alrededor de la compuerta; se dejó libre su sala de aproximación y se verificaron de nuevo todas las rutas.

Recursos originales: `scripts/generate_warehouse.py` y `scripts/generate_mission.py`. No cambia la geometría generada previamente del Nivel 0. Se requiere actualizar el mod en todos los equipos y reiniciar el mundo/servidor para cargar la nueva dimensión.
