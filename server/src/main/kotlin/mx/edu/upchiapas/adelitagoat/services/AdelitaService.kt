package mx.edu.upchiapas.adelitagoat.services

import mx.edu.upchiapas.adelitagoat.models.*
import org.mindrot.jbcrypt.BCrypt
import java.security.SecureRandom
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.sql.Timestamp
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.sql.DataSource
import kotlin.math.roundToInt

class AdelitaService(private val dataSource: DataSource) {
    private val secureRandom = SecureRandom()
    private val mexicoOffset = ZoneOffset.ofHours(-6)
    private val isoFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    private fun nowOffset(): OffsetDateTime = OffsetDateTime.now(mexicoOffset)
    private fun Timestamp.toIsoString(): String = this.toInstant().atOffset(mexicoOffset).format(isoFormatter)

    private fun generateToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun register(req: RegisterRequest): AuthResponse {
        val cleanName = req.name.trim()
        val cleanEmail = req.email.trim().lowercase(Locale.ROOT)
        val cleanPhone = req.phone?.trim()?.takeIf { it.isNotEmpty() }

        if (cleanName.length < 2) {
            throw BadRequestException("El nombre debe tener al menos 2 caracteres.")
        }
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            throw BadRequestException("Ingresa un correo electrónico válido.")
        }
        if (req.password.length < 6) {
            throw BadRequestException("La contraseña debe tener al menos 6 caracteres.")
        }

        return dataSource.connection.use { conn ->
            val prevAuto = conn.autoCommit
            conn.autoCommit = false
            try {
                conn.prepareStatement("SELECT id FROM users WHERE LOWER(email) = LOWER(?)").use { ps ->
                    ps.setString(1, cleanEmail)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) {
                            throw ConflictException("El correo electrónico ya está registrado.")
                        }
                    }
                }

                val passwordHash = BCrypt.hashpw(req.password, BCrypt.gensalt(10))
                val now = nowOffset()
                val userId = conn.prepareStatement(
                    "INSERT INTO users (name, email, password_hash, phone, role, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
                ).use { ps ->
                    ps.setString(1, cleanName)
                    ps.setString(2, cleanEmail)
                    ps.setString(3, passwordHash)
                    ps.setString(4, cleanPhone)
                    ps.setString(5, req.role.name)
                    ps.setTimestamp(6, Timestamp.from(now.toInstant()))
                    ps.executeUpdate()
                    ps.generatedKeys.use { rs ->
                        rs.next()
                        rs.getLong(1)
                    }
                }

