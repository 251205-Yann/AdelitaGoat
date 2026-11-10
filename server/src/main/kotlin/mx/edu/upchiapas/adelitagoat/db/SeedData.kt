package mx.edu.upchiapas.adelitagoat.db

import org.mindrot.jbcrypt.BCrypt
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.Statement
import java.sql.Timestamp
import java.time.OffsetDateTime
import java.time.ZoneOffset
import javax.sql.DataSource

object SeedData {
    private val logger = LoggerFactory.getLogger(SeedData::class.java)
    private val mexicoOffset = ZoneOffset.ofHours(-6)

    private val referenceCategories = listOf(
        Triple("Tacos", "tacos", 1),
        Triple("Tortas", "tortas", 2),
        Triple("Hamburguesas", "hamburguesas", 3),
        Triple("Quesadillas", "quesadillas", 4),
        Triple("Bebidas", "bebidas", 5),
        Triple("Snacks", "snacks", 6)
    )

    fun seedReferenceCategories(dataSource: DataSource): Map<String, Long> {
        return dataSource.connection.use { conn ->
            val previousAutoCommit = conn.autoCommit
            conn.autoCommit = false
            try {
                val categoryIds = ensureReferenceCategories(conn)
                conn.commit()
                logger.info("Categorías de referencia verificadas de forma idempotente ({} categorías).", categoryIds.size)
                categoryIds
            } catch (ex: Exception) {
                conn.rollback()
                logger.error("Error al inicializar categorías de referencia", ex)
                throw ex
            } finally {
                conn.autoCommit = previousAutoCommit
            }
        }
    }

    private fun ensureReferenceCategories(conn: Connection): Map<String, Long> {
        val categoryIds = mutableMapOf<String, Long>()
        for ((name, slug, order) in referenceCategories) {
            categoryIds[slug] = ensureCategory(conn, name, slug, order)
        }
        return categoryIds
    }

