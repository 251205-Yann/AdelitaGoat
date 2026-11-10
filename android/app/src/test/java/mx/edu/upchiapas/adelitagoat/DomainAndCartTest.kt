package mx.edu.upchiapas.adelitagoat

import mx.edu.upchiapas.adelitagoat.domain.*
import org.junit.Assert.*
import org.junit.Test

class DomainAndCartTest {

    @Test
    fun `formatMxnCents formats integer cents without floating point errors`() {
        assertEquals("$65.00 MXN", formatMxnCents(6500L))
        assertEquals("$90.00 MXN", formatMxnCents(9000L))
        assertEquals("$25.50 MXN", formatMxnCents(2550L))
        assertEquals("$0.05 MXN", formatMxnCents(5L))
    }

    @Test
    fun `order status progression follows valid vendor transitions`() {
        assertEquals(OrderStatus.EN_PREPARACION, OrderStatus.RECIBIDO.nextVendorStatus())
        assertEquals(OrderStatus.LISTO, OrderStatus.EN_PREPARACION.nextVendorStatus())
        assertEquals(OrderStatus.ENTREGADO, OrderStatus.LISTO.nextVendorStatus())
        assertNull(OrderStatus.ENTREGADO.nextVendorStatus())
        assertNull(OrderStatus.CANCELADO.nextVendorStatus())

        assertTrue(OrderStatus.RECIBIDO.canCancel())
        assertTrue(OrderStatus.EN_PREPARACION.canCancel())
        assertTrue(OrderStatus.LISTO.canCancel())
        assertFalse(OrderStatus.ENTREGADO.canCancel())
        assertFalse(OrderStatus.CANCELADO.canCancel())
    }

    @Test
    fun `cart state computes item counts and total cents accurately`() {
        val p1 = ProductDto(
            id = 1L,
            stallId = 1L,
            stallName = "AdelitaGOAT",
            campusLocation = "UP Chiapas",
            categoryId = 1L,
            categoryName = "Tacos",
            name = "Tacos de carne asada (4 piezas)",
            description = "Con cebolla y cilantro",
            priceCents = 6500L,
            formattedPrice = "$65.00 MXN",
            available = true,
            imageKey = "taco"
        )
        val p2 = ProductDto(
            id = 2L,
            stallId = 1L,
            stallName = "AdelitaGOAT",
            campusLocation = "UP Chiapas",
            categoryId = 5L,
            categoryName = "Bebidas",
            name = "Agua fresca de horchata",
            description = "500 ml",
            priceCents = 2500L,
            formattedPrice = "$25.00 MXN",
            available = true,
            imageKey = "horchata"
        )

        val cart = CartState(
            stallId = 1L,
            stallName = "AdelitaGOAT",
            campusLocation = "UP Chiapas",
            items = listOf(
                CartItem(product = p1, quantity = 1),
                CartItem(product = p2, quantity = 2)
            ),
            checkoutIdempotencyKey = "idem-test-123"
        )

        assertEquals(3, cart.totalItemCount)
        assertEquals(11500L, cart.subtotalCents)
        assertEquals(11500L, cart.totalCents)
        assertEquals("$115.00 MXN", cart.formattedTotal)
    }
}
