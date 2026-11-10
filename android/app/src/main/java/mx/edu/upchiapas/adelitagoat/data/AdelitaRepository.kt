package mx.edu.upchiapas.adelitagoat.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mx.edu.upchiapas.adelitagoat.domain.*
import java.util.UUID

sealed class CartAddResult {
    data object Added : CartAddResult()
    data class ConflictDifferentStall(
        val currentStallName: String,
        val newProduct: ProductDto,
        val requestedQuantity: Int
    ) : CartAddResult()
}

class AdelitaRepository(
    val sessionStore: SessionStore,
    val apiClient: AdelitaApiClient = AdelitaApiClient(
        baseUrlProvider = { sessionStore.getBaseUrl() },
        tokenProvider = { sessionStore.getToken() }
    )
) {
    private val _currentUser = MutableStateFlow(sessionStore.getSavedUser())
    val currentUser: StateFlow<UserDto?> = _currentUser.asStateFlow()

    private val _cart = MutableStateFlow(CartState())
    val cart: StateFlow<CartState> = _cart.asStateFlow()

    private val _lastConfirmedOrder = MutableStateFlow<OrderDto?>(null)
    val lastConfirmedOrder: StateFlow<OrderDto?> = _lastConfirmedOrder.asStateFlow()

    suspend fun register(req: RegisterRequest): UserDto {
        val res = apiClient.register(req)
        sessionStore.saveSession(res)
        _currentUser.value = res.user
        return res.user
    }

    suspend fun login(req: LoginRequest): UserDto {
        val res = apiClient.login(req)
        sessionStore.saveSession(res)
        _currentUser.value = res.user
        return res.user
    }

    suspend fun refreshSessionUser(): UserDto? {
        val token = sessionStore.getToken() ?: return null
        if (token.isBlank()) return null
        return try {
            val user = apiClient.getMe()
            sessionStore.updateUser(user)
            _currentUser.value = user
            user
        } catch (ex: ApiClientException) {
            if (ex.statusCode == 401) {
                sessionStore.clearSession()
                _currentUser.value = null
                _cart.value = CartState()
            }
            _currentUser.value
        }
    }

    suspend fun logout() {
        apiClient.logout()
        sessionStore.clearSession()
        _currentUser.value = null
        _cart.value = CartState()
        _lastConfirmedOrder.value = null
    }

    // =========================================================================
    // CARRITO DE UN SOLO PUESTO E IDEMPOTENCIA DE APARTADO
    // =========================================================================

    fun addToCart(product: ProductDto, quantity: Int = 1, forceReplaceStall: Boolean = false): CartAddResult {
        if (!product.available || quantity <= 0) return CartAddResult.Added
        val current = _cart.value

        if (current.stallId != null && current.stallId != product.stallId && current.items.isNotEmpty() && !forceReplaceStall) {
            return CartAddResult.ConflictDifferentStall(
                currentStallName = current.stallName,
                newProduct = product,
                requestedQuantity = quantity
            )
        }

        val baseItems = if (forceReplaceStall || current.stallId != product.stallId) {
            emptyList()
        } else {
            current.items
        }

        val existingIndex = baseItems.indexOfFirst { it.product.id == product.id }
        val updatedItems = baseItems.toMutableList()
        if (existingIndex >= 0) {
            val prev = updatedItems[existingIndex]
            updatedItems[existingIndex] = prev.copy(quantity = (prev.quantity + quantity).coerceAtMost(50))
        } else {
            updatedItems.add(CartItem(product = product, quantity = quantity.coerceAtMost(50)))
        }

        _cart.value = CartState(
            stallId = product.stallId,
            stallName = product.stallName,
            campusLocation = product.campusLocation,
            items = updatedItems,
            notes = if (forceReplaceStall) "" else current.notes,
            // Cambiar el contenido del carrito genera una nueva clave de idempotencia para este intento de apartado
            checkoutIdempotencyKey = UUID.randomUUID().toString()
        )
        return CartAddResult.Added
    }

    fun updateCartItemQuantity(productId: Long, newQuantity: Int) {
        val current = _cart.value
        val updated = if (newQuantity <= 0) {
            current.items.filterNot { it.product.id == productId }
        } else {
            current.items.map {
                if (it.product.id == productId) it.copy(quantity = newQuantity.coerceAtMost(50)) else it
            }
        }
        if (updated.isEmpty()) {
            _cart.value = CartState()
        } else {
            _cart.value = current.copy(
                items = updated,
                checkoutIdempotencyKey = UUID.randomUUID().toString()
            )
        }
    }

    fun updateCartNotes(notes: String) {
        _cart.value = _cart.value.copy(notes = notes.take(200))
    }

    fun clearCart() {
        _cart.value = CartState()
    }

    suspend fun confirmCartOrder(): OrderDto {
        val currentCart = _cart.value
        val stallId = currentCart.stallId
            ?: throw ApiClientException(400, "El carrito está vacío.")
        if (currentCart.items.isEmpty()) {
            throw ApiClientException(400, "Agrega al menos un producto al apartado.")
        }

        val idemKey = currentCart.checkoutIdempotencyKey.ifBlank {
            val generated = UUID.randomUUID().toString()
            _cart.value = currentCart.copy(checkoutIdempotencyKey = generated)
            generated
        }

        val req = CreateOrderRequest(
            stallId = stallId,
            idempotencyKey = idemKey,
            notes = currentCart.notes.trim().takeIf { it.isNotEmpty() },
            items = currentCart.items.map {
                CreateOrderItemRequest(productId = it.product.id, quantity = it.quantity)
            }
        )

        val order = apiClient.createOrder(req)
        _lastConfirmedOrder.value = order
        _cart.value = CartState()
        return order
    }
}
