package mx.edu.upchiapas.adelitagoat

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.BadRequestException as KtorBadRequestException
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import mx.edu.upchiapas.adelitagoat.db.DatabaseConnectionInfo
import mx.edu.upchiapas.adelitagoat.db.DatabaseFactory
import mx.edu.upchiapas.adelitagoat.models.ApiException
import mx.edu.upchiapas.adelitagoat.models.ErrorResponse
import mx.edu.upchiapas.adelitagoat.routes.registerApiRoutes
import mx.edu.upchiapas.adelitagoat.services.AdelitaService
import org.slf4j.event.Level

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module(customDb: DatabaseConnectionInfo? = null) {
    val dbInfo = customDb ?: DatabaseFactory.init()
    monitor.subscribe(ApplicationStopped) {
        dbInfo.onShutdown()
    }

    val service = AdelitaService(dbInfo.dataSource)

    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = false
                isLenient = true
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
        )
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader("Idempotency-Key")
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
    }

    install(CallLogging) {
        level = Level.INFO
        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val path = call.request.path()
            "$httpMethod $path -> $status"
        }
    }

    install(StatusPages) {
        exception<ApiException> { call, cause ->
            call.respond(
                HttpStatusCode.fromValue(cause.statusCode),
                ErrorResponse(
                    code = cause.code,
                    message = cause.message,
                    details = cause.details
                )
            )
        }
        exception<KtorBadRequestException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    code = "INVALID_PAYLOAD",
                    message = "El cuerpo de la petición es inválido o está incompleto.",
                    details = listOfNotNull(cause.message)
                )
            )
        }
        exception<SerializationException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    code = "SERIALIZATION_ERROR",
                    message = "Formato JSON inválido en la solicitud.",
                    details = listOfNotNull(cause.message)
                )
            )
        }
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Error no controlado en la API", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    code = "INTERNAL_ERROR",
                    message = "Ocurrió un error interno en el servidor."
                )
            )
        }
    }

    routing {
        registerApiRoutes(service, dbInfo.mode)
    }
}
