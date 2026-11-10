# AdelitaGOAT — especificación de implementación

## Fuentes y alcance

Este archivo concreta el encargo de desarrollo. Los dos PDF y `Untitled.png` de la raíz, junto con el Figma enlazado en la conversación, son las fuentes funcionales y visuales. Si hay conflicto, registrar la decisión en `README.md`. El MVP es una aplicación Android para clientes y vendedores de puestos de comida cercanos a la universidad, con API Ktor y PostgreSQL. El pedido se aparta en la app y se paga **en efectivo al recoger**. No se procesan pagos digitales.

## Entregables verificables

1. `android/`: proyecto que Android Studio pueda abrir directamente, Kotlin, Jetpack Compose, navegación y ViewModels. No dejar pantallas sueltas sin navegación ni datos estáticos como sustituto de la API.
2. `server/`: aplicación Ktor con rutas, modelos, servicios, persistencia PostgreSQL, migraciones y pruebas.
3. `docker-compose.yml`: PostgreSQL local con volumen y configuración de desarrollo, sin secretos reales comprometidos.
4. `README.md`: requisitos, comandos exactos de arranque, configuración de Android Studio y emulador, URL desde el emulador (`10.0.2.2`), usuarios demo, pruebas, decisiones y funcionalidades pendientes.
5. Un contrato HTTP legible en `docs/` o en el README, con métodos, rutas, autenticación, request/response y errores principales.

## Recorridos que deben funcionar de punta a punta

### Cliente

- Una persona puede registrarse con correo y contraseña e iniciar/cerrar sesión. La sesión se conserva al reiniciar la app. La app distingue el rol cliente del vendedor. Contraseñas nunca en texto plano ni logs.
- Puede ver puestos disponibles, buscar por nombre y filtrar por categoría; abrir un puesto y ver menú, precio, descripción y disponibilidad. Estados de carga, error y lista vacía son visibles.
- Puede añadir productos de un solo puesto al carrito, variar cantidades, quitar productos y ver subtotal/total. El servidor recalcula precios y rechaza artículos no disponibles; el cliente no define el total confiable.
- Confirma un apartado, recibe folio único y detalle persistido, con pago `EFECTIVO_AL_RECOGER`. Reintentar accidentalmente no debe crear duplicados si se usa una clave de idempotencia o una protección equivalente. El servidor almacena precio de cada producto en el momento de apartar.
- Consulta historial y detalle/estado del pedido. Estados mínimos: `RECIBIDO`, `EN_PREPARACION`, `LISTO`, `ENTREGADO`, `CANCELADO`. Muestra la progresión en la interfaz. Puede marcar/desmarcar favoritos y verlos tras reiniciar.
- Tras `ENTREGADO`, puede registrar **una** calificación por pedido: valoración general y criterios de calidad de productos, espera, higiene y atención, más comentario opcional. El servidor rechaza calificar pedidos ajenos, no entregados o ya calificados.

### Vendedor

- Un vendedor autenticado ve sólo sus puestos y pedidos. Puede crear/editar productos y cambiar disponibilidad. No puede modificar datos de otro vendedor.
- Ve pedidos entrantes con folio, hora, productos y total. Puede avanzar por transiciones válidas `RECIBIDO → EN_PREPARACION → LISTO → ENTREGADO`; puede cancelar mientras aún no está entregado. La transición se valida en servidor, incluyendo concurrencia básica.
- Al entregar, el pago presencial queda registrado como cobrado de forma explícita; nunca mostrar un cobro digital inexistente.

## API y datos

- Modelar usuarios, puestos, categorías, productos, favoritos, pedidos, líneas de pedido y calificaciones con claves foráneas e índices para consultas habituales.
- Tokens de sesión con expiración y autorización por rol/propietario; las rutas protegidas devuelven `401` o `403` correctamente. Validar entradas y responder errores JSON consistentes. Las operaciones de pedido y cambio de estado usan transacciones.
- Guardar dinero en centavos enteros o `NUMERIC`, nunca `float`/`double` para cálculos. Fechas con zona horaria clara. El folio es único. No poner credenciales reales en el repositorio.
- Datos demo: al menos un vendedor, dos puestos, varios productos y un cliente; semillas repetibles sin duplicados. Explicar credenciales demo y uso exclusivamente local.
- Probar con casos significativos: registro/login, autorización entre vendedores, total recalculado en servidor, producto no disponible, transición ilegal, calificación limitada a pedido entregado y prevención de duplicados.

## Android y diseño

- Reproducir el lenguaje visual de Figma: azul `#2F3C7E`, coral `#F96167`, dorado `#F9E795`, tarjetas redondeadas y navegación inferior para cliente. Mantener accesibilidad básica: contraste, etiquetas y blancos de toque razonables.
- Pantallas mínimas conectadas: bienvenida; login/registro y selección de rol; listado/búsqueda; detalle de puesto y producto; carrito; confirmación y ticket; historial/estado; favoritos; calificación; panel vendedor, pedidos y edición/disponibilidad del menú.
- Mostrar estado real de la API, no pantallas que sólo se vean correctas. Manejar fallos de red, formularios inválidos y reintento. Mantener separación sencilla entre UI, dominio y datos, acorde con `movilesFront`, sin abstraer por abstracción.

## Secuencia de trabajo y evidencia

1. Crear una matriz breve `docs/requirements.md` que mapee cada pantalla/función de las fuentes al entregable. Registrar ambigüedades, especialmente pago digital vs. efectivo, mapas de cercanía y notificaciones.
2. Construir backend, migraciones, datos demo y pruebas; ejecutar pruebas y corregir fallos.
3. Crear app Android Studio y conectar todos los recorridos a la API; compilar con Gradle y corregir fallos. Si hay emulador disponible, ejecutar el recorrido cliente y vendedor; si no, declarar esa limitación exacta.
4. Revisar integración, estados vacíos y errores, ajustar diseño respecto a Figma, y documentar arranque real.
5. Reportar comandos y resultados reales. No afirmar que algo funciona si no se ejecutó. No terminar con un scaffold, mockup o plan. No hacer push, publicar ni borrar originales.

## Fuera del MVP

Google/Apple login, push y pago digital requieren configuración/credenciales externas. Documentarlos como extensiones pendientes, sin botones que prometan una funcionalidad inexistente. La lista de puestos puede usar una ubicación/campus demo fija; documentar esa elección, ya que no hay datos geográficos concretos ni permiso de ubicación definido en las fuentes.
