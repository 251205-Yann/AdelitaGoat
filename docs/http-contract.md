# Contrato HTTP de la API — AdelitaGOAT

Base URL local (desarrollo): `http://localhost:8080`  
Base URL desde emulador Android: `http://10.0.2.2:8080`
Base URL pública (Render): `https://adelitagoat-api.onrender.com`

## Convenciones Generales

- **Formato:** `application/json; charset=utf-8`
- **Dinero:** Todos los montos se expresan en **centavos enteros (`priceCents`, `subtotalCents`, `totalCents`)** y adicionalmente como cadena formateada en MXN (`formattedPrice`, `formattedTotal`, p. ej. `"$65.00 MXN"`), evitando tipos `float`/`double`.
- **Fechas:** Formato ISO-8601 con zona horaria explícita (`OffsetDateTime`, ej. `"2026-11-07T13:30:00-06:00"`).
- **Autenticación:** Header `Authorization: Bearer <token>`. El token es un identificador aleatorio criptográfico emitido al iniciar sesión o registrarse, persistido en la tabla `sessions` con fecha de expiración (`expiresAt`).
- **Errores Consistentes:** Todas las respuestas de error devuelven un objeto JSON con la estructura:
  ```json
  {
    "code": "ERROR_CODE",
    "message": "Descripción legible para el usuario",
    "details": ["Detalle opcional 1"]
  }
  ```

### Códigos de Estado HTTP Principales
- `200 OK` / `201 Created`: Operación exitosa.
- `400 Bad Request`: Entrada inválida, cantidades menores o iguales a 0, productos de múltiples puestos o producto agotado.
- `401 Unauthorized`: Token ausente, inválido o expirado (`UNAUTHORIZED`).
- `403 Forbidden`: El usuario no tiene el rol requerido (`CLIENTE` vs `VENDEDOR`) o intenta operar sobre puestos/productos/pedidos de otro usuario (`FORBIDDEN`).
- `404 Not Found`: Recurso no encontrado (`NOT_FOUND`).
- `409 Conflict`: Correo ya registrado, transición de estado ilegal/concurrente, o pedido ya calificado (`CONFLICT`).

---

## 1. Salud del Servidor

### `GET /api/health`
- **Autenticación:** Pública
- **Respuesta `200 OK`:**
  ```json
  {
    "status": "UP",
    "service": "AdelitaGOAT API",
    "database": "CONNECTED",
    "databaseMode": "POSTGRESQL_DOCKER_OR_LOCAL",
    "timestamp": "2026-11-07T13:30:00-06:00"
  }
  ```

---

## 2. Autenticación y Sesión

### `POST /api/auth/register`
- **Autenticación:** Pública
- **Request:**
  ```json
  {
    "name": "Cliente Ejemplo",
    "email": "cliente.nuevo@example.invalid",
    "password": "Password123",
    "phone": "9610001122",
    "role": "CLIENTE"
  }
  ```
- **Respuesta `201 Created`:**
  ```json
  {
    "token": "fe89a1...",
    "expiresAt": "2026-11-14T13:30:00-06:00",
    "user": {
      "id": 1,
      "name": "Cliente Ejemplo",
      "email": "cliente.nuevo@example.invalid",
      "phone": "9610001122",
      "role": "CLIENTE"
    }
  }
  ```
- **Nota:** Registrar un usuario con `"role": "VENDEDOR"` crea únicamente la cuenta del vendedor; no genera puestos ficticios automáticamente. El vendedor da de alta su puesto real mediante `POST /api/vendor/stalls`.
- **Errores:** `400` (campos inválidos, contraseña < 6 caracteres, rol inválido), `409` (correo ya registrado).

### `POST /api/auth/login`
- **Autenticación:** Pública
- **Request:**
  ```json
  {
    "email": "cliente.nuevo@example.invalid",
    "password": "Password123"
  }
  ```
- **Respuesta `200 OK`:** Mismo cuerpo `AuthResponse` que `/api/auth/register`.
- **Errores:** `401` (credenciales inválidas).

### `GET /api/auth/me`
- **Autenticación:** Requerida (`CLIENTE` o `VENDEDOR`)
- **Respuesta `200 OK`:** Objeto `UserDto`.