    fun seedIfNeeded(dataSource: DataSource) {
        dataSource.connection.use { conn ->
            val previousAutoCommit = conn.autoCommit
            conn.autoCommit = false
            try {
                // 1. Seed Categories idempotently
                val categoryIds = ensureReferenceCategories(conn)

                // 2. Seed Demo Users idempotently (using RFC 2606 .example.invalid test domains)
                val customer1Id = ensureUser(
                    conn = conn,
                    name = "Cliente Demo Uno",
                    email = "cliente1.demo@example.invalid",
                    plainPassword = "Cliente123!",
                    phone = "9610000001",
                    role = "CLIENTE"
                )
                val customer2Id = ensureUser(
                    conn = conn,
                    name = "Cliente Demo Dos",
                    email = "cliente2.demo@example.invalid",
                    plainPassword = "Cliente123!",
                    phone = "9610000002",
                    role = "CLIENTE"
                )
                val vendor1Id = ensureUser(
                    conn = conn,
                    name = "Vendedor Demo Uno",
                    email = "vendedor1.demo@example.invalid",
                    plainPassword = "Vendedor123!",
                    phone = "9610000003",
                    role = "VENDEDOR"
                )
                val vendor2Id = ensureUser(
                    conn = conn,
                    name = "Vendedor Demo Dos",
                    email = "vendedor2.demo@example.invalid",
                    plainPassword = "Vendedor123!",
                    phone = "9610000004",
                    role = "VENDEDOR"
                )

                // 3. Seed Demo Stalls idempotently
                val stallAdelitaId = ensureStall(
                    conn = conn,
                    vendorId = vendor1Id,
                    name = "AdelitaGOAT - Tacos y Tortas",
                    description = "Tortillas recién hechas, carne asada al carbón, tortas crujientes y aguas frescas frente a la UP Chiapas.",
                    campusLocation = "Frente a la Universidad Politécnica de Chiapas",
                    category = "Tacos",
                    prepTimeMinutes = 8,
                    imageKey = "tacos"
                )

                val stallElSolId = ensureStall(
                    conn = conn,
                    vendorId = vendor1Id,
                    name = "Tacos El Sol",
                    description = "Especialistas en tacos al pastor con piña asada, gringas con queso Oaxaca y salsas artesanales.",
                    campusLocation = "Andador Sur - Frente a la UP Chiapas",
                    category = "Tacos",
                    prepTimeMinutes = 10,
                    imageKey = "pastor"
                )

                val stallMecheId = ensureStall(
                    conn = conn,
                    vendorId = vendor2Id,
                    name = "Jugos y Antojitos Doña Meche",
                    description = "Jugos naturales recién exprimidos, licuados energéticos, quesadillas doradas y empanadas chiapanecas.",
                    campusLocation = "Acceso Principal - Universidad Politécnica de Chiapas",
                    category = "Bebidas",
                    prepTimeMinutes = 6,
                    imageKey = "jugos"
                )

                // 4. Seed Products for Stall 1 (AdelitaGOAT)
                val pOrdenAsada = ensureProduct(
                    conn, stallAdelitaId, categoryIds.getValue("tacos"),
                    "Tacos de carne asada (4 piezas)",
                    "Tortillas de maíz calientes con carne asada de res al carbón, cebolla, cilantro y limones.",
                    6500L, true, "taco_asada"
                )
                val pTacoIndividual = ensureProduct(
                    conn, stallAdelitaId, categoryIds.getValue("tacos"),
                    "Taco de carne asada",
                    "Tortilla de maíz recién hecha, carne asada al carbón, cebolla y cilantro.",
                    2000L, true, "taco"
                )
                val pTortaMilanesa = ensureProduct(
                    conn, stallAdelitaId, categoryIds.getValue("tortas"),
                    "Torta de milanesa",
                    "Milanesa crujiente, quesillo, aguacate, jitomate y frijoles refritos en telera dorada.",
                    5500L, true, "torta"
                )
                val pQuesadillaCarne = ensureProduct(
                    conn, stallAdelitaId, categoryIds.getValue("quesadillas"),
                    "Quesadilla con carne",
                    "Queso Oaxaca derretido con carne asada en tortilla dorada al comal.",
                    3500L, true, "quesadilla"
                )
                val pHamburguesa = ensureProduct(
                    conn, stallAdelitaId, categoryIds.getValue("hamburguesas"),
                    "Hamburguesa sencilla",
                    "Carne de res a la parrilla, queso americano, lechuga, jitomate y aderezos.",
                    6000L, true, "hamburguesa"
                )
                val pHorchata = ensureProduct(
                    conn, stallAdelitaId, categoryIds.getValue("bebidas"),
                    "Agua fresca de horchata",
                    "Receta casera bien fría con canela (500 ml).",
                    2500L, true, "horchata"
                )

                // 5. Seed Products for Stall 2 (Tacos El Sol)
                val pOrdenPastor = ensureProduct(
                    conn, stallElSolId, categoryIds.getValue("tacos"),
                    "Orden de tacos al pastor (5 piezas)",
                    "Carne al pastor adobada con piña asada, cilantro, cebolla y salsa de habanero.",
                    6000L, true, "pastor"
                )
                ensureProduct(
                    conn, stallElSolId, categoryIds.getValue("quesadillas"),
                    "Gringa de pastor con quesillo",
                    "Doble tortilla de harina con carne al pastor y abundante queso Oaxaca.",
                    4500L, true, "quesadilla"
                )
                val pJamaica = ensureProduct(
                    conn, stallElSolId, categoryIds.getValue("bebidas"),
                    "Agua de jamaica natural (500 ml)",
                    "Infusión natural de flor de jamaica con hielo.",
                    2000L, true, "bebida"
                )

                // 6. Seed Products for Stall 3 (Jugos Doña Meche)
                ensureProduct(
                    conn, stallMecheId, categoryIds.getValue("bebidas"),
                    "Jugo energético universitario (500 ml)",
                    "Naranja natural con piña, nopal, apio y perejil recién preparado.",
                    3500L, true, "jugo"
                )
                ensureProduct(
                    conn, stallMecheId, categoryIds.getValue("snacks"),
                    "Orden de empanadas chiapanecas (3 piezas)",
                    "Empanadas de maíz rellenas de queso y pollo con repollo, crema y queso Chiapas.",
                    4500L, true, "snack"
                )
                ensureProduct(
                    conn, stallMecheId, categoryIds.getValue("tortas"),
                    "Torta de cochito horneado",
                    "Receta tradicional chiapaneca con lechuga, cebolla morada y chilmol.",
                    5800L, true, "torta"
                )

                // 7. Seed Initial Favorite for Customer 1
                ensureFavorite(conn, customer1Id, stallAdelitaId)

                // 8. Seed Demo Orders & Rating idempotently
                val now = OffsetDateTime.now(mexicoOffset)

                // Order A-1041: ENTREGADO & already rated (shows up in stall ratings and history)
                val order1041Id = ensureOrder(
                    conn = conn,
                    folio = "A-1041",
                    customerId = customer2Id,
                    stallId = stallAdelitaId,
                    status = "ENTREGADO",
                    paymentStatus = "COBRADO_EN_EFECTIVO",
                    subtotalCents = 7500L,
                    totalCents = 7500L,
                    notes = "Sin cebolla en el taco por favor",
                    idempotencyKey = "seed-order-a1041",
                    createdAt = now.minusHours(3),
                    deliveredAt = now.minusHours(2).minusMinutes(45),
                    paidAt = now.minusHours(2).minusMinutes(45),
                    items = listOf(
                        SeedOrderItem(pTortaMilanesa, "Torta de milanesa", 1, 5500L, 5500L),
                        SeedOrderItem(pTacoIndividual, "Taco de carne asada", 1, 2000L, 2000L)
                    )
                )
                ensureRating(
                    conn = conn,
                    orderId = order1041Id,
                    customerId = customer2Id,
                    stallId = stallAdelitaId,
                    overallScore = 5,
                    productQualityScore = 5,
                    waitTimeScore = 5,
                    hygieneScore = 5,
                    serviceAttentionScore = 5,
                    comment = "Súper rápido en el receso de 20 minutos, la torta estaba recién hecha y calientita."
                )

                // Order A-1042: ENTREGADO for Customer 1 (Yanndel) but NOT YET RATED so user can test rating right away or create new orders
                ensureOrder(
                    conn = conn,
                    folio = "A-1042",
                    customerId = customer1Id,
                    stallId = stallAdelitaId,
                    status = "ENTREGADO",
                    paymentStatus = "COBRADO_EN_EFECTIVO",
                    subtotalCents = 9000L,
                    totalCents = 9000L,
                    notes = "Salsa verde aparte",
                    idempotencyKey = "seed-order-a1042",
                    createdAt = now.minusHours(1),
                    deliveredAt = now.minusMinutes(42),
                    paidAt = now.minusMinutes(42),
                    items = listOf(
                        SeedOrderItem(pOrdenAsada, "Tacos de carne asada (4 piezas)", 1, 6500L, 6500L),
                        SeedOrderItem(pHorchata, "Agua fresca de horchata", 1, 2500L, 2500L)
                    )
                )

                // Order A-1043: Active order in RECIBIDO for Vendor 1 & Customer 1
                ensureOrder(
                    conn = conn,
                    folio = "A-1043",
                    customerId = customer1Id,
                    stallId = stallElSolId,
                    status = "RECIBIDO",
                    paymentStatus = "PENDIENTE_EFECTIVO",
                    subtotalCents = 8000L,
                    totalCents = 8000L,
                    notes = "Con piña extra",
                    idempotencyKey = "seed-order-a1043",
                    createdAt = now.minusMinutes(12),
                    deliveredAt = null,
                    paidAt = null,
                    items = listOf(
                        SeedOrderItem(pOrdenPastor, "Orden de tacos al pastor (5 piezas)", 1, 6000L, 6000L),
                        SeedOrderItem(pJamaica, "Agua de jamaica natural (500 ml)", 1, 2000L, 2000L)
                    )
                )

                conn.commit()
                logger.info("Semillas demo verificadas/aplicadas de forma idempotente.")
            } catch (ex: Exception) {
                conn.rollback()
                logger.error("Error durante la carga de semillas demo", ex)
                throw ex
            } finally {
                conn.autoCommit = previousAutoCommit
            }
        }
    }

