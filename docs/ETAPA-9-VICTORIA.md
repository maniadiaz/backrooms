# Etapa 9 — Final de campaña

Versión 0.9.0: la campaña inicial completa es Nivel 0 → descanso → Zona de Carga → lobby de victoria. Todos los participantes del intento, incluidos quienes quedaron eliminados, llegan al atrio de festejo.

El atrio tiene suelo de cuarzo, borde dorado, paredes claras/cian, techo azul iluminado y altura suficiente para cohetes reales. Se lanzan fuegos artificiales a intervalos limitados durante veinte segundos. El equipo está protegido del daño y no puede romper/colocar bloques. Un anuncio central distingue victoria de derrota. El anfitrión puede usar el botón para volver antes al lobby; el temporizador también devuelve al equipo automáticamente.

No hay un tercer nivel accidental: el comando de transición de desarrollo también respeta el límite de dos niveles. Cerrar el mundo o reiniciar continúa con la regla de empezar desde cero. Los inventarios externos siguen protegidos y `/bedroom leave` los restaura.

Se añade una prueba de campaña completa con cuatro participantes simulados: recoge e instala los componentes en ambos niveles, mantiene contacto en las dos salidas generadas, verifica cuarenta ticks de carga y veinte de transición, revive al equipo durante descanso, atraviesa dimensiones y llega a victoria. Se comprueba que la carga no se reutilice entre niveles. Otra prueba verifica el atrio y la creación de cohetes reales. No son cuatro clientes ni una sesión Essential.

La etapa final audita reglas, corrige incidencias y prepara paquetes de cliente/servidor, instrucciones y controles de integridad.

Resultado: compilación correcta y 61 pruebas obligatorias aprobadas, incluida la campaña completa. Se corrigió además una prueba anterior de contacto para que su área de ensayo se despeje explícitamente y no dependa de la semilla aleatoria del laberinto.