### `POST /api/auth/logout`
- **Autenticación:** Requerida
- **Respuesta `200 OK`:** `{"message": "Sesión cerrada correctamente"}`

---

## 3. Catálogo de Puestos, Categorías y Productos (Cliente / Público)

### `GET /api/categories`
- **Autenticación:** Pública (o con token)
- **Respuesta `200 OK`:** Lista de categorías (`id`, `name`, `slug`, `displayOrder`).

### `GET /api/stalls?search=&category=`
- **Autenticación:** Opcional (si envía token de `CLIENTE`, calcula `isFavorite` para ese usuario)
- **Query params:**
  - `search` (opcional): filtra por nombre de puesto, descripción o nombre de producto en su menú.
  - `category` (opcional): filtra por categoría (ej. `Tacos`, `Tortas`, `Bebidas`, `Hamburguesas`, `Quesadillas`, `Snacks`).
- **Respuesta `200 OK`:**
  ```json
  [
    {
      "id": 1,
      "vendorId": 3,
      "vendorName": "Doña Adelita Gómez",
      "name": "AdelitaGOAT - Tacos y Tortas",
      "description": "Tortillas recién hechas, carne asada al carbón, tortas crujientes y aguas frescas frente a la UP Chiapas.",
      "campusLocation": "Frente a la Universidad Politécnica de Chiapas",
      "category": "Tacos",
      "prepTimeMinutes": 8,
      "isOpen": true,
      "imageKey": "tacos",
      "averageRating": 4.9,
      "ratingCount": 1,
      "isFavorite": true
    }
  ]
  ```

### `GET /api/stalls/{stallId}`
- **Autenticación:** Opcional (si envía token, incluye `isFavorite`)
- **Respuesta `200 OK`:** Detalle del puesto (`StallDetailDto`) incluyendo su lista completa de `products` (`id`, `stallId`, `categoryId`, `categoryName`, `name`, `description`, `priceCents`, `formattedPrice`, `available`, `imageKey`) y `recentRatings`.

### `GET /api/products/{productId}`
- **Autenticación:** Pública / Cliente
- **Respuesta `200 OK`:** `ProductDto` junto con información del puesto (`stallName`, `campusLocation`, `stallRating`).

---

## 4. Favoritos (Cliente)

### `GET /api/favorites`
- **Autenticación:** Requerida (`CLIENTE`)
- **Respuesta `200 OK`:** Lista de `StallSummaryDto` marcados como favoritos por el cliente.

### `POST /api/favorites/{stallId}`
- **Autenticación:** Requerida (`CLIENTE`)
- **Respuesta `200 OK`:** `{"stallId": 1, "isFavorite": true}`

### `DELETE /api/favorites/{stallId}`
- **Autenticación:** Requerida (`CLIENTE`)
- **Respuesta `200 OK`:** `{"stallId": 1, "isFavorite": false}`

---

## 5. Apartados / Pedidos y Calificaciones (Cliente)

### `POST /api/orders`
- **Autenticación:** Requerida (`CLIENTE`)
- **Headers:** `Idempotency-Key: <uuid>` (también aceptado en el cuerpo como `idempotencyKey`). Si se repite una petición con el mismo `idempotencyKey` para el mismo cliente, el servidor devuelve el pedido ya creado sin duplicarlo.
- **Request:**
  ```json
  {
    "stallId": 1,
    "idempotencyKey": "550e8400-e29b-41d4-a716-446655440000",
    "notes": "Con todo y salsa verde aparte",
    "items": [
      { "productId": 1, "quantity": 1 },
      { "productId": 5, "quantity": 1 }
    ]
  }
  ```
- **Reglas de validación en servidor:**
  1. Todos los `productId` deben existir y pertenecer al mismo `stallId` indicado.
  2. El puesto debe estar abierto (`isOpen = true`) y cada producto debe tener `available = true`.
  3. El servidor **ignora cualquier precio del cliente** y calcula `unitPriceCents`, `lineTotalCents`, `subtotalCents` y `totalCents` desde la base de datos dentro de una transacción.
  4. Almacena el precio unitario histórico (`unit_price_cents`) en `order_items`.
  5. Establece `paymentMethod = "EFECTIVO_AL_RECOGER"`, `paymentStatus = "PENDIENTE_EFECTIVO"` y `status = "RECIBIDO"`.