    private data class SeedOrderItem(
        val productId: Long,
        val productName: String,
        val quantity: Int,
        val unitPriceCents: Long,
        val lineTotalCents: Long
    )

    private fun ensureCategory(conn: Connection, name: String, slug: String, displayOrder: Int): Long {
        conn.prepareStatement("SELECT id FROM categories WHERE slug = ?").use { ps ->
            ps.setString(1, slug)
            ps.executeQuery().use { rs ->
                if (rs.next()) return rs.getLong("id")
            }
        }
        conn.prepareStatement(
            "INSERT INTO categories (name, slug, display_order) VALUES (?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS
        ).use { ps ->
            ps.setString(1, name)
            ps.setString(2, slug)
            ps.setInt(3, displayOrder)
            ps.executeUpdate()
            ps.generatedKeys.use { rs ->
                rs.next()
                return rs.getLong(1)
            }
        }
    }

    private fun ensureUser(
        conn: Connection,
        name: String,
        email: String,
        plainPassword: String,
        phone: String,
        role: String
    ): Long {
        conn.prepareStatement("SELECT id FROM users WHERE LOWER(email) = LOWER(?)").use { ps ->
            ps.setString(1, email)
            ps.executeQuery().use { rs ->
                if (rs.next()) return rs.getLong("id")
            }
        }
        val passwordHash = BCrypt.hashpw(plainPassword, BCrypt.gensalt(10))
        conn.prepareStatement(
            "INSERT INTO users (name, email, password_hash, phone, role, created_at) VALUES (?, ?, ?, ?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS
        ).use { ps ->
            ps.setString(1, name)
            ps.setString(2, email.lowercase())
            ps.setString(3, passwordHash)
            ps.setString(4, phone)
            ps.setString(5, role)
            ps.setTimestamp(6, Timestamp.from(OffsetDateTime.now(mexicoOffset).toInstant()))
            ps.executeUpdate()
            ps.generatedKeys.use { rs ->
                rs.next()
                return rs.getLong(1)
            }
        }
    }

