package mx.edu.upchiapas.adelitagoat

import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import mx.edu.upchiapas.adelitagoat.db.DatabaseConnectionInfo
import mx.edu.upchiapas.adelitagoat.db.DatabaseFactory
import mx.edu.upchiapas.adelitagoat.db.SeedData
import mx.edu.upchiapas.adelitagoat.models.*
import java.util.UUID
import kotlin.test.*

class AdelitaServerTest {

    private fun createIsolatedTestDb(seedDemoData: Boolean = true): DatabaseConnectionInfo {
        val dbName = "adelita_test_${UUID.randomUUID().toString().replace("-", "")}"
        val jdbcUrl = "jdbc:h2:mem:$dbName;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1"
        return DatabaseFactory.init(
            jdbcUrl = jdbcUrl,
            user = "sa",
            password = "",
            driver = "org.h2.Driver",
            allowEmbeddedFallback = true,
            seedDemoData = seedDemoData
        )
    }

    @Test
    fun `fresh database starts without demo users stalls products or orders but includes reference categories`() = testApplication {
        val dbName = "adelita_empty_${UUID.randomUUID().toString().replace("-", "")}"
        val jdbcUrl = "jdbc:h2:mem:$dbName;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1"
        // No se pasa seedDemoData para verificar el valor por defecto (seedDemoData = false)
        val dbInfo = DatabaseFactory.init(
            jdbcUrl = jdbcUrl,
            user = "sa",
            password = "",
            driver = "org.h2.Driver",
            allowEmbeddedFallback = true
        )
        application { module(dbInfo) }
        val client = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        // 1. Las categorías de referencia existen en base nueva sin semillas demo
        val categoriesRes = client.get("/api/categories")
        assertEquals(HttpStatusCode.OK, categoriesRes.status)
        val categories = categoriesRes.body<List<CategoryDto>>()
        assertEquals(6, categories.size, "Deben existir las 6 categorías de referencia en una base nueva")
        assertTrue(categories.any { it.slug == "tacos" })
        assertTrue(categories.any { it.slug == "bebidas" })

        // 2. El catálogo de puestos inicia vacío
        val stallsRes = client.get("/api/stalls")
        assertEquals(HttpStatusCode.OK, stallsRes.status)
        val stalls = stallsRes.body<List<StallSummaryDto>>()
        assertTrue(stalls.isEmpty(), "Una base nueva sin semillas demo no debe tener puestos ficticios")

        // 3. No existen cuentas demo cargadas por defecto
        val demoLoginRes = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("cliente1.demo@example.invalid", "Cliente123!"))
        }
        assertEquals(HttpStatusCode.Unauthorized, demoLoginRes.status)

        // 4. Verificación directa en tablas SQL: 0 usuarios, 0 puestos, 0 productos, 0 pedidos
        dbInfo.dataSource.connection.use { conn ->
            for (table in listOf("users", "stalls", "products", "orders")) {
                conn.prepareStatement("SELECT COUNT(*) FROM $table").use { ps ->
                    ps.executeQuery().use { rs ->
                        assertTrue(rs.next())
                        assertEquals(0L, rs.getLong(1), "La tabla '$table' debe iniciar con 0 registros cuando seedDemoData=false")
                    }
                }
            }
        }
    }

    @Test
    fun `health endpoint and idempotent seed data do not create duplicates`() = testApplication {
        val dbInfo = createIsolatedTestDb()
        application { module(dbInfo) }
        val client = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        // Ejecutar semilla por segunda y tercera vez para comprobar idempotencia
        SeedData.seedIfNeeded(dbInfo.dataSource)
        SeedData.seedIfNeeded(dbInfo.dataSource)

        val healthRes = client.get("/api/health")
        assertEquals(HttpStatusCode.OK, healthRes.status)
        val health = healthRes.body<HealthResponse>()
        assertEquals("UP", health.status)

        val stallsRes = client.get("/api/stalls")
        assertEquals(HttpStatusCode.OK, stallsRes.status)
        val stalls = stallsRes.body<List<StallSummaryDto>>()
        assertEquals(3, stalls.size, "La semilla repetible debe mantener exactamente los 3 puestos demo sin duplicados")
    }

    @Test
    fun `register login me and logout enforce authentication and reject duplicate email`() = testApplication {
        val dbInfo = createIsolatedTestDb()
        application { module(dbInfo) }
        val client = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        val registerReq = RegisterRequest(
            name = "Carlos Estudiante",
            email = "carlos.nuevo@example.invalid",
            password = "PasswordSegura123",
            phone = "9610001122",
            role = UserRole.CLIENTE
        )

        val regRes = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(registerReq)
        }
        assertEquals(HttpStatusCode.Created, regRes.status)
        val auth = regRes.body<AuthResponse>()
        assertTrue(auth.token.isNotBlank())
        assertEquals(UserRole.CLIENTE, auth.user.role)

        // Registro duplicado debe devolver 409 Conflict
        val dupRes = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(registerReq)
        }
        assertEquals(HttpStatusCode.Conflict, dupRes.status)

        // Login con contraseña incorrecta -> 401
        val badLogin = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email = registerReq.email, password = "wrongpassword"))
        }
        assertEquals(HttpStatusCode.Unauthorized, badLogin.status)

        // Consultar perfil con token válido -> 200
        val meRes = client.get("/api/auth/me") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
        }
        assertEquals(HttpStatusCode.OK, meRes.status)
        assertEquals("carlos.nuevo@example.invalid", meRes.body<UserDto>().email)

        // Cerrar sesión -> invalida el token
        val logoutRes = client.post("/api/auth/logout") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
        }
        assertEquals(HttpStatusCode.OK, logoutRes.status)

        val meAfterLogout = client.get("/api/auth/me") {
            header(HttpHeaders.Authorization, "Bearer ${auth.token}")
        }
        assertEquals(HttpStatusCode.Unauthorized, meAfterLogout.status)
    }

    @Test
    fun `server recalculates total prevents idempotency duplicates and preserves historical product price`() = testApplication {
        val dbInfo = createIsolatedTestDb()
        application { module(dbInfo) }
        val client = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        val customerAuth = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("cliente1.demo@example.invalid", "Cliente123!"))
        }.body<AuthResponse>()

        val vendorAuth = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("vendedor1.demo@example.invalid", "Vendedor123!"))
        }.body<AuthResponse>()

        val stallDetail = client.get("/api/stalls/1").body<StallDetailDto>()
        val product1 = stallDetail.products.first { it.name.contains("Tacos de carne asada (4 piezas)") } // 6500 cents
        val product2 = stallDetail.products.first { it.name.contains("Agua fresca de horchata") } // 2500 cents

        val idemKey = "idem-${UUID.randomUUID()}"
        val orderReq = CreateOrderRequest(
            stallId = 1,
            idempotencyKey = idemKey,
            notes = "Bien dorados",
            items = listOf(
                CreateOrderItemRequest(productId = product1.id, quantity = 2), // 2 * 6500 = 13000
                CreateOrderItemRequest(productId = product2.id, quantity = 1)  // 1 * 2500 = 2500
            )
        )

        val firstRes = client.post("/api/orders") {
            header(HttpHeaders.Authorization, "Bearer ${customerAuth.token}")
            header("Idempotency-Key", idemKey)
            contentType(ContentType.Application.Json)
            setBody(orderReq)
        }
        assertEquals(HttpStatusCode.Created, firstRes.status)
        val createdOrder = firstRes.body<OrderDto>()
        assertEquals(15500L, createdOrder.subtotalCents)
        assertEquals(15500L, createdOrder.totalCents)
        assertEquals("$155.00 MXN", createdOrder.formattedTotal)
        assertEquals(PaymentMethod.EFECTIVO_AL_RECOGER, createdOrder.paymentMethod)
        assertEquals(PaymentStatus.PENDIENTE_EFECTIVO, createdOrder.paymentStatus)

        // Reintentar con la misma clave de idempotencia NO crea duplicado
        val retryRes = client.post("/api/orders") {
            header(HttpHeaders.Authorization, "Bearer ${customerAuth.token}")
            header("Idempotency-Key", idemKey)
            contentType(ContentType.Application.Json)
            setBody(orderReq)
        }
        assertEquals(HttpStatusCode.OK, retryRes.status)
        val retryOrder = retryRes.body<OrderDto>()
        assertEquals(createdOrder.id, retryOrder.id)
        assertEquals(createdOrder.folio, retryOrder.folio)

        // Si el vendedor sube el precio del producto después, el pedido conserva el precio histórico al apartar
        val updatePriceRes = client.put("/api/vendor/products/${product1.id}") {
            header(HttpHeaders.Authorization, "Bearer ${vendorAuth.token}")
            contentType(ContentType.Application.Json)
            setBody(
                UpsertProductRequest(
                    categoryId = product1.categoryId,
                    name = product1.name,
                    description = product1.description,
                    priceCents = 8500L,
                    available = true,
                    imageKey = product1.imageKey
                )
            )
        }
        assertEquals(HttpStatusCode.OK, updatePriceRes.status)

        val fetchedOrder = client.get("/api/orders/${createdOrder.id}") {
            header(HttpHeaders.Authorization, "Bearer ${customerAuth.token}")
        }.body<OrderDto>()
        val historicalItem = fetchedOrder.items.first { it.productId == product1.id }
        assertEquals(6500L, historicalItem.unitPriceCents, "El precio histórico del producto debe conservarse en 6500 centavos")
        assertEquals(15500L, fetchedOrder.totalCents)
    }

    @Test
    fun `rejects unavailable product and cross stall products in cart`() = testApplication {
        val dbInfo = createIsolatedTestDb()
        application { module(dbInfo) }
        val client = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        val customerAuth = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("cliente1.demo@example.invalid", "Cliente123!"))
        }.body<AuthResponse>()

        val vendorAuth = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("vendedor1.demo@example.invalid", "Vendedor123!"))
        }.body<AuthResponse>()

        val stall1Products = client.get("/api/stalls/1").body<StallDetailDto>().products
        val stall2Products = client.get("/api/stalls/2").body<StallDetailDto>().products
        val targetProduct = stall1Products.first()

        // Vendedor desactiva disponibilidad del producto
        val patchRes = client.patch("/api/vendor/products/${targetProduct.id}/availability") {
            header(HttpHeaders.Authorization, "Bearer ${vendorAuth.token}")
            contentType(ContentType.Application.Json)
            setBody(UpdateAvailabilityRequest(available = false))
        }
        assertEquals(HttpStatusCode.OK, patchRes.status)

        // Intentar pedir producto agotado -> 400 Bad Request
        val unavailableOrderRes = client.post("/api/orders") {
            header(HttpHeaders.Authorization, "Bearer ${customerAuth.token}")
            contentType(ContentType.Application.Json)
            setBody(
                CreateOrderRequest(
                    stallId = 1,
                    idempotencyKey = "idem-unavail-${UUID.randomUUID()}",
                    items = listOf(CreateOrderItemRequest(productId = targetProduct.id, quantity = 1))
                )
            )
        }
        assertEquals(HttpStatusCode.BadRequest, unavailableOrderRes.status)

        // Intentar mezclar productos de puesto 1 y puesto 2 -> 400 Bad Request
        val mixedStallRes = client.post("/api/orders") {
            header(HttpHeaders.Authorization, "Bearer ${customerAuth.token}")
            contentType(ContentType.Application.Json)
            setBody(
                CreateOrderRequest(
                    stallId = 1,
                    idempotencyKey = "idem-mixed-${UUID.randomUUID()}",
                    items = listOf(
                        CreateOrderItemRequest(productId = stall1Products[1].id, quantity = 1),
                        CreateOrderItemRequest(productId = stall2Products.first().id, quantity = 1)
                    )
                )
            )
        }
        assertEquals(HttpStatusCode.BadRequest, mixedStallRes.status)
    }

    @Test
    fun `vendor authorization prevents modifying another vendors stall products or orders`() = testApplication {
        val dbInfo = createIsolatedTestDb()
        application { module(dbInfo) }
        val client = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        val vendor2Auth = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("vendedor2.demo@example.invalid", "Vendedor123!"))
        }.body<AuthResponse>()

        val customerAuth = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("cliente1.demo@example.invalid", "Cliente123!"))
        }.body<AuthResponse>()

        // Cliente no puede acceder a rutas de vendedor -> 403
        val customerVendorTry = client.get("/api/vendor/stalls") {
            header(HttpHeaders.Authorization, "Bearer ${customerAuth.token}")
        }
        assertEquals(HttpStatusCode.Forbidden, customerVendorTry.status)

        // Vendedor 2 solo ve su propio puesto (id=3)
        val vendor2Stalls = client.get("/api/vendor/stalls") {
            header(HttpHeaders.Authorization, "Bearer ${vendor2Auth.token}")
        }.body<List<StallDetailDto>>()
        assertEquals(1, vendor2Stalls.size)
        assertEquals("Jugos y Antojitos Doña Meche", vendor2Stalls.first().stall.name)

        // Vendedor 2 intenta crear producto en el puesto 1 (de Vendedor 1) -> 403 Forbidden
        val createInOtherStall = client.post("/api/vendor/stalls/1/products") {
            header(HttpHeaders.Authorization, "Bearer ${vendor2Auth.token}")
            contentType(ContentType.Application.Json)
            setBody(
                UpsertProductRequest(
                    categoryId = 1,
                    name = "Producto Intruso",
                    description = "No debería permitirse",
                    priceCents = 5000L
                )
            )
        }
        assertEquals(HttpStatusCode.Forbidden, createInOtherStall.status)

        // Vendedor 2 intenta cambiar disponibilidad de producto del puesto 1 -> 403 Forbidden
        val stall1Product = client.get("/api/stalls/1").body<StallDetailDto>().products.first()
        val toggleOtherProduct = client.patch("/api/vendor/products/${stall1Product.id}/availability") {
            header(HttpHeaders.Authorization, "Bearer ${vendor2Auth.token}")
            contentType(ContentType.Application.Json)
            setBody(UpdateAvailabilityRequest(available = false))
        }
        assertEquals(HttpStatusCode.Forbidden, toggleOtherProduct.status)
    }

    @Test
    fun `order status transitions cash payment recording and rating rules work end to end`() = testApplication {
        val dbInfo = createIsolatedTestDb()
        application { module(dbInfo) }
        val client = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        val customer1Auth = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("cliente1.demo@example.invalid", "Cliente123!"))
        }.body<AuthResponse>()

        val customer2Auth = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("cliente2.demo@example.invalid", "Cliente123!"))
        }.body<AuthResponse>()

        val vendor1Auth = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("vendedor1.demo@example.invalid", "Vendedor123!"))
        }.body<AuthResponse>()

        val stall1Product = client.get("/api/stalls/1").body<StallDetailDto>().products.first()

        // 1. Cliente 1 crea un nuevo pedido
        val order = client.post("/api/orders") {
            header(HttpHeaders.Authorization, "Bearer ${customer1Auth.token}")
            contentType(ContentType.Application.Json)
            setBody(
                CreateOrderRequest(
                    stallId = 1,
                    idempotencyKey = "idem-flow-${UUID.randomUUID()}",
                    items = listOf(CreateOrderItemRequest(productId = stall1Product.id, quantity = 1))
                )
            )
        }.body<OrderDto>()
        assertEquals(OrderStatus.RECIBIDO, order.status)

        // 2. Intentar calificar antes de ENTREGADO -> 409 Conflict
        val prematureRating = client.post("/api/orders/${order.id}/rating") {
            header(HttpHeaders.Authorization, "Bearer ${customer1Auth.token}")
            contentType(ContentType.Application.Json)
            setBody(CreateRatingRequest(5, 5, 5, 5, 5, "Aún no entregado"))
        }
        assertEquals(HttpStatusCode.Conflict, prematureRating.status)

        // 3. Transición ilegal RECIBIDO -> ENTREGADO directo -> 409 Conflict
        val illegalJump = client.patch("/api/vendor/orders/${order.id}/status") {
            header(HttpHeaders.Authorization, "Bearer ${vendor1Auth.token}")
            contentType(ContentType.Application.Json)
            setBody(UpdateOrderStatusRequest(status = OrderStatus.ENTREGADO))
        }
        assertEquals(HttpStatusCode.Conflict, illegalJump.status)

        // 4. Avanzar RECIBIDO -> EN_PREPARACION -> LISTO -> ENTREGADO
        val inPrep = client.patch("/api/vendor/orders/${order.id}/status") {
            header(HttpHeaders.Authorization, "Bearer ${vendor1Auth.token}")
            contentType(ContentType.Application.Json)
            setBody(UpdateOrderStatusRequest(status = OrderStatus.EN_PREPARACION, expectedCurrentStatus = OrderStatus.RECIBIDO))
        }.body<OrderDto>()
        assertEquals(OrderStatus.EN_PREPARACION, inPrep.status)
        assertEquals(PaymentStatus.PENDIENTE_EFECTIVO, inPrep.paymentStatus)

        // Concurrencia: si se envía expectedCurrentStatus desactualizado (RECIBIDO), rechaza con 409
        val staleTransition = client.patch("/api/vendor/orders/${order.id}/status") {
            header(HttpHeaders.Authorization, "Bearer ${vendor1Auth.token}")
            contentType(ContentType.Application.Json)
            setBody(UpdateOrderStatusRequest(status = OrderStatus.LISTO, expectedCurrentStatus = OrderStatus.RECIBIDO))
        }
        assertEquals(HttpStatusCode.Conflict, staleTransition.status)

        val ready = client.patch("/api/vendor/orders/${order.id}/status") {
            header(HttpHeaders.Authorization, "Bearer ${vendor1Auth.token}")
            contentType(ContentType.Application.Json)
            setBody(UpdateOrderStatusRequest(status = OrderStatus.LISTO, expectedCurrentStatus = OrderStatus.EN_PREPARACION))
        }.body<OrderDto>()
        assertEquals(OrderStatus.LISTO, ready.status)

        val delivered = client.patch("/api/vendor/orders/${order.id}/status") {
            header(HttpHeaders.Authorization, "Bearer ${vendor1Auth.token}")
            contentType(ContentType.Application.Json)
            setBody(UpdateOrderStatusRequest(status = OrderStatus.ENTREGADO, expectedCurrentStatus = OrderStatus.LISTO))
        }.body<OrderDto>()
        assertEquals(OrderStatus.ENTREGADO, delivered.status)
        assertEquals(PaymentStatus.COBRADO_EN_EFECTIVO, delivered.paymentStatus, "Al entregar debe registrarse explícitamente el cobro en efectivo")
        assertNotNull(delivered.deliveredAt)
        assertNotNull(delivered.paidAt)

        // 5. Intentar cancelar un pedido ya entregado -> 409 Conflict
        val cancelAfterDelivery = client.patch("/api/vendor/orders/${order.id}/status") {
            header(HttpHeaders.Authorization, "Bearer ${vendor1Auth.token}")
            contentType(ContentType.Application.Json)
            setBody(UpdateOrderStatusRequest(status = OrderStatus.CANCELADO))
        }
        assertEquals(HttpStatusCode.Conflict, cancelAfterDelivery.status)

        // 6. Cliente 2 intenta calificar el pedido de Cliente 1 -> 403 Forbidden
        val foreignRating = client.post("/api/orders/${order.id}/rating") {
            header(HttpHeaders.Authorization, "Bearer ${customer2Auth.token}")
            contentType(ContentType.Application.Json)
            setBody(CreateRatingRequest(5, 5, 5, 5, 5, "Pedido ajeno"))
        }
        assertEquals(HttpStatusCode.Forbidden, foreignRating.status)

        // 7. Cliente 1 califica su pedido entregado -> 201 Created
        val validRatingRes = client.post("/api/orders/${order.id}/rating") {
            header(HttpHeaders.Authorization, "Bearer ${customer1Auth.token}")
            contentType(ContentType.Application.Json)
            setBody(CreateRatingRequest(5, 5, 4, 5, 5, "Muy rico todo y sin hacer fila"))
        }
        assertEquals(HttpStatusCode.Created, validRatingRes.status)

        // 8. Cliente 1 intenta volver a calificar el mismo pedido -> 409 Conflict
        val duplicateRatingRes = client.post("/api/orders/${order.id}/rating") {
            header(HttpHeaders.Authorization, "Bearer ${customer1Auth.token}")
            contentType(ContentType.Application.Json)
            setBody(CreateRatingRequest(4, 4, 4, 4, 4, "Segunda calificación"))
        }
        assertEquals(HttpStatusCode.Conflict, duplicateRatingRes.status)
    }

    @Test
    fun `customer can mark and unmark favorite stalls and persist them across queries`() = testApplication {
        val dbInfo = createIsolatedTestDb()
        application { module(dbInfo) }
        val client = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        val customerAuth = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("cliente2.demo@example.invalid", "Cliente123!"))
        }.body<AuthResponse>()

        val initialFavs = client.get("/api/favorites") {
            header(HttpHeaders.Authorization, "Bearer ${customerAuth.token}")
        }.body<List<StallSummaryDto>>()
        assertTrue(initialFavs.isEmpty())

        val addRes = client.post("/api/favorites/2") {
            header(HttpHeaders.Authorization, "Bearer ${customerAuth.token}")
        }.body<FavoriteToggleResponse>()
        assertTrue(addRes.isFavorite)

        val favsAfterAdd = client.get("/api/favorites") {
            header(HttpHeaders.Authorization, "Bearer ${customerAuth.token}")
        }.body<List<StallSummaryDto>>()
        assertEquals(1, favsAfterAdd.size)
        assertEquals(2L, favsAfterAdd.first().id)

        val delRes = client.delete("/api/favorites/2") {
            header(HttpHeaders.Authorization, "Bearer ${customerAuth.token}")
        }.body<FavoriteToggleResponse>()
        assertFalse(delRes.isFavorite)

        val favsAfterDel = client.get("/api/favorites") {
            header(HttpHeaders.Authorization, "Bearer ${customerAuth.token}")
        }.body<List<StallSummaryDto>>()
        assertTrue(favsAfterDel.isEmpty())
    }

    @Test
    fun `empty database allows new vendor to create and edit stall load products and serve customer catalog with strict ownership`() = testApplication {
        val dbInfo = createIsolatedTestDb(seedDemoData = false)
        application { module(dbInfo) }
        val client = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        // 1. Registrar vendedor nuevo y cliente nuevo en base sin semillas demo
        val vendor1Auth = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(
                RegisterRequest(
                    name = "Vendedor Puesto Real",
                    email = "vendedor.real@example.invalid",
                    password = "Password123!",
                    phone = "9611112233",
                    role = UserRole.VENDEDOR
                )
            )
        }.body<AuthResponse>()

        val customerAuth = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(
                RegisterRequest(
                    name = "Cliente Real",
                    email = "cliente.real@example.invalid",
                    password = "Password123!",
                    phone = "9614445566",
                    role = UserRole.CLIENTE
                )
            )
        }.body<AuthResponse>()

        // 2. El vendedor recién registrado inicia con 0 puestos y el catálogo público está vacío
        val initialVendorStalls = client.get("/api/vendor/stalls") {
            header(HttpHeaders.Authorization, "Bearer ${vendor1Auth.token}")
        }.body<List<StallDetailDto>>()
        assertTrue(initialVendorStalls.isEmpty(), "El vendedor recién registrado no debe tener puestos ficticios precreados")

        val initialPublicStalls = client.get("/api/stalls").body<List<StallSummaryDto>>()
        assertTrue(initialPublicStalls.isEmpty())

        // 3. Un cliente no puede crear puestos -> 403 Forbidden
        val customerCreateStallTry = client.post("/api/vendor/stalls") {
            header(HttpHeaders.Authorization, "Bearer ${customerAuth.token}")
            contentType(ContentType.Application.Json)
            setBody(
                UpsertStallRequest(
                    name = "Puesto No Autorizado",
                    description = "Intento desde cuenta cliente",
                    campusLocation = "Calle Principal #10",
                    category = "Tacos",
                    prepTimeMinutes = 10
                )
            )
        }
        assertEquals(HttpStatusCode.Forbidden, customerCreateStallTry.status)

        // 4. Validación de entradas inválidas al crear puesto -> 400 Bad Request
        val invalidStallRes = client.post("/api/vendor/stalls") {
            header(HttpHeaders.Authorization, "Bearer ${vendor1Auth.token}")
            contentType(ContentType.Application.Json)
            setBody(
                UpsertStallRequest(
                    name = "A",
                    description = " Corta ",
                    campusLocation = "12",
                    category = "Tacos",
                    prepTimeMinutes = 0
                )
            )
        }
        assertEquals(HttpStatusCode.BadRequest, invalidStallRes.status)

        // 5. El vendedor crea su puesto real -> 201 Created
        val createStallRes = client.post("/api/vendor/stalls") {
            header(HttpHeaders.Authorization, "Bearer ${vendor1Auth.token}")
            contentType(ContentType.Application.Json)
            setBody(
                UpsertStallRequest(
                    name = "Taquería El Rincón Universitario",
                    description = "Tacos recién hechos al comal y aguas naturales",
                    campusLocation = "Portón Poniente, acera frente a entrada peatonal",
                    category = "Tacos",
                    prepTimeMinutes = 12,
                    isOpen = true,
                    imageKey = "tacos"
                )
            )
        }
        assertEquals(HttpStatusCode.Created, createStallRes.status)
        val createdStallDetail = createStallRes.body<StallDetailDto>()
        val stallId = createdStallDetail.stall.id
        assertEquals("Taquería El Rincón Universitario", createdStallDetail.stall.name)
        assertEquals("Portón Poniente, acera frente a entrada peatonal", createdStallDetail.stall.campusLocation)
        assertEquals("Tacos", createdStallDetail.stall.category)
        assertEquals(12, createdStallDetail.stall.prepTimeMinutes)
        assertTrue(createdStallDetail.stall.isOpen)

        // 6. El vendedor carga un producto en su nuevo puesto
        val categories = client.get("/api/categories").body<List<CategoryDto>>()
        val tacosCategory = categories.first { it.slug == "tacos" }
        val createProductRes = client.post("/api/vendor/stalls/$stallId/products") {
            header(HttpHeaders.Authorization, "Bearer ${vendor1Auth.token}")
            contentType(ContentType.Application.Json)
            setBody(
                UpsertProductRequest(
                    categoryId = tacosCategory.id,
                    name = "Orden de tacos surtidos (4 piezas)",
                    description = "Con tortillas de maíz hechas a mano, cilantro y cebolla",
                    priceCents = 6000L,
                    available = true,
                    imageKey = "tacos"
                )
            )
        }
        assertEquals(HttpStatusCode.Created, createProductRes.status)
        val createdProduct = createProductRes.body<ProductDto>()

        // 7. El cliente consulta el catálogo y ve el puesto y su menú
        val catalogStalls = client.get("/api/stalls").body<List<StallSummaryDto>>()
        assertEquals(1, catalogStalls.size)
        assertEquals(stallId, catalogStalls.first().id)
        assertEquals("Taquería El Rincón Universitario", catalogStalls.first().name)

        val publicStallDetail = client.get("/api/stalls/$stallId").body<StallDetailDto>()
        assertEquals(1, publicStallDetail.products.size)
        assertEquals("Orden de tacos surtidos (4 piezas)", publicStallDetail.products.first().name)

        // 8. El vendedor edita su puesto (cambia nombre, ubicación, tiempo de espera y lo cierra temporalmente) -> 200 OK
        val updateStallRes = client.put("/api/vendor/stalls/$stallId") {
            header(HttpHeaders.Authorization, "Bearer ${vendor1Auth.token}")
            contentType(ContentType.Application.Json)
            setBody(
                UpsertStallRequest(
                    name = "Taquería y Tortas El Rincón",
                    description = "Tacos al comal, tortas calientes y aguas del día",
                    campusLocation = "Esquina Sur frente a parada de transporte",
                    category = "Tortas",
                    prepTimeMinutes = 15,
                    isOpen = false,
                    imageKey = "tortas"
                )
            )
        }
        assertEquals(HttpStatusCode.OK, updateStallRes.status)
        val updatedStall = updateStallRes.body<StallDetailDto>().stall
        assertEquals("Taquería y Tortas El Rincón", updatedStall.name)
        assertEquals("Esquina Sur frente a parada de transporte", updatedStall.campusLocation)
        assertEquals("Tortas", updatedStall.category)
        assertEquals(15, updatedStall.prepTimeMinutes)
        assertFalse(updatedStall.isOpen)

        // Si está cerrado, rechaza apartados nuevos -> 400 Bad Request
        val closedOrderTry = client.post("/api/orders") {
            header(HttpHeaders.Authorization, "Bearer ${customerAuth.token}")
            contentType(ContentType.Application.Json)
            setBody(
                CreateOrderRequest(
                    stallId = stallId,
                    idempotencyKey = "idem-closed-${UUID.randomUUID()}",
                    items = listOf(CreateOrderItemRequest(productId = createdProduct.id, quantity = 1))
                )
            )
        }
        assertEquals(HttpStatusCode.BadRequest, closedOrderTry.status)

        // 9.Otro vendedor intenta editar el puesto del Vendedor 1 -> 403 Forbidden
        val vendor2Auth = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(
                RegisterRequest(
                    name = "Segundo Vendedor",
                    email = "otro.vendedor@example.invalid",
                    password = "Password123!",
                    role = UserRole.VENDEDOR
                )
            )
        }.body<AuthResponse>()

        val forbiddenEditRes = client.put("/api/vendor/stalls/$stallId") {
            header(HttpHeaders.Authorization, "Bearer ${vendor2Auth.token}")
            contentType(ContentType.Application.Json)
            setBody(
                UpsertStallRequest(
                    name = "Intento de Secuestro de Puesto",
                    description = "No permitido",
                    campusLocation = "Ubicación intrusa",
                    category = "Tacos",
                    prepTimeMinutes = 10,
                    isOpen = true
                )
            )
        }
        assertEquals(HttpStatusCode.Forbidden, forbiddenEditRes.status)

        // 10. Editar un puesto inexistente -> 404 Not Found
        val notFoundEditRes = client.put("/api/vendor/stalls/999999") {
            header(HttpHeaders.Authorization, "Bearer ${vendor1Auth.token}")
            contentType(ContentType.Application.Json)
            setBody(
                UpsertStallRequest(
                    name = "Puesto Inexistente",
                    description = "No existe",
                    campusLocation = "Ubicación válida",
                    category = "Tacos",
                    prepTimeMinutes = 10,
                    isOpen = true
                )
            )
        }
        assertEquals(HttpStatusCode.NotFound, notFoundEditRes.status)
    }

    @Test
    fun `production supabase config normalizes sslmode masks secrets and fails fast when fallback is disabled`() {
        // 1. Normalización de URL para Supabase Session Pooler (IPv4, puerto 5432, sslmode=require)
        val rawSupabaseJdbc = "jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:5432/postgres"
        assertEquals(
            "jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:5432/postgres?sslmode=require",
            DatabaseFactory.normalizePostgresJdbcUrl(rawSupabaseJdbc)
        )

        val rawSupabaseUri = "postgresql://aws-0-us-west-1.pooler.supabase.com:5432/postgres"
        assertEquals(
            "jdbc:postgresql://aws-0-us-west-1.pooler.supabase.com:5432/postgres?sslmode=require",
            DatabaseFactory.normalizePostgresJdbcUrl(rawSupabaseUri)
        )

        val alreadyWithSsl = "jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:5432/postgres?sslmode=require"
        assertEquals(
            alreadyWithSsl,
            DatabaseFactory.normalizePostgresJdbcUrl(alreadyWithSsl)
        )

        val localUrl = "jdbc:postgresql://localhost:5432/adelitagoat"
        assertEquals(
            localUrl,
            DatabaseFactory.normalizePostgresJdbcUrl(localUrl)
        )

        // 2. Sanitización de credenciales para logs/errores
        val urlWithInlineSecret = "jdbc:postgresql://postgres.ref:ClaveSuperSecreta123@aws-0-us-east-1.pooler.supabase.com:5432/postgres?sslmode=require&password=ClaveQuerySecreta"
        val sanitized = DatabaseFactory.sanitizeJdbcUrlForLogs(urlWithInlineSecret)
        assertFalse(sanitized.contains("ClaveSuperSecreta123"), "No debe exponer contraseña en userinfo")
        assertFalse(sanitized.contains("ClaveQuerySecreta"), "No debe exponer contraseña en query string")
        assertTrue(sanitized.contains("postgres.ref:***@"))
        assertTrue(sanitized.contains("password=***"))

        // 3. Pool pequeño por defecto (maxPoolSize = 4) y sin datos demo
        val dbName = "adelita_pool_${UUID.randomUUID().toString().replace("-", "")}"
        val h2Url = "jdbc:h2:mem:$dbName;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1"
        val dbInfo = DatabaseFactory.init(
            jdbcUrl = h2Url,
            user = "sa",
            password = "",
            driver = "org.h2.Driver",
            allowEmbeddedFallback = false,
            seedDemoData = false
        )
        try {
            assertEquals(4, dbInfo.dataSource.maximumPoolSize, "El pool por defecto debe ser pequeño (4) para Supabase Session Pooler")
        } finally {
            dbInfo.onShutdown()
        }

        // 4. Fail-fast en producción cuando DB_ALLOW_EMBEDDED_FALLBACK=false y PostgreSQL no responde
        val unreachablePgUrl = "jdbc:postgresql://127.0.0.1:59999/postgres?connectTimeout=1&password=SecretoNoExponer999"
        val ex = assertFailsWith<IllegalStateException> {
            DatabaseFactory.init(
                jdbcUrl = unreachablePgUrl,
                user = "postgres.ref",
                password = "SecretoNoExponer999",
                allowEmbeddedFallback = false,
                seedDemoData = false,
                maxPoolSize = 4,
                connectionTimeoutMs = 1_000L
            )
        }
        assertTrue(ex.message?.contains("allowEmbeddedFallback=false") == true)
        assertFalse(ex.message?.contains("SecretoNoExponer999") == true, "El mensaje de error nunca debe filtrar secretos")
    }
}