- **Respuesta `201 Created` (o `200 OK` si fue reintento idempotente):**
  ```json
  {
    "id": 10,
    "folio": "A-1042",
    "customerId": 1,
    "customerName": "Wilbert Yanndel",
    "stallId": 1,
    "stallName": "AdelitaGOAT Tacos & Tortas",
    "campusLocation": "Frente a la Universidad Politécnica de Chiapas",
    "status": "RECIBIDO",
    "paymentMethod": "EFECTIVO_AL_RECOGER",
    "paymentStatus": "PENDIENTE_EFECTIVO",
    "subtotalCents": 9000,
    "totalCents": 9000,
    "formattedTotal": "$90.00 MXN",
    "notes": "Con todo y salsa verde aparte",
    "createdAt": "2026-11-07T13:35:00-06:00",
    "updatedAt": "2026-11-07T13:35:00-06:00",
    "deliveredAt": null,
    "paidAt": null,
    "items": [
      {
        "id": 1,
        "productId": 1,
        "productName": "Orden Tacos de Carne Asada (4 pzas)",
        "quantity": 1,
        "unitPriceCents": 6500,
        "lineTotalCents": 6500,
        "formattedUnitPrice": "$65.00 MXN",
        "formattedLineTotal": "$65.00 MXN"
      },
      {
        "id": 2,
        "productId": 5,
        "productName": "Agua fresca de horchata (500 ml)",
        "quantity": 1,
        "unitPriceCents": 2500,
        "lineTotalCents": 2500,
        "formattedUnitPrice": "$25.00 MXN",
        "formattedLineTotal": "$25.00 MXN"
      }
    ],
    "rating": null
  }
  ```

### `GET /api/orders`
- **Autenticación:** Requerida (`CLIENTE`)
- **Respuesta `200 OK`:** Lista de `OrderDto` del cliente autenticado, ordenados del más reciente al más antiguo.

### `GET /api/orders/{orderId}`
- **Autenticación:** Requerida (`CLIENTE` propietario del pedido o `VENDEDOR` propietario del puesto)
- **Respuesta `200 OK`:** `OrderDto` completo.
- **Errores:** `403` si pertenece a otro cliente/vendedor, `404` si no existe.

### `POST /api/orders/{orderId}/rating`
- **Autenticación:** Requerida (`CLIENTE` propietario del pedido)
- **Request:**
  ```json
  {
    "overallScore": 5,
    "productQualityScore": 5,
    "waitTimeScore": 5,
    "hygieneScore": 5,
    "serviceAttentionScore": 5,
    "comment": "Excelente sabor y mi pedido ya estaba listo cuando llegué al mostrador."
  }
  ```
- **Reglas de validación en servidor:**
  1. El pedido debe pertenecer al cliente autenticado (`403` en caso contrario).
  2. El estado del pedido debe ser estrictamente `ENTREGADO` (`409` en caso contrario).
  3. El pedido no debe tener ya una calificación registrada (`409` si ya fue calificado).
  4. Todas las puntuaciones deben estar entre `1` y `5` (`400` en caso contrario).
- **Respuesta `201 Created`:** Objeto `RatingDto`.

---

## 6. Panel de Vendedor (Rutas Protegidas por Rol `VENDEDOR` y Propiedad)

### `GET /api/vendor/stalls`
- **Autenticación:** Requerida (`VENDEDOR`)
- **Respuesta `200 OK`:** Lista de puestos (`List<StallDetailDto>`) cuyo `vendorId` coincide con el vendedor autenticado, incluyendo los productos y calificaciones recientes de cada puesto. Si el vendedor es recién registrado, devuelve `[]`.

### `POST /api/vendor/stalls`
- **Autenticación:** Requerida (`VENDEDOR`)
- **Request:**
  ```json
  {
    "name": "Taquería El Rincón Universitario",
    "description": "Tacos recién hechos al comal y aguas naturales",
    "campusLocation": "Portón Poniente, acera frente a entrada peatonal",
    "category": "Tacos",
    "prepTimeMinutes": 12,
    "isOpen": true,
    "imageKey": "tacos"
  }
  ```