    private fun ensureStall(
        conn: Connection,
        vendorId: Long,
        name: String,
        description: String,
        campusLocation: String,
        category: String,
        prepTimeMinutes: Int,
        imageKey: String
    ): Long {
        conn.prepareStatement("SELECT id FROM stalls WHERE vendor_id = ? AND name = ?").use { ps ->
            ps.setLong(1, vendorId)
            ps.setString(2, name)
            ps.executeQuery().use { rs ->
                if (rs.next()) return rs.getLong("id")
            }
        }
        conn.prepareStatement(
            """
            INSERT INTO stalls (vendor_id, name, description, campus_location, category, prep_time_minutes, is_open, image_key, created_at)
            VALUES (?, ?, ?, ?, ?, ?, TRUE, ?, ?)
            """.trimIndent(),
            Statement.RETURN_GENERATED_KEYS
        ).use { ps ->
            ps.setLong(1, vendorId)
            ps.setString(2, name)
            ps.setString(3, description)
            ps.setString(4, campusLocation)
            ps.setString(5, category)
            ps.setInt(6, prepTimeMinutes)
            ps.setString(7, imageKey)
            ps.setTimestamp(8, Timestamp.from(OffsetDateTime.now(mexicoOffset).toInstant()))
            ps.executeUpdate()
            ps.generatedKeys.use { rs ->
                rs.next()
                return rs.getLong(1)
            }
        }
    }

