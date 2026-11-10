package mx.edu.upchiapas.adelitagoat.models

import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
enum class UserRole {
    CLIENTE,
    VENDEDOR
}

@Serializable
enum class OrderStatus {
    RECIBIDO,
    EN_PREPARACION,
    LISTO,
    ENTREGADO,
    CANCELADO;

    fun canTransitionTo(next: OrderStatus): Boolean {
        return when (this) {
            RECIBIDO -> next == EN_PREPARACION || next == CANCELADO
            EN_PREPARACION -> next == LISTO || next == CANCELADO
            LISTO -> next == ENTREGADO || next == CANCELADO
            ENTREGADO -> false
            CANCELADO -> false
        }
    }
}

@Serializable
enum class PaymentMethod {
    EFECTIVO_AL_RECOGER
}

@Serializable
enum class PaymentStatus {
    PENDIENTE_EFECTIVO,
    COBRADO_EN_EFECTIVO,
    CANCELADO
}

fun formatMxn(cents: Long): String {
    val whole = cents / 100
    val rem = kotlin.math.abs(cents % 100)
    return String.format(Locale.US, "$%d.%02d MXN", whole, rem)
}

@Serializable
data class ErrorResponse(
    val code: String,
    val message: String,
    val details: List<String> = emptyList()
)

@Serializable
data class HealthResponse(
    val status: String,
    val service: String,
    val database: String,
    val databaseMode: String,
    val timestamp: String
)

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val phone: String? = null,
    val role: UserRole = UserRole.CLIENTE
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class UserDto(
    val id: Long,
    val name: String,
    val email: String,
    val phone: String? = null,
    val role: UserRole
)

@Serializable
data class AuthResponse(
    val token: String,
    val expiresAt: String,
    val user: UserDto
)

@Serializable
data class MessageResponse(
    val message: String
)

@Serializable
data class CategoryDto(
    val id: Long,
    val name: String,
    val slug: String,
    val displayOrder: Int
)

@Serializable
data class ProductDto(
    val id: Long,
    val stallId: Long,
    val stallName: String,
    val campusLocation: String,
    val categoryId: Long,
    val categoryName: String,
    val name: String,
    val description: String,
    val priceCents: Long,
    val formattedPrice: String,
    val available: Boolean,
    val imageKey: String,
    val stallRating: Double = 4.9
)

@Serializable
data class RatingDto(
    val id: Long,
    val orderId: Long,
    val orderFolio: String,
    val customerId: Long,
    val customerName: String,
    val stallId: Long,
    val overallScore: Int,
    val productQualityScore: Int,
    val waitTimeScore: Int,
    val hygieneScore: Int,
    val serviceAttentionScore: Int,
    val comment: String? = null,
    val createdAt: String
)

@Serializable
data class StallSummaryDto(
    val id: Long,
    val vendorId: Long,
    val vendorName: String,
    val name: String,
    val description: String,
    val campusLocation: String,
    val category: String,
    val prepTimeMinutes: Int,
    val isOpen: Boolean,
    val imageKey: String,
    val averageRating: Double,
    val ratingCount: Int,
    val isFavorite: Boolean = false
)

@Serializable
data class StallDetailDto(
    val stall: StallSummaryDto,
    val products: List<ProductDto>,
    val recentRatings: List<RatingDto>
)

@Serializable
data class FavoriteToggleResponse(
    val stallId: Long,
    val isFavorite: Boolean
)

@Serializable
data class CreateOrderItemRequest(
    val productId: Long,
    val quantity: Int
)

@Serializable
data class CreateOrderRequest(
    val stallId: Long,
    val idempotencyKey: String? = null,
    val notes: String? = null,
    val items: List<CreateOrderItemRequest>
)

@Serializable
data class OrderItemDto(
    val id: Long,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitPriceCents: Long,
    val lineTotalCents: Long,
    val formattedUnitPrice: String,
    val formattedLineTotal: String
)

@Serializable
data class OrderDto(
    val id: Long,
    val folio: String,
    val customerId: Long,
    val customerName: String,
    val stallId: Long,
    val stallName: String,
    val campusLocation: String,
    val status: OrderStatus,
    val paymentMethod: PaymentMethod,
    val paymentStatus: PaymentStatus,
    val subtotalCents: Long,
    val totalCents: Long,
    val formattedTotal: String,
    val notes: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val deliveredAt: String? = null,
    val paidAt: String? = null,
    val items: List<OrderItemDto>,
    val rating: RatingDto? = null
)

@Serializable
data class CreateRatingRequest(
    val overallScore: Int,
    val productQualityScore: Int,
    val waitTimeScore: Int,
    val hygieneScore: Int,
    val serviceAttentionScore: Int,
    val comment: String? = null
)

@Serializable
data class UpsertStallRequest(
    val name: String,
    val description: String,
    val campusLocation: String,
    val category: String,
    val prepTimeMinutes: Int,
    val isOpen: Boolean = true,
    val imageKey: String = "tacos"
)

@Serializable
data class UpsertProductRequest(
    val categoryId: Long,
    val name: String,
    val description: String,
    val priceCents: Long,
    val available: Boolean = true,
    val imageKey: String = "taco"
)

@Serializable
data class UpdateAvailabilityRequest(
    val available: Boolean
)

@Serializable
data class UpdateOrderStatusRequest(
    val status: OrderStatus,
    val expectedCurrentStatus: OrderStatus? = null
)

open class ApiException(
    val statusCode: Int,
    val code: String,
    override val message: String,
    val details: List<String> = emptyList()
) : RuntimeException(message)

class BadRequestException(message: String, details: List<String> = emptyList()) :
    ApiException(400, "BAD_REQUEST", message, details)

class UnauthorizedException(message: String = "Sesión inválida o expirada. Inicia sesión nuevamente.") :
    ApiException(401, "UNAUTHORIZED", message)

class ForbiddenException(message: String = "No tienes permisos para realizar esta acción.") :
    ApiException(403, "FORBIDDEN", message)

class NotFoundException(message: String = "El recurso solicitado no existe.") :
    ApiException(404, "NOT_FOUND", message)

class ConflictException(message: String, details: List<String> = emptyList()) :
    ApiException(409, "CONFLICT", message, details)