                val session = createSession(conn, userId)
                conn.commit()
                AuthResponse(
                    token = session.first,
                    expiresAt = session.second,
                    user = UserDto(
                        id = userId,
                        name = cleanName,
                        email = cleanEmail,
                        phone = cleanPhone,
                        role = req.role
                    )
                )
            } catch (ex: Exception) {
                conn.rollback()
                throw ex
            } finally {
                conn.autoCommit = prevAuto
            }
        }
    }

    fun login(req: LoginRequest): AuthResponse {
        val cleanEmail = req.email.trim().lowercase(Locale.ROOT)
        if (cleanEmail.isBlank() || req.password.isBlank()) {
            throw BadRequestException("Debes ingresar correo y contraseña.")
        }

        return dataSource.connection.use { conn ->
            val userRow = conn.prepareStatement(
                "SELECT id, name, email, password_hash, phone, role FROM users WHERE LOWER(email) = LOWER(?)"
            ).use { ps ->
                ps.setString(1, cleanEmail)
                ps.executeQuery().use { rs ->
                    if (!rs.next()) null
                    else Triple(
                        UserDto(
                            id = rs.getLong("id"),
                            name = rs.getString("name"),
                            email = rs.getString("email"),
                            phone = rs.getString("phone"),
                            role = UserRole.valueOf(rs.getString("role"))
                        ),
                        rs.getString("password_hash"),
                        rs.getLong("id")
                    )
                }
            } ?: throw UnauthorizedException("Correo o contraseña incorrectos.")

            if (!BCrypt.checkpw(req.password, userRow.second)) {
                throw UnauthorizedException("Correo o contraseña incorrectos.")
            }

            val session = createSession(conn, userRow.third)
            AuthResponse(
                token = session.first,
                expiresAt = session.second,
                user = userRow.first
            )
        }
    }

    fun authenticate(token: String?): UserDto {
        val cleanToken = token?.trim()?.removePrefix("Bearer ")?.trim()
        if (cleanToken.isNullOrBlank()) {
            throw UnauthorizedException("Token de sesión requerido.")
        }

        return dataSource.connection.use { conn ->
            conn.prepareStatement(
                """
                SELECT u.id, u.name, u.email, u.phone, u.role, s.expires_at
                FROM sessions s
                JOIN users u ON u.id = s.user_id
                WHERE s.token = ?
                """.trimIndent()
            ).use { ps ->
                ps.setString(1, cleanToken)
                ps.executeQuery().use { rs ->
                    if (!rs.next()) {
                        throw UnauthorizedException("Sesión inválida o inexistente.")
                    }
                    val expiresAt = rs.getTimestamp("expires_at").toInstant()
                    if (expiresAt.isBefore(nowOffset().toInstant())) {
                        throw UnauthorizedException("La sesión ha expirado. Inicia sesión nuevamente.")
                    }
                    UserDto(
                        id = rs.getLong("id"),
                        name = rs.getString("name"),
                        email = rs.getString("email"),
                        phone = rs.getString("phone"),
                        role = UserRole.valueOf(rs.getString("role"))
                    )
                }
            }
        }
    }

    fun logout(token: String?) {
        val cleanToken = token?.trim()?.removePrefix("Bearer ")?.trim() ?: return
        dataSource.connection.use { conn ->
            conn.prepareStatement("DELETE FROM sessions WHERE token = ?").use { ps ->
                ps.setString(1, cleanToken)
                ps.executeUpdate()
            }
        }
    }

    private fun createSession(conn: Connection, userId: Long): Pair<String, String> {
        val token = generateToken()
        val now = nowOffset()
        val expiresAt = now.plusDays(7)
        conn.prepareStatement(
            "INSERT INTO sessions (token, user_id, expires_at, created_at) VALUES (?, ?, ?, ?)"
        ).use { ps ->
            ps.setString(1, token)
            ps.setLong(2, userId)
            ps.setTimestamp(3, Timestamp.from(expiresAt.toInstant()))
            ps.setTimestamp(4, Timestamp.from(now.toInstant()))
            ps.executeUpdate()
        }
        return token to expiresAt.format(isoFormatter)
    }

    // =========================================================================
    // 2. CATÁLOGO DE CATEGORÍAS, PUESTOS Y PRODUCTOS
    // =========================================================================

    fun getCategories(): List<CategoryDto> {
        return dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT id, name, slug, display_order FROM categories ORDER BY display_order ASC, id ASC").use { ps ->
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<CategoryDto>()
                    while (rs.next()) {
                        list.add(
                            CategoryDto(
                                id = rs.getLong("id"),
                                name = rs.getString("name"),
                                slug = rs.getString("slug"),
                                displayOrder = rs.getInt("display_order")
                            )
                        )
                    }
                    list
                }
            }
        }
    }

    fun getStalls(search: String?, category: String?, currentUserId: Long?): List<StallSummaryDto> {
        val cleanSearch = search?.trim()?.lowercase(Locale.ROOT)?.takeIf { it.isNotEmpty() }
        val cleanCategory = category?.trim()?.takeIf { it.isNotEmpty() && !it.equals("Todos", ignoreCase = true) }

        return dataSource.connection.use { conn ->
            val favoriteStallIds = if (currentUserId != null) getFavoriteStallIds(conn, currentUserId) else emptySet()
            val allStalls = loadAllStalls(conn, favoriteStallIds)

            allStalls.filter { stall ->
                val matchesCategory = if (cleanCategory == null) {
                    true
                } else {
                    stall.category.equals(cleanCategory, ignoreCase = true) ||
                        stallHasProductInCategory(conn, stall.id, cleanCategory)
                }

                val matchesSearch = if (cleanSearch == null) {
                    true
                } else {
                    stall.name.lowercase(Locale.ROOT).contains(cleanSearch) ||
                        stall.description.lowercase(Locale.ROOT).contains(cleanSearch) ||
                        stall.category.lowercase(Locale.ROOT).contains(cleanSearch) ||
                        stallHasProductMatchingSearch(conn, stall.id, cleanSearch)
                }

                matchesCategory && matchesSearch
            }
        }
    }

    fun getStallDetail(stallId: Long, currentUserId: Long?): StallDetailDto {
        return dataSource.connection.use { conn ->
            val favoriteStallIds = if (currentUserId != null) getFavoriteStallIds(conn, currentUserId) else emptySet()
            val stall = loadStallById(conn, stallId, favoriteStallIds)
                ?: throw NotFoundException("El puesto solicitado no existe.")
            val products = loadProductsForStall(conn, stallId, stall.name, stall.campusLocation, stall.averageRating)
            val ratings = loadRatingsForStall(conn, stallId)
            StallDetailDto(
                stall = stall,
                products = products,
                recentRatings = ratings
            )
        }
    }

    fun getProductById(productId: Long): ProductDto {
        return dataSource.connection.use { conn ->
            conn.prepareStatement(
                """
                SELECT p.id, p.stall_id, s.name AS stall_name, s.campus_location,
                       p.category_id, c.name AS category_name,
                       p.name, p.description, p.price_cents, p.available, p.image_key
                FROM products p
                JOIN stalls s ON s.id = p.stall_id
                JOIN categories c ON c.id = p.category_id
                WHERE p.id = ?
                """.trimIndent()
            ).use { ps ->
                ps.setLong(1, productId)
                ps.executeQuery().use { rs ->
                    if (!rs.next()) throw NotFoundException("El producto solicitado no existe.")
                    val stallId = rs.getLong("stall_id")
                    val ratingStats = loadStallRatingStats(conn, stallId)
                    val priceCents = rs.getLong("price_cents")
                    ProductDto(
                        id = rs.getLong("id"),
                        stallId = stallId,
                        stallName = rs.getString("stall_name"),
                        campusLocation = rs.getString("campus_location"),
                        categoryId = rs.getLong("category_id"),
                        categoryName = rs.getString("category_name"),
                        name = rs.getString("name"),
                        description = rs.getString("description"),
                        priceCents = priceCents,
                        formattedPrice = formatMxn(priceCents),
                        available = rs.getBoolean("available"),
                        imageKey = rs.getString("image_key"),
                        stallRating = ratingStats.first
                    )
                }
            }
        }
    }

    // =========================================================================
    // 3. FAVORITOS (CLIENTE)
    // =========================================================================

    fun getFavorites(userId: Long): List<StallSummaryDto> {
        return dataSource.connection.use { conn ->
            val favIds = getFavoriteStallIds(conn, userId)
            if (favIds.isEmpty()) return emptyList()
            loadAllStalls(conn, favIds).filter { it.id in favIds }
        }
    }

    fun addFavorite(userId: Long, stallId: Long): FavoriteToggleResponse {
        return dataSource.connection.use { conn ->
            loadStallById(conn, stallId, emptySet())
                ?: throw NotFoundException("El puesto indicado no existe.")
            conn.prepareStatement("SELECT id FROM favorites WHERE user_id = ? AND stall_id = ?").use { ps ->
                ps.setLong(1, userId)
                ps.setLong(2, stallId)
                ps.executeQuery().use { rs ->
                    if (!rs.next()) {
                        conn.prepareStatement("INSERT INTO favorites (user_id, stall_id, created_at) VALUES (?, ?, ?)").use { ins ->
                            ins.setLong(1, userId)
                            ins.setLong(2, stallId)
                            ins.setTimestamp(3, Timestamp.from(nowOffset().toInstant()))
                            ins.executeUpdate()
                        }
                    }
                }
            }
            FavoriteToggleResponse(stallId = stallId, isFavorite = true)
        }
    }

    fun removeFavorite(userId: Long, stallId: Long): FavoriteToggleResponse {
        return dataSource.connection.use { conn ->
            conn.prepareStatement("DELETE FROM favorites WHERE user_id = ? AND stall_id = ?").use { ps ->
                ps.setLong(1, userId)
                ps.setLong(2, stallId)
                ps.executeUpdate()
            }
            FavoriteToggleResponse(stallId = stallId, isFavorite = false)
        }
    }

    // =========================================================================
    // 4. PEDIDOS / APARTADOS Y CALIFICACIONES (CLIENTE)
    // =========================================================================

    fun createOrder(customerId: Long, req: CreateOrderRequest, headerIdempotencyKey: String?): Pair<OrderDto, Boolean> {
        val effectiveIdempotencyKey = (headerIdempotencyKey?.trim()?.takeIf { it.isNotEmpty() }
            ?: req.idempotencyKey?.trim()?.takeIf { it.isNotEmpty() })
            ?: throw BadRequestException("Se requiere clave de idempotencia (Idempotency-Key) para confirmar el apartado.")

        if (req.items.isEmpty()) {
            throw BadRequestException("El carrito está vacío. Agrega al menos un producto.")
        }
        for (item in req.items) {
            if (item.quantity <= 0 || item.quantity > 50) {
                throw BadRequestException("Cantidad inválida (${item.quantity}) para el producto ${item.productId}.")
            }
        }

        return dataSource.connection.use { conn ->
            val prevAuto = conn.autoCommit
            conn.autoCommit = false
            try {
                // 1. Verificar si ya existe un pedido para este cliente con la misma clave de idempotencia
                val existingOrderId = findOrderByIdempotencyKey(conn, customerId, effectiveIdempotencyKey)
                if (existingOrderId != null) {
                    val existingDto = loadOrderById(conn, existingOrderId)!!
                    conn.commit()
                    return@use existingDto to false
                }

                // 2. Verificar que el puesto exista y esté abierto
                val stall = loadStallById(conn, req.stallId, emptySet())
                    ?: throw NotFoundException("El puesto seleccionado no existe.")
                if (!stall.isOpen) {
                    throw BadRequestException("El puesto '${stall.name}' se encuentra cerrado en este momento.")
                }

                // 3. Consolidar cantidades por producto y validar existencia, pertenencia al puesto y disponibilidad
                val mergedQuantities = linkedMapOf<Long, Int>()
                for (item in req.items) {
                    mergedQuantities[item.productId] = (mergedQuantities[item.productId] ?: 0) + item.quantity
                }

                data class ValidatedLine(
                    val productId: Long,
                    val productName: String,
                    val quantity: Int,
                    val unitPriceCents: Long,
                    val lineTotalCents: Long
                )

                val validatedLines = mutableListOf<ValidatedLine>()
                var subtotalCents = 0L

                for ((productId, quantity) in mergedQuantities) {
                    conn.prepareStatement(
                        "SELECT id, stall_id, name, price_cents, available FROM products WHERE id = ?"
                    ).use { ps ->
                        ps.setLong(1, productId)
                        ps.executeQuery().use { rs ->
                            if (!rs.next()) {
                                throw BadRequestException("El producto con ID $productId no existe.")
                            }
                            val prodStallId = rs.getLong("stall_id")
                            val prodName = rs.getString("name")
                            val priceCents = rs.getLong("price_cents")
                            val available = rs.getBoolean("available")

                            if (prodStallId != req.stallId) {
                                throw BadRequestException("El producto '$prodName' pertenece a otro puesto. Solo puedes apartar de un puesto a la vez.")
                            }
                            if (!available) {
                                throw BadRequestException("El producto '$prodName' no está disponible actualmente.")
                            }

                            val lineTotal = Math.multiplyExact(priceCents, quantity.toLong())
                            subtotalCents = Math.addExact(subtotalCents, lineTotal)
                            validatedLines.add(
                                ValidatedLine(
                                    productId = productId,
                                    productName = prodName,
                                    quantity = quantity,
                                    unitPriceCents = priceCents,
                                    lineTotalCents = lineTotal
                                )
                            )
                        }
                    }
                }

                val totalCents = subtotalCents
                val now = nowOffset()
                val tempFolio = "TMP-${System.nanoTime()}"

                val orderId = conn.prepareStatement(
                    """
                    INSERT INTO orders (
                        folio, customer_id, stall_id, status, payment_method, payment_status,
                        subtotal_cents, total_cents, notes, idempotency_key, created_at, updated_at
                    ) VALUES (?, ?, ?, 'RECIBIDO', 'EFECTIVO_AL_RECOGER', 'PENDIENTE_EFECTIVO', ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    Statement.RETURN_GENERATED_KEYS
                ).use { ps ->
                    ps.setString(1, tempFolio)
                    ps.setLong(2, customerId)
                    ps.setLong(3, req.stallId)
                    ps.setLong(4, subtotalCents)
                    ps.setLong(5, totalCents)
                    ps.setString(6, req.notes?.trim()?.takeIf { it.isNotEmpty() })
                    ps.setString(7, effectiveIdempotencyKey)
                    ps.setTimestamp(8, Timestamp.from(now.toInstant()))
                    ps.setTimestamp(9, Timestamp.from(now.toInstant()))
                    ps.executeUpdate()
                    ps.generatedKeys.use { rs ->
                        rs.next()
                        rs.getLong(1)
                    }
                }

                val finalFolio = "A-${1040 + orderId}"
                conn.prepareStatement("UPDATE orders SET folio = ? WHERE id = ?").use { ps ->
                    ps.setString(1, finalFolio)
                    ps.setLong(2, orderId)
                    ps.executeUpdate()
                }

                for (line in validatedLines) {
                    conn.prepareStatement(
                        """
                        INSERT INTO order_items (order_id, product_id, product_name, quantity, unit_price_cents, line_total_cents)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """.trimIndent()
                    ).use { ps ->
                        ps.setLong(1, orderId)
                        ps.setLong(2, line.productId)
                        ps.setString(3, line.productName)
                        ps.setInt(4, line.quantity)
                        ps.setLong(5, line.unitPriceCents)
                        ps.setLong(6, line.lineTotalCents)
                        ps.executeUpdate()
                    }
                }

                val createdDto = loadOrderById(conn, orderId)!!
                conn.commit()
                createdDto to true
            } catch (ex: SQLException) {
                conn.rollback()
                // En caso de condición de carrera en idempotency_key, recuperar pedido existente
                val existingId = findOrderByIdempotencyKey(conn, customerId, effectiveIdempotencyKey)
                if (existingId != null) {
                    return@use loadOrderById(conn, existingId)!! to false
                }
                throw ex
            } catch (ex: Exception) {
                conn.rollback()
                throw ex
            } finally {
                conn.autoCommit = prevAuto
            }
        }
    }

    fun getCustomerOrders(customerId: Long): List<OrderDto> {
        return dataSource.connection.use { conn ->
            val orderIds = mutableListOf<Long>()
            conn.prepareStatement("SELECT id FROM orders WHERE customer_id = ? ORDER BY created_at DESC, id DESC").use { ps ->
                ps.setLong(1, customerId)
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        orderIds.add(rs.getLong("id"))
                    }
                }
            }
            orderIds.mapNotNull { loadOrderById(conn, it) }
        }
    }

    fun getOrderDetailForUser(user: UserDto, orderId: Long): OrderDto {
        return dataSource.connection.use { conn ->
            val order = loadOrderById(conn, orderId)
                ?: throw NotFoundException("El pedido solicitado no existe.")
            when (user.role) {
                UserRole.CLIENTE -> {
                    if (order.customerId != user.id) {
                        throw ForbiddenException("No tienes permiso para consultar pedidos de otro cliente.")
                    }
                }
                UserRole.VENDEDOR -> {
                    val stallVendorId = getStallVendorId(conn, order.stallId)
                    if (stallVendorId != user.id) {
                        throw ForbiddenException("No tienes permiso para consultar pedidos de otro vendedor.")
                    }
                }
            }
            order
        }
    }

    fun createRating(customerId: Long, orderId: Long, req: CreateRatingRequest): RatingDto {
        val scores = listOf(
            req.overallScore,
            req.productQualityScore,
            req.waitTimeScore,
            req.hygieneScore,
            req.serviceAttentionScore
        )
        if (scores.any { it !in 1..5 }) {
            throw BadRequestException("Todas las calificaciones deben estar en la escala de 1 a 5 estrellas.")
        }
        val cleanComment = req.comment?.trim()?.takeIf { it.isNotEmpty() }
        if (cleanComment != null && cleanComment.length > 250) {
            throw BadRequestException("El comentario no puede exceder 250 caracteres.")
        }

        return dataSource.connection.use { conn ->
            val prevAuto = conn.autoCommit
            conn.autoCommit = false
            try {
                val orderInfo = conn.prepareStatement(
                    "SELECT id, folio, customer_id, stall_id, status FROM orders WHERE id = ? FOR UPDATE"
                ).use { ps ->
                    ps.setLong(1, orderId)
                    ps.executeQuery().use { rs ->
                        if (!rs.next()) null
                        else OrderBasicRow(
                            id = rs.getLong("id"),
                            folio = rs.getString("folio"),
                            customerId = rs.getLong("customer_id"),
                            stallId = rs.getLong("stall_id"),
                            status = OrderStatus.valueOf(rs.getString("status"))
                        )
                    }
                } ?: throw NotFoundException("El pedido que intentas calificar no existe.")

                if (orderInfo.customerId != customerId) {
                    throw ForbiddenException("No puedes calificar un pedido que pertenece a otro cliente.")
                }

                if (orderInfo.status != OrderStatus.ENTREGADO) {
                    throw ConflictException("Solo se pueden calificar pedidos cuyo estado sea ENTREGADO (estado actual: ${orderInfo.status}).")
                }

                conn.prepareStatement("SELECT id FROM ratings WHERE order_id = ?").use { ps ->
                    ps.setLong(1, orderId)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) {
                            throw ConflictException("Este pedido ya cuenta con una calificación registrada.")
                        }
                    }
                }

                val now = nowOffset()
                val ratingId = conn.prepareStatement(
                    """
                    INSERT INTO ratings (
                        order_id, customer_id, stall_id, overall_score,
                        product_quality_score, wait_time_score, hygiene_score, service_attention_score,
                        comment, created_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    Statement.RETURN_GENERATED_KEYS
                ).use { ps ->
                    ps.setLong(1, orderId)
                    ps.setLong(2, customerId)
                    ps.setLong(3, orderInfo.stallId)
                    ps.setInt(4, req.overallScore)
                    ps.setInt(5, req.productQualityScore)
                    ps.setInt(6, req.waitTimeScore)
                    ps.setInt(7, req.hygieneScore)
                    ps.setInt(8, req.serviceAttentionScore)
                    ps.setString(9, cleanComment)
                    ps.setTimestamp(10, Timestamp.from(now.toInstant()))
                    ps.executeUpdate()
                    ps.generatedKeys.use { rs ->
                        rs.next()
                        rs.getLong(1)
                    }
                }

                val ratingDto = loadRatingById(conn, ratingId)!!
                conn.commit()
                ratingDto
            } catch (ex: Exception) {
                conn.rollback()
                throw ex
            } finally {
                conn.autoCommit = prevAuto
            }
        }
    }

    // =========================================================================
    // 5. PANEL DE VENDEDOR (PUESTOS, PRODUCTOS Y TRANSICIÓN DE PEDIDOS)
    // =========================================================================

    fun getVendorStalls(vendorId: Long): List<StallDetailDto> {
        return dataSource.connection.use { conn ->
            val stalls = loadAllStalls(conn, emptySet()).filter { it.vendorId == vendorId }
            stalls.map { stall ->
                StallDetailDto(
                    stall = stall,
                    products = loadProductsForStall(conn, stall.id, stall.name, stall.campusLocation, stall.averageRating),
                    recentRatings = loadRatingsForStall(conn, stall.id)
                )
            }
        }
    }

    fun createStall(vendorId: Long, req: UpsertStallRequest): StallDetailDto {
        validateUpsertStall(req)
        return dataSource.connection.use { conn ->
            val canonicalCategory = resolveCategory(conn, req.category)
            val resolvedImageKey = req.imageKey.trim().ifEmpty { canonicalCategory.second }
            val now = Timestamp.from(nowOffset().toInstant())

            val stallId = conn.prepareStatement(
                """
                INSERT INTO stalls (vendor_id, name, description, campus_location, category, prep_time_minutes, is_open, image_key, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setLong(1, vendorId)
                ps.setString(2, req.name.trim())
                ps.setString(3, req.description.trim())
                ps.setString(4, req.campusLocation.trim())
                ps.setString(5, canonicalCategory.first)
                ps.setInt(6, req.prepTimeMinutes)
                ps.setBoolean(7, req.isOpen)
                ps.setString(8, resolvedImageKey)
                ps.setTimestamp(9, now)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    rs.getLong(1)
                }
            }
            getStallDetail(stallId, currentUserId = vendorId)
        }
    }

    fun updateStall(vendorId: Long, stallId: Long, req: UpsertStallRequest): StallDetailDto {
        validateUpsertStall(req)
        return dataSource.connection.use { conn ->
            val ownerId = getStallVendorId(conn, stallId)
                ?: throw NotFoundException("El puesto indicado no existe.")
            if (ownerId != vendorId) {
                throw ForbiddenException("No tienes permiso para modificar un puesto de otro vendedor.")
            }
            val canonicalCategory = resolveCategory(conn, req.category)
            val resolvedImageKey = req.imageKey.trim().ifEmpty { canonicalCategory.second }

            conn.prepareStatement(
                """
                UPDATE stalls
                SET name = ?, description = ?, campus_location = ?, category = ?, prep_time_minutes = ?, is_open = ?, image_key = ?
                WHERE id = ?
                """.trimIndent()
            ).use { ps ->
                ps.setString(1, req.name.trim())
                ps.setString(2, req.description.trim())
                ps.setString(3, req.campusLocation.trim())
                ps.setString(4, canonicalCategory.first)
                ps.setInt(5, req.prepTimeMinutes)
                ps.setBoolean(6, req.isOpen)
                ps.setString(7, resolvedImageKey)
                ps.setLong(8, stallId)
                ps.executeUpdate()
            }
            getStallDetail(stallId, currentUserId = vendorId)
        }
    }

    fun createProduct(vendorId: Long, stallId: Long, req: UpsertProductRequest): ProductDto {
        validateUpsertProduct(req)
        return dataSource.connection.use { conn ->
            val ownerId = getStallVendorId(conn, stallId)
                ?: throw NotFoundException("El puesto indicado no existe.")
            if (ownerId != vendorId) {
                throw ForbiddenException("No tienes permiso para agregar productos a un puesto de otro vendedor.")
            }
            verifyCategoryExists(conn, req.categoryId)

            val now = Timestamp.from(nowOffset().toInstant())
            val productId = conn.prepareStatement(
                """
                INSERT INTO products (stall_id, category_id, name, description, price_cents, available, image_key, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                Statement.RETURN_GENERATED_KEYS
            ).use { ps ->
                ps.setLong(1, stallId)
                ps.setLong(2, req.categoryId)
                ps.setString(3, req.name.trim())
                ps.setString(4, req.description.trim())
                ps.setLong(5, req.priceCents)
                ps.setBoolean(6, req.available)
                ps.setString(7, req.imageKey.trim().ifEmpty { "taco" })
                ps.setTimestamp(8, now)
                ps.setTimestamp(9, now)
                ps.executeUpdate()
                ps.generatedKeys.use { rs ->
                    rs.next()
                    rs.getLong(1)
                }
            }
            getProductById(productId)
        }
    }

    fun updateProduct(vendorId: Long, productId: Long, req: UpsertProductRequest): ProductDto {
        validateUpsertProduct(req)
        return dataSource.connection.use { conn ->
            val productOwner = getProductVendorId(conn, productId)
                ?: throw NotFoundException("El producto indicado no existe.")
            if (productOwner != vendorId) {
                throw ForbiddenException("No tienes permiso para modificar productos de otro vendedor.")
            }
            verifyCategoryExists(conn, req.categoryId)

            val now = Timestamp.from(nowOffset().toInstant())
            conn.prepareStatement(
                """
                UPDATE products
                SET category_id = ?, name = ?, description = ?, price_cents = ?, available = ?, image_key = ?, updated_at = ?
                WHERE id = ?
                """.trimIndent()
            ).use { ps ->
                ps.setLong(1, req.categoryId)
                ps.setString(2, req.name.trim())
                ps.setString(3, req.description.trim())
                ps.setLong(4, req.priceCents)
                ps.setBoolean(5, req.available)
                ps.setString(6, req.imageKey.trim().ifEmpty { "taco" })
                ps.setTimestamp(7, now)
                ps.setLong(8, productId)
                ps.executeUpdate()
            }
            getProductById(productId)
        }
    }

    fun updateProductAvailability(vendorId: Long, productId: Long, available: Boolean): ProductDto {
        return dataSource.connection.use { conn ->
            val productOwner = getProductVendorId(conn, productId)
                ?: throw NotFoundException("El producto indicado no existe.")
            if (productOwner != vendorId) {
                throw ForbiddenException("No tienes permiso para modificar la disponibilidad de productos de otro vendedor.")
            }
            val now = Timestamp.from(nowOffset().toInstant())
            conn.prepareStatement("UPDATE products SET available = ?, updated_at = ? WHERE id = ?").use { ps ->
                ps.setBoolean(1, available)
                ps.setTimestamp(2, now)
                ps.setLong(3, productId)
                ps.executeUpdate()
            }
            getProductById(productId)
        }
    }

    fun getVendorOrders(vendorId: Long, stallId: Long?, statusFilter: OrderStatus?): List<OrderDto> {
        return dataSource.connection.use { conn ->
            if (stallId != null) {
                val ownerId = getStallVendorId(conn, stallId)
                    ?: throw NotFoundException("El puesto indicado no existe.")
                if (ownerId != vendorId) {
                    throw ForbiddenException("No tienes permiso para consultar pedidos de un puesto de otro vendedor.")
                }
            }

            val sql = StringBuilder(
                """
                SELECT o.id
                FROM orders o
                JOIN stalls s ON s.id = o.stall_id
                WHERE s.vendor_id = ?
                """.trimIndent()
            )
            if (stallId != null) {
                sql.append(" AND o.stall_id = ?")
            }
            if (statusFilter != null) {
                sql.append(" AND o.status = ?")
            }
            sql.append(" ORDER BY o.created_at DESC, o.id DESC")

            val orderIds = mutableListOf<Long>()
            conn.prepareStatement(sql.toString()).use { ps ->
                var idx = 1
                ps.setLong(idx++, vendorId)
                if (stallId != null) {
                    ps.setLong(idx++, stallId)
                }
                if (statusFilter != null) {
                    ps.setString(idx, statusFilter.name)
                }
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        orderIds.add(rs.getLong("id"))
                    }
                }
            }
            orderIds.mapNotNull { loadOrderById(conn, it) }
        }
    }

    fun updateOrderStatus(vendorId: Long, orderId: Long, req: UpdateOrderStatusRequest): OrderDto {
        return dataSource.connection.use { conn ->
            val prevAuto = conn.autoCommit
            conn.autoCommit = false
            try {
                val row = conn.prepareStatement(
                    """
                    SELECT o.id, o.folio, o.customer_id, o.stall_id, o.status, s.vendor_id
                    FROM orders o
                    JOIN stalls s ON s.id = o.stall_id
                    WHERE o.id = ?
                    FOR UPDATE
                    """.trimIndent()
                ).use { ps ->
                    ps.setLong(1, orderId)
                    ps.executeQuery().use { rs ->
                        if (!rs.next()) null
                        else Pair(
                            OrderBasicRow(
                                id = rs.getLong("id"),
                                folio = rs.getString("folio"),
                                customerId = rs.getLong("customer_id"),
                                stallId = rs.getLong("stall_id"),
                                status = OrderStatus.valueOf(rs.getString("status"))
                            ),
                            rs.getLong("vendor_id")
                        )
                    }
                } ?: throw NotFoundException("El pedido indicado no existe.")

                val (orderInfo, stallOwnerId) = row
                if (stallOwnerId != vendorId) {
                    throw ForbiddenException("No puedes cambiar el estado de un pedido que pertenece a otro vendedor.")
                }

                if (req.expectedCurrentStatus != null && req.expectedCurrentStatus != orderInfo.status) {
                    throw ConflictException(
                        "Conflicto de concurrencia: el pedido ${orderInfo.folio} ya cambió a estado ${orderInfo.status}."
                    )
                }

                if (!orderInfo.status.canTransitionTo(req.status)) {
                    throw ConflictException(
                        "Transición de estado ilegal: no se puede pasar de ${orderInfo.status} a ${req.status}."
                    )
                }

                val nowTs = Timestamp.from(nowOffset().toInstant())
                when (req.status) {
                    OrderStatus.ENTREGADO -> {
                        conn.prepareStatement(
                            """
                            UPDATE orders
                            SET status = 'ENTREGADO',
                                payment_status = 'COBRADO_EN_EFECTIVO',
                                delivered_at = ?,
                                paid_at = ?,
                                updated_at = ?
                            WHERE id = ?
                            """.trimIndent()
                        ).use { ps ->
                            ps.setTimestamp(1, nowTs)
                            ps.setTimestamp(2, nowTs)
                            ps.setTimestamp(3, nowTs)
                            ps.setLong(4, orderId)
                            ps.executeUpdate()
                        }
                    }
                    OrderStatus.CANCELADO -> {
                        conn.prepareStatement(
                            """
                            UPDATE orders
                            SET status = 'CANCELADO',
                                payment_status = 'CANCELADO',
                                updated_at = ?
                            WHERE id = ?
                            """.trimIndent()
                        ).use { ps ->
                            ps.setTimestamp(1, nowTs)
                            ps.setLong(2, orderId)
                            ps.executeUpdate()
                        }
                    }
                    else -> {
                        conn.prepareStatement(
                            "UPDATE orders SET status = ?, updated_at = ? WHERE id = ?"
                        ).use { ps ->
                            ps.setString(1, req.status.name)
                            ps.setTimestamp(2, nowTs)
                            ps.setLong(3, orderId)
                            ps.executeUpdate()
                        }
                    }
                }

                val updatedOrder = loadOrderById(conn, orderId)!!
                conn.commit()
                updatedOrder
            } catch (ex: Exception) {
                conn.rollback()
                throw ex
            } finally {
                conn.autoCommit = prevAuto
            }
        }
    }

    // =========================================================================
    // HELPERS PRIVADOS DE ACCESO A DATOS
    // =========================================================================

    private data class OrderBasicRow(
        val id: Long,
        val folio: String,
        val customerId: Long,
        val stallId: Long,
        val status: OrderStatus
    )

    private fun validateUpsertStall(req: UpsertStallRequest) {
        if (req.name.trim().length < 2) {
            throw BadRequestException("El nombre del puesto debe tener al menos 2 caracteres.")
        }
        if (req.description.trim().length < 4) {
            throw BadRequestException("La descripción del puesto debe tener al menos 4 caracteres.")
        }
        if (req.campusLocation.trim().length < 4) {
            throw BadRequestException("La ubicación textual del puesto debe tener al menos 4 caracteres.")
        }
        if (req.category.trim().isEmpty()) {
            throw BadRequestException("Debes seleccionar una categoría para el puesto.")
        }
        if (req.prepTimeMinutes !in 1..120) {
            throw BadRequestException("El tiempo estimado de preparación debe estar entre 1 y 120 minutos.")
        }
    }

    private fun resolveCategory(conn: Connection, categoryInput: String): Pair<String, String> {
        val clean = categoryInput.trim()
        conn.prepareStatement(
            "SELECT name, slug FROM categories WHERE LOWER(name) = LOWER(?) OR LOWER(slug) = LOWER(?)"
        ).use { ps ->
            ps.setString(1, clean)
            ps.setString(2, clean)
            ps.executeQuery().use { rs ->
                if (rs.next()) {
                    return rs.getString("name") to rs.getString("slug")
                }
            }
        }
        throw BadRequestException("La categoría '$clean' no existe en las categorías de referencia.")
    }

    private fun validateUpsertProduct(req: UpsertProductRequest) {
        if (req.name.trim().length < 2) {
            throw BadRequestException("El nombre del producto debe tener al menos 2 caracteres.")
        }
        if (req.description.trim().length < 4) {
            throw BadRequestException("La descripción del producto debe tener al menos 4 caracteres.")
        }
        if (req.priceCents <= 0) {
            throw BadRequestException("El precio del producto debe ser mayor a $0.00 MXN.")
        }
    }

    private fun verifyCategoryExists(conn: Connection, categoryId: Long) {
        conn.prepareStatement("SELECT id FROM categories WHERE id = ?").use { ps ->
            ps.setLong(1, categoryId)
            ps.executeQuery().use { rs ->
                if (!rs.next()) {
                    throw BadRequestException("La categoría seleccionada ($categoryId) no existe.")
                }
            }
        }
    }

    private fun getStallVendorId(conn: Connection, stallId: Long): Long? {
        conn.prepareStatement("SELECT vendor_id FROM stalls WHERE id = ?").use { ps ->
            ps.setLong(1, stallId)
            ps.executeQuery().use { rs ->
                return if (rs.next()) rs.getLong("vendor_id") else null
            }
        }
    }

    private fun getProductVendorId(conn: Connection, productId: Long): Long? {
        conn.prepareStatement(
            """
            SELECT s.vendor_id
            FROM products p
            JOIN stalls s ON s.id = p.stall_id
            WHERE p.id = ?
            """.trimIndent()
        ).use { ps ->
            ps.setLong(1, productId)
            ps.executeQuery().use { rs ->
                return if (rs.next()) rs.getLong("vendor_id") else null
            }
        }
    }

    private fun getFavoriteStallIds(conn: Connection, userId: Long): Set<Long> {
        val set = mutableSetOf<Long>()
        conn.prepareStatement("SELECT stall_id FROM favorites WHERE user_id = ?").use { ps ->
            ps.setLong(1, userId)
            ps.executeQuery().use { rs ->
                while (rs.next()) {
                    set.add(rs.getLong("stall_id"))
                }
            }
        }
        return set
    }

    private fun loadStallRatingStats(conn: Connection, stallId: Long): Pair<Double, Int> {
        conn.prepareStatement(
            "SELECT COUNT(*) AS cnt, AVG(CAST(overall_score AS DOUBLE PRECISION)) AS avg_score FROM ratings WHERE stall_id = ?"
        ).use { ps ->
            ps.setLong(1, stallId)
            ps.executeQuery().use { rs ->
                if (rs.next()) {
                    val count = rs.getInt("cnt")
                    if (count > 0) {
                        val rawAvg = rs.getDouble("avg_score")
                        val rounded = (rawAvg * 10.0).roundToInt() / 10.0
                        return rounded to count
                    }
                }
                return 4.9 to 0
            }
        }
    }

    private fun loadAllStalls(conn: Connection, favoriteStallIds: Set<Long>): List<StallSummaryDto> {
        val list = mutableListOf<StallSummaryDto>()
        conn.prepareStatement(
            """
            SELECT s.id, s.vendor_id, u.name AS vendor_name, s.name, s.description,
                   s.campus_location, s.category, s.prep_time_minutes, s.is_open, s.image_key
            FROM stalls s
            JOIN users u ON u.id = s.vendor_id
            ORDER BY s.id ASC
            """.trimIndent()
        ).use { ps ->
            ps.executeQuery().use { rs ->
                while (rs.next()) {
                    val stallId = rs.getLong("id")
                    val stats = loadStallRatingStats(conn, stallId)
                    list.add(
                        StallSummaryDto(
                            id = stallId,
                            vendorId = rs.getLong("vendor_id"),
                            vendorName = rs.getString("vendor_name"),
                            name = rs.getString("name"),
                            description = rs.getString("description"),
                            campusLocation = rs.getString("campus_location"),
                            category = rs.getString("category"),
                            prepTimeMinutes = rs.getInt("prep_time_minutes"),
                            isOpen = rs.getBoolean("is_open"),
                            imageKey = rs.getString("image_key"),
                            averageRating = stats.first,
                            ratingCount = stats.second,
                            isFavorite = stallId in favoriteStallIds
                        )
                    )
                }
            }
        }
        return list
    }

    private fun loadStallById(conn: Connection, stallId: Long, favoriteStallIds: Set<Long>): StallSummaryDto? {
        return loadAllStalls(conn, favoriteStallIds).firstOrNull { it.id == stallId }
    }

    private fun stallHasProductInCategory(conn: Connection, stallId: Long, categoryName: String): Boolean {
        conn.prepareStatement(
            """
            SELECT 1 FROM products p
            JOIN categories c ON c.id = p.category_id
            WHERE p.stall_id = ? AND (LOWER(c.name) = LOWER(?) OR LOWER(c.slug) = LOWER(?))
            """.trimIndent()
        ).use { ps ->
            ps.setLong(1, stallId)
            ps.setString(2, categoryName)
            ps.setString(3, categoryName)
            ps.executeQuery().use { rs ->
                return rs.next()
            }
        }
    }

    private fun stallHasProductMatchingSearch(conn: Connection, stallId: Long, query: String): Boolean {
        conn.prepareStatement(
            """
            SELECT 1 FROM products p
            WHERE p.stall_id = ? AND (LOWER(p.name) LIKE ? OR LOWER(p.description) LIKE ?)
            """.trimIndent()
        ).use { ps ->
            ps.setLong(1, stallId)
            ps.setString(2, "%$query%")
            ps.setString(3, "%$query%")
            ps.executeQuery().use { rs ->
                return rs.next()
            }
        }
    }

    private fun loadProductsForStall(
        conn: Connection,
        stallId: Long,
        stallName: String,
        campusLocation: String,
        stallRating: Double
    ): List<ProductDto> {
        val list = mutableListOf<ProductDto>()
        conn.prepareStatement(
            """
            SELECT p.id, p.stall_id, p.category_id, c.name AS category_name,
                   p.name, p.description, p.price_cents, p.available, p.image_key
            FROM products p
            JOIN categories c ON c.id = p.category_id
            WHERE p.stall_id = ?
            ORDER BY p.available DESC, p.id ASC
            """.trimIndent()
        ).use { ps ->
            ps.setLong(1, stallId)
            ps.executeQuery().use { rs ->
                while (rs.next()) {
                    val priceCents = rs.getLong("price_cents")
                    list.add(
                        ProductDto(
                            id = rs.getLong("id"),
                            stallId = stallId,
                            stallName = stallName,
                            campusLocation = campusLocation,
                            categoryId = rs.getLong("category_id"),
                            categoryName = rs.getString("category_name"),
                            name = rs.getString("name"),
                            description = rs.getString("description"),
                            priceCents = priceCents,
                            formattedPrice = formatMxn(priceCents),
                            available = rs.getBoolean("available"),
                            imageKey = rs.getString("image_key"),
                            stallRating = stallRating
                        )
                    )
                }
            }
        }
        return list
    }

    private fun loadRatingsForStall(conn: Connection, stallId: Long): List<RatingDto> {
        val list = mutableListOf<RatingDto>()
        conn.prepareStatement(
            """
            SELECT r.id, r.order_id, o.folio AS order_folio, r.customer_id, u.name AS customer_name,
                   r.stall_id, r.overall_score, r.product_quality_score, r.wait_time_score,
                   r.hygiene_score, r.service_attention_score, r.comment, r.created_at
            FROM ratings r
            JOIN orders o ON o.id = r.order_id
            JOIN users u ON u.id = r.customer_id
            WHERE r.stall_id = ?
            ORDER BY r.created_at DESC, r.id DESC
            """.trimIndent()
        ).use { ps ->
            ps.setLong(1, stallId)
            ps.executeQuery().use { rs ->
                while (rs.next()) {
                    list.add(mapRatingRow(rs))
                }
            }
        }
        return list
    }

    private fun loadRatingById(conn: Connection, ratingId: Long): RatingDto? {
        conn.prepareStatement(
            """
            SELECT r.id, r.order_id, o.folio AS order_folio, r.customer_id, u.name AS customer_name,
                   r.stall_id, r.overall_score, r.product_quality_score, r.wait_time_score,
                   r.hygiene_score, r.service_attention_score, r.comment, r.created_at
            FROM ratings r
            JOIN orders o ON o.id = r.order_id
            JOIN users u ON u.id = r.customer_id
            WHERE r.id = ?
            """.trimIndent()
        ).use { ps ->
            ps.setLong(1, ratingId)
            ps.executeQuery().use { rs ->
                return if (rs.next()) mapRatingRow(rs) else null
            }
        }
    }

    private fun loadRatingByOrderId(conn: Connection, orderId: Long): RatingDto? {
        conn.prepareStatement(
            """
            SELECT r.id, r.order_id, o.folio AS order_folio, r.customer_id, u.name AS customer_name,
                   r.stall_id, r.overall_score, r.product_quality_score, r.wait_time_score,
                   r.hygiene_score, r.service_attention_score, r.comment, r.created_at
            FROM ratings r
            JOIN orders o ON o.id = r.order_id
            JOIN users u ON u.id = r.customer_id
            WHERE r.order_id = ?
            """.trimIndent()
        ).use { ps ->
            ps.setLong(1, orderId)
            ps.executeQuery().use { rs ->
                return if (rs.next()) mapRatingRow(rs) else null
            }
        }
    }

    private fun mapRatingRow(rs: ResultSet): RatingDto {
        return RatingDto(
            id = rs.getLong("id"),
            orderId = rs.getLong("order_id"),
            orderFolio = rs.getString("order_folio"),
            customerId = rs.getLong("customer_id"),
            customerName = rs.getString("customer_name"),
            stallId = rs.getLong("stall_id"),
            overallScore = rs.getInt("overall_score"),
            productQualityScore = rs.getInt("product_quality_score"),
            waitTimeScore = rs.getInt("wait_time_score"),
            hygieneScore = rs.getInt("hygiene_score"),
            serviceAttentionScore = rs.getInt("service_attention_score"),
            comment = rs.getString("comment"),
            createdAt = rs.getTimestamp("created_at").toIsoString()
        )
    }

    private fun findOrderByIdempotencyKey(conn: Connection, customerId: Long, idempotencyKey: String): Long? {
        conn.prepareStatement("SELECT id FROM orders WHERE customer_id = ? AND idempotency_key = ?").use { ps ->
            ps.setLong(1, customerId)
            ps.setString(2, idempotencyKey)
            ps.executeQuery().use { rs ->
                return if (rs.next()) rs.getLong("id") else null
            }
        }
    }

    private fun loadOrderById(conn: Connection, orderId: Long): OrderDto? {
        val items = mutableListOf<OrderItemDto>()
        conn.prepareStatement(
            """
            SELECT id, product_id, product_name, quantity, unit_price_cents, line_total_cents
            FROM order_items
            WHERE order_id = ?
            ORDER BY id ASC
            """.trimIndent()
        ).use { ps ->
            ps.setLong(1, orderId)
            ps.executeQuery().use { rs ->
                while (rs.next()) {
                    val unitCents = rs.getLong("unit_price_cents")
                    val lineCents = rs.getLong("line_total_cents")
                    items.add(
                        OrderItemDto(
                            id = rs.getLong("id"),
                            productId = rs.getLong("product_id"),
                            productName = rs.getString("product_name"),
                            quantity = rs.getInt("quantity"),
                            unitPriceCents = unitCents,
                            lineTotalCents = lineCents,
                            formattedUnitPrice = formatMxn(unitCents),
                            formattedLineTotal = formatMxn(lineCents)
                        )
                    )
                }
            }
        }

        val rating = loadRatingByOrderId(conn, orderId)

        conn.prepareStatement(
            """
            SELECT o.id, o.folio, o.customer_id, u.name AS customer_name,
                   o.stall_id, s.name AS stall_name, s.campus_location,
                   o.status, o.payment_method, o.payment_status,
                   o.subtotal_cents, o.total_cents, o.notes,
                   o.created_at, o.updated_at, o.delivered_at, o.paid_at
            FROM orders o
            JOIN users u ON u.id = o.customer_id
            JOIN stalls s ON s.id = o.stall_id
            WHERE o.id = ?
            """.trimIndent()
        ).use { ps ->
            ps.setLong(1, orderId)
            ps.executeQuery().use { rs ->
                if (!rs.next()) return null
                val totalCents = rs.getLong("total_cents")
                return OrderDto(
                    id = rs.getLong("id"),
                    folio = rs.getString("folio"),
                    customerId = rs.getLong("customer_id"),
                    customerName = rs.getString("customer_name"),
                    stallId = rs.getLong("stall_id"),
                    stallName = rs.getString("stall_name"),
                    campusLocation = rs.getString("campus_location"),
                    status = OrderStatus.valueOf(rs.getString("status")),
                    paymentMethod = PaymentMethod.valueOf(rs.getString("payment_method")),
                    paymentStatus = PaymentStatus.valueOf(rs.getString("payment_status")),
                    subtotalCents = rs.getLong("subtotal_cents"),
                    totalCents = totalCents,
                    formattedTotal = formatMxn(totalCents),
                    notes = rs.getString("notes"),
                    createdAt = rs.getTimestamp("created_at").toIsoString(),
                    updatedAt = rs.getTimestamp("updated_at").toIsoString(),
                    deliveredAt = rs.getTimestamp("delivered_at")?.toIsoString(),
                    paidAt = rs.getTimestamp("paid_at")?.toIsoString(),
                    items = items,
                    rating = rating
                )
            }
        }
    }
}