    private fun ensureProduct(
        conn: Connection,
        stallId: Long,
        categoryId: Long,
        name: String,
        description: String,
        priceCents: Long,
        available: Boolean,
        imageKey: String
    ): Long {
        conn.prepareStatement("SELECT id FROM products WHERE stall_id = ? AND name = ?").use { ps ->
            ps.setLong(1, stallId)
            ps.setString(2, name)
            ps.executeQuery().use { rs ->
                if (rs.next()) return rs.getLong("id")
            }
        }
        val now = Timestamp.from(OffsetDateTime.now(mexicoOffset).toInstant())
        conn.prepareStatement(
            """
            INSERT INTO products (stall_id, category_id, name, description, price_cents, available, image_key, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            Statement.RETURN_GENERATED_KEYS
        ).use { ps ->
            ps.setLong(1, stallId)
            ps.setLong(2, categoryId)
            ps.setString(3, name)
            ps.setString(4, description)
            ps.setLong(5, priceCents)
            ps.setBoolean(6, available)
            ps.setString(7, imageKey)
            ps.setTimestamp(8, now)
            ps.setTimestamp(9, now)
            ps.executeUpdate()
            ps.generatedKeys.use { rs ->
                rs.next()
                return rs.getLong(1)
            }
        }
    }

    private fun ensureFavorite(conn: Connection, userId: Long, stallId: Long) {
        conn.prepareStatement("SELECT id FROM favorites WHERE user_id = ? AND stall_id = ?").use { ps ->
            ps.setLong(1, userId)
            ps.setLong(2, stallId)
            ps.executeQuery().use { rs ->
                if (rs.next()) return
            }
        }
        conn.prepareStatement("INSERT INTO favorites (user_id, stall_id, created_at) VALUES (?, ?, ?)").use { ps ->
            ps.setLong(1, userId)
            ps.setLong(2, stallId)
            ps.setTimestamp(3, Timestamp.from(OffsetDateTime.now(mexicoOffset).toInstant()))
            ps.executeUpdate()
        }
    }

    private fun ensureOrder(
        conn: Connection,
        folio: String,
        customerId: Long,
        stallId: Long,
        status: String,
        paymentStatus: String,
        subtotalCents: Long,
        totalCents: Long,
        notes: String?,
        idempotencyKey: String,
        createdAt: OffsetDateTime,
        deliveredAt: OffsetDateTime?,
        paidAt: OffsetDateTime?,
        items: List<SeedOrderItem>
    ): Long {
        conn.prepareStatement("SELECT id FROM orders WHERE folio = ?").use { ps ->
            ps.setString(1, folio)
            ps.executeQuery().use { rs ->
                if (rs.next()) return rs.getLong("id")
            }
        }
        val orderId = conn.prepareStatement(
            """
            INSERT INTO orders (
                folio, customer_id, stall_id, status, payment_method, payment_status,
                subtotal_cents, total_cents, notes, idempotency_key,
                created_at, updated_at, delivered_at, paid_at
            ) VALUES (?, ?, ?, ?, 'EFECTIVO_AL_RECOGER', ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            Statement.RETURN_GENERATED_KEYS
        ).use { ps ->
            ps.setString(1, folio)
            ps.setLong(2, customerId)
            ps.setLong(3, stallId)
            ps.setString(4, status)
            ps.setString(5, paymentStatus)
            ps.setLong(6, subtotalCents)
            ps.setLong(7, totalCents)
            ps.setString(8, notes)
            ps.setString(9, idempotencyKey)
            ps.setTimestamp(10, Timestamp.from(createdAt.toInstant()))
            ps.setTimestamp(11, Timestamp.from((deliveredAt ?: createdAt).toInstant()))
            ps.setTimestamp(12, deliveredAt?.let { Timestamp.from(it.toInstant()) })
            ps.setTimestamp(13, paidAt?.let { Timestamp.from(it.toInstant()) })
            ps.executeUpdate()
            ps.generatedKeys.use { rs ->
                rs.next()
                rs.getLong(1)
            }
        }

        for (item in items) {
            conn.prepareStatement(
                """
                INSERT INTO order_items (order_id, product_id, product_name, quantity, unit_price_cents, line_total_cents)
                VALUES (?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { ps ->
                ps.setLong(1, orderId)
                ps.setLong(2, item.productId)
                ps.setString(3, item.productName)
                ps.setInt(4, item.quantity)
                ps.setLong(5, item.unitPriceCents)
                ps.setLong(6, item.lineTotalCents)
                ps.executeUpdate()
            }
        }
        return orderId
    }

    private fun ensureRating(
        conn: Connection,
        orderId: Long,
        customerId: Long,
        stallId: Long,
        overallScore: Int,
        productQualityScore: Int,
        waitTimeScore: Int,
        hygieneScore: Int,
        serviceAttentionScore: Int,
        comment: String
    ) {
        conn.prepareStatement("SELECT id FROM ratings WHERE order_id = ?").use { ps ->
            ps.setLong(1, orderId)
            ps.executeQuery().use { rs ->
                if (rs.next()) return
            }
        }
        conn.prepareStatement(
            """
            INSERT INTO ratings (
                order_id, customer_id, stall_id, overall_score,
                product_quality_score, wait_time_score, hygiene_score, service_attention_score,
                comment, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
        ).use { ps ->
            ps.setLong(1, orderId)
            ps.setLong(2, customerId)
            ps.setLong(3, stallId)
            ps.setInt(4, overallScore)
            ps.setInt(5, productQualityScore)
            ps.setInt(6, waitTimeScore)
            ps.setInt(7, hygieneScore)
            ps.setInt(8, serviceAttentionScore)
            ps.setString(9, comment)
            ps.setTimestamp(10, Timestamp.from(OffsetDateTime.now(mexicoOffset).toInstant()))
            ps.executeUpdate()
        }
    }
}
