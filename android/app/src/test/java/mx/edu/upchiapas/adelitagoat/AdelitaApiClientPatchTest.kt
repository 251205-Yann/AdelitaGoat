package mx.edu.upchiapas.adelitagoat

import kotlinx.coroutines.test.runTest
import mx.edu.upchiapas.adelitagoat.data.AdelitaApiClient
import mx.edu.upchiapas.adelitagoat.domain.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AdelitaApiClientPatchTest {

    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `updateProductAvailability sends real HTTP PATCH without method override header`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "id": 1,
                      "stallId": 1,
                      "stallName": "AdelitaGOAT - Tacos y Tortas",
                      "campusLocation": "Frente a la Universidad Politécnica de Chiapas",
                      "categoryId": 1,
                      "categoryName": "Tacos",
                      "name": "Tacos de carne asada (4 piezas)",
                      "description": "Tortillas de maíz calientes",
                      "priceCents": 6500,
                      "formattedPrice": "$65.00 MXN",
                      "available": false,
                      "imageKey": "taco_asada",
                      "stallRating": 4.9
                    }
                    """.trimIndent()
                )
        )

        val client = AdelitaApiClient(
            baseUrlProvider = { mockWebServer.url("/").toString().removeSuffix("/") },
            tokenProvider = { "vendor-token-123" }
        )

        val updated = client.updateProductAvailability(productId = 1L, available = false)
        assertFalse(updated.available)
        assertEquals(1L, updated.id)

        val recorded = mockWebServer.takeRequest()
        assertEquals("PATCH", recorded.method)
        assertEquals("/api/vendor/products/1/availability", recorded.path)
        assertEquals("Bearer vendor-token-123", recorded.getHeader("Authorization"))
        assertNull(recorded.getHeader("X-HTTP-Method-Override"))
        assertTrue(recorded.body.readUtf8().contains("\"available\":false"))
    }

    @Test
    fun `updateOrderStatus sends real HTTP PATCH without method override header`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "id": 3,
                      "folio": "A-1043",
                      "customerId": 1,
                      "customerName": "Wilbert Yanndel Molina",
                      "stallId": 2,
                      "stallName": "Tacos El Sol",
                      "campusLocation": "Andador Sur - Frente a la UP Chiapas",
                      "status": "ENTREGADO",
                      "paymentMethod": "EFECTIVO_AL_RECOGER",
                      "paymentStatus": "COBRADO_EN_EFECTIVO",
                      "subtotalCents": 8000,
                      "totalCents": 8000,
                      "formattedTotal": "$80.00 MXN",
                      "notes": "Con piña extra",
                      "createdAt": "2026-11-07T13:30:00-06:00",
                      "updatedAt": "2026-11-07T13:45:00-06:00",
                      "deliveredAt": "2026-11-07T13:45:00-06:00",
                      "paidAt": "2026-11-07T13:45:00-06:00",
                      "items": [
                        {
                          "id": 1,
                          "productId": 7,
                          "productName": "Orden de tacos al pastor (5 piezas)",
                          "quantity": 1,
                          "unitPriceCents": 6000,
                          "lineTotalCents": 6000,
                          "formattedUnitPrice": "$60.00 MXN",
                          "formattedLineTotal": "$60.00 MXN"
                        }
                      ],
                      "rating": null
                    }
                    """.trimIndent()
                )
        )

        val client = AdelitaApiClient(
            baseUrlProvider = { mockWebServer.url("/").toString().removeSuffix("/") },
            tokenProvider = { "vendor-token-123" }
        )

        val updatedOrder = client.updateOrderStatus(
            orderId = 3L,
            req = UpdateOrderStatusRequest(
                status = OrderStatus.ENTREGADO,
                expectedCurrentStatus = OrderStatus.LISTO
            )
        )

        assertEquals(OrderStatus.ENTREGADO, updatedOrder.status)
        assertEquals(PaymentStatus.COBRADO_EN_EFECTIVO, updatedOrder.paymentStatus)

        val recorded = mockWebServer.takeRequest()
        assertEquals("PATCH", recorded.method)
        assertEquals("/api/vendor/orders/3/status", recorded.path)
        assertEquals("Bearer vendor-token-123", recorded.getHeader("Authorization"))
        assertNull(recorded.getHeader("X-HTTP-Method-Override"))
        val rawBody = recorded.body.readUtf8()
        assertTrue(rawBody.contains("\"status\":\"ENTREGADO\""))
        assertTrue(rawBody.contains("\"expectedCurrentStatus\":\"LISTO\""))
    }

    @Test
    fun `createVendorStall and updateVendorStall send POST and PUT to vendor stalls endpoints`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "stall": {
                        "id": 10,
                        "vendorId": 4,
                        "vendorName": "Vendedor Real",
                        "name": "Puesto Real Universitario",
                        "description": "Antojitos recién hechos",
                        "campusLocation": "Portón Poniente, acera norte",
                        "category": "Tacos",
                        "prepTimeMinutes": 10,
                        "isOpen": true,
                        "imageKey": "tacos",
                        "averageRating": 4.9,
                        "ratingCount": 0,
                        "isFavorite": false
                      },
                      "products": [],
                      "recentRatings": []
                    }
                    """.trimIndent()
                )
        )
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "stall": {
                        "id": 10,
                        "vendorId": 4,
                        "vendorName": "Vendedor Real",
                        "name": "Puesto Real Universitario Editado",
                        "description": "Antojitos y tortas",
                        "campusLocation": "Portón Principal #2",
                        "category": "Tortas",
                        "prepTimeMinutes": 12,
                        "isOpen": false,
                        "imageKey": "tortas",
                        "averageRating": 4.9,
                        "ratingCount": 0,
                        "isFavorite": false
                      },
                      "products": [],
                      "recentRatings": []
                    }
                    """.trimIndent()
                )
        )

        val client = AdelitaApiClient(
            baseUrlProvider = { mockWebServer.url("/").toString().removeSuffix("/") },
            tokenProvider = { "vendor-token-xyz" }
        )

        val created = client.createVendorStall(
            UpsertStallRequest(
                name = "Puesto Real Universitario",
                description = "Antojitos recién hechos",
                campusLocation = "Portón Poniente, acera norte",
                category = "Tacos",
                prepTimeMinutes = 10,
                isOpen = true,
                imageKey = "tacos"
            )
        )
        assertEquals(10L, created.stall.id)
        assertEquals("Puesto Real Universitario", created.stall.name)
        assertTrue(created.stall.isOpen)

        val postRecorded = mockWebServer.takeRequest()
        assertEquals("POST", postRecorded.method)
        assertEquals("/api/vendor/stalls", postRecorded.path)
        assertEquals("Bearer vendor-token-xyz", postRecorded.getHeader("Authorization"))

        val updated = client.updateVendorStall(
            stallId = 10L,
            req = UpsertStallRequest(
                name = "Puesto Real Universitario Editado",
                description = "Antojitos y tortas",
                campusLocation = "Portón Principal #2",
                category = "Tortas",
                prepTimeMinutes = 12,
                isOpen = false,
                imageKey = "tortas"
            )
        )
        assertEquals("Puesto Real Universitario Editado", updated.stall.name)
        assertEquals("Portón Principal #2", updated.stall.campusLocation)
        assertFalse(updated.stall.isOpen)

        val putRecorded = mockWebServer.takeRequest()
        assertEquals("PUT", putRecorded.method)
        assertEquals("/api/vendor/stalls/10", putRecorded.path)
        assertEquals("Bearer vendor-token-xyz", putRecorded.getHeader("Authorization"))
    }

    @Test
    fun `live integration with Ktor server when LIVE_SERVER_URL is set exercises PATCH routes end to end`() = runTest {
        val liveUrl = System.getenv("LIVE_SERVER_URL")?.takeIf { it.isNotBlank() } ?: return@runTest

        var currentToken: String? = null
        val client = AdelitaApiClient(
            baseUrlProvider = { liveUrl },
            tokenProvider = { currentToken }
        )

        // 1. Login Vendedor en el servidor Ktor real usando AdelitaApiClient de Android
        val vendorAuth = client.login(LoginRequest("vendedor1.demo@example.invalid", "Vendedor123!"))
        currentToken = vendorAuth.token

        // 2. Cambiar disponibilidad de producto (PATCH /api/vendor/products/{id}/availability) y restaurarla
        val stalls = client.getVendorStalls()
        val firstProduct = stalls.first().products.first()

        val toggledOff = client.updateProductAvailability(firstProduct.id, false)
        assertFalse(toggledOff.available)

        val toggledOn = client.updateProductAvailability(firstProduct.id, true)
        assertTrue(toggledOn.available)

        // 3. Login Cliente y crear un nuevo apartado desde AdelitaApiClient
        val customerAuth = client.login(LoginRequest("cliente1.demo@example.invalid", "Cliente123!"))
        currentToken = customerAuth.token

        val createdOrder = client.createOrder(
            CreateOrderRequest(
                stallId = firstProduct.stallId,
                idempotencyKey = "android-client-live-${System.currentTimeMillis()}",
                notes = "Creado desde AdelitaApiClient Android",
                items = listOf(CreateOrderItemRequest(productId = firstProduct.id, quantity = 1))
            )
        )
        assertEquals(OrderStatus.RECIBIDO, createdOrder.status)

        // 4. Cambiar a token de Vendedor y avanzar el pedido usando PATCH /api/vendor/orders/{id}/status desde AdelitaApiClient
        currentToken = vendorAuth.token
        val inPrep = client.updateOrderStatus(
            orderId = createdOrder.id,
            req = UpdateOrderStatusRequest(OrderStatus.EN_PREPARACION, OrderStatus.RECIBIDO)
        )
        assertEquals(OrderStatus.EN_PREPARACION, inPrep.status)

        val ready = client.updateOrderStatus(
            orderId = createdOrder.id,
            req = UpdateOrderStatusRequest(OrderStatus.LISTO, OrderStatus.EN_PREPARACION)
        )
        assertEquals(OrderStatus.LISTO, ready.status)

        val delivered = client.updateOrderStatus(
            orderId = createdOrder.id,
            req = UpdateOrderStatusRequest(OrderStatus.ENTREGADO, OrderStatus.LISTO)
        )
        assertEquals(OrderStatus.ENTREGADO, delivered.status)
        assertEquals(PaymentStatus.COBRADO_EN_EFECTIVO, delivered.paymentStatus)
    }
}