- **Validaciones en servidor:**
  - `name`: mínimo 2 caracteres.
  - `description`: mínimo 4 caracteres.
  - `campusLocation`: ubicación textual concreta, mínimo 4 caracteres.
  - `category`: debe coincidir con una categoría existente en `/api/categories` (por nombre o `slug`).
  - `prepTimeMinutes`: entero entre `1` y `120`.
  - `isOpen`: booleano (`true` = abierto, `false` = cerrado).
- **Respuesta `201 Created`:** Objeto `StallDetailDto` del puesto creado.
- **Errores:** `400 Bad Request` (datos inválidos o categoría inexistente), `401 Unauthorized`, `403 Forbidden` (si el usuario es `CLIENTE`).

### `PUT /api/vendor/stalls/{stallId}`
- **Autenticación:** Requerida (`VENDEDOR` propietario de `stallId`)
- **Request:** Mismo esquema `UpsertStallRequest` que `POST /api/vendor/stalls`.
- **Respuesta `200 OK`:** Objeto `StallDetailDto` actualizado.
- **Errores:** `400 Bad Request` (datos inválidos), `403 Forbidden` (si `stallId` pertenece a otro vendedor o el usuario no es `VENDEDOR`), `404 Not Found` (si `stallId` no existe).

### `POST /api/vendor/stalls/{stallId}/products`
- **Autenticación:** Requerida (`VENDEDOR` dueño de `stallId`)
- **Request:**
  ```json
  {
    "categoryId": 1,
    "name": "Torta Especial de Pastor",
    "description": "Telera artesanal con carne al pastor, piña, quesillo y aguacate",
    "priceCents": 6500,
    "available": true,
    "imageKey": "torta"
  }
  ```
- **Respuesta `201 Created`:** `ProductDto`.
- **Errores:** `403` si `stallId` pertenece a otro vendedor.

### `PUT /api/vendor/products/{productId}`
- **Autenticación:** Requerida (`VENDEDOR` dueño del puesto al que pertenece `productId`)
- **Request:** Mismo esquema que creación de producto.
- **Respuesta `200 OK`:** `ProductDto` actualizado.
- **Errores:** `403` si pertenece a otro vendedor, `404` si no existe.

### `PATCH /api/vendor/products/{productId}/availability`
- **Autenticación:** Requerida (`VENDEDOR` dueño del puesto)
- **Request:**
  ```json
  {
    "available": false
  }
  ```
- **Respuesta `200 OK`:** `ProductDto` actualizado.
- **Errores:** `403` si pertenece a otro vendedor.

### `GET /api/vendor/orders?stallId=&status=`
- **Autenticación:** Requerida (`VENDEDOR`)
- **Respuesta `200 OK`:** Lista de `OrderDto` pertenecientes exclusivamente a los puestos del vendedor autenticado. Si se especifica `stallId` de otro vendedor, devuelve `403 Forbidden`.

### `PATCH /api/vendor/orders/{orderId}/status`
- **Autenticación:** Requerida (`VENDEDOR` dueño del puesto del pedido)
- **Request:**
  ```json
  {
    "status": "EN_PREPARACION",
    "expectedCurrentStatus": "RECIBIDO"
  }
  ```
- **Transiciones válidas en servidor:**
  - `RECIBIDO` → `EN_PREPARACION` o `CANCELADO`
  - `EN_PREPARACION` → `LISTO` o `CANCELADO`
  - `LISTO` → `ENTREGADO` o `CANCELADO`
  - `ENTREGADO` → *(estado final, no admite más cambios ni cancelación)*
  - `CANCELADO` → *(estado final, no admite más cambios)*
- **Regla de cobro presencial:**
  - Al transicionar a `ENTREGADO`, la transacción actualiza automáticamente `payment_status = 'COBRADO_EN_EFECTIVO'`, `delivered_at = NOW()` y `paid_at = NOW()`.
  - Al transicionar a `CANCELADO`, actualiza `payment_status = 'CANCELADO'`.
- **Concurrencia:** Si se envía `expectedCurrentStatus` y el estado actual en base de datos ya cambió, o si la transición no es válida desde el estado actual, devuelve `409 Conflict`.
- **Respuesta `200 OK`:** `OrderDto` actualizado.
