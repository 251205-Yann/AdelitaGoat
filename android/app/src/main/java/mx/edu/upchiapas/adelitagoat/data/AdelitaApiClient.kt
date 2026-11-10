package mx.edu.upchiapas.adelitagoat.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mx.edu.upchiapas.adelitagoat.domain.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class ApiClientException(
    val statusCode: Int,
    override val message: String
) : Exception(message)

class AdelitaApiClient(
    private val baseUrlProvider: () -> String,
    private val tokenProvider: () -> String?,
    private val httpClient: OkHttpClient = defaultOkHttpClient()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun register(req: RegisterRequest): AuthResponse =
        request("/api/auth/register", "POST", json.encodeToString(req)) {
            json.decodeFromString(it)
        }

    suspend fun login(req: LoginRequest): AuthResponse =
        request("/api/auth/login", "POST", json.encodeToString(req)) {
            json.decodeFromString(it)
        }

    suspend fun getMe(): UserDto =
        request("/api/auth/me", "GET") {
            json.decodeFromString(it)
        }

    suspend fun logout() {
        runCatching {
            request("/api/auth/logout", "POST", "{}") { it }
        }
    }

    suspend fun getCategories(): List<CategoryDto> =
        request("/api/categories", "GET") {
            json.decodeFromString(it)
        }

    suspend fun getStalls(search: String?, category: String?): List<StallSummaryDto> {
        val params = mutableListOf<String>()
        if (!search.isNullOrBlank()) {
            params.add("search=${URLEncoder.encode(search.trim(), "UTF-8")}")
        }
        if (!category.isNullOrBlank() && !category.equals("Todos", ignoreCase = true)) {
            params.add("category=${URLEncoder.encode(category.trim(), "UTF-8")}")
        }
        val query = if (params.isEmpty()) "" else "?${params.joinToString("&")}"
        return request("/api/stalls$query", "GET") {
            json.decodeFromString(it)
        }
    }

    suspend fun getStallDetail(stallId: Long): StallDetailDto =
        request("/api/stalls/$stallId", "GET") {
            json.decodeFromString(it)
        }

    suspend fun getProduct(productId: Long): ProductDto =
        request("/api/products/$productId", "GET") {
            json.decodeFromString(it)
        }

    suspend fun getFavorites(): List<StallSummaryDto> =
        request("/api/favorites", "GET") {
            json.decodeFromString(it)
        }

    suspend fun addFavorite(stallId: Long): FavoriteToggleResponse =
        request("/api/favorites/$stallId", "POST", "{}") {
            json.decodeFromString(it)
        }

    suspend fun removeFavorite(stallId: Long): FavoriteToggleResponse =
        request("/api/favorites/$stallId", "DELETE") {
            json.decodeFromString(it)
        }

    suspend fun createOrder(req: CreateOrderRequest): OrderDto =
        request(
            path = "/api/orders",
            method = "POST",
            body = json.encodeToString(req),
            extraHeaders = mapOf("Idempotency-Key" to req.idempotencyKey)
        ) {
            json.decodeFromString(it)
        }

    suspend fun getCustomerOrders(): List<OrderDto> =
        request("/api/orders", "GET") {
            json.decodeFromString(it)
        }

    suspend fun getOrderDetail(orderId: Long): OrderDto =
        request("/api/orders/$orderId", "GET") {
            json.decodeFromString(it)
        }

    suspend fun submitRating(orderId: Long, req: CreateRatingRequest): RatingDto =
        request("/api/orders/$orderId/rating", "POST", json.encodeToString(req)) {
            json.decodeFromString(it)
        }

    suspend fun getVendorStalls(): List<StallDetailDto> =
        request("/api/vendor/stalls", "GET") {
            json.decodeFromString(it)
        }

    suspend fun createVendorStall(req: UpsertStallRequest): StallDetailDto =
        request("/api/vendor/stalls", "POST", json.encodeToString(req)) {
            json.decodeFromString(it)
        }

    suspend fun updateVendorStall(stallId: Long, req: UpsertStallRequest): StallDetailDto =
        request("/api/vendor/stalls/$stallId", "PUT", json.encodeToString(req)) {
            json.decodeFromString(it)
        }

    suspend fun createVendorProduct(stallId: Long, req: UpsertProductRequest): ProductDto =
        request("/api/vendor/stalls/$stallId/products", "POST", json.encodeToString(req)) {
            json.decodeFromString(it)
        }

    suspend fun updateVendorProduct(productId: Long, req: UpsertProductRequest): ProductDto =
        request("/api/vendor/products/$productId", "PUT", json.encodeToString(req)) {
            json.decodeFromString(it)
        }

    suspend fun updateProductAvailability(productId: Long, available: Boolean): ProductDto =
        request(
            path = "/api/vendor/products/$productId/availability",
            method = "PATCH",
            body = json.encodeToString(UpdateAvailabilityRequest(available))
        ) {
            json.decodeFromString(it)
        }

    suspend fun getVendorOrders(stallId: Long? = null, status: OrderStatus? = null): List<OrderDto> {
        val params = mutableListOf<String>()
        if (stallId != null) params.add("stallId=$stallId")
        if (status != null) params.add("status=${status.name}")
        val query = if (params.isEmpty()) "" else "?${params.joinToString("&")}"
        return request("/api/vendor/orders$query", "GET") {
            json.decodeFromString(it)
        }
    }

    suspend fun updateOrderStatus(orderId: Long, req: UpdateOrderStatusRequest): OrderDto =
        request(
            path = "/api/vendor/orders/$orderId/status",
            method = "PATCH",
            body = json.encodeToString(req)
        ) {
            json.decodeFromString(it)
        }

    private suspend fun <T> request(
        path: String,
        method: String,
        body: String? = null,
        extraHeaders: Map<String, String> = emptyMap(),
        parser: (String) -> T
    ): T = withContext(Dispatchers.IO) {
        val fullUrl = "${baseUrlProvider().trimEnd('/')}$path"
        val requestBody = when {
            body != null -> body.toRequestBody(jsonMediaType)
            method == "POST" || method == "PUT" || method == "PATCH" -> "".toRequestBody(jsonMediaType)
            else -> null
        }

        val reqBuilder = Request.Builder()
            .url(fullUrl)
            .method(method, requestBody)
            .header("Accept", "application/json")

        val token = tokenProvider()
        if (!token.isNullOrBlank()) {
            reqBuilder.header("Authorization", "Bearer $token")
        }
        for ((k, v) in extraHeaders) {
            reqBuilder.header(k, v)
        }

        try {
            httpClient.newCall(reqBuilder.build()).execute().use { response ->
                val status = response.code
                val responseText = response.body?.string().orEmpty()

                if (status in 200..299) {
                    parser(responseText)
                } else {
                    val parsedError = runCatching {
                        json.decodeFromString<ApiErrorPayload>(responseText)
                    }.getOrNull()
                    val msg = parsedError?.message
                        ?: "Error del servidor (HTTP $status)"
                    throw ApiClientException(status, msg)
                }
            }
        } catch (ex: ApiClientException) {
            throw ex
        } catch (ex: Exception) {
            throw ApiClientException(
                0,
                "No se pudo conectar. Revisa tu conexión a internet e inténtalo de nuevo."
            )
        }
    }

    companion object {
        fun defaultOkHttpClient(): OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .writeTimeout(8, TimeUnit.SECONDS)
                .build()
    }
}
