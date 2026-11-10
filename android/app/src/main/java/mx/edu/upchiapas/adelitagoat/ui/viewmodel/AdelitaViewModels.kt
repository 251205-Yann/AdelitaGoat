package mx.edu.upchiapas.adelitagoat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mx.edu.upchiapas.adelitagoat.data.AdelitaRepository
import mx.edu.upchiapas.adelitagoat.data.CartAddResult
import mx.edu.upchiapas.adelitagoat.domain.*

class AuthViewModel(private val repository: AdelitaRepository) : ViewModel() {
    val currentUser: StateFlow<UserDto?> = repository.currentUser

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _baseUrl = MutableStateFlow(repository.sessionStore.getBaseUrl())
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()

    init {
        if (repository.sessionStore.getToken() != null) {
            viewModelScope.launch {
                repository.refreshSessionUser()
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun updateBaseUrl(newUrl: String) {
        repository.sessionStore.setBaseUrl(newUrl)
        _baseUrl.value = repository.sessionStore.getBaseUrl()
    }

    fun markOnboardingSeen() {
        repository.sessionStore.setOnboardingSeen()
    }

    fun isOnboardingSeen(): Boolean = repository.sessionStore.isOnboardingSeen()

    fun login(email: String, password: String, onSuccess: (UserDto) -> Unit) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            _errorMessage.value = "Ingresa un correo electrónico válido."
            return
        }
        if (password.isBlank()) {
            _errorMessage.value = "Ingresa tu contraseña."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val user = repository.login(LoginRequest(email = cleanEmail, password = password))
                onSuccess(user)
            } catch (ex: Exception) {
                _errorMessage.value = ex.message ?: "No fue posible iniciar sesión."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(
        name: String,
        email: String,
        password: String,
        phone: String,
        role: UserRole,
        onSuccess: (UserDto) -> Unit
    ) {
        val cleanName = name.trim()
        val cleanEmail = email.trim()
        val cleanPhone = phone.trim().takeIf { it.isNotEmpty() }

        if (cleanName.length < 2) {
            _errorMessage.value = "Ingresa tu nombre completo (mínimo 2 caracteres)."
            return
        }
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            _errorMessage.value = "Ingresa un correo institucional o personal válido."
            return
        }
        if (password.length < 6) {
            _errorMessage.value = "La contraseña debe tener al menos 6 caracteres."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val user = repository.register(
                    RegisterRequest(
                        name = cleanName,
                        email = cleanEmail,
                        password = password,
                        phone = cleanPhone,
                        role = role
                    )
                )
                onSuccess(user)
            } catch (ex: Exception) {
                _errorMessage.value = ex.message ?: "No fue posible completar el registro."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.logout()
            _isLoading.value = false
            onLoggedOut()
        }
    }
}

class CustomerViewModel(private val repository: AdelitaRepository) : ViewModel() {
    val cart: StateFlow<CartState> = repository.cart
    val lastConfirmedOrder: StateFlow<OrderDto?> = repository.lastConfirmedOrder

    private val _categories = MutableStateFlow<List<CategoryDto>>(emptyList())
    val categories: StateFlow<List<CategoryDto>> = _categories.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Todos")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _stalls = MutableStateFlow<List<StallSummaryDto>>(emptyList())
    val stalls: StateFlow<List<StallSummaryDto>> = _stalls.asStateFlow()

    private val _isLoadingStalls = MutableStateFlow(false)
    val isLoadingStalls: StateFlow<Boolean> = _isLoadingStalls.asStateFlow()

    private val _stallsError = MutableStateFlow<String?>(null)
    val stallsError: StateFlow<String?> = _stallsError.asStateFlow()

    private val _selectedStallDetail = MutableStateFlow<StallDetailDto?>(null)
    val selectedStallDetail: StateFlow<StallDetailDto?> = _selectedStallDetail.asStateFlow()

    private val _isLoadingStallDetail = MutableStateFlow(false)
    val isLoadingStallDetail: StateFlow<Boolean> = _isLoadingStallDetail.asStateFlow()

    private val _stallDetailError = MutableStateFlow<String?>(null)
    val stallDetailError: StateFlow<String?> = _stallDetailError.asStateFlow()

    private val _selectedProduct = MutableStateFlow<ProductDto?>(null)
    val selectedProduct: StateFlow<ProductDto?> = _selectedProduct.asStateFlow()

    private val _isLoadingProduct = MutableStateFlow(false)
    val isLoadingProduct: StateFlow<Boolean> = _isLoadingProduct.asStateFlow()

    private val _productError = MutableStateFlow<String?>(null)
    val productError: StateFlow<String?> = _productError.asStateFlow()

    private val _favorites = MutableStateFlow<List<StallSummaryDto>>(emptyList())
    val favorites: StateFlow<List<StallSummaryDto>> = _favorites.asStateFlow()

    private val _isLoadingFavorites = MutableStateFlow(false)
    val isLoadingFavorites: StateFlow<Boolean> = _isLoadingFavorites.asStateFlow()

    private val _favoritesError = MutableStateFlow<String?>(null)
    val favoritesError: StateFlow<String?> = _favoritesError.asStateFlow()

    private val _orders = MutableStateFlow<List<OrderDto>>(emptyList())
    val orders: StateFlow<List<OrderDto>> = _orders.asStateFlow()

    private val _isLoadingOrders = MutableStateFlow(false)
    val isLoadingOrders: StateFlow<Boolean> = _isLoadingOrders.asStateFlow()

    private val _ordersError = MutableStateFlow<String?>(null)
    val ordersError: StateFlow<String?> = _ordersError.asStateFlow()

    private val _selectedOrder = MutableStateFlow<OrderDto?>(null)
    val selectedOrder: StateFlow<OrderDto?> = _selectedOrder.asStateFlow()

    private val _isLoadingOrderDetail = MutableStateFlow(false)
    val isLoadingOrderDetail: StateFlow<Boolean> = _isLoadingOrderDetail.asStateFlow()

    private val _orderDetailError = MutableStateFlow<String?>(null)
    val orderDetailError: StateFlow<String?> = _orderDetailError.asStateFlow()

    private val _pendingStallConflict = MutableStateFlow<CartAddResult.ConflictDifferentStall?>(null)
    val pendingStallConflict: StateFlow<CartAddResult.ConflictDifferentStall?> = _pendingStallConflict.asStateFlow()

    private val _isSubmittingOrder = MutableStateFlow(false)
    val isSubmittingOrder: StateFlow<Boolean> = _isSubmittingOrder.asStateFlow()

    private val _checkoutError = MutableStateFlow<String?>(null)
    val checkoutError: StateFlow<String?> = _checkoutError.asStateFlow()

    private val _isSubmittingRating = MutableStateFlow(false)
    val isSubmittingRating: StateFlow<Boolean> = _isSubmittingRating.asStateFlow()

    private val _ratingError = MutableStateFlow<String?>(null)
    val ratingError: StateFlow<String?> = _ratingError.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun clearCheckoutError() {
        _checkoutError.value = null
    }

    fun clearRatingError() {
        _ratingError.value = null
    }

    fun loadHomeCatalog() {
        viewModelScope.launch {
            _isLoadingStalls.value = true
            _stallsError.value = null
            try {
                if (_categories.value.isEmpty()) {
                    _categories.value = repository.apiClient.getCategories()
                }
                val list = repository.apiClient.getStalls(
                    search = _searchQuery.value,
                    category = _selectedCategory.value
                )
                _stalls.value = list
            } catch (ex: Exception) {
                _stallsError.value = ex.message ?: "Error al cargar puestos disponibles."
            } finally {
                _isLoadingStalls.value = false
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        loadHomeCatalog()
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        loadHomeCatalog()
    }

    fun loadStallDetail(stallId: Long) {
        viewModelScope.launch {
            _isLoadingStallDetail.value = true
            _stallDetailError.value = null
            try {
                _selectedStallDetail.value = repository.apiClient.getStallDetail(stallId)
            } catch (ex: Exception) {
                _stallDetailError.value = ex.message ?: "No se pudo cargar el menú del puesto."
            } finally {
                _isLoadingStallDetail.value = false
            }
        }
    }

    fun loadProductDetail(productId: Long) {
        viewModelScope.launch {
            _isLoadingProduct.value = true
            _productError.value = null
            try {
                _selectedProduct.value = repository.apiClient.getProduct(productId)
            } catch (ex: Exception) {
                _productError.value = ex.message ?: "No se pudo cargar el detalle del producto."
            } finally {
                _isLoadingProduct.value = false
            }
        }
    }

    fun toggleFavorite(stallId: Long, currentlyFavorite: Boolean) {
        viewModelScope.launch {
            try {
                val res = if (currentlyFavorite) {
                    repository.apiClient.removeFavorite(stallId)
                } else {
                    repository.apiClient.addFavorite(stallId)
                }
                _stalls.value = _stalls.value.map {
                    if (it.id == stallId) it.copy(isFavorite = res.isFavorite) else it
                }
                _selectedStallDetail.value = _selectedStallDetail.value?.let { detail ->
                    if (detail.stall.id == stallId) {
                        detail.copy(stall = detail.stall.copy(isFavorite = res.isFavorite))
                    } else detail
                }
                loadFavorites()
                _feedbackMessage.value = if (res.isFavorite) {
                    "Puesto guardado en Favoritos"
                } else {
                    "Puesto eliminado de Favoritos"
                }
            } catch (ex: Exception) {
                _feedbackMessage.value = ex.message ?: "No se pudo actualizar favoritos."
            }
        }
    }

    fun loadFavorites() {
        viewModelScope.launch {
            _isLoadingFavorites.value = true
            _favoritesError.value = null
            try {
                _favorites.value = repository.apiClient.getFavorites()
            } catch (ex: Exception) {
                _favoritesError.value = ex.message ?: "Error al cargar tus puestos favoritos."
            } finally {
                _isLoadingFavorites.value = false
            }
        }
    }

    fun addProductToCart(product: ProductDto, quantity: Int = 1) {
        if (!product.available) {
            _feedbackMessage.value = "Este producto está agotado en este momento."
            return
        }
        when (val result = repository.addToCart(product, quantity, forceReplaceStall = false)) {
            is CartAddResult.Added -> {
                _feedbackMessage.value = "Agregado al apartado: ${quantity}x ${product.name}"
            }
            is CartAddResult.ConflictDifferentStall -> {
                _pendingStallConflict.value = result
            }
        }
    }

    fun confirmReplaceCartWithNewStall() {
        val conflict = _pendingStallConflict.value ?: return
        repository.addToCart(conflict.newProduct, conflict.requestedQuantity, forceReplaceStall = true)
        _pendingStallConflict.value = null
        _feedbackMessage.value = "Apartado reiniciado con ${conflict.newProduct.stallName}"
    }

    fun dismissStallConflict() {
        _pendingStallConflict.value = null
    }

    fun updateCartQuantity(productId: Long, newQuantity: Int) {
        repository.updateCartItemQuantity(productId, newQuantity)
    }

    fun updateCartNotes(notes: String) {
        repository.updateCartNotes(notes)
    }

    fun confirmOrder(onOrderCreated: (OrderDto) -> Unit) {
        if (_isSubmittingOrder.value) return
        viewModelScope.launch {
            _isSubmittingOrder.value = true
            _checkoutError.value = null
            try {
                val created = repository.confirmCartOrder()
                _selectedOrder.value = created
                loadCustomerOrders()
                onOrderCreated(created)
            } catch (ex: Exception) {
                _checkoutError.value = ex.message ?: "No fue posible confirmar el apartado."
            } finally {
                _isSubmittingOrder.value = false
            }
        }
    }

    fun loadCustomerOrders() {
        viewModelScope.launch {
            _isLoadingOrders.value = true
            _ordersError.value = null
            try {
                _orders.value = repository.apiClient.getCustomerOrders()
            } catch (ex: Exception) {
                _ordersError.value = ex.message ?: "No se pudo consultar el historial de pedidos."
            } finally {
                _isLoadingOrders.value = false
            }
        }
    }

    fun loadOrderDetail(orderId: Long) {
        viewModelScope.launch {
            _isLoadingOrderDetail.value = true
            _orderDetailError.value = null
            try {
                val detail = repository.apiClient.getOrderDetail(orderId)
                _selectedOrder.value = detail
            } catch (ex: Exception) {
                _orderDetailError.value = ex.message ?: "No se pudo cargar el estado del pedido."
            } finally {
                _isLoadingOrderDetail.value = false
            }
        }
    }

    fun submitRating(
        orderId: Long,
        overallScore: Int,
        productQualityScore: Int,
        waitTimeScore: Int,
        hygieneScore: Int,
        serviceAttentionScore: Int,
        comment: String,
        onSuccess: () -> Unit
    ) {
        if (_isSubmittingRating.value) return
        val cleanComment = comment.trim()
        if (cleanComment.isNotEmpty() && cleanComment.length < 5) {
            _ratingError.value = "Si deseas detallar tu comentario, escribe al menos 5 caracteres (o déjalo vacío)."
            return
        }

        viewModelScope.launch {
            _isSubmittingRating.value = true
            _ratingError.value = null
            try {
                repository.apiClient.submitRating(
                    orderId = orderId,
                    req = CreateRatingRequest(
                        overallScore = overallScore,
                        productQualityScore = productQualityScore,
                        waitTimeScore = waitTimeScore,
                        hygieneScore = hygieneScore,
                        serviceAttentionScore = serviceAttentionScore,
                        comment = cleanComment.ifEmpty { null }
                    )
                )
                loadOrderDetail(orderId)
                loadCustomerOrders()
                loadHomeCatalog()
                _feedbackMessage.value = "¡Gracias por tu calificación!"
                onSuccess()
            } catch (ex: Exception) {
                _ratingError.value = ex.message ?: "No se pudo registrar tu calificación."
            } finally {
                _isSubmittingRating.value = false
            }
        }
    }
}

class VendorViewModel(private val repository: AdelitaRepository) : ViewModel() {
    private val _vendorStalls = MutableStateFlow<List<StallDetailDto>>(emptyList())
    val vendorStalls: StateFlow<List<StallDetailDto>> = _vendorStalls.asStateFlow()

    private val _selectedStallId = MutableStateFlow<Long?>(null)
    val selectedStallId: StateFlow<Long?> = _selectedStallId.asStateFlow()

    private val _categories = MutableStateFlow<List<CategoryDto>>(emptyList())
    val categories: StateFlow<List<CategoryDto>> = _categories.asStateFlow()

    private val _vendorOrders = MutableStateFlow<List<OrderDto>>(emptyList())
    val vendorOrders: StateFlow<List<OrderDto>> = _vendorOrders.asStateFlow()

    private val _statusFilter = MutableStateFlow<OrderStatus?>(null)
    val statusFilter: StateFlow<OrderStatus?> = _statusFilter.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    fun clearMessages() {
        _errorMessage.value = null
        _actionMessage.value = null
    }

    fun loadVendorDashboard() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                if (_categories.value.isEmpty()) {
                    _categories.value = repository.apiClient.getCategories()
                }
                val stalls = repository.apiClient.getVendorStalls()
                _vendorStalls.value = stalls
                val currentStall = _selectedStallId.value
                if (currentStall == null || stalls.none { it.stall.id == currentStall }) {
                    _selectedStallId.value = stalls.firstOrNull()?.stall?.id
                }
                val orders = repository.apiClient.getVendorOrders(
                    stallId = _selectedStallId.value,
                    status = _statusFilter.value
                )
                _vendorOrders.value = orders
            } catch (ex: Exception) {
                _errorMessage.value = ex.message ?: "No se pudo cargar el panel del vendedor."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectVendorStall(stallId: Long) {
        _selectedStallId.value = stallId
        loadVendorDashboard()
    }

    fun saveStall(
        editingStallId: Long?,
        name: String,
        description: String,
        campusLocation: String,
        category: String,
        prepTimeMinutesInput: String,
        isOpen: Boolean,
        imageKey: String,
        onSaved: () -> Unit
    ) {
        val cleanName = name.trim()
        val cleanDesc = description.trim()
        val cleanLocation = campusLocation.trim()
        val cleanCategory = category.trim()
        val prepMinutes = prepTimeMinutesInput.trim().toIntOrNull() ?: -1

        if (cleanName.length < 2) {
            _errorMessage.value = "El nombre del puesto debe tener al menos 2 caracteres."
            return
        }
        if (cleanDesc.length < 4) {
            _errorMessage.value = "Ingresa una descripción del puesto (mínimo 4 caracteres)."
            return
        }
        if (cleanLocation.length < 4) {
            _errorMessage.value = "Ingresa la ubicación textual concreta del puesto (mínimo 4 caracteres)."
            return
        }
        if (cleanCategory.isEmpty()) {
            _errorMessage.value = "Selecciona una categoría para tu puesto."
            return
        }
        if (prepMinutes !in 1..120) {
            _errorMessage.value = "El tiempo estimado de preparación debe estar entre 1 y 120 minutos."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val req = UpsertStallRequest(
                    name = cleanName,
                    description = cleanDesc,
                    campusLocation = cleanLocation,
                    category = cleanCategory,
                    prepTimeMinutes = prepMinutes,
                    isOpen = isOpen,
                    imageKey = imageKey.trim().ifEmpty { "tacos" }
                )
                if (editingStallId == null) {
                    val created = repository.apiClient.createVendorStall(req)
                    _selectedStallId.value = created.stall.id
                    _actionMessage.value = "Puesto '${created.stall.name}' registrado exitosamente."
                } else {
                    val updated = repository.apiClient.updateVendorStall(editingStallId, req)
                    _selectedStallId.value = updated.stall.id
                    _actionMessage.value = "Datos del puesto '${updated.stall.name}' actualizados."
                }
                loadVendorDashboard()
                onSaved()
            } catch (ex: Exception) {
                _errorMessage.value = ex.message ?: "No se pudo guardar la información del puesto."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleStallOpenStatus(stall: StallSummaryDto) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val updated = repository.apiClient.updateVendorStall(
                    stallId = stall.id,
                    req = UpsertStallRequest(
                        name = stall.name,
                        description = stall.description,
                        campusLocation = stall.campusLocation,
                        category = stall.category,
                        prepTimeMinutes = stall.prepTimeMinutes,
                        isOpen = !stall.isOpen,
                        imageKey = stall.imageKey
                    )
                )
                _actionMessage.value = if (updated.stall.isOpen) {
                    "Puesto '${updated.stall.name}' marcado como ABIERTO."
                } else {
                    "Puesto '${updated.stall.name}' marcado como CERRADO."
                }
                loadVendorDashboard()
            } catch (ex: Exception) {
                _errorMessage.value = ex.message ?: "No se pudo cambiar el estado abierto/cerrado del puesto."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setOrderStatusFilter(status: OrderStatus?) {
        _statusFilter.value = status
        loadVendorDashboard()
    }

    fun updateOrderStatus(order: OrderDto, nextStatus: OrderStatus) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val updated = repository.apiClient.updateOrderStatus(
                    orderId = order.id,
                    req = UpdateOrderStatusRequest(
                        status = nextStatus,
                        expectedCurrentStatus = order.status
                    )
                )
                _vendorOrders.value = _vendorOrders.value.map {
                    if (it.id == updated.id) updated else it
                }
                _actionMessage.value = when (nextStatus) {
                    OrderStatus.ENTREGADO -> "Pedido ${updated.folio} entregado y cobrado en efectivo (${updated.formattedTotal})."
                    OrderStatus.CANCELADO -> "Pedido ${updated.folio} cancelado."
                    else -> "Pedido ${updated.folio} actualizado a ${nextStatus.displayTitle()}."
                }
                loadVendorDashboard()
            } catch (ex: Exception) {
                _errorMessage.value = ex.message ?: "Error al actualizar el estado del pedido."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleProductAvailability(product: ProductDto) {
        viewModelScope.launch {
            try {
                val updated = repository.apiClient.updateProductAvailability(
                    productId = product.id,
                    available = !product.available
                )
                _actionMessage.value = if (updated.available) {
                    "'${updated.name}' marcado como disponible."
                } else {
                    "'${updated.name}' marcado como agotado."
                }
                loadVendorDashboard()
            } catch (ex: Exception) {
                _errorMessage.value = ex.message ?: "No se pudo cambiar la disponibilidad."
            }
        }
    }

    fun saveProduct(
        stallId: Long,
        editingProductId: Long?,
        categoryId: Long,
        name: String,
        description: String,
        pricePesosInput: String,
        available: Boolean,
        imageKey: String,
        onSaved: () -> Unit
    ) {
        val cleanName = name.trim()
        val cleanDesc = description.trim()
        val priceDecimal = pricePesosInput.trim().replace(",", ".").toBigDecimalOrNull()
        val priceCents = priceDecimal?.multiply(java.math.BigDecimal(100))?.toLong() ?: -1L

        if (cleanName.length < 2) {
            _errorMessage.value = "El nombre del producto debe tener al menos 2 caracteres."
            return
        }
        if (cleanDesc.length < 4) {
            _errorMessage.value = "Ingresa una descripción clara (mínimo 4 caracteres)."
            return
        }
        if (priceCents <= 0L) {
            _errorMessage.value = "Ingresa un precio válido mayor a $0.00 MXN."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val req = UpsertProductRequest(
                    categoryId = categoryId,
                    name = cleanName,
                    description = cleanDesc,
                    priceCents = priceCents,
                    available = available,
                    imageKey = imageKey
                )
                if (editingProductId == null) {
                    repository.apiClient.createVendorProduct(stallId, req)
                    _actionMessage.value = "Producto '$cleanName' agregado al menú."
                } else {
                    repository.apiClient.updateVendorProduct(editingProductId, req)
                    _actionMessage.value = "Producto '$cleanName' actualizado."
                }
                loadVendorDashboard()
                onSaved()
            } catch (ex: Exception) {
                _errorMessage.value = ex.message ?: "No se pudo guardar el producto."
            } finally {
                _isLoading.value = false
            }
        }
    }
}

class AdelitaViewModelFactory(private val repository: AdelitaRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> AuthViewModel(repository) as T
            modelClass.isAssignableFrom(CustomerViewModel::class.java) -> CustomerViewModel(repository) as T
            modelClass.isAssignableFrom(VendorViewModel::class.java) -> VendorViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
