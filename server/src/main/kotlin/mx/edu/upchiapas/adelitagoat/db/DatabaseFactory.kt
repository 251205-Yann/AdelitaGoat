package mx.edu.upchiapas.adelitagoat.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import java.io.File
import java.sql.DriverManager
import javax.sql.DataSource

data class DatabaseConnectionInfo(
    val dataSource: HikariDataSource,
    val mode: String,
    val onShutdown: () -> Unit = {}
)

object DatabaseFactory {
    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)

    fun init(
        jdbcUrl: String = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/adelitagoat",
        user: String = System.getenv("DB_USER") ?: "adelita_dev",
        password: String = System.getenv("DB_PASSWORD") ?: "adelita_local_dev_only",
        driver: String = System.getenv("DB_DRIVER") ?: "org.postgresql.Driver",
        allowEmbeddedFallback: Boolean = (System.getenv("DB_ALLOW_EMBEDDED_FALLBACK") ?: "true").toBoolean(),
        seedDemoData: Boolean = (System.getenv("SEED_DEMO_DATA") ?: "false").toBoolean(),
        maxPoolSize: Int = (System.getenv("DB_MAX_POOL_SIZE")?.toIntOrNull() ?: 4).coerceIn(1, 20),
        connectionTimeoutMs: Long = (System.getenv("DB_CONNECTION_TIMEOUT_MS")?.toLongOrNull() ?: 15_000L).coerceAtLeast(1_000L)
    ): DatabaseConnectionInfo {
        val normalizedUrl = normalizePostgresJdbcUrl(jdbcUrl)
        val connectionInfo = createDataSource(
            jdbcUrl = normalizedUrl,
            user = user,
            password = password,
            driver = driver,
            allowEmbeddedFallback = allowEmbeddedFallback,
            maxPoolSize = maxPoolSize,
            connectionTimeoutMs = connectionTimeoutMs
        )
        runMigrations(connectionInfo.dataSource)
        SeedData.seedReferenceCategories(connectionInfo.dataSource)
        if (seedDemoData) {
            SeedData.seedIfNeeded(connectionInfo.dataSource)
        }
        return connectionInfo
    }

    internal fun normalizePostgresJdbcUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.startsWith("jdbc:h2:", ignoreCase = true)) {
            return trimmed
        }
        val withJdbcPrefix = when {
            trimmed.startsWith("postgresql://", ignoreCase = true) ->
                "jdbc:postgresql://" + trimmed.substring("postgresql://".length)
            trimmed.startsWith("postgres://", ignoreCase = true) ->
                "jdbc:postgresql://" + trimmed.substring("postgres://".length)
            else -> trimmed
        }
        if (!withJdbcPrefix.startsWith("jdbc:postgresql://", ignoreCase = true)) {
            return withJdbcPrefix
        }
        val isLocal = isLocalPostgresUrl(withJdbcPrefix)
        val hasSslMode = withJdbcPrefix.contains("sslmode=", ignoreCase = true)
        return if (!isLocal && !hasSslMode) {
            val separator = if (withJdbcPrefix.contains('?')) '&' else '?'
            "$withJdbcPrefix${separator}sslmode=require"
        } else {
            withJdbcPrefix
        }
    }

    internal fun isLocalPostgresUrl(jdbcUrl: String): Boolean {
        val afterPrefix = jdbcUrl.removePrefix("jdbc:postgresql://").substringBefore('/').substringBefore('?')
        val hostPort = afterPrefix.substringAfterLast('@')
        val host = if (hostPort.startsWith("[")) {
            hostPort.substringBefore(']') + "]"
        } else {
            hostPort.substringBefore(':')
        }.lowercase()
        return host == "localhost" || host == "127.0.0.1" || host == "[::1]"
    }

    internal fun sanitizeJdbcUrlForLogs(url: String): String {
        return url
            .replace(Regex("(?i)(password=)[^&;]+"), "$1***")
            .replace(Regex("(?i)(jdbc:postgresql://|postgresql://|postgres://)([^:/?#]+):([^@]+)@"), "$1$2:***@")
    }

    private fun buildPostgresHikariConfig(
        jdbcUrl: String,
        user: String,
        password: String,
        driver: String,
        maxPoolSize: Int,
        connectionTimeoutMs: Long
    ): HikariConfig {
        val boundedPoolSize = maxPoolSize.coerceIn(1, 20)
        val boundedConnTimeout = connectionTimeoutMs.coerceAtLeast(1_000L)
        return HikariConfig().apply {
            this.poolName = "AdelitaPostgresPool"
            this.jdbcUrl = jdbcUrl
            this.username = user
            this.password = password
            this.driverClassName = driver
            // Pool pequeño apropiado para Supabase Session Pooler (puerto 5432, IPv4) y Render Free
            this.maximumPoolSize = boundedPoolSize
            this.minimumIdle = 1.coerceAtMost(boundedPoolSize)
            this.connectionTimeout = boundedConnTimeout
            this.validationTimeout = (boundedConnTimeout / 2).coerceIn(500L, 5_000L)
            this.idleTimeout = 120_000L
            this.maxLifetime = 300_000L
            this.keepaliveTime = 60_000L
            this.isAutoCommit = true
            addDataSourceProperty("ApplicationName", "AdelitaGOAT-Ktor")
            addDataSourceProperty("tcpKeepAlive", "true")
            validate()
        }
    }

    private fun createDataSource(
        jdbcUrl: String,
        user: String,
        password: String,
        driver: String,
        allowEmbeddedFallback: Boolean,
        maxPoolSize: Int,
        connectionTimeoutMs: Long
    ): DatabaseConnectionInfo {
        if (jdbcUrl.startsWith("jdbc:h2:")) {
            val config = HikariConfig().apply {
                this.jdbcUrl = jdbcUrl
                this.username = user
                this.password = password
                this.driverClassName = "org.h2.Driver"
                this.maximumPoolSize = maxPoolSize.coerceIn(1, 20)
                this.isAutoCommit = true
                validate()
            }
            val ds = HikariDataSource(config)
            return DatabaseConnectionInfo(ds, "POSTGRESQL_COMPAT_EMBEDDED") {
                runCatching { ds.close() }
            }
        }

        val sanitizedUrl = sanitizeJdbcUrlForLogs(jdbcUrl)
        val mode = if (isLocalPostgresUrl(jdbcUrl)) {
            "POSTGRESQL_DOCKER_OR_LOCAL"
        } else {
            "POSTGRESQL_SUPABASE_OR_REMOTE"
        }

        if (!allowEmbeddedFallback) {
            return try {
                val config = buildPostgresHikariConfig(
                    jdbcUrl = jdbcUrl,
                    user = user,
                    password = password,
                    driver = driver,
                    maxPoolSize = maxPoolSize,
                    connectionTimeoutMs = connectionTimeoutMs
                )
                val ds = HikariDataSource(config)
                logger.info(
                    "Conectado exitosamente a PostgreSQL ({}) en {} (maxPoolSize={})",
                    mode,
                    sanitizedUrl,
                    config.maximumPoolSize
                )
                DatabaseConnectionInfo(ds, mode) {
                    runCatching { ds.close() }
                }
            } catch (ex: Exception) {
                throw IllegalStateException(
                    "No fue posible conectar a PostgreSQL en $sanitizedUrl y allowEmbeddedFallback=false: ${ex.message}",
                    ex
                )
            }
        }

        // Modo desarrollo local con fallback permitido: prueba rápida hacia PostgreSQL local/remoto
        if (canConnect(jdbcUrl, user, password)) {
            logger.info("Conectado exitosamente a PostgreSQL en {} (maxPoolSize={})", sanitizedUrl, maxPoolSize)
            val config = buildPostgresHikariConfig(
                jdbcUrl = jdbcUrl,
                user = user,
                password = password,
                driver = driver,
                maxPoolSize = maxPoolSize,
                connectionTimeoutMs = connectionTimeoutMs
            )
            val ds = HikariDataSource(config)
            return DatabaseConnectionInfo(ds, mode) {
                runCatching { ds.close() }
            }
        }

        // Try Zonky Embedded PostgreSQL (real postgres binary on local host)
        val tryZonky = (System.getenv("USE_ZONKY_EMBEDDED_PG") ?: "false").toBoolean()
        if (tryZonky) {
            try {
                logger.info("Iniciando instancia PostgreSQL embebida (Zonky)...")
                val dataDir = File("build/embedded-pg-data").apply { mkdirs() }
                val embeddedPg = EmbeddedPostgres.builder()
                    .setDataDirectory(dataDir)
                    .setCleanDataDirectory(false)
                    .start()
                val pgUrl = embeddedPg.getJdbcUrl("postgres", "postgres")
                val config = HikariConfig().apply {
                    this.jdbcUrl = pgUrl
                    this.username = "postgres"
                    this.password = "postgres"
                    this.driverClassName = "org.postgresql.Driver"
                    maximumPoolSize = 10
                    validate()
                }
                val ds = HikariDataSource(config)
                return DatabaseConnectionInfo(ds, "POSTGRESQL_EMBEDDED_NATIVE") {
                    runCatching { ds.close() }
                    runCatching { embeddedPg.close() }
                }
            } catch (ex: Exception) {
                logger.warn("PostgreSQL nativo embebido no disponible ({}), activando motor local compatible PostgreSQL.", ex.message)
            }
        }

        // Persistent local PostgreSQL-compatibility mode database so local development works even when Docker Desktop is not installed
        val dbFile = File("./build/data/adelitagoat_pg").absolutePath.replace("\\", "/")
        val fallbackUrl = "jdbc:h2:file:$dbFile;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
        logger.info("PostgreSQL externo no detectado en {}. Usando persistencia local modo PostgreSQL en {}", sanitizedUrl, fallbackUrl)
        val config = HikariConfig().apply {
            this.jdbcUrl = fallbackUrl
            this.username = user
            this.password = password
            this.driverClassName = "org.h2.Driver"
            maximumPoolSize = 10
            isAutoCommit = true
            validate()
        }
        val ds = HikariDataSource(config)
        return DatabaseConnectionInfo(ds, "POSTGRESQL_LOCAL_FALLBACK") {
            runCatching { ds.close() }
        }
    }

    private fun canConnect(jdbcUrl: String, user: String, password: String): Boolean {
        return try {
            Class.forName("org.postgresql.Driver")
            DriverManager.setLoginTimeout(2)
            DriverManager.getConnection(jdbcUrl, user, password).use { conn ->
                conn.isValid(2)
            }
        } catch (_: Exception) {
            false
        }
    }

    fun runMigrations(dataSource: DataSource) {
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .load()
        flyway.migrate()
    }
}
