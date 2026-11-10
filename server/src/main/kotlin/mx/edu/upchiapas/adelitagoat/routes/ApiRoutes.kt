    package mx.edu.upchiapas.adelitagoat.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import mx.edu.upchiapas.adelitagoat.models.*
import mx.edu.upchiapas.adelitagoat.services.AdelitaService
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun Route.registerApiRoutes(service: AdelitaService, databaseMode: String) {

    fun ApplicationCall.optionalUser(): UserDto? {
        val header = request.headers[HttpHeaders.Authorization] ?: return null
        return runCatching { service.authenticate(header) }.getOrNull()
    }

    fun ApplicationCall.requireUser(requiredRole: UserRole? = null): UserDto {
        val header = request.headers[HttpHeaders.Authorization]
        val user = service.authenticate(header)
        if (requiredRole != null && user.role != requiredRole) {
            throw ForbiddenException("Esta operación requiere rol $requiredRole (tu rol actual es ${user.role}).")
        }
        return user
    }

    fun ApplicationCall.requireLongParam(name: String): Long {
        val raw = parameters[name] ?: throw BadRequestException("Falta el parámetro '$name'.")
        return raw.toLongOrNull() ?: throw BadRequestException("El parámetro '$name' debe ser un número entero válido.")
    }

    route("/api") {
        get("/health") {
            call.respond(
                HttpStatusCode.OK,
                HealthResponse(
                    status = "UP",
                    service = "AdelitaGOAT API",
                    database = "CONNECTED",
                    databaseMode = databaseMode,
                    timestamp = OffsetDateTime.now(ZoneOffset.ofHours(-6))
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                )
            )
        }

        // 1. AUTENTICACIÓN
        route("/auth") {
            post("/register") {
                val req = call.receive<RegisterRequest>()
                val res = service.register(req)
                call.respond(HttpStatusCode.Created, res)
            }

            post("/login") {
                val req = call.receive<LoginRequest>()
                val res = service.login(req)
                call.respond(HttpStatusCode.OK, res)
            }

            get("/me") {
                val user = call.requireUser()
                call.respond(HttpStatusCode.OK, user)
            }

            post("/logout") {
                call.requireUser()
                service.logout(call.request.headers[HttpHeaders.Authorization])
                call.respond(HttpStatusCode.OK, MessageResponse("Sesión cerrada correctamente."))
            }
        }

        // 2. CATÁLOGO PÚBLICO / CLIENTE
        get("/categories") {
            call.respond(HttpStatusCode.OK, service.getCategories())
        }

        get("/stalls") {
            val user = call.optionalUser()
            val search = call.request.queryParameters["search"]
            val category = call.request.queryParameters["category"]
            val stalls = service.getStalls(search = search, category = category, currentUserId = user?.id)
            call.respond(HttpStatusCode.OK, stalls)
        }

        get("/stalls/{stallId}") {
            val user = call.optionalUser()
            val stallId = call.requireLongParam("stallId")
            val detail = service.getStallDetail(stallId = stallId, currentUserId = user?.id)
            call.respond(HttpStatusCode.OK, detail)
        }

        get("/stalls/{stallId}/ratings") {
            val stallId = call.requireLongParam("stallId")
            val detail = service.getStallDetail(stallId = stallId, currentUserId = null)
            call.respond(HttpStatusCode.OK, detail.recentRatings)
        }

        get("/products/{productId}") {
            val productId = call.requireLongParam("productId")
            val product = service.getProductById(productId)
            call.respond(HttpStatusCode.OK, product)
        }

        // 3. FAVORITOS (SOLO CLIENTE)
        route("/favorites") {
            get {
                val user = call.requireUser(UserRole.CLIENTE)
                call.respond(HttpStatusCode.OK, service.getFavorites(user.id))
            }

            post("/{stallId}") {
                val user = call.requireUser(UserRole.CLIENTE)
                val stallId = call.requireLongParam("stallId")
                call.respond(HttpStatusCode.OK, service.addFavorite(user.id, stallId))
            }

            delete("/{stallId}") {
                val user = call.requireUser(UserRole.CLIENTE)
                val stallId = call.requireLongParam("stallId")
                call.respond(HttpStatusCode.OK, service.removeFavorite(user.id, stallId))
            }
        }

        // 4. APARTADOS / PEDIDOS Y CALIFICACIONES
        route("/orders") {
            post {
                val user = call.requireUser(UserRole.CLIENTE)
                val req = call.receive<CreateOrderRequest>()
                val idempotencyHeader = call.request.headers["Idempotency-Key"]
                val (order, created) = service.createOrder(user.id, req, idempotencyHeader)
                val status = if (created) HttpStatusCode.Created else HttpStatusCode.OK
                call.respond(status, order)
            }

            get {
                val user = call.requireUser(UserRole.CLIENTE)
                call.respond(HttpStatusCode.OK, service.getCustomerOrders(user.id))
            }

            get("/{orderId}") {
                val user = call.requireUser()
                val orderId = call.requireLongParam("orderId")
                call.respond(HttpStatusCode.OK, service.getOrderDetailForUser(user, orderId))
            }

            post("/{orderId}/rating") {
                val user = call.requireUser(UserRole.CLIENTE)
                val orderId = call.requireLongParam("orderId")
                val req = call.receive<CreateRatingRequest>()
                val rating = service.createRating(user.id, orderId, req)
                call.respond(HttpStatusCode.Created, rating)
            }
        }

        // 5. PANEL DE VENDEDOR
        route("/vendor") {
            get("/stalls") {
                val vendor = call.requireUser(UserRole.VENDEDOR)
                call.respond(HttpStatusCode.OK, service.getVendorStalls(vendor.id))
            }

            post("/stalls") {
                val vendor = call.requireUser(UserRole.VENDEDOR)
                val req = call.receive<UpsertStallRequest>()
                val created = service.createStall(vendor.id, req)
                call.respond(HttpStatusCode.Created, created)
            }

            put("/stalls/{stallId}") {
                val vendor = call.requireUser(UserRole.VENDEDOR)
                val stallId = call.requireLongParam("stallId")
                val req = call.receive<UpsertStallRequest>()
                val updated = service.updateStall(vendor.id, stallId, req)
                call.respond(HttpStatusCode.OK, updated)
            }

            post("/stalls/{stallId}/products") {
                val vendor = call.requireUser(UserRole.VENDEDOR)
                val stallId = call.requireLongParam("stallId")
                val req = call.receive<UpsertProductRequest>()
                val created = service.createProduct(vendor.id, stallId, req)
                call.respond(HttpStatusCode.Created, created)
            }

            put("/products/{productId}") {
                val vendor = call.requireUser(UserRole.VENDEDOR)
                val productId = call.requireLongParam("productId")
                val req = call.receive<UpsertProductRequest>()
                val updated = service.updateProduct(vendor.id, productId, req)
                call.respond(HttpStatusCode.OK, updated)
            }

            patch("/products/{productId}/availability") {
                val vendor = call.requireUser(UserRole.VENDEDOR)
                val productId = call.requireLongParam("productId")
                val req = call.receive<UpdateAvailabilityRequest>()
                val updated = service.updateProductAvailability(vendor.id, productId, req.available)
                call.respond(HttpStatusCode.OK, updated)
            }

            get("/orders") {
                val vendor = call.requireUser(UserRole.VENDEDOR)
                val stallId = call.request.queryParameters["stallId"]?.toLongOrNull()
                val statusParam = call.request.queryParameters["status"]?.takeIf { it.isNotBlank() }?.let {
                    runCatching { OrderStatus.valueOf(it.uppercase()) }.getOrElse {
                        throw BadRequestException("Estado de filtro inválido: $it")
                    }
                }
                call.respond(HttpStatusCode.OK, service.getVendorOrders(vendor.id, stallId, statusParam))
            }

            patch("/orders/{orderId}/status") {
                val vendor = call.requireUser(UserRole.VENDEDOR)
                val orderId = call.requireLongParam("orderId")
                val req = call.receive<UpdateOrderStatusRequest>()
                val updated = service.updateOrderStatus(vendor.id, orderId, req)
                call.respond(HttpStatusCode.OK, updated)
            }
        }
    }
}
