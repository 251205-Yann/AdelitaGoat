package mx.edu.upchiapas.adelitagoat

import mx.edu.upchiapas.adelitagoat.domain.OrderStatus
import mx.edu.upchiapas.adelitagoat.domain.PaymentStatus
import org.junit.Assert.*
import org.junit.Test

class OrderStateRulesTest {

    @Test
    fun `payment status labels clearly distinguish cash pending from cash collected`() {
        assertTrue(PaymentStatus.PENDIENTE_EFECTIVO.displayLabel().contains("efectivo", ignoreCase = true))
        assertTrue(PaymentStatus.COBRADO_EN_EFECTIVO.displayLabel().contains("Cobrado en efectivo", ignoreCase = true))
        assertTrue(PaymentStatus.CANCELADO.displayLabel().contains("cancelado", ignoreCase = true))
    }

    @Test
    fun `order status display titles match Spanish UI contract`() {
        assertEquals("Pedido recibido", OrderStatus.RECIBIDO.displayTitle())
        assertEquals("En preparación", OrderStatus.EN_PREPARACION.displayTitle())
        assertEquals("Listo para recoger", OrderStatus.LISTO.displayTitle())
        assertEquals("Entregado en mostrador", OrderStatus.ENTREGADO.displayTitle())
        assertEquals("Cancelado", OrderStatus.CANCELADO.displayTitle())
    }
}
